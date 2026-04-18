<template>
  <div class="settings-page" v-loading="pageLoading" element-loading-text="加载中...">
    <el-tabs v-model="activeTab" class="page-tabs">
      <!-- 用户管理 - 仅超级管理员可见 -->
      <el-tab-pane label="用户管理" name="users" v-if="isCurrentUserSuperAdmin">
        <el-card>
          <template #header>
            <div class="card-header">
              <el-button type="primary" size="small" @click="openUserDialog"><el-icon><Plus /></el-icon>添加用户</el-button>
            </div>
          </template>
          <el-table :data="userTableList" empty-text="暂无数据" style="width: 100%" table-layout="fixed">
            <el-table-column label="登录名" prop="username" min-width="120" show-overflow-tooltip />
            <el-table-column label="姓名" prop="name" min-width="120" show-overflow-tooltip />
            <el-table-column label="邮箱" prop="email" min-width="200" show-overflow-tooltip />
            <el-table-column label="角色" min-width="128" align="center" show-overflow-tooltip>
              <template #default="{ row }">
                <el-tag :type="getRoleType(row.roleId)" effect="light" size="small">{{ getUserRoleName(row) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="状态" min-width="88" align="center">
              <template #default="{ row }">
                <el-tag :type="row.status === 1 ? 'success' : 'info'" effect="light" size="small">{{ row.status === 1 ? '正常' : '禁用' }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="最后登录" width="176" show-overflow-tooltip>
              <template #default="{ row }">{{ formatTime(row.lastLoginTime) }}</template>
            </el-table-column>
            <el-table-column label="操作" min-width="168" align="center" fixed="right">
              <template #default="{ row }">
                <el-button type="primary" link size="small" @click="editUser(row)">编辑</el-button>
                <el-button type="danger" link size="small" @click="deleteUser(row)" v-if="!isSuperAdmin(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
          <div class="pagination-wrapper">
            <el-pagination
              v-model:current-page="userCurrentPage"
              v-model:page-size="userPageSize"
              :total="userTotal"
              :page-sizes="[10, 20, 50, 100]"
              layout="total, sizes, prev, pager, next, jumper"
            />
          </div>
        </el-card>
      </el-tab-pane>

      <!-- 角色管理 - 仅超级管理员可见 -->
      <el-tab-pane label="角色管理" name="roles" v-if="isCurrentUserSuperAdmin">
        <el-card>
          <template #header>
            <div class="card-header">
              <el-button type="primary" size="small" @click="openRoleDialog"><el-icon><Plus /></el-icon>添加角色</el-button>
            </div>
          </template>
          <el-table :data="roleTableList" empty-text="暂无数据" style="width: 100%" table-layout="fixed">
            <el-table-column label="角色名称" prop="name" min-width="160" show-overflow-tooltip />
            <el-table-column label="角色级别" min-width="108" align="center">
              <template #default="{ row }">
                <el-tag :type="row.id === 1 ? 'danger' : row.id === 2 ? 'warning' : 'info'" effect="light" size="small">{{ row.id === 1 ? '系统级' : row.id === 2 ? '管理级' : '普通' }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="描述" prop="description" min-width="220" show-overflow-tooltip>
              <template #default="{ row }">{{ row.description || '暂无描述' }}</template>
            </el-table-column>
            <el-table-column label="状态" min-width="88" align="center">
              <template #default="{ row }">
                <el-tag :type="row.status === 1 ? 'success' : 'info'" effect="light" size="small">{{ row.status === 1 ? '启用' : '禁用' }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" min-width="208" align="center" fixed="right">
              <template #default="{ row }">
                <el-button type="primary" link size="small" @click="editRole(row)" v-if="row.id !== 1">编辑</el-button>
                <el-button type="primary" link size="small" @click="openPermissionDialog(row)">权限</el-button>
                <el-button type="danger" link size="small" @click="deleteRole(row)" v-if="row.id !== 1">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
          <div class="pagination-wrapper">
            <el-pagination
              v-model:current-page="roleCurrentPage"
              v-model:page-size="rolePageSize"
              :total="roleTotal"
              :page-sizes="[10, 20, 50, 100]"
              layout="total, sizes, prev, pager, next, jumper"
            />
          </div>
        </el-card>
      </el-tab-pane>

      <!-- 系统配置 -->
      <el-tab-pane label="系统配置" name="system">
        <div class="config-sections">
          <el-card class="config-card">
            <el-form label-width="100px">
              <el-form-item label="公司名称"><el-input v-model="systemConfig.companyName" /></el-form-item>
              <el-form-item label="联系电话"><el-input v-model="systemConfig.phone" /></el-form-item>
              <el-form-item label="公司地址"><el-input v-model="systemConfig.address" /></el-form-item>
            </el-form>
          </el-card>
          <el-card class="config-card">
            <el-form label-width="100px">
              <el-form-item label="安全库存预警"><el-select v-model="systemConfig.stockWarning" style="width: 100%"><el-option label="开启" value="开启" /><el-option label="关闭" value="关闭" /></el-select></el-form-item>
              <el-form-item label="呆滞商品天数"><el-input-number v-model="systemConfig.staleDays" :min="1" style="width: 100%" /></el-form-item>
            </el-form>
          </el-card>
        </div>
        <el-button type="primary" @click="saveConfig">保存配置</el-button>
      </el-tab-pane>

      <!-- 商品分类 - 仅超级管理员可见 -->
      <el-tab-pane label="商品分类" name="categories" v-if="isCurrentUserSuperAdmin">
        <el-card>
          <template #header>
            <div class="card-header">
              <el-button type="primary" size="small" @click="openCategoryDialog"><el-icon><Plus /></el-icon>添加分类</el-button>
            </div>
          </template>
          <el-table :data="categoryTableList" empty-text="暂无数据" style="width: 100%" table-layout="fixed">
            <el-table-column label="分类名称" prop="name" min-width="160" show-overflow-tooltip />
            <el-table-column label="分类编码" prop="code" min-width="132" show-overflow-tooltip />
            <el-table-column label="排序" prop="sort" min-width="88" align="center" />
            <el-table-column label="状态" min-width="88" align="center">
              <template #default="{ row }">
                <el-tag :type="row.status === 1 ? 'success' : 'info'" effect="light" size="small">{{ row.status === 1 ? '启用' : '禁用' }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" min-width="160" align="center" fixed="right">
              <template #default="{ row }">
                <el-button type="primary" link size="small" @click="editCategory(row)">编辑</el-button>
                <el-button type="danger" link size="small" @click="deleteCategory(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
          <div class="pagination-wrapper">
            <el-pagination
              v-model:current-page="categoryCurrentPage"
              v-model:page-size="categoryPageSize"
              :total="categoryTotal"
              :page-sizes="[10, 20, 50, 100]"
              layout="total, sizes, prev, pager, next, jumper"
            />
          </div>
        </el-card>
      </el-tab-pane>

      <!-- 操作日志 -->
      <el-tab-pane label="操作日志" name="logs">
        <el-card>
          <el-table :data="paginatedLogs" empty-text="暂无数据" style="width: 100%" :max-height="500" table-layout="fixed">
            <el-table-column label="时间" width="176" show-overflow-tooltip>
              <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
            </el-table-column>
            <el-table-column label="用户名" prop="userName" min-width="120" show-overflow-tooltip />
            <el-table-column label="角色" min-width="128" align="center" show-overflow-tooltip>
              <template #default="{ row }">
                <el-tag :type="getRoleLogType(row.roleName)" effect="light" size="small">{{ row.roleName || '未知角色' }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="模块" prop="module" min-width="112" show-overflow-tooltip />
            <el-table-column label="操作类型" min-width="112" align="center" show-overflow-tooltip>
              <template #default="{ row }">
                <el-tag :type="getActionType(row.action)" effect="light" size="small">{{ row.action }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="内容" prop="content" min-width="220" show-overflow-tooltip />
            <el-table-column label="IP地址" min-width="148" show-overflow-tooltip>
              <template #default="{ row }"><span class="ip-address">{{ row.ip || '未知IP' }}</span></template>
            </el-table-column>
          </el-table>
          <div class="pagination-wrapper">
            <el-pagination
              v-model:current-page="logCurrentPage"
              v-model:page-size="logPageSize"
              :total="logTotal"
              :page-sizes="[10, 20, 50, 100]"
              layout="total, sizes, prev, pager, next, jumper"
            />
          </div>
        </el-card>
      </el-tab-pane>
    </el-tabs>

    <!-- 添加/编辑用户对话框 -->
    <el-dialog v-model="userDialogVisible" :title="editUserMode ? '编辑用户' : '添加用户'" width="500px" destroy-on-close>
      <el-form ref="userFormRef" :model="userForm" :rules="userRules" label-width="100px">
        <el-form-item label="登录名" prop="username"><el-input v-model="userForm.username" placeholder="用于登录，唯一" /></el-form-item>
        <el-form-item label="姓名" prop="name"><el-input v-model="userForm.name" placeholder="用户真实姓名" /></el-form-item>
        <el-form-item label="邮箱" prop="email"><el-input v-model="userForm.email" placeholder="输入邮箱" /></el-form-item>
        <el-form-item label="角色" prop="roleId">
          <el-select v-model="userForm.roleId" style="width: 100%">
            <el-option v-for="role in roleListAll" :key="role.id" :label="role.name" :value="role.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="密码" v-if="!editUserMode"><el-input v-model="userForm.password" type="password" placeholder="设置初始密码" /></el-form-item>
        <el-form-item label="新密码" v-if="editUserMode"><el-input v-model="userForm.password" type="password" placeholder="留空则不修改" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="userDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitUser">{{ editUserMode ? '保存修改' : '确认添加' }}</el-button>
      </template>
    </el-dialog>

    <!-- 添加/编辑角色对话框 -->
    <el-dialog v-model="roleDialogVisible" :title="editRoleMode ? '编辑角色' : '添加角色'" width="500px" destroy-on-close>
      <el-form ref="roleFormRef" :model="roleForm" :rules="roleRules" label-width="100px">
        <el-form-item label="角色名称" prop="name"><el-input v-model="roleForm.name" placeholder="输入角色名称" /></el-form-item>
        <el-form-item label="描述"><el-input v-model="roleForm.description" type="textarea" placeholder="输入角色描述" /></el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="roleForm.status">
            <el-radio :value="1">启用</el-radio>
            <el-radio :value="0">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="roleDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitRole">{{ editRoleMode ? '保存修改' : '确认添加' }}</el-button>
      </template>
    </el-dialog>

    <!-- 添加/编辑分类对话框 -->
    <el-dialog v-model="categoryDialogVisible" :title="editCategoryMode ? '编辑分类' : '添加分类'" width="500px" destroy-on-close>
      <el-form ref="categoryFormRef" :model="categoryForm" :rules="categoryRules" label-width="100px">
        <el-form-item label="分类名称" prop="name"><el-input v-model="categoryForm.name" placeholder="输入分类名称" /></el-form-item>
        <el-form-item label="分类编码"><el-input v-model="categoryForm.code" placeholder="输入分类编码（可选）" /></el-form-item>
        <el-form-item label="排序"><el-input-number v-model="categoryForm.sort" :min="0" style="width: 100%" /></el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="categoryForm.status">
            <el-radio :value="1">启用</el-radio>
            <el-radio :value="0">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="categoryDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitCategory">{{ editCategoryMode ? '保存修改' : '确认添加' }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useDataStore } from '@/stores/data'
import { useUserStore } from '@/stores/user'
import { userApi, roleApi, categoryApi, systemConfigApi, productApi } from '@/api'
import { formatTime } from '@/utils/time'

const router = useRouter()
const route = useRoute()

const dataStore = useDataStore()
const userStore = useUserStore()
const operationLogs = computed(() => dataStore.operationLogs || [])

// 用户管理（服务端分页）
const userTableList = ref([])
const userTotal = ref(0)
const userCurrentPage = ref(1)
const userPageSize = ref(10)

const loadUsersPage = async () => {
  try {
    const res = await userApi.list({
      page: userCurrentPage.value,
      size: userPageSize.value
    })
    const list = res?.list || []
    const total = Number(res?.total) || 0
    if (list.length === 0 && userCurrentPage.value > 1 && total > 0) {
      userCurrentPage.value -= 1
      return loadUsersPage()
    }
    userTableList.value = list
    userTotal.value = total
  } catch (error) {
    console.error('加载用户列表失败:', error)
    userTableList.value = []
    userTotal.value = 0
  }
}

// 判断当前登录用户是否是超级管理员
const isCurrentUserSuperAdmin = computed(() => userStore.userInfo?.roleId === 1)

const SUPER_ADMIN_TABS = ['users', 'roles', 'system', 'categories', 'logs']
const NORMAL_USER_TABS = ['system', 'logs']

const resolveTabFromRoute = () => {
  const tab = String(route.query.tab || '')
  const validTabs = isCurrentUserSuperAdmin.value ? SUPER_ADMIN_TABS : NORMAL_USER_TABS
  if (validTabs.includes(tab)) return tab
  return isCurrentUserSuperAdmin.value ? 'users' : 'system'
}

// 支持通过 /settings?tab=roles 定位页签
const activeTab = ref(resolveTabFromRoute())
watch(
  () => route.query.tab,
  () => {
    activeTab.value = resolveTabFromRoute()
  }
)
const userDialogVisible = ref(false)
const editUserMode = ref(false)
const currentUser = ref(null)
const userFormRef = ref()

// 角色管理：表格分页 + 全量列表供用户表单/显示角色名
const roleTableList = ref([])
const roleTotal = ref(0)
const roleListAll = ref([])
const roleCurrentPage = ref(1)
const rolePageSize = ref(10)

const loadRolesAllForForm = async () => {
  try {
    roleListAll.value = await roleApi.list() || []
  } catch (error) {
    console.error('加载角色列表失败:', error)
    roleListAll.value = []
  }
}

const loadRolePage = async () => {
  try {
    const res = await roleApi.listPage({
      page: roleCurrentPage.value,
      size: rolePageSize.value
    })
    const list = res?.list || []
    const total = Number(res?.total) || 0
    if (list.length === 0 && roleCurrentPage.value > 1 && total > 0) {
      roleCurrentPage.value -= 1
      return loadRolePage()
    }
    roleTableList.value = list
    roleTotal.value = total
  } catch (error) {
    console.error('加载角色分页失败:', error)
    roleTableList.value = []
    roleTotal.value = 0
  }
}

// 打开权限管理页面
const openPermissionDialog = (role) => {
  router.push(`/settings/permission/${role.id}`)
}

// 角色对话框
const roleDialogVisible = ref(false)
const editRoleMode = ref(false)
const currentRole = ref(null)
const roleFormRef = ref()
const roleForm = ref({ name: '', description: '', status: 1 })
const roleRules = {
  name: [{ required: true, message: '请输入角色名称', trigger: 'blur' }]
}

// 商品分类（服务端分页）
const categoryTableList = ref([])
const categoryTotal = ref(0)
const categoryCurrentPage = ref(1)
const categoryPageSize = ref(10)
const categoryDialogVisible = ref(false)
const editCategoryMode = ref(false)
const currentCategory = ref(null)
const categoryFormRef = ref()
const categoryForm = ref({ name: '', code: '', sort: 0, status: 1 })
const categoryRules = {
  name: [{ required: true, message: '请输入分类名称', trigger: 'blur' }]
}

const loadCategoryPage = async () => {
  try {
    const res = await categoryApi.listPage({
      page: categoryCurrentPage.value,
      size: categoryPageSize.value
    })
    const list = res?.list || []
    const total = Number(res?.total) || 0
    if (list.length === 0 && categoryCurrentPage.value > 1 && total > 0) {
      categoryCurrentPage.value -= 1
      return loadCategoryPage()
    }
    categoryTableList.value = list
    categoryTotal.value = total
  } catch (error) {
    console.error('加载分类分页失败:', error)
    categoryTableList.value = []
    categoryTotal.value = 0
  }
}

// 操作日志分页
const pageLoading = ref(true)
const logCurrentPage = ref(1)
const logPageSize = ref(10)
const logTotal = ref(0)
const paginatedLogs = computed(() => operationLogs.value)
const canViewLogs = computed(() => userStore.userInfo?.roleId === 1 || userStore.hasPermission('settings:user'))

const loadOperationLogsPage = async () => {
  if (!canViewLogs.value) return
  const res = await dataStore.loadOperationLogs({
    page: logCurrentPage.value,
    size: logPageSize.value
  })
  logTotal.value = Number(res?.total) || 0
}

// 根据角色ID获取角色名称
const getUserRoleName = (user) => {
  const role = roleListAll.value.find(r => r.id === user.roleId)
  return role ? role.name : '普通用户'
}

const isSuperAdmin = (user) => {
  return user.roleId === 1
}

// 打开角色添加对话框
const openRoleDialog = () => {
  editRoleMode.value = false
  currentRole.value = null
  roleForm.value = { name: '', description: '', status: 1 }
  roleDialogVisible.value = true
}

// 打开角色编辑对话框
const editRole = (role) => {
  editRoleMode.value = true
  currentRole.value = role
  roleForm.value = { name: role.name, description: role.description, status: role.status }
  roleDialogVisible.value = true
}

// 提交角色
const submitRole = async () => {
  await roleFormRef.value?.validate(async (valid) => {
    if (valid) {
      try {
        if (editRoleMode.value) {
          await roleApi.update(currentRole.value.id, roleForm.value)
          ElMessage.success('角色信息已更新')
        } else {
          await roleApi.create(roleForm.value)
          ElMessage.success('角色添加成功')
        }
        roleDialogVisible.value = false
        await Promise.all([loadRolePage(), loadRolesAllForForm()])
      } catch (error) {
        ElMessage.error(error.message || '操作失败')
      }
    }
  })
}

// 删除角色
const deleteRole = async (role) => {
  if (role.id === 1) {
    ElMessage.warning('超级管理员角色不能删除')
    return
  }

  try {
    await ElMessageBox.confirm(`确定要删除角色 "${role.name}" 吗？删除后该角色下的用户将失去对应权限。`, '删除确认', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })

    await roleApi.delete(role.id)
    ElMessage.success('角色已删除')
    await Promise.all([loadRolePage(), loadRolesAllForForm()])
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '删除失败')
    }
  }
}

// 打开添加分类对话框
const openCategoryDialog = () => {
  editCategoryMode.value = false
  currentCategory.value = null
  categoryForm.value = { name: '', code: '', sort: 0, status: 1 }
  categoryDialogVisible.value = true
}

// 打开编辑分类对话框
const editCategory = (category) => {
  editCategoryMode.value = true
  currentCategory.value = category
  categoryForm.value = { name: category.name, code: category.code || '', sort: category.sort || 0, status: category.status }
  categoryDialogVisible.value = true
}

// 提交分类
const submitCategory = async () => {
  await categoryFormRef.value?.validate(async (valid) => {
    if (valid) {
      try {
        if (editCategoryMode.value) {
          await categoryApi.update(currentCategory.value.id, categoryForm.value)
          ElMessage.success('分类信息已更新')
        } else {
          await categoryApi.create(categoryForm.value)
          ElMessage.success('分类添加成功')
        }
        categoryDialogVisible.value = false
        await loadCategoryPage()
        await dataStore.loadCategories()
      } catch (error) {
        ElMessage.error(error.message || '操作失败')
      }
    }
  })
}

// 删除分类
const deleteCategory = async (category) => {
  try {
    // 删除前按分类查 1 条，避免初始化全量拉商品列表
    const productCheck = await productApi.list({
      page: 1,
      size: 1,
      categoryName: category.name
    })
    const productsUsingCategory = Number(productCheck?.total) || (productCheck?.list?.length || 0)
    if (productsUsingCategory > 0) {
      ElMessage.warning(`该分类已被 ${productsUsingCategory} 个商品使用，无法删除`)
      return
    }

    await ElMessageBox.confirm(`确定要删除分类 "${category.name}" 吗？`, '删除确认', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })

    await categoryApi.delete(category.id)
    ElMessage.success('分类已删除')
    await loadCategoryPage()
    await dataStore.loadCategories()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '删除失败')
    }
  }
}

const defaultSystemConfig = {
  companyName: '深圳华创科技有限公司',
  phone: '0755-88888888',
  address: '深圳市南山区科技园',
  stockWarning: '开启',
  staleDays: 90
}
const systemConfig = ref({ ...defaultSystemConfig })

const userForm = ref({ username: '', name: '', email: '', roleId: 3, password: '' })
const userRules = {
  username: [{ required: true, message: '请输入登录名', trigger: 'blur' }],
  name: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  email: [{ required: true, message: '请输入邮箱', trigger: 'blur' }],
  roleId: [{ required: true, message: '请选择角色', trigger: 'change' }]
}

const getRoleType = (roleId) => {
  // 超级管理员显示danger, roleId=2显示warning, 其他显示info
  if (roleId === 1) return 'danger'
  if (roleId === 2) return 'warning'
  return 'info'
}

const getRoleLogType = (roleName) => {
  // 根据角色名称显示不同颜色
  if (roleName === '超级管理员') return 'danger'
  if (roleName === '管理员') return 'warning'
  return 'info'
}

const getActionType = (action) => ({ '登录': '', '创建': 'success', '更新': 'warning', '配置': '', '导出': '' }[action] || 'info')

const openUserDialog = () => {
  editUserMode.value = false
  currentUser.value = null
  userForm.value = { username: '', name: '', email: '', roleId: 3, password: '' }
  userDialogVisible.value = true
}
const editUser = (u) => {
  editUserMode.value = true
  currentUser.value = u
  userForm.value = {
    username: u.username ?? '',
    name: u.name ?? '',
    email: u.email ?? '',
    roleId: u.roleId,
    password: ''
  }
  userDialogVisible.value = true
}
const submitUser = async () => {
  await userFormRef.value?.validate(async (valid) => {
    if (valid) {
      try {
        if (editUserMode.value) {
          // 更新用户信息
          await userApi.update(currentUser.value.id, {
            username: userForm.value.username,
            name: userForm.value.name,
            email: userForm.value.email,
            roleId: userForm.value.roleId,
            password: userForm.value.password || undefined
          })
          ElMessage.success('用户信息已更新')

          // 如果修改的是当前登录用户，同步更新 userStore
          if (userStore.userInfo?.id === currentUser.value.id) {
            userStore.updateUser({
              username: userForm.value.username,
              name: userForm.value.name,
              email: userForm.value.email,
              role: getUserRoleName({ roleId: userForm.value.roleId })
            })
          }
        } else {
          // 添加新用户
          await userApi.create({
            username: userForm.value.username,
            name: userForm.value.name,
            email: userForm.value.email,
            roleId: userForm.value.roleId,
            password: userForm.value.password
          })
          ElMessage.success('用户添加成功')
        }
        userDialogVisible.value = false
        await loadUsersPage()
        await dataStore.loadUsers()
      } catch (error) {
        ElMessage.error(error.message || '操作失败')
      }
    }
  })
}
const loadSystemConfig = async () => {
  try {
    const config = await systemConfigApi.get()
    systemConfig.value = { ...defaultSystemConfig, ...(config || {}) }
  } catch (error) {
    ElMessage.error(error.message || '加载系统配置失败')
  }
}

const saveConfig = async () => {
  try {
    await systemConfigApi.update(systemConfig.value)
    ElMessage.success('系统配置已保存')
    await loadSystemConfig()
  } catch (error) {
    ElMessage.error(error.message || '保存系统配置失败')
  }
}

// 删除用户（超级管理员不能删除）
const deleteUser = async (user) => {
  if (isSuperAdmin(user)) {
    ElMessage.warning('超级管理员不能删除')
    return
  }

  try {
    await ElMessageBox.confirm(`确定要删除用户 "${user.name}" 吗？`, '删除确认', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })

    await userApi.delete(user.id)
    ElMessage.success('用户已删除')
    await loadUsersPage()
    await dataStore.loadUsers()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '删除失败')
    }
  }
}

// 按角色与权限按需加载：超级管理员进设置页拉用户/角色/分类；操作日志按分页参数请求
onMounted(async () => {
  pageLoading.value = true
  try {
    const tasks = []
    tasks.push(loadSystemConfig())
    if (userStore.userInfo?.roleId === 1) {
      tasks.push(
        dataStore.loadUsers(),
        loadOperationLogsPage(),
        loadUsersPage(),
        loadRolesAllForForm(),
        loadRolePage(),
        loadCategoryPage()
      )
    } else if (userStore.hasPermission('settings:user')) {
      tasks.push(loadOperationLogsPage())
    }
    await Promise.all(tasks)
  } finally {
    pageLoading.value = false
  }
})

watch([logCurrentPage, logPageSize], () => {
  loadOperationLogsPage()
})

watch([userCurrentPage, userPageSize], () => {
  if (isCurrentUserSuperAdmin.value) loadUsersPage()
})

watch([roleCurrentPage, rolePageSize], () => {
  if (isCurrentUserSuperAdmin.value) loadRolePage()
})

watch([categoryCurrentPage, categoryPageSize], () => {
  if (isCurrentUserSuperAdmin.value) loadCategoryPage()
})
</script>

<style lang="scss" scoped>
.settings-page {
  min-height: 360px;
  .page-tabs { :deep(.el-tabs__header) { margin-bottom: 16px; } }
  .card-header { display: flex; justify-content: flex-end; align-items: center; }
  .pagination-wrapper { display: flex; justify-content: flex-end; padding-top: 16px; }
  .config-sections { display: grid; grid-template-columns: repeat(2, 1fr); gap: 20px; margin-bottom: 20px; }
  .ip-address { font-family: monospace; color: #606266; }
}
</style>