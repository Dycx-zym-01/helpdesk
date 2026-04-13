import { defineStore } from 'pinia'
import http from '@/api/http'

export const useAppStore = defineStore('app', {
  state: () => ({
    meta: {
      statuses: [],
      priorities: [],
      roles: [],
      categories: []
    },
    notifications: []
  }),
  getters: {
    unreadCount: (state) => state.notifications.filter((item) => !item.read).length
  },
  actions: {
    async fetchMeta() {
      const { data } = await http.get('/meta')
      this.meta = data
      return data
    },
    async fetchNotifications() {
      const { data } = await http.get('/notifications')
      this.notifications = data
      return data
    },
    async readNotification(id) {
      await http.post(`/notifications/${id}/read`)
      const target = this.notifications.find((item) => item.id === id)
      if (target) {
        target.read = true
      }
    },
    async readAllNotifications() {
      await http.post('/notifications/read-all')
      this.notifications = this.notifications.map((item) => ({ ...item, read: true }))
    }
  }
})
