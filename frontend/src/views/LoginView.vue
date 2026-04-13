<script setup>
import { computed, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'

import LanguageSwitch from '@/components/LanguageSwitch.vue'
import { useAuthStore } from '@/stores/auth'
import { useI18n } from '@/i18n'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()
const { t } = useI18n()

const loading = ref(false)
const form = reactive({
  username: 'admin',
  password: '123456'
})

const demoAccounts = computed(() => [
  { username: 'admin', password: '123456', role: t('login.demoAdmin') },
  { username: 'zhangsan', password: '123456', role: t('login.demoEmployee') },
  { username: 'wangwu', password: '123456', role: t('login.demoDeveloper') }
])

async function submit() {
  loading.value = true
  try {
    await authStore.login(form)
    ElMessage.success(t('login.success'))
    router.replace(route.query.redirect || '/')
  } finally {
    loading.value = false
  }
}

function fillAccount(account) {
  form.username = account.username
  form.password = account.password
}
</script>

<template>
  <div class="login-page">
    <LanguageSwitch floating />

    <div class="login-hero">
      <div class="hero-content">
        <span class="hero-badge">{{ t('login.badge') }}</span>
        <h1>{{ t('login.heroTitle') }}</h1>
      </div>
    </div>

    <div class="login-panel glass-card">
      <div class="login-head">
        <h2>{{ t('login.title') }}</h2>
        <p>{{ t('login.desc') }}</p>
      </div>

      <el-form label-position="top" @submit.prevent="submit">
        <el-form-item :label="t('login.username')">
          <el-input v-model="form.username" :placeholder="t('login.usernamePlaceholder')" />
        </el-form-item>
        <el-form-item :label="t('login.password')">
          <el-input v-model="form.password" show-password :placeholder="t('login.passwordPlaceholder')" />
        </el-form-item>
        <el-button type="primary" class="login-button" :loading="loading" @click="submit">
          {{ t('login.submit') }}
        </el-button>
      </el-form>

      <div class="demo-box">
        <div class="demo-title">{{ t('login.demoTitle') }}</div>
        <div class="demo-list">
          <button v-for="item in demoAccounts" :key="item.username" class="demo-item" @click="fillAccount(item)">
            <strong>{{ item.username }}</strong>
            <span>{{ item.role }}</span>
            <small>{{ item.password }}</small>
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.login-page {
  min-height: 100vh;
  position: relative;
  display: grid;
  grid-template-columns: minmax(0, 1.1fr) minmax(340px, 460px);
  background:
    radial-gradient(circle at left top, rgba(90, 143, 97, 0.28), transparent 34%),
    linear-gradient(135deg, #f5f7f1 0%, #edf3ec 52%, #e7eee4 100%);
}

.login-hero {
  display: flex;
  align-items: center;
  padding: 56px;
}

.hero-content {
  max-width: 620px;
}

.hero-badge {
  display: inline-flex;
  padding: 8px 14px;
  border-radius: 999px;
  background: rgba(47, 107, 79, 0.12);
  color: var(--primary);
  font-weight: 700;
}

.hero-content h1 {
  margin: 22px 0 18px;
  font-size: clamp(38px, 4vw, 64px);
  line-height: 1.08;
  letter-spacing: -0.03em;
}

.login-panel {
  align-self: center;
  margin: 32px;
  padding: 30px;
}

.login-head h2 {
  margin: 0 0 8px;
  font-size: 28px;
}

.login-head p {
  margin: 0 0 24px;
  color: var(--muted);
}

.login-button {
  width: 100%;
  height: 44px;
  margin-top: 8px;
}

.demo-box {
  margin-top: 22px;
  padding-top: 18px;
  border-top: 1px solid var(--border);
}

.demo-title {
  margin-bottom: 12px;
  font-weight: 700;
}

.demo-list {
  display: grid;
  gap: 10px;
}

.demo-item {
  border: 1px solid var(--border);
  border-radius: 18px;
  background: white;
  padding: 14px;
  text-align: left;
  cursor: pointer;
}

.demo-item span,
.demo-item small {
  display: block;
  color: var(--muted);
  margin-top: 4px;
}

@media (max-width: 960px) {
  .login-page {
    grid-template-columns: 1fr;
  }

  .login-hero {
    padding: 90px 24px 0;
  }

  .login-panel {
    margin: 24px;
  }
}
</style>
