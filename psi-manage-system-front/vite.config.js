import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { resolve } from 'path'

// 生产预览 / Linux 部署脚本：与 scripts/deploy-linux.sh 中 BACKEND_PORT、FRONTEND_PORT 对齐
const backendPort = process.env.BACKEND_PORT || '8080'
const previewPort = Number.parseInt(process.env.FRONTEND_PORT || '4173', 10)

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': resolve(__dirname, 'src')
    }
  },
  server: {
    // 允许通过局域网 IP 访问（默认仅 localhost 无法从其他设备或本机 IP 打开）
    host: true,
    port: 3000,
    open: true,
    proxy: {
      '/api': {
        target: `http://127.0.0.1:${backendPort}`,
        changeOrigin: true
      }
    }
  },
  preview: {
    host: true,
    port: Number.isFinite(previewPort) ? previewPort : 4173,
    strictPort: true,
    proxy: {
      '/api': {
        target: `http://127.0.0.1:${backendPort}`,
        changeOrigin: true
      }
    }
  },
  css: {
    preprocessorOptions: {
      scss: {
        additionalData: `@use "@/styles/variables.scss" as *;`
      }
    }
  }
})