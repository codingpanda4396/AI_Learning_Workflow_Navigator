<script setup lang="ts">
import { computed, nextTick, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useSessionStore } from '@/stores/session'
import { useTutorStore } from '@/stores/tutor'
import ErrorMessage from '@/components/ErrorMessage.vue'

const route = useRoute()
const router = useRouter()
const sessionStore = useSessionStore()
const tutorStore = useTutorStore()

const taskId = computed(() => Number(route.params.id))
const loadError = computed(() => sessionStore.error)
const task = computed(() => sessionStore.currentTask)
const isLoading = computed(() => sessionStore.runningTask)
const sections = computed(() => task.value?.output.sections ?? [])
const generationReason = computed(() => task.value?.generationReason?.trim() ?? '')

const tutorInput = ref('')
const tutorMessagesContainerRef = ref<HTMLElement | null>(null)

const tutorMessages = computed(() => tutorStore.messages)
const tutorLoading = computed(() => tutorStore.loadingMessages)
const tutorSending = computed(() => tutorStore.sendingMessage)
const tutorLoadError = computed(() => tutorStore.loadError)
const tutorSendError = computed(() => tutorStore.sendError)
const canSendTutorMessage = computed(() => tutorInput.value.trim().length > 0 && !tutorSending.value)

function getStageLabel(stage: string) {
  const map: Record<string, string> = {
    STRUCTURE: '结构构建',
    UNDERSTANDING: '理解深化',
    TRAINING: '训练实战',
    REFLECTION: '复盘总结',
  }
  return map[stage] || stage
}

function getGenerationLabel(mode?: string) {
  const map: Record<string, string> = {
    LLM: 'LLM 生成',
    TEMPLATE_FALLBACK: '模板降级生成',
    RULE_FALLBACK: '规则降级生成',
    CACHED: '历史缓存结果',
  }
  return mode ? map[mode] || mode : '未知来源'
}

function resolveSessionId() {
  const fromQuery = Number(route.query.sessionId)
  if (Number.isFinite(fromQuery) && fromQuery > 0) {
    return fromQuery
  }
  return sessionStore.currentTaskSessionId || sessionStore.sessionId || null
}

async function scrollTutorToBottom() {
  await nextTick()
  if (tutorMessagesContainerRef.value) {
    tutorMessagesContainerRef.value.scrollTop = tutorMessagesContainerRef.value.scrollHeight
  }
}

async function loadTask() {
  await sessionStore.runTask(taskId.value)
}

async function loadTutorMessages() {
  const targetSessionId = resolveSessionId()
  if (!targetSessionId) {
    return
  }
  await tutorStore.load(targetSessionId, taskId.value)
  await scrollTutorToBottom()
}

async function handleRetry() {
  await loadTask()
  await loadTutorMessages()
}

async function handleSendTutorMessage() {
  const content = tutorInput.value.trim()
  const targetSessionId = resolveSessionId()
  if (!content || !targetSessionId || tutorSending.value) {
    return
  }

  try {
    await tutorStore.send(targetSessionId, taskId.value, content)
    tutorInput.value = ''
    await scrollTutorToBottom()
  } catch (error) {
    console.error('发送 Tutor 消息失败:', error)
  }
}

async function handleRetryTutorSend() {
  try {
    await tutorStore.retryLastSend()
    tutorInput.value = ''
    await scrollTutorToBottom()
  } catch (error) {
    console.error('重试 Tutor 消息失败:', error)
  }
}

function formatMessageTime(input: string) {
  const date = new Date(input)
  if (Number.isNaN(date.getTime())) {
    return ''
  }
  return date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
}

function handleContinue() {
  const targetSessionId = resolveSessionId()
  if (!targetSessionId) {
    router.push('/')
    return
  }
  const step = Number(route.query.step)
  const resolvedStep = Number.isFinite(step) && step >= 1 && step <= 4 ? String(step) : '3'
  router.push({
    path: `/session/${targetSessionId}`,
    query: { step: resolvedStep },
  })
}

onMounted(async () => {
  sessionStore.resetTaskState()
  tutorStore.reset()
  await loadTask()
  await loadTutorMessages()
})

watch(
  () => tutorMessages.value.length,
  async () => {
    await scrollTutorToBottom()
  },
)
</script>

<template>
  <div class="task-run-page">
    <header class="header">
      <button class="back-btn" @click="router.back()">← 返回</button>
      <h1 class="task-title">任务详情</h1>
    </header>

    <div v-if="isLoading" class="loading">加载中...</div>
    <ErrorMessage v-else-if="loadError && !task" :message="loadError" @retry="handleRetry" />

    <div v-else-if="task" class="task-content">
      <div class="task-meta">
        <span class="task-stage">{{ getStageLabel(task.stage) }}</span>
        <span class="task-id">任务 #{{ task.taskId }}</span>
        <span class="task-source">来源：{{ getGenerationLabel(task.generationMode) }}</span>
      </div>
      <p v-if="generationReason" class="generation-reason">原因：{{ generationReason }}</p>

      <div v-if="sections.length > 0" class="output-sections">
        <div v-for="section in sections" :key="section.title" class="output-section">
          <h3 class="section-title">{{ section.title }}</h3>

          <ul v-if="section.bullets?.length" class="bullet-list">
            <li v-for="(bullet, idx) in section.bullets" :key="idx">{{ bullet }}</li>
          </ul>

          <ol v-else-if="section.steps?.length" class="step-list">
            <li v-for="(step, idx) in section.steps" :key="idx">{{ step }}</li>
          </ol>

          <ul v-else-if="section.items?.length" class="bullet-list">
            <li v-for="(item, idx) in section.items" :key="idx">{{ item }}</li>
          </ul>

          <p v-else-if="section.text" class="summary-text">{{ section.text }}</p>
          <p v-else class="summary-text">暂无可展示内容。</p>
        </div>
      </div>

      <div v-else class="summary-text">当前任务输出为空，请返回会话页重试。</div>

      <section class="tutor-panel">
        <div class="tutor-header">
          <h3 class="tutor-title">AI Tutor</h3>
          <span class="tutor-hint">围绕当前任务提问，获得即时讲解</span>
        </div>

        <div v-if="tutorLoading" class="tutor-loading">正在加载对话...</div>
        <div v-else-if="tutorLoadError" class="tutor-error-row">
          <span class="tutor-error">{{ tutorLoadError }}</span>
          <button class="tutor-link-btn" @click="loadTutorMessages">重试加载</button>
        </div>
        <div v-else-if="!tutorStore.hasMessages" class="tutor-empty">
          还没有对话记录。你可以先问：“这道题的关键思路是什么？”
        </div>
        <div v-else ref="tutorMessagesContainerRef" class="tutor-messages">
          <div
            v-for="message in tutorMessages"
            :key="message.id"
            class="message-row"
            :class="message.role === 'user' ? 'message-user' : 'message-assistant'"
          >
            <div class="message-bubble">
              <p class="message-text">{{ message.content }}</p>
              <span class="message-time">{{ formatMessageTime(message.createdAt) }}</span>
            </div>
          </div>
        </div>

        <div v-if="tutorSending" class="tutor-thinking">AI 正在思考...</div>

        <form class="tutor-input-row" @submit.prevent="handleSendTutorMessage">
          <textarea
            v-model="tutorInput"
            class="tutor-input"
            :disabled="tutorSending"
            rows="2"
            placeholder="输入你的问题，例如：我不理解这一步为什么这样变形？"
          ></textarea>
          <button type="submit" class="tutor-send-btn" :disabled="!canSendTutorMessage">
            {{ tutorSending ? '发送中...' : '发送' }}
          </button>
        </form>

        <div v-if="tutorSendError" class="tutor-error-row">
          <span class="tutor-error">{{ tutorSendError }}</span>
          <button class="tutor-link-btn" @click="handleRetryTutorSend">重试发送</button>
        </div>
      </section>

      <div class="actions">
        <button class="continue-btn" @click="handleContinue">返回会话</button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.task-run-page { min-height: 100vh; padding: 1.5rem; }
.header { display: flex; align-items: center; gap: 1rem; margin-bottom: 2rem; }
.back-btn { padding: 0.5rem 1rem; border: 1px solid var(--color-border); border-radius: 6px; background: transparent; color: var(--color-text-secondary); }
.task-title { font-size: 1.5rem; font-weight: 600; color: var(--color-text); }
.loading { text-align: center; padding: 3rem; color: var(--color-text-secondary); }
.task-content { max-width: 720px; margin: 0 auto; }
.task-meta { display: flex; align-items: center; gap: 1rem; margin-bottom: 2rem; flex-wrap: wrap; }
.task-stage { padding: 0.25rem 0.75rem; font-size: 0.75rem; font-weight: 600; color: var(--color-primary); background: var(--color-primary-alpha); border-radius: 4px; }
.task-id { font-size: 0.875rem; color: var(--color-text-secondary); }
.task-source { font-size: 0.875rem; color: var(--color-text-secondary); }
.generation-reason { margin: -1rem 0 1.5rem; color: var(--color-text-secondary); font-size: 0.875rem; }
.output-sections { display: flex; flex-direction: column; gap: 2rem; margin-bottom: 2rem; }
.output-section { background: var(--color-bg-elevated); border-radius: 12px; padding: 1.5rem; }
.section-title { font-size: 1rem; font-weight: 600; color: var(--color-text); margin-bottom: 1rem; }
.bullet-list, .step-list { padding-left: 1.25rem; margin: 0; }
.bullet-list li, .step-list li { line-height: 1.7; color: var(--color-text); margin-bottom: 0.5rem; }
.summary-text { line-height: 1.7; color: var(--color-text); padding: 1rem; background: var(--color-bg); border-radius: 8px; border-left: 3px solid var(--color-primary); }

.tutor-panel {
  margin-top: 2rem;
  padding: 1rem;
  border: 1px solid var(--color-border);
  border-radius: 12px;
  background: linear-gradient(180deg, rgba(17, 28, 52, 0.9), rgba(13, 21, 40, 0.9));
}
.tutor-header { display: flex; align-items: baseline; justify-content: space-between; gap: 1rem; margin-bottom: 0.75rem; flex-wrap: wrap; }
.tutor-title { font-size: 1rem; color: var(--color-text); }
.tutor-hint { font-size: 0.8125rem; color: var(--color-text-secondary); }
.tutor-loading, .tutor-empty {
  padding: 1rem;
  border-radius: 8px;
  background: var(--color-bg);
  color: var(--color-text-secondary);
  margin-bottom: 0.75rem;
}
.tutor-messages {
  max-height: 320px;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 0.625rem;
  padding: 0.5rem 0.125rem;
  margin-bottom: 0.75rem;
}
.message-row { display: flex; }
.message-user { justify-content: flex-end; }
.message-assistant { justify-content: flex-start; }
.message-bubble {
  max-width: 88%;
  border: 1px solid var(--color-border);
  border-radius: 10px;
  padding: 0.625rem 0.75rem;
  background: var(--color-bg-surface);
}
.message-user .message-bubble {
  background: rgba(62, 140, 255, 0.15);
  border-color: rgba(62, 140, 255, 0.35);
}
.message-text { white-space: pre-wrap; line-height: 1.6; color: var(--color-text); }
.message-time {
  display: block;
  margin-top: 0.375rem;
  font-size: 0.75rem;
  color: var(--color-text-secondary);
}
.tutor-thinking { margin-bottom: 0.5rem; color: var(--color-primary); font-size: 0.875rem; }
.tutor-input-row { display: flex; gap: 0.625rem; align-items: flex-end; }
.tutor-input {
  width: 100%;
  resize: vertical;
  border: 1px solid var(--color-border);
  border-radius: 8px;
  padding: 0.75rem;
  background: var(--color-bg);
  color: var(--color-text);
}
.tutor-send-btn {
  flex-shrink: 0;
  height: 40px;
  padding: 0 1rem;
  border-radius: 8px;
  background: var(--color-primary);
  color: #fff;
  font-weight: 600;
}
.tutor-send-btn:disabled { opacity: 0.6; cursor: not-allowed; }
.tutor-error-row { display: flex; align-items: center; gap: 0.75rem; margin-bottom: 0.5rem; margin-top: 0.25rem; }
.tutor-error { color: var(--color-error); font-size: 0.875rem; }
.tutor-link-btn { color: var(--color-primary); text-decoration: underline; font-size: 0.875rem; }

.actions { text-align: center; margin-top: 1.5rem; }
.continue-btn { padding: 1rem 2rem; font-weight: 600; color: #fff; background: var(--color-primary); border: none; border-radius: 8px; }

@media (max-width: 768px) {
  .task-run-page { padding: 1rem; }
  .tutor-input-row { flex-direction: column; align-items: stretch; }
  .tutor-send-btn { width: 100%; }
}
</style>
