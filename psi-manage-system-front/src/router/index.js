import { createRouter, createWebHistory } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { useNavigationStore } from '@/stores/navigation'
import { registerLayoutRoutes, clearLayoutDynamicRoutes } from '@/router/dynamic-routes'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue'),
    meta: { title: '登录' }
  },
  {
    path: '/',
    name: 'MainLayout',
    component: () => import('@/layouts/MainLayout.vue'),
    children: []
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

function routeMetaAllowed(userStore, meta) {
  if (!meta?.permissionCode && meta?.permissionMode !== 'SETTINGS_ANY') return true
  if (meta.permissionMode === 'SETTINGS_ANY') {
    return userStore.hasSettingsMenuAccess()
  }
  return userStore.hasPermission(meta.permissionCode)
}

router.beforeEach(async (to, from, next) => {
  document.title = `${to.meta.title || '智链进销存'} - 智链进销存`

  if (to.path === '/login') {
    if (from.path !== '/login') {
      clearLayoutDynamicRoutes(router)
      useNavigationStore().reset()
    }
    next()
    return
  }

  const userStore = useUserStore()
  const navStore = useNavigationStore()

  if (!userStore.isLoggedIn) {
    next('/login')
    return
  }

  const permList = userStore.userInfo?.permissions
  if (!Array.isArray(permList) || permList.length === 0) {
    userStore.logout()
    navStore.reset()
    clearLayoutDynamicRoutes(router)
    ElMessage.warning('请重新登录以同步权限')
    next('/login')
    return
  }

  if (!navStore.loaded) {
    try {
      await navStore.fetchNavigation()
      clearLayoutDynamicRoutes(router)
      registerLayoutRoutes(router, navStore.routes)
    } catch (e) {
      userStore.logout()
      navStore.reset()
      clearLayoutDynamicRoutes(router)
      ElMessage.error(e?.message || '加载导航失败，请重新登录')
      next('/login')
      return
    }
    return next({ ...to, replace: true })
  }

  if (to.path === '/') {
    const hp = navStore.homePath || '/dashboard'
    next({ path: hp, replace: true })
    return
  }

  const leaf = to.matched[to.matched.length - 1]
  const meta = leaf?.meta
  if (meta && (meta.permissionCode || meta.permissionMode === 'SETTINGS_ANY')) {
    if (!routeMetaAllowed(userStore, meta)) {
      ElMessage.warning('无权访问该页面')
      next(navStore.homePath || '/dashboard')
      return
    }
  }

  next()
})

export default router
