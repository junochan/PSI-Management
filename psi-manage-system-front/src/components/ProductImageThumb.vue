<template>
  <span v-if="src" class="product-image-thumb-root">
    <el-image
      class="product-image-thumb-el"
      :src="src"
      :preview-src-list="resolvedPreviewList"
      preview-teleported
      fit="cover"
      :hide-on-click-modal="true"
      :style="inlineStyle"
    />
  </span>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  src: { type: String, default: '' },
  /** 预览图列表；不传或为空时用 [src] */
  previewSrcList: { type: Array, default: null },
  width: { type: [Number, String], default: 36 },
  height: { type: [Number, String], default: 36 },
  radius: { type: [Number, String], default: 8 }
})

const inlineStyle = computed(() => {
  const toCss = (v) => (typeof v === 'number' ? `${v}px` : v)
  return {
    width: toCss(props.width),
    height: toCss(props.height),
    borderRadius: toCss(props.radius)
  }
})

const resolvedPreviewList = computed(() => {
  const p = props.previewSrcList
  if (Array.isArray(p) && p.length > 0) return p
  return props.src ? [props.src] : []
})
</script>

<style scoped>
.product-image-thumb-root {
  display: inline-flex;
  flex-shrink: 0;
  vertical-align: middle;
  line-height: 0;
}

.product-image-thumb-el {
  display: block;
  overflow: hidden;
}

.product-image-thumb-root :deep(.el-image__inner) {
  cursor: zoom-in;
}
</style>
