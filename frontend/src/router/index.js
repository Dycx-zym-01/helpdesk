import { createRouter, createWebHistory } from 'vue-router'
import pinia from '@/stores'
import { useAuthStore } from '@/stores/auth'

const routes = [
  {
    path: '/login',
    name: 'login',
    component: () => import('@/views/LoginView.vue'),
    meta: { public: true }
  },
  {
    path: '/',
    component: () => import('@/layouts/MainLayout.vue'),
    children: [
      {
        path: '',
        name: 'tickets',
        component: () => import('@/views/TicketListView.vue')
      },
      {
        path: 'tickets/new',
        name: 'ticket-create',
        component: () => import('@/views/TicketCreateView.vue')
      },
      {
        path: 'tickets/:id/edit',
        name: 'ticket-edit',
        component: () => import('@/views/TicketCreateView.vue')
      },
      {
        path: 'tickets/:id',
        name: 'ticket-detail',
        component: () => import('@/views/TicketDetailView.vue')
      },
      {
        path: 'stats',
        name: 'stats',
        component: () => import('@/views/StatsView.vue'),
        meta: { roles: ['ADMIN'] }
      },
      {
        path: 'users',
        name: 'users',
        component: () => import('@/views/UserManageView.vue'),
        meta: { roles: ['ADMIN'] }
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach(async (to) => {
  const authStore = useAuthStore(pinia)

  if (to.meta.public) {
    if (to.name === 'login' && authStore.isLoggedIn) {
      return { name: 'tickets' }
    }
    return true
  }

  if (!authStore.isLoggedIn) {
    return { name: 'login', query: { redirect: to.fullPath } }
  }

  if (!authStore.initialized) {
    try {
      await authStore.fetchMe()
    } catch (error) {
      authStore.logout()
      return { name: 'login' }
    }
  }

  const allowRoles = to.meta.roles
  if (allowRoles?.length && !allowRoles.includes(authStore.user?.role)) {
    return { name: 'tickets' }
  }

  return true
})

export default router
