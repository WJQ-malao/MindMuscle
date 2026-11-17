package com.fitness.ai.controller;

import com.fitness.ai.common.Result;
import com.fitness.ai.dto.MetricRequest;
import com.fitness.ai.entity.BodyMetric;
import com.fitness.ai.service.BodyMetricService;
import com.fitness.ai.util.UserContext;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/metrics")
public class BodyMetricController {

    private final BodyMetricService bodyMetricService;

    public BodyMetricController(BodyMetricService bodyMetricService) {
        this.bodyMetricService = bodyMetricService;
    }

    @PostMapping
    public Result<BodyMetric> add(@Valid @RequestBody MetricRequest request) {
        return Result.ok(bodyMetricService.add(
                UserContext.getUserId(), request.weightKg(), request.bodyFat(), request.recordDate()));
    }

    @GetMapping
    public Result<List<BodyMetric>> list(@RequestParam(required = false) Integer days) {
        return Result.ok(bodyMetricService.list(UserContext.getUserId(), days));
    }

    @DeleteMapping("/{id}")
    public Result<String> delete(@PathVariable Long id) {
        bodyMetricService.delete(UserContext.getUserId(), id);
        return Result.ok("已删除");
    }
}
