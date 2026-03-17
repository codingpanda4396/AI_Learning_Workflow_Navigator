<template>
  <PageContainer>
    <AppTopBar current="report" />
    <main class="mx-auto max-w-3xl px-6 py-8">
      <div class="mb-8">
        <h1 class="text-2xl font-bold text-text-primary md:text-3xl">
          学习报告
        </h1>
        <p class="mt-2 text-text-secondary">
          本轮学习总结与下一步建议
        </p>
      </div>

      <LoadingState v-if="loading && !report" message="生成报告中..." />
      <ErrorState v-else-if="error" :message="error">
        <template #action>
          <SecondaryButton @click="fetchReport">重试</SecondaryButton>
        </template>
      </ErrorState>

      <div v-else-if="report" class="space-y-6">
        <FormCard>
          <SectionHeader>本轮结果</SectionHeader>
          <StatusBadge
            :label="resultStatusLabels[report.resultStatus] ?? report.resultStatus"
            :variant="report.resultStatus === 'ACHIEVED' ? 'success' : report.resultStatus === 'NOT_ACHIEVED' ? 'error' : 'warning'"
          />
        </FormCard>

        <FormCard v-if="report.goalReview">
          <SectionHeader>目标回顾</SectionHeader>
          <p class="text-text-primary">{{ report.goalReview }}</p>
        </FormCard>

        <FormCard v-if="report.completedProgress?.length">
          <SectionHeader>已完成进展</SectionHeader>
          <ul class="list-disc space-y-1 pl-5 text-text-secondary">
            <li v-for="(p, i) in report.completedProgress" :key="i">{{ p }}</li>
          </ul>
        </FormCard>

        <FormCard v-if="report.unresolvedIssues?.length">
          <SectionHeader>待解决问题</SectionHeader>
          <ul class="list-disc space-y-1 pl-5 text-amber-700">
            <li v-for="(u, i) in report.unresolvedIssues" :key="i">{{ u }}</li>
          </ul>
        </FormCard>

        <FormCard v-if="report.evidenceSummary?.length">
          <SectionHeader>证据摘要</SectionHeader>
          <ul class="list-disc space-y-1 pl-5 text-text-secondary">
            <li v-for="(e, i) in report.evidenceSummary" :key="i">{{ e }}</li>
          </ul>
        </FormCard>

        <FormCard v-if="report.summaryText">
          <SectionHeader>总结</SectionHeader>
          <p class="text-text-primary">{{ report.summaryText }}</p>
        </FormCard>

        <FormCard v-if="nextAction">
          <SectionHeader>下一步建议</SectionHeader>
          <p class="font-medium text-primary">
            {{ nextActionTypeLabels[nextAction.actionType] ?? nextAction.actionType }}
          </p>
          <p v-if="nextAction.reason" class="mt-2 text-sm text-text-secondary">
            {{ nextAction.reason }}
          </p>
          <p
            v-if="nextAction.requiresReplan"
            class="mt-2 text-sm text-amber-700"
          >
            建议重新规划学习路径
          </p>
        </FormCard>

        <FormCard>
          <SectionHeader>确认下一步</SectionHeader>
          <p class="mb-4 text-sm text-text-secondary">
            请选择你打算执行的下一步动作
          </p>
          <div class="flex flex-wrap gap-2">
            <button
              v-for="(label, val) in nextActionTypeLabels"
              :key="val"
              type="button"
              class="rounded-input border px-4 py-2 text-sm font-medium transition-colors"
              :class="
                selectedAction === val
                  ? 'border-primary bg-primary text-white'
                  : 'border-border text-text-primary hover:border-primary/50'
              "
              @click="selectedAction = val as NextActionTypeType"
            >
              {{ label }}
            </button>
          </div>
          <div class="mt-4 flex gap-3">
            <PrimaryButton
              :loading="confirming"
              :disabled="!selectedAction"
              @click="onConfirm"
            >
              确认并继续
            </PrimaryButton>
            <SecondaryButton @click="router.push('/goal')">
              重新开始
            </SecondaryButton>
          </div>
        </FormCard>

        <div
          v-if="nextHint"
          class="rounded-card border border-green-200 bg-green-50 p-4 text-green-800"
        >
          {{ nextHint }}
        </div>
      </div>
    </main>
  </PageContainer>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import PageContainer from '@/components/layout/PageContainer.vue'
import AppTopBar from '@/components/layout/AppTopBar.vue'
import FormCard from '@/components/ui/FormCard.vue'
import PrimaryButton from '@/components/ui/PrimaryButton.vue'
import SecondaryButton from '@/components/ui/SecondaryButton.vue'
import SectionHeader from '@/components/common/SectionHeader.vue'
import StatusBadge from '@/components/ui/StatusBadge.vue'
import LoadingState from '@/components/ui/LoadingState.vue'
import ErrorState from '@/components/ui/ErrorState.vue'
import { useWorkflowStore } from '@/stores/workflow'
import { getReport, confirmNextAction } from '@/api/session'
import { showToast } from '@/stores/toast'
import { getErrorMessage } from '@/api/request'
import {
  resultStatusLabels,
  nextActionTypeLabels,
} from '@/types/labels'
import type { NextActionTypeType } from '@/types/enums'
import type { LearningReport, NextActionDecision } from '@/types/dto'

const router = useRouter()
const store = useWorkflowStore()

const loading = ref(true)
const confirming = ref(false)
const error = ref<string | null>(null)
const report = ref<LearningReport | null>(null)
const nextHint = ref<string>('')
const selectedAction = ref<NextActionTypeType | ''>('')

const nextAction = computed<NextActionDecision | null>(
  () => store.nextActionDecision ?? report.value?.nextAction ?? null
)

async function fetchReport() {
  if (!store.sessionId) return
  loading.value = true
  error.value = null
  try {
    const data = await getReport(store.sessionId)
    store.report = data.learningReport
    store.nextActionDecision = data.nextActionDecision
    report.value = data.learningReport
    if (data.nextActionDecision?.actionType) {
      selectedAction.value = data.nextActionDecision.actionType
    }
  } catch (err) {
    error.value = getErrorMessage(err)
  } finally {
    loading.value = false
  }
}

async function onConfirm() {
  if (!store.sessionId || !selectedAction.value) return
  confirming.value = true
  try {
    const data = await confirmNextAction(store.sessionId, selectedAction.value)
    nextHint.value = data.nextHint ?? '已确认下一步'
    if (data.requiresReplan) {
      showToast('建议重新规划，请从目标输入重新开始')
    }
  } catch (err) {
    showToast(getErrorMessage(err))
  } finally {
    confirming.value = false
  }
}

onMounted(() => {
  if (store.report) report.value = store.report
  if (!report.value) fetchReport()
})
</script>
