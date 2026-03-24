<template>
  <PageContainer>
    <AppTopBar current="task" />
    <main class="mx-auto max-w-2xl px-6 py-8">
      <StepHeader :title="stepTemplate.title" :goal="stepTemplate.goal" />
      <ProgressBar
        :current="pathCurrent"
        :total="EXECUTION_PHASE_COUNT"
        :labels="EXECUTION_PHASE_LABELS"
      />
      <ExecutionMessageFlow :step="mergedStep" @completed="onStepCompleted" />
    </main>
  </PageContainer>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import ExecutionMessageFlow from '@/components/execution/ExecutionMessageFlow.vue'
import ProgressBar from '@/components/execution/ProgressBar.vue'
import StepHeader from '@/components/execution/StepHeader.vue'
import AppTopBar from '@/components/layout/AppTopBar.vue'
import PageContainer from '@/components/layout/PageContainer.vue'
import { EXECUTION_PHASE_COUNT, EXECUTION_PHASE_LABELS } from '@/constants/executionPath'
import { getExecutionStepConfig } from '@/constants/executionSteps'
import { useAiTutorStore } from '@/stores/aiTutor'
import { showToast } from '@/stores/toast'
import { useWorkflowStore } from '@/stores/workflow'
import type { ExecutionState, ExecutionStep } from '@/types/execution'

const route = useRoute()
const router = useRouter()
const store = useWorkflowStore()
const aiTutorStore = useAiTutorStore()

const phase = ref<ExecutionState>('PROMPT_SHOWN')

const stepIndex = computed(() => {
  const raw = route.query.step
  const n = typeof raw === 'string' ? parseInt(raw, 10) : 1
  return Number.isFinite(n) ? n : 1
})

const pathCurrent = computed(() => {
  const n = stepIndex.value
  if (n < 1) return 1
  if (n > EXECUTION_PHASE_COUNT) return EXECUTION_PHASE_COUNT
  return n
})

const stepTemplate = computed<ExecutionStep>(() => getExecutionStepConfig(stepIndex.value))

const mergedStep = computed<ExecutionStep>(() => ({
  ...stepTemplate.value,
  state: phase.value,
}))

function phaseMetaForStep(stepNum: number): { code: string; label: string } {
  if (stepNum === 1) {
    return { code: 'STRUCTURE', label: '结构认知' }
  }
  return { code: 'STRUCTURE', label: '结构认知' }
}

watch(
  () => ({
    stepId: stepTemplate.value.stepId,
    step: stepIndex.value,
    knowledgePoint: stepTemplate.value.knowledgePoint,
    knowledgeKey: stepTemplate.value.knowledgeKey,
  }),
  (cur, prev) => {
    const stepChanged = !prev || cur.stepId !== prev.stepId
    const label = cur.knowledgePoint?.trim() || '当前主题'
    const key =
      (cur.knowledgeKey && cur.knowledgeKey.trim()) ||
      cur.stepId.replace(/[^a-zA-Z0-9_]+/g, '_').toLowerCase() ||
      'unknown'
    const meta = phaseMetaForStep(cur.step)

    if (stepChanged) {
      aiTutorStore.resetForStep({
        stepId: cur.stepId,
        step: cur.step,
        knowledgeKey: key,
        knowledgeLabel: label,
        phaseCode: meta.code,
        phaseLabel: meta.label,
      })
      return
    }

    aiTutorStore.setContext({
      stepId: cur.stepId,
      step: cur.step,
      knowledgeKey: key,
      knowledgeLabel: label,
      phaseCode: meta.code,
      phaseLabel: meta.label,
    })
  },
  { immediate: true }
)

watch(stepIndex, () => {
  phase.value = 'PROMPT_SHOWN'
})

onMounted(() => {
  const raw = route.query.step
  if (raw === undefined || String(raw).trim() === '') {
    return
  }
  const n = typeof raw === 'string' ? parseInt(raw, 10) : NaN
  const inRange = Number.isFinite(n) && n >= 1 && n <= EXECUTION_PHASE_COUNT
  if (!inRange) {
    showToast('阶段序号无效，已为你打开第 1 阶段。')
    router.replace({
      name: 'execution',
      query: { ...route.query, step: '1' },
    })
    return
  }
  if (n !== 1) {
    showToast('当前仅支持第 1 步，已为你打开第 1 步。')
    router.replace({
      name: 'execution',
      query: { ...route.query, step: '1' },
    })
  }
})

function onStepCompleted() {
  if (store.currentTaskId) {
    router.push({
      name: 'taskRun',
      params: { taskId: store.currentTaskId },
    })
  } else {
    router.push({ name: 'report' })
  }
}
</script>
