-- 用户状态枚举
CREATE TYPE user_status AS ENUM (
  'PENDING',     -- 待激活
  'ACTIVE',      -- 正常使用
  'INACTIVE',    -- 长期未登录
  'LOCKED',      -- 登录被锁定
  'SUSPENDED',   -- 风控暂停
  'BANNED',      -- 封禁
  'DELETED'      -- 已删除
);

-- 用户类型枚举
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