import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useMessageStore = defineStore('message', () => {
  const messages = ref([
    { id: 1, title: '库存预警', content: 'Apple Watch S9 库存严重不足，请及时补货', type: 'warning', read: false, time: '2026-04-11 09:00' },
    { id: 2, title: '新订单提醒', content: '客户 李明科技有限公司 下单了 iPhone 15 Pro Max，数量 5', type: 'order', read: false, time: '2026-04-11 14:23' },
    { id: 3, title: '采购入库', content: '采购单 #PO2026041102 已完成入库', type: 'inbound', read: true, time: '2026-04-10 10:30' },
    { id: 4, title: '售后工单', content: '新的售后工单 #AS2026041001 需处理', type: 'aftersales', read: false, time: '2026-04-10 15:30' }
  ])

  const unreadCount = ref(3)

  const markAsRead = (id) => {
    const msg = messages.value.find(m => m.id === id)
    if (msg && !msg.read) {
      msg.read = true
      unreadCount.value--
    }
  }

  const markAllAsRead = () => {
    messages.value.forEach(m => m.read = true)
    unreadCount.value = 0
  }

  const addMessage = (message) => {
    messages.value.unshift({
      id: Date.now(),
      ...message,
      read: false,
      time: new Date().toLocaleString()
    })
    unreadCount.value++
  }

  return {
    messages,
    unreadCount,
    markAsRead,
    markAllAsRead,
    addMessage
  }
})