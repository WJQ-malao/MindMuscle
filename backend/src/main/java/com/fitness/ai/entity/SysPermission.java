package com.fitness.ai.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_permission")
public class SysPermission {

    @TableId(type = IdType.AUTO)
    private Long id;
    /** 权限编码，如 knowledge:manage */
    private String permCode;
    private String permName;
    /** MENU 菜单 / API 接口 */
    private String permType;
    /** 菜单路径或 API 路径模式 */
    private String resource;
    private LocalDateTime createTime;
}
