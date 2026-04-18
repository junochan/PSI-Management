/**
 * 商品 image 字段：单 URL、逗号分隔、或 JSON 数组（与后端 ProductImageLoader 一致）
 */

export function parseProductImageUrls(image) {
  if (image == null || image === '') return []
  const t = String(image).trim()
  if (!t) return []
  if (t.startsWith('[')) {
    try {
      const arr = JSON.parse(t)
      return Array.isArray(arr) ? arr.map(String).map((s) => s.trim()).filter(Boolean) : []
    } catch {
      return []
    }
  }
  if (t.includes(',')) {
    return t.split(',').map((s) => s.trim()).filter(Boolean)
  }
  return [t]
}

/** 列表/缩略图用首图 */
export function firstProductImageUrl(image) {
  const u = parseProductImageUrls(image)
  return u.length ? u[0] : null
}

/** 表格行：优先行内 productImage/image，否则用商品表 image（与 getRowProductImage 逻辑一致） */
export function productRowPreviewUrls(row, productImageFallback) {
  const fromRow = parseProductImageUrls(row?.productImage || row?.image)
  if (fromRow.length) return fromRow
  return parseProductImageUrls(productImageFallback)
}

/** 提交后端：单张存原 URL，多张存 JSON 数组，避免 URL 内含逗号歧义 */
export function encodeProductImagesForApi(urls) {
  if (!urls?.length) return ''
  const clean = urls.filter(Boolean)
  if (!clean.length) return ''
  if (clean.length === 1) return clean[0]
  return JSON.stringify(clean)
}
