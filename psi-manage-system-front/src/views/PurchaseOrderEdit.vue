<template>
  <div class="purchase-order-edit">
    <el-card>
      <template #header>
        <div class="card-header">
          <div class="header-actions">
            <el-button @click="goBack">返回</el-button>
          </div>
        </div>
      </template>
      <el-form ref="orderFormRef" :model="orderForm" :rules="orderRules" label-width="120px" style="max-width: 700px">
        <el-form-item label="采购单号">
          <el-input v-model="orderForm.orderNo" disabled />
        </el-form-item>
        <el-form-item label="供应商" prop="supplierId">
          <el-select v-model="orderForm.supplierId" style="width: 100%">
            <el-option v-for="s in suppliers" :key="s.id" :label="s.name" :value="s.id" />
          </el-select>
        </el-form-item>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="商品" prop="productId">
              <el-select v-model="orderForm.productId" style="width: 100%" filterable @change="onProductChange">
                <el-option v-for="p in products" :key="p.id" :label="p.name" :value="p.id" />
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
              <el-input-number v-model="orderForm.totalQuantity" :min="1" style="width: 100%" @change="updateQuantities" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="单价（元）" prop="unitPrice">
              <el-input-number v-model="orderForm.unitPrice" :min="0" :precision="2" style="width: 100%" @change="updateAmount" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="待入库">
              <el-input-number v-model="orderForm.pendingQuantity" :min="0" :max="orderForm.totalQuantity" style="width: 100%" disabled />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="已入库">
              <el-input-number v-model="orderForm.inboundQuantity" :min="0" :max="orderForm.totalQuantity" style="width: 100%" disabled />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="采购金额">
              <el-input :value="`¥${(orderForm.amount || 0).toLocaleString()}`" disabled />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="入库状态">
              <el-tag :type="getInboundStatusType(orderForm.inboundStatus)">{{ formatInboundStatus(orderForm.inboundStatus) }}</el-tag>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="付款状态">
          <el-select v-model="orderForm.payStatus" style="width: 100%">
            <el-option label="待付款" value="unpaid" />
            <el-option label="已付款" value="paid" />
            <el-option label="已退款" value="refunded" />
          </el-select>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="orderForm.remark" type="textarea" :rows="3" placeholder="输入备注信息" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="submitEdit">保存修改</el-button>
          <el-button @click="goBack">取消</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { productApi, purchaseApi, supplierApi } from '@/api'
import { firstProductImageUrl } from '@/utils/productImages'

const router = useRouter()
const route = useRoute()
const orderFormRef = ref()

const orderId = computed(() => route.params.id)
const order = ref(null)

const orderForm = ref({
  orderNo: '',
  supplierId: null,
  productId: null,
  productName: '',
  totalQuantity: 1,
  pendingQuantity: 1,
  inboundQuantity: 0,
  unitPrice: 0,
  amount: 0,
  inboundStatus: 'pending',
  payStatus: 'unpaid',
  remark: ''
})

const orderRules = {
  supplierId: [{ required: true, message: '请选择供应商', trigger: 'change' }],
  productId: [{ required: true, message: '请选择商品', trigger: 'change' }],
  totalQuantity: [{ required: true, message: '请输入采购总量', trigger: 'blur' }],
  unitPrice: [{ required: true, message: '请输入单价', trigger: 'blur' }]
}

const suppliers = ref([])
const products = ref([])

// 选中商品的图片
const selectedProductImage = computed(() => {
  if (!orderForm.value.productId) return null
  const product = (products.value || []).find(p => p.id === orderForm.value.productId)
  return firstProductImageUrl(product?.image)
})

// 状态格式化函数 - 英文转中文
const formatInboundStatus = (status) => {
  const statusMap = { 'pending': '待入库', 'partial': '部分入库', 'completed': '已完成', 'cancelled': '已取消', '待入库': '待入库', '部分入库': '部分入库', '已完成': '已完成', '已取消': '已取消' }
  return statusMap[status] || status
}
const formatPayStatus = (status) => {
  const statusMap = { 'unpaid': '待付款', 'paid': '已付款', 'refunded': '已退款', '待付款': '待付款', '已付款': '已付款', '已退款': '已退款' }
  return statusMap[status] || status
}

const getInboundStatusType = (status) => ({ 'completed': 'success', 'partial': 'warning', 'pending': 'info', 'cancelled': 'danger', '已完成': 'success', '部分入库': 'warning', '待入库': 'info', '已取消': 'danger' }[status] || 'info')

// 商品选择变化时自动填充成本价
const onProductChange = (productId) => {
  const product = (products.value || []).find(p => p.id === productId)
  if (product) {
    orderForm.value.unitPrice = product.costPrice || 0
    orderForm.value.productName = product.name
    updateAmount()
  }
}

// 更新金额
const updateAmount = () => {
  orderForm.value.amount = (orderForm.value.totalQuantity || 0) * (orderForm.value.unitPrice || 0)
}

// 更新数量关系：待入库 + 已入库 = 采购总量
const updateQuantities = () => {
  const inboundQty = orderForm.value.inboundQuantity || 0
  orderForm.value.pendingQuantity = Math.max(0, orderForm.value.totalQuantity - inboundQty)
  updateAmount()
}

onMounted(async () => {
  const id = Number(orderId.value)
  if (!id) {
    ElMessage.warning('采购单 ID 无效')
    router.replace('/purchase')
    return
  }
  const [orderRes, supplierRes, productRes] = await Promise.all([
    purchaseApi.get(id),
    supplierApi.list({ page: 1, size: 200 }),
    productApi.list({ page: 1, size: 500 })
  ])
  order.value = orderRes || null
  suppliers.value = supplierRes?.list || []
  products.value = productRes?.list || []
  if (order.value) {
    orderForm.value = {
      orderNo: order.value.orderNo,
      supplierId: order.value.supplierId,
      productId: order.value.productId,
      productName: order.value.productName || order.value.product,
      totalQuantity: order.value.totalQuantity || order.value.quantity,
      pendingQuantity: order.value.pendingQuantity ?? ((order.value.totalQuantity || order.value.quantity || 0) - (order.value.inboundQuantity || 0)),
      inboundQuantity: order.value.inboundQuantity || 0,
      unitPrice: order.value.unitPrice || order.value.price || 0,
      amount: order.value.amount || (order.value.totalQuantity || order.value.quantity || 0) * (order.value.unitPrice || order.value.price || 0),
      inboundStatus: order.value.inboundStatus || 'pending',
      payStatus: order.value.payStatus || 'unpaid',
      remark: order.value.remark || ''
    }
  }
})

const goBack = () => router.back()

const submitEdit = async () => {
  if (!orderFormRef.value) return
  await orderFormRef.value.validate(async (valid) => {
    if (valid) {
      try {
        await purchaseApi.update(Number(orderId.value), {
          supplierId: orderForm.value.supplierId,
          productId: orderForm.value.productId,
          quantity: orderForm.value.totalQuantity,
          unitPrice: orderForm.value.unitPrice,
          remark: orderForm.value.remark
        })
        ElMessage.success('采购单已更新')
        router.push('/purchase')
      } catch (error) {
        ElMessage.error(error.message || '更新失败')
      }
    }
  })
}
</script>

<style lang="scss" scoped>
.purchase-order-edit {
  .card-header {
    display: flex;
    justify-content: flex-end;
    align-items: center;
    .header-actions { display: flex; gap: 12px; }
  }

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
</style>