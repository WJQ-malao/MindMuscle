package com.fitness.ai.service;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fitness.ai.entity.SysConfig;
import com.fitness.ai.mapper.SysConfigMapper;
import org.springframework.stereotype.Service;

/** 系统键值配置：目前用于 AI 教练启停开关（ai_coach_enabled，1=开启 0=关闭） */
@Service
public class SysConfigService {

    public static final String AI_COACH_ENABLED_KEY = "ai_coach_enabled";
    public static final String DISABLED_TIP = "AI 教练功能暂时关闭，请联系管理员开启后再试";

    private final SysConfigMapper configMapper;

    public SysConfigService(SysConfigMapper configMapper) {
        this.configMapper = configMapper;
    }

    /** AI 教练是否开启：缺省视为开启（表未初始化/键不存在时不影响正常使用） */
    public boolean isAiCoachEnabled() {
        SysConfig config = configMapper.selectById(AI_COACH_ENABLED_KEY);
        return config == null || !"0".equals(config.getConfigValue());
    }

    /** 开启/关闭 AI 教练：有记录则更新，无记录则插入（幂等） */
    public void setAiCoachEnabled(boolean enabled) {
        String value = enabled ? "1" : "0";
        int updated = configMapper.update(null, new LambdaUpdateWrapper<SysConfig>()
                .eq(SysConfig::getConfigKey, AI_COACH_ENABLED_KEY)
                .set(SysConfig::getConfigValue, value));
        if (updated == 0) {
            SysConfig config = new SysConfig();
            config.setConfigKey(AI_COACH_ENABLED_KEY);
            config.setConfigValue(value);
            configMapper.insert(config);
        }
    }
}
