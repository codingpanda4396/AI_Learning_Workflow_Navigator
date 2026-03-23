<template>
  <PageContainer>
    <div class="flex min-h-screen flex-col">
      <AppTopBar current="plan" />
      <main
        class="mx-auto flex w-full max-w-lg flex-1 flex-col px-5 pb-6 pt-4 min-h-0"
      >
        <LoadingState v-if="loading && !plan" message="加载中…" />
        <ErrorState v-else-if="error" :message="error">
          <template #action>
            <SecondaryButton @click="fetchPlan">重试</SecondaryButton>
          </template>
        </ErrorState>

        <div
          v-else-if="plan && viewModel && pathSegmentLabels.length"
          class="flex flex-1 flex-col min-h-0 gap-6"
        >
          <div class="min-h-0 flex-1 space-y-5 overflow-y-auto">
            <section
              class="rounded-card border border-border bg-white px-4 py-4 shadow-sm"
            >
              <p class="text-sm leading-snug text-text-primary">
                <span class="font-semibold text-text-primary">👉 你的问题：</span>
                <span>{{ problemLine }}</span>
              </p>
              <p
                class="mt-3 text-sm leading-snug text-text-primary"
              >
                <span class="font-semibold text-text-primary">👉 当前策略：</span>
                <span>{{ strategyLine }}</span>
              </p>
            </section>

            <section class="rounded-card border border-border bg-slate-50/90 px-4 py-4">
              <p class="text-xs font-medium tracking-wide text-text-secondary">
                学习路径
              </p>
              <div
                class="mt-3 flex flex-wrap items-center gap-x-1 gap-y-2 text-sm"
                role="list"
              >
                <template v-for="(label, idx) in pathSegmentLabels" :key="idx">
                  <span
                    v-if="idx > 0"
                    class="px-0.5 text-text-secondary"
                    aria-hidden="true"
                  >→</span>
                  <span
                    role="listitem"
                    class="rounded-md px-2 py-1"
                    :class="
                      idx === currentPathIndex
                        ? 'bg-primary/10 font-semibold text-primary'
                        : 'text-text-secondary'
                    "
                  >
                    {{ label }}
                  </span>
                </template>
              </div>
              <p class="mt-3 text-sm font-medium text-text-primary">
                <span aria-hidden="true">👉</span>
                当前：{{ currentPhaseDisplayName }}
              </p>
            </section>
          </div>

          <div class="shrink-0 border-t border-border/60 pt-4">
            <PrimaryButton
              class="w-full justify-center py-4 text-base font-semibold shadow-md"
              :loading="committing"
              @click="onStartStep"
            >
              开始第1步
            </PrimaryButton>
          </div>
        </div>
      </main>
    </div>
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
import { useWorkflowStore } from '@/stores/workflow'
import { previewPlan, commitPlan } from '@/api/learning-plan'
import { showToast } from '@/stores/toast'
import { getErrorMessage } from '@/api/request'
import {
  buildPlanViewModel,
  planStepsToFlowItems,
} from '@/utils/planPresentationModel'
import {
  buildPlanProblemOneLiner,
  clearDiagnosisRecapSessionFlag,
} from '@/utils/diagnosisRecapCopy'
import type { PlanPreviewData } from '@/types/dto'

const PATH_FOUR_LABELS = ['结构认知', '基本操作', '遍历', '应用'] as const

function truncateText(s: string, max: number): string {
  const t = s.trim()
  if (t.length <= max) return t
  return t.slice(0, max) + '…'
}

const router = useRouter()
const store = useWorkflowStore()

const loading = ref(true)
const committing = ref(false)
const error = ref<string | null>(null)
const plan = ref<PlanPreviewData | null>(null)

const problemLine = computed(() =>
  buildPlanProblemOneLiner(store.learnerProfileSnapshot)
)

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

const pathSegmentLabels = computed(() => {
  const items = flowItems.value
  if (items.length === 4) return [...PATH_FOUR_LABELS]
  return items.map((i) => i.title)
})

const currentPathIndex = computed(() => {
  const vm = viewModel.value
  const n = pathSegmentLabels.value.length
  if (!vm || n === 0) return 0
  return Math.min(Math.max(vm.currentStepIndex, 0), n - 1)
})

const currentPhaseDisplayName = computed(() => {
  const labels = pathSegmentLabels.value
  const i = currentPathIndex.value
  return labels[i] ?? ''
})

const strategyLine = computed(() => {
  const p = plan.value
  const vm = viewModel.value
  if (!p || !vm) return '从第 1 步开始'
  const label = p.recommendedStrategy?.label?.trim()
  if (label) return truncateText(label, 36)
  const sub = vm.showcase?.hero?.subtitle?.trim()
  if (sub) return truncateText(sub, 36)
  const path = vm.pathSummaryLine?.trim()
  if (path) return truncateText(path, 36)
  return '从第 1 步开始'
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
  clearDiagnosisRecapSessionFlag()
  if (store.planPreview && !plan.value) plan.value = store.planPreview
  if (!plan.value) fetchPlan()
})
</script>
