/**
 * 与后端 UiRouteVO.viewKey / ApplicationPermissionRegistry 一致，供动态路由注册
 */
export const viewLoaders = {
  Dashboard: () => import('@/views/Dashboard.vue'),
  Products: () => import('@/views/Products.vue'),
  ProductView: () => import('@/views/ProductViewDetail.vue'),
  ProductAdd: () => import('@/views/ProductDetail.vue'),
  ProductEdit: () => import('@/views/ProductDetail.vue'),
  Purchase: () => import('@/views/Purchase.vue'),
  PurchaseOrderDetail: () => import('@/views/PurchaseOrderDetail.vue'),
  PurchaseOrderEdit: () => import('@/views/PurchaseOrderEdit.vue'),
  SupplierDetail: () => import('@/views/SupplierDetail.vue'),
  SupplierEdit: () => import('@/views/SupplierEdit.vue'),
  InboundDetail: () => import('@/views/InboundDetail.vue'),
  Sales: () => import('@/views/Sales.vue'),
  SalesOrderDetail: () => import('@/views/SalesOrderDetail.vue'),
  CustomerDetail: () => import('@/views/CustomerDetail.vue'),
  CustomerEdit: () => import('@/views/CustomerEdit.vue'),
  AftersalesDetail: () => import('@/views/AftersalesDetail.vue'),
  Inventory: () => import('@/views/Inventory.vue'),
  InventoryDetail: () => import('@/views/InventoryDetail.vue'),
  TransferDetail: () => import('@/views/TransferDetail.vue'),
  WarehouseDetail: () => import('@/views/WarehouseDetail.vue'),
  Settings: () => import('@/views/Settings.vue'),
  RolePermission: () => import('@/views/RolePermissionDetail.vue')
}
