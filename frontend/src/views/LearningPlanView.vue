<template>
  <PageContainer>
    <div class="min-h-screen bg-[radial-gradient(circle_at_top,#dbeafe_0%,#f8fafc_32%,#f8fafc_100%)]">
      <AppTopBar current="plan" />
      <main class="mx-auto flex w-full max-w-7xl flex-col px-4 pb-10 pt-4 md:px-6 lg:px-8">
        <LoadingState v-if="loading && !plan" message="正在生成你的四阶段作战图..." />
        <ErrorState v-else-if="error" :message="error">
          <template #action>
            <SecondaryButton @click="fetchPlan">重新加载规划</SecondaryButton>
          </template>
        </ErrorState>

        <div
          v-else-if="plan && battleMap"
          class="space-y-8"
        >
          <section class="rounded-[28px] border border-white/70 bg-white/75 p-5 shadow-[0_22px_40px_rgba(15,23,42,0.08)] backdrop-blur-sm md:p-6">
            <div class="flex flex-wrap items-end justify-between gap-4">
              <div>
                <p class="text-xs font-semibold uppercase tracking-[0.26em] text-slate-500">
                  Planning Orchestration
                </p>
                <h1 class="mt-2 text-3xl font-semibold tracking-tight text-text-primary md:text-[42px]">
                  规划不是说明页，而是学习编排台
                </h1>
              </div>
              <div class="rounded-2xl border border-slate-200 bg-slate-50 px-4 py-3 text-sm leading-6 text-text-secondary">
                <p v-if="userStateLine">当前状态：{{ userStateLine }}</p>
                <p class="font-medium text-text-primary">问题焦点：{{ problemLine }}</p>
              </div>
            </div>
          </section>

          <PlanStrategyOverview
            :overview="battleMap.strategyOverview"
            :loading="committing"
            @start="onStartStep"
          />

          <PlanStageMap :cards="battleMap.stageCards" />

          <PlanTaskBoard :groups="battleMap.taskGroupsByStage" />
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
import PlanStageMap from '@/components/plan/PlanStageMap.vue'
import PlanStrategyOverview from '@/components/plan/PlanStrategyOverview.vue'
import PlanTaskBoard from '@/components/plan/PlanTaskBoard.vue'
import { useWorkflowStore } from '@/stores/workflow'
import { previewPlan, commitPlan } from '@/api/learning-plan'
import { showToast } from '@/stores/toast'
import { getErrorMessage } from '@/api/request'
import { buildPlanBattleMapView } from '@/utils/planPresentationModel'
import {
  buildPlanProblemOneLiner,
  buildUserStateSummaryLine,
  clearDiagnosisRecapSessionFlag,
} from '@/utils/diagnosisRecapCopy'
import type { PlanPreviewData } from '@/types/dto'

const router = useRouter()
const store = useWorkflowStore()

const loading = ref(true)
const committing = ref(false)
const error = ref<string | null>(null)
const plan = ref<PlanPreviewData | null>(null)

const problemLine = computed(() =>
  buildPlanProblemOneLiner(store.learnerProfileSnapshot)
)

const userStateLine = computed(() =>
  buildUserStateSummaryLine(store.learnerProfileSnapshot)
)

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
  clearDiagnosisRecapSessionFlag()
  if (store.planPreview && !plan.value) plan.value = store.planPreview
  if (!plan.value) fetchPlan()
  else loading.value = false
})
</script>
