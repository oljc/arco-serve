-- ============================================
-- 初始化数据
-- ============================================

INSERT INTO roles (name, display_name, permissions) VALUES
('USER', '普通用户', '["user:read", "user:update_self", "profile:read", "profile:update_self"]'),
('OPERATOR', '运营人员', '["user:read", "user:list", "user:moderate", "content:review", "reports:view"]'),
('ADMIN', '管理员', '["user:*", "role:*", "system:*", "audit:*"]'),
('SUPER_ADMIN', '超级管理员', '["*"]')
ON CONFLICT (name) DO NOTHING;
