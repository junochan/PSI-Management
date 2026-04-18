<template>
  <div class="customer-edit" v-loading="pageLoading" element-loading-text="加载中...">
    <el-card>
      <template #header>
        <div class="card-header">
          <div class="header-actions">
            <el-button @click="goBack">返回</el-button>
          </div>
        </div>
      </template>
      <el-form ref="customerFormRef" :model="customerForm" :rules="customerRules" label-width="120px" style="max-width: 600px">
        <el-form-item label="客户名称" prop="name">
          <el-input v-model="customerForm.name" :maxlength="100" show-word-limit placeholder="输入客户名称" />
        </el-form-item>
        <el-form-item label="客户类型">
          <el-select v-model="customerForm.type" style="width: 100%">
            <el-option label="个人客户" value="个人" />
            <el-option label="企业客户" value="企业" />
          </el-select>
        </el-form-item>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="联系人" prop="contact">
              <el-input v-model="customerForm.contact" :maxlength="50" show-word-limit placeholder="输入联系人姓名" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="联系电话" prop="phone">
              <el-input v-model="customerForm.phone" :maxlength="20" show-word-limit placeholder="输入联系电话" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="customerForm.email" :maxlength="100" show-word-limit placeholder="输入邮箱地址" />
        </el-form-item>
        <el-form-item label="收货地址" prop="address">
          <el-input v-model="customerForm.address" :maxlength="200" show-word-limit placeholder="输入收货地址" />
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="customerForm.remark" type="textarea" :rows="3" :maxlength="500" show-word-limit placeholder="输入备注信息" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="submitEdit">保存修改</el-button>
          <el-button v-if="canManageCustomer" type="danger" plain @click="deleteCustomerConfirm">删除客户</el-button>
          <el-button @click="goBack">取消</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { customerApi } from '@/api'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const canManageCustomer = computed(() => userStore.hasPermission('sales:customer'))

const customerId = computed(() => route.params.id)
const customerFormRef = ref()
const pageLoading = ref(true)

const customerForm = ref({
  name: '',
  type: '个人',
  vipLevel: '普通',
  contact: '',
  phone: '',
  email: '',
  address: '',
  remark: ''
})

const customerRules = {
  name: [
    { required: true, message: '请输入客户名称', trigger: 'blur' },
    { max: 100, message: '客户名称不能超过100个字符', trigger: 'blur' }
  ],
  contact: [{ max: 50, message: '联系人不能超过50个字符', trigger: 'blur' }],
  phone: [
    { required: true, message: '请输入联系电话', trigger: 'blur' },
    { max: 20, message: '联系电话不能超过20个字符', trigger: 'blur' }
  ],
  email: [{ max: 100, message: '邮箱不能超过100个字符', trigger: 'blur' }],
  address: [
    { required: true, message: '请输入收货地址', trigger: 'blur' },
    { max: 200, message: '收货地址不能超过200个字符', trigger: 'blur' }
  ],
  remark: [{ max: 500, message: '备注不能超过500个字符', trigger: 'blur' }]
}

onMounted(async () => {
  pageLoading.value = true
  try {
    const id = Number(customerId.value)
    if (!id) {
      ElMessage.warning('客户 ID 无效')
      router.replace('/sales')
      return
    }
    const customer = await customerApi.get(id)
    if (customer) {
      customerForm.value = {
        name: customer.name,
        type: customer.type,
        vipLevel: customer.vipLevel || '普通',
        contact: customer.contact || '',
        phone: customer.phone,
        email: customer.email || '',
        address: customer.address,
        remark: customer.remark || ''
      }
    }
  } finally {
    pageLoading.value = false
  }
})

const goBack = () => router.back()

const deleteCustomerConfirm = async () => {
  if (!canManageCustomer.value) {
    ElMessage.warning('无客户管理权限')
    return
  }
  const name = customerForm.value.name || '该客户'
  try {
    await ElMessageBox.confirm(`确定要删除客户「${name}」吗？删除后不可恢复。`, '删除确认', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await customerApi.delete(Number(customerId.value))
    ElMessage.success('客户已删除')
    router.replace('/sales')
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '删除失败')
    }
  }
}

const submitEdit = async () => {
  if (!customerFormRef.value) return
  await customerFormRef.value.validate(async (valid) => {
    if (valid) {
      try {
        await customerApi.update(Number(customerId.value), {
          name: customerForm.value.name,
          type: customerForm.value.type,
          vipLevel: customerForm.value.vipLevel,
          contactPerson: customerForm.value.contact,
          contactPhone: customerForm.value.phone,
          email: customerForm.value.email,
          address: customerForm.value.address,
          remark: customerForm.value.remark
        })
        ElMessage.success('客户信息已更新')
        router.push('/sales')
      } catch (error) {
        ElMessage.error(error.message || '更新失败')
      }
    }
  })
}
</script>

<style lang="scss" scoped>
.customer-edit {
  .card-header {
    display: flex;
    justify-content: flex-end;
    align-items: center;
    .header-actions { display: flex; gap: 12px; }
  }
}
</style>