<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import type { AiAnalysisVO, ResumeVO } from '@/types/api'
import { analyzeResume, latestAnalysis } from '@/api/ai'
import { listResumes } from '@/api/resume'
import { formatDateTime } from '@/utils/format'
import ScoreRadar from '@/views/user/components/ScoreRadar.vue'

defineOptions({ name: 'ResumeAnalysisView' })

const route = useRoute()
const router = useRouter()

const resumes = ref<ResumeVO[]>([])
const selectedId = ref<number | undefined>()
const report = ref<AiAnalysisVO | null>(null)
const loadingList = ref(true)
const loadingReport = ref(false)
const analyzing = ref(false)
const hasAttempted = ref(false)

const dimensionConfig = [
  { key: 'skillScore' as const, label: '技能匹配', hint: '与目标岗位的技能重合度' },
  { key: 'experienceScore' as const, label: '经验匹配', hint: '工作与项目经验的相关度' },
  { key: 'educationScore' as const, label: '学历匹配', hint: '学历与目标岗位的匹配度' },
  { key: 'completenessScore' as const, label: '完整度', hint: '简历信息是否充实完整' },
]

async function loadResumes(): Promise<void> {
  loadingList.value = true
  try {
    resumes.value = await listResumes()
    const queryId = Number(route.query.resumeId)
    const target =
      resumes.value.find((r) => r.id === queryId) ?? resumes.value[0]
    selectedId.value = target?.id
  } catch {
    resumes.value = []
  } finally {
    loadingList.value = false
  }
}

async function loadReport(resumeId: number): Promise<void> {
  loadingReport.value = true
  report.value = null
  try {
    report.value = await latestAnalysis(resumeId)
    hasAttempted.value = true
  } catch {
    report.value = null
    hasAttempted.value = true
  } finally {
    loadingReport.value = false
  }
}

async function runAnalyze(): Promise<void> {
  if (!selectedId.value || analyzing.value) return
  analyzing.value = true
  try {
    report.value = await analyzeResume(selectedId.value)
    hasAttempted.value = true
  } catch {
    report.value = null
  } finally {
    analyzing.value = false
  }
}

function handleResumeChange(id: number): void {
  router.replace({ path: '/resume/analysis', query: { resumeId: id } })
  loadReport(id)
}

function scoreColor(score: number): string {
  if (score >= 80) return '#22c55e'
  if (score >= 60) return '#4f6bf6'
  if (score >= 40) return '#f59e0b'
  return '#ef4444'
}

const radarScores = computed(() =>
  report.value
    ? [
        { name: '综合', value: report.value.overallScore },
        { name: '技能', value: report.value.skillScore },
        { name: '经验', value: report.value.experienceScore },
        { name: '学历', value: report.value.educationScore },
        { name: '完整度', value: report.value.completenessScore },
      ]
    : [],
)

const sourceLabel = computed(() =>
  report.value?.source === 'LLM'
    ? { text: 'AI 大模型生成', type: 'primary' as const }
    : { text: '规则引擎生成', type: 'info' as const },
)

function editSuggestion(text: string): void {
  if (!selectedId.value) return
  const section = /技能|技术栈/.test(text) ? 'skills' : /经历|项目|工作|实习/.test(text)
    ? 'experiences' : /简介|总结|自我/.test(text) ? 'summary' : 'basic'
  router.push({ path: `/resume/edit/${selectedId.value}`, query: { section } })
}

onMounted(async () => {
  await loadResumes()
  if (selectedId.value) await loadReport(selectedId.value)
})
</script>

<template>
  <div class="page analysis-page">
    <div class="analysis-page__inner">
      <div class="analysis-head">
        <div>
          <h1 class="analysis-head__title grad-text">AI 简历分析</h1>
          <p class="analysis-head__desc">从多个维度评估简历竞争力，并给出针对性的提升建议</p>
        </div>
        <div class="analysis-head__ops">
          <el-select
            v-model="selectedId"
            placeholder="选择简历"
            style="width: 240px"
            :disabled="resumes.length === 0"
            @change="handleResumeChange"
          >
            <el-option
              v-for="resume in resumes"
              :key="resume.id"
              :label="resume.name || `简历 #${resume.id}`"
              :value="resume.id"
            />
          </el-select>
          <button class="grad-btn" type="button" :disabled="!selectedId || analyzing" @click="runAnalyze">
            <el-icon><Refresh /></el-icon>AI 重新分析
          </button>
        </div>
      </div>

      <div v-loading="loadingList" class="analysis-body">
        <el-empty v-if="!loadingList && resumes.length === 0" description="还没有简历，无法进行分析">
          <el-button type="primary" @click="router.push('/resume/edit')">去创建简历</el-button>
        </el-empty>

        <template v-else-if="!loadingList">
          <div v-if="!report && !loadingReport && hasAttempted" class="no-report panel">
            <el-empty :image-size="100" description="该简历还没有分析报告">
              <el-button type="primary" :loading="analyzing" @click="runAnalyze">
                <el-icon><MagicStick /></el-icon>立即 AI 分析
              </el-button>
            </el-empty>
          </div>

          <div v-else v-loading="analyzing" class="report-wrap" element-loading-text="AI 正在分析简历，请稍候…">
            <div v-loading="loadingReport" class="report-grid">
              <template v-if="report">
                <!-- 左列：评分总览 -->
                <div class="report-left">
                  <div class="panel score-card">
                    <div class="score-card__head">
                      <span class="score-card__label">综合评分</span>
                      <el-tag size="small" :type="sourceLabel.type" effect="light">
                        {{ sourceLabel.text }}
                      </el-tag>
                    </div>
                    <div class="score-card__ring">
                      <el-progress
                        type="dashboard"
                        :width="160"
                        :percentage="report.overallScore"
                        :color="scoreColor(report.overallScore)"
                      >
                        <template #default="{ percentage }">
                          <div class="big-score">
                            <span>{{ percentage }}</span>
                            <em>分</em>
                          </div>
                        </template>
                      </el-progress>
                    </div>
                    <div class="score-card__time">
                      分析时间：{{ formatDateTime(report.createTime) }}
                    </div>
                  </div>

                  <div class="panel dimension-card">
                    <h3 class="dimension-card__title">维度得分</h3>
                    <div
                      v-for="dim in dimensionConfig"
                      :key="dim.key"
                      class="dimension-row"
                    >
                      <div class="dimension-row__label">
                        <strong>{{ dim.label }}</strong>
                        <span>{{ dim.hint }}</span>
                      </div>
                      <el-progress
                        :percentage="report[dim.key]"
                        :color="scoreColor(report[dim.key])"
                        :stroke-width="10"
                        :show-text="true"
                      />
                    </div>
                  </div>
                </div>

                <!-- 右列：雷达 + 建议 -->
                <div class="report-right">
                  <div class="panel radar-card">
                    <h3 class="radar-card__title">能力画像</h3>
                    <ScoreRadar :scores="radarScores" />
                  </div>

                  <div class="panel advice-card">
                    <div class="advice-block">
                      <h4 class="advice-block__title advice-block__title--good">
                        <el-icon><CircleCheckFilled /></el-icon>核心优势
                      </h4>
                      <ul v-if="report.strengths.length" class="check-list">
                        <li v-for="item in report.strengths" :key="item">✓ {{ item }}</li>
                      </ul>
                      <p v-else class="advice-block__empty">暂无明显优势项，可通过完善经历来突出亮点</p>
                    </div>

                    <div class="advice-block">
                      <h4 class="advice-block__title advice-block__title--warn">
                        <el-icon><WarningFilled /></el-icon>待提升
                      </h4>
                      <ul v-if="report.weaknesses.length" class="warn-list">
                        <li v-for="item in report.weaknesses" :key="item">⚠ {{ item }}</li>
                      </ul>
                      <p v-else class="advice-block__empty">简历整体表现优秀，继续保持</p>
                    </div>
                  </div>

                  <div class="panel advice-card">
                    <h4 class="suggest-title"><el-icon><MagicStick /></el-icon>优化建议</h4>
                    <ol v-if="report.suggestions.length" class="suggest-list">
                      <li v-for="item in report.suggestions" :key="item">
                        <span class="suggest-list__index"></span>
                        {{ item }}
                        <el-button text type="primary" @click="editSuggestion(item)">去修改</el-button>
                      </li>
                    </ol>
                  </div>

                  <div class="panel target-card">
                    <div class="target-card__head">
                      <el-icon><Aim /></el-icon>
                      <span>适合投递的方向</span>
                    </div>
                    <div v-if="report.targetJobs.length" class="target-card__tags">
                      <el-tag
                        v-for="job in report.targetJobs"
                        :key="job"
                        size="large"
                        effect="plain"
                        class="target-tag"
                      >
                        {{ job }}
                      </el-tag>
                    </div>
                    <el-button
                      v-else
                      text
                      type="primary"
                      @click="router.push('/recommend')"
                    >
                      前往智能匹配查看推荐岗位
                    </el-button>
                  </div>
                </div>
              </template>
            </div>
          </div>
        </template>
      </div>
    </div>
  </div>
</template>

<style scoped>
.analysis-page__inner {
  max-width: 1200px;
  margin: 0 auto;
}

.analysis-head {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 20px;
  flex-wrap: wrap;
}

.analysis-head__title {
  margin: 0;
  font-size: 26px;
  font-weight: 650;
}

.analysis-head__desc {
  margin: 8px 0 0;
  color: var(--c-text-2);
  font-size: 14px;
}

.analysis-head__ops {
  display: flex;
  align-items: center;
  gap: 12px;
}

.analysis-body {
  min-height: 400px;
}

.no-report {
  padding: 40px;
}

.report-grid {
  display: grid;
  grid-template-columns: 420px minmax(0, 1fr);
  gap: 20px;
  align-items: start;
}

.report-left,
.report-right {
  display: flex;
  flex-direction: column;
  gap: 20px;
  min-width: 0;
}

/* 评分总览 */
.score-card {
  padding: 24px;
  text-align: center;
}

.score-card__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.score-card__label {
  color: var(--c-text-2);
  font-size: 14px;
  font-weight: 600;
}

.score-card__ring {
  display: flex;
  justify-content: center;
  margin: 12px 0;
}

.big-score span {
  font-size: 38px;
  font-weight: 650;
  color: var(--c-text);
  line-height: 1.1;
}

.big-score em {
  display: block;
  margin-top: 2px;
  color: var(--c-text-3);
  font-size: 12px;
  font-style: normal;
}

.score-card__time {
  color: var(--c-text-3);
  font-size: 12px;
}

.dimension-card {
  padding: 22px 24px;
}

.dimension-card__title,
.radar-card__title {
  margin: 0 0 18px;
  font-size: 15px;
  font-weight: 700;
}

.dimension-row {
  margin-bottom: 16px;
}

.dimension-row__label {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  margin-bottom: 8px;
}

.dimension-row__label strong {
  font-size: 13px;
}

.dimension-row__label span {
  color: var(--c-text-3);
  font-size: 12px;
}

.radar-card {
  padding: 22px 24px;
}

.advice-card {
  padding: 22px 24px;
}

.advice-block + .advice-block {
  margin-top: 20px;
  padding-top: 20px;
  border-top: 1px dashed var(--c-border);
}

.advice-block__title {
  display: flex;
  align-items: center;
  gap: 6px;
  margin: 0 0 12px;
  font-size: 15px;
  font-weight: 700;
}

.advice-block__title--good {
  color: #16a34a;
}

.advice-block__title--warn {
  color: #d97706;
}

.check-list,
.warn-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  color: #374151;
  font-size: 13px;
  line-height: 1.7;
}

.warn-list {
  color: #92400e;
}

.advice-block__empty {
  margin: 0;
  color: var(--c-text-3);
  font-size: 13px;
}

.suggest-title {
  display: flex;
  align-items: center;
  gap: 6px;
  margin: 0 0 14px;
  font-size: 15px;
  font-weight: 700;
}

.suggest-title .el-icon {
  color: var(--c-primary);
}

.suggest-list {
  counter-reset: suggest;
}

.suggest-list li {
  position: relative;
  padding-left: 34px;
  margin-bottom: 10px;
  color: #374151;
  font-size: 13px;
  line-height: 1.8;
}

.suggest-list__index {
  position: absolute;
  left: 0;
  top: 2px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 22px;
  height: 22px;
  border-radius: 50%;
  color: #fff;
  font-size: 12px;
  font-weight: 700;
  background: var(--c-primary);
}

.suggest-list li::before {
  content: counter(suggest);
  counter-increment: suggest;
  position: absolute;
  left: 0;
  top: 3px;
  width: 22px;
  height: 22px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  color: #fff;
  font-size: 12px;
  font-weight: 700;
  background: var(--c-primary);
}

.suggest-list__index {
  display: none;
}

.target-card {
  padding: 20px 24px;
}

.target-card__head {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 14px;
  font-size: 15px;
  font-weight: 700;
}

.target-card__head .el-icon {
  color: var(--c-primary);
}

.target-card__tags {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.target-tag {
  border-radius: 999px;
  font-size: 13px;
}

@media (max-width: 1000px) {
  .report-grid {
    grid-template-columns: 1fr;
  }
}
</style>
