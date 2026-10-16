<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { publicCompany } from '@/api/company'
import { pageJobs } from '@/api/job'
import PositionCard from '@/components/PositionCard.vue'
import type { JobVO, PublicCompanyVO } from '@/types/api'

const route = useRoute()
const router = useRouter()
const company = ref<PublicCompanyVO | null>(null)
const loading = ref(true)
const failed = ref(false)
const jobs = ref<JobVO[]>([])
const jobsLoading = ref(false)
const jobsFailed = ref(false)
const total = ref(0)
const size = 8
let requestId = 0
let jobRequestId = 0
const page = computed(() => {
  const value = typeof route.query.page === 'string' ? Number(route.query.page) : 1
  return Number.isSafeInteger(value) && value > 0 ? value : 1
})
const website = computed(() => {
  if (!company.value?.website) return null
  try {
    const url = new URL(company.value.website)
    return ['https:', 'http:'].includes(url.protocol) ? url.href : null
  } catch { return null }
})

async function loadJobs() {
  if (!company.value) return
  const companyId = company.value.id
  const id = ++jobRequestId
  jobsLoading.value = true
  jobsFailed.value = false
  jobs.value = []
  try {
    const result = await pageJobs({ companyId, page: page.value, size })
    if (id !== jobRequestId) return
    jobs.value = result.records
    total.value = result.total
  } catch {
    if (id === jobRequestId) jobsFailed.value = true
  } finally { if (id === jobRequestId) jobsLoading.value = false }
}

async function loadCompany() {
  const id = ++requestId
  ++jobRequestId
  loading.value = true
  failed.value = false
  company.value = null
  jobs.value = []
  total.value = 0
  const companyId = Number(route.params.id)
  if (!Number.isSafeInteger(companyId) || companyId <= 0) {
    failed.value = true
    loading.value = false
    return
  }
  try {
    const result = await publicCompany(companyId)
    if (id !== requestId) return
    company.value = result
    void loadJobs()
  } catch {
    if (id === requestId) failed.value = true
  } finally { if (id === requestId) loading.value = false }
}

watch(() => route.params.id, loadCompany, { immediate: true })
watch(page, () => { if (company.value) void loadJobs() })

function changePage(value: number) {
  void router.push({ path: route.path, query: value > 1 ? { page: String(value) } : {} })
}
</script>

<template>
  <div class="page public-company-detail">
    <div class="company-detail-inner">
      <router-link to="/companies" class="back-link"><el-icon aria-hidden="true"><ArrowLeft /></el-icon>浏览企业</router-link>
      <div v-loading="loading" class="company-content" :aria-busy="loading">
        <el-empty v-if="!loading && failed" description="企业不存在、已停用或暂时无法加载">
          <el-button type="primary" @click="loadCompany">重新加载</el-button>
          <el-button @click="router.push('/companies')">浏览其他企业</el-button>
        </el-empty>
        <template v-else-if="company">
          <header class="panel company-profile">
            <div class="company-identity">
              <el-avatar shape="square" :size="72" :src="company.logo || undefined">{{ company.companyName?.[0] || '企' }}</el-avatar>
              <div><h1>{{ company.companyName }}</h1><p>{{ company.industry || '行业未填写' }} · {{ company.scale || '规模未填写' }}</p></div>
            </div>
            <div class="company-profile__count"><strong>{{ company.jobCount }}</strong><span>个在招职位</span></div>
          </header>
          <section class="panel company-about">
            <h2>企业介绍</h2>
            <p class="company-description">{{ company.description || '企业暂未填写介绍。' }}</p>
            <p v-if="company.address" class="company-address"><el-icon aria-hidden="true"><Location /></el-icon>{{ company.address }}</p>
            <a v-if="website" :href="website" target="_blank" rel="noopener noreferrer" class="website-link">访问企业官网 <el-icon aria-hidden="true"><Top /></el-icon></a>
          </section>
          <section class="company-jobs" aria-labelledby="company-jobs-title">
            <div class="jobs-heading"><h2 id="company-jobs-title">在招职位</h2><router-link :to="{ path: '/jobs', query: { companyId: String(company.id) } }">筛选企业职位 <el-icon aria-hidden="true"><ArrowRight /></el-icon></router-link></div>
            <div v-loading="jobsLoading" class="jobs-content" :aria-busy="jobsLoading">
              <el-empty v-if="!jobsLoading && jobsFailed" description="职位加载失败，请重新尝试"><el-button type="primary" @click="loadJobs">重新加载职位</el-button></el-empty>
              <template v-else-if="!jobsLoading">
                <el-empty v-if="!jobs.length" :description="page > 1 ? '当前页暂无职位，请返回上一页' : '暂无在招职位'">
                  <el-button v-if="page > 1" @click="changePage(1)">返回第一页</el-button>
                </el-empty>
                <div v-else class="company-job-grid"><PositionCard v-for="job in jobs" :key="job.id" :job="job" /></div>
                <div v-if="total > size || page > 1" class="company-pagination"><el-pagination background layout="prev, pager, next" :pager-count="5" :total="total" :page-size="size" :current-page="page" @current-change="changePage" /></div>
              </template>
            </div>
          </section>
        </template>
      </div>
    </div>
  </div>
</template>

<style scoped>
.company-detail-inner { max-width:1200px; margin:auto; }.back-link { display:inline-flex; align-items:center; gap:8px; color:var(--c-text-2); margin-bottom:20px; }.back-link:hover,.website-link { color:var(--c-primary); }.company-content { min-height:300px; }
.company-profile { display:flex; justify-content:space-between; align-items:center; flex-wrap:wrap; gap:24px; padding:32px; }.company-identity { display:flex; align-items:center; gap:20px; min-width:0; }.company-identity .el-avatar { flex-shrink:0; }.company-identity > div { min-width:0; }.company-identity h1 { font-size:28px; margin:0 0 10px; overflow-wrap:anywhere; }.company-identity p { margin:0; color:var(--c-text-2); }
.company-profile__count { display:flex; align-items:baseline; gap:8px; color:var(--c-text-2); }.company-profile__count strong { font-size:32px; color:var(--c-primary); }
.company-about { margin-top:24px; padding:28px 32px; }.company-about h2,.jobs-heading h2 { font-size:20px; margin:0; }.company-description { white-space:pre-wrap; overflow-wrap:anywhere; line-height:1.9; color:var(--c-text-2); margin:18px 0; }.company-address { display:flex; align-items:baseline; gap:8px; color:var(--c-text-2); overflow-wrap:anywhere; }.company-address .el-icon { flex-shrink:0; }.website-link { display:inline-flex; align-items:center; gap:6px; }
.company-jobs { margin-top:32px; }.jobs-heading { display:flex; justify-content:space-between; gap:16px; flex-wrap:wrap; align-items:center; margin-bottom:20px; }.jobs-heading > a { display:flex; align-items:center; gap:6px; color:var(--c-primary); }.jobs-content { min-height:200px; }.company-job-grid { display:grid; grid-template-columns:repeat(2,minmax(0,1fr)); gap:18px; }.company-pagination { display:flex; justify-content:center; margin-top:28px; }
@media(max-width:760px) { .company-profile,.company-about { padding:24px 20px; }.company-identity { gap:14px; }.company-identity h1 { font-size:24px; }.company-job-grid { grid-template-columns:1fr; }.company-profile__count strong { font-size:26px; } }
</style>
