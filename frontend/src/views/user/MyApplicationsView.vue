<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { ApplicationStatus, ApplicationVO } from '@/types/api'
import { myApplications, withdrawApplication } from '@/api/application'
import { applicationStatusMap, formatDateTime, salaryText, tagOf } from '@/utils/format'

defineOptions({ name: 'MyApplicationsView' })

const router = useRouter()

const statusOptions = [
  { value: '' as const, label: '全部' },
  { value: 'PENDING' as const, label: '待处理' },
  { value: 'VIEWED' as const, label: '已查看' },
  { value: 'INTERVIEW' as const, label: '面试中' },
  { value: 'REJECTED' as const, label: '已拒绝' },
  { value: 'HIRED' as const, label: '已录用' },
]

const status = ref<string>('')
const list = ref<ApplicationVO[]>([])
const total = ref(0)
const page = ref(1)
const size = 6
const loading = ref(false)
const busyId = ref<number | null>(null)

async function load(): Promise<void> {
  loading.value = true
  try {
    const res = await myApplications({
      status: status.value ? (status.value as ApplicationStatus) : undefined,
      page: page.value,
      size,
    })
    list.value = res.records
    total.value = res.total
  } catch {
    list.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

function changeStatus(value: string): void {
  status.value = value
  page.value = 1
  load()
}

function handlePageChange(next: number): void {
  page.value = next
  load()
}

async function withdraw(item: ApplicationVO): Promise<void> {
  try {
    await ElMessageBox.confirm(
      '撤销后该投递将被删除，确认撤销吗？',
      '撤销投递',
      { type: 'warning', confirmButtonText: '确认撤销', cancelButtonText: '取消' },
    )
  } catch {
    return
  }
  if (busyId.value) return
  busyId.value = item.id
  try {
    await withdrawApplication(item.id)
    ElMessage.success('已撤销该投递')
    if (list.value.length === 1 && page.value > 1) page.value -= 1
    await load()
  } catch {
    /* 提示由拦截器处理 */
  } finally {
    busyId.value = null
  }
}

onMounted(load)
</script>

<template>
  <div class="page applications-page">
    <div class="applications-page__inner">
      <div class="page-head">
        <div>
          <h1 class="page-head__title grad-text">我的投递</h1>
          <p class="page-head__desc">跟踪每一份投递的处理进度</p>
        </div>
        <el-button @click="router.push('/jobs')">
          <el-icon><Search /></el-icon>继续找工作
        </el-button>
      </div>

      <!-- 状态筛选 -->
      <div class="panel filter-bar">
        <el-radio-group v-model="status" @change="changeStatus">
          <el-radio-button
            v-for="opt in statusOptions"
            :key="opt.label"
            :value="opt.value"
          >
            {{ opt.label }}
          </el-radio-button>
        </el-radio-group>
      </div>

      <div v-loading="loading" class="list-area">
        <el-empty
          v-if="!loading && list.length === 0"
          description="还没有投递记录，去看看心仪的岗位吧"
        >
          <el-button type="primary" @click="router.push('/jobs')">去逛职位</el-button>
        </el-empty>

        <template v-else>
          <div class="app-list">
            <div v-for="item in list" :key="item.id" class="panel app-card">
              <div class="app-card__main">
                <div class="app-card__title">
                  <el-avatar :size="42" :src="item.companyLogo || undefined" class="app-card__logo">
                    {{ item.companyName?.[0] || '企' }}
                  </el-avatar>
                  <div class="app-card__name">
                    <router-link class="app-card__job" :to="`/jobs/${item.jobId}`">
                      {{ item.jobName }}
                    </router-link>
                    <div class="app-card__company">{{ item.companyName }}</div>
                  </div>
                </div>

                <div class="app-card__tags">
                  <span v-if="item.salaryMin || item.salaryMax" class="salary-tag grad-text">
                    {{ salaryText(item.salaryMin, item.salaryMax) }}
                  </span>
                  <span>{{ item.city }}</span>
                  <span v-if="item.jobType">{{ item.jobType }}</span>
                  <span>简历：{{ item.resumeName || '—' }}</span>
                </div>

                <p v-if="item.coverLetter" class="app-card__letter">
                  <el-icon><ChatLineSquare /></el-icon>
                  <span :title="item.coverLetter">{{ item.coverLetter }}</span>
                </p>
              </div>

              <div class="app-card__side">
                <div class="app-card__status">
                  <el-tag
                    size="large"
                    :type="tagOf(applicationStatusMap, item.status).type"
                    effect="light"
                  >
                    {{ tagOf(applicationStatusMap, item.status).label }}
                  </el-tag>
                  <el-tooltip
                    v-if="item.matchScore != null"
                    :content="`AI 综合匹配度 ${item.matchScore}%`"
                    placement="top"
                  >
                    <span class="match-badge" :class="{ hot: item.matchScore >= 80 }">
                      AI 匹配 {{ item.matchScore }}%
                    </span>
                  </el-tooltip>
                </div>
                <div class="app-card__time">{{ formatDateTime(item.createTime) }}</div>
                <div class="app-card__ops">
                  <el-button text type="primary" @click="router.push(`/jobs/${item.jobId}`)">
                    查看职位
                  </el-button>
                  <el-button
                    v-if="item.status === 'PENDING'"
                    text
                    type="danger"
                    :loading="busyId === item.id"
                    @click="withdraw(item)"
                  >
                    撤销投递
                  </el-button>
                </div>
              </div>
            </div>
          </div>

          <div v-if="total > size" class="pagination-wrap">
            <el-pagination
              background
              layout="total, prev, pager, next"
              :total="total"
              :page-size="size"
              :current-page="page"
              @current-change="handlePageChange"
            />
          </div>
        </template>
      </div>
    </div>
  </div>
</template>

<style scoped>
.applications-page__inner {
  max-width: 1000px;
  margin: 0 auto;
}

.page-head {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 18px;
}

.page-head__title {
  margin: 0;
  font-size: 26px;
  font-weight: 650;
}

.page-head__desc {
  margin: 8px 0 0;
  color: var(--c-text-2);
  font-size: 14px;
}

.filter-bar {
  padding: 12px 16px;
  margin-bottom: 18px;
}

.list-area {
  min-height: 320px;
}

.app-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.app-card {
  display: flex;
  align-items: stretch;
  justify-content: space-between;
  gap: 20px;
  padding: 20px 24px;
}

.app-card__main {
  flex: 1;
  min-width: 0;
}

.app-card__title {
  display: flex;
  align-items: center;
  gap: 12px;
}

.app-card__logo {
  flex: none;
  background: var(--c-primary);
  color: #fff;
  font-weight: 600;
}

.app-card__name {
  min-width: 0;
}

.app-card__job {
  display: block;
  overflow: hidden;
  color: var(--c-text);
  font-size: 16px;
  font-weight: 700;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.app-card__job:hover {
  color: var(--c-primary);
}

.app-card__company {
  margin-top: 3px;
  color: var(--c-text-2);
  font-size: 13px;
}

.app-card__tags {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 16px;
  margin-top: 14px;
  color: var(--c-text-2);
  font-size: 13px;
}

.salary-tag {
  font-weight: 650;
  font-size: 14px;
}

.app-card__letter {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  margin: 12px 0 0;
  padding: 10px 12px;
  border-radius: 8px;
  color: var(--c-text-2);
  font-size: 12px;
  line-height: 1.8;
  background: #f8fafc;
  border: 1px solid #eef2f7;
}

.app-card__letter .el-icon {
  flex: none;
  margin-top: 2px;
  color: var(--c-primary);
}

.app-card__letter span {
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.app-card__side {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  justify-content: space-between;
  flex: none;
  width: 190px;
  border-left: 1px dashed var(--c-border);
  padding-left: 20px;
}

.app-card__status {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 8px;
}

.match-badge {
  padding: 2px 10px;
  border-radius: 999px;
  color: #6b7280;
  font-size: 12px;
  font-weight: 600;
  background: #f3f4f6;
}

.match-badge.hot {
  color: #fff;
  background: var(--c-primary);
}

.app-card__time {
  color: var(--c-text-3);
  font-size: 12px;
}

.app-card__ops {
  display: flex;
  gap: 2px;
}

.pagination-wrap {
  display: flex;
  justify-content: center;
  margin-top: 24px;
}

@media (max-width: 760px) {
  .app-card {
    flex-direction: column;
  }

  .app-card__side {
    width: 100%;
    border-left: none;
    border-top: 1px dashed var(--c-border);
    padding-left: 0;
    padding-top: 12px;
    align-items: flex-start;
  }

  .app-card__status {
    align-items: flex-start;
    flex-direction: row;
  }
}
</style>
