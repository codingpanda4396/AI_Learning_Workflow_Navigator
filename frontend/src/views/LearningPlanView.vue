<template>
  <PageContainer>
    <AppTopBar current="plan" />
    <main class="mx-auto max-w-3xl px-6 py-8">
      <div class="mb-8">
        <h1 class="text-2xl font-bold text-text-primary md:text-3xl">
          学习计划预览
        </h1>
        <p class="mt-2 text-text-secondary">
          基于你的目标与诊断结果，系统为你生成了以下学习计划。
        </p>
      </div>

      <LoadingState v-if="loading && !plan" message="正在生成计划..." />
      <ErrorState v-else-if="error" :message="error">
        <template #action>
          <SecondaryButton @click="fetchPlan">重试</SecondaryButton>
        </template>
      </ErrorState>

      <div v-else-if="plan" class="space-y-6">
        <FormCard>
          <SectionHeader>推荐入口</SectionHeader>
          <p class="text-lg font-medium text-text-primary">
            {{ plan.recommendedEntry?.title }}
          </p>
          <p
            v-if="plan.recommendedEntry?.reason"
            class="mt-2 text-sm text-text-secondary"
          >
            {{ plan.recommendedEntry.reason }}
          </p>
        </FormCard>

        <FormCard>
          <SectionHeader>推荐策略</SectionHeader>
          <p class="font-medium text-text-primary">
            {{ plan.recommendedStrategy?.label ?? plan.recommendedStrategy?.code }}
          </p>
          <p
            v-if="plan.recommendedStrategy?.reason"
            class="mt-2 text-sm text-text-secondary"
          >
            {{ plan.recommendedStrategy.reason }}
          </p>
        </FormCard>

        <FormCard v-if="plan.stages?.length">
          <SectionHeader>阶段安排</SectionHeader>
          <ul class="space-y-3">
            <li
              v-for="s in plan.stages"
              :key="s.stageCode"
              class="flex items-baseline gap-2"
            >
              <span class="font-medium text-primary">{{ s.title }}</span>
              <span v-if="s.objective" class="text-sm text-text-secondary">
                — {{ s.objective }}
              </span>
            </li>
          </ul>
        </FormCard>

        <FormCard v-if="plan.tasks?.length">
          <SectionHeader>任务列表</SectionHeader>
          <ol class="list-decimal space-y-2 pl-5">
            <li
              v-for="t in plan.tasks"
              :key="t.taskId"
              class="text-text-primary"
            >
              {{ t.title }}
            </li>
          </ol>
        </FormCard>

        <FormCard v-if="plan.successCriteria?.length">
          <SectionHeader>成功标准</SectionHeader>
          <ul class="list-disc space-y-1 pl-5 text-text-secondary">
            <li v-for="(c, i) in plan.successCriteria" :key="i">{{ c }}</li>
          </ul>
        </FormCard>

        <FormCard v-if="plan.risks?.length">
          <SectionHeader>风险提示</SectionHeader>
          <ul class="list-disc space-y-1 pl-5 text-amber-700">
            <li v-for="(r, i) in plan.risks" :key="i">{{ r }}</li>
          </ul>
        </FormCard>

        <div class="flex justify-end">
          <PrimaryButton :loading="committing" @click="onCommit">
            确认并开始执行
          </PrimaryButton>
        </div>
      </div>
    </main>
  </PageContainer>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import PageContainer from '@/components/layout/PageContainer.vue'
import AppTopBar from '@/components/layout/AppTopBar.vue'
import FormCard from '@/components/ui/FormCard.vue'
import PrimaryButton from '@/components/ui/PrimaryButton.vue'
import SecondaryButton from '@/components/ui/SecondaryButton.vue'
import SectionHeader from '@/components/common/SectionHeader.vue'
import LoadingState from '@/components/ui/LoadingState.vue'
import ErrorState from '@/components/ui/ErrorState.vue'
import { useWorkflowStore } from '@/stores/workflow'
import { previewPlan, commitPlan } from '@/api/learning-plan'
import { showToast } from '@/stores/toast'
import { getErrorMessage } from '@/api/request'
import type { PlanPreviewData } from '@/types/dto'

const router = useRouter()
const store = useWorkflowStore()

const loading = ref(true)
const committing = ref(false)
const error = ref<string | null>(null)
const plan = ref<PlanPreviewData | null>(null)

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
