package com.fitness.ai.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("knowledge_file")
public class KnowledgeFile {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String fileName;
    private Integer chunkCount;
    /** 原始文件存储路径（上传时落盘，供管理页查看/下载） */
    private String filePath;
    private LocalDateTime createTime;
}
