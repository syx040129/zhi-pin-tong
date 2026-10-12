<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import LogoMark from '@/components/LogoMark.vue'
const auth = useAuthStore()
const route = useRoute()
const router = useRouter()
const menuOpen = ref(false)
const navItems = [{ path:'/',label:'首页' },{ path:'/jobs',label:'找工作' },{ path:'/recommend',label:'岗位推荐' },{ path:'/resume',label:'我的简历' },{ path:'/ai/interview',label:'面试练习' }]
const displayName = computed(() => auth.user?.nickname || auth.user?.companyName || auth.user?.username || '用户')
watch(() => route.fullPath, () => { menuOpen.value = false })
function handleCommand(command:string) {
  if(command === 'logout') { auth.logout(); router.push('/login') } else router.push(command)
}
function isActive(path:string) { return path === '/' ? route.path === '/' : route.path.startsWith(path) }
</script>
<template>
  <div class="user-layout">
    <a class="skip-link" href="#main-content">跳到主要内容</a>
    <header class="site-header"><div class="site-header__inner">
      <router-link to="/" aria-label="智聘首页"><LogoMark :size="34" show-text /></router-link>
      <nav class="site-nav" aria-label="主导航"><router-link v-for="item in navItems" :key="item.path" :to="item.path" :class="{ active:isActive(item.path) }" :aria-current="isActive(item.path) ? 'page' : undefined">{{ item.label }}</router-link></nav>
      <div class="header-actions">
        <router-link v-if="!auth.isLoggedIn || auth.role === 'COMPANY'" :to="auth.role === 'COMPANY' ? '/company' : '/register?role=COMPANY'" class="employer-link">企业招聘</router-link>
        <template v-if="auth.isLoggedIn"><el-dropdown trigger="click" @command="handleCommand"><button class="account-button" type="button" aria-label="账号菜单"><el-avatar :size="30" :src="auth.user?.avatar || undefined">{{ displayName[0] }}</el-avatar><span>{{ displayName }}</span><el-icon aria-hidden="true"><ArrowDown /></el-icon></button><template #dropdown><el-dropdown-menu><template v-if="auth.role === 'USER'"><el-dropdown-item command="/profile">个人中心</el-dropdown-item><el-dropdown-item command="/applications">我的投递</el-dropdown-item><el-dropdown-item command="/favorites">我的收藏</el-dropdown-item></template><el-dropdown-item v-else :command="auth.roleHome()">进入工作台</el-dropdown-item><el-dropdown-item divided command="logout">退出登录</el-dropdown-item></el-dropdown-menu></template></el-dropdown></template>
        <router-link v-else to="/login" class="primary-button header-login">登录 / 注册</router-link>
        <button class="mobile-menu-toggle" type="button" aria-label="展开导航" :aria-expanded="menuOpen" aria-controls="mobile-navigation" @click="menuOpen = !menuOpen"><el-icon><Close v-if="menuOpen" /><Menu v-else /></el-icon></button>
      </div>
    </div><nav v-if="menuOpen" id="mobile-navigation" class="mobile-navigation" aria-label="移动端导航"><router-link v-for="item in navItems" :key="item.path" :to="item.path" :class="{ active:isActive(item.path) }">{{ item.label }}</router-link><router-link v-if="auth.role === 'USER'" to="/applications">我的投递</router-link><router-link v-if="auth.role === 'USER'" to="/favorites">我的收藏</router-link><router-link v-if="!auth.isLoggedIn" to="/register?role=COMPANY">企业招聘</router-link></nav></header>
    <main id="main-content" tabindex="-1"><router-view /></main>
    <footer class="site-footer"><div><LogoMark :size="28" show-text /><span>发现机会，认真选择。</span></div><nav aria-label="页脚导航"><router-link to="/jobs">浏览职位</router-link><router-link to="/resume">简历管理</router-link><router-link to="/ai/interview">面试练习</router-link></nav><small>© 2026 CareerNova</small></footer>
  </div>
</template>
<style scoped>
.user-layout { min-height:100vh; }.site-header { background:white; border-bottom:1px solid var(--c-border); position:sticky; top:0; z-index:30; }
.site-header__inner { height:72px; max-width:var(--content-width); margin:auto; padding:0 24px; display:flex; gap:44px; align-items:center; }
.site-nav { display:flex; align-self:stretch; gap:28px; }.site-nav a { display:flex; align-items:center; position:relative; font-size:14px; color:var(--c-text-2); white-space:nowrap; }
.site-nav a:hover,.site-nav a.active { color:var(--c-primary); }.site-nav a.active::after { content:''; position:absolute; bottom:0; left:0; right:0; height:3px; border-radius:2px 2px 0 0; background:var(--c-primary); }
.header-actions { margin-left:auto; display:flex; gap:20px; align-items:center; }.employer-link { color:var(--c-text-2); font-size:13px; white-space:nowrap; }.header-login { min-height:38px; padding:0 18px; white-space:nowrap; }
.account-button { display:flex; gap:8px; align-items:center; padding:6px; border:0; background:white; color:var(--c-text); border-radius:7px; }.account-button span:not(.el-avatar) { max-width:100px; overflow:hidden; text-overflow:ellipsis; white-space:nowrap; }.account-button:hover { background:var(--c-bg); }
.mobile-menu-toggle { display:none; width:44px; height:44px; border:1px solid var(--c-border); border-radius:6px; background:white; color:var(--c-text); font-size:20px; }
.mobile-navigation { display:grid; grid-template-columns:repeat(2,minmax(0,1fr)); gap:4px; padding:8px 16px 16px; border-top:1px solid var(--c-border); }.mobile-navigation a { padding:12px; border-radius:6px; }.mobile-navigation a.active { color:var(--c-primary); background:var(--c-primary-soft); }
.site-footer { border-top:1px solid var(--c-border); max-width:1200px; margin:auto; padding:30px 24px 36px; display:flex; gap:28px; align-items:center; color:var(--c-text-2); font-size:12px; }.site-footer > div { display:flex; gap:18px; align-items:center; }.site-footer nav { display:flex; gap:20px; margin-left:auto; }.site-footer a:hover { color:var(--c-primary); }
@media(max-width:1000px) { .site-header__inner { gap:28px; }.site-nav { gap:18px; }.employer-link { display:none; } }
@media(max-width:760px) { .site-header__inner { height:64px; padding:0 16px; gap:12px; }.site-nav { display:none; }.mobile-menu-toggle { display:inline-flex; align-items:center; justify-content:center; }.header-actions { gap:10px; }.account-button > span:not(.el-avatar),.account-button > .el-icon { display:none; }.site-footer { margin:0 16px; padding:24px 0 80px; flex-wrap:wrap; gap:18px; }.site-footer nav { margin-left:0; }.site-footer small { width:100%; } }
</style>
