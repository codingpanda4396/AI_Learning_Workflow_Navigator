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
import { getPrimaryStageAction, getStageFocusLabel, getStageNarrative, getStageShortLabel } from '@/utils/learningNarrative'

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
    label: total > 0 ? `已完成 ${completed}/${total}` : '刚开始这一轮学习',
  }
})

const stageNarrative = computed(() => getStageNarrative(currentStageCard.value?.key ?? currentStepKey.value))
const heroMetaItems = computed(() => [
  { label: '学习目标', value: currentSession.value?.goalText || '完成这一轮学习' },
  { label: '当前主题', value: currentTopic.value },
  { label: '当前阶段', value: getStageShortLabel(currentStageCard.value?.key ?? currentStepKey.value) },
  { label: '整体进度', value: progressSummary.value.label },
])

const stepItems = computed(() =>
  stageCards.value.map((step) => ({
    key: step.key,
    order: step.order,
    title: step.title,
    description: step.order === currentStageCard.value?.order ? stageNarrative.value.summary : step.completionStandard,
    statusLabel: step.state === 'completed' ? '已完成' : step.state === 'current' ? '当前阶段' : '待进入',
    state: step.state,
  })),
)

const actionTitle = computed(() => `现在先做：${stageNarrative.value.title}`)
const actionDescription = computed(() => `${currentTopic.value}。${stageNarrative.value.summary}`)
const actionHelper = computed(() => {
  const weakHint = learningFeedback.value?.weakNodes?.[0]?.nodeName ? `当前优先留意：${learningFeedback.value.weakNodes[0].nodeName}` : ''
  return [stageNarrative.value.reason, weakHint].filter(Boolean).join(' ')
})

const weakPointItems = computed(() => learningFeedback.value?.weakNodes?.slice(0, 3) ?? [])

const recentTrainingSummary = computed(() => {
  if (learningFeedback.value?.diagnosisSummary) return learningFeedback.value.diagnosisSummary
  if (weakPointItems.value.length > 0) return '最近训练已经产出薄弱点分析，建议优先围绕这些节点继续学习。'
  if ((currentSession.value?.progress?.completedTaskCount ?? 0) > 0) return '你已经完成了部分任务，可以继续当前阶段并等待新的训练反馈。'
  return '当前还没有训练结果，先完成这一阶段的学习内容。'
})

const recommendationTitle = computed(() => {
  if (currentDetailTask.value?.status === 'FAILED') return '先重新进入当前任务'
  if (sessionStore.nextTask) return `建议继续推进到「${getStageShortLabel(sessionStore.nextTask.stage)}」`
  return `建议先完成「${getStageShortLabel(currentStageCard.value?.key ?? currentStepKey.value)}」`
})

const recommendationDescription = computed(() => {
  if (currentDetailTask.value?.status === 'FAILED') return '这一步出错后可以重新进入，不会丢失当前学习目标。'
  return '系统会根据当前任务结果自动推进到下一步，你不需要自己判断该切到哪个阶段。'
})

const recommendationReason = computed(() => {
  const weakReason = weakPointItems.value[0]?.reasons?.[0]
  return weakReason || stageNarrative.value.reason
})

const sequenceItems = computed(() =>
  plannedNodes.value.map((plan) => ({
    nodeId: plan.nodeId,
    nodeName: plan.nodeName,
    isCurrent: plan.nodeId === currentDetailTask.value?.nodeId,
    currentStage:
      plan.stages.find((stage) => stage.taskId === currentDetailTask.value?.taskId)?.stage ??
      plan.stages.find((stage) => stage.status === 'RUNNING')?.stage ??
      plan.stages[0]?.stage ??
      'UNKNOWN',
    stageCount: plan.stages.length,
  })),
)

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
      // Optional today. TODO: replace stitched summary/weak-point panels with a backend aggregated session guidance payload.
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
        <button type="button" class="ghost-button" @click="goHistory">成长记录</button>
        <span class="toolbar-text">{{ username }}</span>
        <button type="button" class="ghost-button" @click="handleLogout">退出登录</button>
      </header>

      <SessionSkeleton v-if="isLoading && !currentSession" />
      <ErrorMessage v-else-if="error && !currentSession" :message="error" @retry="handleRetry" />

      <section v-else class="learning-content">
        <SessionSummaryHero
          eyebrow="本轮学习"
          title="你现在在哪，以及接下来做什么"
          subtitle="这一页负责帮你看清当前主题、所在阶段和系统建议的下一步。"
          :goal="currentSession?.goalText || '围绕当前主题完成这一轮学习。'"
          :meta-items="heroMetaItems"
        />

        <div class="content-grid">
          <section class="main-column">
            <CurrentActionCard
              eyebrow="当前行动"
              :title="actionTitle"
              :description="actionDescription"
              :helper="actionHelper"
              :button-text="getPrimaryStageAction(currentStageCard?.key)"
              :status-label="currentStageCard?.state === 'completed' ? '已完成' : '现在要做'"
              :disabled="!currentDetailTask"
              @action="handlePrimaryAction"
            />

            <section class="surface-card">
              <div class="section-head">
                <div>
                  <p class="section-label">当前阶段</p>
                  <h2>{{ getStageShortLabel(currentStageCard?.key ?? currentStepKey) }}</h2>
                </div>
                <p>{{ getStageFocusLabel(currentStageCard?.key ?? currentStepKey) }}</p>
              </div>
              <LearningStepBar :steps="stepItems" />
            </section>

            <section class="surface-card">
              <div class="section-head">
                <div>
                  <p class="section-label">任务序列</p>
                  <h2>这轮学习会沿着哪些知识点推进</h2>
                </div>
                <p>当前任务所在节点会被高亮，方便你知道自己正学到哪里。</p>
              </div>
              <div class="sequence-list">
                <article
                  v-for="item in sequenceItems"
                  :key="item.nodeId"
                  class="sequence-item"
                  :class="{ current: item.isCurrent }"
                >
                  <div>
                    <strong>{{ item.nodeName }}</strong>
                    <p>{{ item.isCurrent ? '当前正在推进' : '后续会进入' }}</p>
                  </div>
                  <div class="sequence-meta">
                    <span>{{ getStageShortLabel(item.currentStage) }}</span>
                    <span>{{ item.stageCount }} 个阶段任务</span>
                  </div>
                </article>
              </div>
            </section>
          </section>

          <aside class="side-column">
            <section class="summary-card">
              <span class="summary-label">最近训练结果</span>
              <h3>系统刚刚看到了什么</h3>
              <p>{{ recentTrainingSummary }}</p>
            </section>

            <section class="summary-card">
              <span class="summary-label">当前薄弱点</span>
              <h3>优先补哪里</h3>
              <ul v-if="weakPointItems.length" class="bullet-list">
                <li v-for="item in weakPointItems" :key="item.nodeId">
                  <strong>{{ item.nodeName }}</strong>
                  <span>{{ item.reasons[0] || '这部分仍需要继续巩固。' }}</span>
                </li>
              </ul>
              <p v-else>当前还没有明确薄弱点，先完成这一阶段学习，系统会补充新的训练判断。</p>
            </section>

            <section class="summary-card">
              <span class="summary-label">系统建议</span>
              <NextStepPanel :title="recommendationTitle" :description="recommendationDescription" />
              <p class="reason-copy">为什么这样建议：{{ recommendationReason }}</p>
            </section>
          </aside>
        </div>
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
.section-head p,
.sequence-item p,
.reason-copy {
  color: var(--color-text-secondary);
  font-size: var(--font-size-sm);
}

.learning-content {
  width: min(1180px, 100%);
  margin: 0 auto;
  display: grid;
  gap: 18px;
}

.content-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.4fr) minmax(280px, 0.86fr);
  gap: 18px;
  align-items: start;
}

.main-column,
.side-column {
  display: grid;
  gap: 18px;
}

.surface-card,
.summary-card {
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
.section-head p,
.summary-card h3,
.summary-card p,
.reason-copy {
  margin: 0;
}

.section-label,
.summary-label {
  color: var(--color-text-muted);
  font-size: var(--font-size-xs);
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.sequence-list,
.bullet-list {
  display: grid;
  gap: 12px;
}

.sequence-item {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  padding: 16px 18px;
  border-radius: var(--radius-lg);
  border: 1px solid rgba(61, 80, 104, 0.38);
  background: rgba(10, 16, 26, 0.76);
}

.sequence-item strong,
.summary-card h3 {
  color: var(--color-text);
}

.sequence-item.current {
  border-color: rgba(107, 159, 255, 0.52);
  box-shadow: inset 0 0 0 1px rgba(107, 159, 255, 0.18);
}

.sequence-meta {
  display: grid;
  justify-items: end;
  gap: 6px;
  color: var(--color-text-secondary);
  font-size: var(--font-size-sm);
}

.bullet-list {
  margin: 0;
  padding-left: 20px;
}

.bullet-list li {
  color: var(--color-text-secondary);
  line-height: 1.7;
}

.bullet-list span {
  display: block;
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

@media (max-width: 980px) {
  .content-grid {
    grid-template-columns: 1fr;
  }
}
</style>
