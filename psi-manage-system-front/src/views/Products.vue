<template>
  <div class="products-page">
    <!-- 商品列表 -->
    <el-card class="table-card" v-loading="productsCardLoading" element-loading-text="加载中...">
      <template #header>
        <div class="card-header">
          <div class="header-actions">
            <el-select
              v-model="filterCategory"
              v-load-more="{ popperClass: 'products-filter-category-dropdown', onLoadMore: loadMoreCategoryOptions, disabled: categoryOptionsLoading || !categoryOptionsHasMore }"
              popper-class="products-filter-category-dropdown"
              class="filter-control filter-category"
              placeholder="按分类筛选"
              clearable
              filterable
              @visible-change="onCategoryFilterVisibleChange"
              @filter-method="onCategoryFilter"
            >
              <el-option v-for="c in categoryOptions" :key="c.id" :label="c.name" :value="c.name" />
            </el-select>
            <el-select v-model="filterStatus" class="filter-control filter-status" placeholder="按状态筛选" clearable filterable>
              <el-option label="在售" value="在售" />
              <el-option label="停售" value="停售" />
            </el-select>
            <el-input
              v-model="searchKeyword"
              class="filter-control filter-search"
              placeholder="搜索商品名称、编码..."
              prefix-icon="Search"
              clearable
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
              class="filter-control filter-threshold"
              :min="0.2"
              :max="0.99"
              :step="0.05"
              :precision="2"
              size="small"
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
        :max-height="tableMaxHeight"
      >
        <el-table-column label="图片" width="76" align="center" fixed="left">
          <template #default="{ row }">
            <div class="product-image-cell">
              <ProductImageThumb
                v-if="firstProductImageUrl(row.image)"
                :src="firstProductImageUrl(row.image)"
                :preview-src-list="parseProductImageUrls(row.image)"
                class="product-thumb-list"
                :width="50"
                :height="50"
                :radius="6"
              />
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
        <el-table-column label="分类" min-width="86" show-overflow-tooltip>
          <template #default="{ row }">{{ row.categoryName || row.category || '-' }}</template>
        </el-table-column>
        <el-table-column label="成本价" min-width="116" align="right" class-name="col-numeric">
          <template #default="{ row }">
            ¥{{ formatAmountDisplay(row.costPrice ?? 0) }}
          </template>
        </el-table-column>
        <el-table-column label="销售价" min-width="116" align="right" class-name="col-numeric">
          <template #default="{ row }">
            <span class="sale-price">¥{{ formatAmountDisplay(row.salePrice ?? 0) }}</span>
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
      :width="productDialogWidth"
      destroy-on-close
    >
      <el-form ref="productFormRef" :model="productForm" :rules="productRules" label-width="100px">
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
          <div class="image-tip">最多 10 张，单张不超过 2MB；支持 JPG、PNG、GIF、WEBP；选择后自动上传至服务器</div>
        </el-form-item>
        <el-form-item label="商品名称" prop="name">
          <el-input
            v-model="productForm.name"
            placeholder="输入商品名称"
            :maxlength="100"
            show-word-limit
          />
        </el-form-item>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="商品分类" prop="categoryName">
              <el-select
                v-model="productForm.categoryName"
                v-load-more="{ popperClass: 'products-form-category-dropdown', onLoadMore: loadMoreCategoryOptions, disabled: categoryOptionsLoading || !categoryOptionsHasMore }"
                popper-class="products-form-category-dropdown"
                placeholder="请选择分类"
                style="width: 100%"
                filterable
                @visible-change="onCategoryFormVisibleChange"
                @filter-method="onCategoryFilter"
              >
                <el-option v-for="c in categoryOptions" :key="c.id" :label="c.name" :value="c.name" />
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
                <el-form-item label="商品描述" prop="description">
          <el-input
            v-model="productForm.description"
            type="textarea"
            :rows="3"
            maxlength="500"
            show-word-limit
            placeholder="输入商品描述"
          />
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
    <el-dialog v-model="productDetailVisible" title="商品详情" :width="detailDialogWidth">
      <div class="product-detail-image-wrapper">
        <div v-if="currentProductDetailImages.length" class="product-detail-images-row">
          <img
            v-for="(u, i) in currentProductDetailImages"
            :key="i"
            :src="u"
            alt="商品图片"
            class="product-detail-thumb"
          />
        </div>
        <span v-else class="product-detail-placeholder">{{ getProductIcon(currentProduct?.categoryName || currentProduct?.category) }}</span>
      </div>
      <el-descriptions :column="2" border>
        <el-descriptions-item label="商品编码">{{ currentProduct?.code }}</el-descriptions-item>
        <el-descriptions-item label="商品名称">{{ currentProduct?.name }}</el-descriptions-item>
        <el-descriptions-item label="品牌">{{ currentProduct?.brand }}</el-descriptions-item>
        <el-descriptions-item label="规格">{{ currentProduct?.spec }}</el-descriptions-item>
        <el-descriptions-item label="分类">{{ currentProduct?.categoryName || currentProduct?.category }}</el-descriptions-item>
        <el-descriptions-item label="成本价">¥{{ formatAmountDisplay(currentProduct?.costPrice ?? 0) }}</el-descriptions-item>
        <el-descriptions-item label="销售价">¥{{ formatAmountDisplay(currentProduct?.salePrice ?? 0) }}</el-descriptions-item>
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
    <el-dialog v-model="importDialogVisible" title="批量导入商品" :width="importDialogWidth">
      <div class="import-template-bar">
        <el-button type="primary" @click="downloadProductImportTemplate">
          <el-icon><Download /></el-icon>
          下载导入模板
        </el-button>
        <span class="import-template-hint">请使用模板表头填写，勿改列名；详见模板内「填写说明」工作表</span>
      </div>
      <el-upload
        ref="productImportUploadRef"
        drag
        action="#"
        :auto-upload="false"
        :on-change="handleFileChange"
        accept=".xlsx,.xls"
      >
        <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
        <div class="el-upload__text">
          将文件拖到此处，或<em>点击上传</em>
        </div>
        <template #tip>
          <div class="el-upload__tip">
            请上传 xlsx / xls，服务端异步解析导入；单文件不超过 10MB
          </div>
        </template>
      </el-upload>
      <template #footer>
        <el-button :disabled="importLoading" @click="importDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="importLoading" @click="executeImport">开始导入</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount, watch, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { productApi, categoryApi, CATEGORY_STATUS } from '@/api'
import * as XLSX from 'xlsx'
import JSZip from 'jszip'
import ProductImageThumb from '@/components/ProductImageThumb.vue'
import { parseProductImageUrls, firstProductImageUrl, encodeProductImagesForApi } from '@/utils/productImages'
import { MAX_IMAGE_UPLOAD_BYTES } from '@/utils/uploadLimits'
import { formatAmountDisplay } from '@/utils/moneyFormat'

const router = useRouter()
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
/** 以图搜图请求用原始文件（multipart），与预览用 Data URL 分离 */
const queryImageFile = ref(null)
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
const productImportUploadRef = ref(null)
const importLoading = ref(false)
const loading = ref(false)
const productsCardLoading = computed(() => loading.value || imageSearchLoading.value)
const imageUploading = ref(false)
const viewportWidth = ref(typeof window !== 'undefined' ? window.innerWidth : 1440)
const viewportHeight = ref(typeof window !== 'undefined' ? window.innerHeight : 900)

/** 分类下拉：分页 + 触底加载，与库存页商品/仓库下拉一致 */
const FILTER_DROPDOWN_PAGE_SIZE = 10
const categoryOptions = ref([])
const categoryOptionsPage = ref(0)
const categoryOptionsTotal = ref(0)
const categoryOptionsKeyword = ref('')
const categoryOptionsLoading = ref(false)
const categoryOptionsHasMore = computed(() => categoryOptions.value.length < categoryOptionsTotal.value)

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

const loadMoreCategoryOptions = async ({ reset = false, keyword = null } = {}) => {
  if (categoryOptionsLoading.value) return
  if (reset) {
    categoryOptions.value = []
    categoryOptionsPage.value = 0
    categoryOptionsTotal.value = 0
    categoryOptionsKeyword.value = keyword ?? ''
  } else if (!categoryOptionsHasMore.value && categoryOptionsPage.value > 0) {
    return
  }
  categoryOptionsLoading.value = true
  try {
    const nextPage = categoryOptionsPage.value + 1
    const res = await categoryApi.listPage({
      page: nextPage,
      size: FILTER_DROPDOWN_PAGE_SIZE,
      status: CATEGORY_STATUS.ENABLED,
      keyword: categoryOptionsKeyword.value || undefined
    })
    const rows = res?.list || []
    categoryOptions.value = mergeOptionsById(categoryOptions.value, rows)
    categoryOptionsPage.value = nextPage
    categoryOptionsTotal.value = Number(res?.total) || categoryOptions.value.length
    if (rows.length < FILTER_DROPDOWN_PAGE_SIZE) {
      categoryOptionsTotal.value = categoryOptions.value.length
    }
  } finally {
    categoryOptionsLoading.value = false
  }
}

const mergeCategoryByName = async (name) => {
  if (!name) return
  if (categoryOptions.value.some((c) => c.name === name)) return
  try {
    const res = await categoryApi.listPage({
      page: 1,
      size: 50,
      status: CATEGORY_STATUS.ENABLED,
      keyword: name
    })
    const rows = (res?.list || []).filter((c) => c.name === name)
    if (rows.length) categoryOptions.value = mergeOptionsById(categoryOptions.value, rows)
  } catch {
    /* ignore */
  }
}

const onCategoryFilterVisibleChange = async (visible) => {
  if (!visible) return
  if (categoryOptions.value.length === 0) {
    await loadMoreCategoryOptions({ reset: true, keyword: '' })
  }
  if (filterCategory.value) await mergeCategoryByName(filterCategory.value)
}

const onCategoryFormVisibleChange = async (visible) => {
  if (!visible) return
  if (categoryOptions.value.length === 0) {
    await loadMoreCategoryOptions({ reset: true, keyword: '' })
  }
  if (productForm.value.categoryName) await mergeCategoryByName(productForm.value.categoryName)
}

const onCategoryFilter = (keyword) => {
  loadMoreCategoryOptions({ reset: true, keyword: keyword || '' })
}
const tableMaxHeight = computed(() => {
  return Math.max(320, Math.min(700, viewportHeight.value - 320))
})
const productDialogWidth = computed(() => {
  if (viewportWidth.value <= 768) return '92vw'
  if (viewportWidth.value <= 1200) return '78vw'
  return '600px'
})
const detailDialogWidth = computed(() => {
  if (viewportWidth.value <= 768) return '92vw'
  if (viewportWidth.value <= 1200) return '70vw'
  return '500px'
})
const importDialogWidth = computed(() => {
  if (viewportWidth.value <= 768) return '92vw'
  if (viewportWidth.value <= 1200) return '76vw'
  return '560px'
})

const currentProductDetailImages = computed(() => parseProductImageUrls(currentProduct.value?.image))

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

// 加载当前页商品列表（分类下拉改为打开时按需分页加载）
const loadProducts = async () => {
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

// 分页加载仅由 el-pagination 的 @current-change / @size-change 触发。
// 若再 watch currentPage：会与 v-model 更新叠加，导致翻页请求 /products 调用两次。

const onProductQueryImageChange = (uploadFile) => {
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
  if (!canProductRead.value) {
    ElMessage.warning('无商品数据查看权限')
    return
  }
  if (!queryImageFile.value) {
    ElMessage.warning('请先上传图片')
    return
  }
  currentPage.value = 1
  await runImageSearch()
}

const runImageSearch = async () => {
  if (!queryImageFile.value) return
  imageSearchLoading.value = true
  try {
    const body = {
      file: queryImageFile.value,
      page: currentPage.value,
      size: pageSize.value,
      keyword: searchKeyword.value || undefined,
      categoryName: filterCategory.value || undefined,
      status: filterStatus.value || undefined,
      similarityThreshold: imageSimilarityThreshold.value
    }
    const res = await productApi.searchByImage(body)
    imageSearchRows.value = res.list || []
    imageSearchTotal.value = Number(res.total) || 0
    imageSearchMode.value = true
    if (imageSearchTotal.value === 0) {
      ElMessage.info('没有相似度达标的商品，可以调低阈值再试')
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
  imageList: []
})

const productRules = {
  imageList: [
    {
      required: true,
      message: '请至少上传 1 张商品图片',
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
  salePrice: [{ required: true, message: '请输入销售价', trigger: 'blur' }],
  description: [{ max: 500, message: '商品描述长度不能超过 500 个字符', trigger: 'blur' }]
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
    imageList: []
  }
  productDialogVisible.value = true
}

// 选择图片后上传至服务端，数据库仅存 URL
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
        await mergeCategoryByName(productForm.value.categoryName)
        const category = categoryOptions.value.find((c) => c.name === productForm.value.categoryName)
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

/** 与后端 ProductDTO 对齐的导入模板列（首行为表头） */
const PRODUCT_IMPORT_TEMPLATE_HEADERS = [
  '商品编码',
  '商品名称',
  '分类名称',
  '品牌',
  '规格',
  '成本价',
  '销售价',
  '状态',
  '图片URL',
  '商品描述',
  '安全库存',
  '初始库存',
  '入库仓库ID'
]

/** 生成并下载规范 Excel 导入模板（含「填写说明」工作表） */
const downloadProductImportTemplate = () => {
  const wsMain = XLSX.utils.aoa_to_sheet([PRODUCT_IMPORT_TEMPLATE_HEADERS])
  wsMain['!cols'] = [
    { wch: 14 },
    { wch: 22 },
    { wch: 14 },
    { wch: 12 },
    { wch: 16 },
    { wch: 10 },
    { wch: 10 },
    { wch: 8 },
    { wch: 36 },
    { wch: 28 },
    { wch: 10 },
    { wch: 10 },
    { wch: 12 }
  ]

  const instructionRows = [
    ['商品批量导入 — 填写说明'],
    [''],
    ['一、必填字段'],
    ['商品名称、分类名称、成本价、销售价 为必填；其余列选填。'],
    [''],
    ['二、字段说明'],
    ['商品编码：可留空，由系统在导入时生成；若填写须保证与现有数据不重复。'],
    ['分类名称：须与「商品分类管理」中的名称完全一致。'],
    ['成本价 / 销售价：数字，支持小数。'],
    ['状态：填「在售」或「停售」；留空则按「在售」处理。'],
    ['图片URL：可填可访问的图片地址；留空则导入后需在界面补传图片。'],
    ['安全库存 / 初始库存：非负整数；初始库存、入库仓库ID 仅在新建商品且需要自动入库时填写。'],
    ['入库仓库ID：须为系统中已存在仓库的数字 ID，可与「仓库管理」对照。'],
    [''],
    ['三、格式要求'],
    ['同文件中若多行各列内容完全相同（含商品编码），仅导入第一条，其余重复行自动跳过。'],
    ['请勿修改首行表头文字或列顺序；从第 2 行起逐行填写一条商品。'],
    ['建议直接使用本模板保存为 .xlsx 后再填写，勿在表头行插入或合并单元格。']
  ]
  const wsHelp = XLSX.utils.aoa_to_sheet(instructionRows)
  wsHelp['!cols'] = [{ wch: 88 }]

  const workbook = XLSX.utils.book_new()
  XLSX.utils.book_append_sheet(workbook, wsMain, '商品导入')
  XLSX.utils.book_append_sheet(workbook, wsHelp, '填写说明')
  const dateStr = new Date().toISOString().slice(0, 10)
  XLSX.writeFile(workbook, `商品导入模板_${dateStr}.xlsx`)
  ElMessage.success('已开始下载导入模板')
}

// 批量导入
const handleImport = async () => {
  if (!canProductAdd.value) {
    ElMessage.warning('无添加商品权限')
    return
  }
  uploadFile.value = null
  importLoading.value = false
  importDialogVisible.value = true
  await nextTick()
  productImportUploadRef.value?.clearFiles()
}

const handleFileChange = (file) => {
  uploadFile.value = file.raw
}

const sleep = (ms) => new Promise((resolve) => setTimeout(resolve, ms))

/** 提交 Excel 至后端异步导入，并轮询任务状态 */
const executeImport = async () => {
  if (!canProductAdd.value) {
    ElMessage.warning('无添加商品权限')
    return
  }
  if (!uploadFile.value) {
    ElMessage.warning('请先选择要导入的文件')
    return
  }
  const maxBytes = 10 * 1024 * 1024
  if (uploadFile.value.size > maxBytes) {
    ElMessage.warning('文件大小不能超过 10MB')
    return
  }

  importLoading.value = true
  try {
    const formData = new FormData()
    formData.append('file', uploadFile.value)
    const submit = await productApi.importProducts(formData)
    const jobId = submit?.jobId
    if (!jobId) {
      ElMessage.error('服务端未返回任务编号')
      return
    }

    const maxPoll = 300
    let task = null
    for (let i = 0; i < maxPoll; i++) {
      await sleep(800)
      task = await productApi.getImportTask(jobId)
      if (task && (task.status === 'COMPLETED' || task.status === 'FAILED')) {
        break
      }
    }

    if (!task) {
      ElMessage.error('无法获取导入任务状态')
      return
    }

    if (task.status === 'FAILED') {
      ElMessage.error(task.message || '导入任务失败')
      return
    }

    if (task.status !== 'COMPLETED') {
      ElMessage.warning('导入仍在处理中，请稍后刷新商品列表查看结果')
      return
    }

    await loadProducts()

    const errs = task.errors || []
    const detail =
      errs.slice(0, 30).join('\n') + (errs.length > 30 ? `\n… 共 ${errs.length} 条` : '')

    const sc = task.successCount ?? 0
    const fc = task.failCount ?? 0

    if (sc === 0 && fc === 0) {
      ElMessage.warning(task.message || '没有导入任何数据行')
    } else if (fc > 0 && detail) {
      ElMessage.warning(task.message || '导入结束')
      await ElMessageBox.alert(detail, '失败明细', { type: 'warning', confirmButtonText: '知道了' })
    } else {
      ElMessage.success(task.message || '导入完成')
    }

    importDialogVisible.value = false
    uploadFile.value = null
    await nextTick()
    productImportUploadRef.value?.clearFiles()
  } catch (e) {
    ElMessage.error(e.message || '导入失败')
  } finally {
    importLoading.value = false
  }
}

// 批量导出 - 普通列表按筛选拉取；以图搜图则按当前查询图与阈值导出图搜命中结果（与列表同上限）
const handleExport = async () => {
  if (!canProductRead.value) {
    ElMessage.warning('无商品数据查看权限')
    return
  }
  if (imageSearchMode.value && !queryImageFile.value) {
    ElMessage.warning('查询图片已失效，请重新以图搜图后再导出')
    return
  }
  loading.value = true
  let rows = []
  try {
    if (imageSearchMode.value) {
      const res = await productApi.searchByImage({
        file: queryImageFile.value,
        page: 1,
        size: 5000,
        keyword: searchKeyword.value || undefined,
        categoryName: filterCategory.value || undefined,
        status: filterStatus.value || undefined,
        similarityThreshold: imageSimilarityThreshold.value
      })
      rows = res.list || []
    } else {
      const res = await productApi.list({
        page: 1,
        size: 5000,
        keyword: searchKeyword.value || undefined,
        categoryName: filterCategory.value || undefined,
        productStatus: filterStatus.value || undefined
      })
      rows = res.list || []
    }
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

  const formatExportDateTime = (v) => {
    if (v == null || v === '') return ''
    const s = typeof v === 'string' ? v : String(v)
    return s.replace('T', ' ').replace(/\.\d{3}Z?$/, '').trim()
  }

  const resolveAbsoluteImageUrl = (url) => {
    const u = String(url || '').trim()
    if (!u) return ''
    if (/^https?:\/\//i.test(u)) return u
    if (u.startsWith('/')) return `${window.location.origin}${u}`
    return `${window.location.origin}/${u}`
  }

  const extFromImageUrl = (url) => {
    const s = String(url).split('?')[0].toLowerCase()
    const m = s.match(/\.(jpe?g|png|gif|webp)$/)
    if (m) return m[1] === 'jpeg' ? 'jpg' : m[1]
    return ''
  }

  const extFromBlob = (blob, url) => {
    const t = (blob && blob.type) || ''
    if (t.includes('png')) return 'png'
    if (t.includes('gif')) return 'gif'
    if (t.includes('webp')) return 'webp'
    if (t.includes('jpeg') || t.includes('jpg')) return 'jpg'
    const fromUrl = extFromImageUrl(url)
    return fromUrl || 'jpg'
  }

  const safeImageBaseName = (p) => {
    const raw = (p.code && String(p.code).trim()) || `id_${p.id}`
    const cleaned = raw.replace(/[\\/:*?"<>|]+/g, '_').trim() || `id_${p.id}`
    return cleaned.slice(0, 72)
  }

  const loadImageBlob = async (url) => {
    const u = String(url || '').trim()
    if (!u) throw new Error('empty')
    if (u.startsWith('data:image')) {
      const res = await fetch(u)
      if (!res.ok) throw new Error('data-url')
      return res.blob()
    }
    const abs = resolveAbsoluteImageUrl(u)
    const token = localStorage.getItem('token')
    const res = await fetch(abs, {
      headers: token ? { Authorization: `Bearer ${token}` } : {}
    })
    if (!res.ok) throw new Error(String(res.status))
    return res.blob()
  }

  const runPool = async (items, limit, fn) => {
    const results = new Array(items.length)
    let cursor = 0
    const worker = async () => {
      while (true) {
        const i = cursor++
        if (i >= items.length) break
        results[i] = await fn(items[i], i)
      }
    }
    const n = Math.min(Math.max(limit, 1), Math.max(items.length, 1))
    await Promise.all(Array.from({ length: n }, () => worker()))
    return results
  }

  const zip = new JSZip()
  const imageTasks = []
  for (const p of rows) {
    const urls = parseProductImageUrls(p?.image)
    urls.forEach((url, idx) => {
      imageTasks.push({ p, idx, url })
    })
  }

  const byProduct = new Map()
  for (const p of rows) {
    byProduct.set(p.id, { paths: [], fails: [] })
  }

  loading.value = true
  try {
    await runPool(imageTasks, 6, async ({ p, idx, url }) => {
      const slot = byProduct.get(p.id)
      const base = safeImageBaseName(p)
      const fileBase = `${base}_pid${p.id}_${idx}`
      try {
        const blob = await loadImageBlob(url)
        const ext = extFromBlob(blob, url)
        const relPath = `images/${fileBase}.${ext}`
        zip.file(relPath, blob)
        slot.paths.push(relPath)
      } catch {
        slot.fails.push(url)
      }
    })
  } catch (e) {
    ElMessage.error(e.message || '打包商品图片失败')
    return
  } finally {
    loading.value = false
  }

  let okImg = 0
  let failImg = 0
  for (const v of byProduct.values()) {
    okImg += v.paths.length
    failImg += v.fails.length
  }

  // 与商品实体及详情一致；图片为 zip 内实际文件路径（非仅 URL）
  const exportData = rows.map((p) => {
    const img = byProduct.get(p.id) || { paths: [], fails: [] }
    return {
      主键ID: p.id ?? '',
      商品编码: p.code || '',
      商品名称: p.name || '',
      品牌: p.brand || '',
      规格: p.spec || '',
      分类ID: p.categoryId ?? '',
      分类名称: p.categoryName || p.category || '',
      成本价: p.costPrice ?? '',
      销售价: p.salePrice ?? '',
      状态: p.statusName || formatProductStatus(p.status),
      商品图片文件: img.paths.join('; '),
      商品描述: p.description || '',
      库存数量: p.stock ?? '',
      安全库存: p.safeStock ?? '',
      创建时间: formatExportDateTime(p.createTime),
      更新时间: formatExportDateTime(p.updateTime)
    }
  })

  const worksheet = XLSX.utils.json_to_sheet(exportData)
  worksheet['!cols'] = [
    { wch: 10 },
    { wch: 14 },
    { wch: 28 },
    { wch: 12 },
    { wch: 16 },
    { wch: 10 },
    { wch: 14 },
    { wch: 10 },
    { wch: 10 },
    { wch: 10 },
    { wch: 44 },
    { wch: 40 },
    { wch: 10 },
    { wch: 10 },
    { wch: 20 },
    { wch: 20 }
  ]

  const workbook = XLSX.utils.book_new()
  XLSX.utils.book_append_sheet(workbook, worksheet, '商品列表')
  const xlsxArray = XLSX.write(workbook, { type: 'array', bookType: 'xlsx' })
  zip.file('商品列表.xlsx', xlsxArray)

  if (failImg > 0) {
    const lines = ['商品编码/标识\t失败URL']
    for (const p of rows) {
      const fails = byProduct.get(p.id)?.fails || []
      const label = (p.code && String(p.code).trim()) || `id_${p.id}`
      for (const u of fails) {
        lines.push(`${label}\t${u}`)
      }
    }
    lines.unshift('以下为浏览器未能拉取并写入压缩包的图片地址，可与网络/跨域/外链权限对照排查：')
    zip.file('图片下载失败明细.txt', lines.join('\n'))
  }

  const dateStr = new Date().toISOString().slice(0, 10)

  loading.value = true
  try {
    const outBlob = await zip.generateAsync({ type: 'blob' })
    const a = document.createElement('a')
    const href = URL.createObjectURL(outBlob)
    a.href = href
    a.download = `商品列表含图片_${dateStr}.zip`
    a.click()
    URL.revokeObjectURL(href)
  } catch (e) {
    ElMessage.error(e.message || '生成压缩包失败')
    return
  } finally {
    loading.value = false
  }

  const tip =
    failImg > 0
      ? `已导出 ${exportData.length} 条；图片 ${okImg} 张已打包，${failImg} 张未写入（见压缩包内「图片下载失败明细.txt」）`
      : `已导出 ${exportData.length} 条商品；${okImg ? `含 ${okImg} 张图片` : '无商品图片'}`
  ElMessage.success(tip)
}

const handleViewportResize = () => {
  viewportWidth.value = window.innerWidth
  viewportHeight.value = window.innerHeight
}

// 初始化加载数据
onMounted(() => {
  handleViewportResize()
  window.addEventListener('resize', handleViewportResize)
  loadProducts()
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleViewportResize)
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
      justify-content: flex-end;
      align-items: center;

      .header-actions {
        display: flex;
        align-items: center;
        flex-wrap: wrap;
        justify-content: flex-end;
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
    }

    .product-image-cell {
      .product-thumb-list {
        border: 1px solid #E4E7ED;
        border-radius: 6px;
        overflow: hidden;
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

    .filter-control {
      :deep(.el-input__wrapper),
      :deep(.el-select__wrapper) {
        min-width: 0;
      }
    }

    .filter-category {
      width: 140px;
    }

    .filter-status {
      width: 120px;
    }

    .filter-search {
      width: 200px;
    }

    .filter-threshold {
      width: 108px;
    }
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

    .product-detail-images-row {
      display: flex;
      flex-wrap: wrap;
      gap: 8px;
      justify-content: center;
    }

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

@media (max-width: 1366px) {
  .products-page {
    .table-card {
      .card-header {
        justify-content: flex-start;
      }

      .header-actions {
        gap: 10px;
      }
    }
  }
}

@media (max-width: 992px) {
  .products-page {
    .table-card {
      .header-actions {
        justify-content: flex-start;
      }

      .filter-control {
        width: 100% !important;
      }

      .filter-threshold {
        width: 120px !important;
      }

      .pagination-wrapper {
        justify-content: center;
      }
    }
  }
}
/* 弹层内容 teleport 到 body，须与 .products-page 平级以便 scoped 命中 */
.import-template-bar {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 10px;
  margin-bottom: 16px;
}

.import-template-hint {
  font-size: 12px;
  color: #909399;
  line-height: 1.5;
}
</style>