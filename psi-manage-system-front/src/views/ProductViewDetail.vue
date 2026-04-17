<template>
  <div class="product-view-page">
    <el-card>
      <template #header>
        <div class="card-header">
          <div class="header-actions">
            <el-button v-if="canProductEdit" type="primary" @click="editProduct">编辑</el-button>
            <el-button @click="goBack">返回</el-button>
          </div>
        </div>
      </template>

      <!-- 商品图片展示 -->
      <div class="product-image-wrapper">
        <el-carousel v-if="productImages.length" height="220px" indicator-position="outside" :autoplay="false">
          <el-carousel-item v-for="(url, i) in productImages" :key="i">
            <div class="carousel-img-wrap">
              <img :src="url" class="product-image" alt="" />
            </div>
          </el-carousel-item>
        </el-carousel>
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
        <el-descriptions-item label="成本价">¥{{ formatAmountDisplay(product?.costPrice ?? 0) }}</el-descriptions-item>
        <el-descriptions-item label="销售价"><span class="sale-price">¥{{ formatAmountDisplay(product?.salePrice ?? 0) }}</span></el-descriptions-item>
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
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { productApi } from '@/api'
import { formatTime } from '@/utils/time'
import { formatAmountDisplay } from '@/utils/moneyFormat'
import { parseProductImageUrls } from '@/utils/productImages'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const canProductEdit = computed(() => userStore.hasPermission('product:edit'))

const productId = computed(() => route.params.id)
const product = ref(null)

const productImages = computed(() => parseProductImageUrls(product.value?.image))

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

// 加载商品数据（单笔详情接口）
const loadProduct = async () => {
  const id = Number(productId.value)
  if (!id) {
    ElMessage.warning('商品 ID 无效')
    router.replace('/products')
    return
  }
  try {
    product.value = await productApi.get(id)
  } catch (e) {
    ElMessage.error(e.message || '加载商品失败')
    router.replace('/products')
  }
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
    justify-content: flex-end;
    align-items: center;
    .header-actions {
      display: flex;
      gap: 12px;
    }
  }

  .product-image-wrapper {
    text-align: center;
    margin-bottom: 24px;
    max-width: 360px;
    margin-left: auto;
    margin-right: auto;

    .carousel-img-wrap {
      display: flex;
      align-items: center;
      justify-content: center;
      height: 220px;
    }

    .product-image {
      max-width: 100%;
      max-height: 220px;
      width: auto;
      height: auto;
      object-fit: contain;
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
  }

  .sale-price {
    font-weight: 600;
    color: #E94560;
  }
}
</style>