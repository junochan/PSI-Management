<template>
  <div class="purchase-order-detail">
    <el-card>
      <template #header>
        <div class="card-header">
          <div class="header-actions">
            <el-button @click="goBack">返回</el-button>
          </div>
        </div>
      </template>
      <el-descriptions :column="2" border>
        <el-descriptions-item label="采购单号"><span class="order-no">{{ order?.orderNo }}</span></el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ formatTime(order?.createTime) }}</el-descriptions-item>
        <el-descriptions-item label="供应商">{{ supplierDisplayName }}</el-descriptions-item>
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
        <el-descriptions-item label="单价"><span class="price">¥{{ (order?.unitPrice || 0).toLocaleString() }}</span></el-descriptions-item>
        <el-descriptions-item label="采购总量"><span class="total-qty">{{ order?.totalQuantity }} 套</span></el-descriptions-item>
        <el-descriptions-item label="采购金额"><span class="amount">¥{{ (order?.amount || 0).toLocaleString() }}</span></el-descriptions-item>
        <el-descriptions-item label="待入库"><span class="pending-qty">{{ order?.pendingQuantity }} 套</span></el-descriptions-item>
        <el-descriptions-item label="已入库"><span class="inbound-qty">{{ order?.inboundQuantity || 0 }} 套</span></el-descriptions-item>
        <el-descriptions-item label="入库状态"><el-tag :type="getInboundStatusType(order?.inboundStatus)">{{ formatInboundStatus(order?.inboundStatus) }}</el-tag></el-descriptions-item>
        <el-descriptions-item label="付款状态"><el-tag :type="getPayStatusType(order?.payStatus)">{{ formatPayStatus(order?.payStatus) }}</el-tag></el-descriptions-item>
        <el-descriptions-item label="付款方式">{{ order?.payMethod || order?.paymentMethod || '-' }}</el-descriptions-item>
        <el-descriptions-item label="期望交货日期">{{ order?.expectDate || order?.expectedDate || '-' }}</el-descriptions-item>
        <el-descriptions-item label="入库仓库">
          <el-tag type="success" effect="light">{{ getWarehouseName(order?.warehouseId) || order?.warehouseName || '-' }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="操作人">{{ order?.operatorName || order?.operator || order?.creatorName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="备注" :span="2">{{ order?.remark || '-' }}</el-descriptions-item>
      </el-descriptions>
    </el-card>

    <!-- 入库进度 -->
    <el-card style="margin-top: 20px" v-if="order?.inboundStatus !== 'completed' && order?.inboundStatus !== 'cancelled'">
      <el-steps :active="order?.inboundStatus === 'partial' ? 2 : 1" finish-status="success">
        <el-step title="创建采购单" description="采购单已创建" />
        <el-step title="部分入库" :description="`已入库 ${order?.inboundQuantity || 0} 套，待入库 ${order?.pendingQuantity} 套`" />
        <el-step title="完成入库" description="入库完成" />
      </el-steps>
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { categoryApi, productApi, purchaseApi, supplierApi, warehouseApi } from '@/api'
import { formatTime } from '@/utils/time'
import { firstProductImageUrl } from '@/utils/productImages'

const router = useRouter()
const route = useRoute()

const orderId = computed(() => route.params.id)
const order = ref(null)
const productDetail = ref(null)
const supplierDetail = ref(null)
const warehouseDetail = ref(null)
const categoryDetail = ref(null)

const resolveId = (value) => {
  const id = Number(value)
  return Number.isFinite(id) && id > 0 ? id : null
}

const loadRelatedData = async () => {
  productDetail.value = null
  supplierDetail.value = null
  warehouseDetail.value = null
  categoryDetail.value = null
  if (!order.value) return

  const productId = resolveId(order.value.productId)
  const supplierId = resolveId(order.value.supplierId)
  const warehouseId = resolveId(order.value.warehouseId)

  const [productResult, supplierResult, warehouseResult] = await Promise.allSettled([
    productId ? productApi.get(productId) : Promise.resolve(null),
    supplierId ? supplierApi.get(supplierId) : Promise.resolve(null),
    warehouseId ? warehouseApi.get(warehouseId) : Promise.resolve(null)
  ])

  if (productResult.status === 'fulfilled') productDetail.value = productResult.value
  if (supplierResult.status === 'fulfilled') supplierDetail.value = supplierResult.value
  if (warehouseResult.status === 'fulfilled') warehouseDetail.value = warehouseResult.value

  const categoryId = resolveId(productDetail.value?.categoryId)
  if (categoryId) {
    try {
      categoryDetail.value = await categoryApi.get(categoryId)
    } catch {
      categoryDetail.value = null
    }
  }
}

// 仓库名称优先使用关联详情接口
const getWarehouseName = (warehouseId) => {
  if (!order.value || order.value.warehouseId !== warehouseId) return ''
  return warehouseDetail.value?.name || order.value.warehouseName || order.value.warehouse || ''
}

// 供应商名称优先使用关联详情接口
const supplierDisplayName = computed(() => {
  if (!order.value) return ''
  return supplierDetail.value?.name || order.value.supplierName || order.value.supplier || '-'
})

// 商品分类优先使用关联详情接口
const productCategoryName = computed(() => {
  if (!order.value) return ''
  return (
    order.value.categoryName ||
    order.value.category ||
    productDetail.value?.categoryName ||
    productDetail.value?.category ||
    categoryDetail.value?.name ||
    '-'
  )
})

// 状态格式化函数
const formatInboundStatus = (status) => {
  const statusMap = { 'pending': '待入库', 'partial': '部分入库', 'completed': '已完成', 'cancelled': '已取消' }
  return statusMap[status] || status
}
const formatPayStatus = (status) => {
  const statusMap = { 'unpaid': '待付款', 'paid': '已付款', 'refunded': '已退款' }
  return statusMap[status] || status
}

const getInboundStatusType = (status) => ({ 'completed': 'success', 'partial': 'warning', 'pending': 'info', 'cancelled': 'danger' }[status] || 'info')
const getPayStatusType = (status) => ({ 'paid': 'success', 'unpaid': 'warning', 'refunded': 'danger' }[status] || 'info')

// 获取商品图片
const getProductImage = (productId) => {
  if (!order.value || order.value.productId !== productId) return ''
  return firstProductImageUrl(
    productDetail.value?.image ||
    order.value.productImage ||
    order.value.image
  )
}

// 商品名称优先使用关联详情接口
const getProductName = (productId, fallbackName) => {
  if (!order.value || order.value.productId !== productId) return fallbackName || '-'
  return productDetail.value?.name || order.value.productName || order.value.product || fallbackName || '-'
}

// 获取商品图标（无图片时使用）
const getProductIcon = (productName) => {
  if (productName?.includes('手机') || productName?.includes('Phone')) return '📱'
  if (productName?.includes('电脑') || productName?.includes('Mac') || productName?.includes('Book')) return '💻'
  if (productName?.includes('耳机') || productName?.includes('Pod')) return '🎧'
  if (productName?.includes('手表') || productName?.includes('Watch')) return '⌚'
  if (productName?.includes('平板') || productName?.includes('iPad')) return '📱'
  return '📦'
}

onMounted(async () => {
  const id = Number(orderId.value)
  if (!id) {
    ElMessage.warning('采购单 ID 无效')
    router.replace('/purchase')
    return
  }
  try {
    order.value = await purchaseApi.get(id)
    await loadRelatedData()
  } catch (e) {
    ElMessage.error(e.message || '加载采购单失败')
    router.replace('/purchase')
  }
})
const goBack = () => router.back()
</script>

<style lang="scss" scoped>
.purchase-order-detail {
  .card-header {
    display: flex;
    justify-content: flex-end;
    align-items: center;
    .header-actions {
      display: flex;
      gap: 12px;
    }
  }
  .order-no {
    color: #E94560;
    font-family: monospace;
  }
  .price {
    font-weight: 600;
    color: #409EFF;
  }
  .amount {
    font-weight: 600;
    color: #E94560;
  }
  .total-qty {
    font-weight: 600;
    color: #303133;
  }
  .pending-qty {
    font-weight: 600;
    color: #E6A23C;
  }
  .inbound-qty {
    font-weight: 600;
    color: #67C23A;
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