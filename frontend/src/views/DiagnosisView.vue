<template>
  <PageContainer>
    <TransitionOverlay v-if="submitTransitionOverlay" :message="DIAGNOSIS_COPY.transition" />
    <AppTopBar current="diagnosis" />
    <main class="mx-auto max-w-2xl px-6 py-8 md:px-8">
      <LoadingState v-if="loading && !sessionReady" :message="DIAGNOSIS_COPY.loading" />
      <ErrorState v-else-if="error" :message="error">
        <template #action>
          <SecondaryButton @click="fetchSession">{{ DIAGNOSIS_COPY.retry }}</SecondaryButton>
        </template>
      </ErrorState>

      <WorkflowPageScaffold v-else-if="sessionReady">
        <template #title>
          <h1 class="text-2xl font-bold text-text-primary md:text-3xl">
            {{ DIAGNOSIS_COPY.title }}
          </h1>
          <p class="mt-2 max-w-xl text-text-secondary">
            {{ DIAGNOSIS_COPY.subtitle }}
          </p>
          <div
            class="mt-4 rounded-xl border border-primary/25 bg-primary-muted/90 px-4 py-3"
          >
            <p class="text-[11px] font-semibold uppercase tracking-wider text-primary/90">
              {{ DIAGNOSIS_COPY.topicLabel }}
            </p>
            <p class="mt-1 text-base font-semibold text-text-primary">
              {{ sessionTopicLabel }}
            </p>
          </div>
          <p class="mt-3 text-sm font-medium leading-snug text-text-primary">
            {{ DIAGNOSIS_COPY.intro }}
          </p>
        </template>

        <template #primary>
          <form class="space-y-5" @submit.prevent="onSubmit">
            <FormCard
              v-for="question in questionCards"
              :key="question.id"
              class="space-y-4"
            >
              <div class="space-y-2">
                <p class="text-sm font-semibold text-primary">
                  {{ DIAGNOSIS_COPY.qPrefix }} {{ question.number }} {{ DIAGNOSIS_COPY.qSuffix }}
                </p>
                <h3 class="text-base font-medium leading-6 text-text-primary">
                  {{ question.prompt }}
                </h3>
              </div>

              <div class="space-y-2">
                <label
                  v-for="opt in question.options"
                  :key="opt.key"
                  class="flex cursor-pointer items-start gap-3 rounded-input border px-4 py-3 text-sm transition-all"
                  :class="
                    question.selected === opt.value
                      ? 'border-primary bg-primary/5 ring-1 ring-primary/20'
                      : 'border-border hover:border-primary/50 hover:bg-primary/5'
                  "
                >
                  <input
                    :checked="question.selected === opt.value"
                    type="radio"
                    :name="question.id"
                    :value="opt.value"
                    class="mt-0.5 h-4 w-4 border-border text-primary focus:ring-primary"
                    @change="selectAnswer(question.id, opt.value)"
                  />
                  <span class="flex-1 text-text-primary">
                    <span
                      v-if="opt.badge"
                      class="mr-2 inline-flex min-w-6 items-center justify-center rounded-full bg-primary/10 px-2 py-0.5 text-xs font-semibold text-primary"
                    >
                      {{ opt.badge }}
                    </span>
                    <span
                      :class="question.selected === opt.value ? 'font-semibold' : ''"
                    >
                      {{ opt.label }}
                    </span>
                  </span>
                </label>
              </div>
            </FormCard>

            <div class="rounded-card border border-border bg-white px-5 py-4 shadow-card">
              <p class="text-sm font-medium text-text-secondary">
                {{ progressMessage }}
              </p>
              <p class="mt-1 text-sm text-text-primary">
                完成后会直接进入学习规划。
              </p>
            </div>

            <div class="flex justify-end pt-1">
              <PrimaryButton :loading="submitting" @click="onSubmit">
                {{ DIAGNOSIS_COPY.submit }}
              </PrimaryButton>
            </div>
          </form>
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
import FormCard from '@/components/ui/FormCard.vue'
import PrimaryButton from '@/components/ui/PrimaryButton.vue'
import SecondaryButton from '@/components/ui/SecondaryButton.vue'
import LoadingState from '@/components/ui/LoadingState.vue'
import ErrorState from '@/components/ui/ErrorState.vue'
import WorkflowPageScaffold from '@/components/workflow/WorkflowPageScaffold.vue'
import TransitionOverlay from '@/components/ui/TransitionOverlay.vue'
import { useWorkflowStore } from '@/stores/workflow'
import { createSession, submitDiagnosis } from '@/api/diagnosis'
import { showToast } from '@/stores/toast'
import { getErrorMessage } from '@/api/request'
import {
  DEFAULT_QUICK_DIAGNOSIS_UI_STATE,
  mapQuickDiagnosisToAnswers,
  type FoundationUiId,
  type BlockerUiId,
  type PaceUiId,
  type QuickDiagnosisUiState,
} from '@/utils/diagnosisSubmitMapper'
import { mapTopicDiagnosisToFoundation } from '@/utils/diagnosisTopicMapper'
import { resolveKnowledgePackId } from '@/composables/useKnowledgePack'
import { getKnowledgeDemoConfig } from '@/constants/KnowledgeConfig'
import type { KnowledgeDemoConfig } from '@/constants/KnowledgeConfig'
import type { DiagnosisOptionValue } from '@/constants/KnowledgeConfig'
import type { KnowledgePackId } from '@/types/knowledgePack'
import { SESSION_KEY_PLAN_DIAGNOSIS_RECAP } from '@/utils/diagnosisRecapCopy'
import { workflowTopicLabelFromStructuredGoal } from '@/utils/workflowTopicLabel'
import { DIAGNOSIS_COPY } from '@/constants/uiCopy'

type QuestionCardId = 'foundation' | 'blocker' | 'pace'

type QuestionOption = {
  key: string
  value: string
  label: string
  badge?: string
}

type QuestionCard = {
  id: QuestionCardId
  number: number
  prompt: string
  selected: string | null
  options: QuestionOption[]
}

const router = useRouter()
const store = useWorkflowStore()

const sessionTopicLabel = computed(() => {
  const t = workflowTopicLabelFromStructuredGoal(store.structuredGoal)
  return t || '当前主题'
})

const loading = ref(true)
const submitting = ref(false)
const submitTransitionOverlay = ref(false)
const error = ref<string | null>(null)

const ui = ref<QuickDiagnosisUiState>({
  foundation: null,
  blocker: null,
  pace: null,
})

const topicDiag = ref<DiagnosisOptionValue | null>(null)

const topicPackId = computed((): KnowledgePackId | null => {
  const id = resolveKnowledgePackId({ structuredGoal: store.structuredGoal })
  return id
})

const topicDemoConfig = computed((): KnowledgeDemoConfig | null => {
  const id = topicPackId.value
  if (!id) return null
  return getKnowledgeDemoConfig(id)
})

const sessionReady = computed(
  () => !!store.diagnosisId && !!store.sessionId && !loading.value && !error.value
)

const foundationOptions: { id: FoundationUiId; label: string }[] = [
  { id: 'fu_none', label: '完全不会，几乎没学过' },
  { id: 'fu_fuzzy', label: '学过一点，但很模糊' },
  { id: 'fu_shaky', label: '基本懂，但做题不稳' },
  { id: 'fu_solid_practice', label: '已经会了，想提高做题能力' },
]

const blockerOptions: { id: BlockerUiId; label: string }[] = [
  { id: 'bk_concept', label: '概念听不懂' },
  { id: 'bk_no_problem', label: '看懂了但不会做题' },
  { id: 'bk_error_prone', label: '做题容易出错' },
  { id: 'bk_transfer', label: '会做基础题，但不会迁移应用' },
]

const paceOptions: { id: PaceUiId; label: string }[] = [
  { id: 'pc_tight', label: '时间紧，先快速抓重点' },
  { id: 'pc_normal', label: '正常推进，理解和练习并重' },
  { id: 'pc_relaxed', label: '时间充裕，想学扎实一点' },
]

const questionCards = computed<QuestionCard[]>(() => {
  const foundationQuestion: QuestionCard = topicDemoConfig.value
    ? {
        id: 'foundation',
        number: 1,
        prompt: topicDemoConfig.value.diagnosisQuestion.question,
        selected: topicDiag.value,
        options: topicDemoConfig.value.diagnosisQuestion.options.map((opt) => ({
          key: opt.value,
          value: opt.value,
          label: opt.label,
          badge: opt.value,
        })),
      }
    : {
        id: 'foundation',
        number: 1,
        prompt: DIAGNOSIS_COPY.qFoundation,
        selected: ui.value.foundation,
        options: foundationOptions.map((opt) => ({
          key: opt.id,
          value: opt.id,
          label: opt.label,
        })),
      }

  return [
    foundationQuestion,
    {
      id: 'blocker',
      number: 2,
      prompt: DIAGNOSIS_COPY.qBlocker,
      selected: ui.value.blocker,
      options: blockerOptions.map((opt) => ({
        key: opt.id,
        value: opt.id,
        label: opt.label,
      })),
    },
    {
      id: 'pace',
      number: 3,
      prompt: DIAGNOSIS_COPY.q3,
      selected: ui.value.pace,
      options: paceOptions.map((opt) => ({
        key: opt.id,
        value: opt.id,
        label: opt.label,
      })),
    },
  ]
})

const answeredCount = computed(() => {
  const foundationAnswered = topicDemoConfig.value ? !!topicDiag.value : !!ui.value.foundation
  return Number(foundationAnswered) + Number(!!ui.value.blocker) + Number(!!ui.value.pace)
})

const progressMessage = computed(() => {
  if (answeredCount.value === 3) return '已完成 3/3，下一步生成你的学习规划。'
  const remaining = 3 - answeredCount.value
  if (answeredCount.value === 0) return '还没开始，先完成 3 个选择。'
  return `已完成 ${answeredCount.value}/3，还差 ${remaining} 题。`
})

function selectAnswer(questionId: QuestionCardId, value: string) {
  if (questionId === 'foundation') {
    if (topicDemoConfig.value) {
      topicDiag.value = value as DiagnosisOptionValue
      return
    }
    ui.value.foundation = value as FoundationUiId
    return
  }
  if (questionId === 'blocker') {
    ui.value.blocker = value as BlockerUiId
    return
  }
  ui.value.pace = value as PaceUiId
}

async function fetchSession() {
  if (!store.goalId) return
  loading.value = true
  error.value = null
  try {
    const data = await createSession(store.goalId)
    store.diagnosisId = data.diagnosisId
    store.sessionId = data.sessionId
    ui.value = { foundation: null, blocker: null, pace: null }
    topicDiag.value = null
  } catch (err) {
    error.value = getErrorMessage(err)
  } finally {
    loading.value = false
  }
}

function delay(ms: number) {
  return new Promise<void>((resolve) => {
    setTimeout(resolve, ms)
  })
}

function fillDiagnosisDefaults(): QuickDiagnosisUiState {
  const nextState: QuickDiagnosisUiState = {
    foundation: ui.value.foundation ?? DEFAULT_QUICK_DIAGNOSIS_UI_STATE.foundation,
    blocker: ui.value.blocker ?? DEFAULT_QUICK_DIAGNOSIS_UI_STATE.blocker,
    pace: ui.value.pace ?? DEFAULT_QUICK_DIAGNOSIS_UI_STATE.pace,
  }

  ui.value = nextState

  if (topicDemoConfig.value && !topicDiag.value) {
    const defaultTopicValue = topicDemoConfig.value.diagnosisQuestion.options[0]?.value ?? null
    if (defaultTopicValue) {
      topicDiag.value = defaultTopicValue
      nextState.foundation = mapTopicDiagnosisToFoundation(topicPackId.value!, defaultTopicValue)
      ui.value = nextState
    }
  }

  return nextState
}

async function onSubmit() {
  const nextState = fillDiagnosisDefaults()

  if (topicDemoConfig.value && topicDiag.value) {
    nextState.foundation = mapTopicDiagnosisToFoundation(
      topicPackId.value!,
      topicDiag.value
    )
    ui.value = nextState
  }

  const ans = mapQuickDiagnosisToAnswers(nextState)
  if (!ans?.length || !store.diagnosisId) {
    showToast(DIAGNOSIS_COPY.toastSubmitFail)
    return
  }
  submitting.value = true
  submitTransitionOverlay.value = true
  const minMs = 900
  const started = Date.now()
  try {
    const data = await submitDiagnosis(store.diagnosisId, ans)
    store.learnerProfileSnapshot = data.learnerProfileSnapshot
    store.diagnosisEvidenceSummary = data.diagnosisEvidenceSummary
    try {
      sessionStorage.setItem(SESSION_KEY_PLAN_DIAGNOSIS_RECAP, '1')
    } catch {
      /* ignore */
    }
    const elapsed = Date.now() - started
    const remaining = Math.max(0, minMs - elapsed)
    if (remaining > 0) await delay(remaining)
    await router.push('/plan')
  } catch (err) {
    showToast(getErrorMessage(err))
  } finally {
    submitTransitionOverlay.value = false
    submitting.value = false
  }
}

onMounted(() => {
  if (store.diagnosisId && store.sessionId) {
    loading.value = false
    return
  }
  fetchSession()
})
</script>




