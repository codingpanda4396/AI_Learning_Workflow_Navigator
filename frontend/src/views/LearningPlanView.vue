<template>
  <PageContainer>
    <AppTopBar current="plan" />
    <main class="mx-auto max-w-2xl px-6 py-10 md:py-14">
      <LoadingState v-if="loading && !plan" message="正在帮你理一理怎么学…" />
      <ErrorState v-else-if="error" :message="error">
        <template #action>
          <SecondaryButton @click="fetchPlan">重试</SecondaryButton>
        </template>
      </ErrorState>

      <template v-else-if="plan && viewModel && currentStep">
        <PlanHero
          :path-summary-line="viewModel.pathSummaryLine"
          :total-steps="viewModel.totalSteps"
          :showcase="viewModel.showcase"
        />
        <StepFlow :items="flowItems" />
        <CurrentStepCard
          :step="currentStep"
          :loading="committing"
          @start="onStartStep"
        />
        <OptionalTips :tips="viewModel.optionalTips" />
      </template>
    </main>
  </PageContainer>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import PageContainer from '@/components/layout/PageContainer.vue'
import AppTopBar from '@/components/layout/AppTopBar.vue'
import SecondaryButton from '@/components/ui/SecondaryButton.vue'
import LoadingState from '@/components/ui/LoadingState.vue'
import ErrorState from '@/components/ui/ErrorState.vue'
import PlanHero from '@/components/plan/PlanHero.vue'
import StepFlow from '@/components/plan/StepFlow.vue'
import CurrentStepCard from '@/components/plan/CurrentStepCard.vue'
import OptionalTips from '@/components/plan/OptionalTips.vue'
import { useWorkflowStore } from '@/stores/workflow'
import { previewPlan, commitPlan } from '@/api/learning-plan'
import { showToast } from '@/stores/toast'
import { getErrorMessage } from '@/api/request'
import {
  buildPlanViewModel,
  planStepsToFlowItems,
} from '@/utils/planPresentationModel'
import type { PlanPreviewData } from '@/types/dto'

const router = useRouter()
const store = useWorkflowStore()

const loading = ref(true)
const committing = ref(false)
const error = ref<string | null>(null)
const plan = ref<PlanPreviewData | null>(null)

const viewModel = computed(() =>
  buildPlanViewModel(plan.value, {
    structuredGoal: store.structuredGoal,
    learnerProfileSnapshot: store.learnerProfileSnapshot,
    sessionId: store.sessionId,
    progress: store.progress,
    taskSequence: store.taskSequence,
  })
)

const flowItems = computed(() =>
  viewModel.value ? planStepsToFlowItems(viewModel.value) : []
)

const currentStep = computed(() => {
  const vm = viewModel.value
  if (!vm?.steps.length) return null
  const i = Math.min(vm.currentStepIndex, vm.steps.length - 1)
  return vm.steps[i] ?? null
})

async function fetchPlan() {
  if (!store.goalId || !store.diagnosisId) return
  loading.value = true
  error.value = null
  try {
    const data = await previewPlan(store.goalId, store.diagnosisId)
    store.planId = data.planId
    store.planPreview = data
    plan.value = data
  } catch (err) {
    error.value = getErrorMessage(err)
  } finally {
    loading.value = false
  }
}

async function onStartStep() {
  if (store.sessionId) {
    if (store.currentTaskId) {
      router.push({
        name: 'taskRun',
        params: { taskId: store.currentTaskId },
      })
    } else {
      router.push({ name: 'task' })
    }
    return
  }
  if (!store.planId) return
  committing.value = true
  try {
    const data = await commitPlan(store.planId)
    store.sessionId = data.sessionId
    store.currentTaskId = data.currentTaskId
    store.taskSequence = data.taskSequence ?? []
    router.push({
      name: 'taskRun',
      params: { taskId: data.currentTaskId },
    })
  } catch (err) {
    showToast(getErrorMessage(err))
  } finally {
    committing.value = false
  }
}

onMounted(() => {
  if (store.planPreview && !plan.value) plan.value = store.planPreview
  if (!plan.value) fetchPlan()
})
</script>
