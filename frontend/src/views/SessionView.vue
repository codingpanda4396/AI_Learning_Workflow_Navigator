<script setup lang="ts">
import { computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import FloatingTutor from '@/components/tutor/FloatingTutor.vue'
import TutorLauncher from '@/components/tutor/TutorLauncher.vue'
import { useAuthStore } from '@/stores/auth'
import { useSessionStore } from '@/stores/session'
import { useWorkflowStore } from '@/stores/workflow'
import type { PlannedNode } from '@/types'
import SessionSkeleton from '@/components/SessionSkeleton.vue'
import ErrorMessage from '@/components/ErrorMessage.vue'
import LearningStepBar from '@/components/LearningStepBar.vue'
import CurrentActionCard from '@/components/CurrentActionCard.vue'
import SessionSummaryHero from '@/components/SessionSummaryHero.vue'
import NextStepPanel from '@/components/NextStepPanel.vue'
import { useTutorPanel } from '@/composables/useTutorPanel'
import { buildTutorContext } from '@/utils/buildTutorContext'
import {
  buildSessionStageViewModels,
  findNodeNameByTask,
  getCurrentLearningStep,
  normalizeLearningStage,
} from '@/utils/learningPlanDisplay'
import { getPrimaryStageAction, getStageNarrative, getStageShortLabel } from '@/utils/learningNarrative'

const SKIP_RESUME_ONCE_KEY = 'ai_learning_skip_resume_once'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const sessionStore = useSessionStore()
const workflowStore = useWorkflowStore()

const sessionId = computed(() => Number(route.params.id))
const currentSession = computed(() => sessionStore.currentSession)
const currentPath = computed(() => sessionStore.currentSessionPath)
const learningFeedback = computed(() => sessionStore.learningFeedback)
const isLoading = computed(() => sessionStore.overviewAsyncStatus === 'RUNNING' || sessionStore.pathAsyncStatus === 'RUNNING')
const error = computed(() => sessionStore.error)
const username = computed(() => authStore.currentUser?.username ?? '')

function dedupeByTaskIdentity<T extends { taskId: number; nodeId?: number; stage?: string }>(items: T[]) {
  const seen = new Set<string>()
  return items.filter((item) => {
    const fallbackKey = `${item.nodeId ?? 'unknown'}:${item.stage ?? 'unknown'}`
    const key = Number.isFinite(item.taskId) && item.taskId > 0 ? `task:${item.taskId}` : `node-stage:${fallbackKey}`
    if (seen.has(key)) return false
    seen.add(key)
    return true
  })
}

const timelineItems = computed(() => dedupeByTaskIdentity(currentSession.value?.timeline ?? []))

const plannedNodes = computed<PlannedNode[]>(() => {
  if (sessionStore.plannedNodes.length > 0) return sessionStore.plannedNodes

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
  return (
    stageCards.value.find((item) => item.stepStatus === 'ACTIVE' || item.stepStatus === 'AVAILABLE' || item.stepStatus === 'ERROR') ??
    stageCards.value.find((item) => item.key === currentStepKey.value) ??
    stageCards.value[0] ??
    null
  )
})

const currentDetailTask = computed(() => {
  const preferredTaskId = sessionStore.nextTask?.taskId
  if (preferredTaskId) {
    const plannedMatch = plannedStageTasks.value.find((task) => task.taskId === preferredTaskId)
    if (plannedMatch) return plannedMatch
  }

  return (
    plannedStageTasks.value.find((task) => task.status === 'RUNNING') ??
    plannedStageTasks.value.find(
      (task) => normalizeLearningStage(task.stage) === currentStepKey.value && task.status !== 'SUCCEEDED',
    ) ??
    plannedStageTasks.value.find((task) => task.status !== 'SUCCEEDED') ??
    plannedStageTasks.value[0] ??
    null
  )
})

const currentTopic = computed(() => {
  if (currentDetailTask.value?.nodeName) return currentDetailTask.value.nodeName
  if (sessionStore.nextTask) {
    return findNodeNameByTask(sessionStore.nextTask.taskId, plannedNodes.value, sessionStore.nextTask.nodeId)
  }
  return plannedNodes.value[0]?.nodeName ?? currentPath.value?.nodes[0]?.nodeName ?? '当前知识点'
})

const progressSummary = computed(() => {
  const total = currentSession.value?.progress?.totalTaskCount ?? timelineItems.value.length
  const completed =
    currentSession.value?.progress?.completedTaskCount ??
    timelineItems.value.filter((item) => item.status === 'SUCCEEDED').length
  return {
    total,
    completed,
    label: total > 0 ? `当前进度 ${Math.min(completed + 1, total)}/${total}` : '当前进度 1/1',
  }
})

const stageNarrative = computed(() => getStageNarrative(currentStageCard.value?.key ?? currentStepKey.value))
const heroMetaItems = computed(() => [
  { label: '学习目标', value: currentSession.value?.goalText || '完成这一轮学习' },
  { label: '课程 / 章节', value: `${currentSession.value?.courseId || '-'} / ${currentSession.value?.chapterId || '-'}` },
  { label: '当前步骤', value: getStageShortLabel(currentStageCard.value?.key ?? currentStepKey.value) },
  { label: '剩余流程', value: progressSummary.value.label },
])

const stepItems = computed(() =>
  stageCards.value.map((step) => ({
    key: step.key,
    order: step.order,
    title: step.title,
    description: step.order === currentStageCard.value?.order ? stageNarrative.value.summary : step.completionStandard,
    statusLabel: step.state === 'completed' ? '已完成' : step.state === 'current' ? '当前进行中' : '尚未开始',
    state: step.state,
  })),
)

const actionTitle = computed(() => `当前任务：${stageNarrative.value.title}`)
const actionDescription = computed(() => `${currentTopic.value}。${stageNarrative.value.summary}`)
const actionHelper = computed(() => {
  const weakHint = learningFeedback.value?.weakNodes?.[0]?.nodeName ? `重点留意：${learningFeedback.value.weakNodes[0].nodeName}` : ''
  return [stageNarrative.value.reason, weakHint].filter(Boolean).join(' ')
})

const nextStepTitle = computed(() => {
  const currentOrder = currentStageCard.value?.order ?? 1
  const next = stageCards.value.find((item) => item.order === currentOrder + 1)
  return next ? `完成后会进入「${next.title}」` : '完成后会回到结果与下一步建议'
})

const nextStepDescription = computed(() => {
  if (currentDetailTask.value?.status === 'FAILED') return '这一步出错后可重新进入，不会丢失当前学习目标。'
  return '系统会根据当前任务结果自动推进到下一步，你不需要自己判断阶段。'
})

const tutorTaskId = computed(() => currentDetailTask.value?.taskId ?? sessionStore.nextTask?.taskId ?? null)
const tutorContext = computed(() =>
  buildTutorContext({
    sessionId: sessionId.value,
    stage: currentDetailTask.value?.stage ?? sessionStore.currentSession?.currentStage,
    taskId: tutorTaskId.value,
    topic: currentTopic.value,
    course: currentSession.value?.courseId,
    chapter: currentSession.value?.chapterId,
    goal: currentSession.value?.goalText,
    taskTitle: actionTitle.value,
    taskGoal: actionDescription.value,
    taskSummary: actionHelper.value,
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
  if (!isValidPositiveId(taskId) || !isValidPositiveId(sessionId.value)) return
  router.push({
    name: 'task-run',
    params: { id: String(taskId) },
    query: {
      sessionId: String(sessionId.value),
      step: String(currentStageCard.value?.order ?? 1),
    },
  })
}

function handlePrimaryAction() {
  if (currentDetailTask.value) {
    openTask(currentDetailTask.value.taskId)
  }
}

async function fetchSession() {
  await Promise.all([sessionStore.fetchSessionOverview(sessionId.value), sessionStore.fetchSessionPath(sessionId.value)])
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
        <span class="toolbar-text">{{ username }}</span>
        <button type="button" class="ghost-button" @click="handleLogout">退出登录</button>
      </header>

      <SessionSkeleton v-if="isLoading && !currentSession" />
      <ErrorMessage v-else-if="error && !currentSession" :message="error" @retry="handleRetry" />

      <section v-else class="learning-content">
        <SessionSummaryHero
          eyebrow="本轮学习"
          title="你现在正在这一轮学习里"
          subtitle="先知道现在在哪，再开始当前这一步。"
          :goal="currentSession?.goalText || '围绕当前主题完成这一轮学习。'"
          :meta-items="heroMetaItems"
        />

        <CurrentActionCard
          eyebrow="当前行动"
          :title="actionTitle"
          :description="actionDescription"
          :helper="actionHelper"
          :button-text="getPrimaryStageAction(currentStageCard?.key)"
          :status-label="currentStageCard?.state === 'completed' ? '已完成' : '现在要做' "
          :disabled="!currentDetailTask"
          @action="handlePrimaryAction"
        />

        <section class="surface-card">
          <div class="section-head">
            <h2>这一轮会怎么推进</h2>
            <p>先理解概念，再做检测，最后看结果并决定下一步。</p>
          </div>
          <LearningStepBar :steps="stepItems" />
        </section>

        <NextStepPanel :title="nextStepTitle" :description="nextStepDescription" />
      </section>
    </main>

    <TutorLauncher :open="tutorPanel.isOpen.value" subtle label="问 Tutor" @toggle="tutorPanel.togglePanel" />
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

.toolbar-text,
.section-head p {
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
  gap: 16px;
  align-items: baseline;
  flex-wrap: wrap;
}

.section-head h2,
.section-head p {
  margin: 0;
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
</style>
