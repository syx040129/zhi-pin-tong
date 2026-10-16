<script setup lang="ts">
import { onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { companyFilters, pageCompanies } from '@/api/company'
import type { CompanyFiltersVO, PublicCompanyVO } from '@/types/api'

const route = useRoute()
const router = useRouter()
const filters = reactive({ keyword: '', industry: '', scale: '', includeInactiveHiring: false })
const options = ref<CompanyFiltersVO>({ industries: [], scales: [] })
const optionsFailed = ref(false)
const companies = ref<PublicCompanyVO[]>([])
const total = ref(0)
const page = ref(1)
const size = 12
const loading = ref(true)
const failed = ref(false)
let requestId = 0

async function loadOptions() {
  optionsFailed.value = false
  try { options.value = await companyFilters() } catch { optionsFailed.value = true }
}

async function loadCompanies() {
  const id = ++requestId
  loading.value = true
  failed.value = false
  try {
    const result = await pageCompanies({
      keyword: filters.keyword.trim() || undefined,
      industry: filters.industry || undefined,
      scale: filters.scale || undefined,
      includeInactiveHiring: filters.includeInactiveHiring,
      page: page.value,
      size,
    })
    if (id !== requestId) return
    companies.value = result.records
    total.value = result.total
  } catch {
    if (id !== requestId) return
    companies.value = []
    total.value = 0
    failed.value = true
  } finally { if (id === requestId) loading.value = false }
}

function search(nextPage = 1) {
  const query: Record<string, string> = {}
  if (filters.keyword.trim()) query.keyword = filters.keyword.trim()
  if (filters.industry) query.industry = filters.industry
  if (filters.scale) query.scale = filters.scale
  if (filters.includeInactiveHiring) query.includeInactiveHiring = 'true'
  if (nextPage > 1) query.page = String(nextPage)
  const target = { path: '/companies', query }
  if (router.resolve(target).fullPath === route.fullPath) void loadCompanies()
  else void router.push(target)
}

function clearFilters() {
  Object.assign(filters, { keyword: '', industry: '', scale: '', includeInactiveHiring: false })
  search()
}

watch(() => route.query, q => {
  filters.keyword = typeof q.keyword === 'string' ? q.keyword : ''
  filters.industry = typeof q.industry === 'string' ? q.industry : ''
  filters.scale = typeof q.scale === 'string' ? q.scale : ''
  filters.includeInactiveHiring = q.includeInactiveHiring === 'true'
  const nextPage = typeof q.page === 'string' ? Number(q.page) : 1
  page.value = Number.isSafeInteger(nextPage) && nextPage > 0 ? nextPage : 1
  void loadCompanies()
}, { immediate: true })

onMounted(loadOptions)
</script>

<template>
  <div class="page company-directory">
    <div class="directory-inner">
      <header class="list-head">
        <h1 class="list-head__title">找公司</h1>
        <p class="list-head__desc">了解企业，发现值得加入的团队与招聘机会。</p>
      </header>

      <form class="panel directory-filters" role="search" @submit.prevent="search()">
        <div class="keyword-row">
          <el-input v-model="filters.keyword" aria-label="公司名称" placeholder="输入公司名称" maxlength="100" clearable @clear="search()">
            <template #prefix><el-icon aria-hidden="true"><Search /></el-icon></template>
          </el-input>
          <button type="submit" class="primary-button">搜索公司</button>
        </div>
        <div class="filter-row">
          <label for="company-industry">行业</label>
          <el-select id="company-industry" v-model="filters.industry" aria-label="企业行业" placeholder="全部行业" clearable filterable @change="search()">
            <el-option v-for="item in options.industries" :key="item" :label="item" :value="item" />
          </el-select>
          <label for="company-scale">规模</label>
          <el-select id="company-scale" v-model="filters.scale" aria-label="企业规模" placeholder="全部规模" clearable @change="search()">
            <el-option v-for="item in options.scales" :key="item" :label="item" :value="item" />
          </el-select>
          <el-checkbox v-model="filters.includeInactiveHiring" @change="search()">包含暂无在招职位的企业</el-checkbox>
        </div>
        <div v-if="optionsFailed" class="options-error">筛选选项加载失败 <el-button text type="primary" @click="loadOptions">重试</el-button></div>
      </form>

      <div v-loading="loading" class="directory-results" :aria-busy="loading">
        <el-empty v-if="!loading && failed" description="企业加载失败，请重新尝试">
          <el-button type="primary" @click="loadCompanies">重新加载</el-button>
        </el-empty>
        <template v-else-if="!loading">
          <p class="result-count" role="status">找到 {{ total }} 家企业</p>
          <el-empty v-if="companies.length === 0" description="没有找到符合条件的企业">
            <el-button type="primary" @click="clearFilters">清空筛选条件</el-button>
          </el-empty>
          <div v-else class="company-grid">
            <router-link v-for="company in companies" :key="company.id" :to="`/companies/${company.id}`" class="panel company-result">
              <div class="company-result__heading">
                <el-avatar shape="square" :size="52" :src="company.logo || undefined">{{ company.companyName?.[0] || '企' }}</el-avatar>
                <div><h2>{{ company.companyName }}</h2><p>{{ company.industry || '行业未填写' }} · {{ company.scale || '规模未填写' }}</p></div>
              </div>
              <p class="company-summary">{{ company.description || '企业暂未填写介绍，点击查看招聘机会。' }}</p>
              <div class="company-result__footer"><span :class="{ hiring: company.jobCount > 0 }">{{ company.jobCount > 0 ? `${company.jobCount} 个在招职位` : '暂无在招职位' }}</span><span>了解企业 <el-icon aria-hidden="true"><ArrowRight /></el-icon></span></div>
            </router-link>
          </div>
          <div v-if="total > size || page > 1" class="directory-pagination">
            <el-pagination background layout="prev, pager, next" :pager-count="5" :total="total" :page-size="size" :current-page="page" @current-change="search" />
          </div>
        </template>
      </div>
    </div>
  </div>
</template>

<style scoped>
.directory-inner { max-width:1200px; margin:auto; }
.list-head h1 { margin:0 0 8px; }.list-head p { margin:0; }
.directory-filters { padding:24px; margin-bottom:24px; }
.keyword-row { display:flex; gap:12px; }.keyword-row .el-input { flex:1; min-width:0; }.keyword-row button { flex-shrink:0; }
.filter-row { display:flex; align-items:center; flex-wrap:wrap; gap:12px; margin-top:18px; }.filter-row .el-select { width:190px; }
.filter-row label,.result-count,.options-error { color:var(--c-text-2); }.options-error { margin-top:8px; }
.directory-results { min-height:260px; }.result-count { margin:0 0 16px; }
.company-grid { display:grid; grid-template-columns:repeat(3,minmax(0,1fr)); gap:18px; }
.company-result { display:block; padding:24px; transition:border-color .18s; min-width:0; }.company-result:hover { border-color:var(--c-primary); }
.company-result__heading { display:flex; gap:14px; align-items:center; }.company-result__heading .el-avatar { flex-shrink:0; }.company-result__heading > div { min-width:0; }
.company-result h2 { margin:0; font-size:18px; overflow-wrap:anywhere; }.company-result__heading p { color:var(--c-text-2); font-size:12px; margin:6px 0 0; overflow-wrap:anywhere; }
.company-summary { display:-webkit-box; -webkit-line-clamp:2; -webkit-box-orient:vertical; overflow:hidden; color:var(--c-text-2); line-height:1.8; height:50px; margin:20px 0; overflow-wrap:anywhere; }
.company-result__footer { display:flex; justify-content:space-between; flex-wrap:wrap; gap:10px; padding-top:16px; border-top:1px solid var(--c-border); font-size:13px; color:var(--c-text-2); }.company-result__footer > span:last-child { display:flex; align-items:center; gap:6px; }.hiring { color:var(--c-primary); font-weight:550; }
.directory-pagination { display:flex; justify-content:center; margin-top:28px; }
@media(max-width:1050px) { .company-grid { grid-template-columns:repeat(2,minmax(0,1fr)); } }
@media(max-width:760px) { .company-grid { grid-template-columns:1fr; }.directory-filters { padding:18px 16px; }.filter-row .el-select { flex:1; width:auto; min-width:90px; }.filter-row .el-checkbox { width:100%; white-space:normal; height:auto; }.filter-row :deep(.el-checkbox__label) { white-space:normal; }.company-result { padding:20px; } }
</style>
