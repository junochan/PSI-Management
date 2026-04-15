import axios from 'axios'
import { ElMessage } from 'element-plus'

// API 基础配置
const api = axios.create({
  baseURL: '/api/v1',
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json'
  }
})

// 请求拦截器 - 添加 token
api.interceptors.request.use(
  config => {
    const token = localStorage.getItem('token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  error => Promise.reject(error)
)

// 响应拦截器 - 处理错误
api.interceptors.response.use(
  response => {
    const res = response.data
    if (res.code !== 200) {
      return Promise.reject(new Error(res.message || '请求失败'))
    }
    return res.data
  },
  error => {
    if (error.response?.status === 401) {
      localStorage.removeItem('token')
      window.location.href = '/login'
    } else if (error.response?.status === 403) {
      const msg = error.response?.data?.message
      if (typeof msg === 'string' && msg) ElMessage.warning(msg)
    }
    return Promise.reject(error)
  }
)

// 上传专用（不设默认 JSON Content-Type，便于 multipart 自动带 boundary）
const uploadApi = axios.create({
  baseURL: '/api/v1',
  timeout: 120000
})
uploadApi.interceptors.request.use(
  config => {
    const token = localStorage.getItem('token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  error => Promise.reject(error)
)
uploadApi.interceptors.response.use(
  response => {
    const res = response.data
    if (res.code !== 200) {
      return Promise.reject(new Error(res.message || '请求失败'))
    }
    return res.data
  },
  error => {
    if (error.response?.status === 401) {
      localStorage.removeItem('token')
      window.location.href = '/login'
    } else if (error.response?.status === 403) {
      const msg = error.response?.data?.message
      if (typeof msg === 'string' && msg) ElMessage.warning(msg)
    }
    return Promise.reject(error)
  }
)

// 商品 API
export const productApi = {
  list: (params) => api.get('/products', { params }),
  get: (id) => api.get(`/products/${id}`),
  create: (data) => api.post('/products', data),
  update: (id, data) => api.put(`/products/${id}`, data),
  delete: (id) => api.delete(`/products/${id}`),
  batchDelete: (ids) => api.delete('/products/batch', { data: ids }),
  uploadImage: (formData) => uploadApi.post('/products/image', formData),
  /** Excel 异步批量导入（multipart，返回 jobId） */
  importProducts: (formData) =>
    uploadApi.post('/products/import', formData, { timeout: 120000 }),
  /** 查询异步导入任务进度 */
  getImportTask: (jobId) => api.get(`/products/import/${jobId}`),
  /** 以图搜图（百炼向量，可能较慢） */
  searchByImage: (data) => api.post('/products/search-by-image', data, { timeout: 120000 })
}

// 供应商 API
export const supplierApi = {
  list: (params) => api.get('/suppliers', { params }),
  get: (id) => api.get(`/suppliers/${id}`),
  create: (data) => api.post('/suppliers', data),
  update: (id, data) => api.put(`/suppliers/${id}`, data),
  delete: (id) => api.delete(`/suppliers/${id}`)
}

/** 供应商所属行业字典（下拉） */
export const supplierIndustryApi = {
  list: () => api.get('/supplier-industries')
}

// 客户 API
export const customerApi = {
  list: (params) => api.get('/customers', { params }),
  get: (id) => api.get(`/customers/${id}`),
  create: (data) => api.post('/customers', data),
  update: (id, data) => api.put(`/customers/${id}`, data),
  delete: (id) => api.delete(`/customers/${id}`)
}

// 仓库 API
export const warehouseApi = {
  /** 下拉用 id/编码/名称（无统计） */
  options: () => api.get('/warehouses/options'),
  list: (params) => api.get('/warehouses', { params }),
  all: () => api.get('/warehouses/all'),
  get: (id) => api.get(`/warehouses/${id}`),
  create: (data) => api.post('/warehouses', data),
  update: (id, data) => api.put(`/warehouses/${id}`, data),
  delete: (id) => api.delete(`/warehouses/${id}`)
}

// 采购订单 API
export const purchaseApi = {
  list: (params) => api.get('/purchase/orders', { params }),
  get: (id) => api.get(`/purchase/orders/${id}`),
  create: (data) => api.post('/purchase/orders', data),
  update: (id, data) => api.put(`/purchase/orders/${id}`, data),
  cancel: (id) => api.put(`/purchase/orders/${id}/cancel`),
  delete: (id) => api.delete(`/purchase/orders/${id}`),
  inbound: (id, data) => api.post(`/purchase/orders/${id}/inbound`, data),
  inboundList: (params) => api.get('/purchase/inbounds', { params }),
  inboundGet: (id) => api.get(`/purchase/inbounds/${id}`),
  stats: () => api.get('/purchase/stats')
}

// 销售订单 API
export const salesApi = {
  list: (params) => api.get('/sales/orders', { params }),
  get: (id) => api.get(`/sales/orders/${id}`),
  create: (data) => api.post('/sales/orders', data),
  update: (id, data) => api.put(`/sales/orders/${id}`, data),
  payment: (id) => api.put(`/sales/orders/${id}/payment`),
  shipping: (id, data) => api.post(`/sales/orders/${id}/shipping`, data),
  shippingList: (params) => api.get('/sales/shipping', { params }),
  received: (id) => api.put(`/sales/orders/${id}/received`),
  cancel: (id) => api.put(`/sales/orders/${id}/cancel`),
  stats: () => api.get('/sales/stats')
}

// 库存 API
export const inventoryApi = {
  list: (params) => api.get('/inventory', { params }),
  get: (id) => api.get(`/inventory/${id}`),
  byProduct: (productId) => api.get(`/inventory/product/${productId}`),
  createTransfer: (data) => api.post('/inventory/transfers', data),
  confirmTransfer: (id) => api.put(`/inventory/transfers/${id}/confirm`),
  transferList: (params) => api.get('/inventory/transfers', { params }),
  warningList: (params) => api.get('/inventory/warnings', { params }),
  handleWarning: (id, remark) => api.put(`/inventory/warnings/${id}/handle`, null, { params: { handleRemark: remark } }),
  stats: () => api.get('/inventory/stats'),
  inbound: (inventoryId, quantity, remark) => api.post('/inventory/inbound', null, { params: { inventoryId, quantity, remark } }),
  outbound: (inventoryId, quantity, remark) => api.post('/inventory/outbound', null, { params: { inventoryId, quantity, remark } }),
  inboundNew: (data) => api.post('/inventory/inbound/new', data),
  outboundList: (params) => api.get('/inventory/outbounds', { params }),
  updateSafeStock: (inventoryId, safeStock) => api.put(`/inventory/${inventoryId}/safe-stock`, null, { params: { safeStock } }),
  updateLocation: (inventoryId, location) => api.put(`/inventory/${inventoryId}/location`, null, { params: { location } }),
  updateStagnantDays: (inventoryId, stagnantDays) => api.put(`/inventory/${inventoryId}/stagnant-days`, null, { params: { stagnantDays } }),
  /** 以图搜图（百炼向量，可能较慢） */
  searchByImage: (data) => api.post('/inventory/search-by-image', data, { timeout: 120000 })
}

// 售后 API
export const aftersalesApi = {
  list: (params) => api.get('/aftersales', { params }),
  get: (id) => api.get(`/aftersales/${id}`),
  create: (data) => api.post('/aftersales', data),
  handle: (id, data) => api.put(`/aftersales/${id}/handle`, data),
  close: (id) => api.put(`/aftersales/${id}/close`)
}

// 认证 API
export const authApi = {
  login: (data) => api.post('/auth/login', data),
  /** 中转页：body { key } 与后端 app.sso-bypass.secret 一致 */
  ssoLogin: (data) => api.post('/auth/sso-login', data),
  logout: () => api.post('/auth/logout'),
  /** 菜单树、权限码、前端路由表（需登录） */
  navigation: () => api.get('/auth/navigation'),
  /** 当前用户修改密码（需登录，成功后建议重新登录） */
  changePassword: (data) => api.post('/auth/change-password', data)
}

// 仪表盘 API（聚合真实统计数据）
export const dashboardApi = {
  overview: (days) => api.get('/dashboard/overview', { params: { days } })
}

// 用户管理 API
export const userApi = {
  list: (params) => api.get('/users', { params }),
  all: () => api.get('/users/all'),
  get: (id) => api.get(`/users/${id}`),
  create: (data) => api.post('/users', data),
  update: (id, data) => api.put(`/users/${id}`, data),
  delete: (id) => api.delete(`/users/${id}`),
  /** 上传头像，返回 { url }，存入用户资料的 avatar 字段 */
  uploadAvatar: (formData) => uploadApi.post('/users/avatar', formData)
}

// 操作日志 API
export const operationLogApi = {
  list: (params) => api.get('/logs', { params })
}

// 角色管理 API
export const roleApi = {
  list: () => api.get('/roles'),
  get: (id) => api.get(`/roles/${id}`),
  permissions: (id) => api.get(`/roles/${id}/permissions`),
  allPermissions: () => api.get('/roles/permissions'),
  updatePermissions: (id, permissionIds) => api.put(`/roles/${id}/permissions`, permissionIds),
  update: (id, data) => api.put(`/roles/${id}`, data),
  create: (data) => api.post('/roles', data),
  delete: (id) => api.delete(`/roles/${id}`)
}

// 商品分类 API
export const categoryApi = {
  list: () => api.get('/categories'),
  get: (id) => api.get(`/categories/${id}`),
  create: (data) => api.post('/categories', data),
  update: (id, data) => api.put(`/categories/${id}`, data),
  delete: (id) => api.delete(`/categories/${id}`)
}

export default api