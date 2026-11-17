package com.fitness.ai.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Mapper
public interface StatsMapper {

    /** 体重趋势 */
    @Select("SELECT record_date AS recordDate, weight_kg AS weightKg, body_fat AS bodyFat " +
            "FROM body_metric WHERE user_id = #{userId} ORDER BY record_date ASC LIMIT #{limit}")
    List<Map<String, Object>> weightTrend(@Param("userId") Long userId, @Param("limit") int limit);

    /** 打卡热力图：近 6 个月每天的打卡次数 */
    @Select("SELECT checkin_date AS date, COUNT(*) AS count FROM checkin " +
            "WHERE user_id = #{userId} AND checkin_date >= DATE_SUB(CURDATE(), INTERVAL 6 MONTH) " +
            "GROUP BY checkin_date")
    List<Map<String, Object>> checkinHeatmap(@Param("userId") Long userId);

    /** 肌群容量分布：总容量 = 组数 x 次数 x 重量 */
    @Select("SELECT i.muscle_group AS muscle, " +
            "SUM(c.sets_done * c.reps_done * COALESCE(NULLIF(c.weight_used, 0), 1)) AS volume " +
            "FROM checkin c JOIN workout_item i ON c.item_id = i.id " +
            "WHERE c.user_id = #{userId} GROUP BY i.muscle_group")
    List<Map<String, Object>> muscleCapacity(@Param("userId") Long userId);

    /** 看板顶部统计：累计打卡天数、连续打卡（简化版：最近 7 天打卡天数） */
    @Select("SELECT COUNT(DISTINCT checkin_date) AS totalDays FROM checkin WHERE user_id = #{userId}")
    Long totalCheckinDays(@Param("userId") Long userId);

    @Select("SELECT COUNT(DISTINCT checkin_date) AS recentDays FROM checkin " +
            "WHERE user_id = #{userId} AND checkin_date >= #{since}")
    Long recentCheckinDays(@Param("userId") Long userId, @Param("since") LocalDate since);
}
