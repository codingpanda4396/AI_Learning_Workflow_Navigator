<template>
  <PageContainer>
    <AppTopBar current="plan" />
    <main class="mx-auto max-w-2xl px-6 py-8">
      <LoadingState v-if="loading && !plan" message="正在生成计划..." />
      <ErrorState v-else-if="error" :message="error">
        <template #action>
          <SecondaryButton @click="fetchPlan">重试</SecondaryButton>
        </template>
      </ErrorState>

      <WorkflowPageScaffold v-else-if="plan && presentation">
        <template #title>
          <h1 class="text-2xl font-bold text-text-primary md:text-3xl">
            你的第一步学习动作
          </h1>
          <p class="mt-2 text-text-secondary">
            系统已根据你的目标与诊断整理好起点；点下方按钮直接进入带练任务。
          </p>
        </template>

        <template #primary>
          <CurrentActionHero
            :title="presentation.firstActionTitle"
            eyebrow="你现在要做的第一步"
            :minutes="presentation.estimatedMinutes"
            :cta-label="presentation.commitCtaLabel"
            :loading="committing"
            @action="onCommit"
          />
          <div class="mt-5 space-y-4">
            <WhyThisStepCard :text="presentation.whyOneLiner" />
            <NextFlowMiniSteps :lines="presentation.nextThreeLines" />
          </div>
        </template>

        <template #secondary>
          <details class="group rounded-card border border-border bg-white">
            <summary
              class="cursor-pointer list-none p-4 text-sm font-medium text-text-primary marker:hidden [&::-webkit-details-marker]:hidden"
            >
              <span class="flex items-center justify-between gap-2">
                查看计划摘要（可选）
                <span class="text-xs font-normal text-text-secondary group-open:hidden">
                  展开
                </span>
                <span class="hidden text-xs font-normal text-text-secondary group-open:inline">
                  收起
                </span>
              </span>
            </summary>
            <div class="space-y-4 border-t border-border p-4 pt-0 text-sm text-text-secondary">
              <div v-if="plan.recommendedStrategy">
                <p class="font-medium text-text-primary">策略</p>
                <p class="mt-1">
                  {{ plan.recommendedStrategy.label ?? plan.recommendedStrategy.code }}
                </p>
              </div>
              <div v-if="plan.tasks?.length">
                <p class="font-medium text-text-primary">任务顺序</p>
                <ol class="mt-1 list-decimal space-y-1 pl-5">
                  <li v-for="t in plan.tasks" :key="t.taskId">{{ t.title }}</li>
                </ol>
              </div>
              <div v-if="plan.successCriteria?.length">
                <p class="font-medium text-text-primary">成功标准</p>
                <ul class="mt-1 list-disc space-y-1 pl-5">
                  <li v-for="(c, i) in plan.successCriteria" :key="i">{{ c }}</li>
                </ul>
              </div>
              <div v-if="plan.risks?.length">
                <p class="font-medium text-amber-800">提示</p>
                <ul class="mt-1 list-disc space-y-1 pl-5 text-amber-900">
                  <li v-for="(r, i) in plan.risks" :key="i">{{ r }}</li>
                </ul>
              </div>
            </div>
          </details>
        </template>
      </WorkflowPageScaffold>
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
import WorkflowPageScaffold from '@/components/workflow/WorkflowPageScaffold.vue'
import CurrentActionHero from '@/components/workflow/CurrentActionHero.vue'
import WhyThisStepCard from '@/components/workflow/WhyThisStepCard.vue'
import NextFlowMiniSteps from '@/components/workflow/NextFlowMiniSteps.vue'
import { useWorkflowStore } from '@/stores/workflow'
import { previewPlan, commitPlan } from '@/api/learning-plan'
import { showToast } from '@/stores/toast'
import { getErrorMessage } from '@/api/request'
import { buildPlanPresentation } from '@/utils/planPresentationModel'
import type { PlanPreviewData } from '@/types/dto'

const router = useRouter()
const store = useWorkflowStore()

const loading = ref(true)
const committing = ref(false)
const error = ref<string | null>(null)
const plan = ref<PlanPreviewData | null>(null)

const presentation = computed(() => buildPlanPresentation(plan.value))

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

async function onCommit() {
  if (!store.planId) return
  committing.value = true
  try {
    const data = await commitPlan(store.planId)
    store.sessionId = data.sessionId
    store.currentTaskId = data.currentTaskId
    store.taskSequence = data.taskSequence ?? []
    router.push('/task')
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
