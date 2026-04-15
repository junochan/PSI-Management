import { createRouter, createWebHistory } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { useNavigationStore } from '@/stores/navigation'
import { registerLayoutRoutes, clearLayoutDynamicRoutes, resolveSafeHomePath } from '@/router/dynamic-routes'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue'),
    meta: { title: '登录' }
  },
  {
    path: '/sso',
    name: 'SsoEntry',
    component: () => import('@/views/SsoEntry.vue'),
    meta: { title: '登录中' }
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

/** 仅匹配到主壳、子级 router-view 无页面时白屏（勿仅用 matched 条数泛化判断，避免误判） */
function isBlankNestedOutlet(to) {
  const m = to.matched
  if (!m.length) return true
  return m.length === 1 && m[0].name === 'MainLayout'
}

router.beforeEach(async (to, from, next) => {
  document.title = `${to.meta.title || '智链进销存'} - 智链进销存`

  if (to.path === '/login' || to.path === '/sso') {
    if (to.path === '/login' && from.path !== '/login') {
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
    const safe = resolveSafeHomePath(navStore, router)
    if (!safe) {
      ElMessage.error('未找到可访问页面，请重新登录或联系管理员')
      userStore.logout()
      navStore.reset()
      clearLayoutDynamicRoutes(router)
      next('/login')
      return
    }
    next({ path: safe, replace: true })
    return
  }

  if (isBlankNestedOutlet(to)) {
    const safe = resolveSafeHomePath(navStore, router)
    if (!safe) {
      ElMessage.error('未找到可访问页面，请重新登录或联系管理员')
      userStore.logout()
      navStore.reset()
      clearLayoutDynamicRoutes(router)
      next('/login')
      return
    }
    if (safe !== to.path) {
      ElMessage.warning('当前页面不可用，已跳转到可访问首页')
    }
    next({ path: safe, replace: true })
    return
  }

  const leaf = to.matched[to.matched.length - 1]
  const meta = leaf?.meta
  if (meta && (meta.permissionCode || meta.permissionMode === 'SETTINGS_ANY')) {
    if (!routeMetaAllowed(userStore, meta)) {
      ElMessage.warning('无权访问该页面')
      const safe = resolveSafeHomePath(navStore, router) || '/login'
      next(safe)
      return
    }
  }

  next()
})

export default router
