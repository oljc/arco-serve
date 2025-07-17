CREATE EXTENSION IF NOT EXISTS "pgcrypto";
CREATE EXTENSION IF NOT EXISTS "pg_trgm";


-- 更新时间戳
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

-- 新用户创建关联记录
CREATE OR REPLACE FUNCTION create_user_related_records()
RETURNS TRIGGER AS $$
DECLARE
    user_role_id UUID;
BEGIN
    -- 创建用户资料记录
    INSERT INTO user_profiles (user_id) VALUES (NEW.id);

    -- 创建安全记录
    INSERT INTO user_security (user_id, password_salt)
    VALUES (NEW.id, encode(gen_random_bytes(32), 'base64'));

    -- 创建偏好记录
    INSERT INTO user_preferences (user_id) VALUES (NEW.id);

    -- 分配默认角色（安全处理）
    SELECT id INTO user_role_id FROM roles WHERE name = 'USER' LIMIT 1;

    IF user_role_id IS NOT NULL THEN
        INSERT INTO user_roles (user_id, role_id)
        VALUES (NEW.id, user_role_id);
    ELSE
        -- 如果默认角色不存在，记录警告但不阻止用户创建
        RAISE NOTICE '警告：默认角色 USER 不存在，用户 % 未分配角色', NEW.username;
    END IF;

    RETURN NEW;
END;
$$ language 'plpgsql';

-- 高频操作用户认证
CREATE OR REPLACE FUNCTION authenticate_user(
    input_username VARCHAR
)
RETURNS TABLE(
    user_id UUID,
    username VARCHAR,
    status user_status,
    password_hash VARCHAR,
    password_salt VARCHAR,
    login_attempts INTEGER,
    locked_until TIMESTAMPTZ
) AS $$
BEGIN
    RETURN QUERY
    SELECT
        u.id,
        u.username,
        u.status,
        u.password_hash,
        s.password_salt,
        s.login_attempts,
        s.locked_until
    FROM users u
    JOIN user_security s ON u.id = s.user_id
    WHERE u.username = input_username
      AND u.deleted_at IS NULL
      AND u.status NOT IN ('DELETED', 'BANNED');
END;
$$ LANGUAGE plpgsql;

-- 更新登录信息
CREATE OR REPLACE FUNCTION update_login_info(
    p_user_id UUID,
    p_ip_address INET,
    p_success BOOLEAN,
    p_user_agent TEXT DEFAULT NULL
)
RETURNS VOID AS $$
DECLARE
    current_attempts INTEGER := 0;
    max_attempts INTEGER := 5;  -- 最大尝试次数
    lock_duration INTERVAL := '30 minutes';  -- 锁定时长
BEGIN
    -- 更新安全表
    UPDATE user_security
    SET
        last_login_at = CASE WHEN p_success THEN CURRENT_TIMESTAMP ELSE last_login_at END,
        last_login_ip = CASE WHEN p_success THEN p_ip_address ELSE last_login_ip END,
        login_attempts = CASE WHEN p_success THEN 0 ELSE login_attempts + 1 END,
        -- 如果失败次数达到上限，设置锁定时间
        locked_until = CASE
            WHEN p_success THEN NULL
            WHEN login_attempts + 1 >= max_attempts THEN CURRENT_TIMESTAMP + lock_duration
            ELSE locked_until
        END,
        updated_at = CURRENT_TIMESTAMP
    WHERE user_id = p_user_id
    RETURNING login_attempts INTO current_attempts;

    -- 记录登录日志
    INSERT INTO user_login_logs (user_id, ip_address, success, user_agent)
    VALUES (p_user_id, p_ip_address, p_success, p_user_agent);

    -- 如果达到锁定条件，发出通知
    IF NOT p_success AND current_attempts >= max_attempts THEN
        RAISE NOTICE '用户 % 因多次登录失败被锁定30分钟', p_user_id;
    END IF;
END;
$$ LANGUAGE plpgsql;
