<template>
  <div class="inbound-detail">
    <el-card>
      <template #header>
        <div class="card-header">
          <h3>入库单详情</h3>
          <div class="header-actions">
            <el-button @click="printInbound">打印入库单</el-button>
            <el-button @click="goBack">返回</el-button>
          </div>
        </div>
      </template>
      <el-descriptions :column="2" border>
        <el-descriptions-item label="入库单号"><span class="order-no success">{{ inbound?.orderNo }}</span></el-descriptions-item>
        <el-descriptions-item label="关联采购单"><span class="order-no">{{ inbound?.purchaseOrderNo || inbound?.purchaseNo }}</span></el-descriptions-item>
        <el-descriptions-item label="供应商">{{ supplierDisplayName }}</el-descriptions-item>
        <el-descriptions-item label="商品">{{ inbound?.productName || inbound?.product }}</el-descriptions-item>
        <el-descriptions-item label="入库数量">{{ inbound?.quantity }} 件</el-descriptions-item>
        <el-descriptions-item label="入库仓库">
          <el-tag type="success" effect="light">{{ getWarehouseName(inbound?.warehouseId) || inbound?.warehouseName || inbound?.warehouse }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="入库时间">{{ formatTime(inbound?.createTime || inbound?.time) }}</el-descriptions-item>
        <el-descriptions-item label="操作人">{{ inbound?.operatorName || inbound?.operator }}</el-descriptions-item>
        <el-descriptions-item label="备注" :span="2">{{ inbound?.remark || '-' }}</el-descriptions-item>
      </el-descriptions>
    </el-card>

    <!-- 入库详情 -->
    <el-card style="margin-top: 20px">
      <template #header><h3>入库详情</h3></template>
      <div class="inbound-info">
        <div class="info-row">
          <div class="info-item">
            <el-icon class="info-icon"><Document /></el-icon>
            <div class="info-content">
              <span class="info-label">采购单号</span>
              <span class="info-value">{{ inbound?.purchaseOrderNo || inbound?.purchaseNo }}</span>
            </div>
          </div>
          <div class="info-item">
            <el-icon class="info-icon"><User /></el-icon>
            <div class="info-content">
              <span class="info-label">供应商</span>
              <span class="info-value">{{ supplierDisplayName }}</span>
            </div>
          </div>
          <div class="info-item">
            <el-icon class="info-icon"><Box /></el-icon>
            <div class="info-content">
              <span class="info-label">商品名称</span>
              <span class="info-value">{{ inbound?.productName || inbound?.product }}</span>
            </div>
          </div>
        </div>
        <div class="info-row">
          <div class="info-item">
            <el-icon class="info-icon"><Sort /></el-icon>
            <div class="info-content">
              <span class="info-label">入库数量</span>
              <span class="info-value">{{ inbound?.quantity }} 件</span>
            </div>
          </div>
          <div class="info-item">
            <el-icon class="info-icon"><Shop /></el-icon>
            <div class="info-content">
              <span class="info-label">入库仓库</span>
              <span class="info-value">{{ getWarehouseName(inbound?.warehouseId) || inbound?.warehouseName || inbound?.warehouse }}</span>
            </div>
          </div>
          <div class="info-item">
            <el-icon class="info-icon"><Clock /></el-icon>
            <div class="info-content">
              <span class="info-label">入库时间</span>
              <span class="info-value">{{ formatTime(inbound?.createTime || inbound?.time) }}</span>
            </div>
          </div>
        </div>
      </div>
    </el-card>

    <!-- 操作记录 -->
    <el-card style="margin-top: 20px">
      <template #header><h3>操作记录</h3></template>
      <el-timeline>
        <el-timeline-item timestamp="采购单创建" placement="top" color="#409EFF">
          <el-card>
            <h4>采购单创建</h4>
            <p>采购单 {{ inbound?.purchaseOrderNo || inbound?.purchaseNo }} 已创建</p>
          </el-card>
        </el-timeline-item>
        <el-timeline-item timestamp="供应商发货" placement="top" color="#E6A23C">
          <el-card>
            <h4>供应商发货</h4>
            <p>供应商 {{ supplierDisplayName }} 已发货</p>
          </el-card>
        </el-timeline-item>
        <el-timeline-item :timestamp="formatTime(inbound?.createTime || inbound?.time)" placement="top" color="#67C23A">
          <el-card>
            <h4>确认入库</h4>
            <p>商品已入库至 {{ getWarehouseName(inbound?.warehouseId) || inbound?.warehouseName || inbound?.warehouse }}，操作人：{{ inbound?.operatorName || inbound?.operator }}</p>
          </el-card>
        </el-timeline-item>
      </el-timeline>
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

const inboundId = computed(() => route.params.id)
const inbound = ref(null)

// 仓库列表
const warehousesList = computed(() => dataStore.warehouses || [])

// 动态获取仓库名称 - 从仓库列表中根据warehouseId查找
const getWarehouseName = (warehouseId) => {
  const warehouse = warehousesList.value.find(w => w.id === warehouseId)
  return warehouse?.name || ''
}

// 通过 supplierId 获取最新的供应商名称
const supplierDisplayName = computed(() => {
  if (!inbound.value) return ''
  // 如果有 supplierId，从供应商列表中获取最新的名称
  if (inbound.value.supplierId) {
    const supplier = dataStore.suppliers.find(s => s.id === inbound.value.supplierId)
    if (supplier) return supplier.name
  }
  // 否则使用存储的名称
  return inbound.value.supplierName || inbound.value.supplier
})

onMounted(async () => {
  await dataStore.loadSuppliers()
  await dataStore.loadWarehouses()
  await dataStore.loadInboundRecords()
  inbound.value = dataStore.inboundRecords.find(i => i.id === Number(inboundId.value))
})

const goBack = () => router.back()
const printInbound = () => { ElMessage.success('打印入库单功能已触发') }
</script>

<style lang="scss" scoped>
.inbound-detail {
  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    h3 { font-size: 18px; font-weight: 600; color: #303133; }
    .header-actions { display: flex; gap: 12px; }
  }
  .order-no { color: #E94560; font-family: monospace; &.success { color: #67C23A; } }
  .inbound-info {
    .info-row {
      display: grid;
      grid-template-columns: repeat(3, 1fr);
      gap: 20px;
      margin-bottom: 20px;
      &:last-child { margin-bottom: 0; }
    }
    .info-item {
      display: flex;
      align-items: center;
      gap: 12px;
      padding: 16px;
      background: #F5F7FA;
      border-radius: 8px;
      .info-icon { font-size: 24px; color: #E94560; }
      .info-content {
        .info-label { font-size: 12px; color: #909399; display: block; }
        .info-value { font-size: 16px; font-weight: 600; color: #303133; }
      }
    }
  }
}
</style>