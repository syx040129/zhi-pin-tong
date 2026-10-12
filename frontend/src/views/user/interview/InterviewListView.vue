<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import type { InterviewVO, JobVO } from '@/types/api'
import { interviewList, interviewStart } from '@/api/ai'
import { pageJobs } from '@/api/job'
import {
  formatDateTime,
  interviewDifficultyMap,
  interviewStatusMap,
  tagOf,
} from '@/utils/format'

defineOptions({ name: 'InterviewListView' })

const router = useRouter()
const route = useRoute()

/* ===== 发起新面试 ===== */
const jobs = ref<JobVO[]>([])
const startForm = ref({
  jobId: undefined as number | undefined,
  difficulty: '中等' as '简单' | '中等' | '困难',
  questionCount: 5,
  title: '',
})
const starting = ref(false)

const questionCountOptions = Array.from({ length: 8 }, (_, i) => i + 3)

async function loadJobs(): Promise<void> {
  try {
    const page = await pageJobs({ page: 1, size: 100 })
    jobs.value = page.records
    const targetId = Number(route.query.jobId)
    if (jobs.value.some(job => job.id === targetId)) startForm.value.jobId = targetId
  } catch {
    jobs.value = []
  }
}

async function startInterview(): Promise<void> {
  if (starting.value) return
  starting.value = true
  try {
    const interview = await interviewStart({
      jobId: startForm.value.jobId && startForm.value.jobId !== 0 ? startForm.value.jobId : undefined,
      difficulty: startForm.value.difficulty,
      questionCount: startForm.value.questionCount,
      title: startForm.value.title.trim() || undefined,
    })
    router.push(`/ai/interview/${interview.id}`)
  } catch {
    /* 提示由拦截器处理 */
  } finally {
    starting.value = false
  }
}

/* ===== 历史记录 ===== */
const history = ref<InterviewVO[]>([])
const loadingHistory = ref(true)

async function loadHistory(): Promise<void> {
  loadingHistory.value = true
  try {
    history.value = await interviewList()
  } catch {
    history.value = []
  } finally {
    loadingHistory.value = false
  }
}

onMounted(async () => {
  await Promise.all([loadJobs(), loadHistory()])
})
</script>

<template>
  <div class="page interview-page">
    <div class="interview-page__inner">
      <div class="page-head">
        <div>
          <h1 class="page-head__title grad-text">AI 模拟面试</h1>
          <p class="page-head__desc">选择岗位方向，AI 考官实时提问并点评你的回答</p>
        </div>
      </div>

      <!-- 发起面试 -->
      <div class="start-card grad-bg">
        <div class="start-card__blob"></div>
        <div class="start-card__left">
          <h2>开始一场新的模拟面试</h2>
          <p>面试官将根据岗位要求生成题目，并对你的回答给出评分与建议</p>
        </div>
        <div class="start-card__form panel">
          <div class="form-grid">
            <div class="form-item">
              <label>目标岗位</label>
              <el-select v-model="startForm.jobId" placeholder="不限岗位" clearable filterable>
                <el-option label="不限岗位（综合题库）" :value="0" />
                <el-option
                  v-for="job in jobs"
                  :key="job.id"
                  :label="`${job.jobName} · ${job.companyName}`"
                  :value="job.id"
                />
              </el-select>
            </div>
            <div class="form-item">
              <label>难度</label>
              <el-radio-group v-model="startForm.difficulty">
                <el-radio-button value="简单">简单</el-radio-button>
                <el-radio-button value="中等">中等</el-radio-button>
                <el-radio-button value="困难">困难</el-radio-button>
              </el-radio-group>
            </div>
            <div class="form-item">
              <label>题目数量</label>
              <el-select v-model="startForm.questionCount" style="width: 130px">
                <el-option
                  v-for="count in questionCountOptions"
                  :key="count"
                  :label="`${count} 题`"
                  :value="count"
                />
              </el-select>
            </div>
            <div class="form-item">
              <label>面试标题（选填）</label>
              <el-input
                v-model="startForm.title"
                placeholder="如：前端一面热身"
                maxlength="40"
              />
            </div>
          </div>
          <div class="start-card__foot">
            <span class="start-card__tip">
              <el-icon><InfoFilled /></el-icon>预计耗时 {{ startForm.questionCount * 3 }} 分钟
            </span>
            <button class="grad-btn start-btn" type="button" :disabled="starting" @click="startInterview">
              <el-icon><VideoPlay /></el-icon>{{ starting ? '正在准备题目…' : '开始面试' }}
            </button>
          </div>
        </div>
      </div>

      <!-- 历史记录 -->
      <div class="history-head">
        <h2 class="history-head__title">面试记录</h2>
      </div>

      <div v-loading="loadingHistory" class="history-area">
        <el-empty
          v-if="!loadingHistory && history.length === 0"
          description="还没有面试记录，开始第一场模拟面试吧"
        />

        <div v-else class="history-list">
          <div v-for="item in history" :key="item.id" class="panel history-card">
            <div class="history-card__main">
              <div class="history-card__title">
                <span class="history-card__name">{{ item.title }}</span>
                <el-tag size="small" :type="tagOf(interviewStatusMap, item.status).type" effect="light">
                  {{ tagOf(interviewStatusMap, item.status).label }}
                </el-tag>
              </div>
              <div class="history-card__meta">
                <span v-if="item.jobName">岗位：{{ item.jobName }}</span>
                <span v-else>综合题库</span>
                <el-tag size="small" :type="tagOf(interviewDifficultyMap, item.difficulty).type" effect="plain">
                  {{ tagOf(interviewDifficultyMap, item.difficulty).label }}
                </el-tag>
                <span>共 {{ item.questionCount }} 题</span>
              </div>
            </div>
            <div class="history-card__side">
              <template v-if="item.status === 'COMPLETED'">
                <div class="history-card__score">
                  <strong>{{ item.totalScore ?? '—' }}</strong>
                  <span>总评分</span>
                </div>
                <el-button type="primary" @click="router.push(`/ai/interview/${item.id}/report`)">
                  查看报告
                </el-button>
              </template>
              <template v-else>
                <div class="history-card__score history-card__score--none">
                  <el-icon :size="26"><VideoPlay /></el-icon>
                </div>
                <el-button type="primary" @click="router.push(`/ai/interview/${item.id}`)">
                  继续答题
                </el-button>
              </template>
              <div class="history-card__time">{{ formatDateTime(item.createTime) }}</div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.interview-page__inner {
  max-width: 1000px;
  margin: 0 auto;
}

.page-head__title {
  margin: 0;
  font-size: 26px;
  font-weight: 650;
}

.page-head__desc {
  margin: 8px 0 18px;
  color: var(--c-text-2);
  font-size: 14px;
}

/* ===== 发起卡片 ===== */
.start-card {
  position: relative;
  display: flex;
  align-items: center;
  gap: 26px;
  overflow: hidden;
  padding: 26px 30px;
  border-radius: var(--radius);
  color: #fff;
}

.start-card__blob {
  position: absolute;
  top: -100px;
  right: 30%;
  width: 320px;
  height: 320px;
  border-radius: 50%;
  background: radial-gradient(closest-side, rgba(255, 255, 255, 0.18), transparent);
  pointer-events: none;
}

.start-card__left {
  position: relative;
  flex: none;
  max-width: 300px;
}

.start-card__left h2 {
  margin: 0;
  font-size: 21px;
  font-weight: 650;
}

.start-card__left p {
  margin: 10px 0 0;
  color: rgba(255, 255, 255, 0.85);
  font-size: 13px;
  line-height: 1.8;
}

.start-card__form {
  position: relative;
  flex: 1;
  color: var(--c-text);
  padding: 20px 22px;
}

.form-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 14px 20px;
}

.form-item {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.form-item label {
  color: var(--c-text-2);
  font-size: 12px;
}

.start-card__foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-top: 16px;
}

.start-card__tip {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  color: var(--c-text-3);
  font-size: 12px;
}

.start-btn {
  height: 40px;
  border-radius: 10px;
  padding: 0 22px;
}

/* ===== 历史记录 ===== */
.history-head {
  margin: 30px 0 14px;
}

.history-head__title {
  margin: 0;
  font-size: 19px;
  font-weight: 650;
}

.history-area {
  min-height: 220px;
}

.history-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.history-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18px;
  padding: 16px 22px;
}

.history-card__main {
  flex: 1;
  min-width: 0;
}

.history-card__title {
  display: flex;
  align-items: center;
  gap: 10px;
}

.history-card__name {
  overflow: hidden;
  font-size: 15px;
  font-weight: 700;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.history-card__meta {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
  margin-top: 8px;
  color: var(--c-text-2);
  font-size: 13px;
}

.history-card__side {
  display: flex;
  align-items: center;
  gap: 16px;
  flex: none;
}

.history-card__score {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.history-card__score strong {
  font-size: 22px;
  font-weight: 650;
  color: #16a34a;
  line-height: 1.1;
}

.history-card__score span {
  color: var(--c-text-3);
  font-size: 11px;
}

.history-card__score--none {
  color: #9ca3af;
}

.history-card__time {
  color: var(--c-text-3);
  font-size: 12px;
}

@media (max-width: 820px) {
  .start-card {
    flex-direction: column;
    align-items: stretch;
  }

  .start-card__left {
    max-width: none;
  }

  .form-grid {
    grid-template-columns: 1fr;
  }

  .history-card {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
