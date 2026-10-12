<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { adminDeleteJob, adminJobStatus, adminJobs } from '@/api/admin'
import type { JobStatus, JobVO } from '@/types/api'
import { formatDateTime, jobStatusMap, salaryText } from '@/utils/format'

defineOptions({ name: 'AdminJobsView' })

const loading = ref(false)
const list = ref<JobVO[]>([])
const total = ref(0)
const togglingId = ref<number | null>(null)
const filters = reactive<{ keyword: string; jobType: string; status: '' | JobStatus }>({
  keyword: '',
  jobType: '',
  status: '',
})
const page = ref(1)
const size = ref(10)

const jobTypeOptions = [
  '后端开发',
  '前端开发',
  '大数据',
  '人工智能',
  '信息安全',
  '测试',
  '运维',
  '游戏开发',
  '运营',
  '数据分析',
  '产品',
  '设计',
  '全栈开发',
]

const statusOptions = Object.keys(jobStatusMap) as JobStatus[]

async function fetchList(): Promise<void> {
  loading.value = true
  try {
    const res = await adminJobs({
      keyword: filters.keyword.trim() || undefined,
      jobType: filters.jobType || undefined,
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
  filters.jobType = ''
  filters.status = ''
  handleSearch()
}

function onSwitchChange(row: JobVO, value: string | number | boolean): void {
  if (togglingId.value !== null) return
  const target: JobStatus = value === 'ONLINE' ? 'ONLINE' : 'OFFLINE'
  togglingId.value = row.id
  void adminJobStatus(row.id, target)
    .then(() => {
      row.status = target
      ElMessage.success(target === 'ONLINE' ? '岗位已上架' : '岗位已下架')
    })
    .catch(() => {
      void fetchList()
    })
    .finally(() => {
      togglingId.value = null
    })
}

async function handleDelete(row: JobVO): Promise<void> {
  try {
    await ElMessageBox.confirm(
      `确定删除岗位「${row.jobName}」（${row.companyName}）吗？删除后不可恢复。`,
      '删除岗位',
      { type: 'error', confirmButtonText: '删除', cancelButtonText: '取消' },
    )
  } catch {
    return
  }
  try {
    await adminDeleteJob(row.id)
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
          <div class="jobs-head__sub">平台全部企业岗位，可上下架或删除违规岗位</div>
        </div>
      </div>

      <div class="jobs-filters">
        <el-input
          v-model="filters.keyword"
          placeholder="搜索岗位名称 / 企业名称"
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
          v-model="filters.jobType"
          placeholder="全部岗位类型"
          clearable
          filterable
          class="type-select"
          @change="handleSearch"
        >
          <el-option v-for="item in jobTypeOptions" :key="item" :label="item" :value="item" />
        </el-select>
        <el-select
          v-model="filters.status"
          placeholder="全部状态"
          clearable
          class="status-select"
          @change="handleSearch"
        >
          <el-option v-for="key in statusOptions" :key="key" :label="jobStatusMap[key].label" :value="key" />
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
              <div class="job-cell__company">{{ row.companyName }}</div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="类型" width="110">
          <template #default="{ row }">
            <el-tag type="primary" effect="plain">{{ row.jobType }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="薪资" width="110">
          <template #default="{ row }">
            <span class="salary-text">{{ salaryText(row.salaryMin, row.salaryMax) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="城市" width="90">
          <template #default="{ row }">{{ row.city }}</template>
        </el-table-column>
        <el-table-column label="学历/经验" width="150">
          <template #default="{ row }">
            <span class="cell-text">{{ row.education }} · {{ row.experience }}</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="120" align="center">
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
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button link type="danger" @click="handleDelete(row)">
              <el-icon style="margin-right: 3px"><Delete /></el-icon>删除
            </el-button>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty description="暂无匹配的岗位" :image-size="90" />
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

.type-select {
  width: 170px;
}

.status-select {
  width: 140px;
}

.job-cell__name {
  font-weight: 600;
  color: var(--c-text);
}

.job-cell__company {
  margin-top: 2px;
  color: var(--c-text-3);
  font-size: 12px;
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
