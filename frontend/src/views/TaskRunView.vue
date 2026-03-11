<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import FloatingTutor from '@/components/tutor/FloatingTutor.vue'
import TutorLauncher from '@/components/tutor/TutorLauncher.vue'
import ErrorMessage from '@/components/ErrorMessage.vue'
import FeedbackPanel from '@/components/FeedbackPanel.vue'
import PracticeQuestionCard from '@/components/PracticeQuestionCard.vue'
import PracticeStatusCard from '@/components/PracticeStatusCard.vue'
import EmptyStatePanel from '@/components/EmptyStatePanel.vue'
import PrimaryButton from '@/components/PrimaryButton.vue'
import { useTutorPanel } from '@/composables/useTutorPanel'
import { usePracticeStore } from '@/stores/practice'
import { useSessionStore } from '@/stores/session'
import { buildTutorContext } from '@/utils/buildTutorContext'
import { getStageShortLabel } from '@/utils/learningNarrative'

const route = useRoute()
const router = useRouter()
const sessionStore = useSessionStore()
const practiceStore = usePracticeStore()

const taskId = computed(() => Number(route.params.id))
const task = computed(() => sessionStore.currentTask)
const loadError = computed(() => sessionStore.error)
const isLoading = computed(() => sessionStore.runningTask)
const sections = computed(() => task.value?.output.sections ?? [])
const answerDrafts = ref<Record<number, string>>({})
const quizPollTimer = ref<number | null>(null)
const questionListRef = ref<HTMLElement | null>(null)
const feedbackSectionRef = ref<HTMLElement | null>(null)

const practiceQuiz = computed(() => practiceStore.quiz)
const practiceItems = computed(() => practiceStore.items)
const practiceFeedbackReport = computed(() => practiceStore.feedbackReport)
const practiceStatusView = computed(() => practiceStore.statusView)
const isTrainingTask = computed(() => task.value?.stage === 'TRAINING')

const sessionNodeName = computed(() => {
  const nodeId = task.value?.nodeId
  if (!nodeId) return ''
  return sessionStore.currentSessionPath?.nodes.find((node) => node.nodeId === nodeId)?.nodeName ?? ''
})

const taskTitle = computed(() => {
  return (
    sessionNodeName.value ||
    sections.value.find((section) => section.title?.trim())?.title?.trim() ||
    sessionStore.currentSession?.goalText ||
    '当前学习任务'
  )
})

const sessionCourse = computed(() => sessionStore.currentSession?.courseId || '当前课程')
const sessionChapter = computed(() => sessionStore.currentSession?.chapterId || '当前章节')
const stepLabel = computed(() => getStageShortLabel(task.value?.stage))
const taskGoal = computed(() => sessionStore.currentSession?.goalText || buildGoalSummary())
const currentStepGoal = computed(() => {
  const first = sections.value.find((section) => section.text?.trim() || section.title?.trim())
  return first?.text?.trim() || first?.title?.trim() || '先读任务内容，再结合 Tutor 推进。'
})

const keyPoints = computed(() => pickSectionList(['关键点', '重点', '核心', '要点'], 4))
const commonMistakes = computed(() => pickSectionList(['易错', '常见错误', '误区', '注意'], 4))
const workedExamples = computed(() => pickSectionList(['示例', '例子', 'worked example'], 3))

const hasSubmittedAnswers = computed(() => {
  const quizStatus = practiceQuiz.value?.quizStatus
  return (
    !!practiceFeedbackReport.value ||
    quizStatus === 'ANSWERED' ||
    quizStatus === 'FEEDBACK_READY' ||
    quizStatus === 'REVIEWING' ||
    quizStatus === 'NEXT_ROUND'
  )
})

const allQuestionsAnswered = computed(() => {
  if (!practiceItems.value.length) return false
  return practiceItems.value.every((item) => (answerDrafts.value[item.questionId] ?? '').trim().length > 0)
})

const mainButtonText = computed(() => {
  if (!isTrainingTask.value) return '完成这一步'
  if (practiceFeedbackReport.value) return '查看结果'
  if (practiceStatusView.value.status === 'ready' && !hasSubmittedAnswers.value) return '进入检测'
  return '完成这一步'
})

const mainButtonDisabled = computed(() => {
  if (isTrainingTask.value && practiceFeedbackReport.value) return false
  return false
})

const tutorContext = computed(() =>
  buildTutorContext({
    sessionId: resolveSessionId(),
    stage: task.value?.stage,
    taskId: taskId.value,
    topic: taskTitle.value,
    course: sessionStore.currentSession?.courseId,
    chapter: sessionStore.currentSession?.chapterId,
    goal: sessionStore.currentSession?.goalText,
    taskTitle: taskTitle.value,
    taskGoal: currentStepGoal.value,
    taskSummary: taskGoal.value,
    session: sessionStore.currentSession,
    task: task.value,
  }),
)

const tutorPanel = useTutorPanel({
  sessionId: computed(() => resolveSessionId()),
  taskId,
  context: tutorContext,
})

function buildGoalSummary() {
  const summaryParts = sections.value
    .flatMap((section) => [section.text, ...(section.bullets ?? []), ...(section.steps ?? []), ...(section.items ?? [])])
    .filter((item): item is string => typeof item === 'string' && item.trim().length > 0)
    .slice(0, 3)
  return summaryParts.length > 0 ? summaryParts.join('；') : '围绕当前知识点完成理解、检测和反馈回看。'
}

function pickSectionList(keywords: string[], limit: number) {
  const matched = sections.value.find((section) =>
    keywords.some((keyword) => `${section.title ?? ''}${section.text ?? ''}`.toLowerCase().includes(keyword.toLowerCase())),
  )
  const source = matched
    ? [...(matched.bullets ?? []), ...(matched.steps ?? []), ...(matched.items ?? []), ...(matched.text ? [matched.text] : [])]
    : sections.value.flatMap((section) => [...(section.bullets ?? []), ...(section.items ?? []), ...(section.steps ?? [])])
  return source.filter((item) => item.trim().length > 0).slice(0, limit)
}

function resolveSessionId() {
  const fromQuery = Number(route.query.sessionId)
  if (Number.isFinite(fromQuery) && fromQuery > 0) return fromQuery
  return sessionStore.currentTaskSessionId || sessionStore.sessionId || null
}

function isValidPositiveId(value: number | null | undefined) {
  return Number.isFinite(value) && (value ?? 0) > 0
}

function getItemDraft(questionId: number) {
  return answerDrafts.value[questionId] ?? ''
}

function setItemDraft(questionId: number, value: string) {
  answerDrafts.value = {
    ...answerDrafts.value,
    [questionId]: value,
  }
}

function clearQuizPoll() {
  if (quizPollTimer.value !== null) {
    window.clearTimeout(quizPollTimer.value)
    quizPollTimer.value = null
  }
}

function shouldPollQuizStatus() {
  if (!practiceStore.quiz) return false
  return (
    practiceStore.quiz.generationStatus === 'PENDING' ||
    practiceStore.quiz.generationStatus === 'RUNNING' ||
    practiceStore.quiz.quizStatus === 'GENERATING' ||
    practiceStore.quiz.quizStatus === 'ANSWERED'
  )
}

async function scrollToElement(target: HTMLElement | null) {
  await nextTick()
  target?.scrollIntoView({ behavior: 'smooth', block: 'start' })
}

async function fetchSessionContext() {
  const sessionId = resolveSessionId()
  if (!sessionId) return
  try {
    await Promise.all([sessionStore.fetchSessionOverview(sessionId), sessionStore.fetchSessionPath(sessionId)])
  } catch {
    // supplemental request
  }
}

async function loadTask() {
  await sessionStore.runTask(taskId.value)
}

function resetQuizAsNotGenerated() {
  practiceStore.reset()
  answerDrafts.value = {}
}

async function syncFeedbackReport(sessionId: number) {
  const quizStatus = practiceStore.quiz?.quizStatus
  if (quizStatus === 'FEEDBACK_READY' || quizStatus === 'REVIEWING' || quizStatus === 'NEXT_ROUND') {
    await practiceStore.loadFeedbackReport(sessionId)
  } else {
    practiceStore.feedbackReport = null
  }
}

async function syncQuizState(generate = false) {
  if (!isTrainingTask.value) return
  const sessionId = resolveSessionId()
  if (!sessionId) return

  try {
    if (generate) {
      await practiceStore.requestQuiz(sessionId)
    } else {
      await practiceStore.loadQuizStatus(sessionId)
    }
  } catch (error) {
    if (practiceStore.lastError?.status === 404) {
      resetQuizAsNotGenerated()
      return
    }
    throw error
  }

  if (practiceStore.quiz?.generationStatus === 'SUCCEEDED') {
    await practiceStore.loadQuiz(sessionId)
  }

  await syncFeedbackReport(sessionId)

  if (shouldPollQuizStatus()) {
    scheduleQuizPoll()
  } else {
    clearQuizPoll()
  }
}

function scheduleQuizPoll() {
  clearQuizPoll()
  const sessionId = resolveSessionId()
  if (!sessionId) return
  quizPollTimer.value = window.setTimeout(async () => {
    try {
      await syncQuizState(false)
    } catch (error) {
      console.error('轮询检测状态失败:', error)
    }
  }, 1200)
}

async function loadTutorMessages() {
  await tutorPanel.ensureMessagesLoaded(true)
}

async function handleRetry() {
  await loadTask()
  await Promise.all([fetchSessionContext(), syncQuizState(false)])
}

async function handleSubmitQuiz() {
  const sessionId = resolveSessionId()
  if (!sessionId || !allQuestionsAnswered.value || practiceStore.submittingQuiz) return

  try {
    await practiceStore.submitQuiz(
      sessionId,
      practiceItems.value.map((item) => ({
        questionId: item.questionId,
        userAnswer: (answerDrafts.value[item.questionId] ?? '').trim(),
      })),
    )
    if (practiceStore.feedbackReport) {
      await sessionStore.fetchLearningFeedback(sessionId)
      await scrollToElement(feedbackSectionRef.value)
    } else {
      scheduleQuizPoll()
    }
  } catch (error) {
    console.error('提交检测答案失败:', error)
  }
}

async function handleFeedbackAction(action: 'REVIEW' | 'NEXT_ROUND') {
  const sessionId = resolveSessionId()
  if (!sessionId || practiceStore.applyingFeedbackAction) return
  try {
    await practiceStore.applyFeedback(sessionId, action)
    await sessionStore.fetchLearningFeedback(sessionId)
  } catch (error) {
    console.error('应用反馈动作失败:', error)
  }
}

async function handlePrimaryAction() {
  if (!isTrainingTask.value) {
    handleContinue()
    return
  }

  if (practiceFeedbackReport.value) {
    await scrollToElement(feedbackSectionRef.value)
    return
  }

  if (practiceStatusView.value.status === 'ready') {
    await scrollToElement(questionListRef.value)
    return
  }

  handleContinue()
}

function handleContinue() {
  const sessionId = resolveSessionId()
  if (!isValidPositiveId(sessionId)) {
    router.push({ name: 'home' })
    return
  }

  const step = Number(route.query.step)
  const resolvedStep = Number.isFinite(step) && step >= 1 && step <= 4 ? String(step) : '3'
  router.push({
    name: 'session',
    params: { id: String(sessionId) },
    query: { step: resolvedStep },
  })
}

onMounted(async () => {
  sessionStore.resetTaskState()
  practiceStore.reset()
  await loadTask()
  await Promise.all([fetchSessionContext(), syncQuizState(false)])
})

onBeforeUnmount(() => {
  clearQuizPoll()
})

watch(
  () => route.params.id,
  async () => {
    clearQuizPoll()
    practiceStore.reset()
    answerDrafts.value = {}
    await loadTask()
    await Promise.all([fetchSessionContext(), syncQuizState(false)])
  },
)
</script>

<template>
  <div class="task-run-shell">
    <main class="task-page">
      <header class="page-toolbar">
        <button type="button" class="ghost-btn" @click="router.back()">返回上一页</button>
        <button type="button" class="ghost-btn" @click="handleContinue">返回本轮学习</button>
      </header>

      <div v-if="isLoading && !task" class="page-state">正在加载任务...</div>
      <ErrorMessage v-else-if="loadError && !task" :message="loadError" @retry="handleRetry" />

      <div v-else-if="task" class="task-layout">
        <section class="main-column">
          <section class="hero-card">
            <p class="eyebrow">当前学习动作</p>
            <h1>{{ taskTitle }}</h1>
            <p class="meta">本轮学习 / {{ stepLabel }} · {{ sessionCourse }} / {{ sessionChapter }}</p>
            <p class="goal">本步目标：{{ currentStepGoal }}</p>
            <p class="guide">建议动作：先读任务，再和 Tutor 一起推进；完成后系统会带你进入下一步。</p>
          </section>

          <section class="content-card">
            <div class="section-head">
              <h2>这一步要学什么</h2>
              <p>先抓住核心内容，不需要先考虑系统阶段。</p>
            </div>

            <article class="content-block">
              <h3>任务内容</h3>
              <p>{{ taskGoal }}</p>
            </article>

            <article v-if="keyPoints.length" class="content-block">
              <h3>关键点</h3>
              <ul>
                <li v-for="item in keyPoints" :key="item">{{ item }}</li>
              </ul>
            </article>

            <article v-if="commonMistakes.length" class="content-block">
              <h3>常见错误 / 检查问题</h3>
              <ul>
                <li v-for="item in commonMistakes" :key="item">{{ item }}</li>
              </ul>
            </article>

            <article v-if="workedExamples.length" class="content-block">
              <h3>Worked Example</h3>
              <ul>
                <li v-for="item in workedExamples" :key="item">{{ item }}</li>
              </ul>
            </article>
          </section>

          <section v-if="isTrainingTask" class="content-card">
            <div class="section-head">
              <h2>检测题</h2>
              <p>这不是正式考试，而是帮助你找出哪里还不稳。</p>
            </div>

            <PracticeStatusCard
              :status="practiceStatusView.status"
              :badge="practiceStatusView.badge"
              :title="practiceStatusView.title"
              :description="practiceStatusView.description"
              :helper="practiceStatusView.helper"
            />

            <div v-if="practiceStatusView.status === 'ready'" ref="questionListRef" class="question-list">
              <PracticeQuestionCard
                v-for="(item, index) in practiceItems"
                :key="item.questionId"
                :item="item"
                :index="index"
                :draft="getItemDraft(item.questionId)"
                :disabled="practiceStore.submittingQuiz"
                @update-draft="setItemDraft(item.questionId, $event)"
              />

              <div class="submit-bar">
                <p class="submit-hint">
                  {{ allQuestionsAnswered ? '可以提交检测了，系统会给你下一步建议。' : '请先完成全部检测题。' }}
                </p>
                <PrimaryButton
                  type="button"
                  :loading="practiceStore.submittingQuiz"
                  :disabled="!allQuestionsAnswered"
                  @click="handleSubmitQuiz"
                >
                  提交检测
                </PrimaryButton>
              </div>
            </div>

            <EmptyStatePanel v-else :title="practiceStatusView.title" :description="practiceStatusView.description" />

            <button
              v-if="practiceStatusView.status === 'failed'"
              type="button"
              class="text-btn"
              @click="syncQuizState(true)"
            >
              重试生成检测题
            </button>
          </section>

          <section v-if="practiceFeedbackReport" ref="feedbackSectionRef" class="content-card">
            <div class="section-head">
              <h2>结果与下一步</h2>
              <p>看清已经掌握了什么、哪里还不稳，以及接下来该点哪里。</p>
            </div>
            <FeedbackPanel
              mode="ready"
              :report="practiceFeedbackReport"
              @review="handleFeedbackAction('REVIEW')"
              @next-round="handleFeedbackAction('NEXT_ROUND')"
            />
          </section>
        </section>

        <aside class="sidebar">
          <section class="side-card">
            <span class="side-label">当前在哪</span>
            <strong>{{ stepLabel }}</strong>
            <p>你正在学习「{{ taskTitle }}」。先完成当前内容，再根据系统提示进入检测或结果页。</p>
          </section>

          <section class="side-card">
            <span class="side-label">Tutor 提示</span>
            <p>如果卡住了，优先向 Tutor 提一个具体问题，例如“我不明白这里为什么这样做”。</p>
          </section>

          <section v-if="isTrainingTask" class="side-card">
            <span class="side-label">检测准备</span>
            <p>{{ practiceStatusView.badge }}：{{ practiceStatusView.description }}</p>
          </section>
        </aside>
      </div>

      <div v-if="task" class="bottom-action">
        <PrimaryButton type="button" :disabled="mainButtonDisabled" @click="handlePrimaryAction">
          {{ mainButtonText }}
        </PrimaryButton>
      </div>
    </main>

    <TutorLauncher :open="tutorPanel.isOpen.value" label="问 Tutor" @toggle="tutorPanel.togglePanel" />
    <FloatingTutor
      :open="tutorPanel.isOpen.value"
      :available="tutorPanel.canUseTutor.value"
      :context-title="tutorContext.contextTitle"
      :context-meta="`${sessionCourse} / ${sessionChapter}`"
      :messages="tutorPanel.messages.value"
      :loading="tutorPanel.loading.value"
      :load-error="tutorPanel.loadError.value"
      :send-error="tutorPanel.sendError.value"
      :sending="tutorPanel.sending.value"
      :input="tutorPanel.input.value"
      :quick-prompts="tutorPanel.quickPrompts"
      @close="tutorPanel.closePanel"
      @retry-load="loadTutorMessages"
      @retry-send="tutorPanel.retrySend"
      @submit="tutorPanel.sendMessage"
      @update-input="tutorPanel.setInput"
      @use-quick-prompt="tutorPanel.useQuickPrompt"
    />
  </div>
</template>

<style scoped>
.task-page {
  min-height: 100dvh;
  padding: clamp(16px, 2.8vw, 32px) clamp(16px, 2.8vw, 32px) 110px;
}

.page-toolbar {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-bottom: 24px;
  flex-wrap: wrap;
}

.ghost-btn {
  min-height: 44px;
  padding: 0 16px;
  border-radius: var(--radius-md);
  border: 1px solid rgba(61, 80, 104, 0.56);
  color: var(--color-text-secondary);
  background: rgba(15, 21, 33, 0.9);
}

.page-state {
  padding: 16px 18px;
  border-radius: var(--radius-lg);
  background: rgba(15, 22, 34, 0.9);
  color: var(--color-text-secondary);
}

.task-layout {
  width: min(1180px, 100%);
  margin: 0 auto;
  display: grid;
  grid-template-columns: minmax(0, 1.6fr) minmax(260px, 0.8fr);
  gap: 20px;
}

.main-column,
.sidebar {
  display: grid;
  gap: 18px;
  align-content: start;
}

.hero-card,
.content-card,
.side-card {
  padding: clamp(22px, 3vw, 30px);
  border-radius: var(--radius-xl);
  border: 1px solid rgba(61, 80, 104, 0.48);
  background: rgba(15, 21, 33, 0.94);
}

.hero-card,
.content-card,
.side-card,
.content-block {
  display: grid;
  gap: 12px;
}

.eyebrow,
.side-label {
  margin: 0;
  color: var(--color-text-muted);
  font-size: var(--font-size-xs);
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.hero-card h1,
.meta,
.goal,
.guide,
.section-head h2,
.section-head p,
.content-block h3,
.content-block p,
.side-card p {
  margin: 0;
}

.meta,
.goal,
.guide,
.section-head p,
.content-block p,
.content-block li,
.side-card p,
.submit-hint {
  color: var(--color-text-secondary);
  line-height: 1.7;
}

.section-head {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: baseline;
  flex-wrap: wrap;
}

.content-block {
  padding: 18px;
  border-radius: var(--radius-lg);
  border: 1px solid rgba(61, 80, 104, 0.38);
  background: rgba(10, 16, 26, 0.76);
}

.content-block ul {
  margin: 0;
  padding-left: 20px;
}

.question-list {
  display: grid;
  gap: 14px;
}

.submit-bar {
  display: grid;
  gap: 12px;
  padding: 18px;
  border-radius: var(--radius-lg);
  border: 1px solid rgba(61, 80, 104, 0.38);
  background: rgba(10, 16, 26, 0.76);
}

.text-btn {
  justify-self: start;
  border: none;
  background: transparent;
  color: var(--color-primary-hover);
  padding: 0;
  font-size: var(--font-size-sm);
}

.side-card strong {
  font-size: var(--font-size-lg);
  color: var(--color-text);
}

.bottom-action {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  display: flex;
  justify-content: center;
  padding: 16px;
  background: linear-gradient(180deg, rgba(9, 13, 21, 0), rgba(9, 13, 21, 0.96) 38%);
}

@media (max-width: 980px) {
  .task-layout {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 640px) {
  .bottom-action {
    padding: 12px 16px 20px;
  }

  .bottom-action :deep(.btn) {
    width: 100%;
  }
}
</style>
