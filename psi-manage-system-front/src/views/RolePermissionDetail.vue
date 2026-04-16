<template>
  <div class="permission-page">
    <el-card>
      <template #header>
        <div class="card-header">
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

        <div class="permission-tree-wrap" v-loading="loading">
          <el-tree
            ref="treeRef"
            class="permission-tree"
            :data="permissionTree"
            show-checkbox
            node-key="id"
            default-expand-all
            :props="treeProps"
            @check="onTreeCheck"
          >
            <template #default="{ data }">
              <span class="tree-node">
                <span class="permission-name">{{ data.name }}</span>
                <span v-if="data.code" class="permission-code">{{ data.code }}</span>
              </span>
            </template>
          </el-tree>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, nextTick } from 'vue'
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
const treeRef = ref(null)

const ORPHAN_GROUP_KEY = '__orphan_menu__'

/** 一级：菜单(type=1)；二级：该菜单下功能(type=2 且 parentId 指向菜单 id) */
const permissionTree = computed(() => {
  const list = allPermissions.value
  if (!list.length) return []

  const menus = list
    .filter((p) => p.type === 1)
    .sort((a, b) => (a.sort ?? 0) - (b.sort ?? 0))
  const funcs = list.filter((p) => p.type === 2)

  const menuIdSet = new Set(menus.map((m) => Number(m.id)))
  const tree = menus.map((m) => ({
    ...m,
    children: funcs
      .filter((f) => Number(f.parentId) === Number(m.id))
      .sort((a, b) => (a.sort ?? 0) - (b.sort ?? 0))
  }))

  const orphans = funcs.filter((f) => {
    const pid = f.parentId != null ? Number(f.parentId) : NaN
    return !Number.isFinite(pid) || !menuIdSet.has(pid)
  })
  if (orphans.length) {
    tree.push({
      id: ORPHAN_GROUP_KEY,
      name: '其他',
      code: '',
      type: 1,
      disabled: true,
      children: orphans.sort((a, b) => (a.sort ?? 0) - (b.sort ?? 0))
    })
  }

  return tree
})

const treeProps = computed(() => ({
  label: 'name',
  children: 'children',
  disabled: (data) => role.value?.id === 1 || data.disabled === true
}))

const syncTreeCheckedKeys = () => {
  nextTick(() => {
    const ids = rolePermissions.value.map((id) => Number(id)).filter((id) => Number.isFinite(id))
    treeRef.value?.setCheckedKeys(ids, false)
  })
}

const onTreeCheck = () => {
  if (role.value?.id === 1 || !treeRef.value) return
  const keys = treeRef.value.getCheckedKeys(false)
  const valid = new Set(allPermissions.value.map((p) => Number(p.id)))
  rolePermissions.value = keys
    .map((k) => Number(k))
    .filter((id) => valid.has(id))
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
  loading.value = true
  try {
    role.value = await roleApi.get(Number(roleId.value))

    // 获取所有权限
    allPermissions.value = await roleApi.allPermissions() || []

    // 获取该角色的权限
    const permissions = await roleApi.permissions(Number(roleId.value))
    rolePermissions.value = permissions.map(p => p.id)
    syncTreeCheckedKeys()
  } catch (error) {
    console.error('加载权限数据失败:', error)
    ElMessage.error('加载数据失败')
  } finally {
    loading.value = false
  }
}

// 返回到系统设置-角色管理页签
const goBack = () => {
  router.push({
    path: '/settings',
    query: { tab: 'roles' }
  })
}

onMounted(() => {
  loadData()
})
</script>

<style lang="scss" scoped>
.permission-page {
  .card-header {
    display: flex;
    justify-content: flex-end;
    align-items: center;
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

  .permission-tree-wrap {
    min-height: 120px;
    padding: 8px 0;
    border: 1px solid var(--el-border-color-lighter);
    border-radius: 8px;
    background: var(--el-fill-color-blank);
  }

  .permission-tree {
    padding: 8px 12px;
    background: transparent;

    :deep(.el-tree-node__content) {
      height: auto;
      min-height: 32px;
      padding: 4px 0;
      align-items: flex-start;
    }

    .tree-node {
      display: inline-flex;
      flex-wrap: wrap;
      align-items: baseline;
      gap: 8px;
    }

    .permission-name {
      font-size: 14px;
      color: #303133;
    }

    .permission-code {
      font-size: 12px;
      color: #909399;
      font-family: monospace;
    }
  }
}
</style>