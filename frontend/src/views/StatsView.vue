<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import * as echarts from 'echarts'

import http from '@/api/http'
import { localizeCategory, useI18n } from '@/i18n'

const { locale, t } = useI18n()
const loading = ref(false)
const overview = ref({
  totalTickets: 0,
  resolvedTickets: 0,
  unresolvedTickets: 0,
  resolvedRate: 0,
  averageResolutionHours: 0,
  categoryDistribution: [],
  statusDistribution: [],
  developerMetrics: [],
  trendPoints: []
})

const categoryChartRef = ref()
const trendChartRef = ref()
const devChartRef = ref()

let categoryChart
let trendChart
let devChart

const cards = computed(() => [
  { label: t('stats.total'), value: overview.value.totalTickets },
  { label: t('stats.resolved'), value: overview.value.resolvedTickets },
  { label: t('stats.unresolved'), value: overview.value.unresolvedTickets },
  { label: t('stats.resolvedRate'), value: `${formatNumber(overview.value.resolvedRate)}%` },
  {
    label: t('stats.averageHours'),
    value: t('stats.hoursUnit', { value: formatNumber(overview.value.averageResolutionHours) })
  }
])

onMounted(async () => {
  await loadOverview()
  window.addEventListener('resize', resizeCharts)
})

watch(locale, async () => {
  await nextTick()
  if (categoryChartRef.value && trendChartRef.value && devChartRef.value) {
    renderCharts()
  }
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', resizeCharts)
  categoryChart?.dispose()
  trendChart?.dispose()
  devChart?.dispose()
})

async function loadOverview() {
  loading.value = true
  try {
    const { data } = await http.get('/statistics/overview')
    overview.value = data
    await nextTick()
    renderCharts()
  } finally {
    loading.value = false
  }
}

function formatNumber(value) {
  return Number(value || 0).toFixed(1).replace(/\.0$/, '')
}

function renderCharts() {
  categoryChart?.dispose()
  trendChart?.dispose()
  devChart?.dispose()

  categoryChart = echarts.init(categoryChartRef.value)
  trendChart = echarts.init(trendChartRef.value)
  devChart = echarts.init(devChartRef.value)

  categoryChart.setOption({
    tooltip: { trigger: 'item' },
    series: [
      {
        type: 'pie',
        radius: ['42%', '68%'],
        itemStyle: { borderRadius: 12, borderColor: '#fff', borderWidth: 4 },
        data: overview.value.categoryDistribution.map((item) => ({
          name: localizeCategory(item.name),
          value: item.value
        }))
      }
    ]
  })

  trendChart.setOption({
    tooltip: { trigger: 'axis' },
    legend: { data: [t('stats.newTickets'), t('stats.resolvedTickets')] },
    xAxis: {
      type: 'category',
      data: overview.value.trendPoints.map((item) => item.label.slice(5))
    },
    yAxis: { type: 'value' },
    series: [
      {
        name: t('stats.newTickets'),
        type: 'line',
        smooth: true,
        data: overview.value.trendPoints.map((item) => item.createdCount),
        areaStyle: { opacity: 0.15 }
      },
      {
        name: t('stats.resolvedTickets'),
        type: 'line',
        smooth: true,
        data: overview.value.trendPoints.map((item) => item.resolvedCount)
      }
    ]
  })

  devChart.setOption({
    tooltip: { trigger: 'axis' },
    xAxis: {
      type: 'category',
      data: overview.value.developerMetrics.map((item) => item.userName)
    },
    yAxis: { type: 'value' },
    series: [
      {
        name: t('stats.handledHours'),
        type: 'bar',
        barWidth: 28,
        data: overview.value.developerMetrics.map((item) => item.handledHours),
        itemStyle: { color: '#2f6b4f', borderRadius: [8, 8, 0, 0] }
      }
    ]
  })
}

function resizeCharts() {
  categoryChart?.resize()
  trendChart?.resize()
  devChart?.resize()
}
</script>

<template>
  <div class="stats-page" v-loading="loading">
    <section class="glass-card block">
      <div class="toolbar">
        <div>
          <h2 class="section-title">{{ t('stats.title') }}</h2>
        </div>
        <el-button @click="loadOverview">{{ t('common.refresh') }}</el-button>
      </div>

      <div class="stat-grid" style="margin-top: 18px">
        <div v-for="item in cards" :key="item.label" class="stat-card">
          <span>{{ item.label }}</span>
          <strong>{{ item.value }}</strong>
        </div>
      </div>
    </section>

    <section class="chart-grid">
      <div class="glass-card chart-card">
        <div class="chart-title">{{ t('stats.categoryChart') }}</div>
        <div ref="categoryChartRef" class="chart-box"></div>
      </div>

      <div class="glass-card chart-card">
        <div class="chart-title">{{ t('stats.developerChart') }}</div>
        <div ref="devChartRef" class="chart-box"></div>
      </div>
    </section>

    <section class="glass-card chart-card">
      <div class="chart-title">{{ t('stats.trendChart') }}</div>
      <div ref="trendChartRef" class="chart-box wide"></div>
    </section>

    <section class="glass-card block">
      <div class="toolbar" style="margin-bottom: 14px">
        <span class="section-title" style="font-size: 18px; margin: 0">{{ t('stats.developerDetails') }}</span>
        <span class="section-subtitle">{{ t('stats.developerDetailsSub') }}</span>
      </div>

      <el-table :data="overview.developerMetrics">
        <el-table-column prop="userName" :label="t('stats.developerName')" />
        <el-table-column prop="handledHours" :label="t('stats.handledHoursColumn')" />
        <el-table-column prop="resolvedCount" :label="t('stats.resolvedCount')" />
        <el-table-column prop="processingCount" :label="t('stats.processingCount')" />
        <el-table-column prop="averageHours" :label="t('stats.averageHoursColumn')" />
      </el-table>
    </section>
  </div>
</template>

<style scoped>
.stats-page {
  display: grid;
  gap: 18px;
}

.block,
.chart-card {
  padding: 22px;
}

.chart-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(320px, 1fr));
  gap: 18px;
}

.stat-card {
  padding: 18px;
  border-radius: 20px;
  border: 1px solid var(--border);
  background: white;
}

.stat-card span {
  display: block;
  color: var(--muted);
  margin-bottom: 12px;
}

.stat-card strong {
  font-size: 28px;
}

.chart-title {
  margin-bottom: 12px;
  font-size: 18px;
  font-weight: 700;
}

.chart-box {
  height: 320px;
}

.chart-box.wide {
  height: 360px;
}
</style>
