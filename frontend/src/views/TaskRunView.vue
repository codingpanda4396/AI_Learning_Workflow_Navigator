<template>
  <PageContainer>
    <AppTopBar current="task" />
    <main class="mx-auto max-w-[1260px] px-4 py-6 md:px-6 lg:px-8">
      <LoadingState v-if="loading && !task" message="正在打开当前任务..." />
      <ErrorState v-else-if="error" :message="error">
        <template #action>
          <SecondaryButton @click="fetchTask">重试</SecondaryButton>
        </template>
      </ErrorState>
      <EmptyState v-else-if="!task && !loading" message="当前没有任务">
        <template #action>
          <SecondaryButton @click="router.push('/report')">查看结果</SecondaryButton>
        </template>
      </EmptyState>

      <section v-else-if="task" class="space-y-5 pb-28">
        <StageProgressHeader
          :topic-name="workbenchTopicName"
          :phase-progress="pageModel.workbench.phaseProgress"
          :task-status-label="pageModel.workbench.taskStatusLabel"
          :strategy-line="pageModel.header.strategyLine"
        />

        <section class="grid grid-cols-1 gap-6 lg:grid-cols-12 lg:items-start">
          <div
            class="space-y-6 lg:col-span-8"
            :data-phase="pageModel.workbench.emphasisPhase"
          >
            <CurrentTaskCard
              :model="pageModel.workbench.currentTask"
              :emphasis-phase="pageModel.workbench.emphasisPhase"
            />

            <template v-if="useDrivingSeatLayout">
              <ScaffoldActionCard
                :product="pageModel.workbench.scaffoldProduct"
                :cards="pageModel.scaffoldCards"
                :phase-prompt-chips="pageModel.tutorConsole.phasePromptChips"
                :sending="mainActionLoading"
                @send-card="onSendScaffoldCard"
                @prefill-card="onPrefillScaffoldCard"
                @prefill-chip="onPrefillPhaseChip"
              />

              <UserExpressionPanel
                :chat-turns="chatTurns"
                :draft-value="draftInput"
                :input-label="pageModel.mainAction.inputLabel || '你想补充的'"
                :input-placeholder="pageModel.mainAction.inputPlaceholder || ''"
                :input-placeholder-soft="pageModel.tutorConsole.inputPlaceholderSoft"
                :primary-action-label="pageModel.mainAction.primaryActionLabel"
                :sending="mainActionLoading"
                :can-submit-chat="canSubmitDrivingChat"
                :show-restate="showRestateSection"
                :show-advance="showAdvanceSection"
                :micro-check-labels="microCheckLabels"
                :checks="microChecks"
                :can-advance="canAdvanceDriving"
                :advancing="advancing"
                advance-label="我已经搭好这个点的框架，进入下一步"
                :restate-what="restateWhat"
                :restate-problem="restateProblem"
                :restate-relate="restateRelate"
                :emphasis-phase="pageModel.workbench.emphasisPhase"
                @update:draft-value="draftInput = $event"
                @update:checks="microChecks = $event"
                @update:restate-what="restateWhat = $event"
                @update:restate-problem="restateProblem = $event"
                @update:restate-relate="restateRelate = $event"
                @submit-chat="handlePrimaryAction"
                @advance="onAdvanceDrivingSeat"
                @stuck="onStuckFromPanel"
              />
            </template>

            <ExecutionMainActionCard
              v-else
              :model="pageModel.mainAction"
              :draft-value="draftInput"
              :loading="mainActionLoading"
              :disabled="mainActionDisabled"
              :can-submit="canSubmitMainAction"
              :closure-summary="closureSummary"
              :closure-point1="closurePoint1"
              :closure-point2="closurePoint2"
              :closure-next="closureNext"
              :learner-reflection="completeForm.learnerReflection"
              :completion-status="completeForm.completionStatus"
              @update:draft-value="draftInput = $event"
              @update:closure-summary="closureSummary = $event"
              @update:closure-point1="closurePoint1 = $event"
              @update:closure-point2="closurePoint2 = $event"
              @update:closure-next="closureNext = $event"
              @update:learner-reflection="completeForm.learnerReflection = $event"
              @update:completion-status="completeForm.completionStatus = $event as TaskCompletionStatusType"
              @use-chip="fillDraftInput"
              @submit="handlePrimaryAction"
            />

            <FeedbackPanel :model="pageModel.feedback" @action="handleFeedbackAction" />

            <ExecutionHelpCollapse
              :sections="pageModel.helpSections"
              :transcript="useDrivingSeatLayout ? [] : transcriptItems"
            />
          </div>

          <aside class="space-y-4 lg:col-span-4">
            <WhyThisStepCard :model="pageModel.workbench.whyThisStep" />
            <StageRuleCard :model="pageModel.workbench.stageRules" />
            <TopicSpecialHintCard :model="pageModel.workbench.topicHints" />
            <StageProgressMiniCard :model="pageModel.workbench.stageMini" />
            <section
              v-if="pageModel.progressRail.stuckActions.length"
              class="rounded-2xl border border-amber-100 bg-amber-50/40 p-4"
            >
              <p class="text-xs font-semibold uppercase tracking-wide text-amber-900/90">
                {{ pageModel.progressRail.stuckSectionTitle }}
              </p>
              <ul class="mt-3 space-y-2">
                <li v-for="(action, i) in pageModel.progressRail.stuckActions" :key="i">
                  <button
                    type="button"
                    class="w-full rounded-xl border border-amber-200/90 bg-white px-3 py-2 text-left text-sm text-slate-800 transition hover:border-primary/40 hover:bg-primary/5"
                    :data-testid="`stuck-action-${i}`"
                    @click="onStuckAction(action)"
                  >
                    {{ action }}
                  </button>
                </li>
              </ul>
            </section>
          </aside>
        </section>

        <BottomActionBar
          :primary-label="bottomPrimaryLabel"
          :primary-loading="mainActionLoading"
          :primary-disabled="bottomPrimaryDisabled"
          :show-advance="useDrivingSeatLayout && showAdvanceSection"
          :can-advance="canAdvanceDriving"
          :advancing="advancing"
          advance-label="进入下一阶段"
          @save-draft="saveDraftExplicit"
          @open-tutor="openAiTutorPanel"
          @primary="handlePrimaryAction"
          @advance="onAdvanceDrivingSeat"
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
import BottomActionBar from '@/components/task-run/BottomActionBar.vue'
import CurrentTaskCard from '@/components/task-run/CurrentTaskCard.vue'
import ExecutionHelpCollapse from '@/components/task-run/ExecutionHelpCollapse.vue'
import ExecutionMainActionCard from '@/components/task-run/ExecutionMainActionCard.vue'
import FeedbackPanel from '@/components/task-run/FeedbackPanel.vue'
import ScaffoldActionCard from '@/components/task-run/ScaffoldActionCard.vue'
import StageProgressHeader from '@/components/task-run/StageProgressHeader.vue'
import StageProgressMiniCard from '@/components/task-run/StageProgressMiniCard.vue'
import StageRuleCard from '@/components/task-run/StageRuleCard.vue'
import TopicSpecialHintCard from '@/components/task-run/TopicSpecialHintCard.vue'
import UserExpressionPanel from '@/components/task-run/UserExpressionPanel.vue'
import WhyThisStepCard from '@/components/task-run/WhyThisStepCard.vue'
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
import type {
  ExecutionGuideFeedbackModel,
  ExecutionPageViewModel,
} from '@/types/executionGuide'
import { TaskCompletionStatus, type TaskCompletionStatusType } from '@/types/enums'
import { buildCompleteTaskPayload } from '@/utils/buildCompleteTaskPayload'
import { buildExecutionPageModel, createEmptyWorkbenchModel } from '@/utils/buildExecutionPageModel'
import { buildTaskGuidedSteps, getCurrentGuidedStepId } from '@/utils/taskGuidedSteps'
import { useKnowledgePack } from '@/composables/useKnowledgePack'

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
const latestFeedback = ref<ExecutionGuideFeedbackModel | null>(null)
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

const restateWhat = ref('')
const restateProblem = ref('')
const restateRelate = ref('')
const microChecks = ref<boolean[]>([false, false, false])

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

function countChatPairs(turns: ChatTurn[]): number {
  let n = 0
  for (let i = 0; i < turns.length - 1; i++) {
    if (turns[i]!.role === 'USER' && turns[i + 1]!.role === 'ASSISTANT') n++
  }
  return n
}

/** 与 mainAction.mode !== 'closure' 对齐：不依赖 pageModel，避免与 canAdvance 循环 */
const useDrivingSeatLayout = computed(() => {
  if (!task.value) return false
  if (legacyComplete.value) return false
  return taskState.value !== 'PASS'
})

const chatPairs = computed(() => countChatPairs(chatTurns.value))

const knowledgePack = computed(() =>
  useKnowledgePack({
    scaffold: scaffold.value,
    structuredGoal: store.structuredGoal,
    plan: store.planPreview,
  })
)

const microCheckLabels = computed(() => {
  const fromPack = knowledgePack.value?.execution.microCheckLabels ?? []
  if (fromPack.length >= 2) return fromPack.slice(0, 3)
  const raw = scaffold.value?.selfCheckTemplates?.filter((s) => s?.trim()).slice(0, 3) ?? []
  if (raw.length >= 2) return raw
  return [
    '我能说出这个概念大概在解决什么',
    '我能说出一个最小例子或场景',
    '我能说出它和相邻概念的一点关系',
  ]
})

watch(
  microCheckLabels,
  (labels) => {
    microChecks.value = labels.map(() => false)
  },
  { immediate: true }
)

const showRestateSection = computed(
  () =>
    useDrivingSeatLayout.value &&
    (taskState.value === 'ORIENT' || taskState.value === 'EXPLORE') &&
    chatPairs.value >= 1
)

const showAdvanceSection = computed(
  () =>
    useDrivingSeatLayout.value && taskState.value === 'EXPLORE' && chatPairs.value >= 1
)

const canAdvanceDriving = computed(() => {
  if (!microChecks.value.length || !microChecks.value.every(Boolean)) return false
  if (!showRestateSection.value) return true
  return (
    restateWhat.value.trim().length >= 2 &&
    restateProblem.value.trim().length >= 2 &&
    restateRelate.value.trim().length >= 2
  )
})

const pageModel = computed<ExecutionPageViewModel>(() => {
  if (!task.value) {
    return EMPTY_PAGE_MODEL
  }
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

const workbenchTopicName = computed(() => {
  const w = pageModel.value.workbench.topicHints.topicDisplayName?.trim()
  if (w) return w
  return (
    pageModel.value.header.operationConsole?.knowledgePointName ||
    pageModel.value.header.title ||
    '当前任务'
  )
})

const bottomPrimaryLabel = computed(() => {
  if (pageModel.value.mainAction.mode === 'closure') return '完成本任务'
  return pageModel.value.mainAction.primaryActionLabel || '提交本轮'
})

const bottomPrimaryDisabled = computed(() => {
  if (mainActionLoading.value) return true
  if (pageModel.value.mainAction.mode === 'closure') return false
  if (useDrivingSeatLayout.value) return !canSubmitDrivingChat.value
  return !canSubmitMainAction.value
})

const transcriptItems = computed(() =>
  chatTurns.value.map((item) => ({
    role: item.role,
    speaker: item.role === 'USER' ? '你的回答' : '反馈',
    content: item.content,
  }))
)

const canSubmitDrivingChat = computed(() => {
  const mode = pageModel.value?.mainAction.mode
  if (mode === 'closure') return false
  return !!draftInput.value.trim()
})

const mainActionLoading = computed(() => {
  if (useDrivingSeatLayout.value) {
    return (
      sending.value ||
      advancing.value ||
      submittingCheckpoint.value ||
      submittingSelf.value
    )
  }
  const mode = pageModel.value?.mainAction.mode
  if (mode === 'closure') return completing.value
  if (taskState.value === 'CHECK') return submittingCheckpoint.value
  if (taskState.value === 'SELF_EXPLAIN' || taskState.value === 'REMEDIAL') {
    return submittingSelf.value
  }
  return sending.value
})

const mainActionDisabled = computed(() => mainActionLoading.value)
const canSubmitMainAction = computed(() => {
  const mode = pageModel.value?.mainAction.mode
  if (mode === 'closure') return true
  return !!draftInput.value.trim()
})

function firstLine(text: string, fallback: string) {
  const line = text
    .split(/\n/)
    .map((item) => item.trim())
    .find(Boolean)
  return line || fallback
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
  latestFeedback.value = null
  restateWhat.value = ''
  restateProblem.value = ''
  restateRelate.value = ''
  microChecks.value = microCheckLabels.value.map(() => false)
}

function fillDraftInput(text: string) {
  const value = text.trim()
  if (!value) return
  draftInput.value = draftInput.value.trim()
    ? `${draftInput.value.trim()}\n${value}`
    : value
}

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

function saveDraftExplicit() {
  const id = task.value?.taskId
  if (!id) return
  localStorage.setItem(draftStorageKey(id), draftInput.value)
  showToast('草稿已保存')
}

function openAiTutorPanel() {
  if (!task.value) return
  aiTutorStore.setContext({
    stepId: task.value.taskId,
    step: guidedStepPosition.value.current,
    knowledgeKey: knowledgePack.value?.id ?? 'unknown',
    knowledgeLabel: knowledgePack.value?.tutor.focusLabel ?? task.value.title,
    phaseCode: pageModel.value.header.phaseCode ?? 'STRUCTURE',
    phaseLabel: pageModel.value.header.phaseDisplayZh ?? '',
  })
  aiTutorStore.openPanel()
}

function onStuckFromPanel() {
  const actions = pageModel.value.progressRail.stuckActions
  if (actions.length) {
    onStuckAction(actions[0]!)
    return
  }
  fillDraftInput('我卡住了：请用最短路径给我下一步线索。')
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
    fillDraftInput('请用更短、更少的术语，把上一步压缩成两三句说给我。')
    return
  }
  fillDraftInput(action)
}

function buildMessageFeedback(
  userInput: string,
  response: TaskMessageResponse
): ExecutionGuideFeedbackModel {
  const mastered = firstLine(
    userInput,
    '你已经开始写了。'
  )
  const gap = firstLine(
    currentGuidance.value?.bullets?.[0] || response.nextSuggestedPrompts?.[0] || '',
    response.taskState === 'EXPLORE'
      ? '再补一句为什么。'
      : '再补这一处就能继续。'
  )
  const nextStep = firstLine(
    response.nextSuggestedPrompts?.[0] || recommendedUserActions.value[0]?.label || '',
    response.taskState === 'EXPLORE'
      ? '按这一点继续写。'
      : '顺着这一点继续。'
  )
  return {
    visible: true,
    title: '本轮反馈',
    mastered: `你已经说出了一个有效切口：${mastered}`,
    strengths: `你抓住了：${mastered}`,
    gap: `现在最该补的是：${gap}`,
    keyIssues: [gap].filter(Boolean).slice(0, 2),
    errorTags: response.taskState === 'EXPLORE' ? ['因果链'] : ['结构'],
    nextRestateAsk: nextStep,
    nextStep,
    primaryCta: 'apply_suggestion',
    actions: [
      { id: 'apply_suggestion', label: '补一句' },
      { id: 'restate', label: '重新表达' },
      { id: 'show_example', label: '看例子' },
    ],
  }
}

function buildSelfExplainFeedback(
  response: SelfExplanationResponse
): ExecutionGuideFeedbackModel {
  const weak = response.evaluation === 'WEAK'
  const miss =
    response.missingPoints?.[0] || '还差一句「为什么成立」或「少了会怎样」。'
  return {
    visible: true,
    title: '本轮反馈',
    mastered: weak
      ? '方向对了，先补这一处。'
      : '这一步已经讲清主要关系了。',
    strengths: weak ? '主线方向对，缺一处关键点。' : '关键关系已经讲顺。',
    gap: weak ? miss : '这一步可以进入检查。',
    keyIssues: weak ? [miss] : [],
    errorTags: weak ? ['表述缺口'] : [],
    nextRestateAsk: weak ? response.nextAction || '按这一点重写一小段。' : '直接答检查题。',
    nextStep: weak
      ? response.nextAction || '按这一点继续写。'
      : '直接答检查题。',
    primaryCta: weak ? 'apply_suggestion' : 'restate',
    actions: weak
      ? [
          { id: 'apply_suggestion', label: '补一句' },
          { id: 'restate', label: '重新表达' },
          { id: 'show_example', label: '看例子' },
        ]
      : [],
  }
}

function buildCheckpointFeedback(
  response: CheckpointResponse
): ExecutionGuideFeedbackModel {
  const passed = response.result !== 'FAIL'
  const reason = response.reason || '答案里还没有说清最关键的判断依据。'
  return {
    visible: true,
    title: '本轮反馈',
    mastered: passed
      ? '这道检查题已经答出来了。'
      : '前面已经够了，只差这一个判断。',
    strengths: passed ? '判断与依据对齐。' : '方向接近，差一句依据。',
    gap: passed ? '这一步过了。' : reason,
    keyIssues: passed ? [] : [reason],
    errorTags: passed ? [] : ['判断依据'],
    nextRestateAsk: passed
      ? '收住这一步，再继续。'
      : response.suggestedRemedialAction || '用一句话写清你的判断依据，再试一次。',
    nextStep: passed
      ? '收住这一步，再继续。'
      : response.suggestedRemedialAction || '回到主卡，补上当前暴露出的关键缺口。',
    primaryCta: passed ? 'restate' : 'apply_suggestion',
    actions: passed
      ? []
      : [
          { id: 'apply_suggestion', label: '补一句' },
          { id: 'restate', label: '重新表达' },
        ],
  }
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
    chatTurns.value.push({
      role: 'USER',
      content: text,
      detectedAction: response.detectedAction,
    })
    chatTurns.value.push({
      role: 'ASSISTANT',
      content: response.assistantReply,
    })
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

function openAiTutorAfterScaffold() {
  if (!task.value) return
  aiTutorStore.setContext({
    stepId: task.value.taskId,
    step: guidedStepPosition.value.current,
    knowledgeKey: knowledgePack.value?.id ?? 'unknown',
    knowledgeLabel: knowledgePack.value?.tutor.focusLabel ?? task.value.title,
    phaseCode: pageModel.value.header.phaseCode ?? 'STRUCTURE',
    phaseLabel: pageModel.value.header.phaseDisplayZh ?? '',
  })
  aiTutorStore.openPanel()
}

async function onPrefillPhaseChip(line: string) {
  draftInput.value = line
  await nextTick()
  openAiTutorAfterScaffold()
}

async function onSendScaffoldCard(prompt: string) {
  if (taskState.value === 'ORIENT' || taskState.value === 'EXPLORE') {
    await sendMessageWithContent(prompt)
    openAiTutorAfterScaffold()
    return
  }
  await onPrefillScaffoldCard(prompt)
}

async function onPrefillScaffoldCard(prompt: string) {
  draftInput.value = prompt
  await nextTick()
  const el = document.querySelector<HTMLTextAreaElement>('[data-testid="driving-seat-input"]')
  el?.focus()
  el?.setSelectionRange(el.value.length, el.value.length)
  openAiTutorAfterScaffold()
}

async function onAdvanceDrivingSeat() {
  if (!canAdvanceDriving.value) return
  const body = [
    `它是什么：${restateWhat.value.trim()}`,
    `它解决什么问题：${restateProblem.value.trim()}`,
    `它和谁有关：${restateRelate.value.trim()}`,
    '',
    '我认为当前知识点的最小框架已经搭好，请带我继续推进。',
  ].join('\n')
  advancing.value = true
  try {
    const res = await sendMessageWithContent(body)
    if (res) {
      microChecks.value = microCheckLabels.value.map(() => false)
    }
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
    chatTurns.value.push({
      role: 'USER',
      content,
    })
    taskState.value = response.taskState
    checkpointQuestion.value = response.checkpointQuestion || ''
    selfExplainMissingPoints.value = response.missingPoints ?? []
    draftInput.value = ''
    latestFeedback.value = buildSelfExplainFeedback(response)
    await loadGuidance()
    showToast(
      response.evaluation === 'WEAK'
        ? '先补这一处，再试一次。'
        : '这一步可以继续了。'
    )
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
    chatTurns.value.push({
      role: 'USER',
      content: answer,
    })
    taskState.value = response.taskState
    draftInput.value = ''
    latestFeedback.value = buildCheckpointFeedback(response)
    await loadGuidance()
    showToast(
      response.result === 'FAIL'
        ? response.reason || '还差一点，补一句再试。'
        : '这一步过了，继续收束。'
    )
  } catch (err) {
    showToast(getErrorMessage(err))
  } finally {
    submittingCheckpoint.value = false
  }
}

async function onComplete() {
  if (!store.sessionId || !task.value) return

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
  const mode = pageModel.value?.mainAction.mode
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

function handleFeedbackAction(actionId: string) {
  if (actionId === 'apply_suggestion') {
    fillDraftInput(latestFeedback.value?.gap || latestFeedback.value?.nextStep || '')
    return
  }
  if (actionId === 'restate') {
    const hint =
      latestFeedback.value?.nextRestateAsk ||
      latestFeedback.value?.nextStep ||
      latestFeedback.value?.gap ||
      ''
    draftInput.value = hint ? `${hint}\n\n` : ''
    void nextTick(() => {
      document.querySelector<HTMLTextAreaElement>('[data-testid="driving-seat-input"]')?.focus()
    })
    return
  }
  if (actionId === 'show_example') {
    fillDraftInput(
      tutorPromptFor(
        'minimal_example',
        task.value?.title || scaffold.value?.learningObjective || '当前任务'
      )
    )
  }
}

onMounted(() => {
  fetchTask()
})
</script>
