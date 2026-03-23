<template>
  <PageContainer>
    <AppTopBar current="task" />
    <main class="mx-auto max-w-2xl px-6 py-8">
      <LoadingState v-if="loading && !task" message="加载任务中..." />
      <ErrorState v-else-if="error" :message="error">
        <template #action>
          <SecondaryButton @click="fetchTask">重试</SecondaryButton>
        </template>
      </ErrorState>
      <EmptyState
        v-else-if="!task && !loading"
        message="暂无任务"
      >
        <template #action>
          <SecondaryButton @click="router.push('/report')">
            查看报告
          </SecondaryButton>
        </template>
      </EmptyState>

      <WorkflowPageScaffold v-else-if="task">
        <template #title>
          <div class="flex flex-wrap items-start justify-between gap-3">
            <div>
              <template v-if="cognitiveHeadlineBody && !legacyComplete">
                <p class="text-sm font-semibold text-primary">
                  这一步我们一起搞懂
                </p>
                <p class="mt-0.5 text-base font-medium text-text-primary">
                  {{ cognitiveHeadlineBody }}
                </p>
              </template>
              <h1
                class="text-2xl font-bold text-text-primary md:text-3xl"
                :class="cognitiveHeadlineBody && !legacyComplete ? 'mt-2' : ''"
              >
                {{ task.title }}
              </h1>
              <p
                v-if="progress"
                class="mt-1 text-sm text-text-secondary"
              >
                进度：第 {{ progress.currentIndex }} 步 / 共 {{ progress.totalTasks }} 步
              </p>
              <p class="mt-2 text-sm font-medium text-text-primary">
                当前环节：{{ currentStepLabel }}
              </p>
            </div>
            <StatusBadge
              v-if="progress"
              :label="`${progress.currentIndex}/${progress.totalTasks}`"
            />
          </div>
        </template>

        <template #primary>
          <div
            class="rounded-card border-2 border-primary/25 bg-primary/5 p-5 text-sm leading-relaxed text-text-primary whitespace-pre-line"
          >
            {{ currentActionLine }}
          </div>

          <div v-if="!legacyComplete" class="mt-6">
            <p class="mb-2 text-xs font-medium text-text-secondary">
              学习进度（不用细看）
            </p>
            <GuidedStepCard
              :steps="guidedSteps"
              :current-step-id="currentGuidedStepId"
              @select="onGuidedSelect"
            />
          </div>

          <div id="guided-orient" class="scroll-mt-28 mt-8">
            <FormCard>
              <SectionHeader>这步要达成什么</SectionHeader>
              <p class="text-text-primary">{{ task.goal }}</p>
            </FormCard>
          </div>

          <div
            v-if="scaffold && !legacyComplete"
            id="guided-explore"
            class="scroll-mt-28 mt-6 space-y-4"
          >
            <FormCard
              v-if="requiredScaffoldPrompts.length || optionalScaffoldPrompts.length"
            >
              <SectionHeader>如果你有点不确定</SectionHeader>
              <p
                v-if="requiredScaffoldPrompts.length"
                class="mb-2 text-xs text-text-secondary"
              >
                可以从这里起步（点一下会填进对话区）：
              </p>
              <div
                v-if="requiredScaffoldPrompts.length"
                class="mb-4 flex flex-wrap gap-2"
              >
                <button
                  v-for="p in requiredScaffoldPrompts"
                  :key="'req' + (p.promptId || p.prompt)"
                  type="button"
                  class="rounded-input border-2 border-primary/40 bg-primary/5 px-3 py-1.5 text-left text-xs font-medium text-primary hover:bg-primary/10"
                  @click="messageInput = p.prompt"
                >
                  {{
                    p.prompt.length > 48
                      ? p.prompt.slice(0, 48) + '…'
                      : p.prompt
                  }}
                </button>
              </div>
              <template v-if="optionalScaffoldPrompts.length">
                <p class="mb-2 text-xs text-text-secondary">还想多试几句（选做）</p>
                <div class="flex flex-wrap gap-2">
                  <button
                    v-for="p in optionalScaffoldPrompts"
                    :key="'opt' + (p.promptId || p.prompt)"
                    type="button"
                    class="rounded-input border border-border bg-white px-3 py-1.5 text-left text-xs text-text-primary hover:bg-gray-50"
                    @click="messageInput = p.prompt"
                  >
                    {{
                      p.prompt.length > 40
                        ? p.prompt.slice(0, 40) + '…'
                        : p.prompt
                    }}
                  </button>
                </div>
              </template>
            </FormCard>
            <FormCard v-else-if="fallbackQuickPrompts.length">
              <SectionHeader>如果你有点不确定</SectionHeader>
              <p class="mb-2 text-xs text-text-secondary">
                可以从这里起步（点一下会填进对话区）：
              </p>
              <div class="flex flex-wrap gap-2">
                <button
                  v-for="(t, i) in fallbackQuickPrompts"
                  :key="'q' + i"
                  type="button"
                  class="rounded-input border border-primary/30 bg-primary/5 px-3 py-1.5 text-left text-xs text-primary hover:bg-primary/10"
                  @click="messageInput = t"
                >
                  {{ t.length > 40 ? t.slice(0, 40) + '…' : t }}
                </button>
              </div>
            </FormCard>

            <TaskCoachPanel
              v-model:message-input="messageInput"
              collapsible
              :chat-turns="chatTurns"
              :sending="sending"
              :input-disabled="coachInputDisabled"
              placeholder="说说你的理解，或告诉我卡在哪里"
              :action-labels="actionLabels"
              @send="sendMessage"
            />
          </div>

          <div
            v-if="scaffold && !legacyComplete"
            id="guided-explain"
            class="scroll-mt-28 mt-6"
          >
            <FormCard
              v-if="exploreRoundCount >= 2 && taskState === 'EXPLORE'"
            >
              <SectionHeader>试试看你能不能讲清楚</SectionHeader>
              <p class="mb-1 text-sm font-medium text-text-primary">
                用你自己的话讲一遍本任务在说什么（不用很标准，说清楚就好）。
              </p>
              <p class="mb-2 text-xs text-text-secondary">
                大约三五句话即可（不少于约 35 字）。没有标准答案，你理解对就行。
              </p>
              <textarea
                v-model="selfExplainInput"
                rows="3"
                class="mb-3 w-full rounded-input border border-border px-3 py-2 text-text-primary"
                placeholder="我这样理解：……"
              />
              <PrimaryButton
                :loading="submittingSelf"
                :disabled="!selfExplainInput.trim()"
                @click="submitSelfExplanation"
              >
                我讲完了，继续
              </PrimaryButton>
              <div
                v-if="selfExplainMissingPoints.length"
                class="mt-3 rounded-input border border-amber-200 bg-amber-50/90 p-3 text-xs text-amber-950"
              >
                <p class="font-medium">还可以再具体一点</p>
                <ul class="mt-1 list-disc space-y-0.5 pl-4">
                  <li
                    v-for="(pt, idx) in selfExplainMissingPoints"
                    :key="'mp' + idx"
                  >
                    {{ pt }}
                  </li>
                </ul>
              </div>
            </FormCard>
          </div>

          <div
            v-if="scaffold && !legacyComplete"
            id="guided-check"
            class="scroll-mt-28 mt-6"
          >
            <FormCard v-if="taskState === 'CHECK'">
              <SectionHeader>快速练一下</SectionHeader>
              <p class="mb-2 font-medium text-text-primary">{{ checkpointQuestion }}</p>
              <textarea
                v-model="checkpointAnswer"
                rows="2"
                class="mb-3 w-full rounded-input border border-border px-3 py-2"
                placeholder="用一两句话说说你的想法"
              />
              <PrimaryButton
                :loading="submittingCheckpoint"
                :disabled="!checkpointAnswer.trim()"
                @click="submitCheckpoint"
              >
                写好了，继续
              </PrimaryButton>
            </FormCard>
            <div
              v-if="taskState === 'REMEDIAL'"
              class="rounded-card border border-amber-200 bg-amber-50 p-4 text-sm text-amber-900"
            >
              还差一小步：再和导师聊一轮，把卡点说清楚后，可以再点「我讲完了，继续」试一次。
            </div>
          </div>

          <div id="guided-wrap" class="scroll-mt-28 mt-6">
            <FormCard>
              <SectionHeader>好了就进入下一步</SectionHeader>
              <p
                v-if="!legacyComplete && taskState !== 'PASS'"
                class="text-sm text-amber-800"
              >
                还差一小步：先在上面对话里把卡点聊清楚，再用自己的话讲一遍，最后快速练一下，然后回到这里。
              </p>

              <template v-else-if="legacyComplete || taskState === 'PASS'">
                <p class="mb-4 text-sm text-text-secondary">
                  {{
                    legacyComplete
                      ? '选好完成状态，填完下面几项就可以继续。'
                      : '核心流程已经走完。用一句话收个尾，就能进入下一步。'
                  }}
                </p>

                <div v-if="!legacyComplete" class="space-y-4">
                  <div>
                    <label class="mb-1.5 block text-sm font-medium text-text-primary">
                      一句话说说你学会了什么
                    </label>
                    <textarea
                      v-model="closureSummary"
                      rows="2"
                      class="w-full rounded-input border border-border px-3 py-2 text-text-primary"
                      placeholder="随便写写，至少 10 个字即可"
                    />
                  </div>
                  <div class="grid gap-4 sm:grid-cols-2">
                    <div>
                      <label class="mb-1.5 block text-sm font-medium text-text-primary">
                        收获要点 1
                      </label>
                      <input
                        v-model="closurePoint1"
                        type="text"
                        class="w-full rounded-input border border-border px-3 py-2 text-text-primary"
                        placeholder="例如：核心定义"
                      />
                    </div>
                    <div>
                      <label class="mb-1.5 block text-sm font-medium text-text-primary">
                        收获要点 2
                      </label>
                      <input
                        v-model="closurePoint2"
                        type="text"
                        class="w-full rounded-input border border-border px-3 py-2 text-text-primary"
                        placeholder="例如：易错点"
                      />
                    </div>
                  </div>
                  <div>
                    <label class="mb-1.5 block text-sm font-medium text-text-primary">
                      接下来怎么练（可选）
                    </label>
                    <input
                      v-model="closureNext"
                      type="text"
                      class="w-full rounded-input border border-border px-3 py-2 text-text-primary"
                      placeholder="留空会用一轮默认建议"
                    />
                  </div>
                </div>

                <details
                  class="mt-4 rounded-input border border-border bg-gray-50/80 p-3 text-sm"
                >
                  <summary class="cursor-pointer font-medium text-text-primary">
                    更多选项
                  </summary>
                  <div class="mt-3 space-y-3 border-t border-border pt-3">
                    <div>
                      <label class="mb-1.5 block text-xs font-medium text-text-primary">
                        完成状态
                      </label>
                      <select
                        v-model="completeForm.completionStatus"
                        class="w-full rounded-input border border-border px-3 py-2 text-text-primary"
                      >
                        <option
                          v-for="(label, val) in taskCompletionStatusLabels"
                          :key="val"
                          :value="val"
                        >
                          {{ label }}
                        </option>
                      </select>
                    </div>
                    <div>
                      <label class="mb-1.5 block text-xs font-medium text-text-primary">
                        补充反思（可选）
                      </label>
                      <textarea
                        v-model="completeForm.learnerReflection"
                        rows="2"
                        class="w-full rounded-input border border-border px-3 py-2 text-text-primary"
                        placeholder="还想记下的内容"
                      />
                    </div>
                  </div>
                </details>

                <div class="mt-6">
                  <PrimaryButton :loading="completing" @click="onComplete">
                    进入下一步
                  </PrimaryButton>
                </div>
              </template>
            </FormCard>
          </div>
        </template>

        <template #secondary>
          <details
            v-if="task.completionCriteria?.length || (scaffold?.whyThisTask && !legacyComplete)"
            class="rounded-card border border-border bg-white"
          >
            <summary
              class="cursor-pointer p-4 text-sm font-medium text-text-primary marker:hidden [&::-webkit-details-marker]:hidden"
            >
              （可选）如果你还想更稳一点
            </summary>
            <div class="space-y-3 border-t border-border p-4 text-sm text-text-secondary">
              <p v-if="scaffold?.whyThisTask && !legacyComplete">
                {{ scaffold.whyThisTask }}
              </p>
              <ul
                v-if="task.completionCriteria?.length"
                class="list-disc space-y-1 pl-5"
              >
                <li v-for="(c, i) in task.completionCriteria" :key="i">{{ c }}</li>
              </ul>
            </div>
          </details>
        </template>
      </WorkflowPageScaffold>
    </main>
  </PageContainer>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import PageContainer from '@/components/layout/PageContainer.vue'
import AppTopBar from '@/components/layout/AppTopBar.vue'
import FormCard from '@/components/ui/FormCard.vue'
import PrimaryButton from '@/components/ui/PrimaryButton.vue'
import SecondaryButton from '@/components/ui/SecondaryButton.vue'
import SectionHeader from '@/components/common/SectionHeader.vue'
import StatusBadge from '@/components/ui/StatusBadge.vue'
import LoadingState from '@/components/ui/LoadingState.vue'
import ErrorState from '@/components/ui/ErrorState.vue'
import EmptyState from '@/components/ui/EmptyState.vue'
import WorkflowPageScaffold from '@/components/workflow/WorkflowPageScaffold.vue'
import GuidedStepCard from '@/components/workflow/GuidedStepCard.vue'
import TaskCoachPanel from '@/components/workflow/TaskCoachPanel.vue'
import { useWorkflowStore } from '@/stores/workflow'
import {
  getCurrentTask,
  getTaskScaffold,
  postTaskMessage,
  postSelfExplanation,
  postCheckpoint,
  completeTask,
} from '@/api/task'
import { showToast } from '@/stores/toast'
import { getErrorMessage } from '@/api/request'
import { taskCompletionStatusLabels } from '@/types/labels'
import { TaskCompletionStatus } from '@/types/enums'
import {
  buildTaskGuidedSteps,
  getCurrentGuidedStepId,
  getCurrentActionInstruction,
  getTaskCognitiveHeadlineBody,
} from '@/utils/taskGuidedSteps'
import { buildCompleteTaskPayload } from '@/utils/buildCompleteTaskPayload'
import type {
  CurrentTaskItem,
  ProgressItem,
  TaskScaffoldResponse,
} from '@/types/dto'

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
const messageInput = ref('')
const selfExplainInput = ref('')
const checkpointAnswer = ref('')
const checkpointQuestion = ref('')
const legacyComplete = ref(false)
const taskStartedAt = ref(Date.now())

const closureSummary = ref('')
const closurePoint1 = ref('')
const closurePoint2 = ref('')
const closureNext = ref('')
const selfExplainMissingPoints = ref<string[]>([])

const chatTurns = ref<
  { role: 'USER' | 'ASSISTANT'; content: string; detectedAction?: string }[]
>([])

const completeForm = ref({
  completionStatus: TaskCompletionStatus.COMPLETED,
  learnerReflection: '',
})

const actionLabels: Record<string, string> = {
  ASK_FOR_EXPLANATION: '求解释',
  ASK_FOR_EXAMPLE: '求举例',
  ASK_FOR_COMPARISON: '求对比',
  ASK_FOR_SIMPLIFICATION: '求简化',
  SELF_EXPLANATION: '自己复述',
  CONFUSION_SIGNAL: '表达困惑',
  SEEK_DIRECT_ANSWER: '直接要答案',
  OFF_TOPIC: '跑题',
  GENERIC: '一般',
  ANSWER_CHECK: '检查作答',
}

const guidedSteps = computed(() =>
  buildTaskGuidedSteps(legacyComplete.value, scaffold.value)
)

const cognitiveHeadlineBody = computed(() =>
  legacyComplete.value ? '' : getTaskCognitiveHeadlineBody(scaffold.value)
)

const exploreUnitPrompts = computed(() => {
  const u = scaffold.value?.cognitiveUnits?.find((x) => x.unitId === 'explore')
  return u?.prompts ?? []
})

const requiredScaffoldPrompts = computed(() =>
  exploreUnitPrompts.value.filter((p) => p.required)
)

const optionalScaffoldPrompts = computed(() =>
  exploreUnitPrompts.value.filter((p) => !p.required)
)

const fallbackQuickPrompts = computed(() => {
  if (exploreUnitPrompts.value.length) return []
  const s = scaffold.value
  if (!s) return []
  const a = s.recommendedAskTemplates ?? []
  const b = [
    ...(s.recommendedFollowupTemplates ?? []),
    ...(s.selfCheckTemplates ?? []).slice(0, 2),
  ]
  return [...a, ...b].filter(Boolean).slice(0, 6)
})

const currentGuidedStepId = computed(() =>
  getCurrentGuidedStepId(
    taskState.value,
    exploreRoundCount.value,
    legacyComplete.value
  )
)

const currentStepLabel = computed(() => {
  const id = currentGuidedStepId.value
  return guidedSteps.value.find((s) => s.id === id)?.label ?? '推进学习'
})

const currentActionLine = computed(() =>
  getCurrentActionInstruction(
    taskState.value,
    exploreRoundCount.value,
    scaffold.value,
    task.value?.goal ?? '',
    legacyComplete.value
  )
)

const coachInputDisabled = computed(
  () =>
    sending.value ||
    taskState.value === 'CHECK' ||
    taskState.value === 'PASS' ||
    legacyComplete.value
)

function onGuidedSelect(id: string) {
  document.getElementById(`guided-${id}`)?.scrollIntoView({
    behavior: 'smooth',
    block: 'start',
  })
}

function isUiChatMessage(m: {
  role: 'USER' | 'ASSISTANT' | 'SYSTEM'
  content: string
  detectedAction?: string
}): m is { role: 'USER' | 'ASSISTANT'; content: string; detectedAction?: string } {
  return m.role === 'USER' || m.role === 'ASSISTANT'
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
    const tid = data.currentTask.taskId
    const rid = typeof route.params.taskId === 'string' ? route.params.taskId : ''
    if (route.name === 'task' || (route.name === 'taskRun' && rid && rid !== tid)) {
      router.replace({ name: 'taskRun', params: { taskId: tid } })
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
  taskStartedAt.value = Date.now()
  try {
    const s = await getTaskScaffold(taskId, store.sessionId)
    scaffold.value = s
    legacyComplete.value = false
    taskState.value =
      s.executionSnapshot?.currentState || s.currentExecutionState || 'ORIENT'
    exploreRoundCount.value = s.executionSnapshot?.exploreTurnCount || 0
    checkpointQuestion.value = s.executionSnapshot?.checkpointQuestion || ''
    chatTurns.value = (s.recentMessages || [])
      .filter(isUiChatMessage)
      .map((m) => ({
        role: m.role,
        content: m.content,
        detectedAction: m.detectedAction || undefined,
      }))
    selfExplainInput.value = ''
    checkpointAnswer.value = ''
    selfExplainMissingPoints.value = []
  } catch {
    scaffold.value = null
    legacyComplete.value = true
    taskState.value = 'PASS'
    showToast('脚手架暂不可用，已改为简单完成模式，直接填表继续即可。')
  }
}

async function sendMessage() {
  if (!store.sessionId || !task.value || !messageInput.value.trim()) return
  sending.value = true
  const content = messageInput.value.trim()
  messageInput.value = ''
  try {
    const res = await postTaskMessage(task.value.taskId, store.sessionId, content)
    chatTurns.value.push({ role: 'USER', content, detectedAction: res.detectedAction })
    chatTurns.value.push({ role: 'ASSISTANT', content: res.assistantReply })
    taskState.value = res.taskState
    if (res.taskState === 'EXPLORE') exploreRoundCount.value++
  } catch (err) {
    showToast(getErrorMessage(err))
    messageInput.value = content
  } finally {
    sending.value = false
  }
}

async function submitSelfExplanation() {
  if (!store.sessionId || !task.value) return
  submittingSelf.value = true
  try {
    const res = await postSelfExplanation(
      task.value.taskId,
      store.sessionId,
      selfExplainInput.value.trim()
    )
    taskState.value = res.taskState
    if (res.checkpointQuestion) checkpointQuestion.value = res.checkpointQuestion
    if (res.evaluation === 'WEAK') {
      showToast('可以再写具体一点（参考下方提示），不追求标准答案。')
      selfExplainMissingPoints.value = res.missingPoints ?? []
    } else {
      selfExplainMissingPoints.value = []
    }
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
    const res = await postCheckpoint(
      task.value.taskId,
      store.sessionId,
      checkpointAnswer.value.trim()
    )
    taskState.value = res.taskState
    if (res.result === 'FAIL') {
      showToast(res.reason || '还差一点点，再试一次或回去聊聊')
      checkpointAnswer.value = ''
    } else {
      showToast('很好，这一步练过了。可以到下面进入下一步。')
    }
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
      showToast('请用至少 10 个字总结本任务收获')
      return
    }
    if (!closurePoint1.value.trim() || !closurePoint2.value.trim()) {
      showToast('请填写两个收获要点')
      return
    }
  }
  completing.value = true
  try {
    const userMsgCount = chatTurns.value.filter((m) => m.role === 'USER').length
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
      userMessageCount: userMsgCount,
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
