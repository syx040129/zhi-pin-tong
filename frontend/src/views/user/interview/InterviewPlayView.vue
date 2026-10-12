<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import type { InterviewQuestionVO, InterviewVO } from '@/types/api'
import { interviewAnswer, interviewDetail, interviewFinish } from '@/api/ai'
import { interviewDifficultyMap, interviewStatusMap, tagOf } from '@/utils/format'

defineOptions({ name: 'InterviewPlayView' })

const route = useRoute()
const router = useRouter()

const interviewId = computed(() => Number(route.params.id))

const detail = ref<InterviewVO | null>(null)
const questions = ref<InterviewQuestionVO[]>([])
const loading = ref(true)
const submitting = ref(false)
const finishing = ref(false)
const failed = ref(false)

const currentIndex = ref(0)
const draft = ref('')

const answeredCount = computed(() => questions.value.filter((q) => q.answer?.trim()).length)
const isCompleted = computed(() => detail.value?.status === 'COMPLETED')
const current = computed(() => questions.value[currentIndex.value])
const isCurrentAnswered = computed(() => Boolean(current.value?.answer?.trim()))

async function loadDetail(): Promise<void> {
  if (!Number.isFinite(interviewId.value) || interviewId.value <= 0) {
    failed.value = true
    loading.value = false
    return
  }
  loading.value = true
  failed.value = false
  try {
    const data = await interviewDetail(interviewId.value)
    detail.value = data
    questions.value = (data.questions ?? []).map((q) => ({ ...q }))
    const firstUnanswered = questions.value.findIndex((q) => !q.answer?.trim())
    currentIndex.value = firstUnanswered >= 0 ? firstUnanswered : 0
    syncDraft()
  } catch {
    detail.value = null
    failed.value = true
  } finally {
    loading.value = false
  }
}

function syncDraft(): void {
  draft.value = current.value?.answer ?? ''
}

function prev(): void {
  if (currentIndex.value <= 0) return
  currentIndex.value -= 1
  syncDraft()
}

function next(): void {
  if (currentIndex.value >= questions.value.length - 1) return
  currentIndex.value += 1
  syncDraft()
}

function goTo(index: number): void {
  currentIndex.value = index
  syncDraft()
}

function goNextUnanswered(): boolean {
  const index = questions.value.findIndex(
    (q, i) => i > currentIndex.value && !q.answer?.trim(),
  )
  if (index >= 0) {
    goTo(index)
    return true
  }
  const first = questions.value.findIndex((q) => !q.answer?.trim())
  if (first >= 0) {
    goTo(first)
    return true
  }
  return false
}

async function submitAnswer(): Promise<void> {
  if (!detail.value || submitting.value) return
  const question = current.value
  if (!question || isCurrentAnswered.value) return
  if (!draft.value.trim()) {
    ElMessage.warning('请先写下你的回答再提交')
    return
  }
  submitting.value = true
  try {
    const updated = await interviewAnswer(interviewId.value, {
      questionId: question.id,
      answer: draft.value.trim(),
    })
    const index = questions.value.findIndex((q) => q.id === question.id)
    if (index >= 0) {
      questions.value[index] = { ...updated, answer: updated.answer ?? draft.value.trim() }
    }
    draft.value = ''
    ElMessage.success('回答已提交，AI 点评生成完毕')
    goNextUnanswered()
  } catch {
    /* 提示由拦截器处理 */
  } finally {
    submitting.value = false
  }
}

async function finishInterview(): Promise<void> {
  if (!detail.value || finishing.value) return
  if (answeredCount.value < questions.value.length) {
    ElMessage.warning(`还有 ${questions.value.length - answeredCount.value} 道题未作答，建议全部完成后结束`)
    return
  }
  finishing.value = true
  try {
    await interviewFinish(interviewId.value)
    ElMessage.success('面试已结束，正在生成报告…')
    router.replace(`/ai/interview/${interviewId.value}/report`)
  } catch {
    /* 提示由拦截器处理 */
  } finally {
    finishing.value = false
  }
}

function goReport(): void {
  router.push(`/ai/interview/${interviewId.value}/report`)
}

function scoreColor(score: number): string {
  if (score >= 80) return '#22c55e'
  if (score >= 60) return '#4f6bf6'
  return '#f59e0b'
}

onMounted(loadDetail)
</script>

<template>
  <div class="page play-page">
    <div class="play-page__inner">
      <div v-loading="loading" class="play-body">
        <el-result
          v-if="failed && !loading"
          icon="error"
          title="面试不存在或已结束"
          sub-title="请返回面试列表重新开始一场模拟面试"
        >
          <template #extra>
            <el-button type="primary" @click="router.push('/ai/interview')">返回面试列表</el-button>
          </template>
        </el-result>

        <template v-else-if="detail && questions.length > 0 && !loading">
          <!-- 顶部信息 -->
          <div class="panel meta-card">
            <div class="meta-card__info">
              <div class="meta-card__titles">
                <h1>{{ detail.title }}</h1>
                <el-tag size="small" :type="tagOf(interviewStatusMap, detail.status).type" effect="light">
                  {{ tagOf(interviewStatusMap, detail.status).label }}
                </el-tag>
              </div>
              <div class="meta-card__sub">
                <span v-if="detail.jobName">岗位：{{ detail.jobName }}</span>
                <el-tag size="small" :type="tagOf(interviewDifficultyMap, detail.difficulty).type" effect="plain">
                  难度：{{ tagOf(interviewDifficultyMap, detail.difficulty).label }}
                </el-tag>
                <span>共 {{ questions.length }} 题</span>
              </div>
            </div>

            <div class="meta-card__actions">
              <el-button v-if="isCompleted" type="primary" @click="goReport">
                <el-icon><Document /></el-icon>查看报告
              </el-button>
              <el-button v-else @click="router.push('/ai/interview')">退出面试</el-button>
            </div>
          </div>

          <!-- 进度 -->
          <div class="panel progress-card">
            <div class="progress-card__text">
              <span>答题进度</span>
              <strong>{{ answeredCount }} / {{ questions.length }}</strong>
            </div>
            <el-progress
              :percentage="Math.round((answeredCount / questions.length) * 100)"
              :stroke-width="10"
              :color="answeredCount === questions.length ? '#22c55e' : '#4f6bf6'"
            />
            <div class="question-dots">
              <button
                v-for="(q, index) in questions"
                :key="q.id"
                type="button"
                class="q-dot"
                :class="{
                  active: index === currentIndex,
                  done: q.answer?.trim(),
                }"
                :title="`第 ${index + 1} 题${q.answer?.trim() ? '（已作答）' : ''}`"
                @click="goTo(index)"
              >
                {{ index + 1 }}
              </button>
            </div>
          </div>

          <!-- 当前题目 -->
          <div class="panel question-card">
            <div class="question-card__head">
              <span class="question-card__no">第 {{ currentIndex + 1 }} 题</span>
              <el-tag v-if="current.questionType" size="small" type="primary" effect="plain">
                {{ current.questionType }}
              </el-tag>
              <el-tag size="small" type="info" effect="plain">
                难度：{{ current.difficulty }}
              </el-tag>
              <span v-if="isCurrentAnswered" class="answered-flag">
                <el-icon><CircleCheckFilled /></el-icon>已回答
              </span>
            </div>
            <p class="question-card__text">{{ current.question }}</p>

            <!-- 已作答：只读展示 -->
            <template v-if="isCurrentAnswered">
              <div class="answer-view">
                <div class="answer-view__label">我的回答</div>
                <p class="answer-view__text">{{ current.answer }}</p>
              </div>

              <div v-if="current.score != null" class="feedback-view">
                <div class="feedback-view__score">
                  <el-progress
                    type="dashboard"
                    :width="96"
                    :percentage="current.score"
                    :color="scoreColor(current.score)"
                  >
                    <template #default="{ percentage }">
                      <span class="feedback-view__num">{{ percentage }}<i>分</i></span>
                    </template>
                  </el-progress>
                  <div class="feedback-view__label">
                    <strong>{{ current.score >= 80 ? '答得很好' : current.score >= 60 ? '表现不错' : '还需加强' }}</strong>
                    <span>AI 评分</span>
                  </div>
                </div>
                <div class="feedback-view__content">
                  <div class="feedback-view__tip">AI 点评</div>
                  <p>{{ current.feedback || '暂无点评内容' }}</p>
                </div>
              </div>
            </template>

            <!-- 未作答：输入回答 -->
            <template v-else>
              <div class="answer-editor">
                <el-input
                  v-model="draft"
                  type="textarea"
                  :rows="7"
                  maxlength="2000"
                  show-word-limit
                  placeholder="组织好语言后作答，建议采用「观点 + 案例 + 总结」的结构…"
                />
              </div>
              <button
                class="grad-btn submit-btn"
                type="button"
                :disabled="submitting"
                @click="submitAnswer"
              >
                <el-icon><MagicStick /></el-icon>
                {{ submitting ? 'AI 正在点评…' : '提交本回答并获取 AI 点评' }}
              </button>
            </template>
          </div>

          <!-- 底部导航 -->
          <div class="nav-bar">
            <el-button :disabled="currentIndex === 0" @click="prev">
              <el-icon><ArrowLeft /></el-icon>上一题
            </el-button>
            <div class="nav-bar__center">
              <template v-if="isCompleted">
                <span class="nav-tip">本场面试已完成，报告已生成</span>
              </template>
              <template v-else-if="answeredCount === questions.length">
                <button class="grad-btn" type="button" :disabled="finishing" @click="finishInterview">
                  <el-icon><Finished /></el-icon>
                  {{ finishing ? '正在生成报告…' : '结束面试并生成报告' }}
                </button>
              </template>
              <span v-else class="nav-tip">共 {{ questions.length }} 题，完成全部题目后即可生成报告</span>
            </div>
            <el-button
              :disabled="currentIndex >= questions.length - 1"
              @click="next"
            >
              下一题<el-icon><ArrowRight /></el-icon>
            </el-button>
          </div>
        </template>
      </div>
    </div>
  </div>
</template>

<style scoped>
.play-page__inner {
  max-width: 860px;
  margin: 0 auto;
}

.play-body {
  min-height: 400px;
}

/* ===== 顶部信息 ===== */
.meta-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 20px 24px;
  margin-bottom: 16px;
}

.meta-card__titles {
  display: flex;
  align-items: center;
  gap: 10px;
}

.meta-card__titles h1 {
  margin: 0;
  font-size: 20px;
  font-weight: 650;
}

.meta-card__sub {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
  margin-top: 8px;
  color: var(--c-text-2);
  font-size: 13px;
}

.meta-card__actions {
  flex: none;
}

/* ===== 进度 ===== */
.progress-card {
  padding: 18px 24px;
  margin-bottom: 16px;
}

.progress-card__text {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
  color: var(--c-text-2);
  font-size: 13px;
}

.progress-card__text strong {
  color: var(--c-text);
  font-size: 15px;
}

.question-dots {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 14px;
}

.q-dot {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  border: 1px solid var(--c-border);
  border-radius: 8px;
  cursor: pointer;
  color: var(--c-text-2);
  font-size: 12px;
  background: #f8fafc;
  transition: border-color 0.2s ease, background-color 0.2s ease, box-shadow 0.2s ease;
}

.q-dot:hover {
  border-color: var(--c-primary);
}

.q-dot.done {
  color: #16a34a;
  border-color: #bbf7d0;
  background: #f0fdf4;
}

.q-dot.active {
  color: #fff;
  border-color: transparent;
  background: var(--c-primary);
}

/* ===== 题目 ===== */
.question-card {
  padding: 24px;
  margin-bottom: 16px;
}

.question-card__head {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.question-card__no {
  padding: 3px 12px;
  border-radius: 999px;
  color: var(--c-primary);
  font-size: 13px;
  font-weight: 700;
  background: #eef1fe;
}

.answered-flag {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  margin-left: auto;
  color: #16a34a;
  font-size: 13px;
  font-weight: 600;
}

.question-card__text {
  margin: 18px 0 0;
  color: var(--c-text);
  font-size: 17px;
  font-weight: 600;
  line-height: 1.9;
}

.answer-view {
  margin-top: 20px;
  padding: 16px;
  border-radius: 12px;
  background: #f8fafc;
  border: 1px solid var(--c-border);
}

.answer-view__label {
  color: var(--c-text-2);
  font-size: 13px;
  font-weight: 600;
}

.answer-view__text {
  margin: 10px 0 0;
  color: #374151;
  font-size: 14px;
  line-height: 1.9;
  white-space: pre-wrap;
  word-break: break-word;
}

.feedback-view {
  display: flex;
  align-items: center;
  gap: 20px;
  margin-top: 16px;
  padding: 18px;
  border-radius: 12px;
  background: var(--c-primary-soft);
  border: 1px solid #dbe3fd;
}

.feedback-view__score {
  display: flex;
  align-items: center;
  gap: 12px;
  flex: none;
}

.feedback-view__num {
  font-size: 20px;
  font-weight: 650;
  color: var(--c-text);
}

.feedback-view__num i {
  font-size: 11px;
  font-style: normal;
}

.feedback-view__label {
  display: flex;
  flex-direction: column;
}

.feedback-view__label strong {
  font-size: 15px;
}

.feedback-view__label span {
  color: var(--c-text-2);
  font-size: 12px;
}

.feedback-view__content {
  flex: 1;
  min-width: 0;
}

.feedback-view__tip {
  margin-bottom: 6px;
  color: var(--c-primary);
  font-size: 13px;
  font-weight: 700;
}

.feedback-view__content p {
  margin: 0;
  color: #374151;
  font-size: 13px;
  line-height: 1.9;
}

.answer-editor {
  margin-top: 18px;
}

.submit-btn {
  margin-top: 14px;
  height: 44px;
  width: 100%;
  border-radius: 12px;
  font-size: 15px;
}

/* ===== 底部导航 ===== */
.nav-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  padding: 4px 0 20px;
}

.nav-bar__center {
  display: flex;
  justify-content: center;
}

.nav-tip {
  color: var(--c-text-3);
  font-size: 13px;
}

@media (max-width: 700px) {
  .feedback-view {
    flex-direction: column;
    align-items: flex-start;
  }

  .nav-bar {
    flex-wrap: wrap;
  }
}
</style>
