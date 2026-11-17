package com.fitness.ai.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fitness.ai.entity.SysRole;
import com.fitness.ai.entity.UserRole;
import com.fitness.ai.mapper.SysRoleMapper;
import com.fitness.ai.mapper.UserRoleMapper;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

/**
 * RBAC 角色服务：用户-角色多对多解析。
 * 角色与权限点维护在 sys_role / sys_user_role / sys_permission / sys_role_permission 四张表。
 */
@Service
public class RoleService {

    private final SysRoleMapper roleMapper;
    private final UserRoleMapper userRoleMapper;

    public RoleService(SysRoleMapper roleMapper, UserRoleMapper userRoleMapper) {
        this.roleMapper = roleMapper;
        this.userRoleMapper = userRoleMapper;
    }

    /** 解析用户拥有的全部角色编码 */
    public List<String> getRoleCodes(Long userId) {
        List<UserRole> mappings = userRoleMapper.selectList(
                new LambdaQueryWrapper<UserRole>().eq(UserRole::getUserId, userId));
        if (mappings.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> roleIds = mappings.stream().map(UserRole::getRoleId).toList();
        return roleMapper.selectBatchIds(roleIds).stream()
                .map(SysRole::getRoleCode)
                .toList();
    }

    /** 为新注册用户分配默认 USER 角色 */
    public void assignDefaultRole(Long userId) {
        SysRole userRole = roleMapper.selectOne(
                new LambdaQueryWrapper<SysRole>().eq(SysRole::getRoleCode, "USER"));
        if (userRole == null) {
            throw new IllegalStateException("系统缺少默认 USER 角色，请联系管理员");
        }
        UserRole mapping = new UserRole();
        mapping.setUserId(userId);
        mapping.setRoleId(userRole.getId());
        userRoleMapper.insert(mapping);
    }
}
