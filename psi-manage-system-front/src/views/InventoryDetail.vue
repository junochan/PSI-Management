<template>
  <div class="inventory-detail-page">
    <el-card>
      <template #header>
        <div class="card-header">
          <h3>库存详情</h3>
          <el-button @click="goBack">返回</el-button>
        </div>
      </template>

      <!-- 商品图片展示 -->
      <div class="stock-product-image" v-if="getProductImage(stock?.productId)">
        <img :src="getProductImage(stock?.productId)" class="product-image-preview" />
        <div class="product-image-info">
          <h4>{{ getProductName(stock?.productId, stock?.productName || stock?.name) }}</h4>
          <p>SKU: {{ stock?.sku }}</p>
        </div>
      </div>

      <!-- 基本信息 -->
      <el-descriptions :column="2" border class="info-section">
        <el-descriptions-item label="SKU编码"><span class="sku">{{ stock?.sku }}</span></el-descriptions-item>
        <el-descriptions-item label="商品名称">{{ getProductName(stock?.productId, stock?.productName || stock?.name) }}</el-descriptions-item>
        <el-descriptions-item label="规格">{{ stock?.spec || '-' }}</el-descriptions-item>
        <el-descriptions-item label="分类">{{ getCategoryName(stock?.productId) || stock?.category || '-' }}</el-descriptions-item>
        <el-descriptions-item label="所属仓库"><span class="warehouse-name">{{ getWarehouseName(stock?.warehouseId) || stock?.warehouseName }}</span></el-descriptions-item>
        <el-descriptions-item label="库位">
          <el-input v-model="stockLocationEdit" size="small" style="width: 120px" placeholder="输入库位" />
          <el-button type="primary" size="small" @click="updateLocation" style="margin-left: 8px">更新</el-button>
        </el-descriptions-item>
        <el-descriptions-item label="当前库存">
          <span :class="{ 'low-stock': stock?.stock < stock?.safeStock }">{{ stock?.stock }} 件</span>
        </el-descriptions-item>
        <el-descriptions-item label="库存预警值">
          <el-input-number v-model="stockSafeStockEdit" :min="0" size="small" style="width: 100px" />
          <el-button type="primary" size="small" @click="updateSafeStock" style="margin-left: 8px">更新</el-button>
        </el-descriptions-item>
        <el-descriptions-item label="呆滞预警天数">
          <el-input-number v-model="stockStagnantDaysEdit" :min="1" size="small" style="width: 100px" />
          <el-button type="primary" size="small" @click="updateStagnantDays" style="margin-left: 8px">更新</el-button>
        </el-descriptions-item>
        <el-descriptions-item label="呆滞状态">
          <el-tag :type="getStagnantStatusType(stock)" effect="light">{{ getStagnantStatusText(stock) }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="成本价">¥{{ stock?.costPrice }}</el-descriptions-item>
        <el-descriptions-item label="库存价值"><span class="amount">¥{{ stock?.stockValue }}</span></el-descriptions-item>
        <el-descriptions-item label="库存状态">
          <el-tag :type="getStockStatusType(stock?.status)" effect="light">{{ formatStockStatus(stock?.status) }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="最后入库">{{ formatTime(stock?.lastInboundTime) }}</el-descriptions-item>
        <el-descriptions-item label="最后出库">{{ formatTime(stock?.lastOutboundTime) }}</el-descriptions-item>
      </el-descriptions>

      <!-- 操作按钮 -->
      <div class="action-buttons">
        <el-button type="success" @click="openStockInbound"><el-icon><Plus /></el-icon>入库</el-button>
        <el-button type="warning" @click="openStockOutbound"><el-icon><Minus /></el-icon>出库</el-button>
        <el-button type="primary" @click="openPurchaseFromStock" v-if="stock?.stock < stock?.safeStock"><el-icon><ShoppingCart /></el-icon>创建采购</el-button>
      </div>

      <!-- 入库记录 -->
      <el-divider content-position="left">
        <span>入库记录（最近10条）</span>
        <el-button type="primary" link size="small" @click="goToAllInboundRecords" style="margin-left: 12px">查看全部</el-button>
      </el-divider>
      <el-table :data="stockInboundRecords" empty-text="暂无数据" style="width: 100%" max-height="300" size="small">
        <el-table-column label="入库单号" min-width="120">
          <template #default="{ row }"><span class="order-no success">{{ row.orderNo }}</span></template>
        </el-table-column>
        <el-table-column label="采购单号" min-width="120">
          <template #default="{ row }"><span class="order-no">{{ row.purchaseOrderNo || row.purchaseNo || '-' }}</span></template>
        </el-table-column>
        <el-table-column label="数量" width="80" align="center">
          <template #default="{ row }">{{ row.quantity }}</template>
        </el-table-column>
        <el-table-column label="入库时间" min-width="140">
          <template #default="{ row }">{{ formatTime(row.createTime || row.time) }}</template>
        </el-table-column>
        <el-table-column label="操作人" min-width="100">
          <template #default="{ row }">{{ row.operatorName || row.operator || '-' }}</template>
        </el-table-column>
        <el-table-column label="备注" min-width="120" show-overflow-tooltip>
          <template #default="{ row }">{{ row.remark || '-' }}</template>
        </el-table-column>
      </el-table>

      <!-- 出库记录 -->
      <el-divider content-position="left">
        <span>出库记录（最近10条）</span>
        <el-button type="primary" link size="small" @click="goToAllOutboundRecords" style="margin-left: 12px">查看全部</el-button>
      </el-divider>
      <el-table :data="stockOutboundRecords" empty-text="暂无数据" style="width: 100%" max-height="300" size="small">
        <el-table-column label="出库单号" min-width="120">
          <template #default="{ row }"><span class="order-no warning">{{ row.orderNo }}</span></template>
        </el-table-column>
        <el-table-column label="销售单号" min-width="120">
          <template #default="{ row }"><span class="order-no">{{ row.salesOrderNo || row.salesNo || '-' }}</span></template>
        </el-table-column>
        <el-table-column label="数量" width="80" align="center">
          <template #default="{ row }">{{ row.quantity }}</template>
        </el-table-column>
        <el-table-column label="出库时间" min-width="140">
          <template #default="{ row }">{{ formatTime(row.createTime || row.time) }}</template>
        </el-table-column>
        <el-table-column label="操作人" min-width="100">
          <template #default="{ row }">{{ row.operatorName || row.operator || '-' }}</template>
        </el-table-column>
        <el-table-column label="备注" min-width="120" show-overflow-tooltip>
          <template #default="{ row }">{{ row.remark || '-' }}</template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 入库对话框 -->
    <el-dialog v-model="stockInboundVisible" title="入库操作" width="500px" destroy-on-close>
      <el-radio-group v-model="stockInboundType" style="margin-bottom: 20px">
        <el-radio value="purchase">采购入库（关联采购单）</el-radio>
        <el-radio value="other">其他入库（直接入库）</el-radio>
      </el-radio-group>

      <!-- 采购入库表单 -->
      <el-form v-if="stockInboundType === 'purchase'" ref="stockInboundFormRef" :model="stockInboundForm" :rules="stockInboundRules" label-width="100px">
        <el-alert v-if="stockPendingPurchaseOrders.length === 0" type="warning" title="当前没有该商品待入库的采购单" description="请先在采购管理中创建采购单。" :closable="false" show-icon style="margin-bottom: 16px" />
        <el-form-item label="商品">
          <el-input :value="stockInboundForm.productName" disabled />
        </el-form-item>
        <el-form-item label="仓库">
          <el-input :value="stockInboundForm.warehouseName" disabled />
        </el-form-item>
        <el-form-item label="当前库存">
          <el-input :value="`${stockInboundForm.currentStock} 件`" disabled />
        </el-form-item>
        <el-form-item label="采购单" prop="purchaseOrderId">
          <el-select v-model="stockInboundForm.purchaseOrderId" placeholder="请选择采购单" style="width: 100%" @change="onStockPurchaseOrderChange" :disabled="stockPendingPurchaseOrders.length === 0">
            <el-option v-for="po in stockPendingPurchaseOrders" :key="po.id" :label="`#${po.orderNo} - ${po.supplierName || ''} - 待入库${po.pendingQuantity || (po.totalQuantity - (po.inboundQuantity || 0))}件`" :value="po.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="入库数量" prop="quantity">
          <el-input-number v-model="stockInboundForm.quantity" :min="1" :max="stockInboundForm.maxQuantity || 9999" style="width: 100%" />
          <div class="quantity-tip" v-if="stockInboundForm.maxQuantity">最大可入库: {{ stockInboundForm.maxQuantity }} 件</div>
        </el-form-item>
        <el-form-item label="入库备注">
          <el-input v-model="stockInboundForm.remark" placeholder="输入入库备注" />
        </el-form-item>
      </el-form>

      <!-- 其他入库表单 -->
      <el-form v-if="stockInboundType === 'other'" ref="stockOtherInboundFormRef" :model="stockOtherInboundForm" :rules="stockOtherInboundRules" label-width="100px">
        <el-form-item label="商品">
          <el-input :value="stockOtherInboundForm.productName" disabled />
        </el-form-item>
        <el-form-item label="仓库">
          <el-input :value="stockOtherInboundForm.warehouseName" disabled />
        </el-form-item>
        <el-form-item label="当前库存">
          <el-input :value="`${stockOtherInboundForm.currentStock} 件`" disabled />
        </el-form-item>
        <el-form-item label="入库数量" prop="quantity">
          <el-input-number v-model="stockOtherInboundForm.quantity" :min="1" style="width: 100%" />
        </el-form-item>
        <el-form-item label="入库备注">
          <el-input v-model="stockOtherInboundForm.remark" placeholder="输入入库备注" />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="stockInboundVisible = false">取消</el-button>
        <el-button type="primary" @click="submitStockInbound" :loading="loading" :disabled="stockInboundType === 'purchase' && stockPendingPurchaseOrders.length === 0">确认入库</el-button>
      </template>
    </el-dialog>

    <!-- 出库对话框 -->
    <el-dialog v-model="stockOutboundVisible" title="出库操作" width="500px" destroy-on-close>
      <el-radio-group v-model="stockOutboundType" style="margin-bottom: 20px">
        <el-radio value="sales">销售出库（关联销售单）</el-radio>
        <el-radio value="other">其他出库（直接出库）</el-radio>
      </el-radio-group>

      <!-- 销售出库表单 -->
      <el-form v-if="stockOutboundType === 'sales'" ref="stockOutboundFormRef" :model="stockOutboundForm" :rules="stockOutboundRules" label-width="100px">
        <el-alert v-if="stockPendingSalesOrders.length === 0" type="warning" title="当前没有该商品待发货的销售订单" description="请先在销售管理中创建销售订单。" :closable="false" show-icon style="margin-bottom: 16px" />
        <el-form-item label="商品">
          <el-input :value="stockOutboundForm.productName" disabled />
        </el-form-item>
        <el-form-item label="仓库">
          <el-input :value="stockOutboundForm.warehouseName" disabled />
        </el-form-item>
        <el-form-item label="当前库存">
          <el-input :value="`${stockOutboundForm.currentStock} 件`" disabled />
        </el-form-item>
        <el-form-item label="销售单" prop="salesOrderId">
          <el-select v-model="stockOutboundForm.salesOrderId" placeholder="请选择销售单" style="width: 100%" @change="onStockSalesOrderChange" :disabled="stockPendingSalesOrders.length === 0">
            <el-option v-for="so in stockPendingSalesOrders" :key="so.id" :label="`#${so.orderNo} - ${so.customerName || ''} - 待发货${so.pendingQuantity || so.quantity}件`" :value="so.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="出库数量" prop="quantity">
          <el-input-number v-model="stockOutboundForm.quantity" :min="1" :max="stockOutboundForm.maxQuantity || 9999" style="width: 100%" />
          <div class="quantity-tip" v-if="stockOutboundForm.maxQuantity">最大可出库: {{ stockOutboundForm.maxQuantity }} 件</div>
        </el-form-item>
        <el-form-item label="出库备注">
          <el-input v-model="stockOutboundForm.remark" placeholder="输入出库备注" />
        </el-form-item>
      </el-form>

      <!-- 其他出库表单 -->
      <el-form v-if="stockOutboundType === 'other'" ref="stockOtherOutboundFormRef" :model="stockOtherOutboundForm" :rules="stockOtherOutboundRules" label-width="100px">
        <el-form-item label="商品">
          <el-input :value="stockOtherOutboundForm.productName" disabled />
        </el-form-item>
        <el-form-item label="仓库">
          <el-input :value="stockOtherOutboundForm.warehouseName" disabled />
        </el-form-item>
        <el-form-item label="当前库存">
          <el-input :value="`${stockOtherOutboundForm.currentStock} 件`" disabled />
        </el-form-item>
        <el-form-item label="出库数量" prop="quantity">
          <el-input-number v-model="stockOtherOutboundForm.quantity" :min="1" :max="stockOtherOutboundForm.maxQuantity || 9999" style="width: 100%" />
          <div class="quantity-tip" v-if="stockOtherOutboundForm.maxQuantity">最大可出库: {{ stockOtherOutboundForm.maxQuantity }} 件</div>
        </el-form-item>
        <el-form-item label="出库备注" prop="remark">
          <el-input v-model="stockOtherOutboundForm.remark" placeholder="输入出库原因（必填）" />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="stockOutboundVisible = false">取消</el-button>
        <el-button type="primary" @click="submitStockOutbound" :loading="loading" :disabled="stockOutboundType === 'sales' && stockPendingSalesOrders.length === 0">确认出库</el-button>
      </template>
    </el-dialog>

    <!-- 创建采购单对话框 -->
    <el-dialog v-model="purchaseDialogVisible" title="创建采购单" width="700px" destroy-on-close>
      <el-form ref="purchaseFormRef" :model="purchaseForm" :rules="purchaseRules" label-width="100px">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="商品">
              <el-input :value="purchaseForm.productName" disabled />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="SKU">
              <el-input :value="purchaseForm.sku" disabled />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="当前库存">
              <el-input :value="`${purchaseForm.currentStock} 件`" disabled />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="安全库存">
              <el-input :value="`${purchaseForm.safeStock} 件`" disabled />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="供应商" prop="supplierId">
              <el-select v-model="purchaseForm.supplierId" placeholder="请选择供应商" style="width: 100%" filterable>
                <el-option v-for="s in suppliersList" :key="s.id" :label="s.name" :value="s.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="采购日期">
              <el-date-picker v-model="purchaseForm.date" type="date" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="采购数量" prop="quantity">
              <el-input-number v-model="purchaseForm.quantity" :min="1" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="单价（元）">
              <el-input-number v-model="purchaseForm.unitPrice" :min="0" :precision="2" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="采购金额">
              <el-input :value="`¥${(purchaseForm.quantity * purchaseForm.unitPrice || 0).toLocaleString()}`" disabled />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="期望交货">
              <el-date-picker v-model="purchaseForm.deliveryDate" type="date" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="备注">
          <el-input v-model="purchaseForm.remark" placeholder="输入采购备注" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="purchaseDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitPurchase">确认创建</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useDataStore } from '@/stores/data'
import { useUserStore } from '@/stores/user'
import { inventoryApi, purchaseApi, salesApi } from '@/api'
import { formatTime } from '@/utils/time'
import { firstProductImageUrl } from '@/utils/productImages'

const router = useRouter()
const route = useRoute()
const dataStore = useDataStore()
const userStore = useUserStore()

const hasInventoryMenu = computed(() => userStore.hasPermission('inventory'))
/** 与列表页一致：流水数据仅在具备 inventory:records 时拉取 */
const canInventoryRecordsTab = computed(() => userStore.hasPermission('inventory:records'))
const canInventoryOutbound = computed(
  () => hasInventoryMenu.value || userStore.hasPermission('inventory:outbound')
)

const stockId = computed(() => route.params.id)
const loading = ref(false)

// 表单refs
const stockInboundFormRef = ref()
const stockOutboundFormRef = ref()
const stockOtherInboundFormRef = ref()
const stockOtherOutboundFormRef = ref()
const purchaseFormRef = ref()

// 编辑字段
const stockSafeStockEdit = ref(10)
const stockLocationEdit = ref('')
const stockStagnantDaysEdit = ref(90)

// 对话框状态
const stockInboundVisible = ref(false)
const stockOutboundVisible = ref(false)
const purchaseDialogVisible = ref(false)
const stockInboundType = ref('purchase')
const stockOutboundType = ref('sales')

// 库存数据
const stock = ref(null)

// 计算属性
const products = computed(() => dataStore.products || [])
const warehousesList = computed(() => dataStore.warehouses || [])
const categoriesList = computed(() => dataStore.categories || [])
const suppliersList = computed(() => dataStore.suppliers || [])
const purchaseOrders = computed(() => dataStore.purchaseOrders || [])
const salesOrders = computed(() => dataStore.salesOrders || [])
const inboundRecords = computed(() => dataStore.inboundRecords || [])
const outboundRecords = computed(() => dataStore.outboundRecords || [])

// 入库记录
const stockInboundRecords = computed(() => {
  if (!stock.value) return []
  return inboundRecords.value.filter(r =>
    r.productId === stock.value.productId &&
    r.warehouseId === stock.value.warehouseId
  ).slice(0, 10)
})

// 出库记录
const stockOutboundRecords = computed(() => {
  if (!stock.value) return []
  return outboundRecords.value.filter(r =>
    r.productId === stock.value.productId &&
    r.warehouseId === stock.value.warehouseId
  ).slice(0, 10)
})

// 待入库采购单
const stockPendingPurchaseOrders = computed(() => {
  if (!stockInboundForm.value.productId) return []
  return purchaseOrders.value.filter(po =>
    po.productId === stockInboundForm.value.productId &&
    po.inboundStatus !== 'completed' && po.inboundStatus !== 'cancelled'
  )
})

// 待发货销售单
const stockPendingSalesOrders = computed(() => {
  if (!stockOutboundForm.value.productId) return []
  return salesOrders.value.filter(so =>
    so.productId === stockOutboundForm.value.productId &&
    so.status === 'pending' &&
    so.payStatus === 'paid'
  )
})

// 入库表单
const stockInboundForm = ref({
  inventoryId: null,
  productId: null,
  productName: '',
  warehouseId: null,
  warehouseName: '',
  currentStock: 0,
  purchaseOrderId: null,
  quantity: 1,
  maxQuantity: 0,
  remark: ''
})

const stockOtherInboundForm = ref({
  inventoryId: null,
  productName: '',
  warehouseName: '',
  currentStock: 0,
  quantity: 1,
  remark: ''
})

// 出库表单
const stockOutboundForm = ref({
  inventoryId: null,
  productId: null,
  productName: '',
  warehouseId: null,
  warehouseName: '',
  currentStock: 0,
  salesOrderId: null,
  quantity: 1,
  maxQuantity: 0,
  remark: ''
})

const stockOtherOutboundForm = ref({
  inventoryId: null,
  productName: '',
  warehouseName: '',
  currentStock: 0,
  quantity: 1,
  maxQuantity: 0,
  remark: ''
})

// 采购表单
const purchaseForm = ref({
  productId: null,
  productName: '',
  sku: '',
  currentStock: 0,
  safeStock: 0,
  supplierId: null,
  date: new Date(),
  quantity: 1,
  unitPrice: 0,
  deliveryDate: null,
  remark: ''
})

// 表单验证规则
const stockInboundRules = {
  purchaseOrderId: [{ required: true, message: '请选择采购单', trigger: 'change' }],
  quantity: [{ required: true, message: '请输入入库数量', trigger: 'blur' }]
}

const stockOutboundRules = {
  salesOrderId: [{ required: true, message: '请选择销售单', trigger: 'change' }],
  quantity: [{ required: true, message: '请输入出库数量', trigger: 'blur' }]
}

const stockOtherInboundRules = {
  quantity: [{ required: true, message: '请输入入库数量', trigger: 'blur' }]
}

const stockOtherOutboundRules = {
  quantity: [{ required: true, message: '请输入出库数量', trigger: 'blur' }],
  remark: [{ required: true, message: '出库备注为必填项', trigger: 'blur' }]
}

const purchaseRules = {
  supplierId: [{ required: true, message: '请选择供应商', trigger: 'change' }],
  quantity: [{ required: true, message: '请输入采购数量', trigger: 'blur' }]
}

// 工具函数
const getProductImage = (productId) => {
  const product = products.value.find(p => p.id === productId)
  return firstProductImageUrl(product?.image)
}

// 获取商品名称（从商品列表动态获取最新名称）
const getProductName = (productId, fallbackName) => {
  const product = products.value.find(p => p.id === productId)
  return product?.name || fallbackName || '-'
}

const getWarehouseName = (warehouseId) => {
  const warehouse = warehousesList.value.find(w => w.id === warehouseId)
  return warehouse?.name || ''
}

const getCategoryName = (productId) => {
  const product = products.value.find(p => p.id === productId)
  if (product) {
    if (product.categoryId) {
      const category = categoriesList.value.find(c => c.id === product.categoryId)
      return category?.name || product.categoryName || product.category || ''
    }
    return product.categoryName || product.category || ''
  }
  return ''
}

const getStockStatusType = (status) => ({ 'normal': 'success', 'warning': 'warning', 'critical': 'danger', '正常': 'success', '偏低': 'warning', '紧急补货': 'danger' }[status] || 'info')

const formatStockStatus = (status) => {
  const statusMap = { 'normal': '正常', 'warning': '偏低', 'critical': '紧急补货', '正常': '正常', '偏低': '偏低', '紧急补货': '紧急补货' }
  return statusMap[status] || status
}

// 呆滞计算
const getStagnantDays = (stockData) => {
  const now = new Date()
  if (stockData?.lastOutboundTime) {
    const lastOutbound = new Date(stockData.lastOutboundTime)
    return Math.floor((now - lastOutbound) / (1000 * 60 * 60 * 24))
  }
  if (stockData?.lastInboundTime) {
    const lastInbound = new Date(stockData.lastInboundTime)
    return Math.floor((now - lastInbound) / (1000 * 60 * 60 * 24))
  }
  return 0
}

const getStagnantStatusType = (stockData) => {
  const stagnantDays = getStagnantDays(stockData)
  const warningDays = stockData?.stagnantDays || 90
  if (stagnantDays >= warningDays) return 'danger'
  return 'success'
}

const getStagnantStatusText = (stockData) => {
  const stagnantDays = getStagnantDays(stockData)
  const warningDays = stockData?.stagnantDays || 90
  if (stagnantDays >= warningDays) return `呆滞${stagnantDays}天`
  return `正常`
}

// 加载数据（与库存页一致：无出入库记录权限不拉流水接口）
const loadData = async () => {
  const tasks = [
    dataStore.loadInventory(),
    dataStore.loadProducts(),
    dataStore.loadWarehouses(),
    dataStore.loadCategories(),
    dataStore.loadSuppliers(),
    dataStore.loadPurchaseOrders()
  ]
  if (
    canInventoryRecordsTab.value ||
    canInventoryOutbound.value ||
    userStore.hasPermission('sales') ||
    userStore.hasPermission('sales:view')
  ) {
    tasks.push(dataStore.loadSalesOrders())
  }
  if (canInventoryRecordsTab.value) {
    tasks.push(dataStore.loadInboundRecords(), dataStore.loadOutboundRecords())
  }
  await Promise.all(tasks)

  // 查找库存数据
  const inventoryData = dataStore.inventoryData || []
  stock.value = inventoryData.find(i => i.id === Number(stockId.value))

  if (stock.value) {
    stockSafeStockEdit.value = stock.value.safeStock || 10
    stockLocationEdit.value = stock.value.location || ''
    stockStagnantDaysEdit.value = stock.value.stagnantDays || 90
  }
}

// 更新安全库存
const updateSafeStock = async () => {
  if (!stock.value || !stockSafeStockEdit.value) return
  loading.value = true
  try {
    await inventoryApi.updateSafeStock(stock.value.id, stockSafeStockEdit.value)
    ElMessage.success('库存预警值已更新')
    stock.value.safeStock = stockSafeStockEdit.value
    await dataStore.loadInventory()
  } catch (error) {
    ElMessage.error(error.message || '更新失败')
  } finally {
    loading.value = false
  }
}

// 更新库位
const updateLocation = async () => {
  if (!stock.value) return
  loading.value = true
  try {
    await inventoryApi.updateLocation(stock.value.id, stockLocationEdit.value)
    ElMessage.success('库位已更新')
    stock.value.location = stockLocationEdit.value
    await dataStore.loadInventory()
  } catch (error) {
    ElMessage.error(error.message || '更新失败')
  } finally {
    loading.value = false
  }
}

// 更新呆滞预警天数
const updateStagnantDays = async () => {
  if (!stock.value || !stockStagnantDaysEdit.value) return
  loading.value = true
  try {
    await inventoryApi.updateStagnantDays(stock.value.id, stockStagnantDaysEdit.value)
    ElMessage.success('呆滞预警天数已更新')
    stock.value.stagnantDays = stockStagnantDaysEdit.value
    await dataStore.loadInventory()
  } catch (error) {
    ElMessage.error(error.message || '更新失败')
  } finally {
    loading.value = false
  }
}

// 打开入库对话框
const openStockInbound = async () => {
  await dataStore.loadPurchaseOrders()
  stockInboundType.value = 'purchase'
  stockInboundForm.value = {
    inventoryId: stock.value.id,
    productId: stock.value.productId,
    productName: getProductName(stock.value.productId, stock.value.productName || stock.value.name),
    warehouseId: stock.value.warehouseId,
    warehouseName: stock.value.warehouseName,
    currentStock: stock.value.stock ?? 0,
    purchaseOrderId: null,
    quantity: 1,
    maxQuantity: 0,
    remark: ''
  }
  stockOtherInboundForm.value = {
    inventoryId: stock.value.id,
    productName: getProductName(stock.value.productId, stock.value.productName || stock.value.name),
    warehouseName: stock.value.warehouseName,
    currentStock: stock.value.stock ?? 0,
    quantity: 1,
    remark: ''
  }
  stockInboundVisible.value = true
}

// 打开出库对话框
const openStockOutbound = async () => {
  const currentStock = stock.value?.stock ?? 0
  if (currentStock <= 0) {
    ElMessage.warning('当前库存为0，无法出库')
    return
  }
  await dataStore.loadSalesOrders()
  stockOutboundType.value = 'sales'
  stockOutboundForm.value = {
    inventoryId: stock.value.id,
    productId: stock.value.productId,
    productName: getProductName(stock.value.productId, stock.value.productName || stock.value.name),
    warehouseId: stock.value.warehouseId,
    warehouseName: stock.value.warehouseName,
    currentStock: currentStock,
    salesOrderId: null,
    quantity: 1,
    maxQuantity: 0,
    remark: ''
  }
  stockOtherOutboundForm.value = {
    inventoryId: stock.value.id,
    productName: getProductName(stock.value.productId, stock.value.productName || stock.value.name),
    warehouseName: stock.value.warehouseName,
    currentStock: currentStock,
    quantity: 1,
    maxQuantity: currentStock,
    remark: ''
  }
  stockOutboundVisible.value = true
}

// 选择采购单时设置最大入库数量
const onStockPurchaseOrderChange = (purchaseOrderId) => {
  const po = purchaseOrders.value.find(p => p.id === purchaseOrderId)
  if (po) {
    const pendingQty = po.pendingQuantity || (po.totalQuantity || po.quantity || 0) - (po.inboundQuantity || 0)
    stockInboundForm.value.maxQuantity = pendingQty > 0 ? pendingQty : 1
    stockInboundForm.value.quantity = pendingQty > 0 ? pendingQty : 1
  }
}

// 选择销售单时设置最大出库数量
const onStockSalesOrderChange = (salesOrderId) => {
  const so = salesOrders.value.find(s => s.id === salesOrderId)
  if (so) {
    const pendingQty = so.pendingQuantity || so.quantity || 1
    const currentStock = stockOutboundForm.value.currentStock || 0
    const maxQty = Math.min(pendingQty, currentStock)
    stockOutboundForm.value.maxQuantity = maxQty > 0 ? maxQty : 1
    stockOutboundForm.value.quantity = maxQty > 0 ? Math.min(maxQty, 1) : 1
  }
}

// 提交入库
const submitStockInbound = async () => {
  if (stockInboundType.value === 'purchase') {
    if (!stockInboundFormRef.value) return
    await stockInboundFormRef.value.validate(async (valid) => {
      if (valid) {
        loading.value = true
        try {
          await purchaseApi.inbound(stockInboundForm.value.purchaseOrderId, {
            warehouseId: stockInboundForm.value.warehouseId,
            quantity: stockInboundForm.value.quantity,
            remark: stockInboundForm.value.remark
          })
          ElMessage.success('入库成功')
          stockInboundVisible.value = false
          await loadData()
        } catch (error) {
          ElMessage.error(error.message || '入库失败')
        } finally {
          loading.value = false
        }
      }
    })
  } else {
    if (!stockOtherInboundFormRef.value) return
    await stockOtherInboundFormRef.value.validate(async (valid) => {
      if (valid) {
        loading.value = true
        try {
          await inventoryApi.inbound(
            stockOtherInboundForm.value.inventoryId,
            stockOtherInboundForm.value.quantity,
            stockOtherInboundForm.value.remark
          )
          ElMessage.success('入库成功')
          stockInboundVisible.value = false
          await loadData()
        } catch (error) {
          ElMessage.error(error.message || '入库失败')
        } finally {
          loading.value = false
        }
      }
    })
  }
}

// 提交出库
const submitStockOutbound = async () => {
  if (stockOutboundType.value === 'sales') {
    if (!stockOutboundFormRef.value) return
    await stockOutboundFormRef.value.validate(async (valid) => {
      if (valid) {
        loading.value = true
        try {
          await salesApi.shipping(stockOutboundForm.value.salesOrderId, {
            warehouseId: stockOutboundForm.value.warehouseId,
            quantity: stockOutboundForm.value.quantity,
            remark: stockOutboundForm.value.remark
          })
          ElMessage.success('出库成功')
          stockOutboundVisible.value = false
          await loadData()
        } catch (error) {
          ElMessage.error(error.message || '出库失败')
        } finally {
          loading.value = false
        }
      }
    })
  } else {
    if (!stockOtherOutboundFormRef.value) return
    await stockOtherOutboundFormRef.value.validate(async (valid) => {
      if (valid) {
        loading.value = true
        try {
          await inventoryApi.outbound(
            stockOtherOutboundForm.value.inventoryId,
            stockOtherOutboundForm.value.quantity,
            stockOtherOutboundForm.value.remark
          )
          ElMessage.success('出库成功')
          stockOutboundVisible.value = false
          await loadData()
        } catch (error) {
          ElMessage.error(error.message || '出库失败')
        } finally {
          loading.value = false
        }
      }
    })
  }
}

// 打开采购对话框
const openPurchaseFromStock = async () => {
  await dataStore.loadSuppliers()
  const product = products.value.find(p => p.id === stock.value.productId)
  purchaseForm.value = {
    productId: stock.value.productId,
    productName: getProductName(stock.value.productId, stock.value.productName || product?.name || ''),
    sku: stock.value.sku || product?.code || '',
    currentStock: stock.value.stock || 0,
    safeStock: stock.value.safeStock || 10,
    supplierId: null,
    date: new Date(),
    quantity: Math.max(1, (stock.value.safeStock || 10) - (stock.value.stock || 0)),
    unitPrice: product?.costPrice || stock.value.costPrice || 0,
    deliveryDate: new Date(Date.now() + 7 * 24 * 60 * 60 * 1000),
    remark: `库存预警采购：当前库存${stock.value.stock}，安全库存${stock.value.safeStock}`
  }
  purchaseDialogVisible.value = true
}

// 提交采购单
const submitPurchase = async () => {
  if (!purchaseFormRef.value) return
  await purchaseFormRef.value.validate(async (valid) => {
    if (valid) {
      loading.value = true
      try {
        await purchaseApi.create({
          supplierId: purchaseForm.value.supplierId,
          productId: purchaseForm.value.productId,
          quantity: purchaseForm.value.quantity,
          unitPrice: purchaseForm.value.unitPrice,
          expectDate: purchaseForm.value.deliveryDate,
          remark: purchaseForm.value.remark
        })
        ElMessage.success('采购单创建成功')
        purchaseDialogVisible.value = false
        await loadData()
      } catch (error) {
        ElMessage.error(error.message || '创建失败')
      } finally {
        loading.value = false
      }
    }
  })
}

// 返回
const goBack = () => router.back()

// 跳转到全部入库记录
const goToAllInboundRecords = () => {
  router.push('/inventory?tab=records&subtab=inbound')
}

// 跳转到全部出库记录
const goToAllOutboundRecords = () => {
  router.push('/inventory?tab=records&subtab=outbound')
}

onMounted(() => {
  loadData()
})
</script>

<style lang="scss" scoped>
.inventory-detail-page {
  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    h3 {
      font-size: 18px;
      font-weight: 600;
      color: #303133;
    }
  }

  .stock-product-image {
    display: flex;
    align-items: center;
    gap: 16px;
    padding: 16px;
    background: #F5F7FA;
    border-radius: 8px;
    margin-bottom: 20px;

    .product-image-preview {
      width: 100px;
      height: 100px;
      object-fit: cover;
      border-radius: 8px;
    }

    .product-image-info {
      h4 {
        font-size: 16px;
        font-weight: 600;
        color: #303133;
        margin-bottom: 4px;
      }
      p {
        font-size: 13px;
        color: #909399;
      }
    }
  }

  .info-section {
    margin-bottom: 20px;
  }

  .sku {
    color: #E94560;
    font-family: monospace;
  }

  .warehouse-name {
    font-weight: 600;
    color: #409EFF;
  }

  .amount {
    font-weight: 600;
    color: #E94560;
  }

  .low-stock {
    color: #E6A23C;
    font-weight: 600;
  }

  .order-no {
    color: #E94560;
    font-family: monospace;
    &.success { color: #67C23A; }
    &.warning { color: #E6A23C; }
  }

  .action-buttons {
    display: flex;
    gap: 12px;
    margin-bottom: 20px;
  }

  .quantity-tip {
    font-size: 12px;
    color: #E6A23C;
    margin-top: 4px;
  }
}
</style>