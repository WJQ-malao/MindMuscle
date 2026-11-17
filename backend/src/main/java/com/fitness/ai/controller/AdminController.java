package com.fitness.ai.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fitness.ai.common.Result;
import com.fitness.ai.entity.SysRole;
import com.fitness.ai.entity.User;
import com.fitness.ai.entity.UserRole;
import com.fitness.ai.mapper.CheckinMapper;
import com.fitness.ai.mapper.KnowledgeFileMapper;
import com.fitness.ai.mapper.SysRoleMapper;
import com.fitness.ai.mapper.UserMapper;
import com.fitness.ai.mapper.UserRoleMapper;
import com.fitness.ai.mapper.WorkoutPlanMapper;
import com.fitness.ai.security.RequireRole;
import com.fitness.ai.util.UserContext;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** 平台管理接口：仅管理员可用（RBAC: admin:overview 权限点） */
@RestController
@RequestMapping("/api/admin")
@RequireRole("ADMIN")
public class AdminController {

    private final UserMapper userMapper;
    private final CheckinMapper checkinMapper;
    private final WorkoutPlanMapper planMapper;
    private final KnowledgeFileMapper knowledgeFileMapper;
    private final UserRoleMapper userRoleMapper;
    private final SysRoleMapper roleMapper;

    public AdminController(UserMapper userMapper, CheckinMapper checkinMapper,
                           WorkoutPlanMapper planMapper, KnowledgeFileMapper knowledgeFileMapper,
                           UserRoleMapper userRoleMapper, SysRoleMapper roleMapper) {
        this.userMapper = userMapper;
        this.checkinMapper = checkinMapper;
        this.planMapper = planMapper;
        this.knowledgeFileMapper = knowledgeFileMapper;
        this.userRoleMapper = userRoleMapper;
        this.roleMapper = roleMapper;
    }

    /** 平台概览：核心指标 + 最近注册用户 */
    @GetMapping("/overview")
    public Result<Map<String, Object>> overview() {
        Map<String, Object> result = new HashMap<>();
        result.put("totalUsers", userMapper.selectCount(null));
        result.put("totalCheckins", checkinMapper.selectCount(null));
        result.put("totalPlans", planMapper.selectCount(null));
        result.put("knowledgeFiles", knowledgeFileMapper.selectCount(null));

        List<User> recentUsers = userMapper.selectList(new LambdaQueryWrapper<User>()
                .select(User::getId, User::getUsername, User::getNickname, User::getRole, User::getCreateTime)
                .orderByDesc(User::getId)
                .last("LIMIT 10"));
        result.put("recentUsers", recentUsers);
        return Result.ok(result);
    }

    /** 用户管理：全量用户列表（含关联表解析出的角色集合） */
    @GetMapping("/users")
    public Result<List<Map<String, Object>>> users() {
        List<User> users = userMapper.selectList(new LambdaQueryWrapper<User>()
                .select(User::getId, User::getUsername, User::getNickname, User::getCreateTime)
                .orderByAsc(User::getId));
        List<SysRole> allRoles = roleMapper.selectList(null);

        List<Map<String, Object>> result = users.stream().map(u -> {
            Map<String, Object> row = new HashMap<>();
            row.put("id", u.getId());
            row.put("username", u.getUsername());
            row.put("nickname", u.getNickname());
            row.put("createTime", u.getCreateTime());
            List<Long> roleIds = userRoleMapper.selectList(new LambdaQueryWrapper<UserRole>()
                            .eq(UserRole::getUserId, u.getId())).stream()
                    .map(UserRole::getRoleId).toList();
            List<String> roleCodes = allRoles.stream()
                    .filter(r -> roleIds.contains(r.getId()))
                    .map(SysRole::getRoleCode).toList();
            row.put("roles", roleCodes);
            return row;
        }).toList();
        return Result.ok(result);
    }

    /** 调整用户角色：整体替换该用户的角色集合（事务） */
    @PutMapping("/users/{userId}/roles")
    @Transactional
    public Result<String> updateRoles(@PathVariable Long userId, @RequestBody Map<String, List<String>> body) {
        List<String> roleCodes = body.getOrDefault("roles", List.of());
        if (Objects.equals(userId, UserContext.getUserId())) {
            throw new IllegalArgumentException("不能修改自己的角色");
        }
        User target = userMapper.selectById(userId);
        if (target == null) {
            throw new IllegalArgumentException("用户不存在");
        }
        List<SysRole> allRoles = roleMapper.selectList(null);
        List<SysRole> newRoles = allRoles.stream()
                .filter(r -> roleCodes.contains(r.getRoleCode()))
                .toList();
        if (newRoles.isEmpty()) {
            throw new IllegalArgumentException("至少保留一个角色");
        }

        // 安全护栏：若剥夺某用户的 ADMIN，需保证系统至少还剩一个管理员
        boolean hadAdmin = userRoleMapper.selectCount(new LambdaQueryWrapper<UserRole>()
                .eq(UserRole::getUserId, userId)
                .in(UserRole::getRoleId, adminRoleIds(allRoles))) > 0;
        boolean keepsAdmin = newRoles.stream().anyMatch(r -> "ADMIN".equals(r.getRoleCode()));
        if (hadAdmin && !keepsAdmin) {
            long otherAdmins = userRoleMapper.selectCount(new LambdaQueryWrapper<UserRole>()
                    .ne(UserRole::getUserId, userId)
                    .in(UserRole::getRoleId, adminRoleIds(allRoles)));
            if (otherAdmins == 0) {
                throw new IllegalArgumentException("系统至少保留一名管理员");
            }
        }

        userRoleMapper.delete(new LambdaQueryWrapper<UserRole>().eq(UserRole::getUserId, userId));
        for (SysRole role : newRoles) {
            UserRole mapping = new UserRole();
            mapping.setUserId(userId);
            mapping.setRoleId(role.getId());
            userRoleMapper.insert(mapping);
        }
        // 冗余主角色字段同步（兼容旧逻辑）
        target.setRole(newRoles.stream().anyMatch(r -> "ADMIN".equals(r.getRoleCode())) ? "ADMIN" : "USER");
        userMapper.updateById(target);
        return Result.ok("角色已更新");
    }

    private List<Long> adminRoleIds(List<SysRole> allRoles) {
        return allRoles.stream()
                .filter(r -> "ADMIN".equals(r.getRoleCode()))
                .map(SysRole::getId)
                .toList();
    }
}
