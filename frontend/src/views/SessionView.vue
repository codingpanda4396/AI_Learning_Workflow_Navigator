<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useSessionStore } from '@/stores/session'
import { useWorkflowStore } from '@/stores/workflow'
import type { WorkflowStepNumber } from '@/types/workflow'
import PageHeader from '@/components/PageHeader.vue'
import StepProgress from '@/components/StepProgress.vue'
import PrimaryButton from '@/components/PrimaryButton.vue'
import SessionSkeleton from '@/components/SessionSkeleton.vue'
import ErrorMessage from '@/components/ErrorMessage.vue'

const SKIP_RESUME_ONCE_KEY = 'ai_learning_skip_resume_once'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const sessionStore = useSessionStore()
const workflowStore = useWorkflowStore()

type SessionStepNumber = 1 | 2 | 3

type StepMeta = {
  step: SessionStepNumber
  title: string
  task: string
}

type StepProgressItem = {
  step: WorkflowStepNumber
  title: string
  doneCount: number
  totalCount: number
  percent: number
  status: 'pending' | 'running' | 'done' | 'blocked'
}

const sessionId = computed(() => Number(route.params.id))
const browsingStep = ref<SessionStepNumber>(1)
const expandedTaskIds = ref<number[]>([])

function resolveStep(raw: unknown): SessionStepNumber {
  const value = Number(raw)
  if (Number.isFinite(value) && value >= 1 && value <= 3) {
    return value as SessionStepNumber
  }
  return 1
}

const steps: StepMeta[] = [
  { step: 1, title: '学习计划', task: '确认当前学习目标与计划任务' },
  { step: 2, title: '分步学习', task: '执行当前任务并推进进度' },
  { step: 3, title: '阶段反馈', task: '查看阶段进度与下一步建议' },
]

function stageLabel(stage: string) {
  const map: Record<string, string> = {
    STRUCTURE: '结构构建',
    UNDERSTANDING: '理解深化',
    TRAINING: '训练实战',
    EVALUATION: '阶段评估',
    REFLECTION: '复盘总结',
  }
  return map[stage] || stage
}

function stageGuide(stage: string) {
  const map: Record<string, string> = {
    STRUCTURE: '梳理关键概念、边界与关系，形成知识框架。',
    UNDERSTANDING: '解释机制和因果链，识别并纠正常见误区。',
    TRAINING: '完成训练题并提交答案，根据反馈修正。',
    EVALUATION: '对当前阶段完成情况进行验证并整理改进点。',
    REFLECTION: '复盘错误模式，提炼下一步改进动作。',
  }
  return map[stage] || '按任务提示完成学习。'
}

const currentSession = computed(() => sessionStore.currentSession)
const learningFeedback = computed(() => sessionStore.learningFeedback)
const isLoading = computed(() => sessionStore.fetchingSession || sessionStore.recoveringSession)
const error = computed(() => sessionStore.error)
const currentStep = computed(() => browsingStep.value)
const currentStepMeta = computed(() => steps.find((item) => item.step === currentStep.value) ?? steps[0]!)
const username = computed(() => authStore.currentUser?.username ?? '')

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

const timelineItems = computed(() => dedupeByTaskIdentity(currentSession.value?.timeline ?? []))

const plannedNodes = computed(() => {
  if (sessionStore.plannedNodes.length > 0) {
    return sessionStore.plannedNodes
  }
  const grouped = new Map<number, { nodeId: number; nodeName: string; status: string; stages: Array<{ taskId: number; stage: string; objective: string; status: string }> }>()
  for (const item of timelineItems.value) {
    const existing = grouped.get(item.nodeId) ?? {
      nodeId: item.nodeId,
      nodeName: `节点 ${item.nodeId}`,
      status: item.status,
      stages: [],
    }
    existing.stages.push({
      taskId: item.taskId,
      stage: item.stage,
      objective: stageGuide(item.stage),
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

const nextPlannedTask = computed(() => {
  if (sessionStore.nextTask) {
    return plannedStageTasks.value.find((item) => item.taskId === sessionStore.nextTask?.taskId) ?? null
  }
  return plannedStageTasks.value.find((item) => item.status !== 'SUCCEEDED') ?? plannedStageTasks.value[0] ?? null
})

const totalTaskCount = computed(() => currentSession.value?.progress?.totalTaskCount ?? timelineItems.value.length)
const completedTaskCount = computed(() => currentSession.value?.progress?.completedTaskCount ?? 0)
const completionPercent = computed(() => {
  const total = totalTaskCount.value
  if (total <= 0) {
    return 0
  }
  return Math.round((completedTaskCount.value / total) * 100)
})

const stepProgressData = computed<StepProgressItem[]>(() => {
  const timeline = timelineItems.value
  const failedCount = timeline.filter((item) => item.status === 'FAILED').length
  const step1Done = plannedNodes.value.length > 0
  const step2HasProgress = completedTaskCount.value > 0 || sessionStore.nextTask !== null
  const step3Ready = totalTaskCount.value > 0
  const step3Done = !!learningFeedback.value || (currentSession.value?.progress?.completionRate ?? 0) >= 1

  return [
    {
      step: 1,
      title: steps[0]!.title,
      doneCount: step1Done ? 1 : 0,
      totalCount: 1,
      percent: step1Done ? 100 : 0,
      status: step1Done ? 'done' : 'pending',
    },
    {
      step: 2,
      title: steps[1]!.title,
      doneCount: completedTaskCount.value,
      totalCount: totalTaskCount.value,
      percent: completionPercent.value,
      status:
        totalTaskCount.value === 0
          ? (step1Done ? 'running' : 'pending')
          : completedTaskCount.value >= totalTaskCount.value
            ? 'done'
            : failedCount > 0
              ? 'blocked'
              : step2HasProgress
                ? 'running'
                : 'pending',
    },
    {
      step: 3,
      title: steps[2]!.title,
      doneCount: step3Done ? 1 : 0,
      totalCount: 1,
      percent: step3Done ? 100 : (step3Ready ? 60 : 0),
      status: step3Done ? 'done' : (step3Ready ? 'running' : 'pending'),
    },
  ]
})

function resolveTaskPath(taskId: number, stage: string) {
  return stage === 'TRAINING' ? `/task/${taskId}/submit` : `/task/${taskId}/run`
}

function openTask(taskId: number, stage: string) {
  router.push({
    path: resolveTaskPath(taskId, stage),
    query: {
      sessionId: String(sessionId.value),
      step: String(currentStep.value),
    },
  })
}

function toggleTask(taskId: number) {
  if (expandedTaskIds.value.includes(taskId)) {
    expandedTaskIds.value = expandedTaskIds.value.filter((id) => id !== taskId)
    return
  }
  expandedTaskIds.value = [...expandedTaskIds.value, taskId]
}

function isTaskExpanded(taskId: number) {
  return expandedTaskIds.value.includes(taskId)
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
      // feedback is non-blocking in MVP session view
    }
  }
}

function handleRunNextTask() {
  if (!sessionStore.nextTask) return
  openTask(sessionStore.nextTask.taskId, sessionStore.nextTask.stage)
}

async function handleRetry() {
  await fetchSession()
}

function goToStep(step: SessionStepNumber) {
  browsingStep.value = step
}

function handlePrevious() {
  if (currentStep.value > 1) goToStep((currentStep.value - 1) as SessionStepNumber)
}

function handleNext() {
  if (currentStep.value < 3) goToStep((currentStep.value + 1) as SessionStepNumber)
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
  browsingStep.value = resolveStep(route.query.step)
  await fetchSession()
})
</script>

<template>
  <main class="workflow-page">
    <header class="toolbar">
      <button class="ghost-button" @click="goHome">返回首页</button>
      <button class="ghost-button" @click="goHistory">历史记录</button>
      <span class="workflow-id">Session #{{ sessionId }}</span>
      <span class="workflow-id">{{ username }}</span>
      <button type="button" class="ghost-button" @click="handleLogout">退出登录</button>
    </header>

    <SessionSkeleton v-if="isLoading && !currentSession" />
    <ErrorMessage v-else-if="error && !currentSession" :message="error" @retry="handleRetry" />

    <section v-else class="workflow-content">
      <PageHeader eyebrow="Learning Flow" title="MVP 三步学习流程" :subtitle="`当前关注：${currentStepMeta.task}`" />
      <StepProgress :steps="stepProgressData" :current-step="currentStep" />

      <article class="step-card">
        <div class="step-head">
          <h2>{{ currentStepMeta.step }}. {{ currentStepMeta.title }}</h2>
          <span class="status-tag">可切换查看全部步骤</span>
        </div>

        <section v-if="currentStep === 1" class="panel">
          <h3>学习计划</h3>
          <p v-if="currentSession"><strong>课程/章节：</strong>{{ currentSession.courseId }} / {{ currentSession.chapterId }}</p>
          <p v-if="currentSession"><strong>学习目标：</strong>{{ currentSession.goalText }}</p>
          <p v-if="nextPlannedTask">
            <strong>推荐起始任务：</strong>任务 #{{ nextPlannedTask.taskId }}（{{ stageLabel(nextPlannedTask.stage) }}）
          </p>
          <p v-if="plannedNodes.length === 0">当前暂无计划任务。</p>
          <div v-else class="task-list">
            <article v-for="plan in plannedNodes" :key="`plan-node-${plan.nodeId}`" class="task-item">
              <div class="task-body">
                <p><strong>{{ plan.nodeName }}</strong> · 节点 #{{ plan.nodeId }}</p>
                <p><strong>节点状态：</strong>{{ plan.status }}</p>
                <div class="task-list">
                  <article v-for="stage in plan.stages" :key="stage.taskId" class="task-item">
                    <div class="task-body">
                      <p><strong>任务 #{{ stage.taskId }}</strong> · {{ stageLabel(stage.stage) }}</p>
                      <p>{{ stage.objective || stageGuide(stage.stage) }}</p>
                      <p><strong>状态：</strong>{{ stage.status }}</p>
                    </div>
                  </article>
                </div>
              </div>
            </article>
          </div>
        </section>

        <section v-if="currentStep === 2" class="panel">
          <h3>分步学习</h3>
          <p><strong>任务进度：</strong>{{ completedTaskCount }}/{{ totalTaskCount }}（{{ completionPercent }}%）</p>
          <p v-if="nextPlannedTask">
            <strong>当前建议任务：</strong>任务 #{{ nextPlannedTask.taskId }}（{{ stageLabel(nextPlannedTask.stage) }}）
          </p>
          <p v-if="nextPlannedTask"><strong>任务说明：</strong>{{ nextPlannedTask.objective || stageGuide(nextPlannedTask.stage) }}</p>
          <p v-if="sessionStore.currentTask">
            <strong>最近执行任务：</strong>
            #{{ sessionStore.currentTask.taskId }}（{{ stageLabel(sessionStore.currentTask.stage) }}）/{{ sessionStore.currentTask.status }}
          </p>
          <p v-if="sessionStore.taskResult">
            <strong>最近提交结果：</strong>
            分数 {{ sessionStore.taskResult.score }}，下一动作 {{ sessionStore.taskResult.nextAction }}
          </p>
          <p v-if="!currentSession || timelineItems.length === 0">暂无学习步骤。</p>
          <div v-else class="task-list">
            <article v-for="item in timelineItems" :key="item.taskId" class="task-item">
              <button class="task-toggle" @click="toggleTask(item.taskId)">
                <span>任务 #{{ item.taskId }} - {{ stageLabel(item.stage) }}</span>
                <span>{{ isTaskExpanded(item.taskId) ? '收起' : '展开' }}</span>
              </button>
              <div v-if="isTaskExpanded(item.taskId)" class="task-body">
                <p><strong>状态：</strong>{{ item.status }}</p>
                <p><strong>该做什么：</strong>{{ stageGuide(item.stage) }}</p>
                <PrimaryButton type="button" @click="openTask(item.taskId, item.stage)">
                  打开该步骤任务
                </PrimaryButton>
              </div>
            </article>
          </div>
          <PrimaryButton v-if="sessionStore.nextTask" type="button" @click="handleRunNextTask">继续当前推荐任务</PrimaryButton>
        </section>

        <section v-if="currentStep === 3" class="panel">
          <h3>阶段反馈</h3>
          <p v-if="!currentSession">暂无总结数据。</p>
          <template v-else>
            <p><strong>当前已完成以下任务：</strong>{{ completedTaskCount }}/{{ totalTaskCount }}</p>
            <p><strong>当前学习进度：</strong>{{ completionPercent }}%</p>
            <p><strong>当前阶段：</strong>{{ stageLabel(currentSession.currentStage) }}</p>
            <div class="mastery-list">
              <p v-for="item in currentSession.masterySummary" :key="item.nodeId">
                当前已覆盖这些内容：{{ item.nodeName }}（{{ Math.round(item.masteryValue * 100) }}%）
              </p>
            </div>
            <template v-if="learningFeedback">
              <p><strong>阶段总结：</strong>{{ learningFeedback.diagnosisSummary }}</p>
              <p v-if="learningFeedback.weakNodes.length > 0"><strong>建议下一步继续完成：</strong></p>
              <ul v-if="learningFeedback.weakNodes.length > 0">
                <li v-for="node in learningFeedback.weakNodes" :key="node.nodeId">{{ node.nodeName }}（建议继续训练）</li>
              </ul>
            </template>
            <p v-if="sessionStore.nextTask">
              <strong>下一建议：</strong>
              进入任务 #{{ sessionStore.nextTask.taskId }}（{{ stageLabel(sessionStore.nextTask.stage) }}）
            </p>
            <p v-else>如需进一步验证，可进入训练/测验环节。</p>
          </template>
        </section>

        <nav class="actions">
          <button class="ghost-button" :disabled="currentStep === 1" @click="handlePrevious">上一步</button>
          <PrimaryButton v-if="currentStep < 3" type="button" @click="handleNext">下一步</PrimaryButton>
          <PrimaryButton v-else type="button" @click="goHome">完成并返回首页</PrimaryButton>
        </nav>
      </article>
    </section>
  </main>
</template>

<style scoped>
.workflow-page {
  min-height: 100dvh;
  padding: clamp(16px, 2.8vw, 30px);
}

.toolbar {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  gap: var(--space-md);
  margin-bottom: var(--space-lg);
  flex-wrap: wrap;
}

.workflow-id {
  color: var(--color-text-secondary);
  font-size: var(--font-size-sm);
}

.workflow-content {
  display: flex;
  flex-direction: column;
  gap: var(--space-xl);
  max-width: 960px;
  margin: 0 auto;
}

.step-card {
  border: 1px solid var(--color-border);
  border-radius: var(--radius-xl);
  background: linear-gradient(
    165deg,
    var(--color-bg-elevated),
    var(--color-bg-surface)
  );
  padding: clamp(16px, 2.8vw, 26px);
  box-shadow: var(--shadow-md);
  display: flex;
  flex-direction: column;
  gap: var(--space-lg);
  transition: all var(--duration-normal) var(--ease-smooth);
}

.step-card:hover {
  box-shadow: var(--shadow-lg);
}

.step-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: var(--space-sm);
}

.status-tag {
  border: 1px solid var(--color-border);
  border-radius: var(--radius-full);
  padding: 6px 14px;
  color: var(--color-text-secondary);
  font-size: var(--font-size-xs);
  background: var(--color-bg);
}

.panel {
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  background: var(--color-bg);
  padding: var(--space-lg);
}

.path-grid {
  display: grid;
  gap: var(--space-md);
}

.path-item {
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  padding: var(--space-lg);
  cursor: pointer;
  transition: all var(--duration-normal) var(--ease-smooth);
  background: var(--color-bg-surface);
}

.path-item:hover {
  border-color: var(--color-border-hover);
  transform: translateY(-2px);
}

.path-item.selected {
  border-color: var(--color-primary);
  box-shadow: 0 0 0 3px var(--color-primary-alpha);
}

.task-list {
  display: grid;
  gap: var(--space-md);
  margin-bottom: var(--space-lg);
}

.task-item {
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  overflow: hidden;
  background: var(--color-bg-surface);
}

.task-toggle {
  width: 100%;
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: var(--color-bg-elevated);
  border: none;
  color: var(--color-text);
  padding: var(--space-md);
  font-size: var(--font-size-md);
  cursor: pointer;
  transition: all var(--duration-fast) var(--ease-smooth);
}

.task-toggle:hover {
  background: var(--color-bg-hover);
}

.task-body {
  padding: var(--space-lg);
  display: grid;
  gap: var(--space-md);
  background: var(--color-bg);
}

.mastery-list {
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  padding: var(--space-md);
  margin: var(--space-md) 0;
  background: var(--color-bg-surface);
}

.actions {
  display: grid;
  grid-template-columns: 140px 1fr;
  gap: var(--space-lg);
  padding-top: var(--space-lg);
  border-top: 1px solid var(--color-border);
}

.ghost-button {
  min-height: 48px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  color: var(--color-text-secondary);
  background: var(--color-bg-surface);
  font-size: var(--font-size-md);
  transition: all var(--duration-fast) var(--ease-smooth);
}

.ghost-button:hover:not(:disabled) {
  background: var(--color-bg-hover);
  border-color: var(--color-border-hover);
  color: var(--color-text);
}

.ghost-button:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

@media (max-width: 900px) {
  .actions {
    grid-template-columns: 1fr;
  }
}
</style>

