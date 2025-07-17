-- ============================================
-- 用户模块
-- ============================================

-- 用户主表
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id BIGSERIAL UNIQUE NOT NULL,

    username VARCHAR(32) NOT NULL UNIQUE,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,

    status user_status DEFAULT 'PENDING' NOT NULL,
    user_type user_type DEFAULT 'USER' NOT NULL,
    email_verified BOOLEAN DEFAULT FALSE NOT NULL,

    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL,
    deleted_at TIMESTAMPTZ,

    CONSTRAINT users_username_format CHECK (username ~ '^[a-zA-Z0-9_]{3,32}$'),
    CONSTRAINT users_email_format CHECK (email ~ '^[^@\s]+@[^@\s]+\.[^@\s]+$')
);

-- 用户资料表
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

-- 用户安全表
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

-- 用户偏好表
CREATE TABLE user_preferences (
    user_id UUID PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    theme VARCHAR(20) DEFAULT 'light',
    language VARCHAR(10) DEFAULT 'zh-CN',
    notifications JSONB DEFAULT '{}',
    preferences JSONB DEFAULT '{}',
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

-- OAuth账户表
CREATE TABLE user_oauth_accounts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
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
-- 索引
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

-- ============================================
-- 触发器
-- ============================================

CREATE TRIGGER update_users_updated_at
    BEFORE UPDATE ON users FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_profiles_updated_at
    BEFORE UPDATE ON user_profiles FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_security_updated_at
    BEFORE UPDATE ON user_security FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER create_user_related_records_trigger
    AFTER INSERT ON users FOR EACH ROW
    EXECUTE FUNCTION create_user_related_records();

-- ============================================
-- 视图
-- ============================================

-- 用户基础信息视图
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
    p.phone,
    p.phone_verified,
    s.last_login_at,
    s.login_attempts,
    s.locked_until,
    CASE WHEN s.locked_until > CURRENT_TIMESTAMP THEN true ELSE false END AS is_locked,
    u.created_at,
    u.updated_at
FROM users u
LEFT JOIN user_profiles p ON u.id = p.user_id
LEFT JOIN user_security s ON u.id = s.user_id
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


-- 核心用户表优化
ALTER TABLE users SET (
    autovacuum_analyze_scale_factor = 0.02,
    autovacuum_vacuum_scale_factor = 0.01,
    autovacuum_vacuum_threshold = 1000
);

-- 用户资料表优化
ALTER TABLE user_profiles SET (
    autovacuum_analyze_scale_factor = 0.05,
    fillfactor = 90  -- 为更新预留空间
);

-- 安全表优化
ALTER TABLE user_security SET (
    autovacuum_analyze_scale_factor = 0.05,
    fillfactor = 85  -- 频繁更新的表
);
