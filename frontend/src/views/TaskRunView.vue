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
          :primary-loading="scaffoldEngine.submitting"
          @primary="goNextPhase"
        />
      </section>
    </main>
  </PageContainer>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted, reactive, ref, watch } from 'vue'
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
import { getCurrentTask, getTaskScaffold } from '@/api/task'
import { supportsLearningScaffoldEngine } from '@/constants/learningScaffoldPack'
import { TASKRUN_COPY } from '@/constants/uiCopy'
import {
  DFS_BFS_NEXT_LABELS,
  DFS_BFS_PHASE_GOALS,
  DFS_BFS_REFLECTION_STRATEGIES,
  DFS_BFS_SCAFFOLD_BUTTONS,
  DFS_BFS_STRUCTURE_QUESTIONS,
  TRAINING_MAX_ROUNDS,
  TRAINING_SYSTEM_OPENER,
  UNDERSTANDING_CLOSURE_HINT,
  UNDERSTANDING_MAX_TURNS,
  UNDERSTANDING_SYSTEM_OPENER,
} from '@/constants/dfsBfsExecutionConfig'
import { showToast } from '@/stores/toast'
import { useAiTutorStore } from '@/stores/aiTutor'
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
const aiTutorStore = useAiTutorStore()

const loading = ref(true)
const error = ref<string | null>(null)
const task = ref<CurrentTaskItem | null>(null)
const progress = ref<ProgressItem | null>(null)
const scaffold = ref<TaskScaffoldResponse | null>(null)
const taskStartedAt = ref(Date.now())

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

// ─── Phase routing ───

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

watch(
  currentPhase,
  async (phase) => {
    const current = typeof route.query.phase === 'string' ? route.query.phase.toLowerCase() : ''
    if (current === phase) return
    await router.replace({ query: { ...route.query, phase } })
  },
  { immediate: true },
)

// ─── Scaffold buttons ───

const scaffoldButtons = computed(() => DFS_BFS_SCAFFOLD_BUTTONS)

// ─── Next action ───

const nextActionLabel = computed(() => DFS_BFS_NEXT_LABELS[currentPhase.value])

const canGoNext = computed(() => {
  switch (currentPhase.value) {
    case 'structure':
      return structureState.completedQuestionIds.length >= structureState.questions.length
    case 'understanding':
      return understandingState.turnCount >= 1 && understandingState.messages.some((m) => m.role === 'assistant')
    case 'training':
      return !!trainingState.finalDraft
    case 'reflection':
      return reflectionState.selectedStrategyIds.length > 0 || reflectionState.userReflectionText.trim().length > 0
    default:
      return false
  }
})

async function goNextPhase() {
  if (!canGoNext.value) return
  if (currentPhase.value === 'reflection') {
    await submitReflectionToBackend()
    router.push('/report')
    return
  }
  const next = nextPhase(currentPhase.value)
  if (!next) return

  if (currentPhase.value === 'structure') {
    await submitStructureToBackend()
  }

  await router.replace({ query: { ...route.query, phase: next } })

  if (next === 'understanding' && understandingState.messages.length === 0) {
    understandingState.messages.push(makeSystemMessage(UNDERSTANDING_SYSTEM_OPENER))
  }
  if (next === 'training' && trainingState.messages.length === 0) {
    trainingState.messages.push(makeSystemMessage(TRAINING_SYSTEM_OPENER))
  }
  if (next === 'reflection') {
    buildReflectionSummary()
  }
}

// ─── STRUCTURE handlers ───

function handleStructurePick(optionId: string) {
  if (structureState.isLocked) return
  structureState.selectedOptionId = optionId
  structureState.isLocked = true

  const q = structureState.questions[structureState.currentQuestionIndex]
  if (q) {
    if (!structureState.completedQuestionIds.includes(q.id)) {
      structureState.completedQuestionIds.push(q.id)
    }
    submitStructureSelectionToEngine(q.id, optionId)
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

async function submitStructureToBackend() {
  for (const q of structureState.questions) {
    if (!structureState.completedQuestionIds.includes(q.id)) {
      structureState.completedQuestionIds.push(q.id)
    }
  }
}

// ─── UNDERSTANDING handlers ───

async function handleUnderstandingSend(text: string) {
  if (!task.value || !store.sessionId) return
  const userMsg = makeUserMessage(text)
  understandingState.messages.push(userMsg)
  understandingState.draftInput = ''
  understandingState.turnCount++

  try {
    const scaffoldKey = understandingState.scaffoldKey
    const inputText = scaffoldKey ? `[scaffold:${scaffoldKey}] ${text}` : text
    const res = await scaffoldEngine.submit(inputText)

    const reply = res?.tutor?.content
      || res?.feedbackPayload?.completeness
      || res?.feedbackPayload?.issuePoints?.[0]
      || '已收到你的问题，请继续探索。'

    understandingState.messages.push(makeAssistantMessage(String(reply)))
    understandingState.scaffoldKey = null

    if (understandingState.turnCount >= UNDERSTANDING_MAX_TURNS) {
      understandingState.messages.push(makeSystemMessage(UNDERSTANDING_CLOSURE_HINT))
      understandingState.isReadyToAdvance = true
    }
  } catch (err) {
    showToast(getErrorMessage(err))
  }
}

function handleUnderstandingInject(prompt: string, scaffoldKey: string) {
  understandingState.draftInput = prompt
  understandingState.scaffoldKey = scaffoldKey
}

// ─── TRAINING handlers ───

async function handleTrainingSend(text: string) {
  if (!task.value || !store.sessionId) return
  const userMsg = makeUserMessage(text)
  trainingState.messages.push(userMsg)
  trainingState.draftInput = ''
  trainingState.roundCount++

  try {
    const res = await scaffoldEngine.submit(text)

    const tutorContent = res?.tutor?.content || ''
    const completeness = res?.feedbackPayload?.completeness || ''
    const issues = res?.feedbackPayload?.issuePoints?.filter(Boolean) ?? []
    const nextAction = res?.feedbackPayload?.nextAction || ''

    let replyParts: string[] = []
    if (tutorContent) replyParts.push(tutorContent)
    else {
      if (completeness) replyParts.push(completeness)
      if (issues.length) replyParts.push(issues[0]!)
      if (nextAction) replyParts.push(nextAction)
    }

    const reply = replyParts.length > 0
      ? replyParts.join('\n\n')
      : '已收到你的表达，请继续补充。'

    trainingState.messages.push(makeAssistantMessage(reply))

    const canProceed = res?.tutor?.canProceed || res?.canProceed || false
    if (trainingState.roundCount >= TRAINING_MAX_ROUNDS || canProceed) {
      trainingState.finalDraft = completeness || text
      trainingState.isReadyToAdvance = true
    }
  } catch (err) {
    showToast(getErrorMessage(err))
  }
}

// ─── REFLECTION handlers ───

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
    learned.push('能区分 DFS 和 BFS 的基本搜索行为')
  }
  if (understandingState.turnCount > 0) {
    learned.push('理解了 DFS 回退和 BFS 分层推进的原因')
  }
  if (trainingState.finalDraft) {
    learned.push('用自己的话完成了 DFS/BFS 的完整表达')
  }

  reflectionState.summary = {
    learnedPoints: learned,
    finalUnderstanding: trainingState.finalDraft || '',
  }
}

async function submitReflectionToBackend() {
  const text =
    `REFLECTION:strategies=${reflectionState.selectedStrategyIds.join(',')};` +
    `text=${reflectionState.userReflectionText}`
  await scaffoldEngine.submit(text)
}

// ─── VM assembly ───

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
  busy: scaffoldEngine.submitting || scaffoldEngine.loading,
}))

const workbenchTopicName = computed(() => {
  return task.value?.title || store.planPreview?.knowledgeKey || 'DFS / BFS'
})

// ─── Data fetching ───

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
  } catch (err) {
    error.value = getErrorMessage(err)
  } finally {
    loading.value = false
  }
}

async function loadScaffold(taskId: string) {
  if (!store.sessionId) return
  taskStartedAt.value = Date.now()
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

// ─── AI tutor context ───

watch(
  [task, currentPhase],
  () => {
    if (!task.value) return
    aiTutorStore.setContext({
      stepId: task.value.taskId,
      step: PHASE_SEQUENCE.indexOf(currentPhase.value) + 1,
      knowledgeKey: scaffold.value?.packId || store.planPreview?.knowledgeKey || 'unknown',
      knowledgeLabel: workbenchTopicName.value,
      phaseCode: PHASE_KEY_TO_CODE[currentPhase.value],
      phaseLabel: DFS_BFS_PHASE_GOALS[currentPhase.value],
      currentAction: '',
      floatingLabel: '给我提示',
      panelTitle: '学习助手',
      quickQuestions: [],
    })
    aiTutorStore.showTaskRunFloatingFab = true
  },
  { immediate: true },
)

// ─── Helpers ───

let msgIdCounter = 0
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

onUnmounted(() => {
  aiTutorStore.showTaskRunFloatingFab = false
})
</script>
