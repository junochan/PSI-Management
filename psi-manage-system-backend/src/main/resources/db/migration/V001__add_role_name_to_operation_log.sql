-- 添加 role_name 字段到 sys_operation_log 表
ALTER TABLE sys_operation_log ADD COLUMN role_name VARCHAR(50) COMMENT '角色名称' AFTER user_name;