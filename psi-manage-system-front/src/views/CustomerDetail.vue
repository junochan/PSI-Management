<template>
  <div class="customer-detail">
    <el-card>
      <template #header>
        <div class="card-header">
          <h3>客户详情</h3>
          <div class="header-actions">
            <el-button type="primary" @click="editCustomer">编辑客户</el-button>
            <el-button @click="goBack">返回</el-button>
          </div>
        </div>
      </template>
      <el-descriptions :column="2" border>
        <el-descriptions-item label="客户名称"><span class="customer-name">{{ customer?.name }}</span></el-descriptions-item>
        <el-descriptions-item label="客户类型">
          <el-tag :type="customer?.type === '企业' ? 'warning' : 'info'" effect="light">{{ customer?.type }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="会员等级">
          <el-tag :type="(customer?.vipLevel || '普通') === 'VIP' ? 'warning' : 'info'" effect="light">{{ customer?.vipLevel || '普通' }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="联系电话">{{ customer?.phone }}</el-descriptions-item>
        <el-descriptions-item label="收货地址" :span="2">{{ customer?.address }}</el-descriptions-item>
        <el-descriptions-item label="备注" :span="2">{{ customer?.remark || '-' }}</el-descriptions-item>
      </el-descriptions>
    </el-card>

    <!-- 消费统计 -->
    <el-card style="margin-top: 20px">
      <template #header><h3>消费统计</h3></template>
      <div class="stats-grid">
        <div class="stat-item">
          <div class="stat-icon"><el-icon><Money /></el-icon></div>
          <div class="stat-content">
            <span class="stat-value">¥{{ customerStats.totalAmount?.toLocaleString() }}</span>
            <span class="stat-label">累计消费</span>
          </div>
        </div>
        <div class="stat-item">
          <div class="stat-icon"><el-icon><Document /></el-icon></div>
          <div class="stat-content">
            <span class="stat-value">{{ customerStats.totalOrders }}</span>
            <span class="stat-label">订单数量</span>
          </div>
        </div>
        <div class="stat-item">
          <div class="stat-icon"><el-icon><TrendCharts /></el-icon></div>
          <div class="stat-content">
            <span class="stat-value">¥{{ customerStats.avgAmount?.toLocaleString() }}</span>
            <span class="stat-label">平均客单价</span>
          </div>
        </div>
      </div>
    </el-card>

    <!-- 历史订单 -->
    <el-card style="margin-top: 20px">
      <template #header><h3>历史订单</h3></template>
      <el-table :data="customerOrders" empty-text="暂无数据" style="width: 100%">
        <el-table-column label="订单编号" width="150">
          <template #default="{ row }"><span class="order-no">{{ row.orderNo }}</span></template>
        </el-table-column>
        <el-table-column label="商品" min-width="180" show-overflow-tooltip>
          <template #default="{ row }">
            <div class="product-cell-mini">
              <img v-if="getProductImage(row.productId)" :src="getProductImage(row.productId)" class="product-thumb-mini" />
              <span v-else class="product-icon-mini">{{ getProductIcon(getProductName(row.productId, row.productName || row.product)) }}</span>
              <span class="product-name-mini">{{ getProductName(row.productId, row.productName || row.product) }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="数量" width="80" align="center">
          <template #default="{ row }">{{ row.quantity }}</template>
        </el-table-column>
        <el-table-column label="金额" width="100" align="right">
          <template #default="{ row }"><span class="amount">¥{{ (row.amount || 0).toLocaleString() }}</span></template>
        </el-table-column>
        <el-table-column label="订单状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)" effect="light">{{ formatOrderStatus(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="下单时间" width="140" show-overflow-tooltip>
          <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="80" align="center">
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

const customerId = computed(() => route.params.id)
const customer = ref(null)

// 消费统计 - 从订单数据计算
const customerStats = computed(() => {
  const orders = customerOrders.value
  const totalOrders = orders.length
  const totalAmount = orders.reduce((sum, o) => sum + (o.amount || 0), 0)
  const avgAmount = totalOrders > 0 ? Math.round(totalAmount / totalOrders) : 0
  return { totalOrders, totalAmount, avgAmount }
})

// 状态格式化函数
const formatOrderStatus = (status) => {
  const statusMap = { 'pending': '待发货', 'shipped': '处理中', 'completed': '已完成', 'cancelled': '已取消' }
  return statusMap[status] || status
}

const getStatusType = (status) => ({ 'completed': 'success', 'shipped': 'warning', 'pending': 'warning', 'cancelled': 'danger', '已完成': 'success', '处理中': 'warning', '待发货': 'warning', '已取消': 'danger' }[status] || 'info')

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
    dataStore.loadCustomers(),
    dataStore.loadSalesOrders(),
    dataStore.loadProducts()
  ])
  customer.value = dataStore.customers.find(c => c.id === Number(customerId.value))
})

const goBack = () => router.back()
const editCustomer = () => { router.push(`/sales/customer/edit/${customerId.value}`) }
const viewOrder = (row) => { router.push(`/sales/order/${row.id}`) }
</script>

<style lang="scss" scoped>
.customer-detail {
  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    h3 { font-size: 18px; font-weight: 600; color: #303133; }
    .header-actions { display: flex; gap: 12px; }
  }
  .customer-name { font-weight: 600; color: #303133; }
  .order-no { color: #E94560; font-family: monospace; }
  .amount { font-weight: 600; color: #E94560; }
  .stats-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 20px; }
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