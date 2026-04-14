<template>
  <div class="supplier-detail">
    <el-card>
      <template #header>
        <div class="card-header">
          <h3>供应商详情</h3>
          <div class="header-actions">
            <el-button type="primary" @click="editSupplier">编辑供应商</el-button>
            <el-button @click="goBack">返回</el-button>
          </div>
        </div>
      </template>
      <el-descriptions :column="2" border>
        <el-descriptions-item label="供应商名称"><span class="supplier-name">{{ supplier?.name }}</span></el-descriptions-item>
        <el-descriptions-item label="所属行业">{{ supplier?.industryNames || '—' }}</el-descriptions-item>
        <el-descriptions-item label="联系电话">{{ supplier?.phone }}</el-descriptions-item>
        <el-descriptions-item label="邮箱">{{ supplier?.email || '-' }}</el-descriptions-item>
        <el-descriptions-item label="地址" :span="2">{{ supplier?.address }}</el-descriptions-item>
        <el-descriptions-item label="备注" :span="2">{{ supplier?.remark || '-' }}</el-descriptions-item>
      </el-descriptions>
    </el-card>

    <!-- 合作统计 -->
    <el-card style="margin-top: 20px">
      <template #header><h3>合作统计</h3></template>
      <div class="stats-grid">
        <div class="stat-item">
          <div class="stat-icon"><el-icon><Document /></el-icon></div>
          <div class="stat-content">
            <span class="stat-value">{{ supplierStats.orders }}</span>
            <span class="stat-label">合作订单</span>
          </div>
        </div>
        <div class="stat-item">
          <div class="stat-icon"><el-icon><Money /></el-icon></div>
          <div class="stat-content">
            <span class="stat-value">¥{{ supplierStats.amount?.toLocaleString() }}</span>
            <span class="stat-label">总采购额</span>
          </div>
        </div>
      </div>
    </el-card>

    <!-- 采购记录 -->
    <el-card style="margin-top: 20px">
      <template #header><h3>采购记录</h3></template>
      <el-table :data="supplierOrders" style="width: 100%">
        <el-table-column label="采购单号" width="150">
          <template #default="{ row }"><span class="order-no">{{ row.orderNo }}</span></template>
        </el-table-column>
        <el-table-column label="商品" min-width="150">
          <template #default="{ row }">
            <div class="product-cell-mini">
              <img v-if="getProductImage(row.productId)" :src="getProductImage(row.productId)" class="product-thumb-mini" />
              <span v-else class="product-icon-mini">{{ getProductIcon(getProductName(row.productId, row.productName || row.product)) }}</span>
              <span class="product-name-mini">{{ getProductName(row.productId, row.productName || row.product) }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="数量" width="80">
          <template #default="{ row }">{{ row.totalQuantity }} 套</template>
        </el-table-column>
        <el-table-column label="金额" width="100">
          <template #default="{ row }"><span class="amount">¥{{ row.amount?.toLocaleString() }}</span></template>
        </el-table-column>
        <el-table-column label="入库状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getInboundStatusType(row.inboundStatus)" effect="light">{{ formatInboundStatus(row.inboundStatus) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="付款状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getPayStatusType(row.payStatus)" effect="light">{{ formatPayStatus(row.payStatus) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" width="140">
          <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="80">
          <template #default="{ row }">
            <el-button type="primary" link @click="viewOrder(row)"><el-icon><View /></el-icon></el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useDataStore } from '@/stores/data'
import { formatTime } from '@/utils/time'

const router = useRouter()
const route = useRoute()
const dataStore = useDataStore()

const supplierId = computed(() => route.params.id)
const supplier = ref(null)

// 获取当前供应商的最新名称（用于订单显示）
const currentSupplierName = computed(() => supplier.value?.name || '')

const supplierOrders = computed(() => {
  return dataStore.purchaseOrders.filter(o => o.supplierId === Number(supplierId.value))
})

// 计算统计数据
const supplierStats = computed(() => {
  const orders = supplierOrders.value
  const totalOrders = orders.length
  const totalAmount = orders.reduce((sum, o) => sum + (o.amount || 0), 0)
  return { orders: totalOrders, amount: totalAmount }
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

const getInboundStatusType = (status) => ({ 'completed': 'success', 'partial': 'warning', 'pending': 'info', 'cancelled': 'danger', '已完成': 'success', '部分入库': 'warning', '待入库': 'info', '已取消': 'danger' }[status] || 'info')
const getPayStatusType = (status) => ({ 'paid': 'success', 'unpaid': 'warning', 'refunded': 'danger', '已付款': 'success', '待付款': 'warning', '已退款': 'danger' }[status] || 'info')

// 获取商品图片
const getProductImage = (productId) => {
  const product = dataStore.products.find(p => p.id === productId)
  return product?.image || null
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
    dataStore.loadSupplierIndustries(),
    dataStore.loadPurchaseOrders(),
    dataStore.loadProducts()
  ])
  supplier.value = dataStore.suppliers.find(s => s.id === Number(supplierId.value))
})

const goBack = () => router.back()
const editSupplier = () => { router.push(`/purchase/supplier/edit/${supplierId.value}`) }
const viewOrder = (row) => { router.push(`/purchase/order/${row.id}`) }
</script>

<style lang="scss" scoped>
.supplier-detail {
  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    h3 { font-size: 18px; font-weight: 600; color: #303133; }
    .header-actions { display: flex; gap: 12px; }
  }
  .supplier-name { font-weight: 600; color: #303133; }
  .order-no { color: #E94560; font-family: monospace; }
  .amount { font-weight: 600; color: #E94560; }
  .stats-grid { display: grid; grid-template-columns: repeat(2, 1fr); gap: 20px; }
  .stat-item {
    display: flex;
    align-items: center;
    gap: 16px;
    padding: 16px;
    background: #F5F7FA;
    border-radius: 8px;
    .stat-icon {
      width: 48px;
      height: 48px;
      border-radius: 12px;
      background: rgba(233, 69, 96, 0.1);
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 24px;
      color: #E94560;
    }
    .stat-content {
      .stat-value { font-size: 24px; font-weight: 700; color: #303133; display: block; }
      .stat-label { font-size: 13px; color: #909399; }
    }
  }

  // 商品图片迷你样式
  .product-cell-mini {
    display: flex;
    align-items: center;
    gap: 8px;

    .product-thumb-mini {
      width: 32px;
      height: 32px;
      object-fit: cover;
      border-radius: 4px;
    }

    .product-icon-mini {
      width: 32px;
      height: 32px;
      display: flex;
      align-items: center;
      justify-content: center;
      background: #F5F7FA;
      border-radius: 4px;
      font-size: 16px;
    }

    .product-name-mini {
      font-size: 13px;
      color: #303133;
    }
  }
}
</style>