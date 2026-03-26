<template>
  <PageContainer>
    <AppTopBar current="report" />
    <main class="mx-auto max-w-3xl px-6 py-8">
      <div class="mb-8">
        <h1 class="text-2xl font-bold text-text-primary md:text-3xl">
          {{ REPORT_COPY.title }}
        </h1>
        <p class="mt-2 text-text-secondary">
          {{ REPORT_COPY.subtitle }}
        </p>
      </div>

      <LoadingState v-if="loading && !report" :message="REPORT_COPY.loading" />
      <ErrorState v-else-if="error" :message="error">
        <template #action>
          <SecondaryButton @click="fetchReport">{{ REPORT_COPY.retry }}</SecondaryButton>
        </template>
      </ErrorState>

      <div v-else-if="report" class="space-y-6">
        <FormCard>
          <SectionHeader>{{ REPORT_COPY.result }}</SectionHeader>
          <StatusBadge
            :label="resultStatusLabels[report.resultStatus] ?? report.resultStatus"
            :variant="report.resultStatus === 'ACHIEVED' ? 'success' : report.resultStatus === 'NOT_ACHIEVED' ? 'error' : 'warning'"
          />
        </FormCard>

        <FormCard v-if="report.goalReview">
          <SectionHeader>{{ REPORT_COPY.goalReview }}</SectionHeader>
          <p class="text-text-primary">{{ report.goalReview }}</p>
        </FormCard>

        <FormCard v-if="report.completedProgress?.length">
          <SectionHeader>{{ REPORT_COPY.completedProgress }}</SectionHeader>
          <ul class="list-disc space-y-1 pl-5 text-text-secondary">
            <li v-for="(p, i) in report.completedProgress" :key="i">{{ p }}</li>
          </ul>
        </FormCard>

        <FormCard v-if="report.unresolvedIssues?.length">
          <SectionHeader>{{ REPORT_COPY.unresolvedIssues }}</SectionHeader>
          <ul class="list-disc space-y-1 pl-5 text-amber-700">
            <li v-for="(u, i) in report.unresolvedIssues" :key="i">{{ u }}</li>
          </ul>
        </FormCard>

        <FormCard v-if="report.evidenceSummary?.length">
          <SectionHeader>{{ REPORT_COPY.evidenceSummary }}</SectionHeader>
          <ul class="list-disc space-y-1 pl-5 text-text-secondary">
            <li v-for="(e, i) in report.evidenceSummary" :key="i">{{ e }}</li>
          </ul>
        </FormCard>

        <FormCard v-if="report.summaryText">
          <SectionHeader>{{ REPORT_COPY.summarySection }}</SectionHeader>
          <p class="text-text-primary">{{ report.summaryText }}</p>
        </FormCard>

        <FormCard
          v-if="report.learningMethodProfile && report.learningMethodProfile.questioningQuality !== 'UNKNOWN'"
        >
          <SectionHeader>{{ REPORT_COPY.learningMethod }}</SectionHeader>
          <p class="mb-3 text-sm text-text-secondary">
            {{ REPORT_COPY.learningMethodHint }}
          </p>
          <ul class="space-y-2 text-sm text-text-primary">
            <li>
              <span class="text-text-secondary">{{ REPORT_COPY.methodQuestioning }}</span>
              {{ methodQualityLabel(report.learningMethodProfile.questioningQuality) }}
            </li>
            <li>
              <span class="text-text-secondary">{{ REPORT_COPY.methodSelfExplain }}</span>
              {{
                report.learningMethodProfile.selfExplanationPerformed
                  ? report.learningMethodProfile.selfExplanationQuality || REPORT_COPY.submitted
                  : REPORT_COPY.notDetected
              }}
            </li>
            <li>
              <span class="text-text-secondary">{{ REPORT_COPY.methodCheck }}</span>
              {{
                report.learningMethodProfile.checkPassed === true
                  ? REPORT_COPY.passed
                  : report.learningMethodProfile.checkPassed === false
                    ? REPORT_COPY.partialFail
                    : '—'
              }}
            </li>
            <li v-if="report.learningMethodProfile.positiveSignals?.length">
              <span class="text-text-secondary">{{ REPORT_COPY.methodPositive }}</span>
              {{ report.learningMethodProfile.positiveSignals.join('；') }}
            </li>
            <li
              v-if="report.learningMethodProfile.antiPatternObserved?.length"
              class="text-amber-800"
            >
              <span class="text-text-secondary">{{ REPORT_COPY.methodAnti }}</span>
              {{ report.learningMethodProfile.antiPatternObserved.join('；') }}
            </li>
            <li v-if="report.learningMethodProfile.nextMethodAdvice?.length">
              <span class="text-text-secondary">{{ REPORT_COPY.methodNextAdvice }}</span>
              {{ report.learningMethodProfile.nextMethodAdvice.join(' ') }}
            </li>
          </ul>
        </FormCard>

        <FormCard v-if="nextAction">
          <SectionHeader>{{ REPORT_COPY.nextActionSection }}</SectionHeader>
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
            {{ REPORT_COPY.replanBanner }}
          </p>
        </FormCard>

        <FormCard>
          <SectionHeader>{{ REPORT_COPY.confirmCardTitle }}</SectionHeader>
          <p class="mb-4 text-sm text-text-secondary">
            {{ REPORT_COPY.confirmCardHint }}
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
              {{ REPORT_COPY.confirmSubmit }}
            </PrimaryButton>
            <SecondaryButton @click="router.push('/goal')">
              {{ REPORT_COPY.restart }}
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
import { REPORT_COPY } from '@/constants/uiCopy'

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

function methodQualityLabel(q?: string) {
  if (q === 'GOOD') return '较好'
  if (q === 'BASIC') return '一般'
  if (q === 'LOW') return '偏少'
  return q ?? '—'
}

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
    nextHint.value = data.nextHint ?? REPORT_COPY.nextHintDefault
    if (data.requiresReplan) {
      showToast(REPORT_COPY.toastReplan)
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


