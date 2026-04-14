-- =============================================================================
-- 权限与角色关联初始化（与 ApplicationPermissionRegistry、前端菜单对齐）
-- 使用方式：在已有 smart_ims 库中执行；会清空 sys_role_permission、sys_permission 后重灌。
-- 注意：若业务表引用了 permission_id，请先备份；本脚本假定仅角色关联使用权限主键。
-- =============================================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

TRUNCATE TABLE `sys_role_permission`;
DELETE FROM `sys_permission`;
SET FOREIGN_KEY_CHECKS = 1;

ALTER TABLE `sys_permission` AUTO_INCREMENT = 1;

-- type: 1=菜单 2=按钮/操作；parent_id 指向一级菜单主键；path/icon 仅菜单使用
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

-- 说明：商品写接口仅认 product:add / product:edit / product:delete；菜单码 products 只用于侧栏/入口，不单独放行写操作。
-- 销售专员：保留「商品管理」菜单(products)+商品查看(product:view)，无添加/编辑/删除子权限时界面与接口均为只读。
-- inventory:records：出入库流水只读；与入库/出库操作码区分，便于仅开放稽核账号。

INSERT INTO `sys_role_permission` (`role_id`, `permission_id`) VALUES
(1, 1), (1, 2), (1, 3), (1, 4), (1, 5), (1, 6), (1, 7),
(1, 8), (1, 9), (1, 10), (1, 11), (1, 12), (1, 13), (1, 14), (1, 15),
(1, 16), (1, 17), (1, 18), (1, 19), (1, 20), (1, 21), (1, 22), (1, 23), (1, 24), (1, 25),
(1, 26), (1, 27), (1, 28), (1, 29), (1, 30), (1, 31), (1, 32), (1, 33), (1, 34), (1, 35),
-- 仓管：库存菜单 + 查看/记录/仓库/调拨/入出库/调整（与库存页各 Tab 一致）
(2, 1), (2, 5), (2, 19), (2, 35), (2, 28), (2, 20), (2, 29), (2, 30), (2, 31),
-- 销售：商品查看 + 销售订单全流程 + 客户/售后 + 付款/收货
(3, 1), (3, 2), (3, 4), (3, 8), (3, 16), (3, 17), (3, 18), (3, 26), (3, 27), (3, 32), (3, 33),
-- 采购：商品维护 + 采购/供应商/入库
(4, 1), (4, 2), (4, 3), (4, 8), (4, 9), (4, 10), (4, 11), (4, 12), (4, 13), (4, 14), (4, 15), (4, 34),
(5, 1), (5, 6), (5, 21), (5, 22);
