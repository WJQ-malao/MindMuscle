-- ============================================================================
-- 打卡粒度升级：按组打卡
-- 每个打卡记录代表"某动作的某一组"（user_id + item_id + 日期 + set_no 唯一）
-- 语义变更，开发期历史打卡数据清空
-- ============================================================================
USE fitness_db;
SET NAMES utf8mb4;

ALTER TABLE checkin
    ADD COLUMN set_no INT NOT NULL DEFAULT 1 COMMENT '第几组' AFTER item_id,
    ADD UNIQUE KEY uk_user_item_date_set (user_id, item_id, checkin_date, set_no);

TRUNCATE TABLE checkin;
