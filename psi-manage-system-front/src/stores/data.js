import { defineStore } from 'pinia'
import { ref } from 'vue'
import { productApi, supplierApi, supplierIndustryApi, customerApi, warehouseApi, purchaseApi, salesApi, inventoryApi, aftersalesApi, userApi, operationLogApi, categoryApi } from '@/api'
import { useUserStore } from '@/stores/user'

export const useDataStore = defineStore('data', () => {
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
  const inboundLoading = ref(false)

  // 出库记录 - 从后端获取
  const outboundRecords = ref([])
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
      const res = await productApi.list({ page: 1, size: 500, ...params })
      products.value = res.list || []
    } catch (error) {
      console.error('加载商品失败:', error)
      products.value = []
    } finally {
      productsLoading.value = false
    }
  }

  // 加载供应商列表
  const loadSuppliers = async () => {
    suppliersLoading.value = true
    try {
      const res = await supplierApi.list({ page: 1, size: 100 })
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
  const loadCustomers = async () => {
    customersLoading.value = true
    try {
      const res = await customerApi.list({ page: 1, size: 100 })
      customers.value = res.list || []
    } catch (error) {
      console.error('加载客户失败:', error)
      customers.value = []
    } finally {
      customersLoading.value = false
    }
  }

  // 加载仓库列表（仅「库存菜单」或 inventory:warehouse 拉完整统计；否则下拉 options）
  const loadWarehouses = async () => {
    warehousesLoading.value = true
    try {
      const userStore = useUserStore()
      const fullList =
        userStore.hasPermission('inventory') || userStore.hasPermission('inventory:warehouse')
      if (fullList) {
        const res = await warehouseApi.list({ page: 1, size: 100 })
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

  // 加载商品分类
  const loadCategories = async () => {
    categoriesLoading.value = true
    try {
      const res = await categoryApi.list()
      categories.value = res || []
    } catch (error) {
      console.error('加载分类失败:', error)
      categories.value = []
    } finally {
      categoriesLoading.value = false
    }
  }

  // 加载用户列表
  const loadUsers = async () => {
    usersLoading.value = true
    try {
      const res = await userApi.all()
      users.value = res || []
    } catch (error) {
      console.error('加载用户失败:', error)
      users.value = []
    } finally {
      usersLoading.value = false
    }
  }

  // 加载操作日志
  const loadOperationLogs = async () => {
    operationLogsLoading.value = true
    try {
      const res = await operationLogApi.list({ page: 1, size: 100 })
      operationLogs.value = res.list || []
    } catch (error) {
      console.error('加载操作日志失败:', error)
      operationLogs.value = []
    } finally {
      operationLogsLoading.value = false
    }
  }

  // 加载采购订单（列表页另有服务端分页；此处供全量统计/下拉等）
  const loadPurchaseOrders = async (params = {}) => {
    purchaseLoading.value = true
    try {
      const res = await purchaseApi.list({ page: 1, size: 500, ...params })
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
      const res = await purchaseApi.inboundList({ page: 1, size: 500, ...params })
      inboundRecords.value = res.list || []
    } catch (error) {
      console.error('加载入库记录失败:', error)
      inboundRecords.value = []
    } finally {
      inboundLoading.value = false
    }
  }

  // 加载出库记录（从销售订单中筛选已发货的）
  const loadOutboundRecords = async () => {
    outboundLoading.value = true
    try {
      // 获取销售订单，已发货的作为出库记录
      const res = await salesApi.list({ page: 1, size: 100 })
      // 筛选已发货的订单作为出库记录
      const shippedOrders = (res.list || []).filter(order =>
        order.status === 'shipped' || order.status === 'completed' || order.shippedQuantity > 0
      )
      // 转换为出库记录格式
      outboundRecords.value = shippedOrders.map(order => ({
        id: order.id,
        orderNo: `OUT${order.id.toString().padStart(6, '0')}`,
        salesOrderNo: order.orderNo,
        salesOrderId: order.id,
        customerName: order.customerName,
        productName: order.productName,
        productId: order.productId,
        quantity: order.shippedQuantity || order.quantity,
        warehouseId: order.warehouseId,
        warehouseName: order.warehouseName,
        createTime: order.shipTime || order.updateTime,
        operatorName: order.operatorName
      }))
    } catch (error) {
      console.error('加载出库记录失败:', error)
      outboundRecords.value = []
    } finally {
      outboundLoading.value = false
    }
  }

  // 加载销售订单（列表页另有服务端分页；此处供下拉等）
  const loadSalesOrders = async (params = {}) => {
    salesLoading.value = true
    try {
      const res = await salesApi.list({ page: 1, size: 500, ...params })
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
      const res = await inventoryApi.list({ page: 1, size: 500, ...params })
      inventoryData.value = res.list || []
    } catch (error) {
      console.error('加载库存失败:', error)
      inventoryData.value = []
    } finally {
      inventoryLoading.value = false
    }
  }

  // 加载预警数据
  const loadWarnings = async () => {
    warningsLoading.value = true
    try {
      const res = await inventoryApi.warningList({ page: 1, size: 100 })
      warnings.value = res.list || []
    } catch (error) {
      console.error('加载预警失败:', error)
      warnings.value = []
    } finally {
      warningsLoading.value = false
    }
  }

  // 加载调拨记录
  const loadTransfers = async () => {
    transfersLoading.value = true
    try {
      const res = await inventoryApi.transferList({ page: 1, size: 100 })
      transfers.value = res.list || []
    } catch (error) {
      console.error('加载调拨记录失败:', error)
      transfers.value = []
    } finally {
      transfersLoading.value = false
    }
  }

  // 加载售后工单
  const loadAftersales = async () => {
    aftersalesLoading.value = true
    try {
      const res = await aftersalesApi.list({ page: 1, size: 100 })
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
    inboundLoading,
    outboundRecords,
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