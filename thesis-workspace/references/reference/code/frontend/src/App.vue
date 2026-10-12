<script setup lang="ts">
import AiChatWidget from '@/components/AiChatWidget.vue'
import { ElConfigProvider } from 'element-plus'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
</script>

<template>
  <ElConfigProvider :locale="zhCn">
    <router-view />
    <AiChatWidget />
  </ElConfigProvider>
</template>
