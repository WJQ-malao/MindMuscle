-- ============================================================================
-- 知识库文件原件存储：knowledge_file 增加原始文件路径字段
-- 背景：系统原本只存向量与切片，不保留原始文档，导致管理页无法查看内容
-- ============================================================================
USE fitness_db;
SET NAMES utf8mb4;

ALTER TABLE knowledge_file
    ADD COLUMN file_path VARCHAR(500) NULL COMMENT '原始文件存储路径(相对后端运行目录)' AFTER chunk_count;
