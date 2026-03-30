<template>
  <PageContainer>
    <AppTopBar current="task" />
    <main class="mx-auto max-w-[1280px] px-4 py-6 md:px-6 lg:px-8">
      <LoadingState v-if="loading && !task" :message="TASKRUN_COPY.loading" />
      <ErrorState v-else-if="error" :message="error">
        <template #action>
          <SecondaryButton @click="fetchTask">{{ TASKRUN_COPY.retry }}</SecondaryButton>
        </template>
      </ErrorState>
      <EmptyState v-else-if="!task && !loading" :message="TASKRUN_COPY.empty">
        <template #action>
          <SecondaryButton @click="router.push('/report')">{{ TASKRUN_COPY.viewReport }}</SecondaryButton>
        </template>
      </EmptyState>

      <section v-else-if="task" class="pb-28">
        <div class="mx-auto max-w-3xl space-y-5">
          <TaskRunPhaseHeader
            :topic-name="workbenchTopicName"
            :phase-progress="headerPhaseProgress"
            :goal-line="phaseGoalLine"
            :phase-hint="phaseHintLine"
          />

          <div class="space-y-5" :data-phase="pageModel.workbench.emphasisPhase">
            <MainTaskWorkbenchCard
              :model="pageModel.workbench.currentTask"
              :why-this-step="pageModel.workbench.whyThisStep"
              :scaffold-product="pageModel.workbench.scaffoldProduct"
              :hint-reveal="pageModel.workbench.hintReveal"
              :guide-sections="pageModel.workbench.guideSections"
              :emphasis-phase="pageModel.workbench.emphasisPhase"
            />

            <TaskExpressionPanel
              ref="expressionPanelRef"
              :draft-value="draftInput"
              :placeholder="pageModel.mainAction.inputPlaceholder || ''"
              :placeholder-soft="pageModel.tutorConsole.inputPlaceholderSoft"
              :sending="mainActionLoading"
              :helper-text="pageModel.workbench.expressionLayout.helperText"
              :low-friction-prompt="pageModel.workbench.expressionLayout.lowFrictionPrompt"
              :structured-fields="pageModel.workbench.expressionLayout.fields"
              :structured-inputs="structuredInputs"
              :starter-chips="pageModel.mainAction.chips"
              :emphasis-phase="pageModel.workbench.emphasisPhase"
              :checkpoint-prompt="checkpointQuestionDisplay"
              :show-advance="showAdvanceSection"
              :micro-check-labels="microCheckLabels"
              :checks="microChecks"
              @update:draft-value="draftInput = $event"
              @update:structured-inputs="structuredInputs = $event"
              @update:checks="microChecks = $event"
              @chip="fillDraftInput"
            />

            <TaskFeedbackDeck
              :class="feedbackEmphasisClass"
              :model="pageModel.feedback"
              :schema="pageModel.workbench.feedbackSchema"
              @action="handleFeedbackAction"
            />

            <ReflectionSummaryCard v-if="reflectionSummaryForWorkbench" :summary="reflectionSummaryForWorkbench" />
          </div>
        </div>

        <TaskRunDualActionBar
          :primary-label="dualPrimaryLabel"
          :secondary-label="'我还没想清楚'"
          :primary-loading="mainActionLoading || advancing"
          :primary-disabled="dualPrimaryDisabled"
          :secondary-disabled="mainActionLoading || advancing"
          @primary="onPrimaryBar"
          @secondary="onStuckFromPanel"
        />
      </section>
    </main>
  </PageContainer>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import AppTopBar from '@/components/layout/AppTopBar.vue'
import PageContainer from '@/components/layout/PageContainer.vue'
import MainTaskWorkbenchCard from '@/components/task-run/MainTaskWorkbenchCard.vue'
import TaskExpressionPanel from '@/components/task-run/TaskExpressionPanel.vue'

type TaskExpressionPanelExposed = InstanceType<typeof TaskExpressionPanel>
import TaskFeedbackDeck from '@/components/task-run/TaskFeedbackDeck.vue'
import TaskRunDualActionBar from '@/components/task-run/TaskRunDualActionBar.vue'
import TaskRunPhaseHeader from '@/components/task-run/TaskRunPhaseHeader.vue'
import ReflectionSummaryCard from '@/components/task-run/ReflectionSummaryCard.vue'
import EmptyState from '@/components/ui/EmptyState.vue'
import ErrorState from '@/components/ui/ErrorState.vue'
import LoadingState from '@/components/ui/LoadingState.vue'
import SecondaryButton from '@/components/ui/SecondaryButton.vue'
import { getErrorMessage } from '@/api/request'
import {
  completeTask,
  getCurrentTask,
  getCurrentTaskGuidance,
  getTaskScaffold,
  postCheckpoint,
  postSelfExplanation,
  postTaskMessage,
} from '@/api/task'
import { TASKRUN_COPY } from '@/constants/uiCopy'
import { fallbackGuidanceForState, tutorPromptFor } from '@/constants/taskRunUi'
import { showToast } from '@/stores/toast'
import { useAiTutorStore } from '@/stores/aiTutor'
import { useWorkflowStore } from '@/stores/workflow'
import type {
  CheckpointResponse,
  CurrentGuidanceBlock,
  CurrentTaskItem,
  ProgressItem,
  RecommendedUserActionItem,
  SelfExplanationResponse,
  TaskMessageResponse,
  TaskScaffoldResponse,
} from '@/types/dto'
import type { ExecutionGuideFeedbackModel, ExecutionPageViewModel } from '@/types/executionGuide'
import { TaskCompletionStatus, type TaskCompletionStatusType } from '@/types/enums'
import { buildCompleteTaskPayload } from '@/utils/buildCompleteTaskPayload'
import { buildExecutionPageModel, createEmptyWorkbenchModel } from '@/utils/buildExecutionPageModel'
import { buildTaskGuidedSteps, getCurrentGuidedStepId } from '@/utils/taskGuidedSteps'
import type { ReflectionSummary } from '@/types/scaffoldEngine'

interface ChatTurn {
  role: 'USER' | 'ASSISTANT'
  content: string
  detectedAction?: string
}

const route = useRoute()
const router = useRouter()
const store = useWorkflowStore()
const aiTutorStore = useAiTutorStore()

const loading = ref(true)
const completing = ref(false)
const sending = ref(false)
const advancing = ref(false)
const submittingSelf = ref(false)
const submittingCheckpoint = ref(false)
const error = ref<string | null>(null)
const task = ref<CurrentTaskItem | null>(null)
const progress = ref<ProgressItem | null>(null)
const scaffold = ref<TaskScaffoldResponse | null>(null)
const taskState = ref('ORIENT')
const exploreRoundCount = ref(0)
const checkpointQuestion = ref('')
const legacyComplete = ref(false)
const taskStartedAt = ref(Date.now())
const currentGuidance = ref<CurrentGuidanceBlock | null>(null)
const recommendedUserActions = ref<RecommendedUserActionItem[]>([])
const selfExplainMissingPoints = ref<string[]>([])
const chatTurns = ref<ChatTurn[]>([])
const draftInput = ref('')
const structuredInputs = ref<string[]>([])
const latestFeedback = ref<ExecutionGuideFeedbackModel | null>(null)
const microChecks = ref<boolean[]>([false, false, false])
const closureSummary = ref('')
const closurePoint1 = ref('')
const closurePoint2 = ref('')
const closureNext = ref('')
const completeForm = ref<{
  completionStatus: TaskCompletionStatusType
  learnerReflection: string
}>({
  completionStatus: TaskCompletionStatus.COMPLETED,
  learnerReflection: '',
})

const expressionPanelRef = ref<TaskExpressionPanelExposed | null>(null)

const guidedStepsList = computed(() => buildTaskGuidedSteps(legacyComplete.value, scaffold.value))
const guidedStepId = computed(() =>
  getCurrentGuidedStepId(taskState.value, exploreRoundCount.value, legacyComplete.value)
)
const guidedStepPosition = computed(() => {
  const steps = guidedStepsList.value
  const idx = steps.findIndex((step) => step.id === guidedStepId.value)
  return {
    current: idx >= 0 ? idx + 1 : 1,
    total: Math.max(steps.length, 1),
  }
})

const EMPTY_PAGE_MODEL: ExecutionPageViewModel = {
  currentStepIndex: 1,
  currentStepTitle: '',
  header: {
    phaseCode: '',
    phaseDisplayZh: '',
    anchorActionLine: '',
    heroTitle: '',
    heroSubtitle: '',
    completionCriteria: [],
    metaLine: '',
    title: '',
    stageLabel: '',
    stepLabel: '1/1',
    estimatedTime: '',
    subtitle: '',
    trackTitle: '',
    trackSubtitle: '',
    knowledgePoints: [],
    operationConsole: {
      knowledgePointName: '',
      knowledgePointType: 'CONCEPT',
      roundGoal: '',
      completionStandardLines: [],
      estimatedTimeLabel: '',
    },
  },
  mainAction: {
    mode: 'guided-input',
    phaseCode: '',
    phaseDisplayZh: '',
    eyebrow: '',
    title: '',
    description: '',
    inputLabel: '',
    inputPlaceholder: '',
    primaryActionLabel: '',
    chips: [],
    focusLabel: '',
    focusTitle: '',
    focusObjective: '',
    focusReason: '',
    focusTips: [],
  },
  feedback: {
    visible: false,
    title: '当前反馈',
    mastered: '',
    gap: '',
    nextStep: '',
    actions: [],
  },
  progressRail: {
    stageSectionTitle: '',
    stageLabel: '',
    deliverableSectionTitle: '',
    deliverableLine: '',
    stuckSectionTitle: '',
    stuckActions: [],
    nextSectionTitle: '',
    nextPreview: '',
    knowledgeOutline: [],
  },
  helpSections: [],
  scaffoldCards: [],
  tutorConsole: {
    currentDirective: '',
    inputPlaceholderSoft: '',
    stageDisplay: '',
    currentDeliverable: '',
    stuckActions: [],
    phasePromptChips: [],
  },
  workbench: createEmptyWorkbenchModel(),
}

const pageModel = computed<ExecutionPageViewModel>(() => {
  if (!task.value) return EMPTY_PAGE_MODEL
  return buildExecutionPageModel({
    task: task.value,
    progress: progress.value,
    planTasks: store.planPreview?.tasks ?? [],
    scaffold: scaffold.value,
    taskState: taskState.value,
    guidedStepCurrent: guidedStepPosition.value.current,
    guidedStepTotal: guidedStepPosition.value.total,
    currentGuidance: currentGuidance.value,
    recommendedUserActions: recommendedUserActions.value,
    latestFeedback: latestFeedback.value,
    checkpointQuestion: checkpointQuestion.value,
    selfExplainMissingPoints: selfExplainMissingPoints.value,
    legacyComplete: legacyComplete.value,
    structuredGoal: store.structuredGoal,
    plan: store.planPreview,
    canAdvanceDriving: canAdvanceDriving.value,
  })
})

const reflectionSummaryForWorkbench = computed((): ReflectionSummary | null => scaffold.value?.reflectionSummary ?? null)

const headerPhaseProgress = computed(() => {
  const api = scaffold.value?.phaseProgress
  if (api?.phases?.length) {
    return {
      phases: api.phases as typeof pageModel.value.workbench.phaseProgress.phases,
      currentPhase: api.currentPhase as typeof pageModel.value.workbench.phaseProgress.currentPhase,
      overallRatio: api.overallRatio,
      taskIndexLabel: api.taskIndexLabel,
      stepLabel: api.stepLabel,
    }
  }
  return pageModel.value.workbench.phaseProgress
})

const workbenchTopicName = computed(() => {
  const w = pageModel.value.workbench.topicHints.topicDisplayName?.trim()
  if (w) return w
  return pageModel.value.header.operationConsole?.knowledgePointName || pageModel.value.header.title || '当前任务'
})

const phaseGoalLine = computed(() => {
  const s = pageModel.value.header.subtitle?.trim()
  if (s) return s
  return pageModel.value.workbench.stageMini.untilNextPhase?.trim() || pageModel.value.header.anchorActionLine || ''
})

const phaseHintLine = computed(() => pageModel.value.workbench.stageRules.rules[0] || '')

const checkpointQuestionDisplay = computed(() => {
  if (taskState.value !== 'CHECK') return ''
  return checkpointQuestion.value.trim()
})

const microCheckLabels = computed(() => {
  const raw = scaffold.value?.selfCheckTemplates?.filter((s) => s?.trim()).slice(0, 3) ?? []
  if (raw.length >= 2) return raw
  return ['我能定位它在解决什么', '我能说出关键机制或依据', '我能用一句话收束这一步']
})

watch(
  microCheckLabels,
  (labels) => {
    microChecks.value = labels.map(() => false)
  },
  { immediate: true }
)

watch(
  () => pageModel.value.workbench.expressionLayout.fields,
  (fields) => {
    if (pageModel.value.workbench.emphasisPhase === 'STRUCTURE') {
      structuredInputs.value = []
      return
    }
    structuredInputs.value = fields.map((_, index) => structuredInputs.value[index] ?? '')
  },
  { immediate: true }
)

watch(
  structuredInputs,
  (parts) => {
    if (pageModel.value.workbench.emphasisPhase === 'STRUCTURE') return
    const fields = pageModel.value.workbench.expressionLayout.fields
    const merged = parts
      .map((value, index) => {
        const trimmed = value.trim()
        if (!trimmed) return ''
        const label = fields[index]?.label || `字段 ${index + 1}`
        return `${label}：${trimmed}`
      })
      .filter(Boolean)
      .join('\n')
    if (merged) draftInput.value = merged
  },
  { deep: true }
)

watch(
  () => task.value?.taskId,
  () => {
    if (!task.value?.taskId) return
    nextTick(() => expressionPanelRef.value?.focus())
  },
  { flush: 'post' }
)

const showAdvanceSection = computed(() => taskState.value === 'EXPLORE' && chatTurns.value.length >= 1)
const canAdvanceDriving = computed(() => microChecks.value.length > 0 && microChecks.value.every(Boolean))

const primaryShowsAdvance = computed(() => showAdvanceSection.value && canAdvanceDriving.value)

const dualPrimaryLabel = computed(() => {
  if (primaryShowsAdvance.value) {
    return scaffold.value?.actionBar?.nextActionLabel || TASKRUN_COPY.advancePhase
  }
  if (pageModel.value.mainAction.mode === 'closure') return '完成本任务'
  return (
    scaffold.value?.actionBar?.submitActionLabel ||
    pageModel.value.mainAction.primaryActionLabel ||
    '提交本轮表达'
  )
})

const dualPrimaryDisabled = computed(() => {
  if (mainActionLoading.value || advancing.value) return true
  if (pageModel.value.mainAction.mode === 'closure') return false
  if (primaryShowsAdvance.value) return false
  return !draftInput.value.trim()
})

const feedbackEmphasisClass = computed(() => {
  const ph = pageModel.value.workbench.emphasisPhase
  if (ph === 'REFLECTION' || ph === 'TRAINING') return 'ring-2 ring-emerald-200/60 rounded-2xl p-1'
  return ''
})

const mainActionLoading = computed(() => {
  const mode = pageModel.value.mainAction.mode
  if (mode === 'closure') return completing.value
  if (taskState.value === 'CHECK') return submittingCheckpoint.value
  if (taskState.value === 'SELF_EXPLAIN' || taskState.value === 'REMEDIAL') return submittingSelf.value
  return sending.value
})

watch(
  [task, () => pageModel.value.workbench.tutorAssist, () => pageModel.value.workbench.currentTask.currentAction],
  () => {
    if (!task.value) return
    aiTutorStore.setContext({
      stepId: task.value.taskId,
      step: guidedStepPosition.value.current,
      knowledgeKey: scaffold.value?.packId || store.planPreview?.knowledgeKey || 'unknown',
      knowledgeLabel: workbenchTopicName.value,
      phaseCode: pageModel.value.header.phaseCode || 'STRUCTURE',
      phaseLabel: pageModel.value.header.phaseDisplayZh || '',
      currentAction: pageModel.value.workbench.currentTask.currentAction,
      floatingLabel: pageModel.value.workbench.tutorAssist.floatingLabel,
      panelTitle: pageModel.value.workbench.tutorAssist.panelTitle,
      quickQuestions: pageModel.value.workbench.tutorAssist.quickQuestions,
    })
  },
  { immediate: true }
)

function draftStorageKey(taskId: string) {
  return `task-exec-draft-${taskId}`
}

let draftPersistTimer: ReturnType<typeof setTimeout> | null = null
watch(draftInput, (v) => {
  const id = task.value?.taskId
  if (!id) return
  if (draftPersistTimer) clearTimeout(draftPersistTimer)
  draftPersistTimer = setTimeout(() => {
    localStorage.setItem(draftStorageKey(id), v)
  }, 450)
})

function firstLine(text: string, fallback: string) {
  const line = text.split('\n').map((item) => item.trim()).find(Boolean)
  return line || fallback
}

function feedbackFromBoard(
  title: string,
  board?: TaskMessageResponse['feedbackBoard']
): ExecutionGuideFeedbackModel | null {
  if (!board) return null
  return {
    visible: true,
    title,
    mastered: board.correct || '',
    strengths: board.correct || '',
    gap: board.missing || '',
    keyIssues: board.missing ? [board.missing] : [],
    errorTags: board.confused ? [board.confused] : [],
    nextRestateAsk: board.nextFix || '',
    nextStep: board.nextFix || '',
    actions: board.actions ?? [],
  }
}

function buildMessageFeedback(userInput: string, response: TaskMessageResponse): ExecutionGuideFeedbackModel {
  const fromBoard = feedbackFromBoard('本轮反馈', response.feedbackBoard)
  if (fromBoard) return fromBoard
  return {
    visible: true,
    title: '本轮反馈',
    mastered: `你已经开始围绕当前任务输出：${firstLine(userInput, '已提交当前表达。')}`,
    strengths: `你已经开始围绕当前任务输出：${firstLine(userInput, '已提交当前表达。')}`,
    gap: firstLine(response.nextSuggestedPrompts?.[0] || '', '再补一句关键因果或判断依据。'),
    keyIssues: [firstLine(response.nextSuggestedPrompts?.[0] || '', '再补一句关键因果或判断依据。')],
    errorTags: ['当前动作还没收束'],
    nextRestateAsk: firstLine(response.nextSuggestedPrompts?.[0] || '', '顺着当前提示继续补一小段。'),
    nextStep: firstLine(response.nextSuggestedPrompts?.[0] || '', '顺着当前提示继续补一小段。'),
    actions: [
      { id: 'apply_suggestion', label: '补这一处' },
      { id: 'restate', label: '重新表达' },
      { id: 'show_example', label: '看例子' },
    ],
  }
}

function buildSelfExplainFeedback(response: SelfExplanationResponse): ExecutionGuideFeedbackModel {
  return (
    feedbackFromBoard('本轮反馈', response.feedbackBoard) || {
      visible: true,
      title: '本轮反馈',
      mastered: response.evaluation === 'WEAK' ? '方向对了，但还差关键补充。' : '这一步已经讲清主要关系了。',
      strengths: response.evaluation === 'WEAK' ? '方向对了，但还差关键补充。' : '这一步已经讲清主要关系了。',
      gap: firstLine(response.missingPoints?.[0] || '', '只补当前最关键的一句。'),
      keyIssues: response.missingPoints?.slice(0, 2) || [],
      errorTags: ['表达缺口'],
      nextRestateAsk: response.nextAction || '只补当前缺口，再重新表达。',
      nextStep: response.nextAction || '只补当前缺口，再重新表达。',
      actions: [
        { id: 'apply_suggestion', label: '补这一处' },
        { id: 'restate', label: '重新表达' },
      ],
    }
  )
}

function buildCheckpointFeedback(response: CheckpointResponse): ExecutionGuideFeedbackModel {
  return (
    feedbackFromBoard('本轮反馈', response.feedbackBoard) || {
      visible: true,
      title: '本轮反馈',
      mastered: response.result === 'FAIL' ? '前面的准备已经够了。' : '这道检查题已经通过。',
      strengths: response.result === 'FAIL' ? '前面的准备已经够了。' : '这道检查题已经通过。',
      gap: response.reason || response.suggestedRemedialAction || '',
      keyIssues: response.reason ? [response.reason] : [],
      errorTags: response.result === 'FAIL' ? ['判断依据不够完整'] : [],
      nextRestateAsk: response.suggestedRemedialAction || '回到主卡，把判断依据补成一句完整的话。',
      nextStep: response.suggestedRemedialAction || '回到主卡，把判断依据补成一句完整的话。',
      actions: response.result === 'FAIL' ? [{ id: 'apply_suggestion', label: '补这一处' }] : [],
    }
  )
}

function resetClosureFields() {
  closureSummary.value = ''
  closurePoint1.value = ''
  closurePoint2.value = ''
  closureNext.value = ''
  completeForm.value = {
    completionStatus: TaskCompletionStatus.COMPLETED,
    learnerReflection: '',
  }
}

function resetInteractionDraft() {
  draftInput.value = ''
  structuredInputs.value = []
  latestFeedback.value = null
}

function fillDraftInput(text: string) {
  const value = text.trim()
  if (!value) return
  draftInput.value = draftInput.value.trim() ? `${draftInput.value.trim()}\n${value}` : value
}

function onStuckFromPanel() {
  const actions = pageModel.value.progressRail.stuckActions
  if (actions.length) {
    onStuckAction(actions[0]!)
    return
  }
  fillDraftInput('我卡住了：请给我下一步最短提示。')
}

function onStuckAction(action: string) {
  const focus = task.value?.title || scaffold.value?.learningObjective || '当前任务'
  if (action === '看例子') {
    fillDraftInput(tutorPromptFor('minimal_example', focus))
    return
  }
  if (action === '看对比') {
    fillDraftInput(tutorPromptFor('concept_compare', focus))
    return
  }
  if (action === '请求简化') {
    fillDraftInput('请用更短、更少术语的方式提示我这一步。')
    return
  }
  fillDraftInput(action)
}

function applyFallbackGuidance() {
  const fallback = fallbackGuidanceForState(taskState.value, false)
  currentGuidance.value = fallback.guidance
  recommendedUserActions.value = fallback.actions
}

async function loadGuidance() {
  if (!store.sessionId) return
  try {
    const data = await getCurrentTaskGuidance(store.sessionId)
    currentGuidance.value = data.currentGuidance ?? null
    recommendedUserActions.value = data.recommendedUserActions ?? []
  } catch {
    applyFallbackGuidance()
  }
}

function syncRuntimeFromScaffold(data: TaskScaffoldResponse) {
  taskState.value = data.executionSnapshot?.currentState || data.currentExecutionState || 'ORIENT'
  exploreRoundCount.value = data.executionSnapshot?.exploreTurnCount || 0
  checkpointQuestion.value = data.executionSnapshot?.checkpointQuestion || ''
}

function isUiChatMessage(
  message: { role: 'USER' | 'ASSISTANT' | 'SYSTEM'; content: string; detectedAction?: string }
): message is ChatTurn {
  return message.role === 'USER' || message.role === 'ASSISTANT'
}

async function fetchTask() {
  if (!store.sessionId) return
  loading.value = true
  error.value = null
  try {
    const data = await getCurrentTask(store.sessionId)
    store.currentTask = data.currentTask
    store.progress = data.progress
    task.value = data.currentTask
    progress.value = data.progress

    if (!data.currentTask) {
      store.currentTaskId = null
      router.push('/report')
      return
    }

    store.currentTaskId = data.currentTask.taskId
    const routeTaskId = typeof route.params.taskId === 'string' ? route.params.taskId : ''
    if (route.name === 'task' || (route.name === 'taskRun' && routeTaskId && routeTaskId !== data.currentTask.taskId)) {
      router.replace({ name: 'taskRun', params: { taskId: data.currentTask.taskId } })
    }

    await loadScaffold(data.currentTask.taskId)
  } catch (err) {
    error.value = getErrorMessage(err)
  } finally {
    loading.value = false
  }
}

async function loadScaffold(taskId: string) {
  if (!store.sessionId) return
  resetClosureFields()
  resetInteractionDraft()
  taskStartedAt.value = Date.now()
  try {
    const data = await getTaskScaffold(taskId, store.sessionId)
    scaffold.value = data
    legacyComplete.value = false
    syncRuntimeFromScaffold(data)
    const saved = localStorage.getItem(draftStorageKey(taskId))
    if (saved) draftInput.value = saved
    chatTurns.value = (data.recentMessages || [])
      .filter(isUiChatMessage)
      .map((message) => ({
        role: message.role,
        content: message.content,
        detectedAction: message.detectedAction || undefined,
      }))
    selfExplainMissingPoints.value = []
    await loadGuidance()
  } catch {
    scaffold.value = null
    legacyComplete.value = true
    taskState.value = 'PASS'
    applyFallbackGuidance()
    showToast('当前任务先切到简化模式。')
  }
}

async function sendMessageWithContent(content: string): Promise<TaskMessageResponse | null> {
  if (!store.sessionId || !task.value) return null
  const text = content.trim()
  if (!text) return null

  sending.value = true
  try {
    const response = await postTaskMessage(task.value.taskId, store.sessionId, text)
    chatTurns.value.push({ role: 'USER', content: text, detectedAction: response.detectedAction })
    chatTurns.value.push({ role: 'ASSISTANT', content: response.assistantReply })
    draftInput.value = ''
    taskState.value = response.taskState
    if (response.taskState === 'EXPLORE') exploreRoundCount.value += 1
    await loadGuidance()
    latestFeedback.value = buildMessageFeedback(text, response)
    return response
  } catch (err) {
    showToast(getErrorMessage(err))
    return null
  } finally {
    sending.value = false
  }
}

async function sendMessage() {
  await sendMessageWithContent(draftInput.value)
}

async function onAdvanceDrivingSeat() {
  if (!canAdvanceDriving.value) return
  const body = [
    ...structuredInputs.value.filter((item) => item.trim()),
    '我认为当前阶段已经可以进入下一步，请继续推进。',
  ].join('\n')
  advancing.value = true
  try {
    const res = await sendMessageWithContent(body)
    if (res) microChecks.value = microCheckLabels.value.map(() => false)
  } finally {
    advancing.value = false
  }
}

async function submitSelfExplanation() {
  if (!store.sessionId || !task.value) return
  const content = draftInput.value.trim()
  if (!content) return

  submittingSelf.value = true
  try {
    const response = await postSelfExplanation(task.value.taskId, store.sessionId, content)
    chatTurns.value.push({ role: 'USER', content })
    taskState.value = response.taskState
    checkpointQuestion.value = response.checkpointQuestion || ''
    selfExplainMissingPoints.value = response.missingPoints ?? []
    draftInput.value = ''
    latestFeedback.value = buildSelfExplainFeedback(response)
    await loadGuidance()
    showToast(response.evaluation === 'WEAK' ? '先补这一处，再试一次。' : '这一步可以继续了。')
  } catch (err) {
    showToast(getErrorMessage(err))
  } finally {
    submittingSelf.value = false
  }
}

async function submitCheckpoint() {
  if (!store.sessionId || !task.value) return
  const answer = draftInput.value.trim()
  if (!answer) return

  submittingCheckpoint.value = true
  try {
    const response = await postCheckpoint(task.value.taskId, store.sessionId, answer)
    chatTurns.value.push({ role: 'USER', content: answer })
    taskState.value = response.taskState
    draftInput.value = ''
    latestFeedback.value = buildCheckpointFeedback(response)
    await loadGuidance()
    showToast(response.result === 'FAIL' ? response.reason || '还差一点，补一句再试。' : '这一步过了，继续收束。')
  } catch (err) {
    showToast(getErrorMessage(err))
  } finally {
    submittingCheckpoint.value = false
  }
}

async function onComplete() {
  if (!store.sessionId || !task.value) return

  if (!closureSummary.value.trim()) closureSummary.value = structuredInputs.value[0]?.trim() || draftInput.value.trim()
  if (!closurePoint1.value.trim()) closurePoint1.value = structuredInputs.value[1]?.trim() || microCheckLabels.value[0] || ''
  if (!closurePoint2.value.trim()) closurePoint2.value = structuredInputs.value[2]?.trim() || microCheckLabels.value[1] || ''
  if (!closureNext.value.trim()) {
    closureNext.value =
      structuredInputs.value[3]?.trim() ||
      pageModel.value.feedback.nextStep ||
      '继续下一任务前先复盘这一步。'
  }

  if (!legacyComplete.value) {
    if (closureSummary.value.trim().length < 10) {
      showToast('请先写一句总结。')
      return
    }
    if (!closurePoint1.value.trim() || !closurePoint2.value.trim()) {
      showToast('请写下两个要点。')
      return
    }
  }

  completing.value = true
  try {
    const payload = buildCompleteTaskPayload({
      sessionId: store.sessionId,
      completionStatus: completeForm.value.completionStatus,
      legacyComplete: legacyComplete.value,
      summaryText: closureSummary.value,
      learnedPoint1: closurePoint1.value,
      learnedPoint2: closurePoint2.value,
      nextPracticeIntent: closureNext.value,
      learnerReflection: completeForm.value.learnerReflection,
      taskStartedAt: taskStartedAt.value,
      userMessageCount: chatTurns.value.filter((turn) => turn.role === 'USER').length,
    })
    const data = await completeTask(task.value.taskId, payload)
    store.currentTask = null
    resetClosureFields()

    if (data.nextTaskAvailable && data.nextTaskId) {
      store.currentTaskId = data.nextTaskId
      await fetchTask()
    } else {
      router.push('/report')
    }
  } catch (err) {
    showToast(getErrorMessage(err))
  } finally {
    completing.value = false
  }
}

async function handlePrimaryAction() {
  const mode = pageModel.value.mainAction.mode
  if (mode === 'closure') {
    await onComplete()
    return
  }
  if (taskState.value === 'CHECK') {
    await submitCheckpoint()
    return
  }
  if (taskState.value === 'SELF_EXPLAIN' || taskState.value === 'REMEDIAL') {
    await submitSelfExplanation()
    return
  }
  await sendMessage()
}

async function onPrimaryBar() {
  if (primaryShowsAdvance.value) {
    await onAdvanceDrivingSeat()
    return
  }
  await handlePrimaryAction()
}

function handleFeedbackAction(actionId: string) {
  if (actionId === 'apply_suggestion') {
    fillDraftInput(latestFeedback.value?.gap || latestFeedback.value?.nextStep || '')
    return
  }
  if (actionId === 'restate') {
    draftInput.value = latestFeedback.value?.nextRestateAsk || latestFeedback.value?.nextStep || ''
    return
  }
  if (actionId === 'show_example') {
    fillDraftInput(tutorPromptFor('minimal_example', task.value?.title || scaffold.value?.learningObjective || '当前任务'))
  }
}

onMounted(() => {
  fetchTask()
})
</script>
