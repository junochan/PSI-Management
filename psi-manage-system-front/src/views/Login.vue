<template>
  <div class="login-container">
    <div class="login-left">
      <div class="login-left-content">
        <h1>智链进销存</h1>
        <p>Smart IMS - 智能化企业管理系统</p>
        <div class="features">
          <div class="feature-item">
            <el-icon><Box /></el-icon>
            <span>商品管理</span>
          </div>
          <div class="feature-item">
            <el-icon><ShoppingCart /></el-icon>
            <span>采购管理</span>
          </div>
          <div class="feature-item">
            <el-icon><Money /></el-icon>
            <span>销售管理</span>
          </div>
          <div class="feature-item">
            <el-icon><DataAnalysis /></el-icon>
            <span>报表分析</span>
          </div>
        </div>
      </div>
    </div>
    <div class="login-right">
      <div class="login-form-container">
        <h2>欢迎登录</h2>
        <p>请输入您的用户名和密码</p>
        <el-form ref="loginFormRef" :model="loginForm" :rules="loginRules" @submit.prevent="handleLogin">
          <el-form-item prop="username">
            <el-input
              v-model="loginForm.username"
              placeholder="请输入用户名"
              size="large"
              prefix-icon="User"
            />
          </el-form-item>
          <el-form-item prop="password">
            <el-input
              v-model="loginForm.password"
              type="password"
              placeholder="请输入密码"
              size="large"
              prefix-icon="Lock"
              show-password
            />
          </el-form-item>
          <el-form-item>
            <el-checkbox v-model="loginForm.remember">记住密码</el-checkbox>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" size="large" :loading="loading" @click="handleLogin" class="login-btn">
              登 录
            </el-button>
          </el-form-item>
        </el-form>
              </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { useNavigationStore } from '@/stores/navigation'
import { authApi } from '@/api'
import { registerLayoutRoutes, clearLayoutDynamicRoutes, resolveSafeHomePath } from '@/router/dynamic-routes'
import router from '@/router'

const vueRouter = useRouter()
const userStore = useUserStore()
const navigationStore = useNavigationStore()

const loginFormRef = ref()
const loading = ref(false)

const loginForm = reactive({
  username: '',
  password: '',
  remember: false
})

const loginRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码长度不能少于6位', trigger: 'blur' }
  ]
}

const handleLogin = async () => {
  if (!loginFormRef.value) return

  await loginFormRef.value.validate(async (valid) => {
    if (valid) {
      loading.value = true

      try {
        // 调用后端登录API
        const res = await authApi.login({
          username: loginForm.username,
          password: loginForm.password
        })

        // 更新用户状态（传递真实的JWT token）
        userStore.login({
          id: res.userId,
          username: res.username,
          name: res.name,
          roleId: res.roleId,
          role: res.roleName,
          email: res.email,
          phone: res.phone,
          permissions: res.permissions || []
        }, res.token)

        clearLayoutDynamicRoutes(router)
        navigationStore.reset()
        await navigationStore.fetchNavigation()
        registerLayoutRoutes(router, navigationStore.routes)

        ElMessage.success('登录成功！')

        if (loginForm.remember) {
          localStorage.setItem('rememberedUser', loginForm.username)
        }

        const safe = resolveSafeHomePath(navigationStore, router)
        if (!safe) {
          ElMessage.error('未找到可访问页面，请检查角色权限配置')
          userStore.logout()
          navigationStore.reset()
          clearLayoutDynamicRoutes(router)
          return
        }
        vueRouter.push(safe)
      } catch (error) {
        ElMessage.error(error.message || '登录失败，请检查用户名和密码')
      } finally {
        loading.value = false
      }
    }
  })
}

// 初始化时检查记住的用户名
const rememberedUser = localStorage.getItem('rememberedUser')
if (rememberedUser) {
  loginForm.username = rememberedUser
  loginForm.remember = true
}
</script>

<style lang="scss" scoped>
.login-container {
  display: flex;
  width: 100%;
  height: 100vh;

  .login-left {
    width: 50%;
    background: linear-gradient(135deg, #1A1A2E 0%, #16213E 100%);
    display: flex;
    align-items: center;
    justify-content: center;
    position: relative;

    &::before {
      content: '';
      position: absolute;
      top: 0;
      left: 0;
      right: 0;
      bottom: 0;
      background: url('data:image/svg+xml,<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100"><circle cx="50" cy="50" r="40" fill="none" stroke="rgba(233,69,96,0.1)" stroke-width="0.5"/><circle cx="30" cy="30" r="20" fill="none" stroke="rgba(233,69,96,0.08)" stroke-width="0.5"/><circle cx="70" cy="70" r="25" fill="none" stroke="rgba(233,69,96,0.06)" stroke-width="0.5"/></svg>');
      background-size: 200px;
      opacity: 0.5;
    }

    .login-left-content {
      text-align: center;
      color: #fff;
      position: relative;
      z-index: 1;

      h1 {
        font-family: 'Playfair Display', serif;
        font-size: 48px;
        font-weight: 700;
        margin-bottom: 16px;
        background: linear-gradient(135deg, #fff 0%, #ff6b6b 100%);
        -webkit-background-clip: text;
        -webkit-text-fill-color: transparent;
        background-clip: text;
      }

      p {
        font-size: 16px;
        color: rgba(255, 255, 255, 0.6);
        letter-spacing: 4px;
        margin-bottom: 40px;
      }

      .features {
        display: flex;
        justify-content: center;
        gap: 32px;

        .feature-item {
          display: flex;
          align-items: center;
          gap: 8px;
          padding: 12px 20px;
          background: rgba(255, 255, 255, 0.05);
          border-radius: 8px;
          color: rgba(255, 255, 255, 0.7);
          font-size: 14px;

          .el-icon {
            font-size: 20px;
            color: #E94560;
          }
        }
      }
    }
  }

  .login-right {
    width: 50%;
    display: flex;
    align-items: center;
    justify-content: center;
    background: #F5F7FA;

    .login-form-container {
      width: 380px;
      padding: 40px;
      background: #fff;
      border-radius: 16px;
      box-shadow: 0 8px 32px rgba(0, 0, 0, 0.1);

      h2 {
        font-size: 24px;
        font-weight: 600;
        color: #303133;
        margin-bottom: 8px;
      }

      p {
        font-size: 14px;
        color: #909399;
        margin-bottom: 32px;
      }

      .login-btn {
        width: 100%;
        height: 48px;
        font-size: 16px;
      }

      .login-footer {
        margin-top: 16px;
        text-align: center;

        p {
          font-size: 12px;
          color: #909399;
        }
      }
    }
  }
}

@media (max-width: 1024px) {
  .login-container {
    .login-left {
      width: 100%;
    }
    .login-right {
      position: absolute;
      width: 100%;
      background: transparent;

      .login-form-container {
        background: rgba(26, 26, 46, 0.9);
        h2, p, .login-footer p {
          color: #fff;
        }
      }
    }
  }
}
</style>