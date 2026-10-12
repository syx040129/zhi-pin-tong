<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { companyJobs, deleteCompanyJob, updateCompanyJobStatus } from '@/api/company'
import type { JobStatus, JobVO } from '@/types/api'
import { formatDateTime, jobStatusMap, salaryText } from '@/utils/format'

defineOptions({ name: 'CompanyJobsView' })

const router = useRouter()

const loading = ref(false)
const list = ref<JobVO[]>([])
const total = ref(0)
const togglingId = ref<number | null>(null)
const filters = reactive<{ keyword: string; status: '' | JobStatus }>({ keyword: '', status: '' })
const page = ref(1)
const size = ref(10)

async function fetchList(): Promise<void> {
  loading.value = true
  try {
    const res = await companyJobs({
      keyword: filters.keyword.trim() || undefined,
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

function handleReset(): void {
  filters.keyword = ''
  filters.status = ''
  handleSearch()
}

async function applyStatus(row: JobVO, target: JobStatus): Promise<void> {
  if (togglingId.value !== null) return
  togglingId.value = row.id
  try {
    await updateCompanyJobStatus(row.id, target)
    row.status = target
    ElMessage.success(target === 'ONLINE' ? '岗位已上架' : '岗位已下架')
  } catch {
    await fetchList()
  } finally {
    togglingId.value = null
  }
}

function onSwitchChange(row: JobVO, value: string | number | boolean): void {
  void applyStatus(row, value === 'ONLINE' ? 'ONLINE' : 'OFFLINE')
}

function toggleRow(row: JobVO): void {
  void applyStatus(row, row.status === 'ONLINE' ? 'OFFLINE' : 'ONLINE')
}

function goEdit(row: JobVO): void {
  void router.push(`/company/jobs/${row.id}/edit`)
}

async function handleDelete(row: JobVO): Promise<void> {
  try {
    await ElMessageBox.confirm(
      `确定删除岗位「${row.jobName}」吗？删除后不可恢复。`,
      '删除岗位',
      { type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消' },
    )
  } catch {
    return
  }
  try {
    await deleteCompanyJob(row.id)
    ElMessage.success('岗位已删除')
    if (list.value.length === 1 && page.value > 1) page.value -= 1
    await fetchList()
  } catch {
    // 拦截器已提示
  }
}

onMounted(() => {
  void fetchList()
})
</script>

<template>
  <div class="jobs-page">
    <div class="panel jobs-panel">
      <div class="jobs-head">
        <div>
          <div class="jobs-head__title">岗位管理</div>
          <div class="jobs-head__sub">发布、编辑与上下架企业招聘岗位</div>
        </div>
        <button type="button" class="grad-btn" @click="router.push('/company/jobs/create')">
          <el-icon style="margin-right: 5px"><Plus /></el-icon>发布岗位
        </button>
      </div>

      <div class="jobs-filters">
        <el-input
          v-model="filters.keyword"
          placeholder="搜索岗位名称 / 类型"
          clearable
          class="keyword-input"
          @keyup.enter="handleSearch"
          @clear="handleSearch"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>
        <el-select
          v-model="filters.status"
          placeholder="全部状态"
          clearable
          class="status-select"
          @change="handleSearch"
        >
          <el-option v-for="(opt, key) in jobStatusMap" :key="key" :label="opt.label" :value="key" />
        </el-select>
        <el-button type="primary" @click="handleSearch">
          <el-icon style="margin-right: 4px"><Search /></el-icon>查询
        </el-button>
        <el-button @click="handleReset">重置</el-button>
      </div>

      <el-table :data="list" v-loading="loading" stripe class="jobs-table">
        <el-table-column label="岗位" min-width="220">
          <template #default="{ row }">
            <div class="job-cell">
              <div class="job-cell__name">{{ row.jobName }}</div>
              <el-tag size="small" type="primary" effect="plain">{{ row.jobType }}</el-tag>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="薪资" width="120">
          <template #default="{ row }">
            <span class="salary-text">{{ salaryText(row.salaryMin, row.salaryMax) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="城市" width="100">
          <template #default="{ row }">
            <span><el-icon style="vertical-align: -2px"><Location /></el-icon> {{ row.city }}</span>
          </template>
        </el-table-column>
        <el-table-column label="学历/经验" width="150">
          <template #default="{ row }">
            <span class="cell-text">{{ row.education }} · {{ row.experience }}</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <el-switch
              v-model="row.status"
              :active-value="'ONLINE'"
              :inactive-value="'OFFLINE'"
              :loading="togglingId === row.id"
              :disabled="togglingId !== null"
              inline-prompt
              active-text="在招"
              inactive-text="下线"
              @change="(val: string | number | boolean) => onSwitchChange(row, val)"
            />
          </template>
        </el-table-column>
        <el-table-column label="浏览量" width="90" align="right">
          <template #default="{ row }">{{ row.viewCount ?? 0 }}</template>
        </el-table-column>
        <el-table-column label="发布时间" width="170">
          <template #default="{ row }">{{ formatDateTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" :disabled="togglingId !== null" @click="goEdit(row)">
              <el-icon style="margin-right: 3px"><EditPen /></el-icon>编辑
            </el-button>
            <el-button link :type="row.status === 'ONLINE' ? 'warning' : 'success'" :disabled="togglingId !== null" @click="toggleRow(row)">
              <el-icon style="margin-right: 3px"><Top v-if="row.status === 'OFFLINE'" /><Bottom v-else /></el-icon>
              {{ row.status === 'ONLINE' ? '下架' : '上架' }}
            </el-button>
            <el-button link type="danger" @click="handleDelete(row)">
              <el-icon style="margin-right: 3px"><Delete /></el-icon>删除
            </el-button>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty description="暂无岗位，点击右上角发布岗位" :image-size="90" />
        </template>
      </el-table>

      <div class="jobs-pager">
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
.jobs-page {
  min-height: 100%;
}

.jobs-panel {
  padding: 20px 22px;
}

.jobs-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  margin-bottom: 18px;
}

.jobs-head__title {
  font-size: 17px;
  font-weight: 600;
  color: var(--c-text);
}

.jobs-head__sub {
  margin-top: 4px;
  color: var(--c-text-3);
  font-size: 13px;
}

.jobs-filters {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
  margin-bottom: 16px;
}

.keyword-input {
  width: 260px;
}

.status-select {
  width: 160px;
}

.job-cell {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 6px;
}

.job-cell__name {
  font-weight: 600;
  color: var(--c-text);
}

.salary-text {
  color: #f97316;
  font-weight: 600;
}

.cell-text {
  color: var(--c-text-2);
}

.jobs-table {
  width: 100%;
}

.jobs-pager {
  display: flex;
  justify-content: flex-end;
  padding-top: 16px;
}
</style>
