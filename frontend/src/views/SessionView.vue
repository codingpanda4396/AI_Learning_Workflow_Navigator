<script setup lang="ts">
import { computed, onMounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import AppShell from '@/components/common/AppShell.vue';
import ErrorState from '@/components/common/ErrorState.vue';
import LoadingState from '@/components/common/LoadingState.vue';
import InfoCard from '@/components/cards/InfoCard.vue';
import ProgressCard from '@/components/cards/ProgressCard.vue';
import StageBadge from '@/components/common/StageBadge.vue';
import TaskTimeline from '@/components/panels/TaskTimeline.vue';
import { formatStage } from '@/utils/format';
import { useFeedbackStore } from '@/stores/feedback';
import { useSessionStore } from '@/stores/session';

const route = useRoute();
const router = useRouter();
const sessionStore = useSessionStore();
const feedbackStore = useFeedbackStore();

const sessionId = computed(() => Number(route.params.sessionId));
const overview = computed(() => sessionStore.overview);
const nextActionLabel = computed(() => {
  if (overview.value?.currentStage === 'TRAINING') {
    return '去训练';
  }
  return '去查看报告';
});

const recentTrainingSummary = computed(() => {
  const report = feedbackStore.report;
  if (!report?.overallSummary && !report?.diagnosisSummary) {
    return '当前还没有训练反馈，可先进入当前任务。';
  }
  return report.overallSummary || report.diagnosisSummary || '已生成训练总结。';
});

async function openPrimary() {
  const taskId = overview.value?.nextTask?.taskId;
  if (!taskId) {
    return;
  }
  await router.push(`/tasks/${taskId}/run`);
}

async function openSecondary() {
  if (!overview.value) {
    return;
  }
  if (overview.value.currentStage === 'TRAINING') {
    await router.push(`/sessions/${sessionId.value}/quiz`);
    return;
  }
  await router.push(`/sessions/${sessionId.value}/report`);
}

onMounted(async () => {
  await sessionStore.fetchOverview(sessionId.value);
  try {
    await feedbackStore.fetchReport(sessionId.value);
  } catch {
    // Report may not exist yet.
  }
});
</script>

<template>
  <AppShell>
    <LoadingState v-if="sessionStore.loading && !overview" />
    <ErrorState v-else-if="sessionStore.error && !overview" :message="sessionStore.error" />
    <div v-else-if="overview" class="space-y-8">
      <section class="grid gap-4 md:grid-cols-2 xl:grid-cols-4">
        <InfoCard title="当前在学" :value="overview.chapterId" :hint="overview.courseId" />
        <InfoCard title="当前阶段" :value="formatStage(overview.currentStage)" :hint="`Session #${overview.sessionId}`" />
        <InfoCard title="下一任务" :value="overview.nextTask ? `#${overview.nextTask.taskId}` : '暂无'" :hint="overview.nextTask ? formatStage(overview.nextTask.stage) : '等待后端返回'" />
        <ProgressCard :progress="overview.progress" />
      </section>

      <section class="grid gap-8 lg:grid-cols-[1.15fr_0.85fr]">
        <TaskTimeline :items="overview.timeline" :current-stage="overview.currentStage" />

        <div class="space-y-6">
          <div class="rounded-[2rem] bg-white p-6 shadow-sm ring-1 ring-slate-200">
            <p class="text-sm text-slate-500">下一步该做什么</p>
            <div class="mt-3 flex items-center gap-3">
              <StageBadge :stage="overview.nextTask?.stage || overview.currentStage" />
              <span class="text-sm text-slate-600">
                {{ overview.nextTask ? `任务 #${overview.nextTask.taskId}` : '等待下一任务' }}
              </span>
            </div>
            <p class="mt-4 text-sm leading-7 text-slate-600">
              当前页只负责导航和摘要，不展开学习内容。进入任务页后再专注学习。
            </p>
            <div class="mt-6 flex flex-wrap gap-3">
              <button class="rounded-2xl bg-slate-900 px-5 py-3 text-sm font-medium text-white disabled:opacity-50" :disabled="!overview.nextTask" @click="openPrimary">
                去学习当前任务
              </button>
              <button class="rounded-2xl border border-slate-200 px-5 py-3 text-sm font-medium text-slate-700" @click="openSecondary">
                {{ nextActionLabel }}
              </button>
            </div>
          </div>

          <div class="rounded-[2rem] bg-white p-6 shadow-sm ring-1 ring-slate-200">
            <p class="text-sm text-slate-500">最近训练状态摘要</p>
            <p class="mt-3 text-sm leading-7 text-slate-600">{{ recentTrainingSummary }}</p>
          </div>

          <div class="rounded-[2rem] bg-white p-6 shadow-sm ring-1 ring-slate-200">
            <p class="text-sm text-slate-500">学习目标</p>
            <p class="mt-3 text-sm leading-7 text-slate-700">{{ overview.goalText || '未设置目标' }}</p>
          </div>
        </div>
      </section>
    </div>
  </AppShell>
</template>
