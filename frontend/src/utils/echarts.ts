import * as echarts from 'echarts/core'
import { BarChart, LineChart, PieChart, RadarChart } from 'echarts/charts'
import { AriaComponent, GridComponent, LegendComponent, TitleComponent, TooltipComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'
import type { EChartsOption } from 'echarts'
import { onBeforeUnmount, onMounted, shallowRef, toValue, watch } from 'vue'
import type { MaybeRefOrGetter } from 'vue'

echarts.use([BarChart, LineChart, PieChart, RadarChart, AriaComponent, GridComponent, LegendComponent, TitleComponent, TooltipComponent, CanvasRenderer])
const motionAllowed = () => !window.matchMedia('(prefers-reduced-motion: reduce)').matches
export function initEChart(el:HTMLElement, option:EChartsOption):echarts.EChartsType {
  const chart=echarts.init(el)
  chart.setOption({ ...option, animation:motionAllowed(), aria:{ enabled:true } })
  return chart
}
export function useChart(option:MaybeRefOrGetter<EChartsOption>) {
  const elRef=shallowRef<HTMLElement|null>(null)
  let chart:echarts.EChartsType|null=null
  let observer:ResizeObserver|null=null
  const render=()=>{ chart?.setOption({ ...toValue(option),animation:motionAllowed(),aria:{ enabled:true } },true) }
  const attach=()=>{
    if(chart?.getDom()===elRef.value) return
    observer?.disconnect()
    chart?.dispose()
    chart=null
    if(!elRef.value) return
    chart=echarts.init(elRef.value)
    render()
    observer=new ResizeObserver(()=>chart?.resize())
    observer.observe(elRef.value)
  }
  onMounted(attach)
  watch(elRef,attach,{ flush:'post' })
  watch(option,render)
  onBeforeUnmount(()=>{ observer?.disconnect(); chart?.dispose(); chart=null })
  return { elRef }
}
