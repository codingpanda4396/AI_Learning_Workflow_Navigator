<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, reactive } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import AppShell from '@/components/common/AppShell.vue';
import EmptyState from '@/components/common/EmptyState.vue';
import ErrorState from '@/components/common/ErrorState.vue';
import LoadingState from '@/components/common/LoadingState.vue';
import PageSection from '@/components/common/PageSection.vue';
import QuizQuestionCard from '@/components/common/QuizQuestionCard.vue';
import { useQuizStore } from '@/stores/quiz';

const route = useRoute();
const router = useRouter();
const quizStore = useQuizStore();
const sessionId = computed(() => Number(route.params.sessionId));
const answers = reactive<Record<number, string>>({});
let timer: number | undefined;

const statusText = computed(() => {
  switch (quizStore.status) {
    case 'generating':
      return '正在生成题目';
    case 'submitting':
      return '正在评估答案';
    case 'completed':
      return '检测已完成';
    case 'error':
      return '检测暂时不可用';
    default:
      return '';
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
    <div class="space-y-6 pb-26">
      <PageSection
        eyebrow="在线检测"
        title="完成检测后，系统会生成学习反馈"
        description="先独立作答，再根据反馈确认你已经掌握了什么、还需要继续巩固什么。"
      >
        <div v-if="statusText" class="rounded-2xl border border-slate-200 bg-slate-50 px-4 py-3 text-sm text-slate-600">
          {{ statusText }}
        </div>
      </PageSection>

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
      </section>
      <EmptyState
        v-else
        title="检测题目还没准备好"
        description="点击下方按钮开始生成题目。生成完成后，页面会自动切换到答题状态。"
      >
        <button class="rounded-2xl bg-slate-950 px-5 py-3 text-sm font-semibold text-white transition hover:bg-slate-800 disabled:opacity-60" :disabled="quizStore.loading" @click="generate">
          开始生成题目
        </button>
      </EmptyState>

      <div class="fixed inset-x-0 bottom-0 z-30 border-t border-slate-200 bg-white/95 backdrop-blur">
        <div class="mx-auto flex max-w-6xl justify-end px-5 py-4 md:px-6">
          <button
            v-if="quizStore.quiz?.questions.length"
            class="rounded-2xl bg-slate-950 px-6 py-3 text-sm font-semibold text-white transition hover:bg-slate-800 disabled:cursor-not-allowed disabled:opacity-60"
            :disabled="quizStore.submitting"
            @click="submit"
          >
            提交答案
          </button>
          <button
            v-else
            class="rounded-2xl bg-slate-950 px-6 py-3 text-sm font-semibold text-white transition hover:bg-slate-800 disabled:opacity-60"
            :disabled="quizStore.loading || quizStore.status === 'generating'"
            @click="generate"
          >
            {{ quizStore.status === 'generating' ? '正在生成题目' : '开始生成题目' }}
          </button>
        </div>
      </div>
    </div>
  </AppShell>
</template>
