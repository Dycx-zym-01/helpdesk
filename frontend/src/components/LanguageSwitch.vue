<script setup>
import { useI18n } from '@/i18n'

defineProps({
  floating: {
    type: Boolean,
    default: false
  }
})

const { locale, setLocale, t } = useI18n()
</script>

<template>
  <div class="language-switch glass-card" :class="{ floating }">
    <button
      class="lang-button"
      :class="{ active: locale === 'zh' }"
      type="button"
      @click="setLocale('zh')"
    >
      {{ t('language.zh') }}
    </button>
    <button
      class="lang-button"
      :class="{ active: locale === 'en' }"
      type="button"
      @click="setLocale('en')"
    >
      {{ t('language.en') }}
    </button>
  </div>
</template>

<style scoped>
.language-switch {
  display: inline-flex;
  gap: 6px;
  padding: 6px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.92);
}

.language-switch.floating {
  position: absolute;
  top: 22px;
  left: 22px;
  z-index: 20;
}

.lang-button {
  border: none;
  background: transparent;
  border-radius: 999px;
  padding: 8px 12px;
  color: var(--muted);
  font-weight: 700;
  cursor: pointer;
  transition: 0.2s ease;
}

.lang-button.active {
  background: var(--primary);
  color: white;
}
</style>
