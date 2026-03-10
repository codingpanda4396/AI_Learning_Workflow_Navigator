<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import FloatingTutor from '@/components/tutor/FloatingTutor.vue'
import TutorLauncher from '@/components/tutor/TutorLauncher.vue'
import ErrorMessage from '@/components/ErrorMessage.vue'
import FeedbackPanel from '@/components/FeedbackPanel.vue'
import PracticeQuestionCard from '@/components/PracticeQuestionCard.vue'
import PrimaryButton from '@/components/PrimaryButton.vue'
import { useTutorPanel } from '@/composables/useTutorPanel'
import { buildTrainingSteps, deriveTrainingStatuses, getTrainingActionContent } from '@/constants/trainingStage'
import { usePracticeStore } from '@/stores/practice'
import { useSessionStore } from '@/stores/session'
import { buildTutorContext } from '@/utils/buildTutorContext'
import { getLearningStageDisplay, normalizeLearningStage } from '@/utils/learningPlanDisplay'

const route = useRoute()
const router = useRouter()
const sessionStore = useSessionStore()
const practiceStore = usePracticeStore()

const taskId = computed(() => Number(route.params.id))
const task = computed(() => sessionStore.currentTask)
const loadError = computed(() => sessionStore.error)
const isLoading = computed(() => sessionStore.runningTask)
const isTrainingTask = computed(() => task.value?.stage === 'TRAINING')
const sections = computed(() => task.value?.output.sections ?? [])
const answerDrafts = ref<Record<number, string>>({})
const quizPollTimer = ref<number | null>(null)
const questionListRef = ref<HTMLElement | null>(null)
const feedbackSectionRef = ref<HTMLElement | null>(null)

const practiceQuiz = computed(() => practiceStore.quiz)
const practiceItems = computed(() => practiceStore.items)
const practiceFeedbackReport = computed(() => practiceStore.feedbackReport)

const sessionNodeName = computed(() => {
  const nodeId = task.value?.nodeId
  if (!nodeId) {
    return ''
  }
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

const stageLabel = computed(() => getLearningStageDisplay(normalizeLearningStage(task.value?.stage)).title)
const sessionCourse = computed(() => sessionStore.currentSession?.courseId || '当前课程')
const sessionChapter = computed(() => sessionStore.currentSession?.chapterId || '当前章节')
const taskGoal = computed(() => sessionStore.currentSession?.goalText || buildGoalSummary())
const taskSummary = computed(() => buildGoalSummary())

const practiceQueryError = computed(() => {
  if (practiceStore.lastError?.status === 404) {
    return null
  }
  return practiceStore.itemsError
})

const feedbackError = computed(() => {
  if (practiceStore.lastError?.status === 404) {
    return null
  }
  return practiceStore.loadingFeedback ? null : practiceStore.submitError
})

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

const trainingState = computed(() =>
  deriveTrainingStatuses({
    task: task.value,
    taskError: loadError.value,
    quiz: practiceQuiz.value,
    quizError: practiceQueryError.value,
    feedbackReport: practiceFeedbackReport.value,
    feedbackError: feedbackError.value,
    hasSubmittedAnswers: hasSubmittedAnswers.value,
    requestingQuiz: practiceStore.requestingQuiz,
    pollingQuiz: practiceStore.pollingQuiz,
    submittingQuiz: practiceStore.submittingQuiz,
    loadingFeedback: practiceStore.loadingFeedback,
  }),
)

const trainingSteps = computed(() => buildTrainingSteps(trainingState.value.view))
const actionContent = computed(() => getTrainingActionContent(trainingState.value.view))
const showGoalSection = computed(
  () => trainingState.value.view === 'quiz_not_generated' || trainingState.value.view === 'quiz_generating',
)
const showQuestionSection = computed(() => trainingState.value.view === 'quiz_ready')
const showFeedbackSection = computed(() => trainingState.value.view === 'feedback_ready')

const allQuestionsAnswered = computed(() => {
  if (!practiceItems.value.length) {
    return false
  }
  return practiceItems.value.every((item) => (answerDrafts.value[item.questionId] ?? '').trim().length > 0)
})

const progressText = computed(() => {
  if (!isTrainingTask.value) {
    return '跟随 Tutor 完成当前任务'
  }
  if (!practiceQuiz.value) {
    return '检测题尚未生成'
  }
  return `${practiceQuiz.value.answeredCount}/${practiceQuiz.value.questionCount} 题`
})

const progressHint = computed(() => {
  if (!isTrainingTask.value) {
    return '先理解任务目标，再结合 Tutor 继续推进。'
  }
  if (trainingState.value.view === 'feedback_ready') {
    return '检测已完成，可以直接查看反馈。'
  }
  if (trainingState.value.view === 'feedback_generating') {
    return '答案已提交，系统正在生成反馈。'
  }
  if (trainingState.value.view === 'quiz_ready') {
    return '题目已生成，可以逐题填写并一次性提交。'
  }
  if (trainingState.value.view === 'error') {
    return '检测流程失败，可直接重试。'
  }
  return '先生成检测题，再开始本轮检测。'
})

const sidebarTips = computed(() => {
  const tips = [`当前阶段：${stageLabel.value}`, '遇到卡点时，先向 Tutor 提一个具体问题。']
  if (isTrainingTask.value) {
    tips.push('先独立作答，再对照反馈页查看薄弱点。')
  } else {
    tips.push('先完成当前任务，再返回会话继续下一步。')
  }
  return tips
})

const lightweightNotice = computed(() => {
  if (trainingState.value.view === 'quiz_generating') {
    return '检测题生成中，稍后会自动刷新。'
  }
  if (trainingState.value.view === 'feedback_generating') {
    return '反馈整理中，稍后会自动刷新到反馈区块。'
  }
  return ''
})

const quizStatusLabel = computed(() => {
  if (!practiceQuiz.value) {
    return '未生成'
  }
  if (practiceQuiz.value.generationStatus === 'FAILED' || practiceQuiz.value.quizStatus === 'FAILED') {
    return '失败，可重试'
  }
  if (practiceQuiz.value.generationStatus === 'PENDING' || practiceQuiz.value.generationStatus === 'RUNNING') {
    return '生成中'
  }
  if (practiceQuiz.value.quizStatus === 'QUIZ_READY') {
    return '已生成，可开始检测'
  }
  if (practiceQuiz.value.quizStatus === 'FEEDBACK_READY') {
    return '反馈已生成'
  }
  return practiceQuiz.value.quizStatus
})

function buildGoalSummary() {
  const summaryParts = sections.value
    .flatMap((section) => [section.text, ...(section.bullets ?? []), ...(section.steps ?? []), ...(section.items ?? [])])
    .filter((item): item is string => typeof item === 'string' && item.trim().length > 0)
    .slice(0, 2)
  if (summaryParts.length > 0) {
    return summaryParts.join('；')
  }
  return '围绕当前知识点完成理解、检测和反馈回看。'
}

function resolveSessionId() {
  const fromQuery = Number(route.query.sessionId)
  if (Number.isFinite(fromQuery) && fromQuery > 0) {
    return fromQuery
  }
  return sessionStore.currentTaskSessionId || sessionStore.sessionId || null
}

function isValidPositiveId(value: number | null | undefined) {
  return Number.isFinite(value) && (value ?? 0) > 0
}

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
    taskGoal: taskGoal.value,
    taskSummary: taskSummary.value,
    session: sessionStore.currentSession,
    task: task.value,
  }),
)

const tutorPanel = useTutorPanel({
  sessionId: computed(() => resolveSessionId()),
  taskId,
  context: tutorContext,
})

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
  if (!practiceStore.quiz) {
    return false
  }
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
  if (!sessionId) {
    return
  }
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
  if (!isTrainingTask.value) {
    return
  }
  const sessionId = resolveSessionId()
  if (!sessionId) {
    return
  }
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
  if (!sessionId) {
    return
  }
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

async function handlePrimaryAction() {
  if (!isTrainingTask.value) {
    return
  }
  if (trainingState.value.view === 'quiz_not_generated') {
    await syncQuizState(true)
    return
  }
  if (trainingState.value.view === 'quiz_ready') {
    await scrollToElement(questionListRef.value)
    return
  }
  if (trainingState.value.view === 'feedback_ready') {
    await scrollToElement(feedbackSectionRef.value)
    return
  }
  if (trainingState.value.view === 'error') {
    await handleRetry()
  }
}

async function handleSubmitQuiz() {
  const sessionId = resolveSessionId()
  if (!sessionId || !allQuestionsAnswered.value || practiceStore.submittingQuiz) {
    return
  }
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
  if (!sessionId || practiceStore.applyingFeedbackAction) {
    return
  }
  try {
    await practiceStore.applyFeedback(sessionId, action)
    await sessionStore.fetchLearningFeedback(sessionId)
  } catch (error) {
    console.error('应用反馈动作失败:', error)
  }
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
        <button type="button" class="ghost-btn" @click="handleContinue">返回会话</button>
      </header>

      <div v-if="isLoading && !task" class="page-state">正在加载任务...</div>
      <ErrorMessage v-else-if="loadError && !task" :message="loadError" @retry="handleRetry" />

      <div v-else-if="task" class="task-layout">
        <section class="main-column">
          <section class="hero-card">
            <p class="eyebrow">Task Run</p>
            <h1>{{ taskTitle }}</h1>
            <p class="meta">{{ sessionCourse }} / {{ sessionChapter }} · {{ stageLabel }}</p>
            <p class="goal">{{ taskGoal }}</p>
          </section>

          <section v-if="!isTrainingTask" class="content-card">
            <div class="section-head">
              <h2>任务内容</h2>
              <p>先阅读本次任务，再结合 Tutor 继续推进。</p>
            </div>

            <div class="goal-list">
              <article v-for="section in sections" :key="section.title" class="goal-item">
                <h3>{{ section.title }}</h3>
                <p v-if="section.text">{{ section.text }}</p>
                <ul v-else-if="section.bullets?.length">
                  <li v-for="(bullet, index) in section.bullets" :key="`${section.title}-${index}`">{{ bullet }}</li>
                </ul>
                <ol v-else-if="section.steps?.length">
                  <li v-for="(step, index) in section.steps" :key="`${section.title}-${index}`">{{ step }}</li>
                </ol>
                <ul v-else-if="section.items?.length">
                  <li v-for="(item, index) in section.items" :key="`${section.title}-${index}`">{{ item }}</li>
                </ul>
              </article>
            </div>
          </section>

          <section v-if="showGoalSection" class="content-card">
            <div class="section-head">
              <h2>这一轮要学什么</h2>
              <p>保留原有任务说明区，只在这里增量接入检测题生成状态。</p>
            </div>

            <div class="quiz-status-banner">
              <strong>检测题状态</strong>
              <span>{{ quizStatusLabel }}</span>
            </div>

            <div class="goal-list">
              <article v-for="section in sections" :key="section.title" class="goal-item">
                <h3>{{ section.title }}</h3>
                <p v-if="section.text">{{ section.text }}</p>
                <ul v-else-if="section.bullets?.length">
                  <li v-for="(bullet, index) in section.bullets" :key="`${section.title}-${index}`">{{ bullet }}</li>
                </ul>
                <ol v-else-if="section.steps?.length">
                  <li v-for="(step, index) in section.steps" :key="`${section.title}-${index}`">{{ step }}</li>
                </ol>
                <ul v-else-if="section.items?.length">
                  <li v-for="(item, index) in section.items" :key="`${section.title}-${index}`">{{ item }}</li>
                </ul>
                <p v-else>当前任务内容已准备完成，可以继续进入检测。</p>
              </article>
            </div>
          </section>

          <section v-if="showQuestionSection" ref="questionListRef" class="content-card">
            <div class="section-head">
              <h2>检测题</h2>
              <p>支持逐题填写，并一次性提交全部答案。</p>
            </div>

            <div v-if="practiceItems.length" class="question-list">
              <PracticeQuestionCard
                v-for="(item, index) in practiceItems"
                :key="item.questionId"
                :item="item"
                :index="index"
                :draft="getItemDraft(item.questionId)"
                :disabled="practiceStore.submittingQuiz"
                @update-draft="setItemDraft(item.questionId, $event)"
              />
            </div>
            <div v-else class="inline-state">暂时还没有检测题。</div>

            <div class="submit-bar">
              <p class="submit-hint">
                {{ allQuestionsAnswered ? '所有题目已填写，可以提交。' : '请先完成所有题目的填写。' }}
              </p>
              <PrimaryButton
                type="button"
                :loading="practiceStore.submittingQuiz"
                :disabled="!allQuestionsAnswered"
                @click="handleSubmitQuiz"
              >
                一次性提交
              </PrimaryButton>
            </div>
          </section>

          <section v-if="showFeedbackSection" ref="feedbackSectionRef" class="content-card">
            <div class="section-head">
              <h2>反馈页</h2>
              <p>展示 overallSummary、questionResults、weaknesses 和 suggestedNextAction。</p>
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
            <span class="side-label">当前步骤</span>
            <strong>{{ stageLabel }}</strong>
            <p>{{ actionContent.title }}</p>
            <p class="status-copy">检测题状态：{{ quizStatusLabel }}</p>
            <PrimaryButton
              v-if="isTrainingTask"
              type="button"
              :loading="actionContent.loading"
              :disabled="actionContent.disabled"
              @click="handlePrimaryAction"
            >
              {{ actionContent.buttonText }}
            </PrimaryButton>
          </section>

          <section class="side-card">
            <span class="side-label">完成进度</span>
            <strong>{{ progressText }}</strong>
            <p>{{ progressHint }}</p>
            <div v-if="isTrainingTask" class="step-list">
              <div v-for="step in trainingSteps" :key="step.key" class="step-row">
                <span>{{ step.title }}</span>
                <span>{{ step.state === 'done' ? '已完成' : step.state === 'current' ? '当前' : '待开始' }}</span>
              </div>
            </div>
          </section>

          <section class="side-card">
            <span class="side-label">学习提示</span>
            <ul class="tip-list">
              <li v-for="tip in sidebarTips" :key="tip">{{ tip }}</li>
            </ul>
          </section>

          <p v-if="lightweightNotice" class="lightweight-tip">{{ lightweightNotice }}</p>
          <p v-if="trainingState.view === 'error'" class="error-tip">
            {{ practiceQueryError || feedbackError || '当前检测流程加载失败，请重试。' }}
          </p>
        </aside>
      </div>
    </main>

    <TutorLauncher :open="tutorPanel.isOpen.value" @toggle="tutorPanel.togglePanel" />
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
  padding: clamp(16px, 2.8vw, 32px);
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

.page-state,
.inline-state,
.lightweight-tip,
.error-tip {
  padding: 16px 18px;
  border-radius: var(--radius-lg);
  background: rgba(15, 22, 34, 0.9);
  color: var(--color-text-secondary);
}

.error-tip {
  border: 1px solid rgba(255, 122, 138, 0.35);
  color: var(--color-error);
}

.task-layout {
  width: min(1180px, 100%);
  margin: 0 auto;
  display: grid;
  grid-template-columns: minmax(0, 1.55fr) minmax(280px, 0.85fr);
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

.hero-card {
  display: grid;
  gap: 10px;
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
.section-head h2,
.section-head p,
.side-card p,
.status-copy {
  margin: 0;
}

.meta,
.goal,
.section-head p,
.side-card p,
.goal-item p,
.goal-item li,
.status-copy {
  color: var(--color-text-secondary);
  line-height: 1.7;
}

.section-head {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: baseline;
  flex-wrap: wrap;
  margin-bottom: 18px;
}

.goal-list,
.question-list {
  display: grid;
  gap: 14px;
}

.goal-item,
.quiz-status-banner,
.submit-bar {
  display: grid;
  gap: 10px;
  padding: 18px;
  border-radius: var(--radius-lg);
  border: 1px solid rgba(61, 80, 104, 0.4);
  background: rgba(10, 16, 26, 0.78);
}

.goal-item ul,
.goal-item ol,
.tip-list {
  margin: 0;
  padding-left: 20px;
}

.submit-bar {
  margin-top: 14px;
}

.submit-hint {
  margin: 0;
  color: var(--color-text-secondary);
}

.side-card {
  display: grid;
  gap: 12px;
}

.side-card strong {
  font-size: var(--font-size-lg);
  color: var(--color-text);
}

.step-list {
  display: grid;
  gap: 8px;
}

.step-row {
  display: flex;
  justify-content: space-between;
  gap: 10px;
  color: var(--color-text-secondary);
  font-size: var(--font-size-sm);
}

.tip-list {
  color: var(--color-text-secondary);
}

@media (max-width: 980px) {
  .task-layout {
    grid-template-columns: 1fr;
  }
}
</style>
