-- ============================================
-- 数据库构建
-- ============================================

\i core/enums.sql
\i core/base.sql
\i schema/user.sql
\i schema/auth.sql
\i schema/logs.sql
\i core/data.sql

DO $$
BEGIN
    RAISE NOTICE '数据库构建完成！';
END $$;
