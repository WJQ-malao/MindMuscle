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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

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
                request.dayOfWeek(), request.sets(), request.reps(), request.weightKg()));
    }

    @PutMapping("/items/{itemId}")
    public Result<WorkoutItem> updateItem(@PathVariable Long itemId,
                                          @Valid @RequestBody ItemUpsertRequest request) {
        return Result.ok(workoutService.updateItem(UserContext.getUserId(), itemId,
                request.exerciseName(), request.muscleGroup(),
                request.dayOfWeek(), request.sets(), request.reps(), request.weightKg()));
    }

    @DeleteMapping("/items/{itemId}")
    public Result<String> deleteItem(@PathVariable Long itemId) {
        workoutService.deleteItem(UserContext.getUserId(), itemId);
        return Result.ok("已删除");
    }

    @PostMapping("/checkins")
    public Result<Checkin> checkin(@Valid @RequestBody CheckinRequest request) {
        return Result.ok(workoutService.checkin(
                UserContext.getUserId(), request.itemId(), request.setNo(),
                request.repsDone(), request.weightUsed()));
    }

    @GetMapping("/checkins/recent")
    public Result<List<Checkin>> recent() {
        return Result.ok(workoutService.recentCheckins(UserContext.getUserId(), 10));
    }

    /** 已打卡的"动作ID→已打组号列表"，默认今天，可传 date=YYYY-MM-DD 查看历史日期 */
    @GetMapping("/checkins/today")
    public Result<Map<Long, List<Integer>>> todayChecked(
            @RequestParam(required = false) String date) {
        return Result.ok(workoutService.checkedSets(UserContext.getUserId(), parseDate(date)));
    }

    /** 撤销某动作某一组的打卡，默认撤销今天的，可传 date 撤销历史记录 */
    @DeleteMapping("/checkins/today/{itemId}/{setNo}")
    public Result<String> undoSet(@PathVariable Long itemId, @PathVariable Integer setNo,
                                  @RequestParam(required = false) String date) {
        workoutService.undoSetCheckin(UserContext.getUserId(), itemId, setNo, parseDate(date));
        return Result.ok("已撤销该组打卡");
    }

    /** date 参数解析：缺省为今天 */
    private LocalDate parseDate(String date) {
        return date == null || date.isBlank() ? LocalDate.now() : LocalDate.parse(date);
    }
}
