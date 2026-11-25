-- ============================================================================
-- 用户头像：sys_user 增加 avatar 字段（头像地址 /api/avatars/**，空=未设置）
-- ============================================================================
USE fitness_db;
SET NAMES utf8mb4;

ALTER TABLE sys_user
    ADD COLUMN avatar VARCHAR(500) NULL COMMENT '头像地址(/api/avatars/**)，空=未设置' AFTER height_cm;
