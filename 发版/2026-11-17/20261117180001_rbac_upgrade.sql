-- ============================================================================
-- RBAC 权限模型升级：sys_role / sys_user_role / sys_permission / sys_role_permission
-- 说明：升级后用户角色以 sys_user_role 关联表为准，sys_user.role 保留作为冗余主角色字段
-- ============================================================================
USE fitness_db;
SET NAMES utf8mb4;

-- 角色表
CREATE TABLE IF NOT EXISTS sys_role (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    role_code   VARCHAR(50)  NOT NULL UNIQUE COMMENT '角色编码: USER/ADMIN',
    role_name   VARCHAR(100) NOT NULL COMMENT '角色名称',
    description VARCHAR(255) NULL COMMENT '角色描述',
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- 用户-角色关联表（多对多）
CREATE TABLE IF NOT EXISTS sys_user_role (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id     BIGINT   NOT NULL COMMENT '用户ID',
    role_id     BIGINT   NOT NULL COMMENT '角色ID',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_role (user_id, role_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- 权限点表（菜单级 + 接口级）
CREATE TABLE IF NOT EXISTS sys_permission (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    perm_code   VARCHAR(100) NOT NULL UNIQUE COMMENT '权限编码',
    perm_name   VARCHAR(100) NOT NULL COMMENT '权限名称',
    perm_type   VARCHAR(20)  NOT NULL COMMENT 'MENU 菜单 / API 接口',
    resource    VARCHAR(255) NULL COMMENT '菜单路径或API路径模式',
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- 角色-权限关联表（多对多）
CREATE TABLE IF NOT EXISTS sys_role_permission (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    role_id       BIGINT NOT NULL COMMENT '角色ID',
    permission_id BIGINT NOT NULL COMMENT '权限ID',
    UNIQUE KEY uk_role_perm (role_id, permission_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- ========== 种子数据 ==========

INSERT INTO sys_role (role_code, role_name, description) VALUES
('USER',  '普通用户',   '健身会员，使用个人数据与AI教练功能'),
('ADMIN', '系统管理员', '平台运营管理，维护知识库与查看平台数据');

INSERT INTO sys_permission (perm_code, perm_name, perm_type, resource) VALUES
('dashboard:view', '查看个人看板',       'MENU', '/dashboard'),
('chat:use',       '使用AI教练',         'MENU', '/chat'),
('metric:manage',  '管理身体数据',       'MENU', '/metrics'),
('workout:manage', '管理训练计划与打卡', 'MENU', '/workout'),
('knowledge:manage', '维护健身知识库',   'MENU', '/knowledge'),
('admin:overview', '查看平台运营数据',   'API',  '/api/admin/**');

-- ADMIN 拥有全部权限
INSERT INTO sys_role_permission (role_id, permission_id)
SELECT r.id, p.id FROM sys_role r JOIN sys_permission p WHERE r.role_code = 'ADMIN';

-- USER 拥有除知识库维护与平台数据外的全部权限
INSERT INTO sys_role_permission (role_id, permission_id)
SELECT r.id, p.id FROM sys_role r JOIN sys_permission p
WHERE r.role_code = 'USER'
  AND p.perm_code NOT IN ('knowledge:manage', 'admin:overview');

-- 存量用户迁移：按 sys_user.role 生成关联关系
INSERT INTO sys_user_role (user_id, role_id)
SELECT u.id, r.id FROM sys_user u JOIN sys_role r ON r.role_code = u.role
WHERE NOT EXISTS (SELECT 1 FROM sys_user_role ur WHERE ur.user_id = u.id);
