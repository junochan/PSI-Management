<template>
  <el-config-provider :locale="locale" :table="tableConfig">
    <router-view />
  </el-config-provider>
</template>

<script setup>
import zhCn from 'element-plus/es/locale/lang/zh-cn'

const locale = zhCn

/** 全局表格省略号悬停气泡：见 styles/index.scss 中 .ep-table-overflow-tooltip */
const tableConfig = {
  tooltipOptions: {
    popperClass: 'ep-table-overflow-tooltip'
  }
}
</script>

<style>
#app {
  width: 100%;
  height: 100vh;
}
</style>