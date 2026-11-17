package com.fitness.ai.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fitness.ai.entity.User;
import com.fitness.ai.mapper.UserMapper;
import com.fitness.ai.util.JwtUtil;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class AuthService {

    private final UserMapper userMapper;
    private final JwtUtil jwtUtil;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    public AuthService(UserMapper userMapper, JwtUtil jwtUtil) {
        this.userMapper = userMapper;
        this.jwtUtil = jwtUtil;
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
        return buildLoginResult(user);
    }

    public Map<String, Object> login(String username, String password) {
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, username));
        if (user == null || !encoder.matches(password, user.getPassword())) {
            throw new IllegalArgumentException("用户名或密码错误");
        }
        return buildLoginResult(user);
    }

    private Map<String, Object> buildLoginResult(User user) {
        Map<String, Object> result = new HashMap<>();
        result.put("token", jwtUtil.generateToken(user.getId(), user.getRole()));
        result.put("userId", user.getId());
        result.put("nickname", user.getNickname());
        result.put("role", user.getRole());
        return result;
    }
}
