<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules, UploadRequestOptions } from 'element-plus'
import { getCompanyProfile, updateCompanyProfile, uploadCompanyLogo } from '@/api/company'
import type { CompanyForm, CompanyVO } from '@/types/api'
import { companyStatusMap, tagOf } from '@/utils/format'

defineOptions({ name: 'CompanyProfileView' })

const loading = ref(false)
const saving = ref(false)
const uploading = ref(false)
const profile = ref<CompanyVO | null>(null)
const formRef = ref<FormInstance>()

const industryOptions = [
  '互联网/软件开发',
  '电子商务',
  '大数据/数据服务',
  '人工智能',
  '信息安全',
  '游戏/文娱',
  '金融',
  '制造业',
]

const scaleOptions = ['0-20人', '20-99人', '100-499人', '500-999人', '1000-9999人']

const form = reactive<CompanyForm>({
  companyName: '',
  industry: undefined,
  scale: undefined,
  address: undefined,
  website: undefined,
  description: undefined,
})

const rules: FormRules = {
  companyName: [{ required: true, message: '请输入企业名称', trigger: 'blur' }],
}

const fallbackChar = computed(() => {
  const name = form.companyName || profile.value?.companyName || '企'
  return name.trim().charAt(0) || '企'
})

function fillForm(data: CompanyVO): void {
  form.companyName = data.companyName ?? ''
  form.industry = data.industry ?? undefined
  form.scale = data.scale ?? undefined
  form.address = data.address ?? undefined
  form.website = data.website ?? undefined
  form.description = data.description ?? undefined
}

async function fetchProfile(): Promise<void> {
  loading.value = true
  try {
    profile.value = await getCompanyProfile()
    if (profile.value) fillForm(profile.value)
  } catch {
    // 错误提示由请求拦截器统一处理
  } finally {
    loading.value = false
  }
}

async function handleLogoUpload(options: UploadRequestOptions): Promise<void> {
  if (uploading.value) return
  uploading.value = true
  try {
    await uploadCompanyLogo(options.file)
    ElMessage.success('企业 Logo 已更新')
    await fetchProfile()
  } catch {
    // 拦截器已提示
  } finally {
    uploading.value = false
  }
}

async function handleSave(): Promise<void> {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  saving.value = true
  try {
    const payload: CompanyForm = {
      companyName: (form.companyName ?? '').trim(),
      industry: form.industry || undefined,
      scale: form.scale || undefined,
      address: form.address || undefined,
      website: form.website || undefined,
      description: form.description || undefined,
    }
    await updateCompanyProfile(payload)
    ElMessage.success('企业资料已保存')
    await fetchProfile()
  } catch {
    // 拦截器已提示
  } finally {
    saving.value = false
  }
}

function handleReset(): void {
  if (profile.value) {
    fillForm(profile.value)
    formRef.value?.clearValidate()
  }
}

onMounted(() => {
  void fetchProfile()
})
</script>

<template>
  <div v-loading="loading" class="profile-page">
    <div class="panel head-card">
      <div class="logo-wrap">
        <el-upload
          :show-file-list="false"
          :http-request="handleLogoUpload"
          accept="image/*"
          :disabled="uploading"
          class="logo-uploader"
        >
          <div class="logo-box">
            <img v-if="profile?.logo" :src="profile.logo" alt="企业Logo" class="logo-img" />
            <div v-else class="logo-img logo-fallback">{{ fallbackChar }}</div>
            <div class="logo-mask">
              <el-icon v-if="!uploading"><Upload /></el-icon>
              <span>{{ uploading ? '上传中…' : '更换' }}</span>
            </div>
          </div>
        </el-upload>
      </div>

      <div class="head-info">
        <div class="head-info__title">
          {{ profile?.companyName || '未命名企业' }}
          <el-tag v-if="profile" :type="tagOf(companyStatusMap, profile.status).type" round class="status-tag">
            {{ tagOf(companyStatusMap, profile.status).label }}
          </el-tag>
        </div>
        <div class="head-info__rows">
          <div class="head-info__row">
            <el-icon><UserFilled /></el-icon>
            <span class="head-info__label">绑定账号</span>
            <span class="head-info__value">{{ profile?.username || '-' }}</span>
          </div>
          <div v-if="profile?.contactPhone" class="head-info__row">
            <el-icon><Phone /></el-icon>
            <span class="head-info__label">联系电话</span>
            <span class="head-info__value">{{ profile.contactPhone }}</span>
          </div>
          <div class="head-info__row">
            <el-icon><OfficeBuilding /></el-icon>
            <span class="head-info__label">所属行业</span>
            <span class="head-info__value">{{ profile?.industry || '-' }}</span>
          </div>
        </div>
      </div>
    </div>

    <div class="panel form-card">
      <div class="section-title">
        编辑企业资料
        <span class="section-title__hint">绑定登录账号与联系电话由系统维护，仅展示不可修改</span>
      </div>

      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px" class="company-form">
        <el-row :gutter="24">
          <el-col :xs="24" :md="12">
            <el-form-item label="企业名称" prop="companyName">
              <el-input v-model="form.companyName" placeholder="请输入企业全称" maxlength="60" show-word-limit clearable />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :md="12">
            <el-form-item label="所属行业" prop="industry">
              <el-select
                v-model="form.industry"
                placeholder="请选择或输入行业"
                filterable
                allow-create
                default-first-option
                clearable
                class="full-width"
              >
                <el-option v-for="item in industryOptions" :key="item" :label="item" :value="item" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :xs="24" :md="12">
            <el-form-item label="公司规模" prop="scale">
              <el-select v-model="form.scale" placeholder="请选择公司规模" clearable class="full-width">
                <el-option v-for="item in scaleOptions" :key="item" :label="item" :value="item" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :xs="24" :md="12">
            <el-form-item label="企业官网" prop="website">
              <el-input v-model="form.website" placeholder="https://example.com" maxlength="200" clearable>
                <template #prefix>
                  <el-icon><Link /></el-icon>
                </template>
              </el-input>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="所在地址" prop="address">
              <el-input v-model="form.address" placeholder="请输入详细办公地址" maxlength="120" clearable>
                <template #prefix>
                  <el-icon><Location /></el-icon>
                </template>
              </el-input>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="企业介绍" prop="description">
              <el-input
                v-model="form.description"
                type="textarea"
                :rows="5"
                maxlength="800"
                show-word-limit
                placeholder="介绍一下公司主营业务、团队与文化，帮助候选人了解你们"
              />
            </el-form-item>
          </el-col>
        </el-row>

        <div class="form-actions">
          <button type="button" class="grad-btn" :disabled="saving" @click="handleSave">
            <el-icon v-if="saving" class="is-loading" style="margin-right: 5px"><Loading /></el-icon>
            {{ saving ? '保存中…' : '保存修改' }}
          </button>
          <el-button :disabled="saving" @click="handleReset">重置</el-button>
        </div>
      </el-form>
    </div>
  </div>
</template>

<style scoped>
.profile-page {
  min-height: 100%;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.head-card {
  display: flex;
  align-items: center;
  gap: 24px;
  padding: 22px 26px;
}

.logo-box {
  position: relative;
  width: 88px;
  height: 88px;
  overflow: hidden;
  border-radius: 16px;
  cursor: pointer;
  background: var(--c-primary-soft);
}

.logo-img {
  width: 88px;
  height: 88px;
  display: flex;
  align-items: center;
  justify-content: center;
  object-fit: cover;
}

.logo-fallback {
  color: var(--c-primary);
  font-size: 40px;
  font-weight: 700;
}

.logo-mask {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 2px;
  color: #fff;
  font-size: 12px;
  background: rgba(15, 23, 42, 0.45);
  opacity: 0;
  transition: opacity 0.2s ease;
}

.logo-box:hover .logo-mask {
  opacity: 1;
}

.head-info {
  min-width: 0;
  flex: 1;
}

.head-info__title {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 20px;
  font-weight: 700;
  color: var(--c-text);
}

.head-info__rows {
  display: flex;
  flex-wrap: wrap;
  gap: 10px 28px;
  margin-top: 14px;
}

.head-info__row {
  display: flex;
  align-items: center;
  gap: 6px;
  color: var(--c-text-3);
  font-size: 13px;
}

.head-info__label {
  color: var(--c-text-3);
}

.head-info__value {
  color: var(--c-text-2);
  font-weight: 500;
}

.form-card {
  padding: 22px 26px 26px;
}

.section-title {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 20px;
  font-size: 16px;
  font-weight: 600;
  color: var(--c-text);
}

.section-title__hint {
  font-size: 12px;
  font-weight: 400;
  color: var(--c-text-3);
}

.company-form {
  max-width: 900px;
}

.full-width {
  width: 100%;
}

.form-actions {
  display: flex;
  gap: 12px;
  margin-top: 6px;
}
</style>
