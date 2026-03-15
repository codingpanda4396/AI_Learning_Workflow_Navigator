<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { generateDiagnosisApi, submitDiagnosisApi } from '@/api/modules/diagnosis';
import AppShell from '@/components/common/AppShell.vue';
import ErrorState from '@/components/common/ErrorState.vue';
import LoadingState from '@/components/common/LoadingState.vue';
import DiagnosisFooter from '@/components/diagnosis/DiagnosisFooter.vue';
import ProgressIndicator from '@/components/diagnosis/ProgressIndicator.vue';
import QuestionCard from '@/components/diagnosis/QuestionCard.vue';
import { useDiagnosisStore } from '@/stores/diagnosis';
import type { DiagnosisAnswer, DiagnosisAnswerValue, DiagnosisNextAction, DiagnosisQuestion } from '@/types/diagnosis';

const route = useRoute();
const router = useRouter();
const diagnosisStore = useDiagnosisStore();

const sessionId = computed(() => String(route.params.sessionId || route.query.sessionId || '').trim());
const goalText = computed(() => String(route.query.goal || '').trim());
const courseId = computed(() => String(route.query.course || '').trim());
const chapterId = computed(() => String(route.query.chapter || '').trim());

const diagnosisId = ref('');
const responseSessionId = ref('');
const nextAction = ref<DiagnosisNextAction | null>(null);
const questions = ref<DiagnosisQuestion[]>([]);
const diagnosisExplanation = ref('');
const loading = ref(false);
const submitting = ref(false);
const error = ref('');

const totalQuestions = computed(() => questions.value.length);
const currentQuestion = computed(() => diagnosisStore.currentQuestion);
const currentStep = computed(() => diagnosisStore.currentIndex + 1);
const currentAnswer = computed(() => {
  const questionId = currentQuestion.value?.questionId;
  return questionId ? diagnosisStore.answers[questionId] : undefined;
});
const isLastQuestion = computed(() => diagnosisStore.currentIndex >= totalQuestions.value - 1);
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
const buttonDisabled = computed(() => !isCurrentAnswered.value || loading.value || submitting.value);

function syncCurrentQuestion() {
  const next = questions.value[diagnosisStore.currentIndex] || null;
  diagnosisStore.setCurrentQuestion(next);
}

function updateAnswer(value: DiagnosisAnswerValue) {
  if (!currentQuestion.value) {
    return;
  }
  diagnosisStore.updateAnswer(currentQuestion.value.questionId, value);
}

async function enterPlanFlow(targetAction?: DiagnosisNextAction | null) {
  const target = targetAction?.target;
  const targetSessionId = String(target?.params?.sessionId ?? responseSessionId.value ?? sessionId.value);
  const targetDiagnosisId = String(target?.params?.diagnosisId ?? diagnosisId.value);
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

function buildSubmitAnswers(): DiagnosisAnswer[] {
  return questions.value
    .filter((question) => diagnosisStore.answers[question.questionId] !== undefined)
    .map((question) => {
      const value = diagnosisStore.answers[question.questionId];
      if (question.type === 'TEXT') {
        return {
          questionId: question.questionId,
          text: typeof value === 'string' ? value : '',
        };
      }
      if (question.type === 'MULTIPLE_CHOICE') {
        return {
          questionId: question.questionId,
          selectedOptionCodes: Array.isArray(value) ? value : typeof value === 'string' ? [value] : [],
        };
      }
      return {
        questionId: question.questionId,
        selectedOptionCode: typeof value === 'string' ? value : Array.isArray(value) ? value[0] : undefined,
      };
    });
}

async function submitCurrentAnswers() {
  if (!diagnosisId.value) {
    error.value = '诊断会话缺失，请刷新后再试。';
    return;
  }

  submitting.value = true;
  error.value = '';
  try {
    const response = await submitDiagnosisApi(diagnosisId.value, buildSubmitAnswers());
    await enterPlanFlow(response.nextAction ?? nextAction.value);
  } catch (submitError) {
    error.value = submitError instanceof Error ? submitError.message : '提交失败，请稍后重试。';
  } finally {
    submitting.value = false;
  }
}

async function handleContinue() {
  if (!currentQuestion.value || !isCurrentAnswered.value) {
    return;
  }
  if (!isLastQuestion.value) {
    diagnosisStore.setCurrentIndex(diagnosisStore.currentIndex + 1);
    syncCurrentQuestion();
    return;
  }
  await submitCurrentAnswers();
}

async function loadDiagnosis() {
  diagnosisStore.reset();
  questions.value = [];
  diagnosisId.value = '';
  responseSessionId.value = '';
  nextAction.value = null;
  diagnosisExplanation.value = '';
  error.value = '';

  if (!sessionId.value) {
    error.value = '缺少生成能力诊断所需的会话信息。';
    return;
  }

  loading.value = true;
  try {
    const response = await generateDiagnosisApi(sessionId.value);
    diagnosisId.value = response.diagnosisId;
    responseSessionId.value = response.sessionId;
    nextAction.value = response.nextAction ?? null;
    questions.value = response.questions;
    diagnosisExplanation.value = response.diagnosisExplanation?.trim() ?? '';
    diagnosisStore.setCurrentIndex(0);
    syncCurrentQuestion();
    if (!questions.value.length) {
      error.value = '暂时没有可用的诊断题目，请稍后重试。';
    }
  } catch (loadError) {
    error.value = loadError instanceof Error ? loadError.message : '题目加载失败，请稍后重试。';
  } finally {
    loading.value = false;
  }
}

onMounted(loadDiagnosis);
</script>

<template>
  <AppShell>
    <div class="mx-auto max-w-[860px] space-y-6 pb-10">
      <section class="app-hero">
        <h1 class="text-xl font-semibold tracking-[-0.02em] text-slate-900">
          {{ goalText || '为本次学习定制路径' }}
        </h1>
        <p class="mt-2 text-sm text-slate-600">
          {{ diagnosisExplanation || '下面几道题是围绕你的目标定制的，用于生成更适合你的学习路径。' }}
        </p>
        <p class="mt-1 text-sm text-slate-500">
          答完后将为你生成个性化学习路径。
        </p>
      </section>

      <LoadingState v-if="loading" />

      <template v-else-if="!error && currentQuestion">
        <ProgressIndicator :current="currentStep" :total="totalQuestions" />
        <QuestionCard :question="currentQuestion" :model-value="currentAnswer" @update:model-value="updateAnswer" />
        <DiagnosisFooter
          :disabled="buttonDisabled"
          :loading="submitting"
          :is-last="isLastQuestion"
          @continue="handleContinue"
        />
      </template>

      <div v-else class="space-y-4">
        <ErrorState :message="error || '页面加载失败，请稍后重试。'" />
        <button class="app-btn app-btn-secondary app-btn-md" type="button" @click="loadDiagnosis">
          重新加载
        </button>
      </div>
    </div>
  </AppShell>
</template>
