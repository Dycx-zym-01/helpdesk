<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'

import http from '@/api/http'
import { useAppStore } from '@/stores/app'
import { localizeRole, useI18n } from '@/i18n'

const appStore = useAppStore()
const { t } = useI18n()
const loading = ref(false)
const users = ref([])

const dialog = reactive({
  visible: false,
  mode: 'create'
})

const form = reactive({
  id: null,
  username: '',
  name: '',
  password: '',
  email: '',
  role: 'EMPLOYEE',
  enabled: true
})

const roleOptions = computed(() =>
  appStore.meta.roles.map((item) => ({
    ...item,
    label: localizeRole(item.value)
  }))
)

onMounted(async () => {
  if (!appStore.meta.roles.length) {
    await appStore.fetchMeta()
  }
  await loadUsers()
})

async function loadUsers() {
  loading.value = true
  try {
    const { data } = await http.get('/users')
    users.value = data
  } finally {
    loading.value = false
  }
}

function openCreate() {
  dialog.visible = true
  dialog.mode = 'create'
  form.id = null
  form.username = ''
  form.name = ''
  form.password = '123456'
  form.email = ''
  form.role = 'EMPLOYEE'
  form.enabled = true
}

function openEdit(row) {
  dialog.visible = true
  dialog.mode = 'edit'
  form.id = row.id
  form.username = row.username
  form.name = row.name
  form.password = ''
  form.email = row.email
  form.role = row.role
  form.enabled = row.enabled
}

async function submit() {
  if (dialog.mode === 'create') {
    await http.post('/users', {
      username: form.username,
      name: form.name,
      password: form.password,
      email: form.email,
      role: form.role
    })
    ElMessage.success(t('users.createSuccess'))
  } else {
    await http.put(`/users/${form.id}`, {
      name: form.name,
      password: form.password,
      email: form.email,
      role: form.role,
      enabled: form.enabled
    })
    ElMessage.success(t('users.updateSuccess'))
  }

  dialog.visible = false
  await loadUsers()
}
</script>

<template>
  <div class="users-page">
    <section class="glass-card block">
      <div class="toolbar">
        <div>
          <h2 class="section-title">{{ t('users.title') }}</h2>
        </div>
        <el-button type="primary" @click="openCreate">{{ t('users.addUser') }}</el-button>
      </div>
    </section>

    <section class="glass-card block">
      <el-table :data="users" v-loading="loading">
        <el-table-column prop="username" :label="t('users.username')" min-width="140" />
        <el-table-column prop="name" :label="t('users.name')" min-width="120" />
        <el-table-column prop="email" :label="t('users.email')" min-width="220" />
        <el-table-column :label="t('users.role')" min-width="120">
          <template #default="{ row }">{{ localizeRole(row.role) }}</template>
        </el-table-column>
        <el-table-column :label="t('users.status')" min-width="100">
          <template #default="{ row }">
            <el-tag :type="row.enabled ? 'success' : 'info'">
              {{ row.enabled ? t('common.enabled') : t('common.disabled') }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="t('common.actions')" width="140" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">{{ t('common.edit') }}</el-button>
          </template>
        </el-table-column>
      </el-table>
    </section>

    <el-dialog
      v-model="dialog.visible"
      :title="dialog.mode === 'create' ? t('users.createTitle') : t('users.editTitle')"
      width="520px"
    >
      <el-form label-position="top">
        <el-form-item v-if="dialog.mode === 'create'" :label="t('users.username')">
          <el-input v-model="form.username" />
        </el-form-item>
        <el-form-item :label="t('users.name')">
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item :label="t('users.email')">
          <el-input v-model="form.email" />
        </el-form-item>
        <div class="dialog-grid">
          <el-form-item :label="t('users.role')">
            <el-select v-model="form.role">
              <el-option v-for="item in roleOptions" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
          </el-form-item>
          <el-form-item :label="dialog.mode === 'create' ? t('users.initialPassword') : t('users.resetPassword')">
            <el-input v-model="form.password" />
          </el-form-item>
        </div>
        <el-form-item v-if="dialog.mode === 'edit'" :label="t('users.accountStatus')">
          <el-switch
            v-model="form.enabled"
            :active-text="t('common.enabled')"
            :inactive-text="t('common.disabled')"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialog.visible = false">{{ t('common.cancel') }}</el-button>
        <el-button type="primary" @click="submit">{{ t('common.save') }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.users-page {
  display: grid;
  gap: 18px;
}

.block {
  padding: 22px;
}

.dialog-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: 14px;
}
</style>
