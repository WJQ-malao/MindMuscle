-- 基于 SpringBoot 融合 RAG 与 AI Agent 的个性化健身智能可视化系统
-- 数据库初始化脚本（MySQL 8.x）
CREATE DATABASE IF NOT EXISTS fitness_db DEFAULT CHARSET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE fitness_db;
SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS sys_user (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    username    VARCHAR(100) NOT NULL UNIQUE COMMENT '用户名',
    password    VARCHAR(255) NOT NULL COMMENT 'BCrypt 加密后的密码',
    nickname    VARCHAR(100) NULL COMMENT '昵称',
    role        VARCHAR(20)  NOT NULL DEFAULT 'USER' COMMENT 'USER/ADMIN',
    height_cm   DOUBLE       NULL COMMENT '身高（cm）',
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS body_metric (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id     BIGINT   NOT NULL COMMENT '用户ID',
    weight_kg   DOUBLE   NOT NULL COMMENT '体重（kg）',
    body_fat    DOUBLE   NULL COMMENT '体脂率（%）',
    record_date DATE     NOT NULL COMMENT '记录日期',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user_date (user_id, record_date)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS workout_plan (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id     BIGINT       NOT NULL COMMENT '用户ID',
    title       VARCHAR(200) NOT NULL COMMENT '计划名称',
    goal        VARCHAR(50)  NULL COMMENT '目标：增肌/减脂等',
    status      VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE/ARCHIVED',
    source      VARCHAR(20)  NOT NULL DEFAULT 'MANUAL' COMMENT 'AI/MANUAL 来源',
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user_status (user_id, status)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS workout_item (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    plan_id       BIGINT       NOT NULL COMMENT '所属计划ID',
    day_of_week   INT          NOT NULL COMMENT '1-7 对应周一到周日',
    exercise_name VARCHAR(100) NOT NULL COMMENT '动作名称',
    muscle_group  VARCHAR(20)  NOT NULL COMMENT '胸/背/腿/肩/手臂/核心/有氧',
    sets          INT          NOT NULL COMMENT '组数',
    reps          INT          NOT NULL COMMENT '次数（有氧为分钟）',
    sort          INT          NOT NULL DEFAULT 0 COMMENT '同日排序',
    INDEX idx_plan (plan_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE IF NOT EXISTS checkin (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id     BIGINT   NOT NULL COMMENT '用户ID',
    item_id     BIGINT   NOT NULL COMMENT '训练项ID',
    sets_done   INT      NOT NULL COMMENT '完成组数',
    reps_done   INT      NOT NULL COMMENT '每组次数',
    weight_used DOUBLE   NOT NULL DEFAULT 0 COMMENT '使用重量（kg）',
    checkin_date DATE    NOT NULL COMMENT '打卡日期',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user_date (user_id, checkin_date)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- 知识库文件登记（向量数据存 Milvus，这里只存元信息）
CREATE TABLE IF NOT EXISTS knowledge_file (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    file_name   VARCHAR(255) NOT NULL COMMENT '文件名',
    chunk_count INT          NOT NULL DEFAULT 0 COMMENT '切片数量',
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
