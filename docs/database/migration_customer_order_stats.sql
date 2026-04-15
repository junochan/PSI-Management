-- 客户累计订单/金额/最近下单时间：与 sales_order 对齐，并供列表展示。
-- 执行前请备份。若列已存在可跳过对应 ALTER。

ALTER TABLE `customer`
  ADD COLUMN `last_order_time` DATETIME DEFAULT NULL COMMENT '最近有效销售单下单时间（不含已取消）' AFTER `total_amount`;

-- 历史数据回填：先清零再按有效订单聚合
UPDATE `customer` SET `total_orders` = 0, `total_amount` = 0, `last_order_time` = NULL WHERE `deleted` = 0;

UPDATE `customer` `c`
INNER JOIN (
  SELECT `customer_id`,
         COUNT(*) AS `cnt`,
         COALESCE(SUM(`amount`), 0) AS `samt`,
         MAX(`create_time`) AS `last_t`
  FROM `sales_order`
  WHERE `deleted` = 0
    AND (`status` IS NULL OR `status` NOT IN ('cancelled', '已取消'))
  GROUP BY `customer_id`
) `x` ON `c`.`id` = `x`.`customer_id`
SET `c`.`total_orders` = `x`.`cnt`,
    `c`.`total_amount` = `x`.`samt`,
    `c`.`last_order_time` = `x`.`last_t`;
