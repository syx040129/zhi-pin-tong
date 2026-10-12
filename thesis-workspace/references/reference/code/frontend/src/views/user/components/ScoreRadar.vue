<script setup lang="ts">
import { computed } from 'vue'
import { useChart } from '@/utils/echarts'
import type { EChartsOption } from 'echarts'
export interface RadarScore { name:string; value:number }
const props=defineProps<{ scores:RadarScore[]; name?:string }>()
const option=computed<EChartsOption>(()=>({
  animation: !window.matchMedia('(prefers-reduced-motion: reduce)').matches,
  tooltip:{ trigger:'item' },
  radar:{ indicator:props.scores.map(s=>({ name:s.name,max:100 })), radius:'64%',splitArea:{ areaStyle:{ color:['#f6f8fc','#edf2fa'] } },axisName:{ color:'#647185',fontSize:12 } },
  series:[{ type:'radar',symbolSize:5,data:[{value:props.scores.map(s=>s.value),name:props.name||'评分',areaStyle:{color:'rgba(49,89,216,.18)'},lineStyle:{color:'#3159d8',width:2},itemStyle:{color:'#3159d8'}}] }]
}))
const { elRef }=useChart(option)
</script>
<template><div ref="elRef" class="score-radar" role="img" :aria-label="scores.map(s=>`${s.name} ${s.value}分`).join('，')" /></template>
<style scoped>.score-radar { width:100%; height:260px; }</style>
