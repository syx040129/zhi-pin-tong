<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import type { EChartsOption } from 'echarts'
import { useChart } from '@/utils/echarts'
import { adminOverview, adminTrends } from '@/api/admin'
import type { AdminOverview, AdminTrends } from '@/types/api'

defineOptions({ name: 'AdminDashboardView' })

const overviewLoading = ref(false)
const trendLoading = ref(false)
const overview = ref<AdminOverview | null>(null)
const trends = ref<AdminTrends | null>(null)
const days = ref(180)

type NumericKey =
  | 'totalUsers'
  | 'totalCompanies'
  | 'totalJobs'
  | 'totalApplications'
  | 'totalResumes'
  | 'totalInterviews'
  | 'pendingReports'

interface StatCardConfig {
  label: string
  key: NumericKey
  icon: string
  color: string
}

const statCards: StatCardConfig[] = [
  { label: '用户总数', key: 'totalUsers', icon: 'User', color: '#3159d8' },
  { label: '企业总数', key: 'totalCompanies', icon: 'OfficeBuilding', color: '#5677c9' },
  { label: '岗位总数', key: 'totalJobs', icon: 'Briefcase', color: '#6d77a3' },
  { label: '投递总数', key: 'totalApplications', icon: 'Tickets', color: '#ac690d' },
  { label: '简历总数', key: 'totalResumes', icon: 'Document', color: '#187a66' },
  { label: 'AI 面试', key: 'totalInterviews', icon: 'ChatDotRound', color: '#6366f1' },
  { label: '待处理举报', key: 'pendingReports', icon: 'Warning', color: '#c94444' },
]

function statValue(key: NumericKey): number {
  const value = overview.value?.[key]
  return typeof value === 'number' ? value : 0
}

const chartPalette = ['#3159d8', '#5677c9', '#6d77a3', '#ac690d', '#187a66', '#c94444']

function pad(num: number): string {
  return String(num).padStart(2, '0')
}

function dayKey(date: Date): string {
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`
}

const dayDates = computed(() => {
  const result: string[] = []
  const now = new Date()
  for (let i = days.value - 1; i >= 0; i -= 1) {
    const d = new Date(now)
    d.setDate(now.getDate() - i)
    result.push(dayKey(d))
  }
  return result
})

function buildMap(points: { date: string; count: number }[]): Map<string, number> {
  const map = new Map<string, number>()
  for (const point of points ?? []) {
    const key = (point.date || '').slice(0, 10)
    if (key) map.set(key, Number(point.count) || 0)
  }
  return map
}

const usersMap = computed(() => buildMap(trends.value?.users ?? []))
const jobsMap = computed(() => buildMap(trends.value?.jobs ?? []))
const applicationsMap = computed(() => buildMap(trends.value?.applications ?? []))

const userSeries = computed(() => dayDates.value.map((d) => usersMap.value.get(d) ?? 0))
const jobSeries = computed(() => dayDates.value.map((d) => jobsMap.value.get(d) ?? 0))
const applicationSeries = computed(() => dayDates.value.map((d) => applicationsMap.value.get(d) ?? 0))

const lineOption = computed<EChartsOption>(() => ({
  color: ['#3159d8', '#5677c9', '#ac690d'],
  tooltip: { trigger: 'axis' },
  legend: { top: 0, right: 0, icon: 'circle', itemWidth: 8, textStyle: { color: '#6b7280' } },
  grid: { left: 10, right: 18, top: 42, bottom: 10, containLabel: true },
  xAxis: {
    type: 'category',
    boundaryGap: false,
    data: dayDates.value,
    axisTick: { show: false },
    axisLine: { lineStyle: { color: '#d7dcec' } },
    axisLabel: { color: '#9ca3af', fontSize: 11 },
  },
  yAxis: {
    type: 'value',
    minInterval: 1,
    splitLine: { lineStyle: { color: '#eef1f8' } },
    axisLabel: { color: '#9ca3af' },
  },
  series: [
    {
      name: '新增用户',
      type: 'line',
      smooth: true,
      showSymbol: false,
      data: userSeries.value,
      areaStyle: {
        color: {
          type: 'linear',
          x: 0,
          y: 0,
          x2: 0,
          y2: 1,
          colorStops: [
            { offset: 0, color: 'rgba(79, 107, 246, 0.28)' },
            { offset: 1, color: 'rgba(79, 107, 246, 0.02)' },
          ],
        },
      },
    },
    {
      name: '新增岗位',
      type: 'line',
      smooth: true,
      showSymbol: false,
      data: jobSeries.value,
      areaStyle: {
        color: {
          type: 'linear',
          x: 0,
          y: 0,
          x2: 0,
          y2: 1,
          colorStops: [
            { offset: 0, color: 'rgba(34, 211, 238, 0.25)' },
            { offset: 1, color: 'rgba(34, 211, 238, 0.02)' },
          ],
        },
      },
    },
    {
      name: '新增投递',
      type: 'line',
      smooth: true,
      showSymbol: false,
      data: applicationSeries.value,
      areaStyle: {
        color: {
          type: 'linear',
          x: 0,
          y: 0,
          x2: 0,
          y2: 1,
          colorStops: [
            { offset: 0, color: 'rgba(245, 158, 11, 0.2)' },
            { offset: 1, color: 'rgba(245, 158, 11, 0.02)' },
          ],
        },
      },
    },
  ],
}))

const pieOption = computed<EChartsOption>(() => {
  const items = (overview.value?.jobTypeDistribution ?? []).filter((item) => (Number(item.value) || 0) > 0)
  return {
    color: chartPalette,
    tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
    legend: { bottom: 0, type: 'scroll', icon: 'circle', textStyle: { color: '#6b7280' } },
    series: [
      {
        name: '岗位类型分布',
        type: 'pie',
        radius: ['40%', '66%'],
        center: ['50%', '46%'],
        itemStyle: { borderColor: '#fff', borderWidth: 2, borderRadius: 6 },
        label: { show: false },
        data: items.map((item) => ({ name: item.name, value: item.value })),
      },
    ],
  }
})

const companyTopOption = computed<EChartsOption>(() => {
  const items = [...(overview.value?.companyJobTop ?? [])].reverse()
  return {
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    grid: { left: 10, right: 30, top: 10, bottom: 10, containLabel: true },
    xAxis: {
      type: 'value',
      minInterval: 1,
      splitLine: { lineStyle: { color: '#eef1f8' } },
      axisLabel: { color: '#9ca3af' },
    },
    yAxis: {
      type: 'category',
      data: items.map((item) => item.name),
      axisTick: { show: false },
      axisLine: { show: false },
      axisLabel: { color: '#6b7280', width: 110, overflow: 'truncate' },
    },
    series: [
      {
        name: '在线岗位数',
        type: 'bar',
        barMaxWidth: 16,
        data: items.map((item) => item.value),
        itemStyle: {
          borderRadius: [0, 8, 8, 0],
          color: {
            type: 'linear',
            x: 0,
            y: 0,
            x2: 1,
            y2: 0,
            colorStops: [
              { offset: 0, color: '#3159d8' },
              { offset: 1, color: '#5677c9' },
            ],
          },
        },
      },
    ],
  }
})

const { elRef: lineRef } = useChart(lineOption)
const { elRef: pieRef } = useChart(pieOption)
const { elRef: topRef } = useChart(companyTopOption)

async function fetchOverview(): Promise<void> {
  overviewLoading.value = true
  try {
    overview.value = await adminOverview()
  } catch {
    // 拦截器已提示
  } finally {
    overviewLoading.value = false
  }
}

async function fetchTrends(): Promise<void> {
  trendLoading.value = true
  try {
    trends.value = await adminTrends(days.value)
  } catch {
    // 拦截器已提示
  } finally {
    trendLoading.value = false
  }
}

watch(days, () => {
  void fetchTrends()
})

onMounted(() => {
  void fetchOverview()
  void fetchTrends()
})
</script>

<template>
  <div class="admin-dash">
    <div class="dash-hero">
      <div>
        <div class="dash-hero__title">智聘AI 平台数据看板</div>
        <div class="dash-hero__sub">实时掌握平台用户、企业与招聘动态</div>
      </div>
      <el-tag size="large" effect="dark" round class="dash-hero__tag">平台运营总览</el-tag>
    </div>

    <div v-loading="overviewLoading" class="stat-grid">
      <div v-for="card in statCards" :key="card.key" class="stat-card">
        <div class="stat-card__icon" :style="{ background: card.color + '12', color: card.color }">
          <el-icon :size="21"><component :is="card.icon" /></el-icon>
        </div>
        <div class="stat-card__meta">
          <div class="stat-card__value">{{ statValue(card.key) }}</div>
          <div class="stat-card__label">{{ card.label }}</div>
        </div>
      </div>
    </div>

    <div class="panel trend-panel">
      <div class="panel-head">
        <div>
          <div class="panel-head__title">增长趋势</div>
          <div class="panel-head__sub">按自然日统计最近 {{ days }} 天用户 / 岗位 / 投递的新增数量</div>
        </div>
        <el-radio-group v-model="days" size="small">
          <el-radio-button :value="30">30天</el-radio-button>
          <el-radio-button :value="90">90天</el-radio-button>
          <el-radio-button :value="180">180天</el-radio-button>
          <el-radio-button :value="365">365天</el-radio-button>
        </el-radio-group>
      </div>
      <div v-loading="trendLoading" class="chart-box">
        <div ref="lineRef" class="chart chart--line"></div>
      </div>
    </div>

    <div class="chart-row">
      <div class="panel chart-panel">
        <div class="panel-head">
          <div class="panel-head__title">岗位类型分布</div>
        </div>
        <div class="chart-box">
          <div ref="pieRef" class="chart"></div>
          <div v-if="!(overview?.jobTypeDistribution ?? []).some((i) => i.value > 0)" class="chart-empty">
            <el-empty description="暂无岗位数据" :image-size="80" />
          </div>
        </div>
      </div>

      <div class="panel chart-panel">
        <div class="panel-head">
          <div class="panel-head__title">企业在线岗位 Top6</div>
        </div>
        <div class="chart-box">
          <div ref="topRef" class="chart"></div>
          <div v-if="!(overview?.companyJobTop ?? []).length" class="chart-empty">
            <el-empty description="暂无企业岗位数据" :image-size="80" />
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.admin-dash {
  min-height: 100%;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.dash-hero {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20px 26px;
  border-radius: 16px;
  background:
    radial-gradient(900px 180px at 85% -30%, rgba(34, 211, 238, 0.22), transparent 60%),
    linear-gradient(120deg, #0b1220 0%, #182a55 100%);
  box-shadow: var(--shadow-md);
}

.dash-hero__title {
  color: #fff;
  font-size: 19px;
  font-weight: 700;
}

.dash-hero__sub {
  margin-top: 6px;
  color: #9fb0d6;
  font-size: 13px;
}

.dash-hero__tag {
  background: var(--c-primary);
  border: none;
}

.stat-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(170px, 1fr));
  gap: 14px;
}

.stat-card {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px;
  background: #fff;
  border: 1px solid var(--c-border);
  border-radius: var(--radius);
  box-shadow: var(--shadow-sm);
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.stat-card:hover {
  
  box-shadow: var(--shadow-md);
}

.stat-card__icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 42px;
  height: 42px;
  border-radius: 12px;
  color: #fff;
  flex: none;
}

.stat-card__value {
  font-size: 21px;
  font-weight: 700;
  color: var(--c-text);
  line-height: 1.15;
}

.stat-card__label {
  margin-top: 3px;
  font-size: 12px;
  color: var(--c-text-3);
}

.trend-panel,
.chart-panel {
  padding: 18px 20px;
  min-width: 0;
}

.panel-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 8px;
}

.panel-head__title {
  font-size: 15px;
  font-weight: 600;
  color: var(--c-text);
}

.panel-head__sub {
  margin-top: 3px;
  font-size: 12px;
  color: var(--c-text-3);
}

.chart-box {
  position: relative;
}

.chart {
  width: 100%;
}

.chart--line {
  height: 320px;
}

.chart-panel .chart {
  height: 300px;
}

.chart-empty {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(255, 255, 255, 0.72);
  pointer-events: none;
}

.chart-row {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(380px, 1fr));
  gap: 16px;
}

@media (max-width: 900px) {
  .dash-hero {
    flex-direction: column;
    align-items: flex-start;
    gap: 10px;
  }

  .panel-head {
    flex-direction: column;
  }
}
</style>
