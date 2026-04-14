-- Add stock and safe_stock columns to product table
-- Migration: Add stock fields to product

ALTER TABLE `product`
ADD COLUMN `stock` INT DEFAULT 0 COMMENT '库存数量' AFTER `description`,
ADD COLUMN `safe_stock` INT DEFAULT 10 COMMENT '安全库存预警值' AFTER `stock`;

-- Update existing products with default stock values
UPDATE `product` SET `stock` = 0, `safe_stock` = 10 WHERE `stock` IS NULL;