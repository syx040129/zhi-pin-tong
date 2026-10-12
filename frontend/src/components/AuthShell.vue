<script setup lang="ts">
import LogoMark from '@/components/LogoMark.vue'
defineProps<{ registration?:boolean }>()
</script>
<template>
  <div class="auth-shell"><aside class="auth-story"><router-link to="/" aria-label="返回智聘首页"><LogoMark :size="36" show-text /></router-link><div class="auth-story__content"><h1>{{ registration ? '新的机会，值得认真准备。' : '让下一步，更有方向。' }}</h1><p>找工作、整理简历、练习面试。<br>把每一步准备，留在这里。</p><div class="auth-story__steps"><div><el-icon aria-hidden="true"><Document /></el-icon><span><strong>让经历清晰可见</strong><small>整理简历，查看分析与改进建议</small></span></div><div><el-icon aria-hidden="true"><Briefcase /></el-icon><span><strong>发现合适的职位</strong><small>按城市、技能与经验筛选机会</small></span></div><div><el-icon aria-hidden="true"><ChatLineRound /></el-icon><span><strong>为面试做好准备</strong><small>按岗位练习，保存每次复盘</small></span></div></div></div><small class="auth-story__footer">CareerNova · 智聘</small></aside><main class="auth-form-side"><router-link to="/" class="auth-back"><el-icon aria-hidden="true"><ArrowLeft /></el-icon>返回首页</router-link><div class="auth-form-card"><slot /></div><p class="auth-bottom">{{ registration ? '注册后可管理简历、投递记录与企业招聘信息。' : '欢迎回来，继续查看你的求职与招聘进展。' }}</p></main></div>
</template>
<style scoped>
.auth-shell { min-height:100vh; display:grid; grid-template-columns:minmax(360px,.9fr) minmax(0,1.1fr); background:white; }
.auth-story { display:flex; flex-direction:column; padding:44px 64px; background:#edf3fb; border-right:1px solid #dfe7f1; }.auth-story__content { margin:auto 0; padding:56px 0; max-width:380px; }.auth-story h1 { font-size:38px; font-weight:650; line-height:1.45; letter-spacing:-1px; margin:0; }.auth-story p { font-size:16px; line-height:1.8; margin:24px 0 40px; color:#526277; }
.auth-story__steps { display:grid; gap:26px; }.auth-story__steps > div { display:flex; gap:16px; align-items:center; }.auth-story__steps .el-icon { font-size:24px; color:var(--c-primary); }.auth-story__steps strong { display:block; font-size:15px; font-weight:550; }.auth-story__steps small { display:block; font-size:13px; color:var(--c-text-2); margin-top:6px; }.auth-story__footer { color:var(--c-text-2); font-size:12px; }
.auth-form-side { padding:40px; display:flex; flex-direction:column; }.auth-back { display:inline-flex; align-items:center; gap:8px; color:var(--c-text-2); font-size:13px; align-self:flex-end; min-height:36px; }.auth-form-card { width:100%; max-width:440px; margin:auto; padding:40px 0; }.auth-bottom { text-align:center; color:var(--c-text-2); font-size:12px; }
@media(max-width:1000px) { .auth-story { padding:36px; }.auth-story h1 { font-size:32px; } }
@media(max-width:760px) { .auth-shell { grid-template-columns:1fr; }.auth-story { display:none; }.auth-form-side { padding:24px; min-height:100vh; }.auth-form-card { padding:30px 0; }.auth-bottom { line-height:1.7; } }
</style>
