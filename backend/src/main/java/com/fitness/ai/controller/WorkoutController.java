package com.fitness.ai.controller;

import com.fitness.ai.common.Result;
import com.fitness.ai.dto.CheckinRequest;
import com.fitness.ai.entity.Checkin;
import com.fitness.ai.entity.WorkoutItem;
import com.fitness.ai.entity.WorkoutPlan;
import com.fitness.ai.service.WorkoutService;
import com.fitness.ai.util.UserContext;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class WorkoutController {

    private final WorkoutService workoutService;

    public WorkoutController(WorkoutService workoutService) {
        this.workoutService = workoutService;
    }

    @GetMapping("/plans")
    public Result<List<WorkoutPlan>> plans() {
        return Result.ok(workoutService.listPlans(UserContext.getUserId()));
    }

    @GetMapping("/plans/{planId}/items")
    public Result<List<WorkoutItem>> items(@PathVariable Long planId) {
        return Result.ok(workoutService.listItems(planId));
    }

    @PostMapping("/checkins")
    public Result<Checkin> checkin(@Valid @RequestBody CheckinRequest request) {
        return Result.ok(workoutService.checkin(
                UserContext.getUserId(), request.itemId(), request.setsDone(),
                request.repsDone(), request.weightUsed()));
    }

    @GetMapping("/checkins/recent")
    public Result<List<Checkin>> recent() {
        return Result.ok(workoutService.recentCheckins(UserContext.getUserId(), 10));
    }
}
