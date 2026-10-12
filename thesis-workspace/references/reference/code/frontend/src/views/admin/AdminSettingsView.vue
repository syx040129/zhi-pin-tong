<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useMediaQuery } from '@/composables/useMediaQuery'
import { ElMessage } from 'element-plus'
import { adminAiConfig, adminAiTest } from '@/api/admin'
import type { AiConfig, AiMode, AiTestResult } from '@/types/api'

defineOptions({ name: 'AdminSettingsView' })

const smallScreen = useMediaQuery('(max-width: 760px)')
const loading = ref(false)
const testing = ref(false)
const config = ref<AiConfig | null>(null)
const testResult = ref<AiTestResult | null>(null)

const modeInfo: Record<AiMode, { label: string; type: 'success' | 'warning' | 'info'; desc: string }> = {
  REAL: {
    label: '真实模式',
    type: 'success',
    desc: '已接入大模型 API，AI 功能将真实调用模型生成分析与建议。',
  },
  MOCK: {
    label: '模拟模式',
    type: 'warning',
    desc: '使用内置规则与模拟数据返回演示结果，不消耗真实 API 额度。',
  },
  DISABLED: {
    label: '已禁用',
    type: 'info',
    desc: 'AI 功能整体关闭，平台将不提供 AI 相关能力。',
  },
}

const mode = computed<AiMode>(() => config.value?.mode ?? 'DISABLED')
const modeMeta = computed(() => modeInfo[mode.value] ?? modeInfo.DISABLED)

async function fetchConfig(): Promise<void> {
  loading.value = true
  try {
    config.value = await adminAiConfig()
  } catch {
    // 拦截器已提示
  } finally {
    loading.value = false
  }
}

async function runTest(): Promise<void> {
  if (testing.value) return
  testing.value = true
  testResult.value = null
  try {
    testResult.value = await adminAiTest()
    if (testResult.value.ok) ElMessage.success('AI 连通性测试通过')
  } catch {
    // 拦截器已提示
  } finally {
    testing.value = false
  }
}

onMounted(() => {
  void fetchConfig()
})
</script>

<template>
  <div v-loading="loading" class="settings-page">
    <el-alert
      type="info"
      :closable="false"
      show-icon
      class="readonly-alert"
    >
      <template #title>
        AI 配置为只读信息，由后端 application.yml 的 ai.* 配置项提供；修改配置后需重启服务生效，API Key 不会回显。
      </template>
    </el-alert>

    <div class="panel mode-card">
      <div class="mode-card__main">
        <div class="mode-card__icon">
          <el-icon :size="26"><MagicStick /></el-icon>
        </div>
        <div class="mode-card__info">
          <div class="mode-card__title">
            当前 AI 运行模式
            <el-tag :type="modeMeta.type" effect="dark" round size="large">{{ modeMeta.label }}</el-tag>
          </div>
          <div class="mode-card__desc">{{ modeMeta.desc }}</div>
        </div>
      </div>

      <el-divider />

      <div class="switch-rows">
        <div class="switch-row">
          <div>
            <div class="switch-row__label">功能总开关</div>
            <div class="switch-row__hint">enabled：是否启用 AI 相关功能（后端只读）</div>
          </div>
          <el-switch :model-value="Boolean(config?.enabled)" disabled />
        </div>
        <div class="switch-row">
          <div>
            <div class="switch-row__label">模拟模式</div>
            <div class="switch-row__hint">mock：启用后返回规则模拟结果，不请求真实模型</div>
          </div>
          <el-switch :model-value="Boolean(config?.mock)" disabled />
        </div>
        <div class="switch-row">
          <div>
            <div class="switch-row__label">API Key</div>
            <div class="switch-row__hint">密钥仅保存在后端环境，出于安全考虑不会回显</div>
          </div>
          <el-tag :type="config?.hasApiKey ? 'success' : 'danger'" round effect="plain">
            {{ config?.hasApiKey ? '已配置' : '未配置' }}
          </el-tag>
        </div>
      </div>
    </div>

    <div class="panel detail-card">
      <div class="detail-head">
        <div class="detail-head__title">服务端配置详情</div>
        <button type="button" class="grad-btn" :disabled="testing" @click="runTest">
          <el-icon v-if="testing" class="is-loading" style="margin-right: 5px"><Loading /></el-icon>
          <el-icon v-else style="margin-right: 5px"><Connection /></el-icon>
          {{ testing ? '测试中…' : '测试 AI 连通性' }}
        </button>
      </div>

      <el-descriptions :column="smallScreen ? 1 : 2" border class="detail-descriptions">
        <el-descriptions-item label="接口地址 Base URL">
          <span class="mono-text">{{ config?.baseUrl || '-' }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="对话模型 Model">
          <span class="mono-text">{{ config?.model || '-' }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="向量模型 Embedding">
          <span class="mono-text">{{ config?.embeddingModel || '-' }}</span>
          <el-tag :type="config?.embeddingEnabled ? 'success' : 'info'" size="small" effect="plain">
            {{ config?.embeddingEnabled ? '真实模型' : '本地向量' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="推荐候选池大小">
          {{ config?.recommendPoolSize ?? '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="推荐结果数量">
          {{ config?.recommendLimit ?? '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="运行模式">
          <el-tag :type="modeMeta.type" size="small" round>{{ modeMeta.label }}</el-tag>
        </el-descriptions-item>
      </el-descriptions>

      <transition name="el-fade-in">
        <el-alert
          v-if="testResult"
          :type="testResult.ok ? 'success' : 'error'"
          show-icon
          :closable="true"
          class="test-result"
        >
          <template #title>{{ testResult.ok ? 'AI 连通性测试通过' : 'AI 连通性测试失败' }}</template>
          <div class="test-result__lines">
            <div>提示信息：{{ testResult.message }}</div>
            <div v-if="testResult.reply">返回内容：{{ testResult.reply }}</div>
            <div v-if="testResult.latencyMs != null">耗时：{{ testResult.latencyMs }} ms</div>
            <div>当前模式：{{ modeInfo[testResult.mode]?.label ?? testResult.mode }}</div>
          </div>
        </el-alert>
      </transition>
    </div>
  </div>
</template>

<style scoped>
.settings-page {
  min-height: 100%;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.readonly-alert {
  border-radius: 12px;
}

.mode-card,
.detail-card {
  padding: 22px 24px;
}

.mode-card__main {
  display: flex;
  align-items: center;
  gap: 16px;
}

.mode-card__icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 56px;
  height: 56px;
  flex: none;
  border-radius: 16px;
  color: #fff;
  background: var(--c-primary);
  box-shadow: 0 10px 22px rgba(79, 107, 246, 0.28);
}

.mode-card__title {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 17px;
  font-weight: 700;
  color: var(--c-text);
}

.mode-card__desc {
  margin-top: 8px;
  color: var(--c-text-2);
  font-size: 13px;
  line-height: 1.7;
}

.switch-rows {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.switch-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.switch-row__label {
  font-size: 14px;
  font-weight: 600;
  color: var(--c-text);
}

.switch-row__hint {
  margin-top: 3px;
  color: var(--c-text-3);
  font-size: 12px;
}

.detail-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 16px;
}

.detail-head__title {
  font-size: 16px;
  font-weight: 600;
  color: var(--c-text);
}

.detail-descriptions {
  max-width: 100%;
}

.mono-text {
  font-family: Consolas, Menlo, monospace;
  font-size: 13px;
}

.test-result {
  margin-top: 16px;
}

.test-result__lines {
  display: flex;
  flex-direction: column;
  gap: 6px;
  font-size: 13px;
  line-height: 1.7;
}
</style>
