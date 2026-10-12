<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import type { CompanyVO, JobVO, MatchResultVO, OptimizeResultVO, ResumeVO } from '@/types/api'
import { getJob } from '@/api/job'
import { publicCompany } from '@/api/company'
import { addFavorite, removeFavorite } from '@/api/favorite'
import { listResumes } from '@/api/resume'
import { apply } from '@/api/application'
import { matchJob, optimizeResume } from '@/api/ai'
import { submitReport } from '@/api/report'
import { useAuthStore } from '@/stores/auth'
import { applicationStatusMap, jobStatusMap, salaryText, tagOf } from '@/utils/format'

defineOptions({ name: 'JobDetailView' })

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const isUser = computed(() => auth.isLoggedIn && auth.role === 'USER')
const jobId = computed(() => Number(route.params.id))

const job = ref<JobVO | null>(null)
const company = ref<CompanyVO | null>(null)
const loading = ref(true)
const favBusy = ref(false)

/* ===== 简历与投递 ===== */
const resumes = ref<ResumeVO[]>([])
const applyDialog = ref(false)
const applying = ref(false)
const applyForm = reactive({
  resumeId: undefined as number | undefined,
  coverLetter: '',
})

async function loadResumes(): Promise<void> {
  if (!isUser.value) return
  try {
    resumes.value = await listResumes()
    if (resumes.value.length > 0) {
      applyForm.resumeId = resumes.value[0].id
    }
  } catch {
    resumes.value = []
  }
}

/* ===== 岗位与公司 ===== */
async function loadJob(): Promise<void> {
  if (!Number.isFinite(jobId.value) || jobId.value <= 0) {
    job.value = null
    loading.value = false
    return
  }
  loading.value = true
  try {
    const data = await getJob(jobId.value)
    job.value = data
    if (data.companyId) {
      try {
        company.value = await publicCompany(data.companyId)
      } catch {
        company.value = null
      }
    }
    loadResumes()
  } catch {
    job.value = null
  } finally {
    loading.value = false
  }
}

/* ===== 收藏 ===== */
async function toggleFavorite(): Promise<void> {
  if (!job.value || favBusy.value) return
  if (!isUser.value) {
    ElMessage.info('登录后即可收藏职位')
    router.push({ path: '/login', query: { redirect: route.fullPath } })
    return
  }
  favBusy.value = true
  const target = !job.value.favorite
  try {
    if (target) await addFavorite(job.value.id)
    else await removeFavorite(job.value.id)
    if (job.value) job.value.favorite = target
    ElMessage.success(target ? '收藏成功' : '已取消收藏')
  } catch {
    /* 提示由拦截器处理 */
  } finally {
    favBusy.value = false
  }
}

/* ===== 立即投递 ===== */
function openApply(): void {
  if (!job.value) return
  if (!isUser.value) {
    router.push({ path: '/login', query: { redirect: route.fullPath } })
    return
  }
  if (job.value.applicationStatus) {
    ElMessage.info('你已投递过该职位，请耐心等待企业反馈')
    return
  }
  applyDialog.value = true
}

async function submitApply(): Promise<void> {
  if (!job.value || applying.value) return
  if (!applyForm.resumeId) {
    ElMessage.warning('请选择要投递的简历')
    return
  }
  applying.value = true
  try {
    await apply({
      jobId: job.value.id,
      resumeId: applyForm.resumeId,
      coverLetter: applyForm.coverLetter.trim() || undefined,
    })
    ElMessage.success('投递成功，祝你早日拿到 offer！')
    applyDialog.value = false
    applyForm.coverLetter = ''
    if (job.value) job.value.applicationStatus = 'PENDING'
  } catch {
    /* 提示由拦截器处理 */
  } finally {
    applying.value = false
  }
}

/* ===== AI 匹配度分析 ===== */
const matchLoading = ref(false)
const match = ref<MatchResultVO | null>(null)
const matchFailed = ref(false)

async function runMatch(): Promise<void> {
  if (!job.value || matchLoading.value) return
  matchLoading.value = true
  matchFailed.value = false
  try {
    match.value = await matchJob({ jobId: job.value.id })
  } catch {
    match.value = null
    matchFailed.value = true
  } finally {
    matchLoading.value = false
  }
}

/* ===== AI 优化简历（弹窗内自动触发） ===== */
const optimizeDialog = ref(false)
const optimizing = ref(false)
const optimizeResult = ref<OptimizeResultVO | null>(null)
const optimizeFailed = ref(false)
const openItemIndexes = ref<number[]>([0])

async function openOptimize(): Promise<void> {
  if (optimizing.value) return
  if (resumes.value.length === 0) {
    router.push('/resume')
    return
  }
  optimizeResult.value = null
  optimizeFailed.value = false
  optimizeDialog.value = true
  optimizing.value = true
  try {
    optimizeResult.value = await optimizeResume({
      resumeId: resumes.value[0].id,
      jobId: jobId.value,
    })
    openItemIndexes.value = optimizeResult.value.items.map((_, index) => index)
  } catch {
    optimizeFailed.value = true
  } finally {
    optimizing.value = false
  }
}

/* ===== 举报 ===== */
const reportDialog = ref(false)
const reporting = ref(false)
const reportReasons = ['虚假职位信息', '薪资与实际不符', '疑似诈骗', '其他违规内容']
const reportForm = reactive({
  reason: reportReasons[0],
  detail: '',
})

async function submitReportJob(): Promise<void> {
  if (!job.value || reporting.value) return
  reporting.value = true
  try {
    await submitReport({
      targetType: 'JOB',
      targetId: job.value.id,
      reason: reportForm.reason,
      detail: reportForm.detail.trim() || undefined,
    })
    ElMessage.success('举报已提交，平台将尽快核实处理')
    reportDialog.value = false
    reportForm.detail = ''
  } catch {
    /* 提示由拦截器处理 */
  } finally {
    reporting.value = false
  }
}

function matchPercent(): number {
  return match.value?.matchScore ?? 0
}

function companyDesc(): string {
  return (
    company.value?.description ||
    '该企业暂未完善介绍，可查看其发布的职位了解更多。'
  )
}

onMounted(loadJob)
</script>

<template>
  <div class="page job-detail">
    <div class="job-detail__inner">
      <div v-loading="loading" class="job-detail__body">
        <!-- 加载失败 -->
        <el-result
          v-if="!loading && !job"
          icon="error"
          title="职位不存在或已删除"
          sub-title="该职位可能已下线，去看看其他机会吧"
        >
          <template #extra>
            <el-button type="primary" @click="router.push('/jobs')">返回职位列表</el-button>
          </template>
        </el-result>

        <template v-else-if="job">
          <div class="detail-grid">
            <!-- 左侧主体 -->
            <div class="detail-main">
              <div class="panel head-card">
                <div class="head-card__back">
                  <el-button text @click="router.push('/jobs')">
                    <el-icon><ArrowLeft /></el-icon>&nbsp;返回职位列表
                  </el-button>
                </div>
                <div class="head-card__title-row">
                  <div class="head-card__titles">
                    <h1 class="head-card__name">{{ job.jobName }}</h1>
                    <div class="head-card__tags">
                      <el-tag
                        :type="tagOf(jobStatusMap, job.status).type"
                        effect="light"
                      >
                        {{ tagOf(jobStatusMap, job.status).label }}
                      </el-tag>
                      <el-tag v-if="job.jobType" type="primary" effect="plain">{{ job.jobType }}</el-tag>
                      <el-tag
                        v-if="job.applicationStatus"
                        :type="tagOf(applicationStatusMap, job.applicationStatus).type"
                      >
                        已投递（{{ tagOf(applicationStatusMap, job.applicationStatus).label }}）
                      </el-tag>
                    </div>
                  </div>
                  <div class="head-card__salary grad-text">
                    {{ salaryText(job.salaryMin, job.salaryMax) }}
                  </div>
                </div>

                <div class="head-card__meta">
                  <span><el-icon><Location /></el-icon>{{ job.city || '地点不限' }}</span>
                  <span>{{ job.education || '学历不限' }}</span>
                  <span>{{ job.experience || '经验不限' }}</span>
                  <span><el-icon><View /></el-icon>{{ job.viewCount }} 次浏览</span>
                </div>

                <div class="head-card__skills">
                  <template v-if="job.skills && job.skills.length > 0">
                    <span class="head-card__skill-label">技能要求：</span>
                    <el-tag
                      v-for="skill in job.skills"
                      :key="skill.id ?? skill.skillName"
                      type="info"
                      effect="plain"
                    >
                      {{ skill.skillName }}
                    </el-tag>
                  </template>
                  <span v-else class="no-skill">暂无技能标签</span>
                </div>
              </div>

              <div class="panel section-card">
                <h2 class="section-card__title"><el-icon><Document /></el-icon>职位描述</h2>
                <div class="rich-text">{{ job.description || '暂无职位描述' }}</div>
              </div>

              <div class="panel section-card">
                <h2 class="section-card__title"><el-icon><Checked /></el-icon>任职要求</h2>
                <div class="rich-text">{{ job.requirements || '暂无任职要求' }}</div>
              </div>

              <!-- 企业信息 -->
              <div class="panel company-card">
                <h2 class="section-card__title"><el-icon><OfficeBuilding /></el-icon>公司信息</h2>
                <div class="company-card__row">
                  <el-avatar :size="56" :src="company?.logo || job.companyLogo || undefined" class="company-card__logo">
                    {{ job.companyName?.[0] || '企' }}
                  </el-avatar>
                  <div class="company-card__info">
                    <div class="company-card__name">{{ job.companyName }}</div>
                    <div class="company-card__chips">
                      <el-tag v-if="company?.industry || job.companyIndustry" size="small" effect="plain" type="primary">
                        {{ company?.industry || job.companyIndustry }}
                      </el-tag>
                      <el-tag v-if="company?.scale" size="small" effect="plain">{{ company.scale }}</el-tag>
                      <el-tag v-if="company?.jobCount != null" size="small" effect="plain">
                        在招 {{ company.jobCount }} 个职位
                      </el-tag>
                    </div>
                  </div>
                </div>
                <div v-if="company?.address" class="company-card__address">
                  <el-icon><Location /></el-icon>{{ company.address }}
                </div>
                <p class="company-card__desc">{{ companyDesc() }}</p>
              </div>
            </div>

            <!-- 右侧操作栏（sticky） -->
            <aside class="detail-side">
              <div class="panel action-card">
                <template v-if="isUser">
                  <template v-if="job.applicationStatus">
                    <div class="applied-tip">
                      <el-icon :size="22"><CircleCheckFilled /></el-icon>
                      <div>
                        <strong>已投递该职位</strong>
                        <p>状态：{{ tagOf(applicationStatusMap, job.applicationStatus).label }}，请留意企业反馈</p>
                      </div>
                    </div>
                    <el-button class="grad-btn action-btn" @click="router.push('/applications')">
                      查看我的投递
                    </el-button>
                  </template>

                  <template v-else-if="job.status === 'ONLINE'">
                    <button class="grad-btn action-btn" type="button" @click="openApply">
                      <el-icon><Promotion /></el-icon>立即投递
                    </button>

                    <template v-if="resumes.length > 0">
                      <el-button
                        class="action-btn match-btn"
                        :loading="matchLoading"
                        @click="runMatch"
                      >
                        <el-icon><MagicStick /></el-icon>AI 匹配度分析
                      </el-button>

                      <div v-if="match" class="match-result">
                        <div class="match-result__head">
                          <el-progress
                            type="dashboard"
                            :width="84"
                            :percentage="matchPercent()"
                            :color="matchPercent() >= 80 ? '#22c55e' : matchPercent() >= 60 ? '#4f6bf6' : '#f59e0b'"
                          >
                            <template #default="{ percentage }">
                              <span class="match-result__score">{{ percentage }}<i>%</i></span>
                            </template>
                          </el-progress>
                          <div class="match-result__labels">
                            <strong>{{ matchPercent() >= 80 ? '非常适合' : matchPercent() >= 60 ? '比较适合' : '需要提升' }}</strong>
                            <span>人岗匹配度</span>
                          </div>
                        </div>
                        <div class="match-result__skills">
                          <el-tag v-if="match.matchedSkills.length" size="small" type="success" effect="light">
                            匹配 {{ match.matchedSkills.length }} 项技能
                          </el-tag>
                          <el-tag v-if="match.missingSkills.length" size="small" type="danger" effect="light">
                            缺失 {{ match.missingSkills.length }} 项技能
                          </el-tag>
                        </div>
                        <p class="match-result__reason" :title="match.reason">{{ match.reason }}</p>
                        <p>{{ match.executionNote }}</p>
                        <el-button type="primary" text size="small" @click="router.push(`/recommend/${job.id}`)">
                          查看完整匹配分析 <el-icon><ArrowRight /></el-icon>
                        </el-button>
                        <el-button type="primary" plain size="small" :loading="optimizing" @click="openOptimize">
                          <el-icon><MagicStick /></el-icon>AI 优化简历
                        </el-button>
                      </div>

                      <p v-else-if="matchFailed" class="match-tip">
                        分析失败，请确认简历完整后重试
                      </p>
                    </template>
                    <template v-else>
                      <div class="no-resume-tip">
                        <p>投递前需要先创建简历</p>
                        <el-button type="primary" @click="router.push('/resume')">去创建简历</el-button>
                      </div>
                    </template>
                  </template>

                  <template v-else>
                    <div class="offline-tip">
                      <el-icon :size="20"><WarningFilled /></el-icon>
                      该职位已下线，无法投递
                    </div>
                  </template>
                </template>

                <template v-else>
                  <el-button class="grad-btn action-btn" @click="router.push({ path: '/login', query: { redirect: route.fullPath } })">
                    <el-icon><Lock /></el-icon>登录后投递
                  </el-button>
                  <p class="guest-tip">登录求职者账号后可收藏、投递并查看 AI 匹配度</p>
                </template>

                <div class="action-card__divider"></div>

                <button
                  type="button"
                  class="fav-line"
                  :disabled="favBusy"
                  @click="toggleFavorite"
                >
                  <el-icon :size="18" :class="{ faved: job.favorite }">
                    <StarFilled v-if="job.favorite" />
                    <Star v-else />
                  </el-icon>
                  {{ job.favorite ? '已收藏' : '收藏职位' }}
                </button>

                <div class="side-foot">
                  <span>发布于 {{ job.createTime?.replace('T', ' ').slice(0, 10) }}</span>
                  <el-button text size="small" type="danger" @click="reportDialog = true">
                    <el-icon><Warning /></el-icon>举报
                  </el-button>
                </div>
              </div>
            </aside>
          </div>
        </template>
      </div>
    </div>

    <!-- ===== 投递弹窗 ===== -->
    <el-dialog v-model="applyDialog" title="投递职位" width="520px" :close-on-click-modal="false">
      <template v-if="job">
        <div class="dialog-job">
          <strong>{{ job.jobName }}</strong>
          <span class="grad-text">{{ salaryText(job.salaryMin, job.salaryMax) }}</span>
          <span>{{ job.companyName }} · {{ job.city }}</span>
        </div>

        <el-empty
          v-if="resumes.length === 0"
          :image-size="90"
          description="还没有简历，投递前请先创建一份"
        >
          <el-button type="primary" @click="router.push('/resume')">去创建简历</el-button>
        </el-empty>

        <el-form v-else label-position="top">
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
              placeholder="简单介绍自己，或说明与岗位的匹配点，让 HR 更快了解你"
            />
          </el-form-item>
        </el-form>
      </template>
      <template #footer>
        <el-button @click="applyDialog = false">取消</el-button>
        <el-button
          type="primary"
          :loading="applying"
          :disabled="resumes.length === 0"
          @click="submitApply"
        >
          确认投递
        </el-button>
      </template>
    </el-dialog>

    <!-- ===== AI 简历优化弹窗 ===== -->
    <el-dialog
      v-model="optimizeDialog"
      title="AI 简历优化"
      width="760px"
      :close-on-click-modal="false"
    >
      <div v-loading="optimizing" class="optimize-body">
        <el-result
          v-if="optimizeFailed"
          icon="error"
          title="优化失败"
          sub-title="AI 服务暂不可用，请稍后再试"
        >
          <template #extra>
            <el-button type="primary" @click="openOptimize">重试</el-button>
          </template>
        </el-result>
        <template v-else-if="optimizeResult">
          <div class="optimize-summary">
            <el-icon :size="18"><MagicStick /></el-icon>
            <p>{{ optimizeResult.summary }}</p>
          </div>
          <el-collapse v-model="openItemIndexes">
            <el-collapse-item
              v-for="(item, index) in optimizeResult.items"
              :key="item.field"
              :name="index"
            >
              <template #title>
                <span class="optimize-field">{{ item.field }}</span>
                <el-tag size="small" type="primary" effect="plain">AI 已优化</el-tag>
              </template>
              <div class="opt-row">
                <div class="opt-col">
                  <div class="opt-col__label">简历原内容</div>
                  <p class="opt-col__text">{{ item.original || '（空）' }}</p>
                </div>
                <el-icon class="opt-arrow"><ArrowRight /></el-icon>
                <div class="opt-col opt-col--grad">
                  <div class="opt-col__label">AI 优化后</div>
                  <p class="opt-col__text">{{ item.optimized || '（空）' }}</p>
                </div>
              </div>
              <div v-if="item.suggestion" class="opt-suggestion">
                <strong>修改建议：</strong>{{ item.suggestion }}
              </div>
              <div v-if="item.keywords.length" class="opt-keywords">
                <span>关键词：</span>
                <el-tag v-for="kw in item.keywords" :key="kw" size="small" type="warning" effect="plain">
                  {{ kw }}
                </el-tag>
              </div>
            </el-collapse-item>
          </el-collapse>
        </template>
      </div>
      <template #footer>
        <el-button @click="optimizeDialog = false">关闭</el-button>
        <el-button type="primary" :loading="optimizing" @click="openOptimize">重新优化</el-button>
      </template>
    </el-dialog>

    <!-- ===== 举报弹窗 ===== -->
    <el-dialog v-model="reportDialog" title="举报该职位" width="480px" :close-on-click-modal="false">
      <el-form label-position="top">
        <el-form-item label="举报原因" required>
          <el-radio-group v-model="reportForm.reason">
            <el-radio v-for="reason in reportReasons" :key="reason" :value="reason">
              {{ reason }}
            </el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="补充说明（选填）">
          <el-input
            v-model="reportForm.detail"
            type="textarea"
            :rows="3"
            maxlength="300"
            placeholder="请补充具体情况，便于平台核实"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="reportDialog = false">取消</el-button>
        <el-button type="danger" :loading="reporting" @click="submitReportJob">提交举报</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.job-detail__inner {
  max-width: 1200px;
  margin: 0 auto;
}

.job-detail__body {
  min-height: 60vh;
}

.detail-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 340px;
  gap: 20px;
  align-items: start;
}

/* ===== 左侧卡片 ===== */
.head-card,
.section-card,
.company-card {
  padding: 24px;
  margin-bottom: 18px;
}

.head-card__back {
  margin: -10px 0 8px -12px;
}

.head-card__title-row {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 20px;
}

.head-card__name {
  margin: 0;
  font-size: 26px;
  font-weight: 650;
  color: var(--c-text);
}

.head-card__tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 12px;
}

.head-card__salary {
  flex: none;
  font-size: 26px;
  font-weight: 650;
}

.head-card__meta {
  display: flex;
  flex-wrap: wrap;
  gap: 22px;
  margin-top: 18px;
  color: var(--c-text-2);
  font-size: 14px;
}

.head-card__meta span {
  display: inline-flex;
  align-items: center;
  gap: 5px;
}

.head-card__skills {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
  margin-top: 20px;
  padding-top: 16px;
  border-top: 1px dashed var(--c-border);
}

.head-card__skill-label {
  color: var(--c-text-2);
  font-size: 13px;
}

.no-skill {
  color: var(--c-text-3);
  font-size: 13px;
}

.section-card__title {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 0 0 14px;
  font-size: 17px;
  font-weight: 700;
}

.section-card__title .el-icon {
  color: var(--c-primary);
}

.rich-text {
  color: #374151;
  font-size: 14px;
  line-height: 2;
  white-space: pre-line;
  word-break: break-word;
}

/* 公司卡片 */
.company-card__row {
  display: flex;
  align-items: center;
  gap: 14px;
}

.company-card__logo {
  flex: none;
  background: var(--c-primary);
  color: #fff;
  font-weight: 600;
}

.company-card__name {
  font-size: 16px;
  font-weight: 700;
}

.company-card__chips {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: 8px;
}

.company-card__address {
  display: flex;
  align-items: center;
  gap: 5px;
  margin-top: 16px;
  color: var(--c-text-2);
  font-size: 13px;
}

.company-card__desc {
  margin: 12px 0 0;
  color: var(--c-text-2);
  font-size: 13px;
  line-height: 1.9;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

/* ===== 右侧操作栏 ===== */
.detail-side {
  position: sticky;
  top: 88px;
}

.action-card {
  padding: 22px;
}

.action-btn {
  width: 100%;
  height: 44px;
  font-size: 15px;
  border-radius: 12px;
  margin-bottom: 12px;
}

.match-btn {
  color: var(--c-primary);
  background: #fff;
  border: 1px solid rgba(79, 107, 246, 0.45);
  box-shadow: none;
}

.match-btn:hover {
  background: rgba(79, 107, 246, 0.05);
  border-color: var(--c-primary);
}

.match-result {
  padding: 16px;
  border-radius: 12px;
  background: var(--c-primary-soft);
  border: 1px solid #dbe3fd;
}

.match-result__head {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
}

.match-result__score {
  font-size: 18px;
  font-weight: 650;
  color: var(--c-text);
}

.match-result__score i {
  font-size: 11px;
  font-style: normal;
}

.match-result__labels {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.match-result__labels strong {
  font-size: 16px;
}

.match-result__labels span {
  color: var(--c-text-2);
  font-size: 12px;
}

.match-result__skills {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: 10px;
}

.match-result__reason {
  margin: 10px 0 6px;
  color: var(--c-text-2);
  font-size: 12px;
  line-height: 1.7;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.match-tip {
  margin: 0;
  color: #b45309;
  font-size: 13px;
  text-align: center;
}

.no-resume-tip {
  margin-top: 4px;
  padding: 16px;
  text-align: center;
  border-radius: 12px;
  background: #fffbeb;
  border: 1px dashed #fcd34d;
}

.no-resume-tip p {
  margin: 0 0 10px;
  color: #92400e;
  font-size: 13px;
}

.applied-tip {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  margin-bottom: 14px;
  padding: 14px;
  border-radius: 12px;
  background: #f0fdf4;
  border: 1px solid #bbf7d0;
}

.applied-tip .el-icon {
  color: #16a34a;
  flex: none;
}

.applied-tip strong {
  font-size: 14px;
}

.applied-tip p {
  margin: 4px 0 0;
  color: var(--c-text-2);
  font-size: 12px;
}

.offline-tip {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 14px;
  border-radius: 12px;
  color: #92400e;
  font-size: 13px;
  background: #fffbeb;
  border: 1px solid #fde68a;
}

.guest-tip {
  margin: 4px 0 0;
  color: var(--c-text-3);
  font-size: 12px;
  text-align: center;
  line-height: 1.7;
}

.action-card__divider {
  margin: 16px 0;
  border-top: 1px dashed var(--c-border);
}

.fav-line {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  width: 100%;
  padding: 10px 0;
  border: none;
  border-radius: 10px;
  cursor: pointer;
  color: var(--c-text-2);
  font-size: 14px;
  background: #f8f9fc;
  transition: border-color 0.2s ease, background-color 0.2s ease, box-shadow 0.2s ease;
}

.fav-line:hover {
  color: #f59e0b;
  background: rgba(245, 158, 11, 0.1);
}

.fav-line .faved {
  color: #f59e0b;
}

.side-foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 8px;
  color: var(--c-text-3);
  font-size: 12px;
}

/* ===== 弹窗内容 ===== */
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

.optimize-body {
  min-height: 260px;
}

.optimize-summary {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  margin-bottom: 14px;
  padding: 12px 14px;
  border-radius: 10px;
  color: #1e40af;
  background: #eef2ff;
  border: 1px solid #c7d2fe;
}

.optimize-summary .el-icon {
  flex: none;
  margin-top: 2px;
}

.optimize-summary p {
  margin: 0;
  font-size: 13px;
  line-height: 1.8;
}

.optimize-field {
  margin-right: 10px;
  font-weight: 600;
}

.opt-row {
  display: flex;
  align-items: stretch;
  gap: 12px;
}

.opt-col {
  flex: 1;
  min-width: 0;
  padding: 12px;
  border-radius: 10px;
  background: #f8f9fb;
  border: 1px solid var(--c-border);
}

.opt-col--grad {
  background: linear-gradient(135deg, #eef1fe, #e7fbff);
  border-color: #cfd9fd;
}

.opt-col__label {
  margin-bottom: 8px;
  color: var(--c-text-2);
  font-size: 12px;
  font-weight: 600;
}

.opt-col__text {
  margin: 0;
  color: #374151;
  font-size: 13px;
  line-height: 1.8;
  white-space: pre-line;
  word-break: break-word;
}

.opt-arrow {
  align-self: center;
  flex: none;
  color: var(--c-primary);
}

.opt-suggestion {
  margin-top: 10px;
  color: #b45309;
  font-size: 13px;
  line-height: 1.8;
}

.opt-keywords {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 6px;
  margin-top: 10px;
}

.opt-keywords span {
  color: var(--c-text-2);
  font-size: 13px;
}

@media (max-width: 1000px) {
  .detail-grid {
    grid-template-columns: 1fr;
  }

  .detail-side {
    position: static;
  }
}
</style>
