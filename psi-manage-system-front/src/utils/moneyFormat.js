/**
 * 金额展示：无「分」则不带小数；否则保留两位小数（四舍五入），默认带 zh-CN 千分位。
 *
 * @param {number|string|null|undefined} val
 * @param {{ withGrouping?: boolean }} [options]
 * @returns {string}
 */
export function formatAmountDisplay(val, options = {}) {
  const { withGrouping = true } = options
  if (val === null || val === undefined || val === '') return '-'
  const n = Number(val)
  if (Number.isNaN(n)) return '-'
  const cents = Math.round(n * 100)
  const hasFraction = cents % 100 !== 0
  const yuan = cents / 100
  if (!withGrouping) {
    if (!hasFraction) return String(Math.trunc(yuan))
    return yuan.toFixed(2)
  }
  if (!hasFraction) {
    return yuan.toLocaleString('zh-CN', { maximumFractionDigits: 0 })
  }
  return yuan.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}
