-- ============================================
-- 日志模块
-- ============================================

-- 用户登录日志表
CREATE TABLE user_login_logs (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    ip_address INET NOT NULL,
    user_agent TEXT,
    success BOOLEAN NOT NULL,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);


CREATE INDEX idx_login_logs_user_time ON user_login_logs(user_id, created_at DESC);
CREATE INDEX idx_login_logs_ip ON user_login_logs(ip_address);
CREATE INDEX idx_login_logs_success_time ON user_login_logs(success, created_at DESC);
CREATE INDEX idx_login_logs_recent ON user_login_logs(created_at DESC) WHERE created_at > CURRENT_TIMESTAMP - INTERVAL '7 days';

-- 高频写入表优化
ALTER TABLE user_login_logs SET (
    autovacuum_analyze_scale_factor = 0.01,
    autovacuum_vacuum_scale_factor = 0.02,
    autovacuum_vacuum_threshold = 5000,
    toast_tuple_target = 128
);