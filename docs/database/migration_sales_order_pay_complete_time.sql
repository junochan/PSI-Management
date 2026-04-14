-- =====================================================
-- 升级脚本：sales_order 增加 pay_time / complete_time
-- 适用：在引入「销售单发货进度各阶段时间」之前已建库、且表结构无下列字段时执行一次
-- 执行前请备份数据库；执行成功后可忽略重复执行（重复执行会因列已存在而报错）
-- =====================================================

USE `smart_ims`;

ALTER TABLE `sales_order`
  ADD COLUMN `pay_time` DATETIME DEFAULT NULL COMMENT '确认付款时间' AFTER `ship_time`,
  ADD COLUMN `complete_time` DATETIME DEFAULT NULL COMMENT '确认收货/订单完成时间' AFTER `pay_time`;
