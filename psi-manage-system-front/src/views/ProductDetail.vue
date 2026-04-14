<template>
  <div class="product-detail-page">
    <el-card>
      <template #header>
        <div class="card-header">
          <h3>{{ editMode ? '编辑商品' : '添加商品' }}</h3>
          <el-button @click="goBack">返回</el-button>
        </div>
      </template>
      <el-form ref="productFormRef" :model="productForm" :rules="productRules" label-width="120px" style="max-width: 600px">
        <el-form-item label="商品图片" prop="image">
          <div class="image-upload-wrapper">
            <el-upload
              class="product-image-uploader"
              action="#"
              :auto-upload="false"
              :show-file-list="false"
              :on-change="handleImageChange"
              accept="image/*"
            >
              <img v-if="productForm.image" :src="productForm.image" class="product-image" alt="" />
              <el-icon v-else-if="!imageUploading" class="product-image-uploader-icon"><Plus /></el-icon>
              <el-icon v-else class="product-image-uploader-icon is-loading"><Loading /></el-icon>
            </el-upload>
            <div class="image-tip">支持 JPG、PNG、GIF、WEBP；选择后自动上传，仅保存图片地址</div>
          </div>
        </el-form-item>
        <el-form-item label="商品名称" prop="name"><el-input v-model="productForm.name" placeholder="输入商品名称" /></el-form-item>
        <el-row :gutter="20">
          <el-col :span="12"><el-form-item label="商品分类" prop="categoryName"><el-select v-model="productForm.categoryName" placeholder="请选择分类" style="width: 100%" filterable><el-option v-for="c in categoriesList" :key="c.id" :label="c.name" :value="c.name" /></el-select></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="商品编码"><el-input v-model="productForm.code" placeholder="系统自动生成" disabled /></el-form-item></el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12"><el-form-item label="品牌" prop="brand"><el-input v-model="productForm.brand" placeholder="输入品牌名称" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="规格" prop="spec"><el-input v-model="productForm.spec" placeholder="输入规格参数" /></el-form-item></el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12"><el-form-item label="成本价" prop="costPrice"><el-input-number v-model="productForm.costPrice" :min="0" :precision="2" style="width: 100%" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="销售价" prop="salePrice"><el-input-number v-model="productForm.salePrice" :min="0" :precision="2" style="width: 100%" /></el-form-item></el-col>
        </el-row>
        <el-form-item label="商品状态">
          <el-radio-group v-model="productForm.status"><el-radio value="在售">在售</el-radio><el-radio value="停售">停售</el-radio></el-radio-group>
        </el-form-item>
        <el-form-item label="商品描述"><el-input v-model="productForm.description" type="textarea" :rows="4" placeholder="输入商品描述" /></el-form-item>
        <el-form-item>
          <el-button type="primary" @click="submitProduct">{{ editMode ? '保存修改' : '确认添加' }}</el-button>
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
import { useDataStore } from '@/stores/data'
import { useUserStore } from '@/stores/user'
import { productApi } from '@/api'

const router = useRouter()
const route = useRoute()
const dataStore = useDataStore()
const userStore = useUserStore()
const productFormRef = ref()
const imageUploading = ref(false)

const productId = computed(() => route.params.id)
const editMode = computed(() => !!productId.value)
const categoriesList = computed(() => dataStore.categories || [])

const productForm = ref({ name: '', categoryName: '', brand: '', spec: '', costPrice: 0, salePrice: 0, status: '在售', description: '', image: '' })
const productRules = {
  image: [{ required: true, message: '请上传商品图片', trigger: 'change' }],
  name: [{ required: true, message: '请输入商品名称', trigger: 'blur' }],
  categoryName: [{ required: true, message: '请选择商品分类', trigger: 'change' }],
  costPrice: [{ required: true, message: '请输入成本价', trigger: 'blur' }],
  salePrice: [{ required: true, message: '请输入销售价', trigger: 'blur' }]
}

const handleImageChange = async (file) => {
  const raw = file?.raw
  if (!raw) return
  imageUploading.value = true
  try {
    const formData = new FormData()
    formData.append('file', raw)
    const data = await productApi.uploadImage(formData)
    productForm.value.image = data?.url || ''
    productFormRef.value?.validateField('image')
  } catch (error) {
    ElMessage.error(error.message || '图片上传失败')
  } finally {
    imageUploading.value = false
  }
}

onMounted(async () => {
  if (!editMode.value && !userStore.hasPermission('product:add')) {
    ElMessage.warning('无添加商品权限')
    router.replace('/products')
    return
  }
  if (editMode.value && !userStore.hasPermission('product:edit')) {
    ElMessage.warning('无编辑商品权限')
    router.replace(`/products/view/${productId.value}`)
    return
  }
  await dataStore.loadProducts()
  await dataStore.loadCategories()
  if (editMode.value) {
    const product = dataStore.products.find(p => p.id === Number(productId.value))
    if (product) {
      productForm.value = {
        name: product.name,
        categoryName: product.categoryName || product.category,
        brand: product.brand,
        spec: product.spec,
        costPrice: product.costPrice,
        salePrice: product.salePrice,
        status: product.status || '在售',
        description: product.description || '',
        image: product.image || '',
        code: product.code
      }
    }
  }
})

const goBack = () => router.back()
const submitProduct = async () => {
  if (!productFormRef.value) return
  await productFormRef.value.validate(async (valid) => {
    if (valid) {
      try {
        // 构造后端需要的DTO格式
        const category = categoriesList.value.find(c => c.name === productForm.value.categoryName)
        const productDTO = {
          code: productForm.value.code || `P${Date.now()}`,
          name: productForm.value.name,
          brand: productForm.value.brand,
          spec: productForm.value.spec,
          categoryId: category?.id || 1,
          categoryName: productForm.value.categoryName,
          costPrice: productForm.value.costPrice,
          salePrice: productForm.value.salePrice,
          status: productForm.value.status || '在售',
          image: productForm.value.image,
          description: productForm.value.description
        }

        if (editMode.value) {
          await dataStore.updateProduct(Number(productId.value), productDTO)
          ElMessage.success('商品信息已更新')
        } else {
          await dataStore.addProduct(productDTO)
          ElMessage.success('商品添加成功')
        }
        router.push('/products')
      } catch (error) {
        ElMessage.error(error.message || '操作失败')
      }
    }
  })
}
</script>

<style lang="scss" scoped>
.product-detail-page {
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

  .image-upload-wrapper {
    display: flex;
    flex-direction: column;
    align-items: flex-start;
    gap: 8px;
  }

  .product-image-uploader {
    .product-image {
      width: 120px;
      height: 120px;
      display: block;
      object-fit: cover;
      border-radius: 8px;
    }

    :deep(.el-upload) {
      border: 1px dashed #d9d9d9;
      border-radius: 8px;
      cursor: pointer;
      position: relative;
      overflow: hidden;
      width: 120px;
      height: 120px;
      display: flex;
      align-items: center;
      justify-content: center;
      transition: border-color 0.3s;

      &:hover {
        border-color: #E94560;
      }
    }

    .product-image-uploader-icon {
      font-size: 28px;
      color: #8c939d;
    }
  }

  .image-tip {
    font-size: 12px;
    color: #909399;
  }
}
</style>