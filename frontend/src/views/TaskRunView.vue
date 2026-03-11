<script setup lang="ts">
import { computed, onMounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import AppShell from '@/components/common/AppShell.vue';
import ErrorState from '@/components/common/ErrorState.vue';
import LoadingState from '@/components/common/LoadingState.vue';
import StageBadge from '@/components/common/StageBadge.vue';
import { formatOutputContent } from '@/utils/format';
import { useTaskStore } from '@/stores/task';

const route = useRoute();
const router = useRouter();
const taskStore = useTaskStore();

const taskId = computed(() => Number(route.params.taskId));
const detail = computed(() => taskStore.currentTaskDetail);
const result = computed(() => taskStore.currentTaskResult);
const isTraining = computed(() => (result.value?.stage || detail.value?.stage) === 'TRAINING');

async function backToSession() {
  const sessionId = detail.value?.sessionId;
  if (sessionId) {
    await router.push(`/sessions/${sessionId}`);
  }
}

async function goQuiz() {
  const sessionId = detail.value?.sessionId;
  if (sessionId) {
    await router.push(`/sessions/${sessionId}/quiz`);
  }
}

onMounted(async () => {
  await taskStore.fetchTaskDetail(taskId.value);
  await taskStore.runTask(taskId.value);
});
</script>

<template>
  <AppShell>
    <LoadingState v-if="taskStore.loading && !result" />
    <ErrorState v-else-if="taskStore.error && !result" :message="taskStore.error" />
    <div v-else class="grid gap-8 lg:grid-cols-[1.3fr_0.7fr]">
      <section class="rounded-[2rem] bg-white p-8 shadow-sm ring-1 ring-slate-200">
        <div class="flex items-center justify-between gap-4">
          <div>
            <p class="text-xs uppercase tracking-[0.24em] text-slate-400">专注学习区</p>
            <h2 class="mt-3 text-3xl font-semibold text-slate-900">{{ detail?.objective || `任务 #${taskId}` }}</h2>
          </div>
          <StageBadge :stage="result?.stage || detail?.stage" />
        </div>

        <pre class="mt-8 overflow-auto rounded-[1.5rem] bg-slate-950 p-6 text-sm leading-7 text-slate-100">{{ formatOutputContent(result?.output || detail?.output) }}</pre>
      </section>

      <aside class="space-y-6">
        <div class="rounded-[2rem] bg-white p-6 shadow-sm ring-1 ring-slate-200">
          <p class="text-sm text-slate-500">当前 session</p>
          <p class="mt-2 text-xl font-semibold text-slate-900">#{{ detail?.sessionId || '--' }}</p>
          <p class="mt-4 text-sm text-slate-500">当前 task stage</p>
          <p class="mt-2 text-base font-medium text-slate-900">{{ result?.stage || detail?.stage || '--' }}</p>
        </div>

        <div class="rounded-[2rem] bg-white p-6 shadow-sm ring-1 ring-slate-200">
          <button class="w-full rounded-2xl border border-slate-200 px-4 py-3 text-sm font-medium text-slate-700" @click="backToSession">
            返回总览
          </button>
          <button
            v-if="isTraining"
            class="mt-3 w-full rounded-2xl bg-slate-900 px-4 py-3 text-sm font-medium text-white"
            @click="goQuiz"
          >
            进入检测
          </button>
          <button
            v-else
            class="mt-3 w-full rounded-2xl bg-slate-100 px-4 py-3 text-sm font-medium text-slate-700"
            @click="backToSession"
          >
            返回总览，继续下一步
          </button>
        </div>
      </aside>
    </div>
  </AppShell>
</template>
