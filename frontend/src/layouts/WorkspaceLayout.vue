<script setup lang="ts">
import { computed, onMounted, onBeforeUnmount, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import LogoMark from '@/components/LogoMark.vue'
const props = defineProps<{ role:'COMPANY'|'ADMIN' }>()
const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const collapsed = ref(false)
const mobile = ref(false)
const drawerOpen = ref(false)
const root = computed(() => props.role === 'COMPANY' ? '/company' : '/admin')
const label = computed(() => props.role === 'COMPANY' ? '招聘工作台' : '平台管理')
const companyMenus = [{ path:'/company',label:'招聘概览',icon:'DataAnalysis' },{ path:'/company/jobs',label:'岗位管理',icon:'Briefcase' },{ path:'/company/applications',label:'收到投递',icon:'Tickets' },{ path:'/company/candidates',label:'候选人库',icon:'UserFilled' },{ path:'/company/profile',label:'企业信息',icon:'OfficeBuilding' }]
const adminMenus = [{ path:'/admin',label:'平台概览',icon:'DataAnalysis' },{ path:'/admin/users',label:'用户管理',icon:'User' },{ path:'/admin/companies',label:'企业管理',icon:'OfficeBuilding' },{ path:'/admin/jobs',label:'岗位管理',icon:'Briefcase' },{ path:'/admin/resumes',label:'简历管理',icon:'Document' },{ path:'/admin/applications',label:'投递管理',icon:'Tickets' },{ path:'/admin/reports',label:'举报处理',icon:'Warning' },{ path:'/admin/logs',label:'操作日志',icon:'List' },{ path:'/admin/settings',label:'AI 服务设置',icon:'Setting' }]
const menus = computed(() => props.role === 'COMPANY' ? companyMenus : adminMenus)
const name = computed(() => auth.user?.companyName || auth.user?.nickname || auth.user?.username || '账号')
function active(path:string) { return path === root.value ? route.path === path : route.path.startsWith(path) }
let media:MediaQueryList
function updateMobile() { mobile.value = media.matches; drawerOpen.value = false }
onMounted(() => { media = window.matchMedia('(max-width: 900px)'); updateMobile(); media.addEventListener('change',updateMobile) })
onBeforeUnmount(() => media?.removeEventListener('change',updateMobile))
watch(() => route.fullPath,() => { drawerOpen.value = false })
function toggle() { if(mobile.value) drawerOpen.value = !drawerOpen.value; else collapsed.value = !collapsed.value }
function handleCommand(command:string) { if(command === 'logout') { auth.logout(); router.push('/login') } else router.push(command) }
</script>
<template>
  <div class="workspace" :class="{ 'is-collapsed':collapsed && !mobile,'is-mobile':mobile }">
    <a class="skip-link" href="#workspace-content">跳到主要内容</a>
    <button v-if="mobile && drawerOpen" type="button" class="workspace-scrim" aria-label="关闭导航" @click="drawerOpen=false" />
    <aside id="workspace-sidebar" class="workspace-sidebar" :class="{ 'is-open':drawerOpen }" :inert="mobile && !drawerOpen">
      <router-link :to="root" class="workspace-brand" aria-label="工作台首页"><LogoMark :size="32" :show-text="!collapsed || mobile" /></router-link>
      <span v-if="!collapsed || mobile" class="workspace-label">{{ label }}</span>
      <nav class="workspace-nav" :aria-label="label"><router-link v-for="item in menus" :key="item.path" :to="item.path" :class="{ active:active(item.path) }" :aria-current="active(item.path) ? 'page' : undefined" :title="item.label"><el-icon aria-hidden="true"><component :is="item.icon" /></el-icon><span v-if="!collapsed || mobile">{{ item.label }}</span></router-link></nav>
      <router-link to="/" class="workspace-site-link"><el-icon aria-hidden="true"><Back /></el-icon><span v-if="!collapsed || mobile">返回招聘网站</span></router-link>
    </aside>
    <div class="workspace-main">
      <header class="workspace-topbar"><button type="button" class="workspace-toggle" :aria-label="mobile ? '展开导航' : (collapsed ? '展开侧边栏' : '收起侧边栏')" :aria-expanded="mobile ? drawerOpen : !collapsed" aria-controls="workspace-sidebar" @click="toggle"><el-icon aria-hidden="true"><Fold v-if="!collapsed && !mobile" /><Expand v-else /></el-icon></button><span class="workspace-breadcrumb">{{ label }}</span><span class="workspace-separator">/</span><span class="workspace-page-title">{{ route.meta.title }}</span><div class="workspace-account"><el-dropdown trigger="click" @command="handleCommand"><button type="button" class="workspace-account-button" aria-label="账号菜单"><el-avatar :size="30" :src="auth.user?.avatar || undefined">{{ name[0] }}</el-avatar><span>{{ name }}</span><el-icon aria-hidden="true"><ArrowDown /></el-icon></button><template #dropdown><el-dropdown-menu><el-dropdown-item v-if="role==='COMPANY'" command="/company/profile">企业资料</el-dropdown-item><el-dropdown-item command="/">招聘网站</el-dropdown-item><el-dropdown-item divided command="logout">退出登录</el-dropdown-item></el-dropdown-menu></template></el-dropdown></div></header>
      <main id="workspace-content" class="workspace-content" tabindex="-1"><router-view /></main>
    </div>
  </div>
</template>
<style scoped>
.workspace { min-height:100vh; --sidebar-width:224px; }.workspace.is-collapsed { --sidebar-width:76px; }
.workspace-sidebar { position:fixed; top:0; bottom:0; left:0; width:var(--sidebar-width); background:white; border-right:1px solid var(--c-border); z-index:40; display:flex; flex-direction:column; padding:0 16px 20px; }
.workspace-brand { display:flex; align-items:center; min-height:76px; padding:0 6px; }.workspace-label { font-size:12px; color:var(--c-text-2); padding:12px 14px 14px; border-top:1px solid var(--c-border); }
.workspace-nav { display:flex; flex-direction:column; gap:6px; flex:1; overflow-y:auto; padding-top:8px; }.workspace-nav a { display:flex; align-items:center; gap:12px; min-height:44px; padding:0 14px; border-radius:6px; color:var(--c-text-2); font-size:14px; white-space:nowrap; }
.workspace-nav a > .el-icon { font-size:18px; flex-shrink:0; }.workspace-nav a:hover { background:var(--c-bg); color:var(--c-text); }.workspace-nav a.active { color:var(--c-primary); background:var(--c-primary-soft); font-weight:550; }
.workspace-site-link { display:flex; gap:12px; align-items:center; border-top:1px solid var(--c-border); padding:20px 14px 0; color:var(--c-text-2); font-size:13px; }
.workspace-main { margin-left:var(--sidebar-width); min-width:0; }.workspace-topbar { min-height:72px; display:flex; align-items:center; gap:16px; padding:12px 32px; background:white; border-bottom:1px solid var(--c-border); }
.workspace-toggle { display:flex; align-items:center; justify-content:center; width:40px; height:40px; background:white; border:1px solid var(--c-border); color:var(--c-text-2); border-radius:6px; font-size:18px; }.workspace-toggle:hover { color:var(--c-primary); }
.workspace-breadcrumb,.workspace-separator { color:var(--c-text-2); font-size:13px; }.workspace-page-title { font-size:14px; }.workspace-account { margin-left:auto; }.workspace-account-button { display:flex; align-items:center; gap:8px; background:white; border:0; border-radius:6px; padding:6px; color:var(--c-text); }.workspace-account-button > span:not(.el-avatar) { max-width:180px; overflow:hidden; text-overflow:ellipsis; white-space:nowrap; }
.workspace-content { padding:32px; max-width:1640px; margin:auto; min-width:0; }.workspace-scrim { position:fixed; inset:0; z-index:35; border:0; background:rgba(30,48,75,.3); }
@media(max-width:900px) { .workspace { --sidebar-width:224px; }.workspace-sidebar { transform:translateX(-100%); }.workspace-sidebar.is-open { transform:translateX(0); }.workspace-main { margin-left:0; }.workspace-topbar { padding:10px 16px; gap:10px; min-height:64px; }.workspace-content { padding:24px 16px 96px; }.workspace-breadcrumb,.workspace-separator { display:none; }.workspace-account-button > span:not(.el-avatar) { max-width:110px; } }
</style>
