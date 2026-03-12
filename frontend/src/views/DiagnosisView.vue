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
const goalText = computed(() => String(route.query.goal || '暂未提供学习目标，请先从首页创建学习任务后进入诊断。'));
const courseId = computed(() => String(route.query.course || '未填写课程'));
const chapterId = computed(() => String(route.query.chapter || '未填写章节'));
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

const currentStep = computed(() => (questions.value.length ? diagnosisStore.currentQuestionIndex + 1 : 0));
const submitButtonText = computed(() => diagnosisStore.nextAction?.label || '进入个性化学习路径');

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
  await diagnosisStore.submitDiagnosis();
}

async function retryGenerate() {
  if (!sessionId.value) {
    return;
  }
  await diagnosisStore.generateDiagnosis(sessionId.value);
}

async function enterPlanFlow() {
  const numericSessionId = Number(sessionId.value);
  if (!Number.isFinite(numericSessionId) || numericSessionId <= 0) {
    await router.push('/');
    return;
  }
  await router.push({
    path: '/plan',
    query: {
      sessionId: numericSessionId,
      goal: goalText.value,
      course: courseId.value,
      chapter: chapterId.value,
    },
  });
}

onMounted(async () => {
  diagnosisStore.reset();
  if (!sessionId.value) {
    diagnosisStore.error = '缺少学习会话信息，请先从首页创建学习目标。';
    return;
  }
  await diagnosisStore.generateDiagnosis(sessionId.value);
});
</script>

<template>
  <AppShell>
    <div class="space-y-6 pb-10">
      <DiagnosisGoalSummaryCard :session-id="sessionId" :goal="goalText" :course="courseId" :chapter="chapterId" />

      <LoadingState v-if="isGenerating" />

      <ErrorState v-else-if="isError" :message="diagnosisStore.error" />

      <template v-else-if="isResult && diagnosisStore.capabilityProfile">
        <CapabilityProfileCard :profile="diagnosisStore.capabilityProfile" />

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
        <div class="grid gap-6 lg:grid-cols-[minmax(0,1fr)_320px]">
          <DiagnosisQuestionCard :question="currentQuestion" :model-value="currentAnswer" @update:model-value="updateAnswer" />
          <DiagnosisProgressCard :current="currentStep" :total="questions.length" />
        </div>

        <div class="flex flex-col gap-3 rounded-[1.6rem] border border-slate-200 bg-white p-5 shadow-sm md:flex-row md:items-center md:justify-between">
          <p class="text-sm leading-6 text-slate-600">
            回答完成后，系统会生成能力画像，并把这份画像作为后续学习路径、Tutor 辅助和训练难度的参考。
          </p>
          <div class="flex flex-wrap gap-3">
            <button
              type="button"
              class="rounded-2xl border border-slate-200 px-4 py-2.5 text-sm font-medium text-slate-700 transition hover:bg-slate-50"
              :disabled="diagnosisStore.currentQuestionIndex === 0"
              @click="previousQuestion"
            >
              上一题
            </button>
            <button
              v-if="diagnosisStore.currentQuestionIndex < questions.length - 1"
              type="button"
              class="rounded-2xl bg-slate-950 px-4 py-2.5 text-sm font-semibold text-white transition hover:bg-slate-800 disabled:cursor-not-allowed disabled:bg-slate-300"
              :disabled="!isCurrentAnswered"
              @click="nextQuestion"
            >
              下一题
            </button>
            <button
              v-else
              type="button"
              class="rounded-2xl bg-sky-600 px-4 py-2.5 text-sm font-semibold text-white transition hover:bg-sky-500 disabled:cursor-not-allowed disabled:bg-slate-300"
              :disabled="!isCurrentAnswered || isSubmitting"
              @click="submitDiagnosis"
            >
              {{ isSubmitting ? '系统正在分析你的回答...' : '提交诊断' }}
            </button>
          </div>
        </div>
      </template>

      <div v-if="isSubmitting" class="rounded-[1.8rem] border border-sky-100 bg-sky-50 p-6 text-sm leading-7 text-sky-700">
        系统正在分析你的回答并构建能力画像，请稍等一下。
      </div>

      <div v-if="isError" class="flex justify-start">
        <button
          type="button"
          class="rounded-2xl border border-slate-200 px-4 py-2.5 text-sm font-medium text-slate-700 transition hover:bg-slate-50"
          @click="retryGenerate"
        >
          重新生成诊断问题
        </button>
      </div>
    </div>
  </AppShell>
</template>
