<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import ErrorMessage from '@/components/ErrorMessage.vue'
import FeedbackPanel from '@/components/FeedbackPanel.vue'
import LearningProgressStepper from '@/components/LearningProgressStepper.vue'
import PracticeQuestionCard from '@/components/PracticeQuestionCard.vue'
import TrainingActionCard from '@/components/TrainingActionCard.vue'
import TrainingStageHeader from '@/components/TrainingStageHeader.vue'
import TutorAssistPanel from '@/components/TutorAssistPanel.vue'
import {
  buildTrainingSteps,
  deriveTrainingStatuses,
  getTrainingActionContent,
} from '@/constants/trainingStage'
import { usePracticeStore } from '@/stores/practice'
import { useSessionStore } from '@/stores/session'
import { useTutorStore } from '@/stores/tutor'

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
  '这道题的关键思路是什么？',
  '为什么这里要先求齐次解？',
  '特解一般怎么设？',
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
    '训练阶段学习'
  )
})

const stageLabel = computed(() => {
  const map: Record<string, string> = {
    STRUCTURE: '结构构建',
    UNDERSTANDING: '分步学习',
    TRAINING: '训练检测',
    REFLECTION: '复盘总结',
  }
  return task.value?.stage ? map[task.value.stage] || task.value.stage : '训练检测'
})

const sessionCourse = computed(() => sessionStore.currentSession?.courseId || '当前课程')
const sessionChapter = computed(() => sessionStore.currentSession?.chapterId || '当前章节')
const trainingGoal = computed(() => sessionStore.currentSession?.goalText || buildGoalSummary())
const secondaryMeta = computed(() => '训练任务已生成')

const summaryItems = computed(() => [
  {
    label: '当前训练目标',
    value: buildGoalSummary(),
  },
  {
    label: '测验进度',
    value: practiceQuiz.value ? `${practiceQuiz.value.answeredCount}/${practiceQuiz.value.questionCount} 已作答` : '等待生成',
  },
  {
    label: '学习助手',
    value: tutorMessages.value.length > 0 ? `已记录 ${tutorMessages.value.length} 条对话` : '随时可以提问',
  },
])

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
const feedbackPanelMode = computed<'empty' | 'loading' | 'ready'>(() => {
  if (trainingState.value.view === 'feedback_ready') {
    return 'ready'
  }
  if (trainingState.value.view === 'feedback_generating') {
    return 'loading'
  }
  return 'empty'
})

const showGoalSection = computed(
  () => trainingState.value.view === 'quiz_not_generated' || trainingState.value.view === 'quiz_generating',
)
const showQuestionSection = computed(() => trainingState.value.view === 'quiz_ready')
const showFeedbackSection = computed(() => trainingState.value.view === 'feedback_ready')
const tutorAtBottom = computed(() => trainingState.value.view === 'feedback_ready')

function buildGoalSummary() {
  const summaryParts = sections.value
    .flatMap((section) => [section.text, ...(section.bullets ?? []), ...(section.steps ?? []), ...(section.items ?? [])])
    .filter((item): item is string => typeof item === 'string' && item.trim().length > 0)
    .slice(0, 2)
  if (summaryParts.length > 0) {
    return summaryParts.join('；')
  }
  return '围绕当前知识点完成理解梳理、训练作答和反馈复盘。'
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
    // Session context is supplemental for this page.
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
      console.error('轮询测验状态失败:', error)
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
    console.error('提交测验答案失败:', error)
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
    console.error('发送学习助手消息失败:', error)
  }
}

async function handleRetryTutorSend() {
  try {
    await tutorStore.retryLastSend()
    tutorInput.value = ''
  } catch (error) {
    console.error('重试学习助手消息失败:', error)
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
  <main class="training-stage-page">
    <header class="page-toolbar">
      <button type="button" class="ghost-btn" @click="router.back()">返回上一页</button>
      <button type="button" class="ghost-btn" @click="handleContinue">返回会话</button>
    </header>

    <div v-if="isLoading && !task" class="page-state">正在加载训练页面...</div>
    <ErrorMessage v-else-if="loadError && !task" :message="loadError" @retry="handleRetry" />

    <div v-else-if="task" class="page-content">
      <TrainingStageHeader
        :title="taskTitle"
        :course="sessionCourse"
        :chapter="sessionChapter"
        :stage-label="stageLabel"
        :goal="trainingGoal"
        :secondary-meta="secondaryMeta"
        :summary-items="summaryItems"
      />

      <LearningProgressStepper v-if="isTrainingTask" :steps="trainingSteps" />

      <section v-if="!isTrainingTask" class="state-panel">
        <h2>当前任务还不在训练检测阶段</h2>
        <p>训练测验、反馈建议与学习助手闭环会在训练阶段开放。</p>
      </section>

      <template v-else>
        <TrainingActionCard
          :title="actionContent.title"
          :description="actionContent.description"
          :button-text="actionContent.buttonText"
          :loading="actionContent.loading"
          :disabled="actionContent.disabled"
          @action="handlePrimaryAction"
        />

        <section v-if="trainingState.view === 'error'" class="state-panel error">
          <h2>训练状态加载失败</h2>
          <p>{{ practiceQueryError || feedbackError || '请重试当前训练流程。' }}</p>
        </section>

        <section v-if="showGoalSection" class="layout-grid">
          <section class="content-card">
            <div class="section-head">
              <h2>当前训练目标</h2>
              <p>先理解目标，再决定是否生成本章测验。</p>
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
                <p v-else>当前任务内容已准备完成，可以继续进入测验。</p>
              </article>
            </div>
          </section>

          <div class="side-stack">
            <FeedbackPanel :mode="feedbackPanelMode" />
            <TutorAssistPanel
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
          </div>
        </section>

        <section v-if="showQuestionSection" ref="questionListRef" class="layout-grid">
          <section class="content-card">
            <div class="section-head">
              <h2>本章测验</h2>
              <p>请依次完成作答，提交后系统会生成正式反馈。</p>
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
            <div v-else class="state-panel">当前还没有生成测验题</div>
          </section>

          <div class="side-stack compact">
            <FeedbackPanel :mode="feedbackPanelMode" />
            <TutorAssistPanel
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
          </div>
        </section>

        <section v-if="showFeedbackSection" ref="feedbackSectionRef" class="feedback-layout">
          <FeedbackPanel
            mode="ready"
            :report="practiceFeedbackReport"
            @review="handleFeedbackAction('REVIEW')"
            @next-round="handleFeedbackAction('NEXT_ROUND')"
          />

          <TutorAssistPanel
            v-if="tutorAtBottom"
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
        </section>

        <section v-if="trainingState.view === 'feedback_generating'" class="layout-grid">
          <section class="content-card">
            <div class="section-head">
              <h2>作答已提交</h2>
              <p>系统正在分析你的表现，反馈生成后会自动显示在下方。</p>
            </div>
            <FeedbackPanel mode="loading" />
          </section>

          <TutorAssistPanel
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
        </section>
      </template>
    </div>
  </main>
</template>

<style scoped>
.training-stage-page {
  min-height: 100dvh;
  padding: clamp(16px, 2.8vw, 32px);
}

.page-toolbar {
  display: flex;
  justify-content: flex-end;
  gap: var(--space-md);
  margin-bottom: var(--space-xl);
  flex-wrap: wrap;
}

.ghost-btn {
  min-height: 46px;
  padding: 0 16px;
  border-radius: var(--radius-md);
  border: 1px solid rgba(61, 80, 104, 0.56);
  color: var(--color-text-secondary);
  background: rgba(15, 21, 33, 0.9);
}

.page-state,
.state-panel {
  padding: clamp(20px, 3vw, 28px);
  border-radius: var(--radius-xl);
  border: 1px solid rgba(61, 80, 104, 0.5);
  background: rgba(15, 22, 34, 0.9);
  color: var(--color-text-secondary);
}

.state-panel {
  display: grid;
  gap: var(--space-sm);
}

.state-panel.error {
  border-color: rgba(255, 122, 138, 0.4);
}

.page-content {
  width: min(1180px, 100%);
  margin: 0 auto;
  display: grid;
  gap: clamp(18px, 2.4vw, 28px);
}

.layout-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.45fr) minmax(280px, 0.9fr);
  gap: var(--space-lg);
}

.feedback-layout {
  display: grid;
  gap: var(--space-lg);
}

.content-card {
  display: grid;
  gap: var(--space-lg);
  padding: clamp(20px, 3vw, 30px);
  border-radius: var(--radius-xl);
  border: 1px solid rgba(61, 80, 104, 0.48);
  background: linear-gradient(160deg, rgba(18, 26, 39, 0.96), rgba(10, 16, 25, 0.92));
  box-shadow: var(--shadow-sm);
}

.section-head {
  display: flex;
  justify-content: space-between;
  gap: var(--space-md);
  align-items: baseline;
  flex-wrap: wrap;
}

.section-head p {
  color: var(--color-text-secondary);
}

.goal-list,
.question-list,
.side-stack {
  display: grid;
  gap: var(--space-md);
}

.goal-item {
  display: grid;
  gap: 10px;
  padding: var(--space-lg);
  border-radius: var(--radius-lg);
  border: 1px solid rgba(61, 80, 104, 0.42);
  background: rgba(10, 16, 26, 0.8);
}

.goal-item p,
.goal-item li {
  color: var(--color-text-secondary);
  line-height: 1.7;
}

.goal-item ul,
.goal-item ol {
  margin: 0;
  padding-left: 20px;
}

.compact {
  align-content: start;
}

@media (max-width: 980px) {
  .layout-grid {
    grid-template-columns: 1fr;
  }
}
</style>
