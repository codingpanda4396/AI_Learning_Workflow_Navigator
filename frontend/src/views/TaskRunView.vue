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
        <div class="mx-auto max-w-6xl space-y-5" :data-phase="currentPhase">
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
          :primary-disabled="!canGoNext"
          :primary-loading="nextActionLoading"
          @primary="goNextPhase"
        />
      </section>
    </main>
  </PageContainer>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
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
import { postCompleteConversationStage } from '@/api/learningScaffold'
import { streamAiTutorChat, type AiTutorChatMessagePayload } from '@/api/tutor'
import { getCurrentTask, getTaskScaffold } from '@/api/task'
import { supportsLearningScaffoldEngine } from '@/constants/learningScaffoldPack'
import { TASKRUN_COPY } from '@/constants/uiCopy'
import {
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
import type { CurrentTaskItem, ProgressItem, TaskScaffoldResponse } from '@/types/dto'
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
const task = ref<CurrentTaskItem | null>(null)
const progress = ref<ProgressItem | null>(null)
const scaffold = ref<TaskScaffoldResponse | null>(null)
const advancingPhase = ref(false)

const structureState = reactive<StructurePhaseState>(
  createEmptyStructureState(DFS_BFS_STRUCTURE_QUESTIONS),
)
const understandingState = reactive<UnderstandingPhaseState>(createEmptyUnderstandingState())
const trainingState = reactive<TrainingPhaseState>(createEmptyTrainingState())
const reflectionState = reactive<ReflectionPhaseState>(
  createEmptyReflectionState(DFS_BFS_REFLECTION_STRATEGIES),
)

const scaffoldEngine = useLearningScaffoldEngine({
  taskId: () => task.value?.taskId,
  sessionId: () => store.sessionId,
  enabled: () =>
    supportsLearningScaffoldEngine(scaffold.value?.packId ?? store.planPreview?.packId) &&
    !!task.value &&
    !loading.value,
})

const PHASE_QUERY_TO_KEY: Record<string, PhaseKey> = {
  structure: 'structure',
  understanding: 'understanding',
  training: 'training',
  reflection: 'reflection',
}

const phaseFromQuery = computed<PhaseKey>(() => {
  const raw = typeof route.query.phase === 'string' ? route.query.phase.toLowerCase() : ''
  return PHASE_QUERY_TO_KEY[raw] ?? 'structure'
})

const currentPhase = computed<PhaseKey>(() => phaseFromQuery.value)
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
const nextActionLoading = computed(() => advancingPhase.value || currentPhaseBusy.value)

const canGoNext = computed(() => {
  switch (currentPhase.value) {
    case 'structure':
      return structureState.completedQuestionIds.length >= structureState.questions.length
    case 'understanding':
      return understandingState.canProceed
    case 'training':
      return trainingState.canProceed && !!trainingState.finalDraft
    case 'reflection':
      return reflectionState.selectedStrategyIds.length > 0 || reflectionState.userReflectionText.trim().length > 0
    default:
      return false
  }
})

watch(
  currentPhase,
  async (phase) => {
    const current = typeof route.query.phase === 'string' ? route.query.phase.toLowerCase() : ''
    if (current === phase) return
    await router.replace({ query: { ...route.query, phase } })
  },
  { immediate: true },
)

watch(
  () => scaffoldEngine.stage?.stageKey,
  async (stageKey) => {
    if (!stageKey) return
    const phaseKey = PHASE_CODE_TO_KEY[stageKey as keyof typeof PHASE_CODE_TO_KEY]
    if (phaseKey && phaseKey !== currentPhase.value) {
      await router.replace({ query: { ...route.query, phase: phaseKey } })
    }
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
  if (!canGoNext.value || !task.value || !store.sessionId) return
  advancingPhase.value = true
  try {
    if (currentPhase.value === 'reflection') {
      await submitReflectionToBackend()
      await router.push('/report')
      return
    }

    if (currentPhase.value === 'understanding') {
      await postCompleteConversationStage(task.value.taskId, {
        sessionId: store.sessionId,
        stageKey: 'UNDERSTANDING',
      })
      await scaffoldEngine.loadStage()
    }

    if (currentPhase.value === 'training') {
      await postCompleteConversationStage(task.value.taskId, {
        sessionId: store.sessionId,
        stageKey: 'TRAINING',
        finalDraft: trainingState.finalDraft ?? undefined,
      })
      await scaffoldEngine.loadStage()
    }

    const next = nextPhase(currentPhase.value)
    if (!next) return
    if (next === 'reflection') {
      buildReflectionSummary()
    }
    await router.replace({ query: { ...route.query, phase: next } })
    ensureConversationState(next)
  } catch (err) {
    showToast(getErrorMessage(err))
  } finally {
    advancingPhase.value = false
  }
}

function handleStructurePick(optionId: string) {
  if (structureState.isLocked) return
  structureState.selectedOptionId = optionId
  structureState.isLocked = true

  const q = structureState.questions[structureState.currentQuestionIndex]
  if (q) {
    if (!structureState.completedQuestionIds.includes(q.id)) {
      structureState.completedQuestionIds.push(q.id)
    }
    void submitStructureSelectionToEngine(q.id, optionId)
  }
}

function handleStructureNext() {
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

async function submitStructureSelectionToEngine(questionId: string, optionId: string) {
  const text = `STRUCTURE:${questionId}:${optionId}`
  await scaffoldEngine.submit(text)
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
  if (understandingState.canProceed || understandingState.messages.some((m) => m.role === 'assistant')) {
    learned.push('理解了 DFS 为什么会回退、BFS 为什么会分层推进')
  }
  if (trainingState.finalDraft) {
    learned.push('用自己的话讲清了 DFS / BFS 的核心差异')
  }

  reflectionState.summary = {
    learnedPoints: learned,
    finalUnderstanding: trainingState.finalDraft || '',
  }
}

async function submitReflectionToBackend() {
  if (scaffoldEngine.stage?.stageKey !== 'REFLECTION') return
  const text =
    `REFLECTION:strategies=${reflectionState.selectedStrategyIds.join(',')};` +
    `text=${reflectionState.userReflectionText}`
  await scaffoldEngine.submit(text)
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
          knowledge: scaffold.value?.packId || store.planPreview?.knowledgeKey || 'dfs_bfs',
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
          state.canProceed = payload.canProceed === 'true'
          state.completionHint = payload.completionHint || null
          state.stageSummary = payload.summary || null
          if (phase === 'training') {
            trainingState.finalDraft = payload.finalDraft || null
          }
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
  return task.value?.title || store.planPreview?.knowledgeKey || '深度优先与广度优先（DFS / BFS）'
})

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
    if (
      route.name === 'task' ||
      (route.name === 'taskRun' && routeTaskId && routeTaskId !== data.currentTask.taskId)
    ) {
      router.replace({ name: 'taskRun', params: { taskId: data.currentTask.taskId } })
    }

    await loadScaffold(data.currentTask.taskId)
    ensureConversationState(currentPhase.value)
  } catch (err) {
    error.value = getErrorMessage(err)
  } finally {
    loading.value = false
  }
}

async function loadScaffold(taskId: string) {
  if (!store.sessionId) return
  try {
    const data = await getTaskScaffold(taskId, store.sessionId)
    scaffold.value = data

    const enginePhase = data.phaseProgress?.currentPhase
    if (enginePhase) {
      const phaseKey = PHASE_CODE_TO_KEY[enginePhase as keyof typeof PHASE_CODE_TO_KEY]
      if (phaseKey && phaseKey !== currentPhase.value) {
        await router.replace({ query: { ...route.query, phase: phaseKey } })
      }
    }
  } catch {
    scaffold.value = null
    showToast('当前任务先切到简化模式。')
  }
}

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

onMounted(() => {
  fetchTask()
})
</script>
