-- ============================================================================
-- RBAC 权限点补录：动作库维护（exercise:manage）
-- 仅 ADMIN 持有；USER 无此权限，访问管理接口会被 @RequireRole 拦截（403）
-- 存量库需手动执行本脚本；全新部署已挂载进 docker-entrypoint-initdb.d
-- ============================================================================
USE fitness_db;

INSERT INTO sys_permission (perm_code, perm_name, perm_type, resource)
SELECT 'exercise:manage', '维护健身动作库', 'MENU', '/admin/exercises'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE perm_code = 'exercise:manage');

INSERT INTO sys_role_permission (role_id, permission_id)
SELECT r.id, p.id FROM sys_role r JOIN sys_permission p
WHERE r.role_code = 'ADMIN' AND p.perm_code = 'exercise:manage'
  AND NOT EXISTS (
      SELECT 1 FROM sys_role_permission rp WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );
