-- 已有库升级：新增 supplier_industry，并将 supplier.industry 文本改为 industry_id
-- 执行前请备份。若你的 supplier 表已无 `industry` 列且已有 `industry_id`，请跳过对应步骤。

SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS `supplier_industry` (
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

INSERT IGNORE INTO `supplier_industry` (`id`, `name`, `code`, `sort`, `status`, `deleted`) VALUES
(1, '童装零售', 'KIDS_RETAIL', 10, 1, 0),
(2, '儿童服饰制造 / 生产', 'KIDS_MFG', 20, 1, 0),
(3, '服装批发', 'APPAREL_WHOLESALE', 30, 1, 0),
(4, '母婴用品零售', 'MOTHER_BABY', 40, 1, 0),
(5, '鞋帽零售', 'SHOES_HATS', 50, 1, 0);

-- 以下三步请按实际表结构执行（若列已存在会报错，可忽略或注释掉对应行）
-- 1) 增加外键列
-- ALTER TABLE `supplier` ADD COLUMN `industry_id` BIGINT DEFAULT NULL COMMENT '所属行业ID' AFTER `contact`, ADD KEY `idx_industry_id` (`industry_id`);

-- 2) 从旧文本列迁移（仅当仍存在 `industry` 列时执行）
-- UPDATE `supplier` s INNER JOIN `supplier_industry` i ON i.`name` = s.`industry` AND i.`deleted` = 0 SET s.`industry_id` = i.`id` WHERE s.`industry` IS NOT NULL AND TRIM(s.`industry`) <> '';

-- 3) 删除旧列
-- ALTER TABLE `supplier` DROP COLUMN `industry`;

-- 4) 外键（可选）
-- ALTER TABLE `supplier` ADD CONSTRAINT `fk_supplier_industry` FOREIGN KEY (`industry_id`) REFERENCES `supplier_industry` (`id`) ON DELETE SET NULL ON UPDATE CASCADE;
