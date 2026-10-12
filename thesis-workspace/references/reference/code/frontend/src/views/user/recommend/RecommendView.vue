<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import type { RecommendItemVO, ResumeVO } from '@/types/api'
import { getRecommendations, refreshRecommendations } from '@/api/ai'
import { listResumes } from '@/api/resume'
import { apply } from '@/api/application'
import { salaryText } from '@/utils/format'

defineOptions({ name: 'RecommendView' })

const router = useRouter()

const resumes = ref<ResumeVO[]>([])
const list = ref<RecommendItemVO[]>([])
const loading = ref(true)
const refreshing = ref(false)
const hasResume = ref<boolean | null>(null)

const weights = [
  { label: '语义理解', percent: 50, color: '#4f6bf6' },
  { label: '技能匹配', percent: 25, color: '#22d3ee' },
  { label: '经验匹配', percent: 15, color: '#8b5cf6' },
  { label: '学历匹配', percent: 10, color: '#34d399' },
]

const medalColors = [
  '#edf2ff',
  '#edf2ff',
  '#edf2ff',
]

async function loadResumes(): Promise<void> {
  try {
    resumes.value = await listResumes()
    hasResume.value = resumes.value.length > 0
  } catch {
    resumes.value = []
    hasResume.value = false
  }
}

async function loadRecommendations(): Promise<void> {
  loading.value = true
  try {
    const data = await getRecommendations()
    list.value = data ?? []
  } catch {
    list.value = []
  } finally {
    loading.value = false
  }
}

async function refresh(): Promise<void> {
  if (refreshing.value) return
  refreshing.value = true
  try {
    const data = await refreshRecommendations()
    list.value = data ?? []
    ElMessage.success('推荐结果已刷新')
  } catch {
    /* 提示由拦截器处理 */
  } finally {
    refreshing.value = false
  }
}

/* ===== 投递弹窗 ===== */
const applyTarget = ref<RecommendItemVO | null>(null)
const applying = ref(false)
const applyForm = reactive({
  resumeId: undefined as number | undefined,
  coverLetter: '',
})

function openApply(item: RecommendItemVO): void {
  applyTarget.value = item
  if (resumes.value.length > 0) applyForm.resumeId = resumes.value[0].id
  applyForm.coverLetter = ''
}

function handleApplyDialogUpdate(visible: boolean): void {
  if (!visible) applyTarget.value = null
}

async function submitApply(): Promise<void> {
  const target = applyTarget.value
  if (!target || applying.value) return
  if (!applyForm.resumeId) {
    ElMessage.warning('请选择要投递的简历')
    return
  }
  applying.value = true
  try {
    await apply({
      jobId: target.jobId,
      resumeId: applyForm.resumeId,
      coverLetter: applyForm.coverLetter.trim() || undefined,
    })
    ElMessage.success('投递成功，祝你早日拿到 offer！')
    applyTarget.value = null
  } catch {
    /* 提示由拦截器处理 */
  } finally {
    applying.value = false
  }
}

function subScores(item: RecommendItemVO): { label: string; value: number }[] {
  return [
    { label: '技能', value: item.skillScore },
    { label: '经验', value: item.experienceScore },
    { label: '语义', value: item.semanticScore ?? 0 },
    { label: '学历', value: item.educationScore ?? 0 },
  ]
}

function scoreColor(score: number): string {
  if (score >= 80) return '#22c55e'
  if (score >= 60) return '#4f6bf6'
  if (score >= 40) return '#f59e0b'
  return '#ef4444'
}

onMounted(async () => {
  await loadResumes()
  if (hasResume.value) await loadRecommendations()
  else loading.value = false
})
</script>

<template>
  <div class="page recommend">
    <div class="recommend__inner">
      <!-- 算法介绍横幅 -->
      <div class="algo-banner">
        <div class="algo-banner__glow"></div>
        <div class="algo-banner__text">
          <h1 class="grad-text">岗位推荐</h1>
          <p>结合简历文本相似度、技能、经验与学历，为你筛选适合的职位；具体执行来源可在匹配分析中查看</p>
          <div class="algo-banner__score-hint">
            <span>综合匹配度 = 简历 × 岗位 的</span>
            <em>四维评分</em>
          </div>
        </div>
        <div class="algo-weights">
          <div
            v-for="w in weights"
            :key="w.label"
            class="algo-weight"
            :style="{ '--w': w.color }"
          >
            <div class="algo-weight__bar">
              <span :style="{ width: `${w.percent}%`, background: w.color }"></span>
            </div>
            <div class="algo-weight__meta">
              <strong>{{ w.label }}</strong>
              <em>{{ w.percent }}%</em>
            </div>
          </div>
        </div>
      </div>

      <!-- 无简历引导 -->
      <div v-if="hasResume === false" class="panel no-resume">
        <el-empty :image-size="130" description="还没有简历，无法进行智能匹配">
          <p class="no-resume__hint">上传或创建简历后，AI 才能理解你的技能与经历</p>
          <el-button type="primary" size="large" @click="router.push('/resume/edit')">
            <el-icon><DocumentAdd /></el-icon>去创建简历
          </el-button>
        </el-empty>
      </div>

      <template v-else>
        <!-- 列表头 -->
        <div class="rec-head">
          <div>
            <h2 class="rec-head__title">为你推荐的职位</h2>
            <p class="rec-head__desc">按匹配评分排序，结合职位要求与个人意向进行选择</p>
          </div>
          <el-button :loading="refreshing" @click="refresh">
            <el-icon><Refresh /></el-icon>刷新推荐
          </el-button>
        </div>

        <div v-loading="loading" class="rec-list">
          <el-empty
            v-if="!loading && list.length === 0"
            description="暂时没有合适的推荐岗位"
          >
            <el-button type="primary" @click="refresh">刷新推荐结果</el-button>
          </el-empty>

          <div v-else class="rec-cards">
            <div v-for="(item, index) in list" :key="item.jobId" class="rec-card panel">
              <div
                class="rec-card__rank"
                :class="{ 'rec-card__rank--hot': index < 3 }"
                :style="index < 3 ? { background: medalColors[index] } : {}"
              >
                {{ index < 3 ? 'TOP' : index + 1 }}
              </div>

              <div class="rec-card__ring">
                <el-progress
                  type="dashboard"
                  :width="104"
                  :percentage="item.matchScore"
                  :color="scoreColor(item.matchScore)"
                >
                  <template #default="{ percentage }">
                    <div class="match-num">
                      <strong>{{ percentage }}</strong>
                      <span>匹配度</span>
                    </div>
                  </template>
                </el-progress>
              </div>

              <div class="rec-card__body">
                <div class="rec-card__job">
                  <h3 :title="item.jobName">{{ item.jobName }}</h3>
                  <el-tag v-if="item.jobType" size="small" type="primary" effect="plain">
                    {{ item.jobType }}
                  </el-tag>
                </div>
                <div class="rec-card__company">
                  <el-avatar :size="22" :src="item.companyLogo || undefined">
                    {{ item.companyName?.[0] || '企' }}
                  </el-avatar>
                  <span :title="item.companyName">{{ item.companyName }}</span>
                  <i>{{ item.city }}</i>
                </div>
                <div class="rec-card__salary grad-text">
                  {{ salaryText(item.salaryMin, item.salaryMax) }}
                </div>
              </div>

              <div class="rec-card__subs">
                <div v-for="sub in subScores(item)" :key="sub.label" class="sub-row">
                  <span>{{ sub.label }}</span>
                  <div class="sub-row__bar">
                    <i
                      :style="{
                        width: `${sub.value}%`,
                        background: scoreColor(sub.value),
                      }"
                    ></i>
                  </div>
                  <em>{{ sub.value }}</em>
                </div>
              </div>

              <div class="rec-card__skills">
                <el-tag
                  v-for="skill in item.matchedSkills"
                  :key="`m-${skill}`"
                  size="small"
                  type="success"
                  effect="light"
                >
                  ✓ {{ skill }}
                </el-tag>
                <el-tag
                  v-for="skill in item.missingSkills"
                  :key="`x-${skill}`"
                  size="small"
                  type="danger"
                  effect="light"
                >
                  ✗ {{ skill }}
                </el-tag>
              </div>

              <p class="rec-card__reason" :title="item.reason">{{ item.reason }}</p>

              <div class="rec-card__actions">
                <el-button text type="primary" @click="router.push(`/jobs/${item.jobId}`)">
                  查看岗位
                </el-button>
                <el-button text type="success" @click="router.push(`/recommend/${item.jobId}`)">
                  <el-icon><DataAnalysis /></el-icon>匹配分析
                </el-button>
                <el-button text @click="router.push({ path: '/ai/interview', query: { jobId: item.jobId } })">模拟面试</el-button>
                <button class="grad-btn apply-btn" type="button" @click="openApply(item)">
                  <el-icon><Promotion /></el-icon>投递
                </button>
              </div>
            </div>
          </div>
        </div>
      </template>
    </div>

    <!-- 投递弹窗 -->
    <el-dialog
      :model-value="Boolean(applyTarget)"
      title="投递职位"
      width="520px"
      :close-on-click-modal="false"
      @update:model-value="handleApplyDialogUpdate"
    >
      <template v-if="applyTarget">
        <div class="dialog-job">
          <strong>{{ applyTarget.jobName }}</strong>
          <span class="grad-text">{{ salaryText(applyTarget.salaryMin, applyTarget.salaryMax) }}</span>
          <span>{{ applyTarget.companyName }} · {{ applyTarget.city }}</span>
        </div>

        <el-form label-position="top">
          <el-form-item label="选择简历" required>
            <el-select v-model="applyForm.resumeId" placeholder="请选择简历" style="width: 100%">
              <el-option
                v-for="resume in resumes"
                :key="resume.id"
                :value="resume.id"
                :label="`${resume.name || '未命名简历'}（完成度 ${resume.completionScore}%）`"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="求职信（选填）">
            <el-input
              v-model="applyForm.coverLetter"
              type="textarea"
              :rows="4"
              maxlength="1000"
              show-word-limit
              placeholder="简单介绍自己，或说明与岗位的匹配点"
            />
          </el-form-item>
        </el-form>
      </template>
      <template #footer>
        <el-button @click="applyTarget = null">取消</el-button>
        <el-button type="primary" :loading="applying" @click="submitApply">
          确认投递
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.recommend__inner {
  max-width: 1200px;
  margin: 0 auto;
}

/* ===== 算法横幅 ===== */
.algo-banner {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 40px;
  overflow: hidden;
  padding: 36px 44px;
  border-radius: var(--radius);
  color: #fff;
  background: linear-gradient(135deg, #0a1030 0%, #1e2f7a 55%, #0e7490 100%);
}

.algo-banner__glow {
  position: absolute;
  right: -80px;
  top: -120px;
  width: 380px;
  height: 380px;
  border-radius: 50%;
  background: radial-gradient(closest-side, rgba(34, 211, 238, 0.35), transparent);
  pointer-events: none;
}

.algo-banner__text {
  position: relative;
  max-width: 520px;
}

.algo-banner__text h1 {
  margin: 0;
  font-size: 30px;
  font-weight: 650;
}

.algo-banner__text p {
  margin: 12px 0 0;
  color: rgba(226, 232, 240, 0.85);
  font-size: 14px;
  line-height: 1.8;
}

.algo-banner__score-hint {
  margin-top: 12px;
  color: rgba(226, 232, 240, 0.7);
  font-size: 13px;
}

.algo-banner__score-hint em {
  margin-left: 6px;
  font-style: normal;
  color: #a5f3fc;
  font-weight: 700;
}

.algo-weights {
  position: relative;
  display: flex;
  flex-direction: column;
  gap: 14px;
  width: 300px;
  padding: 18px 22px;
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.08);
  backdrop-filter: blur(6px);
  border: 1px solid rgba(255, 255, 255, 0.16);
}

.algo-weight__bar {
  height: 8px;
  border-radius: 999px;
  overflow: hidden;
  background: rgba(255, 255, 255, 0.16);
}

.algo-weight__bar span {
  display: block;
  height: 100%;
  border-radius: 999px;
}

.algo-weight__meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 6px;
  font-size: 13px;
}

.algo-weight__meta strong {
  font-weight: 600;
}

.algo-weight__meta em {
  font-style: normal;
  color: #a5f3fc;
  font-weight: 700;
}

.no-resume {
  margin-top: 24px;
  padding: 36px;
}

.no-resume__hint {
  margin: -6px 0 18px;
  color: var(--c-text-3);
  font-size: 13px;
}

/* ===== 列表 ===== */
.rec-head {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 16px;
  margin: 30px 0 18px;
}

.rec-head__title {
  margin: 0;
  font-size: 20px;
  font-weight: 650;
}

.rec-head__desc {
  margin: 6px 0 0;
  color: var(--c-text-2);
  font-size: 13px;
}

.rec-list {
  min-height: 300px;
}

.rec-cards {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.rec-card {
  position: relative;
  display: flex;
  align-items: center;
  gap: 22px;
  padding: 22px 24px 22px 30px;
  transition: box-shadow 0.2s ease;
}

.rec-card:hover {
  box-shadow: var(--shadow-md);
}

.rec-card__rank {
  position: absolute;
  left: 0;
  top: 24px;
  bottom: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  border-radius: 0 10px 10px 0;
  color: #64748b;
  font-size: 12px;
  font-weight: 650;
  background: #f1f5f9;
  writing-mode: vertical-lr;
}

.rec-card__rank--hot {
  color: #fff;
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.15);
}

.rec-card__ring {
  flex: none;
}

.match-num {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.match-num strong {
  color: var(--c-text);
  font-size: 24px;
  font-weight: 650;
  line-height: 1.1;
}

.match-num span {
  margin-top: 4px;
  color: var(--c-text-3);
  font-size: 11px;
}

.rec-card__body {
  flex: 1;
  min-width: 200px;
}

.rec-card__job {
  display: flex;
  align-items: center;
  gap: 10px;
}

.rec-card__job h3 {
  margin: 0;
  max-width: 260px;
  overflow: hidden;
  font-size: 18px;
  font-weight: 700;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.rec-card__company {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 8px;
  color: var(--c-text-2);
  font-size: 13px;
}

.rec-card__company span {
  max-width: 180px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.rec-card__company i {
  color: var(--c-text-3);
  font-style: normal;
}

.rec-card__company i::before {
  content: '·';
  margin-right: 8px;
}

.rec-card__salary {
  margin-top: 10px;
  font-size: 20px;
  font-weight: 650;
}

.rec-card__subs {
  width: 150px;
  flex: none;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.sub-row {
  display: grid;
  grid-template-columns: 32px 1fr 26px;
  align-items: center;
  gap: 6px;
}

.sub-row span {
  color: var(--c-text-3);
  font-size: 11px;
}

.sub-row__bar {
  height: 6px;
  border-radius: 999px;
  overflow: hidden;
  background: #eef1f6;
}

.sub-row__bar i {
  display: block;
  height: 100%;
  border-radius: 999px;
}

.sub-row em {
  color: var(--c-text-2);
  font-size: 11px;
  font-style: normal;
  text-align: right;
}

.rec-card__skills {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  width: 210px;
  flex: none;
}

.rec-card__reason {
  width: 240px;
  margin: 0;
  color: var(--c-text-2);
  font-size: 12px;
  line-height: 1.8;
  flex: none;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.rec-card__actions {
  display: flex;
  flex-direction: column;
  align-items: stretch;
  gap: 4px;
  flex: none;
}

.apply-btn {
  height: 36px;
  border-radius: 10px;
  font-size: 13px;
}

.dialog-job {
  display: flex;
  align-items: center;
  gap: 14px;
  margin-bottom: 16px;
  padding: 12px 14px;
  border-radius: 10px;
  background: #f7f9ff;
  border: 1px solid #e3e8fb;
}

.dialog-job strong {
  font-size: 15px;
}

.dialog-job span {
  color: var(--c-text-2);
  font-size: 13px;
}

@media (max-width: 1080px) {
  .algo-banner {
    flex-direction: column;
    align-items: flex-start;
  }

  .algo-weights {
    width: 100%;
  }

  .rec-card {
    flex-wrap: wrap;
  }

  .rec-card__reason {
    width: 100%;
  }

  .rec-card__actions {
    width: 100%;
    flex-direction: row;
    justify-content: flex-end;
  }
}
</style>
