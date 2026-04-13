<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'

import LanguageSwitch from '@/components/LanguageSwitch.vue'
import { useAuthStore } from '@/stores/auth'
import { useAppStore } from '@/stores/app'
import { FILE_BASE_URL } from '@/utils/runtime'
import { formatDateTime } from '@/utils/format'
import { localizeNotificationContent, localizeNotificationTitle, localizeRole, useI18n } from '@/i18n'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()
const appStore = useAppStore()
const { locale, t } = useI18n()

const notificationVisible = ref(false)
const operationGuideLabel = computed(() => (locale.value === 'en' ? 'Guide' : '操作说明'))
const operationGuideFilename = computed(() =>
  locale.value === 'en' ? 'Operation-Guide-English.docx' : '操作说明-中文版.docx'
)
const operationGuideUrl = computed(() => {
  const encodedFilename = encodeURIComponent(operationGuideFilename.value)
  return `${FILE_BASE_URL}/uploads/operation-guides/${encodedFilename}`
})

const menus = computed(() => {
  const base = [
    { index: 'tickets', label: t('nav.tickets') },
    { index: 'ticket-create', label: t('nav.create') },
    { index: 'operation-guide', label: operationGuideLabel.value }
  ]
  if (authStore.user?.role === 'ADMIN') {
    base.push({ index: 'stats', label: t('nav.stats') })
    base.push({ index: 'users', label: t('nav.users') })
  }
  return base
})

const activeMenu = computed(() => (['ticket-detail', 'ticket-edit'].includes(route.name) ? 'tickets' : route.name))

onMounted(async () => {
  if (!appStore.meta.statuses.length) {
    await appStore.fetchMeta()
  }
  if (authStore.isLoggedIn) {
    await appStore.fetchNotifications()
  }
})

function onMenuSelect(index) {
  if (index === 'operation-guide') {
    downloadOperationGuide()
    return
  }
  router.push({ name: index })
}

async function openNotifications() {
  notificationVisible.value = true
  await appStore.fetchNotifications()
}

async function openNotification(item) {
  if (!item.read) {
    await appStore.readNotification(item.id)
  }
  notificationVisible.value = false
  if (item.ticketId) {
    router.push({ name: 'ticket-detail', params: { id: item.ticketId } })
  }
}

async function readAll() {
  await appStore.readAllNotifications()
  ElMessage.success(t('layout.markAllReadSuccess'))
}

function downloadOperationGuide() {
  const link = document.createElement('a')
  link.href = operationGuideUrl.value
  link.download = operationGuideFilename.value
  link.click()
}

function logout() {
  authStore.logout()
  router.replace({ name: 'login' })
}
</script>

<template>
  <div class="page-shell layout-shell">
    <aside class="sidebar glass-card">
      <div class="sidebar-top">
        <LanguageSwitch />
      </div>

      <div class="brand">
        <div class="brand-mark">SITC</div>
        <div>
          <h1>{{ t('common.appName') }}</h1>
        </div>
      </div>

      <el-menu :default-active="String(activeMenu)" class="nav-menu" @select="onMenuSelect">
        <el-menu-item v-for="item in menus" :key="item.index" :index="item.index">
          {{ item.label }}
        </el-menu-item>
      </el-menu>

      <div class="sidebar-footer">
        <div class="sidebar-note">{{ t('layout.roleTitle') }}</div>
        <strong>{{ localizeRole(authStore.user?.role) }}</strong>
      </div>
    </aside>

    <main class="layout-main">
      <header class="layout-header glass-card">
        <div>
          <div class="welcome-title">{{ t('layout.welcome', { name: authStore.user?.name || '' }) }}</div>
        </div>

        <div class="header-actions">
          <el-badge :value="appStore.unreadCount" :hidden="!appStore.unreadCount">
            <el-button plain @click="openNotifications">{{ t('layout.notifications') }}</el-button>
          </el-badge>
          <el-button type="primary" @click="logout">{{ t('layout.logout') }}</el-button>
        </div>
      </header>

      <section class="layout-content">
        <router-view />
      </section>
    </main>

    <el-drawer v-model="notificationVisible" :title="t('layout.drawerTitle')" size="420px">
      <div class="toolbar" style="margin-bottom: 12px">
        <span class="section-subtitle">{{ t('layout.unreadCount', { count: appStore.unreadCount }) }}</span>
        <el-button link type="primary" @click="readAll">{{ t('layout.markAllRead') }}</el-button>
      </div>

      <div v-if="appStore.notifications.length" class="notification-list">
        <button
          v-for="item in appStore.notifications"
          :key="item.id"
          class="notification-item"
          :class="{ unread: !item.read }"
          @click="openNotification(item)"
        >
          <div class="notification-head">
            <strong>{{ localizeNotificationTitle(item) }}</strong>
            <span>{{ formatDateTime(item.createdAt) }}</span>
          </div>
          <p>{{ localizeNotificationContent(item) }}</p>
          <small v-if="item.ticketNo">{{ t('layout.relatedTicket', { ticketNo: item.ticketNo }) }}</small>
        </button>
      </div>
      <div v-else class="empty-block">{{ t('layout.noNotifications') }}</div>
    </el-drawer>
  </div>
</template>

<style scoped>
.layout-shell {
  display: grid;
  grid-template-columns: 236px minmax(0, 1fr);
  gap: 18px;
  padding: 18px;
  height: 100dvh;
  overflow: hidden;
}

.sidebar {
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 20px;
  min-height: 0;
  overflow: auto;
}

.sidebar-top {
  display: flex;
  justify-content: flex-start;
}

.brand {
  display: flex;
  align-items: center;
  gap: 14px;
}

.brand-mark {
  width: 48px;
  height: 48px;
  border-radius: 18px;
  display: grid;
  place-items: center;
  font-size: 18px;
  font-weight: 800;
  color: white;
  background: linear-gradient(135deg, #2f6b4f, #6ca377);
}

.brand h1 {
  margin: 0;
  font-size: 17px;
  line-height: 1.35;
}

.nav-menu {
  border-right: none;
  background: transparent;
}

.nav-menu :deep(.el-menu-item) {
  height: 52px;
  line-height: 52px;
  padding: 0 14px;
  font-size: 18px;
}

.sidebar-footer {
  margin-top: auto;
  padding: 16px;
  border-radius: 18px;
  background: var(--primary-soft);
}

.sidebar-note {
  color: var(--muted);
  margin-bottom: 6px;
}

.layout-main {
  display: flex;
  flex-direction: column;
  gap: 18px;
  min-height: 0;
  overflow: hidden;
}

.layout-header {
  padding: 18px 22px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.welcome-title {
  font-size: 22px;
  font-weight: 700;
}

.header-actions {
  display: flex;
  gap: 12px;
  align-items: center;
}

.layout-content {
  min-width: 0;
  min-height: 0;
  overflow: auto;
}

.notification-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.notification-item {
  width: 100%;
  padding: 14px;
  text-align: left;
  border-radius: 18px;
  border: 1px solid var(--border);
  background: white;
  cursor: pointer;
}

.notification-item.unread {
  border-color: rgba(47, 107, 79, 0.3);
  background: rgba(47, 107, 79, 0.05);
}

.notification-head {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 10px;
}

.notification-head span,
.notification-item small,
.notification-item p {
  color: var(--muted);
}

.notification-item p {
  margin: 0 0 8px;
  line-height: 1.6;
}

@media (max-width: 960px) {
  .layout-shell {
    grid-template-columns: 1fr;
    height: auto;
    min-height: 100dvh;
    overflow: visible;
  }

  .sidebar {
    order: 2;
    min-height: auto;
    overflow: visible;
  }

  .layout-header {
    flex-direction: column;
    align-items: flex-start;
  }

  .layout-main,
  .layout-content {
    min-height: auto;
    overflow: visible;
  }
}
</style>
