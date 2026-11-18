package com.fitness.ai.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fitness.ai.dto.PlanCreateRequest;
import com.fitness.ai.entity.Checkin;
import com.fitness.ai.entity.WorkoutItem;
import com.fitness.ai.entity.WorkoutPlan;
import com.fitness.ai.mapper.CheckinMapper;
import com.fitness.ai.mapper.WorkoutItemMapper;
import com.fitness.ai.mapper.WorkoutPlanMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class WorkoutService {

    private final WorkoutPlanMapper planMapper;
    private final WorkoutItemMapper itemMapper;
    private final CheckinMapper checkinMapper;

    public WorkoutService(WorkoutPlanMapper planMapper, WorkoutItemMapper itemMapper, CheckinMapper checkinMapper) {
        this.planMapper = planMapper;
        this.itemMapper = itemMapper;
        this.checkinMapper = checkinMapper;
    }

    public List<WorkoutPlan> listPlans(Long userId) {
        return planMapper.selectList(new LambdaQueryWrapper<WorkoutPlan>()
                .eq(WorkoutPlan::getUserId, userId)
                .orderByDesc(WorkoutPlan::getId));
    }

    public List<WorkoutItem> listItems(Long planId) {
        return itemMapper.selectList(new LambdaQueryWrapper<WorkoutItem>()
                .eq(WorkoutItem::getPlanId, planId)
                .orderByAsc(WorkoutItem::getDayOfWeek)
                .orderByAsc(WorkoutItem::getSort));
    }

    /** 规则模板生成训练计划：具体动作编排由确定性规则完成，
     * 大模型只负责对话和触发，不参与数值与编排决策（护栏设计）。
     * 新计划创建时自动归档旧的进行中计划，保证同时只有一个 ACTIVE。
     */
    @Transactional
    public WorkoutPlan generatePlan(Long userId, String goal, int daysPerWeek) {
        if (daysPerWeek < 1 || daysPerWeek > 7) {
            throw new IllegalArgumentException("每周训练天数必须在 1-7 之间");
        }
        List<WorkoutPlan> activePlans = planMapper.selectList(new LambdaQueryWrapper<WorkoutPlan>()
                .eq(WorkoutPlan::getUserId, userId)
                .eq(WorkoutPlan::getStatus, "ACTIVE"));
        for (WorkoutPlan old : activePlans) {
            old.setStatus("ARCHIVED");
            planMapper.updateById(old);
        }

        WorkoutPlan plan = new WorkoutPlan();
        plan.setUserId(userId);
        plan.setGoal(goal);
        plan.setStatus("ACTIVE");
        plan.setSource("AI");
        plan.setTitle(goal + "训练计划（每周" + daysPerWeek + "练）");
        planMapper.insert(plan);

        List<WorkoutItem> items = buildTemplate(goal, daysPerWeek);
        for (WorkoutItem item : items) {
            item.setPlanId(plan.getId());
            itemMapper.insert(item);
        }
        return plan;
    }

    /** 经典分化模板：根据每周天数选择训练分化方式 */
    private List<WorkoutItem> buildTemplate(String goal, int daysPerWeek) {
        // 训练日分配（从周一开始取连续天数）
        List<Integer> days = new ArrayList<>();
        for (int i = 1; i <= daysPerWeek; i++) {
            days.add(i);
        }
        List<WorkoutItem> items = new ArrayList<>();
        int sort = 0;
        for (int i = 0; i < days.size(); i++) {
            String[] session = pickSession(goal, daysPerWeek, i);
            int finalI = i;
            int finalSort = sort;
            items.addAll(List.of(
                    item(days.get(finalI), session[0], session[1], 4, 10, finalSort),
                    item(days.get(finalI), session[0], session[2], 3, 12, finalSort + 1),
                    item(days.get(finalI), "有氧", "慢跑/跳绳", 20, 1, finalSort + 2)
            ));
            sort += 3;
        }
        return items;
    }

    /** 返回 {肌群, 动作1, 动作2}，按目标和当天序号轮换 */
    private String[] pickSession(String goal, int daysPerWeek, int index) {
        boolean fatLoss = goal != null && goal.contains("脂");
        String[][] rotation = fatLoss
                ? new String[][]{
                    {"腿", "深蹲", "箭步蹲"},
                    {"胸", "俯卧撑", "哑铃卧推"},
                    {"背", "引体向上", "划船"},
                    {"核心", "平板支撑", "卷腹"},
                    {"有氧", "HIIT循环", "波比跳"}}
                : new String[][]{
                    {"胸", "杠铃卧推", "哑铃飞鸟"},
                    {"背", "引体向上", "杠铃划船"},
                    {"腿", "深蹲", "硬拉"},
                    {"肩", "哑铃推举", "侧平举"},
                    {"手臂", "弯举", "臂屈伸"}};
        return rotation[index % rotation.length];
    }

    private WorkoutItem item(int day, String muscle, String exercise, int sets, int reps, int sort) {
        WorkoutItem it = new WorkoutItem();
        it.setDayOfWeek(day);
        it.setMuscleGroup(muscle);
        it.setExerciseName(exercise);
        it.setSets(sets);
        it.setReps(reps);
        it.setSort(sort);
        return it;
    }

    @Transactional
    public Checkin checkin(Long userId, Long itemId, Integer setsDone, Integer repsDone, Double weightUsed) {
        WorkoutItem item = itemMapper.selectById(itemId);
        if (item == null) {
            throw new IllegalArgumentException("训练项不存在");
        }
        WorkoutPlan plan = planMapper.selectById(item.getPlanId());
        if (plan == null || !plan.getUserId().equals(userId)) {
            throw new IllegalArgumentException("无权操作该训练项");
        }
        if (setsDone == null || setsDone < 1 || setsDone > 30) {
            throw new IllegalArgumentException("组数必须在 1-30 之间");
        }
        if (repsDone == null || repsDone < 1 || repsDone > 100) {
            throw new IllegalArgumentException("次数必须在 1-100 之间");
        }
        Checkin checkin = new Checkin();
        checkin.setUserId(userId);
        checkin.setItemId(itemId);
        checkin.setSetsDone(setsDone);
        checkin.setRepsDone(repsDone);
        checkin.setWeightUsed(weightUsed == null ? 0 : weightUsed);
        checkin.setCheckinDate(LocalDate.now());
        checkinMapper.insert(checkin);
        return checkin;
    }

    public List<Checkin> recentCheckins(Long userId, int limit) {
        return checkinMapper.selectList(new LambdaQueryWrapper<Checkin>()
                .eq(Checkin::getUserId, userId)
                .orderByDesc(Checkin::getId)
                .last("LIMIT " + limit));
    }

    // ============ 手动编辑（Keep 式） ============

    /** 手动创建空计划（来源 MANUAL），旧的进行中计划自动归档 */
    @Transactional
    public WorkoutPlan createManualPlan(Long userId, String title, String goal) {
        List<WorkoutPlan> activePlans = planMapper.selectList(new LambdaQueryWrapper<WorkoutPlan>()
                .eq(WorkoutPlan::getUserId, userId)
                .eq(WorkoutPlan::getStatus, "ACTIVE"));
        for (WorkoutPlan old : activePlans) {
            old.setStatus("ARCHIVED");
            planMapper.updateById(old);
        }
        WorkoutPlan plan = new WorkoutPlan();
        plan.setUserId(userId);
        plan.setTitle(title);
        plan.setGoal(goal);
        plan.setStatus("ACTIVE");
        plan.setSource("MANUAL");
        planMapper.insert(plan);
        return plan;
    }

    /**
     * 篮子模式创建计划（Keep 式流程：先选动作再建计划）：
     * 动作按选择顺序轮流分摊到每周 N 个训练日，之后再手动微调。
     */
    @Transactional
    public WorkoutPlan createPlanFromBasket(Long userId, String title, String goal, int daysPerWeek,
                                            List<PlanCreateRequest.Item> items) {
        WorkoutPlan plan = createManualPlan(userId, title, goal);
        int[] daySort = new int[8];
        for (int i = 0; i < items.size(); i++) {
            PlanCreateRequest.Item src = items.get(i);
            int dayOfWeek = i % daysPerWeek + 1;
            WorkoutItem it = new WorkoutItem();
            it.setPlanId(plan.getId());
            it.setExerciseName(src.exerciseName());
            it.setMuscleGroup(src.muscleGroup());
            it.setDayOfWeek(dayOfWeek);
            it.setSets(src.sets());
            it.setReps(src.reps());
            it.setWeightKg(src.weightKg());
            it.setSort(daySort[dayOfWeek]++);
            itemMapper.insert(it);
        }
        return plan;
    }

    /** 为计划的某一天添加动作 */
    public WorkoutItem addItem(Long userId, Long planId, String exerciseName, String muscleGroup,
                               int dayOfWeek, int sets, int reps, Double weightKg) {
        requireOwnedPlan(userId, planId);
        WorkoutItem it = new WorkoutItem();
        it.setPlanId(planId);
        it.setExerciseName(exerciseName);
        it.setMuscleGroup(muscleGroup);
        it.setDayOfWeek(dayOfWeek);
        it.setSets(sets);
        it.setReps(reps);
        it.setWeightKg(weightKg);
        Long maxSort = itemMapper.selectCount(new LambdaQueryWrapper<WorkoutItem>()
                .eq(WorkoutItem::getPlanId, planId)
                .eq(WorkoutItem::getDayOfWeek, dayOfWeek));
        it.setSort(maxSort.intValue());
        itemMapper.insert(it);
        return it;
    }

    /** 修改动作（换动作/调组数次数重量/移到别的训练日） */
    public WorkoutItem updateItem(Long userId, Long itemId, String exerciseName, String muscleGroup,
                                  int dayOfWeek, int sets, int reps, Double weightKg) {
        WorkoutItem it = requireOwnedItem(userId, itemId);
        it.setExerciseName(exerciseName);
        it.setMuscleGroup(muscleGroup);
        it.setDayOfWeek(dayOfWeek);
        it.setSets(sets);
        it.setReps(reps);
        it.setWeightKg(weightKg);
        itemMapper.updateById(it);
        return it;
    }

    /** 删除某个动作 */
    public void deleteItem(Long userId, Long itemId) {
        WorkoutItem it = requireOwnedItem(userId, itemId);
        itemMapper.deleteById(it.getId());
    }

    private WorkoutPlan requireOwnedPlan(Long userId, Long planId) {
        WorkoutPlan plan = planMapper.selectById(planId);
        if (plan == null || !plan.getUserId().equals(userId)) {
            throw new IllegalArgumentException("计划不存在或无权操作");
        }
        return plan;
    }

    /** 带归属校验的明细查询（对外接口用） */
    public List<WorkoutItem> listItemsChecked(Long userId, Long planId) {
        requireOwnedPlan(userId, planId);
        return listItems(planId);
    }

    private WorkoutItem requireOwnedItem(Long userId, Long itemId) {
        WorkoutItem it = itemMapper.selectById(itemId);
        if (it == null) {
            throw new IllegalArgumentException("训练项不存在");
        }
        requireOwnedPlan(userId, it.getPlanId());
        return it;
    }

    /** 供 AI 工具读取：用户当前计划 + 明细的文本摘要 */
    public String summarizeActivePlan(Long userId) {
        WorkoutPlan plan = planMapper.selectOne(new LambdaQueryWrapper<WorkoutPlan>()
                .eq(WorkoutPlan::getUserId, userId)
                .eq(WorkoutPlan::getStatus, "ACTIVE")
                .orderByDesc(WorkoutPlan::getId)
                .last("LIMIT 1"));
        if (plan == null) {
            return "当前没有进行中的训练计划";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("计划：").append(plan.getTitle()).append("，目标：").append(plan.getGoal()).append("。\n");
        Map<Integer, List<WorkoutItem>> byDay = new LinkedHashMap<>();
        for (WorkoutItem it : listItems(plan.getId())) {
            byDay.computeIfAbsent(it.getDayOfWeek(), k -> new ArrayList<>()).add(it);
        }
        String[] dayNames = {"", "周一", "周二", "周三", "周四", "周五", "周六", "周日"};
        byDay.forEach((day, list) -> {
            sb.append(dayNames[day]).append("：");
            list.forEach(it -> sb.append(it.getExerciseName())
                    .append(it.getWeightKg() != null ? " " + it.getWeightKg() + "kg" : "")
                    .append(" ").append(it.getSets()).append("组x").append(it.getReps())
                    .append(it.getMuscleGroup().equals("有氧") ? "分钟" : "次").append("；"));
            sb.append("\n");
        });
        return sb.toString();
    }
}
