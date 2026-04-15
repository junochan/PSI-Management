<template>
  <div class="purchase-order-detail">
    <el-card>
      <template #header>
        <div class="card-header">
          <h3>采购单详情</h3>
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
        <el-descriptions-item label="付款方式">{{ order?.payMethod || '-' }}</el-descriptions-item>
        <el-descriptions-item label="期望交货日期">{{ order?.expectDate || '-' }}</el-descriptions-item>
        <el-descriptions-item label="入库仓库">
          <el-tag type="success" effect="light">{{ getWarehouseName(order?.warehouseId) || order?.warehouseName || '-' }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="操作人">{{ order?.operatorName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="备注" :span="2">{{ order?.remark || '-' }}</el-descriptions-item>
      </el-descriptions>
    </el-card>

    <!-- 入库进度 -->
    <el-card style="margin-top: 20px" v-if="order?.inboundStatus !== 'completed' && order?.inboundStatus !== 'cancelled'">
      <template #header><h3>入库进度</h3></template>
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
import { useDataStore } from '@/stores/data'
import { formatTime } from '@/utils/time'
import { firstProductImageUrl } from '@/utils/productImages'

const router = useRouter()
const route = useRoute()
const dataStore = useDataStore()

const orderId = computed(() => route.params.id)
const order = ref(null)

// 仓库列表
const warehousesList = computed(() => dataStore.warehouses || [])

// 动态获取仓库名称 - 从仓库列表中根据warehouseId查找
const getWarehouseName = (warehouseId) => {
  const warehouse = warehousesList.value.find(w => w.id === warehouseId)
  return warehouse?.name || ''
}

// 通过 supplierId 获取最新的供应商名称
const supplierDisplayName = computed(() => {
  if (!order.value) return ''
  // 如果有 supplierId，从供应商列表中获取最新的名称
  if (order.value.supplierId) {
    const supplier = dataStore.suppliers.find(s => s.id === order.value.supplierId)
    if (supplier) return supplier.name
  }
  // 否则使用存储的名称
  return order.value.supplierName || order.value.supplier
})

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
  const product = dataStore.products.find(p => p.id === productId)
  return firstProductImageUrl(product?.image)
}

// 获取商品名称（从商品列表动态获取最新名称）
const getProductName = (productId, fallbackName) => {
  const product = dataStore.products.find(p => p.id === productId)
  return product?.name || fallbackName || '-'
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
  await Promise.all([
    dataStore.loadSuppliers(),
    dataStore.loadPurchaseOrders(),
    dataStore.loadProducts(),
    dataStore.loadCategories(),
    dataStore.loadWarehouses()
  ])
  order.value = dataStore.purchaseOrders.find(o => o.id === Number(orderId.value))
})
const goBack = () => router.back()
</script>

<style lang="scss" scoped>
.purchase-order-detail {
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