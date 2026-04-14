-- 为 MyBatis-Plus 逻辑删除（@TableLogic）补充 deleted 字段
-- 在已存在的数据库上执行一次；若某列已存在会报错，可跳过对应语句或先检查 information_schema
-- 2026-04-13：分类表与后端一致为 sys_category（旧库若仍为 product_category 请先 RENAME 或改用下列对应表名）

USE `smart_ims`;

ALTER TABLE `sys_user` ADD COLUMN `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除' AFTER `update_time`;
ALTER TABLE `sys_role` ADD COLUMN `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除' AFTER `update_time`;
ALTER TABLE `sys_permission` ADD COLUMN `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除' AFTER `update_time`;
ALTER TABLE `sys_operation_log` ADD COLUMN `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除' AFTER `create_time`;
ALTER TABLE `sys_category` ADD COLUMN `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除' AFTER `update_time`;
ALTER TABLE `product` ADD COLUMN `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除' AFTER `update_time`;
ALTER TABLE `supplier` ADD COLUMN `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除' AFTER `update_time`;
ALTER TABLE `customer` ADD COLUMN `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除' AFTER `update_time`;
ALTER TABLE `warehouse` ADD COLUMN `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除' AFTER `update_time`;
ALTER TABLE `inventory` ADD COLUMN `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除' AFTER `update_time`;
ALTER TABLE `inventory_warning` ADD COLUMN `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除' AFTER `create_time`;
ALTER TABLE `inventory_transfer` ADD COLUMN `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除' AFTER `update_time`;
ALTER TABLE `purchase_order` ADD COLUMN `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除' AFTER `update_time`;
ALTER TABLE `inbound_record` ADD COLUMN `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除' AFTER `create_time`;
ALTER TABLE `sales_order` ADD COLUMN `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除' AFTER `update_time`;
ALTER TABLE `aftersales_order` ADD COLUMN `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除' AFTER `update_time`;
