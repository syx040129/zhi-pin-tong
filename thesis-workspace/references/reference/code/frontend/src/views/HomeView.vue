<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { pageJobs } from '@/api/job'
import { homeStats } from '@/api/home'
import { getRecommendations } from '@/api/ai'
import { useAuthStore } from '@/stores/auth'
import { useRouter } from 'vue-router'
import { salaryText } from '@/utils/format'
import type { HomeStats, JobVO, RecommendItemVO } from '@/types/api'
import PositionCard from '@/components/PositionCard.vue'
const auth = useAuthStore()
const router = useRouter()
const keyword = ref('')
const city = ref('')
const category = ref('')
const categories = ['', '后端开发', '前端开发', '人工智能', '产品', '设计']
const cities = ['上海', '北京', '杭州', '深圳', '广州', '成都']
const jobs = ref<JobVO[]>([])
const stats = ref<HomeStats | null>(null)
const recommendations = ref<RecommendItemVO[]>([])
const loading = ref(true)
const failed = ref(false)
const isUser = computed(() => auth.isLoggedIn && auth.role === 'USER')
let requestId = 0
function search(value = keyword.value) {
  router.push({ path: '/jobs', query: { ...(value.trim() ? { keyword: value.trim() } : {}), ...(city.value ? { city: city.value } : {}) } })
}
async function loadJobs() {
  const id = ++requestId
  loading.value = true
  failed.value = false
  try {
    const result = await pageJobs({ sort: 'hot', page: 1, size: 6, jobType: category.value || undefined })
    if (id === requestId) jobs.value = result.records
  } catch { if (id === requestId) { jobs.value = []; failed.value = true } }
  finally { if (id === requestId) loading.value = false }
}
async function loadRecommendations() {
  recommendations.value = []
  if (!isUser.value) return
  try { recommendations.value = (await getRecommendations()).slice(0, 3) } catch { /* 接口负责提示 */ }
}
watch(category, loadJobs)
watch(isUser, loadRecommendations)
onMounted(() => {
  void loadJobs()
  void loadRecommendations()
  void homeStats().then(value => { stats.value = value }).catch(() => undefined)
})
</script>
<template>
  <div class="home">
    <section class="career-hero">
      <div class="career-hero__inner">
        <div class="career-hero__main">
          <h1>下一份工作，<br>从这里开始。</h1>
          <p>发现合适的岗位，也为下一次面试做好准备。</p>
          <form class="career-search" role="search" @submit.prevent="search()">
            <el-input v-model="keyword" aria-label="职位或技能关键词" placeholder="搜索职位、技能关键词…" clearable><template #prefix><el-icon aria-hidden="true"><Search /></el-icon></template></el-input>
            <el-select v-model="city" aria-label="工作城市" placeholder="全国" clearable><el-option v-for="item in cities" :key="item" :value="item" :label="item" /></el-select>
            <button class="primary-button" type="submit">搜索职位</button>
          </form>
          <div class="popular-search"><span>热门搜索</span><button v-for="word in ['Java', '前端', '数据分析', '产品经理']" :key="word" type="button" @click="search(word)">{{ word }}</button></div>
          <div v-if="stats" class="career-hero__stats"><span><strong>{{ stats.jobCount }}</strong> 个在线岗位</span><span><strong>{{ stats.companyCount }}</strong> 家招聘企业</span></div>
        </div>
        <aside class="career-guide">
          <div class="career-guide__title"><span>{{ isUser ? `${auth.user?.nickname || '你好'}，继续你的求职计划` : '把准备变成机会' }}</span><el-icon aria-hidden="true"><Briefcase /></el-icon></div>
          <template v-if="!auth.isLoggedIn || isUser">
            <router-link to="/resume"><el-icon aria-hidden="true"><Document /></el-icon><div><strong>整理一份好简历</strong><span>补全经历，让优势清晰可见</span></div><el-icon aria-hidden="true"><ArrowRight /></el-icon></router-link>
            <router-link to="/recommend"><el-icon aria-hidden="true"><Aim /></el-icon><div><strong>找到合适的岗位</strong><span>对照技能与要求，了解匹配依据</span></div><el-icon aria-hidden="true"><ArrowRight /></el-icon></router-link>
            <router-link to="/ai/interview"><el-icon aria-hidden="true"><ChatLineRound /></el-icon><div><strong>提前练习面试</strong><span>针对岗位作答，复盘改进方向</span></div><el-icon aria-hidden="true"><ArrowRight /></el-icon></router-link>
          </template>
          <template v-else><p>查看岗位、处理投递与招聘进展。</p><router-link :to="auth.roleHome()">进入{{ auth.role === 'ADMIN' ? '管理' : '招聘' }}工作台<el-icon aria-hidden="true"><ArrowRight /></el-icon></router-link></template>
          <div class="career-guide__foot">{{ auth.isLoggedIn ? '每一步，都更接近适合你的选择。' : '登录后保存简历、收藏和投递记录。' }}</div>
        </aside>
      </div>
    </section>
    <section v-if="isUser && recommendations.length" class="home-section">
      <div class="home-section__heading"><div><h2>适合你的机会</h2><p>根据你的简历推荐，查看岗位要求再做决定。</p></div><router-link to="/recommend">查看全部推荐</router-link></div>
      <div class="personal-jobs"><router-link v-for="item in recommendations" :key="item.jobId" :to="`/recommend/${item.jobId}`" class="personal-job"><div class="personal-job__score">{{ item.matchScore }}<small>/100 匹配评分</small></div><h3>{{ item.jobName }}</h3><p>{{ item.companyName }}</p><strong>{{ salaryText(item.salaryMin, item.salaryMax) }}</strong><span>{{ item.city }}</span></router-link></div>
    </section>
    <section class="home-section">
      <div class="home-section__heading"><div><h2>正在招聘</h2><p>从职位、城市和技能出发，发现新的可能。</p></div><router-link to="/jobs">浏览全部职位</router-link></div>
      <div class="category-tabs" aria-label="职位分类"><button v-for="item in categories" :key="item" type="button" :aria-pressed="category === item" :class="{ active: category === item }" @click="category = item">{{ item || '全部职位' }}</button></div>
      <div v-if="loading" class="home-jobs" aria-label="正在加载职位" aria-busy="true"><div v-for="n in 6" :key="n" class="job-skeleton"><el-skeleton :rows="4" animated /></div></div>
      <el-empty v-else-if="failed" description="职位加载失败，请重新尝试"><el-button type="primary" @click="loadJobs">重新加载</el-button></el-empty>
      <el-empty v-else-if="!jobs.length" description="该分类暂时没有在线岗位"><el-button @click="category = ''">查看全部职位</el-button></el-empty>
      <div v-else class="home-jobs"><PositionCard v-for="job in jobs" :key="job.id" :job="job" /></div>
    </section>
    <section class="home-section employer-section"><div><h2>为团队找到下一位伙伴</h2><p>发布岗位、查看候选人，让招聘进展井然有序。</p></div><router-link :to="auth.role === 'COMPANY' ? '/company/jobs/create' : '/register?role=COMPANY'" class="secondary-button">{{ auth.role === 'COMPANY' ? '发布岗位' : '企业入驻' }}<el-icon aria-hidden="true"><ArrowRight /></el-icon></router-link></section>
  </div>
</template>
<style scoped>
.home { padding-bottom:64px; }
.career-hero { background:#edf3fb; border-bottom:1px solid #dee7f2; }
.career-hero__inner { max-width:var(--content-width); margin:auto; padding:64px 24px 56px; display:grid; grid-template-columns:minmax(0,1fr) 340px; gap:72px; align-items:center; }
.career-hero h1 { font-size:clamp(36px,4vw,50px); line-height:1.3; letter-spacing:-1.5px; font-weight:650; margin:0; color:#213448; }
.career-hero__main > p { color:#526277; font-size:16px; margin:22px 0 28px; }
.career-search { display:flex; align-items:center; padding:7px; border:1px solid #c9d6e5; border-radius:9px; background:white; gap:6px; }
.career-search > .el-input { flex:1; min-width:0; }.career-search > .el-select { width:100px; flex-shrink:0; }
.career-search :deep(.el-input__wrapper),.career-search :deep(.el-select__wrapper) { box-shadow:none; min-height:44px; }
.career-search .primary-button { flex-shrink:0; }
.popular-search { display:flex; gap:12px; align-items:center; flex-wrap:wrap; font-size:13px; margin-top:14px; color:var(--c-text-2); }
.popular-search button { border:0; background:none; padding:4px 0; color:#526277; cursor:pointer; font:inherit; }.popular-search button:hover { color:var(--c-primary); }
.career-hero__stats { display:flex; gap:28px; font-size:13px; color:#526277; margin-top:30px; }.career-hero__stats strong { color:#213448; font-size:17px; font-weight:650; margin-right:4px; }
.career-guide { border:1px solid #d8e1ec; background:#fff; border-radius:12px; overflow:hidden; }
.career-guide__title { display:flex; align-items:center; justify-content:space-between; padding:22px 22px 14px; font-size:16px; font-weight:600; }.career-guide__title > .el-icon { color:var(--c-primary); }
.career-guide > a { display:flex; align-items:center; gap:14px; margin:0 12px; padding:16px 10px; border-top:1px solid var(--c-border); }.career-guide > a:hover { background:var(--c-primary-soft); }
.career-guide > a > .el-icon:first-child { font-size:20px; color:var(--c-primary); }.career-guide > a > .el-icon:last-child { color:var(--c-text-2); margin-left:auto; }
.career-guide a strong { display:block; font-size:14px; font-weight:550; }.career-guide a span { display:block; font-size:12px; color:var(--c-text-2); margin-top:5px; }
.career-guide > p { margin:12px 22px 22px; color:var(--c-text-2); font-size:14px; }.career-guide__foot { padding:14px 22px; background:#f8fafc; color:var(--c-text-2); font-size:12px; }
.home-section { max-width:var(--content-width); margin:auto; padding:42px 24px 0; }.home-section__heading { display:flex; gap:20px; justify-content:space-between; align-items:center; margin-bottom:22px; }
.home-section h2 { font-size:24px; font-weight:600; margin:0; }.home-section__heading p,.employer-section p { font-size:14px; color:var(--c-text-2); margin:8px 0 0; }
.home-section__heading > a { font-size:14px; color:var(--c-primary); flex-shrink:0; padding:8px 0; }
.category-tabs { display:flex; gap:8px; margin-bottom:20px; flex-wrap:wrap; }.category-tabs button { padding:9px 17px; border:1px solid transparent; background:transparent; border-radius:6px; font:inherit; font-size:14px; cursor:pointer; color:var(--c-text-2); }
.category-tabs button:hover { color:var(--c-primary); }.category-tabs button.active { border-color:#c8d6f8; background:var(--c-primary-soft); color:var(--c-primary); }
.home-jobs { display:grid; grid-template-columns:repeat(3,minmax(0,1fr)); gap:18px; }.job-skeleton { padding:28px; background:white; border:1px solid var(--c-border); border-radius:12px; min-height:245px; }
.personal-jobs { display:grid; grid-template-columns:repeat(3,minmax(0,1fr)); gap:18px; }.personal-job { padding:22px; border:1px solid var(--c-border); border-radius:10px; background:white; }.personal-job:hover { border-color:var(--c-primary); }
.personal-job__score { color:var(--c-primary); font-size:24px; font-weight:650; }.personal-job__score small { font-size:12px; font-weight:400; margin-left:6px; }.personal-job h3 { font-size:17px; margin:16px 0 8px; }.personal-job p { font-size:13px; color:var(--c-text-2); margin:0 0 16px; }.personal-job > strong { font-size:15px; }.personal-job > span { float:right; font-size:13px; color:var(--c-text-2); }
.employer-section { display:flex; justify-content:space-between; align-items:center; gap:24px; margin-top:44px; padding-top:30px; border-top:1px solid var(--c-border); }
@media(max-width:1050px) { .career-hero__inner { gap:32px; grid-template-columns:minmax(0,1fr) 300px; }.home-jobs { grid-template-columns:repeat(2,minmax(0,1fr)); } }
@media(max-width:760px) { .career-hero__inner { grid-template-columns:1fr; padding:36px 20px; gap:28px; }.career-guide { display:none; }.home-section { padding:30px 20px 0; }.home-section__heading { align-items:start; }.home-section h2 { font-size:22px; }.home-section__heading p { max-width:25ch; line-height:1.6; }.personal-jobs { grid-template-columns:1fr; }.career-hero h1 { letter-spacing:-1px; }.career-search { flex-wrap:wrap; }.career-search > .el-input { flex-basis:calc(100% - 110px); }.career-search .primary-button { width:100%; }.employer-section { flex-wrap:wrap; margin:32px 20px 0; padding:26px 0 0; } }
@media(max-width:560px) { .home-jobs { grid-template-columns:1fr; }.category-tabs { gap:4px; }.category-tabs button { padding:10px 12px; } }
</style>
