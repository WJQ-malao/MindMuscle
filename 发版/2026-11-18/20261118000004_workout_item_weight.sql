-- ============================================================================
-- 训练动作建议重量：workout_item 增加 weight_kg 字段（力量训练动作的计划重量）
-- NULL = 自重/未指定（有氧、自重动作不需要）
-- ============================================================================
USE fitness_db;

ALTER TABLE workout_item
    ADD COLUMN weight_kg DOUBLE NULL COMMENT '建议重量(kg), 空=自重/未指定' AFTER reps;
