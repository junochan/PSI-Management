<template>
  <div class="sales-page">
    <el-tabs v-model="activeTab" class="page-tabs">
      <!-- 销售订单 -->
      <el-tab-pane label="销售订单" name="orders">
        <el-card v-loading="salesOrdersTabLoading" element-loading-text="加载中...">
          <template #header>
            <div class="card-header">
              <div class="header-actions page-filter-bar">
                <el-select
                  v-model="filterCustomer"
                  v-load-more="{ popperClass: 'sales-filter-customer-dropdown', onLoadMore: loadMoreCustomerOptions, disabled: customerOptionsLoading || !customerOptionsHasMore }"
                  popper-class="sales-filter-customer-dropdown"
                  class="page-filter-ctl page-filter-ctl--wide"
                  placeholder="按客户筛选"
                  clearable
                  filterable
                  @visible-change="onFilterCustomerSelectVisibleChange"
                  @filter-method="onCustomerFilter"
                >
                  <el-option v-for="c in customerOptions" :key="c.id" :label="c.name" :value="c.id" />
                </el-select>
                <el-select
                  v-model="filterProduct"
                  v-load-more="{ popperClass: 'sales-filter-product-dropdown', onLoadMore: loadMoreProductOptions, disabled: productOptionsLoading || !productOptionsHasMore }"
                  popper-class="sales-filter-product-dropdown"
                  class="page-filter-ctl page-filter-ctl--wide"
                  placeholder="按商品筛选"
                  clearable
                  filterable
                  @visible-change="onFilterProductSelectVisibleChange"
                  @filter-method="onProductFilter"
                >
                  <el-option v-for="p in productOptions" :key="p.id" :label="p.name" :value="p.id" />
                </el-select>
                <el-select v-model="filterPayStatus" class="page-filter-ctl page-filter-ctl--narrow" placeholder="按付款状态筛选" clearable filterable>
                  <el-option label="待付款" value="unpaid" />
                  <el-option label="已付款" value="paid" />
                </el-select>
                <el-select v-model="filterOrderStatus" class="page-filter-ctl page-filter-ctl--narrow" placeholder="按订单状态筛选" clearable>
                  <el-option label="待处理" value="pending" />
                  <el-option label="已发货" value="shipped" />
                  <el-option label="已完成" value="completed" />
                  <el-option label="已取消" value="cancelled" />
                </el-select>
                <el-date-picker
                  v-model="filterCreateTimeRange"
                  class="page-filter-ctl page-filter-ctl--daterange"
                  type="daterange"
                  range-separator="至"
                  start-placeholder="下单开始"
                  end-placeholder="下单结束"
                  value-format="YYYY-MM-DD"
                />
                <el-input v-model="searchKeyword" class="page-filter-ctl page-filter-ctl--search" placeholder="搜索订单号、客户名称..." prefix-icon="Search" clearable />
                <el-button v-if="canAddSalesOrder" class="page-filter-primary" type="primary" @click="openSalesDialog"><el-icon><Plus /></el-icon>新建销售单</el-button>
              </div>
            </div>
          </template>
          <!-- 查询结果统计 -->
          <div class="query-stats">
            <div class="stat-item">
              <span class="stat-label">订单数</span>
              <span class="stat-value">{{ salesOrderTotal }}</span>
            </div>
            <div class="stat-item">
              <span class="stat-label">订单总额</span>
              <span class="stat-value amount">¥{{ formatAmountDisplay(orderListSummary.totalAmount) }}</span>
            </div>
            <div class="stat-item">
              <span class="stat-label">待付款</span>
              <span class="stat-value warning">¥{{ formatAmountDisplay(orderListSummary.unpaidAmount) }}</span>
            </div>
            <div class="stat-item">
              <span class="stat-label">已付款</span>
              <span class="stat-value success">¥{{ formatAmountDisplay(orderListSummary.paidAmount) }}</span>
            </div>
          </div>
          <el-table :data="salesOrderTableRows" empty-text="暂无数据" style="width: 100%" :max-height="500">
            <el-table-column label="单号" width="130">
              <template #default="{ row }"><span class="order-no">{{ row.orderNo }}</span></template>
            </el-table-column>
            <el-table-column label="客户" min-width="140" show-overflow-tooltip>
              <template #default="{ row }">
                <div class="customer-cell">
                  <div class="customer-icon">{{ row.customerType === '企业' ? '🏢' : '👤' }}</div>
                  <div class="customer-info">
                    <h4>{{ row.customerName || row.customer }}</h4>
                  </div>
                </div>
              </template>
            </el-table-column>
            <el-table-column label="商品" min-width="220" show-overflow-tooltip>
              <template #default="{ row }">
                <div class="product-cell-mini">
                  <ProductImageThumb
                    v-if="getRowProductImage(row)"
                    :src="getRowProductImage(row)"
                    :preview-src-list="getRowProductPreviewList(row)"
                    class="product-thumb-mini"
                    :width="32"
                    :height="32"
                    :radius="4"
                  />
                  <span v-else class="product-icon-mini">{{ getProductIcon(getProductName(row.productId, row.productName)) }}</span>
                  <span class="product-name-mini">{{ getProductName(row.productId, row.productName || row.product) }}</span>
                </div>
              </template>
            </el-table-column>
            <el-table-column label="数量" width="70" align="center">
              <template #default="{ row }">{{ row.quantity }}</template>
            </el-table-column>
            <el-table-column label="金额" min-width="116" align="right">
              <template #default="{ row }"><span class="amount">¥{{ formatAmountDisplay(row.amount ?? 0) }}</span></template>
            </el-table-column>
            <el-table-column label="状态" width="80" align="center">
              <template #default="{ row }"><el-tag :type="getStatusType(row.status)" effect="light" size="small">{{ formatOrderStatus(row.status) }}</el-tag></template>
            </el-table-column>
            <el-table-column label="付款" width="80" align="center">
              <template #default="{ row }"><el-tag :type="getPayStatusType(row.payStatus)" effect="light" size="small">{{ formatPayStatus(row.payStatus) }}</el-tag></template>
            </el-table-column>
            <el-table-column label="下单时间" prop="createTime" width="176" show-overflow-tooltip />
            <el-table-column label="操作" width="220" align="center">
              <template #default="{ row }">
                <el-button type="primary" link size="small" @click="viewSalesOrder(row)">详情</el-button>
                <el-button
                  v-if="canConfirmPayment && row.status === 'pending' && row.payStatus === 'unpaid'"
                  type="success"
                  link
                  size="small"
                  @click="handlePayment(row)"
                >确认付款</el-button>
                <el-button
                  v-if="canShipOrder && row.status === 'pending' && row.payStatus === 'paid'"
                  type="primary"
                  link
                  size="small"
                  @click="handleShip(row)"
                >发货</el-button>
                <el-button
                  v-if="canReceiveOrder && row.status === 'shipped'"
                  type="success"
                  link
                  size="small"
                  @click="handleReceived(row)"
                >确认收货</el-button>
              </template>
            </el-table-column>
          </el-table>
          <div class="pagination-wrapper">
            <el-pagination
              v-model:current-page="currentPage"
              v-model:page-size="pageSize"
              :total="salesOrderTotal"
              :page-sizes="[10, 20, 50, 100]"
              layout="total, sizes, prev, pager, next, jumper"
            />
          </div>
        </el-card>
      </el-tab-pane>

      <!-- 客户管理 -->
      <el-tab-pane label="客户管理" name="customers">
        <div class="customers-tab-wrap" v-loading="salesCustomersTabLoading" element-loading-text="加载中...">
        <div class="customer-search-bar">
          <el-input v-model="customerSearchKeyword" placeholder="搜索客户名称..." prefix-icon="Search" clearable style="width: 200px" />
          <el-button v-if="canManageCustomer" type="primary" @click="openCustomerDialog"><el-icon><Plus /></el-icon>添加客户</el-button>
        </div>
        <el-empty v-if="!salesCustomersTabLoading && !filteredCustomers.length" class="grid-empty" description="暂无数据" :image-size="80" />
        <div v-if="filteredCustomers.length" class="customers-grid">
          <el-card class="customer-card" v-for="c in filteredCustomers" :key="c.id">
            <div class="customer-header">
              <div class="customer-avatar">{{ c.type === '企业' ? '🏢' : '👤' }}</div>
              <div class="customer-info">
                <h4>{{ c.name }}</h4>
                <p>{{ c.type }}客户</p>
              </div>
            </div>
            <div class="customer-stats">
              <div class="customer-stat">
                <span class="stat-label">累计消费</span>
                <span class="stat-value">¥{{ formatAmountDisplay(c.totalAmount || 0) }}</span>
              </div>
              <div class="customer-stat">
                <span class="stat-label">订单数</span>
                <span class="stat-value">{{ c.totalOrders || 0 }}</span>
              </div>
              <div class="customer-stat">
                <span class="stat-label">最近购买</span>
                <span class="stat-value">{{ formatCustomerLastPurchase(c.lastOrderTime) }}</span>
              </div>
            </div>
            <div class="customer-contact">
              <div class="contact-item"><el-icon><Phone /></el-icon><span>{{ c.phone }}</span></div>
              <div class="contact-item"><el-icon><Location /></el-icon><span>{{ c.address }}</span></div>
            </div>
            <div class="customer-actions">
              <el-button type="primary" @click="viewCustomerDetail(c)">查看详情</el-button>
              <el-button v-if="canManageCustomer" @click="editCustomer(c)">编辑</el-button>
              <el-button v-if="canManageCustomer" type="danger" plain @click="deleteCustomerConfirm(c)">删除</el-button>
            </div>
          </el-card>
        </div>
        </div>
      </el-tab-pane>

      <!-- 售后服务 -->
      <el-tab-pane label="售后服务" name="aftersales">
        <el-card v-loading="salesAftersalesTabLoading" element-loading-text="加载中...">
          <template #header>
            <div class="card-header">
              <div class="header-actions page-filter-bar">
                <el-input
                  v-model="aftersalesSearchKeyword"
                  class="page-filter-ctl page-filter-ctl--search-compact"
                  placeholder="工单号/订单/客户"
                  prefix-icon="Search"
                  clearable
                />
                <el-select v-model="aftersalesFilterStatus" class="page-filter-ctl page-filter-ctl--narrow" placeholder="按状态筛选" clearable filterable>
                  <el-option label="待处理" value="待处理" />
                  <el-option label="处理中" value="处理中" />
                  <el-option label="已完成" value="已完成" />
                  <el-option label="已关闭" value="已关闭" />
                </el-select>
                <el-button v-if="canAftersalesCreate" class="page-filter-primary" type="primary" @click="openAftersalesDialog"><el-icon><Plus /></el-icon>创建售后工单</el-button>
              </div>
            </div>
          </template>
          <el-table :data="aftersalesTableRows" empty-text="暂无数据" style="width: 100%" :max-height="500">
            <el-table-column label="工单号" width="140">
              <template #default="{ row }"><span class="order-no warning">{{ row.orderNo }}</span></template>
            </el-table-column>
            <el-table-column label="关联订单" width="140" show-overflow-tooltip>
              <template #default="{ row }"><span class="order-no">{{ row.salesOrderNo }}</span></template>
            </el-table-column>
            <el-table-column label="客户" prop="customerName" width="100" show-overflow-tooltip />
            <el-table-column label="类型" width="90" align="center">
              <template #default="{ row }"><el-tag :type="getAftersalesTypeColor(row.type)" effect="light" size="small">{{ row.type }}</el-tag></template>
            </el-table-column>
            <el-table-column label="问题描述" prop="content" min-width="150" show-overflow-tooltip />
            <el-table-column label="期望处理" prop="expectHandle" width="100" show-overflow-tooltip />
            <el-table-column label="状态" width="90" align="center">
              <template #default="{ row }"><el-tag :type="getAftersalesStatusType(row.status)" effect="light" size="small">{{ formatAftersalesStatus(row.status) }}</el-tag></template>
            </el-table-column>
            <el-table-column label="创建时间" prop="createTime" width="176" show-overflow-tooltip />
            <el-table-column label="操作" width="80" align="center">
              <template #default="{ row }">
                <el-button type="primary" link size="small" @click="viewAftersalesDetail(row)">详情</el-button>
              </template>
            </el-table-column>
          </el-table>
          <div class="pagination-wrapper">
            <el-pagination
              v-model:current-page="aftersalesCurrentPage"
              v-model:page-size="aftersalesPageSize"
              :total="aftersalesTableTotal"
              :page-sizes="[10, 20, 50, 100]"
              layout="total, sizes, prev, pager, next, jumper"
            />
          </div>
        </el-card>
      </el-tab-pane>
    </el-tabs>

    <!-- 新建销售单对话框 -->
    <el-dialog v-model="salesDialogVisible" title="新建销售单" width="700px" destroy-on-close>
      <el-form ref="salesFormRef" :model="salesForm" :rules="salesRules" label-width="100px">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="客户" prop="customerId">
              <el-select
                v-model="salesForm.customerId"
                v-load-more="{ popperClass: 'sales-dialog-customer-dropdown', onLoadMore: loadMoreCustomerOptions, disabled: customerOptionsLoading || !customerOptionsHasMore }"
                popper-class="sales-dialog-customer-dropdown"
                placeholder="请选择客户"
                style="width: 100%"
                filterable
                @visible-change="onSalesDialogCustomerSelectVisibleChange"
                @filter-method="onCustomerFilter"
              >
                <el-option v-for="c in customerOptions" :key="c.id" :label="c.name" :value="c.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="订单日期">
              <el-date-picker v-model="salesForm.date" type="date" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="商品选择" prop="productId">
              <el-select
                v-model="salesForm.productId"
                v-load-more="{ popperClass: 'sales-dialog-product-dropdown', onLoadMore: loadMoreProductOptions, disabled: productOptionsLoading || !productOptionsHasMore }"
                popper-class="sales-dialog-product-dropdown"
                placeholder="请选择商品"
                style="width: 100%"
                filterable
                @change="onSalesProductChange"
                @visible-change="onSalesDialogProductSelectVisibleChange"
                @filter-method="onProductFilter"
              >
                <el-option v-for="p in selectableProductOptions" :key="p.id" :label="p.name" :value="p.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="商品图片">
              <div class="selected-product-image">
                <ProductImageThumb
                  v-if="selectedProductImage"
                  :src="selectedProductImage"
                  :preview-src-list="salesSelectedProductPreviewList"
                  class="product-preview"
                  :width="80"
                  :height="80"
                  :radius="8"
                />
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
            <el-form-item label="销售数量" prop="quantity">
              <el-input-number
                v-model="salesForm.quantity"
                :min="1"
                :max="salesQtyMax"
                :disabled="salesStockLoading || (salesProductStockRemain !== null && salesProductStockRemain <= 0)"
                style="width: 100%"
              />
              <div v-if="salesForm.productId" class="sales-stock-tip">
                <template v-if="salesStockLoading">正在查询库存…</template>
                <template v-else-if="salesProductStockRemain !== null">
                  库存剩余：<strong>{{ salesProductStockRemain }}</strong> 件
                  <span v-if="salesProductStockRemain <= 0" class="sales-stock-warn">（当前无可用库存）</span>
                </template>
              </div>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="单价">
              <el-input :value="getSalePrice()" disabled />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="付款方式">
              <el-select v-model="salesForm.payMethod" style="width: 100%">
                <el-option label="银行转账" value="银行转账" />
                <el-option label="支付宝" value="支付宝" />
                <el-option label="微信支付" value="微信支付" />
                <el-option label="现金" value="现金" />
                <el-option label="月结" value="月结" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="发票类型">
              <el-select v-model="salesForm.invoiceType" style="width: 100%">
                <el-option label="不需要发票" value="不需要发票" />
                <el-option label="普通发票" value="普通发票" />
                <el-option label="增值税专用发票" value="增值税专用发票" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="备注" prop="remark">
          <el-input
            v-model="salesForm.remark"
            type="textarea"
            :rows="3"
            :maxlength="SALES_ORDER_REMARK_MAX_LENGTH"
            show-word-limit
            placeholder="输入订单备注"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="salesDialogVisible = false">取消</el-button>
        <el-button
          type="primary"
          :disabled="salesStockLoading || (salesProductStockRemain !== null && salesProductStockRemain <= 0)"
          @click="submitSales"
        >确认创建</el-button>
      </template>
    </el-dialog>

    <!-- 添加客户对话框 -->
    <el-dialog v-model="customerDialogVisible" :title="customerForm.id ? '编辑客户' : '添加客户'" width="700px" destroy-on-close>
      <el-form ref="customerFormRef" :model="customerForm" :rules="customerRules" label-width="100px">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="客户名称" prop="name">
              <el-input v-model="customerForm.name" :maxlength="100" show-word-limit placeholder="输入客户名称" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="客户类型">
              <el-select v-model="customerForm.type" style="width: 100%" filterable>
                <el-option label="个人客户" value="个人" />
                <el-option label="企业客户" value="企业" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="收货地址" prop="address">
          <el-input v-model="customerForm.address" :maxlength="200" show-word-limit placeholder="输入详细收货地址" />
        </el-form-item>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="联系人" prop="contact">
              <el-input v-model="customerForm.contact" :maxlength="50" show-word-limit placeholder="输入联系人姓名" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="联系电话" prop="phone">
              <el-input v-model="customerForm.phone" :maxlength="20" show-word-limit placeholder="输入联系电话" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="customerForm.email" :maxlength="100" show-word-limit placeholder="输入邮箱地址" />
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="customerForm.remark" type="textarea" :rows="3" :maxlength="500" show-word-limit placeholder="输入备注信息" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="customerDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitCustomer">{{ customerForm.id ? '保存修改' : '确认添加' }}</el-button>
      </template>
    </el-dialog>

    <!-- 创建售后工单对话框 -->
    <el-dialog v-model="aftersalesDialogVisible" title="创建售后工单" width="700px" destroy-on-close>
      <el-form ref="aftersalesFormRef" :model="aftersalesForm" :rules="aftersalesRules" label-width="100px">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="关联订单" prop="salesOrderId">
              <el-select
                v-model="aftersalesForm.salesOrderId"
                v-load-more="{ popperClass: 'sales-aftersales-order-dropdown', onLoadMore: loadMoreSalesOrderOptions, disabled: salesOrderOptionsLoading || !salesOrderOptionsHasMore }"
                popper-class="sales-aftersales-order-dropdown"
                placeholder="请选择订单"
                style="width: 100%"
                filterable
                @visible-change="onSalesOrderSelectVisibleChange"
                @filter-method="onSalesOrderFilter"
              >
                <el-option v-for="o in salesOrderOptions" :key="o.id" :label="`${o.orderNo} - ${o.customerName || o.customer}`" :value="o.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="问题类型">
              <el-select v-model="aftersalesForm.type" style="width: 100%">
                <el-option label="质量问题" value="质量问题" />
                <el-option label="退换货" value="退换货" />
                <el-option label="物流问题" value="物流问题" />
                <el-option label="发票问题" value="发票问题" />
                <el-option label="其他" value="其他" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="问题描述" prop="content">
          <el-input
            v-model="aftersalesForm.content"
            type="textarea"
            :rows="3"
            maxlength="500"
            show-word-limit
            placeholder="请详细描述问题（最多500字）"
          />
        </el-form-item>
        <el-form-item label="期望处理">
          <el-select v-model="aftersalesForm.expect" style="width: 100%">
            <el-option label="退货退款" value="退货退款" />
            <el-option label="换货" value="换货" />
            <el-option label="维修" value="维修" />
            <el-option label="补偿" value="补偿" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="aftersalesDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitAftersales">确认创建</el-button>
      </template>
    </el-dialog>

    <!-- 售后详情对话框 -->
    <el-dialog v-model="aftersalesDetailVisible" title="售后工单详情" width="600px" destroy-on-close class="aftersales-detail-dialog">
      <el-descriptions :column="2" border class="aftersales-detail-descriptions">
        <el-descriptions-item label="工单号"><span class="order-no warning">{{ currentAftersales?.orderNo }}</span></el-descriptions-item>
        <el-descriptions-item label="关联订单"><span class="order-no">{{ currentAftersales?.salesOrderNo }}</span></el-descriptions-item>
        <el-descriptions-item label="客户">{{ currentAftersales?.customerName }}</el-descriptions-item>
        <el-descriptions-item label="问题类型">
          <el-tag :type="getAftersalesTypeColor(currentAftersales?.type)" effect="light" size="small">{{ currentAftersales?.type }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="问题描述" :span="2">
          <el-tooltip
            :content="currentAftersales?.content || '-'"
            placement="top-start"
            effect="dark"
            :show-after="200"
            popper-class="ep-table-overflow-tooltip aftersales-detail-tooltip"
          >
            <div class="aftersales-detail-text">{{ currentAftersales?.content || '-' }}</div>
          </el-tooltip>
        </el-descriptions-item>
        <el-descriptions-item label="期望处理">{{ currentAftersales?.expectHandle || '-' }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="getAftersalesStatusType(currentAftersales?.status)" effect="light">{{ formatAftersalesStatus(currentAftersales?.status) }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ currentAftersales?.createTime }}</el-descriptions-item>
        <el-descriptions-item label="处理人">{{ currentAftersales?.handlerName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="处理结果" :span="2">{{ currentAftersales?.handleResult || '-' }}</el-descriptions-item>
        <el-descriptions-item label="退款金额" :span="2" v-if="currentAftersales?.refundAmount">
          <span class="amount">¥{{ formatAmountDisplay(currentAftersales?.refundAmount) }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="处理时间">{{ currentAftersales?.handleTime || '-' }}</el-descriptions-item>
        <el-descriptions-item label="备注">
          <el-tooltip
            :content="currentAftersales?.remark || '-'"
            placement="top-start"
            effect="dark"
            :show-after="200"
            popper-class="ep-table-overflow-tooltip aftersales-detail-tooltip"
          >
            <div class="aftersales-detail-text">{{ currentAftersales?.remark || '-' }}</div>
          </el-tooltip>
        </el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <el-button @click="aftersalesDetailVisible = false">关闭</el-button>
        <el-button type="primary" @click="openHandleAftersales(currentAftersales)" v-if="formatAftersalesStatus(currentAftersales?.status) === '待处理'">处理工单</el-button>
        <el-button type="success" @click="closeAftersalesOrder(currentAftersales)" v-if="formatAftersalesStatus(currentAftersales?.status) === '待处理' || formatAftersalesStatus(currentAftersales?.status) === '处理中'">关闭工单</el-button>
      </template>
    </el-dialog>

    <!-- 处理售后工单对话框 -->
    <el-dialog v-model="handleAftersalesVisible" title="处理售后工单" width="500px" destroy-on-close>
      <el-form ref="handleAftersalesFormRef" :model="handleAftersalesForm" :rules="handleAftersalesRules" label-width="100px">
        <el-form-item label="处理结果" prop="handleResult">
          <el-select v-model="handleAftersalesForm.handleResult" placeholder="请选择处理结果" style="width: 100%">
            <el-option label="同意退货退款" value="同意退货退款" />
            <el-option label="同意换货" value="同意换货" />
            <el-option label="同意维修" value="同意维修" />
            <el-option label="同意补偿" value="同意补偿" />
            <el-option label="拒绝申请" value="拒绝申请" />
            <el-option label="部分退款" value="部分退款" />
          </el-select>
        </el-form-item>
        <el-form-item label="退款金额" v-if="handleAftersalesForm.handleResult.includes('退款')">
          <el-input-number v-model="handleAftersalesForm.refundAmount" :min="0" :precision="2" style="width: 100%" />
        </el-form-item>
        <el-form-item label="处理备注" prop="remark">
          <el-input
            v-model="handleAftersalesForm.remark"
            type="textarea"
            :rows="3"
            maxlength="500"
            show-word-limit
            placeholder="输入处理备注（最多500字）"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="handleAftersalesVisible = false">取消</el-button>
        <el-button type="primary" @click="submitHandleAftersales">确认处理</el-button>
      </template>
    </el-dialog>

    <!-- 订单详情对话框 -->
    <el-dialog v-model="orderDetailVisible" title="销售单详情" width="500px">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="订单编号">{{ currentOrder?.orderNo }}</el-descriptions-item>
        <el-descriptions-item label="客户">{{ currentOrder?.customer }}</el-descriptions-item>
        <el-descriptions-item label="商品">{{ currentOrder?.product }}</el-descriptions-item>
        <el-descriptions-item label="数量">{{ currentOrder?.quantity }}</el-descriptions-item>
        <el-descriptions-item label="金额">¥{{ formatAmountDisplay(currentOrder?.amount ?? 0) }}</el-descriptions-item>
        <el-descriptions-item label="订单状态"><el-tag :type="getStatusType(currentOrder?.status)">{{ formatOrderStatus(currentOrder?.status) }}</el-tag></el-descriptions-item>
        <el-descriptions-item label="付款状态"><el-tag :type="getPayStatusType(currentOrder?.payStatus)">{{ formatPayStatus(currentOrder?.payStatus) }}</el-tag></el-descriptions-item>
        <el-descriptions-item label="下单时间">{{ currentOrder?.createTime }}</el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <el-button @click="orderDetailVisible = false">关闭</el-button>
        <el-button type="primary" @click="printOrder">打印订单</el-button>
      </template>
    </el-dialog>

    <!-- 发货对话框 -->
    <el-dialog v-model="shipDialogVisible" title="填写发货信息" width="700px" destroy-on-close>
      <div v-loading="shipDialogLoading" element-loading-text="正在加载库存与客户信息...">
      <el-form ref="shipFormRef" :model="shipForm" :rules="shipRules" label-width="120px">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="订单编号">
              <el-input :value="shipForm.orderNo" disabled />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="客户名称">
              <el-input :value="shipForm.customer" disabled />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="订单商品">
              <el-input :value="`${shipForm.productName} (订单数量: ${shipForm.orderQuantity})`" disabled />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="当前库存">
              <div class="stock-info-display">
                <span v-for="s in shipForm.stockInfo" :key="s.warehouseId" class="stock-item" :class="{ 'low-stock': s.stock < shipForm.orderQuantity }">
                  {{ getWarehouseName(s.warehouseId) || s.warehouseName }}: {{ s.stock }}
                </span>
              </div>
            </el-form-item>
          </el-col>
        </el-row>
        <el-divider content-position="left">物流信息</el-divider>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="物流公司" prop="logisticsCompany">
              <el-select v-model="shipForm.logisticsCompany" placeholder="请选择物流公司" style="width: 100%" filterable>
                <el-option label="顺丰速运" value="顺丰速运" />
                <el-option label="京东物流" value="京东物流" />
                <el-option label="中通快递" value="中通快递" />
                <el-option label="圆通速递" value="圆通速递" />
                <el-option label="韵达快递" value="韵达快递" />
                <el-option label="申通快递" value="申通快递" />
                <el-option label="邮政EMS" value="邮政EMS" />
                <el-option label="德邦物流" value="德邦物流" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="物流单号" prop="trackingNo">
              <el-input v-model="shipForm.trackingNo" placeholder="输入物流单号" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="发货仓库">
              <el-select v-model="shipForm.warehouseId" placeholder="请选择发货仓库" style="width: 100%" filterable @change="onWarehouseChange">
                <el-option v-for="w in shipForm.stockInfo" :key="w.warehouseId" :label="getWarehouseName(w.warehouseId) || w.warehouseName" :value="w.warehouseId">
                  <div style="display: flex; justify-content: space-between;">
                    <span>{{ getWarehouseName(w.warehouseId) || w.warehouseName }}</span>
                    <span :style="{ color: w.stock < shipForm.orderQuantity ? '#E6A23C' : '#67C23A' }">库存: {{ w.stock }}</span>
                  </div>
                </el-option>
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="预计到达">
              <el-date-picker v-model="shipForm.estimatedDate" type="date" placeholder="选择预计到达日期" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-divider content-position="left">收货地址</el-divider>
        <el-form-item label="收货人" prop="receiverName">
          <el-input v-model="shipForm.receiverName" placeholder="输入收货人姓名" />
        </el-form-item>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="联系电话" prop="receiverPhone">
              <el-input v-model="shipForm.receiverPhone" placeholder="输入收货人电话" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="备用电话">
              <el-input v-model="shipForm.receiverPhone2" placeholder="输入备用电话（可选）" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="收货地址" prop="receiverAddress">
          <el-input v-model="shipForm.receiverAddress" placeholder="输入详细收货地址" />
        </el-form-item>
        <el-form-item label="发货备注" prop="remark">
          <el-input
            v-model="shipForm.remark"
            type="textarea"
            :rows="3"
            :maxlength="500"
            show-word-limit
            placeholder="输入发货备注信息（最多500字）"
          />
        </el-form-item>
      </el-form>
      </div>
      <template #footer>
        <el-button @click="shipDialogVisible = false" :disabled="shipDialogLoading">取消</el-button>
        <el-button type="primary" :disabled="shipDialogLoading" @click="submitShip">确认发货</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useDataStore } from '@/stores/data'
import { useUserStore } from '@/stores/user'
import { salesApi, customerApi, aftersalesApi, inventoryApi, productApi } from '@/api'
import dayjs from 'dayjs'
import relativeTime from 'dayjs/plugin/relativeTime'
import 'dayjs/locale/zh-cn'
import ProductImageThumb from '@/components/ProductImageThumb.vue'
import { firstProductImageUrl, parseProductImageUrls, productRowPreviewUrls } from '@/utils/productImages'
import { formatAmountDisplay } from '@/utils/moneyFormat'
import { isProductSelectableForOrder } from '@/utils/productStatus'

dayjs.extend(relativeTime)
dayjs.locale('zh-cn')

const router = useRouter()
const route = useRoute()
const dataStore = useDataStore()
const userStore = useUserStore()

const canAddSalesOrder = computed(() => userStore.hasPermission('sales:add'))
/** 确认付款：更新订单支付状态 */
const canConfirmPayment = computed(
  () => userStore.hasPermission('sales:payment') || userStore.hasPermission('sales:add')
)
const canShipOrder = computed(() => userStore.hasPermission('sales:ship'))
const canReceiveOrder = computed(
  () => userStore.hasPermission('sales:receive') || userStore.hasPermission('sales:ship')
)
const canManageCustomer = computed(() => userStore.hasPermission('sales:customer'))

/** 客户卡片「最近购买」：来自后端 lastOrderTime（与累计订单/金额同步维护） */
const formatCustomerLastPurchase = (t) => {
  if (t == null || t === '') return '—'
  const d = dayjs(t)
  return d.isValid() ? d.fromNow() : '—'
}
const canAftersalesCreate = computed(() => userStore.hasPermission('sales:aftersales'))

const activeTab = ref('orders')
const searchKeyword = ref('')
const filterCustomer = ref(null)
const filterProduct = ref(null)
const filterPayStatus = ref(null)
const filterOrderStatus = ref(null)
const filterCreateTimeRange = ref(null)
const currentPage = ref(1)
const pageSize = ref(10)
const customerSearchKeyword = ref('')
const aftersalesSearchKeyword = ref('')
const aftersalesFilterStatus = ref(null)
const aftersalesCurrentPage = ref(1)
const aftersalesPageSize = ref(10)
const salesDialogVisible = ref(false)
const customerDialogVisible = ref(false)
const aftersalesDialogVisible = ref(false)
const aftersalesDetailVisible = ref(false)
const handleAftersalesVisible = ref(false)
const orderDetailVisible = ref(false)
const shipDialogVisible = ref(false)
/** 发货弹窗已打开但库存/客户回填尚未完成时为 true，避免点击后长时间无反馈 */
const shipDialogLoading = ref(false)
const salesFormRef = ref()
const customerFormRef = ref()
const aftersalesFormRef = ref()
const handleAftersalesFormRef = ref()
const currentAftersales = ref(null)
const shipFormRef = ref()
const currentOrder = ref(null)
const loading = ref(false)
const salesOrdersTabLoading = computed(() => activeTab.value === 'orders' && loading.value)
const salesCustomersTabLoading = computed(
  () => activeTab.value === 'customers' && (loading.value || dataStore.customersLoading)
)
const salesAftersalesTabLoading = computed(() => activeTab.value === 'aftersales' && loading.value)

// 统计数据
const salesStats = ref({
  monthAmount: 0,
  lastMonthAmount: 0,
  amountChangePercent: 0,
  monthOrderCount: 0,
  lastMonthOrderCount: 0,
  orderChangePercent: 0,
  activeCustomerCount: 0,
  lastMonthActiveCustomerCount: 0,
  customerChange: 0,
  pendingAftersalesCount: 0
})

// 数据从后端获取（订单下拉改为按需加载）
const products = computed(() => dataStore.products || [])
const customers = computed(() => dataStore.customers || [])
const filteredCustomers = computed(() => {
  let result = customers.value
  if (customerSearchKeyword.value) {
    result = result.filter(c =>
      (c.name && c.name.includes(customerSearchKeyword.value))
    )
  }
  return result
})
const inventoryData = computed(() => dataStore.inventoryData || [])
const warehouses = computed(() => dataStore.warehouses || [])

/** 筛选/弹窗内远程下拉：首屏 10 条，滚动触底继续分页（与库存页一致） */
const FILTER_DROPDOWN_PAGE_SIZE = 10
const customerOptions = ref([])
const customerOptionsPage = ref(0)
const customerOptionsTotal = ref(0)
const customerOptionsKeyword = ref('')
const customerOptionsLoading = ref(false)
const customerOptionsHasMore = computed(() => customerOptions.value.length < customerOptionsTotal.value)

const productOptions = ref([])
const productOptionsPage = ref(0)
const productOptionsTotal = ref(0)
const productOptionsKeyword = ref('')
const productOptionsLoading = ref(false)
const productOptionsHasMore = computed(() => productOptions.value.length < productOptionsTotal.value)

const salesOrderOptions = ref([])
const salesOrderOptionsPage = ref(0)
const salesOrderOptionsTotal = ref(0)
const salesOrderOptionsKeyword = ref('')
const salesOrderOptionsLoading = ref(false)
const salesOrderOptionsHasMore = computed(() => salesOrderOptions.value.length < salesOrderOptionsTotal.value)

const mergeOptionsById = (base, extra) => {
  const map = new Map()
  ;(base || []).forEach((item) => {
    if (item?.id != null) map.set(item.id, item)
  })
  ;(extra || []).forEach((item) => {
    if (item?.id != null) map.set(item.id, item)
  })
  return Array.from(map.values())
}

const loadMoreCustomerOptions = async ({ reset = false, keyword = null } = {}) => {
  if (customerOptionsLoading.value) return
  if (reset) {
    customerOptions.value = []
    customerOptionsPage.value = 0
    customerOptionsTotal.value = 0
    customerOptionsKeyword.value = keyword ?? ''
  } else if (!customerOptionsHasMore.value && customerOptionsPage.value > 0) {
    return
  }
  customerOptionsLoading.value = true
  try {
    const nextPage = customerOptionsPage.value + 1
    const res = await customerApi.list({
      page: nextPage,
      size: FILTER_DROPDOWN_PAGE_SIZE,
      keyword: customerOptionsKeyword.value || undefined
    })
    const rows = res?.list || []
    customerOptions.value = mergeOptionsById(customerOptions.value, rows)
    customerOptionsPage.value = nextPage
    customerOptionsTotal.value = Number(res?.total) || customerOptions.value.length
    if (rows.length < FILTER_DROPDOWN_PAGE_SIZE) {
      customerOptionsTotal.value = customerOptions.value.length
    }
  } finally {
    customerOptionsLoading.value = false
  }
}

const loadMoreProductOptions = async ({ reset = false, keyword = null } = {}) => {
  if (productOptionsLoading.value) return
  if (reset) {
    productOptions.value = []
    productOptionsPage.value = 0
    productOptionsTotal.value = 0
    productOptionsKeyword.value = keyword ?? ''
  } else if (!productOptionsHasMore.value && productOptionsPage.value > 0) {
    return
  }
  productOptionsLoading.value = true
  try {
    const nextPage = productOptionsPage.value + 1
    const res = await productApi.list({
      page: nextPage,
      size: FILTER_DROPDOWN_PAGE_SIZE,
      keyword: productOptionsKeyword.value || undefined
    })
    const rows = res?.list || []
    productOptions.value = mergeOptionsById(productOptions.value, rows)
    productOptionsPage.value = nextPage
    productOptionsTotal.value = Number(res?.total) || productOptions.value.length
    if (rows.length < FILTER_DROPDOWN_PAGE_SIZE) {
      productOptionsTotal.value = productOptions.value.length
    }
  } finally {
    productOptionsLoading.value = false
  }
}

const loadMoreSalesOrderOptions = async ({ reset = false, keyword = null } = {}) => {
  if (salesOrderOptionsLoading.value) return
  if (reset) {
    salesOrderOptions.value = []
    salesOrderOptionsPage.value = 0
    salesOrderOptionsTotal.value = 0
    salesOrderOptionsKeyword.value = keyword ?? ''
  } else if (!salesOrderOptionsHasMore.value && salesOrderOptionsPage.value > 0) {
    return
  }
  salesOrderOptionsLoading.value = true
  try {
    const nextPage = salesOrderOptionsPage.value + 1
    const res = await salesApi.list({
      page: nextPage,
      size: FILTER_DROPDOWN_PAGE_SIZE,
      keyword: salesOrderOptionsKeyword.value || undefined
    })
    const rows = res?.list || []
    salesOrderOptions.value = mergeOptionsById(salesOrderOptions.value, rows)
    salesOrderOptionsPage.value = nextPage
    salesOrderOptionsTotal.value = Number(res?.total) || salesOrderOptions.value.length
    if (rows.length < FILTER_DROPDOWN_PAGE_SIZE) {
      salesOrderOptionsTotal.value = salesOrderOptions.value.length
    }
  } finally {
    salesOrderOptionsLoading.value = false
  }
}

const mergeCustomerById = async (customerId) => {
  if (customerId == null || customerId === '') return
  const cid = Number(customerId)
  if (customerOptions.value.some((c) => Number(c.id) === cid)) return
  try {
    const c = await customerApi.get(customerId)
    if (c) customerOptions.value = mergeOptionsById(customerOptions.value, [c])
  } catch {
    /* ignore */
  }
}

const mergeProductById = async (productId) => {
  if (productId == null || productId === '') return
  const pid = Number(productId)
  if (productOptions.value.some((p) => Number(p.id) === pid)) return
  try {
    const p = await productApi.get(productId)
    if (p) productOptions.value = mergeOptionsById(productOptions.value, [p])
  } catch {
    /* ignore */
  }
}

const mergeSalesOrderById = async (orderId) => {
  if (orderId == null || orderId === '') return
  const oid = Number(orderId)
  if (salesOrderOptions.value.some((o) => Number(o.id) === oid)) return
  try {
    const o = await salesApi.get(orderId)
    if (o) salesOrderOptions.value = mergeOptionsById(salesOrderOptions.value, [o])
  } catch {
    /* ignore */
  }
}

const onFilterCustomerSelectVisibleChange = async (visible) => {
  if (!visible) return
  if (customerOptions.value.length === 0) {
    await loadMoreCustomerOptions({ reset: true, keyword: '' })
  }
  if (filterCustomer.value != null) await mergeCustomerById(filterCustomer.value)
}

const onFilterProductSelectVisibleChange = async (visible) => {
  if (!visible) return
  if (productOptions.value.length === 0) {
    await loadMoreProductOptions({ reset: true, keyword: '' })
  }
  if (filterProduct.value != null) await mergeProductById(filterProduct.value)
}

const onSalesDialogCustomerSelectVisibleChange = async (visible) => {
  if (!visible) return
  if (customerOptions.value.length === 0) {
    await loadMoreCustomerOptions({ reset: true, keyword: '' })
  }
  if (salesForm.value.customerId != null) await mergeCustomerById(salesForm.value.customerId)
}

const onSalesDialogProductSelectVisibleChange = async (visible) => {
  if (!visible) return
  if (productOptions.value.length === 0) {
    await loadMoreProductOptions({ reset: true, keyword: '' })
  }
  if (salesForm.value.productId != null) await mergeProductById(salesForm.value.productId)
}

const onSalesOrderSelectVisibleChange = async (visible) => {
  if (!visible) return
  if (salesOrderOptions.value.length === 0) {
    await loadMoreSalesOrderOptions({ reset: true, keyword: '' })
  }
  if (aftersalesForm.value.salesOrderId != null) {
    await mergeSalesOrderById(aftersalesForm.value.salesOrderId)
  }
}

const onCustomerFilter = (keyword) => {
  loadMoreCustomerOptions({ reset: true, keyword: keyword || '' })
}

const onProductFilter = (keyword) => {
  loadMoreProductOptions({ reset: true, keyword: keyword || '' })
}

const onSalesOrderFilter = (keyword) => {
  loadMoreSalesOrderOptions({ reset: true, keyword: keyword || '' })
}

const selectableProductOptions = computed(() =>
  productOptions.value.filter(isProductSelectableForOrder)
)
const productLookupList = computed(() => mergeOptionsById(products.value, productOptions.value))

// 动态获取仓库名称 - 从仓库列表中根据warehouseId查找
const getWarehouseName = (warehouseId) => {
  const warehouse = warehouses.value.find(w => w.id === warehouseId)
  return warehouse?.name || ''
}

/** 销售订单主表：服务端分页 */
const salesOrderTableRows = ref([])
const salesOrderTotal = ref(0)
const orderListSummary = ref({
  totalAmount: 0,
  unpaidAmount: 0,
  paidAmount: 0
})

const fetchSalesOrderTable = async (showLoading = true) => {
  if (showLoading) loading.value = true
  try {
    const res = await salesApi.list({
      page: currentPage.value,
      size: pageSize.value,
      keyword: searchKeyword.value || undefined,
      customerId: filterCustomer.value || undefined,
      productId: filterProduct.value || undefined,
      payStatus: filterPayStatus.value || undefined,
      salesOrderStatus: filterOrderStatus.value || undefined,
      createTimeStart: filterCreateTimeRange.value?.[0],
      createTimeEnd: filterCreateTimeRange.value?.[1]
    })
    salesOrderTableRows.value = res.list || []
    salesOrderTotal.value = Number(res.total) || 0
    const s = res.summary || {}
    orderListSummary.value = {
      totalAmount: Number(s.totalAmount) || 0,
      unpaidAmount: Number(s.unpaidAmount) || 0,
      paidAmount: Number(s.paidAmount) || 0
    }
  } catch (e) {
    ElMessage.error(e.message || '加载销售订单失败')
    salesOrderTableRows.value = []
    salesOrderTotal.value = 0
    orderListSummary.value = { totalAmount: 0, unpaidAmount: 0, paidAmount: 0 }
  } finally {
    if (showLoading) loading.value = false
  }
}

watch(
  [filterCustomer, filterProduct, filterPayStatus, filterOrderStatus, filterCreateTimeRange, searchKeyword],
  () => {
    currentPage.value = 1
    fetchSalesOrderTable()
  }
)

watch([currentPage, pageSize], () => {
  fetchSalesOrderTable()
})

/** 售后工单：服务端分页 */
const aftersalesTableRows = ref([])
const aftersalesTableTotal = ref(0)

const fetchAftersalesTable = async (showLoading = true) => {
  if (showLoading) loading.value = true
  try {
    const res = await aftersalesApi.list({
      page: aftersalesCurrentPage.value,
      size: aftersalesPageSize.value,
      keyword: aftersalesSearchKeyword.value || undefined,
      aftersalesStatus: aftersalesFilterStatus.value || undefined
    })
    aftersalesTableRows.value = res.list || []
    aftersalesTableTotal.value = Number(res.total) || 0
  } catch (e) {
    ElMessage.error(e.message || '加载售后工单失败')
    aftersalesTableRows.value = []
    aftersalesTableTotal.value = 0
  } finally {
    if (showLoading) loading.value = false
  }
}

watch([aftersalesSearchKeyword, aftersalesFilterStatus], () => {
  aftersalesCurrentPage.value = 1
  fetchAftersalesTable()
})

watch([aftersalesCurrentPage, aftersalesPageSize], () => {
  fetchAftersalesTable()
})

// 加载统计数据
const loadStats = async () => {
  try {
    const stats = await salesApi.stats()
    salesStats.value = stats || {
      monthAmount: 0,
      lastMonthAmount: 0,
      amountChangePercent: 0,
      monthOrderCount: 0,
      lastMonthOrderCount: 0,
      orderChangePercent: 0,
      activeCustomerCount: 0,
      lastMonthActiveCustomerCount: 0,
      customerChange: 0,
      pendingAftersalesCount: 0
    }
  } catch (error) {
    console.error('加载销售统计数据失败:', error)
  }
}

// 加载数据
const loadData = async () => {
  loading.value = true
  try {
    await Promise.all([
      dataStore.loadCustomers(),
      dataStore.loadAftersales(),
      dataStore.loadWarehouses({ forceOptions: true })
    ])
    await loadStats()
    await fetchSalesOrderTable(false)
    await fetchAftersalesTable(false)
  } finally {
    loading.value = false
  }
}

const loadSalesDialogOptions = async () => {
  await Promise.all([
    loadMoreCustomerOptions({ reset: true, keyword: '' }),
    loadMoreProductOptions({ reset: true, keyword: '' })
  ])
}

const loadAftersalesDialogOptions = async () => {
  await loadMoreSalesOrderOptions({ reset: true, keyword: '' })
}

const SALES_ORDER_REMARK_MAX_LENGTH = 500

const salesForm = ref({ customerId: null, date: new Date(), productId: null, quantity: 1, payMethod: '银行转账', invoiceType: '不需要发票', remark: '' })
/** 选中商品在各仓库存合计；null 表示未选商品或尚未拉取 */
const salesProductStockRemain = ref(null)
const salesStockLoading = ref(false)

const salesQtyMax = computed(() => {
  if (salesProductStockRemain.value == null || salesProductStockRemain.value <= 0) return undefined
  return salesProductStockRemain.value
})

const salesRules = computed(() => ({
  customerId: [{ required: true, message: '请选择客户', trigger: 'change' }],
  productId: [{ required: true, message: '请选择商品', trigger: 'change' }],
  quantity: [
    { required: true, message: '请输入数量', trigger: 'blur' },
    {
      validator: (rule, value, callback) => {
        if (value == null || value === '') {
          callback()
          return
        }
        if (salesProductStockRemain.value != null && salesProductStockRemain.value > 0 && value > salesProductStockRemain.value) {
          callback(new Error(`销售数量不能大于库存剩余（${salesProductStockRemain.value} 件）`))
          return
        }
        if (salesProductStockRemain.value != null && salesProductStockRemain.value <= 0) {
          callback(new Error('当前商品无可用库存'))
          return
        }
        callback()
      },
      trigger: ['blur', 'change']
    }
  ],
  remark: [{ max: SALES_ORDER_REMARK_MAX_LENGTH, message: `备注不能超过${SALES_ORDER_REMARK_MAX_LENGTH}个字符`, trigger: 'blur' }]
}))

const fetchSalesProductStock = async (productId) => {
  if (!productId) {
    salesProductStockRemain.value = null
    return
  }
  salesStockLoading.value = true
  try {
    const list = await inventoryApi.byProduct(productId)
    const rows = Array.isArray(list) ? list : []
    const total = rows.reduce((s, r) => s + (Number(r.stock) || 0), 0)
    salesProductStockRemain.value = total
    if (total > 0) {
      salesForm.value.quantity = Math.min(Math.max(1, salesForm.value.quantity), total)
    }
  } catch (e) {
    salesProductStockRemain.value = null
    ElMessage.error(e.message || '加载库存失败')
  } finally {
    salesStockLoading.value = false
  }
}

const onSalesProductChange = (productId) => {
  fetchSalesProductStock(productId)
}

const customerForm = ref({ name: '', type: '个人', address: '', contact: '', phone: '', email: '', remark: '' })
const customerRules = {
  name: [
    { required: true, message: '请输入客户名称', trigger: 'blur' },
    { max: 100, message: '客户名称不能超过100个字符', trigger: 'blur' }
  ],
  address: [
    { required: true, message: '请输入地址', trigger: 'blur' },
    { max: 200, message: '收货地址不能超过200个字符', trigger: 'blur' }
  ],
  phone: [
    { required: true, message: '请输入联系电话', trigger: 'blur' },
    { max: 20, message: '联系电话不能超过20个字符', trigger: 'blur' }
  ],
  contact: [{ max: 50, message: '联系人不能超过50个字符', trigger: 'blur' }],
  email: [{ max: 100, message: '邮箱不能超过100个字符', trigger: 'blur' }],
  remark: [{ max: 500, message: '备注不能超过500个字符', trigger: 'blur' }]
}

const aftersalesForm = ref({ salesOrderId: null, type: '质量问题', content: '', expect: '退货退款' })
const aftersalesRules = {
  salesOrderId: [{ required: true, message: '请选择关联订单', trigger: 'change' }],
  content: [
    { required: true, message: '请描述问题', trigger: 'blur' },
    { max: 500, message: '问题描述不能超过500个字符', trigger: 'blur' }
  ]
}

// 处理售后表单
const handleAftersalesForm = ref({ handleResult: '', refundAmount: 0, remark: '' })
const handleAftersalesRules = {
  handleResult: [{ required: true, message: '请选择处理结果', trigger: 'change' }],
  remark: [{ max: 500, message: '处理备注不能超过500个字符', trigger: 'blur' }]
}

const shipForm = ref({
  orderNo: '',
  orderId: null,
  customer: '',
  customerId: null,
  productId: null,
  productName: '',
  orderQuantity: 0,
  /** 本次发货数量（与后端 ShippingDTO.quantity 对应） */
  shipQuantity: 0,
  logisticsCompany: '',
  trackingNo: '',
  warehouse: '深圳仓库A区',
  warehouseId: 1,
  estimatedDate: new Date(Date.now() + 3 * 24 * 60 * 60 * 1000),
  receiverName: '',
  receiverPhone: '',
  receiverPhone2: '',
  receiverAddress: '',
  remark: '',
  stockInfo: [] // 商品库存信息
})
const shipRules = {
  logisticsCompany: [{ required: true, message: '请选择物流公司', trigger: 'change' }],
  trackingNo: [{ required: true, message: '请输入物流单号', trigger: 'blur' }],
  receiverName: [{ required: true, message: '请输入收货人姓名', trigger: 'blur' }],
  receiverPhone: [{ required: true, message: '请输入收货人电话', trigger: 'blur' }],
  receiverAddress: [{ required: true, message: '请输入收货地址', trigger: 'blur' }],
  remark: [{ max: 500, message: '发货备注不能超过500个字符', trigger: 'blur' }]
}

const getCustomerPhone = (id) => customers.value.find(c => Number(c.id) === Number(id))?.phone || ''

/** 发货回填：按客户 ID 解析档案（ID 类型统一 + 列表不全时拉详情） */
const resolveCustomerForShipment = async (customerId) => {
  if (customerId == null || customerId === '') return null
  const idNum = Number(customerId)
  const fromList = (list) => (list || []).find((c) => Number(c?.id) === idNum)
  let c = fromList(customers.value)
  if (c) return c
  await dataStore.loadCustomers()
  c = fromList(customers.value)
  if (c) return c
  try {
    return await customerApi.get(customerId)
  } catch {
    return null
  }
}
const getSalePrice = () => {
  const p = productLookupList.value.find(p => p.id === salesForm.value.productId)
  return p ? `¥${formatAmountDisplay(p.salePrice)}` : ''
}

// 状态格式化函数 - 英文转中文
const formatOrderStatus = (status) => {
  const statusMap = {
    'pending': '待发货',
    'shipped': '已发货',
    'completed': '已完成',
    'cancelled': '已取消',
    '待发货': '待发货',
    '已发货': '已发货',
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

// 获取商品图片
const getProductImageById = (productId) => {
  const product = productLookupList.value.find(p => p.id === productId)
  return firstProductImageUrl(product?.image)
}

const getRowProductImage = (row) => {
  const imageFromRow = firstProductImageUrl(row?.productImage || row?.image)
  if (imageFromRow) return imageFromRow
  return getProductImageById(row?.productId)
}

const getRowProductPreviewList = (row) => {
  const product = productLookupList.value.find((p) => p.id === row?.productId)
  return productRowPreviewUrls(row, product?.image)
}

// 获取商品名称（从商品列表动态获取最新名称）
const getProductName = (productId, fallbackName) => {
  const product = productLookupList.value.find(p => p.id === productId)
  return product?.name || fallbackName || '-'
}

// 选中商品的图片
const selectedProductImage = computed(() => {
  if (!salesForm.value.productId) return null
  return getProductImageById(salesForm.value.productId)
})

const salesSelectedProductPreviewList = computed(() => {
  const p = productLookupList.value.find((x) => x.id === salesForm.value.productId)
  return parseProductImageUrls(p?.image)
})

// 获取商品图标（无图片时使用）
const getProductIcon = (productName) => {
  if (productName?.includes('手机') || productName?.includes('Phone')) return '📱'
  if (productName?.includes('电脑') || productName?.includes('Mac') || productName?.includes('Book')) return '💻'
  if (productName?.includes('耳机') || productName?.includes('Pod')) return '🎧'
  if (productName?.includes('手表') || productName?.includes('Watch')) return '⌚'
  if (productName?.includes('平板') || productName?.includes('iPad')) return '📱'
  return '📦'
}

const getStatusType = (status) => ({ 'completed': 'success', 'shipped': 'success', 'pending': 'warning', 'cancelled': 'danger', '已完成': 'success', '已发货': 'success', '待发货': 'warning', '已取消': 'danger' }[status] || 'info')
const getPayStatusType = (status) => ({ 'paid': 'success', 'unpaid': 'warning', 'refunded': 'danger', '已付款': 'success', '待付款': 'warning', '已退款': 'danger' }[status] || 'info')
const getTypeColor = (type) => ({ '质量问题': 'warning', '退换货': 'info', '物流问题': 'info' }[type] || 'info')

// 售后状态格式化函数
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
  const typeMap = {
    'pending': 'warning',
    'processing': 'primary',
    'resolved': 'success',
    'completed': 'success',
    'closed': 'info',
    '待处理': 'warning',
    '处理中': 'primary',
    '已解决': 'success',
    '已完成': 'success',
    '已关闭': 'info'
  }
  return typeMap[status] || 'info'
}

const getAftersalesTypeColor = (type) => {
  const colorMap = {
    '质量问题': 'danger',
    '退换货': 'warning',
    '物流问题': 'info',
    '发票问题': 'info',
    '其他': 'info'
  }
  return colorMap[type] || 'info'
}

const openSalesDialog = () => {
  if (!canAddSalesOrder.value) {
    ElMessage.warning('无新建销售单权限')
    return
  }
  salesForm.value = { customerId: null, date: new Date(), productId: null, quantity: 1, payMethod: '银行转账', invoiceType: '不需要发票', remark: '' }
  salesProductStockRemain.value = null
  salesDialogVisible.value = true
  void loadSalesDialogOptions()
}
const openCustomerDialog = () => {
  if (!canManageCustomer.value) {
    ElMessage.warning('无客户管理权限')
    return
  }
  customerForm.value = { name: '', type: '个人', address: '', contact: '', phone: '', email: '', remark: '' }
  customerDialogVisible.value = true
}
const openAftersalesDialog = () => {
  if (!canAftersalesCreate.value) {
    ElMessage.warning('无售后工单权限')
    return
  }
  aftersalesForm.value = { salesOrderId: null, type: '质量问题', content: '', expect: '退货退款' }
  aftersalesDialogVisible.value = true
  void loadAftersalesDialogOptions()
}

// 提交销售订单 - 调用后端API
const submitSales = async () => {
  if (!canAddSalesOrder.value) {
    ElMessage.warning('无新建销售单权限')
    return
  }
  if (!salesFormRef.value) return
  if (salesForm.value.productId && salesStockLoading.value) {
    ElMessage.warning('正在查询库存，请稍候')
    return
  }
  if (salesProductStockRemain.value != null && salesForm.value.quantity > salesProductStockRemain.value) {
    ElMessage.warning('销售数量不能大于库存剩余')
    return
  }
  if (salesProductStockRemain.value !== null && salesProductStockRemain.value <= 0) {
    ElMessage.warning('当前商品无可用库存')
    return
  }
  await salesFormRef.value.validate(async (valid) => {
    if (valid) {
      loading.value = true
      try {
        const p = productLookupList.value.find(p => p.id === salesForm.value.productId)
        await salesApi.create({
          customerId: salesForm.value.customerId,
          productId: salesForm.value.productId,
          quantity: salesForm.value.quantity,
          unitPrice: p?.salePrice || 0,
          payMethod: salesForm.value.payMethod,
          remark: salesForm.value.remark
        })
        ElMessage.success('销售单创建成功')
        salesDialogVisible.value = false
        await Promise.all([
          fetchSalesOrderTable(false),
          loadStats()
        ])
      } catch (error) {
        ElMessage.error(error.message || '创建失败')
      } finally {
        loading.value = false
      }
    }
  })
}

// 提交客户 - 调用后端API
const submitCustomer = async () => {
  if (!canManageCustomer.value) {
    ElMessage.warning('无客户管理权限')
    return
  }
  if (!customerFormRef.value) return
  await customerFormRef.value.validate(async (valid) => {
    if (valid) {
      loading.value = true
      try {
        // 与后端 CustomerDTO（contactPerson / contactPhone）一致，避免仅依赖别名导致遗漏
        const customerData = {
          name: customerForm.value.name,
          type: customerForm.value.type,
          address: customerForm.value.address,
          contactPerson: customerForm.value.contact,
          contactPhone: customerForm.value.phone,
          email: customerForm.value.email,
          remark: customerForm.value.remark
        }

        if (customerForm.value.id) {
          // 编辑客户
          await customerApi.update(customerForm.value.id, customerData)
          ElMessage.success('客户更新成功')
        } else {
          // 新建客户
          await customerApi.create(customerData)
          ElMessage.success('客户添加成功')
        }

        customerDialogVisible.value = false
        await dataStore.loadCustomers()
      } catch (error) {
        ElMessage.error(error.message || '操作失败')
      } finally {
        loading.value = false
      }
    }
  })
}

// 编辑客户
const editCustomer = (c) => {
  if (!canManageCustomer.value) {
    ElMessage.warning('无客户管理权限')
    return
  }
  customerForm.value = {
    id: c.id,
    name: c.name,
    type: c.type,
    address: c.address,
    contact: c.contact,
    phone: c.phone,
    email: c.email,
    remark: c.remark
  }
  customerDialogVisible.value = true
}

// 删除客户
const deleteCustomerConfirm = async (c) => {
  if (!canManageCustomer.value) {
    ElMessage.warning('无客户管理权限')
    return
  }
  try {
    await ElMessageBox.confirm(`确定要删除客户「${c.name}」吗？删除后不可恢复。`, '删除确认', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    loading.value = true
    try {
      await customerApi.delete(c.id)
      ElMessage.success('客户已删除')
      await dataStore.loadCustomers()
      await loadStats()
    } catch (error) {
      ElMessage.error(error.message || '删除失败')
    } finally {
      loading.value = false
    }
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '删除失败')
    }
  }
}

// 提交售后工单 - 调用后端API
const submitAftersales = async () => {
  if (!canAftersalesCreate.value) {
    ElMessage.warning('无售后工单权限')
    return
  }
  if (!aftersalesFormRef.value) return
  await aftersalesFormRef.value.validate(async (valid) => {
    if (valid) {
      loading.value = true
      try {
        await aftersalesApi.create({
          salesOrderId: aftersalesForm.value.salesOrderId,
          type: aftersalesForm.value.type,
          content: aftersalesForm.value.content,
          expectHandle: aftersalesForm.value.expect
        })
        ElMessage.success('售后工单创建成功')
        aftersalesDialogVisible.value = false
        await Promise.all([
          dataStore.loadAftersales(),
          fetchAftersalesTable(false),
          loadStats()
        ])
      } catch (error) {
        ElMessage.error(error.message || '创建失败')
      } finally {
        loading.value = false
      }
    }
  })
}

const viewSalesOrder = (row) => { router.push(`/sales/order/${row.id}`) }
const handleShip = async (row) => {
  if (!canShipOrder.value) {
    ElMessage.warning('无发货权限')
    return
  }
  const orderQty = row.quantity || 0
  const shippedQty = row.shippedQuantity || 0
  const pendingShip =
    row.pendingQuantity != null && row.pendingQuantity !== undefined
      ? row.pendingQuantity
      : Math.max(0, orderQty - shippedQty)

  shipForm.value = {
    orderNo: row.orderNo,
    orderId: row.id,
    customer: row.customerName || row.customer,
    customerId: row.customerId,
    productId: row.productId,
    productName: row.productName || row.product,
    orderQuantity: row.quantity,
    shipQuantity: pendingShip,
    logisticsCompany: '',
    trackingNo: '',
    warehouse: '',
    warehouseId: row.warehouseId != null ? row.warehouseId : 1,
    estimatedDate: new Date(Date.now() + 3 * 24 * 60 * 60 * 1000),
    receiverName: row.receiverName || '',
    receiverPhone: row.receiverPhone || '',
    receiverPhone2: '',
    receiverAddress: row.receiverAddress || '',
    remark: '',
    stockInfo: []
  }
  shipDialogVisible.value = true
  shipDialogLoading.value = true
  try {
    await Promise.all([dataStore.loadInventory(), dataStore.loadWarehouses({ forceOptions: true })])
    const customer = await resolveCustomerForShipment(row.customerId)
    const receiverName =
      (customer?.contact && String(customer.contact).trim()) ||
      customer?.name ||
      row.receiverName ||
      ''
    const receiverPhone = customer?.phone || row.receiverPhone || ''
    const receiverAddress = customer?.address || row.receiverAddress || ''

    const stockInfo = inventoryData.value
      .filter(i => i.productId === row.productId || i.productName === row.product)
      .map(i => ({
        warehouseId: i.warehouseId,
        warehouseName: getWarehouseName(i.warehouseId) || i.warehouseName || i.warehouse,
        stock: i.stock || 0,
        safeStock: i.safeStock || 10
      }))

    if (stockInfo.length === 0) {
      stockInfo.push(...warehouses.value.map(w => ({
        warehouseId: w.id,
        warehouseName: w.name,
        stock: 0,
        safeStock: 10
      })))
    }

    shipForm.value = {
      orderNo: row.orderNo,
      orderId: row.id,
      customer: row.customerName || row.customer,
      customerId: row.customerId,
      productId: row.productId,
      productName: row.productName || row.product,
      orderQuantity: row.quantity,
      shipQuantity: pendingShip,
      logisticsCompany: '',
      trackingNo: '',
      warehouse: stockInfo[0]?.warehouseName || getWarehouseName(stockInfo[0]?.warehouseId) || '',
      warehouseId: stockInfo[0]?.warehouseId || 1,
      estimatedDate: new Date(Date.now() + 3 * 24 * 60 * 60 * 1000),
      receiverName,
      receiverPhone,
      receiverPhone2: '',
      receiverAddress,
      remark: '',
      stockInfo
    }
  } catch (e) {
    ElMessage.error(e?.message || '加载发货数据失败')
    shipDialogVisible.value = false
  } finally {
    shipDialogLoading.value = false
  }
}

// 仓库选择变化
const onWarehouseChange = (warehouseId) => {
  const warehouse = shipForm.value.stockInfo.find(w => w.warehouseId === warehouseId)
  if (warehouse) {
    shipForm.value.warehouse = warehouse.warehouseName
  }
}

// 发货 - 调用后端API
const submitShip = async () => {
  if (!canShipOrder.value) {
    ElMessage.warning('无发货权限')
    return
  }
  if (shipDialogLoading.value) {
    ElMessage.warning('正在加载发货数据，请稍候')
    return
  }
  if (!shipFormRef.value) return
  await shipFormRef.value.validate(async (valid) => {
    if (valid) {
      loading.value = true
      try {
        await salesApi.shipping(shipForm.value.orderId, {
          warehouseId: shipForm.value.warehouseId,
          quantity: shipForm.value.shipQuantity,
          logisticsCompany: shipForm.value.logisticsCompany,
          logisticsNo: shipForm.value.trackingNo,
          estimatedDate: shipForm.value.estimatedDate ? dayjs(shipForm.value.estimatedDate).format('YYYY-MM-DD') : undefined,
          receiverName: shipForm.value.receiverName || undefined,
          receiverPhone: [shipForm.value.receiverPhone, shipForm.value.receiverPhone2].filter(Boolean).join(' / ') || undefined,
          receiverAddress: shipForm.value.receiverAddress || undefined,
          remark: shipForm.value.remark || undefined
        })
        ElMessage.success(`订单 ${shipForm.value.orderNo} 已发货，物流单号: ${shipForm.value.trackingNo}`)
        shipDialogVisible.value = false
        await Promise.all([
          fetchSalesOrderTable(false),
          dataStore.loadInventory(),
          loadStats()
        ])
      } catch (error) {
        ElMessage.error(error.message || '发货失败')
      } finally {
        loading.value = false
      }
    }
  })
}

// 确认付款
const handlePayment = async (row) => {
  if (!canConfirmPayment.value) {
    ElMessage.warning('无确认付款权限')
    return
  }
  try {
    await salesApi.payment(row.id)
    ElMessage.success(`订单 ${row.orderNo} 已确认付款`)
    await Promise.all([
      fetchSalesOrderTable(false),
      loadStats()
    ])
  } catch (error) {
    ElMessage.error(error.message || '确认付款失败')
  }
}

// 确认收货
const handleReceived = async (row) => {
  if (!canReceiveOrder.value) {
    ElMessage.warning('无确认收货权限')
    return
  }
  try {
    await salesApi.received(row.id)
    ElMessage.success(`订单 ${row.orderNo} 已确认收货`)
    await Promise.all([
      fetchSalesOrderTable(false),
      loadStats()
    ])
  } catch (error) {
    ElMessage.error(error.message || '确认收货失败')
  }
}

const printSalesOrder = (row) => { ElMessage.success(`打印销售单 - ${row.orderNo}`) }
const createSalesFromCustomer = (c) => {
  if (!canAddSalesOrder.value) {
    ElMessage.warning('无新建销售单权限')
    return
  }
  salesForm.value = { customerId: c.id, date: new Date(), productId: null, quantity: 1, payMethod: '银行转账', invoiceType: '不需要发票', remark: '' }
  salesProductStockRemain.value = null
  salesDialogVisible.value = true
  void loadSalesDialogOptions()
  void mergeCustomerById(c.id)
}
const viewCustomerDetail = (c) => { router.push(`/sales/customer/${c.id}`) }

// 售后详情
const viewAftersalesDetail = (row) => {
  currentAftersales.value = row
  aftersalesDetailVisible.value = true
}

// 打开处理售后对话框
const openHandleAftersales = (row) => {
  currentAftersales.value = row
  handleAftersalesForm.value = { handleResult: '', refundAmount: 0, remark: '' }
  aftersalesDetailVisible.value = false
  handleAftersalesVisible.value = true
}

// 提交处理售后
const submitHandleAftersales = async () => {
  if (!handleAftersalesFormRef.value) return
  await handleAftersalesFormRef.value.validate(async (valid) => {
    if (valid) {
      loading.value = true
      try {
        await aftersalesApi.handle(currentAftersales.value.id, {
          handleResult: handleAftersalesForm.value.handleResult,
          refundAmount: handleAftersalesForm.value.refundAmount,
          remark: handleAftersalesForm.value.remark
        })
        ElMessage.success('售后工单处理成功')
        handleAftersalesVisible.value = false
        await Promise.all([
          dataStore.loadAftersales(),
          fetchAftersalesTable(false),
          loadStats()
        ])
      } catch (error) {
        ElMessage.error(error.message || '处理失败')
      } finally {
        loading.value = false
      }
    }
  })
}

// 关闭售后工单
const closeAftersalesOrder = async (row) => {
  try {
    await aftersalesApi.close(row.id)
    ElMessage.success('售后工单已关闭')
    aftersalesDetailVisible.value = false
    await Promise.all([
      dataStore.loadAftersales(),
      fetchAftersalesTable(false),
      loadStats()
    ])
  } catch (error) {
    ElMessage.error(error.message || '关闭失败')
  }
}

const printOrder = () => { ElMessage.success('打印订单功能已触发'); orderDetailVisible.value = false }

// 初始化加载（详情页「确认发货」带 shipOrderId 时自动打开发货弹窗）
onMounted(async () => {
  await loadData()
  const shipOid = route.query.shipOrderId
  if (!shipOid) return
  let row = null
  try {
    row = await salesApi.get(Number(shipOid))
  } catch {
    row = (salesOrderTableRows.value || []).find((o) => String(o.id) === String(shipOid)) || null
  }
  await router.replace({ path: route.path, query: {} })
  if (row && canShipOrder.value && row.status === 'pending' && row.payStatus === 'paid') {
    activeTab.value = 'orders'
    await handleShip(row)
  }
})
</script>

<style lang="scss" scoped>
.sales-page {
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
      .warning-text { color: #F56C6C; }
      .stat-change { font-size: 12px;
        &.up { color: #67C23A; }
        &.down { color: #F56C6C; }
      }
    }
  }
  .stats-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 20px; margin-bottom: 24px; }
  .page-tabs { :deep(.el-tabs__header) { margin-bottom: 16px; } }
  .card-header {
    display: flex;
    justify-content: flex-end;
    align-items: flex-start;
    width: 100%;
    min-width: 0;

    .header-actions {
      display: flex;
      align-items: center;
      gap: 10px 12px;
    }
  }

  /* 列表卡片筛选栏：允许换行与弹性宽度，适配 13–15 寸屏 */
  .page-filter-bar {
    flex-wrap: wrap;
    justify-content: flex-end;
    width: 100%;
    max-width: 100%;
    box-sizing: border-box;

    @media (max-width: 1400px) {
      justify-content: flex-start;
    }

    .page-filter-primary {
      flex: 0 0 auto;
    }

    .page-filter-ctl--wide {
      width: 160px;
      flex: 1 1 150px;
      min-width: 120px;
      max-width: 200px;
    }

    .page-filter-ctl--narrow {
      width: 132px;
      flex: 1 1 120px;
      min-width: 112px;
      max-width: 168px;
    }

    .page-filter-ctl--daterange {
      width: 248px;
      flex: 2 1 220px;
      min-width: 200px;
      max-width: 380px;
    }

    .page-filter-ctl--search {
      width: 200px;
      flex: 3 1 200px;
      min-width: 180px;
      max-width: 100%;
    }

    /** 售后服务 Tab：控件少，避免搜索框被 flex 拉满整行 */
    .page-filter-ctl--search-compact {
      width: 200px;
      flex: 0 1 200px;
      min-width: 160px;
      max-width: 240px;
    }

    @media (max-width: 1400px) {
      .page-filter-ctl--wide,
      .page-filter-ctl--narrow,
      .page-filter-ctl--daterange,
      .page-filter-ctl--search {
        width: auto;
        max-width: none;
      }

      .page-filter-ctl--search-compact {
        flex: 1 1 200px;
        max-width: min(100%, 280px);
        min-width: 0;
      }

      .page-filter-ctl--wide {
        flex: 1 1 calc(50% - 6px);
        min-width: 136px;
      }

      .page-filter-ctl--narrow {
        flex: 1 1 calc(50% - 6px);
        min-width: 120px;
      }

      .page-filter-ctl--daterange {
        flex: 1 1 100%;
        min-width: 0;
      }

      .page-filter-ctl--search {
        flex: 1 1 min(100%, 420px);
        min-width: 0;
      }
    }

    @media (max-width: 720px) {
      .page-filter-ctl--wide,
      .page-filter-ctl--narrow,
      .page-filter-ctl--search {
        flex: 1 1 100%;
        min-width: 0;
      }

      .page-filter-ctl--search-compact {
        flex: 1 1 100%;
        max-width: none;
      }
    }

    .page-filter-ctl--daterange :deep(.el-range-editor.el-input__wrapper) {
      width: 100%;
      max-width: 100%;
      box-sizing: border-box;
    }
  }
  .order-no { color: #E94560; &.warning { color: #E6A23C; } }
  .amount { font-weight: 600; color: #E94560; }
  .customer-cell { display: flex; align-items: center; gap: 12px;
    .customer-icon { width: 40px; height: 40px; border-radius: 8px; background: #F5F7FA; display: flex; align-items: center; justify-content: center; font-size: 20px; }
    .customer-info { h4 { font-size: 14px; font-weight: 600; color: #303133; } p { font-size: 12px; color: #909399; } }
  }
  .customers-tab-wrap { min-height: 280px; }
  .customer-search-bar { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }
  .grid-empty { padding: 32px 0; }
  .customers-grid { display: grid; grid-template-columns: repeat(2, 1fr); gap: 20px; margin-bottom: 20px; }
  .customer-card {
    :deep(.el-card__body) { padding: 24px; }
    .customer-header { display: flex; align-items: center; gap: 16px; margin-bottom: 16px; }
    .customer-avatar { width: 56px; height: 56px; border-radius: 14px; background: rgba(233, 69, 96, 0.15); display: flex; align-items: center; justify-content: center; font-size: 28px; }
    .customer-info { flex: 1; h4 { font-size: 16px; font-weight: 600; color: #303133; margin-bottom: 4px; } p { font-size: 13px; color: #909399; } }
    .customer-stats { display: grid; grid-template-columns: repeat(3, 1fr); gap: 16px; padding: 16px 0; border-top: 1px solid #E4E7ED; border-bottom: 1px solid #E4E7ED; margin-bottom: 16px; }
    .customer-stat { text-align: center; .stat-label { display: block; font-size: 12px; color: #909399; margin-bottom: 6px; } .stat-value { font-size: 16px; font-weight: 700; color: #303133; } }
    .customer-contact { margin-bottom: 16px; }
    .contact-item { display: flex; align-items: center; gap: 10px; padding: 8px 0; font-size: 13px; color: #606266; }
    .customer-actions { display: flex; gap: 12px; }
  }
  .add-customer-btn { width: 100%; justify-content: center; }
  .stock-info-display { display: flex; flex-wrap: wrap; gap: 8px; }
  .stock-item { padding: 4px 12px; border-radius: 4px; background: rgba(103, 194, 58, 0.1); color: #67C23A; font-size: 13px; font-weight: 500; }
  .stock-item.low-stock { background: rgba(230, 162, 60, 0.1); color: #E6A23C; }

  .sales-stock-tip {
    margin-top: 6px;
    font-size: 12px;
    color: #606266;
    .sales-stock-warn { color: #F56C6C; margin-left: 4px; }
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
    }
  }

  .pagination-wrapper { display: flex; justify-content: flex-end; padding-top: 16px; }

  // 商品图片迷你样式
  .product-cell-mini {
    display: flex;
    align-items: center;
    gap: 8px;

    .product-thumb-mini {
      flex-shrink: 0;
      border-radius: 4px;
      overflow: hidden;
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
      flex: 1;
      min-width: 0;
      font-size: 13px;
      color: #303133;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }
  }

  // 选中商品图片预览样式
  .selected-product-image {
    display: flex;
    align-items: center;
    justify-content: center;

    .product-preview {
      border-radius: 8px;
      overflow: hidden;
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

:deep(.aftersales-detail-dialog .el-dialog__body) {
  max-height: 65vh;
  overflow-y: auto;
  overflow-x: hidden;
}

:deep(.aftersales-detail-dialog .el-descriptions__label) {
  width: 88px;
  white-space: nowrap;
  vertical-align: top;
}

:deep(.aftersales-detail-dialog .el-descriptions__content) {
  vertical-align: top;
  min-width: 0;
}

:deep(.aftersales-detail-dialog .el-descriptions__table) {
  width: 100%;
  table-layout: fixed;
}

:deep(.aftersales-detail-dialog .aftersales-detail-text) {
  display: block;
  width: 100%;
  max-width: 100%;
  white-space: nowrap;
  text-overflow: ellipsis;
  line-height: 1.5;
  overflow: hidden;
  overflow-x: hidden;
  padding-right: 4px;
}

</style>

<style lang="scss">
.el-popper.aftersales-detail-tooltip[role='tooltip'],
.el-tooltip__popper.aftersales-detail-tooltip {
  max-width: min(320px, calc(100vw - 48px)) !important;
  width: auto !important;
  box-sizing: border-box !important;
}

.el-popper.aftersales-detail-tooltip[role='tooltip'] .el-popper__content,
.el-tooltip__popper.aftersales-detail-tooltip .el-popper__content {
  max-width: 100% !important;
  white-space: normal !important;
  word-break: break-word !important;
  overflow-wrap: anywhere !important;
  line-height: 1.5;
}
</style>