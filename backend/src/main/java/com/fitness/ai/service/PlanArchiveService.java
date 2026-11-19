package com.fitness.ai.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fitness.ai.entity.PlanArchive;
import com.fitness.ai.entity.PlanArchiveItem;
import com.fitness.ai.entity.WorkoutItem;
import com.fitness.ai.entity.WorkoutPlan;
import com.fitness.ai.mapper.PlanArchiveItemMapper;
import com.fitness.ai.mapper.PlanArchiveMapper;
import com.fitness.ai.mapper.WorkoutItemMapper;
import com.fitness.ai.mapper.WorkoutPlanMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class PlanArchiveService {

    private final PlanArchiveMapper archiveMapper;
    private final PlanArchiveItemMapper archiveItemMapper;
    private final WorkoutPlanMapper planMapper;
    private final WorkoutItemMapper itemMapper;

    public PlanArchiveService(PlanArchiveMapper archiveMapper, PlanArchiveItemMapper archiveItemMapper,
                              WorkoutPlanMapper planMapper, WorkoutItemMapper itemMapper) {
        this.archiveMapper = archiveMapper;
        this.archiveItemMapper = archiveItemMapper;
        this.planMapper = planMapper;
        this.itemMapper = itemMapper;
    }

    public List<PlanArchive> list(Long userId) {
        return archiveMapper.selectList(new LambdaQueryWrapper<PlanArchive>()
                .eq(PlanArchive::getUserId, userId)
                .orderByDesc(PlanArchive::getId));
    }

    /** 存档明细（含归属校验） */
    public List<PlanArchiveItem> items(Long userId, Long archiveId) {
        requireOwned(userId, archiveId);
        return archiveItemMapper.selectList(new LambdaQueryWrapper<PlanArchiveItem>()
                .eq(PlanArchiveItem::getArchiveId, archiveId)
                .orderByAsc(PlanArchiveItem::getDayOfWeek)
                .orderByAsc(PlanArchiveItem::getSort));
    }

    /** 把训练计划快照保存为存档（深拷贝明细，之后改计划不影响存档） */
    @Transactional
    public PlanArchive createFromPlan(Long userId, Long planId, String name) {
        WorkoutPlan plan = planMapper.selectById(planId);
        if (plan == null || !plan.getUserId().equals(userId)) {
            throw new IllegalArgumentException("计划不存在或无权操作");
        }
        PlanArchive archive = new PlanArchive();
        archive.setUserId(userId);
        archive.setName(name);
        archive.setGoal(plan.getGoal());
        archiveMapper.insert(archive);

        List<WorkoutItem> items = itemMapper.selectList(new LambdaQueryWrapper<WorkoutItem>()
                .eq(WorkoutItem::getPlanId, planId)
                .orderByAsc(WorkoutItem::getDayOfWeek)
                .orderByAsc(WorkoutItem::getSort));
        for (WorkoutItem src : items) {
            PlanArchiveItem it = new PlanArchiveItem();
            it.setArchiveId(archive.getId());
            it.setDayOfWeek(src.getDayOfWeek());
            it.setExerciseName(src.getExerciseName());
            it.setMuscleGroup(src.getMuscleGroup());
            it.setSets(src.getSets());
            it.setReps(src.getReps());
            it.setWeightKg(src.getWeightKg());
            it.setSort(src.getSort());
            archiveItemMapper.insert(it);
        }
        return archive;
    }

    /** 重命名存档 */
    public PlanArchive rename(Long userId, Long archiveId, String name) {
        PlanArchive archive = requireOwned(userId, archiveId);
        archive.setName(name);
        archiveMapper.updateById(archive);
        return archive;
    }

    /** 删除存档（连带明细） */
    @Transactional
    public void delete(Long userId, Long archiveId) {
        requireOwned(userId, archiveId);
        archiveItemMapper.delete(new LambdaQueryWrapper<PlanArchiveItem>()
                .eq(PlanArchiveItem::getArchiveId, archiveId));
        archiveMapper.deleteById(archiveId);
    }

    private PlanArchive requireOwned(Long userId, Long archiveId) {
        PlanArchive archive = archiveMapper.selectById(archiveId);
        if (archive == null || !archive.getUserId().equals(userId)) {
            throw new IllegalArgumentException("存档不存在或无权操作");
        }
        return archive;
    }
}
