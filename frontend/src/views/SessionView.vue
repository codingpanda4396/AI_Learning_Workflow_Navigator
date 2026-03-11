<script setup lang="ts">
import { computed, onMounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import AppShell from '@/components/common/AppShell.vue';
import ErrorState from '@/components/common/ErrorState.vue';
import LoadingState from '@/components/common/LoadingState.vue';
import PageSection from '@/components/common/PageSection.vue';
import PrimaryActionCard from '@/components/common/PrimaryActionCard.vue';
import ProgressSummary from '@/components/common/ProgressSummary.vue';
import SecondaryInfoCard from '@/components/common/SecondaryInfoCard.vue';
import StagePill from '@/components/common/StagePill.vue';
import { formatStage } from '@/utils/format';
import { useFeedbackStore } from '@/stores/feedback';
import { useSessionStore } from '@/stores/session';

const route = useRoute();
const router = useRouter();
const sessionStore = useSessionStore();
const feedbackStore = useFeedbackStore();

const sessionId = computed(() => Number(route.params.sessionId));
const overview = computed(() => sessionStore.overview);
const report = computed(() => feedbackStore.report);

const currentTaskTitle = computed(() => {
  const stage = overview.value?.nextTask?.stage || overview.value?.currentStage;
  if (!stage) {
    return '等待系统生成下一步';
  }
  return `当前建议先完成：${formatStage(stage)}`;
});

const currentTaskGoal = computed(() => {
  const stage = overview.value?.nextTask?.stage || overview.value?.currentStage;
  switch (stage) {
    case 'STRUCTURE':
      return '先搭起这一章的知识框架，知道要学哪些关键概念。';
    case 'UNDERSTANDING':
      return '把原理真正讲清楚，确认你不是只记住结论。';
    case 'TRAINING':
      return '用题目检查自己是否真的掌握，并找出不稳的地方。';
    case 'REFLECTION':
      return '结合刚才的结果复盘原因，把易错点补上。';
    case 'EVALUATE':
      return '查看这一轮学习反馈，决定接下来该继续还是巩固。';
    default:
      return '系统正在为你安排下一步，请稍后刷新。';
  }
});

const nextStepText = computed(() => {
  const stage = overview.value?.nextTask?.stage || overview.value?.currentStage;
  if (stage === 'TRAINING') {
    return '完成后会进入检测与反馈。';
  }
  if (stage === 'EVALUATE') {
    return '完成后会看到这轮学习报告。';
  }
  return '完成后系统会继续推进到下一步学习。';
});

const learningGoal = computed(() => overview.value?.goalText || '这轮学习还没有补充目标说明。');

const recentTrainingSummary = computed(() => {
  if (!report.value) {
    return '完成检测后，这里会汇总你最近一次训练的表现。';
  }
  return report.value.overallSummary || report.value.diagnosisSummary || '最近一次训练已经完成，可以继续往下走。';
});

const progressSummaryText = computed(() => {
  const progress = overview.value?.progress;
  if (!progress) {
    return '系统正在整理当前进度。';
  }
  return `这轮学习已经完成 ${progress.completedTaskCount} 步，共规划 ${progress.totalTaskCount} 步。`;
});

const simpleTimeline = computed(() =>
  (overview.value?.timeline ?? []).slice(0, 5).map((item) => `${formatStage(item.stage)} · ${item.status || '待开始'}`),
);

async function continueCurrentLearning() {
  const nextTaskId = overview.value?.nextTask?.taskId;
  if (nextTaskId) {
    await router.push(`/tasks/${nextTaskId}/run`);
    return;
  }
  if (overview.value?.currentStage === 'TRAINING') {
    await router.push(`/sessions/${sessionId.value}/quiz`);
    return;
  }
  if (overview.value?.currentStage === 'EVALUATE') {
    await router.push(`/sessions/${sessionId.value}/report`);
  }
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
    <div v-else-if="overview" class="space-y-6">
      <PageSection compact>
        <div class="grid gap-3 rounded-[1.6rem] border border-slate-200 bg-slate-50 p-4 md:grid-cols-3 md:p-5">
          <div>
            <p class="text-xs font-semibold uppercase tracking-[0.18em] text-slate-400">当前课程</p>
            <p class="mt-2 text-sm font-medium text-slate-900">{{ overview.courseId }} / {{ overview.chapterId }}</p>
          </div>
          <div>
            <p class="text-xs font-semibold uppercase tracking-[0.18em] text-slate-400">当前阶段</p>
            <div class="mt-2">
              <StagePill :stage="overview.currentStage" />
            </div>
          </div>
          <div>
            <p class="text-xs font-semibold uppercase tracking-[0.18em] text-slate-400">当前进度</p>
            <p class="mt-2 text-sm font-medium text-slate-900">{{ progressSummaryText }}</p>
          </div>
        </div>
      </PageSection>

      <PrimaryActionCard
        eyebrow="当前学习导航"
        :title="currentTaskTitle"
        :description="currentTaskGoal"
        :helper="`当前属于 ${formatStage(overview.nextTask?.stage || overview.currentStage)}。${nextStepText}`"
        button-label="继续当前学习"
        :disabled="!overview.nextTask && overview.currentStage !== 'TRAINING' && overview.currentStage !== 'EVALUATE'"
        @action="continueCurrentLearning"
      />

      <div class="grid gap-5 lg:grid-cols-3">
        <ProgressSummary :progress="overview.progress" />

        <SecondaryInfoCard title="最近一次训练结果摘要" :description="recentTrainingSummary" />

        <SecondaryInfoCard title="学习目标" :description="learningGoal" />
      </div>

      <PageSection v-if="simpleTimeline.length" compact title="已安排的学习步骤" description="这里只保留简化视图，帮助你快速确认这轮学习的大致顺序。">
        <div class="grid gap-3 md:grid-cols-2">
          <div v-for="(item, index) in simpleTimeline" :key="`${item}-${index}`" class="rounded-2xl border border-slate-200 bg-white p-4 text-sm text-slate-700">
            {{ item }}
          </div>
        </div>
      </PageSection>
    </div>
  </AppShell>
</template>
