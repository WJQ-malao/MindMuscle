package com.fitness.ai.controller;

import com.fitness.ai.common.Result;
import com.fitness.ai.dto.CheckinRequest;
import com.fitness.ai.dto.ItemUpsertRequest;
import com.fitness.ai.dto.PlanCreateRequest;
import com.fitness.ai.entity.Checkin;
import com.fitness.ai.entity.WorkoutItem;
import com.fitness.ai.entity.WorkoutPlan;
import com.fitness.ai.service.WorkoutService;
import com.fitness.ai.util.UserContext;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
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

    @PostMapping("/plans")
    public Result<WorkoutPlan> createPlan(@Valid @RequestBody PlanCreateRequest request) {
        return Result.ok(workoutService.createPlanFromBasket(
                UserContext.getUserId(), request.title(), request.goal(),
                request.daysPerWeek(), request.items()));
    }

    @GetMapping("/plans/{planId}/items")
    public Result<List<WorkoutItem>> items(@PathVariable Long planId) {
        return Result.ok(workoutService.listItemsChecked(UserContext.getUserId(), planId));
    }

    @PostMapping("/plans/{planId}/items")
    public Result<WorkoutItem> addItem(@PathVariable Long planId,
                                       @Valid @RequestBody ItemUpsertRequest request) {
        return Result.ok(workoutService.addItem(UserContext.getUserId(), planId,
                request.exerciseName(), request.muscleGroup(),
                request.dayOfWeek(), request.sets(), request.reps()));
    }

    @PutMapping("/items/{itemId}")
    public Result<WorkoutItem> updateItem(@PathVariable Long itemId,
                                          @Valid @RequestBody ItemUpsertRequest request) {
        return Result.ok(workoutService.updateItem(UserContext.getUserId(), itemId,
                request.exerciseName(), request.muscleGroup(),
                request.dayOfWeek(), request.sets(), request.reps()));
    }

    @DeleteMapping("/items/{itemId}")
    public Result<String> deleteItem(@PathVariable Long itemId) {
        workoutService.deleteItem(UserContext.getUserId(), itemId);
        return Result.ok("已删除");
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
