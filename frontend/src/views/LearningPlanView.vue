<template>
  <PageContainer>
    <div class="min-h-screen bg-[radial-gradient(circle_at_top,#dbeafe_0%,#f8fafc_32%,#f8fafc_100%)]">
      <AppTopBar current="plan" />
      <main class="mx-auto flex w-full max-w-3xl flex-col px-4 pb-10 pt-4 md:max-w-4xl md:px-6 lg:px-8">
        <LoadingState v-if="loading && !plan" message="正在生成你的学习路径…" />
        <ErrorState v-else-if="error" :message="error">
          <template #action>
            <SecondaryButton @click="fetchPlan">重新加载规划</SecondaryButton>
          </template>
        </ErrorState>

        <div
          v-else-if="plan && battleMap"
          class="space-y-6"
        >
          <PlanRecommendationCard
            :knowledge="battleMap.strategyOverview.currentKnowledge"
            :recommended-start="battleMap.recommendedStartPhrase"
            :why-line="battleMap.whyShortLine"
            :total-time="battleMap.totalEstimatedLabel"
            :loading="committing"
            @start="onStartStep"
          />

          <PlanScaffoldPreview
            :expanded-stage-code="battleMap.expandedStageCode"
          />

          <PlanPhasePathStrip
            :items="battleMap.pathStrip"
            :expanded-stage-code="battleMap.expandedStageCode"
          />

          <PlanCurrentPhaseTasks
            :groups="battleMap.taskGroupsByStage"
            :expanded-stage-code="battleMap.expandedStageCode"
            :loading="committing"
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
import PlanRecommendationCard from '@/components/plan/PlanRecommendationCard.vue'
import PlanScaffoldPreview from '@/components/plan/PlanScaffoldPreview.vue'
import PlanPhasePathStrip from '@/components/plan/PlanPhasePathStrip.vue'
import PlanCurrentPhaseTasks from '@/components/plan/PlanCurrentPhaseTasks.vue'
import { useWorkflowStore } from '@/stores/workflow'
import { previewPlan, commitPlan } from '@/api/learning-plan'
import { showToast } from '@/stores/toast'
import { getErrorMessage } from '@/api/request'
import { buildPlanBattleMapView } from '@/utils/planPresentationModel'
import { clearDiagnosisRecapSessionFlag } from '@/utils/diagnosisRecapCopy'
import type { PlanPreviewData } from '@/types/dto'

const router = useRouter()
const store = useWorkflowStore()

const loading = ref(true)
const committing = ref(false)
const error = ref<string | null>(null)
const plan = ref<PlanPreviewData | null>(null)

const battleMap = computed(() =>
  buildPlanBattleMapView(plan.value, {
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
