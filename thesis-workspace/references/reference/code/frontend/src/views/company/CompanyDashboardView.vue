<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import type { EChartsOption } from 'echarts'
import { useChart } from '@/utils/echarts'
import { companyStats } from '@/api/company'
import type { CompanyStats } from '@/types/api'
import { applicationStatusMap } from '@/utils/format'
import { useAuthStore } from '@/stores/auth'

defineOptions({ name: 'CompanyDashboardView' })

const router = useRouter()
const auth = useAuthStore()

const loading = ref(false)
const stats = ref<CompanyStats | null>(null)

interface StatCard {
  label: string
  value: number
  icon: string
  tone: string
}

const cards = computed<StatCard[]>(() => {
  const s = stats.value
  return [
    { label: '岗位总数', value: s?.jobCount ?? 0, icon: 'Briefcase', tone: '#3159d8' },
    { label: '在线岗位', value: s?.onlineJobCount ?? 0, icon: 'Monitor', tone: '#5677c9' },
    { label: '收到简历', value: s?.applicationCount ?? 0, icon: 'Document', tone: '#6d77a3' },
    { label: '待处理', value: s?.pendingCount ?? 0, icon: 'AlarmClock', tone: '#ac690d' },
    { label: '面试人数', value: s?.interviewCount ?? 0, icon: 'ChatDotRound', tone: '#187a66' },
    { label: '录用人数', value: s?.hiredCount ?? 0, icon: 'Medal', tone: '#c94444' },
  ]
})

const chartPalette = ['#3159d8', '#5677c9', '#6d77a3', '#ac690d', '#187a66', '#c94444']

function statusLabel(name: string): string {
  return applicationStatusMap[name]?.label ?? name
}

const statusItems = computed(() =>
  (stats.value?.applicationStatus ?? []).map((s) => ({
    label: statusLabel(s.name),
    value: Number(s.value) || 0,
  })),
)

const hasStatusData = computed(() => statusItems.value.some((item) => item.value > 0))

const pieOption = computed<EChartsOption>(() => ({
  color: chartPalette,
  tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
  legend: { bottom: 0, icon: 'circle', type: 'scroll', textStyle: { color: '#6b7280' } },
  series: [
    {
      name: '投递状态分布',
      type: 'pie',
      radius: ['46%', '70%'],
      center: ['50%', '44%'],
      itemStyle: { borderColor: '#fff', borderWidth: 2, borderRadius: 6 },
      label: { show: false },
      data: statusItems.value
        .filter((item) => item.value > 0)
        .map((item) => ({ name: item.label, value: item.value })),
    },
  ],
}))

const barOption = computed<EChartsOption>(() => ({
  tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
  grid: { left: 8, right: 16, top: 30, bottom: 8, containLabel: true },
  xAxis: {
    type: 'category',
    data: statusItems.value.map((item) => item.label),
    axisTick: { show: false },
    axisLine: { lineStyle: { color: '#d7dcec' } },
    axisLabel: { color: '#6b7280' },
  },
  yAxis: {
    type: 'value',
    minInterval: 1,
    splitLine: { lineStyle: { color: '#eef1f8' } },
    axisLabel: { color: '#6b7280' },
  },
  series: [
    {
      name: '投递数',
      type: 'bar',
      barMaxWidth: 36,
      itemStyle: {
        borderRadius: [6, 6, 0, 0],
        color: {
          type: 'linear',
          x: 0,
          y: 0,
          x2: 0,
          y2: 1,
          colorStops: [
            { offset: 0, color: '#3159d8' },
            { offset: 1, color: '#5677c9' },
          ],
        },
      },
      data: statusItems.value.map((item) => item.value),
    },
  ],
}))

const { elRef: pieRef } = useChart(pieOption)
const { elRef: barRef } = useChart(barOption)

async function fetchStats(): Promise<void> {
  loading.value = true
  try {
    stats.value = await companyStats()
  } catch {
    // 错误提示由请求拦截器统一处理
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  void fetchStats()
})
</script>

<template>
  <div v-loading="loading" class="dash-page">
    <div class="hero">
      <div class="hero__left">
        <div class="hero__title">
          欢迎回来，<span class="grad-text">{{
            auth.user?.companyName || auth.user?.nickname || '企业管理员'
          }}</span>
        </div>
        <div class="hero__sub">这里是你的招聘数据总览，AI 匹配助你高效找到合适的人才</div>
      </div>
      <div class="hero__actions">
        <button type="button" class="grad-btn" @click="router.push('/company/jobs/create')">
          <el-icon style="margin-right: 5px"><CirclePlus /></el-icon>发布岗位
        </button>
        <button type="button" class="ghost-btn" @click="router.push('/company/applications')">
          <el-icon style="margin-right: 5px"><Tickets /></el-icon>查看投递
        </button>
      </div>
    </div>

    <div class="cards">
      <div v-for="card in cards" :key="card.label" class="stat-card">
        <div class="stat-card__icon" :style="{ background: card.tone + '12', color: card.tone }">
          <el-icon :size="22"><component :is="card.icon" /></el-icon>
        </div>
        <div class="stat-card__meta">
          <div class="stat-card__value">{{ card.value }}</div>
          <div class="stat-card__label">{{ card.label }}</div>
        </div>
      </div>
    </div>

    <div class="charts">
      <div class="panel chart-card">
        <div class="chart-card__head">
          <div class="chart-card__title">投递状态分布</div>
          <el-tag size="small" effect="plain">占比</el-tag>
        </div>
        <div class="chart-box">
          <div ref="pieRef" class="chart"></div>
          <div v-if="!hasStatusData" class="chart-empty">
            <el-empty description="暂无投递数据" :image-size="80" />
          </div>
        </div>
      </div>

      <div class="panel chart-card">
        <div class="chart-card__head">
          <div class="chart-card__title">各状态投递数量</div>
          <el-tag size="small" effect="plain">数量</el-tag>
        </div>
        <div class="chart-box">
          <div ref="barRef" class="chart"></div>
          <div v-if="!hasStatusData" class="chart-empty">
            <el-empty description="暂无投递数据" :image-size="80" />
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.dash-page {
  min-height: 100%;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.hero {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 22px 26px;
  border-radius: 16px;
  background:
    radial-gradient(1000px 200px at 90% -20%, rgba(34, 211, 238, 0.25), transparent 60%),
    linear-gradient(120deg, #101c42 0%, #1a2c63 100%);
  box-shadow: var(--shadow-md);
}

.hero__title {
  color: #fff;
  font-size: 20px;
  font-weight: 700;
  letter-spacing: 0.3px;
}

.hero__sub {
  margin-top: 8px;
  color: #aab6d8;
  font-size: 13px;
}

.hero__actions {
  display: flex;
  align-items: center;
  gap: 12px;
  flex: none;
}

.ghost-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  height: 38px;
  padding: 0 18px;
  border: 1px solid rgba(255, 255, 255, 0.35);
  border-radius: 10px;
  cursor: pointer;
  color: #fff;
  font-size: 14px;
  background: rgba(255, 255, 255, 0.1);
  transition: background-color 0.2s ease;
}

.ghost-btn:hover {
  background: rgba(255, 255, 255, 0.18);
}

.cards {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(190px, 1fr));
  gap: 14px;
}

.stat-card {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 18px;
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
  width: 46px;
  height: 46px;
  border-radius: 12px;
  color: #fff;
  box-shadow: 0 8px 18px rgba(79, 107, 246, 0.22);
}

.stat-card__value {
  font-size: 22px;
  font-weight: 700;
  color: var(--c-text);
  line-height: 1.15;
}

.stat-card__label {
  margin-top: 3px;
  font-size: 13px;
  color: var(--c-text-3);
}

.charts {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(380px, 1fr));
  gap: 16px;
}

.chart-card {
  padding: 18px 20px;
  min-width: 0;
}

.chart-card__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 6px;
}

.chart-card__title {
  font-size: 15px;
  font-weight: 600;
  color: var(--c-text);
}

.chart-box {
  position: relative;
}

.chart {
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

@media (max-width: 900px) {
  .hero {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
