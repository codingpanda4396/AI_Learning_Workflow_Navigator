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
      <ActionFlow v-model:phase="phase" :step="mergedStep" @completed="onStepCompleted" />
    </main>
  </PageContainer>
</template>

<script setup lang="ts">
import { computed, ref, watch, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import PageContainer from '@/components/layout/PageContainer.vue'
import AppTopBar from '@/components/layout/AppTopBar.vue'
import StepHeader from '@/components/execution/StepHeader.vue'
import ProgressBar from '@/components/execution/ProgressBar.vue'
import ActionFlow from '@/components/execution/ActionFlow.vue'
import {
  EXECUTION_PHASE_COUNT,
  EXECUTION_PHASE_LABELS,
} from '@/constants/executionPath'
import { getExecutionStepConfig } from '@/constants/executionSteps'
import { useWorkflowStore } from '@/stores/workflow'
import { showToast } from '@/stores/toast'
import type { ExecutionState, ExecutionStep } from '@/types/execution'

const route = useRoute()
const router = useRouter()
const store = useWorkflowStore()

const phase = ref<ExecutionState>('PROMPT_SHOWN')

const stepIndex = computed(() => {
  const raw = route.query.step
  const n = typeof raw === 'string' ? parseInt(raw, 10) : 1
  return Number.isFinite(n) ? n : 1
})

/** 路径展示用：夹在 1..EXECUTION_PHASE_COUNT */
const pathCurrent = computed(() => {
  const n = stepIndex.value
  if (n < 1) return 1
  if (n > EXECUTION_PHASE_COUNT) return EXECUTION_PHASE_COUNT
  return n
})

const stepTemplate = computed<ExecutionStep>(() =>
  getExecutionStepConfig(stepIndex.value)
)

const mergedStep = computed<ExecutionStep>(() => ({
  ...stepTemplate.value,
  state: phase.value,
}))

watch(stepIndex, () => {
  phase.value = 'PROMPT_SHOWN'
})

onMounted(() => {
  const raw = route.query.step
  if (raw === undefined || String(raw).trim() === '') {
    return
  }
  const n = typeof raw === 'string' ? parseInt(raw, 10) : NaN
  const inRange =
    Number.isFinite(n) && n >= 1 && n <= EXECUTION_PHASE_COUNT
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
