package com.fitness.ai.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_config")
public class SysConfig {

    /** 字符串主键必须指定 INPUT，否则 MP 默认雪花算法会覆盖手动设置的键 */
    @TableId(type = IdType.INPUT)
    private String configKey;
    private String configValue;
    private LocalDateTime updateTime;
}
