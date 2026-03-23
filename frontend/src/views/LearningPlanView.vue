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
        <DiagnosisRecapCard
          v-if="showDiagnosisRecap"
          :bullets="recapBullets"
          :closing-line="recapClosing"
          @dismiss="onDismissDiagnosisRecap"
        />
        <PlanHero
          :path-summary-line="viewModel.pathSummaryLine"
          :total-steps="viewModel.totalSteps"
          :showcase="viewModel.showcase"
          :user-state-line="userStateSummaryLine"
        />
        <ShowcaseMindImageHint
          v-if="viewModel.showcase?.mindImageHint"
          :hint="viewModel.showcase.mindImageHint"
        />
        <StepFlow :items="flowItems" />
        <CurrentStepCard
          :step="currentStep"
          :showcase-focus="viewModel.showcase?.focusType ?? 'default'"
          :loading="committing"
          @start="onStartStep"
        />
        <ShowcaseJudgmentTips
          v-if="viewModel.showcase?.judgmentTips?.length"
          :tips="viewModel.showcase.judgmentTips"
        />
        <OptionalTips :tips="viewModel.optionalTips" />

        <section
          class="mt-10 rounded-card border border-border bg-slate-50/80 px-5 py-6 text-center md:text-left"
        >
          <p class="text-sm font-medium text-text-primary md:text-base">
            <span aria-hidden="true">👇</span>
            下一步你可以直接开始这一小步
          </p>
          <PrimaryButton
            class="mt-4 w-full justify-center py-3 md:inline-flex md:w-auto md:min-w-[240px]"
            :loading="committing"
            @click="onStartStep"
          >
            进入学习（执行页）
          </PrimaryButton>
        </section>
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
import PrimaryButton from '@/components/ui/PrimaryButton.vue'
import LoadingState from '@/components/ui/LoadingState.vue'
import ErrorState from '@/components/ui/ErrorState.vue'
import PlanHero from '@/components/plan/PlanHero.vue'
import StepFlow from '@/components/plan/StepFlow.vue'
import CurrentStepCard from '@/components/plan/CurrentStepCard.vue'
import OptionalTips from '@/components/plan/OptionalTips.vue'
import ShowcaseMindImageHint from '@/components/plan/ShowcaseMindImageHint.vue'
import ShowcaseJudgmentTips from '@/components/plan/ShowcaseJudgmentTips.vue'
import DiagnosisRecapCard from '@/components/plan/DiagnosisRecapCard.vue'
import { useWorkflowStore } from '@/stores/workflow'
import { previewPlan, commitPlan } from '@/api/learning-plan'
import { showToast } from '@/stores/toast'
import { getErrorMessage } from '@/api/request'
import {
  buildPlanViewModel,
  planStepsToFlowItems,
} from '@/utils/planPresentationModel'
import {
  buildDiagnosisRecapBullets,
  buildUserStateSummaryLine,
  clearDiagnosisRecapSessionFlag,
  diagnosisRecapClosingLine,
  shouldShowDiagnosisRecapFromSession,
} from '@/utils/diagnosisRecapCopy'
import type { PlanPreviewData } from '@/types/dto'

const router = useRouter()
const store = useWorkflowStore()

const loading = ref(true)
const committing = ref(false)
const error = ref<string | null>(null)
const plan = ref<PlanPreviewData | null>(null)
const showDiagnosisRecap = ref(false)

const recapBullets = computed(() =>
  buildDiagnosisRecapBullets(store.learnerProfileSnapshot)
)
const recapClosing = diagnosisRecapClosingLine()
const userStateSummaryLine = computed(() =>
  buildUserStateSummaryLine(store.learnerProfileSnapshot)
)

function onDismissDiagnosisRecap() {
  showDiagnosisRecap.value = false
  clearDiagnosisRecapSessionFlag()
}

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

function goExecution() {
  const query: Record<string, string> = { step: '1' }
  if (store.planId) query.planId = store.planId
  router.push({ path: '/execution', query })
}

async function onStartStep() {
  if (store.sessionId) {
    goExecution()
    return
  }
  if (!store.planId) return
  committing.value = true
  try {
    const data = await commitPlan(store.planId)
    store.sessionId = data.sessionId
    store.currentTaskId = data.currentTaskId
    store.taskSequence = data.taskSequence ?? []
    goExecution()
  } catch (err) {
    showToast(getErrorMessage(err))
  } finally {
    committing.value = false
  }
}

onMounted(() => {
  showDiagnosisRecap.value = shouldShowDiagnosisRecapFromSession()
  if (store.planPreview && !plan.value) plan.value = store.planPreview
  if (!plan.value) fetchPlan()
})
</script>
