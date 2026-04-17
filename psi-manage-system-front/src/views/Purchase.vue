<template>
  <div class="purchase-page">
    <el-alert
      v-if="showAuxDataHint"
      type="info"
      :closable="false"
      show-icon
      class="perm-hint"
      title="当前账号缺少商品或仓库目录的查看权限，部分筛选、下拉与缩略图可能不可用；如需完整体验请在角色中勾选「商品查看」「库存查看」或对应采购/库存操作权限。"
    />
    <!-- 标签页 -->
    <el-tabs v-model="activeTab" class="page-tabs">
      <!-- 采购订单 -->
      <el-tab-pane label="采购订单" name="orders">
        <el-card>
          <template #header>
            <div class="card-header">
              <div class="header-actions">
                <el-select v-model="filterSupplier" placeholder="按供应商筛选" clearable filterable style="width: 160px">
                  <el-option v-for="s in suppliersList" :key="s.id" :label="s.name" :value="s.id" />
                </el-select>
                <el-select v-model="filterProduct" placeholder="按商品筛选" clearable filterable style="width: 160px">
                  <el-option v-for="p in productList" :key="p.id" :label="p.name" :value="p.id" />
                </el-select>
                <el-select v-model="filterInboundStatus" placeholder="按入库状态筛选" clearable filterable style="width: 120px">
                  <el-option label="待入库" value="pending" />
                  <el-option label="部分入库" value="partial" />
                  <el-option label="已完成" value="completed" />
                </el-select>
                <el-select v-model="filterPayStatus" placeholder="按付款状态筛选" clearable filterable style="width: 120px">
                  <el-option label="待付款" value="unpaid" />
                  <el-option label="已付款" value="paid" />
                </el-select>
                <el-date-picker
                  v-model="filterExpectDateRange"
                  type="daterange"
                  range-separator="至"
                  start-placeholder="期望交货开始"
                  end-placeholder="期望交货结束"
                  style="width: 200px"
                  value-format="YYYY-MM-DD"
                />
                <el-input v-model="searchKeyword" placeholder="搜索采购单号、供应商..." prefix-icon="Search" clearable style="width: 180px" />
                <el-button v-if="canAddPurchase" type="primary" @click="openPurchaseDialog">
                  <el-icon><Plus /></el-icon>
                  新建采购单
                </el-button>
              </div>
            </div>
          </template>
          <!-- 查询结果统计 -->
          <div class="query-stats">
            <div class="stat-item">
              <span class="stat-label">采购订单数</span>
              <span class="stat-value">{{ purchaseOrderTotal }}</span>
            </div>
            <div class="stat-item">
              <span class="stat-label">采购金额</span>
              <span class="stat-value amount">¥{{ formatAmountDisplay(purchaseOrderSummary.totalAmount) }}</span>
            </div>
            <div class="stat-item">
              <span class="stat-label">已付款</span>
              <span class="stat-value success">¥{{ formatAmountDisplay(purchaseOrderSummary.paidAmount) }}</span>
            </div>
            <div class="stat-item">
              <span class="stat-label">待付款</span>
              <span class="stat-value warning">¥{{ formatAmountDisplay(purchaseOrderSummary.unpaidAmount) }}</span>
            </div>
            <div class="stat-item">
              <span class="stat-label">已退款</span>
              <span class="stat-value info">¥{{ formatAmountDisplay(purchaseOrderSummary.refundedAmount) }}</span>
            </div>
          </div>
          <el-table :data="purchaseOrderTableRows" empty-text="暂无数据" style="width: 100%" :max-height="500">
            <el-table-column label="单号" min-width="140">
              <template #default="{ row }">
                <span class="order-no">{{ row.orderNo }}</span>
              </template>
            </el-table-column>
            <el-table-column label="供应商" min-width="120" show-overflow-tooltip>
              <template #default="{ row }">
                <span>{{ getSupplierDisplayName(row) }}</span>
              </template>
            </el-table-column>
            <el-table-column label="商品" min-width="150" show-overflow-tooltip>
              <template #default="{ row }">
                <div class="product-cell-mini">
                  <img v-if="getProductImage(row.productId)" :src="getProductImage(row.productId)" class="product-thumb-mini" />
                  <span v-else class="product-icon-mini">{{ getProductIcon(getProductName(row.productId, row.productName || row.product)) }}</span>
                  <span class="product-name-mini">{{ getProductName(row.productId, row.productName || row.product) }}</span>
                </div>
              </template>
            </el-table-column>
            <el-table-column label="总量" width="70" align="center">
              <template #default="{ row }">
                <span class="total-qty">{{ row.quantity || row.totalQuantity }}</span>
              </template>
            </el-table-column>
            <el-table-column label="待入" width="70" align="center">
              <template #default="{ row }">
                <span :class="{ 'pending-qty': (row.pendingQuantity ?? (row.totalQuantity || row.quantity || 0) - (row.inboundQuantity || 0)) > 0 }">{{ row.pendingQuantity ?? (row.totalQuantity || row.quantity || 0) - (row.inboundQuantity || 0) }}</span>
              </template>
            </el-table-column>
            <el-table-column label="已入" width="70" align="center">
              <template #default="{ row }">
                <span class="inbound-qty">{{ row.inboundQuantity || 0 }}</span>
              </template>
            </el-table-column>
            <el-table-column label="金额" min-width="100" align="right" show-overflow-tooltip>
              <template #default="{ row }">
                <span class="amount">¥{{ formatAmountDisplay(row.amount || 0) }}</span>
              </template>
            </el-table-column>
            <el-table-column label="入库状态" width="90" align="center">
              <template #default="{ row }">
                <el-tag :type="getInboundStatusType(row.inboundStatus)" effect="light" size="small">{{ formatInboundStatus(row.inboundStatus) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="付款" width="80" align="center">
              <template #default="{ row }">
                <el-tag :type="getPayStatusType(row.payStatus)" effect="light" size="small">{{ formatPayStatus(row.payStatus) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="期望交货" min-width="110" show-overflow-tooltip>
              <template #default="{ row }">
                <span :class="{ 'overdue': isExpectDateOverdue(row.expectDate) && row.inboundStatus !== 'completed' }">{{ formatExpectDate(row.expectDate) }}</span>
              </template>
            </el-table-column>
            <el-table-column label="下单时间" prop="createTime" width="176" show-overflow-tooltip />
            <el-table-column label="操作" width="140" fixed="right" align="center">
              <template #default="{ row }">
                <el-button v-if="canViewPurchase" type="primary" link size="small" @click="viewPurchaseOrder(row)">详情</el-button>
                <el-button
                  v-if="canEditPurchase && row.inboundStatus === 'pending'"
                  type="primary"
                  link
                  size="small"
                  @click="editPurchaseOrder(row)"
                >编辑</el-button>
                <el-button
                  v-if="canEditPurchase && row.inboundStatus === 'pending'"
                  type="danger"
                  link
                  size="small"
                  @click="deletePurchaseOrder(row)"
                >删除</el-button>
              </template>
            </el-table-column>
          </el-table>
          <div class="pagination-wrapper">
            <el-pagination
              v-model:current-page="currentPage"
              v-model:page-size="pageSize"
              :total="purchaseOrderTotal"
              :page-sizes="[10, 20, 50, 100]"
              layout="total, sizes, prev, pager, next, jumper"
            />
          </div>
        </el-card>
      </el-tab-pane>

      <!-- 入库记录 -->
      <el-tab-pane label="入库记录" name="inbound">
        <el-card>
          <template #header>
            <div class="card-header">
              <div class="header-actions">
                <el-date-picker
                  v-model="filterInboundTimeRange"
                  type="daterange"
                  range-separator="至"
                  start-placeholder="入库开始"
                  end-placeholder="入库结束"
                  style="width: 200px"
                  value-format="YYYY-MM-DD"
                />
                <el-select v-model="filterInboundOperator" placeholder="按操作人筛选" clearable filterable style="width: 120px">
                  <el-option v-for="u in inboundOperators" :key="u" :label="u" :value="u" />
                </el-select>
                <el-select v-model="filterInboundWarehouse" placeholder="按仓库筛选" clearable filterable style="width: 120px">
                  <el-option v-for="w in warehousesList" :key="w.id" :label="w.name" :value="w.id" />
                </el-select>
                <el-input v-model="inboundSearchKeyword" placeholder="搜索入库单号、采购单号..." prefix-icon="Search" clearable style="width: 180px" />
              </div>
            </div>
          </template>
          <el-table :data="inboundTableRows" empty-text="暂无数据" style="width: 100%" :max-height="500">
            <el-table-column label="入库单号" width="140">
              <template #default="{ row }">
                <span class="order-no success">{{ row.orderNo }}</span>
              </template>
            </el-table-column>
            <el-table-column label="采购单号" width="140">
              <template #default="{ row }">
                <span class="order-no clickable" @click="goToPurchaseOrder(row)">{{ row.purchaseOrderNo || row.purchaseNo }}</span>
              </template>
            </el-table-column>
            <el-table-column label="供应商" min-width="120" show-overflow-tooltip>
              <template #default="{ row }">{{ getSupplierDisplayName(row) }}</template>
            </el-table-column>
            <el-table-column label="商品" min-width="150" show-overflow-tooltip>
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
            <el-table-column label="仓库" min-width="120" show-overflow-tooltip>
              <template #default="{ row }">{{ getWarehouseName(row.warehouseId) || row.warehouseName || row.warehouse }}</template>
            </el-table-column>
            <el-table-column label="入库时间" width="176" show-overflow-tooltip>
              <template #default="{ row }">{{ row.createTime || row.time }}</template>
            </el-table-column>
            <el-table-column label="操作人" width="100" show-overflow-tooltip>
              <template #default="{ row }">{{ row.operatorName || row.operator }}</template>
            </el-table-column>
            <el-table-column label="备注" min-width="120" show-overflow-tooltip>
              <template #default="{ row }">{{ row.remark || '-' }}</template>
            </el-table-column>
          </el-table>
          <div class="pagination-wrapper">
            <el-pagination
              v-model:current-page="inboundCurrentPage"
              v-model:page-size="inboundPageSize"
              :total="inboundTableTotal"
              :page-sizes="[10, 20, 50, 100]"
              layout="total, sizes, prev, pager, next, jumper"
            />
          </div>
        </el-card>
      </el-tab-pane>

      <!-- 供应商管理：仅 purchase:supplier（勿用 purchase:add/edit 或父级菜单码代替） -->
      <el-tab-pane v-if="canPurchaseSuppliersTab" label="供应商管理" name="suppliers">
        <div class="supplier-search-bar">
          <el-input
            v-model="supplierSearchKeyword"
            placeholder="搜索供应商名称、联系人、电话..."
            prefix-icon="Search"
            clearable
            style="width: 200px"
          />
          <el-button v-if="canAddSupplier" type="primary" @click="openSupplierDialog">
            <el-icon><Plus /></el-icon>
            添加供应商
          </el-button>
        </div>
        <el-empty v-if="!filteredSuppliersListWithStats.length" class="grid-empty" description="暂无数据" :image-size="80" />
        <div v-else class="suppliers-grid">
          <el-card class="supplier-card" v-for="s in filteredSuppliersListWithStats" :key="s.id">
            <div class="supplier-header">
              <div class="supplier-avatar">🏭</div>
              <div class="supplier-info">
                <h4>{{ s.name }}</h4>
                <p class="supplier-industry">{{ s.industryNames || '未设置行业' }}</p>
                <p>{{ s.address }}</p>
              </div>
            </div>
            <div class="supplier-stats">
              <div class="supplier-stat">
                <span class="stat-label">合作订单</span>
                <span class="stat-value">{{ s.orders }}</span>
              </div>
              <div class="supplier-stat">
                <span class="stat-label">总采购额</span>
                <span class="stat-value">¥{{ formatAmountDisplay(s.amount ?? 0) }}</span>
              </div>
            </div>
            <div class="supplier-contact">
              <div class="contact-item">
                <el-icon><Phone /></el-icon>
                <span>{{ s.phone }}</span>
              </div>
              <div class="contact-item">
                <el-icon><Message /></el-icon>
                <span>{{ s.email }}</span>
              </div>
            </div>
            <div class="supplier-actions">
              <el-button v-if="canPurchaseSuppliersTab" type="primary" @click="viewSupplierDetail(s)">查看详情</el-button>
              <el-button v-if="canEditSupplierEntity" @click="editSupplier(s)">编辑</el-button>
              <el-button v-if="canEditSupplierEntity" type="danger" plain @click="deleteSupplier(s)">删除</el-button>
            </div>
          </el-card>
        </div>
      </el-tab-pane>
    </el-tabs>

    <!-- 新建采购单对话框 -->
    <el-dialog v-model="purchaseDialogVisible" title="新建采购单" width="760px" destroy-on-close>
      <el-form ref="purchaseFormRef" :model="purchaseForm" :rules="purchaseRules" label-width="100px">
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
            <el-form-item label="商品选择" prop="productId">
              <el-select
                v-model="purchaseForm.productId"
                placeholder="请选择商品"
                style="width: 100%"
                filterable
                @change="onProductChange"
              >
                <el-option
                  v-for="p in productListForNewOrder"
                  :key="p.id"
                  :label="p.name"
                  :value="p.id"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="商品图片">
              <div class="selected-product-image">
                <img v-if="selectedProductImage" :src="selectedProductImage" class="product-preview" />
                <div v-else class="product-preview-placeholder">
                  <span class="placeholder-icon">📦</span>
                  <span class="placeholder-text">请先选择商品</span>
                </div>
              </div>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="采购总量" prop="totalQuantity">
              <el-input-number v-model="purchaseForm.totalQuantity" :min="1" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="单价（元）" prop="price">
              <el-input-number v-model="purchaseForm.price" :min="0" :precision="2" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="待入库">
              <el-input :model-value="formatQtyDisplay(purchaseForm.pendingQuantity)" disabled class="purchase-qty-readonly" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="已入库">
              <el-input :model-value="formatQtyDisplay(purchaseForm.inboundQuantity)" disabled class="purchase-qty-readonly" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="24">
            <el-form-item label="采购金额">
              <el-input :value="`¥${formatAmountDisplay(purchaseForm.totalQuantity * purchaseForm.price || 0)}`" disabled />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="期望交货">
              <el-date-picker v-model="purchaseForm.deliveryDate" type="date" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="付款方式">
              <el-select v-model="purchaseForm.payMethod" style="width: 100%">
                <el-option label="账期30天" value="账期30天" />
                <el-option label="现结" value="现结" />
                <el-option label="账期15天" value="账期15天" />
                <el-option label="账期60天" value="账期60天" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="备注">
          <el-input v-model="purchaseForm.remark" placeholder="输入采购备注" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="purchaseDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitPurchase" :loading="submitting">确认创建</el-button>
      </template>
    </el-dialog>

    <!-- 添加供应商对话框 -->
    <el-dialog v-model="supplierDialogVisible" :title="supplierForm.id ? '编辑供应商' : '添加供应商'" width="700px" destroy-on-close>
      <el-form ref="supplierFormRef" :model="supplierForm" :rules="supplierRules" label-width="100px">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="供应商名称" prop="name">
              <el-input v-model="supplierForm.name" placeholder="输入供应商名称" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="所属行业">
              <el-select
                v-model="supplierForm.industryIds"
                multiple
                collapse-tags
                collapse-tags-tooltip
                clearable
                placeholder="请选择所属行业（可多选）"
                style="width: 100%"
                filterable
              >
                <el-option v-for="it in supplierIndustriesList" :key="it.id" :label="it.name" :value="it.id" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="地址" prop="address">
          <el-input v-model="supplierForm.address" placeholder="输入详细地址" />
        </el-form-item>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="联系人" prop="contact">
              <el-input v-model="supplierForm.contact" placeholder="输入联系人姓名" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="联系电话" prop="phone">
              <el-input v-model="supplierForm.phone" placeholder="输入联系电话" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="supplierForm.email" placeholder="输入邮箱地址" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="supplierForm.remark" placeholder="输入备注信息" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="supplierDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitSupplier">确认添加</el-button>
      </template>
    </el-dialog>

    <!-- 订单详情对话框 -->
    <el-dialog v-model="orderDetailVisible" title="采购单详情" width="500px">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="采购单号">{{ currentOrder?.orderNo }}</el-descriptions-item>
        <el-descriptions-item label="供应商">{{ getSupplierDisplayName(currentOrder) }}</el-descriptions-item>
        <el-descriptions-item label="商品">
          <div class="product-cell">
            <img v-if="getProductImage(currentOrder?.productId)" :src="getProductImage(currentOrder?.productId)" class="product-thumb" />
            <span v-else class="product-icon">{{ getProductIcon(getProductName(currentOrder?.productId, currentOrder?.productName || currentOrder?.product)) }}</span>
            <span class="product-name">{{ getProductName(currentOrder?.productId, currentOrder?.productName || currentOrder?.product) }}</span>
          </div>
        </el-descriptions-item>
        <el-descriptions-item label="数量">{{ currentOrder?.totalQuantity || currentOrder?.quantity }}</el-descriptions-item>
        <el-descriptions-item label="金额">¥{{ formatAmountDisplay(currentOrder?.amount ?? 0) }}</el-descriptions-item>
        <el-descriptions-item label="入库状态">
          <el-tag :type="getInboundStatusType(currentOrder?.inboundStatus)">{{ formatInboundStatus(currentOrder?.inboundStatus) }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="付款状态">
          <el-tag :type="getPayStatusType(currentOrder?.payStatus)">{{ formatPayStatus(currentOrder?.payStatus) }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ currentOrder?.createTime }}</el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <el-button @click="orderDetailVisible = false">关闭</el-button>
        <el-button type="primary" @click="printPurchaseOrder">打印采购单</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useDataStore } from '@/stores/data'
import { useUserStore } from '@/stores/user'
import { productApi, purchaseApi, supplierApi, warehouseApi } from '@/api'
import { firstProductImageUrl } from '@/utils/productImages'
import { formatAmountDisplay } from '@/utils/moneyFormat'
import { isProductSelectableForOrder } from '@/utils/productStatus'

const router = useRouter()
const dataStore = useDataStore()
const userStore = useUserStore()

const hasPurchaseMenu = computed(() => userStore.hasPermission('purchase'))

/** 供应商管理 Tab / 增删改供应商：仅 purchase:supplier（与权限表「供应商管理」一致） */
const canPurchaseSuppliersTab = computed(() => userStore.hasPermission('purchase:supplier'))

/** 查看：菜单或「采购订单查看」 */
const canViewPurchase = computed(
  () => userStore.hasPermission('purchase:view') || hasPurchaseMenu.value
)
/** 写操作仅认 purchase:add / purchase:edit，不再因仅有菜单码 purchase 显示按钮 */
const canAddPurchase = computed(() => userStore.hasPermission('purchase:add'))
const canEditPurchase = computed(() => userStore.hasPermission('purchase:edit'))
const canAddSupplier = computed(() => userStore.hasPermission('purchase:supplier'))
const canEditSupplierEntity = computed(() => userStore.hasPermission('purchase:supplier'))

/** 与后端 GET /products、GET /warehouses 的 OR 规则一致，避免无意义请求 */
const canLoadProductList = computed(
  () =>
    userStore.hasPermission('products') ||
    userStore.hasPermission('product:view') ||
    userStore.hasPermission('purchase') ||
    userStore.hasPermission('purchase:view')
)
const canLoadWarehouseList = computed(
  () =>
    userStore.hasPermission('inventory') ||
    userStore.hasPermission('inventory:view') ||
    userStore.hasPermission('purchase') ||
    userStore.hasPermission('purchase:view')
)

const showAuxDataHint = computed(
  () => !canLoadProductList.value || !canLoadWarehouseList.value
)

const activeTab = ref('orders')
const searchKeyword = ref('')
const filterSupplier = ref(null)
const filterProduct = ref(null)
const filterInboundStatus = ref(null)
const filterPayStatus = ref(null)
const filterExpectDateRange = ref(null)
const currentPage = ref(1)
const pageSize = ref(10)
const inboundCurrentPage = ref(1)
const inboundPageSize = ref(10)
const filterInboundTimeRange = ref(null)
const filterInboundOperator = ref(null)
const filterInboundWarehouse = ref(null)
const inboundSearchKeyword = ref('')
const supplierSearchKeyword = ref('')
const purchaseDialogVisible = ref(false)
const supplierDialogVisible = ref(false)
const orderDetailVisible = ref(false)
const purchaseFormRef = ref()
const supplierFormRef = ref()
const currentOrder = ref(null)
const submitting = ref(false)
const loading = ref(false)

// 统计数据
const purchaseStats = ref({
  monthAmount: 0,
  pendingInboundCount: 0,
  supplierCount: 0,
  unpaidCount: 0,
  unpaidAmount: 0
})

// 数据从后端获取
const productList = computed(() => dataStore.products || [])
/** 新建采购单商品下拉：排除停售（列表筛选仍用全部商品） */
const productListForNewOrder = computed(() => productList.value.filter(isProductSelectableForOrder))
const suppliersList = computed(() => dataStore.suppliers || [])
const supplierIndustriesList = computed(() => dataStore.supplierIndustries || [])
const warehousesList = computed(() => dataStore.warehouses || [])

// 动态获取仓库名称 - 从仓库列表中根据warehouseId查找
const getWarehouseName = (warehouseId) => {
  const warehouse = warehousesList.value.find(w => w.id === warehouseId)
  return warehouse?.name || ''
}

/** 只读数量展示：无步进器占位，大数字用千分位，避免挤占宽度 */
const formatQtyDisplay = (n) => {
  if (n == null || n === '') return '—'
  const num = Number(n)
  if (Number.isNaN(num)) return String(n)
  return num.toLocaleString()
}

const purchaseOrders = computed(() => dataStore.purchaseOrders || [])
const inboundRecords = computed(() => dataStore.inboundRecords || [])

// 供应商列表带统计数据（从采购订单计算）
const suppliersListWithStats = computed(() => {
  const suppliers = dataStore.suppliers || []
  const orders = dataStore.purchaseOrders || []
  return suppliers.map(s => {
    const supplierOrders = orders.filter(o => o.supplierId === s.id)
    return {
      ...s,
      orders: supplierOrders.length,
      amount: supplierOrders.reduce((sum, o) => sum + (o.amount || 0), 0)
    }
  })
})

const filteredSuppliersListWithStats = computed(() => {
  const list = suppliersListWithStats.value
  const kw = supplierSearchKeyword.value?.trim()
  if (!kw) return list
  const lower = kw.toLowerCase()
  return list.filter(s => {
    const hay = [
      s.name,
      s.code,
      s.contact,
      s.phone,
      s.email,
      s.address,
      s.industryNames
    ]
      .filter(Boolean)
      .map(v => String(v).toLowerCase())
    return hay.some(t => t.includes(lower))
  })
})

/** 采购订单主表：服务端分页 */
const purchaseOrderTableRows = ref([])
const purchaseOrderTotal = ref(0)
const purchaseOrderSummary = ref({
  totalAmount: 0,
  paidAmount: 0,
  unpaidAmount: 0,
  refundedAmount: 0
})

const fetchPurchaseOrderTable = async (showLoading = true) => {
  if (showLoading) loading.value = true
  try {
    const res = await purchaseApi.list({
      page: currentPage.value,
      size: pageSize.value,
      keyword: searchKeyword.value || undefined,
      supplierId: filterSupplier.value || undefined,
      productId: filterProduct.value || undefined,
      inboundStatus: filterInboundStatus.value || undefined,
      payStatus: filterPayStatus.value || undefined,
      expectDateStart: filterExpectDateRange.value?.[0],
      expectDateEnd: filterExpectDateRange.value?.[1]
    })
    purchaseOrderTableRows.value = res.list || []
    purchaseOrderTotal.value = Number(res.total) || 0
    const s = res.summary || {}
    purchaseOrderSummary.value = {
      totalAmount: Number(s.totalAmount) || 0,
      paidAmount: Number(s.paidAmount) || 0,
      unpaidAmount: Number(s.unpaidAmount) || 0,
      refundedAmount: Number(s.refundedAmount) || 0
    }
  } catch (e) {
    ElMessage.error(e.message || '加载采购订单失败')
    purchaseOrderTableRows.value = []
    purchaseOrderTotal.value = 0
    purchaseOrderSummary.value = { totalAmount: 0, paidAmount: 0, unpaidAmount: 0, refundedAmount: 0 }
  } finally {
    if (showLoading) loading.value = false
  }
}

watch(
  [filterSupplier, filterProduct, filterInboundStatus, filterPayStatus, filterExpectDateRange, searchKeyword],
  () => {
    currentPage.value = 1
    fetchPurchaseOrderTable()
  }
)

watch([currentPage, pageSize], () => {
  fetchPurchaseOrderTable()
})

/** 入库记录：服务端分页 */
const inboundTableRows = ref([])
const inboundTableTotal = ref(0)

const fetchInboundTable = async (showLoading = true) => {
  if (showLoading) loading.value = true
  try {
    const res = await purchaseApi.inboundList({
      page: inboundCurrentPage.value,
      size: inboundPageSize.value,
      keyword: inboundSearchKeyword.value || undefined,
      warehouseId: filterInboundWarehouse.value || undefined,
      operatorName: filterInboundOperator.value || undefined,
      createTimeStart: filterInboundTimeRange.value?.[0],
      createTimeEnd: filterInboundTimeRange.value?.[1]
    })
    inboundTableRows.value = res.list || []
    inboundTableTotal.value = Number(res.total) || 0
  } catch (e) {
    ElMessage.error(e.message || '加载入库记录失败')
    inboundTableRows.value = []
    inboundTableTotal.value = 0
  } finally {
    if (showLoading) loading.value = false
  }
}

// 操作人下拉：由当前已加载的入库记录去重（辅助筛选）
const inboundOperators = computed(() => {
  const operators = new Set()
  inboundRecords.value.forEach(r => {
    if (r.operatorName || r.operator) {
      operators.add(r.operatorName || r.operator)
    }
  })
  return Array.from(operators)
})

watch(
  [filterInboundTimeRange, filterInboundOperator, filterInboundWarehouse, inboundSearchKeyword],
  () => {
    inboundCurrentPage.value = 1
    fetchInboundTable()
  }
)

watch([inboundCurrentPage, inboundPageSize], () => {
  fetchInboundTable()
})

// 加载统计数据
const loadStats = async () => {
  try {
    const stats = await purchaseApi.stats()
    purchaseStats.value = stats || {
      monthAmount: 0,
      pendingInboundCount: 0,
      supplierCount: 0,
      unpaidCount: 0,
      unpaidAmount: 0
    }
  } catch (error) {
    console.error('加载统计数据失败:', error)
  }
}

// 加载数据：采购相关接口始终拉；行业字典仅供应商管理需要
const loadData = async () => {
  loading.value = true
  try {
    const tasks = [
      dataStore.loadSuppliers(),
      dataStore.loadPurchaseOrders(),
      dataStore.loadInboundRecords()
    ]
    if (canPurchaseSuppliersTab.value) {
      tasks.push(dataStore.loadSupplierIndustries())
    }
    if (canLoadProductList.value) {
      tasks.push(dataStore.loadProducts())
    }
    if (canLoadWarehouseList.value) {
      tasks.push(dataStore.loadWarehouses())
    }
    await Promise.all(tasks)
    await loadStats()
    await fetchPurchaseOrderTable(false)
    await fetchInboundTable(false)
  } finally {
    loading.value = false
  }
}

// 表单
const purchaseForm = ref({
  supplierId: null,
  productId: null,
  date: new Date(),
  totalQuantity: 1,
  pendingQuantity: 1,
  inboundQuantity: 0,
  price: 0,
  warehouseId: 1,
  deliveryDate: new Date(Date.now() + 5 * 24 * 60 * 60 * 1000),
  payMethod: '账期30天',
  remark: ''
})

const purchaseRules = {
  supplierId: [{ required: true, message: '请选择供应商', trigger: 'change' }],
  productId: [{ required: true, message: '请选择商品', trigger: 'change' }],
  totalQuantity: [{ required: true, message: '请输入采购总量', trigger: 'blur' }],
  price: [{ required: true, message: '请输入单价', trigger: 'blur' }]
}

// 监听采购总量变化，自动更新待入库数量
watch(() => purchaseForm.value.totalQuantity, (newVal) => {
  purchaseForm.value.pendingQuantity = newVal
})

const supplierForm = ref({
  name: '',
  industryIds: [],
  address: '',
  contact: '',
  phone: '',
  email: '',
  remark: ''
})

const supplierRules = {
  name: [{ required: true, message: '请输入供应商名称', trigger: 'blur' }],
  address: [{ required: true, message: '请输入地址', trigger: 'blur' }],
  phone: [{ required: true, message: '请输入联系电话', trigger: 'blur' }]
}

// 商品选择变化时自动填充成本价
const onProductChange = (productId) => {
  const product = productList.value.find(p => p.id === productId)
  if (product) {
    purchaseForm.value.price = product.costPrice || 0
  }
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

// 获取商品图片
const getProductImage = (productId) => {
  const product = productList.value.find(p => p.id === productId)
  return firstProductImageUrl(product?.image)
}

// 获取商品名称（从商品列表动态获取最新名称）
const getProductName = (productId, fallbackName) => {
  const product = productList.value.find(p => p.id === productId)
  return product?.name || fallbackName || '-'
}

// 选中商品的图片
const selectedProductImage = computed(() => {
  if (!purchaseForm.value.productId) return null
  return getProductImage(purchaseForm.value.productId)
})

// 辅助函数
const getSupplierAddress = (id) => {
  const s = suppliersList.value.find(s => s.id === id)
  return s?.address || ''
}

// 通过 supplierId 获取最新的供应商名称
const getSupplierDisplayName = (row) => {
  // 如果有 supplierId，从供应商列表中获取最新的名称
  if (row.supplierId) {
    const supplier = suppliersList.value.find(s => s.id === row.supplierId)
    if (supplier) return supplier.name
  }
  // 否则使用存储的名称
  return row.supplierName || row.supplier
}

// 状态格式化函数 - 英文转中文
const formatInboundStatus = (status) => {
  const statusMap = {
    'pending': '待入库',
    'partial': '部分入库',
    'completed': '已完成',
    'cancelled': '已取消',
    '待入库': '待入库',
    '部分入库': '部分入库',
    '已完成': '已完成',
    '已取消': '已取消'
  }
  return statusMap[status] || status
}

const formatPayStatus = (status) => {
  const statusMap = {
    'unpaid': '待付款',
    'paid': '已付款',
    'refunded': '已退款',
    '待付款': '待付款',
    '已付款': '已付款',
    '已退款': '已退款'
  }
  return statusMap[status] || status
}

const getInboundStatusType = (status) => {
  const typeMap = { 'completed': 'success', 'partial': 'warning', 'pending': 'info', 'cancelled': 'danger', '已完成': 'success', '部分入库': 'warning', '待入库': 'info', '已取消': 'danger' }
  return typeMap[status] || 'info'
}

const getPayStatusType = (status) => {
  const typeMap = { 'paid': 'success', 'unpaid': 'warning', 'refunded': 'danger', '已付款': 'success', '待付款': 'warning', '已退款': 'danger' }
  return typeMap[status] || 'info'
}

// 格式化期望交货日期
const formatExpectDate = (expectDate) => {
  if (!expectDate) return '-'
  // 如果是字符串格式，直接截取日期部分
  if (typeof expectDate === 'string') {
    return expectDate.substring(0, 10)
  }
  return expectDate
}

// 判断期望交货日期是否已过期
const isExpectDateOverdue = (expectDate) => {
  if (!expectDate) return false
  const today = new Date()
  today.setHours(0, 0, 0, 0)
  const expect = new Date(expectDate)
  return expect < today
}

// 打开对话框
const openPurchaseDialog = async () => {
  if (!canAddPurchase.value) {
    ElMessage.warning('无新建采购单权限')
    return
  }
  await loadData()
  purchaseForm.value = {
    supplierId: null,
    productId: null,
    date: new Date(),
    totalQuantity: 1,
    pendingQuantity: 1,
    inboundQuantity: 0,
    price: 0,
    warehouseId: 1,
    deliveryDate: new Date(Date.now() + 5 * 24 * 60 * 60 * 1000),
    payMethod: '账期30天',
    remark: ''
  }
  purchaseDialogVisible.value = true
}

const openSupplierDialog = () => {
  if (!canAddSupplier.value) {
    ElMessage.warning('无添加供应商权限')
    return
  }
  supplierForm.value = { name: '', industryIds: [], address: '', contact: '', phone: '', email: '', remark: '' }
  supplierDialogVisible.value = true
}

// 提交采购订单 - 调用后端API
const submitPurchase = async () => {
  if (!canAddPurchase.value) {
    ElMessage.warning('无新建采购单权限')
    return
  }
  if (!purchaseFormRef.value) return
  await purchaseFormRef.value.validate(async (valid) => {
    if (valid) {
      submitting.value = true
      try {
        await purchaseApi.create({
          supplierId: purchaseForm.value.supplierId,
          productId: purchaseForm.value.productId,
          quantity: purchaseForm.value.totalQuantity,
          unitPrice: purchaseForm.value.price,
          warehouseId: purchaseForm.value.warehouseId,
          remark: purchaseForm.value.remark
        })
        ElMessage.success('采购单创建成功')
        purchaseDialogVisible.value = false
        await Promise.all([
          dataStore.loadPurchaseOrders(),
          fetchPurchaseOrderTable(false),
          loadStats()
        ])
      } catch (error) {
        ElMessage.error(error.message || '创建失败')
      } finally {
        submitting.value = false
      }
    }
  })
}

// 提交供应商 - 调用后端API
const submitSupplier = async () => {
  if (supplierForm.value.id && !canEditSupplierEntity.value) {
    ElMessage.warning('无编辑供应商权限')
    return
  }
  if (!supplierForm.value.id && !canAddSupplier.value) {
    ElMessage.warning('无添加供应商权限')
    return
  }
  if (!supplierFormRef.value) return
  await supplierFormRef.value.validate(async (valid) => {
    if (valid) {
      submitting.value = true
      try {
        const supplierData = {
          name: supplierForm.value.name,
          address: supplierForm.value.address,
          contactPerson: supplierForm.value.contact,
          contactPhone: supplierForm.value.phone,
          email: supplierForm.value.email,
          industryIds: Array.isArray(supplierForm.value.industryIds) ? [...supplierForm.value.industryIds] : [],
          remark: supplierForm.value.remark
        }

        if (supplierForm.value.id) {
          // 编辑供应商
          await supplierApi.update(supplierForm.value.id, supplierData)
          ElMessage.success('供应商更新成功')
        } else {
          // 新建供应商
          await supplierApi.create(supplierData)
          ElMessage.success('供应商添加成功')
        }

        supplierDialogVisible.value = false
        await Promise.all([
          dataStore.loadSuppliers(),
          loadStats()
        ])
      } catch (error) {
        ElMessage.error(error.message || '操作失败')
      } finally {
        submitting.value = false
      }
    }
  })
}

// 其他操作
const viewPurchaseOrder = (row) => {
  router.push(`/purchase/order/${row.id}`)
}

const editPurchaseOrder = (row) => {
  if (!canEditPurchase.value) {
    ElMessage.warning('无编辑采购单权限')
    return
  }
  router.push(`/purchase/edit/${row.id}`)
}

// 删除采购单
const deletePurchaseOrder = async (row) => {
  if (!canEditPurchase.value) {
    ElMessage.warning('无删除采购单权限')
    return
  }
  try {
    await ElMessageBox.confirm(`确定要删除采购单 "${row.orderNo}" 吗？`, '删除确认', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await purchaseApi.delete(row.id)
    ElMessage.success('采购单已删除')
    await Promise.all([
      dataStore.loadPurchaseOrders(),
      fetchPurchaseOrderTable(false),
      loadStats()
    ])
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '删除失败')
    }
  }
}

// 点击采购单号跳转到采购单详情
const goToPurchaseOrder = (row) => {
  const purchaseNo = row.purchaseOrderNo || row.purchaseNo
  const order = purchaseOrders.value.find(o => o.orderNo === purchaseNo)
  if (order) {
    router.push(`/purchase/order/${order.id}`)
  } else {
    ElMessage.warning('未找到对应的采购单')
  }
}

const createPurchaseFromSupplier = async (s) => {
  if (!canAddPurchase.value) {
    ElMessage.warning('无新建采购单权限')
    return
  }
  await loadData()
  purchaseForm.value.supplierId = s.id
  purchaseDialogVisible.value = true
}

const viewSupplierDetail = (s) => {
  router.push(`/purchase/supplier/${s.id}`)
}

const editSupplier = (s) => {
  if (!canEditSupplierEntity.value) {
    ElMessage.warning('无编辑供应商权限')
    return
  }
  supplierForm.value = {
    id: s.id,
    name: s.name,
    industryIds: Array.isArray(s.industryIds) ? [...s.industryIds] : [],
    address: s.address,
    phone: s.phone,
    email: s.email,
    contact: s.contact,
    remark: s.remark
  }
  supplierDialogVisible.value = true
}

const deleteSupplier = async (s) => {
  if (!canEditSupplierEntity.value) {
    ElMessage.warning('无删除供应商权限')
    return
  }
  try {
    await ElMessageBox.confirm(
      `确定删除供应商「${s.name}」吗？若存在关联的采购单或入库记录，将无法删除。`,
      '删除确认',
      { confirmButtonText: '删除', cancelButtonText: '取消', type: 'warning' }
    )
    await supplierApi.delete(s.id)
    ElMessage.success('供应商已删除')
    await Promise.all([
      dataStore.loadSuppliers(),
      dataStore.loadPurchaseOrders(),
      dataStore.loadInboundRecords(),
      loadStats()
    ])
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '删除失败')
    }
  }
}

const printPurchaseOrder = () => {
  ElMessage.success('打印采购单功能已触发')
  orderDetailVisible.value = false
}

const syncPurchaseTabToPermissions = () => {
  if (activeTab.value === 'suppliers' && !canPurchaseSuppliersTab.value) {
    activeTab.value = 'orders'
  }
}

watch([canPurchaseSuppliersTab], () => syncPurchaseTabToPermissions())

// 初始化加载
onMounted(() => {
  loadData()
  syncPurchaseTabToPermissions()
})
</script>

<style lang="scss" scoped>
.purchase-page {
  .perm-hint {
    margin-bottom: 12px;
  }
  .stats-bar {
    display: flex;
    justify-content: space-around;
    align-items: center;
    padding: 16px 24px;
    margin-bottom: 16px;
    background: linear-gradient(135deg, #f5f7fa 0%, #fff 100%);
    border-radius: 12px;
    border: 1px solid #e4e7ed;
    .stat-item {
      display: flex;
      align-items: center;
      gap: 8px;
      .stat-label { font-size: 13px; color: #909399; }
      .stat-value { font-size: 18px; font-weight: 600; color: #303133; }
      .warning-text { color: #E6A23C; }
      .stat-extra { font-size: 12px; color: #E6A23C; }
    }
  }
  .stats-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 20px; margin-bottom: 24px; }

  .page-tabs {
    :deep(.el-tabs__header) { margin-bottom: 16px; }
  }

  .card-header {
    display: flex; justify-content: flex-end; align-items: center;
    .header-actions { display: flex; align-items: center; gap: 16px; }
  }

  .order-no { color: #E94560; font-family: monospace;
    &.success { color: #67C23A; }
    &.clickable { cursor: pointer; &:hover { text-decoration: underline; } }
  }
  .amount { font-weight: 600; color: #E94560; }
  .total-qty { font-weight: 600; color: #303133; }
  .pending-qty { color: #E6A23C; font-weight: 600; }
  .inbound-qty { color: #67C23A; font-weight: 600; }
  .overdue { color: #F56C6C; font-weight: 600; }

  .supplier-cell {
    display: flex; align-items: center; gap: 12px;
    .supplier-icon { width: 40px; height: 40px; border-radius: 8px; background: #F5F7FA; display: flex; align-items: center; justify-content: center; font-size: 20px; }
    .supplier-info { h4 { font-size: 14px; font-weight: 600; color: #303133; } p { font-size: 12px; color: #909399; } }
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

  // 商品图片详情样式
  .product-cell {
    display: flex;
    align-items: center;
    gap: 12px;

    .product-thumb {
      width: 40px;
      height: 40px;
      object-fit: cover;
      border-radius: 6px;
    }

    .product-icon {
      width: 40px;
      height: 40px;
      display: flex;
      align-items: center;
      justify-content: center;
      background: #F5F7FA;
      border-radius: 6px;
      font-size: 20px;
    }

    .product-name {
      font-size: 14px;
      color: #303133;
    }
  }

  .supplier-search-bar {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 20px;
  }

  .grid-empty {
    padding: 32px 0;
  }

  .suppliers-grid {
    display: grid; grid-template-columns: repeat(2, 1fr); gap: 20px; margin-bottom: 20px;
  }

  .supplier-card {
    :deep(.el-card__body) { padding: 24px; }
    .supplier-header { display: flex; align-items: center; gap: 16px; margin-bottom: 16px; }
    .supplier-avatar { width: 56px; height: 56px; border-radius: 14px; background: rgba(233, 69, 96, 0.15); display: flex; align-items: center; justify-content: center; font-size: 28px; }
    .supplier-info { flex: 1; h4 { font-size: 16px; font-weight: 600; color: #303133; margin-bottom: 4px; } .supplier-industry { font-size: 13px; color: #909399; margin-bottom: 4px; } p { font-size: 13px; color: #909399; } }
    .supplier-stats { display: grid; grid-template-columns: repeat(2, 1fr); gap: 16px; padding: 16px 0; border-top: 1px solid #E4E7ED; border-bottom: 1px solid #E4E7ED; margin-bottom: 16px; }
    .supplier-stat { text-align: center; .stat-label { display: block; font-size: 12px; color: #909399; margin-bottom: 6px; } .stat-value { font-size: 16px; font-weight: 700; color: #303133; } }
    .supplier-contact { margin-bottom: 16px; }
    .contact-item { display: flex; align-items: center; gap: 10px; padding: 8px 0; font-size: 13px; color: #606266; }
    .supplier-actions { display: flex; gap: 12px; }
  }

  // 查询结果统计
  .query-stats {
    display: flex;
    justify-content: flex-start;
    gap: 24px;
    padding: 12px 16px;
    background: #F5F7FA;
    border-radius: 8px;
    margin-bottom: 16px;
    .stat-item {
      display: flex;
      align-items: center;
      gap: 8px;
      .stat-label { font-size: 13px; color: #909399; }
      .stat-value { font-size: 15px; font-weight: 600; color: #303133; }
      .stat-value.amount { color: #E94560; }
      .stat-value.warning { color: #E6A23C; }
      .stat-value.success { color: #67C23A; }
      .stat-value.info { color: #909399; }
    }
  }

  .pagination-wrapper { display: flex; justify-content: flex-end; padding-top: 16px; }

  // 选中商品图片预览样式
  .selected-product-image {
    display: flex;
    align-items: center;
    justify-content: center;

    .product-preview {
      width: 80px;
      height: 80px;
      object-fit: cover;
      border-radius: 8px;
    }

    .product-preview-placeholder {
      width: 80px;
      height: 80px;
      background: #F5F7FA;
      border-radius: 8px;
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;

      .placeholder-icon {
        font-size: 32px;
      }

      .placeholder-text {
        font-size: 11px;
        color: #909399;
        margin-top: 4px;
      }
    }
  }
}

/* 新建采购单对话框 teleport 到 body，需与 .purchase-page 平级才能命中 */
.purchase-qty-readonly {
  width: 100%;
  :deep(.el-input__wrapper) {
    justify-content: flex-end;
  }
  :deep(.el-input__inner) {
    font-variant-numeric: tabular-nums;
    text-align: right;
    overflow-x: auto;
  }
}
</style>