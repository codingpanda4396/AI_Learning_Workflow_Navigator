<template>
  <PageContainer>
    <div class="min-h-screen bg-background">
      <AppTopBar current="plan" />
      <main class="mx-auto flex w-full max-w-3xl flex-col px-4 pb-10 pt-4 md:max-w-4xl md:px-6 lg:px-8">
        <LoadingState v-if="loading && !plan" :message="PLAN_COPY.loading" />
        <ErrorState v-else-if="error" :message="error">
          <template #action>
            <SecondaryButton @click="fetchPlan">{{ PLAN_COPY.reload }}</SecondaryButton>
          </template>
        </ErrorState>

        <div
          v-else-if="plan && actionPanel"
          class="space-y-6"
        >
          <PlanHero
            :topic="actionPanel.hero.topic"
            :headline="actionPanel.hero.headline"
            :subline="actionPanel.hero.subline"
            :current-problem-label="actionPanel.hero.currentProblemLabel"
            :problem-line="actionPanel.hero.problemLine"
            :strategy-line="actionPanel.hero.strategyLine"
            :recommended-stage-label-zh="actionPanel.hero.recommendedStageLabelZh"
            :recommended-stage-label-en="actionPanel.hero.recommendedStageLabelEn"
            :total-estimated-label="actionPanel.hero.totalEstimatedLabel"
            :total-steps="actionPanel.hero.totalSteps"
            :why-this-first="actionPanel.hero.whyThisFirst"
            :start-button-label="actionPanel.hero.startButtonLabel"
            :loading="committing"
            @start="onStartStep"
          />

          <PlanTimeline
            :items="actionPanel.timeline"
          />

          <PlanStepAccordion
            :panels="actionPanel.stagePanels"
            :loading="committing"
            :action-label="actionPanel.hero.startButtonLabel"
            @start="onStartStep"
          />
        </div>
      </main>
    </div>
  </PageContainer>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import PageContainer from '@/components/layout/PageContainer.vue'
import AppTopBar from '@/components/layout/AppTopBar.vue'
import SecondaryButton from '@/components/ui/SecondaryButton.vue'
import LoadingState from '@/components/ui/LoadingState.vue'
import ErrorState from '@/components/ui/ErrorState.vue'
import PlanHero from '@/components/plan/PlanHero.vue'
import PlanTimeline from '@/components/plan/PlanTimeline.vue'
import PlanStepAccordion from '@/components/plan/PlanStepAccordion.vue'
import { useWorkflowStore } from '@/stores/workflow'
import { previewPlan, commitPlan } from '@/api/learning-plan'
import { showToast } from '@/stores/toast'
import { getErrorMessage } from '@/api/request'
import { buildPlanActionPanelView } from '@/utils/planPresentationModel'
import { clearDiagnosisRecapSessionFlag } from '@/utils/diagnosisRecapCopy'
import type { PlanPreviewData } from '@/types/dto'
import { PLAN_COPY } from '@/constants/uiCopy'

const router = useRouter()
const store = useWorkflowStore()

const loading = ref(true)
const committing = ref(false)
const error = ref<string | null>(null)
const plan = ref<PlanPreviewData | null>(null)

const actionPanel = computed(() =>
  buildPlanActionPanelView(plan.value, {
    structuredGoal: store.structuredGoal,
    learnerProfileSnapshot: store.learnerProfileSnapshot,
    sessionId: store.sessionId,
    progress: store.progress,
    taskSequence: store.taskSequence,
  })
)

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
