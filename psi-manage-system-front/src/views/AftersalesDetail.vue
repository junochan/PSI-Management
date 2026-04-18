<template>
  <div class="aftersales-detail" v-loading="pageLoading" element-loading-text="加载中...">
    <el-card>
      <template #header>
        <div class="card-header">
          <div class="header-actions">
            <el-button type="primary" @click="handleResolve" v-if="isProcessing(order?.status)">确认解决</el-button>
            <el-button @click="goBack">返回</el-button>
          </div>
        </div>
      </template>
      <el-descriptions :column="2" border>
        <el-descriptions-item label="工单编号"><span class="order-no">{{ order?.orderNo }}</span></el-descriptions-item>
        <el-descriptions-item label="关联订单"><span class="order-no">{{ order?.salesNo }}</span></el-descriptions-item>
        <el-descriptions-item label="客户">{{ order?.customer }}</el-descriptions-item>
        <el-descriptions-item label="问题类型">
          <el-tag :type="getTypeColor(order?.type)" effect="light">{{ order?.type }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="工单状态" :span="2">
          <el-tag :type="getAftersalesStatusType(order?.status)" effect="light">{{ formatAftersalesStatus(order?.status) }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="问题描述" :span="2">{{ order?.content }}</el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ formatTime(order?.createTime) }}</el-descriptions-item>
      </el-descriptions>
    </el-card>

    <!-- 处理进度 -->
    <el-card style="margin-top: 20px">
      <el-steps :active="isResolved(order?.status) ? 3 : 2" finish-status="success">
        <el-step title="工单创建" description="售后工单已提交" />
        <el-step title="问题审核" description="正在核实问题详情" />
        <el-step title="处理解决" :description="isResolved(order?.status) ? '问题已解决' : '等待处理'" />
      </el-steps>
    </el-card>

    <!-- 处理记录 -->
    <el-card style="margin-top: 20px">
      <template #header>
        <div class="card-header">
          <el-button type="primary" @click="addRecord"><el-icon><Plus /></el-icon>添加记录</el-button>
        </div>
      </template>
      <el-timeline>
        <el-timeline-item timestamp="2026-04-10 15:30" placement="top" color="#E94560">
          <el-card>
            <h4>工单创建</h4>
            <p>客户提交售后申请，问题描述：{{ order?.content }}</p>
          </el-card>
        </el-timeline-item>
        <el-timeline-item timestamp="2026-04-10 16:00" placement="top" v-if="isResolved(order?.status)">
          <el-card>
            <h4>问题审核</h4>
            <p>客服已确认问题，安排处理方案</p>
          </el-card>
        </el-timeline-item>
        <el-timeline-item timestamp="2026-04-11 10:00" placement="top" color="#67C23A" v-if="isResolved(order?.status)">
          <el-card>
            <h4>问题解决</h4>
            <p>售后问题已处理完成，客户满意</p>
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
import { aftersalesApi } from '@/api'
import { formatTime } from '@/utils/time'

const router = useRouter()
const route = useRoute()
const orderId = computed(() => route.params.id)
const order = ref(null)
const pageLoading = ref(true)

const getTypeColor = (type) => ({ '质量问题': 'warning', '退换货': 'info', '物流问题': 'info', '发票问题': 'info' }[type] || 'info')

// 状态格式化函数
const formatAftersalesStatus = (status) => {
  const statusMap = {
    'pending': '待处理',
    'processing': '处理中',
    'resolved': '已解决',
    'completed': '已完成',
    'closed': '已关闭',
    '待处理': '待处理',
    '处理中': '处理中',
    '已解决': '已解决',
    '已完成': '已完成',
    '已关闭': '已关闭'
  }
  return statusMap[status] || status
}
const getAftersalesStatusType = (status) => {
  const formattedStatus = formatAftersalesStatus(status)
  const typeMap = {
    '待处理': 'warning',
    '处理中': 'primary',
    '已解决': 'success',
    '已完成': 'success',
    '已关闭': 'info'
  }
  return typeMap[formattedStatus] || 'info'
}
// 判断是否为处理中状态（兼容英文和中文）
const isProcessing = (status) => {
  return formatAftersalesStatus(status) === '待处理' || formatAftersalesStatus(status) === '处理中'
}
// 判断是否为已解决状态（兼容英文和中文）
const isResolved = (status) => {
  return formatAftersalesStatus(status) === '已解决' || formatAftersalesStatus(status) === '已完成' || formatAftersalesStatus(status) === '已关闭'
}

onMounted(async () => {
  pageLoading.value = true
  try {
    const id = Number(orderId.value)
    if (!id) {
      ElMessage.warning('工单 ID 无效')
      router.replace('/sales')
      return
    }
    try {
      order.value = await aftersalesApi.get(id)
    } catch (e) {
      ElMessage.error(e.message || '加载售后工单失败')
      router.replace('/sales')
    }
  } finally {
    pageLoading.value = false
  }
})

const goBack = () => router.back()
const handleResolve = () => {
  ElMessage.success('售后工单已标记为已解决')
  router.push('/sales')
}
const addRecord = () => { ElMessage.success('添加处理记录功能已触发') }
</script>

<style lang="scss" scoped>
.aftersales-detail {
  .card-header {
    display: flex;
    justify-content: flex-end;
    align-items: center;
    .header-actions { display: flex; gap: 12px; }
  }
  .order-no { color: #E94560; }
}
</style>