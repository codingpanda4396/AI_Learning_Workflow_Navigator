<script setup lang="ts">
import { computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import FloatingTutor from '@/components/tutor/FloatingTutor.vue'
import TutorLauncher from '@/components/tutor/TutorLauncher.vue'
import { useAuthStore } from '@/stores/auth'
import { useSessionStore } from '@/stores/session'
import { useWorkflowStore } from '@/stores/workflow'
import type { PageLevelFlow, PlannedNode } from '@/types'
import SessionSkeleton from '@/components/SessionSkeleton.vue'
import ErrorMessage from '@/components/ErrorMessage.vue'
import LearningStepBar from '@/components/LearningStepBar.vue'
import CurrentActionCard from '@/components/CurrentActionCard.vue'
import SessionSummaryHero from '@/components/SessionSummaryHero.vue'
import { useTutorPanel } from '@/composables/useTutorPanel'
import { buildTutorContext } from '@/utils/buildTutorContext'
import {
  buildSessionStageViewModels,
  findNodeNameByTask,
  getCurrentLearningStep,
  getLearningStageDisplay,
  getStatusLabel,
  mapTaskToDisplayMeta,
  normalizeLearningStage,
} from '@/utils/learningPlanDisplay'

const SKIP_RESUME_ONCE_KEY = 'ai_learning_skip_resume_once'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const sessionStore = useSessionStore()
const workflowStore = useWorkflowStore()

const sessionId = computed(() => Number(route.params.id))

function dedupeByTaskIdentity<T extends { taskId: number; nodeId?: number; stage?: string }>(items: T[]) {
  const seen = new Set<string>()
  return items.filter((item) => {
    const fallbackKey = `${item.nodeId ?? 'unknown'}:${item.stage ?? 'unknown'}`
    const key = Number.isFinite(item.taskId) && item.taskId > 0 ? `task:${item.taskId}` : `node-stage:${fallbackKey}`
    if (seen.has(key)) {
      return false
    }
    seen.add(key)
    return true
  })
}

const currentSession = computed(() => sessionStore.currentSession)
const currentPath = computed(() => sessionStore.currentSessionPath)
const learningFeedback = computed(() => sessionStore.learningFeedback)
const pageFlow = computed<PageLevelFlow>(() => (learningFeedback.value ? 'feedback-review' : 'learning-session'))
const isLoading = computed(() => sessionStore.overviewAsyncStatus === 'RUNNING' || sessionStore.pathAsyncStatus === 'RUNNING')
const error = computed(() => sessionStore.error)
const username = computed(() => authStore.currentUser?.username ?? '')

const timelineItems = computed(() => dedupeByTaskIdentity(currentSession.value?.timeline ?? []))

const plannedNodes = computed<PlannedNode[]>(() => {
  if (sessionStore.plannedNodes.length > 0) {
    return sessionStore.plannedNodes
  }

  const grouped = new Map<number, PlannedNode>()
  for (const item of timelineItems.value) {
    const existing = grouped.get(item.nodeId) ?? {
      nodeId: item.nodeId,
      nodeName: currentPath.value?.nodes.find((node) => node.nodeId === item.nodeId)?.nodeName ?? `主题 ${item.nodeId}`,
      status: item.status,
      stages: [],
    }
    existing.stages.push({
      taskId: item.taskId,
      stage: item.stage,
      objective: '',
      status: item.status,
    })
    grouped.set(item.nodeId, existing)
  }

  return [...grouped.values()]
})

const plannedStageTasks = computed(() =>
  plannedNodes.value.flatMap((plan) =>
    plan.stages.map((stage) => ({
      ...stage,
      nodeId: plan.nodeId,
      nodeName: plan.nodeName,
      nodeStatus: plan.status,
    })),
  ),
)

const currentStepKey = computed(() =>
  getCurrentLearningStep(timelineItems.value, sessionStore.nextTask, sessionStore.normalizedCurrentStage),
)

const stageCards = computed(() =>
  buildSessionStageViewModels(
    timelineItems.value,
    sessionStore.nextTask,
    sessionStore.normalizedCurrentStage,
    sessionStore.pageAsyncStatus,
  ),
)

const currentStageCard = computed(() => {
  const active = stageCards.value.find((item) => item.stepStatus === 'ACTIVE' || item.stepStatus === 'AVAILABLE' || item.stepStatus === 'ERROR')
  if (active) {
    return active
  }
  return stageCards.value.find((item) => item.key === currentStepKey.value) ?? stageCards.value[0] ?? null
})

const currentStepDefinition = computed(() => getLearningStageDisplay(currentStageCard.value?.key ?? currentStepKey.value))

const currentDetailTask = computed(() => {
  const preferredTaskId = sessionStore.nextTask?.taskId
  if (preferredTaskId) {
    const plannedMatch = plannedStageTasks.value.find((task) => task.taskId === preferredTaskId)
    if (plannedMatch) {
      return plannedMatch
    }
  }

  const runningTask = plannedStageTasks.value.find((task) => task.status === 'RUNNING')
  if (runningTask) {
    return runningTask
  }

  if (currentStepKey.value !== 'UNKNOWN') {
    const sameStepTask = plannedStageTasks.value.find(
      (task) => normalizeLearningStage(task.stage) === currentStepKey.value && task.status !== 'SUCCEEDED',
    )
    if (sameStepTask) {
      return sameStepTask
    }
  }

  return plannedStageTasks.value.find((task) => task.status !== 'SUCCEEDED') ?? plannedStageTasks.value[0] ?? null
})

const currentTopic = computed(() => {
  if (currentDetailTask.value?.nodeName) {
    return currentDetailTask.value.nodeName
  }
  if (sessionStore.nextTask) {
    return findNodeNameByTask(sessionStore.nextTask.taskId, plannedNodes.value, sessionStore.nextTask.nodeId)
  }
  return plannedNodes.value[0]?.nodeName ?? currentPath.value?.nodes[0]?.nodeName ?? '当前知识点'
})

const currentStageDetail = computed(() => {
  const base = currentStageCard.value ?? stageCards.value[0]
  if (!base) {
    return {
      title: '准备中',
      topic: currentTopic.value,
      objective: '等待阶段数据返回后继续。',
      statusLabel: getStatusLabel('PENDING'),
      actionText: '查看详情',
      reason: '系统正在同步当前学习阶段。',
      isActionable: false,
      isBusy: false,
      busyLabel: null as string | null,
      todo: '等待系统准备完成',
      nextResult: '准备好后会自动给出下一步。',
    }
  }

  const taskMeta = currentDetailTask.value ? mapTaskToDisplayMeta(currentDetailTask.value, currentTopic.value) : null
  const currentTodo =
    taskMeta?.displayTitle ||
    (base.stepStatus === 'LOCKED' ? '等待上一阶段完成' : `${base.title}：先做这一阶段的主任务`)

  return {
    title: base.title,
    topic: currentTopic.value,
    objective: base.objective,
    statusLabel: currentDetailTask.value ? getStatusLabel(currentDetailTask.value.status) : base.statusLabel,
    actionText: base.actionText,
    reason: base.reason,
    isActionable: base.isActionable && !base.isBusy,
    isBusy: base.isBusy,
    busyLabel: base.busyLabel,
    todo: currentTodo,
    nextResult: base.stepStatus === 'LOCKED' ? '上一阶段完成后，这里会自动解锁。' : '完成后会自动推进到下一步。',
  }
})

const progressSummary = computed(() => {
  const total = currentSession.value?.progress?.totalTaskCount ?? timelineItems.value.length
  const completed =
    currentSession.value?.progress?.completedTaskCount ??
    timelineItems.value.filter((item) => item.status === 'SUCCEEDED').length
  const percent = total > 0 ? Math.round((completed / total) * 100) : 0
  return {
    total,
    completed,
    percent,
    label: total > 0 ? `${completed}/${total} · ${percent}%` : '还没有任务进度',
  }
})

const estimatedDuration = computed(() => {
  const total = currentSession.value?.progress?.totalTaskCount ?? Math.max(timelineItems.value.length, 1)
  const minutes = Math.max(20, total * 8)
  return `约 ${minutes} 分钟`
})

const heroMetaItems = computed(() => [
  {
    label: '主题',
    value: currentTopic.value,
  },
  {
    label: '课程 / 章节',
    value: `${currentSession.value?.courseId || '-'} / ${currentSession.value?.chapterId || '-'}`,
  },
  {
    label: '当前步骤',
    value: currentStageDetail.value.title,
  },
  {
    label: '预计时长',
    value: estimatedDuration.value,
  },
])

const stepItems = computed(() =>
  stageCards.value.map((step) => ({
    key: step.key,
    order: step.order,
    title: step.title,
    description: step.objective,
    statusLabel: step.statusLabel,
    state: step.state,
  })),
)

const actionDescription = computed(() => `${currentStageDetail.value.objective} 当前先做：${currentStageDetail.value.todo}`)

const actionHelper = computed(() => {
  const feedbackHint =
    learningFeedback.value?.weakNodes.length
      ? `需要重点关注：${learningFeedback.value.weakNodes.slice(0, 2).map((item) => item.nodeName).join('、')}`
      : ''
  return [currentStageDetail.value.nextResult, feedbackHint].filter(Boolean).join(' ')
})
const tutorTaskId = computed(() => currentDetailTask.value?.taskId ?? sessionStore.nextTask?.taskId ?? null)
const tutorContext = computed(() =>
  buildTutorContext({
    sessionId: sessionId.value,
    stage: currentDetailTask.value?.stage ?? sessionStore.currentSession?.currentStage,
    stepLabel: currentStepDefinition.value.title,
    taskId: tutorTaskId.value,
    topic: currentTopic.value,
    course: currentSession.value?.courseId,
    chapter: currentSession.value?.chapterId,
    goal: currentSession.value?.goalText,
    taskTitle: currentStageDetail.value.todo,
    taskGoal: currentStageDetail.value.objective,
    taskSummary: actionDescription.value,
    session: currentSession.value,
  }),
)
const tutorPanel = useTutorPanel({
  sessionId,
  taskId: tutorTaskId,
  context: tutorContext,
})

function isValidPositiveId(value: number | null | undefined) {
  return Number.isFinite(value) && (value ?? 0) > 0
}

function openTask(taskId: number) {
  if (!isValidPositiveId(taskId) || !isValidPositiveId(sessionId.value)) {
    return
  }
  router.push({
    name: 'task-run',
    params: { id: String(taskId) },
    query: {
      sessionId: String(sessionId.value),
      step: String(currentStepDefinition.value.order),
    },
  })
}

function handlePrimaryAction() {
  if (!currentStageDetail.value.isActionable) {
    return
  }
  if (currentDetailTask.value) {
    openTask(currentDetailTask.value.taskId)
  }
}

async function fetchSession() {
  await Promise.all([
    sessionStore.fetchSessionOverview(sessionId.value),
    sessionStore.fetchSessionPath(sessionId.value),
  ])

  if ((currentSession.value?.timeline.length ?? 0) === 0) {
    await sessionStore.planSession(sessionId.value)
    await sessionStore.fetchSessionOverview(sessionId.value)
  }

  if (!learningFeedback.value && (currentSession.value?.progress?.totalTaskCount ?? 0) > 0) {
    try {
      await sessionStore.fetchLearningFeedback(sessionId.value)
    } catch {
      // optional
    }
  }
}

async function handleRetry() {
  await fetchSession()
}

async function goHome() {
  localStorage.setItem(SKIP_RESUME_ONCE_KEY, '1')
  await router.replace({ name: 'home', query: { skipResume: '1' } })
}

async function goHistory() {
  await router.push({ name: 'history' })
}

async function handleLogout() {
  authStore.clearAuth()
  sessionStore.reset()
  workflowStore.reset()
  await router.replace({ name: 'auth' })
}

onMounted(async () => {
  await fetchSession()
})
</script>

<template>
  <div class="session-shell">
  <main class="learning-page">
    <header class="toolbar">
      <button type="button" class="ghost-button" @click="goHome">返回首页</button>
      <button type="button" class="ghost-button" @click="goHistory">历史记录</button>
      <span class="toolbar-text">Session #{{ sessionId }}</span>
      <span class="toolbar-text">{{ username }}</span>
      <button type="button" class="ghost-button" @click="handleLogout">退出登录</button>
    </header>

    <SessionSkeleton v-if="isLoading && !currentSession" />
    <ErrorMessage v-else-if="error && !currentSession" :message="error" @retry="handleRetry" />

    <section v-else class="learning-content">
      <SessionSummaryHero
        :eyebrow="pageFlow === 'feedback-review' ? 'Review' : 'This Round'"
        title="本轮学习"
        subtitle="先看清目标，再跟着下一步继续。"
        :goal="currentSession?.goalText || '围绕当前主题完成这一轮学习。'"
        :meta-items="heroMetaItems"
      />

      <section class="surface-card">
        <div class="section-head">
          <h2>学习步骤</h2>
          <p>一轮学习只分四步，当前该做什么会自动高亮。</p>
        </div>
        <LearningStepBar :steps="stepItems" />
      </section>

      <CurrentActionCard
        eyebrow="当前行动"
        :title="currentStageDetail.title"
        :description="actionDescription"
        :helper="actionHelper"
        :button-text="currentStageDetail.isBusy ? '处理中...' : currentStageDetail.actionText"
        :status-label="currentStageDetail.busyLabel || currentStageDetail.statusLabel"
        :disabled="!currentStageDetail.isActionable"
        @action="handlePrimaryAction"
      />

      <p class="footer-note">当前进度：{{ progressSummary.label }}</p>
    </section>
  </main>

  <TutorLauncher :open="tutorPanel.isOpen.value" subtle label="需要提示？" @toggle="tutorPanel.togglePanel" />
  <FloatingTutor
    :open="tutorPanel.isOpen.value"
    :available="tutorPanel.canUseTutor.value"
    :context-title="tutorContext.contextTitle"
    :context-meta="`${currentSession?.courseId || '-'} / ${currentSession?.chapterId || '-'}`"
    :messages="tutorPanel.messages.value"
    :loading="tutorPanel.loading.value"
    :load-error="tutorPanel.loadError.value"
    :send-error="tutorPanel.sendError.value"
    :sending="tutorPanel.sending.value"
    :input="tutorPanel.input.value"
    :quick-prompts="tutorPanel.quickPrompts"
    @close="tutorPanel.closePanel"
    @retry-load="tutorPanel.ensureMessagesLoaded(true)"
    @retry-send="tutorPanel.retrySend"
    @submit="tutorPanel.sendMessage"
    @update-input="tutorPanel.setInput"
    @use-quick-prompt="tutorPanel.useQuickPrompt"
  />
  </div>
</template>

<style scoped>
.learning-page {
  min-height: 100dvh;
  padding: clamp(16px, 2.8vw, 32px);
}

.toolbar {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  gap: 12px;
  margin-bottom: 24px;
  flex-wrap: wrap;
}

.toolbar-text {
  color: var(--color-text-secondary);
  font-size: var(--font-size-sm);
}

.learning-content {
  width: min(960px, 100%);
  margin: 0 auto;
  display: grid;
  gap: 18px;
}

.surface-card {
  padding: clamp(22px, 3vw, 30px);
  border: 1px solid rgba(61, 80, 104, 0.48);
  border-radius: var(--radius-xl);
  background: rgba(15, 21, 33, 0.94);
  display: grid;
  gap: 18px;
}

.section-head {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  gap: 16px;
  flex-wrap: wrap;
}

.section-head h2,
.section-head p,
.footer-note {
  margin: 0;
}

.section-head p,
.footer-note {
  color: var(--color-text-secondary);
}

.ghost-button {
  min-height: 46px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  color: var(--color-text-secondary);
  background: rgba(10, 15, 24, 0.92);
  padding: 0 16px;
  font-size: var(--font-size-sm);
}

.ghost-button:hover {
  color: var(--color-text);
  border-color: var(--color-border-hover);
}
</style>
