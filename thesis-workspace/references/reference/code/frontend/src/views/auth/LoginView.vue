<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import type { FormInstance, FormRules } from 'element-plus'
import { login } from '@/api/auth'
import { useAuthStore } from '@/stores/auth'
import AuthShell from '@/components/AuthShell.vue'

defineOptions({ name: 'LoginView' })

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const formRef = ref<FormInstance>()
const loading = ref(false)
const form = reactive({
  username: '',
  password: '',
})

const rules: FormRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
}

const demos: {
  label: string
  username: string
  password: string
  type: 'primary' | 'success' | 'warning'
}[] = [
  { label: '求职者', username: 'user', password: '123456', type: 'primary' },
  { label: '企业', username: 'company', password: '123456', type: 'success' },
  { label: '管理员', username: 'admin', password: '123456', type: 'warning' },
]

function quickFill(username: string, password: string): void {
  form.username = username
  form.password = password
}

async function handleLogin(): Promise<void> {
  if (!formRef.value || loading.value) return
  try {
    await formRef.value.validate()
  } catch {
    return
  }
  loading.value = true
  try {
    const resp = await login(form.username.trim(), form.password)
    auth.setLogin(resp)
    const redirect = (route.query.redirect as string | undefined) || auth.roleHome()
    router.replace(redirect.startsWith('/') ? redirect : auth.roleHome())
  } catch {
    // 请求拦截器显示失败原因，保留表单输入供重试。
  } finally {
    loading.value = false
  }
}
</script>
<template><AuthShell><h2 class="auth-title">欢迎回来</h2><p class="auth-subtitle">登录账号，继续你的求职与招聘计划。</p><el-form ref="formRef" :model="form" :rules="rules" size="large" label-position="top" @submit.prevent>
          <el-form-item label="用户名" prop="username">
            <el-input v-model="form.username" name="username" :spellcheck="false" placeholder="请输入用户名" autocomplete="username">
              <template #prefix><el-icon><User /></el-icon></template>
            </el-input>
          </el-form-item>
          <el-form-item label="密码" prop="password">
            <el-input
              v-model="form.password" name="password"
              type="password"
              show-password
              placeholder="请输入密码"
              autocomplete="current-password"
              @keyup.enter="handleLogin"
            >
              <template #prefix><el-icon><Lock /></el-icon></template>
            </el-input>
          </el-form-item>
          <el-button class="submit-btn grad-btn" :loading="loading" @click="handleLogin">
            登录
          </el-button>
        </el-form><p class="auth-switch">还没有账号？<router-link to="/register">创建账号</router-link></p><details class="demo-accounts"><summary>使用演示账号</summary><div class="demo-options"><button v-for="demo in demos" :key="demo.username" type="button" @click="quickFill(demo.username,demo.password)"><span>{{ demo.label }}</span><small>{{ demo.username }}</small></button></div><p>选择身份会填入演示账号，点击登录继续。</p></details></AuthShell></template>
<style scoped>
.auth-title { font-size:28px; font-weight:600; margin:0 0 10px; }.auth-subtitle { color:var(--c-text-2); margin:0 0 30px; font-size:14px; }
.submit-btn { width:100%; margin-top:10px; min-height:44px; }.auth-switch { margin:22px 0 0; font-size:13px; text-align:center; color:var(--c-text-2); }.auth-switch a { color:var(--c-primary); margin-left:6px; }
.demo-accounts { margin-top:28px; padding-top:18px; border-top:1px solid var(--c-border); font-size:13px; color:var(--c-text-2); }.demo-accounts summary { cursor:pointer; padding:6px 0; }.demo-options { display:flex; gap:8px; margin-top:12px; }.demo-options button { flex:1; border:1px solid var(--c-border); border-radius:6px; padding:10px; background:white; color:var(--c-text); }.demo-options button:hover { border-color:var(--c-primary); }.demo-options small { display:block; color:var(--c-text-2); margin-top:3px; }.demo-accounts p { font-size:12px; margin:12px 0 0; }
.form-row { display:grid; grid-template-columns:1fr 1fr; gap:16px; }.role-hint { font-size:12px; color:var(--c-text-2); line-height:1.6; margin-top:8px; }
@media(max-width:480px) { .form-row { grid-template-columns:1fr; gap:0; } }
</style>
