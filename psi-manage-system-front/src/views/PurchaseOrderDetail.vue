<template>
  <div class="purchase-order-detail" v-loading="pageLoading" element-loading-text="加载中...">
    <el-card>
      <template #header>
        <div class="card-header">
          <div class="header-actions">
            <el-button @click="goBack">返回</el-button>
          </div>
        </div>
      </template>
      <el-descriptions :column="2" border class="purchase-order-detail-descriptions">
        <el-descriptions-item label="采购单号"><span class="order-no">{{ order?.orderNo }}</span></el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ formatTime(order?.createTime) }}</el-descriptions-item>
        <el-descriptions-item label="供应商">{{ order?.supplierName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="商品">
          <div class="product-cell">
            <ProductImageThumb
              v-if="getProductImage(order?.productImage)"
              :src="getProductImage(order?.productImage)"
              :preview-src-list="parseProductImageUrls(order?.productImage || order?.image)"
              class="product-thumb"
              :width="40"
              :height="40"
              :radius="6"
            />
            <span v-else class="product-icon">{{ getProductIcon(getProductName(order?.productName)) }}</span>
            <span class="product-name">{{ getProductName(order?.productName) }}</span>
          </div>
        </el-descriptions-item>
        <el-descriptions-item label="商品分类">
          <el-tag type="info" effect="light">{{ order?.categoryName || '-' }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="单价"><span class="price">¥{{ formatAmountDisplay(order?.unitPrice || 0) }}</span></el-descriptions-item>
        <el-descriptions-item label="采购总量"><span class="total-qty">{{ order?.totalQuantity }} 套</span></el-descriptions-item>
        <el-descriptions-item label="采购金额"><span class="amount">¥{{ formatAmountDisplay(order?.amount || 0) }}</span></el-descriptions-item>
        <el-descriptions-item label="待入库"><span class="pending-qty">{{ order?.pendingQuantity }} 套</span></el-descriptions-item>
        <el-descriptions-item label="已入库"><span class="inbound-qty">{{ order?.inboundQuantity || 0 }} 套</span></el-descriptions-item>
        <el-descriptions-item label="采购流程">{{ purchaseFlowText }}</el-descriptions-item>
        <el-descriptions-item label="入库状态"><el-tag :type="getInboundStatusType(order?.inboundStatus)">{{ formatInboundStatus(order?.inboundStatus) }}</el-tag></el-descriptions-item>
        <el-descriptions-item label="付款状态"><el-tag :type="getPayStatusType(order?.payStatus)">{{ formatPayStatus(order?.payStatus) }}</el-tag></el-descriptions-item>
        <el-descriptions-item label="付款方式">{{ order?.payMethod || order?.paymentMethod || '-' }}</el-descriptions-item>
        <el-descriptions-item label="期望交货日期">{{ formatExpectDate(order?.expectDate || order?.expectedDate) }}</el-descriptions-item>
        <el-descriptions-item label="入库仓库">
          <el-tag type="success" effect="light">{{ order?.warehouseName || '-' }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="操作人">{{ order?.operatorName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="备注">
          <el-tooltip
            :content="order?.remark || '-'"
            placement="top-start"
            effect="dark"
            :show-after="200"
            popper-class="ep-table-overflow-tooltip purchase-order-detail-tooltip"
          >
            <div class="purchase-order-detail-remark-text">{{ order?.remark || '-' }}</div>
          </el-tooltip>
        </el-descriptions-item>
      </el-descriptions>
    </el-card>

    <!-- 入库进度 -->
    <el-card style="margin-top: 20px" v-if="shouldShowInboundSteps">
      <el-steps :active="inboundStepActive" finish-status="success">
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
import { purchaseApi } from '@/api'
import { formatTime } from '@/utils/time'
import ProductImageThumb from '@/components/ProductImageThumb.vue'
import { firstProductImageUrl, parseProductImageUrls } from '@/utils/productImages'
import { formatAmountDisplay } from '@/utils/moneyFormat'

const router = useRouter()
const route = useRoute()

const orderId = computed(() => route.params.id)
const order = ref(null)
const pageLoading = ref(true)

const normalizeInboundStatus = (status) => {
  const key = (status || '').toString().trim()
  const map = {
    pending: 'pending',
    partial: 'partial',
    completed: 'completed',
    cancelled: 'cancelled',
    待入库: 'pending',
    部分入库: 'partial',
    已完成: 'completed',
    已取消: 'cancelled'
  }
  return map[key] || key
}

const normalizePayStatus = (status) => {
  const key = (status || '').toString().trim()
  const map = {
    unpaid: 'unpaid',
    paid: 'paid',
    refunded: 'refunded',
    待付款: 'unpaid',
    已付款: 'paid',
    已退款: 'refunded'
  }
  return map[key] || key
}

const purchaseFlowText = computed(() => {
  if (!order.value) return '-'
  const inboundStatus = normalizeInboundStatus(order.value.inboundStatus)
  const payStatus = normalizePayStatus(order.value.payStatus)
  if (inboundStatus === 'cancelled') return '已取消'
  if (inboundStatus === 'completed') {
    return payStatus === 'paid' ? '采购完成（已入库并已付款）' : '已入库完成（待付款）'
  }
  if (inboundStatus === 'partial') return '采购进行中（部分入库）'
  if (inboundStatus === 'pending') return '采购进行中（待入库）'
  return formatInboundStatus(inboundStatus) || '-'
})

const normalizedInboundStatus = computed(() => normalizeInboundStatus(order.value?.inboundStatus))

const shouldShowInboundSteps = computed(() => normalizedInboundStatus.value !== 'cancelled')

const inboundStepActive = computed(() => {
  if (normalizedInboundStatus.value === 'completed') return 3
  if (normalizedInboundStatus.value === 'partial') return 2
  return 1
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
const getProductImage = (image) => {
  return firstProductImageUrl(image)
}

const getProductName = (name) => {
  return name || '-'
}

const formatExpectDate = (expectDate) => {
  if (!expectDate) return '-'
  if (typeof expectDate === 'string') return expectDate.substring(0, 10)
  return expectDate
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
  pageLoading.value = true
  try {
    const id = Number(orderId.value)
    if (!id) {
      ElMessage.warning('采购单 ID 无效')
      router.replace('/purchase')
      return
    }
    try {
      const orderRes = await purchaseApi.get(id)
      order.value = orderRes
        ? {
            ...orderRes,
            payMethod: orderRes.payMethod || orderRes.paymentMethod || '',
            expectDate: orderRes.expectDate || orderRes.expectedDate || null
          }
        : null
    } catch (e) {
      ElMessage.error(e.message || '加载采购单失败')
      router.replace('/purchase')
    }
  } finally {
    pageLoading.value = false
  }
})
const goBack = () => {
  if (window.history.length > 1) {
    router.back()
    return
  }
  router.replace('/purchase')
}
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

  :deep(.purchase-order-detail-descriptions .el-descriptions__label) {
    width: 88px;
    white-space: nowrap;
    vertical-align: top;
  }

  :deep(.purchase-order-detail-descriptions .el-descriptions__content) {
    vertical-align: top;
    min-width: 0;
  }

  :deep(.purchase-order-detail-descriptions .el-descriptions__table) {
    width: 100%;
    table-layout: fixed;
  }

  :deep(.purchase-order-detail-descriptions .purchase-order-detail-remark-text) {
    display: block;
    width: 100%;
    max-width: 100%;
    white-space: nowrap;
    text-overflow: ellipsis;
    line-height: 1.5;
    overflow: hidden;
    overflow-x: hidden;
    padding-right: 4px;
  }

  // 商品图片样式
  .product-cell {
    display: flex;
    align-items: center;
    gap: 12px;

    .product-thumb {
      flex-shrink: 0;
      border-radius: 6px;
      overflow: hidden;
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

<style lang="scss">
.el-popper.purchase-order-detail-tooltip[role='tooltip'],
.el-tooltip__popper.purchase-order-detail-tooltip {
  max-width: min(320px, calc(100vw - 48px)) !important;
  width: auto !important;
  box-sizing: border-box !important;
}

.el-popper.purchase-order-detail-tooltip[role='tooltip'] .el-popper__content,
.el-tooltip__popper.purchase-order-detail-tooltip .el-popper__content {
  max-width: 100% !important;
  white-space: normal !important;
  word-break: break-word !important;
  overflow-wrap: anywhere !important;
  line-height: 1.5;
}
</style>