<script setup lang="ts">
import { computed } from 'vue'

defineOptions({ name: 'LogoMark' })

const props = withDefaults(
  defineProps<{
    /** 方块尺寸，支持数字(px)或 CSS 长度字符串 */
    size?: number | string
    /** 是否在图标右侧展示「智聘」品牌文字 */
    showText?: boolean
    /** 文字字号，缺省按 size 推算 */
    textSize?: number | string
  }>(),
  { size: 32, showText: false },
)

const boxSize = computed(() =>
  typeof props.size === 'number' ? `${props.size}px` : props.size,
)
const boxFontSize = computed(() => {
  const base = typeof props.size === 'number' ? props.size : 32
  return `${Math.round(base * 0.56)}px`
})
const textFontSize = computed(() => {
  if (props.textSize) return props.textSize
  const base = typeof props.size === 'number' ? props.size : 32
  return `${Math.max(16, Math.round(base * 0.5))}px`
})
</script>

<template>
  <span class="logo-mark">
    <span
      class="logo-mark__box"
      :style="{ width: boxSize, height: boxSize, fontSize: boxFontSize }"
    >
      智
    </span>
    <span
      v-if="showText"
      class="logo-mark__text"
      :style="{ fontSize: textFontSize }"
    >
      智聘
    </span>
  </span>
</template>

<style scoped>
.logo-mark {
  display: inline-flex;
  align-items: center;
  gap: 9px;
  line-height: 1;
}

.logo-mark__box {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex: none;
  border-radius: 9px;
  color: #fff;
  font-family: 'PingFang SC', 'Microsoft YaHei', sans-serif;
  font-weight: 700;
  background: var(--c-primary);
  
}

.logo-mark__text {
  font-weight: 700;
  letter-spacing: 0.5px;
  white-space: nowrap;
}
</style>
