<template>
  <div class="transfer-detail">
    <el-card>
      <template #header>
        <div class="card-header">
          <div class="header-actions">
            <el-button type="primary" @click="confirmReceive" v-if="isTransferring(transfer?.status)">确认收货</el-button>
            <el-button type="danger" @click="cancelTransfer" v-if="isTransferring(transfer?.status)">取消调拨</el-button>
            <el-button @click="printTransfer">打印调拨单</el-button>
            <el-button @click="goBack">返回</el-button>
          </div>
        </div>
      </template>
      <el-descriptions :column="2" border>
        <el-descriptions-item label="调拨单号"><span class="order-no">{{ transfer?.orderNo }}</span></el-descriptions-item>
        <el-descriptions-item label="调拨状态">
          <el-tag :type="getTransferStatusType(transfer?.status)" effect="light">{{ formatTransferStatus(transfer?.status) }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="商品">{{ transfer?.product }}</el-descriptions-item>
        <el-descriptions-item label="调拨数量">{{ transfer?.quantity }}</el-descriptions-item>
        <el-descriptions-item label="调出仓库">
          <el-tag type="info" effect="light">{{ transfer?.from }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="调入仓库">
          <el-tag type="success" effect="light">{{ transfer?.to }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ formatTime(transfer?.createTime) }}</el-descriptions-item>
        <el-descriptions-item label="操作人">{{ transfer?.operator }}</el-descriptions-item>
      </el-descriptions>
    </el-card>

    <!-- 调拨进度 -->
    <el-card style="margin-top: 20px">
      <el-steps :active="isTransferCompleted(transfer?.status) ? 3 : 2" finish-status="success">
        <el-step title="创建调拨" description="调拨单已创建" />
        <el-step title="商品运输" :description="isTransferCompleted(transfer?.status) ? '运输完成' : '正在运输中'" />
        <el-step title="确认收货" :description="isTransferCompleted(transfer?.status) ? '已确认收货' : '等待确认'" />
      </el-steps>
    </el-card>

    <!-- 调拨详情 -->
    <el-card style="margin-top: 20px">
      <div class="transfer-info">
        <div class="warehouse-box">
          <div class="warehouse-label">调出仓库</div>
          <div class="warehouse-name">{{ transfer?.from }}</div>
          <el-icon class="arrow-icon"><ArrowRight /></el-icon>
          <div class="warehouse-label">调入仓库</div>
          <div class="warehouse-name">{{ transfer?.to }}</div>
        </div>
        <div class="transfer-meta">
          <div class="meta-item">
            <el-icon><Box /></el-icon>
            <span>商品：{{ transfer?.product }}</span>
          </div>
          <div class="meta-item">
            <el-icon><Sort /></el-icon>
            <span>数量：{{ transfer?.quantity }}</span>
          </div>
          <div class="meta-item">
            <el-icon><User /></el-icon>
            <span>操作人：{{ transfer?.operator }}</span>
          </div>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { inventoryApi } from '@/api'
import { formatTime } from '@/utils/time'

const router = useRouter()
const route = useRoute()

const transferId = computed(() => route.params.id)
const transfer = ref(null)

// 状态格式化函数
const formatTransferStatus = (status) => {
  const statusMap = { 'transferring': '调拨中', 'completed': '已完成', 'cancelled': '已取消', '调拨中': '调拨中', '已完成': '已完成', '已取消': '已取消' }
  return statusMap[status] || status
}
const getTransferStatusType = (status) => {
  const formattedStatus = formatTransferStatus(status)
  return { '已完成': 'success', '调拨中': 'warning', '已取消': 'danger' }[formattedStatus] || 'info'
}
// 判断是否为调拨中状态
const isTransferring = (status) => formatTransferStatus(status) === '调拨中'
// 判断是否为已完成状态
const isTransferCompleted = (status) => formatTransferStatus(status) === '已完成'

onMounted(async () => {
  const id = Number(transferId.value)
  if (!id) {
    ElMessage.warning('调拨单 ID 无效')
    router.replace('/inventory')
    return
  }
  try {
    transfer.value = await inventoryApi.transferGet(id)
  } catch (e) {
    ElMessage.error(e.message || '加载调拨单失败')
    router.replace('/inventory')
  }
})

const goBack = () => router.back()
const confirmReceive = () => {
  ElMessage.success('调拨单已确认收货')
  router.push('/inventory')
}
const cancelTransfer = () => {
  ElMessage.success('调拨单已取消')
  router.push('/inventory')
}
const printTransfer = () => { ElMessage.success('打印调拨单功能已触发') }
</script>

<style lang="scss" scoped>
.transfer-detail {
  .card-header {
    display: flex;
    justify-content: flex-end;
    align-items: center;
    .header-actions { display: flex; gap: 12px; }
  }
  .order-no { color: #E94560; font-family: monospace; }
  .transfer-info {
    .warehouse-box {
      display: flex;
      align-items: center;
      justify-content: center;
      gap: 24px;
      padding: 32px;
      background: #F5F7FA;
      border-radius: 12px;
      margin-bottom: 24px;
      .warehouse-label { font-size: 13px; color: #909399; }
      .warehouse-name { font-size: 18px; font-weight: 600; color: #303133; margin-top: 4px; }
      .arrow-icon { font-size: 32px; color: #E94560; }
    }
    .transfer-meta {
      display: flex;
      gap: 24px;
      .meta-item {
        display: flex;
        align-items: center;
        gap: 8px;
        font-size: 14px;
        color: #606266;
        .el-icon { font-size: 18px; color: #E94560; }
      }
    }
  }
}
</style>