<template>
  <PageContainer>
    <AppTopBar current="task" />
    <main class="mx-auto max-w-execution px-4 py-6 md:px-6 lg:px-8">
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

      <section v-else-if="task" ref="executionSectionRef" class="pb-28">
        <div class="space-y-5" :data-phase="currentPhase">
          <ExecutionWorkbenchPage
            :vm="workbenchVm"
            :scaffold-buttons="scaffoldButtons"
            @exit="router.push('/plan')"
            @structure:pick="handleStructurePick"
            @structure:next="handleStructureNext"
            @understanding:send="handleUnderstandingSend"
            @understanding:draft="understandingState.draftInput = $event"
            @understanding:inject="handleUnderstandingInject"
            @training:send="handleTrainingSend"
            @training:draft="trainingState.draftInput = $event"
            @reflection:toggle-strategy="handleReflectionToggle"
            @reflection:text="reflectionState.userReflectionText = $event"
          />
        </div>
        <BottomActionBar
          :primary-label="nextActionLabel"
          :primary-disabled="currentPhase !== 'reflection' && !canGoNext"
          :primary-loading="nextActionLoading"
          @primary="goNextPhase"
        />
      </section>
    </main>
  </PageContainer>
</template>

<script setup lang="ts">
import { computed, nextTick, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import AppTopBar from '@/components/layout/AppTopBar.vue'
import PageContainer from '@/components/layout/PageContainer.vue'
import ExecutionWorkbenchPage from '@/components/task-run/ExecutionWorkbenchPage.vue'
import BottomActionBar from '@/components/task-run/BottomActionBar.vue'
import EmptyState from '@/components/ui/EmptyState.vue'
import ErrorState from '@/components/ui/ErrorState.vue'
import LoadingState from '@/components/ui/LoadingState.vue'
import SecondaryButton from '@/components/ui/SecondaryButton.vue'
import { getErrorMessage } from '@/api/request'
import { postCompleteConversationStage, postCompleteStructureStage } from '@/api/learningScaffold'
import { getSessionFlowState } from '@/api/session'
import { streamAiTutorChat, type AiTutorChatMessagePayload } from '@/api/tutor'
import { completeTask } from '@/api/task'
import { supportsLearningScaffoldEngine } from '@/constants/learningScaffoldPack'
import { TASKRUN_COPY } from '@/constants/uiCopy'
import {
  DFS_BFS_CONFUSION_POINTS,
  DFS_BFS_NEXT_LABELS,
  DFS_BFS_PHASE_GOALS,
  DFS_BFS_REFLECTION_STRATEGIES,
  DFS_BFS_SCAFFOLD_BUTTONS,
  DFS_BFS_STRUCTURE_QUESTIONS,
  TRAINING_SYSTEM_OPENER,
  UNDERSTANDING_SYSTEM_OPENER,
} from '@/constants/dfsBfsExecutionConfig'
import { showToast } from '@/stores/toast'
import { useWorkflowStore } from '@/stores/workflow'
import { useLearningScaffoldEngine } from '@/composables/useLearningScaffoldEngine'
import { useTaskRunSession } from '@/composables/useTaskRunSession'
import { buildCompleteTaskPayload } from '@/utils/buildCompleteTaskPayload'
import type { CurrentTaskData, ProgressItem } from '@/types/dto'
import type {
  ChatMessage,
  ExecutionWorkbenchVm,
  PhaseKey,
  ReflectionPhaseState,
  StructurePhaseState,
  TrainingPhaseState,
  UnderstandingPhaseState,
} from '@/types/executionWorkbench'
import {
  createEmptyReflectionState,
  createEmptyStructureState,
  createEmptyTrainingState,
  createEmptyUnderstandingState,
  nextPhase,
  PHASE_CODE_TO_KEY,
  PHASE_KEY_TO_CODE,
  PHASE_SEQUENCE,
} from '@/types/executionWorkbench'

const route = useRoute()
const router = useRouter()
const store = useWorkflowStore()

const loading = ref(true)
const error = ref<string | null>(null)
const task = ref<CurrentTaskData | null>(null)
const progress = ref<ProgressItem | null>(null)
/** 与后端引擎当前阶段对齐，供 GET .../scaffold?stage= 使用 */
const engineStageKeyHint = ref<string | null>(null)
const advancingPhase = ref(false)
const latestStructureSubmit = ref<Promise<unknown> | null>(null)
const executionSectionRef = ref<HTMLElement | null>(null)
const taskStartedAt = ref(Date.now())

const structureState = reactive<StructurePhaseState>(
  createEmptyStructureState(DFS_BFS_STRUCTURE_QUESTIONS),
)
const understandingState = reactive<UnderstandingPhaseState>(createEmptyUnderstandingState())
const trainingState = reactive<TrainingPhaseState>(createEmptyTrainingState())
const reflectionState = reactive<ReflectionPhaseState>(
  createEmptyReflectionState(DFS_BFS_REFLECTION_STRATEGIES),
)

const PHASE_QUERY_TO_KEY: Record<string, PhaseKey> = {
  structure: 'structure',
  understanding: 'understanding',
  training: 'training',
  reflection: 'reflection',
}

function resolveEnginePhase(): PhaseKey | null {
  const stageKey = scaffoldEngine.stage?.stageKey
  if (!stageKey) return null
  return PHASE_CODE_TO_KEY[stageKey as keyof typeof PHASE_CODE_TO_KEY] ?? null
}

async function syncRoutePhase(phase: PhaseKey) {
  if (currentPhase.value === phase) return
  await router.replace({ query: { ...route.query, phase } })
}

function resetWorkbenchState() {
  Object.assign(structureState, createEmptyStructureState(DFS_BFS_STRUCTURE_QUESTIONS))
  Object.assign(understandingState, createEmptyUnderstandingState())
  Object.assign(trainingState, createEmptyTrainingState())
  Object.assign(reflectionState, createEmptyReflectionState(DFS_BFS_REFLECTION_STRATEGIES))
  latestStructureSubmit.value = null
  advancingPhase.value = false
  engineStageKeyHint.value = null
}

function scrollExecutionIntoView() {
  nextTick(() => {
    executionSectionRef.value?.scrollIntoView({ behavior: 'smooth', block: 'start' })
  })
}

const phaseFromQuery = computed<PhaseKey>(() => {
  const raw = typeof route.query.phase === 'string' ? route.query.phase.toLowerCase() : ''
  return PHASE_QUERY_TO_KEY[raw] ?? 'structure'
})

const currentPhase = computed<PhaseKey>(() => phaseFromQuery.value)

const scaffoldEngine = useLearningScaffoldEngine({
  taskId: () => task.value?.taskId,
  sessionId: () => store.sessionId,
  stageApiKey: () =>
    engineStageKeyHint.value ??
    task.value?.currentStage ??
    PHASE_KEY_TO_CODE[currentPhase.value],
  enabled: () =>
    supportsLearningScaffoldEngine(task.value?.knowledge ?? store.planPreview?.packId) &&
    !!task.value &&
    !loading.value,
})

watch(
  () => scaffoldEngine.stage?.stageKey,
  (k) => {
    if (k) engineStageKeyHint.value = k
  },
)

const { fetchTask } = useTaskRunSession({
  route,
  router,
  store,
  scaffoldEngine,
  task,
  progress,
  engineStageKeyHint,
  taskStartedAt,
  loading,
  error,
  onTaskIdentityChange: resetWorkbenchState,
})

const scaffoldButtons = computed(() => DFS_BFS_SCAFFOLD_BUTTONS)
const nextActionLabel = computed(() => DFS_BFS_NEXT_LABELS[currentPhase.value])
const currentPhaseBusy = computed(() => {
  switch (currentPhase.value) {
    case 'structure':
    case 'reflection':
      return scaffoldEngine.submitting || scaffoldEngine.loading
    case 'understanding':
      return understandingState.streaming
    case 'training':
      return trainingState.streaming
    default:
      return false
  }
})
/** REFLECTION：不等待脚手架 loading/submitting，主按钮可随时收口进报告 */
const nextActionLoading = computed(() => {
  if (currentPhase.value === 'reflection') {
    return advancingPhase.value
  }
  return advancingPhase.value || currentPhaseBusy.value
})

/** 演示友好：一句话即可；不依赖导师 XML 中的 can_proceed / final_draft */
function lastTrainingUserUtterance(): string {
  for (let i = trainingState.messages.length - 1; i >= 0; i--) {
    const m = trainingState.messages[i]
    if (m.role === 'user' && m.content.trim().length > 0) {
      return m.content.trim()
    }
  }
  return ''
}

function effectiveTrainingFinalDraft(): string {
  const fd = trainingState.finalDraft?.trim()
  if (fd) return fd
  return lastTrainingUserUtterance()
}

const canGoNext = computed(() => {
  switch (currentPhase.value) {
    case 'structure':
      return !latestStructureSubmit.value && !scaffoldEngine.submitting
    case 'understanding':
      return !understandingState.streaming
    case 'training':
      return !trainingState.streaming
    case 'reflection':
      return true
    default:
      return false
  }
})

/** 仅处理非法或缺失的 phase query；合法 phase 由脚手架引擎 stageKey 同步（见下） */
watch(
  () => route.query.phase,
  () => {
    const raw = typeof route.query.phase === 'string' ? route.query.phase.toLowerCase() : ''
    if (raw && !PHASE_QUERY_TO_KEY[raw]) {
      void router.replace({ query: { ...route.query, phase: resolveEnginePhase() ?? 'structure' } })
      return
    }
    if (!raw) {
      void router.replace({ query: { ...route.query, phase: 'structure' } })
    }
  },
  { immediate: true },
)

/** 脚手架引擎为阶段权威来源：与 URL 不一致时对齐（不再由 getTaskScaffold 写 URL） */
watch(
  () => scaffoldEngine.stage?.stageKey,
  async (stageKey) => {
    if (!stageKey) return
    const phaseKey = PHASE_CODE_TO_KEY[stageKey as keyof typeof PHASE_CODE_TO_KEY]
    if (!phaseKey || phaseKey === currentPhase.value) return
    await router.replace({ query: { ...route.query, phase: phaseKey } })
  },
)

watch(
  () => (typeof route.query.phase === 'string' ? route.query.phase : 'structure'),
  (_p, oldPhase) => {
    if (oldPhase === undefined) return
    scrollExecutionIntoView()
  },
)

watch(
  [() => task.value?.taskId, currentPhase],
  () => {
    ensureConversationState(currentPhase.value)
  },
  { immediate: true },
)

watch(
  () => [
    task.value?.taskId,
    understandingState.messages,
    understandingState.draftInput,
    understandingState.turnCount,
    understandingState.canProceed,
    understandingState.completionHint,
    understandingState.stageSummary,
  ],
  () => {
    persistConversationState('understanding')
  },
  { deep: true },
)

watch(
  () => [
    task.value?.taskId,
    trainingState.messages,
    trainingState.draftInput,
    trainingState.roundCount,
    trainingState.canProceed,
    trainingState.finalDraft,
    trainingState.completionHint,
    trainingState.stageSummary,
  ],
  () => {
    persistConversationState('training')
  },
  { deep: true },
)

async function goNextPhase() {
  if (!task.value || !store.sessionId) return

  // REFLECTION：直接收尾并进入报告，不与引擎 stageKey / URL 对齐纠缠
  if (currentPhase.value === 'reflection') {
    advancingPhase.value = true
    try {
      await completeCurrentTaskAndAdvance()
    } catch (err) {
      showToast(getErrorMessage(err))
    } finally {
      advancingPhase.value = false
    }
    return
  }

  if (!canGoNext.value) return
  advancingPhase.value = true
  try {
    const enginePhaseBeforeSubmit = resolveEnginePhase()
    if (enginePhaseBeforeSubmit && enginePhaseBeforeSubmit !== currentPhase.value) {
      if (enginePhaseBeforeSubmit === 'reflection') {
        buildReflectionSummary()
      }
      await syncRoutePhase(enginePhaseBeforeSubmit)
      ensureConversationState(enginePhaseBeforeSubmit)
      return
    }

    if (currentPhase.value === 'structure') {
      if (latestStructureSubmit.value) {
        await latestStructureSubmit.value
      }
      const enginePhaseAfterSync = resolveEnginePhase()
      if (enginePhaseAfterSync && enginePhaseAfterSync !== 'structure') {
        await syncRoutePhase(enginePhaseAfterSync)
        ensureConversationState(enginePhaseAfterSync)
        return
      }
      await autoCompleteStructureForDemo()
      const csr = await postCompleteStructureStage(task.value.taskId, {
        sessionId: store.sessionId,
      })
      engineStageKeyHint.value = csr.nextStageKey
      await scaffoldEngine.loadStage({ force: true })
    }

    if (currentPhase.value === 'understanding') {
      const ucr = await postCompleteConversationStage(task.value.taskId, {
        sessionId: store.sessionId,
        stageKey: 'UNDERSTANDING',
      })
      engineStageKeyHint.value = ucr.nextStageKey
      await scaffoldEngine.loadStage({ force: true })
    }

    if (currentPhase.value === 'training') {
      const tcr = await postCompleteConversationStage(task.value.taskId, {
        sessionId: store.sessionId,
        stageKey: 'TRAINING',
        finalDraft: effectiveTrainingFinalDraft() || undefined,
      })
      engineStageKeyHint.value = tcr.nextStageKey
      await scaffoldEngine.loadStage({ force: true })
    }

    const next = nextPhase(currentPhase.value)
    if (!next) return
    const enginePhaseAfterSubmit = resolveEnginePhase()
    // 引擎若仍返回「当前阶段」（常见于 loadStage 与后端状态短暂不一致），应前进到顺序上的 next；
    // 若引擎已跳到更后阶段，则跟随引擎。
    const targetPhase =
      enginePhaseAfterSubmit && enginePhaseAfterSubmit !== currentPhase.value
        ? enginePhaseAfterSubmit
        : next
    if (targetPhase === 'reflection') {
      buildReflectionSummary()
    }
    await syncRoutePhase(targetPhase)
    ensureConversationState(targetPhase)
  } catch (err) {
    showToast(getErrorMessage(err))
  } finally {
    advancingPhase.value = false
  }
}

function handleStructurePick(optionId: string) {
  if (structureState.isLocked || latestStructureSubmit.value) return
  structureState.selectedOptionId = optionId
  structureState.isLocked = true

  const q = structureState.questions[structureState.currentQuestionIndex]
  if (q) {
    if (!structureState.completedQuestionIds.includes(q.id)) {
      structureState.completedQuestionIds.push(q.id)
    }
    void submitStructureSelectionToEngine(q.id, optionId).catch((err) => {
      structureState.isLocked = false
      structureState.selectedOptionId = null
      structureState.completedQuestionIds = structureState.completedQuestionIds.filter((id) => id !== q.id)
      showToast(getErrorMessage(err))
    })
  }
}

function handleStructureNext() {
  if (latestStructureSubmit.value) return
  const q = structureState.questions[structureState.currentQuestionIndex]
  if (q && !structureState.completedQuestionIds.includes(q.id)) {
    structureState.completedQuestionIds.push(q.id)
  }
  if (structureState.currentQuestionIndex < structureState.questions.length - 1) {
    structureState.currentQuestionIndex++
    structureState.selectedOptionId = null
    structureState.isLocked = false
    structureState.feedback = null
  }
}

async function autoCompleteStructureForDemo() {
  for (let index = 0; index < structureState.questions.length; index++) {
    const question = structureState.questions[index]
    if (structureState.completedQuestionIds.includes(question.id)) continue

    const optionId = question.correctId || question.options[0]?.id
    if (!optionId) continue

    structureState.currentQuestionIndex = index
    structureState.selectedOptionId = optionId
    structureState.isLocked = true
    structureState.feedback = null
    structureState.completedQuestionIds.push(question.id)

    try {
      await submitStructureSelectionToEngine(question.id, optionId)
    } catch (err) {
      structureState.completedQuestionIds = structureState.completedQuestionIds.filter(
        (id) => id !== question.id,
      )
      structureState.selectedOptionId = null
      structureState.isLocked = false
      throw err
    }
  }

  structureState.currentQuestionIndex = Math.max(0, structureState.questions.length - 1)
  structureState.selectedOptionId = null
  structureState.isLocked = false
}

async function submitStructureSelectionToEngine(questionId: string, optionId: string) {
  const previousSubmit = latestStructureSubmit.value
  if (previousSubmit) {
    await previousSubmit
  }
  const text = `STRUCTURE:${questionId}:${optionId}`
  const submitPromise = scaffoldEngine.submit(text)
  latestStructureSubmit.value = submitPromise
  try {
    const result = await submitPromise
    if (!result) {
      throw new Error('当前题目的反馈同步失败，请重试这一题')
    }
  } finally {
    if (latestStructureSubmit.value === submitPromise) {
      latestStructureSubmit.value = null
    }
  }
}

async function handleUnderstandingSend(text: string) {
  await submitPhaseConversation('understanding', text)
}

function handleUnderstandingInject(prompt: string, scaffoldKey: string) {
  understandingState.draftInput = prompt
  understandingState.scaffoldKey = scaffoldKey
}

async function handleTrainingSend(text: string) {
  await submitPhaseConversation('training', text)
}

function handleReflectionToggle(strategyId: string) {
  const idx = reflectionState.selectedStrategyIds.indexOf(strategyId)
  if (idx >= 0) {
    reflectionState.selectedStrategyIds.splice(idx, 1)
  } else {
    reflectionState.selectedStrategyIds.push(strategyId)
  }
}

function buildReflectionSummary() {
  const learned: string[] = []
  if (structureState.completedQuestionIds.length > 0) {
    learned.push('能区分 DFS 与 BFS 的基本搜索行为')
  }
  if (understandingState.messages.some((m) => m.role === 'assistant' && m.content.trim().length > 0)) {
    learned.push('理解了 DFS 为什么会回退、BFS 为什么会分层推进')
  }
  if (effectiveTrainingFinalDraft()) {
    learned.push('用自己的话讲清了 DFS / BFS 的核心差异')
  }

  reflectionState.summary = {
    learnedPoints: learned,
    finalUnderstanding: effectiveTrainingFinalDraft(),
  }
  reflectionState.confusionPoints = DFS_BFS_CONFUSION_POINTS
}

async function completeCurrentTaskAndAdvance() {
  if (!task.value || !store.sessionId) return

  buildReflectionSummary()
  const selectedStrategyLabels = reflectionState.availableStrategies
    .filter((item) => reflectionState.selectedStrategyIds.includes(item.id))
    .map((item) => item.label)

  const learnedPoints = reflectionState.summary?.learnedPoints ?? []
  const payload = buildCompleteTaskPayload({
    sessionId: store.sessionId,
    completionStatus: 'COMPLETED',
    legacyComplete: false,
    summaryText:
      reflectionState.summary?.finalUnderstanding ||
      effectiveTrainingFinalDraft() ||
      reflectionState.userReflectionText ||
      '本轮已完成当前任务，并形成了阶段性的理解总结。',
    learnedPoint1: learnedPoints[0] || '已经能说出当前主题的核心差异',
    learnedPoint2: learnedPoints[1] || '已经把这轮任务沉淀成可复述的判断线索',
    unresolvedQuestions: reflectionState.confusionPoints,
    behaviorSignals: selectedStrategyLabels.map((label) => `NEXT_RULE:${label}`),
    nextPracticeIntent:
      selectedStrategyLabels.join('；') ||
      reflectionState.userReflectionText ||
      '下一轮先用自己的话判断，再根据反馈修正。',
    learnerReflection: reflectionState.userReflectionText,
    taskStartedAt: taskStartedAt.value,
    userMessageCount: countUserMessages(),
  })

  const result = await completeTask(task.value.taskId, payload)
  clearConversationSnapshots(task.value.taskId)
  store.progress = result.sessionProgress
    ? {
        currentIndex: result.sessionProgress.completedTasks,
        totalTasks: result.sessionProgress.totalTasks,
      }
    : null

  store.report = null
  store.nextActionDecision = null
  if (result.nextTaskAvailable && result.nextTaskId) {
    store.currentTaskId = result.nextTaskId
  } else {
    store.currentTaskId = null
  }
  store.currentTask = null
  task.value = null
  progress.value = null
  resetWorkbenchState()
  if (result.reportReady || result.nextRoute === '/report') {
    await router.push('/report')
    return
  }
  if (result.nextTaskAvailable && result.nextTaskId) {
    await router.push({ name: 'taskRun', params: { taskId: result.nextTaskId } })
    return
  }
  try {
    const flow = await getSessionFlowState(store.sessionId)
    if (flow.reportReady) {
      await router.push('/report')
      return
    }
    if (flow.currentTaskId) {
      store.currentTaskId = flow.currentTaskId
      await router.push({ name: 'taskRun', params: { taskId: flow.currentTaskId } })
      return
    }
  } catch {
    // Fall through to report to keep the closure path moving.
  }
  await router.push('/report')
}

async function submitPhaseConversation(phase: 'understanding' | 'training', text: string) {
  if (!task.value || !store.sessionId) return

  const state = phase === 'understanding' ? understandingState : trainingState
  if (state.streaming) return

  state.error = null
  const userMsg = makeUserMessage(text)
  state.messages.push(userMsg)
  state.draftInput = ''
  if (phase === 'understanding') {
    understandingState.turnCount++
    understandingState.scaffoldKey = null
  } else {
    trainingState.roundCount++
  }

  const assistantMsg = makeAssistantMessage('')
  state.messages.push(assistantMsg)
  state.streaming = true

  try {
    await streamAiTutorChat(
      {
        messages: toTutorPayloadMessages(state.messages),
        context: {
          step: PHASE_SEQUENCE.indexOf(phase) + 1,
          knowledge: task.value?.knowledge || store.planPreview?.knowledgeKey || 'dfs_bfs',
          knowledgeLabel: workbenchTopicName.value,
          phase: PHASE_KEY_TO_CODE[phase],
          sessionId: store.sessionId,
          taskId: task.value.taskId,
        },
      },
      {
        onDelta: (chunk) => {
          assistantMsg.content += chunk
        },
        onDone: (payload) => {
          if (phase === 'understanding') {
            understandingState.canProceed = false
            understandingState.completionHint = payload.completionHint || null
            understandingState.stageSummary = payload.summary || null
            return
          }
          trainingState.canProceed = payload.canProceed === 'true'
          trainingState.completionHint = payload.completionHint || null
          trainingState.stageSummary = payload.summary || null
          trainingState.finalDraft = payload.finalDraft || null
        },
      }
    )

    assistantMsg.content = assistantMsg.content.trim()
    if (!assistantMsg.content) {
      state.messages.pop()
      throw new Error('导师没有返回可展示的内容')
    }
  } catch (err) {
    const msg = getErrorMessage(err)
    state.error = msg
    if (!assistantMsg.content.trim()) {
      state.messages.pop()
    }
    showToast(msg)
  } finally {
    state.streaming = false
    persistConversationState(phase)
  }
}

const workbenchVm = computed<ExecutionWorkbenchVm>(() => ({
  topicName: workbenchTopicName.value,
  phase: currentPhase.value,
  renderState: 'prompt',
  phaseGoal: DFS_BFS_PHASE_GOALS[currentPhase.value],
  phaseProgress: {
    phases: PHASE_SEQUENCE.map((k) => PHASE_KEY_TO_CODE[k]),
    currentPhase: PHASE_KEY_TO_CODE[currentPhase.value],
    overallRatio: PHASE_SEQUENCE.indexOf(currentPhase.value) / PHASE_SEQUENCE.length,
  },
  structure: structureState,
  understanding: understandingState,
  training: trainingState,
  reflection: reflectionState,
  busy: currentPhaseBusy.value,
}))

const workbenchTopicName = computed(() => {
  return store.planPreview?.knowledgeKey || task.value?.knowledge || '深度优先与广度优先（DFS / BFS）'
})

type ConversationSnapshot = {
  messages: ChatMessage[]
  draftInput: string
  turnCount?: number
  roundCount?: number
  canProceed: boolean
  completionHint: string | null
  stageSummary: string | null
  finalDraft?: string | null
}

function ensureConversationState(phase: PhaseKey) {
  if (!task.value) return
  if (phase === 'understanding') {
    if (understandingState.messages.length > 0) return
    const snapshot = loadConversationSnapshot('understanding')
    if (snapshot) {
      syncMsgCounter(snapshot.messages)
      understandingState.messages = snapshot.messages
      understandingState.draftInput = snapshot.draftInput
      understandingState.turnCount = snapshot.turnCount ?? 0
      understandingState.canProceed = snapshot.canProceed
      understandingState.completionHint = snapshot.completionHint
      understandingState.stageSummary = snapshot.stageSummary
      return
    }
    understandingState.messages.push(makeSystemMessage(UNDERSTANDING_SYSTEM_OPENER))
    return
  }
  if (phase === 'training') {
    if (trainingState.messages.length > 0) return
    const snapshot = loadConversationSnapshot('training')
    if (snapshot) {
      syncMsgCounter(snapshot.messages)
      trainingState.messages = snapshot.messages
      trainingState.draftInput = snapshot.draftInput
      trainingState.roundCount = snapshot.roundCount ?? 0
      trainingState.canProceed = snapshot.canProceed
      trainingState.completionHint = snapshot.completionHint
      trainingState.stageSummary = snapshot.stageSummary
      trainingState.finalDraft = snapshot.finalDraft ?? null
      return
    }
    trainingState.messages.push(makeSystemMessage(TRAINING_SYSTEM_OPENER))
  }
}

function persistConversationState(phase: 'understanding' | 'training') {
  if (!task.value) return
  const key = conversationStorageKey(phase)
  if (!key) return
  const snapshot: ConversationSnapshot =
    phase === 'understanding'
      ? {
          messages: understandingState.messages,
          draftInput: understandingState.draftInput,
          turnCount: understandingState.turnCount,
          canProceed: understandingState.canProceed,
          completionHint: understandingState.completionHint,
          stageSummary: understandingState.stageSummary,
        }
      : {
          messages: trainingState.messages,
          draftInput: trainingState.draftInput,
          roundCount: trainingState.roundCount,
          canProceed: trainingState.canProceed,
          completionHint: trainingState.completionHint,
          stageSummary: trainingState.stageSummary,
          finalDraft: trainingState.finalDraft,
        }
  window.sessionStorage.setItem(key, JSON.stringify(snapshot))
}

function loadConversationSnapshot(phase: 'understanding' | 'training'): ConversationSnapshot | null {
  const key = conversationStorageKey(phase)
  if (!key) return null
  const raw = window.sessionStorage.getItem(key)
  if (!raw) return null
  try {
    return JSON.parse(raw) as ConversationSnapshot
  } catch {
    window.sessionStorage.removeItem(key)
    return null
  }
}

function conversationStorageKey(phase: 'understanding' | 'training') {
  if (!task.value?.taskId) return null
  return `taskrun:${task.value.taskId}:${phase}`
}

function clearConversationSnapshots(taskId: string) {
  window.sessionStorage.removeItem(`taskrun:${taskId}:understanding`)
  window.sessionStorage.removeItem(`taskrun:${taskId}:training`)
}

function countUserMessages() {
  const understandingUser = understandingState.messages.filter((msg) => msg.role === 'user').length
  const trainingUser = trainingState.messages.filter((msg) => msg.role === 'user').length
  return understandingUser + trainingUser
}

function toTutorPayloadMessages(messages: ChatMessage[]): AiTutorChatMessagePayload[] {
  return messages
    .filter((msg) => msg.content.trim().length > 0)
    .map((msg) => ({
      role: msg.role,
      content: msg.content,
    }))
}

let msgIdCounter = 0
function syncMsgCounter(messages: ChatMessage[]) {
  msgIdCounter += messages.length + 10
}
function makeSystemMessage(content: string): ChatMessage {
  return { id: `sys-${++msgIdCounter}`, role: 'system', content, timestamp: Date.now() }
}
function makeUserMessage(content: string): ChatMessage {
  return { id: `usr-${++msgIdCounter}`, role: 'user', content, timestamp: Date.now() }
}
function makeAssistantMessage(content: string): ChatMessage {
  return { id: `ast-${++msgIdCounter}`, role: 'assistant', content, timestamp: Date.now() }
}

watch(
  () => [store.sessionId, route.name, route.params.taskId] as const,
  () => {
    void fetchTask()
  },
  { immediate: true },
)
</script>
