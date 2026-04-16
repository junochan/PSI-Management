<template>
  <div class="customer-edit">
    <el-card>
      <template #header>
        <div class="card-header">
          <div class="header-actions">
            <el-button v-if="canManageCustomer" type="danger" plain @click="deleteCustomerConfirm">删除客户</el-button>
            <el-button @click="goBack">返回</el-button>
          </div>
        </div>
      </template>
      <el-form ref="customerFormRef" :model="customerForm" :rules="customerRules" label-width="120px" style="max-width: 600px">
        <el-form-item label="客户名称" prop="name">
          <el-input v-model="customerForm.name" placeholder="输入客户名称" />
        </el-form-item>
        <el-form-item label="客户类型">
          <el-select v-model="customerForm.type" style="width: 100%">
            <el-option label="个人客户" value="个人" />
            <el-option label="企业客户" value="企业" />
          </el-select>
        </el-form-item>
        <el-form-item label="联系电话" prop="phone">
          <el-input v-model="customerForm.phone" placeholder="输入联系电话" />
        </el-form-item>
        <el-form-item label="收货地址" prop="address">
          <el-input v-model="customerForm.address" placeholder="输入收货地址" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="customerForm.remark" type="textarea" :rows="3" placeholder="输入备注信息" />
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

const customerForm = ref({
  name: '',
  type: '个人',
  vipLevel: '普通',
  phone: '',
  address: '',
  remark: ''
})

const customerRules = {
  name: [{ required: true, message: '请输入客户名称', trigger: 'blur' }],
  phone: [{ required: true, message: '请输入联系电话', trigger: 'blur' }],
  address: [{ required: true, message: '请输入收货地址', trigger: 'blur' }]
}

onMounted(async () => {
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
      phone: customer.phone,
      address: customer.address,
      remark: customer.remark || ''
    }
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
          contactPhone: customerForm.value.phone,
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