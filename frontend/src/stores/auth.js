import { defineStore } from 'pinia'
import http from '@/api/http'

const TOKEN_KEY = 'helpdesk-token'
const USER_KEY = 'helpdesk-user'

export const useAuthStore = defineStore('auth', {
  state: () => ({
    token: localStorage.getItem(TOKEN_KEY) || '',
    user: JSON.parse(localStorage.getItem(USER_KEY) || 'null'),
    initialized: false
  }),
  getters: {
    isLoggedIn: (state) => Boolean(state.token)
  },
  actions: {
    persist(auth) {
      this.token = auth.token
      this.user = auth.user
      localStorage.setItem(TOKEN_KEY, auth.token)
      localStorage.setItem(USER_KEY, JSON.stringify(auth.user))
    },
    async login(form) {
      const { data } = await http.post('/auth/login', form)
      this.persist(data)
      this.initialized = true
      return data
    },
    async fetchMe() {
      if (!this.token) {
        this.initialized = true
        return null
      }
      const { data } = await http.get('/auth/me')
      this.persist(data)
      this.initialized = true
      return data
    },
    logout() {
      this.token = ''
      this.user = null
      this.initialized = true
      localStorage.removeItem(TOKEN_KEY)
      localStorage.removeItem(USER_KEY)
    }
  }
})
