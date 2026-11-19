-- 计划存档（模板）：把某周训练计划快照保存为可复用模板
USE fitness_db;
SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS plan_archive (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL COMMENT '归属用户',
    name VARCHAR(100) NOT NULL COMMENT '存档名称',
    goal VARCHAR(50) NULL COMMENT '训练目标（快照自计划）',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_user (user_id)
) COMMENT '训练计划存档（模板），结构同 workout_plan + workout_item 快照';

CREATE TABLE IF NOT EXISTS plan_archive_item (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    archive_id BIGINT NOT NULL COMMENT '所属存档',
    day_of_week TINYINT NOT NULL COMMENT '1-7 对应周一到周日',
    exercise_name VARCHAR(100) NOT NULL,
    muscle_group VARCHAR(50) NOT NULL,
    sets INT NOT NULL,
    reps INT NOT NULL,
    weight_kg DOUBLE NULL COMMENT '建议重量，NULL=自重',
    sort INT DEFAULT 0,
    KEY idx_archive (archive_id)
) COMMENT '计划存档明细';
