import { viewLoaders } from './view-loaders'

const dynamicRouteNames = new Set()

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
    router.addRoute('MainLayout', {
      path: r.path,
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
