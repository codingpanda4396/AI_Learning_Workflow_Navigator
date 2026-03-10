<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import ErrorMessage from '@/components/ErrorMessage.vue'
import FeedbackPanel from '@/components/FeedbackPanel.vue'
import PracticeQuestionCard from '@/components/PracticeQuestionCard.vue'
import PrimaryButton from '@/components/PrimaryButton.vue'
import TutorAssistPanel from '@/components/TutorAssistPanel.vue'
import {
  buildTrainingSteps,
  deriveTrainingStatuses,
  getTrainingActionContent,
} from '@/constants/trainingStage'
import { usePracticeStore } from '@/stores/practice'
import { useSessionStore } from '@/stores/session'
import { useTutorStore } from '@/stores/tutor'
import { getLearningStageDisplay, normalizeLearningStage } from '@/utils/learningPlanDisplay'

const route = useRoute()
const router = useRouter()
const sessionStore = useSessionStore()
const tutorStore = useTutorStore()
const practiceStore = usePracticeStore()

const taskId = computed(() => Number(route.params.id))
const task = computed(() => sessionStore.currentTask)
const loadError = computed(() => sessionStore.error)
const isLoading = computed(() => sessionStore.runningTask)
const isTrainingTask = computed(() => task.value?.stage === 'TRAINING')
const sections = computed(() => task.value?.output.sections ?? [])
const tutorInput = ref('')
const answerDrafts = ref<Record<number, string>>({})
const submittingPracticeItemId = ref<number | null>(null)
const quizPollTimer = ref<number | null>(null)
const questionListRef = ref<HTMLElement | null>(null)
const feedbackSectionRef = ref<HTMLElement | null>(null)

const practiceQuiz = computed(() => practiceStore.quiz)
const practiceItems = computed(() => practiceStore.items)
const practiceSubmissions = computed(() => practiceStore.submissions)
const practiceFeedbackReport = computed(() => practiceStore.feedbackReport)
const tutorMessages = computed(() => tutorStore.messages)
const tutorLoading = computed(() => tutorStore.loadingMessages)
const tutorSending = computed(() => tutorStore.sendingMessage)
const tutorLoadError = computed(() => tutorStore.loadError)
const tutorSendError = computed(() => tutorStore.sendError)
const tutorChips = [
  '这一步最关键的思路是什么？',
  '可以换一种更好懂的讲法吗？',
  '帮我先提示一下，不要直接给答案。',
]

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

const practiceQueryError = computed(() => {
  if (practiceStore.lastError?.status === 404) {
    return null
  }
  return practiceStore.itemsError || practiceStore.submissionsError
})

const feedbackError = computed(() => {
  if (practiceStore.lastError?.status === 404) {
    return null
  }
  return practiceStore.loadingFeedback ? null : practiceStore.submitError
})

const hasSubmittedAnswers = computed(() => practiceSubmissions.value.length > 0)

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
    submittingAnswer: practiceStore.submittingAnswer,
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

const progressText = computed(() => {
  if (!isTrainingTask.value) {
    return '跟随 Tutor 完成当前任务'
  }
  if (!practiceQuiz.value) {
    return '练习未生成'
  }
  return `${practiceQuiz.value.answeredCount}/${practiceQuiz.value.questionCount} 题`
})

const progressHint = computed(() => {
  if (!isTrainingTask.value) {
    return '先理解任务目标，再和 Tutor 一步步推进。'
  }
  if (trainingState.value.view === 'feedback_ready') {
    return '练习已完成，可以查看结果。'
  }
  if (trainingState.value.view === 'feedback_generating') {
    return '答案已提交，系统正在整理结果。'
  }
  if (trainingState.value.view === 'quiz_ready') {
    return '练习已准备好，可以开始作答。'
  }
  return '先看目标，再决定是否开始本章练习。'
})

const sidebarTips = computed(() => {
  const tips = [
    `当前步骤：${stageLabel.value}`,
    '遇到卡点时，先向 Tutor 提一个具体问题。',
  ]
  if (isTrainingTask.value) {
    tips.push('做练习时先写自己的思路，再看提示。')
  } else {
    tips.push('先完成这一任务，再返回会话继续下一步。')
  }
  return tips
})

const lightweightNotice = computed(() => {
  if (trainingState.value.view === 'quiz_generating') {
    return '练习题正在生成，稍后会自动出现。'
  }
  if (trainingState.value.view === 'feedback_generating') {
    return '结果正在整理，稍后会自动刷新。'
  }
  return ''
})

function buildGoalSummary() {
  const summaryParts = sections.value
    .flatMap((section) => [section.text, ...(section.bullets ?? []), ...(section.steps ?? []), ...(section.items ?? [])])
    .filter((item): item is string => typeof item === 'string' && item.trim().length > 0)
    .slice(0, 2)
  if (summaryParts.length > 0) {
    return summaryParts.join('；')
  }
  return '围绕当前知识点完成理解、练习和结果回看。'
}

function resolveSessionId() {
  const fromQuery = Number(route.query.sessionId)
  if (Number.isFinite(fromQuery) && fromQuery > 0) {
    return fromQuery
  }
  return sessionStore.currentTaskSessionId || sessionStore.sessionId || null
}

function getLatestSubmission(itemId: number) {
  return latestSubmissionByItem.value.get(itemId) ?? null
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

function clearQuizPoll() {
  if (quizPollTimer.value !== null) {
    window.clearTimeout(quizPollTimer.value)
    quizPollTimer.value = null
  }
}

function shouldPollQuizStatus() {
  return practiceStore.quiz?.status === 'GENERATING' || practiceStore.quiz?.status === 'ANSWERED'
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
    await Promise.all([
      sessionStore.fetchSessionOverview(sessionId),
      sessionStore.fetchSessionPath(sessionId),
    ])
  } catch {
    // supplemental
  }
}

async function loadTask() {
  await sessionStore.runTask(taskId.value)
}

async function refreshQuizRelated(sessionId: number) {
  await practiceStore.loadQuiz(sessionId, taskId.value)
  if (shouldPollQuizStatus()) {
    return
  }
  await practiceStore.loadSubmissions(sessionId, taskId.value)
  if (
    practiceStore.quiz?.status === 'FEEDBACK_READY' ||
    practiceStore.quiz?.status === 'REVIEWING' ||
    practiceStore.quiz?.status === 'NEXT_ROUND'
  ) {
    try {
      await practiceStore.loadFeedbackReport(sessionId, taskId.value)
    } catch {
      practiceStore.feedbackReport = null
    }
  } else {
    practiceStore.feedbackReport = null
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
      if (shouldPollQuizStatus()) {
        scheduleQuizPoll()
      }
    } catch (error) {
      console.error('轮询练习状态失败:', error)
    }
  }, 1200)
}

function resetQuizAsNotGenerated() {
  practiceStore.quiz = null
  practiceStore.items = []
  practiceStore.feedbackReport = null
  practiceStore.submissions = []
  practiceStore.clearErrors()
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
  } catch (error) {
    if (practiceStore.lastError?.status === 404) {
      resetQuizAsNotGenerated()
      return
    }
    throw error
  }

  if (shouldPollQuizStatus()) {
    practiceStore.feedbackReport = null
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
}

async function handleRetry() {
  await loadTask()
  await Promise.all([fetchSessionContext(), loadTrainingClosure(), loadTutorMessages()])
}

async function handlePrimaryAction() {
  if (!isTrainingTask.value) {
    return
  }
  if (trainingState.value.view === 'quiz_not_generated') {
    await loadTrainingClosure(true)
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

async function handleSubmitPractice(itemId: number) {
  const answer = answerDrafts.value[itemId]?.trim() ?? ''
  const sessionId = resolveSessionId()
  if (!sessionId || !answer || practiceStore.submittingAnswer) {
    return
  }
  submittingPracticeItemId.value = itemId
  try {
    await practiceStore.submitAnswer(sessionId, taskId.value, itemId, answer)
    await refreshQuizRelated(sessionId)
    if (shouldPollQuizStatus()) {
      scheduleQuizPoll()
    }
  } catch (error) {
    console.error('提交练习答案失败:', error)
  } finally {
    submittingPracticeItemId.value = null
  }
}

async function handleFeedbackAction(action: 'REVIEW' | 'NEXT_ROUND') {
  const sessionId = resolveSessionId()
  if (!sessionId || practiceStore.applyingFeedbackAction) {
    return
  }
  try {
    await practiceStore.applyFeedback(sessionId, taskId.value, action)
    await sessionStore.fetchLearningFeedback(sessionId)
    await loadTrainingClosure()
  } catch (error) {
    console.error('应用反馈动作失败:', error)
  }
}

async function handleSendTutorMessage() {
  const content = tutorInput.value.trim()
  const sessionId = resolveSessionId()
  if (!content || !sessionId || tutorStore.sendingMessage) {
    return
  }

  try {
    try {
      await tutorStore.sendStream(sessionId, taskId.value, content)
    } catch {
      await tutorStore.send(sessionId, taskId.value, content)
    }
    tutorInput.value = ''
  } catch (error) {
    console.error('发送 Tutor 消息失败:', error)
  }
}

async function handleRetryTutorSend() {
  try {
    await tutorStore.retryLastSend()
    tutorInput.value = ''
  } catch (error) {
    console.error('重试 Tutor 消息失败:', error)
  }
}

function useTutorChip(value: string) {
  tutorInput.value = value
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
  await Promise.all([fetchSessionContext(), loadTrainingClosure(), loadTutorMessages()])
})

onBeforeUnmount(() => {
  clearQuizPoll()
})

watch(
  () => route.params.id,
  async () => {
    clearQuizPoll()
    tutorStore.reset()
    practiceStore.reset()
    await loadTask()
    await Promise.all([fetchSessionContext(), loadTrainingClosure(), loadTutorMessages()])
  },
)
</script>

<template>
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

        <TutorAssistPanel
          class="tutor-main"
          :messages="tutorMessages"
          :loading="tutorLoading"
          :load-error="tutorLoadError"
          :send-error="tutorSendError"
          :sending="tutorSending"
          :input="tutorInput"
          :chips="tutorChips"
          @retry-load="loadTutorMessages"
          @retry-send="handleRetryTutorSend"
          @submit="handleSendTutorMessage"
          @update-input="tutorInput = $event"
          @use-chip="useTutorChip"
        />

        <section v-if="!isTrainingTask" class="content-card">
          <div class="section-head">
            <h2>任务内容</h2>
            <p>先看本次任务，再结合 Tutor 一步步推进。</p>
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
            <h2>这一步要学什么</h2>
            <p>先看清目标，再决定是否开始本章练习。</p>
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
              <p v-else>当前任务内容已准备完成，可以继续进入练习。</p>
            </article>
          </div>
        </section>

        <section v-if="showQuestionSection" ref="questionListRef" class="content-card">
          <div class="section-head">
            <h2>开始做练习</h2>
            <p>按顺序作答，提交后系统会自动生成结果。</p>
          </div>

          <div v-if="practiceItems.length" class="question-list">
            <PracticeQuestionCard
              v-for="(item, index) in practiceItems"
              :key="item.itemId"
              :item="item"
              :index="index"
              :draft="getItemDraft(item.itemId)"
              :submission="getLatestSubmission(item.itemId)"
              :submitting="submittingPracticeItemId === item.itemId"
              @update-draft="setItemDraft(item.itemId, $event)"
              @submit="handleSubmitPractice(item.itemId)"
            />
          </div>
          <div v-else class="inline-state">暂时还没有练习题。</div>
        </section>

        <section v-if="showFeedbackSection" ref="feedbackSectionRef" class="content-card">
          <div class="section-head">
            <h2>查看结果</h2>
            <p>系统已经整理好你的表现和下一步建议。</p>
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
          {{ practiceQueryError || feedbackError || '当前训练流程加载失败，请重试。' }}
        </p>
      </aside>
    </div>
  </main>
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
.side-card p {
  margin: 0;
}

.meta,
.goal,
.section-head p,
.side-card p,
.goal-item p,
.goal-item li {
  color: var(--color-text-secondary);
  line-height: 1.7;
}

.tutor-main {
  min-height: 420px;
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

.goal-item {
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
