<template>
  <div class="supplier-edit">
    <el-card>
      <template #header>
        <div class="card-header">
          <h3>编辑供应商</h3>
          <div class="header-actions">
            <el-button @click="goBack">返回</el-button>
          </div>
        </div>
      </template>
      <el-form ref="supplierFormRef" :model="supplierForm" :rules="supplierRules" label-width="120px" style="max-width: 600px">
        <el-form-item label="供应商名称" prop="name">
          <el-input v-model="supplierForm.name" placeholder="输入供应商名称" />
        </el-form-item>
        <el-form-item label="所属行业">
          <el-select
            v-model="supplierForm.industryIds"
            multiple
            collapse-tags
            collapse-tags-tooltip
            clearable
            placeholder="请选择所属行业（可多选）"
            style="width: 100%"
            filterable
          >
            <el-option v-for="it in supplierIndustriesList" :key="it.id" :label="it.name" :value="it.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="地址" prop="address">
          <el-input v-model="supplierForm.address" placeholder="输入地址" />
        </el-form-item>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="联系电话" prop="phone">
              <el-input v-model="supplierForm.phone" placeholder="输入联系电话" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="邮箱" prop="email">
              <el-input v-model="supplierForm.email" placeholder="输入邮箱" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="备注">
          <el-input v-model="supplierForm.remark" type="textarea" :rows="3" placeholder="输入备注信息" />
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
import { useDataStore } from '@/stores/data'
import { useUserStore } from '@/stores/user'
import { supplierApi } from '@/api'

const router = useRouter()
const route = useRoute()
const dataStore = useDataStore()
const userStore = useUserStore()

const supplierId = computed(() => route.params.id)
const supplierFormRef = ref()

const supplierForm = ref({
  name: '',
  industryIds: [],
  address: '',
  phone: '',
  email: '',
  remark: ''
})

const supplierIndustriesList = computed(() => dataStore.supplierIndustries || [])

const supplierRules = {
  name: [{ required: true, message: '请输入供应商名称', trigger: 'blur' }],
  address: [{ required: true, message: '请输入地址', trigger: 'blur' }],
  phone: [{ required: true, message: '请输入联系电话', trigger: 'blur' }]
}

onMounted(async () => {
  if (!userStore.hasPermission('purchase:supplier')) {
    ElMessage.warning('无供应商管理权限')
    router.replace('/purchase')
    return
  }
  await Promise.all([dataStore.loadSuppliers(), dataStore.loadSupplierIndustries()])
  const supplier = dataStore.suppliers.find(s => s.id === Number(supplierId.value))
  if (supplier) {
    supplierForm.value = {
      name: supplier.name,
      industryIds: Array.isArray(supplier.industryIds) ? [...supplier.industryIds] : [],
      address: supplier.address,
      phone: supplier.phone,
      email: supplier.email || '',
      remark: supplier.remark || ''
    }
  }
})

const goBack = () => router.back()

const submitEdit = async () => {
  if (!supplierFormRef.value) return
  await supplierFormRef.value.validate(async (valid) => {
    if (valid) {
      try {
        await supplierApi.update(Number(supplierId.value), {
          name: supplierForm.value.name,
          industryIds: Array.isArray(supplierForm.value.industryIds) ? [...supplierForm.value.industryIds] : [],
          address: supplierForm.value.address,
          contactPhone: supplierForm.value.phone,
          email: supplierForm.value.email,
          remark: supplierForm.value.remark
        })
        await dataStore.loadSuppliers()
        ElMessage.success('供应商信息已更新')
        router.push('/purchase')
      } catch (error) {
        ElMessage.error(error.message || '更新失败')
      }
    }
  })
}
</script>

<style lang="scss" scoped>
.supplier-edit {
  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    h3 { font-size: 18px; font-weight: 600; color: #303133; }
    .header-actions { display: flex; gap: 12px; }
  }
}
</style>