<script setup lang="ts">
import type { UploadRequestOptions } from 'element-plus'
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { ResumeParseVO, ResumeVO } from '@/types/api'
import { deleteResume, listResumes, parseResume, uploadResumeFile } from '@/api/resume'
import { formatDate } from '@/utils/format'
import { downloadResumeFile } from '@/utils/download'

defineOptions({ name: 'ResumeListView' })

const router = useRouter()

const resumes = ref<ResumeVO[]>([])
const loading = ref(true)
const busyId = ref<number | null>(null)

async function loadResumes(): Promise<void> {
  loading.value = true
  try {
    resumes.value = await listResumes()
  } catch {
    resumes.value = []
  } finally {
    loading.value = false
  }
}

/* ===== 上传附件 ===== */
async function handleUpload(resumeId: number, options: { file: File }): Promise<void> {
  const file = options.file
  if (busyId.value === resumeId) return
  const allowed = ['.pdf', '.doc', '.docx', '.txt']
  const ext = file.name.slice(file.name.lastIndexOf('.')).toLowerCase()
  if (!allowed.includes(ext)) {
    ElMessage.warning('仅支持 pdf / doc / docx / txt 格式')
    return
  }
  if (file.size > 20 * 1024 * 1024) {
    ElMessage.warning('附件大小不能超过 20MB')
    return
  }
  busyId.value = resumeId
  try {
    await uploadResumeFile(resumeId, file)
    ElMessage.success('附件上传成功，可点击「AI 解析」自动填充简历')
    await loadResumes()
  } catch {
    /* 提示由拦截器处理 */
  } finally {
    busyId.value = null
  }
}

function handleUploadRequest(resumeId: number, options: unknown): void {
  const file = (options as { file?: File }).file
  if (file) handleUpload(resumeId, { file })
}

/* ===== AI 解析 ===== */
const parseDialog = ref(false)
const parsing = ref(false)
const parseResult = ref<ResumeParseVO | null>(null)

async function runParse(resume: ResumeVO): Promise<void> {
  if (busyId.value) return
  busyId.value = resume.id
  parsing.value = true
  parseResult.value = null
  parseDialog.value = true
  try {
    parseResult.value = await parseResume(resume.id)
  } catch {
    parseDialog.value = false
  } finally {
    busyId.value = null
    parsing.value = false
  }
}

function goPerfect(): void {
  if (!parseResult.value) return
  const id = parseResult.value.resumeId
  parseDialog.value = false
  router.push({
    path: `/resume/edit/${id}`,
    state: { parsed: JSON.parse(JSON.stringify(parseResult.value)) },
  })
}

/* ===== 删除 ===== */
async function removeResume(resume: ResumeVO): Promise<void> {
  try {
    await ElMessageBox.confirm(
      `删除后无法恢复，确认删除简历「${resume.name || '未命名'}」吗？`,
      '删除简历',
      { type: 'warning', confirmButtonText: '确认删除', cancelButtonText: '取消' },
    )
  } catch {
    return
  }
  if (busyId.value) return
  busyId.value = resume.id
  try {
    await deleteResume(resume.id)
    ElMessage.success('简历已删除')
    await loadResumes()
  } catch {
    /* 提示由拦截器处理 */
  } finally {
    busyId.value = null
  }
}

function skillsOf(resume: ResumeVO): string[] {
  return resume.skills ?? []
}

onMounted(loadResumes)
</script>

<template>
  <div class="page resume-list">
    <div class="resume-list__inner">
      <div class="list-head">
        <div>
          <h1 class="list-head__title grad-text">我的简历</h1>
          <p class="list-head__desc">管理你的简历，AI 会基于它为你分析、匹配并优化</p>
        </div>
        <button class="grad-btn list-head__create" type="button" @click="router.push('/resume/edit')">
          <el-icon><Plus /></el-icon>新建简历
        </button>
      </div>

      <div v-loading="loading" class="resume-area">
        <el-empty
          v-if="!loading && resumes.length === 0"
          :image-size="140"
          description="还没有简历，创建第一份简历开始 AI 求职之旅"
        >
          <button class="grad-btn" type="button" @click="router.push('/resume/edit')">
            <el-icon><DocumentAdd /></el-icon>创建第一份简历
          </button>
        </el-empty>

        <div v-else class="resume-grid">
          <div v-for="resume in resumes" :key="resume.id" class="resume-card panel">
            <div class="resume-card__top">
              <el-progress
                type="dashboard"
                :width="92"
                :percentage="resume.completionScore"
                :color="resume.completionScore >= 80 ? '#22c55e' : resume.completionScore >= 60 ? '#4f6bf6' : '#f59e0b'"
              >
                <template #default="{ percentage }">
                  <div class="completion">
                    <span>{{ percentage }}<i>%</i></span>
                    <em>完成度</em>
                  </div>
                </template>
              </el-progress>

              <div class="resume-card__info">
                <div class="resume-card__name-line">
                  <h3>{{ resume.name || '未命名简历' }}</h3>
                  <el-tag v-if="resume.resumeFileUrl" size="small" type="success" effect="plain">
                    已传附件
                  </el-tag>
                </div>
                <div class="resume-card__meta">
                  <span v-if="resume.education">{{ resume.education }}</span>
                  <span v-if="resume.school">{{ resume.school }}</span>
                  <span v-if="resume.major">{{ resume.major }}</span>
                </div>
                <div class="resume-card__time">
                  更新于 {{ formatDate(resume.updateTime) }}
                </div>
              </div>
            </div>

            <div v-if="skillsOf(resume).length" class="resume-card__skills">
              <el-tag
                v-for="skill in skillsOf(resume)"
                :key="skill"
                size="small"
                type="primary"
                effect="plain"
              >
                {{ skill }}
              </el-tag>
            </div>
            <p v-if="resume.summary" class="resume-card__summary">{{ resume.summary }}</p>

            <div class="resume-card__actions">
              <el-button v-if="resume.resumeFileUrl" text @click="downloadResumeFile(resume.resumeFileUrl)">下载附件</el-button>
              <el-button text type="primary" @click="router.push(`/resume/edit/${resume.id}`)">
                <el-icon><EditPen /></el-icon>编辑
              </el-button>
              <el-upload
                :show-file-list="false"
                accept=".pdf,.doc,.docx,.txt"
                :http-request="(options: UploadRequestOptions) => handleUploadRequest(resume.id, options)"
              >
                <el-button text :loading="busyId === resume.id">
                  <el-icon><Upload /></el-icon>上传附件
                </el-button>
              </el-upload>
              <el-button text type="warning" :loading="parsing && parseDialog" @click="runParse(resume)">
                <el-icon><MagicStick /></el-icon>AI 解析
              </el-button>
              <el-button text type="success" @click="router.push(`/resume/analysis?resumeId=${resume.id}`)">
                <el-icon><TrendCharts /></el-icon>AI 分析
              </el-button>
              <el-button text type="danger" @click="removeResume(resume)">
                <el-icon><Delete /></el-icon>删除
              </el-button>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- AI 解析结果 -->
    <el-dialog v-model="parseDialog" title="AI 简历解析" width="640px" :close-on-click-modal="false">
      <div v-loading="parsing" class="parse-body">
        <template v-if="parseResult">
          <div class="parse-summary">
            <el-icon :size="18"><DocumentChecked /></el-icon>
            <span>文件「{{ parseResult.fileName }}」解析完成，识别出以下内容</span>
          </div>
          <el-alert title="解析结果尚未保存，确认后进入编辑页核对并保存。已有内容会保留。" type="info" :closable="false" />

          <div class="parse-fields">
            <div class="parse-field">
              <span>姓名</span><strong>{{ parseResult.name || '未识别' }}</strong>
            </div>
            <div class="parse-field">
              <span>手机</span><strong>{{ parseResult.phone || '未识别' }}</strong>
            </div>
            <div class="parse-field">
              <span>邮箱</span><strong>{{ parseResult.email || '未识别' }}</strong>
            </div>
            <div class="parse-field">
              <span>学历</span><strong>{{ parseResult.education || '未识别' }}</strong>
            </div>
            <div class="parse-field">
              <span>学校</span><strong>{{ parseResult.school || '未识别' }}</strong>
            </div>
            <div class="parse-field">
              <span>专业</span><strong>{{ parseResult.major || '未识别' }}</strong>
            </div>
          </div>

          <div v-if="parseResult.skills.length" class="parse-skills">
            <span>技能：</span>
            <el-tag v-for="skill in parseResult.skills" :key="skill" size="small" effect="plain">
              {{ skill }}
            </el-tag>
          </div>

          <div class="parse-text">
            <div class="parse-text__title">原文预览</div>
            <pre>{{ parseResult.text.slice(0, 2000) }}{{ parseResult.text.length > 2000 ? '……' : '' }}</pre>
          </div>
        </template>
      </div>
      <template #footer>
        <el-button @click="parseDialog = false">关闭</el-button>
        <el-button type="primary" :disabled="!parseResult || parsing" @click="goPerfect">确认并核对内容</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.resume-list__inner {
  max-width: 1200px;
  margin: 0 auto;
}

.list-head {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 22px;
}

.list-head__title {
  margin: 0;
  font-size: 26px;
  font-weight: 650;
}

.list-head__desc {
  margin: 8px 0 0;
  color: var(--c-text-2);
  font-size: 14px;
}

.list-head__create {
  height: 42px;
  padding: 0 22px;
  border-radius: 12px;
}

.resume-area {
  min-height: 300px;
}

.resume-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 18px;
}

.resume-card {
  padding: 24px;
  transition: box-shadow 0.2s ease;
}

.resume-card:hover {
  box-shadow: var(--shadow-md);
}

.resume-card__top {
  display: flex;
  align-items: center;
  gap: 20px;
}

.completion {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.completion span {
  font-size: 18px;
  font-weight: 650;
  color: var(--c-text);
  line-height: 1;
}

.completion span i {
  font-size: 11px;
  font-style: normal;
}

.completion em {
  margin-top: 6px;
  color: var(--c-text-3);
  font-size: 11px;
  font-style: normal;
}

.resume-card__info {
  flex: 1;
  min-width: 0;
}

.resume-card__name-line {
  display: flex;
  align-items: center;
  gap: 10px;
}

.resume-card__name-line h3 {
  margin: 0;
  overflow: hidden;
  font-size: 18px;
  font-weight: 700;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.resume-card__meta {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 10px;
  color: var(--c-text-2);
  font-size: 13px;
}

.resume-card__meta span + span::before {
  content: '·';
  margin-right: 8px;
  color: var(--c-text-3);
}

.resume-card__time {
  margin-top: 6px;
  color: var(--c-text-3);
  font-size: 12px;
}

.resume-card__skills {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: 18px;
}

.resume-card__summary {
  margin: 14px 0 0;
  color: var(--c-text-2);
  font-size: 13px;
  line-height: 1.8;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.resume-card__actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 2px;
  margin-top: 16px;
  padding-top: 12px;
  border-top: 1px dashed var(--c-border);
}

/* ===== 解析弹窗 ===== */
.parse-body {
  min-height: 200px;
}

.parse-summary {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 14px;
  border-radius: 10px;
  color: #1e40af;
  background: #eef2ff;
  border: 1px solid #c7d2fe;
}

.parse-summary span {
  font-size: 13px;
}

.parse-fields {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
  margin-top: 16px;
}

.parse-field {
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 10px 12px;
  border-radius: 10px;
  background: #f8fafc;
  border: 1px solid var(--c-border);
}

.parse-field span {
  color: var(--c-text-3);
  font-size: 12px;
}

.parse-field strong {
  font-size: 13px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.parse-skills {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 6px;
  margin-top: 14px;
}

.parse-skills > span {
  color: var(--c-text-2);
  font-size: 13px;
}

.parse-text {
  margin-top: 16px;
}

.parse-text__title {
  margin-bottom: 8px;
  color: var(--c-text-2);
  font-size: 13px;
  font-weight: 600;
}

.parse-text pre {
  max-height: 220px;
  margin: 0;
  padding: 12px;
  overflow: auto;
  border-radius: 10px;
  background: #f8fafc;
  border: 1px solid var(--c-border);
  color: #475569;
  font-size: 12px;
  line-height: 1.8;
  white-space: pre-wrap;
  word-break: break-all;
}

@media (max-width: 960px) {
  .resume-grid {
    grid-template-columns: 1fr;
  }
}
</style>
