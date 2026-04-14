-- 供应商支持多个所属行业：新增 supplier_industry_link，迁移原 supplier.industry_id 后删除该列
-- 执行前请备份。

SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS `supplier_industry_link` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '关联主键',
  `supplier_id` BIGINT NOT NULL COMMENT '供应商ID',
  `industry_id` BIGINT NOT NULL COMMENT '行业ID',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_supplier_industry` (`supplier_id`, `industry_id`),
  KEY `idx_industry_id` (`industry_id`),
  CONSTRAINT `fk_sil_supplier` FOREIGN KEY (`supplier_id`) REFERENCES `supplier` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_sil_industry` FOREIGN KEY (`industry_id`) REFERENCES `supplier_industry` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='供应商与行业多对多关联';

-- 若存在旧列 industry_id，则迁入关联表（每供应商一条）
-- INSERT INTO supplier_industry_link (supplier_id, industry_id)
-- SELECT id, industry_id FROM supplier WHERE industry_id IS NOT NULL
-- ON DUPLICATE KEY UPDATE industry_id = VALUES(industry_id);

-- 然后删除外键与列（按实际库中约束名调整）：
-- ALTER TABLE `supplier` DROP FOREIGN KEY `fk_supplier_industry`;
-- ALTER TABLE `supplier` DROP COLUMN `industry_id`;
