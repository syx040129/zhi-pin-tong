<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { adminApplications, adminDeleteApplication } from '@/api/admin'
import type { ApplicationStatus, ApplicationVO } from '@/types/api'
import { applicationStatusMap, formatDateTime, tagOf } from '@/utils/format'

defineOptions({ name: 'AdminApplicationsView' })

const loading = ref(false)
const list = ref<ApplicationVO[]>([])
const total = ref(0)
const busy = ref('')
const filters = reactive<{ keyword: string; status: '' | ApplicationStatus }>({ keyword: '', status: '' })
const page = ref(1)
const size = ref(10)

const statusOptions = Object.keys(applicationStatusMap) as ApplicationStatus[]

function scoreType(score?: number | null): 'success' | 'warning' | 'danger' | 'info' {
  if (score == null) return 'info'
  if (score >= 80) return 'success'
  if (score >= 60) return 'warning'
  return 'danger'
}

async function fetchList(): Promise<void> {
  loading.value = true
  try {
    const res = await adminApplications({
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

async function handleDelete(row: ApplicationVO): Promise<void> {
  try {
    await ElMessageBox.confirm(
      `确定删除「${row.candidateName}」对岗位「${row.jobName}」的投递记录吗？`,
      '删除投递记录',
      { type: 'error', confirmButtonText: '删除', cancelButtonText: '取消' },
    )
  } catch {
    return
  }
  busy.value = `delete-${row.id}`
  try {
    await adminDeleteApplication(row.id)
    ElMessage.success('投递记录已删除')
    if (list.value.length === 1 && page.value > 1) page.value -= 1
    await fetchList()
  } catch {
    // 拦截器已提示
  } finally {
    busy.value = ''
  }
}

onMounted(() => {
  void fetchList()
})
</script>

<template>
  <div class="apps-page">
    <div class="panel apps-panel">
      <div class="apps-head">
        <div>
          <div class="apps-head__title">投递管理</div>
          <div class="apps-head__sub">平台全部投递记录，用于数据核查与违规处理</div>
        </div>
      </div>

      <div class="apps-filters">
        <el-input
          v-model="filters.keyword"
          placeholder="搜索候选人 / 岗位 / 企业"
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
          <el-option
            v-for="key in statusOptions"
            :key="key"
            :label="applicationStatusMap[key].label"
            :value="key"
          />
        </el-select>
        <el-button type="primary" @click="handleSearch">
          <el-icon style="margin-right: 4px"><Search /></el-icon>查询
        </el-button>
        <el-button @click="handleReset">重置</el-button>
      </div>

      <el-table :data="list" v-loading="loading" stripe class="apps-table">
        <el-table-column label="候选人" width="130">
          <template #default="{ row }">
            <div class="candidate-cell">
              <el-avatar :size="32" :src="row.candidateAvatar || undefined" class="candidate-avatar">
                {{ (row.candidateName || '候').charAt(0) }}
              </el-avatar>
              <span class="candidate-name">{{ row.candidateName }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="jobName" label="投递岗位" min-width="150" show-overflow-tooltip />
        <el-table-column prop="companyName" label="招聘企业" min-width="160" show-overflow-tooltip />
        <el-table-column label="简历" width="140" show-overflow-tooltip>
          <template #default="{ row }">{{ row.resumeName || '未提交简历' }}</template>
        </el-table-column>
        <el-table-column label="匹配度" width="96" align="center">
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
        <el-table-column label="求职信" min-width="180" show-overflow-tooltip>
          <template #default="{ row }">{{ row.coverLetter || '未填写' }}</template>
        </el-table-column>
        <el-table-column label="投递时间" width="170">
          <template #default="{ row }">{{ formatDateTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button link type="danger" :disabled="busy !== ''" @click="handleDelete(row)">
              <el-icon style="margin-right: 3px"><Delete /></el-icon>删除
            </el-button>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty description="暂无匹配的投递记录" :image-size="90" />
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

.keyword-input {
  width: 280px;
}

.status-select {
  width: 150px;
}

.candidate-cell {
  display: flex;
  align-items: center;
  gap: 8px;
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

.apps-table {
  width: 100%;
}

.apps-pager {
  display: flex;
  justify-content: flex-end;
  padding-top: 16px;
}
</style>
