<template>
  <div class="product-detail-page">
    <el-card>
      <template #header>
        <div class="card-header">
          <el-button @click="goBack">返回</el-button>
        </div>
      </template>
      <el-form ref="productFormRef" :model="productForm" :rules="productRules" label-width="120px" style="max-width: 640px">
        <el-form-item label="商品图片" prop="imageList">
          <div class="product-images-editor">
            <div v-for="(url, index) in productForm.imageList" :key="url + '-' + index" class="product-image-tile">
              <img :src="url" alt="" class="product-image" />
              <el-button type="danger" link size="small" class="remove-img-btn" @click="removeProductImage(index)">移除</el-button>
            </div>
            <el-upload
              v-if="productForm.imageList.length < 10"
              class="product-image-uploader"
              action="#"
              :auto-upload="false"
              :show-file-list="false"
              :disabled="imageUploading"
              :on-change="handleImageChange"
              accept="image/*"
            >
              <el-icon v-if="!imageUploading" class="product-image-uploader-icon"><Plus /></el-icon>
              <el-icon v-else class="product-image-uploader-icon is-loading"><Loading /></el-icon>
            </el-upload>
          </div>
          <div class="image-tip">最多 10 张，单张不超过 2MB；支持 JPG、PNG、GIF、WEBP；选择后自动上传，仅保存图片地址</div>
        </el-form-item>
        <el-form-item label="商品名称" prop="name">
          <el-input v-model="productForm.name" placeholder="输入商品名称" :maxlength="100" show-word-limit />
        </el-form-item>
        <el-row :gutter="20">
          <el-col :span="12"><el-form-item label="商品分类" prop="categoryName"><el-select v-model="productForm.categoryName" placeholder="请选择分类" style="width: 100%" filterable><el-option v-for="c in categoriesList" :key="c.id" :label="c.name" :value="c.name" :disabled="!isCategoryEnabled(c)" /></el-select></el-form-item></el-col>
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
import { useUserStore } from '@/stores/user'
import { categoryApi, productApi, CATEGORY_STATUS } from '@/api'
import { parseProductImageUrls, encodeProductImagesForApi } from '@/utils/productImages'
import { MAX_IMAGE_UPLOAD_BYTES } from '@/utils/uploadLimits'
import { isCategoryEnabled } from '@/utils/categoryStatus'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const productFormRef = ref()
const imageUploading = ref(false)
/** 列表接口带 status=1；编辑时若商品挂在已禁用分类上，再合并该条以便展示 */
const categoriesList = ref([])

const productId = computed(() => route.params.id)
const editMode = computed(() => !!productId.value)

const productForm = ref({
  name: '',
  categoryName: '',
  brand: '',
  spec: '',
  costPrice: 0,
  salePrice: 0,
  status: '在售',
  description: '',
  imageList: []
})
const productRules = {
  imageList: [
    {
      validator: (_, v, cb) => {
        if (!v?.length) cb(new Error('请至少上传 1 张商品图片'))
        else if (v.length > 10) cb(new Error('商品图片最多 10 张'))
        else cb()
      },
      trigger: 'change'
    }
  ],
  name: [
    { required: true, message: '请输入商品名称', trigger: 'blur' },
    { max: 100, message: '商品名称长度不能超过 100 个字符', trigger: 'blur' }
  ],
  categoryName: [{ required: true, message: '请选择商品分类', trigger: 'change' }],
  costPrice: [{ required: true, message: '请输入成本价', trigger: 'blur' }],
  salePrice: [{ required: true, message: '请输入销售价', trigger: 'blur' }]
}

const handleImageChange = async (file) => {
  const raw = file?.raw
  if (!raw) return
  if (raw.size > MAX_IMAGE_UPLOAD_BYTES) {
    ElMessage.warning('商品图片大小不能超过 2MB')
    return
  }
  if (productForm.value.imageList.length >= 10) {
    ElMessage.warning('商品图片最多 10 张')
    return
  }
  imageUploading.value = true
  try {
    const formData = new FormData()
    formData.append('file', raw)
    const data = await productApi.uploadImage(formData)
    const url = data?.url || ''
    if (url) {
      productForm.value.imageList = [...productForm.value.imageList, url]
    }
    productFormRef.value?.validateField('imageList')
  } catch (error) {
    ElMessage.error(error.message || '图片上传失败')
  } finally {
    imageUploading.value = false
  }
}

const removeProductImage = (index) => {
  productForm.value.imageList = productForm.value.imageList.filter((_, i) => i !== index)
  productFormRef.value?.validateField('imageList')
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
  const enabledRes = await categoryApi.list({ status: CATEGORY_STATUS.ENABLED })
  categoriesList.value = Array.isArray(enabledRes) ? [...enabledRes] : []
  if (editMode.value) {
    const id = Number(productId.value)
    if (!id) {
      ElMessage.warning('商品 ID 无效')
      router.replace('/products')
      return
    }
    try {
      const product = await productApi.get(id)
      productForm.value = {
        name: product.name,
        categoryName: product.categoryName || product.category,
        brand: product.brand,
        spec: product.spec,
        costPrice: product.costPrice,
        salePrice: product.salePrice,
        status: product.status || '在售',
        description: product.description || '',
        imageList: parseProductImageUrls(product.image),
        code: product.code
      }
      if (product.categoryId != null) {
        try {
          const catRow = await categoryApi.get(product.categoryId)
          if (catRow && !isCategoryEnabled(catRow) && !categoriesList.value.some((c) => c.id === catRow.id)) {
            categoriesList.value = [catRow, ...categoriesList.value]
          }
        } catch {
          /* 分类详情失败不影响主流程 */
        }
      }
    } catch (e) {
      ElMessage.error(e.message || '加载商品失败')
      router.replace('/products')
    }
  }
})

const goBack = () => router.back()
const submitProduct = async () => {
  if (!productFormRef.value) return
  await productFormRef.value.validate(async (valid) => {
    if (valid) {
      try {
        const category = (categoriesList.value || []).find(c => c.name === productForm.value.categoryName)
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
          image: encodeProductImagesForApi(productForm.value.imageList),
          description: productForm.value.description
        }

        if (editMode.value) {
          await productApi.update(Number(productId.value), productDTO)
          ElMessage.success('商品信息已更新')
        } else {
          await productApi.create(productDTO)
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
    justify-content: flex-end;
    align-items: center;
  }

  .product-images-editor {
    display: flex;
    flex-wrap: wrap;
    gap: 12px;
    align-items: flex-start;

    .product-image-tile {
      display: flex;
      flex-direction: column;
      align-items: center;
      gap: 4px;

      .product-image {
        width: 88px;
        height: 88px;
        object-fit: cover;
        border-radius: 8px;
        border: 1px solid #e4e7ed;
        display: block;
      }

      .remove-img-btn {
        padding: 0;
      }
    }
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
