<template>
  <div class="sso-entry" v-loading="ssoLoading" element-loading-text="正在登录…"></div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { authApi } from '@/api'
import { useUserStore } from '@/stores/user'
import { useNavigationStore } from '@/stores/navigation'
import { clearLayoutDynamicRoutes, registerLayoutRoutes, resolveSafeHomePath } from '@/router/dynamic-routes'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const navigationStore = useNavigationStore()
const ssoLoading = ref(true)

onMounted(async () => {
  try {
    const raw = route.query.key ?? route.query.token
    const key = raw != null ? String(raw).trim() : ''
    if (!key) {
      ElMessage.error('缺少凭证')
      await router.replace('/login')
      return
    }

    const res = await authApi.ssoLogin({ key })
    userStore.login(
      {
        id: res.userId,
        username: res.username,
        name: res.name,
        roleId: res.roleId,
        role: res.roleName,
        email: res.email,
        phone: res.phone,
        permissions: res.permissions || []
      },
      res.token
    )
    clearLayoutDynamicRoutes(router)
    navigationStore.reset()
    await navigationStore.fetchNavigation()
    registerLayoutRoutes(router, navigationStore.routes)

    const safe = resolveSafeHomePath(navigationStore, router)
    if (!safe) {
      ElMessage.error('未找到可访问页面，请检查角色权限配置')
      userStore.logout()
      navigationStore.reset()
      clearLayoutDynamicRoutes(router)
      await router.replace('/login')
      return
    }
    await router.replace(safe)
  } catch (e) {
    ElMessage.error(e?.message || '中转登录失败')
    await router.replace('/login')
  } finally {
    ssoLoading.value = false
  }
})
</script>

<style scoped>
.sso-entry {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 100vh;
  color: #606266;
  font-size: 14px;
}
</style>
