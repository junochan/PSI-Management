<template>
  <div class="main-layout">
    <!-- 侧边栏 -->
    <aside class="sidebar" :class="{ collapsed: sidebarCollapsed }">
      <div class="sidebar-toggle" @click="sidebarCollapsed = !sidebarCollapsed">
        <el-icon>
          <component :is="sidebarCollapsed ? 'Expand' : 'Fold'" />
        </el-icon>
      </div>
      <div class="logo">
        <h1 v-if="!sidebarCollapsed">智链</h1>
        <h1 v-else class="logo-mini">智</h1>
        <span v-if="!sidebarCollapsed">Smart IMS</span>
      </div>

      <nav class="nav-menu">
        <NavMenuTree :items="navMenus" :collapsed="sidebarCollapsed" />
      </nav>
    </aside>

    <!-- 主内容区 -->
    <main class="main-content" :class="{ expanded: sidebarCollapsed }">
      <!-- 顶栏 -->
      <header class="header">
        <div class="header-left">
          <h2>{{ pageTitle }}</h2>
          <p>{{ currentDate }}</p>
        </div>
        <div class="header-right">
          <el-dropdown trigger="click" @command="handleUserCommand">
            <div class="user-info">
              <el-avatar class="user-avatar" :src="currentUserInfo.avatar ? resolveMediaUrl(currentUserInfo.avatar) : undefined">
                {{ currentUserInfo.name?.charAt(0) || '超' }}
              </el-avatar>
              <div class="user-details">
                <h4>{{ currentUserInfo.name || '超级管理员' }}</h4>
                <p>{{ currentUserInfo.role || '超级管理员' }}</p>
              </div>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile">个人设置</el-dropdown-item>
                <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </header>

      <!-- 页面内容 -->
      <div class="page-content">
        <router-view />
      </div>
    </main>

    <!-- 个人设置弹框 -->
    <el-dialog v-model="profileDialogVisible" title="个人设置" width="500px" destroy-on-close>
      <el-tabs v-model="profileTab">
        <!-- 基本信息 -->
        <el-tab-pane label="基本信息" name="info">
          <div class="profile-avatar-section">
            <el-upload
              class="avatar-uploader"
              action="#"
              :auto-upload="false"
              :show-file-list="false"
              :disabled="avatarUploading"
              :on-change="handleAvatarChange"
              accept="image/*"
            >
              <img v-if="profileForm.avatar" :src="resolveMediaUrl(profileForm.avatar)" class="avatar-image" />
              <el-avatar v-else class="avatar-placeholder" :size="80">{{ currentUserInfo.name?.charAt(0) || '超' }}</el-avatar>
              <div class="avatar-upload-tip">
                <span>📷 更换头像</span>
              </div>
            </el-upload>
          </div>
          <el-form ref="profileFormRef" :model="profileForm" :rules="profileRules" label-width="80px">
            <el-form-item label="姓名">
              <el-input :value="currentUserInfo.name" disabled />
            </el-form-item>
            <el-form-item label="登录名">
              <el-input :value="currentUserInfo.username" disabled />
            </el-form-item>
            <el-form-item label="邮箱" prop="email">
              <el-input v-model="profileForm.email" placeholder="输入邮箱地址" />
            </el-form-item>
            <el-form-item label="手机号" prop="phone">
              <el-input v-model="profileForm.phone" placeholder="输入手机号码" />
            </el-form-item>
            <el-form-item label="职位">
              <el-input :value="currentUserInfo.role" disabled />
            </el-form-item>
          </el-form>
        </el-tab-pane>

        <!-- 修改密码 -->
        <el-tab-pane label="修改密码" name="password">
          <el-form ref="passwordFormRef" :model="passwordForm" :rules="passwordRules" label-width="100px">
            <el-form-item label="当前密码" prop="currentPassword">
              <el-input v-model="passwordForm.currentPassword" type="password" placeholder="输入当前密码" show-password />
            </el-form-item>
            <el-form-item label="新密码" prop="newPassword">
              <el-input v-model="passwordForm.newPassword" type="password" placeholder="输入新密码" show-password />
            </el-form-item>
            <el-form-item label="确认新密码" prop="confirmPassword">
              <el-input v-model="passwordForm.confirmPassword" type="password" placeholder="再次输入新密码" show-password />
            </el-form-item>
            <el-alert type="info" :closable="false" show-icon style="margin-top: 16px">
              <template #title>密码要求</template>
              密码长度至少6位，建议包含字母和数字
            </el-alert>
          </el-form>
        </el-tab-pane>
      </el-tabs>
      <template #footer>
        <el-button @click="profileDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveProfile">保存设置</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { useNavigationStore } from '@/stores/navigation'
import { useDataStore } from '@/stores/data'
import { userApi } from '@/api'
import NavMenuTree from '@/components/NavMenuTree.vue'

const resolveMediaUrl = (url) => {
  if (!url || typeof url !== 'string') return ''
  if (url.startsWith('http://') || url.startsWith('https://') || url.startsWith('data:')) return url
  if (url.startsWith('/')) return url
  return `/${url}`
}

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const navigationStore = useNavigationStore()
const dataStore = useDataStore()

const navMenus = computed(() => navigationStore.menus || [])

// 仅在有「用户管理」权限时拉全量用户（供顶栏与设置页）；避免仅有仪表盘等权限时请求 /users/all 被 403
onMounted(async () => {
  if (userStore.hasPermission('settings:user')) {
    await dataStore.loadUsers()
  }
})

// 角色ID映射
const roleMap = {
  1: '超级管理员',
  2: '仓管主管',
  3: '销售专员',
  4: '采购专员',
  5: '财务专员'
}

// 优先用用户管理接口拉到的列表；无权限时仅用登录态 userInfo + 角色映射
const currentUserInfo = computed(() => {
  const base = userStore.userInfo
  const userId = base?.id
  if (!userId) {
    return base || { name: '用户', role: '' }
  }
  const user = dataStore.users.find((u) => u.id === userId)
  if (user) {
    return {
      ...user,
      role: roleMap[user.roleId] || base?.role || '普通用户'
    }
  }
  return {
    ...base,
    role: base?.role || roleMap[base.roleId] || '普通用户'
  }
})

const sidebarCollapsed = ref(false)
const profileDialogVisible = ref(false)
const profileTab = ref('info')
const profileFormRef = ref()
const passwordFormRef = ref()

// 个人设置表单
const profileForm = ref({
  avatar: '',
  email: 'admin@inventory.com',
  phone: '138****8888'
})

const profileRules = {
  email: [
    { type: 'email', message: '请输入正确的邮箱格式', trigger: 'blur' }
  ],
  phone: [
    { pattern: /^1[3-9]\d{9}$|^1[3-9]\d{9}\*{4}\d{4}$/, message: '请输入正确的手机号', trigger: 'blur' }
  ]
}

// 修改密码表单
const passwordForm = ref({
  currentPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const passwordRules = {
  currentPassword: [
    { required: true, message: '请输入当前密码', trigger: 'blur' }
  ],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, message: '密码长度至少6位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认新密码', trigger: 'blur' },
    {
      validator: (rule, value, callback) => {
        if (value !== passwordForm.value.newPassword) {
          callback(new Error('两次输入的密码不一致'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ]
}

const pageTitle = computed(() => route.meta.title || '仪表盘')
const currentDate = computed(() => {
  const now = new Date()
  const weekDays = ['星期日', '星期一', '星期二', '星期三', '星期四', '星期五', '星期六']
  return `${now.getFullYear()}年${now.getMonth() + 1}月${now.getDate()}日 ${weekDays[now.getDay()]}`
})

const handleUserCommand = (command) => {
  if (command === 'logout') {
    ElMessageBox.confirm('确定要退出登录吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    }).then(() => {
      userStore.logout()
      router.push('/login')
      ElMessage.success('已退出登录')
    })
  } else if (command === 'profile') {
    // 初始化表单数据
    profileForm.value = {
      avatar: currentUserInfo.value.avatar || '',
      email: currentUserInfo.value.email || '',
      phone: currentUserInfo.value.phone || ''
    }
    passwordForm.value = {
      currentPassword: '',
      newPassword: '',
      confirmPassword: ''
    }
    profileTab.value = 'info'
    profileDialogVisible.value = true
  }
}

// 选择头像后上传到服务器，数据库只保存返回的访问地址
const avatarUploading = ref(false)
const handleAvatarChange = async (file) => {
  const raw = file.raw
  if (!raw) return
  avatarUploading.value = true
  try {
    const formData = new FormData()
    formData.append('file', raw)
    const res = await userApi.uploadAvatar(formData)
    profileForm.value.avatar = res.url
    ElMessage.success('头像已上传')
  } catch (e) {
    const msg = typeof e?.message === 'string' ? e.message : ''
    if (msg) ElMessage.error(msg)
  } finally {
    avatarUploading.value = false
  }
}

// 保存个人设置
const saveProfile = async () => {
  if (profileTab.value === 'info') {
    try {
      await profileFormRef.value?.validate()
      const uid = userStore.userInfo?.id
      if (!uid) {
        ElMessage.error('未登录或用户信息缺失，无法保存')
        return
      }
      const base = currentUserInfo.value
      const username = base.username ?? userStore.userInfo?.username
      const name = base.name ?? userStore.userInfo?.name
      const roleId = base.roleId ?? userStore.userInfo?.roleId
      if (!username || !name || roleId == null) {
        ElMessage.error('用户信息不完整，请重新登录后再试')
        return
      }
      await userApi.update(uid, {
        username,
        name,
        email: profileForm.value.email,
        phone: profileForm.value.phone || undefined,
        avatar: profileForm.value.avatar || undefined,
        roleId
      })
      userStore.updateUser({
        email: profileForm.value.email,
        phone: profileForm.value.phone,
        avatar: profileForm.value.avatar || ''
      })
      if (userStore.hasPermission('settings:user')) {
        await dataStore.loadUsers()
      }
      ElMessage.success('个人信息已更新')
      profileDialogVisible.value = false
    } catch (e) {
      const msg = typeof e?.message === 'string' ? e.message : ''
      if (msg) ElMessage.error(msg)
    }
  } else if (profileTab.value === 'password') {
    await passwordFormRef.value?.validate((valid) => {
      if (valid) {
        // 模拟密码修改验证
        if (passwordForm.value.currentPassword !== 'admin123') {
          ElMessage.error('当前密码错误')
          return
        }
        ElMessage.success('密码修改成功，请重新登录')
        profileDialogVisible.value = false
        // 模拟重新登录
        setTimeout(() => {
          userStore.logout()
          router.push('/login')
        }, 1500)
      }
    })
  }
}
</script>

<style lang="scss" scoped>
.main-layout {
  display: flex;
  width: 100%;
  min-height: 100vh;

  .sidebar {
    width: 260px;
    background: linear-gradient(180deg, #16213E 0%, #1A1A2E 100%);
    border-right: 1px solid rgba(233, 69, 96, 0.2);
    display: flex;
    flex-direction: column;
    position: fixed;
    left: 0;
    top: 0;
    height: 100vh;
    z-index: 100;
    transition: width 0.3s ease;

    &.collapsed {
      width: 80px;

      .nav-menu {
        :deep(.nav-item) {
          justify-content: center;
          padding: 14px;
        }
      }
    }

    .sidebar-toggle {
      position: absolute;
      right: -16px;
      top: 24px;
      width: 32px;
      height: 32px;
      background: #E94560;
      border-radius: 50%;
      display: flex;
      align-items: center;
      justify-content: center;
      cursor: pointer;
      color: #fff;
      font-size: 16px;
      z-index: 10;
      transition: all 0.3s ease;

      &:hover {
        transform: scale(1.1);
        background: #f0a500;
      }
    }

    .logo {
      padding: 24px;
      border-bottom: 1px solid rgba(255, 255, 255, 0.08);
      min-height: 80px;

      h1 {
        font-family: 'Playfair Display', serif;
        font-size: 28px;
        font-weight: 700;
        margin-bottom: 8px;
        background: linear-gradient(135deg, #fff 0%, #ff6b6b 100%);
        -webkit-background-clip: text;
        -webkit-text-fill-color: transparent;
        background-clip: text;
      }

      .logo-mini {
        font-size: 24px;
        text-align: center;
      }

      span {
        font-size: 12px;
        color: rgba(255, 255, 255, 0.5);
        letter-spacing: 3px;
        text-transform: uppercase;
      }
    }

    .nav-menu {
      flex: 1;
      padding: 16px 12px;

      /* .nav-item 在子组件 NavMenuTree 内，必须用 :deep 穿透 scoped */
      :deep(.nav-item) {
        display: flex;
        align-items: center;
        gap: 14px;
        padding: 14px 16px;
        margin: 4px 0;
        border-radius: 12px;
        color: rgba(255, 255, 255, 0.7);
        font-size: 14px;
        font-weight: 500;
        cursor: pointer;
        transition: all 0.3s ease;
        position: relative;
        overflow: hidden;
        text-decoration: none;

        &:hover {
          background: rgba(233, 69, 96, 0.1);
          color: #fff;
        }

        &.active {
          background: rgba(233, 69, 96, 0.15);
          color: #fff;
        }

        .nav-icon {
          font-size: 20px;
          opacity: 0.8;
        }

        .nav-badge {
          margin-left: auto;
          background: #E94560;
          color: #fff;
          font-size: 11px;
          padding: 2px 8px;
          border-radius: 10px;
          font-weight: 600;
        }
      }

      :deep(.nav-group) {
        margin-bottom: 4px;
      }

      :deep(.nav-group-title) {
        font-size: 11px;
        color: rgba(255, 255, 255, 0.35);
        padding: 8px 12px 4px;
        letter-spacing: 0.5px;
      }
    }
  }

  .main-content {
    margin-left: 260px;
    flex: 1;
    background: #F5F7FA;
    display: flex;
    flex-direction: column;
    transition: margin-left 0.3s ease;

    &.expanded {
      margin-left: 80px;
    }

    .header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 20px 32px;
      background: #fff;
      border-bottom: 1px solid #E4E7ED;
      position: sticky;
      top: 0;
      z-index: 50;

      .header-left {
        h2 {
          font-size: 24px;
          font-weight: 700;
          color: #303133;
          margin-bottom: 8px;
        }

        p {
          font-size: 14px;
          color: #909399;
        }
      }

      .header-right {
        display: flex;
        align-items: center;
        gap: 16px;

        .message-badge {
          .el-button {
            background: transparent;
            border: 1px solid #DCDFE6;

            &:hover {
              background: #F5F7FA;
            }
          }
        }

        .user-info {
          display: flex;
          align-items: center;
          gap: 12px;
          padding: 8px 16px;
          background: #F5F7FA;
          border-radius: 12px;
          cursor: pointer;
          transition: all 0.3s ease;

          &:hover {
            background: #E4E7ED;
          }

          .user-avatar {
            background: linear-gradient(135deg, #E94560 0%, #f0a500 100%);
            color: #fff;
            font-weight: 700;
          }

          .user-details {
            h4 {
              font-size: 14px;
              font-weight: 600;
              color: #303133;
              margin: 0;
            }

            p {
              font-size: 12px;
              margin: 5px 0 0;
              line-height: 1.2;
              color: rgba(144, 147, 153, 0.58);
            }
          }
        }
      }
    }

    .page-content {
      flex: 1;
      padding: 24px 32px;
      overflow-y: auto;
    }
  }
}

// 个人设置弹框样式
.profile-avatar-section {
  text-align: center;
  margin-bottom: 24px;

  .avatar-uploader {
    display: inline-block;
    cursor: pointer;

    :deep(.el-upload) {
      position: relative;
      border-radius: 50%;
      overflow: hidden;

      &:hover .avatar-upload-tip {
        opacity: 1;
      }
    }
  }

  .avatar-image {
    width: 80px;
    height: 80px;
    border-radius: 50%;
    object-fit: cover;
  }

  .avatar-placeholder {
    background: linear-gradient(135deg, #E94560 0%, #f0a500 100%);
    color: #fff;
    font-size: 32px;
    font-weight: 700;
  }

  .avatar-upload-tip {
    position: absolute;
    bottom: 0;
    left: 0;
    right: 0;
    background: rgba(0, 0, 0, 0.5);
    color: #fff;
    font-size: 12px;
    padding: 4px 0;
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 4px;
    opacity: 0;
    transition: opacity 0.3s;
  }
}
</style>