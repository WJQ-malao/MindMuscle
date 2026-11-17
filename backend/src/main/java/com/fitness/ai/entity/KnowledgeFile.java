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
    private LocalDateTime createTime;
}
