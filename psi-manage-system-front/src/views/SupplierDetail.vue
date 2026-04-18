<template>
  <div class="supplier-detail" v-loading="pageLoading" element-loading-text="加载中...">
    <el-card>
      <template #header>
        <div class="card-header">
          <div class="header-actions">
            <el-button v-if="canManageSupplier" type="primary" @click="editSupplier">编辑供应商</el-button>
            <el-button @click="goBack">返回</el-button>
          </div>
        </div>
      </template>
      <el-descriptions :column="2" border>
        <el-descriptions-item label="供应商名称"><span class="supplier-name">{{ supplier?.name }}</span></el-descriptions-item>
        <el-descriptions-item label="所属行业">{{ supplier?.industryNames || '—' }}</el-descriptions-item>
        <el-descriptions-item label="联系人">{{ supplier?.contact || '-' }}</el-descriptions-item>
        <el-descriptions-item label="联系电话">{{ supplier?.phone || '-' }}</el-descriptions-item>
        <el-descriptions-item label="邮箱" :span="2">{{ formatSupplierEmailDisplay(supplier?.email) }}</el-descriptions-item>
        <el-descriptions-item label="地址" :span="2">{{ supplier?.address }}</el-descriptions-item>
        <el-descriptions-item label="备注" :span="2">{{ supplier?.remark || '-' }}</el-descriptions-item>
      </el-descriptions>
    </el-card>

    <!-- 合作统计 -->
    <el-card style="margin-top: 20px">
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
            <span class="stat-value">¥{{ formatAmountDisplay(supplierStats.amount ?? 0) }}</span>
            <span class="stat-label">总采购额</span>
          </div>
        </div>
      </div>
    </el-card>

    <!-- 采购记录 -->
    <el-card style="margin-top: 20px">
      <el-table :data="supplierOrders" empty-text="暂无数据" style="width: 100%">
        <el-table-column label="采购单号" width="150">
          <template #default="{ row }"><span class="order-no">{{ row.orderNo }}</span></template>
        </el-table-column>
        <el-table-column label="商品" min-width="150">
          <template #default="{ row }">
            <div class="product-cell-mini">
              <img v-if="getProductImage(row)" :src="getProductImage(row)" class="product-thumb-mini" />
              <span v-else class="product-icon-mini">{{ getProductIcon(getProductName(row)) }}</span>
              <span class="product-name-mini">{{ getProductName(row) }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="数量" width="80">
          <template #default="{ row }">{{ row.totalQuantity }} 套</template>
        </el-table-column>
        <el-table-column label="金额" width="100">
          <template #default="{ row }"><span class="amount">¥{{ formatAmountDisplay(row.amount ?? 0) }}</span></template>
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
        <el-table-column label="创建时间" width="176">
          <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="80">
          <template #default="{ row }">
            <el-button type="primary" link @click="viewOrder(row)"><el-icon><View /></el-icon></el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="ordersCurrentPage"
          v-model:page-size="ordersPageSize"
          :total="ordersTotal"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next, jumper"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { supplierApi, purchaseApi } from '@/api'
import { formatTime } from '@/utils/time'
import { firstProductImageUrl } from '@/utils/productImages'
import { formatAmountDisplay } from '@/utils/moneyFormat'

/** 供应商邮箱只读展示：空或纯空白时统一为「未填写」 */
const formatSupplierEmailDisplay = (email) => {
  const t = email == null ? '' : String(email).trim()
  return t || '未填写'
}

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const canManageSupplier = computed(() => userStore.hasPermission('purchase:supplier'))

const supplierId = computed(() => route.params.id)
const supplier = ref(null)
const pageLoading = ref(true)
const purchaseOrders = ref([])
const ordersCurrentPage = ref(1)
const ordersPageSize = ref(10)
const ordersTotal = ref(0)

// 获取当前供应商的最新名称（用于订单显示）
const currentSupplierName = computed(() => supplier.value?.name || '')

const supplierOrders = computed(() => {
  return purchaseOrders.value
})

// 合作订单数、总采购额与列表一致：来自 GET /suppliers/{id} 聚合字段
const supplierStats = computed(() => {
  const s = supplier.value
  return {
    orders: Number(s?.purchaseOrderCount ?? 0),
    amount: Number(s?.totalPurchaseAmount ?? 0)
  }
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

// 商品图片以采购单返回为准
const getProductImage = (row) => {
  return firstProductImageUrl(row?.productImage || row?.image)
}

// 商品名称以采购单返回为准
const getProductName = (row) => {
  return row?.productName || row?.product || '-'
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
    if (!userStore.hasPermission('purchase:supplier')) {
      ElMessage.warning('无供应商管理权限')
      router.replace('/purchase')
      return
    }
    const id = Number(supplierId.value)
    if (!id) {
      ElMessage.warning('供应商 ID 无效')
      router.replace('/purchase')
      return
    }
    try {
      supplier.value = await supplierApi.get(id)
    } catch (e) {
      ElMessage.error(e.message || '加载供应商失败')
      router.replace('/purchase')
      return
    }
    await fetchSupplierOrders()
  } finally {
    pageLoading.value = false
  }
})

const fetchSupplierOrders = async () => {
  const id = Number(supplierId.value)
  const res = await purchaseApi.list({
    page: ordersCurrentPage.value,
    size: ordersPageSize.value,
    supplierId: id
  })
  purchaseOrders.value = res.list || []
  ordersTotal.value = Number(res.total) || 0
}

watch([ordersCurrentPage, ordersPageSize], () => {
  fetchSupplierOrders()
})

const goBack = () => router.back()
const editSupplier = () => { router.push(`/purchase/supplier/edit/${supplierId.value}`) }
const viewOrder = (row) => { router.push(`/purchase/order/${row.id}`) }
</script>

<style lang="scss" scoped>
.supplier-detail {
  .pagination-wrapper {
    display: flex;
    justify-content: flex-end;
    padding-top: 16px;
  }
  .card-header {
    display: flex;
    justify-content: flex-end;
    align-items: center;
    .header-actions { display: flex; gap: 12px; }
  }
  .supplier-name { font-weight: 600; color: #303133; }
  .order-no { color: #E94560; }
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