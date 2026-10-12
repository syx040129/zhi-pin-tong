<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import type { FormInstance, FormRules } from 'element-plus'
import { ElMessage } from 'element-plus'
import type { UserRole } from '@/types/api'
import { register } from '@/api/auth'
import AuthShell from '@/components/AuthShell.vue'

defineOptions({ name: 'RegisterView' })

const router = useRouter()
const route = useRoute()
const formRef = ref<FormInstance>()
const loading = ref(false)

const form = reactive({
  role: (route.query.role === 'COMPANY' ? 'COMPANY' : 'USER') as Exclude<UserRole, 'ADMIN'>,
  username: '',
  password: '',
  confirmPassword: '',
  nickname: '',
  phone: '',
  email: '',
})

const roleOptions = [
  { value: 'USER' as const, label: '求职者' },
  { value: 'COMPANY' as const, label: '企业' },
]

const rules: FormRules = {
  role: [{ required: true, message: '请选择账号类型', trigger: 'change' }],
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { pattern: /^[a-zA-Z0-9_]{4,20}$/, message: '4-20 位字母、数字或下划线', trigger: 'blur' },
  ],
  nickname: [{ required: true, message: '请输入昵称', trigger: 'blur' }],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 30, message: '密码长度为 6-30 位', trigger: 'blur' },
  ],
  confirmPassword: [
    { required: true, message: '请再次输入密码', trigger: 'blur' },
    {
      validator: (_rule, value: string, callback) => {
        if (value !== form.password) callback(new Error('两次输入的密码不一致'))
        else callback()
      },
      trigger: 'blur',
    },
  ],
  phone: [
    {
      pattern: /^1[3-9]\d{9}$/,
      message: '请输入正确的手机号',
      trigger: 'blur',
    },
  ],
  email: [
    {
      type: 'email',
      message: '请输入正确的邮箱地址',
      trigger: 'blur',
    },
  ],
}

async function handleRegister(): Promise<void> {
  if (!formRef.value || loading.value) return
  try {
    await formRef.value.validate()
  } catch {
    return
  }
  loading.value = true
  try {
    await register({
      role: form.role,
      username: form.username.trim(),
      password: form.password,
      nickname: form.nickname.trim(),
      phone: form.phone.trim() || undefined,
      email: form.email.trim() || undefined,
    })
    ElMessage.success('注册成功，请登录')
    router.replace('/login')
  } catch {
    // 请求拦截器显示失败原因，保留表单输入供重试。
  } finally {
    loading.value = false
  }
}
</script>
<template><AuthShell registration><h2 class="auth-title">创建账号</h2><p class="auth-subtitle">选择你的身份，开始使用智聘。</p><el-form @submit.prevent ref="formRef" :model="form" :rules="rules" size="large" label-position="top" class="reg-form">
          <el-form-item label="账号类型" prop="role">
            <el-radio-group v-model="form.role">
              <el-radio-button v-for="opt in roleOptions" :key="opt.value" :value="opt.value">
                {{ opt.label }}
              </el-radio-button>
            </el-radio-group>
            <div v-if="form.role === 'COMPANY'" class="role-hint">
              企业注册说明：企业名称默认使用昵称，注册完成后可进入企业端完善资料。
            </div>
          </el-form-item>

          <div class="form-row">
            <el-form-item label="用户名" prop="username">
              <el-input v-model="form.username" name="username" :spellcheck="false" placeholder="4-20 位字母、数字或下划线" autocomplete="username">
                <template #prefix><el-icon><User /></el-icon></template>
              </el-input>
            </el-form-item>
            <el-form-item label="昵称" prop="nickname">
              <el-input v-model="form.nickname" placeholder="请输入昵称" />
            </el-form-item>
          </div>

          <div class="form-row">
            <el-form-item label="密码" prop="password">
              <el-input
                v-model="form.password" name="password"
                type="password"
                show-password
                placeholder="6-30 位"
                autocomplete="new-password"
              >
                <template #prefix><el-icon><Lock /></el-icon></template>
              </el-input>
            </el-form-item>
            <el-form-item label="确认密码" prop="confirmPassword">
              <el-input
                v-model="form.confirmPassword"
                type="password"
                show-password
                placeholder="再次输入密码"
                autocomplete="new-password"
              >
                <template #prefix><el-icon><Lock /></el-icon></template>
              </el-input>
            </el-form-item>
          </div>

          <el-form-item label="手机号" prop="phone">
            <el-input v-model="form.phone" type="tel" autocomplete="tel" name="phone" placeholder="选填，便于企业联系你">
              <template #prefix><el-icon><Iphone /></el-icon></template>
            </el-input>
          </el-form-item>
          <el-form-item label="邮箱" prop="email">
            <el-input v-model="form.email" type="email" autocomplete="email" name="email" placeholder="选填，用于接收通知">
              <template #prefix><el-icon><Message /></el-icon></template>
            </el-input>
          </el-form-item>

          <el-button class="submit-btn grad-btn" :loading="loading" @click="handleRegister">
            创建账号
          </el-button>
        </el-form><p class="auth-switch">已有账号？<router-link to="/login">直接登录</router-link></p></AuthShell></template>
<style scoped>
.auth-title { font-size:28px; font-weight:600; margin:0 0 10px; }.auth-subtitle { color:var(--c-text-2); margin:0 0 30px; font-size:14px; }
.submit-btn { width:100%; margin-top:10px; min-height:44px; }.auth-switch { margin:22px 0 0; font-size:13px; text-align:center; color:var(--c-text-2); }.auth-switch a { color:var(--c-primary); margin-left:6px; }
.demo-accounts { margin-top:28px; padding-top:18px; border-top:1px solid var(--c-border); font-size:13px; color:var(--c-text-2); }.demo-accounts summary { cursor:pointer; padding:6px 0; }.demo-options { display:flex; gap:8px; margin-top:12px; }.demo-options button { flex:1; border:1px solid var(--c-border); border-radius:6px; padding:10px; background:white; color:var(--c-text); }.demo-options button:hover { border-color:var(--c-primary); }.demo-options small { display:block; color:var(--c-text-2); margin-top:3px; }.demo-accounts p { font-size:12px; margin:12px 0 0; }
.form-row { display:grid; grid-template-columns:1fr 1fr; gap:16px; }.role-hint { font-size:12px; color:var(--c-text-2); line-height:1.6; margin-top:8px; }
@media(max-width:480px) { .form-row { grid-template-columns:1fr; gap:0; } }
</style>
