<template>
  <PageContainer>
    <AppTopBar current="report" />
    <main class="mx-auto max-w-6xl px-4 py-6 md:px-6 lg:px-8">
      <LoadingState v-if="loading && !report" :message="REPORT_COPY.loading" />
      <ErrorState v-else-if="error" :message="error">
        <template #action>
          <SecondaryButton @click="fetchReport">{{ REPORT_COPY.retry }}</SecondaryButton>
        </template>
      </ErrorState>

      <section v-else-if="report" class="space-y-6">
        <section class="overflow-hidden rounded-[32px] border border-border bg-[radial-gradient(circle_at_top_left,_rgba(30,58,95,0.12),_transparent_40%),linear-gradient(135deg,#ffffff_0%,#e8eef5_45%,#f1f5f9_100%)] shadow-[0_24px_60px_rgba(15,23,42,0.08)]">
          <div class="grid gap-6 px-5 py-6 md:grid-cols-[minmax(0,1.4fr)_320px] md:px-8 md:py-8">
            <div class="space-y-5">
              <div class="flex flex-wrap items-center gap-3">
                <span class="rounded-full bg-primary px-3 py-1 text-xs font-semibold tracking-[0.16em] text-white">
                  {{ REPORT_COPY.heroEyebrow }}
                </span>
                <span
                  class="inline-flex items-center rounded-full px-3 py-1 text-sm font-semibold"
                  :class="heroStatusClass"
                >
                  {{ resultStatusLabels[report.resultStatus] ?? report.resultStatus }}
                </span>
              </div>

              <div class="space-y-3">
                <p class="text-sm font-medium text-slate-500">{{ report.goalReview || REPORT_COPY.goalFallback }}</p>
                <h1 class="max-w-3xl text-3xl font-semibold tracking-tight text-slate-950 md:text-5xl">
                  {{ report.finalSummary || report.summaryText || REPORT_COPY.summaryFallback }}
                </h1>
                <p class="max-w-2xl text-sm leading-7 text-slate-600 md:text-base">
                  {{ recommendedReason }}
                </p>
              </div>

              <div class="flex flex-wrap gap-3">
                <button
                  v-if="recommendedAction"
                  type="button"
                  class="rounded-full bg-accent px-5 py-3 text-sm font-semibold text-white transition hover:bg-accent-hover"
                  @click="selectedAction = recommendedAction.actionType"
                >
                  {{ recommendedAction.actionLabel || REPORT_COPY.primaryCta }}
                </button>
                <SecondaryButton @click="router.push('/goal')">
                  {{ REPORT_COPY.restart }}
                </SecondaryButton>
              </div>
            </div>

            <div class="grid gap-3 sm:grid-cols-3 md:grid-cols-1">
              <article
                v-for="item in heroMetrics"
                :key="item.label"
                class="rounded-[24px] border border-white/70 bg-white/80 p-4 backdrop-blur"
              >
                <p class="text-[11px] font-semibold uppercase tracking-[0.18em] text-slate-400">{{ item.label }}</p>
                <p class="mt-3 text-lg font-semibold text-slate-950">{{ item.value }}</p>
                <p class="mt-2 text-sm leading-6 text-slate-600">{{ item.hint }}</p>
              </article>
            </div>
          </div>
        </section>

        <section
          v-if="growthComparison.show"
          class="overflow-hidden rounded-[28px] border border-border bg-white p-5 shadow-card md:p-7"
        >
          <p class="text-xs font-semibold uppercase tracking-[0.16em] text-primary">{{ REPORT_COPY.growthEyebrow }}</p>
          <h2 class="mt-2 text-2xl font-semibold tracking-tight text-text-primary">{{ REPORT_COPY.growthTitle }}</h2>
          <p class="mt-2 max-w-2xl text-sm leading-relaxed text-text-secondary">{{ REPORT_COPY.growthSubtitle }}</p>
          <div class="mt-6 grid gap-4 md:grid-cols-2">
            <article class="rounded-2xl border border-border bg-primary-muted/50 p-5">
              <p class="text-[11px] font-semibold uppercase tracking-[0.14em] text-text-muted">{{ REPORT_COPY.growthBeforeLabel }}</p>
              <p class="mt-3 text-sm leading-7 text-text-primary">{{ growthComparison.before }}</p>
            </article>
            <article
              class="rounded-2xl border border-emerald-200/80 bg-emerald-50/60 p-5 shadow-sm ring-1 ring-emerald-100/80"
            >
              <p class="text-[11px] font-semibold uppercase tracking-[0.14em] text-emerald-800">{{ REPORT_COPY.growthAfterLabel }}</p>
              <ul class="mt-3 space-y-2 text-sm leading-7 text-emerald-950">
                <li v-for="(line, i) in growthComparison.afterLines" :key="i" class="flex gap-2">
                  <span class="mt-2 h-1.5 w-1.5 shrink-0 rounded-full bg-emerald-500" />
                  <span>{{ line }}</span>
                </li>
              </ul>
            </article>
          </div>
        </section>

        <section class="grid gap-6 xl:grid-cols-[minmax(0,1.2fr)_minmax(300px,0.8fr)]">
          <div class="space-y-6">
            <article v-if="taskHighlights.length" class="rounded-[28px] border border-slate-200 bg-white p-5 shadow-sm md:p-6">
              <div class="flex items-center justify-between gap-4">
                <div>
                  <p class="text-xs font-semibold uppercase tracking-[0.16em] text-sky-600">{{ REPORT_COPY.learnedEyebrow }}</p>
                  <h2 class="mt-2 text-2xl font-semibold tracking-tight text-slate-950">{{ REPORT_COPY.learnedTitle }}</h2>
                </div>
                <span class="rounded-full bg-sky-50 px-3 py-1 text-sm font-medium text-sky-700">
                  {{ learnedPoints.length }} {{ REPORT_COPY.learnedCountSuffix }}
                </span>
              </div>

              <div class="mt-5 grid gap-3 md:grid-cols-2">
                <article
                  v-for="(item, index) in learnedPoints"
                  :key="item + index"
                  class="rounded-[22px] border border-sky-100 bg-sky-50/70 p-4"
                >
                  <p class="text-[11px] font-semibold uppercase tracking-[0.16em] text-sky-700">
                    {{ REPORT_COPY.learnedCardTag }} {{ index + 1 }}
                  </p>
                  <p class="mt-3 text-sm leading-7 text-slate-800">{{ item }}</p>
                </article>
              </div>
            </article>

            <article class="rounded-[28px] border border-slate-200 bg-white p-5 shadow-sm md:p-6">
              <p class="text-xs font-semibold uppercase tracking-[0.16em] text-amber-600">{{ REPORT_COPY.gapEyebrow }}</p>
              <h2 class="mt-2 text-2xl font-semibold tracking-tight text-slate-950">{{ REPORT_COPY.gapTitle }}</h2>
              <div class="mt-5 space-y-3">
                <div
                  v-for="(item, index) in remainingGaps"
                  :key="item + index"
                  class="rounded-[22px] border border-amber-100 bg-amber-50/80 p-4"
                >
                  <p class="text-sm leading-7 text-slate-800">{{ item }}</p>
                </div>
              </div>
            </article>

            <article class="rounded-[28px] border border-slate-200 bg-white p-5 shadow-sm md:p-6">
              <p class="text-xs font-semibold uppercase tracking-[0.16em] text-slate-500">{{ REPORT_COPY.timelineEyebrow }}</p>
              <h2 class="mt-2 text-2xl font-semibold tracking-tight text-slate-950">{{ REPORT_COPY.timelineTitle }}</h2>
              <div class="mt-5 space-y-3">
                <article
                  v-for="item in taskHighlights"
                  :key="item.taskId"
                  class="rounded-[22px] border border-slate-200 bg-slate-50 p-4"
                >
                  <div class="flex flex-wrap items-center justify-between gap-3">
                    <div>
                      <p class="text-base font-semibold text-slate-950">{{ item.title }}</p>
                      <p class="mt-1 text-sm text-slate-500">{{ taskCompletionStatusLabels[item.completionStatus] ?? item.completionStatus }}</p>
                    </div>
                    <span class="rounded-full bg-white px-3 py-1 text-xs font-semibold tracking-[0.14em] text-slate-500">
                      {{ item.taskId }}
                    </span>
                  </div>
                  <p class="mt-3 text-sm leading-7 text-slate-700">{{ item.learned }}</p>
                  <p v-if="item.issue" class="mt-2 text-sm leading-7 text-amber-700">{{ item.issue }}</p>
                </article>
              </div>
            </article>
          </div>

          <div class="space-y-6">
            <article class="rounded-[28px] border border-slate-200 bg-white p-5 shadow-sm md:p-6">
              <p class="text-xs font-semibold uppercase tracking-[0.16em] text-emerald-600">{{ REPORT_COPY.methodEyebrow }}</p>
              <h2 class="mt-2 text-2xl font-semibold tracking-tight text-slate-950">{{ REPORT_COPY.methodTitle }}</h2>
              <p class="mt-3 text-sm leading-7 text-slate-600">{{ methodReview.summary }}</p>

              <div class="mt-5 grid gap-3">
                <article class="rounded-[22px] border border-emerald-100 bg-emerald-50/70 p-4">
                  <p class="text-[11px] font-semibold uppercase tracking-[0.16em] text-emerald-700">{{ REPORT_COPY.methodStrengths }}</p>
                  <div class="mt-3 flex flex-wrap gap-2">
                    <span
                      v-for="item in methodReview.strengths"
                      :key="item"
                      class="rounded-full bg-white px-3 py-1 text-sm text-slate-700"
                    >
                      {{ item }}
                    </span>
                  </div>
                </article>

                <article
                  v-if="methodReview.risks.length"
                  class="rounded-[22px] border border-amber-100 bg-amber-50/80 p-4"
                >
                  <p class="text-[11px] font-semibold uppercase tracking-[0.16em] text-amber-700">{{ REPORT_COPY.methodRisks }}</p>
                  <div class="mt-3 space-y-2">
                    <p v-for="item in methodReview.risks" :key="item" class="text-sm leading-7 text-slate-700">{{ item }}</p>
                  </div>
                </article>

                <article class="rounded-[22px] border border-slate-200 bg-slate-50 p-4">
                  <p class="text-[11px] font-semibold uppercase tracking-[0.16em] text-slate-500">{{ REPORT_COPY.methodNext }}</p>
                  <div class="mt-3 space-y-2">
                    <p v-for="item in methodReview.nextFocus" :key="item" class="text-sm leading-7 text-slate-700">{{ item }}</p>
                  </div>
                </article>
              </div>
            </article>

            <article class="rounded-[28px] border border-slate-200 bg-white p-5 shadow-sm md:p-6">
              <p class="text-xs font-semibold uppercase tracking-[0.16em] text-violet-600">{{ REPORT_COPY.evidenceEyebrow }}</p>
              <h2 class="mt-2 text-2xl font-semibold tracking-tight text-slate-950">{{ REPORT_COPY.evidenceTitle }}</h2>
              <div class="mt-5 space-y-3">
                <div
                  v-for="(item, index) in evidenceDigest"
                  :key="item + index"
                  class="rounded-[20px] border border-slate-200 bg-slate-50 px-4 py-3"
                >
                  <p class="text-sm leading-7 text-slate-700">{{ item }}</p>
                </div>
              </div>
            </article>

            <article class="rounded-[28px] border border-slate-950 bg-slate-950 p-5 text-white shadow-sm md:p-6">
              <p class="text-xs font-semibold uppercase tracking-[0.16em] text-slate-300">{{ REPORT_COPY.nextEyebrow }}</p>
              <h2 class="mt-2 text-2xl font-semibold tracking-tight">{{ recommendedAction?.title || REPORT_COPY.nextTitle }}</h2>
              <p class="mt-3 text-sm leading-7 text-slate-300">{{ recommendedReason }}</p>
              <p v-if="recommendedAction?.nextEntryPoint" class="mt-3 text-sm leading-7 text-white/90">
                {{ recommendedAction?.nextEntryPoint }}
              </p>
              <p
                v-if="recommendedAction?.requiresReplan"
                class="mt-4 rounded-[18px] border border-white/15 bg-white/10 px-4 py-3 text-sm text-amber-100"
              >
                {{ REPORT_COPY.replanBanner }}
              </p>

              <div class="mt-5 flex flex-wrap gap-2">
                <button
                  v-for="(label, val) in nextActionTypeLabels"
                  :key="val"
                  type="button"
                  class="rounded-full border px-4 py-2 text-sm font-medium transition"
                  :class="
                    selectedAction === val
                      ? 'border-white bg-white text-slate-950'
                      : 'border-white/20 bg-white/5 text-white/82 hover:border-white/40'
                  "
                  @click="selectedAction = val as NextActionTypeType"
                >
                  {{ label }}
                </button>
              </div>

              <div class="mt-5 flex flex-wrap gap-3">
                <PrimaryButton
                  :loading="confirming"
                  :disabled="!selectedAction"
                  @click="onConfirm"
                >
                  {{ REPORT_COPY.confirmSubmit }}
                </PrimaryButton>
                <SecondaryButton class="border-white/20 bg-white/5 text-white hover:bg-white/10" @click="router.push('/goal')">
                  {{ REPORT_COPY.restart }}
                </SecondaryButton>
              </div>

              <div
                v-if="nextHint"
                class="mt-5 rounded-[20px] border border-emerald-300/20 bg-emerald-400/10 px-4 py-4 text-sm leading-7 text-emerald-50"
              >
                {{ nextHint }}
              </div>
            </article>
          </div>
        </section>
      </section>
    </main>
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
import { useWorkflowStore } from '@/stores/workflow'
import { getReport, confirmNextAction } from '@/api/session'
import { showToast } from '@/stores/toast'
import { getErrorMessage } from '@/api/request'
import { resultStatusLabels, nextActionTypeLabels, taskCompletionStatusLabels } from '@/types/labels'
import type { LearningMethodReview, LearningReport, RecommendedNextStep, TaskHighlight } from '@/types/dto'
import type { NextActionTypeType } from '@/types/enums'

const REPORT_COPY = {
  heroEyebrow: '最终报告',
  loading: '正在整理结果…',
  retry: '重试',
  restart: '重新开始',
  goalFallback: '本轮目标已完成执行回收',
  summaryFallback: '这轮学习已经结束，现在进入结果回顾与下一步判断。',
  primaryCta: '采用系统建议',
  learnedEyebrow: '本轮带走',
  learnedTitle: '你已经学会了什么',
  learnedCountSuffix: '个收获',
  learnedCardTag: '带走',
  learnedFallback: '本轮已经形成了一次完整的学习闭环。',
  gapEyebrow: '仍待补稳',
  gapTitle: '还需要补什么',
  gapFallback: '当前没有显著薄弱点，下一步可以开始轻量应用。',
  timelineEyebrow: '执行回顾',
  timelineTitle: '这轮任务怎么走完的',
  methodEyebrow: '学习方式',
  methodTitle: '这轮你是怎么学的',
  methodStrengths: '做得比较好的地方',
  methodRisks: '还要注意的地方',
  methodNext: '下一轮重点',
  methodFallback: '这轮已经形成基本推进节奏，下一轮继续保留自我解释和检查动作。',
  methodNextFallback: '下一轮继续先说自己的理解，再用反馈校正。',
  evidenceEyebrow: '结论依据',
  evidenceTitle: '系统为什么这样判断',
  evidenceFallback: '已经记录到本轮的关键执行证据。',
  nextEyebrow: '下一步',
  nextTitle: '接下来怎么走',
  nextReasonFallback: '系统会根据这轮执行结果，给出下一步建议。',
  replanBanner: '建议重新规划学习路径',
  confirmSubmit: '确认下一步',
  nextHintDefault: '已确认下一步',
  toastReplan: '建议重新规划，请从目标输入重新开始',
  metric1Label: '收获',
  metric1Hint: '本轮至少沉淀出这些可带走的判断点。',
  metric2Label: '执行证据',
  metric2Hint: '不是主观印象，而是根据真实执行记录生成。',
  metric2Fallback: '已完成结果回收',
  metric3Label: '系统建议',
  metric3Hint: '报告页不是终点，还会直接接住下一步。',
  metric3Fallback: '继续判断下一步',
  metricLearnedUnit: '条结果',
  growthEyebrow: '成长证据',
  growthTitle: '学习前后对比',
  growthSubtitle: '左侧为诊断时的起点信号，右侧为本轮沉淀的收获，用于呈现能力提升而非流水账总结。',
  growthBeforeLabel: '学习前起点',
  growthAfterLabel: '学习后带走',
  growthBeforeFallback: '诊断阶段已记录你的起点状态；完成本轮任务后，右侧会沉淀可带走的收获。',
} as const

const router = useRouter()
const store = useWorkflowStore()

const loading = ref(true)
const confirming = ref(false)
const error = ref<string | null>(null)
const report = ref<LearningReport | null>(null)
const nextHint = ref('')
const selectedAction = ref<NextActionTypeType | ''>('')

const recommendedAction = computed<RecommendedNextStep | null>(() => {
  if (report.value?.recommendedNextStep) {
    return report.value.recommendedNextStep
  }
  const legacy = store.nextActionDecision ?? report.value?.nextAction
  if (!legacy) return null
  return {
    actionType: legacy.actionType,
    reason: legacy.reason,
    nextEntryPoint: legacy.nextEntryPoint,
    signals: legacy.adjustmentSignals,
    requiresReplan: legacy.requiresReplan,
  }
})

const recommendedReason = computed(() => {
  return recommendedAction.value?.reason || REPORT_COPY.nextReasonFallback
})

const learnedPoints = computed(() => {
  return report.value?.whatYouLearned?.length
    ? report.value.whatYouLearned
    : report.value?.completedProgress?.length
      ? report.value.completedProgress
      : [REPORT_COPY.learnedFallback]
})

const growthComparison = computed(() => {
  if (!report.value) {
    return { show: false, before: '', afterLines: [] as string[] }
  }
  const ev = store.diagnosisEvidenceSummary
  const profile = store.learnerProfileSnapshot
  let before: string = REPORT_COPY.growthBeforeFallback
  if (ev?.summary?.trim()) {
    before = ev.summary.trim()
  } else if (ev?.keyEvidence?.length) {
    before = ev.keyEvidence.slice(0, 3).join('；')
  } else if (profile?.blockingPoint) {
    before = `卡点信号：${profile.blockingPoint}`
  } else if (profile?.foundationLevel) {
    before = `基础自评：${profile.foundationLevel}`
  }

  const rawAfter = learnedPoints.value.filter((s) => s && s !== REPORT_COPY.learnedFallback).slice(0, 5)
  const afterLines = rawAfter.length ? rawAfter : [REPORT_COPY.learnedFallback]
  return {
    show: true,
    before,
    afterLines,
  }
})

const remainingGaps = computed(() => {
  return report.value?.whatStillNeedsWork?.length
    ? report.value.whatStillNeedsWork
    : report.value?.unresolvedIssues?.length
      ? report.value.unresolvedIssues
      : [REPORT_COPY.gapFallback]
})

const evidenceDigest = computed(() => {
  return report.value?.evidenceDigest?.length
    ? report.value.evidenceDigest
    : report.value?.evidenceSummary?.length
      ? report.value.evidenceSummary
      : [REPORT_COPY.evidenceFallback]
})

const taskHighlights = computed<TaskHighlight[]>(() => {
  return report.value?.taskHighlights?.length
    ? report.value.taskHighlights
    : []
})

const methodReview = computed<Required<LearningMethodReview>>(() => {
  const source = report.value?.learningMethodReview
  return {
    headline: source?.headline ?? '',
    summary: source?.summary ?? REPORT_COPY.methodFallback,
    strengths: source?.strengths ?? [],
    risks: source?.risks ?? [],
    nextFocus: source?.nextFocus ?? [REPORT_COPY.methodNextFallback],
  }
})

const heroStatusClass = computed(() => {
  if (report.value?.resultStatus === 'ACHIEVED') {
    return 'bg-emerald-100 text-emerald-800'
  }
  if (report.value?.resultStatus === 'NOT_ACHIEVED') {
    return 'bg-rose-100 text-rose-700'
  }
  return 'bg-amber-100 text-amber-800'
})

const heroMetrics = computed(() => {
  return [
    {
      label: REPORT_COPY.metric1Label,
      value: learnedPoints.value.length + ' ' + REPORT_COPY.metricLearnedUnit,
      hint: REPORT_COPY.metric1Hint,
    },
    {
      label: REPORT_COPY.metric2Label,
      value: evidenceDigest.value[0] || REPORT_COPY.metric2Fallback,
      hint: REPORT_COPY.metric2Hint,
    },
    {
      label: REPORT_COPY.metric3Label,
      value: recommendedAction.value?.title || REPORT_COPY.metric3Fallback,
      hint: REPORT_COPY.metric3Hint,
    },
  ]
})

async function fetchReport() {
  if (!store.sessionId) return
  loading.value = true
  error.value = null
  try {
    const data = await getReport(store.sessionId)
    store.report = data.learningReport
    store.nextActionDecision = data.nextActionDecision
    report.value = data.learningReport
    const action = data.learningReport.recommendedNextStep?.actionType || data.nextActionDecision?.actionType
    if (action) {
      selectedAction.value = action
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
  if (store.report) {
    report.value = store.report
  }
  if (!report.value) {
    void fetchReport()
  } else {
    const action = store.report?.recommendedNextStep?.actionType || store.nextActionDecision?.actionType
    if (action) {
      selectedAction.value = action
    }
  }
})
</script>
