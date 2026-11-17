package com.fitness.ai.service;

import com.fitness.ai.entity.BodyMetric;
import com.fitness.ai.mapper.StatsMapper;
import com.fitness.ai.util.UserContext;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

@Service
public class DashboardService {

    private final StatsMapper statsMapper;
    private final BodyMetricService bodyMetricService;

    public DashboardService(StatsMapper statsMapper, BodyMetricService bodyMetricService) {
        this.statsMapper = statsMapper;
        this.bodyMetricService = bodyMetricService;
    }

    /** 看板一次拉全：指标卡 + 体重趋势 + 打卡热力 + 肌群容量 */
    public Map<String, Object> overview(Long userId) {
        Map<String, Object> result = new HashMap<>();
        result.put("weightTrend", statsMapper.weightTrend(userId, 90));
        result.put("checkinHeatmap", statsMapper.checkinHeatmap(userId));
        result.put("muscleCapacity", statsMapper.muscleCapacity(userId));
        result.put("totalCheckinDays", statsMapper.totalCheckinDays(userId));
        result.put("recent7Days", statsMapper.recentCheckinDays(userId, LocalDate.now().minusDays(7)));

        BodyMetric latest = bodyMetricService.latest(userId);
        Map<String, Object> card = new HashMap<>();
        if (latest != null) {
            card.put("weightKg", latest.getWeightKg());
            card.put("bodyFat", latest.getBodyFat());
            card.put("recordDate", latest.getRecordDate());
        }
        result.put("latestMetric", card);
        return result;
    }

    public Map<String, Object> overviewForCurrentUser() {
        return overview(UserContext.getUserId());
    }
}
