<script setup lang="ts">
import { computed, nextTick, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import type {
  ExperienceForm,
  ExperienceType,
  ResumeForm,
  ResumeParseVO,
  ResumeVO,
} from '@/types/api'
import { createResume, getResume, updateResume } from '@/api/resume'
import { experienceTypeMap } from '@/utils/format'

defineOptions({ name: 'ResumeEditView' })

const route = useRoute()
const router = useRouter()

const isEdit = computed(() => Number.isFinite(Number(route.params.id)))
const resumeId = computed(() => (Number.isFinite(Number(route.params.id)) ? Number(route.params.id) : undefined))

const loading = ref(false)
const saving = ref(false)

const emptyForm = (): ResumeForm => ({
  name: '',
  phone: '',
  email: '',
  education: '',
  school: '',
  major: '',
  skills: [],
  summary: '',
  experiences: [],
})

const form = reactive<ResumeForm>(emptyForm())

const skillsModel = computed<string[]>({
  get: () => form.skills ?? [],
  set: (value: string[]) => {
    form.skills = value
  },
})

const educationOptions = ['高中及以下', '大专', '本科', '硕士', '博士']
const experienceTypeOptions = Object.entries(experienceTypeMap).map(([value, opt]) => ({
  value: value as ExperienceType,
  label: opt.label,
}))

function blankExperience(): ExperienceForm {
  return {
    type: 'WORK',
    title: '',
    company: '',
    startDate: '',
    endDate: '',
    content: '',
  }
}

function addExperience(): void {
  form.experiences = form.experiences ?? []
  form.experiences.push(blankExperience())
}

function removeExperience(index: number): void {
  form.experiences = form.experiences ?? []
  form.experiences.splice(index, 1)
}

function moveExperience(index: number, offset: number): void {
  const list = form.experiences ?? []
  const target = index + offset
  if (target < 0 || target >= list.length) return
  const temp = list[index]
  list[index] = list[target]
  list[target] = temp
}

function toExperienceForm(exp: ResumeVO['experiences'][number]): ExperienceForm {
  return {
    id: exp.id,
    type: exp.type,
    title: exp.title,
    company: exp.company ?? '',
    content: exp.content ?? '',
    startDate: exp.startDate ?? '',
    endDate: exp.endDate ?? '',
    sort: exp.sort,
  }
}

function applyResume(resume: ResumeVO): void {
  form.name = resume.name ?? ''
  form.phone = resume.phone ?? ''
  form.email = resume.email ?? ''
  form.education = resume.education ?? ''
  form.school = resume.school ?? ''
  form.major = resume.major ?? ''
  form.skills = [...(resume.skills ?? [])]
  form.summary = resume.summary ?? ''
  form.experiences = resume.experiences.map(toExperienceForm)
}

/** 从解析结果补齐空字段（不覆盖已有内容） */
function applyParsed(parsed: ResumeParseVO): void {
  if (!form.summary && parsed.summary) form.summary = parsed.summary
  if (!form.name && parsed.name) form.name = parsed.name
  if (!form.phone && parsed.phone) form.phone = parsed.phone
  if (!form.email && parsed.email) form.email = parsed.email
  if (!form.education && parsed.education) form.education = parsed.education
  if (!form.school && parsed.school) form.school = parsed.school
  if (!form.major && parsed.major) form.major = parsed.major
  const existSkills = new Set(form.skills ?? [])
  for (const skill of parsed.skills) {
    if (!existSkills.has(skill)) {
      form.skills = [...(form.skills ?? []), skill]
      existSkills.add(skill)
    }
  }
  if (parsed.experiences.length && (form.experiences?.length ?? 0) === 0) {
    form.experiences = parsed.experiences.map((exp) => ({
      type: exp.type,
      title: exp.title,
      company: exp.company ?? '',
      startDate: exp.startDate ?? '',
      endDate: exp.endDate ?? '',
      content: exp.content ?? '',
    }))
  }
}

async function loadResume(): Promise<void> {
  if (resumeId.value === undefined) return
  loading.value = true
  try {
    const resume = await getResume(resumeId.value)
    applyResume(resume)
  } catch {
    /* 提示由拦截器处理，保持空白表单 */
  } finally {
    loading.value = false
  }
}

/* ===== 校验与保存 ===== */
function cleanExperiences(): ExperienceForm[] {
  return (form.experiences ?? [])
    .filter((exp) => exp.title.trim())
    .map((exp, index) => ({
      ...exp,
      type: exp.type || 'WORK',
      title: exp.title.trim(),
      company: exp.company?.trim() || undefined,
      startDate: exp.startDate || undefined,
      endDate: exp.endDate || undefined,
      content: exp.content?.trim() || undefined,
      sort: index,
    }))
}

function validateBasics(): string | null {
  if (!form.name?.trim()) return '请填写简历名称（你的姓名）'
  if (form.phone?.trim() && !/^1[3-9]\d{9}$/.test(form.phone.trim())) {
    return '手机号格式不正确'
  }
  if (form.email?.trim() && !/^[\w.+-]+@[\w-]+(\.[\w-]+)+$/.test(form.email.trim())) {
    return '邮箱格式不正确'
  }
  return null
}

function buildPayload(): ResumeForm {
  return {
    name: form.name?.trim(),
    phone: form.phone?.trim() || '',
    email: form.email?.trim() || '',
    education: form.education || '',
    school: form.school?.trim() || '',
    major: form.major?.trim() || '',
    skills: (form.skills ?? []).map((s) => s.trim()).filter(Boolean),
    summary: form.summary?.trim() || '',
    experiences: cleanExperiences(),
  }
}

async function saveResume(): Promise<void> {
  if (saving.value) return
  const error = validateBasics()
  if (error) {
    ElMessage.warning(error)
    return
  }
  saving.value = true
  try {
    const payload = buildPayload()
    if (resumeId.value !== undefined) {
      await updateResume(resumeId.value, payload)
      ElMessage.success('简历已保存')
    } else {
      await createResume(payload)
      ElMessage.success('简历创建成功')
    }
    router.push('/resume')
  } catch {
    /* 提示由拦截器处理 */
  } finally {
    saving.value = false
  }
}

function resetForm(): void {
  const blank = emptyForm()
  form.name = blank.name
  form.phone = blank.phone
  form.email = blank.email
  form.education = blank.education
  form.school = blank.school
  form.major = blank.major
  form.skills = []
  form.summary = blank.summary
  form.experiences = []
  ElMessage.success('已清空表单')
}

function backToList(): void {
  router.push('/resume')
}

function consumeParsedResume(): ResumeParseVO | undefined {
  const st = history.state as { parsed?: ResumeParseVO } | null
  if (!st?.parsed) return undefined
  const parsed = st.parsed
  const next: Record<string, unknown> = { ...st }
  delete next.parsed
  history.replaceState(next, '')
  return parsed
}

onMounted(async () => {
  const parsed = consumeParsedResume()
  await loadResume()
  if (parsed) applyParsed(parsed)
  await nextTick()
  const section = String(route.query.section || '')
  if (['basic', 'skills', 'summary', 'experiences'].includes(section)) {
    document.getElementById(`resume-${section}`)?.scrollIntoView({ behavior: 'smooth', block: 'center' })
  }
})

watch(
  () => route.params.id,
  () => {
    const blank = emptyForm()
    form.name = blank.name
    form.phone = blank.phone
    form.email = blank.email
    form.education = blank.education
    form.school = blank.school
    form.major = blank.major
    form.skills = []
    form.summary = blank.summary
    form.experiences = []
    loadResume()
  },
)
</script>

<template>
  <div class="page resume-edit">
    <div class="resume-edit__inner">
      <div v-loading="loading" class="resume-edit__content">
        <div class="edit-head">
          <div>
            <h1 class="edit-head__title grad-text">
              {{ isEdit ? '编辑简历' : '新建简历' }}
            </h1>
            <p class="edit-head__desc">完善度越高，AI 匹配与优化效果越好</p>
          </div>
          <el-button text @click="backToList">
            <el-icon><ArrowLeft /></el-icon>&nbsp;返回我的简历
          </el-button>
        </div>

        <div id="resume-basic" class="panel edit-block">
          <h2 class="edit-block__title"><el-icon><User /></el-icon>基本信息</h2>
          <el-form label-width="90px" class="basic-form">
            <el-row :gutter="20">
              <el-col :span="12">
                <el-form-item label="姓名" required>
                  <el-input v-model="form.name" placeholder="请输入姓名" maxlength="30" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="学历">
                  <el-select v-model="form.education" placeholder="请选择学历" clearable style="width: 100%">
                    <el-option v-for="edu in educationOptions" :key="edu" :label="edu" :value="edu" />
                  </el-select>
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="手机号">
                  <el-input v-model="form.phone" placeholder="请输入手机号" maxlength="11" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="邮箱">
                  <el-input v-model="form.email" placeholder="请输入邮箱" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="学校">
                  <el-input v-model="form.school" placeholder="请输入毕业院校" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="专业">
                  <el-input v-model="form.major" placeholder="请输入所学专业" />
                </el-form-item>
              </el-col>
            </el-row>
          </el-form>
        </div>

        <div id="resume-skills" class="panel edit-block">
          <h2 class="edit-block__title"><el-icon><Coin /></el-icon>技能标签</h2>
          <el-select
            v-model="skillsModel"
            multiple
            filterable
            allow-create
            default-first-option
            placeholder="输入技能后回车创建，如：Vue3、Java、数据分析"
            style="width: 100%"
          />
        </div>

        <div id="resume-summary" class="panel edit-block">
          <h2 class="edit-block__title"><el-icon><Document /></el-icon>个人简介</h2>
          <el-input
            v-model="form.summary"
            type="textarea"
            :rows="4"
            maxlength="500"
            show-word-limit
            placeholder="用几句话介绍自己，突出优势与求职意向"
          />
        </div>

        <div id="resume-experiences" class="panel edit-block">
          <div class="exp-head">
            <h2 class="edit-block__title exp-head__title"><el-icon><Tickets /></el-icon>教育 / 工作 / 项目经历</h2>
            <el-button type="primary" plain @click="addExperience">
              <el-icon><Plus /></el-icon>添加经历
            </el-button>
          </div>

          <el-empty
            v-if="(form.experiences ?? []).length === 0"
            :image-size="80"
            description="还没有任何经历，点击右上角添加"
          />

          <div
            v-for="(exp, index) in form.experiences ?? []"
            :key="`${exp.type}-${index}`"
            class="exp-item"
          >
            <div class="exp-item__bar">
              <div class="exp-item__index">{{ index + 1 }}</div>
              <el-select v-model="exp.type" style="width: 130px">
                <el-option
                  v-for="opt in experienceTypeOptions"
                  :key="opt.value"
                  :label="opt.label"
                  :value="opt.value"
                />
              </el-select>
              <el-input v-model="exp.title" placeholder="职务 / 项目 / 学位名称" class="exp-title" />
              <div class="exp-item__ops">
                <el-button text :disabled="index === 0" @click="moveExperience(index, -1)">
                  <el-icon><Top /></el-icon>
                </el-button>
                <el-button
                  text
                  :disabled="index === (form.experiences ?? []).length - 1"
                  @click="moveExperience(index, 1)"
                >
                  <el-icon><Bottom /></el-icon>
                </el-button>
                <el-button text type="danger" @click="removeExperience(index)">
                  <el-icon><Delete /></el-icon>
                </el-button>
              </div>
            </div>

            <div class="exp-item__row">
              <el-input
                v-model="exp.company"
                :placeholder="exp.type === 'EDUCATION' ? '学校 / 机构' : '公司 / 单位'"
              />
              <el-date-picker
                v-model="exp.startDate"
                type="month"
                value-format="YYYY-MM"
                placeholder="开始时间"
                style="width: 150px"
              />
              <span class="exp-item__dash">至</span>
              <el-date-picker
                v-model="exp.endDate"
                type="month"
                value-format="YYYY-MM"
                placeholder="结束时间"
                style="width: 150px"
              />
            </div>
            <el-input
              v-model="exp.content"
              type="textarea"
              :rows="3"
              maxlength="1000"
              placeholder="描述具体职责、成果或项目亮点（建议量化）"
            />
          </div>
        </div>

        <div class="save-bar panel">
          <div class="save-bar__hint">
            <el-icon :size="16"><InfoFilled /></el-icon>
            建议同时填写经历与技能，有助于提升 AI 分析准确性
          </div>
          <div class="save-bar__ops">
            <el-button @click="resetForm">清空</el-button>
            <el-button @click="backToList">取消</el-button>
            <el-button type="primary" class="grad-save" :loading="saving" @click="saveResume">
              保存简历
            </el-button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.resume-edit__inner {
  max-width: 960px;
  margin: 0 auto;
  padding-bottom: 90px;
}

.edit-head {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 20px;
}

.edit-head__title {
  margin: 0;
  font-size: 26px;
  font-weight: 650;
}

.edit-head__desc {
  margin: 8px 0 0;
  color: var(--c-text-2);
  font-size: 14px;
}

.edit-block {
  padding: 24px;
  margin-bottom: 18px;
}

.edit-block__title {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 0 0 20px;
  padding-bottom: 14px;
  font-size: 16px;
  font-weight: 700;
  border-bottom: 1px solid var(--c-border);
}

.edit-block__title .el-icon {
  color: var(--c-primary);
}

.exp-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.exp-head__title {
  margin: 0;
  flex: 1;
}

.exp-item {
  padding: 18px;
  margin-bottom: 16px;
  border-radius: 12px;
  background: #fafbff;
  border: 1px solid #e5e9f8;
}

.exp-item__bar {
  display: flex;
  align-items: center;
  gap: 12px;
}

.exp-item__index {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex: none;
  width: 26px;
  height: 26px;
  border-radius: 50%;
  color: #fff;
  font-size: 13px;
  font-weight: 700;
  background: var(--c-primary);
}

.exp-title {
  flex: 1;
}

.exp-item__ops {
  display: flex;
  flex: none;
}

.exp-item__row {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-top: 14px;
}

.exp-item__row .el-input {
  flex: 1;
}

.exp-item__dash {
  color: var(--c-text-3);
  font-size: 13px;
}

.exp-item .el-textarea {
  margin-top: 12px;
}

/* ===== 底部悬浮操作条 ===== */
.save-bar {
  position: sticky;
  bottom: 16px;
  z-index: 20;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 14px 20px;
  box-shadow: var(--shadow-md);
}

.save-bar__hint {
  display: flex;
  align-items: center;
  gap: 6px;
  color: var(--c-text-3);
  font-size: 13px;
}

.save-bar__hint .el-icon {
  color: var(--c-primary);
}

.save-bar__ops {
  display: flex;
  gap: 8px;
  flex: none;
}

.grad-save {
  color: #fff;
  background: var(--c-primary);
  border: none;
  font-weight: 600;
}

@media (max-width: 900px) {
  .save-bar {
    flex-direction: column;
    align-items: stretch;
  }

  .save-bar__ops {
    justify-content: flex-end;
  }

  .exp-item__bar {
    flex-wrap: wrap;
  }

  .exp-title {
    flex: none;
    width: 100%;
  }
}
</style>
