import { defineStore } from 'pinia'
import { ref } from 'vue'
import { authApi } from '@/api'
import { useUserStore } from '@/stores/user'

export const useNavigationStore = defineStore('navigation', () => {
  const menus = ref([])
  const permissions = ref([])
  const routes = ref([])
  const homePath = ref('/dashboard')
  const loaded = ref(false)
  const loading = ref(false)

  function reset() {
    menus.value = []
    permissions.value = []
    routes.value = []
    homePath.value = '/dashboard'
    loaded.value = false
    loading.value = false
  }

  /**
   * 拉取导航并同步 userStore.permissions
   */
  async function fetchNavigation() {
    loading.value = true
    try {
      const data = await authApi.navigation()
      menus.value = data.menus || []
      permissions.value = data.permissions || []
      routes.value = data.routes || []
      homePath.value = data.homePath || '/dashboard'
      loaded.value = true
      const userStore = useUserStore()
      if (userStore.userInfo) {
        userStore.updateUser({ permissions: permissions.value })
      }
      return data
    } finally {
      loading.value = false
    }
  }

  return {
    menus,
    permissions,
    routes,
    homePath,
    loaded,
    loading,
    fetchNavigation,
    reset
  }
})
