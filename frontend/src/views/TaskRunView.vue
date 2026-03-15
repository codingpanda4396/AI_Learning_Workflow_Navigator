<script setup lang="ts">
import { computed, onMounted } from 'vue';
import { useRoute } from 'vue-router';
import AppShell from '@/components/common/AppShell.vue';
import ErrorState from '@/components/common/ErrorState.vue';
import LearningContentSection from '@/components/common/LearningContentSection.vue';
import LoadingState from '@/components/common/LoadingState.vue';
import AppButton from '@/components/ui/AppButton.vue';
import { getPreviewMetricsSnapshot, trackFirstTaskCompleted } from '@/utils/previewMetrics';
import { useTaskStore } from '@/stores/task';
import { useLearningFlowStore } from '@/stores/learningFlow';
import { normalizeLearningContent } from '@/utils/taskContent';

const route = useRoute();
const taskStore = useTaskStore();
const flowStore = useLearningFlowStore();

const isLearnRoute = computed(() => route.name === 'learn-task');
const sessionId = computed(() =>
  isLearnRoute.value ? Number(route.params.sessionId) : (taskStore.currentTaskDetail?.sessionId ?? 0),
);
const taskId = computed(() =>
  isLearnRoute.value ? (flowStore.snapshot?.currentTaskId ?? taskStore.currentTaskDetail?.taskId ?? 0) : Number(route.params.taskId),
);

const detail = computed(() => taskStore.currentTaskDetail);
const result = computed(() => taskStore.currentTaskResult);
const isTraining = computed(() => (result.value?.stage || detail.value?.stage) === 'TRAINING');

const content = computed(() =>
  normalizeLearningContent(result.value?.output ?? detail.value?.output, detail.value?.objective || ''),
);

function backToProgress() {
  flowStore.goToStage('NEXT_ACTION');
}

async function goPrimary() {
  trackFirstTaskCompleted(taskId.value);
  console.info('[metrics] first task progress', getPreviewMetricsSnapshot());
  if (isTraining.value) {
    await flowStore.goToStage('TRAINING');
    return;
  }
  await flowStore.goToStage('NEXT_ACTION');
}

onMounted(async () => {
  if (isLearnRoute.value && sessionId.value) {
    await flowStore.ensureCurrentTaskLoaded();
  } else {
    const tid = Number(route.params.taskId);
    if (Number.isFinite(tid)) {
      await taskStore.fetchTaskDetail(tid);
      await taskStore.runTask(tid);
    }
  }
});
</script>

<template>
  <AppShell>
    <LoadingState v-if="taskStore.loading && !result" />
    <ErrorState v-else-if="taskStore.error && !result" :message="taskStore.error" />

    <div v-else class="mx-auto max-w-[720px] space-y-8">
      <section class="app-hero">
        <p class="app-eyebrow">当前学习任务</p>
        <h1 class="app-title-lg mt-4">{{ content.title }}</h1>
        <p class="app-text-lead mt-4">{{ content.summary || '完成本步后进入练习。' }}</p>
      </section>

      <LearningContentSection title="这一步的重点" :description="detail?.objective || content.summary" />
      <LearningContentSection title="建议顺序" :items="content.suggestedSequence" numbered />

      <div class="rounded-2xl border border-slate-200/80 bg-slate-50/50 p-5">
        <p class="text-sm text-slate-600">完成后将进入练习，系统会根据结果给出评估与下一步建议。</p>
      </div>

      <div class="flex flex-col gap-3">
        <AppButton size="lg" block @click="goPrimary">
          {{ isTraining ? '进入练习' : '我已完成，进入练习' }}
        </AppButton>
        <AppButton variant="secondary" block @click="backToProgress">返回当前进度</AppButton>
      </div>
    </div>
  </AppShell>
</template>
