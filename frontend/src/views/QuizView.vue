<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, reactive } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import AppShell from '@/components/common/AppShell.vue';
import EmptyState from '@/components/common/EmptyState.vue';
import ErrorState from '@/components/common/ErrorState.vue';
import LoadingState from '@/components/common/LoadingState.vue';
import QuizQuestionCard from '@/components/common/QuizQuestionCard.vue';
import { useQuizStore } from '@/stores/quiz';

const route = useRoute();
const router = useRouter();
const quizStore = useQuizStore();
const sessionId = computed(() => Number(route.params.sessionId));
const answers = reactive<Record<number, string>>({});
let timer: number | undefined;

const statusLabel = computed(() => {
  switch (quizStore.status) {
    case 'generating':
      return '生成中';
    case 'ready':
      return '可作答';
    case 'submitting':
      return '提交中';
    case 'completed':
      return '已完成';
    case 'error':
      return '生成失败';
    default:
      return '未生成';
  }
});

function startPolling() {
  if (timer) {
    window.clearInterval(timer);
  }
  timer = window.setInterval(async () => {
    const snapshot = await quizStore.fetchQuizStatus(sessionId.value);
    const done = snapshot.generationStatus === 'SUCCEEDED' || snapshot.quizStatus === 'READY';
    if (done) {
      if (timer) {
        window.clearInterval(timer);
      }
      await quizStore.fetchQuiz(sessionId.value);
    }
  }, 2000);
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
  await router.push(`/sessions/${sessionId.value}/report`);
}

onMounted(async () => {
  try {
    const status = await quizStore.fetchQuizStatus(sessionId.value);
    if (status.generationStatus === 'SUCCEEDED' || status.quizStatus === 'READY') {
      await quizStore.fetchQuiz(sessionId.value);
    } else if (status.generationStatus === 'PENDING' || status.generationStatus === 'RUNNING') {
      startPolling();
    }
  } catch {
    quizStore.status = 'idle';
  }
});

onBeforeUnmount(() => {
  if (timer) {
    window.clearInterval(timer);
  }
});
</script>

<template>
  <AppShell>
    <div class="space-y-6">
      <section class="rounded-[2rem] bg-white p-6 shadow-sm ring-1 ring-slate-200">
        <div class="flex flex-wrap items-center justify-between gap-4">
          <div>
            <p class="text-xs uppercase tracking-[0.24em] text-slate-400">Quiz</p>
            <h2 class="mt-2 text-3xl font-semibold text-slate-900">训练检测</h2>
            <p class="mt-3 text-sm text-slate-600">状态：{{ statusLabel }}</p>
          </div>
          <button
            class="rounded-2xl bg-slate-900 px-5 py-3 text-sm font-medium text-white disabled:opacity-60"
            :disabled="quizStore.loading || quizStore.status === 'generating'"
            @click="generate"
          >
            {{ quizStore.quiz ? '重新生成题目' : '生成训练题' }}
          </button>
        </div>
      </section>

      <LoadingState v-if="quizStore.loading && !quizStore.quiz" />
      <ErrorState v-else-if="quizStore.error && !quizStore.quiz" :message="quizStore.error" />
      <section v-else-if="quizStore.quiz?.questions.length" class="space-y-5">
        <QuizQuestionCard
          v-for="(question, index) in quizStore.quiz.questions"
          :key="question.questionId"
          v-model="answers[question.questionId]"
          :index="index"
          :question="question"
        />

        <div class="flex justify-end">
          <button
            class="rounded-2xl bg-slate-900 px-6 py-3 text-sm font-medium text-white disabled:opacity-60"
            :disabled="quizStore.submitting"
            @click="submit"
          >
            提交答案
          </button>
        </div>
      </section>

      <EmptyState
        v-else
        title="当前还没有题目"
        description="点击上方按钮触发 quiz 生成。若后端正在生成，页面会自动轮询状态。"
      />
    </div>
  </AppShell>
</template>
