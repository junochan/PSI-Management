<template>
  <div class="permission-page">
    <el-card>
      <template #header>
        <div class="card-header">
          <h3>权限管理</h3>
          <el-button @click="goBack">返回</el-button>
        </div>
      </template>

      <!-- 角色信息 -->
      <div class="role-info">
        <div class="role-header">
          <h4>{{ role?.name }}</h4>
          <el-tag :type="role?.id === 1 ? 'danger' : 'info'" effect="light" size="small">{{ role?.id === 1 ? '系统级' : '普通' }}</el-tag>
        </div>
        <p class="role-description">{{ role?.description || '暂无描述' }}</p>
      </div>

      <el-divider />

      <!-- 权限列表 -->
      <div class="permission-section">
        <div class="section-header">
          <h4>权限配置</h4>
          <el-button type="primary" @click="savePermissions" :disabled="role?.id === 1">保存权限</el-button>
        </div>

        <el-alert v-if="role?.id === 1" type="warning" title="超级管理员拥有所有权限，不可修改" :closable="false" show-icon style="margin-bottom: 16px" />

        <div class="permission-grid">
          <div class="permission-category" v-for="category in permissionCategories" :key="category.name">
            <h5 class="category-title">{{ category.name }}</h5>
            <div class="permission-items">
              <div class="permission-item" v-for="p in category.permissions" :key="p.id">
                <el-checkbox
                  :model-value="hasPermission(p.id)"
                  :disabled="role?.id === 1"
                  @change="(val) => togglePermission(p.id, val)"
                >
                  <span class="permission-name">{{ p.name }}</span>
                  <span class="permission-code">{{ p.code }}</span>
                </el-checkbox>
              </div>
            </div>
          </div>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { roleApi } from '@/api'

const router = useRouter()
const route = useRoute()

const roleId = computed(() => route.params.id)
const role = ref(null)
const allPermissions = ref([])
const rolePermissions = ref([]) // 当前角色的权限ID列表
const loading = ref(false)

// 权限分类
const permissionCategories = computed(() => {
  // 将权限按模块分类
  const categories = {}
  allPermissions.value.forEach(p => {
    const module = p.module || '其他'
    if (!categories[module]) {
      categories[module] = { name: module, permissions: [] }
    }
    categories[module].permissions.push(p)
  })
  return Object.values(categories)
})

// 判断是否有某权限
const hasPermission = (permissionId) => {
  return rolePermissions.value.includes(permissionId)
}

// 切换权限
const togglePermission = (permissionId, has) => {
  if (has) {
    if (!rolePermissions.value.includes(permissionId)) {
      rolePermissions.value.push(permissionId)
    }
  } else {
    rolePermissions.value = rolePermissions.value.filter(id => id !== permissionId)
  }
}

// 保存权限
const savePermissions = async () => {
  if (role.value?.id === 1) {
    ElMessage.warning('超级管理员权限不可修改')
    return
  }
  loading.value = true
  try {
    const validPermissionIds = rolePermissions.value.map(id => Number(id))
    await roleApi.updatePermissions(role.value.id, validPermissionIds)
    ElMessage.success('权限已保存')
  } catch (error) {
    ElMessage.error(error.message || '保存失败')
  } finally {
    loading.value = false
  }
}

// 加载角色和权限数据
const loadData = async () => {
  try {
    // 获取角色信息
    const roles = await roleApi.list() || []
    role.value = roles.find(r => r.id === Number(roleId.value))

    // 获取所有权限
    allPermissions.value = await roleApi.allPermissions() || []

    // 获取该角色的权限
    const permissions = await roleApi.permissions(Number(roleId.value))
    rolePermissions.value = permissions.map(p => p.id)
  } catch (error) {
    console.error('加载权限数据失败:', error)
    ElMessage.error('加载数据失败')
  }
}

// 返回
const goBack = () => router.back()

onMounted(() => {
  loadData()
})
</script>

<style lang="scss" scoped>
.permission-page {
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

  .role-info {
    .role-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 8px;
      h4 {
        font-size: 20px;
        font-weight: 600;
        color: #303133;
      }
    }
    .role-description {
      font-size: 14px;
      color: #909399;
    }
  }

  .permission-section {
    .section-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 16px;
      h4 {
        font-size: 16px;
        font-weight: 600;
        color: #303133;
      }
    }
  }

  .permission-grid {
    display: grid;
    grid-template-columns: repeat(2, 1fr);
    gap: 24px;

    .permission-category {
      .category-title {
        font-size: 14px;
        font-weight: 600;
        color: #606266;
        margin-bottom: 12px;
        padding-bottom: 8px;
        border-bottom: 1px solid #E4E7ED;
      }

      .permission-items {
        .permission-item {
          display: flex;
          align-items: center;
          padding: 8px 0;

          .permission-name {
            font-size: 14px;
            color: #303133;
          }

          .permission-code {
            font-size: 12px;
            color: #909399;
            margin-left: 8px;
            font-family: monospace;
          }
        }
      }
    }
  }
}

@media (max-width: 1200px) {
  .permission-page {
    .permission-grid {
      grid-template-columns: 1fr;
    }
  }
}
</style>