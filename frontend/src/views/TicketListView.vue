<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'

import http from '@/api/http'
import { useAuthStore } from '@/stores/auth'
import { useAppStore } from '@/stores/app'
import { formatDateTime, priorityTagType, statusTagType } from '@/utils/format'
import { localizeCategory, localizePriority, localizeStatus, useI18n } from '@/i18n'

const router = useRouter()
const authStore = useAuthStore()
const appStore = useAppStore()
const { t } = useI18n()

const loading = ref(false)
const tickets = ref([])
const developers = ref([])
const pagination = reactive({
  page: 1,
  size: 5,
  total: 0
})

const filters = reactive({
  status: '',
  category: '',
  priority: '',
  keyword: '',
  assigneeId: null,
  mine: false
})

const assignDialog = reactive({
  visible: false,
  ticketId: null,
  assigneeId: null
})

const resolveDialog = reactive({
  visible: false,
  ticketId: null,
  comment: ''
})

const isAdmin = computed(() => authStore.user?.role === 'ADMIN')
const isDeveloper = computed(() => authStore.user?.role === 'DEVELOPER')

const statusOptions = computed(() =>
  appStore.meta.statuses.map((item) => ({ ...item, label: localizeStatus(item.value) }))
)
const categoryOptions = computed(() =>
  appStore.meta.categories.map((item) => ({ ...item, label: localizeCategory(item.value) }))
)
const priorityOptions = computed(() =>
  appStore.meta.priorities.map((item) => ({ ...item, label: localizePriority(item.value) }))
)

onMounted(async () => {
  if (!appStore.meta.statuses.length) {
    await appStore.fetchMeta()
  }
  if (isAdmin.value || isDeveloper.value) {
    await loadDevelopers()
  }
  await loadTickets()
})

async function loadDevelopers() {
  const { data } = await http.get('/users', { params: { role: 'DEVELOPER' } })
  developers.value = data
}

async function loadTickets() {
  loading.value = true
  try {
    const params = {
      page: pagination.page - 1,
      size: pagination.size
    }
    Object.entries(filters).forEach(([key, value]) => {
      if (value !== '' && value !== null && value !== false) {
        params[key] = value
      }
    })
    const { data } = await http.get('/tickets', { params })
    tickets.value = data.content
    pagination.total = data.totalElements
  } finally {
    loading.value = false
  }
}

async function resetFilters() {
  filters.status = ''
  filters.category = ''
  filters.priority = ''
  filters.keyword = ''
  filters.assigneeId = null
  filters.mine = false
  pagination.page = 1
  await loadTickets()
}

function viewDetail(row) {
  router.push({ name: 'ticket-detail', params: { id: row.id } })
}

function openAssign(row) {
  assignDialog.visible = true
  assignDialog.ticketId = row.id
  assignDialog.assigneeId = row.assignee?.id || null
}

function editDraft(row) {
  router.push({ name: 'ticket-edit', params: { id: row.id } })
}

async function submitAssign() {
  await http.post(`/tickets/${assignDialog.ticketId}/assign`, {
    assigneeId: assignDialog.assigneeId
  })
  ElMessage.success(t('ticketList.assignSuccess'))
  assignDialog.visible = false
  await loadTickets()
}

async function claim(row) {
  await http.post(`/tickets/${row.id}/claim`)
  ElMessage.success(t('ticketDetail.claimSuccess'))
  await loadTickets()
}

function openResolve(row) {
  resolveDialog.visible = true
  resolveDialog.ticketId = row.id
  resolveDialog.comment = ''
}

async function submitResolve() {
  await http.post(`/tickets/${resolveDialog.ticketId}/resolve`, {
    comment: resolveDialog.comment
  })
  ElMessage.success(t('ticketList.resolveSuccess'))
  resolveDialog.visible = false
  await loadTickets()
}

async function exportData() {
  const params = {}
  Object.entries(filters).forEach(([key, value]) => {
    if (value !== '' && value !== null && value !== false) {
      params[key] = value
    }
  })
  const { data } = await http.get('/tickets/export', {
    params,
    responseType: 'blob'
  })
  const url = URL.createObjectURL(data)
  const link = document.createElement('a')
  link.href = url
  link.download = t('ticketList.exportSuccessName')
  link.click()
  URL.revokeObjectURL(url)
}

function canAssign(row) {
  return isAdmin.value && !['DRAFT', 'RESOLVED'].includes(row.status)
}

function canClaim(row) {
  return isDeveloper.value && !row.assignee && row.status === 'PENDING'
}

function canResolve(row) {
  return ((isDeveloper.value && row.assignee?.id === authStore.user?.id) || isAdmin.value) &&
    row.status === 'PROCESSING'
}

function canEditDraft(row) {
  return row.status === 'DRAFT' && row.creator?.id === authStore.user?.id
}

function canDeleteDraft(row) {
  return row.status === 'DRAFT' && (row.creator?.id === authStore.user?.id || isAdmin.value)
}

async function deleteDraft(row) {
  try {
    await ElMessageBox.confirm(t('ticketList.deleteDraftConfirm'), t('ticketList.deleteDraft'))
  } catch {
    return
  }
  await http.delete(`/tickets/${row.id}`)
  ElMessage.success(t('ticketList.deleteDraftSuccess'))
  await loadTickets()
}

function formatListDateTime(value) {
  if (!value) {
    return '-'
  }
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) {
    return formatDateTime(value)
  }
  const pad = (num) => String(num).padStart(2, '0')
  return `${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}`
}

function getRowActions(row) {
  const actions = []
  if (canEditDraft(row)) {
    actions.push({ command: 'editDraft', label: t('ticketList.editDraft') })
  }
  if (canDeleteDraft(row)) {
    actions.push({ command: 'deleteDraft', label: t('ticketList.deleteDraft') })
  }
  if (canAssign(row)) {
    actions.push({ command: 'assign', label: t('ticketList.assign') })
  }
  if (canClaim(row)) {
    actions.push({ command: 'claim', label: t('ticketList.claim') })
  }
  if (canResolve(row)) {
    actions.push({ command: 'resolve', label: t('ticketList.resolve') })
  }
  return actions
}

function handleRowAction(command, row) {
  if (command === 'editDraft') {
    editDraft(row)
    return
  }
  if (command === 'deleteDraft') {
    deleteDraft(row)
    return
  }
  if (command === 'assign') {
    openAssign(row)
    return
  }
  if (command === 'claim') {
    claim(row)
    return
  }
  if (command === 'resolve') {
    openResolve(row)
  }
}
</script>

<template>
  <div class="list-page">
    <section class="glass-card block">
      <div class="toolbar">
        <div>
          <h2 class="section-title">{{ t('ticketList.title') }}</h2>
        </div>
        <div class="toolbar-actions">
          <el-button type="primary" @click="router.push({ name: 'ticket-create' })">
            {{ t('ticketList.submitTicket') }}
          </el-button>
          <el-button plain @click="exportData">{{ t('common.export') }}</el-button>
        </div>
      </div>

      <el-form class="filter-grid" label-position="top">
        <el-form-item :label="t('ticketList.status')">
          <el-select v-model="filters.status" clearable :placeholder="t('ticketList.allStatus')">
            <el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>

        <el-form-item :label="t('ticketList.category')">
          <el-select v-model="filters.category" clearable :placeholder="t('ticketList.allCategory')">
            <el-option v-for="item in categoryOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>

        <el-form-item :label="t('ticketList.priority')">
          <el-select v-model="filters.priority" clearable :placeholder="t('ticketList.allPriority')">
            <el-option v-for="item in priorityOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>

        <el-form-item v-if="isAdmin || isDeveloper" :label="t('ticketList.assignee')">
          <el-select v-model="filters.assigneeId" clearable :placeholder="t('ticketList.allAssignee')">
            <el-option v-for="item in developers" :key="item.id" :label="item.name" :value="item.id" />
          </el-select>
        </el-form-item>

        <el-form-item :label="t('ticketList.keyword')">
          <el-input v-model="filters.keyword" clearable :placeholder="t('ticketList.keywordPlaceholder')" />
        </el-form-item>

        <el-form-item v-if="isDeveloper" :label="t('ticketList.quickFilter')">
          <el-switch v-model="filters.mine" :active-text="t('ticketList.mineOnly')" />
        </el-form-item>
      </el-form>

      <div class="toolbar" style="margin-top: 6px">
        <span class="section-subtitle">{{ t('ticketList.total', { count: pagination.total }) }}</span>
        <div class="toolbar-actions">
          <el-button @click="resetFilters">{{ t('common.reset') }}</el-button>
          <el-button type="primary" @click="pagination.page = 1; loadTickets()">{{ t('common.search') }}</el-button>
        </div>
      </div>
    </section>

    <section class="glass-card block table-block">
      <div class="table-wrap">
        <el-table height="100%" :data="tickets" v-loading="loading" :empty-text="t('common.noData')">
          <el-table-column prop="ticketNo" :label="t('ticketList.ticketNo')" width="168" show-overflow-tooltip />
          <el-table-column prop="title" :label="t('ticketList.titleColumn')" min-width="118" show-overflow-tooltip />
          <el-table-column :label="t('ticketList.category')" width="104" show-overflow-tooltip>
            <template #default="{ row }">{{ localizeCategory(row.category) }}</template>
          </el-table-column>
          <el-table-column :label="t('ticketList.priority')" width="84">
            <template #default="{ row }">
              <el-tag :type="priorityTagType(row.priority)">{{ localizePriority(row.priority) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column :label="t('ticketList.status')" width="92">
            <template #default="{ row }">
              <el-tag class="status-tag" :type="statusTagType(row.status)">{{ localizeStatus(row.status) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column :label="t('ticketList.creator')" width="88" show-overflow-tooltip>
            <template #default="{ row }">{{ row.creator?.name || '-' }}</template>
          </el-table-column>
          <el-table-column :label="t('ticketList.assignee')" width="88" show-overflow-tooltip>
            <template #default="{ row }">{{ row.assignee?.name || t('common.unassigned') }}</template>
          </el-table-column>
          <el-table-column :label="t('ticketList.updatedAt')" width="108">
            <template #default="{ row }">{{ formatListDateTime(row.updatedAt) }}</template>
          </el-table-column>
          <el-table-column :label="t('common.actions')" width="118" align="center">
            <template #default="{ row }">
              <div class="row-actions">
                <el-button link type="primary" @click="viewDetail(row)">{{ t('common.detail') }}</el-button>
                <el-dropdown v-if="getRowActions(row).length" @command="(command) => handleRowAction(command, row)">
                  <el-button link type="primary">{{ t('common.actions') }}</el-button>
                  <template #dropdown>
                    <el-dropdown-menu>
                      <el-dropdown-item
                        v-for="action in getRowActions(row)"
                        :key="action.command"
                        :command="action.command"
                      >
                        {{ action.label }}
                      </el-dropdown-item>
                    </el-dropdown-menu>
                  </template>
                </el-dropdown>
              </div>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <div class="pager">
        <el-pagination
          v-model:current-page="pagination.page"
          v-model:page-size="pagination.size"
          layout="total, sizes, prev, pager, next"
          :page-sizes="[5, 10, 20, 50]"
          :total="pagination.total"
          @current-change="loadTickets"
          @size-change="pagination.page = 1; loadTickets()"
        />
      </div>
    </section>

    <el-dialog v-model="assignDialog.visible" :title="t('ticketList.assignTitle')" width="420px">
      <el-form label-position="top">
        <el-form-item :label="t('ticketList.assignee')">
          <el-select v-model="assignDialog.assigneeId" :placeholder="t('ticketList.assignPlaceholder')">
            <el-option v-for="item in developers" :key="item.id" :label="item.name" :value="item.id" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="assignDialog.visible = false">{{ t('common.cancel') }}</el-button>
        <el-button type="primary" :disabled="!assignDialog.assigneeId" @click="submitAssign">
          {{ t('common.confirm') }}
        </el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="resolveDialog.visible" :title="t('ticketList.resolveTitle')" width="520px">
      <el-form label-position="top">
        <el-form-item :label="t('ticketList.resolveComment')">
          <el-input
            v-model="resolveDialog.comment"
            type="textarea"
            :rows="4"
            :placeholder="t('ticketList.resolvePlaceholder')"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="resolveDialog.visible = false">{{ t('common.cancel') }}</el-button>
        <el-button type="primary" @click="submitResolve">{{ t('common.confirm') }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.list-page {
  display: grid;
  grid-template-rows: auto minmax(0, 1fr);
  gap: 18px;
  height: 100%;
  min-height: 0;
}

.block {
  padding: 22px;
}

.table-block {
  display: flex;
  flex-direction: column;
  min-height: 0;
}

.table-wrap {
  flex: 1;
  min-height: 0;
}

.table-wrap :deep(.el-table) {
  height: 100%;
}

.table-wrap :deep(.el-table th .cell) {
  font-size: 16px;
  font-weight: 700;
}

.table-wrap :deep(.el-table .cell) {
  font-size: 15px;
}

.toolbar-actions {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.filter-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: 8px 14px;
  margin-top: 18px;
}

.row-actions {
  display: flex;
  justify-content: center;
  gap: 6px;
}

.pager {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
  flex-shrink: 0;
}

@media (max-width: 960px) {
  .list-page {
    height: auto;
    min-height: auto;
  }

  .table-block,
  .table-wrap {
    min-height: auto;
  }

  .table-wrap :deep(.el-table) {
    height: auto;
  }
}
</style>
