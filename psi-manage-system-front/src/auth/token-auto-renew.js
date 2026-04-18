import { authApi } from '@/api'
import { useUserStore } from '@/stores/user'

const RENEW_INTERVAL_MS = 30 * 60 * 1000
const ACTIVITY_GUARD_MS = 5000
const ACTIVE_EVENTS = ['click', 'keydown', 'mousemove']

let initialized = false
let renewInFlight = null
let lastRenewAt = 0
let lastActivityHandledAt = 0
const TOKEN_UPDATED_AT_KEY = 'tokenUpdatedAt'

async function renewTokenIfNeeded(userStore) {
  if (!userStore.token) return
  const tokenUpdatedAt = Number(localStorage.getItem(TOKEN_UPDATED_AT_KEY) || 0)
  if (tokenUpdatedAt > lastRenewAt) {
    lastRenewAt = tokenUpdatedAt
  }
  const now = Date.now()
  if (now - lastRenewAt < RENEW_INTERVAL_MS) return
  if (renewInFlight) return

  renewInFlight = authApi.refresh()
    .then((res) => {
      if (res?.token) {
        userStore.setToken(res.token)
        lastRenewAt = Date.now()
      }
    })
    .catch(() => {
      // 401/403 由全局响应拦截器统一处理
    })
    .finally(() => {
      renewInFlight = null
    })

  await renewInFlight
}

/**
 * 初始化 token 自动续期（活跃滑动续期）
 */
export function initTokenAutoRenew(pinia) {
  if (initialized) return
  initialized = true

  const userStore = useUserStore(pinia)

  const handleActivity = () => {
    const now = Date.now()
    if (now - lastActivityHandledAt < ACTIVITY_GUARD_MS) return
    lastActivityHandledAt = now
    renewTokenIfNeeded(userStore)
  }

  const handleVisibilityChange = () => {
    if (document.visibilityState === 'visible') {
      handleActivity()
    }
  }

  for (const eventName of ACTIVE_EVENTS) {
    window.addEventListener(eventName, handleActivity, { passive: true })
  }
  document.addEventListener('visibilitychange', handleVisibilityChange)
}
