<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useSessionStore } from '@/stores/session'
import type { WorkflowStepNumber } from '@/types/workflow'
import PageHeader from '@/components/PageHeader.vue'
import StepProgress from '@/components/StepProgress.vue'
import PrimaryButton from '@/components/PrimaryButton.vue'
import SessionSkeleton from '@/components/SessionSkeleton.vue'
import ErrorMessage from '@/components/ErrorMessage.vue'

const SKIP_RESUME_ONCE_KEY = 'ai_learning_skip_resume_once'

type StepMeta = {
  step: WorkflowStepNumber
  title: string
  task: string
}

const route = useRoute()
const router = useRouter()
const sessionStore = useSessionStore()

const sessionId = computed(() => Number(route.params.id))
const notes = ref('')
const browsingStep = ref<WorkflowStepNumber>(1)
const hasInitializedStep = ref(false)

const steps: StepMeta[] = [
  { step: 1, title: '目标诊断', task: '明确目标边界和能力基线' },
  { step: 2, title: '路径规划', task: '拆解学习节点和阶段路径' },
  { step: 3, title: '分步学习', task: '执行任务并记录掌握度变化' },
  { step: 4, title: '总结反馈', task: '输出复盘建议与下一步行动' },
]

const currentSession = computed(() => sessionStore.currentSession)
const isLoading = computed(() => sessionStore.fetchingSession || sessionStore.recoveringSession)
const error = computed(() => sessionStore.error)

function stageToStep(stage?: string | null): WorkflowStepNumber {
  if (stage === 'UNDERSTANDING') return 2
  if (stage === 'TRAINING') return 3
  if (stage === 'REFLECTION') return 4
  return 1
}

const backendStep = computed<WorkflowStepNumber>(() => {
  const nextTaskStage = sessionStore.nextTask?.stage
  if (nextTaskStage) return stageToStep(nextTaskStage)
  return stageToStep(currentSession.value?.currentStage)
})

const currentStep = computed(() => browsingStep.value)
const currentStepMeta = computed(() => steps.find((item) => item.step === currentStep.value) ?? steps[0]!)

const stepInputEntries = computed<Array<[string, unknown]>>(() => {
  if (!currentSession.value) return []
  const common: Array<[string, unknown]> = [
    ['goal', currentSession.value.goalText],
    ['courseId', currentSession.value.courseId],
    ['chapterId', currentSession.value.chapterId],
  ]
  if (currentStep.value === 1) return common
  if (currentStep.value === 2) return [...common, ['currentStage', currentSession.value.currentStage], ['nextTask', sessionStore.nextTask?.taskId ?? 'N/A']]
  if (currentStep.value === 3) return [['nextTask', sessionStore.nextTask?.taskId ?? 'N/A'], ['nextTaskStage', sessionStore.nextTask?.stage ?? 'N/A']]
  return [['progress', currentSession.value.progress?.completionRate ?? 0]]
})

const stepOutputEntries = computed<Array<[string, unknown]>>(() => {
  if (!currentSession.value) return []
  if (currentStep.value === 1) return [['summary', '会话创建成功，已完成目标诊断初始化。']]
  if (currentStep.value === 2) return [['timeline', currentSession.value.timeline.map((item) => `${item.stage}:${item.status}`)], ['summary', '学习路径已生成。']]
  if (currentStep.value === 3) return [['mastery', currentSession.value.masterySummary.map((item) => `${item.nodeName}:${Math.round(item.masteryValue * 100)}%`)]]
  return [['summary', '可以返回首页开始新目标。']]
})

function toLabel(key: string | number) {
  const mapping: Record<string, string> = {
    goal: '学习目标',
    courseId: '课程 ID',
    chapterId: '章节 ID',
    currentStage: '当前阶段',
    nextTask: '下一任务',
    nextTaskStage: '下一任务阶段',
    timeline: '路径摘要',
    mastery: '掌握度',
    progress: '完成率',
    summary: '摘要',
  }
  const normalized = String(key)
  return mapping[normalized] ?? normalized
}

function valueToText(value: unknown) {
  if (Array.isArray(value)) return value.join(' / ')
  if (typeof value === 'number' && value <= 1 && value >= 0) return `${Math.round(value * 100)}%`
  if (typeof value === 'object' && value !== null) return JSON.stringify(value)
  return String(value)
}

function syncStepByBackend() {
  if (!hasInitializedStep.value) {
    browsingStep.value = backendStep.value
    hasInitializedStep.value = true
    return
  }
  if (backendStep.value > browsingStep.value) {
    browsingStep.value = backendStep.value
  }
}

async function fetchSession() {
  await sessionStore.fetchSessionOverview(sessionId.value)
  await sessionStore.fetchSessionPath(sessionId.value)
  syncStepByBackend()
}

function goToStep(step: WorkflowStepNumber) {
  browsingStep.value = step
}

function handlePrevious() {
  if (currentStep.value > 1) goToStep((currentStep.value - 1) as WorkflowStepNumber)
}

function handleNext() {
  if (currentStep.value < 4) {
    goToStep((currentStep.value + 1) as WorkflowStepNumber)
  }
}

function handleRunTask() {
  if (!sessionStore.nextTask) return
  const targetPath =
    sessionStore.nextTask.stage === 'TRAINING'
      ? `/task/${sessionStore.nextTask.taskId}/submit`
      : `/task/${sessionStore.nextTask.taskId}/run`
  router.push({ path: targetPath, query: { sessionId: String(sessionId.value) } })
}

async function goHome() {
  localStorage.setItem(SKIP_RESUME_ONCE_KEY, '1')
  await router.replace({ name: 'home', query: { skipResume: '1' } })
}

async function handleRetry() {
  await fetchSession()
}

onMounted(async () => {
  await fetchSession()
})
</script>

<template>
  <main class="workflow-page">
    <header class="toolbar">
      <button class="ghost-button" @click="goHome">返回首页</button>
      <span class="workflow-id">Session #{{ sessionId }}</span>
    </header>

    <SessionSkeleton v-if="isLoading && !currentSession" />
    <ErrorMessage v-else-if="error && !currentSession" :message="error" @retry="handleRetry" />

    <section v-else class="workflow-content">
      <PageHeader eyebrow="Learning Flow" title="四步学习流程执行页" :subtitle="`当前任务：${currentStepMeta.task}`" />
      <StepProgress :steps="steps" :current-step="currentStep" />

      <article class="step-card">
        <div class="step-head">
          <h2>{{ currentStepMeta.step }}. {{ currentStepMeta.title }}</h2>
          <span class="status-tag">后端推荐步骤：第 {{ backendStep }} 步</span>
        </div>

        <div class="step-grid">
          <section class="panel">
            <h3>当前输入</h3>
            <p v-if="stepInputEntries.length === 0" class="empty-text">暂无输入</p>
            <dl v-else class="entry-list">
              <template v-for="[key, value] in stepInputEntries" :key="String(key)">
                <dt>{{ toLabel(key) }}</dt>
                <dd>{{ valueToText(value) }}</dd>
              </template>
            </dl>
          </section>

          <section class="panel">
            <h3>本步产出</h3>
            <p v-if="stepOutputEntries.length === 0" class="empty-text">暂无产出</p>
            <dl v-else class="entry-list">
              <template v-for="[key, value] in stepOutputEntries" :key="String(key)">
                <dt>{{ toLabel(key) }}</dt>
                <dd>{{ valueToText(value) }}</dd>
              </template>
            </dl>
          </section>
        </div>

        <section class="notes">
          <label for="notes">步骤备注</label>
          <textarea id="notes" v-model="notes" rows="3" placeholder="记录本步关注点。"></textarea>
        </section>

        <nav class="actions">
          <button class="ghost-button" :disabled="currentStep === 1" @click="handlePrevious">上一步</button>
          <PrimaryButton v-if="currentStep < 4" type="button" :disabled="isLoading" @click="handleNext">下一步</PrimaryButton>
          <PrimaryButton v-else type="button" :disabled="isLoading" @click="goHome">完成并返回首页</PrimaryButton>
        </nav>

        <nav v-if="currentStep === 3 && sessionStore.nextTask" class="actions">
          <PrimaryButton type="button" :disabled="isLoading" @click="handleRunTask">进入任务执行</PrimaryButton>
        </nav>
      </article>
    </section>
  </main>
</template>

<style scoped>
.workflow-page { min-height: 100dvh; padding: clamp(16px, 2.8vw, 30px); }
.toolbar { display: flex; justify-content: space-between; align-items: center; margin-bottom: var(--space-lg); }
.workflow-id { color: var(--color-text-secondary); font-size: var(--font-size-sm); }
.workflow-content { display: flex; flex-direction: column; gap: var(--space-lg); max-width: 960px; margin: 0 auto; }
.step-card { border: 1px solid var(--color-border); border-radius: var(--radius-xl); background: linear-gradient(165deg, rgba(16, 27, 50, 0.94), rgba(8, 14, 26, 0.96)); padding: clamp(16px, 2.8vw, 26px); box-shadow: var(--shadow-md); display: flex; flex-direction: column; gap: var(--space-lg); }
.step-head { display: flex; justify-content: space-between; align-items: center; gap: var(--space-sm); }
.status-tag { border: 1px solid var(--color-border); border-radius: 999px; padding: 4px 10px; color: var(--color-text-secondary); font-size: var(--font-size-xs); }
.step-grid { display: grid; grid-template-columns: 1fr 1fr; gap: var(--space-md); }
.panel { border: 1px solid var(--color-border); border-radius: var(--radius-md); background: rgba(12, 20, 38, 0.8); padding: var(--space-md); }
.entry-list { display: grid; grid-template-columns: 120px 1fr; gap: var(--space-sm); margin: 0; }
.entry-list dt { color: var(--color-text-secondary); font-size: var(--font-size-sm); }
.entry-list dd { margin: 0; color: var(--color-text); font-size: var(--font-size-sm); word-break: break-word; }
.empty-text { color: var(--color-text-secondary); font-size: var(--font-size-sm); }
.notes { display: flex; flex-direction: column; gap: var(--space-sm); }
.notes textarea { width: 100%; border: 1px solid var(--color-border); border-radius: var(--radius-md); background: #0a1225; color: var(--color-text); padding: var(--space-md); }
.actions { display: grid; grid-template-columns: 140px 1fr; gap: var(--space-md); }
.ghost-button { min-height: 44px; border: 1px solid var(--color-border); border-radius: var(--radius-md); color: var(--color-text-secondary); background: rgba(12, 21, 42, 0.8); }
@media (max-width: 900px) { .step-grid, .actions { grid-template-columns: 1fr; } }
</style>
