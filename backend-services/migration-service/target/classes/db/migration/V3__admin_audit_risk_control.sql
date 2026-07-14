SET NAMES utf8mb4;

ALTER TABLE admin_roles
  ADD COLUMN system_builtin bit(1) NOT NULL DEFAULT b'0';

ALTER TABLE exam_modules
  ADD COLUMN system_builtin bit(1) NOT NULL DEFAULT b'0';

ALTER TABLE users
  ADD COLUMN enabled bit(1) NOT NULL DEFAULT b'1';

CREATE TABLE admin_operation_logs (
  id bigint NOT NULL AUTO_INCREMENT,
  admin_id bigint DEFAULT NULL,
  admin_username varchar(255) DEFAULT NULL,
  module varchar(80) NOT NULL,
  action varchar(80) NOT NULL,
  target_id varchar(255) DEFAULT NULL,
  request_method varchar(20) DEFAULT NULL,
  request_path varchar(500) DEFAULT NULL,
  ip_address varchar(100) DEFAULT NULL,
  user_agent varchar(1000) DEFAULT NULL,
  success bit(1) NOT NULL,
  error_message varchar(1000) DEFAULT NULL,
  request_body longtext DEFAULT NULL,
  response_body longtext DEFAULT NULL,
  created_at datetime(6) NOT NULL,
  PRIMARY KEY (id),
  KEY idx_admin_operation_admin_time (admin_id, created_at),
  KEY idx_admin_operation_module_action (module, action),
  KEY idx_admin_operation_success_time (success, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE admin_permission_change_logs (
  id bigint NOT NULL AUTO_INCREMENT,
  admin_id bigint DEFAULT NULL,
  admin_username varchar(255) DEFAULT NULL,
  role_id bigint NOT NULL,
  role_code varchar(255) DEFAULT NULL,
  before_permission_ids longtext DEFAULT NULL,
  after_permission_ids longtext DEFAULT NULL,
  added_permission_ids longtext DEFAULT NULL,
  removed_permission_ids longtext DEFAULT NULL,
  created_at datetime(6) NOT NULL,
  PRIMARY KEY (id),
  KEY idx_permission_change_role_time (role_id, created_at),
  KEY idx_permission_change_admin_time (admin_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

UPDATE admin_roles
SET system_builtin = b'1'
WHERE code IN ('ADMIN', 'USER');

UPDATE exam_modules
SET system_builtin = b'1'
WHERE code IN ('shop', 'selected-readings', 'cet4', 'cet6', 'toefl', 'ielts', 'kaoyan', 'gre');

INSERT INTO admin_permissions (code, name, description, menu_path, sort_order)
SELECT 'AUDIT_LOGS', '审计日志', '查看管理员操作日志和权限变更记录', '/admin/audit', 8
WHERE NOT EXISTS (SELECT 1 FROM admin_permissions WHERE code = 'AUDIT_LOGS');

INSERT INTO admin_role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM admin_roles r
JOIN admin_permissions p ON p.code = 'AUDIT_LOGS'
WHERE r.code = 'ADMIN'
  AND NOT EXISTS (
    SELECT 1 FROM admin_role_permissions rp
    WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );
