/**
 * 商品分类 status 与后端 SysCategory 一致：1 启用，0 禁用。
 * 缺省或非 0 数字按启用处理，避免旧数据无字段时被误过滤。
 */
export function isCategoryEnabled(c) {
  const s = c?.status
  if (s === 0 || s === '0') return false
  return true
}
