-- ============================================================================
-- 动作插图地址回填：image_url 指向前端静态资源 /exercises/{id}.svg
-- enrich 脚本(03)只建 image_url 列不填值，由本脚本按主键回填
-- 对应资源：frontend/public/exercises/*.svg（46 张，按动作 id 命名）
-- 说明：image_url 按 id 生成，旧库执行前请确认 exercises/{id}.svg 资源齐全
-- ============================================================================
USE fitness_db;

UPDATE exercise_lib SET image_url = CONCAT('/exercises/', id, '.svg') WHERE image_url IS NULL OR image_url = '';
