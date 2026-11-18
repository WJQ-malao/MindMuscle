-- ============================================================================
-- 动作插图地址回填：image_url 指向前端静态资源 /exercises/{序号}.svg
-- enrich 脚本(03)只建 image_url 列不填值，由本脚本回填
-- 对应资源：frontend/public/exercises/*.svg（46 张，按动作库插入顺序编号）
--
-- 注意：不能直接用 CONCAT(id)，DELETE 清表不重置自增 id，
--       重建过的库 id 会从 46 之后继续累加，与 SVG 文件名对不上。
--       这里按 id 顺序（= 插入顺序 = 脚本顺序）重新编号。
-- ============================================================================
USE fitness_db;

SET @img_seq := 0;
UPDATE exercise_lib SET image_url = CONCAT('/exercises/', (@img_seq := @img_seq + 1), '.svg') ORDER BY id;
