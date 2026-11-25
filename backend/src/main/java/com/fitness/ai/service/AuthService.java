package com.fitness.ai.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fitness.ai.entity.User;
import com.fitness.ai.mapper.UserMapper;
import com.fitness.ai.service.RoleService;
import com.fitness.ai.util.JwtUtil;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AuthService {

    private final UserMapper userMapper;
    private final JwtUtil jwtUtil;
    private final RoleService roleService;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    public AuthService(UserMapper userMapper, JwtUtil jwtUtil, RoleService roleService) {
        this.userMapper = userMapper;
        this.jwtUtil = jwtUtil;
        this.roleService = roleService;
    }

    public Map<String, Object> register(String username, String password, String nickname, Double heightCm) {
        Long count = userMapper.selectCount(new LambdaQueryWrapper<User>().eq(User::getUsername, username));
        if (count > 0) {
            throw new IllegalArgumentException("用户名已存在");
        }
        User user = new User();
        user.setUsername(username);
        user.setPassword(encoder.encode(password));
        user.setNickname(nickname == null || nickname.isBlank() ? username : nickname);
        user.setRole("USER");
        user.setHeightCm(heightCm);
        userMapper.insert(user);
        // RBAC: 为新用户分配默认 USER 角色（sys_user_role 关联）
        roleService.assignDefaultRole(user.getId());
        return buildLoginResult(user, roleService.getRoleCodes(user.getId()));
    }

    public Map<String, Object> login(String username, String password) {
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, username));
        if (user == null || !encoder.matches(password, user.getPassword())) {
            throw new IllegalArgumentException("用户名或密码错误");
        }
        return buildLoginResult(user, roleService.getRoleCodes(user.getId()));
    }

    private Map<String, Object> buildLoginResult(User user, List<String> roles) {
        Map<String, Object> result = new HashMap<>();
        result.put("token", jwtUtil.generateToken(user.getId(), roles));
        result.put("userId", user.getId());
        result.put("username", user.getUsername());
        result.put("nickname", user.getNickname());
        result.put("avatar", user.getAvatar());
        result.put("roles", roles);
        result.put("role", roles.contains("ADMIN") ? "ADMIN" : (roles.isEmpty() ? "USER" : roles.get(0)));
        return result;
    }
}
