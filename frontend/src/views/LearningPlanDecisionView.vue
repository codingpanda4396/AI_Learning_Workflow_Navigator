<template>
  <PageContainer>
    <div class="min-h-screen bg-[linear-gradient(180deg,#f8fafc_0%,#eef2ff_24%,#f8fafc_100%)]">
      <AppTopBar current="plan" />
      <main
        class="mx-auto flex w-full max-w-6xl flex-col gap-6 px-4 pb-16 pt-4 md:px-6 lg:px-8"
      >
        <LoadingState
          v-if="loading && !plan"
          :message="PLAN_COPY.loading"
        />
        <ErrorState
          v-else-if="error"
          :message="error"
        >
          <template #action>
            <SecondaryButton @click="fetchPlan">{{ PLAN_COPY.reload }}</SecondaryButton>
          </template>
        </ErrorState>

        <template v-else-if="plan && decisionModel">
          <PlanDecisionHero
            :model="decisionModel.hero"
            :loading="committing"
            :disabled="!canCommit"
            @start="onStartStep"
          />

          <PlanReasonBlock :model="decisionModel.reasoning" />

          <section class="grid gap-6 xl:grid-cols-[minmax(0,1.35fr)_minmax(320px,0.9fr)]">
            <PlanPathPreview :model="decisionModel.pathPreview" />
            <PlanFirstTaskCard :model="decisionModel.firstTask" />
          </section>

          <section class="rounded-[28px] border border-slate-200/80 bg-white px-5 py-6 shadow-[0_18px_60px_rgba(15,23,42,0.08)] md:px-8">
            <div class="flex flex-col gap-5 md:flex-row md:items-end md:justify-between">
              <div class="max-w-2xl">
                <p class="text-xs font-medium uppercase tracking-[0.22em] text-slate-500">
                  最后一步
                </p>
                <h2 class="mt-3 text-2xl font-semibold tracking-tight text-slate-950 md:text-3xl">
                  现在进入执行页，先完成第一步。
                </h2>
                <p class="mt-3 text-sm leading-7 text-slate-600 md:text-base">
                  {{ decisionModel.contrast }}
                </p>
              </div>

              <div class="flex w-full flex-col gap-3 md:w-auto md:items-end">
                <PrimaryButton
                  class="w-full justify-center md:min-w-[220px]"
                  :loading="committing"
                  :disabled="!canCommit"
                  @click="onStartStep"
                >
                  {{ decisionModel.hero.ctaLabel }}
                </PrimaryButton>
                <p class="text-sm text-slate-500">
                  {{ decisionModel.hero.ctaSubtext }}
                </p>
              </div>
            </div>
          </section>
        </template>
      </main>
    </div>
  </PageContainer>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import PageContainer from '@/components/layout/PageContainer.vue'
import AppTopBar from '@/components/layout/AppTopBar.vue'
import PrimaryButton from '@/components/ui/PrimaryButton.vue'
import SecondaryButton from '@/components/ui/SecondaryButton.vue'
import LoadingState from '@/components/ui/LoadingState.vue'
import ErrorState from '@/components/ui/ErrorState.vue'
import PlanDecisionHero from '@/components/plan/decision/PlanDecisionHero.vue'
import PlanReasonBlock from '@/components/plan/decision/PlanReasonBlock.vue'
import PlanFirstTaskCard from '@/components/plan/decision/PlanFirstTaskCard.vue'
import PlanPathPreview from '@/components/plan/decision/PlanPathPreview.vue'
import { useWorkflowStore } from '@/stores/workflow'
import { previewPlan, commitPlan } from '@/api/learning-plan'
import { showToast } from '@/stores/toast'
import { getErrorMessage } from '@/api/request'
import { buildLearningPlanDecisionViewModel } from '@/utils/learningPlanDecisionModel'
import { clearDiagnosisRecapSessionFlag } from '@/utils/diagnosisRecapCopy'
import type { PlanPreviewData } from '@/types/dto'
import { PLAN_COPY } from '@/constants/uiCopy'

const router = useRouter()
const store = useWorkflowStore()

const loading = ref(true)
const committing = ref(false)
const error = ref<string | null>(null)
const plan = ref<PlanPreviewData | null>(null)

const decisionModel = computed(() => {
  if (!plan.value) return null
  return buildLearningPlanDecisionViewModel(plan.value, {
    structuredGoal: store.structuredGoal,
    goalContextSnapshot: store.goalContextSnapshot,
    learnerProfileSnapshot: store.learnerProfileSnapshot,
    diagnosisEvidenceSummary: store.diagnosisEvidenceSummary,
  })
})

const canCommit = computed(() => Boolean(store.planId))

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
  router.push({ name: 'task' })
}

async function onStartStep() {
  if (store.sessionId && store.currentTaskId) {
    goExecution()
    return
  }
  if (!store.planId) return
  committing.value = true
  try {
    if (store.sessionId && !store.currentTaskId) {
      store.sessionId = null
      store.currentTask = null
      store.progress = null
      store.taskSequence = []
    }
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
  clearDiagnosisRecapSessionFlag()
  if (store.planPreview && !plan.value) plan.value = store.planPreview
  if (plan.value?.planId && !store.planId) {
    store.planId = plan.value.planId
  }
  if (!plan.value) fetchPlan()
  else loading.value = false
})
</script>
