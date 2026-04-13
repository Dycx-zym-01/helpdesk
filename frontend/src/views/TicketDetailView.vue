<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'

import http from '@/api/http'
import { useAuthStore } from '@/stores/auth'
import { useAppStore } from '@/stores/app'
import { formatDateTime, priorityTagType, statusTagType } from '@/utils/format'
import { FILE_BASE_URL } from '@/utils/runtime'
import { localizeCategory, localizePriority, localizeRole, localizeStatus, useI18n } from '@/i18n'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const appStore = useAppStore()
const { t } = useI18n()

const loading = ref(false)
const detail = ref(null)
const developers = ref([])
const commentText = ref('')
const commentUploadRef = ref()
const commentFileList = ref([])
const sendCommentEmail = ref(false)

const assignDialog = reactive({
  visible: false,
  assigneeId: null
})

const resolveDialog = reactive({
  visible: false,
  comment: ''
})

const isAdmin = computed(() => authStore.user?.role === 'ADMIN')
const isDeveloper = computed(() => authStore.user?.role === 'DEVELOPER')
const canAssign = computed(() => isAdmin.value && !['DRAFT', 'RESOLVED'].includes(detail.value?.status))
const canClaim = computed(
  () => isDeveloper.value && detail.value && !detail.value.assignee && detail.value.status === 'PENDING'
)
const canResolve = computed(
  () =>
    detail.value &&
    (((isDeveloper.value && detail.value.assignee?.id === authStore.user?.id) || isAdmin.value) && detail.value.status === 'PROCESSING')
)
const canSendCommentEmail = computed(
  () => Boolean(detail.value?.assignee) && ['PROCESSING', 'RESOLVED'].includes(detail.value?.status)
)
const canEditDraft = computed(
  () => detail.value?.status === 'DRAFT' && detail.value?.creator?.id === authStore.user?.id
)
const canDeleteDraft = computed(
  () => detail.value?.status === 'DRAFT' && ((detail.value?.creator?.id === authStore.user?.id) || isAdmin.value)
)
const canSubmitComment = computed(() => Boolean(commentText.value.trim()) || commentFileList.value.length > 0)

onMounted(async () => {
  if (!appStore.meta.categories.length) {
    await appStore.fetchMeta()
  }
  if (isAdmin.value) {
    const { data } = await http.get('/users', { params: { role: 'DEVELOPER' } })
    developers.value = data
  }
  await loadDetail()
})

async function loadDetail() {
  loading.value = true
  try {
    const { data } = await http.get(`/tickets/${route.params.id}`)
    detail.value = data
    assignDialog.assigneeId = data.assignee?.id || null
    if (!canSendCommentEmail.value) {
      sendCommentEmail.value = false
    }
  } finally {
    loading.value = false
  }
}

async function claim() {
  await http.post(`/tickets/${route.params.id}/claim`)
  ElMessage.success(t('ticketDetail.claimSuccess'))
  await loadDetail()
}

function editDraft() {
  router.push({ name: 'ticket-edit', params: { id: route.params.id } })
}

function handleCommentFileChange(_, files) {
  commentFileList.value = files
}

function resetCommentForm() {
  commentText.value = ''
  sendCommentEmail.value = false
  commentFileList.value = []
  commentUploadRef.value?.clearFiles()
}

async function submitComment() {
  const formData = new FormData()
  const requestSendEmail = sendCommentEmail.value && canSendCommentEmail.value
  formData.append('content', commentText.value.trim())
  formData.append('sendEmail', String(requestSendEmail))

  commentFileList.value.forEach((item) => {
    if (item.raw) {
      formData.append('files', item.raw)
    }
  })

  const { data } = await http.post(`/tickets/${route.params.id}/comments`, formData)

  detail.value = data.ticket
  assignDialog.assigneeId = data.ticket?.assignee?.id || null
  resetCommentForm()
  ElMessage.success(t('ticketDetail.commentSuccess'))
  if (data.emailDispatch?.requested && data.emailDispatch?.message) {
    if (data.emailDispatch.sent) {
      ElMessage.success(data.emailDispatch.message)
    } else {
      ElMessage.warning(data.emailDispatch.message)
    }
  }
}

async function submitAssign() {
  await http.post(`/tickets/${route.params.id}/assign`, {
    assigneeId: assignDialog.assigneeId
  })
  assignDialog.visible = false
  ElMessage.success(t('ticketDetail.assignSuccess'))
  await loadDetail()
}

async function submitResolve() {
  await http.post(`/tickets/${route.params.id}/resolve`, {
    comment: resolveDialog.comment
  })
  resolveDialog.visible = false
  ElMessage.success(t('ticketDetail.resolveSuccess'))
  await loadDetail()
}

async function deleteDraft() {
  try {
    await ElMessageBox.confirm(t('ticketList.deleteDraftConfirm'), t('ticketList.deleteDraft'))
  } catch {
    return
  }
  await http.delete(`/tickets/${route.params.id}`)
  ElMessage.success(t('ticketList.deleteDraftSuccess'))
  router.replace({ name: 'tickets' })
}
</script>

<template>
  <div v-loading="loading" class="detail-page">
    <section v-if="detail" class="glass-card detail-header">
      <div class="toolbar">
        <div>
          <div class="header-topline">{{ detail.ticketNo }}</div>
          <h2 class="section-title" style="margin-top: 8px">{{ detail.title }}</h2>
          <p class="section-subtitle">
            {{
              t('ticketDetail.createdAtBy', {
                createdAt: formatDateTime(detail.createdAt),
                assignee: detail.assignee?.name || t('common.unassigned')
              })
            }}
          </p>
        </div>

        <div class="detail-actions">
          <el-tag :type="priorityTagType(detail.priority)">{{ localizePriority(detail.priority) }}</el-tag>
          <el-tag class="status-tag" :type="statusTagType(detail.status)">{{ localizeStatus(detail.status) }}</el-tag>
          <el-button @click="router.push({ name: 'tickets' })">{{ t('common.backToList') }}</el-button>
          <el-button v-if="canEditDraft" plain @click="editDraft">{{ t('ticketList.editDraft') }}</el-button>
          <el-button v-if="canDeleteDraft" type="danger" plain @click="deleteDraft">
            {{ t('ticketList.deleteDraft') }}
          </el-button>
          <el-button v-if="canAssign" type="warning" @click="assignDialog.visible = true">
            {{ t('ticketList.assign') }}
          </el-button>
          <el-button v-if="canClaim" type="success" @click="claim">{{ t('ticketList.claim') }}</el-button>
          <el-button v-if="canResolve" type="success" @click="resolveDialog.visible = true">
            {{ t('ticketList.resolve') }}
          </el-button>
        </div>
      </div>
    </section>

    <section v-if="detail" class="content-grid">
      <div class="glass-card panel">
        <h3 class="panel-title">{{ t('ticketDetail.ticketInfo') }}</h3>
        <div class="meta-grid">
          <div>
            <span>{{ t('ticketList.category') }}</span>
            <strong>{{ localizeCategory(detail.category) }}</strong>
          </div>
          <div>
            <span>{{ t('ticketDetail.creator') }}</span>
            <strong>{{ detail.creator?.name || '-' }}</strong>
          </div>
          <div>
            <span>{{ t('ticketDetail.assignee') }}</span>
            <strong>{{ detail.assignee?.name || t('common.unassigned') }}</strong>
          </div>
          <div>
            <span>{{ t('ticketDetail.updatedAt') }}</span>
            <strong>{{ formatDateTime(detail.updatedAt) }}</strong>
          </div>
          <div>
            <span>{{ t('ticketDetail.resolvedAt') }}</span>
            <strong>{{ formatDateTime(detail.resolvedAt) }}</strong>
          </div>
        </div>

        <h3 class="panel-title" style="margin-top: 24px">{{ t('ticketDetail.description') }}</h3>
        <div class="description-box">{{ detail.description }}</div>

        <h3 class="panel-title" style="margin-top: 24px">{{ t('ticketDetail.attachments') }}</h3>
        <div v-if="detail.attachments.length" class="attachment-list">
          <a
            v-for="item in detail.attachments"
            :key="item.id"
            class="attachment-item"
            :href="`${FILE_BASE_URL}${item.url}`"
            target="_blank"
          >
            <strong>{{ item.originalName }}</strong>
            <span>
              {{
                t('ticketDetail.uploadedMeta', {
                  user: item.uploadedBy,
                  time: formatDateTime(item.uploadedAt)
                })
              }}
            </span>
          </a>
        </div>
        <div v-else class="empty-block">{{ t('ticketDetail.noAttachments') }}</div>
      </div>

      <div class="glass-card panel">
        <h3 class="panel-title">{{ t('ticketDetail.comments') }}</h3>
        <div v-if="detail.comments.length" class="comment-list">
          <div v-for="item in detail.comments" :key="item.id" class="comment-item">
            <div class="comment-head">
              <strong>{{ item.author?.name }}</strong>
              <span>
                {{
                  t('ticketDetail.authorMeta', {
                    role: localizeRole(item.author?.role),
                    time: formatDateTime(item.createdAt)
                  })
                }}
              </span>
            </div>
            <p v-if="item.content">{{ item.content }}</p>
            <div v-if="item.attachments?.length" class="comment-attachment-list">
              <a
                v-for="attachment in item.attachments"
                :key="attachment.id"
                class="attachment-item comment-attachment-item"
                :href="`${FILE_BASE_URL}${attachment.url}`"
                target="_blank"
              >
                <strong>{{ attachment.originalName }}</strong>
                <span>
                  {{
                    t('ticketDetail.uploadedMeta', {
                      user: attachment.uploadedBy,
                      time: formatDateTime(attachment.uploadedAt)
                    })
                  }}
                </span>
              </a>
            </div>
          </div>
        </div>
        <div v-else class="empty-block">{{ t('ticketDetail.noComments') }}</div>

        <div v-if="detail.status !== 'DRAFT'" class="comment-form">
          <el-input
            v-model="commentText"
            type="textarea"
            :rows="5"
            maxlength="500"
            show-word-limit
            :placeholder="t('ticketDetail.commentPlaceholder')"
          />
          <el-upload
            ref="commentUploadRef"
            class="comment-upload"
            drag
            multiple
            :auto-upload="false"
            :on-change="handleCommentFileChange"
            :file-list="commentFileList"
          >
            <div>{{ t('ticketDetail.commentAttachmentsHint') }}</div>
            <template #tip>
              <div class="section-subtitle">{{ t('ticketDetail.commentAttachmentsTip') }}</div>
            </template>
          </el-upload>
          <label class="comment-email-option">
            <el-checkbox v-model="sendCommentEmail" :disabled="!canSendCommentEmail">
              {{ t('ticketDetail.sendCommentEmail') }}
            </el-checkbox>
            <span>
              {{
                canSendCommentEmail
                  ? t('ticketDetail.sendCommentEmailTip')
                  : t('ticketDetail.sendCommentEmailClaimRequired')
              }}
            </span>
          </label>
          <div class="comment-actions">
            <el-button type="primary" :disabled="!canSubmitComment" @click="submitComment">
              {{ t('ticketDetail.addComment') }}
            </el-button>
          </div>
        </div>
      </div>
    </section>

    <el-dialog v-model="assignDialog.visible" :title="t('ticketDetail.assignTitle')" width="420px">
      <el-form label-position="top">
        <el-form-item :label="t('ticketDetail.assignee')">
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

    <el-dialog v-model="resolveDialog.visible" :title="t('ticketDetail.resolveTitle')" width="520px">
      <el-form label-position="top">
        <el-form-item :label="t('ticketDetail.resolveDialogComment')">
          <el-input
            v-model="resolveDialog.comment"
            type="textarea"
            :rows="4"
            :placeholder="t('ticketDetail.resolveDialogPlaceholder')"
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
.detail-page {
  display: grid;
  gap: 18px;
}

.detail-header,
.panel {
  padding: 24px;
}

.header-topline {
  color: var(--primary);
  font-weight: 700;
  letter-spacing: 0.08em;
}

.detail-actions {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  align-items: center;
}

.content-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.15fr) minmax(320px, 0.85fr);
  gap: 18px;
}

.panel-title {
  margin: 0 0 14px;
  font-size: 18px;
}

.meta-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(160px, 1fr));
  gap: 14px;
}

.meta-grid span {
  display: block;
  color: var(--muted);
  margin-bottom: 6px;
}

.description-box {
  padding: 16px;
  border-radius: 18px;
  background: rgba(47, 107, 79, 0.05);
  line-height: 1.8;
  white-space: pre-wrap;
}

.attachment-list,
.comment-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.attachment-item,
.comment-item {
  display: block;
  padding: 14px;
  border: 1px solid var(--border);
  border-radius: 18px;
  background: white;
}

.attachment-item span,
.comment-head span {
  color: var(--muted);
  font-size: 13px;
}

.comment-head {
  display: flex;
  justify-content: space-between;
  gap: 12px;
}

.comment-item p {
  margin: 10px 0 0;
  line-height: 1.7;
}

.comment-attachment-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin-top: 12px;
}

.comment-attachment-item {
  padding: 12px;
}

.comment-form {
  margin-top: 18px;
  padding-top: 18px;
  border-top: 1px solid var(--border);
}

.comment-upload {
  margin-top: 14px;
}

.comment-email-option {
  display: flex;
  flex-direction: column;
  gap: 6px;
  margin-top: 14px;
  color: var(--muted);
  font-size: 13px;
}

.comment-actions {
  display: flex;
  justify-content: flex-end;
  margin-top: 14px;
}

@media (max-width: 1100px) {
  .content-grid {
    grid-template-columns: 1fr;
  }
}
</style>
