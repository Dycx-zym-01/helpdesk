<script setup>
import { computed } from 'vue'
import { ElConfigProvider } from 'element-plus'
import en from 'element-plus/es/locale/lang/en'
import zhCn from 'element-plus/es/locale/lang/zh-cn'

import { useI18n } from '@/i18n'

const { locale } = useI18n()

const elementLocale = computed(() => (locale.value === 'en' ? en : zhCn))
</script>

<template>
  <el-config-provider :locale="elementLocale">
    <router-view />
  </el-config-provider>
</template>
