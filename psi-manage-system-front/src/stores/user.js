import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

export const useUserStore = defineStore('user', () => {
  const userInfo = ref(null)
  const token = ref(localStorage.getItem('token') || '')
  const TOKEN_UPDATED_AT_KEY = 'tokenUpdatedAt'

  const isLoggedIn = computed(() => !!userInfo.value || !!token.value)

  const login = (user, jwtToken) => {
    userInfo.value = {
      ...user,
      email: user.email || '',
      phone: user.phone || '',
      avatar: user.avatar || '',
      permissions: Array.isArray(user.permissions) ? user.permissions : []
    }
    // 使用真实的JWT token，如果提供了就用它，否则保持localStorage中的token
    if (jwtToken) {
      token.value = jwtToken
      localStorage.setItem('token', jwtToken)
      localStorage.setItem(TOKEN_UPDATED_AT_KEY, String(Date.now()))
    }
    localStorage.setItem('userInfo', JSON.stringify(userInfo.value))
  }

  const logout = () => {
    userInfo.value = null
    token.value = ''
    localStorage.removeItem('token')
    localStorage.removeItem(TOKEN_UPDATED_AT_KEY)
    localStorage.removeItem('userInfo')
  }

  const setToken = (jwtToken) => {
    if (!jwtToken) return
    token.value = jwtToken
    localStorage.setItem('token', jwtToken)
    localStorage.setItem(TOKEN_UPDATED_AT_KEY, String(Date.now()))
  }

  const initUser = () => {
    const savedUser = localStorage.getItem('userInfo')
    if (savedUser) {
      const parsed = JSON.parse(savedUser)
      if (!Array.isArray(parsed.permissions)) {
        parsed.permissions = []
      }
      userInfo.value = parsed
    }
  }

  /** 是否具备某权限标识（与后端 sys_permission.code 一致） */
  const hasPermission = (code) => {
    if (!code) return true
    const codes = userInfo.value?.permissions
    if (!codes || !codes.length) return false
    return codes.includes(code)
  }

  /** 系统设置入口：一级 settings 或任意 settings:* */
  const hasSettingsMenuAccess = () => {
    const codes = userInfo.value?.permissions || []
    return codes.includes('settings') || codes.some((c) => c && c.startsWith('settings:'))
  }

  const updateUser = (data) => {
    userInfo.value = { ...userInfo.value, ...data }
    localStorage.setItem('userInfo', JSON.stringify(userInfo.value))
  }

  // 初始化用户信息
  initUser()

  return {
    userInfo,
    token,
    isLoggedIn,
    login,
    logout,
    setToken,
    initUser,
    updateUser,
    hasPermission,
    hasSettingsMenuAccess
  }
})