import axios from 'axios'
import fs from 'fs'
import path from 'path'
import os from 'os'

const DEFAULT_DIR = path.join(os.homedir(), '.psi-smart-ims')
export const TOKEN_PATH = path.join(DEFAULT_DIR, 'token')

export function loadToken () {
  const t = process.env.PSI_TOKEN?.trim()
  if (t) return t
  try {
    return fs.readFileSync(TOKEN_PATH, 'utf8').trim()
  } catch {
    return ''
  }
}

export function saveToken (token) {
  fs.mkdirSync(DEFAULT_DIR, { recursive: true })
  fs.writeFileSync(TOKEN_PATH, String(token), 'utf8')
}

export function clearToken () {
  try {
    fs.unlinkSync(TOKEN_PATH)
  } catch {
    /* ignore */
  }
}

/**
 * 创建已解包 Result.data 的 API 客户端（与前端 axios 行为一致）
 */
export function createApi ({
  baseURL = process.env.PSI_API_BASE || 'http://localhost:8080/api/v1',
  token = '',
  timeout = 120000
} = {}) {
  const instance = axios.create({
    baseURL: baseURL.replace(/\/$/, ''),
    timeout: Number(timeout) || 120000
  })
  instance.interceptors.request.use((cfg) => {
    if (token) cfg.headers.Authorization = `Bearer ${token}`
    const isMultipart =
      cfg.data && typeof cfg.data.getHeaders === 'function'
    if (!isMultipart && !cfg.headers['Content-Type'] && cfg.data && typeof cfg.data === 'object') {
      cfg.headers['Content-Type'] = 'application/json'
    }
    return cfg
  })
  instance.interceptors.response.use(
    (res) => {
      const body = res.data
      if (body && typeof body === 'object' && 'code' in body && body.code !== 200) {
        return Promise.reject(new Error(body.message || '请求失败'))
      }
      if (body && typeof body === 'object' && 'data' in body) return body.data
      return body
    },
    (err) => {
      const msg =
        err.response?.data?.message ||
        (typeof err.response?.data === 'string' ? err.response.data : null) ||
        err.message
      return Promise.reject(new Error(msg))
    }
  )
  return instance
}

/** /util/encode 返回纯文本，不走 Result 包装 */
export async function fetchUtilEncode (baseURL, password) {
  const root = baseURL.replace(/\/$/, '')
  const url = `${root}/util/encode`
  const res = await axios.get(url, {
    params: { password },
    responseType: 'text',
    timeout: 30000
  })
  return res.data
}
