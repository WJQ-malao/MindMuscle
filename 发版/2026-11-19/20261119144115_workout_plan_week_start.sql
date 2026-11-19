-- 动作库升级：计划绑定周（一天一计划）
-- 新增 week_start 字段：计划所属周的周一日期，AI/手动创建的计划均归属具体某一周
USE fitness_db;
SET NAMES utf8mb4;

ALTER TABLE workout_plan
    ADD COLUMN week_start DATE NULL COMMENT '计划所属周的周一日期（一天一计划）' AFTER source;

-- 存量进行中的计划回填为本周一（历史归档计划无周信息，前端展示兼容空值）
UPDATE workout_plan
SET week_start = DATE_SUB(CURDATE(), INTERVAL WEEKDAY(CURDATE()) DAY)
WHERE status = 'ACTIVE' AND week_start IS NULL;
