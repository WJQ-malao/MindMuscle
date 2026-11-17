package com.fitness.ai.tools;

import com.fitness.ai.entity.BodyMetric;
import com.fitness.ai.entity.Checkin;
import com.fitness.ai.entity.User;
import com.fitness.ai.entity.WorkoutPlan;
import com.fitness.ai.mapper.UserMapper;
import com.fitness.ai.service.BodyMetricService;
import com.fitness.ai.service.WorkoutService;
import com.fitness.ai.util.UserContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * AI Agent 工具组：大模型通过 ReAct 循环自主决策调用哪些工具。
 * 所有工具的用户身份从 UserContext 获取（由 JWT 解析写入），
 * 模型既看不到也伪造不了 userId，从机制上防止越权操作。
 * 计划编排、BMI 等计算全部用 Java 确定性逻辑完成，模型只负责对话与决策。
 */
@Component
public class FitnessTools {

    private final BodyMetricService bodyMetricService;
    private final WorkoutService workoutService;
    private final UserMapper userMapper;

    public FitnessTools(BodyMetricService bodyMetricService,
                        WorkoutService workoutService,
                        UserMapper userMapper) {
        this.bodyMetricService = bodyMetricService;
        this.workoutService = workoutService;
        this.userMapper = userMapper;
    }

    @Tool(description = "查询当前用户的身体档案，包括身高、最近一次体重和体脂率。" +
            "当用户询问自己的身体状况、BMI、是否需要先了解用户数据时调用。")
    public String getMyBodyProfile() {
        Long userId = UserContext.getUserId();
        User user = userMapper.selectById(userId);
        BodyMetric latest = bodyMetricService.latest(userId);
        if (latest == null) {
            return "身高 " + user.getHeightCm() + "cm，尚未记录体重和体脂数据。";
        }
        return String.format("身高 %.1fcm，最近体重 %.1fkg（%s记录），体脂率 %s。",
                user.getHeightCm(),
                latest.getWeightKg(),
                latest.getRecordDate(),
                latest.getBodyFat() == null ? "未记录" : String.format("%.1f%%", latest.getBodyFat()));
    }

    @Tool(description = "根据身高体重计算 BMI 并给出体重分级评价。参数为身高（厘米）和体重（公斤）。")
    public String calculateBmi(@ToolParam(description = "身高，单位厘米") double heightCm,
                               @ToolParam(description = "体重，单位公斤") double weightKg) {
        if (heightCm <= 0 || weightKg <= 0) {
            return "ERROR: 身高和体重必须为正数";
        }
        double bmi = weightKg / Math.pow(heightCm / 100, 2);
        String category;
        if (bmi < 18.5) {
            category = "偏瘦";
        } else if (bmi < 24) {
            category = "正常";
        } else if (bmi < 28) {
            category = "超重";
        } else {
            category = "肥胖";
        }
        return String.format("BMI = %.1f，属于「%s」范围（中国成人标准：18.5-24 正常）。", bmi, category);
    }

    @Tool(description = "为用户生成一周训练计划并写入系统。参数：goal 为训练目标（如 增肌 / 减脂），" +
            "daysPerWeek 为每周训练天数（1-7）。生成后应告知用户计划已保存到训练计划列表。")
    public String createWorkoutPlan(@ToolParam(description = "训练目标，如：增肌、减脂") String goal,
                                    @ToolParam(description = "每周训练天数，1-7 的整数") int daysPerWeek) {
        WorkoutPlan plan = workoutService.generatePlan(UserContext.getUserId(), goal, daysPerWeek);
        return "训练计划已生成并保存：" + plan.getTitle() +
                "（计划ID: " + plan.getId() + "），用户可在训练计划页查看每天的具体动作安排。";
    }

    @Tool(description = "查询用户当前进行中的训练计划及每天的动作安排。" +
            "当用户问「今天练什么」「我的计划是什么」或需要基于现有计划给建议时调用。")
    public String getMyActivePlan() {
        return workoutService.summarizeActivePlan(UserContext.getUserId());
    }

    @Tool(description = "为用户记录一次训练打卡。itemId 为训练项ID，" +
            "setsDone 为实际完成组数（1-30），repsDone 为每组次数（1-100），" +
            "weightUsed 为使用重量（公斤，自重训练可传 0）。")
    public String recordCheckin(@ToolParam(description = "训练项ID") Long itemId,
                                @ToolParam(description = "实际完成组数，1-30") int setsDone,
                                @ToolParam(description = "每组次数，1-100") int repsDone,
                                @ToolParam(description = "使用重量（公斤），自重训练传 0", required = false) Double weightUsed) {
        try {
            Checkin checkin = workoutService.checkin(
                    UserContext.getUserId(), itemId, setsDone, repsDone, weightUsed);
            return String.format("打卡成功：%s 完成 %d 组 x %d 次，重量 %.1fkg。",
                    checkin.getCheckinDate(), setsDone, repsDone,
                    checkin.getWeightUsed());
        } catch (IllegalArgumentException | IllegalStateException e) {
            return "ERROR: " + e.getMessage() + "，请核对参数后重试。";
        }
    }

    @Tool(description = "查询用户最近的训练打卡记录（最多10条）。" +
            "当用户问训练历史、最近练得怎么样时调用。")
    public String getRecentCheckins() {
        List<Checkin> list = workoutService.recentCheckins(UserContext.getUserId(), 10);
        if (list.isEmpty()) {
            return "最近没有打卡记录。";
        }
        return list.stream()
                .map(c -> String.format("%s：%d组x%d次，重量%.1fkg",
                        c.getCheckinDate(), c.getSetsDone(), c.getRepsDone(), c.getWeightUsed()))
                .collect(Collectors.joining("\n"));
    }
}
