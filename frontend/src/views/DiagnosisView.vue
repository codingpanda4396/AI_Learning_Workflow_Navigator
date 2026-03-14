<script setup lang="ts">
import { computed, onMounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import AppShell from '@/components/common/AppShell.vue';
import ErrorState from '@/components/common/ErrorState.vue';
import LoadingState from '@/components/common/LoadingState.vue';
import PrimaryActionCard from '@/components/common/PrimaryActionCard.vue';
import CapabilityProfileCard from '@/components/diagnosis/CapabilityProfileCard.vue';
import DiagnosisGoalSummaryCard from '@/components/diagnosis/DiagnosisGoalSummaryCard.vue';
import DiagnosisProgressCard from '@/components/diagnosis/DiagnosisProgressCard.vue';
import DiagnosisQuestionCard from '@/components/diagnosis/DiagnosisQuestionCard.vue';
import { useDiagnosisStore } from '@/stores/diagnosis';
import {
  resolveDiagnosisFallbackText,
  resolveDiagnosisMetaSummary,
  resolveDiagnosisSourceLabel,
  resolveDiagnosisStatusLabel,
} from '@/types/diagnosis';
import type { DiagnosisAnswerValue, DiagnosisQuestion } from '@/types/diagnosis';

const route = useRoute();
const router = useRouter();
const diagnosisStore = useDiagnosisStore();

const sessionId = computed(() => String(route.params.sessionId || route.query.sessionId || '').trim());
const goalText = computed(() => String(route.query.goal || '暂未提供本次学习目标'));
const courseId = computed(() => String(route.query.course || '暂未提供课程信息'));
const chapterId = computed(() => String(route.query.chapter || '暂未提供章节信息'));
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
const submitButtonText = computed(() => diagnosisStore.nextAction?.label || '进入个性化学习路径');
const statusText = computed(() => resolveDiagnosisStatusLabel(diagnosisStore.status));
const sourceText = computed(() => resolveDiagnosisSourceLabel(diagnosisStore.fallback?.contentSource));
const fallbackText = computed(() => resolveDiagnosisFallbackText(diagnosisStore.fallback));
const helperMetaText = computed(() => resolveDiagnosisMetaSummary(diagnosisStore.metadata));

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
    diagnosisStore.error = '缺少生成能力诊断所需的会话记录。';
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
          :status="statusText"
        />

        <PrimaryActionCard
          eyebrow="建议下一步"
          title="进入个性化学习路径"
          description="系统已经完成当前能力快照，接下来会结合你的目标、节奏和当前水平，生成更适合你的学习路径。"
          :helper="diagnosisStore.nextAction?.label || '确认后进入下一步学习规划。'"
          :button-label="submitButtonText"
          @action="enterPlanFlow"
        />

        <section class="rounded-[1.6rem] border border-slate-200 bg-slate-50/80 p-5 text-sm text-slate-500">
          <p class="text-xs font-semibold uppercase tracking-[0.18em] text-slate-400">辅助信息</p>
          <div class="mt-4 grid gap-3 md:grid-cols-3">
            <div class="rounded-2xl border border-slate-200 bg-white/80 p-4">
              <p class="text-xs text-slate-400">生成方式</p>
              <p class="mt-2 font-medium leading-6 text-slate-700">{{ sourceText }}</p>
            </div>
            <div class="rounded-2xl border border-slate-200 bg-white/80 p-4">
              <p class="text-xs text-slate-400">生成说明</p>
              <p class="mt-2 font-medium leading-6 text-slate-700">{{ fallbackText }}</p>
            </div>
            <div class="rounded-2xl border border-slate-200 bg-white/80 p-4">
              <p class="text-xs text-slate-400">诊断辅助信息</p>
              <p class="mt-2 font-medium leading-6 text-slate-700">{{ helperMetaText }}</p>
            </div>
          </div>
        </section>
      </template>

      <template v-else-if="currentQuestion">
        <div class="grid gap-6 lg:grid-cols-[minmax(0,1fr)_320px]">
          <DiagnosisQuestionCard :question="currentQuestion" :model-value="currentAnswer" @update:model-value="updateAnswer" />
          <div class="space-y-4">
            <DiagnosisProgressCard :current="currentStep" :total="questions.length" />
            <div class="rounded-[1.6rem] border border-slate-200 bg-slate-50/90 p-5 text-sm leading-6 text-slate-600">
              <p class="text-xs font-semibold uppercase tracking-[0.18em] text-slate-400">辅助说明</p>
              <div class="mt-3 space-y-3">
                <div>
                  <p class="text-xs text-slate-400">诊断状态</p>
                  <p class="mt-1 font-medium text-slate-700">{{ statusText }}</p>
                </div>
                <div>
                  <p class="text-xs text-slate-400">生成方式</p>
                  <p class="mt-1 font-medium text-slate-700">{{ sourceText }}</p>
                </div>
                <div>
                  <p class="text-xs text-slate-400">生成说明</p>
                  <p class="mt-1 font-medium text-slate-700">{{ fallbackText }}</p>
                </div>
                <div>
                  <p class="text-xs text-slate-400">诊断辅助信息</p>
                  <p class="mt-1 font-medium text-slate-700">{{ helperMetaText }}</p>
                </div>
              </div>
            </div>
          </div>
        </div>

        <div class="flex flex-col gap-3 rounded-[1.6rem] border border-slate-200 bg-white p-5 shadow-sm md:flex-row md:items-center md:justify-between">
          <p class="text-sm leading-6 text-slate-600">
            按你的实际情况回答即可。系统会用这份信息生成能力快照，并据此安排后续学习路径。
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
              {{ isSubmitting ? '正在生成能力快照...' : '完成诊断并生成快照' }}
            </button>
          </div>
        </div>
      </template>

      <div v-if="isSubmitting" class="rounded-[1.8rem] border border-sky-100 bg-sky-50 p-6 text-sm leading-7 text-sky-700">
        系统正在根据你的回答生成能力快照，并准备下一步个性化学习路径。
      </div>

      <div v-if="isError" class="flex justify-start">
        <button
          type="button"
          class="rounded-2xl border border-slate-200 px-4 py-2.5 text-sm font-medium text-slate-700 transition hover:bg-slate-50"
          @click="retryCurrentAction"
        >
          {{ canRetrySubmit ? '重新提交诊断' : '重新生成诊断' }}
        </button>
      </div>
    </div>
  </AppShell>
</template>
