<template>
  <div class="dashboard-page" v-loading="loading" element-loading-text="加载中...">
    <!-- 统计卡片 -->
    <div class="stats-grid">
      <el-card class="stat-card dashboard-stat-card" v-for="stat in statistics" :key="stat.label">
        <template #header>
          <span class="stat-card-title">{{ stat.label }}</span>
        </template>
        <div class="stat-card-body">
          <div class="stat-icon" :class="stat.iconClass">
            <el-icon><component :is="stat.icon" /></el-icon>
          </div>
          <div class="stat-value">{{ stat.value }}</div>
        </div>
      </el-card>
    </div>

    <!-- 图表区域 -->
    <div class="charts-grid">
      <el-card class="chart-card main-chart">
        <template #header>
          <div class="chart-header">
            <span class="chart-card-title">销售趋势</span>
            <div class="chart-tabs">
              <el-radio-group v-model="chartPeriod" size="small">
                <el-radio-button value="7">近7天</el-radio-button>
                <el-radio-button value="30">近30天</el-radio-button>
                <el-radio-button value="90">近90天</el-radio-button>
              </el-radio-group>
            </div>
          </div>
        </template>
        <div class="chart-container" ref="salesChartRef"></div>
      </el-card>

      <el-card class="chart-card category-chart">
        <template #header>
          <span class="chart-card-title">分类销售 Top5</span>
        </template>
        <div v-if="categoryData.length" class="category-list">
          <div class="category-item" v-for="(cat, index) in categoryData" :key="cat.name + '-' + index">
            <div
              class="category-rank"
              :class="{ 'top-1': index === 0, 'top-2': index === 1, 'top-3': index === 2 }"
            >{{ index + 1 }}</div>
            <div class="category-info">
              <h4>{{ cat.name }}</h4>
              <div class="category-bar">
                <div class="bar-fill" :style="{ width: cat.percent + '%', background: cat.barColor }"></div>
              </div>
            </div>
            <div class="category-value">
              <h5>{{ cat.amount }}</h5>
              <p>{{ cat.percent }}%</p>
            </div>
          </div>
        </div>
        <el-empty v-else class="category-chart-empty" description="暂无数据" :image-size="72" />
      </el-card>
    </div>

    <!-- Top列表区域 -->
    <div class="top-lists-grid">
      <!-- 商品销售Top5 -->
      <el-card class="top-list-card">
        <template #header>
          <span class="top-list-card-title">商品销售 Top5</span>
        </template>
        <div v-if="productSalesTop5.length" class="top-list">
          <div class="top-item" v-for="(item, index) in productSalesTop5" :key="item.productId ?? 'p-' + index">
            <div class="top-rank" :class="{ 'top-1': index === 0, 'top-2': index === 1, 'top-3': index === 2 }">{{ index + 1 }}</div>
            <div class="top-info">
              <h4>{{ item.productName }}</h4>
              <p>{{ item.orderCount }}笔订单</p>
            </div>
            <div class="top-value">{{ formatCurrency(item.totalAmount) }}</div>
          </div>
        </div>
        <el-empty v-else class="top-list-empty" description="暂无数据" :image-size="72" />
      </el-card>

      <!-- 客户销售Top5 -->
      <el-card class="top-list-card">
        <template #header>
          <span class="top-list-card-title">客户销售 Top5</span>
        </template>
        <div v-if="customerSalesTop5.length" class="top-list">
          <div class="top-item" v-for="(item, index) in customerSalesTop5" :key="item.customerId ?? 'c-' + index">
            <div class="top-rank" :class="{ 'top-1': index === 0, 'top-2': index === 1, 'top-3': index === 2 }">{{ index + 1 }}</div>
            <div class="top-info">
              <h4>{{ item.customerName }}</h4>
              <p>{{ item.orderCount }}笔订单</p>
            </div>
            <div class="top-value">{{ formatCurrency(item.totalAmount) }}</div>
          </div>
        </div>
        <el-empty v-else class="top-list-empty" description="暂无数据" :image-size="72" />
      </el-card>

      <!-- 库存预警Top10 -->
      <el-card class="top-list-card warning-list">
        <template #header>
          <div class="card-header">
            <span class="top-list-card-title">库存预警 Top10</span>
            <el-tag type="warning" effect="light" size="small">{{ inventoryWarningTop10.length }}项</el-tag>
          </div>
        </template>
        <div v-if="inventoryWarningTop10.length" class="top-list compact">
          <div class="top-item warning" v-for="item in inventoryWarningTop10" :key="item.id">
            <div class="top-info">
              <h4>{{ item.productName }}</h4>
              <p>{{ item.warehouseName }}</p>
            </div>
            <div class="top-stock">
              <span class="stock-current">{{ item.stock }}</span>
              <span class="stock-safe">/ {{ item.safeStock }}</span>
            </div>
            <div class="top-status">
              <el-tag :type="getStockStatusType(item)" effect="light" size="small">{{ getStockStatusText(item) }}</el-tag>
            </div>
          </div>
        </div>
        <el-empty v-else class="top-list-empty" description="暂无数据" :image-size="72" />
      </el-card>

      <!-- 库存呆滞Top10 -->
      <el-card class="top-list-card stagnant-list">
        <template #header>
          <div class="card-header">
            <span class="top-list-card-title">库存呆滞 Top10</span>
            <el-tag type="danger" effect="light" size="small">{{ inventoryStagnantTop10.length }}项</el-tag>
          </div>
        </template>
        <div v-if="inventoryStagnantTop10.length" class="top-list compact">
          <div class="top-item stagnant" v-for="item in inventoryStagnantTop10" :key="item.id">
            <div class="top-info">
              <h4>{{ item.productName }}</h4>
              <p>{{ item.warehouseName }}</p>
            </div>
            <div class="top-days">
              <el-tag type="danger" effect="light" size="small">呆滞{{ item.stagnantDays }}天</el-tag>
            </div>
          </div>
        </div>
        <el-empty v-else class="top-list-empty" description="暂无数据" :image-size="72" />
      </el-card>
    </div>

    <!-- 近期订单 -->
    <el-card class="orders-card">
      <template #header>
        <div class="card-header">
          <span class="orders-card-title">近期订单</span>
          <el-button type="primary" link @click="router.push('/sales')">查看全部</el-button>
        </div>
      </template>
      <el-table :data="recentOrders" empty-text="暂无数据" class="recent-orders-table" style="width: 100%">
        <el-table-column label="订单编号" prop="orderNo" width="165">
          <template #default="{ row }">
            <span class="order-no">{{ row.orderNo }}</span>
          </template>
        </el-table-column>
        <!-- 不设 width，仅 min-width，剩余宽度由「商品信息」列吸收 -->
        <el-table-column label="商品信息" min-width="160" show-overflow-tooltip>
          <template #default="{ row }">
            <div class="product-cell">
              <ProductImageThumb
                v-if="getProductImage(row)"
                :src="getProductImage(row)"
                :preview-src-list="parseProductImageUrls(row?.productImage || row?.image)"
                class="product-thumb"
                :width="36"
                :height="36"
                :radius="8"
              />
              <div v-else class="product-icon">{{ row.productIcon }}</div>
              <div class="product-info">
                <h4 class="product-info-title">{{ row.product }}</h4>
                <p class="product-info-spec">{{ row.spec }}</p>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="客户" prop="customer" width="184" show-overflow-tooltip />
        <el-table-column label="金额" width="120" align="right">
          <template #default="{ row }">
            <span class="amount">¥{{ formatAmountDisplay(row.amount ?? 0) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="102" align="center">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)" effect="light">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column
          label="下单时间"
          prop="createTime"
          width="176"
          class-name="col-order-time"
          show-overflow-tooltip
        />
        <el-table-column label="操作" width="92" fixed="right" align="center">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="viewOrder(row)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import * as echarts from 'echarts'
import { dashboardApi } from '@/api'
import ProductImageThumb from '@/components/ProductImageThumb.vue'
import { firstProductImageUrl, parseProductImageUrls } from '@/utils/productImages'
import { formatAmountDisplay } from '@/utils/moneyFormat'

const router = useRouter()

const salesChartRef = ref()
const chartPeriod = ref('7')
const loading = ref(false)
/** 后端聚合的仪表盘数据 */
const overview = ref(null)

/** 金额：千分位；有小数则两位，整数不带小数 */
const formatCurrency = (amount) => `¥${formatAmountDisplay(amount ?? 0)}`

// 统计卡片：与 /sales/stats、商品/客户总数一致
const statistics = computed(() => {
  const s = overview.value?.summary
  if (!s) {
    return [
      { icon: 'Money', iconClass: 'stat-icon--purple', value: '—', label: '本月销售额' },
      { icon: 'ShoppingCart', iconClass: 'stat-icon--green', value: '—', label: '本月销售订单数' },
      { icon: 'Box', iconClass: 'stat-icon--blue', value: '—', label: '商品数' },
      { icon: 'User', iconClass: 'stat-icon--orange', value: '—', label: '客户数量' }
    ]
  }
  return [
    { icon: 'Money', iconClass: 'stat-icon--purple', value: formatCurrency(s.monthAmount), label: '本月销售额' },
    { icon: 'ShoppingCart', iconClass: 'stat-icon--green', value: s.monthOrderCount ?? 0, label: '本月销售订单数' },
    { icon: 'Box', iconClass: 'stat-icon--blue', value: s.productCount ?? 0, label: '商品数' },
    { icon: 'User', iconClass: 'stat-icon--orange', value: s.customerCount ?? 0, label: '客户数量' }
  ]
})

// 分类销售 Top5：按订单金额汇总到商品分类（与所选天数区间一致）
const categoryData = computed(() => {
  const list = overview.value?.categorySalesTop5 || []
  const colors = ['#8B5CF6', '#00d26a', '#3b82f6', '#f0a500', '#e94560']
  return list.map((cat, i) => ({
    name: cat.categoryName,
    value: Number(cat.amount) || 0,
    amount: formatCurrency(cat.amount),
    percent: cat.percent ?? 0,
    barColor: colors[i % colors.length]
  }))
})

// 近期订单 - 后端按时间倒序最近 10 条
const recentOrders = computed(() => {
  const orders = overview.value?.recentOrders || []
  return orders.map(order => ({
    ...order,
    productIcon: '📱',
    product: getProductName(order.productId, order.productName || order.product),
    spec: order.sku || '',
    customer: order.customerName || order.customer,
    status: getStatusText(order.status)
  }))
})

const productSalesTop5 = computed(() => overview.value?.productSalesTop5 || [])

const customerSalesTop5 = computed(() => overview.value?.customerSalesTop5 || [])

const inventoryWarningTop10 = computed(() => overview.value?.inventoryWarningTop10 || [])

const inventoryStagnantTop10 = computed(() => overview.value?.inventoryStagnantTop10 || [])

// 状态转中文
const getStatusText = (status) => {
  const statusMap = {
    'completed': '已完成',
    'pending': '处理中',
    'shipped': '已发货',
    'unpaid': '待付款',
    'cancelled': '已取消'
  }
  return statusMap[status] || status || '处理中'
}

// 获取状态类型
const getStatusType = (status) => {
  const typeMap = {
    '已完成': 'success',
    'completed': 'success',
    '处理中': 'warning',
    'pending': 'info',
    'shipped': 'warning',
    '待发货': 'warning',
    '已取消': 'danger',
    'cancelled': 'danger',
    '待付款': 'info',
    'unpaid': 'info'
  }
  return typeMap[status] || 'info'
}

// 获取库存状态类型
const getStockStatusType = (item) => {
  const safe = item.safeStock ?? 0
  if (item.stock <= 0) return 'danger'
  if (safe > 0 && item.stock < safe / 2) return 'danger'
  if (item.stock < safe) return 'warning'
  return 'success'
}

// 获取库存状态文本
const getStockStatusText = (item) => {
  const safe = item.safeStock ?? 0
  if (item.stock <= 0) return '缺货'
  if (safe > 0 && item.stock < safe / 2) return '紧急'
  if (item.stock < safe) return '偏低'
  return '正常'
}

// 获取商品图片（后端近期订单会带 productImage；与商品表 image 字段格式一致）
const getProductImage = (row) => firstProductImageUrl(row?.productImage || row?.image)

// 商品名称以仪表盘接口返回为准
const getProductName = (productId, fallbackName) => {
  return fallbackName || '-'
}

const formatTrendDayLabel = (isoDate) => {
  if (!isoDate) return ''
  const parts = String(isoDate).split('-')
  if (parts.length < 3) return isoDate
  const m = parseInt(parts[1], 10)
  const d = parseInt(parts[2], 10)
  return `${m}月${d}日`
}

let salesChartInstance = null
const onSalesChartResize = () => {
  salesChartInstance?.resize()
}

// 销售趋势：与分类/排名同一区间，数据来自后端按日汇总
const initSalesChart = () => {
  if (!salesChartRef.value) return

  const trend = overview.value?.salesTrend || []
  const days = trend.length || (chartPeriod.value === '7' ? 7 : chartPeriod.value === '30' ? 30 : 90)
  const labels = trend.length
    ? trend.map((p) => formatTrendDayLabel(p.date))
    : []
  const seriesData = trend.map((p) => Number(p.amount) || 0)

  if (salesChartInstance) {
    salesChartInstance.dispose()
    salesChartInstance = null
  }
  salesChartInstance = echarts.init(salesChartRef.value)

  salesChartInstance.setOption({
    tooltip: {
      trigger: 'axis',
      formatter: (params) => {
        const p = params[0]
        const amt = formatAmountDisplay(Number(p.value))
        return `${p.axisValue}<br/>销售额: ¥${amt}`
      }
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '3%',
      top: '10%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: labels,
      axisLine: { lineStyle: { color: '#DCDFE6' } },
      axisLabel: { color: '#909399', interval: days === 90 ? 9 : days === 30 ? 4 : 0 }
    },
    yAxis: {
      type: 'value',
      axisLine: { show: false },
      axisLabel: {
        color: '#909399',
        formatter: (v) => `¥${formatAmountDisplay(Number(v))}`
      },
      splitLine: { lineStyle: { color: '#E4E7ED' } }
    },
    series: [{
      name: '销售额',
      type: 'line',
      smooth: true,
      symbol: 'circle',
      symbolSize: 8,
      data: seriesData,
      lineStyle: { color: '#E94560', width: 3 },
      itemStyle: { color: '#E94560' },
      areaStyle: {
        color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: 'rgba(233, 69, 96, 0.3)' },
          { offset: 1, color: 'rgba(233, 69, 96, 0.05)' }
        ])
      }
    }]
  })

  window.removeEventListener('resize', onSalesChartResize)
  window.addEventListener('resize', onSalesChartResize)
}

const loadOverview = async () => {
  loading.value = true
  try {
    const days = Number(chartPeriod.value)
    overview.value = await dashboardApi.overview(days)
  } catch (e) {
    console.error('加载仪表盘失败:', e)
    overview.value = null
  } finally {
    loading.value = false
  }
}

watch(chartPeriod, async () => {
  await loadOverview()
  initSalesChart()
})

// 查看订单 - 直接跳转到订单详情页
const viewOrder = (row) => {
  router.push(`/sales/order/${row.id}`)
}

// 统计、图表、列表数据均来自 /dashboard/overview（以真实接口数据为准）
const loadData = async () => {
  await loadOverview()
  initSalesChart()
}

onMounted(() => {
  loadData()
})
</script>

<style lang="scss" scoped>
.dashboard-page {
  min-height: 360px;
  .stats-grid {
    display: grid;
    grid-template-columns: repeat(4, 1fr);
    gap: 12px;
    margin-bottom: 24px;

    .stat-card.dashboard-stat-card {
      /* 避免与全局 .stat-card 根节点 padding 叠压导致内容区异常 */
      padding: 0;

      :deep(.el-card__header) {
        padding: 10px 16px;
        border-bottom: 1px solid #ebeef5;
      }

      .stat-card-title {
        font-size: 14px;
        font-weight: 500;
        color: #606266;
      }

      :deep(.el-card__body) {
        padding: 12px 16px;
      }

      .stat-card-body {
        display: flex;
        align-items: center;
        gap: 14px;
      }

      .stat-icon {
        width: 36px;
        height: 36px;
        border-radius: 8px;
        display: flex;
        align-items: center;
        justify-content: center;
        font-size: 18px;
        flex-shrink: 0;

        &--purple {
          background: rgba(139, 92, 246, 0.1);
          color: #8B5CF6;
        }

        &--green {
          background: rgba(103, 194, 58, 0.1);
          color: #67C23A;
        }

        &--blue {
          background: rgba(64, 158, 255, 0.1);
          color: #409EFF;
        }

        &--orange {
          background: rgba(230, 162, 60, 0.1);
          color: #E6A23C;
        }
      }

      .stat-value {
        font-size: 22px;
        font-weight: 700;
        color: #303133;
        line-height: 1.2;
        min-width: 0;
      }
    }
  }

  .charts-grid {
    display: grid;
    grid-template-columns: 2fr 1fr;
    gap: 20px;
    margin-bottom: 24px;

    .chart-card {
      :deep(.el-card__header) {
        padding: 16px 20px;
        border-bottom: 1px solid #E4E7ED;
      }

      .chart-header {
        display: flex;
        justify-content: space-between;
        align-items: center;
        gap: 12px;
        flex-wrap: wrap;
      }

      .chart-card-title {
        font-size: 15px;
        font-weight: 600;
        color: #303133;
      }

      .chart-container {
        height: 280px;
      }

      .category-chart-empty {
        padding: 48px 0;
      }

      .category-list {
        .category-item {
          display: flex;
          align-items: center;
          gap: 16px;
          padding: 16px 0;
          border-bottom: 1px solid #E4E7ED;

          &:last-child {
            border-bottom: none;
          }

          .category-rank {
            width: 32px;
            height: 32px;
            border-radius: 10px;
            display: flex;
            align-items: center;
            justify-content: center;
            font-size: 15px;
            font-weight: 700;
            flex-shrink: 0;
            background: #F5F7FA;
            color: #909399;

            &.top-1 {
              background: #E94560;
              color: #fff;
            }

            &.top-2 {
              background: #E6A23C;
              color: #fff;
            }

            &.top-3 {
              background: #409EFF;
              color: #fff;
            }
          }

          .category-info {
            flex: 1;

            h4 {
              font-size: 14px;
              font-weight: 600;
              color: #303133;
              margin-bottom: 8px;
            }

            .category-bar {
              height: 6px;
              background: #E4E7ED;
              border-radius: 3px;
              overflow: hidden;

              .bar-fill {
                height: 100%;
                border-radius: 3px;
                transition: width 0.5s ease;
              }
            }
          }

          .category-value {
            text-align: right;

            h5 {
              font-size: 16px;
              font-weight: 700;
              color: #303133;
            }

            p {
              font-size: 12px;
              color: #909399;
            }
          }
        }
      }
    }
  }

  .orders-card {
    :deep(.el-card__body) {
      width: 100%;
    }

    .recent-orders-table {
      width: 100%;
      /* 让表体与表头表格拉满容器，剩余宽度由仅设 min-width 的「商品信息」列吸收 */
      :deep(.el-table__body table),
      :deep(.el-table__header table) {
        width: 100%;
      }
    }

    :deep(.el-card__header) {
      padding: 16px 20px;
      border-bottom: 1px solid #E4E7ED;
    }

    .card-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      gap: 12px;
      flex-wrap: wrap;
    }

    .orders-card-title {
      font-size: 15px;
      font-weight: 600;
      color: #303133;
    }

    .order-no {
      color: #E94560;
    }

    :deep(.col-order-time .cell) {
      white-space: nowrap;
    }

    .product-cell {
      display: flex;
      align-items: center;
      gap: 10px;
      min-width: 0;

      .product-thumb {
        flex-shrink: 0;
        border-radius: 8px;
        overflow: hidden;
      }

      .product-icon {
        width: 36px;
        height: 36px;
        flex-shrink: 0;
        border-radius: 8px;
        background: #F5F7FA;
        display: flex;
        align-items: center;
        justify-content: center;
        font-size: 18px;
      }

      .product-info {
        min-width: 0;
        flex: 1;

        .product-info-title,
        .product-info-spec {
          overflow: hidden;
          text-overflow: ellipsis;
          white-space: nowrap;
        }

        .product-info-title {
          font-size: 14px;
          font-weight: 600;
          color: #303133;
          margin: 0 0 2px;
          line-height: 1.3;
        }

        .product-info-spec {
          font-size: 12px;
          color: #909399;
          margin: 0;
          line-height: 1.3;
        }
      }
    }

    .amount {
      font-weight: 600;
      color: #E94560;
    }
  }

  .top-lists-grid {
    display: grid;
    grid-template-columns: repeat(4, 1fr);
    gap: 20px;
    margin-bottom: 24px;

    .top-list-card {
      :deep(.el-card__header) {
        padding: 16px 20px;
        border-bottom: 1px solid #E4E7ED;
      }

      .card-header {
        display: flex;
        justify-content: space-between;
        align-items: center;
        gap: 12px;
        flex-wrap: wrap;
      }

      .top-list-card-title {
        font-size: 15px;
        font-weight: 600;
        color: #303133;
      }

      .top-list-empty {
        padding: 20px 0;
        :deep(.el-empty__description) {
          margin-top: 8px;
        }
      }

      .top-list {
        .top-item {
          display: flex;
          align-items: center;
          gap: 12px;
          padding: 12px 0;
          border-bottom: 1px solid #E4E7ED;

          &:last-child {
            border-bottom: none;
          }

          .top-rank {
            width: 28px;
            height: 28px;
            border-radius: 8px;
            display: flex;
            align-items: center;
            justify-content: center;
            font-size: 14px;
            font-weight: 700;
            background: #F5F7FA;
            color: #909399;

            &.top-1 {
              background: #E94560;
              color: #fff;
            }

            &.top-2 {
              background: #E6A23C;
              color: #fff;
            }

            &.top-3 {
              background: #409EFF;
              color: #fff;
            }
          }

          .top-info {
            flex: 1;

            h4 {
              font-size: 14px;
              font-weight: 600;
              color: #303133;
              margin-bottom: 2px;
            }

            p {
              font-size: 12px;
              color: #909399;
            }
          }

          .top-value {
            font-size: 16px;
            font-weight: 700;
            color: #E94560;
          }
        }

        &.compact .top-item {
          padding: 10px 0;

          .top-stock {
            .stock-current {
              font-weight: 700;
              color: #E6A23C;
            }

            .stock-safe {
              color: #909399;
              font-size: 12px;
            }
          }

          .top-status {
            display: flex;
            align-items: center;
          }

          .top-days {
            display: flex;
            align-items: center;
          }
        }
      }

      &.warning-list .top-item.warning {
        background: rgba(230, 162, 60, 0.05);
        margin: 4px 0;
        padding: 10px;
        border-radius: 8px;
      }

      &.stagnant-list .top-item.stagnant {
        background: rgba(245, 108, 108, 0.05);
        margin: 4px 0;
        padding: 10px;
        border-radius: 8px;
      }
    }
  }
}

@media (max-width: 1400px) {
  .dashboard-page {
    .stats-grid {
      grid-template-columns: repeat(2, 1fr);
    }
    .charts-grid {
      grid-template-columns: 1fr;
    }
    .top-lists-grid {
      grid-template-columns: repeat(2, 1fr);
    }
  }
}
</style>