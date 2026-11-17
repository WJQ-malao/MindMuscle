package com.fitness.ai.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fitness.ai.entity.BodyMetric;
import com.fitness.ai.mapper.BodyMetricMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class BodyMetricService {

    private final BodyMetricMapper bodyMetricMapper;

    public BodyMetricService(BodyMetricMapper bodyMetricMapper) {
        this.bodyMetricMapper = bodyMetricMapper;
    }

    public BodyMetric add(Long userId, Double weightKg, Double bodyFat, LocalDate recordDate) {
        BodyMetric metric = new BodyMetric();
        metric.setUserId(userId);
        metric.setWeightKg(weightKg);
        metric.setBodyFat(bodyFat);
        metric.setRecordDate(recordDate == null ? LocalDate.now() : recordDate);
        bodyMetricMapper.insert(metric);
        return metric;
    }

    public List<BodyMetric> list(Long userId, Integer days) {
        LambdaQueryWrapper<BodyMetric> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BodyMetric::getUserId, userId);
        if (days != null && days > 0) {
            wrapper.ge(BodyMetric::getRecordDate, LocalDate.now().minusDays(days));
        }
        wrapper.orderByAsc(BodyMetric::getRecordDate);
        return bodyMetricMapper.selectList(wrapper);
    }

    /** 最近一次体测记录，用于 BMI 计算和看板展示 */
    public BodyMetric latest(Long userId) {
        return bodyMetricMapper.selectOne(new LambdaQueryWrapper<BodyMetric>()
                .eq(BodyMetric::getUserId, userId)
                .orderByDesc(BodyMetric::getRecordDate)
                .orderByDesc(BodyMetric::getId)
                .last("LIMIT 1"));
    }

    public void delete(Long userId, Long id) {
        bodyMetricMapper.delete(new LambdaQueryWrapper<BodyMetric>()
                .eq(BodyMetric::getId, id)
                .eq(BodyMetric::getUserId, userId));
    }
}
