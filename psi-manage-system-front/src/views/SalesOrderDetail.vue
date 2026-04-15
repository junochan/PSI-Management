<template>
  <div class="sales-order-detail">
    <el-card>
      <template #header>
        <div class="card-header">
          <h3>销售单详情</h3>
          <div class="header-actions">
            <el-button type="success" @click="handlePayment" v-if="order?.status === 'pending' && order?.payStatus === 'unpaid'">确认付款</el-button>
            <el-button type="primary" @click="handleShip" v-if="order?.status === 'pending' && order?.payStatus === 'paid'">确认发货</el-button>
            <el-button type="success" @click="handleReceived" v-if="order?.status === 'shipped'">确认收货</el-button>
            <el-button @click="goBack">返回</el-button>
          </div>
        </div>
      </template>
      <el-descriptions :column="2" border>
        <el-descriptions-item label="订单编号"><span class="order-no">{{ order?.orderNo }}</span></el-descriptions-item>
        <el-descriptions-item label="下单时间">{{ formatTime(order?.createTime) }}</el-descriptions-item>
        <el-descriptions-item label="客户">{{ order?.customerName || order?.customer }}</el-descriptions-item>
        <el-descriptions-item label="商品">
          <div class="product-cell">
            <img v-if="getProductImage(order?.productId)" :src="getProductImage(order?.productId)" class="product-thumb" />
            <span v-else class="product-icon">{{ getProductIcon(getProductName(order?.productId, order?.productName || order?.product)) }}</span>
            <span class="product-name">{{ getProductName(order?.productId, order?.productName || order?.product) }}</span>
          </div>
        </el-descriptions-item>
        <el-descriptions-item label="商品分类">
          <el-tag type="info" effect="light">{{ productCategoryName }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="订购数量"><span class="total-qty">{{ order?.quantity }}</span></el-descriptions-item>
        <el-descriptions-item label="销售单价"><span class="unit-price">¥{{ order?.unitPrice || (order?.amount && order?.quantity ? (order?.amount / order?.quantity).toFixed(2) : '0.00') }}</span></el-descriptions-item>
        <el-descriptions-item label="订单金额"><span class="amount">¥{{ order?.amount }}</span></el-descriptions-item>
        <el-descriptions-item label="已发货"><span class="shipped-qty">{{ shippedQuantity }}</span></el-descriptions-item>
        <el-descriptions-item label="待发货"><span class="pending-qty">{{ pendingQuantity }}</span></el-descriptions-item>
        <el-descriptions-item label="订单状态"><el-tag :type="getStatusType(order?.status)">{{ formatOrderStatus(order?.status) }}</el-tag></el-descriptions-item>
        <el-descriptions-item label="付款状态"><el-tag :type="getPayStatusType(order?.status === 'completed' ? 'paid' : order?.payStatus)">{{ formatPayStatus(order?.status === 'completed' ? 'paid' : order?.payStatus) }}</el-tag></el-descriptions-item>
        <el-descriptions-item label="付款方式">{{ order?.payMethod || '-' }}</el-descriptions-item>
        <el-descriptions-item label="期望到达日期">{{ order?.expectArriveDate || '-' }}</el-descriptions-item>
        <el-descriptions-item label="操作人">{{ order?.operatorName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="备注">{{ order?.remark || '-' }}</el-descriptions-item>
      </el-descriptions>
    </el-card>

    <!-- 发货进度 -->
    <el-card style="margin-top: 20px">
      <template #header><h3>发货进度</h3></template>
      <el-steps :active="getStepActive()" finish-status="success">
        <el-step title="下单成功" :description="stepDescriptions.place" />
        <el-step title="付款确认" :description="stepDescriptions.pay" />
        <el-step title="发货处理" :description="stepDescriptions.ship" />
        <el-step title="订单完成" :description="stepDescriptions.complete" />
      </el-steps>
    </el-card>

    <!-- 物流信息 -->
    <el-card style="margin-top: 20px" v-if="order?.logisticsNo">
      <template #header>
        <h3>物流信息</h3>
      </template>
      <el-descriptions :column="2" border>
        <el-descriptions-item label="物流公司">
          <span class="logistics-company">{{ order?.logisticsCompany }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="物流单号">
          <span class="tracking-no">{{ order?.logisticsNo }}</span>
          <el-button type="primary" link size="small" @click="copyTrackingNo" style="margin-left: 8px">复制</el-button>
        </el-descriptions-item>
        <el-descriptions-item label="发货仓库">{{ getWarehouseName(order?.warehouseId) || order?.warehouseName || order?.warehouse }}</el-descriptions-item>
        <el-descriptions-item label="发货时间">{{ formatTime(order?.shipTime) }}</el-descriptions-item>
      </el-descriptions>
      <el-divider content-position="left">收货地址</el-divider>
      <el-descriptions :column="2" border>
        <el-descriptions-item label="收货人">{{ order?.receiverName || '—' }}</el-descriptions-item>
        <el-descriptions-item label="联系电话">{{ order?.receiverPhone || '—' }}</el-descriptions-item>
        <el-descriptions-item label="收货地址" :span="2">{{ order?.receiverAddress || '—' }}</el-descriptions-item>
      </el-descriptions>
    </el-card>

    <!-- 待发货提示 -->
    <el-card style="margin-top: 20px" v-if="order?.status === 'pending' && !order?.logisticsNo">
      <template #header><h3>物流信息</h3></template>
      <el-empty description="暂无物流信息，请点击「确认发货」填写发货信息" :image-size="80" />
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useDataStore } from '@/stores/data'
import { salesApi } from '@/api'
import { formatTime } from '@/utils/time'
import { firstProductImageUrl } from '@/utils/productImages'

const router = useRouter()
const route = useRoute()
const dataStore = useDataStore()

const orderId = computed(() => route.params.id)
const order = ref(null)

/** 详情页拉取单笔订单，确保收货地址等字段与后端一致 */
const fetchOrderDetail = async () => {
  const id = Number(orderId.value)
  if (!id) return
  try {
    order.value = await salesApi.get(id)
  } catch {
    order.value = dataStore.salesOrders.find((o) => o.id === id) || null
  }
}

// 仓库列表
const warehousesList = computed(() => dataStore.warehouses || [])

// 计算发货数量
const shippedQuantity = computed(() => order.value?.shippedQuantity || (order.value?.logisticsNo ? order.value?.quantity : 0))
const pendingQuantity = computed(() => order.value?.pendingQuantity || (order.value?.quantity || 0) - shippedQuantity.value)

const getStatusType = (status) => ({ 'completed': 'success', 'shipped': 'success', 'pending': 'warning', 'cancelled': 'danger', '已完成': 'success', '已发货': 'success', '待发货': 'warning', '已取消': 'danger' }[status] || 'info')
const getPayStatusType = (status) => ({ 'paid': 'success', 'unpaid': 'warning', 'refunded': 'danger', '已付款': 'success', '待付款': 'warning', '已退款': 'danger' }[status] || 'info')

// 获取商品图片
const getProductImage = (productId) => {
  const product = dataStore.products.find(p => p.id === productId)
  return firstProductImageUrl(product?.image)
}

// 获取商品名称（从商品列表动态获取最新名称）
const getProductName = (productId, fallbackName) => {
  const product = dataStore.products.find(p => p.id === productId)
  return product?.name || fallbackName || '-'
}

// 动态获取仓库名称 - 从仓库列表中根据warehouseId查找
const getWarehouseName = (warehouseId) => {
  const warehouse = warehousesList.value.find(w => w.id === warehouseId)
  return warehouse?.name || ''
}

// 获取商品分类名称
const productCategoryName = computed(() => {
  if (!order.value) return ''
  const product = dataStore.products.find(p => p.id === order.value.productId)
  if (product) {
    // 从分类列表中查找分类名称
    if (product.categoryId) {
      const category = dataStore.categories.find(c => c.id === product.categoryId)
      return category?.name || product.categoryName || product.category || ''
    }
    return product.categoryName || product.category || ''
  }
  return ''
})

// 获取商品图标（无图片时使用）
const getProductIcon = (productName) => {
  if (productName?.includes('手机') || productName?.includes('Phone')) return '📱'
  if (productName?.includes('电脑') || productName?.includes('Mac') || productName?.includes('Book')) return '💻'
  if (productName?.includes('耳机') || productName?.includes('Pod')) return '🎧'
  if (productName?.includes('手表') || productName?.includes('Watch')) return '⌚'
  if (productName?.includes('平板') || productName?.includes('iPad')) return '📱'
  return '📦'
}

// 状态格式化函数
const formatOrderStatus = (status) => {
  const statusMap = { 'pending': '待发货', 'shipped': '已发货', 'completed': '已完成', 'cancelled': '已取消', '待发货': '待发货', '已发货': '已发货', '已完成': '已完成', '已取消': '已取消' }
  return statusMap[status] || status
}
const formatPayStatus = (status) => {
  const statusMap = { 'unpaid': '待付款', 'paid': '已付款', 'refunded': '已退款', '待付款': '待付款', '已付款': '已付款', '已退款': '已退款' }
  return statusMap[status] || status
}
/** 发货进度各阶段说明（含 yyyy-MM-dd HH:mm:ss，与 formatTime 一致） */
const stepDescriptions = computed(() => {
  const o = order.value
  if (!o) {
    return { place: '—', pay: '—', ship: '—', complete: '—' }
  }
  const suffix = (t) => {
    const s = formatTime(t)
    return s !== '-' ? ` · ${s}` : ''
  }
  const paidOrComplete =
    o.payStatus === 'paid' ||
    o.payStatus === '已付款' ||
    o.status === 'completed' ||
    o.status === '已完成'
  const shipDesc = o.logisticsNo
    ? `已发货 ${shippedQuantity.value}${suffix(o.shipTime)}`
    : o.status === 'pending' || o.status === '待发货'
      ? `待发货 ${pendingQuantity.value}`
      : '处理中'
  return {
    place: `订单已创建${suffix(o.createTime)}`,
    pay: paidOrComplete ? `付款已完成${suffix(o.payTime)}` : '等待付款',
    ship: shipDesc,
    complete:
      o.status === 'completed' || o.status === '已完成'
        ? `订单已完成${suffix(o.completeTime)}`
        : '等待完成'
  }
})

const getStepActive = () => {
  // 已完成状态，所有步骤都完成
  if (order.value?.status === 'completed' || order.value?.status === '已完成') return 4
  // 已取消状态，停留在第一步
  if (order.value?.status === 'cancelled' || order.value?.status === '已取消') return 1
  // 已发货状态，进行到第三步
  if (order.value?.logisticsNo || order.value?.status === 'shipped' || order.value?.status === '已发货') return 3
  // 已付款状态，进行到第二步
  if (order.value?.payStatus === 'paid' || order.value?.payStatus === '已付款') return 2
  // 待发货且未付款，停留在第一步
  return 1
}

// 加载订单数据
const loadOrderData = async () => {
  await Promise.all([
    dataStore.loadSalesOrders(),
    dataStore.loadProducts(),
    dataStore.loadWarehouses(),
    dataStore.loadCategories()
  ])
  await fetchOrderDetail()
}

onMounted(async () => {
  await loadOrderData()
})

const goBack = () => router.back()

// 复制物流单号
const copyTrackingNo = () => {
  if (order.value?.logisticsNo) {
    navigator.clipboard.writeText(order.value.logisticsNo)
    ElMessage.success('物流单号已复制')
  }
}

// 确认付款
const handlePayment = async () => {
  try {
    await salesApi.payment(order.value.id)
    ElMessage.success(`订单 ${order.value.orderNo} 已确认付款`)
    await loadOrderData()
  } catch (error) {
    ElMessage.error(error.message || '确认付款失败')
  }
}

// 发货处理 - 跳转到销售页面并触发发货弹框
const handleShip = () => {
  router.push({
    path: '/sales',
    query: { shipOrderId: orderId.value }
  })
}

// 确认收货
const handleReceived = async () => {
  try {
    await salesApi.received(order.value.id)
    ElMessage.success(`订单 ${order.value.orderNo} 已确认收货`)
    await loadOrderData()
  } catch (error) {
    ElMessage.error(error.message || '确认收货失败')
  }
}
</script>

<style lang="scss" scoped>
.sales-order-detail {
  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    h3 {
      font-size: 18px;
      font-weight: 600;
      color: #303133;
    }
    .header-actions {
      display: flex;
      gap: 12px;
    }
  }

  .order-no {
    color: #E94560;
    font-family: monospace;
  }

  .amount {
    font-weight: 600;
    color: #E94560;
  }

  .unit-price {
    font-weight: 600;
    color: #409EFF;
  }

  .total-qty {
    font-weight: 600;
    color: #303133;
  }

  .shipped-qty {
    font-weight: 600;
    color: #67C23A;
  }

  .pending-qty {
    font-weight: 600;
    color: #E6A23C;
  }

  .logistics-company {
    font-weight: 600;
    color: #409EFF;
  }

  .tracking-no {
    font-family: monospace;
    color: #303133;
  }

  // 商品图片样式
  .product-cell {
    display: flex;
    align-items: center;
    gap: 12px;

    .product-thumb {
      width: 40px;
      height: 40px;
      object-fit: cover;
      border-radius: 6px;
    }

    .product-icon {
      width: 40px;
      height: 40px;
      display: flex;
      align-items: center;
      justify-content: center;
      background: #F5F7FA;
      border-radius: 6px;
      font-size: 20px;
    }

    .product-name {
      font-size: 14px;
      color: #303133;
    }
  }
}
</style>