package com.fitness.ai.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fitness.ai.common.Result;
import com.fitness.ai.entity.Checkin;
import com.fitness.ai.entity.KnowledgeFile;
import com.fitness.ai.entity.User;
import com.fitness.ai.entity.WorkoutPlan;
import com.fitness.ai.mapper.CheckinMapper;
import com.fitness.ai.mapper.KnowledgeFileMapper;
import com.fitness.ai.mapper.UserMapper;
import com.fitness.ai.mapper.WorkoutPlanMapper;
import com.fitness.ai.util.UserContext;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** 平台管理接口：仅管理员可用 */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final UserMapper userMapper;
    private final CheckinMapper checkinMapper;
    private final WorkoutPlanMapper planMapper;
    private final KnowledgeFileMapper knowledgeFileMapper;

    public AdminController(UserMapper userMapper, CheckinMapper checkinMapper,
                           WorkoutPlanMapper planMapper, KnowledgeFileMapper knowledgeFileMapper) {
        this.userMapper = userMapper;
        this.checkinMapper = checkinMapper;
        this.planMapper = planMapper;
        this.knowledgeFileMapper = knowledgeFileMapper;
    }

    /** 平台概览：核心指标 + 最近注册用户 */
    @GetMapping("/overview")
    public Result<Map<String, Object>> overview() {
        requireAdmin();
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

    private void requireAdmin() {
        if (!"ADMIN".equals(UserContext.getRole())) {
            throw new IllegalStateException("仅管理员可访问");
        }
    }
}
