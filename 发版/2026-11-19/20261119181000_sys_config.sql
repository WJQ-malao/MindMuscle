-- 系统配置表：管理员可维护的键值配置（当前用于 AI 教练启停开关）
CREATE TABLE IF NOT EXISTS sys_config (
  config_key   VARCHAR(64)  NOT NULL COMMENT '配置键',
  config_value VARCHAR(255) NOT NULL COMMENT '配置值',
  update_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (config_key)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='系统配置';

-- AI 教练开关：1=开启（默认），0=关闭；关闭后用户发消息会收到"功能暂时关闭，请联系管理员"提示
INSERT INTO sys_config (config_key, config_value) VALUES ('ai_coach_enabled', '1')
ON DUPLICATE KEY UPDATE config_value = VALUES(config_value);
