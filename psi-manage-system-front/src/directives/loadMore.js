const THRESHOLD_PX = 24
/** Element Plus 2.x Select 下拉里实际滚动的是 el-scrollbar 的 wrap */
const findSelectScrollWrap = (popperClass) => {
  if (!popperClass) return null
  const root = document.querySelector(`.${popperClass}`)
  if (!root) return null
  return (
    root.querySelector('.el-select-dropdown__wrap.el-scrollbar__wrap') ||
    root.querySelector('.el-scrollbar__wrap') ||
    root.querySelector('.el-select-dropdown__wrap')
  )
}

const resolveConfig = (binding) => {
  const value = binding?.value || {}
  return {
    popperClass: value.popperClass || '',
    onLoadMore: typeof value.onLoadMore === 'function' ? value.onLoadMore : null,
    disabled: Boolean(value.disabled),
    threshold: Number.isFinite(value.threshold) ? Number(value.threshold) : THRESHOLD_PX
  }
}

export default {
  mounted(el, binding) {
    el.__loadMoreBinding = binding

    let wrapEl = null
    let observer = null
    let clickHandler = null

    const getBinding = () => el.__loadMoreBinding || binding

    const unbindScroll = () => {
      if (wrapEl && wrapEl.__loadMoreScrollHandler) {
        wrapEl.removeEventListener('scroll', wrapEl.__loadMoreScrollHandler)
        delete wrapEl.__loadMoreScrollHandler
      }
      wrapEl = null
    }

    const onScroll = async () => {
      const cfg = resolveConfig(getBinding())
      if (!wrapEl || cfg.disabled || !cfg.onLoadMore) return
      const remain = wrapEl.scrollHeight - wrapEl.scrollTop - wrapEl.clientHeight
      if (remain <= cfg.threshold) {
        await cfg.onLoadMore()
      }
    }

    /** 选项过少不足以出现滚动条时 scroll 事件永远不会触发，需主动补页直到可滚动或无更多 */
    const ensureFillIfShort = async () => {
      const rootBinding = getBinding()
      const cfg0 = resolveConfig(rootBinding)
      if (!cfg0.popperClass || !wrapEl?.isConnected) return

      for (let i = 0; i < 20; i++) {
        const cfg = resolveConfig(getBinding())
        if (!wrapEl || !wrapEl.isConnected || cfg.disabled || !cfg.onLoadMore) return

        const remain = wrapEl.scrollHeight - wrapEl.scrollTop - wrapEl.clientHeight
        if (remain > cfg.threshold + 0.5) return

        await cfg.onLoadMore()
        await new Promise((r) => requestAnimationFrame(r))
      }
    }

    const tryBind = () => {
      const cfg = resolveConfig(getBinding())
      if (!cfg.popperClass) return
      const nextWrap = findSelectScrollWrap(cfg.popperClass)
      if (!nextWrap) return

      if (nextWrap === wrapEl && wrapEl.__loadMoreScrollHandler) {
        return
      }

      unbindScroll()
      wrapEl = nextWrap
      wrapEl.__loadMoreScrollHandler = onScroll
      wrapEl.addEventListener('scroll', wrapEl.__loadMoreScrollHandler, { passive: true })
      requestAnimationFrame(() => {
        void ensureFillIfShort()
      })
    }

    observer = new MutationObserver(() => {
      tryBind()
    })
    observer.observe(document.body, { childList: true, subtree: true })

    clickHandler = () => {
      setTimeout(tryBind, 0)
    }
    el.addEventListener('click', clickHandler)

    el.__loadMoreCleanup = () => {
      unbindScroll()
      if (observer) observer.disconnect()
      if (clickHandler) el.removeEventListener('click', clickHandler)
      delete el.__loadMoreCleanup
      delete el.__loadMoreBinding
    }
  },
  updated(el, binding) {
    el.__loadMoreBinding = binding
    const cfg = resolveConfig(binding)
    if (!cfg.popperClass) return
    setTimeout(() => {
      const wrap =
        findSelectScrollWrap(cfg.popperClass) ||
        document.querySelector(`.${cfg.popperClass} .el-select-dropdown__wrap`)
      if (wrap && !wrap.__loadMoreScrollHandler) {
        const ev = new MouseEvent('click', { bubbles: true })
        el.dispatchEvent(ev)
      }
    }, 0)
  },
  unmounted(el) {
    if (typeof el.__loadMoreCleanup === 'function') {
      el.__loadMoreCleanup()
    }
  }
}
