<template>
  <div class="products-page">
    <!-- 商品列表 -->
    <el-card class="table-card">
      <template #header>
        <div class="card-header">
          <h3>商品列表</h3>
          <div class="header-actions">
            <el-select v-model="filterCategory" placeholder="按分类筛选" clearable filterable style="width: 140px">
              <el-option v-for="c in categoriesList" :key="c.id" :label="c.name" :value="c.name" />
            </el-select>
            <el-select v-model="filterStatus" placeholder="按状态筛选" clearable filterable style="width: 120px">
              <el-option label="在售" value="在售" />
              <el-option label="停售" value="停售" />
            </el-select>
            <el-input
              v-model="searchKeyword"
              placeholder="搜索商品名称、编码..."
              prefix-icon="Search"
              clearable
              style="width: 200px"
            />
            <el-upload
              v-if="canProductRead"
              accept="image/*"
              :auto-upload="false"
              :show-file-list="false"
              @change="onProductQueryImageChange"
            >
              <el-button size="small">+上传图片</el-button>
            </el-upload>
            <img v-if="queryImageDataUrl" :src="queryImageDataUrl" class="image-query-thumb" alt="" />
            <el-input-number
              v-if="canProductRead"
              v-model="imageSimilarityThreshold"
              :min="0.2"
              :max="0.99"
              :step="0.05"
              :precision="2"
              size="small"
              style="width: 108px"
            />
            <el-button v-if="canProductRead" type="primary" size="small" :loading="imageSearchLoading" @click="submitImageSearch">以图搜图</el-button>
            <el-button v-if="imageSearchMode" type="info" size="small" link @click="exitImageSearch">退出图搜</el-button>
            <el-button v-if="canProductAdd" type="success" @click="handleImport"><el-icon><Upload /></el-icon>批量导入</el-button>
            <el-button v-if="canProductRead" type="info" @click="handleExport"><el-icon><Download /></el-icon>批量导出</el-button>
            <el-button v-if="canProductAdd" type="primary" @click="openAddDialog">
              <el-icon><Plus /></el-icon>
              添加商品
            </el-button>
          </div>
        </div>
      </template>

      <el-table
        class="product-list-table"
        :data="paginatedProducts"
        empty-text="暂无数据"
        style="width: 100%"
        table-layout="fixed"
        :max-height="520"
      >
        <el-table-column label="图片" width="76" align="center" fixed="left">
          <template #default="{ row }">
            <div class="product-image-cell">
              <img v-if="row.image" :src="row.image" alt="商品图片" class="product-thumb-list" />
              <span v-else class="product-icon-placeholder">{{ getProductIcon(row.categoryName || row.category) }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="编码" min-width="112" show-overflow-tooltip>
          <template #default="{ row }">
            <span class="product-code">{{ row.code }}</span>
          </template>
        </el-table-column>
        <el-table-column label="商品名称" min-width="200" show-overflow-tooltip>
          <template #default="{ row }">
            <span class="product-name-cell">{{ row.name }}</span>
          </template>
        </el-table-column>
        <el-table-column label="品牌" min-width="96" show-overflow-tooltip>
          <template #default="{ row }">{{ row.brand || '-' }}</template>
        </el-table-column>
        <el-table-column label="规格" min-width="120" show-overflow-tooltip>
          <template #default="{ row }">{{ row.spec || '-' }}</template>
        </el-table-column>
        <el-table-column label="分类" min-width="108" show-overflow-tooltip>
          <template #default="{ row }">{{ row.categoryName || row.category || '-' }}</template>
        </el-table-column>
        <el-table-column label="成本价" width="90" align="right" class-name="col-numeric">
          <template #default="{ row }">
            ¥{{ row.costPrice }}
          </template>
        </el-table-column>
        <el-table-column label="销售价" width="90" align="right" class-name="col-numeric">
          <template #default="{ row }">
            <span class="sale-price">¥{{ row.salePrice }}</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="getProductStatusType(row.status)" effect="light" size="small">{{ formatProductStatus(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="178" fixed="right" align="center" class-name="col-actions">
          <template #default="{ row }">
            <div class="product-row-actions">
              <el-button type="primary" link size="small" @click="viewProduct(row)">详情</el-button>
              <el-button v-if="canProductEdit" type="primary" link size="small" @click="editProduct(row)">编辑</el-button>
              <el-button v-if="canProductDelete" type="danger" link size="small" @click="deleteProductConfirm(row)">删除</el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :total="productListTotalDisplay"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          @current-change="handleProductPageChange"
          @size-change="handleProductSizeChange"
        />
      </div>
    </el-card>

    <!-- 添加/编辑商品对话框 -->
    <el-dialog
      v-model="productDialogVisible"
      :title="editMode ? '编辑商品' : '添加商品'"
      width="600px"
      destroy-on-close
    >
      <el-form ref="productFormRef" :model="productForm" :rules="productRules" label-width="100px">
        <el-form-item label="商品图片" prop="image">
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
          <div class="image-tip">支持 JPG、PNG、GIF、WEBP，建议 200×200；选择后自动上传至服务器</div>
        </el-form-item>
        <el-form-item label="商品名称" prop="name">
          <el-input v-model="productForm.name" placeholder="输入商品名称" />
        </el-form-item>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="商品分类" prop="categoryName">
              <el-select v-model="productForm.categoryName" placeholder="请选择分类" style="width: 100%" filterable>
                <el-option v-for="c in categoriesList" :key="c.id" :label="c.name" :value="c.name" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="商品编码">
              <el-input v-model="productForm.code" placeholder="系统自动生成" disabled />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="品牌" prop="brand">
              <el-input v-model="productForm.brand" placeholder="输入品牌名称" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="规格" prop="spec">
              <el-input v-model="productForm.spec" placeholder="输入规格参数" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="成本价" prop="costPrice">
              <el-input-number v-model="productForm.costPrice" :min="0" :precision="2" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="销售价" prop="salePrice">
              <el-input-number v-model="productForm.salePrice" :min="0" :precision="2" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
                <el-form-item label="商品描述">
          <el-input v-model="productForm.description" type="textarea" :rows="3" placeholder="输入商品描述" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="productDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitProduct">
          {{ editMode ? '保存修改' : '确认添加' }}
        </el-button>
      </template>
    </el-dialog>

    <!-- 商品详情对话框 -->
    <el-dialog v-model="productDetailVisible" title="商品详情" width="500px">
      <div class="product-detail-image-wrapper">
        <img v-if="currentProduct?.image" :src="currentProduct.image" alt="商品图片" class="product-detail-thumb" />
        <span v-else class="product-detail-placeholder">{{ getProductIcon(currentProduct?.categoryName || currentProduct?.category) }}</span>
      </div>
      <el-descriptions :column="2" border>
        <el-descriptions-item label="商品编码">{{ currentProduct?.code }}</el-descriptions-item>
        <el-descriptions-item label="商品名称">{{ currentProduct?.name }}</el-descriptions-item>
        <el-descriptions-item label="品牌">{{ currentProduct?.brand }}</el-descriptions-item>
        <el-descriptions-item label="规格">{{ currentProduct?.spec }}</el-descriptions-item>
        <el-descriptions-item label="分类">{{ currentProduct?.categoryName || currentProduct?.category }}</el-descriptions-item>
        <el-descriptions-item label="成本价">¥{{ currentProduct?.costPrice }}</el-descriptions-item>
        <el-descriptions-item label="销售价">¥{{ currentProduct?.salePrice }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="getProductStatusType(currentProduct?.status)">{{ formatProductStatus(currentProduct?.status) }}</el-tag>
        </el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <el-button @click="productDetailVisible = false">关闭</el-button>
        <el-button v-if="canProductEdit" type="primary" @click="router.push(`/products/edit/${currentProduct?.id}`); productDetailVisible = false">编辑</el-button>
      </template>
    </el-dialog>

    <!-- 批量导入对话框 -->
    <el-dialog v-model="importDialogVisible" title="批量导入商品" width="500px">
      <el-upload
        drag
        action="#"
        :auto-upload="false"
        :on-change="handleFileChange"
        accept=".xlsx,.xls,.csv"
      >
        <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
        <div class="el-upload__text">
          将文件拖到此处，或<em>点击上传</em>
        </div>
        <template #tip>
          <div class="el-upload__tip">
            只能上传 xlsx/xls/csv 文件，且文件大小不超过 10MB
          </div>
        </template>
      </el-upload>
      <template #footer>
        <el-button @click="importDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="executeImport">开始导入</el-button>
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
import { productApi } from '@/api'
import * as XLSX from 'xlsx'

const router = useRouter()
const dataStore = useDataStore()
const userStore = useUserStore()

/** 仅商品查看或更高操作权限（用于列表、导出、以图搜图等） */
const canProductRead = computed(() =>
  userStore.hasPermission('products') ||
  userStore.hasPermission('product:view') ||
  userStore.hasPermission('product:add') ||
  userStore.hasPermission('product:edit') ||
  userStore.hasPermission('product:delete')
)
const canProductAdd = computed(() => userStore.hasPermission('product:add'))
const canProductEdit = computed(() => userStore.hasPermission('product:edit'))
const canProductDelete = computed(() => userStore.hasPermission('product:delete'))

const searchKeyword = ref('')
const filterCategory = ref(null)
const filterStatus = ref(null)
const queryImageDataUrl = ref('')
const imageSimilarityThreshold = ref(0.7)
const imageSearchMode = ref(false)
const imageSearchLoading = ref(false)
const imageSearchRows = ref([])
const imageSearchTotal = ref(0)
const currentPage = ref(1)
const pageSize = ref(10)
const productDialogVisible = ref(false)
const productDetailVisible = ref(false)
const importDialogVisible = ref(false)
const editMode = ref(false)
const productFormRef = ref()
const currentProduct = ref(null)
const uploadFile = ref(null)
const loading = ref(false)
const imageUploading = ref(false)

const categoriesList = computed(() => dataStore.categories || [])

/** 服务端分页：当前页数据 */
const productTableRows = ref([])
const productListTotal = ref(0)

const fetchProducts = async () => {
  if (imageSearchMode.value) return
  loading.value = true
  try {
    const res = await productApi.list({
      page: currentPage.value,
      size: pageSize.value,
      keyword: searchKeyword.value || undefined,
      categoryName: filterCategory.value || undefined,
      productStatus: filterStatus.value || undefined
    })
    productTableRows.value = res.list || []
    productListTotal.value = Number(res.total) || 0
  } catch (error) {
    console.error(error)
    ElMessage.error(error.message || '加载商品失败')
    productTableRows.value = []
    productListTotal.value = 0
  } finally {
    loading.value = false
  }
}

// 加载分类 + 同步 store 商品（供其他模块）+ 当前列表
const loadProducts = async () => {
  await dataStore.loadCategories()
  await dataStore.loadProducts()
  await fetchProducts()
}

const productListTotalDisplay = computed(() =>
  imageSearchMode.value ? imageSearchTotal.value : productListTotal.value
)

// 表格数据：普通模式走服务端分页；以图搜图走向量结果
const paginatedProducts = computed(() => {
  if (imageSearchMode.value) {
    return imageSearchRows.value
  }
  return productTableRows.value
})

watch([filterCategory, filterStatus, searchKeyword], () => {
  if (imageSearchMode.value) return
  currentPage.value = 1
  fetchProducts()
})

watch([currentPage, pageSize], () => {
  if (!imageSearchMode.value) fetchProducts()
})

const onProductQueryImageChange = (uploadFile) => {
  const raw = uploadFile?.raw
  if (!raw) return
  const reader = new FileReader()
  reader.onload = (e) => {
    queryImageDataUrl.value = e.target.result
  }
  reader.readAsDataURL(raw)
}

const submitImageSearch = async () => {
  if (!canProductRead.value) {
    ElMessage.warning('无商品数据查看权限')
    return
  }
  if (!queryImageDataUrl.value) {
    ElMessage.warning('请先上传图片')
    return
  }
  currentPage.value = 1
  await runImageSearch()
}

const runImageSearch = async () => {
  if (!queryImageDataUrl.value) return
  imageSearchLoading.value = true
  try {
    const body = {
      page: currentPage.value,
      size: pageSize.value,
      keyword: searchKeyword.value || undefined,
      categoryName: filterCategory.value || undefined,
      status: filterStatus.value || undefined,
      imageBase64: queryImageDataUrl.value,
      similarityThreshold: imageSimilarityThreshold.value
    }
    const res = await productApi.searchByImage(body)
    imageSearchRows.value = res.list || []
    imageSearchTotal.value = Number(res.total) || 0
    imageSearchMode.value = true
    if (imageSearchTotal.value === 0) {
      ElMessage.info('没有相似度达标的商品，可调低阈值或配置 DASHSCOPE_API_KEY')
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
  fetchProducts()
}

const handleProductPageChange = () => {
  if (imageSearchMode.value) runImageSearch()
  else fetchProducts()
}

const handleProductSizeChange = () => {
  if (imageSearchMode.value) {
    currentPage.value = 1
    runImageSearch()
  } else {
    currentPage.value = 1
    fetchProducts()
  }
}

// 商品表单
const productForm = ref({
  name: '',
  categoryName: '',
  brand: '',
  spec: '',
  costPrice: 0,
  salePrice: 0,
  description: '',
  status: '在售',
  image: ''
})

const productRules = {
  image: [{ required: true, message: '请上传商品图片', trigger: 'change' }],
  name: [{ required: true, message: '请输入商品名称', trigger: 'blur' }],
  categoryName: [{ required: true, message: '请选择商品分类', trigger: 'change' }],
  costPrice: [{ required: true, message: '请输入成本价', trigger: 'blur' }],
  salePrice: [{ required: true, message: '请输入销售价', trigger: 'blur' }]
}

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

// 与采购管理「入库状态」列一致：格式化 + el-tag 配色（并去掉末尾多余标点）
const formatProductStatus = (status) => {
  if (status == null || status === '') return '-'
  let s = String(status).trim()
  s = s.replace(/[.。··•・]+$/u, '').trim()
  const statusMap = {
    在售: '在售',
    停售: '停售'
  }
  return statusMap[s] || s
}

const getProductStatusType = (status) => {
  const label = formatProductStatus(status)
  if (label === '在售') return 'success'
  return 'info'
}

// 打开添加对话框
const openAddDialog = () => {
  if (!canProductAdd.value) {
    ElMessage.warning('无添加商品权限')
    return
  }
  editMode.value = false
  productForm.value = {
    name: '',
    categoryName: '',
    brand: '',
    spec: '',
    costPrice: 0,
    salePrice: 0,
    description: '',
    status: '在售',
    code: '',
    image: ''
  }
  productDialogVisible.value = true
}

// 选择图片后上传至服务端，数据库仅存 URL
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

// 编辑商品
const editProduct = (row) => {
  if (!canProductEdit.value) {
    ElMessage.warning('无编辑商品权限')
    return
  }
  router.push(`/products/edit/${row.id}`)
}

// 查看商品详情
const viewProduct = (row) => {
  router.push(`/products/view/${row.id}`)
}

// 删除商品确认 - 调用后端API
const deleteProductConfirm = async (row) => {
  if (!canProductDelete.value) {
    ElMessage.warning('无删除商品权限')
    return
  }
  try {
    await ElMessageBox.confirm(`确定要删除商品 "${row.name}" 吗？`, '删除确认', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await productApi.delete(row.id)
    ElMessage.success('商品已删除')
    await loadProducts()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '删除失败')
    }
  }
}

// 提交商品 - 调用后端API
const submitProduct = async () => {
  if (!productFormRef.value) return

  await productFormRef.value.validate(async (valid) => {
    if (valid) {
      if (editMode.value && !canProductEdit.value) {
        ElMessage.warning('无编辑商品权限')
        return
      }
      if (!editMode.value && !canProductAdd.value) {
        ElMessage.warning('无添加商品权限')
        return
      }
      loading.value = true
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
          await productApi.update(currentProduct.value.id, productDTO)
          ElMessage.success('商品信息已更新')
        } else {
          await productApi.create(productDTO)
          ElMessage.success('商品添加成功')
        }
        productDialogVisible.value = false
        await loadProducts()
      } catch (error) {
        ElMessage.error(error.message || '操作失败')
      } finally {
        loading.value = false
      }
    }
  })
}

// 批量导入
const handleImport = () => {
  if (!canProductAdd.value) {
    ElMessage.warning('无添加商品权限')
    return
  }
  importDialogVisible.value = true
}

const handleFileChange = (file) => {
  uploadFile.value = file.raw
}

const executeImport = () => {
  if (!canProductAdd.value) {
    ElMessage.warning('无添加商品权限')
    return
  }
  if (!uploadFile.value) {
    ElMessage.warning('请先选择要导入的文件')
    return
  }
  ElMessage.success('批量导入功能演示 - 文件已接收')
  importDialogVisible.value = false
}

// 批量导出 - 按当前筛选条件拉取后导出
const handleExport = async () => {
  if (!canProductRead.value) {
    ElMessage.warning('无商品数据查看权限')
    return
  }
  if (imageSearchMode.value) {
    ElMessage.warning('请先退出以图搜图后再导出')
    return
  }
  loading.value = true
  let rows = []
  try {
    const res = await productApi.list({
      page: 1,
      size: 5000,
      keyword: searchKeyword.value || undefined,
      categoryName: filterCategory.value || undefined,
      productStatus: filterStatus.value || undefined
    })
    rows = res.list || []
  } catch (e) {
    ElMessage.error(e.message || '获取导出数据失败')
    return
  } finally {
    loading.value = false
  }
  if (rows.length === 0) {
    ElMessage.warning('没有可导出的商品数据')
    return
  }

  // 准备导出数据 - 每个字段对应Excel的一列
  const exportData = rows.map(p => ({
    '编码': p.code || '',
    '商品信息': p.name || '',
    '分类': p.categoryName || p.category || '',
    '成本': p.costPrice || 0,
    '售价': p.salePrice || 0
  }))

  // 创建工作表
  const worksheet = XLSX.utils.json_to_sheet(exportData)

  // 设置列宽
  worksheet['!cols'] = [
    { wch: 12 },  // 编码
    { wch: 30 },  // 商品信息
    { wch: 15 },  // 分类
    { wch: 12 },  // 成本
    { wch: 12 }   // 售价
  ]

  // 创建工作簿
  const workbook = XLSX.utils.book_new()
  XLSX.utils.book_append_sheet(workbook, worksheet, '商品列表')

  // 导出Excel文件
  XLSX.writeFile(workbook, `商品列表_${new Date().toISOString().slice(0,10)}.xlsx`)

  ElMessage.success(`已导出 ${exportData.length} 条商品数据`)
}

// 初始化加载数据
onMounted(() => {
  loadProducts()
})
</script>

<style lang="scss" scoped>
.products-page {
  .quick-actions {
    display: grid;
    grid-template-columns: repeat(3, 1fr);
    gap: 16px;
    margin-bottom: 24px;

    .action-card {
      cursor: pointer;
      transition: all 0.3s ease;
      text-align: center;
      padding: 24px;

      &:hover {
        background: rgba(233, 69, 96, 0.05);
        border-color: #E94560;
      }

      .el-icon {
        font-size: 32px;
        color: #E94560;
        margin-bottom: 12px;
      }

      p {
        font-size: 14px;
        color: #606266;
      }
    }
  }

  .table-card {
    :deep(.el-card__header) {
      padding: 16px 20px;
      border-bottom: 1px solid #E4E7ED;
    }

    .card-header {
      display: flex;
      justify-content: space-between;
      align-items: center;

      h3 {
        font-size: 16px;
        font-weight: 600;
        color: #303133;
      }

      .header-actions {
        display: flex;
        align-items: center;
        gap: 16px;
      }
    }

    .product-list-table {
      :deep(.el-table__cell) {
        padding: 8px 10px;
      }
      :deep(.el-table__header .cell) {
        font-weight: 600;
        white-space: nowrap;
      }
      :deep(.col-numeric .cell) {
        font-variant-numeric: tabular-nums;
        white-space: nowrap;
      }
      :deep(.col-actions .cell) {
        white-space: nowrap;
      }
    }

    .product-row-actions {
      display: inline-flex;
      flex-wrap: nowrap;
      align-items: center;
      justify-content: center;
      gap: 2px;
    }

    .product-name-cell {
      display: inline-block;
      max-width: 100%;
      line-height: 1.45;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
      vertical-align: middle;
    }

    .product-code {
      color: #606266;
      font-family: monospace;
      font-size: 13px;
    }

    .product-image-cell {
      .product-thumb-list {
        width: 50px;
        height: 50px;
        object-fit: cover;
        border-radius: 6px;
        border: 1px solid #E4E7ED;
      }
      .product-icon-placeholder {
        width: 50px;
        height: 50px;
        display: flex;
        align-items: center;
        justify-content: center;
        background: #F5F7FA;
        border-radius: 6px;
        font-size: 24px;
      }
    }

    .product-cell {
      display: flex;
      align-items: center;
      gap: 12px;

      .product-info {
        h4 {
          font-size: 14px;
          font-weight: 600;
          color: #303133;
        }

        p {
          font-size: 12px;
          color: #909399;
        }
      }
    }

    .sale-price {
      font-weight: 600;
      color: #E94560;
    }

    .pagination-wrapper {
      display: flex;
      justify-content: flex-end;
      padding-top: 16px;
    }

    .image-query-thumb {
      width: 32px;
      height: 32px;
      object-fit: cover;
      border-radius: 6px;
      vertical-align: middle;
      border: 1px solid #e4e7ed;
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

    .el-upload {
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
      width: 120px;
      height: 120px;
      text-align: center;
    }
  }

  .image-tip {
    font-size: 12px;
    color: #909399;
    margin-top: 8px;
  }

  .product-detail-image-wrapper {
    text-align: center;
    margin-bottom: 20px;

    .product-detail-thumb {
      width: 100px;
      height: 100px;
      object-fit: cover;
      border-radius: 8px;
      border: 1px solid #E4E7ED;
    }

    .product-detail-placeholder {
      width: 100px;
      height: 100px;
      display: inline-flex;
      align-items: center;
      justify-content: center;
      background: #F5F7FA;
      border-radius: 8px;
      border: 1px solid #E4E7ED;
      font-size: 40px;
    }
  }
}
</style>