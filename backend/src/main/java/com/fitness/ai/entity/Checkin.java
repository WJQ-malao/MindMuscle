package com.fitness.ai.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("checkin")
public class Checkin {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long itemId;
    private Integer setsDone;
    private Integer repsDone;
    /** 使用的重量（kg），自重训练为 0 */
    private Double weightUsed;
    private LocalDate checkinDate;
    private LocalDateTime createTime;
}
