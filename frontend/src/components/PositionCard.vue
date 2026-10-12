<script setup lang="ts">
import { salaryText } from '@/utils/format'
import type { JobVO } from '@/types/api'
defineProps<{ job: JobVO }>()
</script>
<template>
  <router-link :to="`/jobs/${job.id}`" class="position-card">
    <div class="position-heading"><h3>{{ job.jobName }}</h3><strong>{{ salaryText(job.salaryMin, job.salaryMax) }}</strong></div>
    <div class="position-meta"><span>{{ job.city || '城市不限' }}</span><span>{{ job.experience || '经验不限' }}</span><span>{{ job.education || '学历不限' }}</span></div>
    <div class="position-skills"><span v-for="skill in job.skills?.slice(0, 3)" :key="skill.skillName">{{ skill.skillName }}</span><span v-if="!job.skills?.length">{{ job.jobType || '查看岗位要求' }}</span></div>
    <div class="position-company"><el-avatar shape="square" :size="36" :src="job.companyLogo || undefined">{{ job.companyName?.[0] || '企' }}</el-avatar><div><span>{{ job.companyName }}</span><small>{{ job.companyIndustry || '查看企业与岗位详情' }}</small></div><el-icon aria-hidden="true"><ArrowRight /></el-icon></div>
  </router-link>
</template>
<style scoped>
.position-card { display:block; min-width:0; padding:24px; background:var(--c-surface); border:1px solid var(--c-border); border-radius:12px; transition:border-color .18s,background-color .18s; }
.position-card:hover { border-color:var(--c-primary); background:#fcfdff; }
.position-heading { display:flex; align-items:baseline; justify-content:space-between; gap:12px; }
.position-heading h3 { margin:0; font-size:18px; font-weight:600; overflow-wrap:anywhere; }
.position-heading strong { flex-shrink:0; color:var(--c-primary); font-size:18px; font-weight:650; }
.position-meta { display:flex; flex-wrap:wrap; gap:12px; color:var(--c-text-2); margin:12px 0 16px; font-size:13px; }
.position-skills { display:flex; flex-wrap:wrap; gap:8px; min-height:26px; }
.position-skills span { padding:4px 9px; border-radius:4px; background:var(--c-bg); font-size:12px; color:var(--c-text-2); }
.position-company { display:flex; align-items:center; gap:12px; margin-top:22px; padding-top:18px; border-top:1px solid var(--c-border); font-size:13px; }
.position-company > div { min-width:0; flex:1; }.position-company div span { display:block; overflow:hidden; text-overflow:ellipsis; white-space:nowrap; }
.position-company small { display:block; color:var(--c-text-2); margin-top:4px; }.position-company > .el-icon { color:var(--c-text-2); }
@media(max-width:480px) { .position-card { padding:20px; }.position-heading { flex-wrap:wrap; } }
</style>
