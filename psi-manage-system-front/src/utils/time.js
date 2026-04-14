/**
 * 格式化时间为 yyyy-MM-dd HH:mm:ss
 * @param {string|Date} time - 时间字符串或Date对象
 * @returns {string} 格式化后的时间字符串
 */
export function formatTime(time) {
  if (!time) return '-'

  let date
  if (typeof time === 'string') {
    // 处理 ISO 格式字符串 (如 "2024-01-15T10:30:00")
    date = new Date(time)
  } else if (time instanceof Date) {
    date = time
  } else {
    return '-'
  }

  // 检查日期是否有效
  if (isNaN(date.getTime())) {
    // 如果是字符串格式，尝试直接替换 T
    if (typeof time === 'string') {
      return time.replace('T', ' ').substring(0, 19)
    }
    return '-'
  }

  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  const hours = String(date.getHours()).padStart(2, '0')
  const minutes = String(date.getMinutes()).padStart(2, '0')
  const seconds = String(date.getSeconds()).padStart(2, '0')

  return `${year}-${month}-${day} ${hours}:${minutes}:${seconds}`
}

/**
 * 格式化时间为 yyyy-MM-dd
 * @param {string|Date} time - 时间字符串或Date对象
 * @returns {string} 格式化后的日期字符串
 */
export function formatDate(time) {
  if (!time) return '-'

  let date
  if (typeof time === 'string') {
    date = new Date(time)
  } else if (time instanceof Date) {
    date = time
  } else {
    return '-'
  }

  if (isNaN(date.getTime())) {
    if (typeof time === 'string') {
      return time.substring(0, 10)
    }
    return '-'
  }

  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')

  return `${year}-${month}-${day}`
}