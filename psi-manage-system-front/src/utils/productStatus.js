/**
 * 新建销售单、采购单时可选的商品：与后端 StatusNameResolver.resolveProductStatusName 一致，
 * 仅排除明确为停售的状态；空值视为在售（兼容旧数据）。
 */
export function isProductSelectableForOrder(product) {
  if (!product) return false
  const raw = product.status
  if (raw == null || String(raw).trim() === '') return true
  const s = String(raw).trim().toLowerCase()
  return s !== 'off_sale' && s !== 'offsale' && s !== 'inactive' && s !== '停售'
}

/**
 * 库存行 + 商品主数据列表：是否允许「创建采购」快捷入口（无主数据时不拦截，避免误伤）。
 */
export function canShortcutPurchaseForStock(stockRow, productList) {
  if (!stockRow?.productId || !Array.isArray(productList)) return true
  const p = productList.find(x => x.id === stockRow.productId)
  if (!p) return true
  return isProductSelectableForOrder(p)
}
