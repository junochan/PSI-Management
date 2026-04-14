<template>
  <template v-for="item in items" :key="item.path + '-' + item.id">
    <router-link
      v-if="!item.children || item.children.length === 0"
      :to="item.path"
      class="nav-item"
      :class="{ active: isActive(item.path) }"
    >
      <el-icon class="nav-icon"><component :is="item.icon || 'Odometer'" /></el-icon>
      <span v-if="!collapsed">{{ item.name }}</span>
    </router-link>
    <div v-else class="nav-group">
      <div v-if="!collapsed" class="nav-group-title">{{ item.name }}</div>
      <NavMenuTree :items="item.children" :collapsed="collapsed" />
    </div>
  </template>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'

defineOptions({ name: 'NavMenuTree' })

const props = defineProps({
  items: { type: Array, default: () => [] },
  collapsed: { type: Boolean, default: false }
})

const route = useRoute()
const path = computed(() => route.path)

const isActive = (menuPath) => {
  if (!menuPath) return false
  const p = path.value
  return p === menuPath || p.startsWith(menuPath + '/')
}
</script>
