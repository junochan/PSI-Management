-- 更新库存表中的商品信息使其与商品表一致
-- 使用 UPDATE 而非 DELETE/INSERT 以避免数据丢失

UPDATE `inventory` i
JOIN `product` p ON i.product_id = p.id
SET
    i.sku = p.code,
    i.product_name = p.name,
    i.spec = p.spec,
    i.category = p.category_name,
    i.cost_price = p.cost_price;

-- 更新库存状态
UPDATE `inventory` SET `status` = 'normal' WHERE `stock` >= `safe_stock`;
UPDATE `inventory` SET `status` = 'warning' WHERE `stock` < `safe_stock` AND `stock` >= `safe_stock` / 2;
UPDATE `inventory` SET `status` = 'critical' WHERE `stock` < `safe_stock` / 2 OR `stock` <= 0;

-- 更新采购订单中的商品信息
UPDATE `purchase_order` po
JOIN `product` p ON po.product_id = p.id
SET
    po.product_name = p.name,
    po.sku = p.code,
    po.unit_price = p.cost_price;

-- 更新入库记录中的商品信息
UPDATE `inbound_record` ir
JOIN `product` p ON ir.product_id = p.id
SET
    ir.product_name = p.name,
    ir.sku = p.code;

-- 更新销售订单中的商品信息
UPDATE `sales_order` so
JOIN `product` p ON so.product_id = p.id
SET
    so.product_name = p.name,
    so.sku = p.code;

-- 清空调拨记录和库存预警（避免关联不一致数据）
DELETE FROM `inventory_transfer`;
DELETE FROM `inventory_warning`;