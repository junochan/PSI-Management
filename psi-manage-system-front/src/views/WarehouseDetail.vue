<template>
  <div class="warehouse-detail">
    <el-card>
      <template #header>
        <div class="card-header">
          <h3>仓库详情</h3>
          <div class="header-actions">
            <el-button type="primary" @click="editWarehouse">编辑仓库</el-button>
            <el-button @click="viewStock">查看库存</el-button>
            <el-button @click="goBack">返回</el-button>
          </div>
        </div>
      </template>
      <el-descriptions :column="2" border>
        <el-descriptions-item label="仓库名称"><span class="warehouse-name">{{ warehouse?.name }}</span></el-descriptions-item>
        <el-descriptions-item label="仓库编码">{{ warehouse?.code || '-' }}</el-descriptions-item>
        <el-descriptions-item label="仓库地址">{{ warehouse?.address }}</el-descriptions-item>
        <el-descriptions-item label="负责人">{{ warehouse?.managerName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="商品种类">{{ warehouse?.totalCategories || warehouse?.categories }} 种</el-descriptions-item>
        <el-descriptions-item label="总库存">{{ warehouse?.totalStock || warehouse?.stock }} 件</el-descriptions-item>
        <el-descriptions-item label="库存价值">¥{{ (warehouse?.totalValue || warehouse?.value) }} 万</el-descriptions-item>
        <el-descriptions-item label="容量利用率">
          <div class="capacity-display">
            <el-progress :percentage="warehouse?.capacity" :color="warehouse?.capacity > 70 ? '#67C23A' : '#E6A23C'" />
          </div>
        </el-descriptions-item>
        <el-descriptions-item label="仓库状态">
          <el-tag :type="warehouse?.status === 1 ? 'success' : 'danger'" effect="light">{{ warehouse?.status === 1 ? '正常' : '停用' }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="备注">{{ warehouse?.remark || '-' }}</el-descriptions-item>
      </el-descriptions>
    </el-card>

    <!-- 仓库统计 -->
    <el-card style="margin-top: 20px">
      <template #header><h3>仓库统计</h3></template>
      <div class="stats-grid">
        <div class="stat-item">
          <div class="stat-icon stat-icon--purple"><el-icon><Box /></el-icon></div>
          <div class="stat-content">
            <span class="stat-value">{{ warehouse?.categories }}</span>
            <span class="stat-label">商品种类</span>
          </div>
        </div>
        <div class="stat-item">
          <div class="stat-icon stat-icon--green"><el-icon><Document /></el-icon></div>
          <div class="stat-content">
            <span class="stat-value">{{ warehouse?.stock }}</span>
            <span class="stat-label">总库存件数</span>
          </div>
        </div>
        <div class="stat-item">
          <div class="stat-icon stat-icon--red"><el-icon><Money /></el-icon></div>
          <div class="stat-content">
            <span class="stat-value">¥{{ warehouse?.value }}万</span>
            <span class="stat-label">库存价值</span>
          </div>
        </div>
        <div class="stat-item">
          <div class="stat-icon stat-icon--blue"><el-icon><Shop /></el-icon></div>
          <div class="stat-content">
            <span class="stat-value">{{ warehouse?.capacity }}%</span>
            <span class="stat-label">容量利用率</span>
          </div>
        </div>
      </div>
    </el-card>

    <!-- 库存商品 -->
    <el-card style="margin-top: 20px">
      <template #header>
        <div class="card-header">
          <h3>库存商品</h3>
          <el-input v-model="searchKeyword" placeholder="搜索商品..." prefix-icon="Search" clearable style="width: 200px" />
        </div>
      </template>
      <el-table :data="filteredStock" empty-text="暂无数据" style="width: 100%">
        <el-table-column label="SKU" width="120">
          <template #default="{ row }"><span class="sku">{{ row.sku }}</span></template>
        </el-table-column>
        <el-table-column label="商品名称" prop="name" min-width="180" />
        <el-table-column label="规格" prop="spec" width="150" />
        <el-table-column label="库存数量" width="100">
          <template #default="{ row }">
            <span :class="{ 'low-stock': row.stock < row.safeStock }">{{ row.stock }}</span>
          </template>
        </el-table-column>
        <el-table-column label="安全库存" prop="safeStock" width="80" />
        <el-table-column label="库存状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getStockStatusType(row.status)" effect="light">{{ row.status }}</el-tag>
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

const router = useRouter()
const route = useRoute()
const dataStore = useDataStore()

const warehouseId = computed(() => route.params.id)
const warehouse = ref(null)
const searchKeyword = ref('')

const filteredStock = computed(() => {
  const stock = dataStore.inventoryData.filter(i => i.warehouse?.includes(warehouse?.value?.name?.split('仓库')[0]))
  if (!searchKeyword.value) return stock
  return stock.filter(i => i.name.includes(searchKeyword.value) || i.sku.includes(searchKeyword.value))
})

const getStockStatusType = (status) => ({ '正常': 'success', '偏低': 'warning', '紧急补货': 'danger' }[status] || 'info')

onMounted(async () => {
  await dataStore.loadWarehouses()
  warehouse.value = dataStore.warehouses.find(w => w.id === Number(warehouseId.value))
})

const goBack = () => router.back()
const editWarehouse = () => { ElMessage.success('编辑仓库功能已触发') }
const viewStock = () => { router.push('/inventory') }
</script>

<style lang="scss" scoped>
.warehouse-detail {
  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    h3 { font-size: 18px; font-weight: 600; color: #303133; }
    .header-actions { display: flex; gap: 12px; }
  }
  .warehouse-name { font-weight: 600; color: #303133; }
  .capacity-display { width: 100%; }
  .sku { color: #E94560; font-family: monospace; }
  .low-stock { color: #E6A23C; font-weight: 600; }
  .stats-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 20px; }
  .stat-item {
    display: flex;
    align-items: center;
    gap: 16px;
    padding: 20px;
    background: #F5F7FA;
    border-radius: 12px;
    .stat-icon {
      width: 48px;
      height: 48px;
      border-radius: 12px;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 24px;
      &--purple { background: rgba(139, 92, 246, 0.1); color: #8B5CF6; }
      &--green { background: rgba(103, 194, 58, 0.1); color: #67C23A; }
      &--red { background: rgba(245, 108, 108, 0.1); color: #F56C6C; }
      &--blue { background: rgba(64, 158, 255, 0.1); color: #409EFF; }
    }
    .stat-content {
      .stat-value { font-size: 24px; font-weight: 700; color: #303133; display: block; }
      .stat-label { font-size: 13px; color: #909399; }
    }
  }
}
</style>