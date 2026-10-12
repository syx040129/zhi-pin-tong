<script setup lang="ts">
import { computed, nextTick, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { chatAsk, chatClear, chatGreeting, chatHistory } from '@/api/ai'
import { useAuthStore } from '@/stores/auth'
import type { ChatMessageVO } from '@/types/api'

defineOptions({ name: 'AiChatWidget' })

const auth = useAuthStore()
const route = useRoute()
const router = useRouter()

/** 登录/注册页不展示悬浮助手，避免遮挡表单 */
const HIDDEN_ROUTES = ['/login', '/register']
const visible = computed(() => !HIDDEN_ROUTES.includes(route.path))

const opened = ref(false)
const loading = ref(false)
const mode = ref('MOCK')
const quickQuestions = ref<string[]>([])
const suggestionChips = ref<string[]>([])
const statusLine = ref('')
const input = ref('')
const listRef = ref<HTMLElement | null>(null)
const messages = ref<ChatMessageVO[]>([])
const state = reactive({
  sessionId: null as number | null,
  greeted: false,
  greeting: '',
  historyLoaded: false,
})

const canSend = computed(() => input.value.trim().length > 0 && !loading.value)
const displayName = computed(
  () => auth.user?.nickname || auth.user?.companyName || auth.user?.username || '我',
)
const avatarChar = computed(() => displayName.value.trim().charAt(0) || '我')

/** 岗位详情页打开助手时，把当前岗位作为上下文一并提问 */
const jobContextId = computed(() => {
  if (route.name === 'JobDetail') {
    const id = Number(route.params.id)
    return Number.isFinite(id) ? id : null
  }
  return null
})

function toggle(): void {
  opened.value = !opened.value
}

function close(): void {
  opened.value = false
}

/* ===== 数据加载 ===== */

async function ensureReady(): Promise<void> {
  if (!state.greeted) {
    await loadGreeting()
  }
  if (!state.historyLoaded && auth.isLoggedIn) {
    await loadHistory()
  }
}

async function loadGreeting(): Promise<void> {
  try {
    const data = await chatGreeting()
    state.greeted = true
    state.greeting = data.greeting
    quickQuestions.value = data.quickQuestions || []
    mode.value = data.mode
    statusLine.value = data.loggedIn ? '已开启对话记录' : '未登录 · 本次对话不会保存'
  } catch {
    state.greeted = true
    state.greeting = '你好，我是智聘AI 智能助手，可以问招聘、简历、面试相关的问题。'
    quickQuestions.value = ['AI 岗位匹配是怎么算分的？', '简历怎么优化更容易过筛？', '面试前应该准备什么？']
    statusLine.value = '助手服务连接失败，请稍后重试'
  }
}

async function loadHistory(): Promise<void> {
  try {
    const data = await chatHistory()
    state.historyLoaded = true
    state.sessionId = data.sessionId ?? null
    const history = data.messages || []
    if (history.length) {
      messages.value = history
      const last = history[history.length - 1]
      suggestionChips.value = last?.suggestions?.length ? last.suggestions : []
      statusLine.value = `已恢复最近对话 · ${history.length} 条消息`
    }
  } catch {
    state.historyLoaded = true
  }
}

/* ===== 问答 ===== */

async function send(text?: string): Promise<void> {
  const content = (text ?? input.value).trim()
  if (!content || loading.value) return
  input.value = ''
  suggestionChips.value = []
  messages.value.push({ role: 'USER', content })
  scrollToBottom()
  loading.value = true
  try {
    const data = await chatAsk({
      content,
      sessionId: state.sessionId,
      contextType: jobContextId.value ? 'JOB' : null,
      contextId: jobContextId.value,
    })
    state.sessionId = data.sessionId ?? state.sessionId
    messages.value.push({
      role: 'ASSISTANT',
      content: data.content,
      suggestions: data.suggestions,
      source: data.source,
    })
    suggestionChips.value = data.suggestions || []
    statusLine.value = data.persisted
      ? `对话已保存 · ${data.messageCount} 条消息`
      : '未登录 · 本次对话不会保存'
  } catch {
    messages.value.push({
      role: 'ASSISTANT',
      content: '抱歉，助手暂时无法回答，请检查网络后重试。',
      suggestions: [],
      source: 'RULE',
    })
  } finally {
    loading.value = false
    scrollToBottom()
  }
}

async function startNewChat(): Promise<void> {
  state.sessionId = null
  messages.value = []
  suggestionChips.value = []
  statusLine.value = auth.isLoggedIn ? '已开启新对话' : '未登录 · 本次对话不会保存'
  ElMessage.success('已开启新对话')
}

async function clearHistory(): Promise<void> {
  if (!auth.isLoggedIn) {
    await startNewChat()
    return
  }
  try {
    await ElMessageBox.confirm('将删除全部对话记录，且不可恢复，是否继续？', '清空对话', {
      type: 'warning',
      confirmButtonText: '清空',
      cancelButtonText: '取消',
    })
  } catch {
    return
  }
  try {
    await chatClear()
    await startNewChat()
    ElMessage.success('对话记录已清空')
  } catch {
    /* 请求层已提示错误 */
  }
}

function handleEnter(event: KeyboardEvent): void {
  if (event.shiftKey) return
  event.preventDefault()
  void send()
}

function scrollToBottom(): void {
  void nextTick(() => {
    const el = listRef.value
    if (el) {
      el.scrollTop = el.scrollHeight
    }
  })
}

/** 轻量文本渲染：仅支持 **加粗** 与换行，避免引入 Markdown 依赖与 XSS 风险 */
function renderHtml(content: string): string {
  const escaped = content
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
  return escaped.replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>').replace(/\n/g, '<br/>')
}

function scrollToTop(): void {
  listRef.value?.scrollTo({ top: 0, behavior: 'smooth' })
}

function goLogin(): void {
  void router.push({ path: '/login', query: { redirect: route.fullPath } })
}

watch(opened, (value) => {
  if (value) {
    void ensureReady()
  }
})

onMounted(() => {
  // 首次进入站点时静默预热欢迎语，点击气泡即可秒开
  void loadGreeting()
})
</script>

<template>
  <div v-if="visible" class="ai-chat">
    <!-- 悬浮气泡 -->
    <button
      class="ai-chat__launcher"
      :class="{ 'is-open': opened }"
      type="button"
      :aria-label="opened ? '收起 AI 智能助手' : '打开 AI 智能助手'"
      @click="toggle"
    >
      <el-icon class="ai-chat__launcher-icon">
        <Close v-if="opened" />
        <ChatDotRound v-else />
      </el-icon>
      <span v-if="!opened" class="ai-chat__launcher-badge">问</span>
    </button>

    <!-- 对话面板 -->
    <transition name="ai-chat-pop">
      <section v-if="opened" class="ai-chat__panel" aria-label="求职助手对话框" @keydown.esc="close">
        <header class="ai-chat__header">
          <div class="ai-chat__title">
            <span class="ai-chat__logo">智</span>
            <div>
              <div class="ai-chat__name">
                智能助手
                <span class="ai-chat__mode">{{ mode }}</span>
              </div>
              <div class="ai-chat__status">{{ statusLine }}</div>
            </div>
          </div>
          <div class="ai-chat__tools">
            <el-tooltip content="新对话" placement="bottom">
              <button class="ai-chat__tool" type="button" aria-label="新对话" @click="startNewChat">
                <el-icon><Plus /></el-icon>
              </button>
            </el-tooltip>
            <el-tooltip content="清空对话记录" placement="bottom">
              <button class="ai-chat__tool" type="button" aria-label="清空对话记录" @click="clearHistory">
                <el-icon><Delete /></el-icon>
              </button>
            </el-tooltip>
            <el-tooltip content="收起" placement="bottom">
              <button class="ai-chat__tool" type="button" aria-label="收起助手" @click="close">
                <el-icon><Minus /></el-icon>
              </button>
            </el-tooltip>
          </div>
        </header>

        <div ref="listRef" class="ai-chat__list" aria-live="polite" aria-relevant="additions text">
          <!-- 欢迎语 -->
          <div v-if="state.greeting" class="ai-chat__row">
            <span class="ai-chat__avatar ai-chat__avatar--ai">智</span>
            <div class="ai-chat__bubble ai-chat__bubble--ai" v-html="renderHtml(state.greeting)" />
          </div>

          <!-- 快捷提问（未产生对话时展示） -->
          <div v-if="!messages.length && quickQuestions.length" class="ai-chat__quick">
            <button
              v-for="item in quickQuestions"
              :key="item"
              class="ai-chat__chip"
              type="button"
              @click="send(item)"
            >
              {{ item }}
            </button>
          </div>

          <!-- 对话记录 -->
          <div
            v-for="(message, index) in messages"
            :key="message.id ?? index"
            class="ai-chat__row"
            :class="{ 'is-user': message.role === 'USER' }"
          >
            <span
              class="ai-chat__avatar"
              :class="message.role === 'USER' ? 'ai-chat__avatar--user' : 'ai-chat__avatar--ai'"
            >
              {{ message.role === 'USER' ? avatarChar : '智' }}
            </span>
            <div
              class="ai-chat__bubble"
              :class="message.role === 'USER' ? 'ai-chat__bubble--user' : 'ai-chat__bubble--ai'"
            >
              <div v-html="renderHtml(message.content)" />
              <span
                v-if="message.role === 'ASSISTANT' && message.source"
                class="ai-chat__source"
              >
                {{ message.source === 'LLM' ? 'AI 模型回答' : '规则引擎回答' }}
              </span>
            </div>
          </div>

          <!-- 思考中 -->
          <div v-if="loading" class="ai-chat__row">
            <span class="ai-chat__avatar ai-chat__avatar--ai">智</span>
            <div class="ai-chat__bubble ai-chat__bubble--ai ai-chat__typing">
              <i /><i /><i />
            </div>
          </div>

          <button v-if="messages.length" type="button" class="ai-chat__back-top" @click="scrollToTop">
            回到顶部
          </button>
        </div>

        <!-- 推荐追问（查看推荐追问） -->
        <div v-if="suggestionChips.length && !loading" class="ai-chat__suggest">
          <span class="ai-chat__suggest-label">猜你想问</span>
          <button
            v-for="item in suggestionChips"
            :key="item"
            class="ai-chat__chip ai-chat__chip--sub"
            type="button"
            @click="send(item)"
          >
            {{ item }}
          </button>
        </div>

        <footer class="ai-chat__footer">
          <el-input
            v-model="input" aria-label="向求职助手提问"
            type="textarea"
            :rows="2"
            resize="none"
            maxlength="500"
            placeholder="问我简历、岗位匹配、面试准备…（Enter 发送，Shift+Enter 换行）"
            @keydown.enter="handleEnter"
          />
          <div class="ai-chat__footer-bar">
            <span v-if="auth.isLoggedIn" class="ai-chat__hint">对话记录已保存</span>
            <button v-else class="ai-chat__login" type="button" @click="goLogin">登录保存记录</button>
            <button class="ai-chat__send" type="button" :disabled="!canSend" @click="send()">
              <el-icon><Promotion /></el-icon>
              发送
            </button>
          </div>
        </footer>
      </section>
    </transition>
  </div>
</template>

<style scoped>
.ai-chat {
  position: fixed;
  right: 24px;
  bottom: 24px;
  z-index: 2000;
}

/* ===== 悬浮气泡 ===== */
.ai-chat__launcher {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 56px;
  height: 56px;
  margin-left: auto;
  padding: 0;
  border: none;
  border-radius: 50%;
  cursor: pointer;
  color: #fff;
  font-size: 24px;
  background: var(--c-primary);
  box-shadow: 0 10px 24px rgba(79, 107, 246, 0.4);
  transition:
    transform 0.2s ease,
    box-shadow 0.2s ease;
  animation: ai-chat-float 3.6s ease-in-out infinite;
}

.ai-chat__launcher:hover {
  transform: translateY(-2px) scale(1.04);
  box-shadow: 0 14px 30px rgba(79, 107, 246, 0.48);
}

.ai-chat__launcher.is-open {
  animation: none;
  background: linear-gradient(135deg, #64748b, #475569);
  box-shadow: 0 8px 18px rgba(15, 23, 42, 0.28);
}

.ai-chat__launcher-icon {
  font-size: 24px;
}

.ai-chat__launcher-badge {
  position: absolute;
  top: -2px;
  right: -2px;
  padding: 1px 5px;
  border-radius: 8px;
  color: #fff;
  font-size: 10px;
  font-weight: 700;
  letter-spacing: 0.4px;
  background: #f97316;
  box-shadow: 0 2px 6px rgba(249, 115, 22, 0.4);
}

@keyframes ai-chat-float {
  0%,
  100% {
    transform: translateY(0);
  }
  50% {
    transform: translateY(-4px);
  }
}

/* ===== 面板 ===== */
.ai-chat__panel {
  display: flex;
  flex-direction: column;
  width: min(390px, calc(100vw - 32px));
  height: min(620px, calc(100vh - 116px));
  margin-bottom: 14px;
  overflow: hidden;
  background: #fff;
  border: 1px solid rgba(226, 232, 240, 0.9);
  border-radius: 18px;
  box-shadow: var(--shadow-lg);
}

.ai-chat-pop-enter-active,
.ai-chat-pop-leave-active {
  transition:
    opacity 0.2s ease,
    transform 0.24s cubic-bezier(0.34, 1.4, 0.64, 1);
}

.ai-chat-pop-enter-from,
.ai-chat-pop-leave-to {
  opacity: 0;
  transform: translateY(16px) scale(0.96);
}

.ai-chat__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 12px 14px;
  color: #fff;
  background: var(--c-primary);
}

.ai-chat__title {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
}

.ai-chat__logo {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex: none;
  width: 34px;
  height: 34px;
  border-radius: 10px;
  font-size: 17px;
  font-weight: 700;
  background: rgba(255, 255, 255, 0.22);
}

.ai-chat__name {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 15px;
  font-weight: 700;
}

.ai-chat__mode {
  padding: 1px 6px;
  border-radius: 6px;
  font-size: 10px;
  font-weight: 600;
  letter-spacing: 0.4px;
  background: rgba(255, 255, 255, 0.24);
}

.ai-chat__status {
  margin-top: 2px;
  font-size: 11px;
  opacity: 0.85;
}

.ai-chat__tools {
  display: flex;
  gap: 6px;
}

.ai-chat__tool {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  color: #fff;
  font-size: 14px;
  background: rgba(255, 255, 255, 0.18);
  transition: background-color 0.2s ease;
}

.ai-chat__tool:hover {
  background: rgba(255, 255, 255, 0.34);
}

/* ===== 消息列表 ===== */
.ai-chat__list {
  position: relative;
  flex: 1;
  overflow-y: auto;
  padding: 16px 14px 8px;
  background: var(--c-bg);
}

.ai-chat__row {
  display: flex;
  gap: 8px;
  margin-bottom: 14px;
}

.ai-chat__row.is-user {
  flex-direction: row-reverse;
}

.ai-chat__avatar {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex: none;
  width: 30px;
  height: 30px;
  border-radius: 10px;
  color: #fff;
  font-size: 13px;
  font-weight: 600;
}

.ai-chat__avatar--ai {
  background: var(--c-primary);
}

.ai-chat__avatar--user {
  background: #64748b;
}

.ai-chat__bubble {
  position: relative;
  max-width: 262px;
  padding: 10px 12px;
  border-radius: 12px;
  font-size: 13px;
  line-height: 1.65;
  word-break: break-word;
}

.ai-chat__bubble--ai {
  color: #1f2937;
  background: #f1f5f9;
  border-top-left-radius: 4px;
}

.ai-chat__bubble--user {
  color: #fff;
  background: var(--c-primary);
  border-top-right-radius: 4px;
}

.ai-chat__source {
  display: block;
  margin-top: 6px;
  color: var(--c-text-3);
  font-size: 10px;
}

.ai-chat__typing {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  height: 36px;
}

.ai-chat__typing i {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #94a3b8;
  animation: ai-chat-blink 1.2s infinite ease-in-out;
}

.ai-chat__typing i:nth-child(2) {
  animation-delay: 0.2s;
}

.ai-chat__typing i:nth-child(3) {
  animation-delay: 0.4s;
}

@keyframes ai-chat-blink {
  0%,
  80%,
  100% {
    opacity: 0.3;
    transform: translateY(0);
  }
  40% {
    opacity: 1;
    transform: translateY(-2px);
  }
}

.ai-chat__quick {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin: 2px 0 16px 38px;
}

.ai-chat__chip {
  padding: 6px 11px;
  border: 1px solid rgba(79, 107, 246, 0.32);
  border-radius: 999px;
  cursor: pointer;
  color: var(--c-primary);
  font-size: 12px;
  background: #fff;
  transition: all 0.18s ease;
}

.ai-chat__chip:hover {
  color: #fff;
  background: var(--c-primary);
  border-color: transparent;
}

.ai-chat__back-top {
  margin: 4px auto 8px;
  width: fit-content;
  padding: 2px 10px;
  border-radius: 999px;
  cursor: pointer;
  color: var(--c-text-2);
  font-size: 11px;
  background: rgba(148, 163, 184, 0.16);
}

/* ===== 推荐追问 ===== */
.ai-chat__suggest {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
  padding: 8px 14px;
  background: #fff;
  border-top: 1px dashed var(--c-border);
}

.ai-chat__suggest-label {
  color: var(--c-text-3);
  font-size: 11px;
}

.ai-chat__chip--sub {
  border-color: rgba(34, 211, 238, 0.5);
  color: #0891b2;
}

/* ===== 输入区 ===== */
.ai-chat__footer {
  padding: 10px 12px 12px;
  background: #fff;
  border-top: 1px solid var(--c-border);
}

.ai-chat__footer :deep(.el-textarea__inner) {
  border-radius: 10px;
  font-size: 13px;
  box-shadow: none;
  background: #f8fafc;
}

.ai-chat__footer-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin-top: 8px;
}

.ai-chat__hint {
  color: var(--c-text-3);
  font-size: 11px;
}

.ai-chat__login {
  border: none;
  padding: 0;
  cursor: pointer;
  color: var(--c-primary);
  font-size: 11px;
  background: transparent;
}

.ai-chat__send {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  height: 32px;
  padding: 0 16px;
  border: none;
  border-radius: 10px;
  cursor: pointer;
  color: #fff;
  font-size: 13px;
  font-weight: 600;
  background: var(--c-primary);
  transition:
    filter 0.2s ease,
    opacity 0.2s ease;
}

.ai-chat__send:hover:not(:disabled) {
  filter: brightness(1.08);
}

.ai-chat__send:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

@media (max-width: 600px) {
  .ai-chat {
    right: 14px;
    bottom: 14px;
  }

  .ai-chat__panel {
    height: min(560px, calc(100vh - 100px));
  }

  .ai-chat__bubble {
    max-width: 210px;
  }
}
</style>
