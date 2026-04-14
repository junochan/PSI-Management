<template>
  <div class="product-view-page">
    <el-card>
      <template #header>
        <div class="card-header">
          <h3>商品详情</h3>
          <div class="header-actions">
            <el-button type="primary" @click="editProduct">编辑</el-button>
            <el-button @click="goBack">返回</el-button>
          </div>
        </div>
      </template>

      <!-- 商品图片展示 -->
      <div class="product-image-wrapper">
        <img v-if="product?.image" :src="product.image" class="product-image" />
        <div v-else class="product-image-placeholder">
          <span class="placeholder-icon">{{ getProductIcon(product?.categoryName || product?.category) }}</span>
        </div>
      </div>

      <!-- 基本信息 -->
      <el-descriptions :column="2" border>
        <el-descriptions-item label="商品编码"><span class="product-code">{{ product?.code }}</span></el-descriptions-item>
        <el-descriptions-item label="商品名称">{{ product?.name }}</el-descriptions-item>
        <el-descriptions-item label="品牌">{{ product?.brand || '-' }}</el-descriptions-item>
        <el-descriptions-item label="规格">{{ product?.spec || '-' }}</el-descriptions-item>
        <el-descriptions-item label="分类">{{ product?.categoryName || product?.category || '-' }}</el-descriptions-item>
        <el-descriptions-item label="成本价">¥{{ product?.costPrice }}</el-descriptions-item>
        <el-descriptions-item label="销售价"><span class="sale-price">¥{{ product?.salePrice }}</span></el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="product?.status === '在售' ? 'success' : 'info'">
            {{ product?.status }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="商品描述" :span="2">{{ product?.description || '-' }}</el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ formatTime(product?.createTime) }}</el-descriptions-item>
        <el-descriptions-item label="更新时间">{{ formatTime(product?.updateTime) }}</el-descriptions-item>
      </el-descriptions>
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useDataStore } from '@/stores/data'
import { formatTime } from '@/utils/time'

const router = useRouter()
const route = useRoute()
const dataStore = useDataStore()

const productId = computed(() => route.params.id)
const product = ref(null)

// 获取商品图标
const getProductIcon = (category) => {
  const iconMap = {
    '电子产品': '📱',
    '服装服饰': '👕',
    '食品饮料': '🍔',
    '家居用品': '🏠',
    '配件': '🎧',
    '手表': '⌚',
    '平板': '📱',
    '电脑': '💻'
  }
  return iconMap[category] || '📦'
}

// 加载商品数据
const loadProduct = async () => {
  await dataStore.loadProducts()
  const products = dataStore.products || []
  product.value = products.find(p => p.id === Number(productId.value))
}

// 编辑商品
const editProduct = () => {
  router.push(`/products/edit/${productId.value}`)
}

// 返回
const goBack = () => router.back()

onMounted(() => {
  loadProduct()
})
</script>

<style lang="scss" scoped>
.product-view-page {
  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    h3 {
      font-size: 18px;
      font-weight: 600;
      color: #303133;
    }
    .header-actions {
      display: flex;
      gap: 12px;
    }
  }

  .product-image-wrapper {
    text-align: center;
    margin-bottom: 24px;

    .product-image {
      width: 200px;
      height: 200px;
      object-fit: cover;
      border-radius: 12px;
      border: 1px solid #E4E7ED;
    }

    .product-image-placeholder {
      width: 200px;
      height: 200px;
      display: inline-flex;
      align-items: center;
      justify-content: center;
      background: #F5F7FA;
      border-radius: 12px;
      border: 1px solid #E4E7ED;

      .placeholder-icon {
        font-size: 80px;
      }
    }
  }

  .product-code {
    color: #606266;
    font-family: monospace;
  }

  .sale-price {
    font-weight: 600;
    color: #E94560;
  }
}
</style>