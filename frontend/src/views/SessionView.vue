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
  buildLearningStepStates,
  findNodeNameByTask,
  getCurrentLearningStep,
  getLearningStageDisplay,
  getPrimaryActionText,
  getStatusLabel,
  LEARNING_STEPS,
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
const currentStepDefinition = computed(() => getLearningStageDisplay(currentStepKey.value))
const recommendedStep = computed(() => LEARNING_STEPS.find((step) => step.key === currentStepKey.value) ?? LEARNING_STEPS[0]!)

const flowSteps = computed(() =>
  buildLearningStepStates(timelineItems.value, sessionStore.nextTask, sessionStore.normalizedCurrentStage).map((step) => ({
    step: step.order,
    title: step.title,
    description: step.objective,
    state: step.state,
  })),
)

const progressSummary = computed(() => {
  const total = currentSession.value?.progress?.totalTaskCount ?? timelineItems.value.length
  const completed =
    currentSession.value?.progress?.completedTaskCount ??
    timelineItems.value.filter((item) => item.status === 'SUCCEEDED').length
  const percent = total > 0 ? Math.round((completed / total) * 100) : 0
  return { total, completed, percent }
})

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
    const pendingSameStep = plannedStageTasks.value.find(
      (task) => normalizeLearningStage(task.stage) === currentStepKey.value && task.status !== 'SUCCEEDED',
    )
    if (pendingSameStep) {
      return pendingSameStep
    }

    const sameStepTask = plannedStageTasks.value.find((task) => normalizeLearningStage(task.stage) === currentStepKey.value)
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

const currentCardMeta = computed(() => {
  if (currentDetailTask.value) {
    return mapTaskToDisplayMeta(currentDetailTask.value, currentTopic.value)
  }

  return {
    phaseKey: currentStepDefinition.value.key,
    phaseLabel: currentStepDefinition.value.title,
    displayTitle: currentStepDefinition.value.title,
    displayDescription: `${currentStepDefinition.value.title}：围绕 ${currentTopic.value}，${currentStepDefinition.value.description}`,
    actionText: currentStepDefinition.value.actionText,
    statusLabel: '未开始',
  }
})

const currentTaskStatusLabel = computed(() =>
  currentDetailTask.value ? getStatusLabel(currentDetailTask.value.status) : getStatusLabel('PENDING'),
)
const primaryActionLabel = computed(() =>
  currentDetailTask.value
    ? getPrimaryActionText(currentDetailTask.value.stage, currentDetailTask.value.status)
    : currentCardMeta.value.actionText,
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
    return {
      taskId: task.taskId,
      stage: task.stage,
      status: task.status,
      nodeName: task.nodeName || currentTopic.value,
      displayTitle: meta.displayTitle,
      displayDescription: meta.displayDescription,
      actionText: getPrimaryActionText(task.stage, task.status),
      statusLabel: meta.statusLabel,
    }
  })
})

const weakPointPreview = computed(() => learningFeedback.value?.weakNodes.slice(0, 3) ?? [])

function resolveTaskPath(taskId: number, stage: string) {
  return normalizeLearningStage(stage) === 'TRAINING' ? `/task/${taskId}/submit` : `/task/${taskId}/run`
}

function openTask(taskId: number, stage: string) {
  router.push({
    path: resolveTaskPath(taskId, stage),
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
  if (currentDetailTask.value) {
    openTask(currentDetailTask.value.taskId, currentDetailTask.value.stage)
    return
  }
  detailExpanded.value = true
}

async function fetchSession() {
  await sessionStore.fetchSessionOverview(sessionId.value)
  await sessionStore.fetchSessionPath(sessionId.value)

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
          title="学习计划"
          :subtitle="`你正在学习 ${currentTopic}，当前建议先完成「${recommendedStep.title}」`"
        />

        <div class="overview-grid">
          <article class="overview-item">
            <span class="overview-label">课程 / 章节</span>
            <strong>{{ currentSession?.courseId || '-' }} / {{ currentSession?.chapterId || '-' }}</strong>
          </article>
          <article class="overview-item">
            <span class="overview-label">学习目标</span>
            <strong>{{ currentSession?.goalText || '先建立对当前知识点的完整理解' }}</strong>
          </article>
          <article class="overview-item">
            <span class="overview-label">推荐起始阶段</span>
            <strong>{{ recommendedStep.title }}</strong>
          </article>
          <article class="overview-item">
            <span class="overview-label">当前进度</span>
            <strong>{{ progressSummary.completed }}/{{ progressSummary.total }} · {{ progressSummary.percent }}%</strong>
          </article>
        </div>
      </section>

      <section class="flow-card">
        <div class="section-head">
          <h2>学习阶段流程</h2>
          <span class="section-tip">固定四阶段，和页面流程分开显示，避免语义混淆</span>
        </div>
        <StepProgress :steps="flowSteps" :current-step="currentStepDefinition.order" />
      </section>

      <section class="current-card">
        <div class="current-card-main">
          <div class="current-head">
            <div>
              <p class="current-label">当前学习阶段</p>
              <h2>{{ currentCardMeta.phaseLabel }}</h2>
            </div>
            <span class="status-badge">{{ currentTaskStatusLabel }}</span>
          </div>

          <div class="current-topic">
            <span class="overview-label">当前学习主题</span>
            <h3>{{ currentTopic }}</h3>
          </div>

          <div class="current-goal-block">
            <article class="goal-card">
              <span class="overview-label">阶段目标</span>
              <p>{{ currentStepDefinition.objective }}</p>
            </article>
            <article class="goal-card">
              <span class="overview-label">学习说明</span>
              <p>{{ currentCardMeta.displayDescription }}</p>
            </article>
          </div>

          <div class="current-actions">
            <PrimaryButton type="button" @click="handlePrimaryAction">
              {{ primaryActionLabel }}
            </PrimaryButton>
            <button type="button" class="ghost-button wide" @click="toggleDetails">
              {{ detailExpanded ? '收起详细任务' : '查看详细任务' }}
            </button>
          </div>
        </div>

        <aside class="current-side">
          <article class="side-card">
            <span class="overview-label">本阶段关注点</span>
            <p>{{ currentStepDefinition.description }}</p>
          </article>

          <article class="side-card" v-if="learningFeedback">
            <span class="overview-label">复盘摘要</span>
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
            <PrimaryButton type="button" @click="openTask(task.taskId, task.stage)">
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
