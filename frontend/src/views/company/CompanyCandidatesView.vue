<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  companyApplications,
  companyCandidateAnalysis,
  companyJobs,
  updateCompanyApplicationStatus,
} from '@/api/company'
import type { ApplicationStatus, ApplicationVO, CompanyCandidateMatchVO, JobVO } from '@/types/api'
import { applicationStatusMap, tagOf } from '@/utils/format'

defineOptions({ name: 'CompanyCandidatesView' })

const route = useRoute()

const loading = ref(false)
const rows = ref<ApplicationVO[]>([])
const jobs = ref<JobVO[]>([])
const drawerOpen = ref(false)
const current = ref<ApplicationVO | null>(null)
const analysis = ref<CompanyCandidateMatchVO | null>(null)
const analysisLoading = ref(false)
const busy = ref('')

const filters = reactive<{
  jobId: '' | number
  status: '' | ApplicationStatus
  keyword: string
}>({
  jobId: '',
  status: '',
  keyword: '',
})

const statusOptions = Object.keys(applicationStatusMap) as ApplicationStatus[]

const successText: Record<string, string> = {
  VIEWED: '已标记为已查看',
  INTERVIEW: '已邀请面试',
  REJECTED: '已标记为不合适',
  HIRED: '已录用该候选人',
}

async function fetchJobs(): Promise<void> {
  try {
    const res = await companyJobs({ page: 1, size: 100 })
    jobs.value = res.records
  } catch {
    // 拦截器已提示
  }
}

async function fetchRows(): Promise<void> {
  loading.value = true
  try {
    const res = await companyApplications({
      jobId: filters.jobId || undefined,
      status: filters.status || undefined,
      page: 1,
      size: 100,
    })
    rows.value = res.records
  } catch {
    // 错误提示由请求拦截器统一处理
  } finally {
    loading.value = false
  }
}

function handleSearch(): void {
  void fetchRows()
}

const filteredRows = computed<ApplicationVO[]>(() => {
  const keyword = filters.keyword.trim().toLowerCase()
  if (!keyword) return rows.value
  return rows.value.filter((row) => {
    const haystack = [
      row.candidateName,
      row.jobName,
      row.resumeName,
      row.candidatePhone,
      row.coverLetter,
    ]
      .filter(Boolean)
      .join(' ')
      .toLowerCase()
    return haystack.includes(keyword)
  })
})

function openDetail(row: ApplicationVO): void {
  current.value = row
  analysis.value = null
  drawerOpen.value = true
}

async function runAnalysis(): Promise<void> {
  if (!current.value || analysisLoading.value) return
  analysisLoading.value = true
  analysis.value = null
  try {
    analysis.value = await companyCandidateAnalysis(current.value.id)
  } catch {
    // 拦截器已提示
  } finally {
    analysisLoading.value = false
  }
}

function ringColor(score?: number | null): string {
  if (score == null) return '#9ca3af'
  if (score >= 85) return '#10b981'
  if (score >= 70) return '#4f6bf6'
  if (score >= 60) return '#f59e0b'
  return '#ef4444'
}

function scoreColor(score?: number | null): string {
  return ringColor(score)
}

async function runStatus(row: ApplicationVO, status: Exclude<ApplicationStatus, 'PENDING'>): Promise<void> {
  if (busy.value) return
  if (status === 'REJECTED' || status === 'HIRED') {
    const text =
      status === 'REJECTED'
        ? `确定将候选人「${row.candidateName}」标记为不合适吗？`
        : `确定录用候选人「${row.candidateName}」吗？`
    try {
      await ElMessageBox.confirm(text, '操作确认', {
        type: status === 'REJECTED' ? 'warning' : 'success',
        confirmButtonText: '确定',
        cancelButtonText: '取消',
      })
    } catch {
      return
    }
  }
  busy.value = `${row.id}-${status}`
  try {
    await updateCompanyApplicationStatus(row.id, status)
    row.status = status
    ElMessage.success(successText[status] ?? '操作成功')
  } catch {
    // 拦截器已提示
  } finally {
    busy.value = ''
  }
}

const match = computed(() => analysis.value?.match ?? null)

onMounted(() => {
  const qJobId = route.query.jobId
  const qStatus = route.query.status
  if (qJobId && /^\d+$/.test(String(qJobId))) filters.jobId = Number(qJobId)
  if (qStatus && statusOptions.includes(qStatus as ApplicationStatus)) {
    filters.status = qStatus as ApplicationStatus
  }
  void fetchJobs()
  void fetchRows()
})
</script>

<template>
  <div class="cand-page">
    <div class="panel list-panel">
      <div class="cand-head">
        <div>
          <div class="cand-head__title">候选人库</div>
          <div class="cand-head__sub">
            汇聚全部投递候选人，点击行查看 AI 深度分析（需候选人已完善简历）
          </div>
        </div>
        <el-tag type="primary" effect="plain" round>
          <el-icon style="margin-right: 4px"><MagicStick /></el-icon>AI 智能分析
        </el-tag>
      </div>

      <div class="cand-filters">
        <el-select
          v-model="filters.jobId"
          placeholder="全部岗位"
          clearable
          filterable
          class="job-select"
          @change="handleSearch"
        >
          <el-option v-for="job in jobs" :key="job.id" :label="job.jobName" :value="job.id" />
        </el-select>
        <el-select
          v-model="filters.status"
          placeholder="全部状态"
          clearable
          class="status-select"
          @change="handleSearch"
        >
          <el-option
            v-for="key in statusOptions"
            :key="key"
            :label="applicationStatusMap[key].label"
            :value="key"
          />
        </el-select>
        <el-input
          v-model="filters.keyword"
          placeholder="搜索候选人 / 岗位 / 简历"
          clearable
          class="keyword-input"
          @keyup.enter="handleSearch"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>
        <el-button type="primary" @click="handleSearch">
          <el-icon style="margin-right: 4px"><Search /></el-icon>查询
        </el-button>
      </div>

      <div v-loading="loading" class="table-wrap">
        <el-table
          :data="filteredRows"
          stripe
          class="cand-table"
          highlight-current-row
          @row-click="(row: ApplicationVO) => openDetail(row)"
        >
          <el-table-column label="候选人" min-width="200">
            <template #default="{ row }">
              <div class="candidate-cell">
                <el-avatar :size="38" :src="row.candidateAvatar || undefined" class="candidate-avatar">
                  {{ (row.candidateName || '候').charAt(0) }}
                </el-avatar>
                <div class="candidate-main">
                  <div class="candidate-name">{{ row.candidateName }}</div>
                  <div class="candidate-contact">
                    <span v-if="row.candidatePhone">{{ row.candidatePhone }}</span>
                    <span v-else>暂无联系方式</span>
                  </div>
                </div>
              </div>
            </template>
          </el-table-column>
          <el-table-column prop="jobName" label="投递岗位" min-width="160" show-overflow-tooltip />
          <el-table-column label="简历" width="150" show-overflow-tooltip>
            <template #default="{ row }">{{ row.resumeName || '未提交简历' }}</template>
          </el-table-column>
          <el-table-column label="匹配度" width="110" align="center">
            <template #default="{ row }">
              <el-tag v-if="row.matchScore != null" :color="scoreColor(row.matchScore)" effect="dark" round size="small" class="score-badge">
                {{ row.matchScore }}%
              </el-tag>
              <span v-else class="dim-text">-</span>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="110" align="center">
            <template #default="{ row }">
              <el-tag :type="tagOf(applicationStatusMap, row.status).type" round>
                {{ tagOf(applicationStatusMap, row.status).label }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="120" align="center">
            <template #default="{ row }">
              <el-button link type="primary" @click.stop="openDetail(row)">
                <el-icon style="margin-right: 3px"><MagicStick /></el-icon>AI 分析
              </el-button>
            </template>
          </el-table-column>
          <template #empty>
            <el-empty
              :description="filters.keyword ? '未找到匹配的候选人' : '暂无候选人，先去发布岗位收取投递吧'"
              :image-size="90"
            />
          </template>
        </el-table>
      </div>
    </div>

    <el-drawer
      v-model="drawerOpen"
      size="620px"
      :title="current ? `候选人分析 · ${current.candidateName}` : '候选人分析'"
      destroy-on-close
    >
      <div v-if="current" class="drawer-body">
        <div class="identity-card">
          <el-avatar :size="52" :src="current.candidateAvatar || undefined" class="identity-avatar">
            {{ (current.candidateName || '候').charAt(0) }}
          </el-avatar>
          <div class="identity-main">
            <div class="identity-name">
              {{ current.candidateName }}
              <el-tag :type="tagOf(applicationStatusMap, current.status).type" size="small" round>
                {{ tagOf(applicationStatusMap, current.status).label }}
              </el-tag>
            </div>
            <div class="identity-contact">
              <span v-if="current.candidatePhone">
                <el-icon style="margin-right: 3px"><Phone /></el-icon>{{ current.candidatePhone }}
              </span>
              <span v-if="current.candidateEmail">
                <el-icon style="margin-right: 3px"><Message /></el-icon>{{ current.candidateEmail }}
              </span>
              <span v-if="!current.candidatePhone && !current.candidateEmail">暂未填写联系方式</span>
            </div>
          </div>
          <el-button type="primary" :loading="analysisLoading" :disabled="analysisLoading" class="analyze-btn" @click="runAnalysis">
            <el-icon style="margin-right: 4px"><MagicStick /></el-icon>AI 候选人分析
          </el-button>
        </div>

        <el-descriptions :column="1" border class="app-info">
          <el-descriptions-item label="投递岗位">{{ current.jobName }}</el-descriptions-item>
          <el-descriptions-item label="使用简历">{{ current.resumeName || '未提交简历' }}</el-descriptions-item>
          <el-descriptions-item label="岗位类型">{{ current.jobType || '-' }} · {{ current.city || '-' }}</el-descriptions-item>
          <el-descriptions-item label="求职信">
            <span class="letter-text">{{ current.coverLetter || '候选人未填写求职信' }}</span>
          </el-descriptions-item>
        </el-descriptions>

        <div v-loading="analysisLoading" class="analysis-body">
          <template v-if="analysis">
            <template v-if="match">
              <div class="match-overview">
                <div class="ring-wrap">
                  <el-progress
                    type="dashboard"
                    :percentage="match.matchScore ?? 0"
                    :width="150"
                    :color="ringColor(match.matchScore)"
                  >
                    <template #default="{ percentage }">
                      <div class="ring-center">
                        <div class="ring-center__value">{{ percentage }}</div>
                        <div class="ring-center__label">综合匹配</div>
                      </div>
                    </template>
                  </el-progress>
                  <div class="source-hint">
                    来源：{{ match.source === 'LLM' ? '大模型智能分析' : '规则引擎分析' }}
                  </div>
                </div>

                <div class="sub-scores">
                  <div class="sub-score">
                    <div class="sub-score__head">
                      <span>语义匹配</span><b>{{ match.semanticScore ?? 0 }}</b>
                    </div>
                    <el-progress :percentage="match.semanticScore ?? 0" :color="scoreColor(match.semanticScore)" :stroke-width="10" />
                  </div>
                  <div class="sub-score">
                    <div class="sub-score__head">
                      <span>技能匹配</span><b>{{ match.skillScore ?? 0 }}</b>
                    </div>
                    <el-progress :percentage="match.skillScore ?? 0" :color="scoreColor(match.skillScore)" :stroke-width="10" />
                  </div>
                  <div class="sub-score">
                    <div class="sub-score__head">
                      <span>经验匹配</span><b>{{ match.experienceScore ?? 0 }}</b>
                    </div>
                    <el-progress :percentage="match.experienceScore ?? 0" :color="scoreColor(match.experienceScore)" :stroke-width="10" />
                  </div>
                  <div class="sub-score">
                    <div class="sub-score__head">
                      <span>学历匹配</span><b>{{ match.educationScore ?? 0 }}</b>
                    </div>
                    <el-progress :percentage="match.educationScore ?? 0" :color="scoreColor(match.educationScore)" :stroke-width="10" />
                  </div>
                </div>
              </div>

              <el-alert type="info" :closable="false" class="reason-alert" show-icon>
                <template #title>匹配理由</template>
                {{ match.reason }}
              </el-alert>

              <div class="skills-grid">
                <div class="skill-panel matched">
                  <div class="skill-panel__title">已具备技能</div>
                  <div v-if="match.matchedSkills?.length" class="skill-tags">
                    <el-tag v-for="skill in match.matchedSkills" :key="skill" type="success" effect="light" round size="small">
                      <el-icon style="margin-right: 3px"><Check /></el-icon>{{ skill }}
                    </el-tag>
                  </div>
                  <div v-else class="no-skills">无匹配技能</div>
                </div>
                <div class="skill-panel missing">
                  <div class="skill-panel__title">待补足技能</div>
                  <div v-if="match.missingSkills?.length" class="skill-tags">
                    <el-tag v-for="skill in match.missingSkills" :key="skill" type="danger" effect="light" round size="small">
                      <el-icon style="margin-right: 3px"><Close /></el-icon>{{ skill }}
                    </el-tag>
                  </div>
                  <div v-else class="no-skills">技能完全覆盖</div>
                </div>
              </div>

              <div class="panel-groups">
                <div class="analysis-panel strength">
                  <div class="analysis-panel__title">优势亮点</div>
                  <ul class="line-list">
                    <li v-for="(item, index) in analysis.strengths" :key="index">
                      <el-icon color="#10b981"><CircleCheckFilled /></el-icon>{{ item }}
                    </li>
                    <li v-if="!analysis.strengths?.length" class="empty-li">暂无</li>
                  </ul>
                </div>
                <div class="analysis-panel risk">
                  <div class="analysis-panel__title">潜在风险</div>
                  <ul class="line-list">
                    <li v-for="(item, index) in analysis.risks" :key="index">
                      <el-icon color="#f59e0b"><WarningFilled /></el-icon>{{ item }}
                    </li>
                    <li v-if="!analysis.risks?.length" class="empty-li">暂无</li>
                  </ul>
                </div>
                <div class="analysis-panel suggest">
                  <div class="analysis-panel__title">跟进建议</div>
                  <ul class="line-list">
                    <li v-for="(item, index) in analysis.suggestions" :key="index">
                      <el-icon color="#4f6bf6"><Aim /></el-icon>{{ item }}
                    </li>
                    <li v-if="!analysis.suggestions?.length" class="empty-li">暂无</li>
                  </ul>
                </div>
              </div>

              <div v-if="analysis.advice" class="advice-box">
                <el-icon color="#4f6bf6" :size="16"><MagicStick /></el-icon>
                <div class="advice-box__content">{{ analysis.advice }}</div>
              </div>
            </template>

            <template v-else>
              <el-alert
                type="info"
                :closable="false"
                show-icon
                title="候选人暂无有效简历，无法进行结构化匹配"
                class="reason-alert"
              />
              <div v-if="analysis.advice" class="advice-box">
                <el-icon color="#4f6bf6" :size="16"><MagicStick /></el-icon>
                <div class="advice-box__content">{{ analysis.advice }}</div>
              </div>
              <el-empty description="无简历时仅提供文字建议" :image-size="70" />
            </template>
          </template>

          <template v-else>
            <el-empty description="点击上方按钮生成 AI 候选人分析" :image-size="80" />
          </template>
        </div>

        <div class="quick-actions">
          <span class="quick-actions__label">快捷处理：</span>
          <template v-if="current.status === 'PENDING'">
            <el-button size="small" :disabled="busy !== ''" @click="runStatus(current, 'VIEWED')">标记已查看</el-button>
            <el-button size="small" type="success" plain :disabled="busy !== ''" @click="runStatus(current, 'INTERVIEW')">邀请面试</el-button>
            <el-button size="small" type="danger" plain :disabled="busy !== ''" @click="runStatus(current, 'REJECTED')">不合适</el-button>
          </template>
          <template v-else-if="current.status === 'VIEWED'">
            <el-button size="small" type="success" plain :disabled="busy !== ''" @click="runStatus(current, 'INTERVIEW')">邀请面试</el-button>
            <el-button size="small" type="danger" plain :disabled="busy !== ''" @click="runStatus(current, 'REJECTED')">不合适</el-button>
          </template>
          <template v-else-if="current.status === 'INTERVIEW'">
            <el-button size="small" type="success" :disabled="busy !== ''" @click="runStatus(current, 'HIRED')">录用</el-button>
            <el-button size="small" type="danger" plain :disabled="busy !== ''" @click="runStatus(current, 'REJECTED')">不合适</el-button>
          </template>
          <el-tag v-else size="small" type="info" effect="plain">流程已结束</el-tag>
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<style scoped>
.cand-page {
  min-height: 100%;
}

.list-panel {
  padding: 20px 22px;
}

.cand-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  margin-bottom: 18px;
}

.cand-head__title {
  font-size: 17px;
  font-weight: 600;
  color: var(--c-text);
}

.cand-head__sub {
  margin-top: 4px;
  color: var(--c-text-3);
  font-size: 13px;
}

.cand-filters {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
  margin-bottom: 16px;
}

.job-select {
  width: 230px;
}

.status-select {
  width: 150px;
}

.keyword-input {
  width: 260px;
}

.cand-table :deep(.el-table__row) {
  cursor: pointer;
}

.candidate-cell {
  display: flex;
  align-items: center;
  gap: 12px;
}

.candidate-avatar {
  flex: none;
  background: var(--c-primary);
  color: #fff;
  font-weight: 600;
}

.candidate-name {
  font-weight: 600;
  color: var(--c-text);
}

.candidate-contact {
  margin-top: 2px;
  color: var(--c-text-3);
  font-size: 12px;
}

.score-badge {
  color: #fff;
}

.dim-text {
  color: var(--c-text-3);
}

.drawer-body {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.identity-card {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 16px;
  border-radius: 14px;
  background: linear-gradient(120deg, rgba(79, 107, 246, 0.08), rgba(34, 211, 238, 0.08));
  border: 1px solid rgba(79, 107, 246, 0.16);
}

.identity-avatar {
  flex: none;
  background: var(--c-primary);
  color: #fff;
  font-size: 20px;
  font-weight: 700;
}

.identity-main {
  flex: 1;
  min-width: 0;
}

.identity-name {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 16px;
  font-weight: 700;
  color: var(--c-text);
}

.identity-contact {
  display: flex;
  flex-wrap: wrap;
  gap: 4px 16px;
  margin-top: 6px;
  color: var(--c-text-3);
  font-size: 12px;
}

.letter-text {
  color: var(--c-text-2);
  white-space: pre-wrap;
  line-height: 1.7;
}

.analysis-body {
  min-height: 240px;
}

.match-overview {
  display: flex;
  align-items: center;
  gap: 28px;
  padding: 14px 6px 6px;
}

.ring-wrap {
  flex: none;
  text-align: center;
}

.ring-center__value {
  font-size: 30px;
  font-weight: 650;
  color: var(--c-text);
  line-height: 1.1;
}

.ring-center__label {
  margin-top: 2px;
  font-size: 12px;
  color: var(--c-text-3);
}

.source-hint {
  margin-top: 4px;
  font-size: 11px;
  color: var(--c-text-3);
}

.sub-scores {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 12px;
  min-width: 0;
}

.sub-score__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 5px;
  color: var(--c-text-2);
  font-size: 13px;
}

.sub-score__head b {
  color: var(--c-text);
  font-weight: 700;
}

.reason-alert {
  margin-top: 4px;
}

.skills-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 14px;
  margin-top: 6px;
}

.skill-panel {
  padding: 12px 14px;
  border-radius: 12px;
}

.skill-panel.matched {
  background: rgba(16, 185, 129, 0.07);
  border: 1px solid rgba(16, 185, 129, 0.18);
}

.skill-panel.missing {
  background: rgba(239, 68, 68, 0.05);
  border: 1px solid rgba(239, 68, 68, 0.16);
}

.skill-panel__title {
  margin-bottom: 8px;
  font-size: 13px;
  font-weight: 600;
  color: var(--c-text);
}

.skill-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.no-skills {
  color: var(--c-text-3);
  font-size: 12px;
}

.panel-groups {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin-top: 14px;
}

.analysis-panel {
  padding: 12px 14px;
  border-radius: 12px;
}

.analysis-panel.strength {
  background: #f0fdf6;
}

.analysis-panel.risk {
  background: #fffbeb;
}

.analysis-panel.suggest {
  background: #eef2ff;
}

.analysis-panel__title {
  margin-bottom: 6px;
  font-size: 13px;
  font-weight: 600;
  color: var(--c-text);
}

.line-list {
  margin: 0;
  padding: 0;
  list-style: none;
}

.line-list li {
  display: flex;
  gap: 6px;
  align-items: flex-start;
  margin-top: 4px;
  color: var(--c-text-2);
  font-size: 13px;
  line-height: 1.6;
}

.line-list li .el-icon {
  margin-top: 3px;
  flex: none;
}

.empty-li {
  color: var(--c-text-3);
}

.advice-box {
  display: flex;
  gap: 10px;
  margin-top: 14px;
  padding: 12px 14px;
  border-radius: 12px;
  background: linear-gradient(120deg, rgba(79, 107, 246, 0.08), rgba(34, 211, 238, 0.08));
  border: 1px solid rgba(79, 107, 246, 0.16);
}

.advice-box__content {
  flex: 1;
  color: var(--c-text-2);
  font-size: 13px;
  line-height: 1.7;
}

.quick-actions {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  padding-top: 12px;
  border-top: 1px dashed var(--c-border);
}

.quick-actions__label {
  color: var(--c-text-3);
  font-size: 13px;
}
</style>
