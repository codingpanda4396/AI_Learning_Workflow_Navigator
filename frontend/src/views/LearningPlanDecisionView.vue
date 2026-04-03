<template>
  <PageContainer>
    <div class="min-h-screen bg-[linear-gradient(180deg,#e8eef5_0%,#ffffff_42%,#f1f5f9_100%)]">
      <AppTopBar current="plan" />
      <main
        class="mx-auto flex w-full max-w-6xl flex-col gap-5 px-4 pb-14 pt-4 md:gap-6 md:px-6 lg:px-8"
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
            @toggle-why="whyOpen = !whyOpen"
          />

          <PlanWhyAccordion
            v-model:expanded="whyOpen"
            :model="decisionModel.reasoning"
          />

          <div class="grid gap-6 xl:grid-cols-12 xl:items-start xl:gap-8">
            <div class="xl:col-span-7">
              <PlanStageRail :model="decisionModel.pathPreview" />
            </div>
            <div class="xl:col-span-5">
              <PlanFirstTaskCard
                :model="decisionModel.firstTask"
                @enter="onStartStep"
              />
            </div>
          </div>
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
import SecondaryButton from '@/components/ui/SecondaryButton.vue'
import LoadingState from '@/components/ui/LoadingState.vue'
import ErrorState from '@/components/ui/ErrorState.vue'
import PlanDecisionHero from '@/components/plan/decision/PlanDecisionHero.vue'
import PlanWhyAccordion from '@/components/plan/decision/PlanWhyAccordion.vue'
import PlanFirstTaskCard from '@/components/plan/decision/PlanFirstTaskCard.vue'
import PlanStageRail from '@/components/plan/decision/PlanStageRail.vue'
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
const whyOpen = ref(false)

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
  if (store.currentTaskId) {
    router.push({ name: 'taskRun', params: { taskId: store.currentTaskId } })
    return
  }
  router.push({ name: 'execution' })
}

async function onStartStep() {
  if (store.sessionId && store.currentTaskId) {
    goExecution()
    return
  }
  const activePlanId = store.planId ?? plan.value?.planId ?? null
  if (!activePlanId) return
  committing.value = true
  try {
    if (store.sessionId && !store.currentTaskId) {
      store.clearRunState()
      store.planId = activePlanId
      if (plan.value) {
        store.planPreview = plan.value
      }
    }
    const data = await commitPlan(activePlanId)
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
