<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import type { MatchResultVO, ResumeVO } from '@/types/api'
import { ElMessage } from 'element-plus'
import { apply } from '@/api/application'
import { getJob } from '@/api/job'
import { matchJob } from '@/api/ai'
import { listResumes } from '@/api/resume'
import SubBarChart from '@/views/user/components/SubBarChart.vue'

defineOptions({ name: 'JobMatchView' })

const route = useRoute()
const router = useRouter()

const jobId = computed(() => Number(route.params.jobId))

const result = ref<MatchResultVO | null>(null)
const loading = ref(true)
const failed = ref(false)
const hasResume = ref<boolean | null>(null)
const resumes = ref<ResumeVO[]>([])
const selectedId = ref<number>()
const applying = ref(false)
const applied = ref(false)

async function submitApply(): Promise<void> {
  if (!selectedId.value || applying.value || applied.value) return
  applying.value = true
  try {
    await apply({ jobId: jobId.value, resumeId: selectedId.value })
    applied.value = true
    ElMessage.success('投递成功，可在我的投递中查看处理结果')
  } catch { /* 提示由拦截器处理 */ }
  finally { applying.value = false }
}

const subConfig = [
  { key: 'skillScore' as const, label: '技能匹配', desc: '简历技能与岗位技能重合度' },
  { key: 'experienceScore' as const, label: '经验匹配', desc: '工作 / 项目经验相关度' },
  { key: 'educationScore' as const, label: '学历匹配', desc: '学历层次与岗位要求' },
  { key: 'semanticScore' as const, label: '语义理解', desc: '简历文本与岗位语义相似度' },
]

function bandOf(score: number): { label: string; color: string } {
  if (score >= 80) return { label: '非常适合', color: '#22c55e' }
  if (score >= 60) return { label: '比较适合', color: '#4f6bf6' }
  return { label: '需要提升', color: '#f59e0b' }
}

async function runMatch(): Promise<void> {
  if (!Number.isFinite(jobId.value) || jobId.value <= 0) {
    failed.value = true
    loading.value = false
    return
  }
  loading.value = true
  failed.value = false
  try {
    result.value = await matchJob({ jobId: jobId.value, resumeId: selectedId.value })
  } catch {
    result.value = null
    failed.value = true
  } finally {
    loading.value = false
  }
}

async function init(): Promise<void> {
  try {
    resumes.value = await listResumes()
    hasResume.value = resumes.value.length > 0
    selectedId.value = resumes.value[0]?.id
    applied.value = Boolean((await getJob(jobId.value)).applicationStatus)
  } catch {
    hasResume.value = false
  }
  if (hasResume.value) await runMatch()
  else loading.value = false
}

const barItems = computed(() => {
  const value = result.value
  if (!value) return []
  return subConfig.map((config) => ({
    label: config.label,
    value: value[config.key],
    color: bandOf(value[config.key]).color,
  }))
})

const band = computed(() =>
  result.value ? bandOf(result.value.matchScore) : { label: '', color: '#4f6bf6' },
)

onMounted(init)
</script>

<template>
  <div class="page match-page">
    <div class="match-page__inner">
      <div class="match-head">
        <el-button text @click="router.back()">
          <el-icon><ArrowLeft /></el-icon>&nbsp;返回
        </el-button>
        <h1 class="match-head__title grad-text">岗位匹配分析</h1>
        <el-select v-if="hasResume" v-model="selectedId" :disabled="loading || applying" placeholder="选择匹配及投递的简历" @change="runMatch">
          <el-option v-for="resume in resumes" :key="resume.id" :value="resume.id" :label="resume.name || `简历 #${resume.id}`" />
        </el-select>
      </div>

      <!-- 无简历引导 -->
      <div v-if="hasResume === false" class="panel no-resume">
        <el-empty :image-size="120" description="匹配分析需要先有一份简历">
          <p class="no-resume__hint">AI 需要理解你的简历内容才能计算匹配度</p>
          <el-button type="primary" size="large" @click="router.push('/resume')">
            去完善简历
          </el-button>
        </el-empty>
      </div>

      <div v-else v-loading="loading" class="match-body">
        <el-result
          v-if="failed && !loading"
          icon="error"
          title="分析失败"
          sub-title="可能是简历信息不足或 AI 服务暂时不可用，请稍后重试"
        >
          <template #extra>
            <el-button type="primary" @click="runMatch">重新分析</el-button>
            <el-button @click="router.push('/resume')">完善简历</el-button>
          </template>
        </el-result>

        <template v-else-if="result && !loading">
          <!-- 顶部概览 -->
          <div class="panel overview-card">
            <div class="overview-card__ring">
              <el-progress
                type="dashboard"
                :width="176"
                :percentage="result.matchScore"
                :color="band.color"
              >
                <template #default="{ percentage }">
                  <div class="overview-num">
                    <strong>{{ percentage }}</strong>
                    <span>综合匹配度</span>
                  </div>
                </template>
              </el-progress>
            </div>

            <div class="overview-card__info">
              <div class="overview-grade" :style="{ color: band.color }">{{ band.label }}</div>
              <h3 :title="result.jobName">{{ result.jobName }}</h3>
              <div class="overview-meta">
                <span>{{ result.companyName }}</span>
                <el-tag size="small" effect="plain">
                  {{ result.source === 'FALLBACK' ? '部分模型调用已降级' : result.source === 'LLM' ? '模型与规则综合分析' : '本地规则分析' }}
                </el-tag>
              </div>
              <p>{{ result.executionNote }}</p>
              <p>评分权重：语义 50% · 技能 25% · 经验 15% · 学历 10%；推荐理由：{{ result.reasonSource === 'LLM' ? '大模型生成' : '规则生成' }}</p>
              <div class="overview-tags">
                <el-tag type="success" effect="light">
                  ✓ 匹配技能 {{ result.matchedSkills.length }} 项
                </el-tag>
                <el-tag type="danger" effect="light">
                  ✗ 缺失技能 {{ result.missingSkills.length }} 项
                </el-tag>
              </div>
            </div>

            <div class="overview-card__actions">
              <button class="grad-btn" type="button" @click="router.push(`/jobs/${result.jobId}`)">
                <el-icon><View /></el-icon>查看岗位
              </button>
              <el-button class="apply-btn" :loading="applying" :disabled="applied" @click="submitApply">
                <el-icon><Promotion /></el-icon>{{ applied ? '已投递' : '投递所选简历' }}
              </el-button>
              <el-button @click="router.push({ path: '/ai/interview', query: { jobId: result.jobId } })">该岗位模拟面试</el-button>
            </div>
          </div>

          <!-- 维度评分 + 雷达理由 -->
          <div class="match-grid">
            <div class="panel chart-card">
              <h3 class="chart-card__title"><el-icon><TrendCharts /></el-icon>维度评分</h3>
              <SubBarChart :items="barItems" />
              <div class="dim-labels">
                <span v-for="c in subConfig" :key="c.key">
                  <i :style="{ background: bandOf(result[c.key]).color }"></i>
                  {{ c.desc }}
                </span>
              </div>
            </div>

            <div class="panel reason-card">
              <h3 class="chart-card__title"><el-icon><MagicStick /></el-icon>AI 分析结论</h3>
              <p class="reason-card__text">{{ result.reason }}</p>

              <div class="reason-card__block">
                <h4><el-icon><CircleCheckFilled /></el-icon>你的匹配优势</h4>
                <div v-if="result.matchedSkills.length" class="skill-chips">
                  <el-tag
                    v-for="skill in result.matchedSkills"
                    :key="skill"
                    type="success"
                    effect="light"
                  >
                    ✓ {{ skill }}
                  </el-tag>
                </div>
                <p v-else class="reason-card__empty">暂未发现明显技能重合</p>
              </div>

              <div class="reason-card__block">
                <h4><el-icon><WarningFilled /></el-icon>建议补足</h4>
                <div v-if="result.missingSkills.length" class="skill-chips">
                  <el-tag
                    v-for="skill in result.missingSkills"
                    :key="skill"
                    type="danger"
                    effect="light"
                  >
                    ✗ {{ skill }}
                  </el-tag>
                </div>
                <p v-else class="reason-card__empty">技能覆盖完整，继续保持</p>
              </div>

              <div class="reason-card__foot">
                完整薪资、要求与福利请在<router-link
                  class="foot-link"
                  :to="`/jobs/${result.jobId}`"
                >岗位详情</router-link>中查看
              </div>
            </div>
          </div>
        </template>
      </div>
    </div>
  </div>
</template>

<style scoped>
.match-page__inner {
  max-width: 1200px;
  margin: 0 auto;
}

.match-head {
  margin-bottom: 18px;
}

.match-head__title {
  margin: 10px 0 0;
  font-size: 24px;
  font-weight: 650;
}

.no-resume {
  padding: 40px;
}

.no-resume__hint {
  margin: -6px 0 18px;
  color: var(--c-text-3);
  font-size: 13px;
}

.match-body {
  min-height: 400px;
}

/* ===== 顶部概览 ===== */
.overview-card {
  display: flex;
  align-items: center;
  gap: 34px;
  padding: 30px 36px;
  margin-bottom: 20px;
}

.overview-card__ring {
  flex: none;
}

.overview-num {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.overview-num strong {
  color: var(--c-text);
  font-size: 40px;
  font-weight: 650;
  line-height: 1;
}

.overview-num span {
  margin-top: 6px;
  color: var(--c-text-3);
  font-size: 13px;
}

.overview-card__info {
  flex: 1;
  min-width: 0;
}

.overview-grade {
  font-size: 20px;
  font-weight: 650;
}

.overview-card__info h3 {
  margin: 8px 0 0;
  font-size: 22px;
  font-weight: 650;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.overview-meta {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-top: 8px;
  color: var(--c-text-2);
  font-size: 13px;
}

.overview-tags {
  display: flex;
  gap: 8px;
  margin-top: 14px;
  flex-wrap: wrap;
}

.overview-card__actions {
  display: flex;
  flex-direction: column;
  gap: 10px;
  flex: none;
}

.overview-card__actions .grad-btn {
  height: 42px;
  padding: 0 26px;
  border-radius: 11px;
}

.apply-btn {
  height: 42px;
  border-radius: 11px;
  color: var(--c-primary);
  background: #fff;
  border: 1px solid rgba(79, 107, 246, 0.45);
}

.apply-btn:hover {
  background: rgba(79, 107, 246, 0.05);
}

/* ===== 下方网格 ===== */
.match-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(0, 1fr);
  gap: 20px;
}

.chart-card,
.reason-card {
  padding: 24px;
}

.chart-card__title {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 0 0 12px;
  font-size: 16px;
  font-weight: 700;
}

.chart-card__title .el-icon {
  color: var(--c-primary);
}

.dim-labels {
  display: flex;
  flex-wrap: wrap;
  gap: 14px;
  padding-top: 12px;
  border-top: 1px dashed var(--c-border);
}

.dim-labels span {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  color: var(--c-text-3);
  font-size: 12px;
}

.dim-labels i {
  width: 10px;
  height: 10px;
  border-radius: 3px;
}

.reason-card__text {
  margin: 0;
  padding: 14px;
  border-radius: 10px;
  color: #374151;
  font-size: 14px;
  line-height: 2;
  background: var(--c-primary-soft);
  border: 1px solid #dbe3fd;
}

.reason-card__block {
  margin-top: 18px;
}

.reason-card__block h4 {
  display: flex;
  align-items: center;
  gap: 6px;
  margin: 0 0 10px;
  font-size: 14px;
  font-weight: 700;
}

.reason-card__block h4 .el-icon {
  color: #16a34a;
}

.reason-card__block:nth-of-type(3) h4 .el-icon {
  color: #d97706;
}

.skill-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.reason-card__empty {
  margin: 0;
  color: var(--c-text-3);
  font-size: 13px;
}

.reason-card__foot {
  margin-top: 20px;
  padding-top: 14px;
  border-top: 1px dashed var(--c-border);
  color: var(--c-text-2);
  font-size: 13px;
}

.reason-card__foot .grad-text {
  font-size: 16px;
  font-weight: 650;
}

.foot-link {
  margin: 0 4px;
  color: var(--c-primary);
  font-weight: 600;
}

@media (max-width: 980px) {
  .overview-card {
    flex-direction: column;
    align-items: flex-start;
    text-align: center;
  }

  .overview-card__info,
  .overview-card__actions {
    width: 100%;
  }

  .overview-card__actions {
    flex-direction: row;
  }

  .match-grid {
    grid-template-columns: 1fr;
  }
}
</style>
