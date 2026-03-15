<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, reactive } from 'vue';
import { useRoute } from 'vue-router';
import AppShell from '@/components/common/AppShell.vue';
import ErrorState from '@/components/common/ErrorState.vue';
import LoadingState from '@/components/common/LoadingState.vue';
import QuizQuestionCard from '@/components/common/QuizQuestionCard.vue';
import AppButton from '@/components/ui/AppButton.vue';
import EmptyStatePanel from '@/components/ui/EmptyStatePanel.vue';
import InfoHint from '@/components/ui/InfoHint.vue';
import StepProgress from '@/components/ui/StepProgress.vue';
import { useQuizStore } from '@/stores/quiz';
import { useLearningFlowStore } from '@/stores/learningFlow';

const route = useRoute();
const quizStore = useQuizStore();
const flowStore = useLearningFlowStore();
const sessionId = computed(() => Number(route.params.sessionId));
const answers = reactive<Record<number, string>>({});
let timer: number | undefined;

const statusText = computed(() => {
  switch (quizStore.status) {
    case 'generating':
      return '正在准备练习题目。';
    case 'submitting':
      return '正在提交，即将进入学习结果评估。';
    case 'answering':
      return '作答完成后点击下方提交。';
    case 'reviewing':
      return '已进入复盘。';
    case 'report-ready':
      return '已就绪，即将跳转。';
    case 'next-round':
      return '已就绪。';
    case 'failed':
      return '练习暂时不可用，请稍后重试。';
    default:
      return '';
  }
});

const questionCount = computed(() => quizStore.quiz?.questions.length ?? 0);
const answeredCount = computed(() =>
  Object.values(answers).filter((item) => String(item || '').trim().length > 0).length,
);

function stopPolling() {
  if (timer) {
    window.clearInterval(timer);
    timer = undefined;
  }
}

async function handleSnapshot() {
  const snapshot = await quizStore.fetchQuizStatus(sessionId.value);
  if (snapshot.quizStatus === 'REPORT_READY' || snapshot.quizStatus === 'REVIEWING' || snapshot.quizStatus === 'NEXT_ROUND') {
    stopPolling();
    await flowStore.goToStage('EVALUATION');
    return;
  }
  if (snapshot.generationStatus === 'SUCCEEDED' && snapshot.quizStatus === 'READY') {
    stopPolling();
    await quizStore.fetchQuiz(sessionId.value);
  }
}

function startPolling() {
  stopPolling();
  timer = window.setInterval(handleSnapshot, 2000);
}

async function generate() {
  await quizStore.generateQuiz(sessionId.value);
  startPolling();
}

async function submit() {
  const payload = (quizStore.quiz?.questions ?? []).map((question) => ({
    questionId: question.questionId,
    answer: answers[question.questionId] || '',
  }));
  await quizStore.submitQuiz(sessionId.value, payload);
  await flowStore.goToStage('EVALUATION');
}

onMounted(async () => {
  try {
    const status = await quizStore.fetchQuizStatus(sessionId.value);
    if (status.quizStatus === 'REPORT_READY' || status.quizStatus === 'REVIEWING' || status.quizStatus === 'NEXT_ROUND') {
      await flowStore.goToStage('EVALUATION');
      return;
    }
    if (status.generationStatus === 'SUCCEEDED' && status.quizStatus === 'READY') {
      await quizStore.fetchQuiz(sessionId.value);
      return;
    }
    if (status.generationStatus === 'PENDING' || status.generationStatus === 'RUNNING' || status.quizStatus === 'GENERATING') {
      startPolling();
    }
  } catch {
    quizStore.status = 'idle';
  }
});

onBeforeUnmount(stopPolling);
</script>

<template>
  <AppShell>
    <div class="space-y-6 pb-28">
      <section class="app-hero">
        <p class="app-eyebrow">练习</p>
        <h1 class="app-title-lg mt-4">完成作答后提交，查看学习结果评估</h1>
        <p class="app-text-lead mt-4 max-w-2xl">
          独立完成下方题目，提交后系统会给出本轮的掌握情况与下一步建议。
        </p>
      </section>

      <StepProgress v-if="questionCount" :current="answeredCount" :total="questionCount" label="已完成" />
      <InfoHint v-if="statusText">{{ statusText }}</InfoHint>

      <LoadingState v-if="quizStore.loading && !quizStore.quiz" />
      <ErrorState v-else-if="quizStore.error && !quizStore.quiz" :message="quizStore.error" />

      <section v-else-if="quizStore.quiz?.questions.length" class="app-stack-md">
        <QuizQuestionCard
          v-for="(question, index) in quizStore.quiz.questions"
          :key="question.questionId"
          v-model="answers[question.questionId]"
          :index="index"
          :question="question"
        />
      </section>

      <EmptyStatePanel
        v-else
        title="练习尚未准备"
        description="点击下方按钮开始生成题目，准备完成后即可作答。"
      >
        <AppButton size="lg" :loading="quizStore.loading" @click="generate">开始练习</AppButton>
      </EmptyStatePanel>

      <div class="fixed inset-x-0 bottom-0 z-30 border-t border-slate-200/80 bg-white/88 backdrop-blur-xl">
        <div class="mx-auto flex max-w-[1120px] justify-end px-4 py-4 md:px-0">
          <AppButton
            v-if="quizStore.quiz?.questions.length"
            size="lg"
            :disabled="quizStore.submitting"
            :loading="quizStore.submitting"
            @click="submit"
          >
            提交练习
          </AppButton>
          <AppButton
            v-else
            size="lg"
            :disabled="quizStore.loading || quizStore.status === 'generating'"
            :loading="quizStore.status === 'generating'"
            @click="generate"
          >
            开始练习
          </AppButton>
        </div>
      </div>
    </div>
  </AppShell>
</template>
