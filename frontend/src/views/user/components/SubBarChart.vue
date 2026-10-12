<script setup lang="ts">
import { computed } from 'vue'
import { useChart } from '@/utils/echarts'
import type { EChartsOption } from 'echarts'

defineOptions({ name: 'SubBarChart' })

export interface SubBarItem {
  label: string
  value: number
  color: string
}

const props = defineProps<{
  items: SubBarItem[]
}>()

const option = computed<EChartsOption>(() => ({
  tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
  grid: { left: 8, right: 16, top: 24, bottom: 8, containLabel: true },
  xAxis: {
    type: 'category',
    data: props.items.map((item) => item.label),
    axisTick: { show: false },
    axisLine: { lineStyle: { color: '#e5e7eb' } },
    axisLabel: { color: '#6b7280' },
  },
  yAxis: {
    type: 'value',
    max: 100,
    splitLine: { lineStyle: { color: '#f1f5f9' } },
    axisLabel: { formatter: '{value}分' },
  },
  series: [
    {
      type: 'bar',
      barWidth: 44,
      data: props.items.map((item) => ({
        value: item.value,
        itemStyle: {
          borderRadius: [8, 8, 0, 0],
          color: {
            type: 'linear',
            x: 0,
            y: 0,
            x2: 0,
            y2: 1,
            colorStops: [
              { offset: 0, color: item.color },
              { offset: 1, color: `${item.color}cc` },
            ],
          },
        },
      })),
      label: {
        show: true,
        position: 'top',
        formatter: '{c}',
        color: '#4b5563',
      },
    },
  ],
}))

const { elRef } = useChart(option)
</script>

<template>
  <div ref="elRef" class="sub-bar-chart"></div>
</template>

<style scoped>
.sub-bar-chart {
  width: 100%;
  height: 320px;
}
</style>
