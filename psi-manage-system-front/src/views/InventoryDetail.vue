<template>
  <div class="inventory-detail-page">
    <el-card v-loading="pageLoading" element-loading-text="加载中...">
      <template #header>
        <div class="card-header">
          <el-button @click="goBack">返回</el-button>
        </div>
      </template>

      <!-- 商品图片展示 -->
      <div class="stock-product-image" v-if="getProductImage(stock?.productId)">
        <ProductImageThumb
          :src="getProductImage(stock?.productId)"
          :preview-src-list="stockHeaderPreviewList"
          class="product-image-preview"
          :width="100"
          :height="100"
          :radius="8"
        />
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
        <el-descriptions-item label="成本价">¥{{ formatAmountDisplay(stock?.costPrice ?? 0) }}</el-descriptions-item>
        <el-descriptions-item label="库存价值"><span class="amount">¥{{ formatAmountDisplay(stock?.stockValue ?? 0) }}</span></el-descriptions-item>
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
        <el-button
          type="primary"
          @click="openPurchaseFromStock"
          v-if="stock?.stock < stock?.safeStock && canShortcutPurchaseForStock(stock, products)"
        ><el-icon><ShoppingCart /></el-icon>创建采购</el-button>
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
        <el-table-column label="入库时间" width="176">
          <template #default="{ row }">{{ formatTime(row.createTime || row.time) }}</template>
        </el-table-column>
        <el-table-column label="操作人" min-width="100">
          <template #default="{ row }">{{ row.operatorName || row.operator || '-' }}</template>
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
        <el-table-column label="商品" min-width="168" show-overflow-tooltip>
          <template #default="{ row }">
            <div class="outbound-product-mini">
              <ProductImageThumb
                v-if="outboundRowThumb(row)"
                :src="outboundRowThumb(row)"
                :preview-src-list="parseProductImageUrls(row?.productImage || row?.image)"
                class="outbound-thumb-mini"
                :width="32"
                :height="32"
                :radius="4"
              />
              <span v-else class="outbound-icon-mini">{{ getCategoryIcon(row.category || '') }}</span>
              <span class="outbound-name-mini">{{ row.productName || '-' }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="数量" width="80" align="center">
          <template #default="{ row }">{{ row.quantity }}</template>
        </el-table-column>
        <el-table-column label="出库时间" width="176">
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
          <el-select
            v-model="stockInboundForm.purchaseOrderId"
            v-load-more="{ popperClass: 'inventory-detail-purchase-order-dropdown', onLoadMore: loadMorePurchaseOrderOptions, disabled: purchaseOrderOptionsLoading || !purchaseOrderOptionsHasMore }"
            popper-class="inventory-detail-purchase-order-dropdown"
            placeholder="请选择采购单"
            style="width: 100%"
            filterable
            @change="onStockPurchaseOrderChange"
            @visible-change="onPurchaseOrderSelectVisibleChange"
            @filter-method="onPurchaseOrderFilter"
            :disabled="stockPendingPurchaseOrders.length === 0"
          >
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
          <el-select
            v-model="stockOutboundForm.salesOrderId"
            v-load-more="{ popperClass: 'inventory-detail-sales-order-dropdown', onLoadMore: loadMoreSalesOrderOptions, disabled: salesOrderOptionsLoading || !salesOrderOptionsHasMore }"
            popper-class="inventory-detail-sales-order-dropdown"
            placeholder="请选择销售单"
            style="width: 100%"
            filterable
            @change="onStockSalesOrderChange"
            @visible-change="onSalesOrderSelectVisibleChange"
            @filter-method="onSalesOrderFilter"
            :disabled="stockPendingSalesOrders.length === 0"
          >
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
              <el-select
                v-model="purchaseForm.supplierId"
                v-load-more="{ popperClass: 'inventory-detail-supplier-dropdown', onLoadMore: loadMoreSupplierOptions, disabled: supplierOptionsLoading || !supplierOptionsHasMore }"
                popper-class="inventory-detail-supplier-dropdown"
                placeholder="请选择供应商"
                style="width: 100%"
                filterable
                @visible-change="onSupplierSelectVisibleChange"
                @filter-method="onSupplierFilter"
              >
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
              <el-input :value="`¥${formatAmountDisplay(purchaseForm.quantity * purchaseForm.unitPrice || 0)}`" disabled />
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
import { useUserStore } from '@/stores/user'
import { useDataStore } from '@/stores/data'
import { inventoryApi, purchaseApi, salesApi, productApi, supplierApi, systemConfigApi } from '@/api'
import { formatTime } from '@/utils/time'
import ProductImageThumb from '@/components/ProductImageThumb.vue'
import { firstProductImageUrl, parseProductImageUrls } from '@/utils/productImages'
import { formatAmountDisplay } from '@/utils/moneyFormat'
import { canShortcutPurchaseForStock } from '@/utils/productStatus'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const dataStore = useDataStore()

/** 与列表页一致：流水数据仅在具备 inventory:records 时拉取 */
const canInventoryRecordsTab = computed(() => userStore.hasPermission('inventory:records'))

const stockId = computed(() => route.params.id)
const loading = ref(false)
const pageLoading = ref(true)
const OPTION_PAGE_SIZE = 20
/** 详情页出入库流水首屏请求条数，与库存主表「出入库记录」Tab 默认每页一致 */
const STOCK_RECORDS_FETCH_SIZE = 10

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
const systemStaleDays = ref(90)

// 对话框状态
const stockInboundVisible = ref(false)
const stockOutboundVisible = ref(false)
const purchaseDialogVisible = ref(false)
const stockInboundType = ref('purchase')
const stockOutboundType = ref('sales')

// 库存数据
const stock = ref(null)
const products = ref([])
const warehousesList = computed(() => dataStore.warehouses || [])
const suppliersList = ref([])
const purchaseOrders = ref([])
const purchaseOrderOptionsPage = ref(1)
const purchaseOrderOptionsHasMore = ref(true)
const purchaseOrderOptionsLoading = ref(false)
const purchaseOrderKeyword = ref('')
const salesOrders = ref([])
const salesOrderOptionsPage = ref(1)
const salesOrderOptionsHasMore = ref(true)
const salesOrderOptionsLoading = ref(false)
const salesOrderKeyword = ref('')
const supplierOptionsPage = ref(1)
const supplierOptionsHasMore = ref(true)
const supplierOptionsLoading = ref(false)
const supplierKeyword = ref('')
const inboundRecords = ref([])
const outboundRecords = ref([])

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
  return purchaseOrders.value.filter(po => Number(po.productId) === Number(stockInboundForm.value.productId) && isPendingPurchaseOrder(po))
})

// 待发货销售单
const stockPendingSalesOrders = computed(() => {
  if (!stockOutboundForm.value.productId) return []
  return salesOrders.value.filter(so => Number(so.productId) === Number(stockOutboundForm.value.productId) && isPendingSalesOrder(so))
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
  const product = (products.value || []).find(p => p.id === productId)
  return firstProductImageUrl(product?.image)
}

const stockHeaderPreviewList = computed(() => {
  const s = stock.value
  const p = (products.value || []).find((pp) => pp.id === s?.productId)
  return parseProductImageUrls(p?.image || s?.productImage || s?.image)
})

/** 出库流水行：优先用接口回填的 productImage（/inventory/outbounds） */
const outboundRowThumb = (row) => firstProductImageUrl(row?.productImage || row?.image)

const getCategoryIcon = (cat) =>
  ({ '手机': '📱', '电脑': '💻', '配件': '🎧', '手表': '⌚', '平板': '📱' }[cat] || '📦')

// 获取商品名称（从商品列表动态获取最新名称）
const getProductName = (productId, fallbackName) => {
  const product = (products.value || []).find(p => p.id === productId)
  return product?.name || fallbackName || '-'
}

const getWarehouseName = (warehouseId) => {
  const warehouse = (warehousesList.value || []).find(w => w.id === warehouseId)
  return warehouse?.name || ''
}

const getCategoryName = (productId) => {
  const product = (products.value || []).find(p => p.id === productId)
  if (!product) return ''
  return product.categoryName || product.category || ''
}

const getStockStatusType = (status) => ({ 'normal': 'success', 'warning': 'warning', 'critical': 'danger', '正常': 'success', '偏低': 'warning', '紧急补货': 'danger' }[status] || 'info')

const formatStockStatus = (status) => {
  const statusMap = { 'normal': '正常', 'warning': '偏低', 'critical': '紧急补货', '正常': '正常', '偏低': '偏低', '紧急补货': '紧急补货' }
  return statusMap[status] || status
}

const parseInventoryMoment = (raw) => {
  if (raw == null || raw === '') return null
  if (raw instanceof Date && !Number.isNaN(raw.getTime())) return raw
  const s = typeof raw === 'string' ? raw.trim().replace(' ', 'T') : String(raw)
  const d = new Date(s)
  return Number.isNaN(d.getTime()) ? null : d
}

const getStagnantDays = (stockData) => {
  const ref =
    parseInventoryMoment(stockData?.lastOutboundTime) ||
    parseInventoryMoment(stockData?.lastInboundTime) ||
    parseInventoryMoment(stockData?.createTime)
  if (!ref) return 0
  return Math.floor((Date.now() - ref.getTime()) / (1000 * 60 * 60 * 24))
}

const getStagnantWarningDays = (stockData) => {
  const row = stockData?.stagnantDays
  if (typeof row === 'number' && row >= 1) return row
  const g = systemStaleDays.value
  return typeof g === 'number' && g >= 1 ? g : 90
}

const getStagnantStatusType = (stockData) => {
  const stagnantDays = getStagnantDays(stockData)
  const warningDays = getStagnantWarningDays(stockData)
  if (stagnantDays >= warningDays) return 'danger'
  return 'success'
}

const getStagnantStatusText = (stockData) => {
  const stagnantDays = getStagnantDays(stockData)
  const warningDays = getStagnantWarningDays(stockData)
  if (stagnantDays >= warningDays) return `呆滞${stagnantDays}天`
  return `正常`
}

const mergeOptionsById = (existing, incoming) => {
  const merged = [...existing]
  const idSet = new Set(merged.map(item => item.id))
  ;(incoming || []).forEach(item => {
    if (!idSet.has(item.id)) {
      merged.push(item)
      idSet.add(item.id)
    }
  })
  return merged
}

const isPendingPurchaseOrder = (item) => item && item.inboundStatus !== 'completed' && item.inboundStatus !== 'cancelled'
const isPendingSalesOrder = (item) => item && item.payStatus === 'paid' && (item.salesOrderStatus || item.status) === 'pending'

const loadMorePurchaseOrderOptions = async ({ reset = false } = {}) => {
  if (purchaseOrderOptionsLoading.value || !stockInboundForm.value.productId) return
  if (reset) {
    purchaseOrderOptionsPage.value = 1
    purchaseOrderOptionsHasMore.value = true
    purchaseOrders.value = []
  } else if (!purchaseOrderOptionsHasMore.value) {
    return
  }
  purchaseOrderOptionsLoading.value = true
  try {
    const res = await purchaseApi.list({
      page: purchaseOrderOptionsPage.value,
      size: OPTION_PAGE_SIZE,
      keyword: purchaseOrderKeyword.value || undefined
    })
    const list = (res?.list || []).filter(item => Number(item.productId) === Number(stockInboundForm.value.productId) && isPendingPurchaseOrder(item))
    purchaseOrders.value = mergeOptionsById(purchaseOrders.value, list)
    if ((res?.list || []).length < OPTION_PAGE_SIZE) {
      purchaseOrderOptionsHasMore.value = false
    } else {
      purchaseOrderOptionsPage.value += 1
    }
  } finally {
    purchaseOrderOptionsLoading.value = false
  }
}

const loadMoreSalesOrderOptions = async ({ reset = false } = {}) => {
  if (salesOrderOptionsLoading.value || !stockOutboundForm.value.productId) return
  if (reset) {
    salesOrderOptionsPage.value = 1
    salesOrderOptionsHasMore.value = true
    salesOrders.value = []
  } else if (!salesOrderOptionsHasMore.value) {
    return
  }
  salesOrderOptionsLoading.value = true
  try {
    const res = await salesApi.list({
      page: salesOrderOptionsPage.value,
      size: OPTION_PAGE_SIZE,
      keyword: salesOrderKeyword.value || undefined,
      payStatus: 'paid',
      salesOrderStatus: 'pending'
    })
    const list = (res?.list || []).filter(item => Number(item.productId) === Number(stockOutboundForm.value.productId) && isPendingSalesOrder(item))
    salesOrders.value = mergeOptionsById(salesOrders.value, list)
    if ((res?.list || []).length < OPTION_PAGE_SIZE) {
      salesOrderOptionsHasMore.value = false
    } else {
      salesOrderOptionsPage.value += 1
    }
  } finally {
    salesOrderOptionsLoading.value = false
  }
}

const loadMoreSupplierOptions = async ({ reset = false } = {}) => {
  if (supplierOptionsLoading.value) return
  if (reset) {
    supplierOptionsPage.value = 1
    supplierOptionsHasMore.value = true
    suppliersList.value = []
  } else if (!supplierOptionsHasMore.value) {
    return
  }
  supplierOptionsLoading.value = true
  try {
    const res = await supplierApi.list({
      page: supplierOptionsPage.value,
      size: OPTION_PAGE_SIZE,
      keyword: supplierKeyword.value || undefined
    })
    const list = res?.list || []
    suppliersList.value = mergeOptionsById(suppliersList.value, list)
    if (list.length < OPTION_PAGE_SIZE) {
      supplierOptionsHasMore.value = false
    } else {
      supplierOptionsPage.value += 1
    }
  } finally {
    supplierOptionsLoading.value = false
  }
}

const onPurchaseOrderSelectVisibleChange = (visible) => {
  if (visible && purchaseOrders.value.length === 0) {
    loadMorePurchaseOrderOptions({ reset: true })
  }
}

const onSalesOrderSelectVisibleChange = (visible) => {
  if (visible && salesOrders.value.length === 0) {
    loadMoreSalesOrderOptions({ reset: true })
  }
}

const onSupplierSelectVisibleChange = (visible) => {
  if (visible && suppliersList.value.length === 0) {
    loadMoreSupplierOptions({ reset: true })
  }
}

const onPurchaseOrderFilter = (keyword) => {
  purchaseOrderKeyword.value = (keyword || '').trim()
  loadMorePurchaseOrderOptions({ reset: true })
}

const onSalesOrderFilter = (keyword) => {
  salesOrderKeyword.value = (keyword || '').trim()
  loadMoreSalesOrderOptions({ reset: true })
}

const onSupplierFilter = (keyword) => {
  supplierKeyword.value = (keyword || '').trim()
  loadMoreSupplierOptions({ reset: true })
}

// 加载数据（与库存页一致：无出入库记录权限不拉流水接口）；当前库存行用单笔详情接口
const loadData = async () => {
  const id = Number(stockId.value)
  if (!id) {
    ElMessage.warning('库存 ID 无效')
    router.replace('/inventory')
    return
  }
  pageLoading.value = true
  try {
    try {
      stock.value = await inventoryApi.get(id)
    } catch (e) {
      ElMessage.error(e.message || '加载库存失败')
      router.replace('/inventory')
      return
    }

    const loadProducts = async () => {
      if (!stock.value?.productId) {
        products.value = []
        return
      }
      try {
        const product = await productApi.get(stock.value.productId)
        products.value = product?.id ? [product] : []
      } catch (error) {
        products.value = []
      }
    }
    const loadWarehousesForPage = async () => {
      await dataStore.loadWarehouses({ forceOptions: true })
    }
    const loadSystemStaleDays = async () => {
      try {
        const cfg = await systemConfigApi.get()
        if (cfg?.staleDays != null && cfg.staleDays >= 1) {
          systemStaleDays.value = cfg.staleDays
        }
      } catch {
        /* 保持默认 90 */
      }
    }
    const loadInboundRecords = async () => {
      const res = await purchaseApi.inboundList({
        page: 1,
        size: STOCK_RECORDS_FETCH_SIZE,
        inventoryId: id
      })
      inboundRecords.value = res.list || []
    }
    const loadOutboundRecords = async () => {
      const res = await inventoryApi.outboundList({
        page: 1,
        size: STOCK_RECORDS_FETCH_SIZE,
        inventoryId: id
      })
      outboundRecords.value = res.list || []
    }

    const tasks = [
      loadProducts(),
      loadWarehousesForPage(),
      loadSystemStaleDays()
    ]
    if (canInventoryRecordsTab.value) {
      tasks.push(loadInboundRecords(), loadOutboundRecords())
    }
    await Promise.all(tasks)

    if (stock.value) {
      stockSafeStockEdit.value = stock.value.safeStock || 10
      stockLocationEdit.value = stock.value.location || ''
      stockStagnantDaysEdit.value =
        stock.value.stagnantDays != null && stock.value.stagnantDays >= 1
          ? stock.value.stagnantDays
          : systemStaleDays.value
    }
  } finally {
    pageLoading.value = false
  }
}

// 更新安全库存
const updateSafeStock = async () => {
  if (!stock.value || !stockSafeStockEdit.value) return
  loading.value = true
  try {
    await inventoryApi.updateSafeStock(stock.value.id, stockSafeStockEdit.value)
    ElMessage.success('库存预警值已更新')
    stock.value = await inventoryApi.get(stock.value.id)
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
    stock.value = await inventoryApi.get(stock.value.id)
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
    stock.value = await inventoryApi.get(stock.value.id)
  } catch (error) {
    ElMessage.error(error.message || '更新失败')
  } finally {
    loading.value = false
  }
}

// 打开入库对话框
const openStockInbound = async () => {
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
  await loadMorePurchaseOrderOptions({ reset: true })
  stockInboundVisible.value = true
}

// 打开出库对话框
const openStockOutbound = async () => {
  const currentStock = stock.value?.stock ?? 0
  if (currentStock <= 0) {
    ElMessage.warning('当前库存为0，无法出库')
    return
  }
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
  await loadMoreSalesOrderOptions({ reset: true })
  stockOutboundVisible.value = true
}

// 选择采购单时设置最大入库数量
const onStockPurchaseOrderChange = (purchaseOrderId) => {
  const po = (purchaseOrders.value || []).find(p => p.id === purchaseOrderId)
  if (po) {
    const pendingQty = po.pendingQuantity || (po.totalQuantity || po.quantity || 0) - (po.inboundQuantity || 0)
    stockInboundForm.value.maxQuantity = pendingQty > 0 ? pendingQty : 1
    stockInboundForm.value.quantity = pendingQty > 0 ? pendingQty : 1
  }
}

// 选择销售单时设置最大出库数量
const onStockSalesOrderChange = (salesOrderId) => {
  const so = (salesOrders.value || []).find(s => s.id === salesOrderId)
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
  if (!canShortcutPurchaseForStock(stock.value, products.value)) {
    ElMessage.warning('该商品已停售，无法创建采购单')
    return
  }
  await loadMoreSupplierOptions({ reset: true })
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
    justify-content: flex-end;
    align-items: center;
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
      flex-shrink: 0;
      border-radius: 8px;
      overflow: hidden;
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

  .outbound-product-mini {
    display: flex;
    align-items: center;
    gap: 8px;
    .outbound-thumb-mini {
      flex-shrink: 0;
      border-radius: 4px;
      overflow: hidden;
    }
    .outbound-icon-mini {
      font-size: 18px;
      flex-shrink: 0;
      width: 32px;
      text-align: center;
    }
    .outbound-name-mini {
      font-size: 13px;
    }
  }
}
</style>