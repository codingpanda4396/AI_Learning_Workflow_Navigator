<template>
  <PageContainer>
    <AppTopBar current="task" />
    <main class="mx-auto max-w-[1280px] px-4 py-6 md:px-6 lg:px-8">
      <LoadingState v-if="loading && !task" message="正在加载任务脚手架..." />
      <ErrorState v-else-if="error" :message="error">
        <template #action>
          <SecondaryButton @click="fetchTask">重试</SecondaryButton>
        </template>
      </ErrorState>
      <EmptyState v-else-if="!task && !loading" message="当前没有可执行任务">
        <template #action>
          <SecondaryButton @click="router.push('/report')">查看报告</SecondaryButton>
        </template>
      </EmptyState>

      <section v-else-if="task" class="space-y-6">
        <header class="overflow-hidden rounded-[36px] border border-slate-200 bg-[radial-gradient(circle_at_top_left,_rgba(79,70,229,0.18),_transparent_34%),radial-gradient(circle_at_bottom_right,_rgba(16,185,129,0.14),_transparent_36%),linear-gradient(180deg,_rgba(248,250,252,1),_rgba(255,255,255,1))] p-6 shadow-card md:p-8">
          <div class="flex flex-wrap items-start justify-between gap-4">
            <div class="max-w-4xl">
              <p class="text-xs font-semibold uppercase tracking-[0.28em] text-primary">
                Scaffolded Execution
              </p>
              <h1 class="mt-3 text-3xl font-bold tracking-tight text-slate-950 md:text-4xl">
                {{ task.title }}
              </h1>
              <p v-if="cognitiveHeadlineBody && !legacyComplete" class="mt-3 text-base leading-7 text-slate-700">
                {{ cognitiveHeadlineBody }}
              </p>
              <p class="mt-4 max-w-3xl text-sm leading-6 text-slate-600 md:text-base">
                {{ currentActionLine }}
              </p>
            </div>
            <div class="flex flex-wrap items-center gap-2">
              <StatusBadge v-if="progress" :label="`任务 ${progress.currentIndex}/${progress.totalTasks}`" />
              <StatusBadge :label="stageViewModel.currentStageTitle" variant="warning" />
              <StatusBadge :label="taskStateLabel" :variant="taskStateVariant" />
            </div>
          </div>

          <div class="mt-7 grid gap-3 md:grid-cols-2 xl:grid-cols-4">
            <article
              v-for="item in stageViewModel.path"
              :key="item.code"
              class="rounded-[24px] border p-4 transition"
              :class="stageStripClass(item.status)"
            >
              <div class="flex flex-wrap items-center gap-2">
                <p class="font-mono text-[11px] font-bold tracking-tight">
                  {{ item.title }}
                </p>
                <span
                  v-if="item.status === 'current'"
                  class="rounded-full bg-amber-100 px-2 py-0.5 text-[10px] font-semibold text-amber-800"
                >
                  当前阶段
                </span>
                <span
                  v-else-if="item.status === 'done'"
                  class="rounded-full bg-emerald-100 px-2 py-0.5 text-[10px] font-semibold text-emerald-800"
                >
                  已经过
                </span>
              </div>
              <p class="mt-3 text-sm font-semibold text-text-primary">
                {{ item.label }}
              </p>
              <p class="mt-2 text-sm leading-6 text-text-secondary">
                {{ item.scanLine }}
              </p>
            </article>
          </div>
        </header>

        <TaskRunMainColumn
          :stage-code="stageViewModel.currentStageCode"
          :stage-title="stageViewModel.currentStageTitle"
          :stage-label="stageViewModel.currentStageLabel"
          :stage-objective="stageViewModel.currentStageObjective"
          :stage-deliverable="stageViewModel.currentStageDeliverable"
          :tutor-role="stageViewModel.currentStageTutorRole"
          :why-now="stageViewModel.currentWhyNow"
          :pass-condition="stageViewModel.currentPassCondition"
          :system-summary="stageViewModel.currentSystemSummary"
          :primary-action-title="stageViewModel.currentPrimaryActionTitle"
          :primary-action-description="stageViewModel.currentPrimaryActionDescription"
          :task-state="taskState"
          :completion-criteria="task.completionCriteria ?? []"
          :template-pool="templatePool"
          :structured-reply="structuredReply"
          :can-send-structured-message="canSendStructuredMessage"
          :coach-input-disabled="coachInputDisabled"
          :sending="sending"
          :chat-turns="displayChatTurns"
          :recommended-actions="recommendedUserActions"
          :self-explain-description="selfExplainDescription"
          :self-explain-badge="selfExplainBadge"
          :self-explain-badge-variant="selfExplainBadgeVariant"
          :self-explain-input="selfExplainInput"
          :can-submit-self-explanation="canSubmitSelfExplanation"
          :submitting-self="submittingSelf"
          :self-explain-missing-points="selfExplainMissingPoints"
          :checkpoint-badge="checkpointBadge"
          :checkpoint-badge-variant="checkpointBadgeVariant"
          :checkpoint-question="checkpointQuestion"
          :checkpoint-answer="checkpointAnswer"
          :submitting-checkpoint="submittingCheckpoint"
          :summary-unlocked="summaryUnlocked"
          :closure-summary="closureSummary"
          :closure-point1="closurePoint1"
          :closure-point2="closurePoint2"
          :closure-next="closureNext"
          :learner-reflection="completeForm.learnerReflection"
          :completion-status="completeForm.completionStatus"
          :completing="completing"
          @update:structured-reply="structuredReply = $event"
          @update:self-explain-input="selfExplainInput = $event"
          @update:checkpoint-answer="checkpointAnswer = $event"
          @update:closure-summary="closureSummary = $event"
          @update:closure-point1="closurePoint1 = $event"
          @update:closure-point2="closurePoint2 = $event"
          @update:closure-next="closureNext = $event"
          @update:learner-reflection="completeForm.learnerReflection = $event"
          @update:completion-status="completeForm.completionStatus = $event"
          @fill-message="fillStructuredReply"
          @send-message="sendMessage"
          @submit-self-explanation="submitSelfExplanation"
          @submit-checkpoint="submitCheckpoint"
          @complete="onComplete"
        />

        <section class="grid gap-4 lg:grid-cols-[minmax(0,1fr),340px]">
          <article class="rounded-[28px] border border-border bg-white p-5 shadow-card">
            <div class="flex flex-wrap items-center justify-between gap-3">
              <div>
                <p class="text-xs font-semibold uppercase tracking-[0.22em] text-text-secondary">
                  Minimal Help
                </p>
                <h2 class="mt-2 text-lg font-semibold text-text-primary">
                  需要最小帮助时再展开
                </h2>
              </div>
              <StatusBadge :label="currentStatus" :variant="taskStateVariant" />
            </div>

            <p class="mt-4 text-sm leading-6 text-text-secondary">
              默认先按上面的阶段主卡推进。如果你确实卡住，再用下面这些受约束动作缩小问题。
            </p>

            <div class="mt-5 grid gap-3 sm:grid-cols-2">
              <button
                v-for="action in tutorActions"
                :key="action.id"
                type="button"
                class="rounded-[18px] border border-border bg-slate-50/80 px-4 py-3 text-left transition hover:border-primary/35 hover:bg-primary/5 disabled:cursor-not-allowed disabled:opacity-50"
                :disabled="coachInputDisabled"
                @click="applyTutorAction(action.id)"
              >
                <p class="text-sm font-semibold text-text-primary">{{ action.label }}</p>
                <p class="mt-1 text-xs leading-5 text-text-secondary">{{ action.description }}</p>
              </button>
            </div>

            <div v-if="recommendedUserActions.length" class="mt-5 rounded-[20px] border border-slate-200 bg-slate-50/80 p-4">
              <p class="text-sm font-semibold text-text-primary">系统更建议你这样答</p>
              <div class="mt-3 flex flex-wrap gap-2">
                <StatusBadge
                  v-for="action in recommendedUserActions"
                  :key="action.code"
                  :label="action.label"
                />
              </div>
            </div>
          </article>

          <article class="rounded-[28px] border border-border bg-white p-5 shadow-card">
            <p class="text-xs font-semibold uppercase tracking-[0.22em] text-text-secondary">
              Progress Snapshot
            </p>
            <h2 class="mt-2 text-lg font-semibold text-text-primary">
              系统为什么这样推进
            </h2>
            <p class="mt-3 text-sm leading-6 text-text-secondary">
              {{ stageViewModel.currentSystemSummary }}
            </p>

            <div class="mt-5 space-y-4">
              <div class="rounded-[20px] border border-emerald-100 bg-emerald-50/70 p-4">
                <p class="text-sm font-semibold text-emerald-900">已经具备</p>
                <ul class="mt-3 list-disc space-y-2 pl-5 text-sm leading-6 text-emerald-900">
                  <li v-for="(item, index) in completedPoints" :key="`done-${index}`">
                    {{ item }}
                  </li>
                </ul>
              </div>

              <div class="rounded-[20px] border border-amber-100 bg-amber-50/80 p-4">
                <p class="text-sm font-semibold text-amber-900">还差什么</p>
                <ul class="mt-3 list-disc space-y-2 pl-5 text-sm leading-6 text-amber-900">
                  <li v-for="(item, index) in missingPoints" :key="`miss-${index}`">
                    {{ item }}
                  </li>
                </ul>
              </div>

              <div class="rounded-[20px] border border-slate-200 bg-slate-50/70 p-4">
                <p class="text-sm font-semibold text-text-primary">下一步会发生什么</p>
                <p class="mt-2 text-sm leading-6 text-text-secondary">
                  {{ nextStepLine }}
                </p>
              </div>
            </div>
          </article>
        </section>

        <details
          v-if="
            task.completionCriteria?.length ||
            (scaffold?.whyThisTask && !legacyComplete) ||
            scaffold?.antiPatterns?.length ||
            scaffold?.completionSignals?.length
          "
          class="rounded-[28px] border border-border bg-white shadow-card"
        >
          <summary class="cursor-pointer px-5 py-4 text-sm font-medium text-text-primary">
            查看补充说明
          </summary>
          <div class="grid gap-5 border-t border-border px-5 py-5 md:grid-cols-3">
            <div v-if="scaffold?.whyThisTask && !legacyComplete">
              <p class="text-sm font-semibold text-text-primary">为什么是这一步</p>
              <p class="mt-2 text-sm leading-6 text-text-secondary">{{ scaffold.whyThisTask }}</p>
            </div>
            <div v-if="scaffold?.completionSignals?.length">
              <p class="text-sm font-semibold text-text-primary">达标信号</p>
              <ul class="mt-2 list-disc space-y-2 pl-5 text-sm leading-6 text-text-secondary">
                <li v-for="(item, index) in scaffold.completionSignals" :key="index">{{ item }}</li>
              </ul>
            </div>
            <div v-if="scaffold?.antiPatterns?.length">
              <p class="text-sm font-semibold text-text-primary">要避免这些表现</p>
              <ul class="mt-2 list-disc space-y-2 pl-5 text-sm leading-6 text-text-secondary">
                <li v-for="(item, index) in scaffold.antiPatterns" :key="index">{{ item }}</li>
              </ul>
            </div>
          </div>
        </details>
      </section>
    </main>
  </PageContainer>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import AppTopBar from '@/components/layout/AppTopBar.vue'
import PageContainer from '@/components/layout/PageContainer.vue'
import TaskRunMainColumn from '@/components/task-run/TaskRunMainColumn.vue'
import EmptyState from '@/components/ui/EmptyState.vue'
import ErrorState from '@/components/ui/ErrorState.vue'
import LoadingState from '@/components/ui/LoadingState.vue'
import SecondaryButton from '@/components/ui/SecondaryButton.vue'
import StatusBadge from '@/components/ui/StatusBadge.vue'
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
import {
  fallbackGuidanceForState,
  TASK_STATE_META,
  TUTOR_ACTIONS,
  tutorPromptFor,
} from '@/constants/taskRunUi'
import { showToast } from '@/stores/toast'
import { useWorkflowStore } from '@/stores/workflow'
import type {
  CurrentGuidanceBlock,
  CurrentTaskItem,
  ProgressItem,
  RecommendedUserActionItem,
  TaskScaffoldResponse,
} from '@/types/dto'
import { TaskCompletionStatus, type TaskCompletionStatusType } from '@/types/enums'
import { buildCompleteTaskPayload } from '@/utils/buildCompleteTaskPayload'
import { getCurrentActionInstruction, getTaskCognitiveHeadlineBody } from '@/utils/taskGuidedSteps'
import { buildTaskRunStageViewModel } from '@/utils/taskRunStages'

interface ChatTurn {
  role: 'USER' | 'ASSISTANT'
  content: string
  detectedAction?: string
}

interface StructuredReplyDraft {
  understanding: string
  uncertainty: string
  confirmation: string
}

interface DisplayChatTurn {
  role: 'USER' | 'ASSISTANT'
  speaker: string
  content: string
  actionLabel: string
  actionVariant: 'default' | 'success' | 'warning' | 'error'
}

const route = useRoute()
const router = useRouter()
const store = useWorkflowStore()

const loading = ref(true)
const completing = ref(false)
const sending = ref(false)
const submittingSelf = ref(false)
const submittingCheckpoint = ref(false)
const error = ref<string | null>(null)
const task = ref<CurrentTaskItem | null>(null)
const progress = ref<ProgressItem | null>(null)
const scaffold = ref<TaskScaffoldResponse | null>(null)
const taskState = ref('ORIENT')
const exploreRoundCount = ref(0)
const structuredReply = ref<StructuredReplyDraft>({
  understanding: '',
  uncertainty: '',
  confirmation: '',
})
const selfExplainInput = ref('')
const checkpointAnswer = ref('')
const checkpointQuestion = ref('')
const legacyComplete = ref(false)
const taskStartedAt = ref(Date.now())
const canComplete = ref(false)
const closureSummary = ref('')
const closurePoint1 = ref('')
const closurePoint2 = ref('')
const closureNext = ref('')
const selfExplainMissingPoints = ref<string[]>([])
const guidancePhase = ref('')
const currentGuidance = ref<CurrentGuidanceBlock | null>(null)
const recommendedUserActions = ref<RecommendedUserActionItem[]>([])
const chatTurns = ref<ChatTurn[]>([])
const completeForm = ref<{
  completionStatus: TaskCompletionStatusType
  learnerReflection: string
}>({
  completionStatus: TaskCompletionStatus.COMPLETED,
  learnerReflection: '',
})

const humanActionLabels: Record<string, string> = {
  ASK_FOR_EXPLANATION: '主动求解释',
  ASK_FOR_EXAMPLE: '请求例子',
  ASK_FOR_COMPARISON: '请求对比',
  ASK_FOR_SIMPLIFICATION: '请求简化',
  SELF_EXPLANATION: '自我表达',
  CONFUSION_SIGNAL: '暴露困惑',
  SEEK_DIRECT_ANSWER: '想直接拿答案',
  OFF_TOPIC: '偏题信号',
  GENERIC: '一般提问',
  ANSWER_CHECK: '尝试过检查点',
}

const cognitiveHeadlineBody = computed(() =>
  legacyComplete.value ? '' : getTaskCognitiveHeadlineBody(scaffold.value)
)
const exploreUnitPrompts = computed(
  () => scaffold.value?.cognitiveUnits?.find((item) => item.unitId === 'explore')?.prompts ?? []
)
const requiredScaffoldPrompts = computed(() => exploreUnitPrompts.value.filter((item) => item.required))
const optionalScaffoldPrompts = computed(() => exploreUnitPrompts.value.filter((item) => !item.required))
const fallbackQuickPrompts = computed(() =>
  exploreUnitPrompts.value.length
    ? []
    : [
        ...(scaffold.value?.recommendedAskTemplates ?? []),
        ...(scaffold.value?.recommendedFollowupTemplates ?? []),
        ...((scaffold.value?.selfCheckTemplates ?? []).slice(0, 2)),
      ]
        .filter(Boolean)
        .slice(0, 6)
)
const templatePool = computed(() => [
  ...requiredScaffoldPrompts.value.map((item) => ({
    key: `required-${item.promptId ?? item.prompt}`,
    prompt: item.prompt,
    required: true,
  })),
  ...optionalScaffoldPrompts.value.map((item) => ({
    key: `optional-${item.promptId ?? item.prompt}`,
    prompt: item.prompt,
    required: false,
  })),
  ...fallbackQuickPrompts.value.map((prompt, index) => ({
    key: `fallback-${index}`,
    prompt,
    required: false,
  })),
])
const currentActionLine = computed(() =>
  getCurrentActionInstruction(
    taskState.value,
    exploreRoundCount.value,
    scaffold.value,
    task.value?.goal ?? '',
    legacyComplete.value
  )
)
const taskStateLabel = computed(() => TASK_STATE_META[taskState.value]?.label ?? taskState.value)
const taskStateVariant = computed(() => TASK_STATE_META[taskState.value]?.variant ?? 'default')
const guidanceTitle = computed(() => currentGuidance.value?.title ?? '')
const guidanceBullets = computed(() => currentGuidance.value?.bullets ?? [])
const coachInputDisabled = computed(
  () =>
    sending.value || taskState.value === 'CHECK' || taskState.value === 'PASS' || legacyComplete.value
)
const canSubmitSelfExplanation = computed(
  () =>
    !legacyComplete.value &&
    ['EXPLORE', 'SELF_EXPLAIN', 'REMEDIAL'].includes(taskState.value) &&
    exploreRoundCount.value >= 2
)
const summaryUnlocked = computed(() => legacyComplete.value || taskState.value === 'PASS')
const selfExplainDescription = computed(() =>
  canSubmitSelfExplanation.value
    ? '现在轮到你用自己的话讲清楚。系统会先看你是否能独立说明，再决定能否进入检查点。'
    : '至少完成两轮引导后，这里才会成为主舞台。'
)
const selfExplainBadge = computed(() => {
  if (taskState.value === 'CHECK' || taskState.value === 'PASS') return '已完成'
  return canSubmitSelfExplanation.value ? '现在就讲' : '等待解锁'
})
const selfExplainBadgeVariant = computed(() =>
  taskState.value === 'CHECK' || taskState.value === 'PASS'
    ? 'success'
    : canSubmitSelfExplanation.value
      ? 'warning'
      : 'default'
)
const checkpointBadge = computed(() => {
  if (taskState.value === 'PASS') return '已通过'
  if (taskState.value === 'CHECK') return '待回答'
  return '未开始'
})
const checkpointBadgeVariant = computed(() =>
  taskState.value === 'PASS' ? 'success' : taskState.value === 'CHECK' ? 'warning' : 'default'
)
const remedialHints = computed(() =>
  selfExplainMissingPoints.value.length ? selfExplainMissingPoints.value : scaffold.value?.fallbackHints ?? []
)
const tutorActions = TUTOR_ACTIONS

const protocolPassCondition = computed(() => {
  if (scaffold.value?.completionSignals?.length) return scaffold.value.completionSignals[0]
  if (task.value?.completionCriteria?.length) return task.value.completionCriteria[0]
  if (taskState.value === 'CHECK') return '你能独立答出检查题，说明不是只跟着提示在走。'
  return '你能用自己的话说清这一步，并指出关键缺口。'
})

const stageViewModel = computed(() =>
  buildTaskRunStageViewModel({
    taskState: taskState.value,
    legacyComplete: legacyComplete.value,
    canSubmitSelfExplanation: canSubmitSelfExplanation.value,
    taskGoal: task.value?.goal ?? '',
    scaffold: scaffold.value,
    guidanceTitle: guidanceTitle.value,
    guidanceBullets: guidanceBullets.value,
    currentActionLine: currentActionLine.value,
    passCondition: protocolPassCondition.value,
  })
)

const displayChatTurns = computed<DisplayChatTurn[]>(() =>
  chatTurns.value.map((turn) => {
    if (turn.role === 'USER') {
      return {
        role: 'USER',
        speaker: '你的回答',
        content: turn.content,
        actionLabel: humanActionLabels[turn.detectedAction ?? ''] ?? '结构化表达',
        actionVariant: 'default',
      }
    }
    const assistantAction = inferAssistantAction(turn.content)
    return {
      role: 'ASSISTANT',
      speaker: '系统引导',
      content: turn.content,
      actionLabel: assistantAction.label,
      actionVariant: assistantAction.variant,
    }
  })
)

const canSendStructuredMessage = computed(() => !!composeStructuredMessage().trim())
const currentStatus = computed(() => {
  if (taskState.value === 'PASS') return '已达到通过条件'
  if (taskState.value === 'CHECK') return '正在独立检查'
  if (taskState.value === 'REMEDIAL') return '正在按缺口纠偏'
  if (canSubmitSelfExplanation.value) return '等待你独立讲清楚'
  if (chatTurns.value.length > 0) return '系统在继续追问'
  return '等待第一轮作答'
})

const completedPoints = computed(() => {
  const items: string[] = []
  if (task.value?.goal) items.push('你已经进入明确任务，不是开放式闲聊。')
  if (chatTurns.value.some((turn) => turn.role === 'USER')) {
    items.push('你已经开始用自己的话表达当前理解。')
  }
  if (canSubmitSelfExplanation.value || ['CHECK', 'PASS'].includes(taskState.value)) {
    items.push('系统已经拿到足够证据，可以要求你独立解释。')
  }
  if (taskState.value === 'PASS') {
    items.push('你已经达到当前步骤的通过门槛。')
  }
  return items.slice(0, 3).length ? items.slice(0, 3) : ['当前目标已经明确，可以开始第一轮作答。']
})

const missingPoints = computed(() => {
  if (taskState.value === 'PASS') return ['进入总结区，沉淀一句结论和下一步动作。']
  if (selfExplainMissingPoints.value.length) return selfExplainMissingPoints.value.slice(0, 3)
  if (taskState.value === 'CHECK') {
    return [checkpointQuestion.value || '用自己的话独立答出当前检查题。']
  }
  if (taskState.value === 'REMEDIAL' && remedialHints.value.length) {
    return remedialHints.value.slice(0, 3)
  }
  if (scaffold.value?.completionSignals?.length) {
    return scaffold.value.completionSignals.slice(0, 3)
  }
  if (scaffold.value?.antiPatterns?.length) {
    return scaffold.value.antiPatterns.slice(0, 3)
  }
  return ['先明确整体理解，再把最不确定的地方主动说出来。']
})

const nextStepLine = computed(() => {
  if (taskState.value === 'PASS') return '下一步：进入总结区，沉淀这次任务的收获并切到后续任务。'
  if (taskState.value === 'CHECK') return '下一步：答对检查题后，系统会放你进入总结区。'
  if (taskState.value === 'REMEDIAL') return '下一步：先补上当前缺口，再回到主线继续推进。'
  if (canSubmitSelfExplanation.value) return '下一步：把机制讲清楚，系统再决定是否给检查题。'
  return '下一步：继续按脚手架作答，系统会决定追问还是给最小提示。'
})

function stageStripClass(status: 'done' | 'current' | 'upcoming') {
  if (status === 'current') {
    return 'border-primary/35 bg-gradient-to-br from-primary/8 via-white to-white shadow-[0_16px_36px_rgba(79,70,229,0.12)]'
  }
  if (status === 'done') {
    return 'border-emerald-200 bg-emerald-50/80 text-emerald-950'
  }
  return 'border-slate-200 bg-white/88'
}

function inferAssistantAction(content: string): {
  label: string
  variant: 'default' | 'success' | 'warning' | 'error'
} {
  const text = content.toLowerCase()
  if (/为什么|先说说|你觉得|试着|想一想|能不能/.test(text)) {
    return { label: '追问', variant: 'default' }
  }
  if (/还缺|少了|不够|不是|纠正|偏了|注意/.test(text)) {
    return { label: '纠偏', variant: 'warning' }
  }
  if (/提示|先看|可以从|想想|一步/.test(text)) {
    return { label: '最小提示', variant: 'warning' }
  }
  if (/检查|确认|判断|通过|是否/.test(text)) {
    return { label: '检查点', variant: 'success' }
  }
  if (/总结|所以|因此|收束|回顾/.test(text)) {
    return { label: '总结', variant: 'success' }
  }
  return { label: '引导', variant: 'default' }
}

function isUiChatMessage(
  message: { role: 'USER' | 'ASSISTANT' | 'SYSTEM'; content: string; detectedAction?: string }
): message is ChatTurn {
  return message.role === 'USER' || message.role === 'ASSISTANT'
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

function resetStructuredReply() {
  structuredReply.value = {
    understanding: '',
    uncertainty: '',
    confirmation: '',
  }
}

function composeStructuredMessage() {
  const parts = [
    ['我现在的理解', structuredReply.value.understanding],
    ['我还不确定', structuredReply.value.uncertainty],
    ['我想确认', structuredReply.value.confirmation],
  ]
    .map(([label, value]) => [label, value.trim()] as const)
    .filter(([, value]) => value)
    .map(([label, value]) => `${label}：${value}`)

  return parts.join('\n')
}

function fillStructuredReply(prompt: string) {
  const text = prompt.trim()
  if (!text) return

  if (/不确定|卡住|不会|困惑|疑问/.test(text)) {
    structuredReply.value.uncertainty = text
    return
  }
  if (/请|能否|想确认|例子|提示|比较|检查/.test(text)) {
    structuredReply.value.confirmation = text
    return
  }
  if (!structuredReply.value.understanding.trim()) {
    structuredReply.value.understanding = text
    return
  }
  if (!structuredReply.value.uncertainty.trim()) {
    structuredReply.value.uncertainty = text
    return
  }
  structuredReply.value.confirmation = text
}

function applyFallbackGuidance() {
  const fallback = fallbackGuidanceForState(taskState.value, canSubmitSelfExplanation.value)
  guidancePhase.value = fallback.phase
  currentGuidance.value = fallback.guidance
  recommendedUserActions.value = fallback.actions
}

async function loadGuidance() {
  if (!store.sessionId) return
  try {
    const data = await getCurrentTaskGuidance(store.sessionId)
    guidancePhase.value = data.guidancePhase ?? ''
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
  canComplete.value = data.executionSnapshot?.canComplete || false
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
  resetStructuredReply()
  taskStartedAt.value = Date.now()
  try {
    const data = await getTaskScaffold(taskId, store.sessionId)
    scaffold.value = data
    legacyComplete.value = false
    syncRuntimeFromScaffold(data)
    chatTurns.value = (data.recentMessages || [])
      .filter(isUiChatMessage)
      .map((message) => ({
        role: message.role,
        content: message.content,
        detectedAction: message.detectedAction || undefined,
      }))
    selfExplainInput.value = ''
    checkpointAnswer.value = ''
    selfExplainMissingPoints.value = []
    await loadGuidance()
  } catch {
    scaffold.value = null
    legacyComplete.value = true
    taskState.value = 'PASS'
    canComplete.value = true
    applyFallbackGuidance()
    showToast('任务脚手架暂不可用，已切换为简化完成模式。')
  }
}

async function sendMessage() {
  if (!store.sessionId || !task.value) return
  const content = composeStructuredMessage().trim()
  if (!content) return

  sending.value = true
  try {
    const response = await postTaskMessage(task.value.taskId, store.sessionId, content)
    chatTurns.value.push({
      role: 'USER',
      content,
      detectedAction: response.detectedAction,
    })
    chatTurns.value.push({
      role: 'ASSISTANT',
      content: response.assistantReply,
    })
    resetStructuredReply()
    taskState.value = response.taskState
    if (response.taskState === 'EXPLORE') exploreRoundCount.value++
    if (response.guidancePhase) guidancePhase.value = response.guidancePhase
    if (response.recommendedUserActions) {
      recommendedUserActions.value = response.recommendedUserActions
    }
    if (response.whetherCanComplete != null) {
      canComplete.value = response.whetherCanComplete
    }
    await loadGuidance()
  } catch (err) {
    showToast(getErrorMessage(err))
  } finally {
    sending.value = false
  }
}

async function submitSelfExplanation() {
  if (!store.sessionId || !task.value) return
  submittingSelf.value = true
  try {
    const response = await postSelfExplanation(
      task.value.taskId,
      store.sessionId,
      selfExplainInput.value.trim()
    )
    taskState.value = response.taskState
    if (response.checkpointQuestion) checkpointQuestion.value = response.checkpointQuestion
    if (response.evaluation === 'WEAK') {
      showToast('还差一点，把缺口补上再试一次。')
      selfExplainMissingPoints.value = response.missingPoints ?? []
    } else {
      selfExplainMissingPoints.value = []
      showToast('这段自我解释通过了，继续进入下一步。')
    }
    await loadGuidance()
  } catch (err) {
    showToast(getErrorMessage(err))
  } finally {
    submittingSelf.value = false
  }
}

async function submitCheckpoint() {
  if (!store.sessionId || !task.value) return
  submittingCheckpoint.value = true
  try {
    const response = await postCheckpoint(
      task.value.taskId,
      store.sessionId,
      checkpointAnswer.value.trim()
    )
    taskState.value = response.taskState
    canComplete.value = response.taskState === 'PASS'
    if (response.result === 'FAIL') {
      showToast(response.reason || '还差一点，回去补一小段再来。')
      checkpointAnswer.value = ''
    } else {
      showToast('检查通过，可以进入总结区。')
    }
    await loadGuidance()
  } catch (err) {
    showToast(getErrorMessage(err))
  } finally {
    submittingCheckpoint.value = false
  }
}

async function applyTutorAction(actionId: string) {
  fillStructuredReply(
    tutorPromptFor(actionId, task.value?.title || scaffold.value?.learningObjective || '当前任务')
  )
  await nextTick()
  document.getElementById('guided-response')?.scrollIntoView({
    behavior: 'smooth',
    block: 'center',
  })
}

async function onComplete() {
  if (!store.sessionId || !task.value) return

  if (!legacyComplete.value) {
    if (closureSummary.value.trim().length < 10) {
      showToast('请至少用 10 个字总结这次任务收获。')
      return
    }
    if (!closurePoint1.value.trim() || !closurePoint2.value.trim()) {
      showToast('请填写两个收获要点。')
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

onMounted(() => {
  fetchTask()
})
</script>
