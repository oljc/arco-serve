CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";
CREATE EXTENSION IF NOT EXISTS "pg_trgm";


CREATE TYPE user_status AS ENUM (
  'PENDING',     -- 待激活
  'ACTIVE',      -- 正常使用
  'INACTIVE',    -- 非活跃
  'LOCKED',      -- 登录被锁定
  'SUSPENDED',   -- 风控暂停
  'BANNED',      -- 封禁
  'DELETED'      -- 已删除
);
CREATE TYPE user_type AS ENUM (
    'SUPER_ADMIN',  -- 超级管理员
    'ADMIN',        -- 管理员
    'TEST',         -- 测试账号
    'OPERATOR',     -- 运营审核人员
    'API',          -- API系统集成
    'USER',         -- 普通用户
    'GUEST',        -- 游客、未注册用户
    'BOT'           -- 系统使用账号
);

CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id BIGSERIAL UNIQUE NOT NULL,

    username VARCHAR(32) NOT NULL UNIQUE,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,

    status user_status DEFAULT 'PENDING' NOT NULL,
    user_type user_type DEFAULT 'REGULAR' NOT NULL,
    email_verified BOOLEAN DEFAULT FALSE NOT NULL,

    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL,
    deleted_at TIMESTAMPTZ,

    CONSTRAINT users_username_format CHECK (username ~ '^[a-zA-Z0-9_]{3,32}$'),
    CONSTRAINT users_email_format CHECK (email ~ '^[^@\s]+@[^@\s]+\.[^@\s]+$')
);


CREATE TABLE user_profiles (
    user_id UUID PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    display_name VARCHAR(100),
    real_name VARCHAR(100),
    avatar_url TEXT,
    bio TEXT,
    phone VARCHAR(20) UNIQUE,
    phone_verified BOOLEAN DEFAULT FALSE,
    timezone VARCHAR(50) DEFAULT 'Asia/Shanghai',
    locale VARCHAR(10) DEFAULT 'zh-CN',
    country_code CHAR(2),
    profile_data JSONB DEFAULT '{}',
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE user_security (
    user_id UUID PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    password_salt VARCHAR(64) NOT NULL,
    password_changed_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    last_login_at TIMESTAMPTZ,
    last_login_ip INET,
    login_attempts INTEGER DEFAULT 0,
    locked_until TIMESTAMPTZ,
    security_settings JSONB DEFAULT '{}',
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

-- ============================================
-- 用户偏好（低频访问）
-- ============================================
CREATE TABLE user_preferences (
    user_id UUID PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    theme VARCHAR(20) DEFAULT 'light',
    language VARCHAR(10) DEFAULT 'zh-CN',
    notifications JSONB DEFAULT '{}',
    preferences JSONB DEFAULT '{}',
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

-- ============================================
-- OAuth账户
-- ============================================

CREATE TABLE user_oauth_accounts (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    provider VARCHAR(50) NOT NULL,
    provider_user_id VARCHAR(255) NOT NULL,
    provider_email VARCHAR(255),
    access_token TEXT,
    expires_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,

    UNIQUE(provider, provider_user_id)
);

-- ============================================
-- 角色权限
-- ============================================

CREATE TABLE roles (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(50) NOT NULL UNIQUE,
    display_name VARCHAR(100) NOT NULL,
    permissions JSONB DEFAULT '[]',
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE user_roles (
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role_id UUID NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    assigned_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, role_id)
);

-- ============================================
-- 登录日志（高频写入）
-- ============================================

CREATE TABLE user_login_logs (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    ip_address INET NOT NULL,
    user_agent TEXT,
    success BOOLEAN NOT NULL,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

-- ============================================
-- 验证码
-- ============================================

CREATE TABLE verification_codes (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    email VARCHAR(255),
    phone VARCHAR(20),
    code VARCHAR(10) NOT NULL,
    code_type VARCHAR(20) NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    used_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT verification_email_or_phone CHECK (
        (email IS NOT NULL AND phone IS NULL) OR
        (email IS NULL AND phone IS NOT NULL)
    )
);

-- ============================================
-- 10. 高效索引设计
-- ============================================

-- 核心用户表索引（最高频查询）
CREATE UNIQUE INDEX idx_users_username ON users(username) WHERE deleted_at IS NULL;
CREATE UNIQUE INDEX idx_users_email ON users(email) WHERE deleted_at IS NULL;
CREATE INDEX idx_users_status ON users(status) WHERE deleted_at IS NULL;
CREATE INDEX idx_users_type_status ON users(user_type, status) WHERE deleted_at IS NULL;

-- 用户资料表索引
CREATE UNIQUE INDEX idx_profiles_phone ON user_profiles(phone) WHERE phone IS NOT NULL;
CREATE INDEX idx_profiles_display_name ON user_profiles(display_name) WHERE display_name IS NOT NULL;

-- 安全表索引
CREATE INDEX idx_security_last_login ON user_security(last_login_at);
CREATE INDEX idx_security_locked ON user_security(locked_until) WHERE locked_until IS NOT NULL;

-- OAuth索引
CREATE INDEX idx_oauth_user_id ON user_oauth_accounts(user_id);
CREATE UNIQUE INDEX idx_oauth_provider ON user_oauth_accounts(provider, provider_user_id);

-- 登录日志索引
CREATE INDEX idx_login_logs_user_time ON user_login_logs(user_id, created_at);
CREATE INDEX idx_login_logs_ip ON user_login_logs(ip_address);

-- ============================================
-- 11. 触发器
-- ============================================

-- 更新时间戳
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER update_users_updated_at
    BEFORE UPDATE ON users FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_profiles_updated_at
    BEFORE UPDATE ON user_profiles FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_security_updated_at
    BEFORE UPDATE ON user_security FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- 新用户创建关联记录
CREATE OR REPLACE FUNCTION create_user_related_records()
RETURNS TRIGGER AS $$
BEGIN
    -- 创建用户资料记录
    INSERT INTO user_profiles (user_id) VALUES (NEW.id);

    -- 创建安全记录
    INSERT INTO user_security (user_id, password_salt)
    VALUES (NEW.id, encode(gen_random_bytes(32), 'base64'));

    -- 创建偏好记录
    INSERT INTO user_preferences (user_id) VALUES (NEW.id);

    -- 分配默认角色
    INSERT INTO user_roles (user_id, role_id)
    SELECT NEW.id, id FROM roles WHERE name = 'USER';

    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER create_user_related_records_trigger
    AFTER INSERT ON users FOR EACH ROW
    EXECUTE FUNCTION create_user_related_records();

-- ============================================
-- 12. 常用查询视图
-- ============================================

-- 用户基础信息视图（最常用）
CREATE VIEW user_basic_info AS
SELECT
    u.id,
    u.user_id,
    u.username,
    u.email,
    u.status,
    u.user_type,
    u.email_verified,
    p.display_name,
    p.avatar_url,
    p.phone_verified,
    u.created_at
FROM users u
LEFT JOIN user_profiles p ON u.id = p.user_id
WHERE u.deleted_at IS NULL;

-- 用户完整信息视图（详情页用）
CREATE VIEW user_complete_info AS
SELECT
    u.*,
    p.display_name,
    p.real_name,
    p.avatar_url,
    p.bio,
    p.phone,
    p.phone_verified,
    p.timezone,
    p.locale,
    p.country_code,
    s.last_login_at,
    s.login_attempts
FROM users u
LEFT JOIN user_profiles p ON u.id = p.user_id
LEFT JOIN user_security s ON u.id = s.user_id
WHERE u.deleted_at IS NULL;

-- ============================================
-- 13. 性能优化函数
-- ============================================

-- 用户认证（高频操作）
CREATE OR REPLACE FUNCTION authenticate_user(
    input_username VARCHAR,
    input_password VARCHAR
)
RETURNS TABLE(
    user_id UUID,
    username VARCHAR,
    status user_status,
    password_hash VARCHAR,
    password_salt VARCHAR,
    login_attempts INTEGER
) AS $$
BEGIN
    RETURN QUERY
    SELECT
        u.id,
        u.username,
        u.status,
        u.password_hash,
        s.password_salt,
        s.login_attempts
    FROM users u
    JOIN user_security s ON u.id = s.user_id
    WHERE u.username = input_username
      AND u.deleted_at IS NULL;
END;
$$ LANGUAGE plpgsql;

-- 更新登录信息
CREATE OR REPLACE FUNCTION update_login_info(
    p_user_id UUID,
    p_ip_address INET,
    p_success BOOLEAN
)
RETURNS VOID AS $$
BEGIN
    -- 更新安全表
    UPDATE user_security
    SET
        last_login_at = CASE WHEN p_success THEN CURRENT_TIMESTAMP ELSE last_login_at END,
        last_login_ip = CASE WHEN p_success THEN p_ip_address ELSE last_login_ip END,
        login_attempts = CASE WHEN p_success THEN 0 ELSE login_attempts + 1 END,
        updated_at = CURRENT_TIMESTAMP
    WHERE user_id = p_user_id;

    -- 记录登录日志
    INSERT INTO user_login_logs (user_id, ip_address, success)
    VALUES (p_user_id, p_ip_address, p_success);
END;
$$ LANGUAGE plpgsql;

-- ============================================
-- 14. 初始化数据
-- ============================================

-- 插入系统角色
INSERT INTO roles (name, display_name, permissions) VALUES
('USER', '普通用户', '["user:read", "user:update_self"]'),
('VIP', 'VIP用户', '["user:read", "user:update_self", "vip:access"]'),
('ADMIN', '管理员', '["*"]')
ON CONFLICT (name) DO NOTHING;


ALTER TABLE users SET (autovacuum_analyze_scale_factor = 0.02);
ALTER TABLE user_login_logs SET (autovacuum_analyze_scale_factor = 0.01);

DO $$
BEGIN
    RAISE NOTICE '用户系统数据库初始化完成！';
END $$;