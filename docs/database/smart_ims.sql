/*
 Navicat Premium Dump SQL

 Source Server         : localhost_3306
 Source Server Type    : MySQL
 Source Server Version : 80045 (8.0.45)
 Source Host           : localhost:3306
 Source Schema         : smart_ims

 Target Server Type    : MySQL
 Target Server Version : 80045 (8.0.45)
 File Encoding         : 65001

 Date: 15/04/2026 14:55:02
*/

CREATE DATABASE IF NOT EXISTS `smart_ims`
DEFAULT CHARACTER SET utf8mb4
DEFAULT COLLATE utf8mb4_unicode_ci;

USE `smart_ims`;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for aftersales_order
-- ----------------------------
DROP TABLE IF EXISTS `aftersales_order`;
CREATE TABLE `aftersales_order`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '售后工单主键ID',
  `order_no` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '工单编号',
  `sales_order_id` bigint NOT NULL COMMENT '销售订单ID',
  `sales_order_no` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '销售订单编号',
  `customer_id` bigint NULL DEFAULT NULL COMMENT '客户ID',
  `customer_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '客户名称',
  `type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '问题类型',
  `content` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '问题描述',
  `expect_handle` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '期望处理方式',
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '状态',
  `handler_id` bigint NULL DEFAULT NULL COMMENT '处理人ID',
  `handler_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '处理人姓名',
  `handle_result` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '处理结果',
  `handle_time` datetime NULL DEFAULT NULL COMMENT '处理时间',
  `refund_amount` decimal(12, 2) NULL DEFAULT NULL COMMENT '退款金额',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '备注',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_order_no`(`order_no` ASC) USING BTREE,
  INDEX `idx_sales_order_id`(`sales_order_id` ASC) USING BTREE,
  INDEX `idx_customer_id`(`customer_id` ASC) USING BTREE,
  INDEX `idx_type`(`type` ASC) USING BTREE,
  INDEX `idx_status`(`status` ASC) USING BTREE,
  INDEX `idx_create_time`(`create_time` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 5 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '售后工单表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of aftersales_order
-- ----------------------------
INSERT INTO `aftersales_order` VALUES (4, 'AS2604140001', 14, 'SO2604140026', 5, '刘静个人', '质量问题', '5', '退货退款', '待处理', NULL, NULL, NULL, NULL, NULL, NULL, '2026-04-14 15:57:19', '2026-04-14 15:57:19', 0);

-- ----------------------------
-- Table structure for customer
-- ----------------------------
DROP TABLE IF EXISTS `customer`;
CREATE TABLE `customer`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '客户主键ID',
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '客户名称',
  `code` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '客户编码',
  `type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '客户类型：企业/个人',
  `address` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '地址',
  `phone` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '联系电话',
  `email` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '邮箱',
  `contact` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '联系人',
  `vip_level` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT 'VIP等级',
  `credit_amount` decimal(12, 2) NULL DEFAULT 0.00 COMMENT '信用额度',
  `total_orders` int NULL DEFAULT 0 COMMENT '累计订单数',
  `total_amount` decimal(12, 2) NULL DEFAULT 0.00 COMMENT '累计消费金额',
  `last_order_time` datetime NULL DEFAULT NULL COMMENT '最近有效销售单下单时间（不含已取消）',
  `status` tinyint NULL DEFAULT 1 COMMENT '状态：1-正常，0-禁用',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '备注',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_type`(`type` ASC) USING BTREE,
  INDEX `idx_vip_level`(`vip_level` ASC) USING BTREE,
  INDEX `idx_status`(`status` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 11 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '客户表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of customer
-- ----------------------------
INSERT INTO `customer` VALUES (1, '李明科技有限公司', NULL, '企业', '深圳市南山区科技园南路88号', '13866668888', 'liming@tech.com', '李明', '普通', 0.00, 1, 700.00, '2026-04-14 10:14:29', 1, NULL, '2026-04-13 16:10:50', '2026-04-15 09:58:19', 0);
INSERT INTO `customer` VALUES (2, '王芳工作室', NULL, '企业', '杭州市西湖区文三路123号', '13944446666', 'wangfang@design.com', '王芳', '普通', 0.00, 1, 1600.00, '2026-04-14 10:14:46', 1, NULL, '2026-04-13 16:10:50', '2026-04-15 09:58:19', 0);
INSERT INTO `customer` VALUES (3, '陈思思', NULL, '个人', '上海市浦东新区世纪大道100号', '13622223333', 'chensisi@personal.com', '陈思思', '普通', 0.00, 3, 466.00, '2026-04-14 10:15:37', 1, NULL, '2026-04-13 16:10:50', '2026-04-15 09:58:19', 0);
INSERT INTO `customer` VALUES (4, '张伟建筑事务所', NULL, '企业', '北京市朝阳区建国路88号', '13511119999', 'zhangwei@arch.com', '张伟', '普通', 0.00, 1, 178.00, '2026-04-14 10:15:01', 1, NULL, '2026-04-13 16:10:50', '2026-04-15 09:58:19', 0);
INSERT INTO `customer` VALUES (5, '刘静个人', NULL, '个人', '广州市天河区珠江新城', '15833337777', 'liujing@personal.com', '刘静', '普通', 0.00, 3, 318.00, '2026-04-14 10:15:59', 1, NULL, '2026-04-13 16:10:50', '2026-04-15 09:58:19', 0);
INSERT INTO `customer` VALUES (7, '南威软件', NULL, '企业', '南威软件2号楼', '17678987987', '', '吴老板', NULL, 0.00, 2, 62000.00, '2026-04-14 18:46:51', 1, '', '2026-04-14 16:30:54', '2026-04-15 09:58:19', 0);
INSERT INTO `customer` VALUES (8, '耐克泉州万达专卖店', NULL, '个人', '泉州鲤城万达', '17867854434', '', '陈老板', NULL, 0.00, 1, 2000.00, '2026-04-14 18:52:05', 1, '', '2026-04-14 18:50:57', '2026-04-15 09:58:19', 0);
INSERT INTO `customer` VALUES (9, '阿迪晋江专卖店', NULL, '个人', '晋江某卖场', '13487689054', '', '李老板', NULL, 0.00, 1, 2800.00, '2026-04-14 19:10:59', 1, '', '2026-04-14 18:51:23', '2026-04-15 09:58:19', 0);

-- ----------------------------
-- Table structure for inbound_record
-- ----------------------------
DROP TABLE IF EXISTS `inbound_record`;
CREATE TABLE `inbound_record`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '入库记录主键ID',
  `order_no` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '入库单号',
  `purchase_order_id` bigint NULL DEFAULT NULL COMMENT '采购订单ID',
  `purchase_order_no` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '采购订单编号',
  `supplier_id` bigint NULL DEFAULT NULL COMMENT '供应商ID',
  `supplier_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '供应商名称',
  `product_id` bigint NOT NULL COMMENT '商品ID',
  `product_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '商品名称',
  `sku` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT 'SKU编码',
  `quantity` int NOT NULL COMMENT '入库数量',
  `unit_price` decimal(10, 2) NULL DEFAULT NULL COMMENT '入库单价',
  `amount` decimal(12, 2) NULL DEFAULT NULL COMMENT '入库金额',
  `warehouse_id` bigint NOT NULL COMMENT '入库仓库ID',
  `warehouse_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '入库仓库名称',
  `batch_no` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '批次号',
  `expire_date` date NULL DEFAULT NULL COMMENT '过期日期',
  `operator_id` bigint NULL DEFAULT NULL COMMENT '操作人ID',
  `operator_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '操作人姓名',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '备注',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '入库时间',
  `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_order_no`(`order_no` ASC) USING BTREE,
  INDEX `idx_purchase_order_id`(`purchase_order_id` ASC) USING BTREE,
  INDEX `idx_supplier_id`(`supplier_id` ASC) USING BTREE,
  INDEX `idx_product_id`(`product_id` ASC) USING BTREE,
  INDEX `idx_warehouse_id`(`warehouse_id` ASC) USING BTREE,
  INDEX `idx_create_time`(`create_time` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 19 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '入库记录表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of inbound_record
-- ----------------------------
INSERT INTO `inbound_record` VALUES (4, 'IN2604130003', 6, 'PO2604130002', 2, '杭州阿里供应链', 6, '儿童纯棉T恤', 'PROD2604130001', 100, 10.00, 1000.00, 1, '深圳仓库A区', NULL, NULL, 1, '张明远', '', '2026-04-13 16:44:33', 0);
INSERT INTO `inbound_record` VALUES (5, 'IN2604140027', 15, 'PO2604140018', 2, '杭州阿里供应链', 7, '卡通运动鞋', 'PROD2604140001', 100, 30.00, 3000.00, 1, '深圳仓库A区', NULL, NULL, NULL, NULL, '', '2026-04-14 10:16:14', 0);
INSERT INTO `inbound_record` VALUES (6, 'IN2604140028', 14, 'PO2604140017', 2, '杭州阿里供应链', 8, '休闲长裤', 'PROD2604140002', 100, 15.00, 1500.00, 1, '深圳仓库A区', NULL, NULL, NULL, NULL, '', '2026-04-14 10:16:30', 0);
INSERT INTO `inbound_record` VALUES (7, 'IN2604140029', 13, 'PO2604140016', 2, '杭州阿里供应链', 9, '网面透气凉鞋', 'PROD2604140003', 50, 9.90, 495.00, 1, '深圳仓库A区', NULL, NULL, NULL, NULL, '', '2026-04-14 10:16:36', 0);
INSERT INTO `inbound_record` VALUES (8, 'IN2604140030', 12, 'PO2604140015', 2, '杭州阿里供应链', 10, '连衣裙', 'PROD2604140004', 50, 16.00, 800.00, 1, '深圳仓库A区', NULL, NULL, NULL, NULL, '', '2026-04-14 10:16:42', 0);
INSERT INTO `inbound_record` VALUES (9, 'IN2604140031', 11, 'PO2604140014', 2, '杭州阿里供应链', 11, '防滑学步鞋', 'PROD2604140005', 100, 11.00, 1100.00, 1, '深圳仓库A区', NULL, NULL, NULL, NULL, '', '2026-04-14 10:16:49', 0);
INSERT INTO `inbound_record` VALUES (10, 'IN2604140032', 10, 'PO2604140013', 2, '杭州阿里供应链', 12, '卫衣套装', 'PROD2604140006', 50, 50.00, 2500.00, 1, '深圳仓库A区', NULL, NULL, NULL, NULL, '', '2026-04-14 10:16:54', 0);
INSERT INTO `inbound_record` VALUES (11, 'IN2604140033', 9, 'PO2604140012', 2, '杭州阿里供应链', 13, '板鞋', 'PROD2604140007', 50, 18.00, 900.00, 1, '深圳仓库A区', NULL, NULL, NULL, NULL, '', '2026-04-14 10:16:59', 0);
INSERT INTO `inbound_record` VALUES (12, 'IN2604140034', 8, 'PO2604140011', 2, '杭州阿里供应链', 14, '儿童短裤', 'PROD2604140008', 200, 16.00, 3200.00, 1, '深圳仓库A区', NULL, NULL, NULL, NULL, '', '2026-04-14 10:17:06', 0);
INSERT INTO `inbound_record` VALUES (13, 'IN2604140035', 7, 'PO2604140010', 2, '杭州阿里供应链', 15, '帆布鞋', 'PROD2604140009', 50, 20.00, 1000.00, 1, '深圳仓库A区', NULL, NULL, NULL, NULL, '', '2026-04-14 10:17:14', 0);
INSERT INTO `inbound_record` VALUES (14, 'IN2604140020', 24, 'PO2604140019', 8, '耐克晋江工厂', 25, '耐克板鞋', 'PROD2604140010', 100, 100.00, 10000.00, 1, '晋江仓库A区', NULL, NULL, 1, '张明远', '', '2026-04-14 14:36:02', 0);
INSERT INTO `inbound_record` VALUES (15, 'IN2604140002', 25, 'PO2604140020', 8, '耐克晋江工厂', 25, '耐克板鞋', 'PROD2604140010', 10, 100.00, 1000.00, 1, '晋江仓库A区', NULL, NULL, 1, '张明远', '', '2026-04-14 16:30:07', 0);
INSERT INTO `inbound_record` VALUES (16, 'IN2604140036', 27, 'PO2604140022', 8, '耐克晋江工厂', 28, '阿迪板鞋', 'PROD2604140013', 1000, 0.00, 0.00, 2, '晋江仓库B区', NULL, NULL, 1, '张明远', '', '2026-04-14 18:46:32', 0);
INSERT INTO `inbound_record` VALUES (17, 'IN2604140037', 26, 'PO2604140021', 8, '耐克晋江工厂', 25, '耐克板鞋', 'PROD2604140010', 100, 100.00, 10000.00, 1, '晋江仓库A区', NULL, NULL, 1, '张明远', '', '2026-04-14 19:24:47', 0);
INSERT INTO `inbound_record` VALUES (18, 'IN2604150001', 28, 'PO2604150001', 3, '广州未东服饰有限公司', 8, '休闲长裤', 'PROD2604140002', 150, 20.00, 3000.00, 3, '晋江仓库C区', NULL, NULL, 1, '张明远', '', '2026-04-15 10:28:13', 0);

-- ----------------------------
-- Table structure for inventory
-- ----------------------------
DROP TABLE IF EXISTS `inventory`;
CREATE TABLE `inventory`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '库存主键ID',
  `sku` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'SKU编码',
  `product_id` bigint NOT NULL COMMENT '商品ID',
  `product_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '商品名称',
  `spec` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '规格',
  `category` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '分类',
  `warehouse_id` bigint NOT NULL COMMENT '仓库ID',
  `warehouse_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '仓库名称',
  `location` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '库位',
  `stock` int NOT NULL DEFAULT 0 COMMENT '当前库存',
  `safe_stock` int NULL DEFAULT 30 COMMENT '安全库存阈值',
  `cost_price` decimal(10, 2) NULL DEFAULT 0.00 COMMENT '成本价',
  `stock_value` decimal(12, 2) NULL DEFAULT 0.00 COMMENT '库存价值',
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '库存状态：normal/warning/critical（与后端一致）',
  `stagnant_days` int NULL DEFAULT NULL COMMENT '呆滞预警天数',
  `last_inbound_time` datetime NULL DEFAULT NULL COMMENT '最后入库时间',
  `last_outbound_time` datetime NULL DEFAULT NULL COMMENT '最后出库时间',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_sku_warehouse`(`sku` ASC, `warehouse_id` ASC) USING BTREE,
  INDEX `idx_product_id`(`product_id` ASC) USING BTREE,
  INDEX `idx_warehouse_id`(`warehouse_id` ASC) USING BTREE,
  INDEX `idx_status`(`status` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 23 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '库存表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of inventory
-- ----------------------------
INSERT INTO `inventory` VALUES (6, 'PROD2604130001', 6, '儿童纯棉T恤', '大童款（8-12 岁）', '童装', 1, '深圳仓库A区', NULL, 96, 10, 10.00, 960.00, 'normal', NULL, '2026-04-13 16:44:33', '2026-04-14 10:27:19', '2026-04-13 16:44:33', '2026-04-13 16:44:33', 0);
INSERT INTO `inventory` VALUES (7, 'PROD2604140001', 7, '卡通运动鞋', '小童款（1-3 岁）', '童鞋', 1, '深圳仓库A区', NULL, 100, 10, 30.00, 3000.00, 'normal', NULL, '2026-04-14 10:16:14', NULL, '2026-04-14 10:16:14', '2026-04-14 10:16:14', 0);
INSERT INTO `inventory` VALUES (8, 'PROD2604140002', 8, '休闲长裤', '大童款（8-12 岁）', '童装', 1, '深圳仓库A区', NULL, 78, 10, 15.00, 1470.00, 'normal', NULL, '2026-04-14 10:16:30', '2026-04-14 10:26:27', '2026-04-14 10:16:30', '2026-04-14 10:16:30', 0);
INSERT INTO `inventory` VALUES (9, 'PROD2604140003', 9, '网面透气凉鞋', '中童款（4-7 岁）', '童鞋', 1, '深圳仓库A区', NULL, 39, 10, 9.90, 485.10, 'normal', NULL, '2026-04-14 10:16:36', '2026-04-14 10:25:25', '2026-04-14 10:16:36', '2026-04-14 10:16:36', 0);
INSERT INTO `inventory` VALUES (10, 'PROD2604140004', 10, '白色连衣裙', '中童款（4-7 岁）', '童装', 1, '深圳仓库A区', NULL, 49, 10, 16.00, 784.00, 'normal', NULL, '2026-04-14 10:16:42', '2026-04-14 10:25:44', '2026-04-14 10:16:42', '2026-04-14 10:16:42', 0);
INSERT INTO `inventory` VALUES (11, 'PROD2604140005', 11, '防滑学步鞋', '婴童款（0-1 岁）', '童鞋', 1, '深圳仓库A区', NULL, 69, 10, 11.00, 1089.00, 'normal', NULL, '2026-04-14 10:16:49', '2026-04-14 10:26:12', '2026-04-14 10:16:49', '2026-04-14 10:16:49', 0);
INSERT INTO `inventory` VALUES (12, 'PROD2604140006', 12, '哪吒休闲套装', '中童款（4-7 岁）', '童装', 1, '深圳仓库A区', NULL, 40, 10, 50.00, 2000.00, 'normal', NULL, '2026-04-14 10:16:54', '2026-04-14 10:26:42', '2026-04-14 10:16:54', '2026-04-14 10:16:54', 0);
INSERT INTO `inventory` VALUES (13, 'PROD2604140007', 13, '休闲板鞋', '小童款（1-3 岁）', '童鞋', 1, '深圳仓库A区', NULL, 40, 10, 18.00, 720.00, 'normal', NULL, '2026-04-14 10:16:59', '2026-04-14 10:26:55', '2026-04-14 10:16:59', '2026-04-14 10:16:59', 0);
INSERT INTO `inventory` VALUES (14, 'PROD2604140008', 14, '鳄鱼短裤', '中童款（4-7 岁）', '童装', 1, '深圳仓库A区', NULL, 200, 10, 16.00, 3200.00, 'normal', NULL, '2026-04-14 10:17:06', NULL, '2026-04-14 10:17:06', '2026-04-14 10:17:06', 0);
INSERT INTO `inventory` VALUES (15, 'PROD2604140009', 15, '帆布鞋', '大童款（8-12 岁）', '童鞋', 1, '深圳仓库A区', NULL, 49, 10, 20.00, 980.00, 'normal', NULL, '2026-04-14 10:17:14', '2026-04-14 10:27:08', '2026-04-14 10:17:14', '2026-04-14 10:17:14', 0);
INSERT INTO `inventory` VALUES (16, 'PROD2604140010', 25, '耐克板鞋', NULL, NULL, 1, '晋江仓库A区', NULL, 200, 10, 0.00, 0.00, 'normal', NULL, '2026-04-14 19:24:47', '2026-04-14 16:44:45', '2026-04-14 14:36:02', '2026-04-14 14:36:02', 0);
INSERT INTO `inventory` VALUES (19, 'SKU000009002', 9, '网面透气凉鞋', '中童款（4-7 岁）', '童鞋', 2, '晋江仓库B区', NULL, 10, 10, 9.90, 99.00, 'normal', NULL, '2026-04-14 17:57:58', NULL, '2026-04-14 17:57:58', '2026-04-14 17:57:58', 0);
INSERT INTO `inventory` VALUES (20, 'SKU000011002', 11, '防滑学步鞋', '婴童款（0-1 岁）', '童鞋', 2, '晋江仓库B区', NULL, 30, 10, 11.00, 330.00, 'normal', NULL, '2026-04-14 17:58:01', NULL, '2026-04-14 17:58:01', '2026-04-14 17:58:01', 0);
INSERT INTO `inventory` VALUES (21, 'SKU000008003', 8, '休闲长裤', '大童款（8-12 岁）', '童装', 3, '晋江仓库C区', NULL, 170, 10, 15.00, 300.00, 'normal', NULL, '2026-04-15 10:28:13', NULL, '2026-04-14 17:58:03', '2026-04-14 17:58:03', 0);
INSERT INTO `inventory` VALUES (22, 'PROD2604140013', 28, '阿迪板鞋', '大童款（8-12 岁）', '童鞋', 2, '晋江仓库B区', NULL, 1000, 10, 0.00, 0.00, 'normal', NULL, '2026-04-14 18:46:32', NULL, '2026-04-14 18:46:32', '2026-04-14 18:46:32', 0);

-- ----------------------------
-- Table structure for inventory_transfer
-- ----------------------------
DROP TABLE IF EXISTS `inventory_transfer`;
CREATE TABLE `inventory_transfer`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '调拨主键ID',
  `order_no` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '调拨单号',
  `product_id` bigint NOT NULL COMMENT '商品ID',
  `product_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '商品名称',
  `sku` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT 'SKU编码',
  `from_warehouse_id` bigint NOT NULL COMMENT '调出仓库ID',
  `from_warehouse_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '调出仓库名称',
  `to_warehouse_id` bigint NOT NULL COMMENT '调入仓库ID',
  `to_warehouse_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '调入仓库名称',
  `quantity` int NOT NULL COMMENT '调拨数量',
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '状态：调拨中/已完成/已取消',
  `operator_id` bigint NULL DEFAULT NULL COMMENT '操作人ID',
  `operator_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '操作人姓名',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '备注',
  `complete_time` datetime NULL DEFAULT NULL COMMENT '完成时间',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_order_no`(`order_no` ASC) USING BTREE,
  INDEX `idx_product_id`(`product_id` ASC) USING BTREE,
  INDEX `idx_status`(`status` ASC) USING BTREE,
  INDEX `idx_create_time`(`create_time` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 10 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '库存调拨记录表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of inventory_transfer
-- ----------------------------
INSERT INTO `inventory_transfer` VALUES (4, 'TR2604140036', 15, '帆布鞋', 'PROD2604140009', 1, '深圳仓库A区', 2, '上海仓库B区', 50, 'pending', NULL, NULL, '', NULL, '2026-04-14 10:18:03', '2026-04-14 10:18:03', 0);
INSERT INTO `inventory_transfer` VALUES (5, 'TR2604140037', 8, '休闲长裤', 'PROD2604140002', 1, '深圳仓库A区', 3, '广州仓库C区', 20, 'completed', NULL, NULL, '', '2026-04-14 17:58:03', '2026-04-14 10:19:04', '2026-04-14 10:19:04', 0);
INSERT INTO `inventory_transfer` VALUES (6, 'TR2604140038', 11, '防滑学步鞋', 'PROD2604140005', 1, '深圳仓库A区', 2, '上海仓库B区', 30, 'completed', NULL, NULL, '', '2026-04-14 17:58:02', '2026-04-14 10:19:14', '2026-04-14 10:19:14', 0);
INSERT INTO `inventory_transfer` VALUES (7, 'TR2604140001', 25, '耐克板鞋', 'PROD2604140010', 1, '晋江仓库A区', 1, '晋江仓库A区', 1, 'completed', NULL, NULL, '', '2026-04-14 17:58:01', '2026-04-14 17:35:15', '2026-04-14 17:35:15', 0);
INSERT INTO `inventory_transfer` VALUES (9, 'TR2604140039', 9, '网面透气凉鞋', 'PROD2604140003', 1, '晋江仓库A区', 2, '晋江仓库B区', 10, 'completed', NULL, NULL, '', '2026-04-14 17:57:59', '2026-04-14 17:54:37', '2026-04-14 17:54:37', 0);

-- ----------------------------
-- Table structure for inventory_warning
-- ----------------------------
DROP TABLE IF EXISTS `inventory_warning`;
CREATE TABLE `inventory_warning`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '预警主键ID',
  `type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '预警类型：critical/warning/info',
  `sku` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT 'SKU编码',
  `product_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '商品名称',
  `content` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '预警内容',
  `warehouse_id` bigint NULL DEFAULT NULL COMMENT '仓库ID',
  `warehouse_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '仓库名称',
  `current_stock` int NULL DEFAULT NULL COMMENT '当前库存',
  `safe_stock` int NULL DEFAULT NULL COMMENT '安全库存',
  `expire_date` datetime NULL DEFAULT NULL COMMENT '过期时间（实体为 LocalDateTime）',
  `stock_age` int NULL DEFAULT NULL COMMENT '库龄（天）',
  `status` tinyint NULL DEFAULT 0 COMMENT '处理状态：0-待处理，1-已处理',
  `handle_time` datetime NULL DEFAULT NULL COMMENT '处理时间',
  `handler_id` bigint NULL DEFAULT NULL COMMENT '处理人ID',
  `handler_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '处理人姓名',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_type`(`type` ASC) USING BTREE,
  INDEX `idx_status`(`status` ASC) USING BTREE,
  INDEX `idx_sku`(`sku` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 5 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '库存预警表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of inventory_warning
-- ----------------------------

-- ----------------------------
-- Table structure for outbound_record
-- ----------------------------
DROP TABLE IF EXISTS `outbound_record`;
CREATE TABLE `outbound_record`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '出库记录主键ID',
  `order_no` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '出库单号',
  `sales_order_id` bigint NULL DEFAULT NULL COMMENT '销售订单ID',
  `sales_order_no` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '销售订单编号',
  `customer_id` bigint NULL DEFAULT NULL COMMENT '客户ID',
  `customer_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '客户名称',
  `product_id` bigint NOT NULL COMMENT '商品ID',
  `product_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '商品名称',
  `sku` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT 'SKU编码',
  `quantity` int NOT NULL COMMENT '出库数量',
  `unit_price` decimal(10, 2) NULL DEFAULT NULL COMMENT '单价',
  `amount` decimal(12, 2) NULL DEFAULT NULL COMMENT '金额',
  `warehouse_id` bigint NULL DEFAULT NULL COMMENT '出库仓库ID',
  `warehouse_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '出库仓库名称',
  `operator_id` bigint NULL DEFAULT NULL COMMENT '操作人ID',
  `operator_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '操作人姓名',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '备注',
  `outbound_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '出库类型',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '出库时间',
  `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_order_no`(`order_no` ASC) USING BTREE,
  INDEX `idx_sales_order_id`(`sales_order_id` ASC) USING BTREE,
  INDEX `idx_customer_id`(`customer_id` ASC) USING BTREE,
  INDEX `idx_product_id`(`product_id` ASC) USING BTREE,
  INDEX `idx_warehouse_id`(`warehouse_id` ASC) USING BTREE,
  INDEX `idx_create_time`(`create_time` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '出库记录表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of outbound_record
-- ----------------------------

-- ----------------------------
-- Table structure for product
-- ----------------------------
DROP TABLE IF EXISTS `product`;
CREATE TABLE `product`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '商品主键ID',
  `code` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '商品编码',
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '商品名称',
  `brand` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '品牌',
  `spec` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '规格',
  `category_id` bigint NULL DEFAULT NULL COMMENT '分类ID',
  `category_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '分类名称',
  `cost_price` decimal(10, 2) NOT NULL DEFAULT 0.00 COMMENT '成本价',
  `sale_price` decimal(10, 2) NOT NULL DEFAULT 0.00 COMMENT '销售价',
  `status` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '状态：在售/停售',
  `image` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '商品图片：单张 URL 或多张 JSON/逗号分隔（建议至多 10 张）',
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '商品描述',
  `stock` int NULL DEFAULT 0 COMMENT '库存数量（汇总展示用，以 inventory 为准）',
  `safe_stock` int NULL DEFAULT 10 COMMENT '安全库存预警值',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_code`(`code` ASC) USING BTREE,
  INDEX `idx_category_id`(`category_id` ASC) USING BTREE,
  INDEX `idx_status`(`status` ASC) USING BTREE,
  INDEX `idx_name`(`name` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 32 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '商品表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of product
-- ----------------------------
INSERT INTO `product` VALUES (6, 'PROD2604130001', '儿童纯棉T恤', '优衣库', '大童款（8-12 岁）', 8, '童装', 10.00, 100.00, '在售', '/api/uploads/products/b0b225ed0d004fbda991a88c5d862d9a.png', '', 0, 10, '2026-04-13 16:42:38', '2026-04-13 16:42:38', 0);
INSERT INTO `product` VALUES (7, 'PROD2604140001', '卡通运动鞋', '熊猫', '小童款（1-3 岁）', 20, '童鞋', 30.00, 120.00, '在售', '/api/uploads/products/7432d9e34a804d80b85b40d243707c81.png', '', 0, 10, '2026-04-14 10:03:25', '2026-04-14 10:03:25', 0);
INSERT INTO `product` VALUES (8, 'PROD2604140002', '休闲长裤', '美特斯邦威', '大童款（8-12 岁）', 8, '童装', 15.00, 89.00, '在售', '/api/uploads/products/8401e210fd76419dac436eefb420ec0f.png', '', 0, 10, '2026-04-14 10:04:06', '2026-04-14 10:04:06', 0);
INSERT INTO `product` VALUES (9, 'PROD2604140003', '网面透气凉鞋', '耐克', '中童款（4-7 岁）', 20, '童鞋', 9.90, 88.00, '在售', '/api/uploads/products/0f8635dc9d99438cb111bc0326ef23b8.png', '', 0, 10, '2026-04-14 10:05:12', '2026-04-14 10:05:12', 0);
INSERT INTO `product` VALUES (10, 'PROD2604140004', '白色连衣裙', '优衣库', '中童款（4-7 岁）', 8, '童装', 16.00, 150.00, '在售', '/api/uploads/products/3c35085d984943438e852f779778242c.png', '', 0, 10, '2026-04-14 10:06:06', '2026-04-14 10:06:06', 0);
INSERT INTO `product` VALUES (11, 'PROD2604140005', '防滑学步鞋', '亚瑟士', '婴童款（0-1 岁）', 20, '童鞋', 11.00, 66.00, '在售', '/api/uploads/products/1c6f9ec0ca3e4b228fb47e09803ed678.png', '', 0, 10, '2026-04-14 10:07:07', '2026-04-14 10:07:07', 0);
INSERT INTO `product` VALUES (12, 'PROD2604140006', '哪吒休闲套装', '哪吒', '中童款（4-7 岁）', 8, '童装', 50.00, 160.00, '在售', '/api/uploads/products/4ea1a946264a41fba201082bc89bcbc2.png', '', 0, 10, '2026-04-14 10:07:37', '2026-04-14 10:07:37', 0);
INSERT INTO `product` VALUES (13, 'PROD2604140007', '休闲板鞋', '耐克', '小童款（1-3 岁）', 20, '童鞋', 18.00, 70.00, '在售', '/api/uploads/products/ca08586f683847ea84c923dad21f11e9.png', '', 0, 10, '2026-04-14 10:08:14', '2026-04-14 10:08:14', 0);
INSERT INTO `product` VALUES (14, 'PROD2604140008', '鳄鱼短裤', '优衣库', '中童款（4-7 岁）', 8, '童装', 16.00, 89.00, '在售', '/api/uploads/products/c80608cb80c44df1aa9c44a54a9554d4.png', '', 0, 10, '2026-04-14 10:08:47', '2026-04-14 10:08:47', 0);
INSERT INTO `product` VALUES (15, 'PROD2604140009', '帆布鞋', '匡威', '大童款（8-12 岁）', 20, '童鞋', 20.00, 80.00, '在售', '/api/uploads/products/40c427f0186840cd8038834ea162996d.png', '', 0, 10, '2026-04-14 10:09:26', '2026-04-14 10:09:26', 0);
INSERT INTO `product` VALUES (25, 'PROD2604140010', '耐克板鞋', '耐克', '青少年款（13-16 岁）', 20, '童鞋', 100.00, 200.00, '在售', '/api/uploads/products/02c5629c7a844c12a6a62e7667864a11.png', '', 0, 10, '2026-04-14 14:25:27', '2026-04-14 14:25:27', 0);
INSERT INTO `product` VALUES (26, 'PROD2604140011', '1', '', '', 20, '童鞋', 20.00, 90.00, '在售', '/api/uploads/products/3747fb9853714d5c890016fc318c89a3.png', '', 0, 10, '2026-04-14 16:27:53', '2026-04-14 16:28:00', 1);
INSERT INTO `product` VALUES (27, 'PROD2604140012', '1', '', '', 20, '童鞋', 10.00, 110.00, '在售', '/api/uploads/products/66aa79e3e710451c973bbf70490c5954.png', '', 0, 10, '2026-04-14 18:27:48', '2026-04-14 18:27:51', 1);
INSERT INTO `product` VALUES (28, 'PROD2604140013', '阿迪板鞋', '阿迪', '大童款（8-12 岁）', 20, '童鞋', 0.00, 200.00, '在售', '/api/uploads/products/efe441e7109c48fa8b507d82cbda3d87.png', '', 0, 10, '2026-04-14 18:45:39', '2026-04-14 18:45:39', 0);
INSERT INTO `product` VALUES (29, 'PROD2604150001', '1', '1', '1', 20, '童鞋', 1.00, 2.00, '在售', NULL, NULL, 0, 10, '2026-04-15 10:48:40', '2026-04-15 10:48:49', 1);
INSERT INTO `product` VALUES (30, 'PROD2604150002', '2', '2', '2', 20, '童鞋', 2.00, 3.00, '在售', NULL, NULL, 0, 10, '2026-04-15 10:48:40', '2026-04-15 10:48:47', 1);
INSERT INTO `product` VALUES (31, 'P1776222161843', 'Moonstart', 'Moonstart', '大童款（8-12 岁）', 20, '童鞋', 20.00, 90.00, '在售', '[\"/api/uploads/products/fb1d1262c56d4c06a09f58612a782257.png\",\"/api/uploads/products/e2c8a88c5fc944c5aa59b4bbd4d638bd.png\",\"/api/uploads/products/b9f0049fc55c47e19b743cb03326dcd3.png\"]', '', 0, 10, '2026-04-15 11:02:42', '2026-04-15 11:02:42', 0);

-- ----------------------------
-- Table structure for purchase_order
-- ----------------------------
DROP TABLE IF EXISTS `purchase_order`;
CREATE TABLE `purchase_order`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '采购订单主键ID',
  `order_no` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '订单编号',
  `supplier_id` bigint NOT NULL COMMENT '供应商ID',
  `supplier_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '供应商名称',
  `product_id` bigint NOT NULL COMMENT '商品ID',
  `product_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '商品名称',
  `sku` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT 'SKU编码',
  `unit_price` decimal(10, 2) NOT NULL COMMENT '采购单价',
  `total_quantity` int NOT NULL COMMENT '采购总量',
  `pending_quantity` int NULL DEFAULT 0 COMMENT '待入库数量',
  `inbound_quantity` int NULL DEFAULT 0 COMMENT '已入库数量',
  `amount` decimal(12, 2) NOT NULL COMMENT '采购金额',
  `inbound_status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '入库状态',
  `pay_status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '付款状态',
  `pay_method` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '付款方式',
  `expect_date` date NULL DEFAULT NULL COMMENT '期望交货日期',
  `warehouse_id` bigint NULL DEFAULT NULL COMMENT '入库仓库ID',
  `warehouse_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '入库仓库名称',
  `operator_id` bigint NULL DEFAULT NULL COMMENT '操作人ID',
  `operator_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '操作人姓名',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '备注',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_order_no`(`order_no` ASC) USING BTREE,
  INDEX `idx_supplier_id`(`supplier_id` ASC) USING BTREE,
  INDEX `idx_product_id`(`product_id` ASC) USING BTREE,
  INDEX `idx_inbound_status`(`inbound_status` ASC) USING BTREE,
  INDEX `idx_pay_status`(`pay_status` ASC) USING BTREE,
  INDEX `idx_create_time`(`create_time` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 29 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '采购订单表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of purchase_order
-- ----------------------------
INSERT INTO `purchase_order` VALUES (6, 'PO2604130002', 2, '杭州阿里供应链', 6, '儿童纯棉T恤', 'PROD2604130001', 10.00, 100, 0, 100, 1000.00, 'completed', 'unpaid', NULL, NULL, 1, NULL, NULL, NULL, '', '2026-04-13 16:43:31', '2026-04-13 16:43:31', 0);
INSERT INTO `purchase_order` VALUES (7, 'PO2604140010', 2, '杭州阿里供应链', 15, '帆布鞋', 'PROD2604140009', 20.00, 50, 0, 50, 1000.00, 'completed', 'unpaid', NULL, NULL, 1, NULL, NULL, NULL, '', '2026-04-14 10:10:26', '2026-04-14 10:10:26', 0);
INSERT INTO `purchase_order` VALUES (8, 'PO2604140011', 2, '杭州阿里供应链', 14, '儿童短裤', 'PROD2604140008', 16.00, 200, 0, 200, 3200.00, 'completed', 'unpaid', NULL, NULL, 1, NULL, NULL, NULL, '', '2026-04-14 10:10:59', '2026-04-14 10:10:59', 0);
INSERT INTO `purchase_order` VALUES (9, 'PO2604140012', 2, '杭州阿里供应链', 13, '板鞋', 'PROD2604140007', 18.00, 50, 0, 50, 900.00, 'completed', 'unpaid', NULL, NULL, 1, NULL, NULL, NULL, '', '2026-04-14 10:11:14', '2026-04-14 10:11:14', 0);
INSERT INTO `purchase_order` VALUES (10, 'PO2604140013', 2, '杭州阿里供应链', 12, '卫衣套装', 'PROD2604140006', 50.00, 50, 0, 50, 2500.00, 'completed', 'unpaid', NULL, NULL, 1, NULL, NULL, NULL, '', '2026-04-14 10:11:37', '2026-04-14 10:11:37', 0);
INSERT INTO `purchase_order` VALUES (11, 'PO2604140014', 2, '杭州阿里供应链', 11, '防滑学步鞋', 'PROD2604140005', 11.00, 100, 0, 100, 1100.00, 'completed', 'unpaid', NULL, NULL, 1, NULL, NULL, NULL, '', '2026-04-14 10:11:58', '2026-04-14 10:11:58', 0);
INSERT INTO `purchase_order` VALUES (12, 'PO2604140015', 2, '杭州阿里供应链', 10, '连衣裙', 'PROD2604140004', 16.00, 50, 0, 50, 800.00, 'completed', 'unpaid', NULL, NULL, 1, NULL, NULL, NULL, '', '2026-04-14 10:12:10', '2026-04-14 10:12:10', 0);
INSERT INTO `purchase_order` VALUES (13, 'PO2604140016', 2, '杭州阿里供应链', 9, '网面透气凉鞋', 'PROD2604140003', 9.90, 50, 0, 50, 495.00, 'completed', 'unpaid', NULL, NULL, 1, NULL, NULL, NULL, '', '2026-04-14 10:12:25', '2026-04-14 10:12:25', 0);
INSERT INTO `purchase_order` VALUES (14, 'PO2604140017', 2, '杭州阿里供应链', 8, '休闲长裤', 'PROD2604140002', 15.00, 100, 0, 100, 1500.00, 'completed', 'unpaid', NULL, NULL, 1, NULL, NULL, NULL, '', '2026-04-14 10:13:05', '2026-04-14 10:13:05', 0);
INSERT INTO `purchase_order` VALUES (15, 'PO2604140018', 2, '杭州阿里供应链', 7, '卡通运动鞋', 'PROD2604140001', 30.00, 100, 0, 100, 3000.00, 'completed', 'unpaid', NULL, NULL, 1, NULL, NULL, NULL, '', '2026-04-14 10:13:21', '2026-04-14 10:13:21', 0);
INSERT INTO `purchase_order` VALUES (24, 'PO2604140019', 8, '耐克晋江工厂', 25, '耐克板鞋', 'PROD2604140010', 100.00, 100, 0, 100, 10000.00, 'completed', 'unpaid', NULL, NULL, 1, NULL, NULL, NULL, '', '2026-04-14 14:34:13', '2026-04-14 14:34:13', 0);
INSERT INTO `purchase_order` VALUES (25, 'PO2604140020', 8, '耐克晋江工厂', 25, '耐克板鞋', 'PROD2604140010', 100.00, 10, 0, 10, 1000.00, 'completed', 'unpaid', NULL, NULL, 1, NULL, NULL, NULL, '', '2026-04-14 16:29:51', '2026-04-14 16:29:51', 0);
INSERT INTO `purchase_order` VALUES (26, 'PO2604140021', 8, '耐克晋江工厂', 25, '耐克板鞋', 'PROD2604140010', 100.00, 100, 0, 100, 10000.00, 'completed', 'unpaid', NULL, NULL, 1, NULL, NULL, NULL, '', '2026-04-14 18:28:39', '2026-04-14 18:28:39', 0);
INSERT INTO `purchase_order` VALUES (27, 'PO2604140022', 8, '耐克晋江工厂', 28, '阿迪板鞋', 'PROD2604140013', 0.00, 1000, 0, 1000, 0.00, 'completed', 'unpaid', NULL, NULL, 1, NULL, NULL, NULL, '', '2026-04-14 18:46:14', '2026-04-14 18:46:14', 0);
INSERT INTO `purchase_order` VALUES (28, 'PO2604150001', 3, '广州未东服饰有限公司', 8, '休闲长裤', 'PROD2604140002', 20.00, 150, 0, 150, 3000.00, 'completed', 'unpaid', NULL, NULL, 1, NULL, NULL, NULL, '', '2026-04-15 10:25:07', '2026-04-15 10:25:07', 0);

-- ----------------------------
-- Table structure for sales_order
-- ----------------------------
DROP TABLE IF EXISTS `sales_order`;
CREATE TABLE `sales_order`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '销售订单主键ID',
  `order_no` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '订单编号',
  `customer_id` bigint NOT NULL COMMENT '客户ID',
  `customer_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '客户名称',
  `customer_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '客户类型',
  `product_id` bigint NOT NULL COMMENT '商品ID',
  `product_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '商品名称',
  `sku` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT 'SKU编码',
  `unit_price` decimal(10, 2) NOT NULL COMMENT '销售单价',
  `quantity` int NOT NULL COMMENT '销售数量',
  `shipped_quantity` int NULL DEFAULT 0 COMMENT '已发货数量',
  `pending_quantity` int NULL DEFAULT 0 COMMENT '待发货数量',
  `amount` decimal(12, 2) NOT NULL COMMENT '销售金额',
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '订单状态',
  `pay_status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '付款状态',
  `pay_method` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '付款方式',
  `invoice_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '发票类型',
  `invoice_no` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '发票编号',
  `receiver_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '收货人姓名',
  `receiver_phone` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '收货人电话（可含主备号合并）',
  `receiver_address` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '收货地址',
  `logistics_company` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '物流公司',
  `logistics_no` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '物流单号',
  `warehouse_id` bigint NULL DEFAULT NULL COMMENT '发货仓库ID',
  `warehouse_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '发货仓库名称',
  `ship_time` datetime NULL DEFAULT NULL COMMENT '发货时间',
  `pay_time` datetime NULL DEFAULT NULL COMMENT '确认付款时间',
  `complete_time` datetime NULL DEFAULT NULL COMMENT '确认收货/订单完成时间',
  `expect_arrive_date` date NULL DEFAULT NULL COMMENT '预计到达日期',
  `operator_id` bigint NULL DEFAULT NULL COMMENT '操作人ID',
  `operator_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '操作人姓名',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '备注',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_order_no`(`order_no` ASC) USING BTREE,
  INDEX `idx_customer_id`(`customer_id` ASC) USING BTREE,
  INDEX `idx_product_id`(`product_id` ASC) USING BTREE,
  INDEX `idx_status`(`status` ASC) USING BTREE,
  INDEX `idx_pay_status`(`pay_status` ASC) USING BTREE,
  INDEX `idx_create_time`(`create_time` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 20 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '销售订单表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sales_order
-- ----------------------------
INSERT INTO `sales_order` VALUES (6, 'SO2604130001', 3, '陈思思', '个人', 6, '儿童纯棉T恤', 'PROD2604130001', 100.00, 3, 3, 0, 300.00, 'completed', 'paid', '银行转账', NULL, NULL, '张三', '13678987657', '福建省泉州市丰泽区东海街道', '圆通速递', '3198756241357', 1, '深圳仓库A区', '2026-04-14 10:27:19', '2026-04-14 19:47:24', '2026-04-14 19:47:34', '2026-04-15', 1, '张明远', '', '2026-04-13 18:00:45', '2026-04-14 19:52:49', 0);
INSERT INTO `sales_order` VALUES (7, 'SO2604140019', 5, '刘静个人', '个人', 15, '帆布鞋', 'PROD2604140009', 80.00, 1, 1, 0, 80.00, 'completed', 'paid', '银行转账', NULL, NULL, '张三', '13678987657', '福建省泉州市丰泽区东海街道', '中通快递', '5621987345216', 1, '深圳仓库A区', '2026-04-14 10:27:08', '2026-04-14 19:47:24', '2026-04-14 19:47:34', '2026-04-14', NULL, NULL, '', '2026-04-14 10:13:59', '2026-04-14 19:52:50', 0);
INSERT INTO `sales_order` VALUES (8, 'SO2604140020', 1, '李明科技有限公司', '企业', 13, '板鞋', 'PROD2604140007', 70.00, 10, 10, 0, 700.00, 'completed', 'paid', '银行转账', NULL, NULL, '张三', '13678987657', '福建省泉州市丰泽区东海街道', '申通快递', '7892456132789', 1, '深圳仓库A区', '2026-04-14 10:26:55', '2026-04-14 19:47:24', '2026-04-14 19:47:34', '2026-04-15', NULL, NULL, '', '2026-04-14 10:14:29', '2026-04-14 19:52:50', 0);
INSERT INTO `sales_order` VALUES (9, 'SO2604140021', 2, '王芳工作室', '企业', 12, '卫衣套装', 'PROD2604140006', 160.00, 10, 10, 0, 1600.00, 'completed', 'paid', '银行转账', NULL, NULL, '张三', '13678987657', '福建省泉州市丰泽区东海街道', '圆通速递', '2417896532107', 1, '深圳仓库A区', '2026-04-14 10:26:42', '2026-04-14 19:47:24', '2026-04-14 19:47:34', '2026-04-15', NULL, NULL, '', '2026-04-14 10:14:46', '2026-04-14 19:52:51', 0);
INSERT INTO `sales_order` VALUES (10, 'SO2604140022', 4, '张伟建筑事务所', '企业', 8, '休闲长裤', 'PROD2604140002', 89.00, 2, 2, 0, 178.00, 'completed', 'paid', '银行转账', NULL, NULL, '张三', '13678987657', '福建省泉州市丰泽区东海街道', '京东物流', 'JDKA765198322109', 1, '深圳仓库A区', '2026-04-14 10:26:27', '2026-04-14 19:47:24', '2026-04-14 19:47:34', '2026-04-15', NULL, NULL, '', '2026-04-14 10:15:01', '2026-04-14 19:52:52', 0);
INSERT INTO `sales_order` VALUES (11, 'SO2604140023', 3, '陈思思', '个人', 11, '防滑学步鞋', 'PROD2604140005', 66.00, 1, 1, 0, 66.00, 'completed', 'paid', '银行转账', NULL, NULL, '张三', '13678987657', '福建省泉州市丰泽区东海街道', '京东物流', 'JDVF512238769102', 1, '深圳仓库A区', '2026-04-14 10:26:12', '2026-04-14 19:47:24', '2026-04-14 19:47:34', '2026-04-15', NULL, NULL, '', '2026-04-14 10:15:23', '2026-04-14 19:52:52', 0);
INSERT INTO `sales_order` VALUES (12, 'SO2604140024', 3, '陈思思', '个人', 6, '儿童纯棉T恤', 'PROD2604130001', 100.00, 1, 1, 0, 100.00, 'completed', 'paid', '银行转账', NULL, NULL, '张三', '13678987657', '福建省泉州市丰泽区东海街道', '京东物流', 'JDVB956755993215', 1, '深圳仓库A区', '2026-04-14 10:26:01', '2026-04-14 19:47:24', '2026-04-14 19:47:34', '2026-04-15', NULL, NULL, '', '2026-04-14 10:15:37', '2026-04-14 19:52:53', 0);
INSERT INTO `sales_order` VALUES (13, 'SO2604140025', 5, '刘静个人', '个人', 10, '连衣裙', 'PROD2604140004', 150.00, 1, 1, 0, 150.00, 'completed', 'paid', '银行转账', NULL, NULL, '张三', '13678987657', '福建省泉州市丰泽区东海街道', '京东物流', 'JDAZ841548378222', 1, '深圳仓库A区', '2026-04-14 10:25:44', '2026-04-14 19:47:24', '2026-04-14 19:47:34', '2026-04-15', NULL, NULL, '', '2026-04-14 10:15:48', '2026-04-14 19:52:53', 0);
INSERT INTO `sales_order` VALUES (14, 'SO2604140026', 5, '刘静个人', '个人', 9, '网面透气凉鞋', 'PROD2604140003', 88.00, 1, 1, 0, 88.00, 'completed', 'paid', '银行转账', NULL, NULL, '张三', '13678987657', '福建省泉州市丰泽区东海街道', '京东物流', 'JDVE249767736175', 1, '深圳仓库A区', '2026-04-14 10:25:25', '2026-04-14 19:47:24', '2026-04-14 19:47:34', '2026-04-15', NULL, NULL, '', '2026-04-14 10:15:59', '2026-04-14 19:52:54', 0);
INSERT INTO `sales_order` VALUES (15, 'SO2604140003', 7, '南威软件', '个人', 25, '耐克板鞋', 'PROD2604140010', 200.00, 10, 10, 0, 2000.00, 'completed', 'paid', '银行转账', NULL, NULL, '张三', '13678987657', '福建省泉州市丰泽区东海街道', '顺丰速运', '23235252352532', 1, '晋江仓库A区', '2026-04-14 16:44:45', '2026-04-14 19:47:24', '2026-04-14 19:47:34', '2026-04-15', 1, '张明远', '', '2026-04-14 16:31:14', '2026-04-14 19:52:56', 0);
INSERT INTO `sales_order` VALUES (16, 'SO2604140027', 7, '南威软件', '个人', 28, '阿迪板鞋', 'PROD2604140013', 200.00, 300, 0, 300, 60000.00, 'pending', 'unpaid', '银行转账', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 1, '张明远', '', '2026-04-14 18:46:51', '2026-04-14 18:46:51', 0);
INSERT INTO `sales_order` VALUES (18, 'SO2604140029', 8, '耐克泉州万达专卖店', '个人', 25, '耐克板鞋', 'PROD2604140010', 200.00, 10, 0, 10, 2000.00, 'pending', 'paid', '银行转账', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 1, '张明远', '', '2026-04-14 18:52:05', '2026-04-14 18:52:05', 0);
INSERT INTO `sales_order` VALUES (19, 'SO2604140030', 9, '阿迪晋江专卖店', '个人', 13, '休闲板鞋', 'PROD2604140007', 70.00, 40, 0, 40, 2800.00, 'pending', 'unpaid', '银行转账', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 2, '王建国', '', '2026-04-14 19:10:59', '2026-04-14 19:10:59', 0);

-- ----------------------------
-- Table structure for supplier
-- ----------------------------
DROP TABLE IF EXISTS `supplier`;
CREATE TABLE `supplier`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '供应商主键ID',
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '供应商名称',
  `code` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '供应商编码',
  `address` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '地址',
  `phone` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '联系电话',
  `email` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '邮箱',
  `contact` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '联系人',
  `delivery_days` int NULL DEFAULT 5 COMMENT '平均货期（天）',
  `bank_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '开户银行',
  `bank_account` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '银行账号',
  `status` tinyint NULL DEFAULT 1 COMMENT '状态：1-合作中，0-已停用',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '备注',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_name`(`name` ASC) USING BTREE,
  INDEX `idx_status`(`status` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 9 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '供应商表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of supplier
-- ----------------------------
INSERT INTO `supplier` VALUES (1, '深圳华强电子', NULL, '广东省深圳市福田区', '0755-83012345', 'contact@hqelectronic.com', '李经理', 5, NULL, NULL, 1, NULL, '2026-04-13 16:10:50', '2026-04-14 13:43:49', 1);
INSERT INTO `supplier` VALUES (2, '湖州织里童乐服饰有限公司', NULL, '浙江省湖州市织里镇童装南路 88 号', '13912345678', 'tongle@163.com', '李经理', 3, NULL, NULL, 1, NULL, '2026-04-13 16:10:50', '2026-04-13 16:10:50', 0);
INSERT INTO `supplier` VALUES (3, '广州未东服饰有限公司', NULL, '广东省广州市白云区嘉禾街望岗工业区二路 16 号', '13887654321', 'weidongfushi@126.com', '王经理', 7, NULL, NULL, 1, NULL, '2026-04-13 16:10:50', '2026-04-13 16:10:50', 0);
INSERT INTO `supplier` VALUES (4, '福建七彩狐服装织造有限公司', NULL, '福建省晋江市陈埭镇河滨北路 188 号', '13555556666', 'qicaihu@qq.com', '张经理', 4, NULL, NULL, 1, NULL, '2026-04-13 16:10:50', '2026-04-13 16:10:50', 0);
INSERT INTO `supplier` VALUES (5, '北京中关村电子', NULL, '北京市海淀区', '010-88881234', 'info@zgc.com', '赵主管', 6, NULL, NULL, 1, NULL, '2026-04-13 16:10:50', '2026-04-14 11:46:58', 1);
INSERT INTO `supplier` VALUES (6, '东莞童趣制衣厂', NULL, '广东省东莞市虎门镇人民北路 518 号', '13677778888', 'tongquzhiyi@163.com', '刘经理', 5, NULL, NULL, 1, '', '2026-04-14 13:45:54', '2026-04-14 13:45:54', 0);
INSERT INTO `supplier` VALUES (7, '绍兴上虞艺通进出口有限公司', NULL, '浙江省绍兴市上虞区百官街道工业园区 12 号', '13799990000', 'yitongapparel@yeah.net', '陈经理', 5, NULL, NULL, 1, '', '2026-04-14 13:46:19', '2026-04-14 13:46:19', 0);
INSERT INTO `supplier` VALUES (8, '耐克晋江工厂', NULL, '泉州晋江001号', '19877776666', '', '陈经理', 5, NULL, NULL, 1, '', '2026-04-14 14:32:06', '2026-04-14 14:32:06', 0);

-- ----------------------------
-- Table structure for supplier_industry
-- ----------------------------
DROP TABLE IF EXISTS `supplier_industry`;
CREATE TABLE `supplier_industry`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '行业主键ID',
  `name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '行业名称',
  `code` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '编码',
  `sort` int NOT NULL DEFAULT 0 COMMENT '排序，越小越靠前',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态：1-启用，0-停用',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_name`(`name` ASC) USING BTREE,
  INDEX `idx_status`(`status` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 6 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '供应商所属行业字典' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of supplier_industry
-- ----------------------------
INSERT INTO `supplier_industry` VALUES (1, '童装零售', 'KIDS_RETAIL', 10, 1, '2026-04-14 11:32:21', '2026-04-14 11:32:21', 0);
INSERT INTO `supplier_industry` VALUES (2, '儿童服饰制造 / 生产', 'KIDS_MFG', 20, 1, '2026-04-14 11:32:21', '2026-04-14 11:32:21', 0);
INSERT INTO `supplier_industry` VALUES (3, '服装批发', 'APPAREL_WHOLESALE', 30, 1, '2026-04-14 11:32:21', '2026-04-14 11:32:21', 0);
INSERT INTO `supplier_industry` VALUES (4, '母婴用品零售', 'MOTHER_BABY', 40, 1, '2026-04-14 11:32:21', '2026-04-14 11:32:21', 0);
INSERT INTO `supplier_industry` VALUES (5, '鞋帽零售', 'SHOES_HATS', 50, 1, '2026-04-14 11:32:21', '2026-04-14 11:32:21', 0);

-- ----------------------------
-- Table structure for supplier_industry_link
-- ----------------------------
DROP TABLE IF EXISTS `supplier_industry_link`;
CREATE TABLE `supplier_industry_link`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '关联主键',
  `supplier_id` bigint NOT NULL COMMENT '供应商ID',
  `industry_id` bigint NOT NULL COMMENT '行业ID',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_supplier_industry`(`supplier_id` ASC, `industry_id` ASC) USING BTREE,
  INDEX `idx_industry_id`(`industry_id` ASC) USING BTREE,
  CONSTRAINT `fk_sil_industry` FOREIGN KEY (`industry_id`) REFERENCES `supplier_industry` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_sil_supplier` FOREIGN KEY (`supplier_id`) REFERENCES `supplier` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE = InnoDB AUTO_INCREMENT = 13 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '供应商与行业多对多关联' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of supplier_industry_link
-- ----------------------------
INSERT INTO `supplier_industry_link` VALUES (7, 2, 1);
INSERT INTO `supplier_industry_link` VALUES (8, 3, 1);
INSERT INTO `supplier_industry_link` VALUES (9, 4, 1);
INSERT INTO `supplier_industry_link` VALUES (10, 6, 2);
INSERT INTO `supplier_industry_link` VALUES (11, 7, 2);
INSERT INTO `supplier_industry_link` VALUES (12, 8, 2);

-- ----------------------------
-- Table structure for sys_category
-- ----------------------------
DROP TABLE IF EXISTS `sys_category`;
CREATE TABLE `sys_category`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '分类主键ID',
  `name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '分类名称',
  `code` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '分类编码',
  `parent_id` bigint NULL DEFAULT 0 COMMENT '父分类ID（0 表示顶级）',
  `sort` int NULL DEFAULT 0 COMMENT '排序',
  `status` tinyint NULL DEFAULT 1 COMMENT '状态：1-启用，0-禁用',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_parent_id`(`parent_id` ASC) USING BTREE,
  INDEX `idx_code`(`code` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 28 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '商品分类表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_category
-- ----------------------------
INSERT INTO `sys_category` VALUES (8, '童装', 'CHILDRENS_CLOTHING', 0, 1, 1, '2026-04-14 10:46:18', '2026-04-14 10:52:22', 0);
INSERT INTO `sys_category` VALUES (20, '童鞋', 'CAT2604140001', 0, 2, 1, '2026-04-14 10:53:28', '2026-04-14 10:53:28', 0);
INSERT INTO `sys_category` VALUES (21, '童鞋', 'CAT2604140002', 0, 2, 1, '2026-04-14 10:54:44', '2026-04-14 10:55:15', 1);
INSERT INTO `sys_category` VALUES (22, '儿童配饰', 'CAT2604140003', 0, 3, 1, '2026-04-14 10:54:52', '2026-04-14 10:54:52', 0);
INSERT INTO `sys_category` VALUES (23, '儿童家居服', 'CAT2604140004', 0, 4, 1, '2026-04-14 10:55:36', '2026-04-14 10:55:36', 0);
INSERT INTO `sys_category` VALUES (24, '儿童内衣', 'CAT2604140005', 0, 5, 1, '2026-04-14 10:55:47', '2026-04-14 10:55:47', 0);
INSERT INTO `sys_category` VALUES (25, '儿童书包 / 箱包', 'CAT2604140006', 0, 6, 1, '2026-04-14 10:55:59', '2026-04-14 10:55:59', 0);
INSERT INTO `sys_category` VALUES (26, '儿童袜子', 'CAT2604140007', 0, 7, 1, '2026-04-14 10:56:07', '2026-04-14 10:56:07', 0);
INSERT INTO `sys_category` VALUES (27, '儿童帽子', 'CAT2604140008', 0, 8, 1, '2026-04-14 10:56:14', '2026-04-14 10:56:14', 0);

-- ----------------------------
-- Table structure for sys_config
-- ----------------------------
DROP TABLE IF EXISTS `sys_config`;
CREATE TABLE `sys_config`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '配置主键ID',
  `config_key` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '配置键',
  `config_value` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '配置值',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_config_key`(`config_key` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 6 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '系统配置表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_config
-- ----------------------------
INSERT INTO `sys_config` VALUES (1, 'companyName', '深圳华创科技有限公司', '2026-04-16 10:00:00', '2026-04-16 10:00:00', 0);
INSERT INTO `sys_config` VALUES (2, 'phone', '0755-88888888', '2026-04-16 10:00:00', '2026-04-16 10:00:00', 0);
INSERT INTO `sys_config` VALUES (3, 'address', '深圳市南山区科技园', '2026-04-16 10:00:00', '2026-04-16 10:00:00', 0);
INSERT INTO `sys_config` VALUES (4, 'stockWarning', '开启', '2026-04-16 10:00:00', '2026-04-16 10:00:00', 0);
INSERT INTO `sys_config` VALUES (5, 'staleDays', '90', '2026-04-16 10:00:00', '2026-04-16 10:00:00', 0);

-- ----------------------------
-- Table structure for sys_operation_log
-- ----------------------------
DROP TABLE IF EXISTS `sys_operation_log`;
CREATE TABLE `sys_operation_log`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '日志主键ID',
  `user_id` bigint NULL DEFAULT NULL COMMENT '操作用户ID',
  `user_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '操作用户名',
  `role_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '操作人角色名称',
  `action` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '操作类型',
  `content` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '操作内容',
  `module` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '操作模块',
  `ip` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '操作IP',
  `status` tinyint NULL DEFAULT 1 COMMENT '操作状态：1-成功，0-失败',
  `error_msg` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '错误信息',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
  `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_user_id`(`user_id` ASC) USING BTREE,
  INDEX `idx_action`(`action` ASC) USING BTREE,
  INDEX `idx_create_time`(`create_time` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 192 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '系统操作日志表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_operation_log
-- ----------------------------
INSERT INTO `sys_operation_log` VALUES (1, 1, '张明远', '超级管理员', '登录', '系统登录', '系统', '192.168.1.100', 1, NULL, '2026-04-11 15:30:25', 0);
INSERT INTO `sys_operation_log` VALUES (2, 3, '李明', '销售专员', '创建', '新建销售订单 #SO2026041102', '销售管理', '192.168.1.105', 1, NULL, '2026-04-11 15:25:10', 0);
INSERT INTO `sys_operation_log` VALUES (3, 2, '王建国', '仓管主管', '更新', '确认入库 #IN2026041102', '采购管理', '192.168.1.108', 1, NULL, '2026-04-11 15:20:45', 0);
INSERT INTO `sys_operation_log` VALUES (4, 1, '张明远', '超级管理员', '配置', '修改系统配置', '系统设置', '192.168.1.100', 1, NULL, '2026-04-11 14:30:20', 0);
INSERT INTO `sys_operation_log` VALUES (5, 4, '张华', '采购专员', '创建', '新建调拨单 #TR2026041103', '库存管理', '192.168.1.112', 1, NULL, '2026-04-11 14:15:33', 0);
INSERT INTO `sys_operation_log` VALUES (6, 5, '陈丽', '财务专员', '导出', '导出财务报表 2026-03', '报表分析', '192.168.1.115', 1, NULL, '2026-04-11 13:45:18', 0);
INSERT INTO `sys_operation_log` VALUES (7, 1, 'admin', '超级管理员', '新增', '新增商品信息', '商品管理', '0:0:0:0:0:0:0:1', 1, NULL, '2026-04-13 16:42:38', 0);
INSERT INTO `sys_operation_log` VALUES (8, 1, 'admin', '超级管理员', '新增', '创建采购订单', '采购管理', '0:0:0:0:0:0:0:1', 1, NULL, '2026-04-13 16:43:31', 0);
INSERT INTO `sys_operation_log` VALUES (9, 1, 'admin', '超级管理员', '入库', '确认采购入库', '采购管理', '0:0:0:0:0:0:0:1', 1, NULL, '2026-04-13 16:44:34', 0);
INSERT INTO `sys_operation_log` VALUES (10, 1, 'admin', '超级管理员', '新增', '创建销售订单', '销售管理', '0:0:0:0:0:0:0:1', 1, NULL, '2026-04-13 18:00:45', 0);
INSERT INTO `sys_operation_log` VALUES (11, NULL, 'anonymousUser', NULL, '新增', '新增商品信息', '商品管理', '0:0:0:0:0:0:0:1', 1, NULL, '2026-04-14 10:03:25', 0);
INSERT INTO `sys_operation_log` VALUES (12, NULL, 'anonymousUser', NULL, '新增', '新增商品信息', '商品管理', '0:0:0:0:0:0:0:1', 1, NULL, '2026-04-14 10:04:06', 0);
INSERT INTO `sys_operation_log` VALUES (13, NULL, 'anonymousUser', NULL, '新增', '新增商品信息', '商品管理', '0:0:0:0:0:0:0:1', 1, NULL, '2026-04-14 10:05:12', 0);
INSERT INTO `sys_operation_log` VALUES (14, NULL, 'anonymousUser', NULL, '新增', '新增商品信息', '商品管理', '0:0:0:0:0:0:0:1', 1, NULL, '2026-04-14 10:06:06', 0);
INSERT INTO `sys_operation_log` VALUES (15, NULL, 'anonymousUser', NULL, '新增', '新增商品信息', '商品管理', '0:0:0:0:0:0:0:1', 1, NULL, '2026-04-14 10:07:07', 0);
INSERT INTO `sys_operation_log` VALUES (16, NULL, 'anonymousUser', NULL, '新增', '新增商品信息', '商品管理', '0:0:0:0:0:0:0:1', 1, NULL, '2026-04-14 10:07:37', 0);
INSERT INTO `sys_operation_log` VALUES (17, NULL, 'anonymousUser', NULL, '新增', '新增商品信息', '商品管理', '0:0:0:0:0:0:0:1', 1, NULL, '2026-04-14 10:08:14', 0);
INSERT INTO `sys_operation_log` VALUES (18, NULL, 'anonymousUser', NULL, '新增', '新增商品信息', '商品管理', '0:0:0:0:0:0:0:1', 1, NULL, '2026-04-14 10:08:47', 0);
INSERT INTO `sys_operation_log` VALUES (19, NULL, 'anonymousUser', NULL, '新增', '新增商品信息', '商品管理', '0:0:0:0:0:0:0:1', 1, NULL, '2026-04-14 10:09:26', 0);
INSERT INTO `sys_operation_log` VALUES (20, NULL, 'anonymousUser', NULL, '新增', '创建采购订单', '采购管理', '0:0:0:0:0:0:0:1', 1, NULL, '2026-04-14 10:10:26', 0);
INSERT INTO `sys_operation_log` VALUES (21, NULL, 'anonymousUser', NULL, '新增', '创建采购订单', '采购管理', '0:0:0:0:0:0:0:1', 1, NULL, '2026-04-14 10:10:59', 0);
INSERT INTO `sys_operation_log` VALUES (22, NULL, 'anonymousUser', NULL, '新增', '创建采购订单', '采购管理', '0:0:0:0:0:0:0:1', 1, NULL, '2026-04-14 10:11:14', 0);
INSERT INTO `sys_operation_log` VALUES (23, NULL, 'anonymousUser', NULL, '新增', '创建采购订单', '采购管理', '0:0:0:0:0:0:0:1', 1, NULL, '2026-04-14 10:11:37', 0);
INSERT INTO `sys_operation_log` VALUES (24, NULL, 'anonymousUser', NULL, '新增', '创建采购订单', '采购管理', '0:0:0:0:0:0:0:1', 1, NULL, '2026-04-14 10:11:58', 0);
INSERT INTO `sys_operation_log` VALUES (25, NULL, 'anonymousUser', NULL, '新增', '创建采购订单', '采购管理', '0:0:0:0:0:0:0:1', 1, NULL, '2026-04-14 10:12:10', 0);
INSERT INTO `sys_operation_log` VALUES (26, NULL, 'anonymousUser', NULL, '新增', '创建采购订单', '采购管理', '0:0:0:0:0:0:0:1', 1, NULL, '2026-04-14 10:12:25', 0);
INSERT INTO `sys_operation_log` VALUES (27, NULL, 'anonymousUser', NULL, '新增', '创建采购订单', '采购管理', '0:0:0:0:0:0:0:1', 1, NULL, '2026-04-14 10:13:05', 0);
INSERT INTO `sys_operation_log` VALUES (28, NULL, 'anonymousUser', NULL, '新增', '创建采购订单', '采购管理', '0:0:0:0:0:0:0:1', 1, NULL, '2026-04-14 10:13:21', 0);
INSERT INTO `sys_operation_log` VALUES (29, NULL, 'anonymousUser', NULL, '新增', '创建销售订单', '销售管理', '0:0:0:0:0:0:0:1', 1, NULL, '2026-04-14 10:13:59', 0);
INSERT INTO `sys_operation_log` VALUES (30, NULL, 'anonymousUser', NULL, '新增', '创建销售订单', '销售管理', '0:0:0:0:0:0:0:1', 1, NULL, '2026-04-14 10:14:29', 0);
INSERT INTO `sys_operation_log` VALUES (31, NULL, 'anonymousUser', NULL, '新增', '创建销售订单', '销售管理', '0:0:0:0:0:0:0:1', 1, NULL, '2026-04-14 10:14:46', 0);
INSERT INTO `sys_operation_log` VALUES (32, NULL, 'anonymousUser', NULL, '新增', '创建销售订单', '销售管理', '0:0:0:0:0:0:0:1', 1, NULL, '2026-04-14 10:15:01', 0);
INSERT INTO `sys_operation_log` VALUES (33, NULL, 'anonymousUser', NULL, '新增', '创建销售订单', '销售管理', '0:0:0:0:0:0:0:1', 1, NULL, '2026-04-14 10:15:23', 0);
INSERT INTO `sys_operation_log` VALUES (34, NULL, 'anonymousUser', NULL, '新增', '创建销售订单', '销售管理', '0:0:0:0:0:0:0:1', 1, NULL, '2026-04-14 10:15:37', 0);
INSERT INTO `sys_operation_log` VALUES (35, NULL, 'anonymousUser', NULL, '新增', '创建销售订单', '销售管理', '0:0:0:0:0:0:0:1', 1, NULL, '2026-04-14 10:15:48', 0);
INSERT INTO `sys_operation_log` VALUES (36, NULL, 'anonymousUser', NULL, '新增', '创建销售订单', '销售管理', '0:0:0:0:0:0:0:1', 1, NULL, '2026-04-14 10:15:59', 0);
INSERT INTO `sys_operation_log` VALUES (37, NULL, 'anonymousUser', NULL, '入库', '确认采购入库', '采购管理', '0:0:0:0:0:0:0:1', 1, NULL, '2026-04-14 10:16:16', 0);
INSERT INTO `sys_operation_log` VALUES (38, NULL, 'anonymousUser', NULL, '入库', '确认采购入库', '采购管理', '0:0:0:0:0:0:0:1', 1, NULL, '2026-04-14 10:16:31', 0);
INSERT INTO `sys_operation_log` VALUES (39, NULL, 'anonymousUser', NULL, '入库', '确认采购入库', '采购管理', '0:0:0:0:0:0:0:1', 1, NULL, '2026-04-14 10:16:37', 0);
INSERT INTO `sys_operation_log` VALUES (40, NULL, 'anonymousUser', NULL, '入库', '确认采购入库', '采购管理', '0:0:0:0:0:0:0:1', 1, NULL, '2026-04-14 10:16:44', 0);
INSERT INTO `sys_operation_log` VALUES (41, NULL, 'anonymousUser', NULL, '入库', '确认采购入库', '采购管理', '0:0:0:0:0:0:0:1', 1, NULL, '2026-04-14 10:16:50', 0);
INSERT INTO `sys_operation_log` VALUES (42, NULL, 'anonymousUser', NULL, '入库', '确认采购入库', '采购管理', '0:0:0:0:0:0:0:1', 1, NULL, '2026-04-14 10:16:55', 0);
INSERT INTO `sys_operation_log` VALUES (43, NULL, 'anonymousUser', NULL, '入库', '确认采购入库', '采购管理', '0:0:0:0:0:0:0:1', 1, NULL, '2026-04-14 10:17:00', 0);
INSERT INTO `sys_operation_log` VALUES (44, NULL, 'anonymousUser', NULL, '入库', '确认采购入库', '采购管理', '0:0:0:0:0:0:0:1', 1, NULL, '2026-04-14 10:17:06', 0);
INSERT INTO `sys_operation_log` VALUES (45, NULL, 'anonymousUser', NULL, '入库', '确认采购入库', '采购管理', '0:0:0:0:0:0:0:1', 1, NULL, '2026-04-14 10:17:15', 0);
INSERT INTO `sys_operation_log` VALUES (46, NULL, 'anonymousUser', NULL, '调拨', '创建库存调拨单', '库存管理', '0:0:0:0:0:0:0:1', 1, NULL, '2026-04-14 10:18:03', 0);
INSERT INTO `sys_operation_log` VALUES (47, NULL, 'anonymousUser', NULL, '调拨', '创建库存调拨单', '库存管理', '0:0:0:0:0:0:0:1', 0, '源仓库库存不足', '2026-04-14 10:18:17', 0);
INSERT INTO `sys_operation_log` VALUES (48, NULL, 'anonymousUser', NULL, '调拨', '创建库存调拨单', '库存管理', '0:0:0:0:0:0:0:1', 0, '源仓库库存不足', '2026-04-14 10:18:21', 0);
INSERT INTO `sys_operation_log` VALUES (49, NULL, 'anonymousUser', NULL, '调拨', '创建库存调拨单', '库存管理', '0:0:0:0:0:0:0:1', 0, '源仓库库存不足', '2026-04-14 10:18:39', 0);
INSERT INTO `sys_operation_log` VALUES (50, NULL, 'anonymousUser', NULL, '调拨', '创建库存调拨单', '库存管理', '0:0:0:0:0:0:0:1', 0, '源仓库库存不足', '2026-04-14 10:18:42', 0);
INSERT INTO `sys_operation_log` VALUES (51, NULL, 'anonymousUser', NULL, '调拨', '创建库存调拨单', '库存管理', '0:0:0:0:0:0:0:1', 1, NULL, '2026-04-14 10:19:04', 0);
INSERT INTO `sys_operation_log` VALUES (52, NULL, 'anonymousUser', NULL, '调拨', '创建库存调拨单', '库存管理', '0:0:0:0:0:0:0:1', 1, NULL, '2026-04-14 10:19:14', 0);
INSERT INTO `sys_operation_log` VALUES (53, NULL, 'anonymousUser', NULL, '付款', '确认订单付款', '销售管理', '0:0:0:0:0:0:0:1', 1, NULL, '2026-04-14 10:20:00', 0);
INSERT INTO `sys_operation_log` VALUES (54, NULL, 'anonymousUser', NULL, '付款', '确认订单付款', '销售管理', '0:0:0:0:0:0:0:1', 1, NULL, '2026-04-14 10:20:01', 0);
INSERT INTO `sys_operation_log` VALUES (55, NULL, 'anonymousUser', NULL, '付款', '确认订单付款', '销售管理', '0:0:0:0:0:0:0:1', 1, NULL, '2026-04-14 10:20:02', 0);
INSERT INTO `sys_operation_log` VALUES (56, NULL, 'anonymousUser', NULL, '付款', '确认订单付款', '销售管理', '0:0:0:0:0:0:0:1', 1, NULL, '2026-04-14 10:20:03', 0);
INSERT INTO `sys_operation_log` VALUES (57, NULL, 'anonymousUser', NULL, '付款', '确认订单付款', '销售管理', '0:0:0:0:0:0:0:1', 1, NULL, '2026-04-14 10:20:04', 0);
INSERT INTO `sys_operation_log` VALUES (58, NULL, 'anonymousUser', NULL, '付款', '确认订单付款', '销售管理', '0:0:0:0:0:0:0:1', 1, NULL, '2026-04-14 10:20:05', 0);
INSERT INTO `sys_operation_log` VALUES (59, NULL, 'anonymousUser', NULL, '付款', '确认订单付款', '销售管理', '0:0:0:0:0:0:0:1', 1, NULL, '2026-04-14 10:20:06', 0);
INSERT INTO `sys_operation_log` VALUES (60, NULL, 'anonymousUser', NULL, '付款', '确认订单付款', '销售管理', '0:0:0:0:0:0:0:1', 1, NULL, '2026-04-14 10:20:08', 0);
INSERT INTO `sys_operation_log` VALUES (61, NULL, 'anonymousUser', NULL, '付款', '确认订单付款', '销售管理', '0:0:0:0:0:0:0:1', 1, NULL, '2026-04-14 10:20:09', 0);
INSERT INTO `sys_operation_log` VALUES (62, NULL, 'anonymousUser', NULL, '发货', '确认订单发货', '销售管理', '0:0:0:0:0:0:0:1', 1, NULL, '2026-04-14 10:25:25', 0);
INSERT INTO `sys_operation_log` VALUES (63, NULL, 'anonymousUser', NULL, '收货', '确认订单收货', '销售管理', '0:0:0:0:0:0:0:1', 1, NULL, '2026-04-14 10:25:29', 0);
INSERT INTO `sys_operation_log` VALUES (64, NULL, 'anonymousUser', NULL, '发货', '确认订单发货', '销售管理', '0:0:0:0:0:0:0:1', 1, NULL, '2026-04-14 10:25:44', 0);
INSERT INTO `sys_operation_log` VALUES (65, NULL, 'anonymousUser', NULL, '收货', '确认订单收货', '销售管理', '0:0:0:0:0:0:0:1', 1, NULL, '2026-04-14 10:25:45', 0);
INSERT INTO `sys_operation_log` VALUES (66, NULL, 'anonymousUser', NULL, '发货', '确认订单发货', '销售管理', '0:0:0:0:0:0:0:1', 1, NULL, '2026-04-14 10:26:01', 0);
INSERT INTO `sys_operation_log` VALUES (67, NULL, 'anonymousUser', NULL, '收货', '确认订单收货', '销售管理', '0:0:0:0:0:0:0:1', 1, NULL, '2026-04-14 10:26:02', 0);
INSERT INTO `sys_operation_log` VALUES (68, NULL, 'anonymousUser', NULL, '发货', '确认订单发货', '销售管理', '0:0:0:0:0:0:0:1', 1, NULL, '2026-04-14 10:26:12', 0);
INSERT INTO `sys_operation_log` VALUES (69, NULL, 'anonymousUser', NULL, '收货', '确认订单收货', '销售管理', '0:0:0:0:0:0:0:1', 1, NULL, '2026-04-14 10:26:13', 0);
INSERT INTO `sys_operation_log` VALUES (70, NULL, 'anonymousUser', NULL, '发货', '确认订单发货', '销售管理', '0:0:0:0:0:0:0:1', 1, NULL, '2026-04-14 10:26:27', 0);
INSERT INTO `sys_operation_log` VALUES (71, NULL, 'anonymousUser', NULL, '收货', '确认订单收货', '销售管理', '0:0:0:0:0:0:0:1', 1, NULL, '2026-04-14 10:26:28', 0);
INSERT INTO `sys_operation_log` VALUES (72, NULL, 'anonymousUser', NULL, '发货', '确认订单发货', '销售管理', '0:0:0:0:0:0:0:1', 1, NULL, '2026-04-14 10:26:42', 0);
INSERT INTO `sys_operation_log` VALUES (73, NULL, 'anonymousUser', NULL, '收货', '确认订单收货', '销售管理', '0:0:0:0:0:0:0:1', 1, NULL, '2026-04-14 10:26:43', 0);
INSERT INTO `sys_operation_log` VALUES (74, NULL, 'anonymousUser', NULL, '发货', '确认订单发货', '销售管理', '0:0:0:0:0:0:0:1', 1, NULL, '2026-04-14 10:26:55', 0);
INSERT INTO `sys_operation_log` VALUES (75, NULL, 'anonymousUser', NULL, '收货', '确认订单收货', '销售管理', '0:0:0:0:0:0:0:1', 1, NULL, '2026-04-14 10:26:56', 0);
INSERT INTO `sys_operation_log` VALUES (76, NULL, 'anonymousUser', NULL, '发货', '确认订单发货', '销售管理', '0:0:0:0:0:0:0:1', 1, NULL, '2026-04-14 10:27:08', 0);
INSERT INTO `sys_operation_log` VALUES (77, NULL, 'anonymousUser', NULL, '收货', '确认订单收货', '销售管理', '0:0:0:0:0:0:0:1', 1, NULL, '2026-04-14 10:27:09', 0);
INSERT INTO `sys_operation_log` VALUES (78, NULL, 'anonymousUser', NULL, '发货', '确认订单发货', '销售管理', '0:0:0:0:0:0:0:1', 1, NULL, '2026-04-14 10:27:19', 0);
INSERT INTO `sys_operation_log` VALUES (79, NULL, 'anonymousUser', NULL, '收货', '确认订单收货', '销售管理', '0:0:0:0:0:0:0:1', 1, NULL, '2026-04-14 10:27:20', 0);
INSERT INTO `sys_operation_log` VALUES (80, NULL, 'anonymousUser', NULL, '修改', '修改用户信息', '用户管理', '0:0:0:0:0:0:0:1', 1, NULL, '2026-04-14 10:38:04', 0);
INSERT INTO `sys_operation_log` VALUES (81, NULL, 'anonymousUser', NULL, '新增', '新增用户账号', '用户管理', '0:0:0:0:0:0:0:1', 1, NULL, '2026-04-14 10:38:37', 0);
INSERT INTO `sys_operation_log` VALUES (82, NULL, 'anonymousUser', NULL, '删除', '删除用户账号', '用户管理', '0:0:0:0:0:0:0:1', 1, NULL, '2026-04-14 10:38:43', 0);
INSERT INTO `sys_operation_log` VALUES (83, 1, 'admin', '超级管理员', '新增', '创建商品分类', '分类管理', '0:0:0:0:0:0:0:1', 1, NULL, '2026-04-14 10:46:18', 0);
INSERT INTO `sys_operation_log` VALUES (84, NULL, 'anonymousUser', NULL, '修改', '修改商品分类', '分类管理', '0:0:0:0:0:0:0:1', 1, NULL, '2026-04-14 10:53:18', 0);
INSERT INTO `sys_operation_log` VALUES (85, NULL, 'anonymousUser', NULL, '新增', '创建商品分类', '分类管理', '0:0:0:0:0:0:0:1', 1, NULL, '2026-04-14 10:53:28', 0);
INSERT INTO `sys_operation_log` VALUES (86, 1, 'admin', '超级管理员', '新增', '创建商品分类', '分类管理', '0:0:0:0:0:0:0:1', 1, NULL, '2026-04-14 10:54:44', 0);
INSERT INTO `sys_operation_log` VALUES (87, 1, 'admin', '超级管理员', '新增', '创建商品分类', '分类管理', '0:0:0:0:0:0:0:1', 1, NULL, '2026-04-14 10:54:52', 0);
INSERT INTO `sys_operation_log` VALUES (88, 1, 'admin', '超级管理员', '删除', '删除商品分类', '分类管理', '0:0:0:0:0:0:0:1', 1, NULL, '2026-04-14 10:55:15', 0);
INSERT INTO `sys_operation_log` VALUES (89, 1, 'admin', '超级管理员', '新增', '创建商品分类', '分类管理', '0:0:0:0:0:0:0:1', 1, NULL, '2026-04-14 10:55:36', 0);
INSERT INTO `sys_operation_log` VALUES (90, 1, 'admin', '超级管理员', '新增', '创建商品分类', '分类管理', '0:0:0:0:0:0:0:1', 1, NULL, '2026-04-14 10:55:47', 0);
INSERT INTO `sys_operation_log` VALUES (91, 1, 'admin', '超级管理员', '新增', '创建商品分类', '分类管理', '0:0:0:0:0:0:0:1', 1, NULL, '2026-04-14 10:55:59', 0);
INSERT INTO `sys_operation_log` VALUES (92, 1, 'admin', '超级管理员', '新增', '创建商品分类', '分类管理', '0:0:0:0:0:0:0:1', 1, NULL, '2026-04-14 10:56:07', 0);
INSERT INTO `sys_operation_log` VALUES (93, 1, 'admin', '超级管理员', '新增', '创建商品分类', '分类管理', '0:0:0:0:0:0:0:1', 1, NULL, '2026-04-14 10:56:14', 0);
INSERT INTO `sys_operation_log` VALUES (94, 1, 'admin', '超级管理员', '修改', '修改商品信息', '商品管理', '0:0:0:0:0:0:0:1', 1, NULL, '2026-04-14 10:56:40', 0);
INSERT INTO `sys_operation_log` VALUES (95, 1, 'admin', '超级管理员', '修改', '修改商品信息', '商品管理', '0:0:0:0:0:0:0:1', 1, NULL, '2026-04-14 10:56:55', 0);
INSERT INTO `sys_operation_log` VALUES (96, 1, 'admin', '超级管理员', '修改', '修改商品信息', '商品管理', '0:0:0:0:0:0:0:1', 1, NULL, '2026-04-14 10:57:26', 0);
INSERT INTO `sys_operation_log` VALUES (97, 1, 'admin', '超级管理员', '修改', '修改商品信息', '商品管理', '0:0:0:0:0:0:0:1', 1, NULL, '2026-04-14 10:58:34', 0);
INSERT INTO `sys_operation_log` VALUES (98, 1, 'admin', '超级管理员', '修改', '修改商品信息', '商品管理', '0:0:0:0:0:0:0:1', 1, NULL, '2026-04-14 10:59:09', 0);
INSERT INTO `sys_operation_log` VALUES (99, 1, 'admin', '超级管理员', '修改', '修改商品信息', '商品管理', '0:0:0:0:0:0:0:1', 1, NULL, '2026-04-14 10:59:13', 0);
INSERT INTO `sys_operation_log` VALUES (100, 1, 'admin', '超级管理员', '修改', '修改商品信息', '商品管理', '0:0:0:0:0:0:0:1', 1, NULL, '2026-04-14 10:59:41', 0);
INSERT INTO `sys_operation_log` VALUES (101, 1, 'admin', '超级管理员', '修改', '修改商品信息', '商品管理', '0:0:0:0:0:0:0:1', 1, NULL, '2026-04-14 11:00:25', 0);
INSERT INTO `sys_operation_log` VALUES (102, 1, 'admin', '超级管理员', '修改', '修改商品信息', '商品管理', '0:0:0:0:0:0:0:1', 1, NULL, '2026-04-14 11:00:45', 0);
INSERT INTO `sys_operation_log` VALUES (103, 1, 'admin', '超级管理员', '修改', '修改商品信息', '商品管理', '0:0:0:0:0:0:0:1', 1, NULL, '2026-04-14 11:01:04', 0);
INSERT INTO `sys_operation_log` VALUES (104, 1, 'admin', '超级管理员', '修改', '修改商品信息', '商品管理', '0:0:0:0:0:0:0:1', 1, NULL, '2026-04-14 11:01:19', 0);
INSERT INTO `sys_operation_log` VALUES (105, 1, 'admin', '超级管理员', '修改', '修改商品信息', '商品管理', '0:0:0:0:0:0:0:1', 1, NULL, '2026-04-14 11:01:46', 0);
INSERT INTO `sys_operation_log` VALUES (106, 1, 'admin', '超级管理员', '修改', '修改商品信息', '商品管理', '0:0:0:0:0:0:0:1', 1, NULL, '2026-04-14 11:01:57', 0);
INSERT INTO `sys_operation_log` VALUES (107, 1, 'admin', '超级管理员', '修改', '修改商品信息', '商品管理', '0:0:0:0:0:0:0:1', 1, NULL, '2026-04-14 11:02:13', 0);
INSERT INTO `sys_operation_log` VALUES (108, 1, 'admin', '超级管理员', '删除', '删除商品', '商品管理', '0:0:0:0:0:0:0:1', 0, '该商品存在库存记录，无法删除。请先清理库存后再删除商品。', '2026-04-14 11:02:23', 0);
INSERT INTO `sys_operation_log` VALUES (109, 1, 'admin', '超级管理员', '删除', '删除商品分类', '分类管理', '0:0:0:0:0:0:0:1', 1, NULL, '2026-04-14 11:04:30', 0);
INSERT INTO `sys_operation_log` VALUES (110, 1, 'admin', '超级管理员', '删除', '删除商品分类', '分类管理', '0:0:0:0:0:0:0:1', 1, NULL, '2026-04-14 11:04:34', 0);
INSERT INTO `sys_operation_log` VALUES (111, 1, 'admin', '超级管理员', '修改', '修改仓库信息', '仓库管理', '0:0:0:0:0:0:0:1', 1, NULL, '2026-04-14 11:30:03', 0);
INSERT INTO `sys_operation_log` VALUES (112, 1, 'admin', '超级管理员', '修改', '修改仓库信息', '仓库管理', '0:0:0:0:0:0:0:1', 1, NULL, '2026-04-14 11:30:10', 0);
INSERT INTO `sys_operation_log` VALUES (113, 1, 'admin', '超级管理员', '修改', '修改仓库信息', '仓库管理', '0:0:0:0:0:0:0:1', 1, NULL, '2026-04-14 11:30:15', 0);
INSERT INTO `sys_operation_log` VALUES (114, 1, 'admin', '超级管理员', '修改', '修改仓库信息', '仓库管理', '0:0:0:0:0:0:0:1', 1, NULL, '2026-04-14 11:30:24', 0);
INSERT INTO `sys_operation_log` VALUES (115, 1, 'admin', '超级管理员', '修改', '修改仓库信息', '仓库管理', '0:0:0:0:0:0:0:1', 1, NULL, '2026-04-14 11:30:36', 0);
INSERT INTO `sys_operation_log` VALUES (116, 1, 'admin', '超级管理员', '修改', '修改仓库信息', '仓库管理', '0:0:0:0:0:0:0:1', 1, NULL, '2026-04-14 11:30:55', 0);
INSERT INTO `sys_operation_log` VALUES (117, 1, 'admin', '超级管理员', '修改', '修改仓库信息', '仓库管理', '0:0:0:0:0:0:0:1', 1, NULL, '2026-04-14 11:31:34', 0);
INSERT INTO `sys_operation_log` VALUES (118, 1, 'admin', '超级管理员', '修改', '修改仓库信息', '仓库管理', '0:0:0:0:0:0:0:1', 1, NULL, '2026-04-14 11:31:51', 0);
INSERT INTO `sys_operation_log` VALUES (119, 1, 'admin', '超级管理员', '修改', '修改仓库信息', '仓库管理', '0:0:0:0:0:0:0:1', 1, NULL, '2026-04-14 11:32:02', 0);
INSERT INTO `sys_operation_log` VALUES (120, 1, '张明远', '超级管理员', '修改', '修改用户信息', '用户管理', '0:0:0:0:0:0:0:1', 1, NULL, '2026-04-14 14:23:54', 0);
INSERT INTO `sys_operation_log` VALUES (121, 1, 'admin', '超级管理员', '新增', '新增商品信息', '商品管理', '0:0:0:0:0:0:0:1', 1, NULL, '2026-04-14 14:25:27', 0);
INSERT INTO `sys_operation_log` VALUES (122, 1, 'admin', '超级管理员', '新增', '创建采购订单', '采购管理', '0:0:0:0:0:0:0:1', 1, NULL, '2026-04-14 14:34:13', 0);
INSERT INTO `sys_operation_log` VALUES (123, 1, 'admin', '超级管理员', '入库', '确认采购入库', '采购管理', '0:0:0:0:0:0:0:1', 1, NULL, '2026-04-14 14:36:03', 0);
INSERT INTO `sys_operation_log` VALUES (124, 1, 'admin', '超级管理员', '修改', '修改角色权限配置', '角色管理', '0:0:0:0:0:0:0:1', 1, NULL, '2026-04-14 14:53:24', 0);
INSERT INTO `sys_operation_log` VALUES (125, 1, 'admin', '超级管理员', '修改', '修改角色权限配置', '角色管理', '0:0:0:0:0:0:0:1', 1, NULL, '2026-04-14 15:19:33', 0);
INSERT INTO `sys_operation_log` VALUES (126, 3, 'liming', '销售专员', '新增', '新增商品信息', '商品管理', '0:0:0:0:0:0:0:1', 1, NULL, '2026-04-14 16:27:53', 0);
INSERT INTO `sys_operation_log` VALUES (127, 3, 'liming', '销售专员', '修改', '修改商品信息', '商品管理', '0:0:0:0:0:0:0:1', 1, NULL, '2026-04-14 16:27:57', 0);
INSERT INTO `sys_operation_log` VALUES (128, 3, 'liming', '销售专员', '删除', '删除商品', '商品管理', '0:0:0:0:0:0:0:1', 1, NULL, '2026-04-14 16:28:00', 0);
INSERT INTO `sys_operation_log` VALUES (129, 1, 'admin', '超级管理员', '新增', '创建采购订单', '采购管理', '0:0:0:0:0:0:0:1', 1, NULL, '2026-04-14 16:29:51', 0);
INSERT INTO `sys_operation_log` VALUES (130, 1, 'admin', '超级管理员', '入库', '确认采购入库', '采购管理', '0:0:0:0:0:0:0:1', 1, NULL, '2026-04-14 16:30:07', 0);
INSERT INTO `sys_operation_log` VALUES (131, 1, 'admin', '超级管理员', '新增', '创建销售订单', '销售管理', '0:0:0:0:0:0:0:1', 1, NULL, '2026-04-14 16:31:14', 0);
INSERT INTO `sys_operation_log` VALUES (132, 1, 'admin', '超级管理员', '付款', '确认订单付款', '销售管理', '0:0:0:0:0:0:0:1', 1, NULL, '2026-04-14 16:31:37', 0);
INSERT INTO `sys_operation_log` VALUES (133, 1, 'admin', '超级管理员', '发货', '确认订单发货', '销售管理', '0:0:0:0:0:0:0:1', 1, NULL, '2026-04-14 16:44:45', 0);
INSERT INTO `sys_operation_log` VALUES (134, 1, 'admin', '超级管理员', '收货', '确认订单收货', '销售管理', '0:0:0:0:0:0:0:1', 1, NULL, '2026-04-14 16:45:04', 0);
INSERT INTO `sys_operation_log` VALUES (135, 2, 'wangjg', '仓管主管', '调拨', '创建库存调拨单', '库存管理', '0:0:0:0:0:0:0:1', 1, NULL, '2026-04-14 17:35:15', 0);
INSERT INTO `sys_operation_log` VALUES (136, 2, 'wangjg', '仓管主管', '新增', '创建新仓库', '仓库管理', '0:0:0:0:0:0:0:1', 1, NULL, '2026-04-14 17:35:45', 0);
INSERT INTO `sys_operation_log` VALUES (137, 2, 'wangjg', '仓管主管', '删除', '删除仓库', '仓库管理', '0:0:0:0:0:0:0:1', 1, NULL, '2026-04-14 17:35:48', 0);
INSERT INTO `sys_operation_log` VALUES (138, 1, 'admin', '超级管理员', '修改', '修改角色权限配置', '角色管理', '0:0:0:0:0:0:0:1', 1, NULL, '2026-04-14 17:36:22', 0);
INSERT INTO `sys_operation_log` VALUES (139, 1, 'admin', '超级管理员', '修改', '修改角色权限配置', '角色管理', '0:0:0:0:0:0:0:1', 1, NULL, '2026-04-14 17:44:07', 0);
INSERT INTO `sys_operation_log` VALUES (140, 1, 'admin', '超级管理员', '修改', '修改角色权限配置', '角色管理', '0:0:0:0:0:0:0:1', 1, NULL, '2026-04-14 17:46:38', 0);
INSERT INTO `sys_operation_log` VALUES (141, 2, 'wangjg', '仓管主管', '调拨', '创建库存调拨单', '库存管理', '0:0:0:0:0:0:0:1', 0, '源仓库库存不足', '2026-04-14 17:47:07', 0);
INSERT INTO `sys_operation_log` VALUES (142, 2, 'wangjg', '仓管主管', '调拨', '创建库存调拨单', '库存管理', '0:0:0:0:0:0:0:1', 0, '\r\n### Error updating database.  Cause: java.sql.SQLIntegrityConstraintViolationException: Duplicate entry \'TR2604140001\' for key \'inventory_transfer.uk_order_no\'\r\n### The error may exist in com/smartims/mapper/InventoryTransferMapper.java (b...(truncated)', '2026-04-14 17:47:14', 0);
INSERT INTO `sys_operation_log` VALUES (143, 1, 'admin', '超级管理员', '修改', '修改角色权限配置', '角色管理', '0:0:0:0:0:0:0:1', 1, NULL, '2026-04-14 17:53:34', 0);
INSERT INTO `sys_operation_log` VALUES (144, 2, 'wangjg', '仓管主管', '调拨', '创建库存调拨单', '库存管理', '0:0:0:0:0:0:0:1', 1, NULL, '2026-04-14 17:54:37', 0);
INSERT INTO `sys_operation_log` VALUES (145, 2, 'wangjg', '仓管主管', '调拨', '确认库存调拨', '库存管理', '0:0:0:0:0:0:0:1', 0, '源仓库库存不足', '2026-04-14 17:54:42', 0);
INSERT INTO `sys_operation_log` VALUES (146, 2, 'wangjg', '仓管主管', '调拨', '确认库存调拨', '库存管理', '0:0:0:0:0:0:0:1', 0, '\r\n### Error updating database.  Cause: java.sql.SQLIntegrityConstraintViolationException: Duplicate entry \'PROD2604140002\' for key \'inventory.uk_sku\'\r\n### The error may exist in com/smartims/mapper/InventoryMapper.java (best guess)\r\n### The ...(truncated)', '2026-04-14 17:54:44', 0);
INSERT INTO `sys_operation_log` VALUES (147, 2, 'wangjg', '仓管主管', '调拨', '确认库存调拨', '库存管理', '0:0:0:0:0:0:0:1', 0, '\r\n### Error updating database.  Cause: java.sql.SQLIntegrityConstraintViolationException: Duplicate entry \'PROD2604140003\' for key \'inventory.uk_sku\'\r\n### The error may exist in com/smartims/mapper/InventoryMapper.java (best guess)\r\n### The ...(truncated)', '2026-04-14 17:54:48', 0);
INSERT INTO `sys_operation_log` VALUES (148, 2, 'wangjg', '仓管主管', '调拨', '确认库存调拨', '库存管理', '0:0:0:0:0:0:0:1', 1, NULL, '2026-04-14 17:57:59', 0);
INSERT INTO `sys_operation_log` VALUES (149, 2, 'wangjg', '仓管主管', '调拨', '确认库存调拨', '库存管理', '0:0:0:0:0:0:0:1', 1, NULL, '2026-04-14 17:58:01', 0);
INSERT INTO `sys_operation_log` VALUES (150, 2, 'wangjg', '仓管主管', '调拨', '确认库存调拨', '库存管理', '0:0:0:0:0:0:0:1', 1, NULL, '2026-04-14 17:58:02', 0);
INSERT INTO `sys_operation_log` VALUES (151, 2, 'wangjg', '仓管主管', '调拨', '确认库存调拨', '库存管理', '0:0:0:0:0:0:0:1', 1, NULL, '2026-04-14 17:58:03', 0);
INSERT INTO `sys_operation_log` VALUES (152, 2, 'wangjg', '仓管主管', '调拨', '确认库存调拨', '库存管理', '0:0:0:0:0:0:0:1', 0, '源仓库库存不足', '2026-04-14 17:58:04', 0);
INSERT INTO `sys_operation_log` VALUES (153, 2, 'wangjg', '仓管主管', '调拨', '确认库存调拨', '库存管理', '0:0:0:0:0:0:0:1', 0, '源仓库库存不足', '2026-04-14 17:58:08', 0);
INSERT INTO `sys_operation_log` VALUES (154, 1, 'admin', '超级管理员', '修改', '修改角色权限配置', '角色管理', '0:0:0:0:0:0:0:1', 1, NULL, '2026-04-14 17:58:47', 0);
INSERT INTO `sys_operation_log` VALUES (155, 1, 'admin', '超级管理员', '修改', '修改角色权限配置', '角色管理', '0:0:0:0:0:0:0:1', 1, NULL, '2026-04-14 17:59:01', 0);
INSERT INTO `sys_operation_log` VALUES (156, 1, 'admin', '超级管理员', '修改', '修改角色权限配置', '角色管理', '0:0:0:0:0:0:0:1', 1, NULL, '2026-04-14 17:59:10', 0);
INSERT INTO `sys_operation_log` VALUES (157, 1, 'admin', '超级管理员', '修改', '修改角色权限配置', '角色管理', '0:0:0:0:0:0:0:1', 1, NULL, '2026-04-14 17:59:18', 0);
INSERT INTO `sys_operation_log` VALUES (158, 1, 'admin', '超级管理员', '修改', '修改角色权限配置', '角色管理', '0:0:0:0:0:0:0:1', 1, NULL, '2026-04-14 17:59:33', 0);
INSERT INTO `sys_operation_log` VALUES (159, 1, 'admin', '超级管理员', '修改', '修改角色权限配置', '角色管理', '0:0:0:0:0:0:0:1', 1, NULL, '2026-04-14 18:00:03', 0);
INSERT INTO `sys_operation_log` VALUES (160, 1, 'admin', '超级管理员', '修改', '修改角色权限配置', '角色管理', '0:0:0:0:0:0:0:1', 1, NULL, '2026-04-14 18:00:36', 0);
INSERT INTO `sys_operation_log` VALUES (161, 1, 'admin', '超级管理员', '修改', '修改角色权限配置', '角色管理', '0:0:0:0:0:0:0:1', 1, NULL, '2026-04-14 18:26:53', 0);
INSERT INTO `sys_operation_log` VALUES (162, 1, 'admin', '超级管理员', '修改', '修改角色权限配置', '角色管理', '0:0:0:0:0:0:0:1', 1, NULL, '2026-04-14 18:27:24', 0);
INSERT INTO `sys_operation_log` VALUES (163, 2, 'wangjg', '仓管主管', '新增', '新增商品信息', '商品管理', '0:0:0:0:0:0:0:1', 1, NULL, '2026-04-14 18:27:48', 0);
INSERT INTO `sys_operation_log` VALUES (164, 2, 'wangjg', '仓管主管', '删除', '删除商品', '商品管理', '0:0:0:0:0:0:0:1', 1, NULL, '2026-04-14 18:27:51', 0);
INSERT INTO `sys_operation_log` VALUES (165, 1, 'admin', '超级管理员', '修改', '修改角色权限配置', '角色管理', '0:0:0:0:0:0:0:1', 1, NULL, '2026-04-14 18:28:14', 0);
INSERT INTO `sys_operation_log` VALUES (166, 2, 'wangjg', '仓管主管', '新增', '创建采购订单', '采购管理', '0:0:0:0:0:0:0:1', 1, NULL, '2026-04-14 18:28:39', 0);
INSERT INTO `sys_operation_log` VALUES (167, 1, 'admin', '超级管理员', '删除', '删除角色', '角色管理', '0:0:0:0:0:0:0:1', 1, NULL, '2026-04-14 18:38:01', 0);
INSERT INTO `sys_operation_log` VALUES (168, 1, 'admin', '超级管理员', '新增', '新增商品信息', '商品管理', '0:0:0:0:0:0:0:1', 1, NULL, '2026-04-14 18:45:39', 0);
INSERT INTO `sys_operation_log` VALUES (169, 1, 'admin', '超级管理员', '新增', '创建采购订单', '采购管理', '0:0:0:0:0:0:0:1', 1, NULL, '2026-04-14 18:46:14', 0);
INSERT INTO `sys_operation_log` VALUES (170, 1, 'admin', '超级管理员', '入库', '确认采购入库', '采购管理', '0:0:0:0:0:0:0:1', 1, NULL, '2026-04-14 18:46:33', 0);
INSERT INTO `sys_operation_log` VALUES (171, 1, 'admin', '超级管理员', '新增', '创建销售订单', '销售管理', '0:0:0:0:0:0:0:1', 1, NULL, '2026-04-14 18:46:51', 0);
INSERT INTO `sys_operation_log` VALUES (172, 1, 'admin', '超级管理员', '新增', '创建销售订单', '销售管理', '0:0:0:0:0:0:0:1', 1, NULL, '2026-04-14 18:47:12', 0);
INSERT INTO `sys_operation_log` VALUES (173, 1, 'admin', '超级管理员', '新增', '创建销售订单', '销售管理', '0:0:0:0:0:0:0:1', 1, NULL, '2026-04-14 18:52:05', 0);
INSERT INTO `sys_operation_log` VALUES (174, 1, 'admin', '超级管理员', '付款', '确认订单付款', '销售管理', '0:0:0:0:0:0:0:1', 1, NULL, '2026-04-14 18:52:09', 0);
INSERT INTO `sys_operation_log` VALUES (175, 1, 'admin', '超级管理员', '修改', '修改角色权限配置', '角色管理', '0:0:0:0:0:0:0:1', 1, NULL, '2026-04-14 19:05:59', 0);
INSERT INTO `sys_operation_log` VALUES (176, 2, 'wangjg', '仓管主管', '付款', '确认订单付款', '销售管理', '0:0:0:0:0:0:0:1', 1, NULL, '2026-04-14 19:07:59', 0);
INSERT INTO `sys_operation_log` VALUES (177, 2, 'wangjg', '仓管主管', '新增', '创建销售订单', '销售管理', '0:0:0:0:0:0:0:1', 1, NULL, '2026-04-14 19:10:59', 0);
INSERT INTO `sys_operation_log` VALUES (178, 1, 'admin', '超级管理员', '修改', '修改商品信息', '商品管理', '0:0:0:0:0:0:0:1', 1, NULL, '2026-04-14 19:24:01', 0);
INSERT INTO `sys_operation_log` VALUES (179, 1, 'admin', '超级管理员', '入库', '确认采购入库', '采购管理', '0:0:0:0:0:0:0:1', 1, NULL, '2026-04-14 19:24:47', 0);
INSERT INTO `sys_operation_log` VALUES (180, 1, 'admin', '超级管理员', '删除', '删除角色', '角色管理', '0:0:0:0:0:0:0:1', 1, NULL, '2026-04-14 19:25:48', 0);
INSERT INTO `sys_operation_log` VALUES (181, 1, 'admin', '超级管理员', '修改', '修改角色权限配置', '角色管理', '0:0:0:0:0:0:0:1', 1, NULL, '2026-04-14 19:27:31', 0);
INSERT INTO `sys_operation_log` VALUES (182, 1, 'admin', '超级管理员', '修改', '修改用户信息', '用户管理', '0:0:0:0:0:0:0:1', 1, NULL, '2026-04-15 09:43:58', 0);
INSERT INTO `sys_operation_log` VALUES (183, 1, 'admin', '超级管理员', '修改', '修改用户信息', '用户管理', '0:0:0:0:0:0:0:1', 1, NULL, '2026-04-15 09:44:13', 0);
INSERT INTO `sys_operation_log` VALUES (184, 1, 'admin', '超级管理员', '新增', '创建采购订单', '采购管理', '0:0:0:0:0:0:0:1', 1, NULL, '2026-04-15 10:25:07', 0);
INSERT INTO `sys_operation_log` VALUES (185, 1, 'admin', '超级管理员', '修改', '修改采购订单', '采购管理', '0:0:0:0:0:0:0:1', 1, NULL, '2026-04-15 10:25:40', 0);
INSERT INTO `sys_operation_log` VALUES (186, 1, 'admin', '超级管理员', '入库', '确认采购入库', '采购管理', '0:0:0:0:0:0:0:1', 1, NULL, '2026-04-15 10:28:13', 0);
INSERT INTO `sys_operation_log` VALUES (187, 1, 'admin', '超级管理员', '导入', '提交 Excel 异步批量导入', '商品管理', '0:0:0:0:0:0:0:1', 1, NULL, '2026-04-15 10:48:40', 0);
INSERT INTO `sys_operation_log` VALUES (188, 1, 'admin', '超级管理员', '删除', '删除商品', '商品管理', '0:0:0:0:0:0:0:1', 1, NULL, '2026-04-15 10:48:47', 0);
INSERT INTO `sys_operation_log` VALUES (189, 1, 'admin', '超级管理员', '删除', '删除商品', '商品管理', '0:0:0:0:0:0:0:1', 1, NULL, '2026-04-15 10:48:49', 0);
INSERT INTO `sys_operation_log` VALUES (190, 1, 'admin', '超级管理员', '新增', '新增商品信息', '商品管理', '0:0:0:0:0:0:0:1', 1, NULL, '2026-04-15 11:02:42', 0);
INSERT INTO `sys_operation_log` VALUES (191, 1, 'admin', '超级管理员', '修改', '修改商品信息', '商品管理', '0:0:0:0:0:0:0:1', 1, NULL, '2026-04-15 11:02:53', 0);

-- ----------------------------
-- Table structure for sys_permission
-- ----------------------------
DROP TABLE IF EXISTS `sys_permission`;
CREATE TABLE `sys_permission`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '权限主键ID',
  `name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '权限名称',
  `code` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '权限编码',
  `type` tinyint NOT NULL COMMENT '权限类型：1-菜单，2-按钮，3-接口',
  `parent_id` bigint NULL DEFAULT 0 COMMENT '父权限ID',
  `path` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '路由路径',
  `icon` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '图标',
  `sort` int NULL DEFAULT 0 COMMENT '排序',
  `status` tinyint NULL DEFAULT 1 COMMENT '状态：1-启用，0-禁用',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_code`(`code` ASC) USING BTREE,
  INDEX `idx_parent_id`(`parent_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 36 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '系统权限表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_permission
-- ----------------------------
INSERT INTO `sys_permission` VALUES (1, '仪表盘', 'dashboard', 1, 0, '/dashboard', 'Odometer', 10, 1, '2026-04-14 17:25:30', '2026-04-14 17:25:30', 0);
INSERT INTO `sys_permission` VALUES (2, '商品管理', 'products', 1, 0, '/products', 'Goods', 20, 1, '2026-04-14 17:25:30', '2026-04-14 17:25:30', 0);
INSERT INTO `sys_permission` VALUES (3, '采购管理', 'purchase', 1, 0, '/purchase', 'ShoppingCart', 30, 1, '2026-04-14 17:25:30', '2026-04-14 17:25:30', 0);
INSERT INTO `sys_permission` VALUES (4, '销售管理', 'sales', 1, 0, '/sales', 'Money', 40, 1, '2026-04-14 17:25:30', '2026-04-14 17:25:30', 0);
INSERT INTO `sys_permission` VALUES (5, '库存管理', 'inventory', 1, 0, '/inventory', 'House', 50, 1, '2026-04-14 17:25:30', '2026-04-14 17:25:30', 0);
INSERT INTO `sys_permission` VALUES (6, '报表分析', 'reports', 1, 0, '/reports', 'DataAnalysis', 60, 1, '2026-04-14 17:25:30', '2026-04-14 17:25:30', 0);
INSERT INTO `sys_permission` VALUES (7, '系统设置', 'settings', 1, 0, '/settings', 'Setting', 70, 1, '2026-04-14 17:25:30', '2026-04-14 17:25:30', 0);
INSERT INTO `sys_permission` VALUES (8, '商品查看', 'product:view', 2, 2, NULL, NULL, 1, 1, '2026-04-14 17:25:30', '2026-04-14 17:25:30', 0);
INSERT INTO `sys_permission` VALUES (9, '商品添加', 'product:add', 2, 2, NULL, NULL, 2, 1, '2026-04-14 17:25:30', '2026-04-14 17:25:30', 0);
INSERT INTO `sys_permission` VALUES (10, '商品编辑', 'product:edit', 2, 2, NULL, NULL, 3, 1, '2026-04-14 17:25:30', '2026-04-14 17:25:30', 0);
INSERT INTO `sys_permission` VALUES (11, '商品删除', 'product:delete', 2, 2, NULL, NULL, 4, 1, '2026-04-14 17:25:30', '2026-04-14 17:25:30', 0);
INSERT INTO `sys_permission` VALUES (12, '采购订单查看', 'purchase:view', 2, 3, NULL, NULL, 1, 1, '2026-04-14 17:25:30', '2026-04-14 17:25:30', 0);
INSERT INTO `sys_permission` VALUES (13, '采购订单创建', 'purchase:add', 2, 3, NULL, NULL, 2, 1, '2026-04-14 17:25:30', '2026-04-14 17:25:30', 0);
INSERT INTO `sys_permission` VALUES (14, '采购订单编辑', 'purchase:edit', 2, 3, NULL, NULL, 3, 1, '2026-04-14 17:25:30', '2026-04-14 17:25:30', 0);
INSERT INTO `sys_permission` VALUES (15, '入库确认', 'purchase:inbound', 2, 3, NULL, NULL, 4, 1, '2026-04-14 17:25:30', '2026-04-14 17:25:30', 0);
INSERT INTO `sys_permission` VALUES (16, '销售订单查看', 'sales:view', 2, 4, NULL, NULL, 1, 1, '2026-04-14 17:25:30', '2026-04-14 17:25:30', 0);
INSERT INTO `sys_permission` VALUES (17, '销售订单创建', 'sales:add', 2, 4, NULL, NULL, 2, 1, '2026-04-14 17:25:30', '2026-04-14 17:25:30', 0);
INSERT INTO `sys_permission` VALUES (18, '发货确认', 'sales:ship', 2, 4, NULL, NULL, 3, 1, '2026-04-14 17:25:30', '2026-04-14 17:25:30', 0);
INSERT INTO `sys_permission` VALUES (19, '库存查看', 'inventory:view', 2, 5, NULL, NULL, 1, 1, '2026-04-14 17:25:30', '2026-04-14 17:25:30', 0);
INSERT INTO `sys_permission` VALUES (20, '库存调拨', 'inventory:transfer', 2, 5, NULL, NULL, 4, 1, '2026-04-14 17:25:30', '2026-04-14 17:25:30', 0);
INSERT INTO `sys_permission` VALUES (21, '报表查看', 'reports:view', 2, 6, NULL, NULL, 1, 1, '2026-04-14 17:25:30', '2026-04-14 17:25:30', 0);
INSERT INTO `sys_permission` VALUES (22, '报表导出', 'reports:export', 2, 6, NULL, NULL, 2, 1, '2026-04-14 17:25:30', '2026-04-14 17:25:30', 0);
INSERT INTO `sys_permission` VALUES (23, '用户管理', 'settings:user', 2, 7, NULL, NULL, 1, 1, '2026-04-14 17:25:30', '2026-04-14 17:25:30', 0);
INSERT INTO `sys_permission` VALUES (24, '角色管理', 'settings:role', 2, 7, NULL, NULL, 2, 1, '2026-04-14 17:25:30', '2026-04-14 17:25:30', 0);
INSERT INTO `sys_permission` VALUES (25, '系统配置', 'settings:config', 2, 7, NULL, NULL, 3, 1, '2026-04-14 17:25:30', '2026-04-14 17:25:30', 0);
INSERT INTO `sys_permission` VALUES (26, '售后工单', 'sales:aftersales', 2, 4, NULL, NULL, 4, 1, '2026-04-14 17:25:30', '2026-04-14 17:25:30', 0);
INSERT INTO `sys_permission` VALUES (27, '客户管理', 'sales:customer', 2, 4, NULL, NULL, 5, 1, '2026-04-14 17:25:30', '2026-04-14 17:25:30', 0);
INSERT INTO `sys_permission` VALUES (28, '仓库管理', 'inventory:warehouse', 2, 5, NULL, NULL, 3, 1, '2026-04-14 17:25:30', '2026-04-14 17:25:30', 0);
INSERT INTO `sys_permission` VALUES (29, '入库操作', 'inventory:inbound', 2, 5, NULL, NULL, 5, 1, '2026-04-14 17:25:30', '2026-04-14 17:25:30', 0);
INSERT INTO `sys_permission` VALUES (30, '出库操作', 'inventory:outbound', 2, 5, NULL, NULL, 6, 1, '2026-04-14 17:25:30', '2026-04-14 17:25:30', 0);
INSERT INTO `sys_permission` VALUES (31, '库存调整', 'inventory:adjust', 2, 5, NULL, NULL, 7, 1, '2026-04-14 17:25:30', '2026-04-14 17:25:30', 0);
INSERT INTO `sys_permission` VALUES (32, '确认付款', 'sales:payment', 2, 4, NULL, NULL, 6, 1, '2026-04-14 17:25:30', '2026-04-14 17:25:30', 0);
INSERT INTO `sys_permission` VALUES (33, '确认收货', 'sales:receive', 2, 4, NULL, NULL, 7, 1, '2026-04-14 17:25:30', '2026-04-14 17:25:30', 0);
INSERT INTO `sys_permission` VALUES (34, '供应商管理', 'purchase:supplier', 2, 3, NULL, NULL, 5, 1, '2026-04-14 17:25:30', '2026-04-14 17:25:30', 0);
INSERT INTO `sys_permission` VALUES (35, '出入库记录', 'inventory:records', 2, 5, NULL, NULL, 2, 1, '2026-04-14 17:25:30', '2026-04-14 17:25:30', 0);

-- ----------------------------
-- Table structure for sys_role
-- ----------------------------
DROP TABLE IF EXISTS `sys_role`;
CREATE TABLE `sys_role`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '角色主键ID',
  `name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '角色名称',
  `code` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '角色编码',
  `description` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '角色描述',
  `status` tinyint NULL DEFAULT 1 COMMENT '状态：1-启用，0-禁用',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_code`(`code` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 6 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '系统角色表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_role
-- ----------------------------
INSERT INTO `sys_role` VALUES (1, '超级管理员', 'SUPER_ADMIN', '拥有系统全部权限', 1, '2026-04-13 16:10:50', '2026-04-13 16:10:50', 0);
INSERT INTO `sys_role` VALUES (2, '仓管主管', 'WAREHOUSE_MANAGER', '负责库存管理和调拨审批', 1, '2026-04-13 16:10:50', '2026-04-14 19:25:48', 1);
INSERT INTO `sys_role` VALUES (3, '销售专员', 'SALES_STAFF', '负责销售订单和客户管理', 1, '2026-04-13 16:10:50', '2026-04-13 16:10:50', 0);
INSERT INTO `sys_role` VALUES (4, '采购专员', 'PURCHASE_STAFF', '负责采购订单和供应商管理', 1, '2026-04-13 16:10:50', '2026-04-13 16:10:50', 0);
INSERT INTO `sys_role` VALUES (5, '财务专员', 'FINANCE_STAFF', '负责财务报表和付款管理', 1, '2026-04-13 16:10:50', '2026-04-14 18:38:01', 1);

-- ----------------------------
-- Table structure for sys_role_permission
-- ----------------------------
DROP TABLE IF EXISTS `sys_role_permission`;
CREATE TABLE `sys_role_permission`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `role_id` bigint NOT NULL COMMENT '角色ID',
  `permission_id` bigint NOT NULL COMMENT '权限ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_role_permission`(`role_id` ASC, `permission_id` ASC) USING BTREE,
  INDEX `idx_role_id`(`role_id` ASC) USING BTREE,
  INDEX `idx_permission_id`(`permission_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 180 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '角色权限关联表（无逻辑删除）' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_role_permission
-- ----------------------------
INSERT INTO `sys_role_permission` VALUES (1, 1, 1, '2026-04-14 17:25:30');
INSERT INTO `sys_role_permission` VALUES (2, 1, 2, '2026-04-14 17:25:30');
INSERT INTO `sys_role_permission` VALUES (3, 1, 3, '2026-04-14 17:25:30');
INSERT INTO `sys_role_permission` VALUES (4, 1, 4, '2026-04-14 17:25:30');
INSERT INTO `sys_role_permission` VALUES (5, 1, 5, '2026-04-14 17:25:30');
INSERT INTO `sys_role_permission` VALUES (6, 1, 6, '2026-04-14 17:25:30');
INSERT INTO `sys_role_permission` VALUES (7, 1, 7, '2026-04-14 17:25:30');
INSERT INTO `sys_role_permission` VALUES (8, 1, 8, '2026-04-14 17:25:30');
INSERT INTO `sys_role_permission` VALUES (9, 1, 9, '2026-04-14 17:25:30');
INSERT INTO `sys_role_permission` VALUES (10, 1, 10, '2026-04-14 17:25:30');
INSERT INTO `sys_role_permission` VALUES (11, 1, 11, '2026-04-14 17:25:30');
INSERT INTO `sys_role_permission` VALUES (12, 1, 12, '2026-04-14 17:25:30');
INSERT INTO `sys_role_permission` VALUES (13, 1, 13, '2026-04-14 17:25:30');
INSERT INTO `sys_role_permission` VALUES (14, 1, 14, '2026-04-14 17:25:30');
INSERT INTO `sys_role_permission` VALUES (15, 1, 15, '2026-04-14 17:25:30');
INSERT INTO `sys_role_permission` VALUES (16, 1, 16, '2026-04-14 17:25:30');
INSERT INTO `sys_role_permission` VALUES (17, 1, 17, '2026-04-14 17:25:30');
INSERT INTO `sys_role_permission` VALUES (18, 1, 18, '2026-04-14 17:25:30');
INSERT INTO `sys_role_permission` VALUES (19, 1, 19, '2026-04-14 17:25:30');
INSERT INTO `sys_role_permission` VALUES (20, 1, 20, '2026-04-14 17:25:30');
INSERT INTO `sys_role_permission` VALUES (21, 1, 21, '2026-04-14 17:25:30');
INSERT INTO `sys_role_permission` VALUES (22, 1, 22, '2026-04-14 17:25:30');
INSERT INTO `sys_role_permission` VALUES (23, 1, 23, '2026-04-14 17:25:30');
INSERT INTO `sys_role_permission` VALUES (24, 1, 24, '2026-04-14 17:25:30');
INSERT INTO `sys_role_permission` VALUES (25, 1, 25, '2026-04-14 17:25:30');
INSERT INTO `sys_role_permission` VALUES (26, 1, 26, '2026-04-14 17:25:30');
INSERT INTO `sys_role_permission` VALUES (27, 1, 27, '2026-04-14 17:25:30');
INSERT INTO `sys_role_permission` VALUES (28, 1, 28, '2026-04-14 17:25:30');
INSERT INTO `sys_role_permission` VALUES (29, 1, 29, '2026-04-14 17:25:30');
INSERT INTO `sys_role_permission` VALUES (30, 1, 30, '2026-04-14 17:25:30');
INSERT INTO `sys_role_permission` VALUES (31, 1, 31, '2026-04-14 17:25:30');
INSERT INTO `sys_role_permission` VALUES (32, 1, 32, '2026-04-14 17:25:30');
INSERT INTO `sys_role_permission` VALUES (33, 1, 33, '2026-04-14 17:25:30');
INSERT INTO `sys_role_permission` VALUES (34, 1, 34, '2026-04-14 17:25:30');
INSERT INTO `sys_role_permission` VALUES (35, 1, 35, '2026-04-14 17:25:30');
INSERT INTO `sys_role_permission` VALUES (45, 3, 1, '2026-04-14 17:25:30');
INSERT INTO `sys_role_permission` VALUES (46, 3, 2, '2026-04-14 17:25:30');
INSERT INTO `sys_role_permission` VALUES (47, 3, 4, '2026-04-14 17:25:30');
INSERT INTO `sys_role_permission` VALUES (48, 3, 8, '2026-04-14 17:25:30');
INSERT INTO `sys_role_permission` VALUES (49, 3, 16, '2026-04-14 17:25:30');
INSERT INTO `sys_role_permission` VALUES (50, 3, 17, '2026-04-14 17:25:30');
INSERT INTO `sys_role_permission` VALUES (51, 3, 18, '2026-04-14 17:25:30');
INSERT INTO `sys_role_permission` VALUES (52, 3, 26, '2026-04-14 17:25:30');
INSERT INTO `sys_role_permission` VALUES (53, 3, 27, '2026-04-14 17:25:30');
INSERT INTO `sys_role_permission` VALUES (54, 3, 32, '2026-04-14 17:25:30');
INSERT INTO `sys_role_permission` VALUES (55, 3, 33, '2026-04-14 17:25:30');
INSERT INTO `sys_role_permission` VALUES (160, 4, 1, '2026-04-14 19:27:31');
INSERT INTO `sys_role_permission` VALUES (161, 4, 2, '2026-04-14 19:27:31');
INSERT INTO `sys_role_permission` VALUES (162, 4, 8, '2026-04-14 19:27:31');
INSERT INTO `sys_role_permission` VALUES (163, 4, 9, '2026-04-14 19:27:31');
INSERT INTO `sys_role_permission` VALUES (164, 4, 10, '2026-04-14 19:27:31');
INSERT INTO `sys_role_permission` VALUES (165, 4, 11, '2026-04-14 19:27:31');
INSERT INTO `sys_role_permission` VALUES (166, 4, 3, '2026-04-14 19:27:31');
INSERT INTO `sys_role_permission` VALUES (167, 4, 12, '2026-04-14 19:27:31');
INSERT INTO `sys_role_permission` VALUES (168, 4, 13, '2026-04-14 19:27:31');
INSERT INTO `sys_role_permission` VALUES (169, 4, 14, '2026-04-14 19:27:31');
INSERT INTO `sys_role_permission` VALUES (170, 4, 15, '2026-04-14 19:27:31');
INSERT INTO `sys_role_permission` VALUES (171, 4, 34, '2026-04-14 19:27:31');
INSERT INTO `sys_role_permission` VALUES (172, 4, 5, '2026-04-14 19:27:31');
INSERT INTO `sys_role_permission` VALUES (173, 4, 19, '2026-04-14 19:27:31');
INSERT INTO `sys_role_permission` VALUES (174, 4, 35, '2026-04-14 19:27:31');
INSERT INTO `sys_role_permission` VALUES (175, 4, 28, '2026-04-14 19:27:31');
INSERT INTO `sys_role_permission` VALUES (176, 4, 20, '2026-04-14 19:27:31');
INSERT INTO `sys_role_permission` VALUES (177, 4, 29, '2026-04-14 19:27:31');
INSERT INTO `sys_role_permission` VALUES (178, 4, 30, '2026-04-14 19:27:31');
INSERT INTO `sys_role_permission` VALUES (179, 4, 31, '2026-04-14 19:27:31');

-- ----------------------------
-- Table structure for sys_user
-- ----------------------------
DROP TABLE IF EXISTS `sys_user`;
CREATE TABLE `sys_user`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '用户主键ID',
  `username` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '用户名',
  `password` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '密码（BCrypt加密）',
  `name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '用户姓名',
  `email` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '邮箱',
  `phone` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '手机号',
  `avatar` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '头像URL',
  `role_id` bigint NULL DEFAULT NULL COMMENT '角色ID',
  `status` tinyint NULL DEFAULT 1 COMMENT '状态：1-正常，0-禁用',
  `last_login_time` datetime NULL DEFAULT NULL COMMENT '最后登录时间',
  `last_login_ip` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '最后登录IP',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 7 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '系统用户表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_user
-- ----------------------------
INSERT INTO `sys_user` VALUES (1, 'admin', '$2b$10$93Ia0x4QRohMJGPusnZb6OOcO/VC4BNE6a6tR52TnvE6C61ZwoJnG', '张明远', 'admin@inventory.com', '13855558888', NULL, 1, 1, '2026-04-15 14:31:05', NULL, '2026-04-13 16:10:50', '2026-04-14 14:08:21', 0);
INSERT INTO `sys_user` VALUES (2, 'wangjg', '$2b$10$93Ia0x4QRohMJGPusnZb6OOcO/VC4BNE6a6tR52TnvE6C61ZwoJnG', '王建国', 'wangjg@inventory.com', '13944446666', NULL, 3, 1, '2026-04-15 09:44:17', NULL, '2026-04-13 16:10:50', '2026-04-14 14:24:15', 0);
INSERT INTO `sys_user` VALUES (3, 'liming', '$2b$10$93Ia0x4QRohMJGPusnZb6OOcO/VC4BNE6a6tR52TnvE6C61ZwoJnG', '李明', 'liming@inventory.com', '13666663333', NULL, 3, 1, '2026-04-14 16:27:11', NULL, '2026-04-13 16:10:50', '2026-04-14 14:24:21', 0);
INSERT INTO `sys_user` VALUES (4, 'zhangh', '$2b$10$93Ia0x4QRohMJGPusnZb6OOcO/VC4BNE6a6tR52TnvE6C61ZwoJnG', '张华', 'zhangh@inventory.com', '13577779999', NULL, 4, 1, '2026-04-14 15:20:35', NULL, '2026-04-13 16:10:50', '2026-04-14 14:24:26', 0);
INSERT INTO `sys_user` VALUES (5, 'chenli', '$2b$10$93Ia0x4QRohMJGPusnZb6OOcO/VC4BNE6a6tR52TnvE6C61ZwoJnG', '陈丽', 'chenli@inventory.com', '15844447777', NULL, 4, 1, '2026-04-14 15:46:55', NULL, '2026-04-13 16:10:50', '2026-04-14 14:24:31', 0);

-- ----------------------------
-- Table structure for warehouse
-- ----------------------------
DROP TABLE IF EXISTS `warehouse`;
CREATE TABLE `warehouse`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '仓库主键ID',
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '仓库名称',
  `code` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '仓库编码',
  `address` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '仓库地址',
  `manager_id` bigint NULL DEFAULT NULL COMMENT '仓库管理员ID',
  `manager_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '仓库管理员姓名',
  `total_categories` int NULL DEFAULT 0 COMMENT '存储品类数',
  `total_stock` int NULL DEFAULT 0 COMMENT '总库存量',
  `total_value` decimal(12, 2) NULL DEFAULT 0.00 COMMENT '库存总值',
  `capacity` int NULL DEFAULT 100 COMMENT '容量上限',
  `capacity_used` decimal(8, 2) NULL DEFAULT 0.00 COMMENT '容量使用率（%）',
  `status` tinyint NULL DEFAULT 1 COMMENT '状态：1-正常，0-停用',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '备注',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_status`(`status` ASC) USING BTREE,
  INDEX `idx_wh_options`(`deleted` ASC, `status` ASC, `name` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 5 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '仓库表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of warehouse
-- ----------------------------
INSERT INTO `warehouse` VALUES (1, '晋江仓库A区', NULL, '福建省泉州市晋江市陈埭镇河滨北路安踏工业园旁 A 区 1 号仓', 2, '王建国', 10, 820, 14688.10, 78, 78.00, 1, '', '2026-04-13 16:10:50', '2026-04-13 16:10:50', 0);
INSERT INTO `warehouse` VALUES (2, '晋江仓库B区', NULL, '福建省泉州市晋江市池店镇泉安中路滨江商务区 B 区 1 号仓', 2, '王建国', 0, 0, 0.00, 65, 65.00, 1, '', '2026-04-13 16:10:50', '2026-04-13 16:10:50', 0);
INSERT INTO `warehouse` VALUES (3, '晋江仓库C区', NULL, '福建省泉州市晋江市西园街道赖厝社区物流园 C 区 1 号仓', 2, '王建国', 0, 0, 0.00, 45, 45.00, 1, '', '2026-04-13 16:10:50', '2026-04-13 16:10:50', 0);
INSERT INTO `warehouse` VALUES (4, '1', NULL, '', NULL, '', 0, 0, 0.00, 50, 50.00, 1, '', '2026-04-14 17:35:45', '2026-04-14 17:35:48', 1);

-- 已有库可手动执行（若不存在同名索引）：ALTER TABLE `warehouse` ADD INDEX `idx_wh_options` (`deleted`, `status`, `name`);

SET FOREIGN_KEY_CHECKS = 1;
