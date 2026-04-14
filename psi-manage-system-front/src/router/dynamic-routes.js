import { viewLoaders } from './view-loaders'

const dynamicRouteNames = new Set()

/** 嵌套在 MainLayout 下须用相对 path；若以 / 开头会被 Vue Router 当成绝对路径，导致无法挂在外壳下、resolve 异常 */
function toLayoutChildPath(path) {
  if (path == null || path === '') return ''
  return String(path).replace(/^\/+/, '')
}

/**
 * 根据后端返回的 routes 注册到 MainLayout 下
 */
export function registerLayoutRoutes(router, routes) {
  if (!routes?.length) return
  for (const r of routes) {
    const loader = viewLoaders[r.viewKey]
    if (!loader) {
      console.warn('[router] 未注册的 viewKey:', r.viewKey)
      continue
    }
    const name = r.routeName
    if (router.hasRoute(name)) {
      router.removeRoute(name)
    }
    const childPath = toLayoutChildPath(r.path)
    if (!childPath) {
      console.warn('[router] 跳过空 path 的路由:', r.routeName)
      continue
    }
    router.addRoute('MainLayout', {
      path: childPath,
      name,
      component: loader,
      meta: {
        title: r.title,
        hidden: r.hidden,
        permissionCode: r.permissionCode,
        permissionMode: r.permissionMode || 'NORMAL'
      }
    })
    dynamicRouteNames.add(name)
  }
}

export function clearLayoutDynamicRoutes(router) {
  for (const name of dynamicRouteNames) {
    if (router.hasRoute(name)) {
      router.removeRoute(name)
    }
  }
  dynamicRouteNames.clear()
}

/** 后端 UiRouteVO.path 转主布局下绝对路径 */
export function routeItemToAbsPath(r) {
  if (!r?.path) return null
  const p = String(r.path).replace(/^\/+/, '')
  return p ? `/${p}` : '/'
}

/**
 * 在动态路由注册完成后，解析第一个可访问的子页面路径。
 * 优先用 routeName 判断（与 addRoute 注册一致），避免仅依赖 matched 条数在部分嵌套/绝对 path 场景下误判。
 */
export function resolveSafeHomePath(navStore, router) {
  const routes = navStore.routes || []

  for (const r of routes) {
    const name = r.routeName
    if (name && router.hasRoute(name)) {
      const p = routeItemToAbsPath(r)
      if (p) return p
    }
  }

  const pathCandidates = []
  const hp = navStore.homePath || '/dashboard'
  pathCandidates.push(hp)
  for (const r of routes) {
    const abs = routeItemToAbsPath(r)
    if (abs) pathCandidates.push(abs)
  }

  const seen = new Set()
  for (const raw of pathCandidates) {
    const p = raw.startsWith('/') ? raw : `/${raw}`
    if (seen.has(p)) continue
    seen.add(p)
    const resolved = router.resolve(p)
    if (resolved.matched.length >= 2) return p
    // 单条匹配且不是仅外壳（例如错误注册成根路由时仍可能有组件）
    if (resolved.matched.length === 1 && resolved.matched[0].name !== 'MainLayout') return p
  }
  return null
}
