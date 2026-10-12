<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import type { JobQuery, JobVO } from '@/types/api'
import { pageJobs } from '@/api/job'
import { addFavorite, removeFavorite } from '@/api/favorite'
import { useAuthStore } from '@/stores/auth'
import { salaryText, tagOf, jobStatusMap } from '@/utils/format'

defineOptions({ name: 'JobListView' })

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const isUser = computed(() => auth.isLoggedIn && auth.role === 'USER')

const cities = ['上海', '北京', '杭州', '深圳', '广州', '成都']
const jobTypes = [
  '后端开发', '前端开发', '大数据', '人工智能', '信息安全', '测试',
  '运维', '游戏开发', '运营', '数据分析', '产品', '设计', '全栈开发',
]
const educationOptions = ['大专', '本科', '硕士']
const experienceOptions = ['在校生', '应届', '1-3年', '3-5年', '5-10年']
const salaryOptions = [
  { label: '10K 以上', value: 10 },
  { label: '15K 以上', value: 15 },
  { label: '20K 以上', value: 20 },
  { label: '25K 以上', value: 25 },
  { label: '30K 以上', value: 30 },
]

const filters = reactive({
  keyword: '',
  city: '',
  jobType: '',
  education: '',
  experience: '',
  salaryMin: '' as number | string,
  sort: 'default' as 'default' | 'hot' | 'salary',
})

const jobs = ref<JobVO[]>([])
const total = ref(0)
const page = ref(1)
const size = 8
const loading = ref(false)
const failed = ref(false)
let requestId = 0
const favBusyId = ref<number | null>(null)

function syncFromQuery(): void {
  const q = route.query
  filters.keyword = typeof q.keyword === 'string' ? q.keyword : ''
  filters.city = typeof q.city === 'string' ? q.city : ''
  filters.jobType = typeof q.jobType === 'string' ? q.jobType : ''
  filters.education = typeof q.education === 'string' ? q.education : ''
  filters.experience = typeof q.experience === 'string' ? q.experience : ''
  const salary = typeof q.salaryMin === 'string' ? Number(q.salaryMin) : NaN
  filters.salaryMin = Number.isFinite(salary) && salary > 0 ? salary : ''
  const sort = typeof q.sort === 'string' ? q.sort : 'default'
  filters.sort = sort === 'hot' || sort === 'salary' ? sort : 'default'
}

function buildParams(): Partial<JobQuery> {
  return {
    keyword: filters.keyword.trim() || undefined,
    city: filters.city || undefined,
    jobType: filters.jobType || undefined,
    education: filters.education || undefined,
    experience: filters.experience || undefined,
    salaryMin: filters.salaryMin === '' ? undefined : Number(filters.salaryMin),
    sort: filters.sort === 'default' ? undefined : filters.sort,
    page: page.value,
    size,
  }
}

async function loadJobs(): Promise<void> {
  const id = ++requestId
  loading.value = true
  failed.value = false
  try {
    const res = await pageJobs(buildParams())
    if (id !== requestId) return
    jobs.value = res.records
    total.value = res.total
  } catch {
    if (id !== requestId) return
    jobs.value = []
    total.value = 0
    failed.value = true
  } finally {
    if (id === requestId) loading.value = false
  }
}

/** 触发搜索：重置页码并同步到地址栏，由路由 query 变化驱动加载 */
function doSearch(): void {
  page.value = 1
  const query: Record<string, string> = {}
  const keyword = filters.keyword.trim()
  if (keyword) query.keyword = keyword
  if (filters.city) query.city = filters.city
  if (filters.jobType) query.jobType = filters.jobType
  if (filters.education) query.education = filters.education
  if (filters.experience) query.experience = filters.experience
  if (filters.salaryMin !== '') query.salaryMin = String(filters.salaryMin)
  if (filters.sort !== 'default') query.sort = filters.sort
  const nextQuery = { ...route.query }
  const same =
    Object.keys(query).length === Object.keys(nextQuery).length &&
    Object.keys(query).every((key) => query[key] === String(nextQuery[key] ?? ''))
  if (same) {
    loadJobs()
    return
  }
  router.push({ path: '/jobs', query })
}

function clearFilters(): void {
  filters.keyword = ''
  filters.city = ''
  filters.jobType = ''
  filters.education = ''
  filters.experience = ''
  filters.salaryMin = ''
  filters.sort = 'default'
  doSearch()
}

async function toggleFavorite(job: JobVO): Promise<void> {
  if (!isUser.value) {
    ElMessage.info('登录后即可收藏职位')
    router.push({ path: '/login', query: { redirect: route.fullPath } })
    return
  }
  if (favBusyId.value === job.id) return
  favBusyId.value = job.id
  const target = !job.favorite
  try {
    if (target) await addFavorite(job.id)
    else await removeFavorite(job.id)
    job.favorite = target
    ElMessage.success(target ? '收藏成功' : '已取消收藏')
  } catch {
    /* 失败提示由拦截器统一处理 */
  } finally {
    favBusyId.value = null
  }
}

watch(
  () => route.query,
  () => {
    syncFromQuery()
    const qPage = Number(route.query.page)
    page.value = Number.isSafeInteger(qPage) && qPage > 0 ? qPage : 1
    loadJobs()
  },
)

function handlePageChange(next: number): void {
  router.push({ path: '/jobs', query: { ...route.query, page: String(next) } })
}

onMounted(() => {
  syncFromQuery()
  const qPage = Number(route.query.page)
  page.value = Number.isSafeInteger(qPage) && qPage > 0 ? qPage : 1
  loadJobs()
})

function jobSalary(job: JobVO): string {
  return salaryText(job.salaryMin, job.salaryMax)
}
</script>

<template>
  <div class="page job-list">
    <div class="job-list__inner">
      <div class="list-head">
        <h1 class="list-head__title grad-text">职位中心</h1>
        <p class="list-head__desc">发现更多适合自己的机会，找到下一份理想工作</p>
      </div>

      <div class="panel filter-card">
        <div class="filter-row">
          <el-input
            v-model="filters.keyword" aria-label="职位关键词"
            class="filter-keyword"
            placeholder="职位关键词（如：前端、Java）"
            clearable
            @keyup.enter="doSearch"
          >
            <template #prefix><el-icon><Search /></el-icon></template>
          </el-input>
          <el-button type="primary" class="search-btn" @click="doSearch">
            <el-icon><Search /></el-icon>&nbsp;搜索
          </el-button>
        </div>

        <div class="filter-row">
          <span class="filter-item__label">城市</span>
          <el-select v-model="filters.city" aria-label="城市" placeholder="全国" clearable @change="doSearch">
            <el-option label="全国" value="" />
            <el-option v-for="c in cities" :key="c" :label="c" :value="c" />
          </el-select>
          <span class="filter-item__label">职位类型</span>
          <el-select v-model="filters.jobType" aria-label="职位类型" placeholder="全部" clearable @change="doSearch">
            <el-option v-for="t in jobTypes" :key="t" :label="t" :value="t" />
          </el-select>
          <span class="filter-item__label">学历</span>
          <el-select v-model="filters.education" aria-label="学历" placeholder="不限" clearable @change="doSearch">
            <el-option label="不限" value="" />
            <el-option v-for="e in educationOptions" :key="e" :label="e" :value="e" />
          </el-select>
        </div>

        <div class="filter-row">
          <span class="filter-item__label">经验</span>
          <el-select v-model="filters.experience" aria-label="经验" placeholder="不限" clearable @change="doSearch">
            <el-option label="不限" value="" />
            <el-option v-for="e in experienceOptions" :key="e" :label="e" :value="e" />
          </el-select>
          <span class="filter-item__label">薪资</span>
          <el-select v-model="filters.salaryMin" aria-label="最低薪资" placeholder="不限" clearable @change="doSearch">
            <el-option label="不限" value="" />
            <el-option
              v-for="s in salaryOptions"
              :key="s.value"
              :label="s.label"
              :value="s.value"
            />
          </el-select>
          <el-radio-group v-model="filters.sort" aria-label="排序方式" class="sort-group" @change="doSearch">
            <el-radio-button value="default">综合</el-radio-button>
            <el-radio-button value="hot">最热</el-radio-button>
            <el-radio-button value="salary">薪资</el-radio-button>
          </el-radio-group>
        </div>
      </div>

      <div v-loading="loading" class="jobs-area">
        <el-empty v-if="!loading && failed" description="职位加载失败，请重新尝试">
          <el-button type="primary" @click="loadJobs">重新加载</el-button>
        </el-empty>
        <el-empty
          v-else-if="!loading && jobs.length === 0"
          description="没有找到符合条件的职位"
        >
          <el-button type="primary" @click="clearFilters">清空筛选条件</el-button>
        </el-empty>

        <template v-else-if="!loading">
          <div class="job-grid">
            <div v-for="job in jobs" :key="job.id" class="job-card panel">
              <div class="job-card__head">
                <el-avatar :size="52" :src="job.companyLogo || undefined" class="job-card__logo">
                  {{ job.companyName?.[0] || '企' }}
                </el-avatar>
                <div class="job-card__main">
                  <router-link :to="`/jobs/${job.id}`" class="job-card__name" :title="job.jobName">{{ job.jobName }}</router-link>
                  <div class="job-card__company" :title="job.companyName">{{ job.companyName }}</div>
                </div>
                <div class="job-card__salary grad-text">{{ jobSalary(job) }}</div>
                <div class="job-card__views">
                  <el-icon><View /></el-icon>{{ job.viewCount }}
                </div>
                <button
                  v-if="isUser"
                  type="button"
                  class="fav-btn" :aria-label="job.favorite ? `取消收藏${job.jobName}` : `收藏${job.jobName}`" :aria-pressed="Boolean(job.favorite)"
                  :class="{ active: job.favorite }"
                  :disabled="favBusyId === job.id"
                  @click.stop="toggleFavorite(job)"
                >
                  <el-icon :size="20">
                    <StarFilled v-if="job.favorite" />
                    <Star v-else />
                  </el-icon>
                </button>
              </div>

              <div class="job-card__meta">
                <span><el-icon><Location /></el-icon>{{ job.city || '不限' }}</span>
                <span>{{ job.education || '学历不限' }}</span>
                <span>{{ job.experience || '经验不限' }}</span>
                <el-tag
                  v-if="job.status"
                  size="small"
                  :type="tagOf(jobStatusMap, job.status).type"
                  effect="light"
                >
                  {{ tagOf(jobStatusMap, job.status).label }}
                </el-tag>
              </div>

              <div class="job-card__tags">
                <el-tag v-if="job.jobType" size="small" type="primary" effect="plain">{{ job.jobType }}</el-tag>
                <el-tag
                  v-for="skill in job.skills?.slice(0, 5)"
                  :key="skill.id ?? skill.skillName"
                  size="small"
                  type="info"
                  effect="plain"
                >
                  {{ skill.skillName }}
                </el-tag>
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
.job-list__inner {
  max-width: 1200px;
  margin: 0 auto;
}

.list-head {
  margin-bottom: 18px;
}

.list-head__title {
  margin: 0;
  font-size: 26px;
  font-weight: 650;
}

.list-head__desc {
  margin: 8px 0 0;
  color: var(--c-text-2);
  font-size: 14px;
}

/* ===== 筛选卡片 ===== */
.filter-card {
  padding: 18px 22px 8px;
  margin-bottom: 18px;
}

.filter-row {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 14px;
}

.filter-row:first-child {
  padding-bottom: 14px;
  border-bottom: 1px dashed var(--c-border);
}

.filter-keyword {
  flex: 1;
}

.search-btn {
  height: 40px;
}

.filter-item__label {
  flex: none;
  color: var(--c-text-2);
  font-size: 13px;
}

.filter-row .el-select {
  width: 140px;
}

.sort-group {
  margin-left: auto;
}

/* ===== 列表 ===== */
.jobs-area {
  min-height: 420px;
}

.job-grid {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.job-card {
  padding: 20px 24px;
  cursor: pointer;
  transition: border-color 0.2s ease, background-color 0.2s ease, box-shadow 0.2s ease;
}

.job-card:hover {
  
  box-shadow: var(--shadow-md);
}

.job-card__head {
  display: flex;
  align-items: center;
  gap: 16px;
}

.job-card__logo {
  flex: none;
  background: var(--c-primary);
  color: #fff;
  font-weight: 600;
}

.job-card__main {
  flex: 1;
  min-width: 0;
}

.job-card__name {
  overflow: hidden;
  color: var(--c-text);
  font-size: 17px;
  font-weight: 700;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.job-card__company {
  margin-top: 5px;
  overflow: hidden;
  color: var(--c-text-2);
  font-size: 13px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.job-card__salary {
  flex: none;
  font-size: 18px;
  font-weight: 650;
}

.job-card__views {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  flex: none;
  color: var(--c-text-3);
  font-size: 13px;
}

.fav-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex: none;
  width: 36px;
  height: 36px;
  border: none;
  border-radius: 50%;
  cursor: pointer;
  color: var(--c-text-3);
  background: #f6f7fb;
  transition: border-color 0.2s ease, background-color 0.2s ease, box-shadow 0.2s ease;
}

.fav-btn:hover,
.fav-btn.active {
  color: #f59e0b;
  background: rgba(245, 158, 11, 0.12);
}

.job-card__meta {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 20px;
  margin-top: 16px;
  color: var(--c-text-2);
  font-size: 13px;
}

.job-card__meta span {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.job-card__tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 14px;
}

.pagination-wrap {
  display: flex;
  justify-content: center;
  margin-top: 26px;
}
</style>
