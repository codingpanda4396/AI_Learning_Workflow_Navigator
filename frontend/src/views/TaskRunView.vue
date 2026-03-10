<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useSessionStore } from '@/stores/session'
import { useTutorStore } from '@/stores/tutor'
import { usePracticeStore } from '@/stores/practice'
import ErrorMessage from '@/components/ErrorMessage.vue'

const route = useRoute()
const router = useRouter()
const sessionStore = useSessionStore()
const tutorStore = useTutorStore()
const practiceStore = usePracticeStore()

const taskId = computed(() => Number(route.params.id))
const task = computed(() => sessionStore.currentTask)
const loadError = computed(() => sessionStore.error)
const isLoading = computed(() => sessionStore.runningTask)
const sections = computed(() => task.value?.output.sections ?? [])
const generationReason = computed(() => task.value?.generationReason?.trim() ?? '')
const isTrainingTask = computed(() => task.value?.stage === 'TRAINING')
const nonTrainingTip = computed(() => {
  if (!task.value || isTrainingTask.value) {
    return ''
  }
  return `当前阶段是 ${getStageLabel(task.value.stage)}，练习测验只在 TRAINING 阶段开放。`
})

const tutorInput = ref('')
const tutorMessagesContainerRef = ref<HTMLElement | null>(null)
const answerDrafts = ref<Record<number, string>>({})
const submittingPracticeItemId = ref<number | null>(null)
const quizPollTimer = ref<number | null>(null)

const tutorMessages = computed(() => tutorStore.messages)
const tutorLoading = computed(() => tutorStore.loadingMessages)
const tutorSending = computed(() => tutorStore.sendingMessage)
const tutorLoadError = computed(() => tutorStore.loadError)
const tutorSendError = computed(() => tutorStore.sendError)
const canSendTutorMessage = computed(() => tutorInput.value.trim().length > 0 && !tutorSending.value)

const practiceQuiz = computed(() => practiceStore.quiz)
const practiceItems = computed(() => practiceStore.items)
const practiceSubmissions = computed(() => practiceStore.submissions)
const practiceFeedbackReport = computed(() => practiceStore.feedbackReport)
const practiceLoading = computed(
  () =>
    practiceStore.loadingItems ||
    practiceStore.loadingSubmissions ||
    practiceStore.requestingQuiz ||
    practiceStore.pollingQuiz ||
    practiceStore.loadingFeedback,
)
const practiceGenerating = computed(() => practiceStore.requestingQuiz || practiceStore.pollingQuiz)
const practiceSubmitting = computed(() => practiceStore.submittingAnswer)
const practiceApplyingAction = computed(() => practiceStore.applyingFeedbackAction)
const practiceError = computed(() => practiceStore.itemsError || practiceStore.submissionsError)
const practiceSubmitError = computed(() => practiceStore.submitError)
const quizStatus = computed(() => practiceQuiz.value?.status ?? '')

const learningFeedback = computed(() => sessionStore.learningFeedback)
const feedbackLoading = computed(() => sessionStore.fetchingLearningFeedback)

const latestSubmissionByItem = computed(() => {
  const ordered = [...practiceSubmissions.value].sort(
    (a, b) => new Date(b.submittedAt).getTime() - new Date(a.submittedAt).getTime(),
  )
  const map = new Map<number, (typeof ordered)[number]>()
  for (const item of ordered) {
    if (!map.has(item.practiceItemId)) {
      map.set(item.practiceItemId, item)
    }
  }
  return map
})

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
    TEMPLATE_FALLBACK: '模板降级',
    RULE_FALLBACK: '规则降级',
    CACHED: '缓存结果',
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

function normalizeOptionLine(option: unknown): string {
  if (typeof option === 'string') {
    return option
  }
  if (typeof option === 'number' || typeof option === 'boolean') {
    return String(option)
  }
  if (option && typeof option === 'object') {
    const record = option as Record<string, unknown>
    const label = typeof record.label === 'string' ? record.label : ''
    const value = typeof record.value === 'string' ? record.value : ''
    if (label && value) {
      return `${label}: ${value}`
    }
    if (label) {
      return label
    }
    if (value) {
      return value
    }
  }
  return ''
}

function resolveOptionLines(options: unknown): string[] {
  if (!Array.isArray(options)) {
    return []
  }
  return options.map(normalizeOptionLine).filter((line) => line.trim().length > 0)
}

function toPercent(value: number) {
  const normalized = value <= 1 ? value * 100 : value
  return Math.round(normalized)
}

function getItemDraft(itemId: number) {
  return answerDrafts.value[itemId] ?? ''
}

function setItemDraft(itemId: number, value: string) {
  answerDrafts.value = {
    ...answerDrafts.value,
    [itemId]: value,
  }
}

function canSubmitPractice(itemId: number) {
  const answer = answerDrafts.value[itemId]?.trim() ?? ''
  return answer.length > 0 && !practiceSubmitting.value
}

function getLatestSubmission(itemId: number) {
  return latestSubmissionByItem.value.get(itemId) ?? null
}

function clearQuizPoll() {
  if (quizPollTimer.value !== null) {
    window.clearTimeout(quizPollTimer.value)
    quizPollTimer.value = null
  }
}

async function refreshQuizRelated(sessionId: number) {
  await practiceStore.loadQuiz(sessionId, taskId.value)
  if (practiceStore.quiz?.status !== 'GENERATING') {
    await practiceStore.loadSubmissions(sessionId, taskId.value)
    if (
      practiceStore.quiz?.status === 'FEEDBACK_READY' ||
      practiceStore.quiz?.status === 'REVIEWING' ||
      practiceStore.quiz?.status === 'NEXT_ROUND'
    ) {
      await practiceStore.loadFeedbackReport(sessionId, taskId.value)
    }
    await sessionStore.fetchLearningFeedback(sessionId)
  }
}

function scheduleQuizPoll() {
  clearQuizPoll()
  const sessionId = resolveSessionId()
  if (!sessionId) {
    return
  }
  quizPollTimer.value = window.setTimeout(async () => {
    try {
      await refreshQuizRelated(sessionId)
      if (practiceStore.quiz?.status === 'GENERATING') {
        scheduleQuizPoll()
      }
    } catch (error) {
      console.error('轮询测验状态失败:', error)
    }
  }, 1200)
}

async function loadTrainingClosure(generate = false) {
  if (!isTrainingTask.value) {
    return
  }
  const sessionId = resolveSessionId()
  if (!sessionId) {
    return
  }
  try {
    if (generate) {
      await practiceStore.requestQuiz(sessionId, taskId.value)
    } else {
      await practiceStore.loadQuiz(sessionId, taskId.value)
    }
  } catch {
    return
  }

  if (practiceStore.quiz?.status === 'GENERATING') {
    scheduleQuizPoll()
    return
  }

  await refreshQuizRelated(sessionId)
}

async function loadTutorMessages() {
  const sessionId = resolveSessionId()
  if (!sessionId) {
    return
  }
  await tutorStore.load(sessionId, taskId.value)
  await scrollTutorToBottom()
}

async function handleRetry() {
  await loadTask()
  await loadTrainingClosure()
  await loadTutorMessages()
}

async function handleRetryPractice() {
  await loadTrainingClosure()
}

async function handleGeneratePractice() {
  await loadTrainingClosure(true)
}

async function handleSubmitPractice(itemId: number) {
  const answer = answerDrafts.value[itemId]?.trim() ?? ''
  const sessionId = resolveSessionId()
  if (!sessionId || !answer || practiceSubmitting.value) {
    return
  }
  submittingPracticeItemId.value = itemId
  try {
    await practiceStore.submitAnswer(sessionId, taskId.value, itemId, answer)
    await refreshQuizRelated(sessionId)
  } catch (error) {
    console.error('提交测验答案失败:', error)
  } finally {
    submittingPracticeItemId.value = null
  }
}

async function handleFeedbackAction(action: 'REVIEW' | 'NEXT_ROUND') {
  const sessionId = resolveSessionId()
  if (!sessionId || practiceApplyingAction.value) {
    return
  }
  try {
    await practiceStore.applyFeedback(sessionId, taskId.value, action)
    await sessionStore.fetchLearningFeedback(sessionId)
  } catch (error) {
    console.error('应用反馈动作失败:', error)
  }
}

async function handleSendTutorMessage() {
  const content = tutorInput.value.trim()
  const sessionId = resolveSessionId()
  if (!content || !sessionId || tutorSending.value) {
    return
  }

  try {
    try {
      await tutorStore.sendStream(sessionId, taskId.value, content)
    } catch {
      await tutorStore.send(sessionId, taskId.value, content)
    }
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
  const sessionId = resolveSessionId()
  if (!sessionId) {
    router.push('/')
    return
  }
  const step = Number(route.query.step)
  const resolvedStep = Number.isFinite(step) && step >= 1 && step <= 4 ? String(step) : '3'
  router.push({
    path: `/session/${sessionId}`,
    query: { step: resolvedStep },
  })
}

onMounted(async () => {
  sessionStore.resetTaskState()
  tutorStore.reset()
  practiceStore.reset()
  await loadTask()
  await Promise.all([loadTrainingClosure(), loadTutorMessages()])
})

onBeforeUnmount(() => {
  clearQuizPoll()
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

      <section class="training-panel">
        <div class="training-header">
          <h3 class="training-title">训练闭环</h3>
          <span v-if="isTrainingTask" class="training-hint">Tutor 学习 -> 异步出题 -> 用户答题 -> 反馈报告</span>
          <span v-else class="training-hint">仅 TRAINING 阶段开放</span>
        </div>

        <div v-if="!isTrainingTask" class="training-note">{{ nonTrainingTip }}</div>

        <template v-else>
          <div class="training-actions">
            <button class="training-btn" :disabled="practiceLoading || practiceGenerating" @click="handleRetryPractice">
              {{ practiceLoading ? '加载中...' : '刷新测验' }}
            </button>
            <button class="training-btn primary" :disabled="practiceGenerating" @click="handleGeneratePractice">
              {{ practiceGenerating ? '生成中...' : '异步生成测验' }}
            </button>
          </div>

          <div v-if="practiceQuiz" class="training-note">
            测验状态 {{ practiceQuiz.status }} ｜ 已答 {{ practiceQuiz.answeredCount }}/{{ practiceQuiz.questionCount }}
            <span v-if="practiceQuiz.generationSource"> ｜ {{ practiceQuiz.generationSource }}</span>
            <span v-if="practiceQuiz.failureReason"> ｜ {{ practiceQuiz.failureReason }}</span>
          </div>

          <div v-if="practiceError" class="training-error-row">
            <span class="training-error">{{ practiceError }}</span>
            <button class="training-link-btn" @click="handleRetryPractice">重试</button>
          </div>

          <div v-else-if="quizStatus === 'GENERATING'" class="training-loading">正在异步生成测验题目...</div>
          <div v-else-if="practiceLoading" class="training-loading">正在加载题目与提交记录...</div>
          <div v-else-if="practiceItems.length === 0" class="training-empty">暂无测验题，点击“异步生成测验”开始。</div>

          <div v-else class="training-item-list">
            <article v-for="item in practiceItems" :key="item.itemId" class="training-item">
              <div class="training-item-head">
                <span class="training-item-type">{{ item.questionType }}</span>
                <span class="training-item-meta">难度 {{ item.difficulty }} ｜ {{ item.source }}</span>
              </div>
              <p class="training-item-stem">{{ item.stem }}</p>

              <ul v-if="resolveOptionLines(item.options).length > 0" class="training-options">
                <li v-for="(line, idx) in resolveOptionLines(item.options)" :key="`${item.itemId}-${idx}`">{{ line }}</li>
              </ul>

              <textarea
                :value="getItemDraft(item.itemId)"
                class="training-answer"
                rows="3"
                :disabled="practiceSubmitting"
                placeholder="输入你的答案..."
                @input="setItemDraft(item.itemId, ($event.target as HTMLTextAreaElement).value)"
              ></textarea>

              <div class="training-submit-row">
                <button
                  class="training-btn primary"
                  :disabled="!canSubmitPractice(item.itemId)"
                  @click="handleSubmitPractice(item.itemId)"
                >
                  {{ submittingPracticeItemId === item.itemId ? '提交中...' : '提交答案' }}
                </button>
              </div>

              <div v-if="getLatestSubmission(item.itemId)" class="training-result">
                <p class="training-result-title">
                  判题结果：
                  <strong>{{ getLatestSubmission(item.itemId)?.isCorrect ? '正确' : '待改进' }}</strong>
                  <span v-if="getLatestSubmission(item.itemId)?.score !== null">
                    （{{ getLatestSubmission(item.itemId)?.score }} 分）
                  </span>
                </p>
                <p v-if="getLatestSubmission(item.itemId)?.feedback" class="training-result-text">
                  {{ getLatestSubmission(item.itemId)?.feedback }}
                </p>
                <div v-if="(getLatestSubmission(item.itemId)?.errorTags ?? []).length > 0" class="training-tags">
                  <span
                    v-for="tag in getLatestSubmission(item.itemId)?.errorTags ?? []"
                    :key="`${item.itemId}-${tag}`"
                    class="training-tag"
                  >
                    {{ tag }}
                  </span>
                </div>
              </div>
            </article>
          </div>

          <div v-if="practiceSubmitError" class="training-error-row">
            <span class="training-error">{{ practiceSubmitError }}</span>
          </div>

          <section class="feedback-panel">
            <div class="feedback-head">
              <h4>测验反馈报告</h4>
              <span v-if="feedbackLoading" class="feedback-loading">更新中...</span>
            </div>

            <div v-if="practiceFeedbackReport">
              <p class="feedback-summary">{{ practiceFeedbackReport.diagnosisSummary }}</p>

              <div v-if="practiceFeedbackReport.strengths.length > 0" class="weak-node-list">
                <article class="weak-node">
                  <p class="weak-node-title">亮点</p>
                  <ul class="bullet-list">
                    <li v-for="(item, idx) in practiceFeedbackReport.strengths" :key="`strength-${idx}`">{{ item }}</li>
                  </ul>
                </article>
              </div>

              <div v-if="practiceFeedbackReport.weaknesses.length > 0" class="weak-node-list">
                <article class="weak-node">
                  <p class="weak-node-title">待复习</p>
                  <ul class="bullet-list">
                    <li v-for="(item, idx) in practiceFeedbackReport.weaknesses" :key="`weak-${idx}`">{{ item }}</li>
                  </ul>
                </article>
              </div>

              <div v-if="practiceFeedbackReport.reviewFocus.length > 0" class="training-tags">
                <span v-for="focus in practiceFeedbackReport.reviewFocus" :key="focus" class="training-tag">
                  {{ focus }}
                </span>
              </div>

              <p class="feedback-summary">{{ practiceFeedbackReport.nextRoundAdvice }}</p>

              <div class="training-actions">
                <button class="training-btn" :disabled="practiceApplyingAction" @click="handleFeedbackAction('REVIEW')">
                  进入 review
                </button>
                <button
                  class="training-btn primary"
                  :disabled="practiceApplyingAction"
                  @click="handleFeedbackAction('NEXT_ROUND')"
                >
                  进入 next_round
                </button>
              </div>
            </div>

            <div v-else-if="learningFeedback">
              <p class="feedback-summary">{{ learningFeedback.diagnosisSummary || '暂无诊断总结。' }}</p>
              <div v-if="learningFeedback.weakNodes.length > 0" class="weak-node-list">
                <article v-for="node in learningFeedback.weakNodes.slice(0, 3)" :key="node.nodeId" class="weak-node">
                  <p class="weak-node-title">{{ node.nodeName }}</p>
                  <p class="weak-node-meta">
                    掌握度 {{ toPercent(node.masteryScore) }}% ｜ 训练正确率 {{ toPercent(node.trainingAccuracy) }}%
                  </p>
                  <p v-if="node.reasons.length > 0" class="weak-node-reason">原因：{{ node.reasons.join('，') }}</p>
                  <div v-if="node.recentErrorTags.length > 0" class="training-tags">
                    <span v-for="tag in node.recentErrorTags" :key="`${node.nodeId}-${tag}`" class="training-tag">
                      {{ tag }}
                    </span>
                  </div>
                </article>
              </div>
              <p v-else class="training-note">暂无明显薄弱点，继续保持当前训练节奏。</p>
            </div>

            <div v-else-if="feedbackLoading" class="feedback-loading">正在加载反馈...</div>
            <div v-else class="training-note">暂无反馈报告，请先完成全部测验题。</div>
          </section>
        </template>
      </section>

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
            placeholder="输入你的问题，例如：我不理解这一题为什么这样判断？"
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

.training-panel {
  margin-top: 2rem;
  padding: 1rem;
  border: 1px solid var(--color-border);
  border-radius: 12px;
  background: rgba(12, 21, 42, 0.7);
}
.training-header { display: flex; align-items: baseline; justify-content: space-between; gap: 0.75rem; margin-bottom: 0.75rem; flex-wrap: wrap; }
.training-title { font-size: 1rem; color: var(--color-text); }
.training-hint { font-size: 0.8125rem; color: var(--color-text-secondary); }
.training-actions { display: flex; gap: 0.625rem; margin-bottom: 0.75rem; flex-wrap: wrap; }
.training-btn {
  min-height: 36px;
  padding: 0 0.875rem;
  border-radius: 8px;
  border: 1px solid var(--color-border);
  color: var(--color-text);
  background: rgba(9, 17, 34, 0.9);
}
.training-btn.primary { background: var(--color-primary); border-color: var(--color-primary); color: #fff; }
.training-btn:disabled { opacity: 0.6; cursor: not-allowed; }
.training-loading, .training-empty, .training-note {
  padding: 0.875rem;
  border-radius: 8px;
  background: var(--color-bg);
  color: var(--color-text-secondary);
}
.training-item-list { display: flex; flex-direction: column; gap: 0.875rem; margin-top: 0.75rem; }
.training-item { border: 1px solid var(--color-border); border-radius: 10px; padding: 0.875rem; background: rgba(10, 17, 33, 0.85); }
.training-item-head { display: flex; justify-content: space-between; align-items: center; gap: 0.5rem; flex-wrap: wrap; margin-bottom: 0.5rem; }
.training-item-type { font-size: 0.8125rem; font-weight: 600; color: var(--color-primary); }
.training-item-meta { font-size: 0.75rem; color: var(--color-text-secondary); }
.training-item-stem { line-height: 1.6; color: var(--color-text); margin-bottom: 0.625rem; }
.training-options { padding-left: 1.25rem; margin: 0 0 0.625rem; color: var(--color-text); }
.training-options li { margin-bottom: 0.375rem; }
.training-answer {
  width: 100%;
  border: 1px solid var(--color-border);
  border-radius: 8px;
  padding: 0.75rem;
  background: var(--color-bg);
  color: var(--color-text);
  resize: vertical;
}
.training-submit-row { display: flex; justify-content: flex-end; margin-top: 0.625rem; }
.training-result {
  margin-top: 0.75rem;
  padding: 0.75rem;
  border-radius: 8px;
  border: 1px solid var(--color-border);
  background: rgba(13, 24, 46, 0.88);
}
.training-result-title { color: var(--color-text); font-size: 0.875rem; margin-bottom: 0.375rem; }
.training-result-text { color: var(--color-text-secondary); line-height: 1.6; }
.training-tags { display: flex; flex-wrap: wrap; gap: 0.375rem; margin-top: 0.5rem; }
.training-tag {
  font-size: 0.75rem;
  padding: 0.2rem 0.5rem;
  border-radius: 999px;
  border: 1px solid rgba(243, 102, 102, 0.35);
  background: rgba(243, 102, 102, 0.15);
  color: #ffd6d6;
}
.training-error-row { display: flex; align-items: center; gap: 0.75rem; margin-top: 0.625rem; }
.training-error { color: var(--color-error); font-size: 0.875rem; }
.training-link-btn { color: var(--color-primary); text-decoration: underline; font-size: 0.875rem; }

.feedback-panel { margin-top: 0.875rem; border: 1px solid var(--color-border); border-radius: 10px; padding: 0.75rem; }
.feedback-head { display: flex; align-items: baseline; justify-content: space-between; gap: 0.5rem; margin-bottom: 0.5rem; }
.feedback-loading { color: var(--color-text-secondary); font-size: 0.8125rem; }
.feedback-summary { color: var(--color-text); line-height: 1.6; margin-bottom: 0.5rem; }
.weak-node-list { display: grid; gap: 0.5rem; }
.weak-node { border: 1px solid var(--color-border); border-radius: 8px; padding: 0.625rem; background: rgba(8, 15, 28, 0.9); }
.weak-node-title { color: var(--color-text); font-weight: 600; }
.weak-node-meta, .weak-node-reason { color: var(--color-text-secondary); font-size: 0.8125rem; line-height: 1.5; }

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
