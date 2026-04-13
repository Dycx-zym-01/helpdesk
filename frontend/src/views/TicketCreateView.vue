<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'

import http from '@/api/http'
import { useAppStore } from '@/stores/app'
import { localizeCategory, localizePriority, useI18n } from '@/i18n'
import { FILE_BASE_URL } from '@/utils/runtime'

const route = useRoute()
const router = useRouter()
const appStore = useAppStore()
const { t } = useI18n()

const loading = ref(false)
const uploadRef = ref()
const fileList = ref([])
const draftDetail = ref(null)

const form = reactive({
  title: '',
  category: '',
  priority: 'MEDIUM',
  description: ''
})

const isEditMode = computed(() => Boolean(route.params.id))
const pageTitle = computed(() => (isEditMode.value ? t('ticketCreate.editTitle') : t('ticketCreate.title')))
const submitLabel = computed(() => (isEditMode.value ? t('ticketCreate.submitDraft') : t('ticketCreate.submit')))
const existingAttachments = computed(() => draftDetail.value?.attachments || [])

const categories = computed(() =>
  appStore.meta.categories.map((item) => ({
    ...item,
    label: localizeCategory(item.value)
  }))
)

const priorities = computed(() =>
  appStore.meta.priorities.map((item) => ({
    ...item,
    label: localizePriority(item.value)
  }))
)

onMounted(async () => {
  if (!appStore.meta.categories.length) {
    await appStore.fetchMeta()
  }

  if (isEditMode.value) {
    await loadDraft()
  } else if (!form.category && categories.value.length) {
    form.category = categories.value[0].value
  }
})

function handleChange(_, files) {
  fileList.value = files
}

async function loadDraft() {
  loading.value = true
  try {
    const { data } = await http.get(`/tickets/${route.params.id}`)
    if (data.status !== 'DRAFT') {
      router.replace({ name: 'ticket-detail', params: { id: data.id } })
      return
    }
    draftDetail.value = data
    form.title = data.title || ''
    form.category = data.category || categories.value[0]?.value || ''
    form.priority = data.priority || 'MEDIUM'
    form.description = data.description || ''
  } finally {
    loading.value = false
  }
}

function appendFormData(formData) {
  formData.append('title', form.title)
  formData.append('category', form.category)
  formData.append('priority', form.priority)
  formData.append('description', form.description)

  fileList.value.forEach((item) => {
    if (item.raw) {
      formData.append('files', item.raw)
    }
  })
}

function resetPendingUploads() {
  fileList.value = []
  uploadRef.value?.clearFiles()
}

async function saveDraft() {
  loading.value = true
  try {
    const formData = new FormData()
    appendFormData(formData)

    const { data } = isEditMode.value
      ? await http.post(`/tickets/${route.params.id}/draft`, formData, {
          headers: { 'Content-Type': 'multipart/form-data' }
        })
      : await http.post('/tickets/drafts', formData, {
          headers: { 'Content-Type': 'multipart/form-data' }
        })

    draftDetail.value = data
    resetPendingUploads()
    ElMessage.success(t('ticketCreate.saveDraftSuccess'))
    if (!isEditMode.value) {
      router.replace({ name: 'ticket-edit', params: { id: data.id } })
    }
  } finally {
    loading.value = false
  }
}

async function submit() {
  loading.value = true
  try {
    const formData = new FormData()
    appendFormData(formData)

    const { data } = isEditMode.value
      ? await http.post(`/tickets/${route.params.id}/submit`, formData, {
          headers: { 'Content-Type': 'multipart/form-data' }
        })
      : await http.post('/tickets', formData, {
          headers: { 'Content-Type': 'multipart/form-data' }
        })

    resetPendingUploads()
    ElMessage.success(t('ticketCreate.success'))
    router.push({ name: 'ticket-detail', params: { id: data.id } })
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="create-page glass-card" v-loading="loading">
    <div class="toolbar">
      <div>
        <h2 class="section-title">{{ pageTitle }}</h2>
      </div>
      <el-button @click="router.push({ name: 'tickets' })">{{ t('common.backToList') }}</el-button>
    </div>

    <el-form label-position="top" class="create-form">
      <el-form-item :label="t('ticketCreate.ticketTitle')" required>
        <el-input
          v-model="form.title"
          maxlength="100"
          show-word-limit
          :placeholder="t('ticketCreate.ticketTitlePlaceholder')"
        />
      </el-form-item>

      <div class="double-grid">
        <el-form-item :label="t('ticketCreate.category')" required>
          <el-select v-model="form.category" clearable :placeholder="t('ticketCreate.categoryPlaceholder')">
            <el-option v-for="item in categories" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>

        <el-form-item :label="t('ticketCreate.priority')" required>
          <el-select v-model="form.priority" :placeholder="t('ticketCreate.priorityPlaceholder')">
            <el-option v-for="item in priorities" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
      </div>

      <el-form-item :label="t('ticketCreate.description')" required>
        <el-input
          v-model="form.description"
          type="textarea"
          :rows="8"
          :placeholder="t('ticketCreate.descriptionPlaceholder')"
        />
      </el-form-item>

      <el-form-item :label="t('ticketCreate.attachments')">
        <div v-if="existingAttachments.length" class="existing-attachments">
          <div class="existing-title">{{ t('ticketCreate.existingAttachments') }}</div>
          <div class="existing-list">
            <a
              v-for="item in existingAttachments"
              :key="item.id"
              class="existing-item"
              :href="`${FILE_BASE_URL}${item.url}`"
              target="_blank"
            >
              {{ item.originalName }}
            </a>
          </div>
        </div>

        <el-upload
          ref="uploadRef"
          drag
          multiple
          :auto-upload="false"
          :on-change="handleChange"
          :file-list="fileList"
        >
          <div>{{ t('ticketCreate.attachmentsHint') }}</div>
          <template #tip>
            <div class="section-subtitle">{{ t('ticketCreate.attachmentsTip') }}</div>
          </template>
        </el-upload>
      </el-form-item>

      <div class="submit-actions">
        <el-button @click="router.push({ name: 'tickets' })">{{ t('common.cancel') }}</el-button>
        <el-button plain :loading="loading" @click="saveDraft">{{ t('ticketCreate.saveDraft') }}</el-button>
        <el-button type="primary" :loading="loading" @click="submit">{{ submitLabel }}</el-button>
      </div>
    </el-form>
  </div>
</template>

<style scoped>
.create-page {
  padding: 24px;
}

.create-form {
  margin-top: 22px;
}

.double-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 16px;
}

.existing-attachments {
  margin-bottom: 14px;
}

.existing-title {
  margin-bottom: 8px;
  font-size: 14px;
  color: var(--muted);
}

.existing-list {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.existing-item {
  padding: 8px 12px;
  border: 1px solid var(--border);
  border-radius: 999px;
  background: white;
}

.submit-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 24px;
}
</style>
