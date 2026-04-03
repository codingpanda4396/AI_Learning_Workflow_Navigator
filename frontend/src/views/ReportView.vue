<template>
  <PageContainer>
    <div class="min-h-screen bg-[linear-gradient(180deg,#e8eef5_0%,#ffffff_42%,#f1f5f9_100%)]">
      <AppTopBar current="report" />
      <main class="mx-auto max-w-4xl px-4 pb-14 pt-4 md:px-6 lg:px-8">
        <LoadingState v-if="loading && !report" :message="REPORT_COPY.loading" />
        <ErrorState v-else-if="error" :message="error">
          <template #action>
            <SecondaryButton @click="fetchReport">{{ REPORT_COPY.retry }}</SecondaryButton>
          </template>
        </ErrorState>
        <section
          v-else-if="reportUnavailableReason === 'not-completed'"
          class="rounded-2xl border border-amber-200 bg-white p-6 shadow-card md:p-8"
        >
          <div class="w-fit rounded-full bg-amber-50 px-3 py-1 text-xs font-semibold tracking-[0.16em] text-amber-700">
            报告暂未生成
          </div>
          <h1 class="mt-4 text-2xl font-semibold tracking-tight text-text-primary md:text-3xl">
            当前学习还没走到「报告」这一步
          </h1>
          <p class="mt-3 max-w-2xl text-sm leading-7 text-text-secondary md:text-base">
            报告只有在本轮任务真正完成后才会生成。请先回到执行页完成当前任务。
          </p>
          <div
            v-if="blockedFlow"
            class="mt-5 rounded-input border border-border bg-primary-muted/60 px-4 py-4 text-sm leading-7 text-text-primary"
          >
            <p>当前进度：{{ blockedFlow.completedTaskCount }}/{{ blockedFlow.totalTaskCount }} 个任务</p>
            <p v-if="blockedFlow.currentTaskId">当前任务：{{ blockedFlow.currentTaskId }}</p>
            <p>现在应停留在：{{ blockedPhaseLabel }}</p>
          </div>
          <div class="mt-6 flex flex-wrap gap-3">
            <PrimaryButton @click="goBackToTask">回到当前任务</PrimaryButton>
            <SecondaryButton @click="fetchReport">重新检查</SecondaryButton>
          </div>
        </section>

        <section v-else-if="report" class="flex flex-col gap-6">
          <!-- 结论：对齐规划页 Hero -->
          <section
            class="relative overflow-hidden rounded-2xl border border-border bg-white px-5 py-6 shadow-card md:rounded-[24px] md:px-7 md:py-7"
          >
            <div
              class="pointer-events-none absolute inset-0 bg-[radial-gradient(ellipse_120%_80%_at_0%_-20%,rgba(30,58,95,0.08),transparent_50%),linear-gradient(180deg,rgba(255,255,255,0.98),rgba(232,238,245,0.5))]"
            />
            <div class="relative space-y-5">
              <div class="flex flex-wrap items-center gap-2">
                <span class="text-xs font-semibold uppercase tracking-[0.2em] text-primary">
                  {{ REPORT_COPY.heroEyebrow }}
                </span>
                <span
                  class="inline-flex items-center rounded-full px-3 py-1 text-sm font-semibold"
                  :class="heroStatusClass"
                >
                  {{ resultStatusLabels[report.resultStatus] ?? report.resultStatus }}
                </span>
              </div>
              <p class="text-sm font-medium text-text-secondary">
                {{ report.goalReview || REPORT_COPY.goalFallback }}
              </p>
              <h1
                class="text-2xl font-semibold tracking-tight text-text-primary md:text-4xl md:leading-[1.12]"
              >
                {{ report.finalSummary || report.summaryText || REPORT_COPY.summaryFallback }}
              </h1>
              <p class="text-sm leading-relaxed text-text-secondary md:text-base">
                {{ recommendedReason }}
              </p>
              <div class="flex flex-wrap gap-2">
                <span
                  v-for="(chip, i) in heroChips"
                  :key="i"
                  class="inline-flex rounded-full border border-primary/20 bg-primary-muted px-3 py-1 text-xs font-medium text-primary"
                >
                  {{ chip }}
                </span>
              </div>

              <div
                v-if="growthComparison.show"
                class="rounded-input border border-border bg-primary-muted/50 px-4 py-4"
              >
                <p class="text-[11px] font-semibold uppercase tracking-[0.14em] text-text-muted">
                  {{ REPORT_COPY.growthCompactLabel }}
                </p>
                <p class="mt-2 text-sm leading-7 text-text-primary">{{ growthComparison.before }}</p>
                <ul class="mt-3 space-y-1.5 border-t border-border/60 pt-3">
                  <li
                    v-for="(line, i) in growthComparison.afterLines.slice(0, 3)"
                    :key="i"
                    class="flex gap-2 text-sm leading-7 text-text-primary"
                  >
                    <span class="mt-2 h-1 w-1 shrink-0 rounded-full bg-primary/60" />
                    <span>{{ line }}</span>
                  </li>
                </ul>
              </div>
            </div>
          </section>

          <!-- 本轮要点 -->
          <section class="overflow-hidden rounded-2xl border border-border bg-white p-5 shadow-card md:p-7">
            <div class="divide-y divide-border">
              <div class="pb-6">
                <p class="text-xs font-semibold uppercase tracking-[0.16em] text-primary">
                  {{ REPORT_COPY.learnedEyebrow }}
                </p>
                <h2 class="mt-2 text-lg font-semibold text-text-primary">{{ REPORT_COPY.learnedTitle }}</h2>
                <ul class="mt-4 list-none space-y-2 text-sm leading-7 text-text-primary">
                  <li v-for="(item, index) in learnedPoints" :key="item + index" class="flex gap-2">
                    <span class="text-primary">·</span>
                    <span>{{ item }}</span>
                  </li>
                </ul>
              </div>
              <div class="py-6">
                <p class="text-xs font-semibold uppercase tracking-[0.16em] text-primary">
                  {{ REPORT_COPY.gapEyebrow }}
                </p>
                <h2 class="mt-2 text-lg font-semibold text-text-primary">{{ REPORT_COPY.gapTitle }}</h2>
                <ul class="mt-4 space-y-2">
                  <li
                    v-for="(item, index) in remainingGaps"
                    :key="item + index"
                    class="rounded-input border border-border bg-background/80 px-4 py-3 text-sm leading-7 text-text-primary"
                  >
                    {{ item }}
                  </li>
                </ul>
              </div>
              <div class="pt-6">
                <p class="text-xs font-semibold uppercase tracking-[0.16em] text-primary">
                  {{ REPORT_COPY.methodEyebrow }}
                </p>
                <h2 class="mt-2 text-lg font-semibold text-text-primary">{{ REPORT_COPY.methodTitle }}</h2>
                <p class="mt-3 text-sm leading-7 text-text-secondary">{{ methodReview.summary }}</p>
                <div v-if="methodReview.strengths.length" class="mt-4 flex flex-wrap gap-2">
                  <span
                    v-for="item in methodReview.strengths"
                    :key="item"
                    class="rounded-full border border-primary/20 bg-primary-muted px-3 py-1 text-xs font-medium text-primary"
                  >
                    {{ item }}
                  </span>
                </div>
                <div v-if="methodReview.risks.length" class="mt-4 space-y-2 text-sm leading-7 text-text-primary">
                  <p v-for="item in methodReview.risks" :key="item">{{ item }}</p>
                </div>
                <ul class="mt-4 list-none space-y-1 text-sm leading-7 text-text-secondary">
                  <li v-for="item in methodReview.nextFocus" :key="item">→ {{ item }}</li>
                </ul>
              </div>
            </div>
          </section>

          <!-- 执行回顾 -->
          <section v-if="taskHighlights.length" class="rounded-2xl border border-border bg-white p-5 shadow-card md:p-7">
            <p class="text-xs font-semibold uppercase tracking-[0.16em] text-primary">
              {{ REPORT_COPY.timelineEyebrow }}
            </p>
            <h2 class="mt-2 text-lg font-semibold text-text-primary">{{ REPORT_COPY.timelineTitle }}</h2>
            <div class="mt-4 space-y-2">
              <article
                v-for="(item, index) in taskHighlights"
                :key="item.taskId"
                class="rounded-input border border-border px-4 py-3"
              >
                <div class="flex flex-wrap items-baseline justify-between gap-2">
                  <p class="text-sm font-semibold text-text-primary">
                    任务 {{ index + 1 }} · {{ item.title }}
                  </p>
                  <span class="text-xs text-text-muted">{{
                    taskCompletionStatusLabels[item.completionStatus] ?? item.completionStatus
                  }}</span>
                </div>
                <p class="mt-2 text-sm leading-7 text-text-secondary">{{ item.learned }}</p>
                <p v-if="item.issue" class="mt-1 text-sm text-accent-hover">{{ item.issue }}</p>
              </article>
            </div>
            <p v-if="evidenceLine" class="mt-4 text-xs leading-relaxed text-text-muted">
              {{ REPORT_COPY.evidenceFootnote }}{{ evidenceLine }}
            </p>
          </section>

          <p
            v-else-if="evidenceLine"
            class="rounded-2xl border border-dashed border-border bg-white/60 px-4 py-3 text-xs leading-relaxed text-text-muted"
          >
            {{ REPORT_COPY.evidenceFootnote }}{{ evidenceLine }}
          </p>

          <!-- 下一步 -->
          <section class="rounded-2xl border border-border bg-white p-5 shadow-card md:p-7">
            <p class="text-xs font-semibold uppercase tracking-[0.16em] text-primary">{{ REPORT_COPY.nextEyebrow }}</p>
            <h2 class="mt-2 text-lg font-semibold text-text-primary">
              {{ recommendedAction?.title || REPORT_COPY.nextTitle }}
            </h2>
            <p class="mt-2 text-sm leading-relaxed text-text-secondary">{{ recommendedReason }}</p>
            <p v-if="recommendedAction?.nextEntryPoint" class="mt-2 text-sm text-text-primary">
              {{ recommendedAction?.nextEntryPoint }}
            </p>
            <p
              v-if="recommendedAction?.requiresReplan"
              class="mt-3 rounded-input border border-amber-200/80 bg-amber-50/80 px-4 py-3 text-sm text-amber-950"
            >
              {{ REPORT_COPY.replanBanner }}
            </p>
            <div class="mt-5">
              <label class="block text-xs font-medium text-text-muted">{{ REPORT_COPY.nextActionLabel }}</label>
              <select
                v-model="selectedAction"
                class="mt-2 w-full rounded-input border border-border bg-white px-3 py-2.5 text-sm text-text-primary shadow-sm focus:border-primary focus:outline-none focus:ring-1 focus:ring-primary/30"
              >
                <option value="" disabled>{{ REPORT_COPY.selectPlaceholder }}</option>
                <option v-for="opt in nextActionSelectOptions" :key="opt.value" :value="opt.value">
                  {{ opt.label }}
                </option>
              </select>
            </div>
            <div class="mt-5 flex flex-wrap gap-3">
              <PrimaryButton :loading="confirming" :disabled="!selectedAction" @click="onConfirm">
                {{ REPORT_COPY.confirmSubmit }}
              </PrimaryButton>
              <SecondaryButton @click="goHome">{{ REPORT_COPY.restart }}</SecondaryButton>
            </div>
            <p
              v-if="nextHint"
              class="mt-4 rounded-input border border-border bg-primary-muted/50 px-4 py-3 text-sm text-text-primary"
            >
              {{ nextHint }}
            </p>
          </section>
        </section>
      </main>
    </div>
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
import { getReport, confirmNextAction, getSessionFlowState } from '@/api/session'
import { showToast } from '@/stores/toast'
import { getErrorMessage, type ApiError } from '@/api/request'
import { resultStatusLabels, nextActionTypeLabels, taskCompletionStatusLabels } from '@/types/labels'
import type { LearningMethodReview, LearningReport, RecommendedNextStep, SessionFlowState, TaskHighlight } from '@/types/dto'
import type { NextActionTypeType } from '@/types/enums'

const REPORT_COPY = {
  heroEyebrow: '最终报告',
  loading: '正在整理结果…',
  retry: '重试',
  restart: '重新开始',
  goalFallback: '本轮目标已完成执行回收',
  summaryFallback: '这轮学习已经结束，现在进入结果回顾与下一步判断。',
  learnedEyebrow: '本轮带走',
  learnedTitle: '你已经学会了什么',
  learnedFallback: '本轮已经形成了一次完整的学习闭环。',
  gapEyebrow: '仍待补稳',
  gapTitle: '还需要补什么',
  gapFallback: '当前没有显著薄弱点，下一步可以开始轻量应用。',
  timelineEyebrow: '执行回顾',
  timelineTitle: '这轮任务怎么走完的',
  methodEyebrow: '学习方式',
  methodTitle: '这轮你是怎么学的',
  methodFallback: '这轮已经形成基本推进节奏，下一轮继续保留自我解释和检查动作。',
  methodNextFallback: '下一轮继续先说自己的理解，再用反馈校正。',
  nextEyebrow: '下一步',
  nextTitle: '接下来怎么走',
  nextReasonFallback: '系统会根据这轮执行结果，给出下一步建议。',
  nextActionLabel: '选择下一步动作',
  selectPlaceholder: '请选择动作',
  replanBanner: '建议重新规划学习路径',
  confirmSubmit: '确认下一步',
  nextHintDefault: '已确认下一步',
  toastReplan: '建议重新规划，请从目标输入重新开始',
  growthCompactLabel: '起点与带走',
  growthBeforeFallback: '诊断阶段已记录你的起点状态；完成本轮任务后，下方会沉淀可带走的要点。',
  evidenceFootnote: '依据：',
  chipTasks: '个任务节点',
  chipEvidenceShort: '执行已记录',
  chipNextFallback: '下一步待定',
} as const

const router = useRouter()
const store = useWorkflowStore()

const loading = ref(true)
const confirming = ref(false)
const error = ref<string | null>(null)
const report = ref<LearningReport | null>(null)
const reportUnavailableReason = ref<'not-completed' | null>(null)
const blockedFlow = ref<SessionFlowState | null>(null)
const nextHint = ref('')
const selectedAction = ref<NextActionTypeType | ''>('')
const canReuseStoredReport = computed(
  () => !!store.sessionId && store.report?.sessionId === store.sessionId,
)

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

const blockedPhaseLabel = computed(() => {
  const phase = blockedFlow.value?.currentPhase
  if (phase === 'task') return '执行阶段'
  if (phase === 'plan') return '规划阶段'
  if (phase === 'diagnosis') return '诊断阶段'
  return '当前流程'
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
  if (report.value?.evidenceDigest?.length) return report.value.evidenceDigest
  if (report.value?.evidenceSummary?.length) return report.value.evidenceSummary
  return [] as string[]
})

const evidenceLine = computed(() => {
  const parts = evidenceDigest.value.map((s) => s?.trim()).filter(Boolean) as string[]
  if (!parts.length) return ''
  const joined = parts.slice(0, 2).join(' · ')
  return joined.length > 160 ? `${joined.slice(0, 157)}…` : joined
})

const taskHighlights = computed<TaskHighlight[]>(() => {
  return report.value?.taskHighlights?.length ? report.value.taskHighlights : []
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
  return 'bg-primary-muted text-primary'
})

const heroChips = computed(() => {
  const chips: string[] = []
  chips.push(`${learnedPoints.value.length} 条收获`)
  if (taskHighlights.value.length) {
    chips.push(`${taskHighlights.value.length}${REPORT_COPY.chipTasks}`)
  } else {
    chips.push(REPORT_COPY.chipEvidenceShort)
  }
  const title = recommendedAction.value?.title?.trim()
  const act = recommendedAction.value?.actionType
  const actLabel =
    act && act in nextActionTypeLabels ? nextActionTypeLabels[act as keyof typeof nextActionTypeLabels] : ''
  chips.push(title || actLabel || REPORT_COPY.chipNextFallback)
  return chips.slice(0, 3)
})

const nextActionSelectOptions = computed(() => {
  return Object.entries(nextActionTypeLabels).map(([value, label]) => ({
    value: value as NextActionTypeType,
    label,
  }))
})

async function fetchReport() {
  if (!store.sessionId) return
  loading.value = true
  error.value = null
  reportUnavailableReason.value = null
  blockedFlow.value = null
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
    const apiError = err as ApiError
    if (apiError.code === 'SESSION_NOT_COMPLETED') {
      store.report = null
      store.nextActionDecision = null
      report.value = null
      error.value = null
      reportUnavailableReason.value = 'not-completed'
      try {
        blockedFlow.value = await getSessionFlowState(store.sessionId)
        store.currentTaskId = blockedFlow.value.currentTaskId ?? null
      } catch {
        blockedFlow.value = null
      }
      return
    }
    error.value = getErrorMessage(err)
  } finally {
    loading.value = false
  }
}

async function goBackToTask() {
  await router.push('/execution')
}

async function goHome() {
  store.clearRunState()
  await router.push('/goal')
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
  nextHint.value = ''
  selectedAction.value = ''
  if (canReuseStoredReport.value) {
    report.value = store.report
  } else {
    store.report = null
    store.nextActionDecision = null
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
