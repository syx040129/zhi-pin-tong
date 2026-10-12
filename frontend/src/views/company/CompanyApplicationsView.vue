<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { companyApplications, companyJobs, updateCompanyApplicationStatus } from '@/api/company'
import type { ApplicationStatus, ApplicationVO, JobVO } from '@/types/api'
import { applicationStatusMap, formatDateTime, tagOf } from '@/utils/format'

defineOptions({ name: 'CompanyApplicationsView' })

const router = useRouter()

const loading = ref(false)
const list = ref<ApplicationVO[]>([])
const total = ref(0)
const busy = ref('')
const jobOptions = ref<JobVO[]>([])
const filters = reactive<{
  jobId: '' | number
  status: '' | ApplicationStatus
}>({ jobId: '', status: '' })
const page = ref(1)
const size = ref(10)

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
    jobOptions.value = res.records
  } catch {
    // 拦截器已提示
  }
}

async function fetchList(): Promise<void> {
  loading.value = true
  try {
    const res = await companyApplications({
      jobId: filters.jobId || undefined,
      status: filters.status || undefined,
      page: page.value,
      size: size.value,
    })
    list.value = res.records
    total.value = res.total
  } catch {
    // 错误提示由请求拦截器统一处理
  } finally {
    loading.value = false
  }
}

function handleSearch(): void {
  page.value = 1
  void fetchList()
}

function scoreType(score?: number | null): 'success' | 'warning' | 'danger' | 'info' {
  if (score == null) return 'info'
  if (score >= 80) return 'success'
  if (score >= 60) return 'warning'
  return 'danger'
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
    ElMessage.success(successText[status] ?? '操作成功')
    await fetchList()
  } catch {
    // 拦截器已提示
  } finally {
    busy.value = ''
  }
}

function openCandidates(): void {
  const query: Record<string, string> = {}
  if (filters.jobId) query.jobId = String(filters.jobId)
  if (filters.status) query.status = filters.status
  void router.push({ path: '/company/candidates', query })
}

onMounted(() => {
  void fetchJobs()
  void fetchList()
})
</script>

<template>
  <div class="apps-page">
    <div class="panel apps-panel">
      <div class="apps-head">
        <div>
          <div class="apps-head__title">收到投递</div>
          <div class="apps-head__sub">处理候选人投递，推进招聘流程</div>
        </div>
        <button type="button" class="grad-btn" @click="openCandidates">
          <el-icon style="margin-right: 5px"><MagicStick /></el-icon>打开 AI 候选人分析
        </button>
      </div>

      <div class="apps-filters">
        <el-select
          v-model="filters.jobId"
          placeholder="全部岗位"
          clearable
          filterable
          class="job-select"
          @change="handleSearch"
        >
          <el-option v-for="job in jobOptions" :key="job.id" :label="job.jobName" :value="job.id" />
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
        <el-button type="primary" @click="handleSearch">查询</el-button>
      </div>

      <el-table :data="list" v-loading="loading" stripe class="apps-table">
        <el-table-column type="expand">
          <template #default="{ row }">
            <div class="cover-block">
              <div class="cover-block__label">求职信</div>
              <div class="cover-block__text">{{ row.coverLetter || '候选人未填写求职信' }}</div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="候选人" min-width="170">
          <template #default="{ row }">
            <div class="candidate-cell">
              <el-avatar :size="34" :src="row.candidateAvatar || undefined" class="candidate-avatar">
                {{ (row.candidateName || '候').charAt(0) }}
              </el-avatar>
              <span class="candidate-name">{{ row.candidateName }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="jobName" label="投递岗位" min-width="150" show-overflow-tooltip />
        <el-table-column label="简历" width="140" show-overflow-tooltip>
          <template #default="{ row }">{{ row.resumeName || '未提交简历' }}</template>
        </el-table-column>
        <el-table-column label="匹配度" width="100" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.matchScore != null" :type="scoreType(row.matchScore)" round effect="dark" size="small">
              {{ row.matchScore }}%
            </el-tag>
            <span v-else class="dim-text">-</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="tagOf(applicationStatusMap, row.status).type" round>
              {{ tagOf(applicationStatusMap, row.status).label }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="投递时间" width="170">
          <template #default="{ row }">{{ formatDateTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="250" fixed="right">
          <template #default="{ row }">
            <template v-if="row.status === 'PENDING'">
              <el-button link type="primary" :disabled="busy !== ''" @click="runStatus(row, 'VIEWED')">
                标记已查看
              </el-button>
              <el-button link type="success" :disabled="busy !== ''" @click="runStatus(row, 'INTERVIEW')">
                邀请面试
              </el-button>
              <el-button link type="danger" :disabled="busy !== ''" @click="runStatus(row, 'REJECTED')">
                不合适
              </el-button>
            </template>
            <template v-else-if="row.status === 'VIEWED'">
              <el-button link type="success" :disabled="busy !== ''" @click="runStatus(row, 'INTERVIEW')">
                邀请面试
              </el-button>
              <el-button link type="danger" :disabled="busy !== ''" @click="runStatus(row, 'REJECTED')">
                不合适
              </el-button>
            </template>
            <template v-else-if="row.status === 'INTERVIEW'">
              <el-button link type="success" :disabled="busy !== ''" @click="runStatus(row, 'HIRED')">
                录用
              </el-button>
              <el-button link type="danger" :disabled="busy !== ''" @click="runStatus(row, 'REJECTED')">
                不合适
              </el-button>
            </template>
            <el-tag v-else size="small" type="info" effect="plain">流程已结束</el-tag>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty description="暂无收到投递" :image-size="90" />
        </template>
      </el-table>

      <div class="apps-pager">
        <el-pagination
          v-model:current-page="page"
          v-model:page-size="size"
          :total="total"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          background
          @size-change="handleSearch"
          @current-change="fetchList"
        />
      </div>
    </div>
  </div>
</template>

<style scoped>
.apps-page {
  min-height: 100%;
}

.apps-panel {
  padding: 20px 22px;
}

.apps-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  margin-bottom: 18px;
}

.apps-head__title {
  font-size: 17px;
  font-weight: 600;
  color: var(--c-text);
}

.apps-head__sub {
  margin-top: 4px;
  color: var(--c-text-3);
  font-size: 13px;
}

.apps-filters {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
  margin-bottom: 16px;
}

.job-select {
  width: 240px;
}

.status-select {
  width: 160px;
}

.apps-table {
  width: 100%;
}

.candidate-cell {
  display: flex;
  align-items: center;
  gap: 10px;
}

.candidate-avatar {
  flex: none;
  background: var(--c-primary);
  color: #fff;
  font-weight: 600;
}

.candidate-name {
  font-weight: 500;
  color: var(--c-text);
}

.dim-text {
  color: var(--c-text-3);
}

.cover-block {
  padding: 10px 24px 14px 60px;
}

.cover-block__label {
  margin-bottom: 6px;
  color: var(--c-text-3);
  font-size: 12px;
}

.cover-block__text {
  color: var(--c-text-2);
  font-size: 13px;
  line-height: 1.7;
  white-space: pre-wrap;
}

.apps-pager {
  display: flex;
  justify-content: flex-end;
  padding-top: 16px;
}
</style>
