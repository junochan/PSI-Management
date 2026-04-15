-- =====================================================
-- 智链进销存管理系统（Smart IMS）数据库脚本
-- 依据后端实体 com.smartims.entity 生成（MyBatis-Plus）
-- Database: MySQL 8.0+
-- Encoding: utf8mb4
-- Version: 2.0
-- Date: 2026-04-13
-- =====================================================

CREATE DATABASE IF NOT EXISTS `smart_ims`
DEFAULT CHARACTER SET utf8mb4
DEFAULT COLLATE utf8mb4_unicode_ci;

USE `smart_ims`;

-- =====================================================
-- 1. 用户与权限
-- =====================================================

DROP TABLE IF EXISTS `sys_user`;
CREATE TABLE `sys_user` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '用户主键ID',
  `username` VARCHAR(50) NOT NULL COMMENT '用户名',
  `password` VARCHAR(100) NOT NULL COMMENT '密码（BCrypt加密）',
  `name` VARCHAR(50) NOT NULL COMMENT '用户姓名',
  `email` VARCHAR(100) DEFAULT NULL COMMENT '邮箱',
  `phone` VARCHAR(20) DEFAULT NULL COMMENT '手机号',
  `avatar` VARCHAR(255) DEFAULT NULL COMMENT '头像URL',
  `role_id` BIGINT DEFAULT NULL COMMENT '角色ID',
  `status` TINYINT DEFAULT 1 COMMENT '状态：1-正常，0-禁用',
  `last_login_time` DATETIME DEFAULT NULL COMMENT '最后登录时间',
  `last_login_ip` VARCHAR(50) DEFAULT NULL COMMENT '最后登录IP',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`),
  UNIQUE KEY `uk_email` (`email`),
  KEY `idx_role_id` (`role_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统用户表';

DROP TABLE IF EXISTS `sys_role`;
CREATE TABLE `sys_role` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '角色主键ID',
  `name` VARCHAR(50) NOT NULL COMMENT '角色名称',
  `code` VARCHAR(50) NOT NULL COMMENT '角色编码',
  `description` VARCHAR(200) DEFAULT NULL COMMENT '角色描述',
  `status` TINYINT DEFAULT 1 COMMENT '状态：1-启用，0-禁用',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_code` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统角色表';

DROP TABLE IF EXISTS `sys_permission`;
CREATE TABLE `sys_permission` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '权限主键ID',
  `name` VARCHAR(50) NOT NULL COMMENT '权限名称',
  `code` VARCHAR(100) NOT NULL COMMENT '权限编码',
  `type` TINYINT NOT NULL COMMENT '权限类型：1-菜单，2-按钮，3-接口',
  `parent_id` BIGINT DEFAULT 0 COMMENT '父权限ID',
  `path` VARCHAR(200) DEFAULT NULL COMMENT '路由路径',
  `icon` VARCHAR(50) DEFAULT NULL COMMENT '图标',
  `sort` INT DEFAULT 0 COMMENT '排序',
  `status` TINYINT DEFAULT 1 COMMENT '状态：1-启用，0-禁用',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_code` (`code`),
  KEY `idx_parent_id` (`parent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统权限表';

DROP TABLE IF EXISTS `sys_role_permission`;
CREATE TABLE `sys_role_permission` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `role_id` BIGINT NOT NULL COMMENT '角色ID',
  `permission_id` BIGINT NOT NULL COMMENT '权限ID',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_role_permission` (`role_id`, `permission_id`),
  KEY `idx_role_id` (`role_id`),
  KEY `idx_permission_id` (`permission_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色权限关联表（无逻辑删除）';

DROP TABLE IF EXISTS `sys_operation_log`;
CREATE TABLE `sys_operation_log` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '日志主键ID',
  `user_id` BIGINT DEFAULT NULL COMMENT '操作用户ID',
  `user_name` VARCHAR(50) DEFAULT NULL COMMENT '操作用户名',
  `role_name` VARCHAR(50) DEFAULT NULL COMMENT '操作人角色名称',
  `action` VARCHAR(50) NOT NULL COMMENT '操作类型',
  `content` VARCHAR(500) NOT NULL COMMENT '操作内容',
  `module` VARCHAR(50) DEFAULT NULL COMMENT '操作模块',
  `ip` VARCHAR(50) DEFAULT NULL COMMENT '操作IP',
  `status` TINYINT DEFAULT 1 COMMENT '操作状态：1-成功，0-失败',
  `error_msg` TEXT DEFAULT NULL COMMENT '错误信息',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_action` (`action`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统操作日志表';

-- =====================================================
-- 2. 商品分类与商品（实体：SysCategory, Product）
-- =====================================================

DROP TABLE IF EXISTS `sys_category`;
CREATE TABLE `sys_category` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '分类主键ID',
  `name` VARCHAR(50) NOT NULL COMMENT '分类名称',
  `code` VARCHAR(50) NOT NULL COMMENT '分类编码',
  `parent_id` BIGINT DEFAULT 0 COMMENT '父分类ID（0 表示顶级）',
  `sort` INT DEFAULT 0 COMMENT '排序',
  `status` TINYINT DEFAULT 1 COMMENT '状态：1-启用，0-禁用',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_code` (`code`),
  KEY `idx_parent_id` (`parent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品分类表';

DROP TABLE IF EXISTS `product`;
CREATE TABLE `product` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '商品主键ID',
  `code` VARCHAR(20) NOT NULL COMMENT '商品编码',
  `name` VARCHAR(100) NOT NULL COMMENT '商品名称',
  `brand` VARCHAR(50) DEFAULT NULL COMMENT '品牌',
  `spec` VARCHAR(100) DEFAULT NULL COMMENT '规格',
  `category_id` BIGINT DEFAULT NULL COMMENT '分类ID',
  `category_name` VARCHAR(50) DEFAULT NULL COMMENT '分类名称',
  `cost_price` DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '成本价',
  `sale_price` DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '销售价',
  `status` VARCHAR(10) DEFAULT NULL COMMENT '状态：在售/停售',
  `image` MEDIUMTEXT DEFAULT NULL COMMENT '商品图片URL或Base64数据URL',
  `description` TEXT DEFAULT NULL COMMENT '商品描述',
  `stock` INT DEFAULT 0 COMMENT '库存数量（汇总展示用，以 inventory 为准）',
  `safe_stock` INT DEFAULT 10 COMMENT '安全库存预警值',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_code` (`code`),
  KEY `idx_category_id` (`category_id`),
  KEY `idx_status` (`status`),
  KEY `idx_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品表';

-- =====================================================
-- 3. 供应商与客户
-- =====================================================

DROP TABLE IF EXISTS `supplier_industry_link`;
DROP TABLE IF EXISTS `supplier`;
DROP TABLE IF EXISTS `supplier_industry`;

CREATE TABLE `supplier_industry` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '行业主键ID',
  `name` VARCHAR(50) NOT NULL COMMENT '行业名称',
  `code` VARCHAR(30) DEFAULT NULL COMMENT '编码',
  `sort` INT NOT NULL DEFAULT 0 COMMENT '排序，越小越靠前',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1-启用，0-停用',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_name` (`name`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='供应商所属行业字典';

INSERT INTO `supplier_industry` (`id`, `name`, `code`, `sort`, `status`, `deleted`) VALUES
(1, '童装零售', 'KIDS_RETAIL', 10, 1, 0),
(2, '儿童服饰制造 / 生产', 'KIDS_MFG', 20, 1, 0),
(3, '服装批发', 'APPAREL_WHOLESALE', 30, 1, 0),
(4, '母婴用品零售', 'MOTHER_BABY', 40, 1, 0),
(5, '鞋帽零售', 'SHOES_HATS', 50, 1, 0);

CREATE TABLE `supplier` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '供应商主键ID',
  `name` VARCHAR(100) NOT NULL COMMENT '供应商名称',
  `code` VARCHAR(20) DEFAULT NULL COMMENT '供应商编码',
  `address` VARCHAR(200) DEFAULT NULL COMMENT '地址',
  `phone` VARCHAR(20) DEFAULT NULL COMMENT '联系电话',
  `email` VARCHAR(100) DEFAULT NULL COMMENT '邮箱',
  `contact` VARCHAR(50) DEFAULT NULL COMMENT '联系人',
  `delivery_days` INT DEFAULT 5 COMMENT '平均货期（天）',
  `bank_name` VARCHAR(100) DEFAULT NULL COMMENT '开户银行',
  `bank_account` VARCHAR(50) DEFAULT NULL COMMENT '银行账号',
  `status` TINYINT DEFAULT 1 COMMENT '状态：1-合作中，0-已停用',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_name` (`name`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='供应商表';

CREATE TABLE `supplier_industry_link` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '关联主键',
  `supplier_id` BIGINT NOT NULL COMMENT '供应商ID',
  `industry_id` BIGINT NOT NULL COMMENT '行业ID，关联 supplier_industry.id',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_supplier_industry` (`supplier_id`, `industry_id`),
  KEY `idx_industry_id` (`industry_id`),
  CONSTRAINT `fk_sil_supplier` FOREIGN KEY (`supplier_id`) REFERENCES `supplier` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_sil_industry` FOREIGN KEY (`industry_id`) REFERENCES `supplier_industry` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='供应商与行业多对多关联';

DROP TABLE IF EXISTS `customer`;
CREATE TABLE `customer` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '客户主键ID',
  `name` VARCHAR(100) NOT NULL COMMENT '客户名称',
  `code` VARCHAR(20) DEFAULT NULL COMMENT '客户编码',
  `type` VARCHAR(20) DEFAULT NULL COMMENT '客户类型：企业/个人',
  `address` VARCHAR(200) DEFAULT NULL COMMENT '地址',
  `phone` VARCHAR(20) DEFAULT NULL COMMENT '联系电话',
  `email` VARCHAR(100) DEFAULT NULL COMMENT '邮箱',
  `contact` VARCHAR(50) DEFAULT NULL COMMENT '联系人',
  `vip_level` VARCHAR(20) DEFAULT NULL COMMENT 'VIP等级',
  `credit_amount` DECIMAL(12,2) DEFAULT 0 COMMENT '信用额度',
  `total_orders` INT DEFAULT 0 COMMENT '累计订单数',
  `total_amount` DECIMAL(12,2) DEFAULT 0 COMMENT '累计消费金额',
  `last_order_time` DATETIME DEFAULT NULL COMMENT '最近有效销售单下单时间（不含已取消）',
  `status` TINYINT DEFAULT 1 COMMENT '状态：1-正常，0-禁用',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
  PRIMARY KEY (`id`),
  KEY `idx_type` (`type`),
  KEY `idx_vip_level` (`vip_level`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='客户表';

-- =====================================================
-- 4. 仓库与库存
-- =====================================================

DROP TABLE IF EXISTS `warehouse`;
CREATE TABLE `warehouse` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '仓库主键ID',
  `name` VARCHAR(100) NOT NULL COMMENT '仓库名称',
  `code` VARCHAR(20) DEFAULT NULL COMMENT '仓库编码',
  `address` VARCHAR(200) DEFAULT NULL COMMENT '仓库地址',
  `manager_id` BIGINT DEFAULT NULL COMMENT '仓库管理员ID',
  `manager_name` VARCHAR(50) DEFAULT NULL COMMENT '仓库管理员姓名',
  `total_categories` INT DEFAULT 0 COMMENT '存储品类数',
  `total_stock` INT DEFAULT 0 COMMENT '总库存量',
  `total_value` DECIMAL(12,2) DEFAULT 0 COMMENT '库存总值',
  `capacity` INT DEFAULT 100 COMMENT '容量上限',
  `capacity_used` DECIMAL(8,2) DEFAULT 0 COMMENT '容量使用率（%）',
  `status` TINYINT DEFAULT 1 COMMENT '状态：1-正常，0-停用',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_name` (`name`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='仓库表';

DROP TABLE IF EXISTS `inventory`;
CREATE TABLE `inventory` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '库存主键ID',
  `sku` VARCHAR(20) NOT NULL COMMENT 'SKU编码',
  `product_id` BIGINT NOT NULL COMMENT '商品ID',
  `product_name` VARCHAR(100) DEFAULT NULL COMMENT '商品名称',
  `spec` VARCHAR(100) DEFAULT NULL COMMENT '规格',
  `category` VARCHAR(50) DEFAULT NULL COMMENT '分类',
  `warehouse_id` BIGINT NOT NULL COMMENT '仓库ID',
  `warehouse_name` VARCHAR(100) DEFAULT NULL COMMENT '仓库名称',
  `location` VARCHAR(50) DEFAULT NULL COMMENT '库位',
  `stock` INT NOT NULL DEFAULT 0 COMMENT '当前库存',
  `safe_stock` INT DEFAULT 30 COMMENT '安全库存阈值',
  `cost_price` DECIMAL(10,2) DEFAULT 0 COMMENT '成本价',
  `stock_value` DECIMAL(12,2) DEFAULT 0 COMMENT '库存价值',
  `status` VARCHAR(20) DEFAULT NULL COMMENT '库存状态：normal/warning/critical（与后端一致）',
  `stagnant_days` INT DEFAULT NULL COMMENT '呆滞预警天数',
  `last_inbound_time` DATETIME DEFAULT NULL COMMENT '最后入库时间',
  `last_outbound_time` DATETIME DEFAULT NULL COMMENT '最后出库时间',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_sku` (`sku`),
  KEY `idx_product_id` (`product_id`),
  KEY `idx_warehouse_id` (`warehouse_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='库存表';

DROP TABLE IF EXISTS `inventory_warning`;
CREATE TABLE `inventory_warning` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '预警主键ID',
  `type` VARCHAR(20) NOT NULL COMMENT '预警类型：critical/warning/info',
  `sku` VARCHAR(20) DEFAULT NULL COMMENT 'SKU编码',
  `product_name` VARCHAR(100) DEFAULT NULL COMMENT '商品名称',
  `content` VARCHAR(500) NOT NULL COMMENT '预警内容',
  `warehouse_id` BIGINT DEFAULT NULL COMMENT '仓库ID',
  `warehouse_name` VARCHAR(100) DEFAULT NULL COMMENT '仓库名称',
  `current_stock` INT DEFAULT NULL COMMENT '当前库存',
  `safe_stock` INT DEFAULT NULL COMMENT '安全库存',
  `expire_date` DATETIME DEFAULT NULL COMMENT '过期时间（实体为 LocalDateTime）',
  `stock_age` INT DEFAULT NULL COMMENT '库龄（天）',
  `status` TINYINT DEFAULT 0 COMMENT '处理状态：0-待处理，1-已处理',
  `handle_time` DATETIME DEFAULT NULL COMMENT '处理时间',
  `handler_id` BIGINT DEFAULT NULL COMMENT '处理人ID',
  `handler_name` VARCHAR(50) DEFAULT NULL COMMENT '处理人姓名',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
  PRIMARY KEY (`id`),
  KEY `idx_type` (`type`),
  KEY `idx_status` (`status`),
  KEY `idx_sku` (`sku`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='库存预警表';

DROP TABLE IF EXISTS `inventory_transfer`;
CREATE TABLE `inventory_transfer` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '调拨主键ID',
  `order_no` VARCHAR(20) NOT NULL COMMENT '调拨单号',
  `product_id` BIGINT NOT NULL COMMENT '商品ID',
  `product_name` VARCHAR(100) DEFAULT NULL COMMENT '商品名称',
  `sku` VARCHAR(20) DEFAULT NULL COMMENT 'SKU编码',
  `from_warehouse_id` BIGINT NOT NULL COMMENT '调出仓库ID',
  `from_warehouse_name` VARCHAR(100) DEFAULT NULL COMMENT '调出仓库名称',
  `to_warehouse_id` BIGINT NOT NULL COMMENT '调入仓库ID',
  `to_warehouse_name` VARCHAR(100) DEFAULT NULL COMMENT '调入仓库名称',
  `quantity` INT NOT NULL COMMENT '调拨数量',
  `status` VARCHAR(20) DEFAULT NULL COMMENT '状态：调拨中/已完成/已取消',
  `operator_id` BIGINT DEFAULT NULL COMMENT '操作人ID',
  `operator_name` VARCHAR(50) DEFAULT NULL COMMENT '操作人姓名',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  `complete_time` DATETIME DEFAULT NULL COMMENT '完成时间',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_no` (`order_no`),
  KEY `idx_product_id` (`product_id`),
  KEY `idx_status` (`status`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='库存调拨记录表';

-- =====================================================
-- 5. 采购与入库
-- =====================================================

DROP TABLE IF EXISTS `purchase_order`;
CREATE TABLE `purchase_order` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '采购订单主键ID',
  `order_no` VARCHAR(20) NOT NULL COMMENT '订单编号',
  `supplier_id` BIGINT NOT NULL COMMENT '供应商ID',
  `supplier_name` VARCHAR(100) DEFAULT NULL COMMENT '供应商名称',
  `product_id` BIGINT NOT NULL COMMENT '商品ID',
  `product_name` VARCHAR(100) DEFAULT NULL COMMENT '商品名称',
  `sku` VARCHAR(20) DEFAULT NULL COMMENT 'SKU编码',
  `unit_price` DECIMAL(10,2) NOT NULL COMMENT '采购单价',
  `total_quantity` INT NOT NULL COMMENT '采购总量',
  `pending_quantity` INT DEFAULT 0 COMMENT '待入库数量',
  `inbound_quantity` INT DEFAULT 0 COMMENT '已入库数量',
  `amount` DECIMAL(12,2) NOT NULL COMMENT '采购金额',
  `inbound_status` VARCHAR(20) DEFAULT NULL COMMENT '入库状态',
  `pay_status` VARCHAR(20) DEFAULT NULL COMMENT '付款状态',
  `pay_method` VARCHAR(50) DEFAULT NULL COMMENT '付款方式',
  `expect_date` DATE DEFAULT NULL COMMENT '期望交货日期',
  `warehouse_id` BIGINT DEFAULT NULL COMMENT '入库仓库ID',
  `warehouse_name` VARCHAR(100) DEFAULT NULL COMMENT '入库仓库名称',
  `operator_id` BIGINT DEFAULT NULL COMMENT '操作人ID',
  `operator_name` VARCHAR(50) DEFAULT NULL COMMENT '操作人姓名',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_no` (`order_no`),
  KEY `idx_supplier_id` (`supplier_id`),
  KEY `idx_product_id` (`product_id`),
  KEY `idx_inbound_status` (`inbound_status`),
  KEY `idx_pay_status` (`pay_status`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='采购订单表';

DROP TABLE IF EXISTS `inbound_record`;
CREATE TABLE `inbound_record` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '入库记录主键ID',
  `order_no` VARCHAR(20) NOT NULL COMMENT '入库单号',
  `purchase_order_id` BIGINT DEFAULT NULL COMMENT '采购订单ID',
  `purchase_order_no` VARCHAR(20) DEFAULT NULL COMMENT '采购订单编号',
  `supplier_id` BIGINT DEFAULT NULL COMMENT '供应商ID',
  `supplier_name` VARCHAR(100) DEFAULT NULL COMMENT '供应商名称',
  `product_id` BIGINT NOT NULL COMMENT '商品ID',
  `product_name` VARCHAR(100) DEFAULT NULL COMMENT '商品名称',
  `sku` VARCHAR(20) DEFAULT NULL COMMENT 'SKU编码',
  `quantity` INT NOT NULL COMMENT '入库数量',
  `unit_price` DECIMAL(10,2) DEFAULT NULL COMMENT '入库单价',
  `amount` DECIMAL(12,2) DEFAULT NULL COMMENT '入库金额',
  `warehouse_id` BIGINT NOT NULL COMMENT '入库仓库ID',
  `warehouse_name` VARCHAR(100) DEFAULT NULL COMMENT '入库仓库名称',
  `batch_no` VARCHAR(50) DEFAULT NULL COMMENT '批次号',
  `expire_date` DATE DEFAULT NULL COMMENT '过期日期',
  `operator_id` BIGINT DEFAULT NULL COMMENT '操作人ID',
  `operator_name` VARCHAR(50) DEFAULT NULL COMMENT '操作人姓名',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '入库时间',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_no` (`order_no`),
  KEY `idx_purchase_order_id` (`purchase_order_id`),
  KEY `idx_supplier_id` (`supplier_id`),
  KEY `idx_product_id` (`product_id`),
  KEY `idx_warehouse_id` (`warehouse_id`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='入库记录表';

-- =====================================================
-- 6. 销售、出库与售后
-- =====================================================

DROP TABLE IF EXISTS `sales_order`;
CREATE TABLE `sales_order` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '销售订单主键ID',
  `order_no` VARCHAR(20) NOT NULL COMMENT '订单编号',
  `customer_id` BIGINT NOT NULL COMMENT '客户ID',
  `customer_name` VARCHAR(100) DEFAULT NULL COMMENT '客户名称',
  `customer_type` VARCHAR(20) DEFAULT NULL COMMENT '客户类型',
  `product_id` BIGINT NOT NULL COMMENT '商品ID',
  `product_name` VARCHAR(100) DEFAULT NULL COMMENT '商品名称',
  `sku` VARCHAR(20) DEFAULT NULL COMMENT 'SKU编码',
  `unit_price` DECIMAL(10,2) NOT NULL COMMENT '销售单价',
  `quantity` INT NOT NULL COMMENT '销售数量',
  `shipped_quantity` INT DEFAULT 0 COMMENT '已发货数量',
  `pending_quantity` INT DEFAULT 0 COMMENT '待发货数量',
  `amount` DECIMAL(12,2) NOT NULL COMMENT '销售金额',
  `status` VARCHAR(20) DEFAULT NULL COMMENT '订单状态',
  `pay_status` VARCHAR(20) DEFAULT NULL COMMENT '付款状态',
  `pay_method` VARCHAR(50) DEFAULT NULL COMMENT '付款方式',
  `invoice_type` VARCHAR(50) DEFAULT NULL COMMENT '发票类型',
  `invoice_no` VARCHAR(50) DEFAULT NULL COMMENT '发票编号',
  `receiver_name` VARCHAR(50) DEFAULT NULL COMMENT '收货人姓名',
  `receiver_phone` VARCHAR(20) DEFAULT NULL COMMENT '收货人电话',
  `receiver_address` VARCHAR(200) DEFAULT NULL COMMENT '收货地址',
  `logistics_company` VARCHAR(50) DEFAULT NULL COMMENT '物流公司',
  `logistics_no` VARCHAR(50) DEFAULT NULL COMMENT '物流单号',
  `warehouse_id` BIGINT DEFAULT NULL COMMENT '发货仓库ID',
  `warehouse_name` VARCHAR(100) DEFAULT NULL COMMENT '发货仓库名称',
  `ship_time` DATETIME DEFAULT NULL COMMENT '发货时间',
  `pay_time` DATETIME DEFAULT NULL COMMENT '确认付款时间',
  `complete_time` DATETIME DEFAULT NULL COMMENT '确认收货/订单完成时间',
  `expect_arrive_date` DATE DEFAULT NULL COMMENT '预计到达日期',
  `operator_id` BIGINT DEFAULT NULL COMMENT '操作人ID',
  `operator_name` VARCHAR(50) DEFAULT NULL COMMENT '操作人姓名',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_no` (`order_no`),
  KEY `idx_customer_id` (`customer_id`),
  KEY `idx_product_id` (`product_id`),
  KEY `idx_status` (`status`),
  KEY `idx_pay_status` (`pay_status`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='销售订单表';

DROP TABLE IF EXISTS `outbound_record`;
CREATE TABLE `outbound_record` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '出库记录主键ID',
  `order_no` VARCHAR(20) NOT NULL COMMENT '出库单号',
  `sales_order_id` BIGINT DEFAULT NULL COMMENT '销售订单ID',
  `sales_order_no` VARCHAR(20) DEFAULT NULL COMMENT '销售订单编号',
  `customer_id` BIGINT DEFAULT NULL COMMENT '客户ID',
  `customer_name` VARCHAR(100) DEFAULT NULL COMMENT '客户名称',
  `product_id` BIGINT NOT NULL COMMENT '商品ID',
  `product_name` VARCHAR(100) DEFAULT NULL COMMENT '商品名称',
  `sku` VARCHAR(20) DEFAULT NULL COMMENT 'SKU编码',
  `quantity` INT NOT NULL COMMENT '出库数量',
  `unit_price` DECIMAL(10,2) DEFAULT NULL COMMENT '单价',
  `amount` DECIMAL(12,2) DEFAULT NULL COMMENT '金额',
  `warehouse_id` BIGINT DEFAULT NULL COMMENT '出库仓库ID',
  `warehouse_name` VARCHAR(100) DEFAULT NULL COMMENT '出库仓库名称',
  `operator_id` BIGINT DEFAULT NULL COMMENT '操作人ID',
  `operator_name` VARCHAR(50) DEFAULT NULL COMMENT '操作人姓名',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  `outbound_type` VARCHAR(50) DEFAULT NULL COMMENT '出库类型',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '出库时间',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_no` (`order_no`),
  KEY `idx_sales_order_id` (`sales_order_id`),
  KEY `idx_customer_id` (`customer_id`),
  KEY `idx_product_id` (`product_id`),
  KEY `idx_warehouse_id` (`warehouse_id`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='出库记录表';

DROP TABLE IF EXISTS `aftersales_order`;
CREATE TABLE `aftersales_order` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '售后工单主键ID',
  `order_no` VARCHAR(20) NOT NULL COMMENT '工单编号',
  `sales_order_id` BIGINT NOT NULL COMMENT '销售订单ID',
  `sales_order_no` VARCHAR(20) DEFAULT NULL COMMENT '销售订单编号',
  `customer_id` BIGINT DEFAULT NULL COMMENT '客户ID',
  `customer_name` VARCHAR(100) DEFAULT NULL COMMENT '客户名称',
  `type` VARCHAR(50) NOT NULL COMMENT '问题类型',
  `content` VARCHAR(500) NOT NULL COMMENT '问题描述',
  `expect_handle` VARCHAR(50) DEFAULT NULL COMMENT '期望处理方式',
  `status` VARCHAR(20) DEFAULT NULL COMMENT '状态',
  `handler_id` BIGINT DEFAULT NULL COMMENT '处理人ID',
  `handler_name` VARCHAR(50) DEFAULT NULL COMMENT '处理人姓名',
  `handle_result` VARCHAR(500) DEFAULT NULL COMMENT '处理结果',
  `handle_time` DATETIME DEFAULT NULL COMMENT '处理时间',
  `refund_amount` DECIMAL(12,2) DEFAULT NULL COMMENT '退款金额',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_no` (`order_no`),
  KEY `idx_sales_order_id` (`sales_order_id`),
  KEY `idx_customer_id` (`customer_id`),
  KEY `idx_type` (`type`),
  KEY `idx_status` (`status`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='售后工单表';

-- =====================================================
-- 7. 初始化数据（演示）
-- =====================================================

INSERT INTO `sys_role` (`id`, `name`, `code`, `description`, `status`) VALUES
(1, '超级管理员', 'SUPER_ADMIN', '拥有系统全部权限', 1),
(2, '仓管主管', 'WAREHOUSE_MANAGER', '负责库存管理和调拨审批', 1),
(3, '销售专员', 'SALES_STAFF', '负责销售订单和客户管理', 1),
(4, '采购专员', 'PURCHASE_STAFF', '负责采购订单和供应商管理', 1),
(5, '财务专员', 'FINANCE_STAFF', '负责财务报表和付款管理', 1);

-- 权限与菜单、ApplicationPermissionRegistry 中 code 对齐；详见 docs/database/permission_init.sql
INSERT INTO `sys_permission` (`id`, `name`, `code`, `type`, `parent_id`, `path`, `icon`, `sort`, `status`) VALUES
(1, '仪表盘', 'dashboard', 1, 0, '/dashboard', 'Odometer', 10, 1),
(2, '商品管理', 'products', 1, 0, '/products', 'Goods', 20, 1),
(3, '采购管理', 'purchase', 1, 0, '/purchase', 'ShoppingCart', 30, 1),
(4, '销售管理', 'sales', 1, 0, '/sales', 'Money', 40, 1),
(5, '库存管理', 'inventory', 1, 0, '/inventory', 'House', 50, 1),
(6, '报表分析', 'reports', 1, 0, '/reports', 'DataAnalysis', 60, 1),
(7, '系统设置', 'settings', 1, 0, '/settings', 'Setting', 70, 1),
(8, '商品查看', 'product:view', 2, 2, NULL, NULL, 1, 1),
(9, '商品添加', 'product:add', 2, 2, NULL, NULL, 2, 1),
(10, '商品编辑', 'product:edit', 2, 2, NULL, NULL, 3, 1),
(11, '商品删除', 'product:delete', 2, 2, NULL, NULL, 4, 1),
(12, '采购订单查看', 'purchase:view', 2, 3, NULL, NULL, 1, 1),
(13, '采购订单创建', 'purchase:add', 2, 3, NULL, NULL, 2, 1),
(14, '采购订单编辑', 'purchase:edit', 2, 3, NULL, NULL, 3, 1),
(15, '入库确认', 'purchase:inbound', 2, 3, NULL, NULL, 4, 1),
(16, '销售订单查看', 'sales:view', 2, 4, NULL, NULL, 1, 1),
(17, '销售订单创建', 'sales:add', 2, 4, NULL, NULL, 2, 1),
(18, '发货确认', 'sales:ship', 2, 4, NULL, NULL, 3, 1),
(19, '库存查看', 'inventory:view', 2, 5, NULL, NULL, 1, 1),
(35, '出入库记录', 'inventory:records', 2, 5, NULL, NULL, 2, 1),
(28, '仓库管理', 'inventory:warehouse', 2, 5, NULL, NULL, 3, 1),
(20, '库存调拨', 'inventory:transfer', 2, 5, NULL, NULL, 4, 1),
(29, '入库操作', 'inventory:inbound', 2, 5, NULL, NULL, 5, 1),
(30, '出库操作', 'inventory:outbound', 2, 5, NULL, NULL, 6, 1),
(31, '库存调整', 'inventory:adjust', 2, 5, NULL, NULL, 7, 1),
(21, '报表查看', 'reports:view', 2, 6, NULL, NULL, 1, 1),
(22, '报表导出', 'reports:export', 2, 6, NULL, NULL, 2, 1),
(23, '用户管理', 'settings:user', 2, 7, NULL, NULL, 1, 1),
(24, '角色管理', 'settings:role', 2, 7, NULL, NULL, 2, 1),
(25, '系统配置', 'settings:config', 2, 7, NULL, NULL, 3, 1),
(26, '售后工单', 'sales:aftersales', 2, 4, NULL, NULL, 4, 1),
(27, '客户管理', 'sales:customer', 2, 4, NULL, NULL, 5, 1),
(32, '确认付款', 'sales:payment', 2, 4, NULL, NULL, 6, 1),
(33, '确认收货', 'sales:receive', 2, 4, NULL, NULL, 7, 1),
(34, '供应商管理', 'purchase:supplier', 2, 3, NULL, NULL, 5, 1);

INSERT INTO `sys_role_permission` (`role_id`, `permission_id`) VALUES
(1, 1), (1, 2), (1, 3), (1, 4), (1, 5), (1, 6), (1, 7),
(1, 8), (1, 9), (1, 10), (1, 11), (1, 12), (1, 13), (1, 14), (1, 15),
(1, 16), (1, 17), (1, 18), (1, 19), (1, 20), (1, 21), (1, 22), (1, 23), (1, 24), (1, 25),
(1, 26), (1, 27), (1, 28), (1, 29), (1, 30), (1, 31), (1, 32), (1, 33), (1, 34), (1, 35),
(2, 1), (2, 5), (2, 19), (2, 35), (2, 28), (2, 20), (2, 29), (2, 30), (2, 31),
(3, 1), (3, 2), (3, 4), (3, 8), (3, 16), (3, 17), (3, 18), (3, 26), (3, 27), (3, 32), (3, 33),
(4, 1), (4, 2), (4, 3), (4, 8), (4, 9), (4, 10), (4, 11), (4, 12), (4, 13), (4, 14), (4, 15), (4, 34),
(5, 1), (5, 6), (5, 21), (5, 22);

-- 演示用户明文密码均为 admin123（BCrypt，与后端 BCryptPasswordEncoder 兼容）
INSERT INTO `sys_user` (`id`, `username`, `password`, `name`, `email`, `phone`, `role_id`, `status`) VALUES
(1, 'admin', '$2b$10$93Ia0x4QRohMJGPusnZb6OOcO/VC4BNE6a6tR52TnvE6C61ZwoJnG', '张明远', 'admin@inventory.com', '138****8888', 1, 1),
(2, 'wangjg', '$2b$10$93Ia0x4QRohMJGPusnZb6OOcO/VC4BNE6a6tR52TnvE6C61ZwoJnG', '王建国', 'wangjg@inventory.com', '139****6666', 2, 1),
(3, 'liming', '$2b$10$93Ia0x4QRohMJGPusnZb6OOcO/VC4BNE6a6tR52TnvE6C61ZwoJnG', '李明', 'liming@inventory.com', '136****3333', 3, 1),
(4, 'zhangh', '$2b$10$93Ia0x4QRohMJGPusnZb6OOcO/VC4BNE6a6tR52TnvE6C61ZwoJnG', '张华', 'zhangh@inventory.com', '135****9999', 4, 1),
(5, 'chenli', '$2b$10$93Ia0x4QRohMJGPusnZb6OOcO/VC4BNE6a6tR52TnvE6C61ZwoJnG', '陈丽', 'chenli@inventory.com', '158****7777', 5, 1);

INSERT INTO `sys_category` (`id`, `name`, `code`, `parent_id`, `sort`, `status`) VALUES
(1, '电子产品', 'ELECTRONICS', 0, 1, 1),
(2, '配件', 'ACCESSORIES', 0, 2, 1),
(3, '手表', 'WATCH', 0, 3, 1),
(4, '平板', 'TABLET', 0, 4, 1),
(5, '服装服饰', 'CLOTHING', 0, 5, 1),
(6, '食品饮料', 'FOOD', 0, 6, 1),
(7, '家居用品', 'HOME', 0, 7, 1);

INSERT INTO `product` (`id`, `code`, `name`, `brand`, `spec`, `category_id`, `category_name`, `cost_price`, `sale_price`, `status`, `description`, `stock`, `safe_stock`) VALUES
(1, 'P001', 'iPhone 15 Pro Max', 'Apple 苹果', '256G 钛金色', 1, '电子产品', 8500.00, 9899.00, '在售', 'Apple iPhone 15 Pro Max，A17 Pro芯片，256GB存储', 200, 50),
(2, 'P002', 'MacBook Pro 14寸', 'Apple 苹果', 'M3 Pro芯片', 1, '电子产品', 16800.00, 18999.00, '在售', 'MacBook Pro 14英寸，M3 Pro芯片，高性能笔记本', 80, 30),
(3, 'P003', 'AirPods Pro 2代', 'Apple 苹果', 'USB-C充电', 2, '配件', 1600.00, 1899.00, '在售', 'AirPods Pro 第二代，主动降噪，USB-C充电盒', 500, 100),
(4, 'P004', 'Apple Watch S9', 'Apple 苹果', '45mm 星光色', 3, '手表', 2800.00, 3299.00, '在售', 'Apple Watch Series 9，45mm星光色铝金属表壳', 50, 30),
(5, 'P005', 'iPad Pro 12.9寸', 'Apple 苹果', 'M2 芯片 256G', 4, '平板', 8500.00, 9299.00, '在售', 'iPad Pro 12.9英寸，M2芯片，256GB存储', 120, 40);

INSERT INTO `supplier` (`id`, `name`, `address`, `phone`, `email`, `contact`, `delivery_days`, `status`) VALUES
(1, '深圳华强电子', '广东省深圳市福田区', '0755-83012345', 'contact@hqelectronic.com', '李经理', 5, 1),
(2, '杭州阿里供应链', '浙江省杭州市余杭区', '0571-88881234', 'order@alisupply.cn', '王总监', 3, 1),
(3, '广州中大配件城', '广东省广州市海珠区', '020-88885678', 'info@zdaccessory.com', '张主管', 7, 1),
(4, '上海自贸区仓储', '上海市浦东新区外高桥', '021-58881234', 'sales@shftz.com', '陈经理', 4, 1),
(5, '北京中关村电子', '北京市海淀区', '010-88881234', 'info@zgc.com', '赵主管', 6, 1);

INSERT INTO `supplier_industry_link` (`supplier_id`, `industry_id`) VALUES
(1, 1), (1, 3),
(2, 2), (2, 4),
(3, 2), (3, 3),
(4, 4), (4, 5),
(5, 1), (5, 5);

INSERT INTO `customer` (`id`, `name`, `type`, `address`, `phone`, `email`, `contact`, `vip_level`, `total_orders`, `total_amount`, `status`) VALUES
(1, '李明科技有限公司', '企业', '深圳市南山区科技园南路88号', '138****8888', 'liming@tech.com', '李明', 'VIP', 86, 328500.00, 1),
(2, '王芳工作室', '企业', '杭州市西湖区文三路123号', '139****6666', 'wangfang@design.com', '王芳', 'VIP', 52, 186200.00, 1),
(3, '陈思思', '个人', '上海市浦东新区世纪大道100号', '136****3333', 'chensisi@personal.com', '陈思思', '普通', 12, 28500.00, 1),
(4, '张伟建筑事务所', '企业', '北京市朝阳区建国路88号', '135****9999', 'zhangwei@arch.com', '张伟', 'VIP', 68, 256800.00, 1),
(5, '刘静个人', '个人', '广州市天河区珠江新城', '158****7777', 'liujing@personal.com', '刘静', '普通', 8, 15200.00, 1);

INSERT INTO `warehouse` (`id`, `name`, `address`, `manager_id`, `manager_name`, `total_categories`, `total_stock`, `total_value`, `capacity`, `capacity_used`, `status`) VALUES
(1, '深圳仓库A区', '广东省深圳市宝安区西乡街道88号', 2, '王建国', 1256, 5680, 856000.00, 1000, 78.00, 1),
(2, '上海仓库B区', '上海市浦东新区外高桥保税区', 2, '王建国', 892, 3420, 428000.00, 800, 65.00, 1),
(3, '广州仓库C区', '广东省广州市白云区机场路128号', 2, '王建国', 420, 1856, 186000.00, 600, 45.00, 1);

INSERT INTO `inventory` (`id`, `sku`, `product_id`, `product_name`, `spec`, `category`, `warehouse_id`, `warehouse_name`, `location`, `stock`, `safe_stock`, `cost_price`, `stock_value`, `status`, `stagnant_days`) VALUES
(1, 'SKU001', 1, 'iPhone 15 Pro Max', '256G 钛金色', '手机', 1, '深圳仓库A区', '01', 128, 50, 8500.00, 1088000.00, 'normal', 90),
(2, 'SKU002', 2, 'MacBook Pro 14寸', 'M3 Pro 芯片', '电脑', 1, '深圳仓库A区', '02', 45, 30, 16800.00, 756000.00, 'warning', 90),
(3, 'SKU003', 3, 'AirPods Pro 2代', 'USB-C 充电盒', '配件', 2, '上海仓库B区', '01', 256, 100, 1600.00, 409600.00, 'normal', 90),
(4, 'SKU004', 4, 'Apple Watch S9', '45mm 星光色', '手表', 3, '广州仓库C区', '01', 12, 30, 2800.00, 39600.00, 'critical', 90),
(5, 'SKU005', 5, 'iPad Pro 12.9寸', 'M2 芯片 256G', '平板', 1, '深圳仓库A区', '03', 78, 40, 8500.00, 725000.00, 'normal', 90);

INSERT INTO `purchase_order` (`id`, `order_no`, `supplier_id`, `supplier_name`, `product_id`, `product_name`, `sku`, `unit_price`, `total_quantity`, `pending_quantity`, `inbound_quantity`, `amount`, `inbound_status`, `pay_status`, `expect_date`, `operator_id`, `operator_name`, `create_time`) VALUES
(1, 'PO2026041101', 1, '深圳华强电子', 1, 'iPhone 15 屏幕总成', 'P001', 1400.00, 200, 80, 120, 280000.00, '部分入库', '待付款', '2026-04-16', 4, '张华', '2026-04-11 09:30:00'),
(2, 'PO2026041102', 2, '杭州阿里供应链', 2, 'MacBook Air 笔记本', 'P002', 8500.00, 50, 0, 50, 425000.00, '已完成', '已付款', '2026-04-13', 4, '张华', '2026-04-10 14:20:00'),
(3, 'PO2026041103', 3, '广州中大配件城', 3, 'AirPods 充电盒', 'P003', 250.00, 500, 500, 0, 125000.00, '待入库', '待付款', '2026-04-17', 4, '张华', '2026-04-10 11:15:00'),
(4, 'PO2026041104', 4, '上海自贸区仓储', 4, 'Apple Watch 表带', 'P004', 120.00, 300, 0, 300, 36000.00, '已完成', '已付款', '2026-04-12', 4, '张华', '2026-04-09 16:45:00'),
(5, 'PO2026041105', 5, '北京中关村电子', 5, 'iPad Pro 保护套', 'P005', 150.00, 150, 0, 0, 22500.00, '已取消', '已退款', '2026-04-14', 4, '张华', '2026-04-08 10:30:00');

INSERT INTO `inbound_record` (`id`, `order_no`, `purchase_order_id`, `purchase_order_no`, `supplier_id`, `supplier_name`, `product_id`, `product_name`, `sku`, `quantity`, `unit_price`, `warehouse_id`, `warehouse_name`, `operator_id`, `operator_name`, `create_time`) VALUES
(1, 'IN2026041101', 2, 'PO2026041102', 2, '杭州阿里供应链', 2, 'MacBook Air 笔记本', 'P002', 30, 8500.00, 1, '深圳仓库A区', 2, '王建国', '2026-04-11 10:30:00'),
(2, 'IN2026041102', 4, 'PO2026041104', 4, '上海自贸区仓储', 4, 'Apple Watch 表带', 'P004', 300, 120.00, 2, '上海仓库B区', 3, '李明', '2026-04-10 14:20:00'),
(3, 'IN2026041001', 1, 'PO2026041101', 1, '深圳华强电子', 1, 'iPhone 15 屏幕总成', 'P001', 120, 1400.00, 1, '深圳仓库A区', 4, '张华', '2026-04-10 09:15:00');

INSERT INTO `sales_order` (`id`, `order_no`, `customer_id`, `customer_name`, `customer_type`, `product_id`, `product_name`, `sku`, `unit_price`, `quantity`, `shipped_quantity`, `pending_quantity`, `amount`, `status`, `pay_status`, `pay_method`, `receiver_name`, `receiver_phone`, `receiver_address`, `operator_id`, `operator_name`, `create_time`) VALUES
(1, 'SO2026041101', 1, '李明科技有限公司', '企业', 1, 'iPhone 15 Pro Max 256G', 'P001', 9899.00, 5, 5, 0, 49495.00, '已完成', '已付款', '银行转账', '李明', '138****8888', '深圳市南山区科技园南路88号', 3, '李明', '2026-04-11 14:23:00'),
(2, 'SO2026041102', 2, '王芳工作室', '企业', 2, 'MacBook Pro 14寸 M3', 'P002', 18999.00, 2, 1, 1, 37998.00, '处理中', '已付款', '支付宝', '王芳', '139****6666', '杭州市西湖区文三路123号', 3, '李明', '2026-04-11 13:45:00'),
(3, 'SO2026041103', 3, '陈思思', '个人', 3, 'AirPods Pro 2代', 'P003', 1899.00, 3, 3, 0, 5697.00, '已完成', '已付款', '微信支付', '陈思思', '136****3333', '上海市浦东新区世纪大道100号', 3, '李明', '2026-04-11 11:30:00'),
(4, 'SO2026041104', 4, '张伟建筑事务所', '企业', 4, 'Apple Watch S9 45mm', 'P004', 3299.00, 8, 0, 8, 26392.00, '已取消', '已退款', '银行转账', '张伟', '135****9999', '北京市朝阳区建国路88号', 3, '李明', '2026-04-11 10:15:00'),
(5, 'SO2026041105', 5, '刘静个人', '个人', 5, 'iPad Pro 12.9寸', 'P005', 9299.00, 1, 0, 1, 9299.00, '待发货', '待付款', '现金', '刘静', '158****7777', '广州市天河区珠江新城', 3, '李明', '2026-04-11 09:20:00');

INSERT INTO `outbound_record` (`id`, `order_no`, `sales_order_id`, `sales_order_no`, `customer_id`, `customer_name`, `product_id`, `product_name`, `sku`, `quantity`, `unit_price`, `amount`, `warehouse_id`, `warehouse_name`, `operator_id`, `operator_name`, `outbound_type`, `create_time`) VALUES
(1, 'OB2026041101', 3, 'SO2026041103', 3, '陈思思', 3, 'AirPods Pro 2代', 'P003', 3, 1899.00, 5697.00, 2, '上海仓库B区', 3, '李明', '销售出库', '2026-04-11 11:35:00');

INSERT INTO `aftersales_order` (`id`, `order_no`, `sales_order_id`, `sales_order_no`, `customer_id`, `customer_name`, `type`, `content`, `expect_handle`, `status`, `handler_id`, `handler_name`, `handle_result`, `handle_time`, `create_time`) VALUES
(1, 'AS2026041001', 3, 'SO2026041005', 3, '陈思思', '质量问题', 'AirPods 左耳无法充电', '换货', '已解决', 3, '李明', '已更换新机，客户确认无问题', '2026-04-10 18:00:00', '2026-04-10 15:30:00'),
(2, 'AS2026041002', 2, 'SO2026040918', 2, '王芳工作室', '退换货', 'MacBook 屏幕有亮点，申请换货', '换货', '已解决', 3, '李明', '已安排换货，新机已送达', '2026-04-10 16:00:00', '2026-04-10 11:20:00'),
(3, 'AS2026040905', 1, 'SO2026040802', 1, '李明科技有限公司', '物流问题', 'iPhone 15 包装破损', '补偿', '已解决', 3, '李明', '已补偿100元运费差价', '2026-04-09 20:00:00', '2026-04-09 16:45:00');

INSERT INTO `inventory_transfer` (`id`, `order_no`, `product_id`, `product_name`, `sku`, `from_warehouse_id`, `from_warehouse_name`, `to_warehouse_id`, `to_warehouse_name`, `quantity`, `status`, `operator_id`, `operator_name`, `create_time`) VALUES
(1, 'TR2026041101', 2, 'MacBook Pro 14寸', 'SKU002', 1, '深圳仓库A区', 2, '上海仓库B区', 20, '调拨中', 2, '王建国', '2026-04-11 10:30:00'),
(2, 'TR2026041102', 3, 'AirPods Pro 2代', 'SKU003', 2, '上海仓库B区', 3, '广州仓库C区', 100, '已完成', 3, '李明', '2026-04-10 16:45:00'),
(3, 'TR2026041103', 5, 'iPad Pro 保护套', 'SKU005', 1, '深圳仓库A区', 3, '广州仓库C区', 50, '调拨中', 4, '张华', '2026-04-11 09:15:00');

INSERT INTO `inventory_warning` (`id`, `type`, `sku`, `product_name`, `content`, `warehouse_id`, `warehouse_name`, `current_stock`, `safe_stock`, `status`, `create_time`) VALUES
(1, 'critical', 'SKU004', 'Apple Watch S9', '当前库存 12 件，连续 3 天低于安全库存 30 件', 3, '广州仓库C区', 12, 30, 0, '2026-04-10 09:00:00'),
(2, 'warning', 'SKU002', 'MacBook Pro 14寸', '当前库存 45 件，低于安全库存 30 件，请及时补货', 1, '深圳仓库A区', 45, 30, 0, '2026-04-09 14:30:00'),
(3, 'warning', 'SKU003', 'AirPods Pro 充电盒', '该批次商品将于 30 天后过期，请优先出库', 2, '上海仓库B区', NULL, NULL, 0, '2026-04-08 10:00:00'),
(4, 'info', 'SKU001', 'iPhone 15 屏幕总成', '该商品库存超过 90 天未动销，建议促销处理', 1, '深圳仓库A区', NULL, NULL, 0, '2026-04-07 08:00:00');

INSERT INTO `sys_operation_log` (`id`, `user_id`, `user_name`, `role_name`, `action`, `content`, `module`, `ip`, `status`, `create_time`) VALUES
(1, 1, '张明远', '超级管理员', '登录', '系统登录', '系统', '192.168.1.100', 1, '2026-04-11 15:30:25'),
(2, 3, '李明', '销售专员', '创建', '新建销售订单 #SO2026041102', '销售管理', '192.168.1.105', 1, '2026-04-11 15:25:10'),
(3, 2, '王建国', '仓管主管', '更新', '确认入库 #IN2026041102', '采购管理', '192.168.1.108', 1, '2026-04-11 15:20:45'),
(4, 1, '张明远', '超级管理员', '配置', '修改系统配置', '系统设置', '192.168.1.100', 1, '2026-04-11 14:30:20'),
(5, 4, '张华', '采购专员', '创建', '新建调拨单 #TR2026041103', '库存管理', '192.168.1.112', 1, '2026-04-11 14:15:33'),
(6, 5, '陈丽', '财务专员', '导出', '导出财务报表 2026-03', '报表分析', '192.168.1.115', 1, '2026-04-11 13:45:18');

-- =====================================================
-- 说明：
-- 1. 分类表名为 sys_category（对应实体 SysCategory），旧版 product_category 已废弃。
-- 2. 后端无 sys_config 实体；订单前缀等若需落库请自行建表或走配置文件。
-- 3. 以图搜图向量存于本地目录（见 application.yml），不写入 MySQL。
-- =====================================================
