<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import type { InterviewReportVO } from '@/types/api'
import { interviewReport } from '@/api/ai'

defineOptions({ name: 'InterviewReportView' })

const route = useRoute()
const router = useRouter()

const interviewId = computed(() => Number(route.params.id))

const report = ref<InterviewReportVO | null>(null)
const loading = ref(true)
const unavailable = ref(false)

function scoreColor(score: number): string {
  if (score >= 80) return '#22c55e'
  if (score >= 60) return '#4f6bf6'
  if (score >= 40) return '#f59e0b'
  return '#ef4444'
}

const dimensionList = computed(() =>
  report.value
    ? Object.entries(report.value.dimensionScores ?? {}).map(([label, value]) => ({
        label,
        value: Number(value) || 0,
      }))
    : [],
)

async function loadReport(): Promise<void> {
  if (!Number.isFinite(interviewId.value) || interviewId.value <= 0) {
    unavailable.value = true
    loading.value = false
    return
  }
  loading.value = true
  unavailable.value = false
  try {
    report.value = await interviewReport(interviewId.value)
  } catch {
    report.value = null
    unavailable.value = true
  } finally {
    loading.value = false
  }
}

onMounted(loadReport)
</script>

<template>
  <div class="page report-page">
    <div class="report-page__inner">
      <div v-loading="loading" class="report-body">
        <!-- 报告未生成 -->
        <div v-if="unavailable && !loading" class="panel unavailable">
          <el-empty :image-size="120" description="本场面试的报告还未生成">
            <p class="unavailable__hint">需要完成全部答题并主动「结束面试」，AI 才会生成报告</p>
            <div class="unavailable__ops">
              <el-button type="primary" @click="router.push(`/ai/interview/${interviewId}`)">
                <el-icon><VideoPlay /></el-icon>继续本场面试
              </el-button>
              <el-button @click="router.push('/ai/interview')">返回面试列表</el-button>
            </div>
          </el-empty>
        </div>

        <template v-else-if="report && !loading">
          <div class="report-head">
            <el-button text @click="router.push('/ai/interview')">
              <el-icon><ArrowLeft /></el-icon>&nbsp;我的面试记录
            </el-button>
            <h1 class="report-head__title grad-text">面试报告</h1>
            <p class="report-head__sub">{{ report.title }}</p>
          </div>

          <!-- 总分概览 -->
          <div class="panel summary-card">
            <div class="summary-card__ring">
              <el-progress
                type="dashboard"
                :width="176"
                :percentage="report.totalScore"
                :color="scoreColor(report.totalScore)"
              >
                <template #default="{ percentage }">
                  <div class="summary-num">
                    <strong>{{ percentage }}</strong>
                    <span>综合评分</span>
                  </div>
                </template>
              </el-progress>
            </div>
            <div class="summary-card__info">
              <div class="summary-card__grade" :style="{ color: scoreColor(report.totalScore) }">
                {{
                  report.totalScore >= 80 ? '面试表现优秀' :
                    report.totalScore >= 60 ? '整体表现良好' :
                    '仍有提升空间'
                }}
              </div>
              <p>
                本次共作答 {{ report.answeredCount }} / {{ report.questionCount }} 道题，AI 基于回答内容从多个维度给出综合评估。
              </p>
              <div class="summary-card__meta">AI 已根据本次答题情况生成综合评估</div>
            </div>
          </div>

          <!-- 维度 + 优缺建议 -->
          <div class="report-grid">
            <div class="panel dim-card">
              <h3 class="block-title"><el-icon><DataAnalysis /></el-icon>维度分析</h3>
              <div
                v-for="dim in dimensionList"
                :key="dim.label"
                class="dim-row"
              >
                <div class="dim-row__label">
                  <strong>{{ dim.label }}</strong>
                  <em>{{ dim.value }}</em>
                </div>
                <el-progress
                  :percentage="dim.value"
                  :color="scoreColor(dim.value)"
                  :stroke-width="10"
                />
              </div>
              <el-empty
                v-if="dimensionList.length === 0"
                :image-size="70"
                description="暂无维度数据"
              />
            </div>

            <div class="report-right">
              <div class="panel text-panel">
                <h3 class="block-title block-title--green">
                  <el-icon><CircleCheckFilled /></el-icon>面试优势
                </h3>
                <ul v-if="report.strengths.length" class="good-list">
                  <li v-for="item in report.strengths" :key="item">✓ {{ item }}</li>
                </ul>
                <p v-else class="text-panel__empty">暂无优势点评</p>
              </div>

              <div class="panel text-panel">
                <h3 class="block-title block-title--amber">
                  <el-icon><WarningFilled /></el-icon>不足之处
                </h3>
                <ul v-if="report.weaknesses.length" class="warn-list">
                  <li v-for="item in report.weaknesses" :key="item">⚠ {{ item }}</li>
                </ul>
                <p v-else class="text-panel__empty">暂无不足点评</p>
              </div>

              <div class="panel text-panel">
                <h3 class="block-title">
                  <el-icon><MagicStick /></el-icon>提升建议
                </h3>
                <ol v-if="report.suggestions.length" class="suggest-list">
                  <li v-for="item in report.suggestions" :key="item">{{ item }}</li>
                </ol>
                <p v-else class="text-panel__empty">暂无提升建议</p>
              </div>
            </div>
          </div>

          <!-- 底部导航 -->
          <div class="report-foot panel">
            <router-link class="report-foot__link" :to="`/ai/interview/${interviewId}`">
              <el-icon><View /></el-icon>题目详情
            </router-link>
            <router-link class="report-foot__link" to="/ai/interview">
              <el-icon><RefreshLeft /></el-icon>再模拟一场
            </router-link>
            <router-link class="report-foot__link" to="/ai/interview">
              <el-icon><Tickets /></el-icon>我的面试记录
            </router-link>
          </div>
        </template>
      </div>
    </div>
  </div>
</template>

<style scoped>
.report-page__inner {
  max-width: 1000px;
  margin: 0 auto;
}

.report-body {
  min-height: 420px;
}

.unavailable {
  padding: 40px;
}

.unavailable__hint {
  margin: -4px 0 16px;
  color: var(--c-text-3);
  font-size: 13px;
}

.unavailable__ops {
  display: flex;
  justify-content: center;
  gap: 12px;
}

.report-head {
  margin-bottom: 18px;
}

.report-head__title {
  margin: 8px 0 0;
  font-size: 26px;
  font-weight: 650;
  text-align: center;
}

.report-head__sub {
  margin: 6px 0 0;
  color: var(--c-text-2);
  font-size: 14px;
  text-align: center;
}

/* ===== 总分 ===== */
.summary-card {
  display: flex;
  align-items: center;
  gap: 36px;
  padding: 28px 36px;
  margin-bottom: 18px;
}

.summary-card__ring {
  flex: none;
}

.summary-num {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.summary-num strong {
  color: var(--c-text);
  font-size: 40px;
  font-weight: 650;
  line-height: 1;
}

.summary-num span {
  margin-top: 6px;
  color: var(--c-text-3);
  font-size: 13px;
}

.summary-card__grade {
  font-size: 24px;
  font-weight: 650;
}

.summary-card__info p {
  margin: 12px 0 0;
  color: var(--c-text-2);
  font-size: 14px;
  line-height: 1.9;
  max-width: 460px;
}

.summary-card__meta {
  margin-top: 12px;
  color: var(--c-text-3);
  font-size: 12px;
}

/* ===== 网格 ===== */
.report-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(0, 1fr);
  gap: 18px;
  align-items: start;
}

.dim-card {
  padding: 24px;
}

.block-title {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 0 0 18px;
  font-size: 16px;
  font-weight: 700;
}

.block-title .el-icon {
  color: var(--c-primary);
}

.block-title--green .el-icon {
  color: #16a34a;
}

.block-title--amber .el-icon {
  color: #d97706;
}

.dim-row {
  margin-bottom: 16px;
}

.dim-row__label {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
}

.dim-row__label strong {
  font-size: 13px;
}

.dim-row__label em {
  color: var(--c-text-2);
  font-size: 13px;
  font-style: normal;
  font-weight: 700;
}

.report-right {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.text-panel {
  padding: 20px 24px;
}

.good-list,
.warn-list,
.suggest-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  color: #374151;
  font-size: 13px;
  line-height: 1.8;
}

.warn-list {
  color: #92400e;
}

.suggest-list {
  counter-reset: suggest;
}

.suggest-list li {
  position: relative;
  padding-left: 30px;
}

.suggest-list li::before {
  content: counter(suggest);
  counter-increment: suggest;
  position: absolute;
  left: 0;
  top: 2px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 20px;
  height: 20px;
  border-radius: 50%;
  color: #fff;
  font-size: 11px;
  font-weight: 700;
  background: var(--c-primary);
}

.text-panel__empty {
  margin: 0;
  color: var(--c-text-3);
  font-size: 13px;
}

/* ===== 底部导航 ===== */
.report-foot {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 36px;
  margin-top: 18px;
  padding: 16px 24px;
}

.report-foot__link {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  color: var(--c-primary);
  font-size: 14px;
  font-weight: 600;
}

.report-foot__link:hover {
  color: var(--c-accent);
}

@media (max-width: 900px) {
  .summary-card {
    flex-direction: column;
    text-align: center;
  }

  .summary-card__info p {
    max-width: none;
  }

  .report-grid {
    grid-template-columns: 1fr;
  }

  .report-foot {
    flex-wrap: wrap;
    gap: 14px 26px;
  }
}
</style>
