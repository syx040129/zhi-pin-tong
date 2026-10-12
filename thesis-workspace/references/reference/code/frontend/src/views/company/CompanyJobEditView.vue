<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { companyJobs, createCompanyJob, updateCompanyJob } from '@/api/company'
import type { JobForm, JobVO } from '@/types/api'

defineOptions({ name: 'CompanyJobEditView' })

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const saving = ref(false)
const formRef = ref<FormInstance>()

const isEdit = computed(() => route.name === 'CompanyJobEdit')
const pageTitle = computed(() => (isEdit.value ? '编辑岗位' : '发布岗位'))

const jobTypeOptions = [
  '后端开发',
  '前端开发',
  '大数据',
  '人工智能',
  '信息安全',
  '测试',
  '运维',
  '游戏开发',
  '运营',
  '数据分析',
  '产品',
  '设计',
  '全栈开发',
]

const cityOptions = ['上海', '北京', '杭州', '深圳', '广州', '成都']
const educationOptions = ['不限', '大专', '本科', '硕士', '博士']
const experienceOptions = ['不限', '在校生', '应届', '1-3年', '3-5年', '5-10年', '10年以上']

interface SkillRow {
  skillName: string
  requiredLevel: number
  weight: number
}

function createSkillRow(): SkillRow {
  return { skillName: '', requiredLevel: 3, weight: 1 }
}

const form = reactive({
  jobName: '',
  jobType: '',
  salaryMin: 0,
  salaryMax: 0,
  city: '',
  education: '',
  experience: '',
  description: '',
  requirements: '',
  skills: [] as SkillRow[],
})

function validateSalary(_rule: unknown, _value: unknown, callback: (error?: Error) => void): void {
  const min = Number(form.salaryMin) || 0
  const max = Number(form.salaryMax) || 0
  if (min > max) {
    callback(new Error('最低薪资不能高于最高薪资'))
  } else {
    callback()
  }
}

const rules: FormRules = {
  jobName: [{ required: true, message: '请输入岗位名称', trigger: 'blur' }],
  jobType: [{ required: true, message: '请选择岗位类型', trigger: 'change' }],
  salaryMin: [{ validator: validateSalary, trigger: 'change' }],
  salaryMax: [{ required: true, validator: validateSalary, trigger: 'change' }],
  city: [{ required: true, message: '请选择工作城市', trigger: 'change' }],
  education: [{ required: true, message: '请选择学历要求', trigger: 'change' }],
  experience: [{ required: true, message: '请选择经验要求', trigger: 'change' }],
  description: [{ required: true, message: '请输入岗位描述', trigger: 'blur' }],
  requirements: [{ required: true, message: '请输入任职要求', trigger: 'blur' }],
}

function fillFromJob(job: JobVO): void {
  form.jobName = job.jobName ?? ''
  form.jobType = job.jobType ?? ''
  form.salaryMin = job.salaryMin ?? 0
  form.salaryMax = job.salaryMax ?? 0
  form.city = job.city ?? ''
  form.education = job.education ?? ''
  form.experience = job.experience ?? ''
  form.description = job.description ?? ''
  form.requirements = job.requirements ?? ''
  form.skills = (job.skills ?? []).map((s) => ({
    skillName: s.skillName ?? '',
    requiredLevel: s.requiredLevel ?? 3,
    weight: s.weight ?? 1,
  }))
}

async function loadJob(): Promise<void> {
  if (!isEdit.value) return
  loading.value = true
  try {
    const jobId = Number(route.params.id)
    const res = await companyJobs({ page: 1, size: 100 })
    const found = res.records.find((item) => item.id === jobId)
    if (found) {
      fillFromJob(found)
    } else {
      ElMessage.warning('未找到对应岗位，请返回列表重试')
    }
  } catch {
    // 错误提示由请求拦截器统一处理
  } finally {
    loading.value = false
  }
}

function buildPayload(): JobForm {
  return {
    jobName: form.jobName.trim(),
    jobType: form.jobType,
    salaryMin: Number(form.salaryMin) || 0,
    salaryMax: Number(form.salaryMax) || 0,
    city: form.city,
    education: form.education,
    experience: form.experience,
    description: form.description.trim(),
    requirements: form.requirements.trim(),
    skills: form.skills
      .filter((s) => s.skillName.trim())
      .map((s) => ({
        skillName: s.skillName.trim(),
        requiredLevel: s.requiredLevel,
        weight: s.weight,
      })),
  }
}

function handleSubmit(): void {
  formRef.value?.validate(async (valid: boolean) => {
    if (!valid || saving.value) return
    saving.value = true
    try {
      const payload = buildPayload()
      if (isEdit.value) {
        await updateCompanyJob(Number(route.params.id), payload)
        ElMessage.success('岗位更新成功')
      } else {
        await createCompanyJob(payload)
        ElMessage.success('岗位发布成功')
      }
      void router.push('/company/jobs')
    } catch {
      // 拦截器已提示
    } finally {
      saving.value = false
    }
  })
}

function goBack(): void {
  void router.push('/company/jobs')
}

onMounted(() => {
  void loadJob()
})
</script>

<template>
  <div v-loading="loading" class="edit-page">
    <div class="edit-head">
      <div class="edit-head__left">
        <el-button circle plain class="back-btn" @click="goBack">
          <el-icon><ArrowLeft /></el-icon>
        </el-button>
        <div>
          <div class="edit-head__title">{{ pageTitle }}</div>
          <div class="edit-head__sub">
            {{ isEdit ? '修改岗位信息并重新上架' : '完善岗位信息，立即开始收取候选人投递' }}
          </div>
        </div>
      </div>
    </div>

    <div class="panel form-card">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="110px" class="job-form">
        <div class="section-label">基本信息</div>

        <el-row :gutter="24">
          <el-col :xs="24" :md="12">
            <el-form-item label="岗位名称" prop="jobName">
              <el-input v-model="form.jobName" placeholder="如：高级前端开发工程师" maxlength="60" show-word-limit clearable />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :md="12">
            <el-form-item label="岗位类型" prop="jobType">
              <el-select v-model="form.jobType" placeholder="请选择岗位类型" filterable clearable class="full-width">
                <el-option v-for="item in jobTypeOptions" :key="item" :label="item" :value="item" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :xs="24" :md="12">
            <el-form-item label="最低薪资(K)" prop="salaryMin">
              <el-input-number
                v-model="form.salaryMin"
                :min="0"
                :max="500"
                :step="1"
                controls-position="right"
                class="full-width"
                placeholder="最低月薪"
              />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :md="12">
            <el-form-item label="最高薪资(K)" prop="salaryMax">
              <el-input-number
                v-model="form.salaryMax"
                :min="0"
                :max="500"
                :step="1"
                controls-position="right"
                class="full-width"
                placeholder="最高月薪，0 或留空表示面议"
              />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :md="12">
            <el-form-item label="工作城市" prop="city">
              <el-select
                v-model="form.city"
                placeholder="请选择或输入城市"
                filterable
                allow-create
                default-first-option
                clearable
                class="full-width"
              >
                <el-option v-for="item in cityOptions" :key="item" :label="item" :value="item" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :xs="24" :md="12">
            <el-form-item label="学历要求" prop="education">
              <el-select v-model="form.education" placeholder="请选择学历要求" class="full-width">
                <el-option v-for="item in educationOptions" :key="item" :label="item" :value="item" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :xs="24" :md="12">
            <el-form-item label="经验要求" prop="experience">
              <el-select v-model="form.experience" placeholder="请选择经验要求" class="full-width">
                <el-option v-for="item in experienceOptions" :key="item" :label="item" :value="item" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <div class="section-label">详细说明</div>

        <el-form-item label="岗位描述" prop="description">
          <el-input
            v-model="form.description"
            type="textarea"
            :rows="5"
            maxlength="2000"
            show-word-limit
            placeholder="岗位职责、工作内容、团队介绍等"
          />
        </el-form-item>
        <el-form-item label="任职要求" prop="requirements">
          <el-input
            v-model="form.requirements"
            type="textarea"
            :rows="5"
            maxlength="2000"
            show-word-limit
            placeholder="学历、经验、技能等硬性要求"
          />
        </el-form-item>

        <div class="section-label">技能要求</div>

        <div class="skills-wrap">
          <div v-for="(skill, index) in form.skills" :key="index" class="skill-row">
            <el-input v-model="skill.skillName" placeholder="技能名称，如 Vue3 / Java / Python" class="skill-name" maxlength="40" />
            <span class="skill-label">要求等级</span>
            <el-input-number v-model="skill.requiredLevel" :min="1" :max="5" :step="1" controls-position="right" class="skill-level" />
            <span class="skill-label">权重</span>
            <el-input-number v-model="skill.weight" :min="0" :max="10" :step="1" controls-position="right" class="skill-weight" />
            <el-button link type="danger" :disabled="form.skills.length <= 1" @click="form.skills.splice(index, 1)">
              <el-icon><Delete /></el-icon>
            </el-button>
          </div>

          <button type="button" class="add-skill" @click="form.skills.push(createSkillRow())">
            <el-icon style="margin-right: 5px"><CirclePlus /></el-icon>添加技能要求
          </button>
        </div>

        <div class="form-actions">
          <button type="button" class="grad-btn" :disabled="saving" @click="handleSubmit">
            <el-icon v-if="saving" class="is-loading" style="margin-right: 5px"><Loading /></el-icon>
            {{ saving ? '提交中…' : isEdit ? '保存修改' : '立即发布' }}
          </button>
          <el-button :disabled="saving" @click="goBack">取消</el-button>
        </div>
      </el-form>
    </div>
  </div>
</template>

<style scoped>
.edit-page {
  min-height: 100%;
}

.edit-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.edit-head__left {
  display: flex;
  align-items: center;
  gap: 14px;
}

.back-btn {
  flex: none;
}

.edit-head__title {
  font-size: 18px;
  font-weight: 700;
  color: var(--c-text);
}

.edit-head__sub {
  margin-top: 3px;
  font-size: 13px;
  color: var(--c-text-3);
}

.form-card {
  padding: 24px 26px 30px;
}

.section-label {
  position: relative;
  padding-left: 12px;
  margin: 6px 0 18px;
  font-size: 15px;
  font-weight: 600;
  color: var(--c-text);
}

.section-label::before {
  content: '';
  position: absolute;
  left: 0;
  top: 50%;
  transform: translateY(-50%);
  width: 4px;
  height: 16px;
  border-radius: 2px;
  background: linear-gradient(180deg, var(--c-primary), var(--c-accent));
}

.section-label + .el-form-item,
.section-label + .el-row {
  margin-top: 0;
}

.job-form {
  max-width: 1000px;
}

.full-width {
  width: 100%;
}

.skills-wrap {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-bottom: 24px;
}

.skill-row {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 14px;
  border: 1px dashed var(--c-border);
  border-radius: 10px;
  background: #fafbfe;
}

.skill-name {
  flex: 1;
  min-width: 200px;
}

.skill-label {
  flex: none;
  color: var(--c-text-3);
  font-size: 13px;
}

.skill-level,
.skill-weight {
  width: 118px;
}

.add-skill {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  height: 40px;
  border: 1px dashed #b7c4fb;
  border-radius: 10px;
  cursor: pointer;
  color: var(--c-primary);
  font-size: 14px;
  background: rgba(79, 107, 246, 0.05);
  transition: background-color 0.2s ease;
}

.add-skill:hover {
  background: rgba(79, 107, 246, 0.1);
}

.form-actions {
  display: flex;
  align-items: center;
  gap: 12px;
  padding-top: 10px;
  border-top: 1px solid #f1f2f8;
}

@media (max-width: 720px) {
  .skill-row {
    flex-wrap: wrap;
  }
}
</style>
