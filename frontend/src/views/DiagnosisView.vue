<script setup lang="ts">
import { computed, onMounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import AppShell from '@/components/common/AppShell.vue';
import ErrorState from '@/components/common/ErrorState.vue';
import LoadingState from '@/components/common/LoadingState.vue';
import CapabilityProfileCard from '@/components/diagnosis/CapabilityProfileCard.vue';
import DiagnosisGoalSummaryCard from '@/components/diagnosis/DiagnosisGoalSummaryCard.vue';
import DiagnosisProgressCard from '@/components/diagnosis/DiagnosisProgressCard.vue';
import DiagnosisQuestionCard from '@/components/diagnosis/DiagnosisQuestionCard.vue';
import { useDiagnosisStore } from '@/stores/diagnosis';
import type { DiagnosisAnswerValue, DiagnosisQuestion } from '@/types/diagnosis';

const route = useRoute();
const router = useRouter();
const diagnosisStore = useDiagnosisStore();

const sessionId = computed(() => String(route.params.sessionId || route.query.sessionId || '').trim());
const goalText = computed(() => String(route.query.goal || 'No goal was provided for this diagnosis flow.'));
const courseId = computed(() => String(route.query.course || 'Course not provided'));
const chapterId = computed(() => String(route.query.chapter || 'Chapter not provided'));
const questions = computed(() => diagnosisStore.questions);
const currentQuestion = computed<DiagnosisQuestion | null>(() => questions.value[diagnosisStore.currentQuestionIndex] || null);
const currentAnswer = computed(() => {
  const questionId = currentQuestion.value?.questionId;
  return questionId ? diagnosisStore.answers[questionId] : undefined;
});

const isGenerating = computed(() => diagnosisStore.loading && questions.value.length === 0);
const isSubmitting = computed(() => diagnosisStore.submitting);
const isResult = computed(() => Boolean(diagnosisStore.capabilityProfile));
const isError = computed(() => Boolean(diagnosisStore.error) && !isGenerating.value && !isSubmitting.value && !isResult.value);
const canRetrySubmit = computed(() => Boolean(questions.value.length) && !isResult.value);
const currentStep = computed(() => (questions.value.length ? diagnosisStore.currentQuestionIndex + 1 : 0));
const submitButtonText = computed(() => diagnosisStore.nextAction?.label || 'Open plan preview');
const statusText = computed(() => diagnosisStore.status || 'GENERATED');
const sourceText = computed(() => diagnosisStore.fallback?.contentSource?.label || 'Contract response');
const fallbackText = computed(() => {
  if (!diagnosisStore.fallback?.applied) {
    return 'No fallback applied';
  }
  return diagnosisStore.fallback.reasons.join(' / ') || 'Rule fallback applied';
});

const isCurrentAnswered = computed(() => {
  const question = currentQuestion.value;
  if (!question || !question.required) {
    return true;
  }

  const answer = currentAnswer.value;
  if (Array.isArray(answer)) {
    return answer.length > 0;
  }
  return typeof answer === 'string' ? answer.trim().length > 0 : false;
});

function updateAnswer(value: DiagnosisAnswerValue) {
  if (!currentQuestion.value) {
    return;
  }
  diagnosisStore.updateAnswer(currentQuestion.value.questionId, value);
}

function previousQuestion() {
  diagnosisStore.setCurrentQuestionIndex(diagnosisStore.currentQuestionIndex - 1);
}

function nextQuestion() {
  if (!isCurrentAnswered.value) {
    return;
  }
  diagnosisStore.setCurrentQuestionIndex(diagnosisStore.currentQuestionIndex + 1);
}

async function submitDiagnosis() {
  if (!isCurrentAnswered.value) {
    return;
  }
  try {
    await diagnosisStore.submitDiagnosis();
  } catch {
    return;
  }
}

async function retryGenerate() {
  if (!sessionId.value) {
    return;
  }
  try {
    await diagnosisStore.generateDiagnosis(sessionId.value);
  } catch {
    return;
  }
}

async function retryCurrentAction() {
  if (canRetrySubmit.value) {
    await submitDiagnosis();
    return;
  }
  await retryGenerate();
}

async function enterPlanFlow() {
  const target = diagnosisStore.nextAction?.target;
  const targetSessionId = String(target?.params?.sessionId ?? diagnosisStore.sessionId ?? sessionId.value);
  const targetDiagnosisId = String(target?.params?.diagnosisId ?? diagnosisStore.diagnosisId);
  const numericSessionId = Number(targetSessionId);

  if (!Number.isFinite(numericSessionId) || numericSessionId <= 0) {
    await router.push('/');
    return;
  }

  await router.push({
    path: target?.route || '/plan',
    query: {
      sessionId: numericSessionId,
      diagnosisId: targetDiagnosisId,
      goal: goalText.value,
      course: courseId.value,
      chapter: chapterId.value,
    },
  });
}

async function loadDiagnosis() {
  diagnosisStore.reset();
  if (!sessionId.value) {
    diagnosisStore.error = 'Missing sessionId for diagnosis.';
    return;
  }
  try {
    await diagnosisStore.generateDiagnosis(sessionId.value);
  } catch {
    return;
  }
}

onMounted(async () => {
  await loadDiagnosis();
});
</script>

<template>
  <AppShell>
    <div class="space-y-6 pb-10">
      <DiagnosisGoalSummaryCard :session-id="sessionId" :goal="goalText" :course="courseId" :chapter="chapterId" />

      <LoadingState v-if="isGenerating" />

      <ErrorState v-else-if="isError" :message="diagnosisStore.error" />

      <template v-else-if="isResult && diagnosisStore.capabilityProfile">
        <CapabilityProfileCard
          :profile="diagnosisStore.capabilityProfile"
          :insights="diagnosisStore.insights"
          :next-action="diagnosisStore.nextAction"
          :status="statusText"
          :fallback-text="fallbackText"
          :source-text="sourceText"
          :metadata="diagnosisStore.metadata"
        />

        <div class="flex justify-end">
          <button
            type="button"
            class="rounded-2xl bg-slate-950 px-5 py-3 text-sm font-semibold text-white transition hover:bg-slate-800"
            @click="enterPlanFlow"
          >
            {{ submitButtonText }}
          </button>
        </div>
      </template>

      <template v-else-if="currentQuestion">
        <div class="grid gap-3 md:grid-cols-3">
          <div class="rounded-2xl border border-slate-200 bg-white p-4 text-sm text-slate-600 shadow-sm">
            <p class="text-xs font-semibold uppercase tracking-[0.18em] text-slate-400">Status</p>
            <p class="mt-2 font-medium text-slate-900">{{ statusText }}</p>
          </div>
          <div class="rounded-2xl border border-slate-200 bg-white p-4 text-sm text-slate-600 shadow-sm">
            <p class="text-xs font-semibold uppercase tracking-[0.18em] text-slate-400">Source</p>
            <p class="mt-2 font-medium text-slate-900">{{ sourceText }}</p>
          </div>
          <div class="rounded-2xl border border-slate-200 bg-white p-4 text-sm text-slate-600 shadow-sm">
            <p class="text-xs font-semibold uppercase tracking-[0.18em] text-slate-400">Fallback</p>
            <p class="mt-2 font-medium text-slate-900">{{ fallbackText }}</p>
          </div>
        </div>

        <div class="grid gap-6 lg:grid-cols-[minmax(0,1fr)_320px]">
          <DiagnosisQuestionCard :question="currentQuestion" :model-value="currentAnswer" @update:model-value="updateAnswer" />
          <DiagnosisProgressCard :current="currentStep" :total="questions.length" />
        </div>

        <div class="flex flex-col gap-3 rounded-[1.6rem] border border-slate-200 bg-white p-5 shadow-sm md:flex-row md:items-center md:justify-between">
          <p class="text-sm leading-6 text-slate-600">
            Answers are submitted with stable option codes. The profile and plan preview now read the flattened contract fields directly.
          </p>
          <div class="flex flex-wrap gap-3">
            <button
              type="button"
              class="rounded-2xl border border-slate-200 px-4 py-2.5 text-sm font-medium text-slate-700 transition hover:bg-slate-50"
              :disabled="diagnosisStore.currentQuestionIndex === 0"
              @click="previousQuestion"
            >
              Previous
            </button>
            <button
              v-if="diagnosisStore.currentQuestionIndex < questions.length - 1"
              type="button"
              class="rounded-2xl bg-slate-950 px-4 py-2.5 text-sm font-semibold text-white transition hover:bg-slate-800 disabled:cursor-not-allowed disabled:bg-slate-300"
              :disabled="!isCurrentAnswered"
              @click="nextQuestion"
            >
              Next
            </button>
            <button
              v-else
              type="button"
              class="rounded-2xl bg-sky-600 px-4 py-2.5 text-sm font-semibold text-white transition hover:bg-sky-500 disabled:cursor-not-allowed disabled:bg-slate-300"
              :disabled="!isCurrentAnswered || isSubmitting"
              @click="submitDiagnosis"
            >
              {{ isSubmitting ? 'Submitting diagnosis...' : 'Submit diagnosis' }}
            </button>
          </div>
        </div>
      </template>

      <div v-if="isSubmitting" class="rounded-[1.8rem] border border-sky-100 bg-sky-50 p-6 text-sm leading-7 text-sky-700">
        We are generating the capability profile and next action from the submitted diagnosis.
      </div>

      <div v-if="isError" class="flex justify-start">
        <button
          type="button"
          class="rounded-2xl border border-slate-200 px-4 py-2.5 text-sm font-medium text-slate-700 transition hover:bg-slate-50"
          @click="retryCurrentAction"
        >
          {{ canRetrySubmit ? 'Retry submit' : 'Regenerate diagnosis' }}
        </button>
      </div>
    </div>
  </AppShell>
</template>
