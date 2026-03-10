<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useSessionStore } from '@/stores/session'
import { useWorkflowStore } from '@/stores/workflow'
import type { PageLevelFlow, PlannedNode } from '@/types'
import PageHeader from '@/components/PageHeader.vue'
import StepProgress from '@/components/StepProgress.vue'
import PrimaryButton from '@/components/PrimaryButton.vue'
import SessionSkeleton from '@/components/SessionSkeleton.vue'
import ErrorMessage from '@/components/ErrorMessage.vue'
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
const detailExpanded = ref(false)

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
      title: '阶段待识别',
      topic: currentTopic.value,
      objective: '等待阶段数据返回。',
      completionStandard: '等待阶段数据返回后继续。',
      statusLabel: getStatusLabel('PENDING'),
      actionText: '查看详情',
      actionHint: '当前还没有足够的阶段数据。',
      reason: '页面正在等待可用的阶段信息。',
      isActionable: false,
      isBusy: false,
      busyLabel: null as string | null,
      todo: '等待阶段数据',
      nextResult: '有了阶段信息后，系统会给出下一步建议。',
      detailDescription: '当前记录不足以判断真实阶段。',
    }
  }

  const taskMeta = currentDetailTask.value ? mapTaskToDisplayMeta(currentDetailTask.value, currentTopic.value) : null
  const currentTodo =
    taskMeta?.displayTitle ||
    (base.stepStatus === 'LOCKED' ? '等待上一阶段完成' : `${base.actionText}：${base.title}`)

  return {
    title: base.title,
    topic: currentTopic.value,
    objective: base.objective,
    completionStandard: base.completionStandard,
    statusLabel: currentDetailTask.value ? getStatusLabel(currentDetailTask.value.status) : base.statusLabel,
    actionText: base.actionText,
    actionHint: base.actionHint,
    reason: base.reason,
    isActionable: base.isActionable && !base.isBusy,
    isBusy: base.isBusy,
    busyLabel: base.busyLabel,
    todo: currentTodo,
    nextResult: base.stepStatus === 'LOCKED' ? '上一阶段完成后，这里会自动解锁。' : '完成后会进入下一阶段或刷新下一步建议。',
    detailDescription: taskMeta?.displayDescription ?? `${base.title}：围绕 ${currentTopic.value}，${base.description}`,
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
    label: total > 0 ? `${completed}/${total} · ${percent}%` : '暂无任务进度',
  }
})

const topSummaryItems = computed(() => [
  {
    label: '学习主题',
    value: currentTopic.value,
    helper: `${currentSession.value?.courseId || '-'} / ${currentSession.value?.chapterId || '-'}`,
  },
  {
    label: '当前阶段',
    value: currentStageDetail.value.title,
    helper: currentStageDetail.value.statusLabel,
  },
  {
    label: '当前进度',
    value: progressSummary.value.label,
    helper: currentSession.value?.goalText || '围绕当前主题持续推进学习任务',
  },
  {
    label: '当前待办',
    value: currentStageDetail.value.todo,
    helper: currentStageDetail.value.reason,
  },
])

const flowSteps = computed(() =>
  stageCards.value.map((step) => ({
    step: step.order,
    title: step.title,
    description: step.objective,
    statusLabel: step.statusLabel,
    actionHint: step.actionHint,
    state: step.state,
  })),
)

const detailTaskList = computed(() => {
  const source =
    plannedStageTasks.value.length > 0
      ? plannedStageTasks.value
      : timelineItems.value.map((item) => ({
          ...item,
          objective: '',
          nodeName: findNodeNameByTask(item.taskId, plannedNodes.value, item.nodeId),
          nodeStatus: item.status,
        }))

  return source.map((task) => {
    const meta = mapTaskToDisplayMeta(task, task.nodeName || currentTopic.value)
    const taskStage = stageCards.value.find((step) => step.key === normalizeLearningStage(task.stage))
    return {
      taskId: task.taskId,
      stage: task.stage,
      nodeName: task.nodeName || currentTopic.value,
      displayTitle: meta.displayTitle,
      displayDescription: meta.displayDescription,
      actionText: taskStage?.stepStatus === 'DONE' ? '查看阶段结果' : taskStage?.actionText ?? meta.actionText,
      statusLabel: meta.statusLabel,
      disabled: !!taskStage?.isBusy || taskStage?.stepStatus === 'LOCKED',
    }
  })
})

const weakPointPreview = computed(() => learningFeedback.value?.weakNodes.slice(0, 3) ?? [])

function resolveTaskPath(taskId: number) {
  return `/task/${taskId}/run`
}

function openTask(taskId: number) {
  router.push({
    path: resolveTaskPath(taskId),
    query: {
      sessionId: String(sessionId.value),
      step: String(currentStepDefinition.value.order),
    },
  })
}

function toggleDetails() {
  detailExpanded.value = !detailExpanded.value
}

function handlePrimaryAction() {
  if (!currentStageDetail.value.isActionable) {
    return
  }
  if (currentDetailTask.value) {
    openTask(currentDetailTask.value.taskId)
    return
  }
  detailExpanded.value = true
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
      // feedback is optional for the session overview
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
  await router.push('/history')
}

async function handleLogout() {
  authStore.clearAuth()
  sessionStore.reset()
  workflowStore.reset()
  await router.replace('/auth')
}

onMounted(async () => {
  await fetchSession()
})
</script>

<template>
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
      <section class="overview-card">
        <PageHeader
          :eyebrow="pageFlow === 'feedback-review' ? 'Feedback Review' : 'Learning Session'"
          title="会话中枢"
          :subtitle="`你现在处于「${currentStageDetail.title}」，当前建议动作是「${currentStageDetail.actionText}」。`"
        />

        <div class="overview-grid">
          <article v-for="item in topSummaryItems" :key="item.label" class="overview-item">
            <span class="overview-label">{{ item.label }}</span>
            <strong>{{ item.value }}</strong>
            <p>{{ item.helper }}</p>
          </article>
        </div>
      </section>

      <section class="flow-card">
        <div class="section-head">
          <h2>当前学习阶段</h2>
          <span class="section-tip">每个阶段都明确展示状态、目标和动作提示。</span>
        </div>
        <StepProgress :steps="flowSteps" :current-step="currentStepDefinition.order" />
      </section>

      <section class="current-card">
        <div class="current-card-main">
          <div class="current-head">
            <div>
              <p class="current-label">当前阶段详情</p>
              <h2>{{ currentStageDetail.title }}</h2>
            </div>
            <span class="status-badge" :class="{ busy: currentStageDetail.isBusy }">
              {{ currentStageDetail.busyLabel || currentStageDetail.statusLabel }}
            </span>
          </div>

          <div class="current-topic">
            <span class="overview-label">当前主题</span>
            <h3>{{ currentStageDetail.topic }}</h3>
          </div>

          <div class="current-goal-block">
            <article class="goal-card">
              <span class="overview-label">阶段目标</span>
              <p>{{ currentStageDetail.objective }}</p>
            </article>
            <article class="goal-card">
              <span class="overview-label">完成标准</span>
              <p>{{ currentStageDetail.completionStandard }}</p>
            </article>
            <article class="goal-card">
              <span class="overview-label">当前状态</span>
              <p>{{ currentStageDetail.statusLabel }}，{{ currentStageDetail.reason }}</p>
            </article>
            <article class="goal-card">
              <span class="overview-label">为什么下一步是这个</span>
              <p>{{ currentStageDetail.detailDescription }}</p>
            </article>
          </div>

          <div class="current-actions">
            <PrimaryButton type="button" :disabled="!currentStageDetail.isActionable" @click="handlePrimaryAction">
              {{ currentStageDetail.actionText }}
            </PrimaryButton>
            <button type="button" class="ghost-button wide" @click="toggleDetails">
              {{ detailExpanded ? '收起详细任务' : '查看详细任务' }}
            </button>
          </div>
        </div>

        <aside class="current-side">
          <article class="side-card combat-panel">
            <span class="overview-label">阶段作战面板</span>
            <div class="combat-line">
              <strong>本阶段目标</strong>
              <p>{{ currentStageDetail.objective }}</p>
            </div>
            <div class="combat-line">
              <strong>你要完成什么</strong>
              <p>{{ currentStageDetail.todo }}</p>
            </div>
            <div class="combat-line">
              <strong>完成后会发生什么</strong>
              <p>{{ currentStageDetail.nextResult }}</p>
            </div>
            <PrimaryButton type="button" :disabled="!currentStageDetail.isActionable" @click="handlePrimaryAction">
              {{ currentStageDetail.actionText }}
            </PrimaryButton>
          </article>

          <article class="side-card" v-if="learningFeedback">
            <span class="overview-label">阶段反馈</span>
            <p>{{ learningFeedback.diagnosisSummary }}</p>
          </article>

          <article class="side-card" v-if="weakPointPreview.length > 0">
            <span class="overview-label">建议继续巩固</span>
            <ul class="side-list">
              <li v-for="node in weakPointPreview" :key="node.nodeId">{{ node.nodeName }}</li>
            </ul>
          </article>
        </aside>
      </section>

      <section class="detail-card">
        <button type="button" class="detail-toggle" @click="toggleDetails">
          <span>查看详细任务</span>
          <span>{{ detailExpanded ? '收起' : '展开全部任务' }}</span>
        </button>

        <div v-if="detailExpanded" class="detail-list">
          <article v-for="task in detailTaskList" :key="task.taskId" class="detail-item">
            <div class="detail-copy">
              <div class="detail-head">
                <h3>{{ task.displayTitle }}</h3>
                <span class="status-chip">{{ task.statusLabel }}</span>
              </div>
              <p class="detail-node">{{ task.nodeName }}</p>
              <p class="detail-description">{{ task.displayDescription }}</p>
            </div>
            <PrimaryButton type="button" :disabled="task.disabled" @click="openTask(task.taskId)">
              {{ task.actionText }}
            </PrimaryButton>
          </article>
        </div>
      </section>
    </section>
  </main>
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
  gap: var(--space-md);
  margin-bottom: var(--space-xl);
  flex-wrap: wrap;
}

.toolbar-text {
  color: var(--color-text-secondary);
  font-size: var(--font-size-sm);
}

.learning-content {
  width: min(1120px, 100%);
  margin: 0 auto;
  display: grid;
  gap: clamp(18px, 2.4vw, 28px);
}

.overview-card,
.flow-card,
.current-card,
.detail-card {
  border: 1px solid var(--color-border);
  border-radius: var(--radius-xl);
  background: linear-gradient(160deg, rgba(21, 29, 43, 0.96), rgba(15, 21, 33, 0.9));
  box-shadow: var(--shadow-md);
}

.overview-card,
.flow-card,
.detail-card {
  padding: clamp(18px, 2.6vw, 28px);
}

.overview-card {
  display: grid;
  gap: var(--space-xl);
}

.overview-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: var(--space-md);
}

.overview-item {
  padding: var(--space-lg);
  border: 1px solid rgba(61, 80, 104, 0.55);
  border-radius: var(--radius-lg);
  background: rgba(13, 17, 23, 0.56);
  display: grid;
  gap: 10px;
}

.overview-item strong {
  font-size: var(--font-size-md);
  line-height: 1.5;
}

.overview-item p {
  margin: 0;
  color: var(--color-text-secondary);
  line-height: 1.6;
}

.overview-label {
  font-size: var(--font-size-xs);
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: var(--color-text-muted);
}

.section-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: var(--space-md);
  margin-bottom: var(--space-lg);
}

.section-tip {
  color: var(--color-text-secondary);
  font-size: var(--font-size-sm);
}

.current-card {
  display: grid;
  grid-template-columns: minmax(0, 1.55fr) minmax(280px, 0.9fr);
  overflow: hidden;
}

.current-card-main {
  padding: clamp(20px, 3vw, 32px);
  display: grid;
  gap: var(--space-xl);
  border-right: 1px solid rgba(61, 80, 104, 0.4);
}

.current-side {
  padding: clamp(18px, 2.6vw, 28px);
  display: grid;
  gap: var(--space-md);
  background: rgba(10, 15, 24, 0.55);
}

.current-head,
.detail-head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: var(--space-md);
}

.current-label {
  margin-bottom: 6px;
  color: var(--color-accent-coral);
  font-size: var(--font-size-sm);
}

.status-badge,
.status-chip {
  padding: 8px 14px;
  border-radius: var(--radius-full);
  border: 1px solid rgba(107, 159, 255, 0.28);
  background: rgba(107, 159, 255, 0.14);
  color: var(--color-text);
  font-size: var(--font-size-xs);
  white-space: nowrap;
}

.status-badge.busy {
  border-color: rgba(255, 184, 77, 0.35);
  background: rgba(255, 184, 77, 0.14);
}

.current-topic {
  display: grid;
  gap: 8px;
}

.current-topic h3 {
  font-size: clamp(1.8rem, 3vw, 2.4rem);
}

.current-goal-block {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: var(--space-md);
}

.goal-card,
.side-card {
  border: 1px solid rgba(61, 80, 104, 0.48);
  border-radius: var(--radius-lg);
  background: rgba(13, 17, 23, 0.58);
  padding: var(--space-lg);
  display: grid;
  gap: 10px;
}

.goal-card p,
.side-card p,
.detail-description,
.detail-node {
  margin: 0;
  line-height: 1.65;
  color: var(--color-text-secondary);
}

.current-actions {
  display: flex;
  gap: var(--space-md);
  flex-wrap: wrap;
}

.wide {
  min-width: 170px;
}

.combat-panel {
  gap: var(--space-lg);
}

.combat-line {
  display: grid;
  gap: 6px;
}

.combat-line strong {
  color: var(--color-text);
  font-size: var(--font-size-sm);
}

.side-list {
  margin: 0;
  padding-left: 18px;
  color: var(--color-text-secondary);
}

.detail-toggle {
  width: 100%;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: var(--space-md);
  padding: 0;
  color: var(--color-text);
  font-size: var(--font-size-md);
}

.detail-list {
  margin-top: var(--space-xl);
  display: grid;
  gap: var(--space-md);
}

.detail-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: var(--space-lg);
  padding: var(--space-lg);
  border: 1px solid rgba(61, 80, 104, 0.5);
  border-radius: var(--radius-lg);
  background: rgba(13, 17, 23, 0.56);
}

.detail-copy {
  display: grid;
  gap: 8px;
}

.detail-node {
  font-size: var(--font-size-sm);
}

.ghost-button {
  min-height: 48px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  color: var(--color-text-secondary);
  background: var(--color-bg-surface);
  padding: 0 16px;
  font-size: var(--font-size-md);
  transition: all var(--duration-fast) var(--ease-smooth);
}

.ghost-button:hover {
  background: var(--color-bg-hover);
  border-color: var(--color-border-hover);
  color: var(--color-text);
}

@media (max-width: 980px) {
  .overview-grid,
  .current-goal-block,
  .current-card {
    grid-template-columns: 1fr;
  }

  .current-card-main {
    border-right: none;
    border-bottom: 1px solid rgba(61, 80, 104, 0.4);
  }

  .detail-item {
    flex-direction: column;
    align-items: stretch;
  }
}

@media (max-width: 720px) {
  .section-head,
  .current-head,
  .detail-head,
  .detail-toggle {
    flex-direction: column;
    align-items: flex-start;
  }

  .current-actions {
    flex-direction: column;
  }
}
</style>
