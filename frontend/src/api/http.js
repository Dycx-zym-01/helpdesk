import axios from 'axios'
import { ElMessage } from 'element-plus'

import { translate } from '@/i18n'
import { API_BASE_URL } from '@/utils/runtime'

const http = axios.create({
  baseURL: API_BASE_URL,
  timeout: 20000
})

http.interceptors.request.use((config) => {
  const token = localStorage.getItem('helpdesk-token')
  if (token) {
    config.headers['X-USER-ID'] = token
  }
  return config
})

http.interceptors.response.use(
  (response) => response,
  (error) => {
    const message = error.response?.data?.message || error.message || translate('common.requestFailed')
    ElMessage.error(message)
    if (error.response?.status === 401) {
      localStorage.removeItem('helpdesk-token')
      localStorage.removeItem('helpdesk-user')
      if (!window.location.pathname.startsWith('/login')) {
        window.location.href = '/login'
      }
    }
    return Promise.reject(error)
  }
)

export default http
