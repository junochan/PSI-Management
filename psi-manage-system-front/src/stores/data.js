import { defineStore } from 'pinia'
import { ref } from 'vue'
import { productApi, supplierApi, supplierIndustryApi, customerApi, warehouseApi, purchaseApi, salesApi, inventoryApi, aftersalesApi, userApi, operationLogApi, categoryApi, CATEGORY_STATUS } from '@/api'
import { useUserStore } from '@/stores/user'

export const useDataStore = defineStore('data', () => {
  const DEFAULT_LIST_PAGE_SIZE = 10
  // 商品数据 - 从后端获取
  const products = ref([])
  const productsLoading = ref(false)

  // 商品分类 - 从后端获取
  const categories = ref([])
  const categoriesLoading = ref(false)

  // 采购订单数据 - 从后端获取
  const purchaseOrders = ref([])
  const purchaseLoading = ref(false)

  // 销售订单数据 - 从后端获取
  const salesOrders = ref([])
  const salesLoading = ref(false)

  // 库存数据 - 从后端获取
  const inventoryData = ref([])
  const inventoryLoading = ref(false)

  // 预警数据 - 从后端获取
  const warnings = ref([])
  const warningsLoading = ref(false)

  // 调拨记录 - 从后端获取
  const transfers = ref([])
  const transfersLoading = ref(false)

  // 入库记录 - 从后端获取
  const inboundRecords = ref([])
  const inboundRecordsTotal = ref(0)
  const inboundLoading = ref(false)

  // 出库记录 - 从后端获取
  const outboundRecords = ref([])
  const outboundRecordsTotal = ref(0)
  const outboundLoading = ref(false)

  // 售后工单 - 从后端获取
  const aftersalesOrders = ref([])
  const aftersalesLoading = ref(false)

  // 供应商数据 - 从后端获取
  const suppliers = ref([])
  const suppliersLoading = ref(false)

  // 供应商行业字典
  const supplierIndustries = ref([])
  const supplierIndustriesLoading = ref(false)

  // 客户数据 - 从后端获取
  const customers = ref([])
  const customersLoading = ref(false)

  // 仓库数据 - 从后端获取
  const warehouses = ref([])
  const warehousesLoading = ref(false)
  /** 合并并发 loadWarehouses，避免多页同时触发重复 GET /warehouses/options */
  let warehousesOptionsInFlight = null
  let warehousesFullInFlight = null

  // 用户数据 - 从后端获取
  const users = ref([])
  const usersLoading = ref(false)

  // 操作日志数据 - 从后端获取
  const operationLogs = ref([])
  const operationLogsLoading = ref(false)

  // ===== 数据加载函数 =====

  // 加载商品列表（下拉等场景可传较大 size）
  const loadProducts = async (params = {}) => {
    productsLoading.value = true
    try {
      const res = await productApi.list({ page: 1, size: DEFAULT_LIST_PAGE_SIZE, ...params })
      products.value = res.list || []
    } catch (error) {
      console.error('加载商品失败:', error)
      products.value = []
    } finally {
      productsLoading.value = false
    }
  }

  // 加载供应商列表
  const loadSuppliers = async (params = {}) => {
    suppliersLoading.value = true
    try {
      const res = await supplierApi.list({ page: 1, size: DEFAULT_LIST_PAGE_SIZE, ...params })
      suppliers.value = res.list || []
    } catch (error) {
      console.error('加载供应商失败:', error)
      suppliers.value = []
    } finally {
      suppliersLoading.value = false
    }
  }

  const loadSupplierIndustries = async () => {
    supplierIndustriesLoading.value = true
    try {
      const list = await supplierIndustryApi.list()
      supplierIndustries.value = Array.isArray(list) ? list : []
    } catch (error) {
      console.error('加载供应商行业失败:', error)
      supplierIndustries.value = []
    } finally {
      supplierIndustriesLoading.value = false
    }
  }

  // 加载客户列表
  const loadCustomers = async (params = {}) => {
    customersLoading.value = true
    try {
      const res = await customerApi.list({ page: 1, size: DEFAULT_LIST_PAGE_SIZE, ...params })
      customers.value = res.list || []
    } catch (error) {
      console.error('加载客户失败:', error)
      customers.value = []
    } finally {
      customersLoading.value = false
    }
  }

  // 加载仓库列表（仅「库存菜单」或 inventory:warehouse 拉完整统计；否则下拉 options）
  const loadWarehouses = async (opts = {}, params = {}) => {
    const userStore = useUserStore()
    const fullList = opts.forceOptions
      ? false
      : opts.forceFull || userStore.hasPermission('inventory') || userStore.hasPermission('inventory:warehouse')

    const run = async () => {
      warehousesLoading.value = true
      try {
        if (fullList) {
          const res = await warehouseApi.list({ page: 1, size: DEFAULT_LIST_PAGE_SIZE, ...params })
          warehouses.value = res.list || []
        } else {
          const list = await warehouseApi.options()
          warehouses.value = Array.isArray(list) ? list : []
        }
      } catch (error) {
        console.error('加载仓库失败:', error)
        warehouses.value = []
      } finally {
        warehousesLoading.value = false
      }
    }

    if (fullList) {
      if (!warehousesFullInFlight) {
        warehousesFullInFlight = run().finally(() => {
          warehousesFullInFlight = null
        })
      }
      return warehousesFullInFlight
    }
    if (!warehousesOptionsInFlight) {
      warehousesOptionsInFlight = run().finally(() => {
        warehousesOptionsInFlight = null
      })
    }
    return warehousesOptionsInFlight
  }

  // 加载商品分类（仅启用，供商品/库存等下拉；分类管理页自行调用 categoryApi.list 不带 status）
  const loadCategories = async () => {
    categoriesLoading.value = true
    try {
      const res = await categoryApi.list({ status: CATEGORY_STATUS.ENABLED })
      categories.value = res || []
    } catch (error) {
      console.error('加载分类失败:', error)
      categories.value = []
    } finally {
      categoriesLoading.value = false
    }
  }

  // 仅拉取当前登录用户详情（供顶栏资料区等，避免为分页设置再请求 /users/all）
  const loadUsers = async () => {
    usersLoading.value = true
    try {
      const userStore = useUserStore()
      const uid = userStore.userInfo?.id
      if (!uid) {
        users.value = []
        return
      }
      const u = await userApi.get(uid)
      users.value = u ? [u] : []
    } catch (error) {
      console.error('加载用户失败:', error)
      users.value = []
    } finally {
      usersLoading.value = false
    }
  }

  // 加载操作日志（按调用方分页参数）
  const loadOperationLogs = async (params = {}) => {
    operationLogsLoading.value = true
    try {
      const res = await operationLogApi.list({
        page: 1,
        size: 10,
        ...params
      })
      operationLogs.value = res.list || []
      return res
    } catch (error) {
      console.error('加载操作日志失败:', error)
      operationLogs.value = []
      return { list: [], total: 0, page: 1, size: 10 }
    } finally {
      operationLogsLoading.value = false
    }
  }

  // 加载采购订单（列表页另有服务端分页；此处供全量统计/下拉等）
  const loadPurchaseOrders = async (params = {}) => {
    purchaseLoading.value = true
    try {
      const res = await purchaseApi.list({ page: 1, size: DEFAULT_LIST_PAGE_SIZE, ...params })
      purchaseOrders.value = res.list || []
    } catch (error) {
      console.error('加载采购订单失败:', error)
      purchaseOrders.value = []
    } finally {
      purchaseLoading.value = false
    }
  }

  // 加载入库记录
  const loadInboundRecords = async (params = {}) => {
    inboundLoading.value = true
    try {
      const res = await purchaseApi.inboundList({ page: 1, size: DEFAULT_LIST_PAGE_SIZE, ...params })
      inboundRecords.value = res.list || []
      inboundRecordsTotal.value = Number(res.total) || 0
    } catch (error) {
      console.error('加载入库记录失败:', error)
      inboundRecords.value = []
      inboundRecordsTotal.value = 0
    } finally {
      inboundLoading.value = false
    }
  }

  // 加载出库记录（统一以后端出库流水为准，兼容手动“其他出库”）
  const loadOutboundRecords = async (params = {}) => {
    outboundLoading.value = true
    try {
      const res = await inventoryApi.outboundList({ page: 1, size: DEFAULT_LIST_PAGE_SIZE, ...params })
      outboundRecords.value = res.list || []
      outboundRecordsTotal.value = Number(res.total) || 0
    } catch (error) {
      console.error('加载出库记录失败:', error)
      outboundRecords.value = []
      outboundRecordsTotal.value = 0
    } finally {
      outboundLoading.value = false
    }
  }

  // 加载销售订单（列表页另有服务端分页；此处供下拉等）
  const loadSalesOrders = async (params = {}) => {
    salesLoading.value = true
    try {
      const res = await salesApi.list({ page: 1, size: DEFAULT_LIST_PAGE_SIZE, ...params })
      salesOrders.value = res.list || []
    } catch (error) {
      console.error('加载销售订单失败:', error)
      salesOrders.value = []
    } finally {
      salesLoading.value = false
    }
  }

  // 加载库存数据（列表页另有服务端分页；此处供表单下拉等）
  const loadInventory = async (params = {}) => {
    inventoryLoading.value = true
    try {
      const res = await inventoryApi.list({ page: 1, size: DEFAULT_LIST_PAGE_SIZE, ...params })
      inventoryData.value = res.list || []
    } catch (error) {
      console.error('加载库存失败:', error)
      inventoryData.value = []
    } finally {
      inventoryLoading.value = false
    }
  }

  // 加载预警数据
  const loadWarnings = async (params = {}) => {
    warningsLoading.value = true
    try {
      const res = await inventoryApi.warningList({ page: 1, size: DEFAULT_LIST_PAGE_SIZE, ...params })
      warnings.value = res.list || []
    } catch (error) {
      console.error('加载预警失败:', error)
      warnings.value = []
    } finally {
      warningsLoading.value = false
    }
  }

  // 加载调拨记录
  const loadTransfers = async (params = {}) => {
    transfersLoading.value = true
    try {
      const res = await inventoryApi.transferList({ page: 1, size: DEFAULT_LIST_PAGE_SIZE, ...params })
      transfers.value = res.list || []
    } catch (error) {
      console.error('加载调拨记录失败:', error)
      transfers.value = []
    } finally {
      transfersLoading.value = false
    }
  }

  // 加载售后工单
  const loadAftersales = async (params = {}) => {
    aftersalesLoading.value = true
    try {
      const res = await aftersalesApi.list({ page: 1, size: DEFAULT_LIST_PAGE_SIZE, ...params })
      aftersalesOrders.value = res.list || []
    } catch (error) {
      console.error('加载售后工单失败:', error)
      aftersalesOrders.value = []
    } finally {
      aftersalesLoading.value = false
    }
  }

  // 初始化所有数据
  const initAllData = async () => {
    await Promise.all([
      loadProducts(),
      loadCategories(),
      loadSuppliers(),
      loadSupplierIndustries(),
      loadCustomers(),
      loadWarehouses(),
      loadPurchaseOrders(),
      loadSalesOrders(),
      loadInventory(),
      loadWarnings(),
      loadTransfers(),
      loadInboundRecords(),
      loadAftersales()
    ])
  }

  // ===== CRUD操作 - 调用后端API =====

  // 添加商品
  const addProduct = async (product) => {
    try {
      await productApi.create(product)
      await loadProducts()
    } catch (error) {
      console.error('添加商品失败:', error)
      throw error
    }
  }

  // 更新商品
  const updateProduct = async (id, data) => {
    try {
      await productApi.update(id, data)
      await loadProducts()
    } catch (error) {
      console.error('更新商品失败:', error)
      throw error
    }
  }

  // 删除商品
  const deleteProduct = async (id) => {
    try {
      await productApi.delete(id)
      await loadProducts()
    } catch (error) {
      console.error('删除商品失败:', error)
      throw error
    }
  }

  // 创建采购订单
  const addPurchaseOrder = async (order) => {
    try {
      await purchaseApi.create(order)
      await loadPurchaseOrders()
    } catch (error) {
      console.error('创建采购订单失败:', error)
      throw error
    }
  }

  // 创建销售订单
  const addSalesOrder = async (order) => {
    try {
      await salesApi.create(order)
      await loadSalesOrders()
    } catch (error) {
      console.error('创建销售订单失败:', error)
      throw error
    }
  }

  // 添加客户
  const addCustomer = async (customer) => {
    try {
      await customerApi.create(customer)
      await loadCustomers()
    } catch (error) {
      console.error('添加客户失败:', error)
      throw error
    }
  }

  // 添加供应商
  const addSupplier = async (supplier) => {
    try {
      await supplierApi.create(supplier)
      await loadSuppliers()
    } catch (error) {
      console.error('添加供应商失败:', error)
      throw error
    }
  }

  // 创建调拨单
  const addTransfer = async (transfer) => {
    try {
      await inventoryApi.createTransfer(transfer)
      await loadTransfers()
    } catch (error) {
      console.error('创建调拨单失败:', error)
      throw error
    }
  }

  // 创建售后工单
  const addAftersales = async (order) => {
    try {
      await aftersalesApi.create(order)
      await loadAftersales()
    } catch (error) {
      console.error('创建售后工单失败:', error)
      throw error
    }
  }

  return {
    // 数据
    products,
    productsLoading,
    categories,
    categoriesLoading,
    purchaseOrders,
    purchaseLoading,
    salesOrders,
    salesLoading,
    inventoryData,
    inventoryLoading,
    warnings,
    warningsLoading,
    transfers,
    transfersLoading,
    inboundRecords,
    inboundRecordsTotal,
    inboundLoading,
    outboundRecords,
    outboundRecordsTotal,
    outboundLoading,
    aftersalesOrders,
    aftersalesLoading,
    suppliers,
    suppliersLoading,
    supplierIndustries,
    supplierIndustriesLoading,
    customers,
    customersLoading,
    warehouses,
    warehousesLoading,
    users,
    usersLoading,
    operationLogs,
    operationLogsLoading,

    // 加载函数
    loadProducts,
    loadCategories,
    loadSuppliers,
    loadSupplierIndustries,
    loadCustomers,
    loadWarehouses,
    loadUsers,
    loadOperationLogs,
    loadPurchaseOrders,
    loadSalesOrders,
    loadInventory,
    loadWarnings,
    loadTransfers,
    loadInboundRecords,
    loadOutboundRecords,
    loadAftersales,
    initAllData,

    // CRUD操作
    addProduct,
    updateProduct,
    deleteProduct,
    addPurchaseOrder,
    addSalesOrder,
    addCustomer,
    addSupplier,
    addTransfer,
    addAftersales
  }
})

// 导出独立的ref供直接使用
export const suppliers = ref([])
export const customers = ref([])
export const warehouses = ref([])