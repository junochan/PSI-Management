<template>
  <div class="inventory-page">
    <el-tabs v-model="activeTab" class="page-tabs">
      <!-- 库存管理 -->
      <el-tab-pane v-if="canInventoryStockTab" label="库存管理" name="stock">
        <el-card>
          <template #header>
            <div class="card-header">
              <div class="header-actions">
                <el-select v-model="filterProduct" placeholder="按商品筛选" clearable filterable style="width: 160px">
                  <el-option v-for="p in products" :key="p.id" :label="p.name" :value="p.id" />
                </el-select>
                <el-select v-model="filterWarehouse" placeholder="按仓库筛选" clearable filterable style="width: 140px">
                  <el-option v-for="w in warehousesList" :key="w.id" :label="w.name" :value="w.id" />
                </el-select>
                <el-select v-model="filterStagnantStatus" placeholder="按呆滞状态筛选" clearable style="width: 120px">
                  <el-option label="呆滞" value="stagnant" />
                  <el-option label="正常" value="normal" />
                </el-select>
                <el-date-picker
                  v-model="filterLastOutboundRange"
                  type="daterange"
                  range-separator="至"
                  start-placeholder="最后出库开始"
                  end-placeholder="最后出库结束"
                  style="width: 200px"
                  value-format="YYYY-MM-DD"
                />
                <el-date-picker
                  v-model="filterLastInboundRange"
                  type="daterange"
                  range-separator="至"
                  start-placeholder="最后入库开始"
                  end-placeholder="最后入库结束"
                  style="width: 200px"
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
                  <img v-if="getProductImage(row.productId)" :src="getProductImage(row.productId)" class="product-thumb" />
                  <div v-else class="product-icon">{{ getCategoryIcon(getCategoryName(row.productId) || row.category) }}</div>
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
            <el-table-column label="操作" width="160" fixed="right" align="center">
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
        <el-card>
          <template #header>
            <div class="card-header">
              <el-button v-if="canTransferOp" type="primary" @click="openTransferDialog()"><el-icon><Plus /></el-icon>新建调拨</el-button>
            </div>
          </template>
          <el-table :data="paginatedTransfers" empty-text="暂无数据" style="width: 100%" :max-height="500" table-layout="fixed">
            <el-table-column label="单号" min-width="132" show-overflow-tooltip>
              <template #default="{ row }"><span class="order-no">{{ row.orderNo }}</span></template>
            </el-table-column>
            <el-table-column label="商品" prop="productName" min-width="108" show-overflow-tooltip />
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
            <el-table-column label="操作" width="120" fixed="right" align="center">
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
              :total="transfers.length"
              :page-sizes="[10, 20, 50, 100]"
              layout="total, sizes, prev, pager, next, jumper"
            />
          </div>
        </el-card>
      </el-tab-pane>

      <!-- 出入库记录 -->
      <el-tab-pane v-if="canInventoryRecordsTab" label="出入库记录" name="records">
        <el-tabs v-model="recordsSubTab" type="card">
          <!-- 入库记录 -->
          <el-tab-pane label="入库记录" name="inbound">
            <el-card>
              <el-table :data="paginatedRecordsInbound" empty-text="暂无数据" style="width: 100%" :max-height="500">
                <el-table-column label="入库单号" width="140">
                  <template #default="{ row }"><span class="order-no success">{{ row.orderNo }}</span></template>
                </el-table-column>
                <el-table-column label="采购单号" width="140">
                  <template #default="{ row }"><span class="order-no">{{ row.purchaseOrderNo || row.purchaseNo }}</span></template>
                </el-table-column>
                <el-table-column label="供应商" min-width="120" show-overflow-tooltip>
                  <template #default="{ row }">{{ row.supplierName || row.supplier }}</template>
                </el-table-column>
                <el-table-column label="商品" min-width="140" show-overflow-tooltip>
                  <template #default="{ row }">{{ row.productName || row.product }}</template>
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
                  :total="inboundRecords.length"
                  :page-sizes="[10, 20, 50, 100]"
                  layout="total, sizes, prev, pager, next, jumper"
                />
              </div>
            </el-card>
          </el-tab-pane>
          <!-- 出库记录 -->
          <el-tab-pane label="出库记录" name="outbound">
            <el-card>
              <el-table :data="paginatedRecordsOutbound" empty-text="暂无数据" style="width: 100%" :max-height="500">
                <el-table-column label="出库单号" width="140">
                  <template #default="{ row }"><span class="order-no warning">{{ row.orderNo }}</span></template>
                </el-table-column>
                <el-table-column label="销售单号" width="140">
                  <template #default="{ row }"><span class="order-no">{{ row.salesOrderNo || row.salesNo }}</span></template>
                </el-table-column>
                <el-table-column label="客户" min-width="120" show-overflow-tooltip>
                  <template #default="{ row }">{{ row.customerName || row.customer }}</template>
                </el-table-column>
                <el-table-column label="商品" min-width="140" show-overflow-tooltip>
                  <template #default="{ row }">{{ row.productName || row.product }}</template>
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
                  :total="outboundRecords.length"
                  :page-sizes="[10, 20, 50, 100]"
                  layout="total, sizes, prev, pager, next, jumper"
                />
              </div>
            </el-card>
          </el-tab-pane>
        </el-tabs>
      </el-tab-pane>

      <!-- 仓库管理 -->
      <el-tab-pane v-if="canInventoryWarehouseTab" label="仓库管理" name="warehouse">
        <el-empty v-if="!warehousesList.length" class="grid-empty" description="暂无数据" :image-size="80" />
        <div v-else class="warehouse-grid">
          <el-card class="warehouse-card" v-for="w in warehousesList" :key="w.id">
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
        <el-button v-if="canInventoryWarehouse" type="primary" class="add-warehouse-btn" @click="openWarehouseDialog"><el-icon><Plus /></el-icon>添加仓库</el-button>
      </el-tab-pane>
    </el-tabs>

    <!-- 调拨对话框 -->
    <el-dialog v-model="transferDialogVisible" title="新建调拨单" width="650px" destroy-on-close>
      <el-form ref="transferFormRef" :model="transferForm" :rules="transferRules" label-width="100px">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="调出仓库" prop="fromWarehouseId">
              <el-select v-model="transferForm.fromWarehouseId" placeholder="请选择仓库" style="width: 100%" filterable>
                <el-option v-for="w in warehousesList" :key="w.id" :label="w.name" :value="w.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="调入仓库" prop="toWarehouseId">
              <el-select v-model="transferForm.toWarehouseId" placeholder="请选择仓库" style="width: 100%" filterable>
                <el-option v-for="w in warehousesList" :key="w.id" :label="w.name" :value="w.id" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="选择商品" prop="productId">
              <el-select v-model="transferForm.productId" placeholder="请选择商品" style="width: 100%" filterable>
                <el-option v-for="inv in inventoryData" :key="inv.productId" :label="`${inv.productName || inv.name}`" :value="inv.productId" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="商品图片">
              <div class="selected-product-image">
                <img v-if="transferProductImage" :src="transferProductImage" class="product-preview" />
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
        <el-form-item label="调拨说明">
          <el-input v-model="transferForm.remark" placeholder="输入调拨说明" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="transferDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitTransfer" :loading="loading">确认调拨</el-button>
      </template>
    </el-dialog>

    <!-- 调拨详情对话框 -->
    <el-dialog v-model="transferDetailVisible" title="调拨单详情" width="600px" destroy-on-close>
      <el-descriptions :column="2" border>
        <el-descriptions-item label="调拨单号"><span class="order-no">{{ currentTransfer?.orderNo }}</span></el-descriptions-item>
        <el-descriptions-item label="SKU编码">{{ currentTransfer?.sku }}</el-descriptions-item>
        <el-descriptions-item label="商品名称" :span="2">{{ currentTransfer?.productName }}</el-descriptions-item>
        <el-descriptions-item label="调出仓库"><span class="warehouse-name">{{ getWarehouseName(currentTransfer?.fromWarehouseId) || currentTransfer?.fromWarehouseName }}</span></el-descriptions-item>
        <el-descriptions-item label="调入仓库"><span class="warehouse-name">{{ getWarehouseName(currentTransfer?.toWarehouseId) || currentTransfer?.toWarehouseName }}</span></el-descriptions-item>
        <el-descriptions-item label="调拨数量">{{ currentTransfer?.quantity }} 件</el-descriptions-item>
        <el-descriptions-item label="可调拨数量">
          <span :class="{ 'low-stock': transferAvailableStock < currentTransfer?.quantity }">{{ transferAvailableStock }} 件</span>
        </el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="getTransferStatusType(currentTransfer?.status)" effect="light">{{ formatTransferStatus(currentTransfer?.status) }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ currentTransfer?.createTime }}</el-descriptions-item>
        <el-descriptions-item label="操作人">{{ currentTransfer?.operatorName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="完成时间">{{ currentTransfer?.completeTime || '-' }}</el-descriptions-item>
        <el-descriptions-item label="备注" :span="2">{{ currentTransfer?.remark || '-' }}</el-descriptions-item>
      </el-descriptions>
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
          <el-input v-model="warehouseForm.name" placeholder="输入仓库名称" />
        </el-form-item>
        <el-form-item label="仓库地址">
          <el-input v-model="warehouseForm.address" placeholder="输入仓库地址" />
        </el-form-item>
        <el-form-item label="容量利用率">
          <el-slider v-model="warehouseForm.capacity" :min="0" :max="100" show-input />
        </el-form-item>
        <el-form-item label="负责人">
          <el-input v-model="warehouseForm.managerName" placeholder="输入负责人姓名" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="warehouseForm.remark" type="textarea" :rows="3" placeholder="输入备注" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="warehouseDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitWarehouse">{{ warehouseEditMode ? '保存修改' : '确认添加' }}</el-button>
      </template>
    </el-dialog>

    <!-- 仓库详情对话框 -->
    <el-dialog v-model="warehouseDetailVisible" title="仓库详情" width="700px" destroy-on-close>
      <el-descriptions :column="2" border>
        <el-descriptions-item label="仓库名称">{{ currentWarehouse?.name }}</el-descriptions-item>
        <el-descriptions-item label="仓库编码">{{ currentWarehouse?.code || '-' }}</el-descriptions-item>
        <el-descriptions-item label="仓库地址" :span="2">{{ currentWarehouse?.address }}</el-descriptions-item>
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
        <el-descriptions-item label="备注" :span="2">{{ currentWarehouse?.remark || '-' }}</el-descriptions-item>
      </el-descriptions>

      <!-- 仓库内库存商品列表 -->
      <el-divider content-position="left">库存商品</el-divider>
      <el-table :data="paginatedWarehouseInventoryList" empty-text="暂无数据" style="width: 100%" max-height="300" size="small">
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
      <div class="pagination-wrapper" v-if="warehouseInventoryList.length > 0">
        <el-pagination
          v-model:current-page="warehouseInventoryCurrentPage"
          v-model:page-size="warehouseInventoryPageSize"
          :total="warehouseInventoryList.length"
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
      <div class="stock-product-image" v-if="getProductImage(currentStock?.productId)">
        <img :src="getProductImage(currentStock?.productId)" class="product-image-preview" />
        <div class="product-image-info">
          <h4>{{ currentStock?.productName || currentStock?.name }}</h4>
          <p>SKU: {{ currentStock?.sku }}</p>
        </div>
      </div>
      <el-descriptions :column="2" border>
        <el-descriptions-item label="SKU编码"><span class="sku">{{ currentStock?.sku }}</span></el-descriptions-item>
        <el-descriptions-item label="商品名称">{{ currentStock?.productName || currentStock?.name }}</el-descriptions-item>
        <el-descriptions-item label="规格">{{ currentStock?.spec || '-' }}</el-descriptions-item>
        <el-descriptions-item label="分类">{{ getCategoryName(currentStock?.productId) || currentStock?.category || '-' }}</el-descriptions-item>
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

    <!-- 入库对话框 -->
    <el-dialog v-model="inboundDialogVisible" title="新建入库" width="500px" destroy-on-close>
      <el-alert v-if="pendingPurchaseOrders.length === 0" type="warning" title="当前没有待入库的采购单" description="请先在采购管理中创建采购单，再进行入库操作。" :closable="false" show-icon style="margin-bottom: 16px" />
      <el-form ref="inboundFormRef" :model="inboundForm" :rules="inboundRules" label-width="100px">
        <el-form-item label="关联采购单" prop="purchaseOrderId">
          <el-select v-model="inboundForm.purchaseOrderId" placeholder="请选择采购单" style="width: 100%" @change="onPurchaseOrderChange" :disabled="pendingPurchaseOrders.length === 0">
            <el-option v-for="po in pendingPurchaseOrders" :key="po.id" :label="`#${po.orderNo} - ${po.supplierName || ''} - ${po.productName || ''} - 待入库${po.pendingQuantity || (po.totalQuantity - (po.inboundQuantity || 0))}件`" :value="po.id" />
          </el-select>
        </el-form-item>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="入库仓库" prop="warehouseId">
              <el-select v-model="inboundForm.warehouseId" style="width: 100%" filterable>
                <el-option v-for="w in warehousesList" :key="w.id" :label="w.name" :value="w.id" />
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
          <el-input v-model="inboundForm.remark" placeholder="输入入库备注" />
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
              <el-select v-model="manualInboundForm.productId" placeholder="请选择商品" style="width: 100%" filterable>
                <el-option v-for="p in productsForManualOtherInbound" :key="p.id" :label="p.name" :value="p.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="商品图片">
              <div class="selected-product-image">
                <img v-if="manualInboundProductImage" :src="manualInboundProductImage" class="product-preview" />
                <div v-else class="product-preview-placeholder">
                  <span class="placeholder-icon">📦</span>
                  <span class="placeholder-text">请先选择商品</span>
                </div>
              </div>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="入库仓库" prop="warehouseId">
          <el-select v-model="manualInboundForm.warehouseId" placeholder="请选择仓库" style="width: 100%" filterable>
            <el-option v-for="w in warehousesList" :key="w.id" :label="w.name" :value="w.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="入库数量" prop="quantity">
          <el-input-number v-model="manualInboundForm.quantity" :min="1" style="width: 100%" />
        </el-form-item>
        <el-form-item label="入库备注">
          <el-input v-model="manualInboundForm.remark" placeholder="输入入库备注" />
        </el-form-item>
      </el-form>

      <!-- 采购入库表单 -->
      <el-form v-if="manualInboundType === 'purchase'" ref="purchaseInboundFormRef" :model="purchaseInboundForm" :rules="purchaseInboundRules" label-width="100px">
        <el-alert v-if="pendingPurchaseOrders.length === 0" type="warning" title="当前没有待入库的采购单" description="请先在采购管理中创建采购单。" :closable="false" show-icon style="margin-bottom: 16px" />
        <el-form-item label="采购单" prop="purchaseOrderId">
          <el-select v-model="purchaseInboundForm.purchaseOrderId" placeholder="请选择采购单" style="width: 100%" @change="onPurchaseInboundOrderChange" :disabled="pendingPurchaseOrders.length === 0">
            <el-option v-for="po in pendingPurchaseOrders" :key="po.id" :label="`#${po.orderNo} - ${po.supplierName || ''} - ${po.productName || ''} - 待入库${po.pendingQuantity || (po.quantity - (po.inboundQuantity || 0))}件`" :value="po.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="入库仓库" prop="warehouseId">
          <el-select v-model="purchaseInboundForm.warehouseId" placeholder="请选择仓库" style="width: 100%" filterable>
            <el-option v-for="w in warehousesList" :key="w.id" :label="w.name" :value="w.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="入库数量" prop="quantity">
          <el-input-number v-model="purchaseInboundForm.quantity" :min="1" :max="purchaseInboundForm.maxQuantity || 9999" style="width: 100%" />
          <div class="quantity-tip" v-if="purchaseInboundForm.maxQuantity">最大可入库: {{ purchaseInboundForm.maxQuantity }} 件</div>
        </el-form-item>
        <el-form-item label="入库备注">
          <el-input v-model="purchaseInboundForm.remark" placeholder="输入入库备注" />
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
          <el-select v-model="salesOutboundForm.salesOrderId" placeholder="请选择销售订单" style="width: 100%" @change="onSalesOutboundOrderChange" :disabled="pendingSalesOrders.length === 0">
            <el-option v-for="so in pendingSalesOrders" :key="so.id" :label="`#${so.orderNo} - ${so.customerName || ''} - ${so.productName || ''} - 待发货${so.pendingQuantity || so.quantity}件`" :value="so.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="出库仓库" prop="warehouseId">
          <el-select v-model="salesOutboundForm.warehouseId" placeholder="请选择仓库" style="width: 100%" filterable>
            <el-option v-for="w in warehousesList" :key="w.id" :label="w.name" :value="w.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="出库数量" prop="quantity">
          <el-input-number v-model="salesOutboundForm.quantity" :min="1" :max="salesOutboundForm.maxQuantity || 9999" style="width: 100%" />
          <div class="quantity-tip" v-if="salesOutboundForm.maxQuantity">最大可出库: {{ salesOutboundForm.maxQuantity }} 件</div>
        </el-form-item>
        <el-form-item label="出库备注">
          <el-input v-model="salesOutboundForm.remark" placeholder="输入出库备注" />
        </el-form-item>
      </el-form>

      <!-- 其他出库表单 -->
      <el-form v-if="manualOutboundType === 'other'" ref="otherOutboundFormRef" :model="otherOutboundForm" :rules="otherOutboundRules" label-width="100px">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="选择库存" prop="inventoryId">
              <el-select v-model="otherOutboundForm.inventoryId" placeholder="请选择库存记录" style="width: 100%" filterable @change="onOtherOutboundInventoryChange">
                <el-option v-for="inv in inventoryData" :key="inv.id" :label="`${inv.productName} - ${getWarehouseName(inv.warehouseId) || inv.warehouseName}`" :value="inv.id" :disabled="inv.stock <= 0" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="商品图片">
              <div class="selected-product-image">
                <img v-if="otherOutboundProductImage" :src="otherOutboundProductImage" class="product-preview" />
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
          <el-input v-model="otherOutboundForm.remark" placeholder="输入出库原因（必填）" />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="manualOutboundVisible = false">取消</el-button>
        <el-button type="primary" @click="submitManualOutbound" :loading="loading" :disabled="manualOutboundType === 'sales' && pendingSalesOrders.length === 0">确认出库</el-button>
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
import { inventoryApi, warehouseApi, purchaseApi, salesApi } from '@/api'
import { firstProductImageUrl } from '@/utils/productImages'
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

const activeTab = ref('stock')

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

const recordsSubTab = ref('inbound')
const searchKeyword = ref('')
const queryImageDataUrl = ref('')
const imageSimilarityThreshold = ref(0.7)
const imageSearchMode = ref(false)
const imageSearchLoading = ref(false)
const imageSearchRows = ref([])
const imageSearchTotal = ref(0)
const filterProduct = ref(null)
const filterWarehouse = ref(null)
const filterStagnantStatus = ref(null)
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
const warehouseInventoryCurrentPage = ref(1)
const warehouseInventoryPageSize = ref(5)
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

// 数据从后端获取
const inventoryData = computed(() => dataStore.inventoryData || [])
const transfers = computed(() => dataStore.transfers || [])
const products = computed(() => dataStore.products || [])
/** 手动入库「其他入库」选商品：与采购一致排除停售 */
const productsForManualOtherInbound = computed(() => products.value.filter(isProductSelectableForOrder))
const warehousesList = computed(() => dataStore.warehouses || [])
const categoriesList = computed(() => dataStore.categories || [])
const suppliersList = computed(() => dataStore.suppliers || [])
const inboundRecords = computed(() => dataStore.inboundRecords || [])
const outboundRecords = computed(() => dataStore.outboundRecords || [])
const purchaseOrders = computed(() => dataStore.purchaseOrders || [])
const salesOrders = computed(() => dataStore.salesOrders || [])

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

// 当前仓库的库存商品列表
const warehouseInventoryList = computed(() => {
  if (!currentWarehouse.value) return []
  return inventoryData.value.filter(i =>
    i.warehouseId === currentWarehouse.value.id
  )
})

// 分页后的仓库库存列表
const paginatedWarehouseInventoryList = computed(() => {
  const start = (warehouseInventoryCurrentPage.value - 1) * warehouseInventoryPageSize.value
  const end = start + warehouseInventoryPageSize.value
  return warehouseInventoryList.value.slice(start, end)
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

// 调拨详情中可调拨数量（源仓库当前库存）
const transferAvailableStock = computed(() => {
  if (!currentTransfer.value) return 0
  const inv = inventoryData.value.find(i =>
    i.productId === currentTransfer.value.productId &&
    i.warehouseId === currentTransfer.value.fromWarehouseId
  )
  return inv?.stock || 0
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

const fetchInventoryTable = async (showLoading = true) => {
  if (imageSearchMode.value) return
  if (showLoading) loading.value = true
  try {
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

// 加载所有数据（无「出入库记录」权限时不拉入库流水/销售订单拼出库，避免越权与多余请求）
const loadData = async () => {
  loading.value = true
  try {
    const tasks = [
      dataStore.loadInventory(),
      dataStore.loadTransfers(),
      dataStore.loadProducts(),
      dataStore.loadWarehouses(),
      dataStore.loadCategories(),
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
    await loadStats()
    await fetchInventoryTable(false)
  } finally {
    loading.value = false
  }
}

const onInventoryQueryImageChange = (uploadFile) => {
  const raw = uploadFile?.raw
  if (!raw) return
  if (raw.size > MAX_IMAGE_UPLOAD_BYTES) {
    ElMessage.warning('查询图片大小不能超过 2MB')
    return
  }
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
  if (!queryImageDataUrl.value) {
    ElMessage.warning('请先上传图片')
    return
  }
  inventoryCurrentPage.value = 1
  await runImageSearch()
}

const runImageSearch = async () => {
  if (!queryImageDataUrl.value) return
  imageSearchLoading.value = true
  try {
    const body = {
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
      imageBase64: queryImageDataUrl.value,
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
  imageSearchRows.value = []
  imageSearchTotal.value = 0
  fetchInventoryTable()
}

const handleInventoryPageChange = () => {
  if (imageSearchMode.value) runImageSearch()
  else fetchInventoryTable()
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
const paginatedTransfers = computed(() => {
  const start = (transferCurrentPage.value - 1) * transferPageSize.value
  const end = start + transferPageSize.value
  return transfers.value.slice(start, end)
})

// 分页后的入库记录
const paginatedRecordsInbound = computed(() => {
  const start = (recordsInboundCurrentPage.value - 1) * recordsInboundPageSize.value
  const end = start + recordsInboundPageSize.value
  return inboundRecords.value.slice(start, end)
})

// 分页后的出库记录
const paginatedRecordsOutbound = computed(() => {
  const start = (recordsOutboundCurrentPage.value - 1) * recordsOutboundPageSize.value
  const end = start + recordsOutboundPageSize.value
  return outboundRecords.value.slice(start, end)
})

const transferForm = ref({ fromWarehouseId: null, toWarehouseId: null, productId: null, quantity: 1, remark: '' })
const warehouseForm = ref({ name: '', address: '', capacity: 50, managerName: '', remark: '' })
const warehouseRules = {
  name: [{ required: true, message: '请输入仓库名称', trigger: 'blur' }]
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
  quantity: [{ required: true, message: '请输入调拨数量', trigger: 'blur' }]
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
  remark: [{ required: true, message: '出库备注为必填项', trigger: 'blur' }]
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
  remark: [{ required: true, message: '出库备注为必填项', trigger: 'blur' }]
}

const getCategoryIcon = (cat) => ({ '手机': '📱', '电脑': '💻', '配件': '🎧', '手表': '⌚', '平板': '📱' }[cat] || '📦')

// 获取商品图片
const getProductImage = (productId) => {
  const product = products.value.find(p => p.id === productId)
  return firstProductImageUrl(product?.image)
}

// 动态获取仓库名称 - 从仓库列表中根据warehouseId查找
const getWarehouseName = (warehouseId) => {
  const warehouse = warehousesList.value.find(w => w.id === warehouseId)
  return warehouse?.name || ''
}

// 动态获取商品分类名称 - 根据productId从商品和分类列表中查找
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

// 调拨对话框选中商品的图片
const transferProductImage = computed(() => {
  if (!transferForm.value.productId) return null
  return getProductImage(transferForm.value.productId)
})

// 手动入库选中商品的图片
const manualInboundProductImage = computed(() => {
  if (!manualInboundForm.value.productId) return null
  return getProductImage(manualInboundForm.value.productId)
})

// 其他出库选中库存的商品图片
const otherOutboundProductImage = computed(() => {
  if (!otherOutboundForm.value.productId) return null
  return getProductImage(otherOutboundForm.value.productId)
})

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
  await loadData()
  if (row) {
    transferForm.value.productId = row.productId
    transferForm.value.fromWarehouseId = row.warehouseId
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
        await dataStore.loadTransfers()
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
            address: warehouseForm.value.address,
            capacity: warehouseForm.value.capacity,
            capacityUsed: warehouseForm.value.capacity,
            managerName: warehouseForm.value.managerName,
            remark: warehouseForm.value.remark
          })
          ElMessage.success('仓库信息已更新')
          warehouseDialogVisible.value = false
          // 刷新仓库列表
          await dataStore.loadWarehouses()
          // 更新当前仓库详情数据
          const updatedWarehouse = warehousesList.value.find(w => w.id === currentWarehouse.value.id)
          if (updatedWarehouse) {
            currentWarehouse.value = updatedWarehouse
          }
        } else {
          // 创建仓库
          await warehouseApi.create({
            name: warehouseForm.value.name,
            address: warehouseForm.value.address,
            capacity: warehouseForm.value.capacity,
            capacityUsed: warehouseForm.value.capacity,
            managerName: warehouseForm.value.managerName,
            remark: warehouseForm.value.remark
          })
          ElMessage.success('仓库添加成功')
          warehouseDialogVisible.value = false
          await dataStore.loadWarehouses()
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
  await dataStore.loadPurchaseOrders()
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
  await dataStore.loadSalesOrders()
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
          await Promise.all([dataStore.loadInventory(), fetchInventoryTable(false), dataStore.loadPurchaseOrders(), dataStore.loadInboundRecords(), loadStats()])
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
          await Promise.all([dataStore.loadInventory(), fetchInventoryTable(false), dataStore.loadInboundRecords(), loadStats()])
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
          await Promise.all([dataStore.loadInventory(), fetchInventoryTable(false), dataStore.loadSalesOrders(), dataStore.loadOutboundRecords(), loadStats()])
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
          await Promise.all([dataStore.loadInventory(), fetchInventoryTable(false), dataStore.loadOutboundRecords(), loadStats()])
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
  await dataStore.loadSuppliers()
  currentStock.value = row
  openPurchaseFromStock(row)
}

// 打开采购弹框 - 回填数据
const openPurchaseFromStock = (stock) => {
  if (!canShortcutPurchaseForStock(stock, products.value)) {
    ElMessage.warning('该商品已停售，无法创建采购单')
    return
  }
  const product = products.value.find(p => p.id === stock.productId)
  purchaseForm.value = {
    productId: stock.productId,
    productName: stock.productName || product?.name || '',
    sku: stock.sku || product?.code || '',
    currentStock: stock.stock || 0,
    safeStock: stock.safeStock || 10,
    supplierId: null,
    date: new Date(),
    quantity: Math.max(1, (stock.safeStock || 10) - (stock.stock || 0)), // 建议采购数量为安全库存减去当前库存
    unitPrice: product?.costPrice || stock.costPrice || 0,
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
        await loadStats()
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
    await Promise.all([dataStore.loadTransfers(), dataStore.loadInventory(), fetchInventoryTable(false)])
  } catch (error) {
    ElMessage.error(error.message || '确认失败')
  } finally {
    loading.value = false
  }
}

// 打开入库对话框
const openInboundDialog = async () => {
  try {
    await loadData()
  } catch (error) {
    console.error('加载数据失败:', error)
  }
  inboundForm.value = {
    purchaseOrderId: null,
    warehouseId: warehousesList.value[0]?.id || null,
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
          dataStore.loadPurchaseOrders(),
          dataStore.loadInboundRecords(),
          dataStore.loadInventory(),
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

const viewTransferDetail = (row) => {
  currentTransfer.value = row
  transferDetailVisible.value = true
}
const viewWarehouseDetail = (w) => {
  currentWarehouse.value = w
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
  warehouseForm.value = { name: '', address: '', capacity: 50, managerName: '', remark: '' }
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
  warehouseForm.value = { name: w.name, address: w.address, capacity: w.capacityUsed || 50, managerName: w.managerName || '', remark: w.remark || '' }
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
    await dataStore.loadWarehouses()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '删除失败')
    }
  }
}

// 打开手动出库对话框
const openManualOutboundDialog = () => {
  if (!canInventoryOutbound.value) {
    ElMessage.warning('无出库操作权限')
    return
  }
  manualOutboundType.value = 'sales'
  salesOutboundForm.value = {
    salesOrderId: null,
    warehouseId: warehousesList.value[0]?.id || null,
    quantity: 1,
    maxQuantity: 0,
    remark: ''
  }
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
  }
}

// 其他出库时选择库存记录自动设置相关信息
const onOtherOutboundInventoryChange = (inventoryId) => {
  const inv = inventoryData.value.find(i => i.id === inventoryId)
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
          await Promise.all([dataStore.loadInventory(), fetchInventoryTable(false), dataStore.loadSalesOrders(), dataStore.loadOutboundRecords(), loadStats()])
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
          await Promise.all([dataStore.loadInventory(), fetchInventoryTable(false), dataStore.loadOutboundRecords(), loadStats()])
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
const openManualInboundDialog = () => {
  if (!canInventoryInbound.value) {
    ElMessage.warning('无入库操作权限')
    return
  }
  manualInboundType.value = 'purchase'
  manualInboundForm.value = {
    productId: null,
    warehouseId: warehousesList.value[0]?.id || null,
    quantity: 1,
    remark: ''
  }
  purchaseInboundForm.value = {
    purchaseOrderId: null,
    warehouseId: warehousesList.value[0]?.id || null,
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
          await Promise.all([dataStore.loadInventory(), fetchInventoryTable(false), dataStore.loadInboundRecords(), loadStats()])
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
          await Promise.all([dataStore.loadInventory(), fetchInventoryTable(false), dataStore.loadPurchaseOrders(), dataStore.loadInboundRecords(), loadStats()])
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
    await dataStore.loadInventory()
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
    await dataStore.loadInventory()
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
    await dataStore.loadInventory()
    await fetchInventoryTable(false)
  } catch (error) {
    ElMessage.error(error.message || '更新失败')
  } finally {
    loading.value = false
  }
}

// 计算呆滞天数（优先用最后出库时间，无出库则用入库时间）
const getStagnantDays = (stock) => {
  const now = new Date()

  // 优先使用最后出库时间（多久没卖了）
  if (stock?.lastOutboundTime) {
    const lastOutbound = new Date(stock.lastOutboundTime)
    return Math.floor((now - lastOutbound) / (1000 * 60 * 60 * 24))
  }

  // 如果从未出库，则用入库时间（入库多久还没卖过）
  if (stock?.lastInboundTime) {
    const lastInbound = new Date(stock.lastInboundTime)
    return Math.floor((now - lastInbound) / (1000 * 60 * 60 * 24))
  }

  return 0
}

// 呆滞状态类型
const getStagnantStatusType = (stock) => {
  const stagnantDays = getStagnantDays(stock)
  const warningDays = stock?.stagnantDays || 90 // 默认90天
  if (stagnantDays >= warningDays) return 'danger'
  return 'success'
}

// 呆滞状态文本
const getStagnantStatusText = (stock) => {
  const stagnantDays = getStagnantDays(stock)
  const warningDays = stock?.stagnantDays || 90
  if (stagnantDays >= warningDays) return `呆滞${stagnantDays}天`
  return `正常`
}

// 初始化加载
onMounted(() => {
  loadData()
  // 处理路由参数
  if (route.query.tab) {
    activeTab.value = route.query.tab
  }
  if (route.query.subtab) {
    recordsSubTab.value = route.query.subtab
  }
  syncActiveTabToPermissions()
})

// 监听路由参数变化
watch(() => route.query, (query) => {
  if (query.tab) {
    activeTab.value = query.tab
  }
  if (query.subtab) {
    recordsSubTab.value = query.subtab
  }
  syncActiveTabToPermissions()
}, { immediate: true })

watch(
  [canInventoryStockTab, canInventoryRecordsTab, canInventoryTransferTab, canInventoryWarehouseTab],
  () => syncActiveTabToPermissions()
)
</script>

<style lang="scss" scoped>
.inventory-page {
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
  .sku { color: #E94560; font-family: monospace; }
  .order-no { color: #E94560; font-family: monospace; &.success { color: #67C23A; } &.warning { color: #E6A23C; } }
  .amount { font-weight: 600; color: #E94560; }
  .warehouse-name { font-weight: 600; color: #409EFF; }
  .low-stock { color: #E6A23C; font-weight: 600; }
  .critical-stock { color: #F56C6C; font-weight: 700; }
  .product-cell { display: flex; align-items: center; gap: 12px;
    .product-thumb { width: 40px; height: 40px; object-fit: cover; border-radius: 8px; }
    .product-icon { width: 40px; height: 40px; border-radius: 8px; background: #F5F7FA; display: flex; align-items: center; justify-content: center; font-size: 20px; }
    .product-info { h4 { font-size: 14px; font-weight: 600; color: #303133; } p { font-size: 12px; color: #909399; } }
  }
  .grid-empty {
    padding: 32px 0;
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
  .add-warehouse-btn { width: 100%; justify-content: center; margin-top: 20px; }
  .quantity-tip { font-size: 12px; color: #E6A23C; margin-top: 4px; }

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
}
</style>