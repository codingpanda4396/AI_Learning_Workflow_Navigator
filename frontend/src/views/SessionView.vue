<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useSessionStore } from '@/stores/session'
import { useWorkflowStore } from '@/stores/workflow'
import type { WorkflowStepNumber } from '@/types/workflow'
import PageHeader from '@/components/PageHeader.vue'
import StepProgress from '@/components/StepProgress.vue'
import PrimaryButton from '@/components/PrimaryButton.vue'
import SessionSkeleton from '@/components/SessionSkeleton.vue'
import ErrorMessage from '@/components/ErrorMessage.vue'

type StepKey = `step${WorkflowStepNumber}`

interface StepMeta {
  step: WorkflowStepNumber
  title: string
  task: string
}

const route = useRoute()
const router = useRouter()
const sessionStore = useSessionStore()
const workflowStore = useWorkflowStore()

const sessionId = computed(() => Number(route.params.id))
const notes = ref('')

const steps: StepMeta[] = [
  { step: 1, title: '诊断目标', task: '明确目标边界和能力基线' },
  { step: 2, title: '生成路径', task: '拆解学习节点和阶段路径' },
  { step: 3, title: '分步学习', task: '执行任务并记录掌握度变化' },
  { step: 4, title: '总结反馈', task: '输出复盘建议与下一步行动' },
]

const currentSession = computed(() => sessionStore.currentSession)
const isLoading = computed(() => sessionStore.isLoading)
const error = computed(() => sessionStore.error)
const currentStep = computed(() => workflowStore.currentStep)
const workflowId = computed(() => workflowStore.workflowId)

const currentStepMeta = computed<StepMeta>(
  () => steps.find((item) => item.step === currentStep.value) ?? steps[0]!,
)

const currentStepState = computed(() => {
  const key = `step${currentStep.value}` as StepKey
  return workflowStore.stepData[key]
})

const stepInputEntries = computed(() => {
  const input = currentStepState.value?.input ?? {}
  return Object.entries(input)
})

const stepOutputEntries = computed(() => {
  const output = currentStepState.value?.output ?? {}
  return Object.entries(output).filter(([key]) => key !== 'notes')
})

function toLabel(key: string) {
  const mapping: Record<string, string> = {
    goal: '学习目标',
    courseId: '课程 ID',
    chapterId: '章节 ID',
    stage: '当前阶段',
    timeline: '路径摘要',
    nextTask: '下一任务',
    mastery: '掌握度',
    summary: '摘要',
  }
  return mapping[key] ?? key
}

function valueToText(value: unknown) {
  if (Array.isArray(value)) {
    return value.join(' / ')
  }
  if (typeof value === 'object' && value !== null) {
    return JSON.stringify(value)
  }
  return String(value)
}

function syncStepNotes() {
  const outputNotes = currentStepState.value?.output.notes
  notes.value = typeof outputNotes === 'string' ? outputNotes : ''
}

function updateStepNotes() {
  const key = currentStep.value
  workflowStore.setStepState(key, {
    output: {
      ...(currentStepState.value?.output ?? {}),
      notes: notes.value,
    },
  })
}

function initializeBaseStepData() {
  if (!workflowStore.goal && currentSession.value?.goal_text) {
    workflowStore.startWorkflow({
      goal: currentSession.value.goal_text,
      courseId: currentSession.value.course_id,
      chapterId: currentSession.value.chapter_id,
    })
  }

  workflowStore.setStepState(1, {
    input: {
      goal: workflowStore.goal || currentSession.value?.goal_text || '',
      courseId: workflowStore.courseId || currentSession.value?.course_id || '',
      chapterId: workflowStore.chapterId || currentSession.value?.chapter_id || '',
    },
    output: {
      summary: '已进入诊断流程，准备校准目标范围。',
    },
    status: 'done',
  })
}

function hydrateFromSessionOverview() {
  if (!currentSession.value) {
    return
  }

  const stageLabel = currentSession.value.current_stage
  const timeline = currentSession.value.timeline.map((item) => `${item.stage}:${item.status}`)
  const mastery = currentSession.value.mastery_summary
    .slice(0, 3)
    .map((item) => `${item.node_name} ${Math.round(item.mastery_value * 100)}%`)

  workflowStore.setStepState(2, {
    input: {
      goal: currentSession.value.goal_text,
      stage: stageLabel,
    },
    output: {
      timeline,
      summary: '学习路径已拆分为结构、理解、训练、反思四段。',
    },
    status: 'done',
  })

  workflowStore.setStepState(3, {
    input: {
      stage: stageLabel,
      nextTask: currentSession.value.next_task.task_id,
    },
    output: {
      mastery,
      summary: '可按任务顺序推进，每一步记录掌握度变化。',
    },
    status: 'running',
  })

  workflowStore.setStepState(4, {
    input: {
      mastery: mastery.length > 0 ? mastery.join(', ') : '暂无',
    },
    output: {
      summary: '完成后可生成总结反馈并规划下一轮学习目标。',
    },
    status: 'idle',
  })
}

async function fetchSession() {
  await sessionStore.fetchSessionOverview(sessionId.value)
  initializeBaseStepData()
  hydrateFromSessionOverview()
  syncStepNotes()
}

function goToStep(step: WorkflowStepNumber) {
  workflowStore.setCurrentStep(step)
  syncStepNotes()
}

function handlePrevious() {
  if (currentStep.value <= 1) {
    return
  }
  goToStep((currentStep.value - 1) as WorkflowStepNumber)
}

function handleNext() {
  updateStepNotes()
  if (currentStep.value >= 4) {
    return
  }

  workflowStore.setStepState(currentStep.value, { status: 'done' })
  const nextStep = (currentStep.value + 1) as WorkflowStepNumber
  workflowStore.setStepState(nextStep, { status: 'running' })
  goToStep(nextStep)
}

function handleFinish() {
  updateStepNotes()
  workflowStore.setStepState(4, { status: 'done' })
  router.push('/')
}

async function handleRetry() {
  await fetchSession()
}

onMounted(async () => {
  workflowStore.loadFromStorage()
  workflowStore.setWorkflowId(String(sessionId.value))
  await fetchSession()
})
</script>

<template>
  <main class="workflow-page">
    <header class="toolbar">
      <button class="ghost-button" @click="router.push('/')">返回首页</button>
      <span class="workflow-id">Workflow #{{ workflowId || sessionId }}</span>
    </header>

    <SessionSkeleton v-if="isLoading && !currentSession" />

    <ErrorMessage
      v-else-if="error && !currentSession"
      :message="error"
      @retry="handleRetry"
    />

    <section v-else class="workflow-content">
      <PageHeader
        eyebrow="Learning Flow"
        title="四步学习流程执行页"
        :subtitle="`当前任务：${currentStepMeta.task}`"
      />

      <StepProgress :steps="steps" :current-step="currentStep" />

      <article class="step-card">
        <div class="step-head">
          <h2>{{ currentStepMeta.step }}. {{ currentStepMeta.title }}</h2>
          <span class="status-tag">状态：{{ currentStepState?.status || 'idle' }}</span>
        </div>

        <div class="step-grid">
          <section class="panel">
            <h3>当前输入</h3>
            <p v-if="stepInputEntries.length === 0" class="empty-text">暂无输入</p>
            <dl v-else class="entry-list">
              <template v-for="[key, value] in stepInputEntries" :key="key">
                <dt>{{ toLabel(key) }}</dt>
                <dd>{{ valueToText(value) }}</dd>
              </template>
            </dl>
          </section>

          <section class="panel">
            <h3>本步产出</h3>
            <p v-if="stepOutputEntries.length === 0" class="empty-text">暂无产出</p>
            <dl v-else class="entry-list">
              <template v-for="[key, value] in stepOutputEntries" :key="key">
                <dt>{{ toLabel(key) }}</dt>
                <dd>{{ valueToText(value) }}</dd>
              </template>
            </dl>
          </section>
        </div>

        <section class="notes">
          <label for="notes">步骤备注（保存在前端状态中）</label>
          <textarea
            id="notes"
            v-model="notes"
            rows="3"
            placeholder="记录本步的判断依据、问题或后续联调关注点。"
            @blur="updateStepNotes"
          ></textarea>
        </section>

        <nav class="actions">
          <button class="ghost-button" :disabled="currentStep === 1" @click="handlePrevious">
            上一步
          </button>
          <PrimaryButton
            v-if="currentStep < 4"
            type="button"
            :disabled="isLoading"
            @click="handleNext"
          >
            下一步
          </PrimaryButton>
          <PrimaryButton
            v-else
            type="button"
            :disabled="isLoading"
            @click="handleFinish"
          >
            完成并返回首页
          </PrimaryButton>
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
  justify-content: space-between;
  align-items: center;
  margin-bottom: var(--space-lg);
}

.workflow-id {
  color: var(--color-text-secondary);
  font-size: var(--font-size-sm);
}

.workflow-content {
  display: flex;
  flex-direction: column;
  gap: var(--space-lg);
  max-width: 960px;
  margin: 0 auto;
}

.step-card {
  border: 1px solid var(--color-border);
  border-radius: var(--radius-xl);
  background: linear-gradient(165deg, rgba(16, 27, 50, 0.94), rgba(8, 14, 26, 0.96));
  padding: clamp(16px, 2.8vw, 26px);
  box-shadow: var(--shadow-md);
  display: flex;
  flex-direction: column;
  gap: var(--space-lg);
}

.step-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: var(--space-sm);
}

.step-head h2 {
  font-size: var(--font-size-lg);
}

.status-tag {
  border: 1px solid var(--color-border);
  border-radius: 999px;
  padding: 4px 10px;
  color: var(--color-text-secondary);
  font-size: var(--font-size-xs);
}

.step-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: var(--space-md);
}

.panel {
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  background: rgba(12, 20, 38, 0.8);
  padding: var(--space-md);
}

.panel h3 {
  margin-bottom: var(--space-sm);
  font-size: var(--font-size-md);
}

.entry-list {
  display: grid;
  grid-template-columns: 120px 1fr;
  gap: var(--space-sm);
  margin: 0;
}

.entry-list dt {
  color: var(--color-text-secondary);
  font-size: var(--font-size-sm);
}

.entry-list dd {
  margin: 0;
  color: var(--color-text);
  font-size: var(--font-size-sm);
  word-break: break-word;
}

.empty-text {
  color: var(--color-text-secondary);
  font-size: var(--font-size-sm);
}

.notes {
  display: flex;
  flex-direction: column;
  gap: var(--space-sm);
}

.notes label {
  font-size: var(--font-size-sm);
  color: var(--color-text-secondary);
}

.notes textarea {
  width: 100%;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  background: #0a1225;
  color: var(--color-text);
  padding: var(--space-md);
}

.notes textarea:focus {
  border-color: var(--color-primary);
  box-shadow: 0 0 0 3px var(--color-primary-alpha);
}

.actions {
  display: grid;
  grid-template-columns: 140px 1fr;
  gap: var(--space-md);
}

.ghost-button {
  min-height: 44px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  color: var(--color-text-secondary);
  background: rgba(12, 21, 42, 0.8);
  transition: border-color 150ms ease, color 150ms ease;
}

.ghost-button:hover:not(:disabled) {
  border-color: var(--color-primary);
  color: var(--color-text);
}

.ghost-button:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

@media (max-width: 900px) {
  .step-grid {
    grid-template-columns: 1fr;
  }

  .actions {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .workflow-page {
    padding: max(14px, env(safe-area-inset-top)) 14px calc(18px + env(safe-area-inset-bottom));
  }
}
</style>
