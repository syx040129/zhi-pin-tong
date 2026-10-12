<script setup lang="ts">
import { downloadResumeFile } from '@/utils/download'
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { adminDeleteResume, adminResumes } from '@/api/admin'
import type { ResumeAdminVO } from '@/types/api'
import { formatDateTime } from '@/utils/format'

defineOptions({ name: 'AdminResumesView' })

const loading = ref(false)
const list = ref<ResumeAdminVO[]>([])
const total = ref(0)
const busy = ref('')
const filters = reactive({ keyword: '' })
const page = ref(1)
const size = ref(10)

const detailVisible = ref(false)
const detailRow = ref<ResumeAdminVO | null>(null)

function scoreColor(score?: number | null): string {
  if (score == null) return '#9ca3af'
  if (score >= 80) return '#10b981'
  if (score >= 60) return '#f59e0b'
  return '#ef4444'
}

async function fetchList(): Promise<void> {
  loading.value = true
  try {
    const res = await adminResumes({
      keyword: filters.keyword.trim() || undefined,
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

function openDetail(row: ResumeAdminVO): void {
  detailRow.value = row
  detailVisible.value = true
}

async function handleDelete(row: ResumeAdminVO): Promise<void> {
  try {
    await ElMessageBox.confirm(
      `确定删除用户「${row.userNickname || row.userName}」的简历吗？删除后不可恢复。`,
      '删除简历',
      { type: 'error', confirmButtonText: '删除', cancelButtonText: '取消' },
    )
  } catch {
    return
  }
  busy.value = `delete-${row.id}`
  try {
    await adminDeleteResume(row.id)
    ElMessage.success('简历已删除')
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
  <div class="resumes-page">
    <div class="panel resumes-panel">
      <div class="resumes-head">
        <div>
          <div class="resumes-head__title">简历管理</div>
          <div class="resumes-head__sub">查看平台求职者简历，支持一键预览与删除</div>
        </div>
      </div>

      <div class="resumes-filters">
        <el-input
          v-model="filters.keyword"
          placeholder="搜索姓名 / 学校 / 专业 / 技能"
          clearable
          class="keyword-input"
          @keyup.enter="handleSearch"
          @clear="handleSearch"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>
        <el-button type="primary" @click="handleSearch">
          <el-icon style="margin-right: 4px"><Search /></el-icon>查询
        </el-button>
      </div>

      <el-table :data="list" v-loading="loading" stripe class="resumes-table">
        <el-table-column label="姓名" width="130">
          <template #default="{ row }">
            <div class="name-cell">
              <div class="name-text">{{ row.userName || '-' }}</div>
              <div class="nickname-text">@{{ row.userNickname || '-' }}</div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="手机号" width="130">
          <template #default="{ row }">{{ row.phone || '-' }}</template>
        </el-table-column>
        <el-table-column label="学历 / 学校 / 专业" min-width="220">
          <template #default="{ row }">
            <el-tag v-if="row.education" size="small" effect="plain" round>{{ row.education }}</el-tag>
            <span class="school-text">
              {{ row.school || '未填写学校' }} · {{ row.major || '未填写专业' }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="技能标签" min-width="180" show-overflow-tooltip>
          <template #default="{ row }">{{ row.skills || '未填写' }}</template>
        </el-table-column>
        <el-table-column label="完善度" width="170">
          <template #default="{ row }">
            <div class="score-cell">
              <el-progress
                :percentage="row.completionScore ?? 0"
                :stroke-width="8"
                :color="scoreColor(row.completionScore)"
                class="score-progress"
              />
              <span class="score-num">{{ row.completionScore ?? 0 }}%</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="更新时间" width="170">
          <template #default="{ row }">{{ formatDateTime(row.updateTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="140" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" :disabled="busy !== ''" @click="openDetail(row)">
              <el-icon style="margin-right: 3px"><View /></el-icon>查看
            </el-button>
            <el-button link type="danger" :disabled="busy !== ''" @click="handleDelete(row)">
              <el-icon style="margin-right: 3px"><Delete /></el-icon>删除
            </el-button>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty description="暂无匹配的简历" :image-size="90" />
        </template>
      </el-table>

      <div class="resumes-pager">
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

    <el-dialog v-model="detailVisible" title="简历详情" width="600px" destroy-on-close>
      <template v-if="detailRow">
        <div class="dialog-resume-head">
          <div class="dialog-name">{{ detailRow.userName || '未填写姓名' }}</div>
          <el-tag size="small" effect="plain" round>
            完善度 {{ detailRow.completionScore ?? 0 }}%
          </el-tag>
        </div>
        <el-descriptions :column="2" border size="small">
          <el-descriptions-item label="用户昵称">{{ detailRow.userNickname || '-' }}</el-descriptions-item>
          <el-descriptions-item label="联系电话">{{ detailRow.phone || '-' }}</el-descriptions-item>
          <el-descriptions-item label="邮箱">{{ detailRow.email || '-' }}</el-descriptions-item>
          <el-descriptions-item label="学历">{{ detailRow.education || '-' }}</el-descriptions-item>
          <el-descriptions-item label="毕业院校">{{ detailRow.school || '-' }}</el-descriptions-item>
          <el-descriptions-item label="专业">{{ detailRow.major || '-' }}</el-descriptions-item>
          <el-descriptions-item label="技能标签" :span="2">{{ detailRow.skills || '-' }}</el-descriptions-item>
          <el-descriptions-item label="创建时间">{{ formatDateTime(detailRow.createTime) }}</el-descriptions-item>
          <el-descriptions-item label="更新时间">{{ formatDateTime(detailRow.updateTime) }}</el-descriptions-item>
        </el-descriptions>
        <el-alert
          v-if="detailRow.resumeFileUrl"
          type="success"
          :closable="false"
          show-icon
          class="file-alert"
        >
          <template #title>
            已上传简历附件
            <el-button text type="primary" @click="downloadResumeFile(detailRow.resumeFileUrl)">下载附件</el-button>
          </template>
        </el-alert>
      </template>
      <template #footer>
        <el-button type="primary" @click="detailVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.resumes-page {
  min-height: 100%;
}

.resumes-panel {
  padding: 20px 22px;
}

.resumes-head {
  margin-bottom: 18px;
}

.resumes-head__title {
  font-size: 17px;
  font-weight: 600;
  color: var(--c-text);
}

.resumes-head__sub {
  margin-top: 4px;
  color: var(--c-text-3);
  font-size: 13px;
}

.resumes-filters {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
  margin-bottom: 16px;
}

.keyword-input {
  width: 300px;
}

.name-cell {
  display: flex;
  flex-direction: column;
}

.name-text {
  font-weight: 600;
  color: var(--c-text);
}

.nickname-text {
  margin-top: 2px;
  color: var(--c-text-3);
  font-size: 12px;
}

.school-text {
  margin-left: 6px;
  color: var(--c-text-2);
  font-size: 13px;
}

.score-cell {
  display: flex;
  align-items: center;
  gap: 8px;
}

.score-progress {
  flex: 1;
  min-width: 70px;
}

.score-num {
  flex: none;
  color: var(--c-text-2);
  font-size: 12px;
}

.resumes-table {
  width: 100%;
}

.resumes-pager {
  display: flex;
  justify-content: flex-end;
  padding-top: 16px;
}

.dialog-resume-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 14px;
}

.dialog-name {
  font-size: 16px;
  font-weight: 700;
  color: var(--c-text);
}

.file-alert {
  margin-top: 14px;
}

.file-link {
  margin-left: 8px;
  color: var(--c-primary);
  font-weight: 600;
}
</style>
