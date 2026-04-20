<template>
  <div class="inventory-page">
    <el-tabs v-model="activeTab" class="page-tabs">
      <!-- 库存管理 -->
      <el-tab-pane v-if="canInventoryStockTab" label="库存管理" name="stock">
        <el-card v-loading="stockTabLoading" element-loading-text="加载中...">
          <template #header>
            <div class="card-header">
              <div class="header-actions">
                <el-select
                  v-model="filterProduct"
                  v-load-more="{ popperClass: 'inventory-filter-product-dropdown', onLoadMore: loadMoreProductOptions, disabled: productOptionsLoading || !productOptionsHasMore }"
                  popper-class="inventory-filter-product-dropdown"
                  placeholder="按商品筛选"
                  clearable
                  filterable
                  style="width: 160px"
                  @visible-change="onProductSelectVisibleChange"
                  @filter-method="onProductFilter"
                >
                  <el-option v-for="p in productOptions" :key="p.id" :label="p.name" :value="p.id" />
                </el-select>
                <el-select
                  v-model="filterWarehouse"
                  v-load-more="{ popperClass: 'inventory-shared-warehouse-dropdown', onLoadMore: loadMoreWarehouseFilterOptions, disabled: warehouseFilterOptionsLoading || !warehouseFilterOptionsHasMore }"
                  popper-class="inventory-shared-warehouse-dropdown"
                  placeholder="按仓库筛选"
                  clearable
                  filterable
                  style="width: 140px"
                  @visible-change="onWarehouseDropdownVisibleChange"
                  @filter-method="onWarehouseFilter"
                >
                  <el-option v-for="w in warehouseFilterOptions" :key="w.id" :label="w.name" :value="w.id" />
                </el-select>
                <el-select v-model="filterStagnantStatus" placeholder="按呆滞状态筛选" clearable style="width: 120px">
                  <el-option label="呆滞" value="stagnant" />
                  <el-option label="正常" value="normal" />
                </el-select>
                <el-date-picker
                  v-model="filterLastOutboundRange"
                  class="inventory-filter-daterange"
                  type="daterange"
                  format="YY-MM-DD"
                  range-separator="-"
                  start-placeholder="出库起"
                  end-placeholder="出库止"
                  value-format="YYYY-MM-DD"
                />
                <el-date-picker
                  v-model="filterLastInboundRange"
                  class="inventory-filter-daterange"
                  type="daterange"
                  format="YY-MM-DD"
                  range-separator="-"
                  start-placeholder="入库起"
                  end-placeholder="入库止"
                  value-format="YYYY-MM-DD"
                />
                <el-input v-model="searchKeyword" placeholder="搜索SKU..." prefix-icon="Search" clearable style="width: 140px" />
                <el-upload
                  v-if="canInventoryRead"
                  accept="image/*"
                  :auto-upload="false"
                  :show-file-list="false"
                  @change="onInventoryQueryImageChange"
                >
                  <el-button size="small">+上传图片</el-button>
                </el-upload>
                <img v-if="queryImageDataUrl" :src="queryImageDataUrl" class="image-query-thumb" alt="" />
                <el-input-number
                  v-if="canInventoryRead"
                  v-model="imageSimilarityThreshold"
                  :min="0.2"
                  :max="0.99"
                  :step="0.05"
                  :precision="2"
                  size="small"
                  style="width: 108px"
                />
                <el-button v-if="canInventoryRead" type="primary" size="small" :loading="imageSearchLoading" @click="submitImageSearch">以图搜图</el-button>
                <el-button v-if="imageSearchMode" type="info" size="small" link @click="exitImageSearch">退出图搜</el-button>
                <div class="header-actions-inout">
                  <el-button v-if="canInventoryInbound" type="success" @click="openManualInboundDialog"><el-icon><Plus /></el-icon>手动入库</el-button>
                  <el-button v-if="canInventoryOutbound" type="warning" @click="openManualOutboundDialog"><el-icon><Minus /></el-icon>手动出库</el-button>
                </div>
              </div>
            </div>
          </template>
          <el-table :data="paginatedInventory" empty-text="暂无数据" style="width: 100%" :max-height="500" table-layout="fixed">
            <el-table-column label="SKU" min-width="100" show-overflow-tooltip>
              <template #default="{ row }"><span class="sku">{{ row.sku }}</span></template>
            </el-table-column>
            <el-table-column label="商品" min-width="160" show-overflow-tooltip>
              <template #default="{ row }">
                <div class="product-cell">
                  <ProductImageThumb
                    v-if="getProductImage(row.productId, row.productImage || row.image)"
                    :src="getProductImage(row.productId, row.productImage || row.image)"
                    :preview-src-list="parseProductImageUrls(row.productImage || row.image)"
                    class="product-thumb"
                    :width="40"
                    :height="40"
                    :radius="8"
                  />
                  <div v-else class="product-icon">{{ getCategoryIcon(row.category || '') }}</div>
                  <div class="product-info"><h4>{{ row.productName || row.name }}</h4></div>
                </div>
              </template>
            </el-table-column>
            <el-table-column label="仓库" min-width="120" show-overflow-tooltip>
              <template #default="{ row }">{{ getWarehouseName(row.warehouseId) || row.warehouseName }}</template>
            </el-table-column>
            <el-table-column label="库存" width="68" align="center">
              <template #default="{ row }">
                <span :class="{ 'low-stock': row.stock < row.safeStock, 'critical-stock': row.stock < row.safeStock / 2 }">{{ row.stock }}</span>
              </template>
            </el-table-column>
            <el-table-column label="安全值" prop="safeStock" width="72" align="center" />
            <el-table-column label="价值" width="92" align="right">
              <template #default="{ row }"><span class="amount">¥{{ ((row.stockValue || row.value || 0) / 10000).toFixed(1) }}万</span></template>
            </el-table-column>
            <el-table-column label="状态" min-width="80" align="center">
              <template #default="{ row }">
                <el-tag :type="getStockStatusType(row.status)" effect="light" size="small">{{ formatStockStatus(row.status) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="最后出库" width="176" show-overflow-tooltip>
              <template #default="{ row }">{{ row.lastOutboundTime || '-' }}</template>
            </el-table-column>
            <el-table-column label="最后入库" width="176" show-overflow-tooltip>
              <template #default="{ row }">{{ row.lastInboundTime || '-' }}</template>
            </el-table-column>
            <el-table-column label="呆滞状态" min-width="90" align="center">
              <template #default="{ row }">
                <el-tag :type="getStagnantStatusType(row)" effect="light" size="small">{{ getStagnantStatusText(row) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="160" align="center">
              <template #default="{ row }">
                <el-button type="primary" link size="small" @click="viewStockDetail(row)">详情</el-button>
                <el-button v-if="canInventoryInbound" type="success" link size="small" @click="openStockInbound(row)">入库</el-button>
                <el-button v-if="canInventoryOutbound" type="warning" link size="small" @click="openStockOutbound(row)">出库</el-button>
              </template>
            </el-table-column>
          </el-table>
          <div class="pagination-wrapper">
            <el-pagination
              v-model:current-page="inventoryCurrentPage"
              v-model:page-size="inventoryPageSize"
              :total="inventoryListTotal"
              :page-sizes="[10, 20, 50, 100]"
              layout="total, sizes, prev, pager, next, jumper"
              @current-change="handleInventoryPageChange"
              @size-change="handleInventorySizeChange"
            />
          </div>
        </el-card>
      </el-tab-pane>

      <!-- 调拨记录 -->
      <el-tab-pane v-if="canInventoryTransferTab" label="调拨记录" name="transfer">
        <el-card v-loading="transferTabLoading" element-loading-text="加载中...">
          <template #header>
            <div class="card-header">
              <div class="header-actions">
                <el-input
                  v-model="transferFilterKeyword"
                  placeholder="单号/商品/SKU"
                  clearable
                  prefix-icon="Search"
                  style="width: 160px"
                />
                <el-select v-model="transferFilterStatus" placeholder="调拨状态" clearable style="width: 120px">
                  <el-option label="待确认" value="pending" />
                  <el-option label="已完成" value="completed" />
                  <el-option label="已取消" value="cancelled" />
                </el-select>
                <el-select
                  v-model="transferFilterWarehouseId"
                  v-load-more="{ popperClass: 'inventory-shared-warehouse-dropdown', onLoadMore: loadMoreWarehouseFilterOptions, disabled: warehouseFilterOptionsLoading || !warehouseFilterOptionsHasMore }"
                  popper-class="inventory-shared-warehouse-dropdown"
                  placeholder="涉及仓库"
                  clearable
                  filterable
                  style="width: 140px"
                  @visible-change="onWarehouseDropdownVisibleChange"
                  @filter-method="onWarehouseFilter"
                >
                  <el-option v-for="w in warehouseFilterOptions" :key="w.id" :label="w.name" :value="w.id" />
                </el-select>
                <el-date-picker
                  v-model="transferFilterDateRange"
                  class="inventory-filter-daterange"
                  type="daterange"
                  format="YY-MM-DD"
                  range-separator="-"
                  start-placeholder="创建起"
                  end-placeholder="创建止"
                  value-format="YYYY-MM-DD"
                  clearable
                />
                <el-button v-if="canTransferOp" type="primary" @click="openTransferDialog()"><el-icon><Plus /></el-icon>新建调拨</el-button>
              </div>
            </div>
          </template>
          <el-table :data="paginatedTransfers" empty-text="暂无数据" style="width: 100%" :max-height="500" table-layout="fixed">
            <el-table-column label="单号" min-width="132" show-overflow-tooltip>
              <template #default="{ row }"><span class="order-no">{{ row.orderNo }}</span></template>
            </el-table-column>
            <el-table-column label="商品" min-width="180" show-overflow-tooltip>
              <template #default="{ row }">
                <div class="product-cell-mini">
                  <ProductImageThumb
                    v-if="getProductImage(row.productId, row.productImage || row.image)"
                    :src="getProductImage(row.productId, row.productImage || row.image)"
                    :preview-src-list="parseProductImageUrls(row.productImage || row.image)"
                    class="product-thumb-mini"
                    :width="32"
                    :height="32"
                    :radius="4"
                  />
                  <span v-else class="product-icon-mini">{{ getCategoryIcon(row.category || '') }}</span>
                  <span class="product-name-mini">{{ row.productName || row.product }}</span>
                </div>
              </template>
            </el-table-column>
            <el-table-column label="SKU" prop="sku" min-width="120" show-overflow-tooltip />
            <el-table-column label="调出仓库" min-width="128" show-overflow-tooltip>
              <template #default="{ row }">{{ getWarehouseName(row.fromWarehouseId) || row.fromWarehouseName }}</template>
            </el-table-column>
            <el-table-column label="调入仓库" min-width="128" show-overflow-tooltip>
              <template #default="{ row }">{{ getWarehouseName(row.toWarehouseId) || row.toWarehouseName }}</template>
            </el-table-column>
            <el-table-column label="调拨数量" width="88" align="center">
              <template #default="{ row }">{{ row.quantity }}</template>
            </el-table-column>
            <el-table-column label="状态" width="88" align="center">
              <template #default="{ row }"><el-tag :type="getTransferStatusType(row.status)" effect="light" size="small">{{ formatTransferStatus(row.status) }}</el-tag></template>
            </el-table-column>
            <el-table-column label="创建时间" prop="createTime" width="176" show-overflow-tooltip />
            <el-table-column label="操作人" min-width="100" show-overflow-tooltip>
              <template #default="{ row }">{{ row.operatorName || row.operator || '-' }}</template>
            </el-table-column>
            <el-table-column label="操作" width="120" align="center">
              <template #default="{ row }">
                <el-button type="primary" link size="small" @click="viewTransferDetail(row)">详情</el-button>
                <el-button
                  v-if="canTransferOp && row.status === 'pending'"
                  type="success"
                  link
                  size="small"
                  @click="confirmTransfer(row)"
                >确认</el-button>
              </template>
            </el-table-column>
          </el-table>
          <div class="pagination-wrapper">
            <el-pagination
              v-model:current-page="transferCurrentPage"
              v-model:page-size="transferPageSize"
              :total="transferTableTotal"
              :page-sizes="[10, 20, 50, 100]"
              layout="total, sizes, prev, pager, next, jumper"
            />
          </div>
        </el-card>
      </el-tab-pane>

      <!-- 出入库记录 -->
      <el-tab-pane v-if="canInventoryRecordsTab" label="出入库记录" name="records">
        <div v-loading="recordsTabLoading" element-loading-text="加载中..." class="records-tab-loading-wrap">
        <el-tabs v-model="recordsSubTab" type="card">
          <!-- 入库记录 -->
          <el-tab-pane label="入库记录" name="inbound">
            <el-card>
              <div class="records-filter-bar">
                <el-input
                  v-model="recordsInboundFilterKeyword"
                  placeholder="单号/商品/仓库/采购单/供应商"
                  clearable
                  prefix-icon="Search"
                  style="width: 220px"
                />
                <el-select
                  v-model="recordsInboundFilterWarehouseId"
                  v-load-more="{ popperClass: 'inventory-shared-warehouse-dropdown', onLoadMore: loadMoreWarehouseFilterOptions, disabled: warehouseFilterOptionsLoading || !warehouseFilterOptionsHasMore }"
                  popper-class="inventory-shared-warehouse-dropdown"
                  placeholder="仓库"
                  clearable
                  filterable
                  style="width: 140px"
                  @visible-change="onWarehouseDropdownVisibleChange"
                  @filter-method="onWarehouseFilter"
                >
                  <el-option v-for="w in warehouseFilterOptions" :key="w.id" :label="w.name" :value="w.id" />
                </el-select>
                <el-date-picker
                  v-model="recordsInboundFilterDateRange"
                  class="inventory-filter-daterange"
                  type="daterange"
                  format="YY-MM-DD"
                  range-separator="-"
                  start-placeholder="入库起"
                  end-placeholder="入库止"
                  value-format="YYYY-MM-DD"
                  clearable
                />
                <el-input v-model="recordsInboundFilterOperator" placeholder="操作人" clearable style="width: 120px" />
              </div>
              <el-table :data="inboundRecords" empty-text="暂无数据" style="width: 100%" :max-height="500">
                <el-table-column label="入库单号" width="140">
                  <template #default="{ row }"><span class="order-no success">{{ row.orderNo }}</span></template>
                </el-table-column>
                <el-table-column label="采购单号" width="140">
                  <template #default="{ row }"><span class="order-no">{{ row.purchaseOrderNo || row.purchaseNo }}</span></template>
                </el-table-column>
                <el-table-column label="供应商" min-width="120" show-overflow-tooltip>
                  <template #default="{ row }">{{ row.supplierName || row.supplier }}</template>
                </el-table-column>
                <el-table-column label="商品" min-width="180" show-overflow-tooltip>
                  <template #default="{ row }">
                    <div class="product-cell-mini">
                      <ProductImageThumb
                        v-if="getProductImage(row.productId, row.productImage || row.image)"
                        :src="getProductImage(row.productId, row.productImage || row.image)"
                        :preview-src-list="parseProductImageUrls(row.productImage || row.image)"
                        class="product-thumb-mini"
                        :width="32"
                        :height="32"
                        :radius="4"
                      />
                      <span v-else class="product-icon-mini">{{ getCategoryIcon(row.category || '') }}</span>
                      <span class="product-name-mini">{{ row.productName || row.product }}</span>
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
                  v-model:current-page="recordsInboundCurrentPage"
                  v-model:page-size="recordsInboundPageSize"
                  :total="inboundRecordsTotal"
                  :page-sizes="[10, 20, 50, 100]"
                  layout="total, sizes, prev, pager, next, jumper"
                />
              </div>
            </el-card>
          </el-tab-pane>
          <!-- 出库记录 -->
          <el-tab-pane label="出库记录" name="outbound">
            <el-card>
              <div class="records-filter-bar">
                <el-input
                  v-model="recordsOutboundFilterKeyword"
                  placeholder="单号/商品/仓库/客户/销售单号"
                  clearable
                  prefix-icon="Search"
                  style="width: 220px"
                />
                <el-select
                  v-model="recordsOutboundFilterWarehouseId"
                  v-load-more="{ popperClass: 'inventory-shared-warehouse-dropdown', onLoadMore: loadMoreWarehouseFilterOptions, disabled: warehouseFilterOptionsLoading || !warehouseFilterOptionsHasMore }"
                  popper-class="inventory-shared-warehouse-dropdown"
                  placeholder="仓库"
                  clearable
                  filterable
                  style="width: 140px"
                  @visible-change="onWarehouseDropdownVisibleChange"
                  @filter-method="onWarehouseFilter"
                >
                  <el-option v-for="w in warehouseFilterOptions" :key="w.id" :label="w.name" :value="w.id" />
                </el-select>
                <el-date-picker
                  v-model="recordsOutboundFilterDateRange"
                  class="inventory-filter-daterange"
                  type="daterange"
                  format="YY-MM-DD"
                  range-separator="-"
                  start-placeholder="出库起"
                  end-placeholder="出库止"
                  value-format="YYYY-MM-DD"
                  clearable
                />
                <el-input v-model="recordsOutboundFilterOperator" placeholder="操作人" clearable style="width: 120px" />
              </div>
              <el-table :data="outboundRecords" empty-text="暂无数据" style="width: 100%" :max-height="500">
                <el-table-column label="出库单号" width="140">
                  <template #default="{ row }"><span class="order-no warning">{{ row.orderNo }}</span></template>
                </el-table-column>
                <el-table-column label="销售单号" width="140">
                  <template #default="{ row }"><span class="order-no">{{ row.salesOrderNo || row.salesNo }}</span></template>
                </el-table-column>
                <el-table-column label="客户" min-width="120" show-overflow-tooltip>
                  <template #default="{ row }">{{ row.customerName || row.customer }}</template>
                </el-table-column>
                <el-table-column label="商品" min-width="180" show-overflow-tooltip>
                  <template #default="{ row }">
                    <div class="product-cell-mini">
                      <ProductImageThumb
                        v-if="getProductImage(row.productId, row.productImage || row.image)"
                        :src="getProductImage(row.productId, row.productImage || row.image)"
                        :preview-src-list="parseProductImageUrls(row.productImage || row.image)"
                        class="product-thumb-mini"
                        :width="32"
                        :height="32"
                        :radius="4"
                      />
                      <span v-else class="product-icon-mini">{{ getCategoryIcon(row.category || '') }}</span>
                      <span class="product-name-mini">{{ row.productName || row.product }}</span>
                    </div>
                  </template>
                </el-table-column>
                <el-table-column label="数量" width="80" align="center">
                  <template #default="{ row }">{{ row.quantity }}</template>
                </el-table-column>
                <el-table-column label="仓库" min-width="120" show-overflow-tooltip>
                  <template #default="{ row }">{{ getWarehouseName(row.warehouseId) || row.warehouseName || row.warehouse }}</template>
                </el-table-column>
                <el-table-column label="出库时间" width="176" show-overflow-tooltip>
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
                  v-model:current-page="recordsOutboundCurrentPage"
                  v-model:page-size="recordsOutboundPageSize"
                  :total="outboundRecordsTotal"
                  :page-sizes="[10, 20, 50, 100]"
                  layout="total, sizes, prev, pager, next, jumper"
                />
              </div>
            </el-card>
          </el-tab-pane>
        </el-tabs>
        </div>
      </el-tab-pane>

      <!-- 仓库管理 -->
      <el-tab-pane v-if="canInventoryWarehouseTab" label="仓库管理" name="warehouse">
        <div v-loading="warehouseTabLoading" element-loading-text="加载中..." class="warehouse-tab-loading-wrap">
        <div class="warehouse-search-bar">
          <el-input v-model="warehouseSearchKeyword" placeholder="搜索仓库名称、编码、地址..." prefix-icon="Search" clearable style="width: 240px" />
          <el-button v-if="canInventoryWarehouse" type="primary" @click="openWarehouseDialog"><el-icon><Plus /></el-icon>添加仓库</el-button>
        </div>
        <el-empty v-if="!warehouseTabLoading && !warehouseGridList.length" class="grid-empty" description="暂无数据" :image-size="80" />
        <el-empty v-else-if="!warehouseTabLoading && !filteredWarehouseGrid.length" class="grid-empty" description="未找到匹配的仓库" :image-size="80" />
        <div v-else class="warehouse-grid">
          <el-card class="warehouse-card" v-for="w in filteredWarehouseGrid" :key="w.id">
            <div class="warehouse-header">
              <div class="warehouse-icon"><el-icon><Shop /></el-icon></div>
              <div class="warehouse-info"><h4>{{ w.name }}</h4><p>{{ w.address }}</p></div>
            </div>
            <div class="warehouse-stats">
              <div class="warehouse-stat"><span class="stat-label">商品种类</span><span class="stat-value">{{ w.totalCategories || 0 }}</span></div>
              <div class="warehouse-stat"><span class="stat-label">总库存</span><span class="stat-value">{{ w.totalStock || 0 }} 件</span></div>
              <div class="warehouse-stat"><span class="stat-label">库存价值</span><span class="stat-value">¥{{ ((w.totalValue || 0) / 10000).toFixed(1) }}万</span></div>
              <div class="warehouse-stat"><span class="stat-label">利用率</span><span class="stat-value" :style="{ color: (w.capacityUsed || 50) > 70 ? '#67C23A' : '#E6A23C' }">{{ (w.capacityUsed || 50).toFixed(0) }}%</span></div>
            </div>
            <div class="warehouse-capacity">
              <div class="capacity-bar"><div class="capacity-fill" :style="{ width: (w.capacityUsed || 50) + '%', background: (w.capacityUsed || 50) > 70 ? 'linear-gradient(90deg, #67C23A, #95d475)' : 'linear-gradient(90deg, #E6A23C, #f5d44d)' }"></div></div>
              <span>容量使用 {{ (w.capacityUsed || 50).toFixed(0) }}%</span>
            </div>
            <div class="warehouse-actions">
              <el-button type="primary" @click="viewWarehouseDetail(w)">查看详情</el-button>
              <el-button v-if="canInventoryWarehouse" @click="editWarehouse(w)">编辑</el-button>
              <el-button v-if="canInventoryWarehouse" type="danger" @click="deleteWarehouse(w)" :disabled="w.totalStock > 0">删除</el-button>
            </div>
          </el-card>
        </div>
        </div>
      </el-tab-pane>
    </el-tabs>

    <!-- 调拨对话框 -->
    <el-dialog v-model="transferDialogVisible" title="新建调拨单" width="650px" destroy-on-close>
      <el-form ref="transferFormRef" :model="transferForm" :rules="transferRules" label-width="100px">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="调出仓库" prop="fromWarehouseId">
              <el-select
                v-model="transferForm.fromWarehouseId"
                v-load-more="{ popperClass: 'inventory-shared-warehouse-dropdown', onLoadMore: loadMoreWarehouseFilterOptions, disabled: warehouseFilterOptionsLoading || !warehouseFilterOptionsHasMore }"
                popper-class="inventory-shared-warehouse-dropdown"
                placeholder="请选择仓库"
                style="width: 100%"
                filterable
                @visible-change="onWarehouseDropdownVisibleChange"
                @filter-method="onWarehouseFilter"
              >
                <el-option v-for="w in warehouseFilterOptions" :key="w.id" :label="w.name" :value="w.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="调入仓库" prop="toWarehouseId">
              <el-select
                v-model="transferForm.toWarehouseId"
                v-load-more="{ popperClass: 'inventory-shared-warehouse-dropdown', onLoadMore: loadMoreWarehouseFilterOptions, disabled: warehouseFilterOptionsLoading || !warehouseFilterOptionsHasMore }"
                popper-class="inventory-shared-warehouse-dropdown"
                placeholder="请选择仓库"
                style="width: 100%"
                filterable
                @visible-change="onWarehouseDropdownVisibleChange"
                @filter-method="onWarehouseFilter"
              >
                <el-option v-for="w in warehouseFilterOptions" :key="w.id" :label="w.name" :value="w.id" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="选择商品" prop="productId">
              <el-select
                v-model="transferForm.productId"
                v-load-more="{ popperClass: 'inventory-transfer-product-dropdown', onLoadMore: loadMoreProductOptions, disabled: productOptionsLoading || !productOptionsHasMore }"
                popper-class="inventory-transfer-product-dropdown"
                placeholder="请选择商品"
                style="width: 100%"
                filterable
                @visible-change="onProductSelectVisibleChange"
                @filter-method="onProductFilter"
              >
                <el-option v-for="p in productOptions" :key="p.id" :label="p.name" :value="p.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="商品图片">
              <div class="selected-product-image">
                <ProductImageThumb
                  v-if="transferProductImage"
                  :src="transferProductImage"
                  :preview-src-list="productPreviewListByFormProductId(transferForm.productId)"
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
        <el-form-item label="调拨数量" prop="quantity">
          <el-input-number v-model="transferForm.quantity" :min="1" style="width: 100%" />
        </el-form-item>
        <el-form-item label="调拨说明" prop="remark">
          <el-input
            v-model="transferForm.remark"
            type="textarea"
            :rows="3"
            :maxlength="REMARK_MAX_LENGTH"
            show-word-limit
            placeholder="输入调拨说明"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="transferDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitTransfer" :loading="loading">确认调拨</el-button>
      </template>
    </el-dialog>

    <!-- 调拨详情对话框 -->
    <el-dialog v-model="transferDetailVisible" class="transfer-detail-dialog" title="调拨单详情" width="600px" destroy-on-close>
      <el-descriptions class="transfer-detail-descriptions" :column="2" border>
        <el-descriptions-item label="调拨单号"><span class="order-no">{{ currentTransfer?.orderNo }}</span></el-descriptions-item>
        <el-descriptions-item label="SKU编码">{{ currentTransfer?.sku }}</el-descriptions-item>
        <el-descriptions-item label="商品名称" :span="2">{{ currentTransfer?.productName }}</el-descriptions-item>
        <el-descriptions-item label="调出仓库"><span class="warehouse-name">{{ getWarehouseName(currentTransfer?.fromWarehouseId) || currentTransfer?.fromWarehouseName }}</span></el-descriptions-item>
        <el-descriptions-item label="调入仓库"><span class="warehouse-name">{{ getWarehouseName(currentTransfer?.toWarehouseId) || currentTransfer?.toWarehouseName }}</span></el-descriptions-item>
        <el-descriptions-item label="调拨数量">{{ currentTransfer?.quantity }} 件</el-descriptions-item>
        <el-descriptions-item label="可调拨数量">
          <span :class="{ 'low-stock': transferDetailSourceStock < currentTransfer?.quantity }">{{ transferDetailSourceStock }} 件</span>
        </el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="getTransferStatusType(currentTransfer?.status)" effect="light">{{ formatTransferStatus(currentTransfer?.status) }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ currentTransfer?.createTime }}</el-descriptions-item>
        <el-descriptions-item label="操作人">{{ currentTransfer?.operatorName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="完成时间">{{ currentTransfer?.completeTime || '-' }}</el-descriptions-item>
      </el-descriptions>
      <div class="transfer-remark-panel">
        <div class="transfer-remark-title">备注</div>
        <div class="transfer-remark-content">{{ currentTransfer?.remark || '-' }}</div>
      </div>
      <template #footer>
        <el-button @click="transferDetailVisible = false">关闭</el-button>
        <el-button
          v-if="canTransferOp && currentTransfer?.status === 'pending'"
          type="success"
          @click="confirmTransfer(currentTransfer)"
        >确认调拨</el-button>
      </template>
    </el-dialog>

    <!-- 仓库编辑对话框 -->
    <el-dialog v-model="warehouseDialogVisible" :title="warehouseEditMode ? '编辑仓库' : '添加仓库'" width="600px" destroy-on-close>
      <el-form ref="warehouseFormRef" :model="warehouseForm" :rules="warehouseRules" label-width="100px">
        <el-form-item label="仓库名称" prop="name">
          <el-input v-model="warehouseForm.name" placeholder="输入仓库名称" maxlength="100" show-word-limit />
        </el-form-item>
        <el-form-item label="仓库编码" prop="code">
          <el-input v-model="warehouseForm.code" placeholder="输入仓库编码" maxlength="20" show-word-limit />
        </el-form-item>
        <el-form-item label="仓库地址" prop="address">
          <el-input v-model="warehouseForm.address" placeholder="输入仓库地址" maxlength="200" show-word-limit />
        </el-form-item>
        <el-form-item label="容量利用率">
          <el-slider v-model="warehouseForm.capacity" :min="0" :max="100" show-input />
        </el-form-item>
        <el-form-item label="负责人" prop="managerName">
          <el-input v-model="warehouseForm.managerName" placeholder="输入负责人姓名" maxlength="50" show-word-limit />
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="warehouseForm.remark" type="textarea" :rows="3" placeholder="输入备注" maxlength="500" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="warehouseDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitWarehouse">{{ warehouseEditMode ? '保存修改' : '确认添加' }}</el-button>
      </template>
    </el-dialog>

    <!-- 仓库详情对话框 -->
    <el-dialog v-model="warehouseDetailVisible" class="warehouse-detail-dialog" title="仓库详情" width="700px" destroy-on-close>
      <el-descriptions class="warehouse-detail-descriptions" :column="2" border>
        <el-descriptions-item label="仓库名称">{{ currentWarehouse?.name }}</el-descriptions-item>
        <el-descriptions-item label="仓库编码">{{ currentWarehouse?.code || '-' }}</el-descriptions-item>
        <el-descriptions-item label="仓库地址" :span="2">
          <div class="descriptions-long-text">{{ currentWarehouse?.address || '-' }}</div>
        </el-descriptions-item>
        <el-descriptions-item label="负责人">{{ currentWarehouse?.managerName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="currentWarehouse?.status === 1 ? 'success' : 'danger'" effect="light">{{ currentWarehouse?.status === 1 ? '正常' : '停用' }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="商品种类">{{ currentWarehouse?.totalCategories || 0 }} 种</el-descriptions-item>
        <el-descriptions-item label="总库存">{{ currentWarehouse?.totalStock || 0 }} 件</el-descriptions-item>
        <el-descriptions-item label="库存价值"><span class="amount">¥{{ ((currentWarehouse?.totalValue || 0) / 10000).toFixed(1) }}万</span></el-descriptions-item>
        <el-descriptions-item label="利用率">
          <span :style="{ color: (currentWarehouse?.capacityUsed || 50) > 70 ? '#67C23A' : '#E6A23C' }">{{ (currentWarehouse?.capacityUsed || 50).toFixed(0) }}%</span>
        </el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ currentWarehouse?.createTime }}</el-descriptions-item>
        <el-descriptions-item label="更新时间">{{ currentWarehouse?.updateTime }}</el-descriptions-item>
      </el-descriptions>
      <div class="warehouse-remark-panel">
        <div class="warehouse-remark-title">备注</div>
        <div class="warehouse-remark-content">{{ currentWarehouse?.remark || '-' }}</div>
      </div>

      <!-- 仓库内库存商品列表 -->
      <el-divider content-position="left">库存商品</el-divider>
      <el-table :data="warehouseDetailInventoryRows" empty-text="暂无数据" style="width: 100%" max-height="300" size="small">
        <el-table-column label="SKU" width="100">
          <template #default="{ row }"><span class="sku">{{ row.sku }}</span></template>
        </el-table-column>
        <el-table-column label="商品名称" prop="productName" min-width="140" show-overflow-tooltip />
        <el-table-column label="库存" width="80" align="center">
          <template #default="{ row }">
            <span :class="{ 'low-stock': row.stock < row.safeStock }">{{ row.stock }}</span>
          </template>
        </el-table-column>
        <el-table-column label="安全库存" prop="safeStock" width="80" align="center" />
        <el-table-column label="库位" prop="location" width="100" show-overflow-tooltip />
        <el-table-column label="状态" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="getStockStatusType(row.status)" effect="light" size="small">{{ formatStockStatus(row.status) }}</el-tag>
          </template>
        </el-table-column>
      </el-table>
      <div class="pagination-wrapper" v-if="warehouseDetailInventoryTotal > 0">
        <el-pagination
          v-model:current-page="warehouseInventoryCurrentPage"
          v-model:page-size="warehouseInventoryPageSize"
          :total="warehouseDetailInventoryTotal"
          :page-sizes="[5, 10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          small
        />
      </div>

      <template #footer>
        <el-button @click="warehouseDetailVisible = false">关闭</el-button>
        <el-button v-if="canInventoryWarehouse" type="primary" @click="editWarehouse(currentWarehouse)">编辑</el-button>
      </template>
    </el-dialog>

    <!-- 库存详情对话框 -->
    <el-dialog v-model="stockDetailVisible" title="库存详情" width="700px" destroy-on-close>
      <!-- 商品图片展示 -->
      <div class="stock-product-image" v-if="getProductImage(currentStock?.productId, currentStock?.productImage || currentStock?.image)">
        <ProductImageThumb
          :src="getProductImage(currentStock?.productId, currentStock?.productImage || currentStock?.image)"
          :preview-src-list="parseProductImageUrls(currentStock?.productImage || currentStock?.image)"
          class="product-image-preview"
          :width="100"
          :height="100"
          :radius="8"
        />
        <div class="product-image-info">
          <h4>{{ currentStock?.productName || currentStock?.name }}</h4>
          <p>SKU: {{ currentStock?.sku }}</p>
        </div>
      </div>
      <el-descriptions :column="2" border>
        <el-descriptions-item label="SKU编码"><span class="sku">{{ currentStock?.sku }}</span></el-descriptions-item>
        <el-descriptions-item label="商品名称">{{ currentStock?.productName || currentStock?.name }}</el-descriptions-item>
        <el-descriptions-item label="规格">{{ currentStock?.spec || '-' }}</el-descriptions-item>
        <el-descriptions-item label="分类">{{ currentStock?.category || '-' }}</el-descriptions-item>
        <el-descriptions-item label="所属仓库"><span class="warehouse-name">{{ getWarehouseName(currentStock?.warehouseId) || currentStock?.warehouseName }}</span></el-descriptions-item>
        <el-descriptions-item label="库位">
          <el-input v-model="stockLocationEdit" size="small" style="width: 120px" placeholder="输入库位" :disabled="!canInventoryAdjust" />
          <el-button v-if="canInventoryAdjust" type="primary" size="small" @click="updateLocation" style="margin-left: 8px">更新</el-button>
        </el-descriptions-item>
        <el-descriptions-item label="当前库存">
          <span :class="{ 'low-stock': currentStock?.stock < currentStock?.safeStock }">{{ currentStock?.stock }} 件</span>
        </el-descriptions-item>
        <el-descriptions-item label="库存预警值">
          <el-input-number v-model="stockSafeStockEdit" :min="0" size="small" style="width: 100px" :disabled="!canInventoryAdjust" />
          <el-button v-if="canInventoryAdjust" type="primary" size="small" @click="updateSafeStock" style="margin-left: 8px">更新</el-button>
        </el-descriptions-item>
        <el-descriptions-item label="呆滞预警天数">
          <el-input-number v-model="stockStagnantDaysEdit" :min="1" size="small" style="width: 100px" :disabled="!canInventoryAdjust" />
          <el-button v-if="canInventoryAdjust" type="primary" size="small" @click="updateStagnantDays" style="margin-left: 8px">更新</el-button>
        </el-descriptions-item>
        <el-descriptions-item label="呆滞状态">
          <el-tag :type="getStagnantStatusType(currentStock)" effect="light">{{ getStagnantStatusText(currentStock) }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="成本价">¥{{ formatAmountDisplay(currentStock?.costPrice ?? 0) }}</el-descriptions-item>
        <el-descriptions-item label="库存价值"><span class="amount">¥{{ formatAmountDisplay(currentStock?.stockValue ?? 0) }}</span></el-descriptions-item>
        <el-descriptions-item label="库存状态">
          <el-tag :type="getStockStatusType(currentStock?.status)" effect="light">{{ formatStockStatus(currentStock?.status) }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="最后入库">{{ currentStock?.lastInboundTime || '-' }}</el-descriptions-item>
        <el-descriptions-item label="最后出库">{{ currentStock?.lastOutboundTime || '-' }}</el-descriptions-item>
      </el-descriptions>

      <!-- 入库记录 -->
      <el-divider content-position="left">入库记录</el-divider>
      <el-table :data="stockInboundRecords" empty-text="暂无数据" style="width: 100%" max-height="200" size="small">
        <el-table-column label="入库单号" width="120">
          <template #default="{ row }"><span class="order-no success">{{ row.orderNo }}</span></template>
        </el-table-column>
        <el-table-column label="数量" width="80" align="center">
          <template #default="{ row }">{{ row.quantity }}</template>
        </el-table-column>
        <el-table-column label="时间" width="176" show-overflow-tooltip>
          <template #default="{ row }">{{ row.createTime }}</template>
        </el-table-column>
        <el-table-column label="操作人">
          <template #default="{ row }">{{ row.operatorName || '-' }}</template>
        </el-table-column>
      </el-table>

      <!-- 出库记录 -->
      <el-divider content-position="left">出库记录</el-divider>
      <el-table :data="stockOutboundRecords" empty-text="暂无数据" style="width: 100%" max-height="200" size="small">
        <el-table-column label="出库单号" width="120">
          <template #default="{ row }"><span class="order-no warning">{{ row.orderNo }}</span></template>
        </el-table-column>
        <el-table-column label="数量" width="80" align="center">
          <template #default="{ row }">{{ row.quantity }}</template>
        </el-table-column>
        <el-table-column label="时间" width="176" show-overflow-tooltip>
          <template #default="{ row }">{{ row.createTime }}</template>
        </el-table-column>
        <el-table-column label="操作人">
          <template #default="{ row }">{{ row.operatorName || '-' }}</template>
        </el-table-column>
      </el-table>

      <template #footer>
        <el-button @click="stockDetailVisible = false">关闭</el-button>
        <el-button v-if="canInventoryInbound" type="success" @click="openStockInbound(currentStock)">入库</el-button>
        <el-button v-if="canInventoryOutbound" type="warning" @click="openStockOutbound(currentStock)">出库</el-button>
        <el-button
          v-if="canShowCreatePurchaseInStockDetail"
          type="primary"
          @click="openPurchaseFromStock(currentStock)"
        >创建采购</el-button>
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
                v-load-more="{ popperClass: 'inventory-purchase-supplier-dropdown', onLoadMore: loadMoreSupplierPurchaseOptions, disabled: supplierPurchaseOptionsLoading || !supplierPurchaseOptionsHasMore }"
                popper-class="inventory-purchase-supplier-dropdown"
                placeholder="请选择供应商"
                style="width: 100%"
                filterable
                @visible-change="onSupplierPurchaseVisibleChange"
                @filter-method="onSupplierPurchaseFilter"
              >
                <el-option v-for="s in supplierPurchaseOptions" :key="s.id" :label="s.name" :value="s.id" />
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
          <el-input v-model="purchaseForm.remark" :maxlength="REMARK_MAX_LENGTH" show-word-limit placeholder="输入采购备注" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="purchaseDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitPurchase">确认创建</el-button>
      </template>
    </el-dialog>

    <!-- 入库对话框 -->
    <el-dialog v-model="inboundDialogVisible" title="新建入库" width="500px" destroy-on-close>
      <el-alert v-if="pendingPurchaseOrders.length === 0" type="warning" title="当前没有待入库的采购单" description="请先在采购管理中创建采购单，再进行入库操作。" :closable="false" show-icon style="margin-bottom: 16px" />
      <el-form ref="inboundFormRef" :model="inboundForm" :rules="inboundRules" label-width="100px">
        <el-form-item label="关联采购单" prop="purchaseOrderId">
          <el-select
            v-model="inboundForm.purchaseOrderId"
            v-load-more="{ popperClass: 'inventory-inbound-po-dropdown', onLoadMore: loadMorePurchaseOrderOptions, disabled: purchaseOrderOptionsLoading || !purchaseOrderOptionsHasMore }"
            popper-class="inventory-inbound-po-dropdown"
            placeholder="请选择采购单"
            style="width: 100%"
            filterable
            @change="onPurchaseOrderChange"
            @visible-change="onPurchaseOrderSelectVisibleChange"
            @filter-method="onPurchaseOrderFilter"
            :disabled="pendingPurchaseOrders.length === 0"
          >
            <el-option v-for="po in pendingPurchaseOrders" :key="po.id" :label="`#${po.orderNo} - ${po.supplierName || ''} - ${po.productName || ''} - 待入库${po.pendingQuantity || (po.totalQuantity - (po.inboundQuantity || 0))}件`" :value="po.id" />
          </el-select>
        </el-form-item>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="入库仓库" prop="warehouseId">
              <el-select
                v-model="inboundForm.warehouseId"
                v-load-more="{ popperClass: 'inventory-shared-warehouse-dropdown', onLoadMore: loadMoreWarehouseFilterOptions, disabled: warehouseFilterOptionsLoading || !warehouseFilterOptionsHasMore }"
                popper-class="inventory-shared-warehouse-dropdown"
                style="width: 100%"
                filterable
                @visible-change="onWarehouseDropdownVisibleChange"
                @filter-method="onWarehouseFilter"
              >
                <el-option v-for="w in warehouseFilterOptions" :key="w.id" :label="w.name" :value="w.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="入库数量" prop="quantity">
              <el-input-number v-model="inboundForm.quantity" :min="1" :max="inboundForm.maxQuantity || 9999" style="width: 100%" />
              <div class="quantity-tip" v-if="inboundForm.maxQuantity">最大可入库: {{ inboundForm.maxQuantity }} 件</div>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="入库备注">
          <el-input v-model="inboundForm.remark" :maxlength="REMARK_MAX_LENGTH" show-word-limit placeholder="输入入库备注" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="inboundDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitInbound" :loading="loading" :disabled="pendingPurchaseOrders.length === 0">确认入库</el-button>
      </template>
    </el-dialog>

    <!-- 库存入库对话框 - 支持两种方式 -->
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
            v-load-more="{ popperClass: 'inventory-stock-inbound-po-dropdown', onLoadMore: loadMorePurchaseOrderOptions, disabled: purchaseOrderOptionsLoading || !purchaseOrderOptionsHasMore }"
            popper-class="inventory-stock-inbound-po-dropdown"
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
          <el-input v-model="stockInboundForm.remark" :maxlength="REMARK_MAX_LENGTH" show-word-limit placeholder="输入入库备注" />
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
          <el-input v-model="stockOtherInboundForm.remark" :maxlength="REMARK_MAX_LENGTH" show-word-limit placeholder="输入入库备注" />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="stockInboundVisible = false">取消</el-button>
        <el-button type="primary" @click="submitStockInbound" :loading="loading" :disabled="stockInboundType === 'purchase' && stockPendingPurchaseOrders.length === 0">确认入库</el-button>
      </template>
    </el-dialog>

    <!-- 库存出库对话框 - 支持两种方式 -->
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
            v-load-more="{ popperClass: 'inventory-stock-outbound-so-dropdown', onLoadMore: loadMoreSalesOrderOptions, disabled: salesOrderOptionsLoading || !salesOrderOptionsHasMore }"
            popper-class="inventory-stock-outbound-so-dropdown"
            placeholder="请选择销售单"
            style="width: 100%"
            filterable
            @change="onStockSalesOrderChange"
            @visible-change="onSalesOrderSelectVisibleChange"
            @filter-method="onSalesOrderFilter"
            :disabled="stockPendingSalesOrders.length === 0"
          >
            <el-option
              v-for="so in stockPendingSalesOrders"
              :key="so.id"
              :label="`#${so.orderNo} - ${so.customerName || ''} - ${so.productName || ''} - 待发货${so.pendingQuantity || so.quantity}件`"
              :value="so.id"
            >
              <div class="sales-order-option">
                <img
                  v-if="resolveSalesOrderImage(so)"
                  class="sales-order-option__thumb"
                  :src="resolveSalesOrderImage(so)"
                  alt="商品图"
                >
                <div v-else class="sales-order-option__thumb-placeholder">📦</div>
                <div class="sales-order-option__meta">
                  <span class="sales-order-option__text">
                    #{{ so.orderNo }} · {{ so.customerName || '未命名客户' }} · {{ so.productName || '未命名商品' }} · 待发货{{ so.pendingQuantity || so.quantity }}件
                  </span>
                </div>
              </div>
            </el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="出库数量" prop="quantity">
          <el-input-number v-model="stockOutboundForm.quantity" :min="1" :max="stockOutboundForm.maxQuantity || 9999" style="width: 100%" />
          <div class="quantity-tip" v-if="stockOutboundForm.maxQuantity">最大可出库: {{ stockOutboundForm.maxQuantity }} 件</div>
        </el-form-item>
        <el-form-item label="出库备注">
          <el-input v-model="stockOutboundForm.remark" :maxlength="REMARK_MAX_LENGTH" show-word-limit placeholder="输入出库备注" />
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
          <el-input v-model="stockOtherOutboundForm.remark" :maxlength="REMARK_MAX_LENGTH" show-word-limit placeholder="输入出库原因（必填）" />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="stockOutboundVisible = false">取消</el-button>
        <el-button type="primary" @click="submitStockOutbound" :loading="loading" :disabled="stockOutboundType === 'sales' && stockPendingSalesOrders.length === 0">确认出库</el-button>
      </template>
    </el-dialog>

    <!-- 手动入库对话框 - 支持两种方式 -->
    <el-dialog v-model="manualInboundVisible" title="手动入库" width="650px" destroy-on-close>
      <el-radio-group v-model="manualInboundType" style="margin-bottom: 20px">
        <el-radio value="purchase">采购入库（关联采购单）</el-radio>
        <el-radio value="other">其他入库（关联商品）</el-radio>
      </el-radio-group>

      <!-- 其他入库表单 -->
      <el-form v-if="manualInboundType === 'other'" ref="manualInboundFormRef" :model="manualInboundForm" :rules="manualInboundRules" label-width="100px">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="选择商品" prop="productId">
              <el-select
                v-model="manualInboundForm.productId"
                v-load-more="{ popperClass: 'inventory-manual-inbound-product-dropdown', onLoadMore: loadMoreProductOptions, disabled: productOptionsLoading || !productOptionsHasMore }"
                popper-class="inventory-manual-inbound-product-dropdown"
                placeholder="请选择商品"
                style="width: 100%"
                filterable
                @visible-change="onProductSelectVisibleChange"
                @filter-method="onProductFilter"
              >
                <el-option v-for="p in productsForManualOtherInbound" :key="p.id" :label="p.name" :value="p.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="商品图片">
              <div class="selected-product-image">
                <ProductImageThumb
                  v-if="manualInboundProductImage"
                  :src="manualInboundProductImage"
                  :preview-src-list="productPreviewListByFormProductId(manualInboundForm.productId)"
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
        <el-form-item label="入库仓库" prop="warehouseId">
          <el-select
            v-model="manualInboundForm.warehouseId"
            v-load-more="{ popperClass: 'inventory-shared-warehouse-dropdown', onLoadMore: loadMoreWarehouseFilterOptions, disabled: warehouseFilterOptionsLoading || !warehouseFilterOptionsHasMore }"
            popper-class="inventory-shared-warehouse-dropdown"
            placeholder="请选择仓库"
            style="width: 100%"
            filterable
            @visible-change="onWarehouseDropdownVisibleChange"
            @filter-method="onWarehouseFilter"
          >
            <el-option v-for="w in warehouseFilterOptions" :key="w.id" :label="w.name" :value="w.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="入库数量" prop="quantity">
          <el-input-number v-model="manualInboundForm.quantity" :min="1" style="width: 100%" />
        </el-form-item>
        <el-form-item label="入库备注">
          <el-input v-model="manualInboundForm.remark" :maxlength="REMARK_MAX_LENGTH" show-word-limit placeholder="输入入库备注" />
        </el-form-item>
      </el-form>

      <!-- 采购入库表单 -->
      <el-form v-if="manualInboundType === 'purchase'" ref="purchaseInboundFormRef" :model="purchaseInboundForm" :rules="purchaseInboundRules" label-width="100px">
        <el-alert v-if="pendingPurchaseOrders.length === 0" type="warning" title="当前没有待入库的采购单" description="请先在采购管理中创建采购单。" :closable="false" show-icon style="margin-bottom: 16px" />
        <el-form-item label="采购单" prop="purchaseOrderId">
          <el-select
            v-model="purchaseInboundForm.purchaseOrderId"
            v-load-more="{ popperClass: 'inventory-manual-inbound-po-dropdown', onLoadMore: loadMorePurchaseOrderOptions, disabled: purchaseOrderOptionsLoading || !purchaseOrderOptionsHasMore }"
            popper-class="inventory-manual-inbound-po-dropdown"
            placeholder="请选择采购单"
            style="width: 100%"
            filterable
            @change="onPurchaseInboundOrderChange"
            @visible-change="onPurchaseOrderSelectVisibleChange"
            @filter-method="onPurchaseOrderFilter"
            :disabled="pendingPurchaseOrders.length === 0"
          >
            <el-option v-for="po in pendingPurchaseOrders" :key="po.id" :label="`#${po.orderNo} - ${po.supplierName || ''} - ${po.productName || ''} - 待入库${po.pendingQuantity || (po.quantity - (po.inboundQuantity || 0))}件`" :value="po.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="入库仓库" prop="warehouseId">
          <el-select
            v-model="purchaseInboundForm.warehouseId"
            v-load-more="{ popperClass: 'inventory-shared-warehouse-dropdown', onLoadMore: loadMoreWarehouseFilterOptions, disabled: warehouseFilterOptionsLoading || !warehouseFilterOptionsHasMore }"
            popper-class="inventory-shared-warehouse-dropdown"
            placeholder="请选择仓库"
            style="width: 100%"
            filterable
            @visible-change="onWarehouseDropdownVisibleChange"
            @filter-method="onWarehouseFilter"
          >
            <el-option v-for="w in warehouseFilterOptions" :key="w.id" :label="w.name" :value="w.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="入库数量" prop="quantity">
          <el-input-number v-model="purchaseInboundForm.quantity" :min="1" :max="purchaseInboundForm.maxQuantity || 9999" style="width: 100%" />
          <div class="quantity-tip" v-if="purchaseInboundForm.maxQuantity">最大可入库: {{ purchaseInboundForm.maxQuantity }} 件</div>
        </el-form-item>
        <el-form-item label="入库备注">
          <el-input v-model="purchaseInboundForm.remark" :maxlength="REMARK_MAX_LENGTH" show-word-limit placeholder="输入入库备注" />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="manualInboundVisible = false">取消</el-button>
        <el-button type="primary" @click="submitManualInbound" :loading="loading" :disabled="manualInboundType === 'purchase' && pendingPurchaseOrders.length === 0">确认入库</el-button>
      </template>
    </el-dialog>

    <!-- 手动出库对话框 - 支持两种方式 -->
    <el-dialog v-model="manualOutboundVisible" title="手动出库" width="650px" destroy-on-close>
      <el-radio-group v-model="manualOutboundType" style="margin-bottom: 20px">
        <el-radio value="sales">销售出库（关联销售订单）</el-radio>
        <el-radio value="other">其他出库（选择库存）</el-radio>
      </el-radio-group>

      <!-- 销售出库表单 -->
      <el-form v-if="manualOutboundType === 'sales'" ref="salesOutboundFormRef" :model="salesOutboundForm" :rules="salesOutboundRules" label-width="100px">
        <el-alert v-if="pendingSalesOrders.length === 0" type="warning" title="当前没有待发货的销售订单" description="请先在销售管理中创建销售订单并确认付款。" :closable="false" show-icon style="margin-bottom: 16px" />
        <el-form-item label="销售订单" prop="salesOrderId">
          <el-select
            v-model="salesOutboundForm.salesOrderId"
            v-load-more="{ popperClass: 'inventory-manual-outbound-so-dropdown', onLoadMore: loadMoreSalesOrderOptions, disabled: salesOrderOptionsLoading || !salesOrderOptionsHasMore }"
            popper-class="inventory-manual-outbound-so-dropdown"
            placeholder="请选择销售订单"
            style="width: 100%"
            filterable
            @change="onSalesOutboundOrderChange"
            @visible-change="onSalesOrderSelectVisibleChange"
            @filter-method="onSalesOrderFilter"
            :disabled="pendingSalesOrders.length === 0"
          >
            <el-option
              v-for="so in pendingSalesOrders"
              :key="so.id"
              :label="`#${so.orderNo} - ${so.customerName || ''} - ${so.productName || ''} - 待发货${so.pendingQuantity || so.quantity}件`"
              :value="so.id"
            >
              <div class="sales-order-option">
                <img
                  v-if="resolveSalesOrderImage(so)"
                  class="sales-order-option__thumb"
                  :src="resolveSalesOrderImage(so)"
                  alt="商品图"
                >
                <div v-else class="sales-order-option__thumb-placeholder">📦</div>
                <div class="sales-order-option__meta">
                  <span class="sales-order-option__text">
                    #{{ so.orderNo }} · {{ so.customerName || '未命名客户' }} · {{ so.productName || '未命名商品' }} · 待发货{{ so.pendingQuantity || so.quantity }}件
                  </span>
                </div>
              </div>
            </el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="出库仓库" prop="warehouseId">
          <el-select
            v-model="salesOutboundForm.warehouseId"
            placeholder="请选择仓库"
            style="width: 100%"
            filterable
            :loading="salesOutboundWarehouseLoading"
            :disabled="!salesOutboundForm.salesOrderId || salesOutboundWarehouseOptions.length === 0"
          >
            <el-option
              v-for="w in salesOutboundWarehouseOptions"
              :key="w.id"
              :label="`${w.name}（库存${w.availableStock}件）`"
              :value="w.id"
            />
          </el-select>
          <div
            v-if="salesOutboundForm.salesOrderId && !salesOutboundWarehouseLoading && salesOutboundWarehouseOptions.length === 0"
            class="quantity-tip"
          >
            当前没有满足出库数量的仓库，请减少出库数量后重试
          </div>
        </el-form-item>
        <el-form-item label="出库数量" prop="quantity">
          <el-input-number
            v-model="salesOutboundForm.quantity"
            :min="1"
            :max="salesOutboundForm.maxQuantity || 9999"
            style="width: 100%"
            @change="onSalesOutboundQuantityChange"
          />
          <div class="quantity-tip" v-if="salesOutboundForm.maxQuantity">最大可出库: {{ salesOutboundForm.maxQuantity }} 件</div>
        </el-form-item>
        <el-form-item label="出库备注">
          <el-input v-model="salesOutboundForm.remark" :maxlength="REMARK_MAX_LENGTH" show-word-limit placeholder="输入出库备注" />
        </el-form-item>
      </el-form>

      <!-- 其他出库表单 -->
      <el-form v-if="manualOutboundType === 'other'" ref="otherOutboundFormRef" :model="otherOutboundForm" :rules="otherOutboundRules" label-width="100px">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="选择库存" prop="inventoryId">
              <el-select
                v-model="otherOutboundForm.inventoryId"
                v-load-more="{ popperClass: 'inventory-manual-outbound-inventory-dropdown', onLoadMore: loadMoreInventoryOptions, disabled: inventoryOptionsLoading || !inventoryOptionsHasMore }"
                popper-class="inventory-manual-outbound-inventory-dropdown"
                placeholder="请选择库存记录"
                style="width: 100%"
                filterable
                @change="onOtherOutboundInventoryChange"
                @visible-change="onInventorySelectVisibleChange"
                @filter-method="onInventoryFilter"
              >
                <el-option
                  v-for="inv in inventoryOptionsLookup"
                  :key="inv.id"
                  :label="`${inv.productName} - ${getWarehouseName(inv.warehouseId) || inv.warehouseName}`"
                  :value="inv.id"
                  :disabled="inv.stock <= 0"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="商品图片">
              <div class="selected-product-image">
                <ProductImageThumb
                  v-if="otherOutboundProductImage"
                  :src="otherOutboundProductImage"
                  :preview-src-list="productPreviewListByFormProductId(otherOutboundForm.productId)"
                  class="product-preview"
                  :width="80"
                  :height="80"
                  :radius="8"
                />
                <div v-else class="product-preview-placeholder">
                  <span class="placeholder-icon">📦</span>
                  <span class="placeholder-text">请先选择库存</span>
                </div>
              </div>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="当前库存">
          <el-input :value="otherOutboundForm.currentStock ? `${otherOutboundForm.currentStock} 件` : ''" disabled />
        </el-form-item>
        <el-form-item label="出库数量" prop="quantity">
          <el-input-number v-model="otherOutboundForm.quantity" :min="1" :max="otherOutboundForm.maxQuantity || 9999" style="width: 100%" />
          <div class="quantity-tip" v-if="otherOutboundForm.maxQuantity">最大可出库: {{ otherOutboundForm.maxQuantity }} 件</div>
        </el-form-item>
        <el-form-item label="出库备注" prop="remark">
          <el-input v-model="otherOutboundForm.remark" :maxlength="REMARK_MAX_LENGTH" show-word-limit placeholder="输入出库原因（必填）" />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="manualOutboundVisible = false">取消</el-button>
        <el-button
          type="primary"
          @click="submitManualOutbound"
          :loading="loading"
          :disabled="manualOutboundType === 'sales' && (pendingSalesOrders.length === 0 || salesOutboundWarehouseOptions.length === 0)"
        >
          确认出库
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useDataStore } from '@/stores/data'
import { useUserStore } from '@/stores/user'
import { inventoryApi, warehouseApi, purchaseApi, salesApi, productApi, supplierApi, systemConfigApi } from '@/api'
import ProductImageThumb from '@/components/ProductImageThumb.vue'
import { firstProductImageUrl, parseProductImageUrls } from '@/utils/productImages'
import { MAX_IMAGE_UPLOAD_BYTES } from '@/utils/uploadLimits'
import { formatAmountDisplay } from '@/utils/moneyFormat'
import { isProductSelectableForOrder, canShortcutPurchaseForStock } from '@/utils/productStatus'

const router = useRouter()
const route = useRoute()
const dataStore = useDataStore()
const userStore = useUserStore()

const hasInventoryMenu = computed(() => userStore.hasPermission('inventory'))

/** 与后端 GET /inventory 可读范围一致，用于列表、以图搜图等 */
const canInventoryRead = computed(() =>
  hasInventoryMenu.value ||
  userStore.hasPermission('inventory:view') ||
  userStore.hasPermission('inventory:records') ||
  userStore.hasPermission('inventory:transfer') ||
  userStore.hasPermission('inventory:warehouse') ||
  userStore.hasPermission('inventory:inbound') ||
  userStore.hasPermission('inventory:outbound') ||
  userStore.hasPermission('inventory:adjust') ||
  userStore.hasPermission('sales') ||
  userStore.hasPermission('sales:view') ||
  userStore.hasPermission('purchase') ||
  userStore.hasPermission('purchase:view')
)
/** 各 Tab 与角色里「库存查看 / 出入库记录 / 仓库管理 / 调拨」等子权限对齐 */
const canInventoryStockTab = computed(
  () =>
    hasInventoryMenu.value ||
    userStore.hasPermission('inventory:view') ||
    userStore.hasPermission('inventory:transfer') ||
    userStore.hasPermission('inventory:inbound') ||
    userStore.hasPermission('inventory:outbound') ||
    userStore.hasPermission('inventory:warehouse') ||
    userStore.hasPermission('inventory:adjust') ||
    userStore.hasPermission('purchase') ||
    userStore.hasPermission('purchase:view') ||
    userStore.hasPermission('sales') ||
    userStore.hasPermission('sales:view')
)
/**
 * 出入库记录 Tab：仅 inventory:records（勿并入 in/out 操作码：仅有入库/出库操作权也会误显本 Tab）
 */
const canInventoryRecordsTab = computed(() => userStore.hasPermission('inventory:records'))
/** 仓库管理 Tab：仅 inventory:warehouse（父级菜单码 alone 不解锁） */
const canInventoryWarehouseTab = computed(() => userStore.hasPermission('inventory:warehouse'))
const canInventoryTransferTab = computed(() => userStore.hasPermission('inventory:transfer'))
/** 菜单码 inventory 仍表示「全部库存能力」；否则按细粒度码控制 */
const canInventoryWarehouse = computed(
  () => hasInventoryMenu.value || userStore.hasPermission('inventory:warehouse')
)
const canInventoryInbound = computed(
  () => hasInventoryMenu.value || userStore.hasPermission('inventory:inbound')
)
const canInventoryOutbound = computed(
  () => hasInventoryMenu.value || userStore.hasPermission('inventory:outbound')
)
const canInventoryAdjust = computed(
  () => hasInventoryMenu.value || userStore.hasPermission('inventory:adjust')
)
const canTransferOp = computed(
  () => hasInventoryMenu.value || userStore.hasPermission('inventory:transfer')
)
const canPurchaseAdd = computed(() => userStore.hasPermission('purchase:add'))

/** 库存页筛选与商品下拉：固定每页 10 条，触底用 page++ 追加，不用放大 size */
const FILTER_DROPDOWN_PAGE_SIZE = 10
const stockFilterDropdownsPrimed = ref(false)

const MAIN_TABS = ['stock', 'transfer', 'records', 'warehouse']
const parseMainTab = (q) =>
  typeof q === 'string' && MAIN_TABS.includes(q) ? q : 'stock'
const RECORD_SUBTABS = ['inbound', 'outbound']
const parseRecordsSubTab = (q) =>
  typeof q === 'string' && RECORD_SUBTABS.includes(q) ? q : 'inbound'

const activeTab = ref(parseMainTab(route.query.tab))

const syncActiveTabToPermissions = () => {
  const allowed = {
    stock: canInventoryStockTab.value,
    transfer: canInventoryTransferTab.value,
    records: canInventoryRecordsTab.value,
    warehouse: canInventoryWarehouseTab.value
  }
  const cur = activeTab.value
  if (allowed[cur]) return
  if (allowed.stock) activeTab.value = 'stock'
  else if (allowed.records) activeTab.value = 'records'
  else if (allowed.transfer) activeTab.value = 'transfer'
  else if (allowed.warehouse) activeTab.value = 'warehouse'
}

syncActiveTabToPermissions()

const recordsSubTab = ref(parseRecordsSubTab(route.query.subtab))
const searchKeyword = ref('')
const queryImageDataUrl = ref('')
const queryImageFile = ref(null)
const imageSimilarityThreshold = ref(0.7)
const imageSearchMode = ref(false)
const imageSearchLoading = ref(false)
const imageSearchRows = ref([])
const imageSearchTotal = ref(0)
const filterProduct = ref(null)
const filterWarehouse = ref(null)
const filterStagnantStatus = ref(null)
/** 与系统设置「呆滞商品天数」一致，用于列表展示与阈值；未加载前默认 90 */
const systemStaleDays = ref(90)
let systemStaleDaysConfigFetched = false
const ensureSystemStaleDaysLoaded = async () => {
  if (systemStaleDaysConfigFetched) return
  systemStaleDaysConfigFetched = true
  try {
    const cfg = await systemConfigApi.get()
    if (cfg?.staleDays != null && cfg.staleDays >= 1) {
      systemStaleDays.value = cfg.staleDays
    }
  } catch {
    systemStaleDaysConfigFetched = false
  }
}
const filterLastOutboundRange = ref(null)
const filterLastInboundRange = ref(null)
const inventoryCurrentPage = ref(1)
const inventoryPageSize = ref(10)
const transferCurrentPage = ref(1)
const transferPageSize = ref(10)
const recordsInboundCurrentPage = ref(1)
const recordsInboundPageSize = ref(10)
const recordsOutboundCurrentPage = ref(1)
const recordsOutboundPageSize = ref(10)
/** 调拨记录筛选 */
const transferFilterKeyword = ref('')
const transferFilterStatus = ref(null)
const transferFilterWarehouseId = ref(null)
const transferFilterDateRange = ref(null)
/** 入库记录筛选 */
const recordsInboundFilterKeyword = ref('')
const recordsInboundFilterWarehouseId = ref(null)
const recordsInboundFilterDateRange = ref(null)
const recordsInboundFilterOperator = ref('')
/** 出库记录筛选 */
const recordsOutboundFilterKeyword = ref('')
const recordsOutboundFilterWarehouseId = ref(null)
const recordsOutboundFilterDateRange = ref(null)
const recordsOutboundFilterOperator = ref('')
const warehouseInventoryCurrentPage = ref(1)
const warehouseInventoryPageSize = ref(5)
const warehouseSearchKeyword = ref('')
const transferDialogVisible = ref(false)
const warehouseDialogVisible = ref(false)
const warehouseEditMode = ref(false)
const currentWarehouse = ref(null)
const warehouseDetailVisible = ref(false)
const stockDetailVisible = ref(false)
const purchaseDialogVisible = ref(false)
const inboundDialogVisible = ref(false)
const transferDetailVisible = ref(false)
const stockInboundVisible = ref(false)
const stockOutboundVisible = ref(false)
const stockInboundType = ref('purchase') // 入库类型：purchase 或 other
const stockOutboundType = ref('sales') // 出库类型：sales 或 other
const manualInboundVisible = ref(false)
const manualOutboundVisible = ref(false)
const manualInboundType = ref('purchase') // 入库类型：purchase 或 other
const manualOutboundType = ref('sales') // 出库类型：sales 或 other
const transferFormRef = ref()
const manualInboundFormRef = ref()
const purchaseInboundFormRef = ref()
const salesOutboundFormRef = ref()
const otherOutboundFormRef = ref()
const stockSafeStockEdit = ref(10)
const stockLocationEdit = ref('')
const stockStagnantDaysEdit = ref(90) // 默认呆滞天数
const warehouseFormRef = ref()
const purchaseFormRef = ref()
const inboundFormRef = ref()
const stockInboundFormRef = ref()
const stockOutboundFormRef = ref()
const stockOtherInboundFormRef = ref()
const stockOtherOutboundFormRef = ref()
const currentStock = ref(null)
const currentTransfer = ref(null)
const loading = ref(false)

// 统计数据
const inventoryStats = ref({
  categoryCount: 0,
  lastMonthCategoryCount: 0,
  categoryChange: 0,
  totalValue: 0,
  lastMonthTotalValue: 0,
  valueChangePercent: 0,
  warningCount: 0,
  transferringCount: 0,
  lastMonthTransferringCount: 0,
  transferringChange: 0
})

// 出入库流水仍走 dataStore；库存主表与列表展示以 inventory 分页接口为准，不再预拉商品/分类/全量仓库做兜底
const inboundRecords = computed(() => dataStore.inboundRecords || [])
const outboundRecords = computed(() => dataStore.outboundRecords || [])
const inboundRecordsTotal = computed(() => Number(dataStore.inboundRecordsTotal) || 0)
const outboundRecordsTotal = computed(() => Number(dataStore.outboundRecordsTotal) || 0)

/** 各主 Tab 内容区 v-loading，避免请求未返回时先闪现「暂无数据」 */
const stockTabLoading = computed(() => {
  if (activeTab.value !== 'stock') return false
  if (imageSearchMode.value) return imageSearchLoading.value
  return loading.value
})
const transferTabLoading = computed(() => activeTab.value === 'transfer' && loading.value)
const recordsTabLoading = computed(() => {
  if (activeTab.value !== 'records') return false
  if (loading.value) return true
  return Boolean(dataStore.inboundLoading || dataStore.outboundLoading)
})
const warehouseTabLoading = computed(() => activeTab.value === 'warehouse' && loading.value)

/** 下拉用：分页 GET /warehouses（每页 10 条，触底翻页），避免 options 一次拉全量超时 */
const warehouseFilterOptions = ref([])
const warehouseFilterOptionsPage = ref(0)
const warehouseFilterOptionsTotal = ref(0)
const warehouseFilterKeyword = ref('')
const warehouseFilterOptionsLoading = ref(false)
const warehouseFilterOptionsHasMore = computed(
  () => warehouseFilterOptions.value.length < warehouseFilterOptionsTotal.value
)
/** 仓库管理 Tab：GET /warehouses 列表 */
const warehouseGridList = ref([])
/** 创建采购弹窗：供应商下拉分页（与筛选区商品、仓库下拉一致） */
const supplierPurchaseOptions = ref([])
const supplierPurchaseOptionsPage = ref(0)
const supplierPurchaseOptionsTotal = ref(0)
const supplierPurchaseKeyword = ref('')
const supplierPurchaseOptionsLoading = ref(false)
const supplierPurchaseOptionsHasMore = computed(
  () => supplierPurchaseOptions.value.length < supplierPurchaseOptionsTotal.value
)
/** 仓库详情内库存表：按仓库分页 GET /inventory */
const warehouseDetailInventoryRows = ref([])
const warehouseDetailInventoryTotal = ref(0)
/** 调拨详情：源仓当前库存，按需 GET /inventory 单条条件查询 */
const transferDetailSourceStock = ref(0)

const filteredWarehouseGrid = computed(() => {
  const list = warehouseGridList.value
  const kw = warehouseSearchKeyword.value?.trim()
  if (!kw) return list
  return list.filter((w) => {
    const hay = [w.name, w.address, w.code, w.managerName].map((f) => (f == null ? '' : String(f))).join(' ')
    return hay.includes(kw)
  })
})

const resetWarehouseFilterOptions = () => {
  warehouseFilterOptions.value = []
  warehouseFilterOptionsPage.value = 0
  warehouseFilterOptionsTotal.value = 0
  warehouseFilterKeyword.value = ''
}

const loadMoreWarehouseFilterOptions = async ({ reset = false, keyword = null } = {}) => {
  if (warehouseFilterOptionsLoading.value) return
  if (reset) {
    warehouseFilterOptions.value = []
    warehouseFilterOptionsPage.value = 0
    warehouseFilterOptionsTotal.value = 0
    warehouseFilterKeyword.value = keyword ?? ''
  } else if (!warehouseFilterOptionsHasMore.value && warehouseFilterOptionsPage.value > 0) {
    return
  }
  warehouseFilterOptionsLoading.value = true
  try {
    const nextPage = warehouseFilterOptionsPage.value + 1
    const res = await warehouseApi.list({
      page: nextPage,
      size: FILTER_DROPDOWN_PAGE_SIZE,
      keyword: warehouseFilterKeyword.value || undefined
    })
    const rows = res?.list || []
    warehouseFilterOptions.value = mergeOptionsById(warehouseFilterOptions.value, rows)
    warehouseFilterOptionsPage.value = nextPage
    warehouseFilterOptionsTotal.value = Number(res?.total) || warehouseFilterOptions.value.length
    if (rows.length < FILTER_DROPDOWN_PAGE_SIZE) {
      warehouseFilterOptionsTotal.value = warehouseFilterOptions.value.length
    }
  } finally {
    warehouseFilterOptionsLoading.value = false
  }
}

/** 打开弹窗前保证至少已有第一页仓库；与其它下拉共用同一缓存 */
const ensureWarehouseFilterOptionsLoaded = async () => {
  if (warehouseFilterOptions.value.length > 0) return
  await loadMoreWarehouseFilterOptions({ reset: true, keyword: '' })
}

const mergeWarehouseById = async (id) => {
  if (id == null || id === '') return
  const nid = Number(id)
  if (warehouseFilterOptions.value.some((w) => Number(w.id) === nid)) return
  try {
    const w = await warehouseApi.get(id)
    if (w) warehouseFilterOptions.value = mergeOptionsById(warehouseFilterOptions.value, [w])
  } catch {
    /* ignore */
  }
}

const onWarehouseDropdownVisibleChange = async (visible) => {
  if (!visible) return
  if (warehouseFilterOptions.value.length === 0) {
    await loadMoreWarehouseFilterOptions({ reset: true, keyword: '' })
  }
  if (filterWarehouse.value != null) await mergeWarehouseById(filterWarehouse.value)
  if (transferFilterWarehouseId.value != null) await mergeWarehouseById(transferFilterWarehouseId.value)
  if (recordsInboundFilterWarehouseId.value != null) await mergeWarehouseById(recordsInboundFilterWarehouseId.value)
  if (recordsOutboundFilterWarehouseId.value != null) await mergeWarehouseById(recordsOutboundFilterWarehouseId.value)
}

const onWarehouseFilter = (keyword) => {
  loadMoreWarehouseFilterOptions({ reset: true, keyword: keyword || '' })
}

const fetchWarehouseGrid = async () => {
  if (!canInventoryWarehouseTab.value) return
  try {
    const res = await warehouseApi.list({ page: 1, size: 500 })
    warehouseGridList.value = res?.list || []
  } catch {
    warehouseGridList.value = []
  }
}

const fetchWarehouseDetailInventory = async () => {
  if (!warehouseDetailVisible.value || !currentWarehouse.value?.id) {
    warehouseDetailInventoryRows.value = []
    warehouseDetailInventoryTotal.value = 0
    return
  }
  try {
    const res = await inventoryApi.list({
      warehouseId: currentWarehouse.value.id,
      page: warehouseInventoryCurrentPage.value,
      size: warehouseInventoryPageSize.value
    })
    warehouseDetailInventoryRows.value = res?.list || []
    warehouseDetailInventoryTotal.value = Number(res?.total) || 0
  } catch {
    warehouseDetailInventoryRows.value = []
    warehouseDetailInventoryTotal.value = 0
  }
}

const loadMoreSupplierPurchaseOptions = async ({ reset = false, keyword = null } = {}) => {
  if (supplierPurchaseOptionsLoading.value) return
  if (reset) {
    supplierPurchaseOptions.value = []
    supplierPurchaseOptionsPage.value = 0
    supplierPurchaseOptionsTotal.value = 0
    supplierPurchaseKeyword.value = keyword ?? ''
  } else if (!supplierPurchaseOptionsHasMore.value && supplierPurchaseOptionsPage.value > 0) {
    return
  }
  supplierPurchaseOptionsLoading.value = true
  try {
    const nextPage = supplierPurchaseOptionsPage.value + 1
    const res = await supplierApi.list({
      page: nextPage,
      size: FILTER_DROPDOWN_PAGE_SIZE,
      keyword: supplierPurchaseKeyword.value || undefined
    })
    const rows = res?.list || []
    supplierPurchaseOptions.value = mergeOptionsById(supplierPurchaseOptions.value, rows)
    supplierPurchaseOptionsPage.value = nextPage
    supplierPurchaseOptionsTotal.value = Number(res?.total) || supplierPurchaseOptions.value.length
    if (rows.length < FILTER_DROPDOWN_PAGE_SIZE) {
      supplierPurchaseOptionsTotal.value = supplierPurchaseOptions.value.length
    }
  } finally {
    supplierPurchaseOptionsLoading.value = false
  }
}

const mergeSupplierByIdForPurchase = async (id) => {
  if (id == null || id === '') return
  const sid = Number(id)
  if (supplierPurchaseOptions.value.some((s) => Number(s.id) === sid)) return
  try {
    const s = await supplierApi.get(id)
    if (s) supplierPurchaseOptions.value = mergeOptionsById(supplierPurchaseOptions.value, [s])
  } catch {
    /* ignore */
  }
}

const onSupplierPurchaseVisibleChange = async (visible) => {
  if (!visible) return
  if (supplierPurchaseOptions.value.length === 0) {
    await loadMoreSupplierPurchaseOptions({ reset: true, keyword: '' })
  }
  if (purchaseForm.value.supplierId != null) await mergeSupplierByIdForPurchase(purchaseForm.value.supplierId)
}

const onSupplierPurchaseFilter = (keyword) => {
  loadMoreSupplierPurchaseOptions({ reset: true, keyword: keyword || '' })
}

const OPTION_PAGE_SIZE = 20
/** 仅商品下拉分页使用 10 / 页；采购单/销售单下拉仍用 OPTION_PAGE_SIZE */
const productOptions = ref([])
const productOptionsPage = ref(0)
const productOptionsTotal = ref(0)
const productOptionsKeyword = ref('')
const productOptionsLoading = ref(false)
const productOptionsHasMore = computed(() => productOptions.value.length < productOptionsTotal.value)

const purchaseOrderOptions = ref([])
const purchaseOrderOptionsPage = ref(0)
const purchaseOrderOptionsTotal = ref(0)
const purchaseOrderOptionsKeyword = ref('')
const purchaseOrderOptionsProductId = ref(null)
const purchaseOrderOptionsLoading = ref(false)
const purchaseOrderOptionsHasMore = computed(() => purchaseOrderOptions.value.length < purchaseOrderOptionsTotal.value)

const salesOrderOptions = ref([])
const salesOrderOptionsPage = ref(0)
const salesOrderOptionsTotal = ref(0)
const salesOrderOptionsKeyword = ref('')
const salesOrderOptionsProductId = ref(null)
const salesOrderOptionsLoading = ref(false)
const salesOrderOptionsHasMore = computed(() => salesOrderOptions.value.length < salesOrderOptionsTotal.value)

const inventoryOptions = ref([])
const inventoryOptionsPage = ref(0)
const inventoryOptionsTotal = ref(0)
const inventoryOptionsKeyword = ref('')
const inventoryOptionsLoading = ref(false)
const inventoryOptionsHasMore = computed(() => inventoryOptions.value.length < inventoryOptionsTotal.value)

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

const loadMorePurchaseOrderOptions = async ({ reset = false, keyword = null, productId } = {}) => {
  if (purchaseOrderOptionsLoading.value) return
  if (reset) {
    purchaseOrderOptions.value = []
    purchaseOrderOptionsPage.value = 0
    purchaseOrderOptionsTotal.value = 0
    purchaseOrderOptionsKeyword.value = keyword ?? ''
    purchaseOrderOptionsProductId.value = productId ?? null
  } else if (!purchaseOrderOptionsHasMore.value && purchaseOrderOptionsPage.value > 0) {
    return
  }
  purchaseOrderOptionsLoading.value = true
  try {
    const nextPage = purchaseOrderOptionsPage.value + 1
    const res = await purchaseApi.list({
      page: nextPage,
      size: OPTION_PAGE_SIZE,
      keyword: purchaseOrderOptionsKeyword.value || undefined,
      productId: purchaseOrderOptionsProductId.value || undefined
    })
    const rawRows = res?.list || []
    const rows = rawRows.filter(po => po.inboundStatus !== 'completed' && po.inboundStatus !== 'cancelled')
    purchaseOrderOptions.value = mergeOptionsById(purchaseOrderOptions.value, rows)
    purchaseOrderOptionsPage.value = nextPage
    purchaseOrderOptionsTotal.value = Number(res?.total) || purchaseOrderOptions.value.length
    if (rawRows.length < OPTION_PAGE_SIZE) {
      purchaseOrderOptionsTotal.value = purchaseOrderOptions.value.length
    }
  } finally {
    purchaseOrderOptionsLoading.value = false
  }
}

const loadMoreSalesOrderOptions = async ({ reset = false, keyword = null, productId } = {}) => {
  if (salesOrderOptionsLoading.value) return
  if (reset) {
    salesOrderOptions.value = []
    salesOrderOptionsPage.value = 0
    salesOrderOptionsTotal.value = 0
    salesOrderOptionsKeyword.value = keyword ?? ''
    salesOrderOptionsProductId.value = productId ?? null
  } else if (!salesOrderOptionsHasMore.value && salesOrderOptionsPage.value > 0) {
    return
  }
  salesOrderOptionsLoading.value = true
  try {
    const nextPage = salesOrderOptionsPage.value + 1
    const res = await salesApi.list({
      page: nextPage,
      size: OPTION_PAGE_SIZE,
      keyword: salesOrderOptionsKeyword.value || undefined,
      productId: salesOrderOptionsProductId.value || undefined,
      payStatus: 'paid',
      salesOrderStatus: 'pending'
    })
    const rows = res?.list || []
    salesOrderOptions.value = mergeOptionsById(salesOrderOptions.value, rows)
    salesOrderOptionsPage.value = nextPage
    salesOrderOptionsTotal.value = Number(res?.total) || salesOrderOptions.value.length
    if (rows.length < OPTION_PAGE_SIZE) {
      salesOrderOptionsTotal.value = salesOrderOptions.value.length
    }
  } finally {
    salesOrderOptionsLoading.value = false
  }
}

const loadMoreInventoryOptions = async ({ reset = false, keyword = null } = {}) => {
  if (inventoryOptionsLoading.value) return
  if (reset) {
    inventoryOptions.value = []
    inventoryOptionsPage.value = 0
    inventoryOptionsTotal.value = 0
    inventoryOptionsKeyword.value = keyword ?? ''
  } else if (!inventoryOptionsHasMore.value && inventoryOptionsPage.value > 0) {
    return
  }
  inventoryOptionsLoading.value = true
  try {
    const nextPage = inventoryOptionsPage.value + 1
    const res = await inventoryApi.list({
      page: nextPage,
      size: OPTION_PAGE_SIZE,
      keyword: inventoryOptionsKeyword.value || undefined
    })
    const rows = res?.list || []
    inventoryOptions.value = mergeOptionsById(inventoryOptions.value, rows)
    inventoryOptionsPage.value = nextPage
    inventoryOptionsTotal.value = Number(res?.total) || inventoryOptions.value.length
    if (rows.length < OPTION_PAGE_SIZE) {
      inventoryOptionsTotal.value = inventoryOptions.value.length
    }
  } finally {
    inventoryOptionsLoading.value = false
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

const onProductSelectVisibleChange = async (visible) => {
  if (!visible) return
  if (productOptions.value.length === 0) {
    await loadMoreProductOptions({ reset: true, keyword: '' })
  }
  if (filterProduct.value != null) await mergeProductById(filterProduct.value)
}

const onPurchaseOrderSelectVisibleChange = (visible) => {
  if (visible && purchaseOrderOptions.value.length === 0) {
    loadMorePurchaseOrderOptions({
      reset: true,
      keyword: '',
      productId: purchaseOrderOptionsProductId.value
    })
  }
}

const onSalesOrderSelectVisibleChange = (visible) => {
  if (visible && salesOrderOptions.value.length === 0) {
    loadMoreSalesOrderOptions({
      reset: true,
      keyword: '',
      productId: salesOrderOptionsProductId.value
    })
  }
}

const onInventorySelectVisibleChange = (visible) => {
  if (visible && inventoryOptions.value.length === 0) {
    loadMoreInventoryOptions({ reset: true, keyword: '' })
  }
}

const onProductFilter = (keyword) => {
  loadMoreProductOptions({ reset: true, keyword: keyword || '' })
}

const onPurchaseOrderFilter = (keyword) => {
  loadMorePurchaseOrderOptions({
    reset: true,
    keyword: keyword || '',
    productId: purchaseOrderOptionsProductId.value
  })
}

const onSalesOrderFilter = (keyword) => {
  loadMoreSalesOrderOptions({
    reset: true,
    keyword: keyword || '',
    productId: salesOrderOptionsProductId.value
  })
}

const onInventoryFilter = (keyword) => {
  loadMoreInventoryOptions({
    reset: true,
    keyword: keyword || ''
  })
}

/** 仅使用下拉分页加载的商品，不再合并 dataStore 首屏商品兜底 */
const products = computed(() => productOptions.value)
/** 手动入库「其他入库」选商品：与采购一致排除停售 */
const productsForManualOtherInbound = computed(() => products.value.filter(isProductSelectableForOrder))
const purchaseOrders = computed(() => purchaseOrderOptions.value)
const salesOrders = computed(() => salesOrderOptions.value)
const inventoryOptionsLookup = computed(() => inventoryOptions.value)

/** 库存详情弹窗「创建采购」：停售商品不展示快捷入口 */
const canShowCreatePurchaseInStockDetail = computed(() => {
  if (!canPurchaseAdd.value || !currentStock.value) return false
  if ((currentStock.value.stock ?? 0) >= (currentStock.value.safeStock ?? 0)) return false
  return canShortcutPurchaseForStock(currentStock.value, products.value)
})

// 当前库存的入库记录
const stockInboundRecords = computed(() => {
  if (!currentStock.value) return []
  return inboundRecords.value.filter(r =>
    r.productId === currentStock.value.productId &&
    r.warehouseId === currentStock.value.warehouseId
  ).slice(0, 5) // 最近5条
})

// 当前库存的出库记录
const stockOutboundRecords = computed(() => {
  if (!currentStock.value) return []
  return outboundRecords.value.filter(r =>
    r.productId === currentStock.value.productId &&
    r.warehouseId === currentStock.value.warehouseId
  ).slice(0, 5) // 最近5条
})

// 待入库的采购单（用于入库管理选择）
const pendingPurchaseOrders = computed(() => {
  return purchaseOrders.value.filter(po =>
    po.inboundStatus !== 'completed' && po.inboundStatus !== 'cancelled'
  )
})

// 待发货的销售订单（用于出库管理选择）
const pendingSalesOrders = computed(() => {
  return salesOrders.value.filter(so =>
    so.status === 'pending' && so.payStatus === 'paid'
  )
})

// 当前商品的待入库采购单（用于库存入库）
const stockPendingPurchaseOrders = computed(() => {
  if (!stockInboundForm.value.productId) return []
  return purchaseOrders.value.filter(po =>
    po.productId === stockInboundForm.value.productId &&
    po.inboundStatus !== 'completed' && po.inboundStatus !== 'cancelled'
  )
})

// 当前商品的待发货销售单（用于库存出库）
const stockPendingSalesOrders = computed(() => {
  if (!stockOutboundForm.value.productId) return []
  return salesOrders.value.filter(so =>
    so.productId === stockOutboundForm.value.productId &&
    so.status === 'pending' &&
    so.payStatus === 'paid'
  )
})

// 加载统计数据
const loadStats = async () => {
  try {
    const stats = await inventoryApi.stats()
    inventoryStats.value = stats || {
      categoryCount: 0,
      lastMonthCategoryCount: 0,
      categoryChange: 0,
      totalValue: 0,
      lastMonthTotalValue: 0,
      valueChangePercent: 0,
      warningCount: 0,
      transferringCount: 0,
      lastMonthTransferringCount: 0,
      transferringChange: 0
    }
  } catch (error) {
    console.error('加载库存统计数据失败:', error)
  }
}

/** 库存主表：服务端分页 + 筛选 */
const inventoryTableRows = ref([])
const inventoryTableTotal = ref(0)
const transferTableRows = ref([])
const transferTableTotal = ref(0)

const fetchInventoryTable = async (showLoading = true) => {
  if (imageSearchMode.value) return
  if (showLoading) loading.value = true
  try {
    await ensureSystemStaleDaysLoaded()
    const res = await inventoryApi.list({
      page: inventoryCurrentPage.value,
      size: inventoryPageSize.value,
      keyword: searchKeyword.value || undefined,
      productId: filterProduct.value || undefined,
      warehouseId: filterWarehouse.value || undefined,
      stagnantStatus: filterStagnantStatus.value || undefined,
      lastOutboundStart: filterLastOutboundRange.value?.[0],
      lastOutboundEnd: filterLastOutboundRange.value?.[1],
      lastInboundStart: filterLastInboundRange.value?.[0],
      lastInboundEnd: filterLastInboundRange.value?.[1]
    })
    inventoryTableRows.value = res.list || []
    inventoryTableTotal.value = Number(res.total) || 0
  } catch (e) {
    ElMessage.error(e.message || '加载库存失败')
    inventoryTableRows.value = []
    inventoryTableTotal.value = 0
  } finally {
    if (showLoading) loading.value = false
  }
}

const inventoryListTotal = computed(() =>
  imageSearchMode.value ? imageSearchTotal.value : inventoryTableTotal.value
)

const paginatedInventory = computed(() => {
  if (imageSearchMode.value) {
    return imageSearchRows.value
  }
  return inventoryTableRows.value
})

const fetchTransferTable = async (showLoading = true) => {
  if (showLoading) loading.value = true
  try {
    const res = await inventoryApi.transferList({
      page: transferCurrentPage.value,
      size: transferPageSize.value,
      keyword: transferFilterKeyword.value?.trim() || undefined,
      transferStatus: transferFilterStatus.value || undefined,
      warehouseId: transferFilterWarehouseId.value || undefined,
      createTimeStart: transferFilterDateRange.value?.[0],
      createTimeEnd: transferFilterDateRange.value?.[1]
    })
    transferTableRows.value = res.list || []
    transferTableTotal.value = Number(res.total) || 0
  } catch (e) {
    ElMessage.error(e.message || '加载调拨记录失败')
    transferTableRows.value = []
    transferTableTotal.value = 0
  } finally {
    if (showLoading) loading.value = false
  }
}

watch(
  [filterProduct, filterWarehouse, filterStagnantStatus, filterLastOutboundRange, filterLastInboundRange, searchKeyword],
  () => {
    if (imageSearchMode.value) return
    inventoryCurrentPage.value = 1
    fetchInventoryTable()
  }
)

watch([inventoryCurrentPage, inventoryPageSize], () => {
  if (!imageSearchMode.value) fetchInventoryTable()
})

watch([transferCurrentPage, transferPageSize], () => {
  fetchTransferTable()
})

watch(
  [transferFilterKeyword, transferFilterStatus, transferFilterWarehouseId, transferFilterDateRange],
  () => {
    if (activeTab.value !== 'transfer' || !canInventoryTransferTab.value) return
    transferCurrentPage.value = 1
    fetchTransferTable()
  }
)

const loadInboundCacheForCurrentPage = () =>
  dataStore.loadInboundRecords({
    page: recordsInboundCurrentPage.value,
    size: recordsInboundPageSize.value,
    keyword: recordsInboundFilterKeyword.value?.trim() || undefined,
    warehouseId: recordsInboundFilterWarehouseId.value || undefined,
    createTimeStart: recordsInboundFilterDateRange.value?.[0],
    createTimeEnd: recordsInboundFilterDateRange.value?.[1],
    operatorName: recordsInboundFilterOperator.value?.trim() || undefined
  })

const loadOutboundCacheForCurrentPage = () =>
  dataStore.loadOutboundRecords({
    page: recordsOutboundCurrentPage.value,
    size: recordsOutboundPageSize.value,
    keyword: recordsOutboundFilterKeyword.value?.trim() || undefined,
    warehouseId: recordsOutboundFilterWarehouseId.value || undefined,
    createTimeStart: recordsOutboundFilterDateRange.value?.[0],
    createTimeEnd: recordsOutboundFilterDateRange.value?.[1],
    operatorName: recordsOutboundFilterOperator.value?.trim() || undefined
  })

watch(
  [
    recordsInboundFilterKeyword,
    recordsInboundFilterWarehouseId,
    recordsInboundFilterDateRange,
    recordsInboundFilterOperator
  ],
  () => {
    if (activeTab.value !== 'records' || recordsSubTab.value !== 'inbound' || !canInventoryRecordsTab.value) return
    recordsInboundCurrentPage.value = 1
    loadInboundCacheForCurrentPage()
  }
)

watch(
  [
    recordsOutboundFilterKeyword,
    recordsOutboundFilterWarehouseId,
    recordsOutboundFilterDateRange,
    recordsOutboundFilterOperator
  ],
  () => {
    if (activeTab.value !== 'records' || recordsSubTab.value !== 'outbound' || !canInventoryRecordsTab.value) return
    recordsOutboundCurrentPage.value = 1
    loadOutboundCacheForCurrentPage()
  }
)

watch(
  [recordsInboundCurrentPage, recordsInboundPageSize],
  () => {
    if (activeTab.value !== 'records' || recordsSubTab.value !== 'inbound' || !canInventoryRecordsTab.value) return
    loadInboundCacheForCurrentPage()
  }
)

watch(
  [recordsOutboundCurrentPage, recordsOutboundPageSize],
  () => {
    if (activeTab.value !== 'records' || recordsSubTab.value !== 'outbound' || !canInventoryRecordsTab.value) return
    loadOutboundCacheForCurrentPage()
  }
)

/**
 * 仅按当前主 Tab 拉取数据：库存管理 / 调拨记录 / 出入库记录 / 仓库管理 互不复用启动请求。
 */
const refreshActiveTabData = async (showLoading = true) => {
  const tab = activeTab.value
  if (showLoading) loading.value = true
  try {
    if (tab === 'stock' && canInventoryStockTab.value) {
      if (!stockFilterDropdownsPrimed.value) {
        stockFilterDropdownsPrimed.value = true
        await Promise.all([
          loadStats(),
          fetchInventoryTable(false),
          loadMoreProductOptions({ reset: true, keyword: '' }),
          loadMoreWarehouseFilterOptions({ reset: true, keyword: '' })
        ])
      } else {
        await Promise.all([loadStats(), fetchInventoryTable(false)])
      }
      return
    }
    if (tab === 'transfer' && canInventoryTransferTab.value) {
      await fetchTransferTable(false)
      return
    }
    if (tab === 'records' && canInventoryRecordsTab.value) {
      await Promise.all([loadInboundCacheForCurrentPage(), loadOutboundCacheForCurrentPage()])
      return
    }
    if (tab === 'warehouse' && canInventoryWarehouseTab.value) {
      await fetchWarehouseGrid()
    }
  } finally {
    if (showLoading) loading.value = false
  }
}

const onInventoryQueryImageChange = (uploadFile) => {
  const raw = uploadFile?.raw
  if (!raw) return
  if (raw.size > MAX_IMAGE_UPLOAD_BYTES) {
    ElMessage.warning('查询图片大小不能超过 2MB')
    return
  }
  queryImageFile.value = raw
  const reader = new FileReader()
  reader.onload = (e) => {
    queryImageDataUrl.value = e.target.result
  }
  reader.readAsDataURL(raw)
}

const submitImageSearch = async () => {
  if (!canInventoryRead.value) {
    ElMessage.warning('无库存数据查看权限')
    return
  }
  if (!queryImageFile.value) {
    ElMessage.warning('请先上传图片')
    return
  }
  inventoryCurrentPage.value = 1
  await runImageSearch()
}

const runImageSearch = async () => {
  if (!queryImageFile.value) return
  imageSearchLoading.value = true
  try {
    await ensureSystemStaleDaysLoaded()
    const body = {
      file: queryImageFile.value,
      page: inventoryCurrentPage.value,
      size: inventoryPageSize.value,
      keyword: searchKeyword.value || undefined,
      productId: filterProduct.value || undefined,
      warehouseId: filterWarehouse.value || undefined,
      stagnantStatus: filterStagnantStatus.value || undefined,
      lastOutboundStart: filterLastOutboundRange.value?.[0],
      lastOutboundEnd: filterLastOutboundRange.value?.[1],
      lastInboundStart: filterLastInboundRange.value?.[0],
      lastInboundEnd: filterLastInboundRange.value?.[1],
      similarityThreshold: imageSimilarityThreshold.value
    }
    const res = await inventoryApi.searchByImage(body)
    imageSearchRows.value = res.list || []
    imageSearchTotal.value = Number(res.total) || 0
    imageSearchMode.value = true
    if (imageSearchTotal.value === 0) {
      ElMessage.info('没有相似度达标的库存行，可调低阈值或配置 DASHSCOPE_API_KEY')
    }
  } catch (e) {
    ElMessage.error(e.message || '以图搜图失败')
  } finally {
    imageSearchLoading.value = false
  }
}

const exitImageSearch = () => {
  imageSearchMode.value = false
  queryImageDataUrl.value = ''
  queryImageFile.value = null
  imageSearchRows.value = []
  imageSearchTotal.value = 0
  fetchInventoryTable()
}

/** 普通列表：翻页 by v-model 已由 watch(inventoryCurrentPage, inventoryPageSize) 拉数，勿再 fetch 否则重复请求 */
const handleInventoryPageChange = () => {
  if (imageSearchMode.value) runImageSearch()
}

const handleInventorySizeChange = () => {
  if (imageSearchMode.value) {
    inventoryCurrentPage.value = 1
    runImageSearch()
  } else {
    inventoryCurrentPage.value = 1
    fetchInventoryTable()
  }
}

// 分页后的调拨记录
const paginatedTransfers = computed(() => transferTableRows.value)

const REMARK_MAX_LENGTH = 500
const transferForm = ref({ fromWarehouseId: null, toWarehouseId: null, productId: null, quantity: 1, remark: '' })
const warehouseForm = ref({ name: '', code: '', address: '', capacity: 50, managerName: '', remark: '' })
const warehouseRules = {
  name: [
    { required: true, message: '请输入仓库名称', trigger: 'blur' },
    { max: 100, message: '仓库名称不能超过100个字符', trigger: 'blur' }
  ],
  code: [{ max: 20, message: '仓库编码不能超过20个字符', trigger: 'blur' }],
  address: [{ max: 200, message: '仓库地址不能超过200个字符', trigger: 'blur' }],
  managerName: [{ max: 50, message: '负责人姓名不能超过50个字符', trigger: 'blur' }],
  remark: [{ max: 500, message: '备注不能超过500个字符', trigger: 'blur' }]
}
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
const inboundForm = ref({
  purchaseOrderId: null,
  warehouseId: null,
  quantity: 1,
  maxQuantity: 0,
  remark: ''
})
const transferRules = {
  fromWarehouseId: [{ required: true, message: '请选择调出仓库', trigger: 'change' }],
  toWarehouseId: [{ required: true, message: '请选择调入仓库', trigger: 'change' }],
  productId: [{ required: true, message: '请选择商品', trigger: 'change' }],
  quantity: [{ required: true, message: '请输入调拨数量', trigger: 'blur' }],
  remark: [{ max: REMARK_MAX_LENGTH, message: `备注不能超过${REMARK_MAX_LENGTH}个字符`, trigger: 'blur' }]
}
const purchaseRules = {
  supplierId: [{ required: true, message: '请选择供应商', trigger: 'change' }],
  quantity: [{ required: true, message: '请输入采购数量', trigger: 'blur' }]
}
const inboundRules = {
  purchaseOrderId: [{ required: true, message: '请选择采购单', trigger: 'change' }],
  warehouseId: [{ required: true, message: '请选择入库仓库', trigger: 'change' }],
  quantity: [
    { required: true, message: '请输入入库数量', trigger: 'blur' },
    {
      validator: (rule, value, callback) => {
        if (value > inboundForm.value.maxQuantity) {
          callback(new Error(`入库数量不能超过待入库数量 (${inboundForm.value.maxQuantity} 件)`))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ]
}

// 库存入库/出库表单
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
const stockInboundRules = {
  purchaseOrderId: [{ required: true, message: '请选择采购单', trigger: 'change' }],
  quantity: [
    { required: true, message: '请输入入库数量', trigger: 'blur' },
    {
      validator: (rule, value, callback) => {
        if (value > stockInboundForm.value.maxQuantity) {
          callback(new Error(`入库数量不能超过待入库数量 (${stockInboundForm.value.maxQuantity} 件)`))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ]
}
const stockOutboundRules = {
  salesOrderId: [{ required: true, message: '请选择销售单', trigger: 'change' }],
  quantity: [
    { required: true, message: '请输入出库数量', trigger: 'blur' },
    {
      validator: (rule, value, callback) => {
        if (value > stockOutboundForm.value.maxQuantity) {
          callback(new Error(`出库数量不能超过待发货数量 (${stockOutboundForm.value.maxQuantity} 件)`))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ]
}

// 其他入库表单
const stockOtherInboundForm = ref({
  inventoryId: null,
  productName: '',
  warehouseName: '',
  currentStock: 0,
  quantity: 1,
  remark: ''
})
const stockOtherInboundRules = {
  quantity: [{ required: true, message: '请输入入库数量', trigger: 'blur' }]
}

// 其他出库表单
const stockOtherOutboundForm = ref({
  inventoryId: null,
  productName: '',
  warehouseName: '',
  currentStock: 0,
  quantity: 1,
  maxQuantity: 0,
  remark: ''
})
const stockOtherOutboundRules = {
  quantity: [
    { required: true, message: '请输入出库数量', trigger: 'blur' },
    {
      validator: (rule, value, callback) => {
        if (stockOtherOutboundForm.value.maxQuantity && value > stockOtherOutboundForm.value.maxQuantity) {
          callback(new Error(`出库数量不能超过当前库存 (${stockOtherOutboundForm.value.maxQuantity} 件)`))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ],
  remark: [
    { required: true, message: '出库备注为必填项', trigger: 'blur' },
    { max: REMARK_MAX_LENGTH, message: `备注不能超过${REMARK_MAX_LENGTH}个字符`, trigger: 'blur' }
  ]
}

// 手动入库表单（商品初始化入库）
const manualInboundForm = ref({
  productId: null,
  warehouseId: null,
  quantity: 1,
  remark: ''
})
const manualInboundRules = {
  productId: [{ required: true, message: '请选择商品', trigger: 'change' }],
  warehouseId: [{ required: true, message: '请选择仓库', trigger: 'change' }],
  quantity: [{ required: true, message: '请输入入库数量', trigger: 'blur' }]
}

// 采购入库表单
const purchaseInboundForm = ref({
  purchaseOrderId: null,
  warehouseId: null,
  quantity: 1,
  maxQuantity: 0,
  remark: ''
})
const purchaseInboundRules = {
  purchaseOrderId: [{ required: true, message: '请选择采购单', trigger: 'change' }],
  warehouseId: [{ required: true, message: '请选择仓库', trigger: 'change' }],
  quantity: [
    { required: true, message: '请输入入库数量', trigger: 'blur' },
    {
      validator: (rule, value, callback) => {
        if (purchaseInboundForm.value.maxQuantity && value > purchaseInboundForm.value.maxQuantity) {
          callback(new Error(`入库数量不能超过待入库数量 (${purchaseInboundForm.value.maxQuantity} 件)`))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ]
}

// 销售出库表单
const salesOutboundForm = ref({
  salesOrderId: null,
  warehouseId: null,
  quantity: 1,
  maxQuantity: 0,
  remark: ''
})
const salesOutboundWarehouseOptions = ref([])
const salesOutboundWarehouseLoading = ref(false)
let salesOutboundWarehouseRequestId = 0
const salesOutboundRules = {
  salesOrderId: [{ required: true, message: '请选择销售订单', trigger: 'change' }],
  warehouseId: [{ required: true, message: '请选择仓库', trigger: 'change' }],
  quantity: [
    { required: true, message: '请输入出库数量', trigger: 'blur' },
    {
      validator: (rule, value, callback) => {
        if (salesOutboundForm.value.maxQuantity && value > salesOutboundForm.value.maxQuantity) {
          callback(new Error(`出库数量不能超过待发货数量 (${salesOutboundForm.value.maxQuantity} 件)`))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ]
}

// 其他出库表单
const otherOutboundForm = ref({
  inventoryId: null,
  productId: null,
  productName: '',
  warehouseId: null,
  warehouseName: '',
  currentStock: 0,
  quantity: 1,
  maxQuantity: 0,
  remark: ''
})
const otherOutboundRules = {
  inventoryId: [{ required: true, message: '请选择库存记录', trigger: 'change' }],
  quantity: [
    { required: true, message: '请输入出库数量', trigger: 'blur' },
    {
      validator: (rule, value, callback) => {
        if (otherOutboundForm.value.maxQuantity && value > otherOutboundForm.value.maxQuantity) {
          callback(new Error(`出库数量不能超过当前库存 (${otherOutboundForm.value.maxQuantity} 件)`))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ],
  remark: [
    { required: true, message: '出库备注为必填项', trigger: 'blur' },
    { max: REMARK_MAX_LENGTH, message: `备注不能超过${REMARK_MAX_LENGTH}个字符`, trigger: 'blur' }
  ]
}

const getCategoryIcon = (cat) => ({ '手机': '📱', '电脑': '💻', '配件': '🎧', '手表': '⌚', '平板': '📱' }[cat] || '📦')

// 列表行图片：仅以库存接口回填的 productImage / image 为准
const getProductImage = (_productId, rowImage) => firstProductImageUrl(rowImage)

const resolveSalesOrderImage = (salesOrder) => {
  if (!salesOrder) return null
  return firstProductImageUrl(salesOrder.productImage || salesOrder.image)
}

const resolveProductImageById = (productId) => {
  if (!productId) return null
  const p = productOptions.value.find(x => x.id === productId)
  return firstProductImageUrl(p?.image)
}

const productPreviewListByFormProductId = (productId) => {
  if (!productId) return []
  const p = productOptions.value.find((x) => x.id === productId)
  return parseProductImageUrls(p?.image)
}

// 名称兜底：仅从已加载的下拉 options / 仓库管理列表解析，不再预拉全量仓库
const getWarehouseName = (warehouseId) => {
  if (warehouseId == null || warehouseId === '') return ''
  const id = Number(warehouseId)
  const fromOpts = warehouseFilterOptions.value.find(w => Number(w.id) === id)
  if (fromOpts?.name) return fromOpts.name
  const fromGrid = warehouseGridList.value.find(w => Number(w.id) === id)
  return fromGrid?.name || ''
}

// 调拨对话框选中商品的图片（依赖下拉分页已加载的商品）
const transferProductImage = computed(() => resolveProductImageById(transferForm.value.productId))

// 手动入库选中商品的图片
const manualInboundProductImage = computed(() => resolveProductImageById(manualInboundForm.value.productId))

// 其他出库选中库存的商品图片
const otherOutboundProductImage = computed(() => resolveProductImageById(otherOutboundForm.value.productId))

const getStockStatusType = (status) => ({ 'normal': 'success', 'warning': 'warning', 'critical': 'danger', '正常': 'success', '偏低': 'warning', '紧急补货': 'danger' }[status] || 'info')

// 状态格式化函数 - 英文转中文
const formatStockStatus = (status) => {
  const statusMap = {
    'normal': '正常',
    'warning': '偏低',
    'critical': '紧急补货',
    '正常': '正常',
    '偏低': '偏低',
    '紧急补货': '紧急补货'
  }
  return statusMap[status] || status
}

// 调拨状态格式化函数
const formatTransferStatus = (status) => {
  const statusMap = {
    'pending': '待确认',
    'completed': '已完成',
    'cancelled': '已取消',
    '待确认': '待确认',
    '已完成': '已完成',
    '已取消': '已取消'
  }
  return statusMap[status] || status
}

const getTransferStatusType = (status) => {
  const typeMap = {
    'pending': 'warning',
    'completed': 'success',
    'cancelled': 'info',
    '待确认': 'warning',
    '已完成': 'success',
    '已取消': 'info'
  }
  return typeMap[status] || 'info'
}

const openTransferDialog = async (row) => {
  if (!canTransferOp.value) {
    ElMessage.warning('无库存调拨权限')
    return
  }
  await ensureWarehouseFilterOptionsLoaded()
  if (row) {
    transferForm.value.productId = row.productId
    transferForm.value.fromWarehouseId = row.warehouseId
    await Promise.all([mergeWarehouseById(row.warehouseId), mergeProductById(row.productId)])
  } else {
    transferForm.value = { fromWarehouseId: null, toWarehouseId: null, productId: null, quantity: 1, remark: '' }
  }
  transferDialogVisible.value = true
}

// 提交调拨 - 调用后端API
const submitTransfer = async () => {
  if (!canTransferOp.value) {
    ElMessage.warning('无库存调拨权限')
    return
  }
  if (!transferFormRef.value) return
  await transferFormRef.value.validate(async (valid) => {
    if (valid) {
      loading.value = true
      try {
        await inventoryApi.createTransfer({
          productId: transferForm.value.productId,
          fromWarehouseId: transferForm.value.fromWarehouseId,
          toWarehouseId: transferForm.value.toWarehouseId,
          quantity: transferForm.value.quantity,
          remark: transferForm.value.remark
        })
        ElMessage.success('调拨单已创建')
        transferDialogVisible.value = false
        await Promise.all([fetchTransferTable(false), fetchInventoryTable(false)])
      } catch (error) {
        ElMessage.error(error.message || '创建失败')
      } finally {
        loading.value = false
      }
    }
  })
}

const submitWarehouse = async () => {
  if (!canInventoryWarehouse.value) {
    ElMessage.warning('无仓库管理权限')
    return
  }
  await warehouseFormRef.value?.validate(async (valid) => {
    if (valid) {
      loading.value = true
      try {
        if (warehouseEditMode.value && currentWarehouse.value) {
          // 编辑仓库
          await warehouseApi.update(currentWarehouse.value.id, {
            name: warehouseForm.value.name,
            code: warehouseForm.value.code,
            address: warehouseForm.value.address,
            capacity: warehouseForm.value.capacity,
            capacityUsed: warehouseForm.value.capacity,
            managerName: warehouseForm.value.managerName,
            remark: warehouseForm.value.remark
          })
          ElMessage.success('仓库信息已更新')
          warehouseDialogVisible.value = false
          // 刷新仓库列表
          await fetchWarehouseGrid()
          resetWarehouseFilterOptions()
          const updatedWarehouse = warehouseGridList.value.find(w => w.id === currentWarehouse.value.id)
          if (updatedWarehouse) {
            currentWarehouse.value = updatedWarehouse
          }
        } else {
          // 创建仓库
          await warehouseApi.create({
            name: warehouseForm.value.name,
            code: warehouseForm.value.code,
            address: warehouseForm.value.address,
            capacity: warehouseForm.value.capacity,
            capacityUsed: warehouseForm.value.capacity,
            managerName: warehouseForm.value.managerName,
            remark: warehouseForm.value.remark
          })
          ElMessage.success('仓库添加成功')
          warehouseDialogVisible.value = false
          await fetchWarehouseGrid()
          resetWarehouseFilterOptions()
        }
      } catch (error) {
        ElMessage.error(error.message || '操作失败')
      } finally {
        loading.value = false
      }
    }
  })
}

const viewStockDetail = (row) => {
  router.push(`/inventory/stock/${row.id}`)
}

// 打开库存入库对话框
const openStockInbound = async (row) => {
  if (!canInventoryInbound.value) {
    ElMessage.warning('无入库操作权限')
    return
  }
  await loadMorePurchaseOrderOptions({ reset: true, keyword: '', productId: row.productId })
  stockInboundType.value = 'purchase'
  stockInboundForm.value = {
    inventoryId: row.id,
    productId: row.productId,
    productName: row.productName || row.name,
    warehouseId: row.warehouseId,
    warehouseName: row.warehouseName,
    currentStock: row.stock ?? 0,
    purchaseOrderId: null,
    quantity: 1,
    maxQuantity: 0,
    remark: ''
  }
  stockOtherInboundForm.value = {
    inventoryId: row.id,
    productName: row.productName || row.name,
    warehouseName: row.warehouseName,
    currentStock: row.stock ?? 0,
    quantity: 1,
    remark: ''
  }
  stockInboundVisible.value = true
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

// 打开库存出库对话框
const openStockOutbound = async (row) => {
  if (!canInventoryOutbound.value) {
    ElMessage.warning('无出库操作权限')
    return
  }
  const currentStock = row.stock ?? 0
  if (currentStock <= 0) {
    ElMessage.warning('当前库存为0，无法出库')
    return
  }
  await loadMoreSalesOrderOptions({ reset: true, keyword: '', productId: row.productId })
  stockOutboundType.value = 'sales'
  stockOutboundForm.value = {
    inventoryId: row.id,
    productId: row.productId,
    productName: row.productName || row.name,
    warehouseId: row.warehouseId,
    warehouseName: row.warehouseName,
    currentStock: currentStock,
    salesOrderId: null,
    quantity: 1,
    maxQuantity: 0,
    remark: ''
  }
  stockOtherOutboundForm.value = {
    inventoryId: row.id,
    productName: row.productName || row.name,
    warehouseName: row.warehouseName,
    currentStock: currentStock,
    quantity: 1,
    maxQuantity: currentStock,
    remark: ''
  }
  stockOutboundVisible.value = true
}

// 选择销售单时设置最大出库数量
const onStockSalesOrderChange = (salesOrderId) => {
  const so = salesOrders.value.find(s => s.id === salesOrderId)
  if (so) {
    const pendingQty = so.pendingQuantity || so.quantity || 1
    const currentStock = stockOutboundForm.value.currentStock || 0
    // 最大出库数量不能超过当前库存
    const maxQty = Math.min(pendingQty, currentStock)
    stockOutboundForm.value.maxQuantity = maxQty > 0 ? maxQty : 1
    stockOutboundForm.value.quantity = maxQty > 0 ? Math.min(maxQty, 1) : 1
  }
}

// 提交库存入库
const submitStockInbound = async () => {
  if (stockInboundType.value === 'purchase') {
    // 采购入库
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
          await Promise.all([
            fetchInventoryTable(false),
            loadMorePurchaseOrderOptions({
              reset: true,
              keyword: '',
              productId: stockInboundForm.value.productId
            }),
            loadInboundCacheForCurrentPage(),
            loadStats()
          ])
        } catch (error) {
          ElMessage.error(error.message || '入库失败')
        } finally {
          loading.value = false
        }
      }
    })
  } else {
    // 其他入库
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
          await Promise.all([fetchInventoryTable(false), loadInboundCacheForCurrentPage(), loadStats()])
        } catch (error) {
          ElMessage.error(error.message || '入库失败')
        } finally {
          loading.value = false
        }
      }
    })
  }
}

// 提交库存出库
const submitStockOutbound = async () => {
  if (stockOutboundType.value === 'sales') {
    // 销售出库
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
          await Promise.all([
            fetchInventoryTable(false),
            loadMoreSalesOrderOptions({
              reset: true,
              keyword: '',
              productId: stockOutboundForm.value.productId
            }),
            loadOutboundCacheForCurrentPage(),
            loadStats()
          ])
        } catch (error) {
          ElMessage.error(error.message || '出库失败')
        } finally {
          loading.value = false
        }
      }
    })
  } else {
    // 其他出库
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
          await Promise.all([fetchInventoryTable(false), loadOutboundCacheForCurrentPage(), loadStats()])
        } catch (error) {
          ElMessage.error(error.message || '出库失败')
        } finally {
          loading.value = false
        }
      }
    })
  }
}

// 创建采购 - 从库存商品
const createPurchaseFromStock = async (row) => {
  if (!canPurchaseAdd.value) {
    ElMessage.warning('无新建采购单权限')
    return
  }
  await loadMoreSupplierPurchaseOptions({ reset: true, keyword: '' })
  currentStock.value = row
  openPurchaseFromStock(row)
}

// 打开采购弹框 - 回填数据
const openPurchaseFromStock = (stock) => {
  if (!canShortcutPurchaseForStock(stock, products.value)) {
    ElMessage.warning('该商品已停售，无法创建采购单')
    return
  }
  purchaseForm.value = {
    productId: stock.productId,
    productName: stock.productName || stock.name || '',
    sku: stock.sku || '',
    currentStock: stock.stock || 0,
    safeStock: stock.safeStock || 10,
    supplierId: null,
    date: new Date(),
    quantity: Math.max(1, (stock.safeStock || 10) - (stock.stock || 0)), // 建议采购数量为安全库存减去当前库存
    unitPrice: stock.costPrice || 0,
    deliveryDate: new Date(Date.now() + 7 * 24 * 60 * 60 * 1000), // 默认7天后
    remark: `库存预警采购：当前库存${stock.stock}，安全库存${stock.safeStock}`
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
        stockDetailVisible.value = false
        await Promise.all([loadStats(), fetchInventoryTable(false)])
      } catch (error) {
        ElMessage.error(error.message || '创建失败')
      } finally {
        loading.value = false
      }
    }
  })
}

// 确认调拨 - 调用后端API
const confirmTransfer = async (row) => {
  if (!canTransferOp.value) {
    ElMessage.warning('无库存调拨权限')
    return
  }
  loading.value = true
  try {
    await inventoryApi.confirmTransfer(row.id)
    ElMessage.success(`调拨单 ${row.orderNo} 已确认收货`)
    transferDetailVisible.value = false
    await Promise.all([fetchTransferTable(false), fetchInventoryTable(false)])
  } catch (error) {
    ElMessage.error(error.message || '确认失败')
  } finally {
    loading.value = false
  }
}

// 打开入库对话框
const openInboundDialog = async () => {
  try {
    await ensureWarehouseFilterOptionsLoaded()
    if (canInventoryRecordsTab.value) {
      await loadInboundCacheForCurrentPage()
    }
    await loadMorePurchaseOrderOptions({ reset: true, keyword: '', productId: null })
  } catch (error) {
    console.error('加载数据失败:', error)
  }
  inboundForm.value = {
    purchaseOrderId: null,
    warehouseId: warehouseFilterOptions.value[0]?.id || null,
    quantity: 1,
    maxQuantity: 0,
    remark: ''
  }
  inboundDialogVisible.value = true
}

// 选择采购单时自动设置最大入库数量
const onPurchaseOrderChange = (purchaseOrderId) => {
  const po = purchaseOrders.value.find(p => p.id === purchaseOrderId)
  if (po) {
    const pendingQty = po.pendingQuantity || (po.quantity - (po.inboundQuantity || 0))
    inboundForm.value.maxQuantity = pendingQty
    inboundForm.value.quantity = pendingQty
  }
}

// 提交入库
const submitInbound = async () => {
  if (!inboundFormRef.value) return
  await inboundFormRef.value.validate(async (valid) => {
    if (valid) {
      loading.value = true
      try {
        await purchaseApi.inbound(inboundForm.value.purchaseOrderId, {
          warehouseId: inboundForm.value.warehouseId,
          quantity: inboundForm.value.quantity,
          remark: inboundForm.value.remark
        })
        ElMessage.success('入库成功，库存已更新')
        inboundDialogVisible.value = false
        await Promise.all([
          loadMorePurchaseOrderOptions({ reset: true, keyword: '', productId: null }),
          loadInboundCacheForCurrentPage(),
          fetchInventoryTable(false),
          loadStats()
        ])
      } catch (error) {
        ElMessage.error(error.message || '入库失败')
      } finally {
        loading.value = false
      }
    }
  })
}

// 查看入库详情
const viewInboundDetail = (row) => {
  router.push(`/inventory/inbound/${row.id}`)
}

// 查看出库详情
const viewOutboundDetail = (row) => {
  router.push(`/sales/orders/${row.salesOrderId}`)
}

const viewTransferDetail = async (row) => {
  currentTransfer.value = row
  transferDetailVisible.value = true
  transferDetailSourceStock.value = 0
  if (!row?.productId || !row?.fromWarehouseId) return
  try {
    const res = await inventoryApi.list({
      productId: row.productId,
      warehouseId: row.fromWarehouseId,
      page: 1,
      size: 1
    })
    const inv = (res?.list || [])[0]
    transferDetailSourceStock.value = inv?.stock ?? 0
  } catch {
    transferDetailSourceStock.value = 0
  }
}
const viewWarehouseDetail = (w) => {
  currentWarehouse.value = w
  warehouseInventoryCurrentPage.value = 1
  warehouseDetailVisible.value = true
}

// 打开添加仓库对话框
const openWarehouseDialog = () => {
  if (!canInventoryWarehouse.value) {
    ElMessage.warning('无仓库管理权限')
    return
  }
  warehouseEditMode.value = false
  currentWarehouse.value = null
  warehouseForm.value = { name: '', code: '', address: '', capacity: 50, managerName: '', remark: '' }
  warehouseDialogVisible.value = true
}

// 打开编辑仓库对话框
const editWarehouse = (w) => {
  if (!canInventoryWarehouse.value) {
    ElMessage.warning('无仓库管理权限')
    return
  }
  warehouseEditMode.value = true
  currentWarehouse.value = w
  warehouseForm.value = { name: w.name, code: w.code || '', address: w.address, capacity: w.capacityUsed || 50, managerName: w.managerName || '', remark: w.remark || '' }
  warehouseDialogVisible.value = true
}

// 删除仓库
const deleteWarehouse = async (w) => {
  if (!canInventoryWarehouse.value) {
    ElMessage.warning('无仓库管理权限')
    return
  }
  if (w.totalStock > 0) {
    ElMessage.warning('该仓库存在库存，无法删除')
    return
  }

  try {
    await ElMessageBox.confirm(`确定要删除仓库 "${w.name}" 吗？`, '删除确认', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })

    await warehouseApi.delete(w.id)
    ElMessage.success('仓库已删除')
    resetWarehouseFilterOptions()
    await fetchWarehouseGrid()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '删除失败')
    }
  }
}

// 打开手动出库对话框
const openManualOutboundDialog = async () => {
  if (!canInventoryOutbound.value) {
    ElMessage.warning('无出库操作权限')
    return
  }
  await ensureWarehouseFilterOptionsLoaded()
  await loadMoreSalesOrderOptions({ reset: true, keyword: '', productId: null })
  await loadMoreInventoryOptions({ reset: true, keyword: '' })
  manualOutboundType.value = 'sales'
  salesOutboundForm.value = {
    salesOrderId: null,
    warehouseId: null,
    quantity: 1,
    maxQuantity: 0,
    remark: ''
  }
  salesOutboundWarehouseOptions.value = []
  otherOutboundForm.value = {
    inventoryId: null,
    productId: null,
    productName: '',
    warehouseId: null,
    warehouseName: '',
    currentStock: 0,
    quantity: 1,
    maxQuantity: 0,
    remark: ''
  }
  manualOutboundVisible.value = true
}

// 销售出库时选择销售订单自动设置最大出库数量
const onSalesOutboundOrderChange = (salesOrderId) => {
  const so = salesOrders.value.find(s => s.id === salesOrderId)
  if (so) {
    const pendingQty = so.pendingQuantity || so.quantity || 1
    salesOutboundForm.value.maxQuantity = pendingQty > 0 ? pendingQty : 1
    salesOutboundForm.value.quantity = pendingQty > 0 ? pendingQty : 1
    salesOutboundForm.value.warehouseId = null
  } else {
    salesOutboundForm.value.maxQuantity = 0
    salesOutboundForm.value.quantity = 1
    salesOutboundForm.value.warehouseId = null
  }
  void reloadSalesOutboundWarehouseOptions()
}

const onSalesOutboundQuantityChange = () => {
  void reloadSalesOutboundWarehouseOptions()
}

const reloadSalesOutboundWarehouseOptions = async () => {
  const salesOrderId = Number(salesOutboundForm.value.salesOrderId || 0)
  const requiredQty = Number(salesOutboundForm.value.quantity || 0)
  if (!salesOrderId || requiredQty <= 0) {
    salesOutboundWarehouseOptions.value = []
    salesOutboundForm.value.warehouseId = null
    return
  }
  const so = salesOrders.value.find((s) => Number(s.id) === salesOrderId)
  const productId = Number(so?.productId || 0)
  if (!productId) {
    salesOutboundWarehouseOptions.value = []
    salesOutboundForm.value.warehouseId = null
    return
  }

  const requestId = ++salesOutboundWarehouseRequestId
  salesOutboundWarehouseLoading.value = true
  try {
    const size = 100
    let page = 1
    let total = 0
    const eligible = []

    while (page <= 20) {
      const res = await inventoryApi.list({
        page,
        size,
        productId
      })
      const rows = res?.list || []
      total = Number(res?.total) || rows.length
      rows.forEach((row) => {
        const stock = Number(row?.stock || 0)
        if (stock >= requiredQty) {
          eligible.push({
            id: row.warehouseId,
            name: row.warehouseName || getWarehouseName(row.warehouseId) || `仓库${row.warehouseId}`,
            availableStock: stock
          })
        }
      })
      if (rows.length < size || page * size >= total) break
      page += 1
    }

    if (requestId !== salesOutboundWarehouseRequestId) return

    const dedupMap = new Map()
    eligible.forEach((item) => {
      const key = Number(item.id)
      const prev = dedupMap.get(key)
      if (!prev || item.availableStock > prev.availableStock) {
        dedupMap.set(key, item)
      }
    })
    const nextOptions = Array.from(dedupMap.values())
    salesOutboundWarehouseOptions.value = nextOptions
    if (!nextOptions.some((w) => Number(w.id) === Number(salesOutboundForm.value.warehouseId))) {
      salesOutboundForm.value.warehouseId = nextOptions[0]?.id || null
    }
  } catch (error) {
    if (requestId !== salesOutboundWarehouseRequestId) return
    salesOutboundWarehouseOptions.value = []
    salesOutboundForm.value.warehouseId = null
    ElMessage.error(error.message || '加载可出库仓库失败')
  } finally {
    if (requestId === salesOutboundWarehouseRequestId) {
      salesOutboundWarehouseLoading.value = false
    }
  }
}

// 其他出库时选择库存记录自动设置相关信息
const onOtherOutboundInventoryChange = (inventoryId) => {
  const inv = inventoryOptionsLookup.value.find(i => i.id === inventoryId)
  if (inv) {
    const stock = inv.stock || 0
    otherOutboundForm.value.productId = inv.productId
    otherOutboundForm.value.productName = inv.productName
    otherOutboundForm.value.warehouseId = inv.warehouseId
    otherOutboundForm.value.warehouseName = inv.warehouseName
    otherOutboundForm.value.currentStock = stock
    otherOutboundForm.value.maxQuantity = stock > 0 ? stock : 1
    otherOutboundForm.value.quantity = stock > 0 ? Math.min(1, stock) : 1
  }
}

// 提交手动出库
const submitManualOutbound = async () => {
  if (manualOutboundType.value === 'sales') {
    // 销售出库
    if (!salesOutboundFormRef.value) return
    await salesOutboundFormRef.value.validate(async (valid) => {
      if (valid) {
        loading.value = true
        try {
          await salesApi.shipping(salesOutboundForm.value.salesOrderId, {
            warehouseId: salesOutboundForm.value.warehouseId,
            quantity: salesOutboundForm.value.quantity,
            remark: salesOutboundForm.value.remark
          })
          ElMessage.success('出库成功')
          manualOutboundVisible.value = false
          await Promise.all([
            fetchInventoryTable(false),
            loadMoreSalesOrderOptions({ reset: true, keyword: '', productId: null }),
            loadOutboundCacheForCurrentPage(),
            loadStats()
          ])
        } catch (error) {
          ElMessage.error(error.message || '出库失败')
        } finally {
          loading.value = false
        }
      }
    })
  } else {
    // 其他出库
    if (!otherOutboundFormRef.value) return
    await otherOutboundFormRef.value.validate(async (valid) => {
      if (valid) {
        loading.value = true
        try {
          await inventoryApi.outbound(
            otherOutboundForm.value.inventoryId,
            otherOutboundForm.value.quantity,
            otherOutboundForm.value.remark
          )
          ElMessage.success('出库成功')
          manualOutboundVisible.value = false
          await Promise.all([fetchInventoryTable(false), loadOutboundCacheForCurrentPage(), loadStats()])
        } catch (error) {
          ElMessage.error(error.message || '出库失败')
        } finally {
          loading.value = false
        }
      }
    })
  }
}

// 打开手动入库对话框
const openManualInboundDialog = async () => {
  if (!canInventoryInbound.value) {
    ElMessage.warning('无入库操作权限')
    return
  }
  await ensureWarehouseFilterOptionsLoaded()
  await loadMoreProductOptions({ reset: true, keyword: '' })
  await loadMorePurchaseOrderOptions({ reset: true, keyword: '', productId: null })
  manualInboundType.value = 'purchase'
  manualInboundForm.value = {
    productId: null,
    warehouseId: warehouseFilterOptions.value[0]?.id || null,
    quantity: 1,
    remark: ''
  }
  purchaseInboundForm.value = {
    purchaseOrderId: null,
    warehouseId: warehouseFilterOptions.value[0]?.id || null,
    quantity: 1,
    maxQuantity: 0,
    remark: ''
  }
  manualInboundVisible.value = true
}

// 采购入库时选择采购单自动设置最大入库数量
const onPurchaseInboundOrderChange = (purchaseOrderId) => {
  const po = purchaseOrders.value.find(p => p.id === purchaseOrderId)
  if (po) {
    const pendingQty = po.pendingQuantity || (po.quantity || 0) - (po.inboundQuantity || 0)
    purchaseInboundForm.value.maxQuantity = pendingQty > 0 ? pendingQty : 1
    purchaseInboundForm.value.quantity = pendingQty > 0 ? pendingQty : 1
  }
}

// 提交手动入库
const submitManualInbound = async () => {
  if (manualInboundType.value === 'other') {
    // 其他入库
    if (!manualInboundFormRef.value) return
    await manualInboundFormRef.value.validate(async (valid) => {
      if (valid) {
        loading.value = true
        try {
          await inventoryApi.inboundNew({
            productId: manualInboundForm.value.productId,
            warehouseId: manualInboundForm.value.warehouseId,
            quantity: manualInboundForm.value.quantity,
            remark: manualInboundForm.value.remark
          })
          ElMessage.success('入库成功')
          manualInboundVisible.value = false
          await Promise.all([fetchInventoryTable(false), loadInboundCacheForCurrentPage(), loadStats()])
        } catch (error) {
          ElMessage.error(error.message || '入库失败')
        } finally {
          loading.value = false
        }
      }
    })
  } else {
    // 采购入库
    if (!purchaseInboundFormRef.value) return
    await purchaseInboundFormRef.value.validate(async (valid) => {
      if (valid) {
        loading.value = true
        try {
          await purchaseApi.inbound(purchaseInboundForm.value.purchaseOrderId, {
            warehouseId: purchaseInboundForm.value.warehouseId,
            quantity: purchaseInboundForm.value.quantity,
            remark: purchaseInboundForm.value.remark
          })
          ElMessage.success('入库成功')
          manualInboundVisible.value = false
          await Promise.all([
            fetchInventoryTable(false),
            loadMorePurchaseOrderOptions({ reset: true, keyword: '', productId: null }),
            loadInboundCacheForCurrentPage(),
            loadStats()
          ])
        } catch (error) {
          ElMessage.error(error.message || '入库失败')
        } finally {
          loading.value = false
        }
      }
    })
  }
}

// 更新库存预警值
const updateSafeStock = async () => {
  if (!canInventoryAdjust.value) {
    ElMessage.warning('无库存调整权限')
    return
  }
  if (!currentStock.value || !stockSafeStockEdit.value) return
  loading.value = true
  try {
    await inventoryApi.updateSafeStock(currentStock.value.id, stockSafeStockEdit.value)
    ElMessage.success('库存预警值已更新')
    currentStock.value.safeStock = stockSafeStockEdit.value
    await fetchInventoryTable(false)
  } catch (error) {
    ElMessage.error(error.message || '更新失败')
  } finally {
    loading.value = false
  }
}

// 更新库位
const updateLocation = async () => {
  if (!canInventoryAdjust.value) {
    ElMessage.warning('无库存调整权限')
    return
  }
  if (!currentStock.value) return
  loading.value = true
  try {
    await inventoryApi.updateLocation(currentStock.value.id, stockLocationEdit.value)
    ElMessage.success('库位已更新')
    currentStock.value.location = stockLocationEdit.value
    await fetchInventoryTable(false)
  } catch (error) {
    ElMessage.error(error.message || '更新失败')
  } finally {
    loading.value = false
  }
}

// 更新呆滞预警天数
const updateStagnantDays = async () => {
  if (!canInventoryAdjust.value) {
    ElMessage.warning('无库存调整权限')
    return
  }
  if (!currentStock.value || !stockStagnantDaysEdit.value) return
  loading.value = true
  try {
    await inventoryApi.updateStagnantDays(currentStock.value.id, stockStagnantDaysEdit.value)
    ElMessage.success('呆滞预警天数已更新')
    currentStock.value.stagnantDays = stockStagnantDaysEdit.value
    await fetchInventoryTable(false)
  } catch (error) {
    ElMessage.error(error.message || '更新失败')
  } finally {
    loading.value = false
  }
}

const parseInventoryMoment = (raw) => {
  if (raw == null || raw === '') return null
  if (raw instanceof Date && !Number.isNaN(raw.getTime())) return raw
  const s = typeof raw === 'string' ? raw.trim().replace(' ', 'T') : String(raw)
  const d = new Date(s)
  return Number.isNaN(d.getTime()) ? null : d
}

// 计算呆滞天数：最后出库 → 最后入库 → 建档时间（与后端列表/筛选一致）
const getStagnantDays = (stock) => {
  const ref =
    parseInventoryMoment(stock?.lastOutboundTime) ||
    parseInventoryMoment(stock?.lastInboundTime) ||
    parseInventoryMoment(stock?.createTime)
  if (!ref) return 0
  return Math.floor((Date.now() - ref.getTime()) / (1000 * 60 * 60 * 24))
}

const getStagnantWarningDays = (stock) => {
  const row = stock?.stagnantDays
  if (typeof row === 'number' && row >= 1) return row
  const g = systemStaleDays.value
  return typeof g === 'number' && g >= 1 ? g : 90
}

// 呆滞状态类型
const getStagnantStatusType = (stock) => {
  const stagnantDays = getStagnantDays(stock)
  const warningDays = getStagnantWarningDays(stock)
  if (stagnantDays >= warningDays) return 'danger'
  return 'success'
}

// 呆滞状态文本
const getStagnantStatusText = (stock) => {
  const stagnantDays = getStagnantDays(stock)
  const warningDays = getStagnantWarningDays(stock)
  if (stagnantDays >= warningDays) return `呆滞${stagnantDays}天`
  return `正常`
}

// 先同步 URL 再拉当前 Tab，避免 immediate 的 activeTab 与 query 不一致
watch(
  () => route.query,
  (query) => {
    if (query.tab) {
      activeTab.value = parseMainTab(query.tab)
    }
    if (query.subtab) {
      recordsSubTab.value = parseRecordsSubTab(query.subtab)
    }
    syncActiveTabToPermissions()
  },
  { immediate: true }
)

watch(
  () => activeTab.value,
  () => {
    refreshActiveTabData()
  },
  { immediate: true }
)

watch(
  () => recordsSubTab.value,
  (sub) => {
    if (activeTab.value !== 'records' || !canInventoryRecordsTab.value) return
    if (sub === 'inbound') void loadInboundCacheForCurrentPage()
    else if (sub === 'outbound') void loadOutboundCacheForCurrentPage()
  }
)

watch(
  [warehouseDetailVisible, currentWarehouse, warehouseInventoryCurrentPage, warehouseInventoryPageSize],
  () => {
    if (warehouseDetailVisible.value) {
      fetchWarehouseDetailInventory()
    }
  }
)

watch(
  [canInventoryStockTab, canInventoryRecordsTab, canInventoryTransferTab, canInventoryWarehouseTab],
  () => syncActiveTabToPermissions()
)
</script>

<style lang="scss" scoped>
.inventory-page {
  .records-filter-bar {
    display: flex;
    flex-wrap: wrap;
    gap: 10px;
    align-items: center;
    margin-bottom: 12px;
  }
  /** 筛选区日期范围：控制宽度；高度与默认表单项一致（不使用 size=small） */
  :deep(.inventory-filter-daterange.el-date-editor--daterange) {
    width: 200px;
    max-width: 200px;
    .el-range-separator {
      flex: 0 0 auto;
      width: 10px;
      padding: 0 2px;
    }
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
      .warning-text { color: #F56C6C; }
    }
  }
  .stats-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 20px; margin-bottom: 24px; }
  .stat-card {
    :deep(.el-card__body) { padding: 20px; }
    .stat-icon { width: 48px; height: 48px; border-radius: 12px; display: flex; align-items: center; justify-content: center; font-size: 24px; margin-bottom: 16px;
      &--purple { background: rgba(139, 92, 246, 0.1); color: #8B5CF6; }
      &--green { background: rgba(103, 194, 58, 0.1); color: #67C23A; }
      &--red { background: rgba(245, 108, 108, 0.1); color: #F56C6C; }
      &--blue { background: rgba(64, 158, 255, 0.1); color: #409EFF; }
    }
    .stat-value { font-size: 28px; font-weight: 700; color: #303133; margin-bottom: 4px; }
    .stat-label { font-size: 14px; color: #909399; margin-bottom: 12px; }
    .stat-trend { display: flex; align-items: center; gap: 4px; font-size: 13px;
      &--up { color: #67C23A; }
      &--down { color: #F56C6C; }
      &--warning { color: #E6A23C; }
    }
  }
  .page-tabs { :deep(.el-tabs__header) { margin-bottom: 16px; } }
  .card-header {
    display: flex;
    justify-content: flex-end;
    align-items: flex-start;
    flex-wrap: wrap;
    gap: 12px;
    .header-actions {
      display: flex;
      align-items: center;
      gap: 10px;
      flex-wrap: wrap;
      flex: 1;
      min-width: 0;
      justify-content: flex-end;
    }
    .header-actions-inout {
      display: inline-flex;
      align-items: center;
      gap: 8px;
      flex-wrap: nowrap;
    }
  }
  .sku { color: #E94560; }
  .order-no { color: #E94560; &.success { color: #67C23A; } &.warning { color: #E6A23C; } }
  .amount { font-weight: 600; color: #E94560; }
  .warehouse-name { font-weight: 600; color: #409EFF; }
  .low-stock { color: #E6A23C; font-weight: 600; }
  .critical-stock { color: #F56C6C; font-weight: 700; }
  .product-cell { display: flex; align-items: center; gap: 12px;
    .product-thumb { flex-shrink: 0; border-radius: 8px; overflow: hidden; }
    .product-icon { width: 40px; height: 40px; border-radius: 8px; background: #F5F7FA; display: flex; align-items: center; justify-content: center; font-size: 20px; }
    .product-info { h4 { font-size: 14px; font-weight: 600; color: #303133; } p { font-size: 12px; color: #909399; } }
  }
  .product-cell-mini {
    display: flex;
    align-items: center;
    gap: 8px;
    .product-thumb-mini { flex-shrink: 0; border-radius: 4px; overflow: hidden; }
    .product-icon-mini { width: 32px; height: 32px; display: flex; align-items: center; justify-content: center; background: #F5F7FA; border-radius: 4px; font-size: 16px; }
    .product-name-mini { font-size: 13px; color: #303133; }
  }
  .grid-empty {
    padding: 32px 0;
  }

  .warehouse-search-bar {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 20px;
  }

  .warehouse-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 20px; }
  .warehouse-card {
    :deep(.el-card__body) { padding: 24px; }
    .warehouse-header { display: flex; align-items: center; gap: 16px; margin-bottom: 20px; }
    .warehouse-icon { width: 60px; height: 60px; border-radius: 16px; background: rgba(233, 69, 96, 0.15); display: flex; align-items: center; justify-content: center; color: #E94560; font-size: 32px; }
    .warehouse-info { h4 { font-size: 18px; font-weight: 600; color: #303133; margin-bottom: 4px; } p { font-size: 13px; color: #909399; } }
    .warehouse-stats { display: grid; grid-template-columns: repeat(2, 1fr); gap: 16px; padding: 16px 0; border-top: 1px solid #E4E7ED; border-bottom: 1px solid #E4E7ED; margin-bottom: 16px; }
    .warehouse-stat { text-align: center; .stat-label { display: block; font-size: 12px; color: #909399; margin-bottom: 4px; } .stat-value { font-size: 16px; font-weight: 700; color: #303133; } }
    .warehouse-capacity { display: flex; align-items: center; gap: 12px;
      .capacity-bar { flex: 1; height: 8px; background: #E4E7ED; border-radius: 4px; overflow: hidden; .capacity-fill { height: 100%; border-radius: 4px; transition: width 0.5s ease; } }
      span { font-size: 13px; color: #606266; white-space: nowrap; }
    }
    .warehouse-actions { display: flex; gap: 12px; margin-top: 16px; }
  }
  .quantity-tip { font-size: 12px; color: #E6A23C; margin-top: 4px; }

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

  .sales-order-option {
    display: flex;
    align-items: flex-start;
    gap: 8px;
    min-width: 0;

    .sales-order-option__thumb {
      width: 24px;
      height: 24px;
      border-radius: 4px;
      border: 1px solid #e4e7ed;
      object-fit: cover;
      flex-shrink: 0;
      background: #f5f7fa;
    }

    .sales-order-option__thumb-placeholder {
      width: 24px;
      height: 24px;
      border-radius: 4px;
      border: 1px solid #e4e7ed;
      display: inline-flex;
      align-items: center;
      justify-content: center;
      background: #f5f7fa;
      color: #909399;
      font-size: 12px;
      flex-shrink: 0;
    }

    .sales-order-option__meta {
      min-width: 0;
      flex: 1;
    }

    .sales-order-option__text {
      display: block;
      color: #909399;
      font-size: 12px;
      line-height: 1.4;
      white-space: normal;
      word-break: break-all;
    }
  }

  .pagination-wrapper { display: flex; justify-content: flex-end; padding-top: 16px; }
  .image-query-thumb {
    width: 32px;
    height: 32px;
    object-fit: cover;
    border-radius: 6px;
    vertical-align: middle;
    border: 1px solid #e4e7ed;
  }

  // 库存详情商品图片展示
  .stock-product-image {
    display: flex;
    align-items: center;
    gap: 16px;
    padding: 16px;
    background: #F5F7FA;
    border-radius: 8px;
    margin-bottom: 16px;

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
}

:deep(.warehouse-detail-dialog .warehouse-detail-descriptions .el-descriptions__label),
:deep(.warehouse-detail-dialog .warehouse-detail-descriptions .el-descriptions__content) {
  vertical-align: top;
}

:deep(.transfer-detail-dialog .transfer-detail-descriptions .el-descriptions__label),
:deep(.transfer-detail-dialog .transfer-detail-descriptions .el-descriptions__content) {
  vertical-align: top;
}

:deep(.transfer-detail-dialog .transfer-remark-panel) {
  margin-top: 12px;
  border: 1px solid #ebeef5;
  border-radius: 4px;
}

:deep(.transfer-detail-dialog .transfer-remark-title) {
  padding: 10px 12px;
  background: #fafafa;
  border-bottom: 1px solid #ebeef5;
  color: #606266;
  font-weight: 600;
}

:deep(.transfer-detail-dialog .transfer-remark-content) {
  padding: 10px 12px;
  max-height: 96px;
  line-height: 1.5;
  overflow-y: auto;
  overflow-x: hidden;
  word-break: break-all;
  white-space: normal;
}

:deep(.warehouse-detail-dialog .warehouse-detail-descriptions .descriptions-long-text) {
  max-height: 4.5em;
  line-height: 1.5;
  overflow-y: auto;
  overflow-x: hidden;
  word-break: break-all;
  white-space: normal;
}

:deep(.warehouse-detail-dialog .warehouse-remark-panel) {
  margin-top: 12px;
  border: 1px solid #ebeef5;
  border-radius: 4px;
}

:deep(.warehouse-detail-dialog .warehouse-remark-title) {
  padding: 10px 12px;
  background: #fafafa;
  border-bottom: 1px solid #ebeef5;
  color: #606266;
  font-weight: 600;
}

:deep(.warehouse-detail-dialog .warehouse-remark-content) {
  padding: 10px 12px;
  max-height: 96px;
  line-height: 1.5;
  overflow-y: auto;
  overflow-x: hidden;
  word-break: break-all;
  white-space: normal;
}

:deep(.inventory-stock-outbound-so-dropdown .el-select-dropdown__item),
:deep(.inventory-manual-outbound-so-dropdown .el-select-dropdown__item) {
  height: auto;
  min-height: 40px;
  line-height: normal;
  padding-top: 6px;
  padding-bottom: 6px;
}

:deep(.inventory-stock-outbound-so-dropdown .el-select-dropdown__item.is-hovering),
:deep(.inventory-manual-outbound-so-dropdown .el-select-dropdown__item.is-hovering) {
  height: auto;
}
</style>

<style lang="scss">
.inventory-stock-outbound-so-dropdown.el-select-dropdown,
.inventory-manual-outbound-so-dropdown.el-select-dropdown {
  width: min(90vw, 560px) !important;
  max-width: min(90vw, 560px) !important;
}

.inventory-stock-outbound-so-dropdown .el-select-dropdown__item,
.inventory-manual-outbound-so-dropdown .el-select-dropdown__item {
  height: auto !important;
  min-height: 40px;
  line-height: normal !important;
  white-space: normal;
  padding-top: 6px;
  padding-bottom: 6px;
}

.inventory-stock-outbound-so-dropdown .sales-order-option,
.inventory-manual-outbound-so-dropdown .sales-order-option {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  min-width: 0;
}

.inventory-stock-outbound-so-dropdown .sales-order-option__thumb,
.inventory-manual-outbound-so-dropdown .sales-order-option__thumb {
  width: 24px;
  height: 24px;
  border-radius: 4px;
  border: 1px solid #e4e7ed;
  object-fit: cover;
  flex-shrink: 0;
  background: #f5f7fa;
}

.inventory-stock-outbound-so-dropdown .sales-order-option__thumb-placeholder,
.inventory-manual-outbound-so-dropdown .sales-order-option__thumb-placeholder {
  width: 24px;
  height: 24px;
  border-radius: 4px;
  border: 1px solid #e4e7ed;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: #f5f7fa;
  color: #909399;
  font-size: 12px;
  flex-shrink: 0;
}

.inventory-stock-outbound-so-dropdown .sales-order-option__meta,
.inventory-manual-outbound-so-dropdown .sales-order-option__meta {
  min-width: 0;
  flex: 1;
}

.inventory-stock-outbound-so-dropdown .sales-order-option__text,
.inventory-manual-outbound-so-dropdown .sales-order-option__text {
  display: block;
  color: #606266;
  font-size: 12px;
  line-height: 1.4;
  white-space: normal;
  word-break: break-all;
}
</style>