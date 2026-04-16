import { printJson } from '../lib/util.mjs'

const PAGE_QUERY_FIELDS = [
  { name: 'page', type: 'number', required: false, desc: '页码，默认 1，最小 1' },
  { name: 'size', type: 'number', required: false, desc: '每页条数，默认 10，范围 1~100' },
  { name: 'sort', type: 'string', required: false, desc: '排序字段' },
  { name: 'order', type: 'string', required: false, desc: '排序方向，asc/desc，默认 desc' },
  { name: 'keyword', type: 'string', required: false, desc: '关键词搜索' },
  { name: 'productId', type: 'number', required: false, desc: '商品 ID 筛选' },
  { name: 'warehouseId', type: 'number', required: false, desc: '仓库 ID 筛选' },
  { name: 'customerId', type: 'number', required: false, desc: '客户 ID 筛选' },
  { name: 'supplierId', type: 'number', required: false, desc: '供应商 ID 筛选' },
  { name: 'categoryName', type: 'string', required: false, desc: '分类名称筛选' },
  { name: 'productStatus', type: 'string', required: false, desc: '商品状态筛选' },
  { name: 'stagnantStatus', type: 'string', required: false, desc: '库存呆滞状态筛选' },
  { name: 'inboundStatus', type: 'string', required: false, desc: '采购入库状态筛选' },
  { name: 'payStatus', type: 'string', required: false, desc: '支付状态筛选' },
  { name: 'salesOrderStatus', type: 'string', required: false, desc: '销售订单状态筛选' },
  { name: 'aftersalesStatus', type: 'string', required: false, desc: '售后状态筛选' },
  { name: 'lastOutboundStart', type: 'string', required: false, desc: '最后出库开始日期 yyyy-MM-dd' },
  { name: 'lastOutboundEnd', type: 'string', required: false, desc: '最后出库结束日期 yyyy-MM-dd' },
  { name: 'lastInboundStart', type: 'string', required: false, desc: '最后入库开始日期 yyyy-MM-dd' },
  { name: 'lastInboundEnd', type: 'string', required: false, desc: '最后入库结束日期 yyyy-MM-dd' },
  { name: 'expectDateStart', type: 'string', required: false, desc: '期望交货开始日期 yyyy-MM-dd' },
  { name: 'expectDateEnd', type: 'string', required: false, desc: '期望交货结束日期 yyyy-MM-dd' },
  { name: 'createTimeStart', type: 'string', required: false, desc: '创建时间开始日期 yyyy-MM-dd' },
  { name: 'createTimeEnd', type: 'string', required: false, desc: '创建时间结束日期 yyyy-MM-dd' },
  { name: 'operatorName', type: 'string', required: false, desc: '操作人（入库记录）' }
]

const SPECS = [
  { command: 'auth login', method: 'POST', path: '/auth/login', body: [
    { name: 'username', type: 'string', required: true, desc: '用户名' },
    { name: 'password', type: 'string', required: true, desc: '密码' },
    { name: 'remember', type: 'boolean', required: false, desc: '记住登录，默认 false' }
  ] },
  { command: 'auth logout', method: 'POST', path: '/auth/logout' },
  { command: 'auth navigation', method: 'GET', path: '/auth/navigation' },
  { command: 'auth sso-login', method: 'POST', path: '/auth/sso-login', body: [
    { name: 'key', type: 'string', required: true, desc: 'SSO 共享密钥' }
  ] },
  { command: 'auth change-password', method: 'POST', path: '/auth/change-password', body: [
    { name: 'currentPassword', type: 'string', required: true, desc: '当前密码' },
    { name: 'newPassword', type: 'string', required: true, desc: '新密码，至少 6 位' }
  ] },
  { command: 'dashboard overview', method: 'GET', path: '/dashboard/overview', query: [
    { name: 'days', type: 'number', required: false, desc: '统计天数，仅 7/30/90，默认 7' }
  ] },

  { command: 'products list', method: 'GET', path: '/products', query: PAGE_QUERY_FIELDS },
  { command: 'products get', method: 'GET', path: '/products/{id}', pathParams: [{ name: 'id', type: 'number', required: true, desc: '商品 ID' }] },
  { command: 'products create', method: 'POST', path: '/products', body: [
    { name: 'code', type: 'string', required: false, desc: '商品编码，通常由系统生成' },
    { name: 'name', type: 'string', required: true, desc: '商品名称' },
    { name: 'brand', type: 'string', required: false, desc: '品牌' },
    { name: 'spec', type: 'string', required: false, desc: '规格' },
    { name: 'categoryId', type: 'number', required: false, desc: '分类 ID' },
    { name: 'categoryName', type: 'string', required: false, desc: '分类名称' },
    { name: 'costPrice', type: 'number', required: true, desc: '成本价' },
    { name: 'salePrice', type: 'number', required: true, desc: '销售价' },
    { name: 'status', type: 'string', required: false, desc: '状态' },
    { name: 'image', type: 'string', required: false, desc: '商品图片 URL，可逗号拼接多图' },
    { name: 'description', type: 'string', required: false, desc: '描述' },
    { name: 'safeStock', type: 'number', required: false, desc: '安全库存' },
    { name: 'initialStock', type: 'number', required: false, desc: '初始库存，仅新建时使用' },
    { name: 'warehouseId', type: 'number', required: false, desc: '初始入库仓库 ID，仅新建时使用' }
  ] },
  { command: 'products update', method: 'PUT', path: '/products/{id}', pathParams: [{ name: 'id', type: 'number', required: true, desc: '商品 ID' }], body: '同 products create' },
  { command: 'products delete', method: 'DELETE', path: '/products/{id}', pathParams: [{ name: 'id', type: 'number', required: true, desc: '商品 ID' }] },
  { command: 'products batch-delete', method: 'DELETE', path: '/products/batch', body: [{ name: 'ids', type: 'number[]', required: true, desc: '商品 ID 数组' }] },
  { command: 'products import-excel', method: 'POST', path: '/products/import', files: [{ name: 'file', required: true, desc: 'Excel 文件' }] },
  { command: 'products import-task', method: 'GET', path: '/products/import/{jobId}', pathParams: [{ name: 'jobId', type: 'string', required: true, desc: '异步导入任务 ID' }] },
  { command: 'products upload-image', method: 'POST', path: '/products/image', files: [{ name: 'file', required: true, desc: '图片文件' }] },
  { command: 'products search-image', method: 'POST', path: '/products/search-by-image', body: [
    { name: 'page', type: 'number', required: false, desc: '页码，默认 1' },
    { name: 'size', type: 'number', required: false, desc: '每页条数，默认 10，范围 1~100' },
    { name: 'keyword', type: 'string', required: false, desc: '关键词' },
    { name: 'categoryName', type: 'string', required: false, desc: '分类名称' },
    { name: 'status', type: 'string', required: false, desc: '商品状态' },
    { name: 'imageBase64', type: 'string', required: true, desc: '查询图片 base64' },
    { name: 'similarityThreshold', type: 'number', required: false, desc: '相似度阈值，0~1' }
  ] },

  { command: 'categories list', method: 'GET', path: '/categories' },
  { command: 'categories get', method: 'GET', path: '/categories/{id}', pathParams: [{ name: 'id', type: 'number', required: true, desc: '分类 ID' }] },
  { command: 'categories create', method: 'POST', path: '/categories', body: [
    { name: 'name', type: 'string', required: false, desc: '分类名称' },
    { name: 'code', type: 'string', required: false, desc: '分类编码' },
    { name: 'parentId', type: 'number', required: false, desc: '父分类 ID' },
    { name: 'sort', type: 'number', required: false, desc: '排序' },
    { name: 'status', type: 'number', required: false, desc: '状态' }
  ] },
  { command: 'categories update', method: 'PUT', path: '/categories/{id}', pathParams: [{ name: 'id', type: 'number', required: true, desc: '分类 ID' }], body: '同 categories create' },
  { command: 'categories delete', method: 'DELETE', path: '/categories/{id}', pathParams: [{ name: 'id', type: 'number', required: true, desc: '分类 ID' }] },

  { command: 'suppliers list', method: 'GET', path: '/suppliers', query: PAGE_QUERY_FIELDS },
  { command: 'suppliers get', method: 'GET', path: '/suppliers/{id}', pathParams: [{ name: 'id', type: 'number', required: true, desc: '供应商 ID' }] },
  { command: 'suppliers create', method: 'POST', path: '/suppliers', body: [
    { name: 'name', type: 'string', required: true, desc: '供应商名称' },
    { name: 'code', type: 'string', required: false, desc: '供应商编码' },
    { name: 'industryIds', type: 'number[]', required: false, desc: '行业 ID 列表' },
    { name: 'contactPerson', type: 'string', required: false, desc: '联系人' },
    { name: 'contactPhone', type: 'string', required: false, desc: '联系电话' },
    { name: 'email', type: 'string', required: false, desc: '邮箱' },
    { name: 'address', type: 'string', required: false, desc: '地址' },
    { name: 'bankName', type: 'string', required: false, desc: '开户行' },
    { name: 'bankAccount', type: 'string', required: false, desc: '银行账号' },
    { name: 'taxNo', type: 'string', required: false, desc: '税号' },
    { name: 'remark', type: 'string', required: false, desc: '备注' }
  ] },
  { command: 'suppliers update', method: 'PUT', path: '/suppliers/{id}', pathParams: [{ name: 'id', type: 'number', required: true, desc: '供应商 ID' }], body: '同 suppliers create' },
  { command: 'suppliers delete', method: 'DELETE', path: '/suppliers/{id}', pathParams: [{ name: 'id', type: 'number', required: true, desc: '供应商 ID' }] },
  { command: 'suppliers batch-delete', method: 'DELETE', path: '/suppliers/batch', body: [{ name: 'ids', type: 'number[]', required: true, desc: '供应商 ID 数组' }] },

  { command: 'customers list', method: 'GET', path: '/customers', query: PAGE_QUERY_FIELDS },
  { command: 'customers get', method: 'GET', path: '/customers/{id}', pathParams: [{ name: 'id', type: 'number', required: true, desc: '客户 ID' }] },
  { command: 'customers create', method: 'POST', path: '/customers', body: [
    { name: 'name', type: 'string', required: true, desc: '客户名称（服务层校验）' },
    { name: 'code', type: 'string', required: false, desc: '客户编码' },
    { name: 'type', type: 'string', required: false, desc: '客户类型' },
    { name: 'contactPerson', type: 'string', required: false, desc: '联系人（兼容别名 contact）' },
    { name: 'contactPhone', type: 'string', required: false, desc: '联系电话（兼容别名 phone）' },
    { name: 'email', type: 'string', required: false, desc: '邮箱' },
    { name: 'address', type: 'string', required: false, desc: '地址' },
    { name: 'vipLevel', type: 'string', required: false, desc: 'VIP 等级' },
    { name: 'remark', type: 'string', required: false, desc: '备注' }
  ] },
  { command: 'customers update', method: 'PUT', path: '/customers/{id}', pathParams: [{ name: 'id', type: 'number', required: true, desc: '客户 ID' }], body: '同 customers create' },
  { command: 'customers delete', method: 'DELETE', path: '/customers/{id}', pathParams: [{ name: 'id', type: 'number', required: true, desc: '客户 ID' }] },
  { command: 'customers batch-delete', method: 'DELETE', path: '/customers/batch', body: [{ name: 'ids', type: 'number[]', required: true, desc: '客户 ID 数组' }] },

  { command: 'warehouses options', method: 'GET', path: '/warehouses/options' },
  { command: 'warehouses list', method: 'GET', path: '/warehouses', query: PAGE_QUERY_FIELDS },
  { command: 'warehouses all', method: 'GET', path: '/warehouses/all' },
  { command: 'warehouses get', method: 'GET', path: '/warehouses/{id}', pathParams: [{ name: 'id', type: 'number', required: true, desc: '仓库 ID' }] },
  { command: 'warehouses create', method: 'POST', path: '/warehouses', body: [
    { name: 'name', type: 'string', required: true, desc: '仓库名称' },
    { name: 'code', type: 'string', required: false, desc: '仓库编码' },
    { name: 'address', type: 'string', required: false, desc: '地址' },
    { name: 'managerName', type: 'string', required: false, desc: '管理员姓名' },
    { name: 'managerPhone', type: 'string', required: false, desc: '管理员电话' },
    { name: 'capacity', type: 'number', required: false, desc: '容量' },
    { name: 'capacityUsed', type: 'number', required: false, desc: '已使用容量' },
    { name: 'remark', type: 'string', required: false, desc: '备注' }
  ] },
  { command: 'warehouses update', method: 'PUT', path: '/warehouses/{id}', pathParams: [{ name: 'id', type: 'number', required: true, desc: '仓库 ID' }], body: '同 warehouses create' },
  { command: 'warehouses delete', method: 'DELETE', path: '/warehouses/{id}', pathParams: [{ name: 'id', type: 'number', required: true, desc: '仓库 ID' }] },
  { command: 'warehouses batch-delete', method: 'DELETE', path: '/warehouses/batch', body: [{ name: 'ids', type: 'number[]', required: true, desc: '仓库 ID 数组' }] },
  { command: 'supplier-industries', method: 'GET', path: '/supplier-industries' },

  { command: 'purchase orders list', method: 'GET', path: '/purchase/orders', query: PAGE_QUERY_FIELDS },
  { command: 'purchase orders get', method: 'GET', path: '/purchase/orders/{id}', pathParams: [{ name: 'id', type: 'number', required: true, desc: '采购订单 ID' }] },
  { command: 'purchase orders create', method: 'POST', path: '/purchase/orders', body: [
    { name: 'supplierId', type: 'number', required: true, desc: '供应商 ID' },
    { name: 'productId', type: 'number', required: true, desc: '商品 ID' },
    { name: 'quantity', type: 'number', required: true, desc: '采购数量，最小 1' },
    { name: 'unitPrice', type: 'number', required: true, desc: '单价' },
    { name: 'expectDate', type: 'string', required: false, desc: '期望交付日期，yyyy-MM-dd' },
    { name: 'warehouseId', type: 'number', required: false, desc: '仓库 ID' },
    { name: 'payMethod', type: 'string', required: false, desc: '付款方式' },
    { name: 'remark', type: 'string', required: false, desc: '备注' }
  ] },
  { command: 'purchase orders update', method: 'PUT', path: '/purchase/orders/{id}', pathParams: [{ name: 'id', type: 'number', required: true, desc: '采购订单 ID' }], body: '同 purchase orders create' },
  { command: 'purchase orders cancel', method: 'PUT', path: '/purchase/orders/{id}/cancel', pathParams: [{ name: 'id', type: 'number', required: true, desc: '采购订单 ID' }] },
  { command: 'purchase orders delete', method: 'DELETE', path: '/purchase/orders/{id}', pathParams: [{ name: 'id', type: 'number', required: true, desc: '采购订单 ID' }] },
  { command: 'purchase orders inbound', method: 'POST', path: '/purchase/orders/{id}/inbound', pathParams: [{ name: 'id', type: 'number', required: true, desc: '采购订单 ID' }], body: [
    { name: 'warehouseId', type: 'number', required: true, desc: '入库仓库 ID' },
    { name: 'quantity', type: 'number', required: true, desc: '入库数量，最小 1' },
    { name: 'batchNo', type: 'string', required: false, desc: '批次号' },
    { name: 'remark', type: 'string', required: false, desc: '备注' }
  ] },
  { command: 'purchase inbounds list', method: 'GET', path: '/purchase/inbounds', query: PAGE_QUERY_FIELDS },
  { command: 'purchase inbounds get', method: 'GET', path: '/purchase/inbounds/{id}', pathParams: [{ name: 'id', type: 'number', required: true, desc: '入库记录 ID' }] },
  { command: 'purchase stats', method: 'GET', path: '/purchase/stats' },

  { command: 'sales orders list', method: 'GET', path: '/sales/orders', query: PAGE_QUERY_FIELDS },
  { command: 'sales orders get', method: 'GET', path: '/sales/orders/{id}', pathParams: [{ name: 'id', type: 'number', required: true, desc: '销售订单 ID' }] },
  { command: 'sales orders create', method: 'POST', path: '/sales/orders', body: [
    { name: 'customerId', type: 'number', required: true, desc: '客户 ID' },
    { name: 'productId', type: 'number', required: true, desc: '商品 ID' },
    { name: 'quantity', type: 'number', required: true, desc: '销售数量，最小 1' },
    { name: 'unitPrice', type: 'number', required: true, desc: '单价' },
    { name: 'warehouseId', type: 'number', required: false, desc: '仓库 ID' },
    { name: 'payMethod', type: 'string', required: false, desc: '付款方式' },
    { name: 'receiverName', type: 'string', required: false, desc: '收货人' },
    { name: 'receiverPhone', type: 'string', required: false, desc: '收货电话' },
    { name: 'receiverAddress', type: 'string', required: false, desc: '收货地址' },
    { name: 'remark', type: 'string', required: false, desc: '备注' }
  ] },
  { command: 'sales orders update', method: 'PUT', path: '/sales/orders/{id}', pathParams: [{ name: 'id', type: 'number', required: true, desc: '销售订单 ID' }], body: '同 sales orders create' },
  { command: 'sales orders payment', method: 'PUT', path: '/sales/orders/{id}/payment', pathParams: [{ name: 'id', type: 'number', required: true, desc: '销售订单 ID' }] },
  { command: 'sales orders shipping', method: 'POST', path: '/sales/orders/{id}/shipping', pathParams: [{ name: 'id', type: 'number', required: true, desc: '销售订单 ID' }], body: [
    { name: 'warehouseId', type: 'number', required: true, desc: '发货仓库 ID' },
    { name: 'quantity', type: 'number', required: true, desc: '发货数量' },
    { name: 'logisticsCompany', type: 'string', required: false, desc: '物流公司' },
    { name: 'logisticsNo', type: 'string', required: false, desc: '物流单号' },
    { name: 'receiverName', type: 'string', required: false, desc: '收货人' },
    { name: 'receiverPhone', type: 'string', required: false, desc: '收货电话' },
    { name: 'receiverAddress', type: 'string', required: false, desc: '收货地址' },
    { name: 'remark', type: 'string', required: false, desc: '备注' }
  ] },
  { command: 'sales orders received', method: 'PUT', path: '/sales/orders/{id}/received', pathParams: [{ name: 'id', type: 'number', required: true, desc: '销售订单 ID' }] },
  { command: 'sales orders cancel', method: 'PUT', path: '/sales/orders/{id}/cancel', pathParams: [{ name: 'id', type: 'number', required: true, desc: '销售订单 ID' }] },
  { command: 'sales stats', method: 'GET', path: '/sales/stats' },

  { command: 'inventory list', method: 'GET', path: '/inventory', query: PAGE_QUERY_FIELDS },
  { command: 'inventory get', method: 'GET', path: '/inventory/{id}', pathParams: [{ name: 'id', type: 'number', required: true, desc: '库存记录 ID' }] },
  { command: 'inventory by-product', method: 'GET', path: '/inventory/product/{productId}', pathParams: [{ name: 'productId', type: 'number', required: true, desc: '商品 ID' }] },
  { command: 'inventory search-image', method: 'POST', path: '/inventory/search-by-image', body: [
    { name: 'page', type: 'number', required: false, desc: '页码，默认 1' },
    { name: 'size', type: 'number', required: false, desc: '每页条数，默认 10，范围 1~100' },
    { name: 'keyword', type: 'string', required: false, desc: '关键词' },
    { name: 'productId', type: 'number', required: false, desc: '商品 ID' },
    { name: 'warehouseId', type: 'number', required: false, desc: '仓库 ID' },
    { name: 'stagnantStatus', type: 'string', required: false, desc: '呆滞状态：stagnant/normal' },
    { name: 'lastOutboundStart', type: 'string', required: false, desc: '最后出库开始日期 yyyy-MM-dd' },
    { name: 'lastOutboundEnd', type: 'string', required: false, desc: '最后出库结束日期 yyyy-MM-dd' },
    { name: 'lastInboundStart', type: 'string', required: false, desc: '最后入库开始日期 yyyy-MM-dd' },
    { name: 'lastInboundEnd', type: 'string', required: false, desc: '最后入库结束日期 yyyy-MM-dd' },
    { name: 'imageBase64', type: 'string', required: true, desc: '查询图片 base64' },
    { name: 'similarityThreshold', type: 'number', required: false, desc: '相似度阈值，0~1' }
  ] },
  { command: 'inventory stats', method: 'GET', path: '/inventory/stats' },
  { command: 'inventory transfers list', method: 'GET', path: '/inventory/transfers', query: PAGE_QUERY_FIELDS },
  { command: 'inventory transfers create', method: 'POST', path: '/inventory/transfers', body: [
    { name: 'productId', type: 'number', required: true, desc: '商品 ID' },
    { name: 'fromWarehouseId', type: 'number', required: true, desc: '源仓库 ID' },
    { name: 'toWarehouseId', type: 'number', required: true, desc: '目标仓库 ID' },
    { name: 'quantity', type: 'number', required: true, desc: '调拨数量，最小 1' },
    { name: 'remark', type: 'string', required: false, desc: '备注' }
  ] },
  { command: 'inventory transfers confirm', method: 'PUT', path: '/inventory/transfers/{id}/confirm', pathParams: [{ name: 'id', type: 'number', required: true, desc: '调拨单 ID' }] },
  { command: 'inventory warnings list', method: 'GET', path: '/inventory/warnings', query: PAGE_QUERY_FIELDS },
  { command: 'inventory warnings handle', method: 'PUT', path: '/inventory/warnings/{id}/handle', pathParams: [{ name: 'id', type: 'number', required: true, desc: '预警记录 ID' }], query: [{ name: 'handleRemark', type: 'string', required: true, desc: '处理备注' }] },
  { command: 'inventory inbound-simple', method: 'POST', path: '/inventory/inbound', query: [
    { name: 'inventoryId', type: 'number', required: true, desc: '库存记录 ID' },
    { name: 'quantity', type: 'number', required: true, desc: '数量' },
    { name: 'remark', type: 'string', required: false, desc: '备注' }
  ] },
  { command: 'inventory outbound-simple', method: 'POST', path: '/inventory/outbound', query: [
    { name: 'inventoryId', type: 'number', required: true, desc: '库存记录 ID' },
    { name: 'quantity', type: 'number', required: true, desc: '数量' },
    { name: 'remark', type: 'string', required: false, desc: '备注' }
  ] },
  { command: 'inventory inbound-new', method: 'POST', path: '/inventory/inbound/new', body: [
    { name: 'productId', type: 'number', required: true, desc: '商品 ID' },
    { name: 'warehouseId', type: 'number', required: true, desc: '仓库 ID' },
    { name: 'quantity', type: 'number', required: true, desc: '入库数量' },
    { name: 'remark', type: 'string', required: false, desc: '备注' }
  ] },
  { command: 'inventory outbounds-list', method: 'GET', path: '/inventory/outbounds', query: PAGE_QUERY_FIELDS },
  { command: 'inventory set-safe-stock', method: 'PUT', path: '/inventory/{id}/safe-stock', pathParams: [{ name: 'id', type: 'number', required: true, desc: '库存记录 ID' }], query: [{ name: 'safeStock', type: 'number', required: true, desc: '安全库存' }] },
  { command: 'inventory set-location', method: 'PUT', path: '/inventory/{id}/location', pathParams: [{ name: 'id', type: 'number', required: true, desc: '库存记录 ID' }], query: [{ name: 'location', type: 'string', required: true, desc: '库位' }] },
  { command: 'inventory set-stagnant-days', method: 'PUT', path: '/inventory/{id}/stagnant-days', pathParams: [{ name: 'id', type: 'number', required: true, desc: '库存记录 ID' }], query: [{ name: 'stagnantDays', type: 'number', required: true, desc: '呆滞预警天数' }] },

  { command: 'aftersales list', method: 'GET', path: '/aftersales', query: PAGE_QUERY_FIELDS },
  { command: 'aftersales get', method: 'GET', path: '/aftersales/{id}', pathParams: [{ name: 'id', type: 'number', required: true, desc: '售后工单 ID' }] },
  { command: 'aftersales create', method: 'POST', path: '/aftersales', body: [
    { name: 'salesOrderId', type: 'number', required: true, desc: '销售订单 ID' },
    { name: 'type', type: 'string', required: true, desc: '售后类型' },
    { name: 'content', type: 'string', required: true, desc: '售后内容' },
    { name: 'expectHandle', type: 'string', required: false, desc: '期望处理方式' },
    { name: 'refundAmount', type: 'number', required: false, desc: '退款金额' },
    { name: 'remark', type: 'string', required: false, desc: '备注' }
  ] },
  { command: 'aftersales handle', method: 'PUT', path: '/aftersales/{id}/handle', pathParams: [{ name: 'id', type: 'number', required: true, desc: '售后工单 ID' }], body: [
    { name: 'handleResult', type: 'string', required: true, desc: '处理结果' },
    { name: 'refundAmount', type: 'number', required: false, desc: '退款金额' },
    { name: 'remark', type: 'string', required: false, desc: '备注' }
  ] },
  { command: 'aftersales close', method: 'PUT', path: '/aftersales/{id}/close', pathParams: [{ name: 'id', type: 'number', required: true, desc: '售后工单 ID' }] },

  { command: 'users list', method: 'GET', path: '/users', query: PAGE_QUERY_FIELDS },
  { command: 'users all', method: 'GET', path: '/users/all' },
  { command: 'users get', method: 'GET', path: '/users/{id}', pathParams: [{ name: 'id', type: 'number', required: true, desc: '用户 ID' }] },
  { command: 'users create', method: 'POST', path: '/users', body: [
    { name: 'username', type: 'string', required: true, desc: '登录名' },
    { name: 'name', type: 'string', required: true, desc: '姓名' },
    { name: 'email', type: 'string', required: false, desc: '邮箱' },
    { name: 'phone', type: 'string', required: false, desc: '手机号' },
    { name: 'roleId', type: 'number', required: false, desc: '角色 ID' },
    { name: 'password', type: 'string', required: false, desc: '密码（通常创建时提供）' },
    { name: 'status', type: 'number', required: false, desc: '状态：1 启用，0 禁用' },
    { name: 'avatar', type: 'string', required: false, desc: '头像 URL（可由上传接口返回）' }
  ] },
  { command: 'users update', method: 'PUT', path: '/users/{id}', pathParams: [{ name: 'id', type: 'number', required: true, desc: '用户 ID' }], body: '同 users create' },
  { command: 'users delete', method: 'DELETE', path: '/users/{id}', pathParams: [{ name: 'id', type: 'number', required: true, desc: '用户 ID' }] },
  { command: 'users upload-avatar', method: 'POST', path: '/users/avatar', files: [{ name: 'file', required: true, desc: '头像图片文件' }] },

  { command: 'roles list', method: 'GET', path: '/roles' },
  { command: 'roles get', method: 'GET', path: '/roles/{id}', pathParams: [{ name: 'id', type: 'number', required: true, desc: '角色 ID' }] },
  { command: 'roles create', method: 'POST', path: '/roles', body: [
    { name: 'name', type: 'string', required: false, desc: '角色名称' },
    { name: 'code', type: 'string', required: false, desc: '角色编码' },
    { name: 'description', type: 'string', required: false, desc: '角色描述' },
    { name: 'status', type: 'number', required: false, desc: '状态' }
  ] },
  { command: 'roles update', method: 'PUT', path: '/roles/{id}', pathParams: [{ name: 'id', type: 'number', required: true, desc: '角色 ID' }], body: '同 roles create' },
  { command: 'roles delete', method: 'DELETE', path: '/roles/{id}', pathParams: [{ name: 'id', type: 'number', required: true, desc: '角色 ID' }] },
  { command: 'roles permissions', method: 'GET', path: '/roles/{id}/permissions', pathParams: [{ name: 'id', type: 'number', required: true, desc: '角色 ID' }] },
  { command: 'roles permissions-all', method: 'GET', path: '/roles/permissions' },
  { command: 'roles set-permissions', method: 'PUT', path: '/roles/{id}/permissions', pathParams: [{ name: 'id', type: 'number', required: true, desc: '角色 ID' }], body: [{ name: 'permissionIds', type: 'number[]', required: true, desc: '权限 ID 数组' }] },

  { command: 'logs', method: 'GET', path: '/logs', query: PAGE_QUERY_FIELDS },
  { command: 'util-encode', method: 'GET', path: '/util/encode', query: [{ name: 'password', type: 'string', required: true, desc: '明文密码' }] }
]

function normalizeCommand (input) {
  return String(input || '').trim().replace(/\s+/g, ' ').toLowerCase()
}

function prettySpec (spec) {
  return {
    command: spec.command,
    endpoint: `${spec.method} ${spec.path}`,
    pathParams: spec.pathParams || [],
    query: spec.query || [],
    body: spec.body || [],
    files: spec.files || []
  }
}

export function registerSpec (program) {
  const spec = program.command('spec').description('查询 CLI 命令对应接口与完整参数')

  spec
    .command('list')
    .description('列出所有命令与接口')
    .option('-q, --query <text>', '按命令或路径筛选')
    .action((opts) => {
      const q = normalizeCommand(opts.query)
      const rows = SPECS
        .filter((s) => !q || s.command.includes(q) || s.path.includes(q))
        .map((s) => ({ command: s.command, endpoint: `${s.method} ${s.path}` }))
      printJson(rows)
    })

  spec
    .command('show <command...>')
    .description('查看指定命令对应接口参数，如: psims spec show products create')
    .action((parts) => {
      const cmd = normalizeCommand(Array.isArray(parts) ? parts.join(' ') : parts)
      const hit = SPECS.find((s) => s.command === cmd)
      if (!hit) {
        throw new Error(`未找到命令规范: ${cmd}`)
      }
      printJson(prettySpec(hit))
    })
}
