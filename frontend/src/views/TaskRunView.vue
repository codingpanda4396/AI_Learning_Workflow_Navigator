<template>
  <PageContainer>
    <AppTopBar current="task" />
    <main class="mx-auto max-w-3xl px-6 py-8">
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

      <div v-else-if="task" class="space-y-6">
        <div class="flex items-center justify-between">
          <h1 class="text-2xl font-bold text-text-primary">
            {{ task.title }}
          </h1>
          <StatusBadge
            v-if="progress"
            :label="`${progress.currentIndex} / ${progress.totalTasks}`"
          />
        </div>

        <!-- 执行阶段条 -->
        <div
          v-if="scaffold"
          class="flex flex-wrap gap-2 rounded-card border border-border bg-gray-50 p-3 text-sm"
        >
          <span class="font-medium text-text-secondary">当前阶段：</span>
          <span
            v-for="step in executionSteps"
            :key="step.key"
            class="rounded-full px-2 py-0.5"
            :class="
              step.key === taskStateHighlight
                ? 'bg-primary text-white'
                : 'bg-white text-text-secondary ring-1 ring-border'
            "
          >
            {{ step.label }}
          </span>
        </div>

        <FormCard>
          <SectionHeader>任务目标</SectionHeader>
          <p class="text-text-primary">{{ task.goal }}</p>
        </FormCard>

        <!-- 脚手架区 -->
        <FormCard v-if="scaffold">
          <SectionHeader>学习脚手架</SectionHeader>
          <p v-if="scaffold.whyThisTask" class="mb-3 text-sm text-text-secondary">
            {{ scaffold.whyThisTask }}
          </p>
          <p class="mb-2 text-sm font-medium text-text-primary">推荐起手问法</p>
          <div class="mb-4 flex flex-wrap gap-2">
            <button
              v-for="(t, i) in scaffold.recommendedAskTemplates"
              :key="'a' + i"
              type="button"
              class="rounded-input border border-primary/40 bg-primary/5 px-3 py-1.5 text-left text-sm text-primary hover:bg-primary/10"
              @click="messageInput = t"
            >
              {{ t }}
            </button>
          </div>
          <p class="mb-2 text-sm font-medium text-text-primary">追问与自检</p>
          <div class="flex flex-wrap gap-2">
            <button
              v-for="(t, i) in [
                ...(scaffold.recommendedFollowupTemplates || []),
                ...(scaffold.selfCheckTemplates || []).slice(0, 2),
              ]"
              :key="'f' + i"
              type="button"
              class="rounded-input border border-border px-3 py-1 text-sm text-text-secondary hover:border-primary/50"
              @click="messageInput = t"
            >
              {{ t.length > 36 ? t.slice(0, 36) + '…' : t }}
            </button>
          </div>
          <div
            v-if="scaffold.fallbackHints?.length"
            class="mt-3 rounded bg-amber-50 p-2 text-xs text-amber-900"
          >
            <span class="font-medium">卡住时：</span>
            {{ scaffold.fallbackHints[0] }}
          </div>
        </FormCard>

        <!-- 导师对话区 -->
        <FormCard v-if="scaffold">
          <SectionHeader>探索对话</SectionHeader>
          <p class="mb-3 text-xs text-text-secondary">
            至少完成 2 轮提问后再提交自我解释。系统会识别你的学习动作（如举例、对比）。
          </p>
          <div
            class="mb-4 max-h-72 space-y-3 overflow-y-auto rounded border border-border bg-gray-50/80 p-3"
          >
            <div
              v-for="(m, idx) in chatTurns"
              :key="idx"
              class="text-sm"
              :class="
                m.role === 'USER' ? 'text-right text-text-primary' : 'text-left text-text-secondary'
              "
            >
              <span
                class="inline-block max-w-[90%] rounded-lg px-3 py-2"
                :class="
                  m.role === 'USER' ? 'bg-primary/15' : 'bg-white shadow-sm ring-1 ring-border'
                "
              >
                {{ m.content }}
              </span>
              <div
                v-if="m.detectedAction && m.role === 'USER'"
                class="mt-0.5 text-xs text-gray-500"
              >
                识别：{{ actionLabel(m.detectedAction) }}
              </div>
            </div>
          </div>
          <div class="flex gap-2">
            <textarea
              v-model="messageInput"
              rows="2"
              class="min-h-[3rem] flex-1 rounded-input border border-border px-3 py-2 text-text-primary focus:border-primary focus:outline-none focus:ring-1 focus:ring-primary"
              placeholder="输入你的问题…"
              :disabled="sending || taskState === 'CHECK' || taskState === 'PASS'"
            />
            <PrimaryButton
              :loading="sending"
              :disabled="!messageInput.trim() || taskState === 'CHECK' || taskState === 'PASS'"
              class="self-end"
              @click="sendMessage"
            >
              发送
            </PrimaryButton>
          </div>
          <div
            v-if="exploreRoundCount >= 2 && taskState === 'EXPLORE'"
            class="mt-4 border-t border-border pt-4"
          >
            <SectionHeader>自我解释</SectionHeader>
            <p class="mb-2 text-xs text-text-secondary">
              用一段话复述你对本任务的理解（不少于约 35 字）。
            </p>
            <textarea
              v-model="selfExplainInput"
              rows="3"
              class="mb-2 w-full rounded-input border border-border px-3 py-2 text-text-primary"
              placeholder="我这样理解：……"
            />
            <PrimaryButton
              :loading="submittingSelf"
              :disabled="!selfExplainInput.trim()"
              @click="submitSelfExplanation"
            >
              提交自我解释
            </PrimaryButton>
          </div>
          <div
            v-if="taskState === 'CHECK'"
            class="mt-4 border-t border-border pt-4"
          >
            <SectionHeader>微检查</SectionHeader>
            <p class="mb-2 font-medium text-text-primary">{{ checkpointQuestion }}</p>
            <textarea
              v-model="checkpointAnswer"
              rows="2"
              class="mb-2 w-full rounded-input border border-border px-3 py-2"
              placeholder="简要作答"
            />
            <PrimaryButton
              :loading="submittingCheckpoint"
              :disabled="!checkpointAnswer.trim()"
              @click="submitCheckpoint"
            >
              提交答案
            </PrimaryButton>
          </div>
          <div
            v-if="taskState === 'REMEDIAL'"
            class="mt-3 rounded bg-amber-50 p-3 text-sm text-amber-900"
          >
            需要再探索一轮：请继续向导师提问，满足轮次后可再次提交自我解释。
          </div>
        </FormCard>

        <FormCard v-if="task.completionCriteria?.length">
          <SectionHeader>完成标准</SectionHeader>
          <ul class="list-disc space-y-1 pl-5 text-text-secondary">
            <li v-for="(c, i) in task.completionCriteria" :key="i">{{ c }}</li>
          </ul>
        </FormCard>

        <FormCard>
          <SectionHeader>标记任务完成</SectionHeader>
          <p
            v-if="taskState !== 'PASS' && !legacyComplete"
            class="mb-3 text-sm text-amber-800"
          >
            须先完成上方脚手架流程（探索 → 自我解释 → 微检查通过）后才能提交。
          </p>
          <p
            v-else-if="taskState === 'PASS' && !legacyComplete"
            class="mb-3 text-sm text-green-700"
          >
            微检查已通过，可提交本轮任务完成。
          </p>
          <p
            v-else-if="legacyComplete"
            class="mb-3 text-sm text-text-secondary"
          >
            当前为直接完成模式（未启用脚手架流程时的行为）。
          </p>
          <form class="space-y-4" @submit.prevent="onComplete">
            <div>
              <label class="mb-1.5 block text-sm font-medium text-text-primary">
                完成状态 <span class="text-red-500">*</span>
              </label>
              <select
                v-model="completeForm.completionStatus"
                class="w-full rounded-input border border-border px-4 py-2.5 text-text-primary focus:border-primary focus:outline-none focus:ring-1 focus:ring-primary"
                required
                :disabled="taskState !== 'PASS' && !legacyComplete"
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
              <label class="mb-1.5 block text-sm font-medium text-text-primary">
                耗时（分钟）
              </label>
              <input
                v-model.number="completeForm.durationMinutes"
                type="number"
                min="0"
                class="w-full rounded-input border border-border px-4 py-2.5 text-text-primary focus:border-primary focus:outline-none focus:ring-1 focus:ring-primary"
                :disabled="taskState !== 'PASS' && !legacyComplete"
              />
            </div>
            <div>
              <label class="mb-1.5 block text-sm font-medium text-text-primary">
                交互次数
              </label>
              <input
                v-model.number="completeForm.interactionCount"
                type="number"
                min="0"
                class="w-full rounded-input border border-border px-4 py-2.5 text-text-primary focus:border-primary focus:outline-none focus:ring-1 focus:ring-primary"
                :disabled="taskState !== 'PASS' && !legacyComplete"
              />
            </div>
            <div>
              <label class="mb-1.5 block text-sm font-medium text-text-primary">
                学习反思
              </label>
              <textarea
                v-model="completeForm.learnerReflection"
                rows="3"
                class="w-full rounded-input border border-border px-4 py-3 text-text-primary placeholder:text-gray-400 focus:border-primary focus:outline-none focus:ring-1 focus:ring-primary"
                placeholder="简要记录你的理解与收获"
                :disabled="taskState !== 'PASS' && !legacyComplete"
              />
            </div>
            <div class="flex gap-3">
              <PrimaryButton
                :loading="completing"
                type="submit"
                :disabled="taskState !== 'PASS' && !legacyComplete"
              >
                提交完成
              </PrimaryButton>
            </div>
          </form>
        </FormCard>
      </div>
    </main>
  </PageContainer>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
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
import type {
  CurrentTaskItem,
  ProgressItem,
  TaskScaffoldResponse,
} from '@/types/dto'

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
/** 脚手架加载失败时允许沿用旧版「直接完成」 */
const legacyComplete = ref(false)

const chatTurns = ref<
  { role: 'USER' | 'ASSISTANT'; content: string; detectedAction?: string }[]
>([])

const executionSteps = [
  { key: 'ORIENT', label: '定向' },
  { key: 'EXPLORE', label: '探索' },
  { key: 'SELF_EXPLAIN', label: '自解释' },
  { key: 'CHECK', label: '微检查' },
  { key: 'PASS', label: '通过' },
]

const taskStateHighlight = computed(() => {
  const s = taskState.value
  if (s === 'REMEDIAL') return 'EXPLORE'
  if (s === 'ASK') return 'EXPLORE'
  if (s === 'INIT') return 'ORIENT'
  return s
})

const completeForm = ref({
  completionStatus: TaskCompletionStatus.COMPLETED,
  durationMinutes: undefined as number | undefined,
  interactionCount: undefined as number | undefined,
  learnerReflection: '',
})

const actionLabels: Record<string, string> = {
  ASK_FOR_EXPLANATION: '求解释',
  ASK_FOR_EXAMPLE: '求举例',
  ASK_FOR_COMPARISON: '求对比',
  ASK_FOR_SIMPLIFICATION: '求简化',
  SELF_EXPLANATION: '自我解释',
  CONFUSION_SIGNAL: '表达困惑',
  SEEK_DIRECT_ANSWER: '直接要答案',
  OFF_TOPIC: '跑题',
  GENERIC: '一般',
  ANSWER_CHECK: '检查作答',
}

function actionLabel(code: string) {
  return actionLabels[code] ?? code
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
      router.push('/report')
      return
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
  try {
    const s = await getTaskScaffold(taskId, store.sessionId)
    scaffold.value = s
    legacyComplete.value = false
    taskState.value = s.currentExecutionState || 'ORIENT'
    exploreRoundCount.value = 0
    chatTurns.value = []
    selfExplainInput.value = ''
    checkpointAnswer.value = ''
    checkpointQuestion.value = ''
  } catch {
    scaffold.value = null
    legacyComplete.value = true
    taskState.value = 'PASS'
    showToast('脚手架暂不可用，已切换为直接提交完成（与未加载脚手架时行为一致）')
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
      showToast('自我解释偏短，请按提示补充后再试')
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
      showToast(res.reason || '未通过，请再探索')
      checkpointAnswer.value = ''
    } else {
      showToast('已通过微检查，可提交任务完成')
    }
  } catch (err) {
    showToast(getErrorMessage(err))
  } finally {
    submittingCheckpoint.value = false
  }
}

async function onComplete() {
  if (!store.sessionId || !task.value) return
  completing.value = true
  try {
    const payload = {
      sessionId: store.sessionId,
      completionStatus: completeForm.value.completionStatus,
      durationMinutes: completeForm.value.durationMinutes,
      interactionCount: completeForm.value.interactionCount,
      learnerReflection: completeForm.value.learnerReflection || undefined,
    }
    const data = await completeTask(task.value.taskId, payload)
    store.currentTask = null
    completeForm.value = {
      completionStatus: TaskCompletionStatus.COMPLETED,
      durationMinutes: undefined,
      interactionCount: undefined,
      learnerReflection: '',
    }
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
