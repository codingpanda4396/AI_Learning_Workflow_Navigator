<script setup lang="ts">
import { computed, onMounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import AppShell from '@/components/common/AppShell.vue';
import ErrorState from '@/components/common/ErrorState.vue';
import LoadingState from '@/components/common/LoadingState.vue';
import NextStepCard from '@/components/common/NextStepCard.vue';
import PageSection from '@/components/common/PageSection.vue';
import ReportBlock from '@/components/common/ReportBlock.vue';
import { useFeedbackStore } from '@/stores/feedback';

const route = useRoute();
const router = useRouter();
const feedbackStore = useFeedbackStore();

const sessionId = computed(() => Number(route.params.sessionId));
const report = computed(() => feedbackStore.report);

const reportSummary = computed(() => report.value?.overallSummary || report.value?.diagnosisSummary || '最新报告暂未就绪。');
const strengths = computed(() => report.value?.strengths?.length ? report.value.strengths : ['暂时还没有稳定优势总结。']);
const weaknesses = computed(() => {
  if (report.value?.weakPoints?.length) {
    return report.value.weakPoints.slice(0, 5).map((item) => item.nodeName);
  }
  if (report.value?.weaknesses?.length) {
    return report.value.weaknesses;
  }
  if (report.value?.reviewFocus?.length) {
    return report.value.reviewFocus;
  }
  return ['暂时还没有薄弱点总结。'];
});
const nextStepTitle = computed(() => {
  const action = report.value?.nextStep?.recommendedAction || report.value?.recommendedAction || 'REVIEW';
  if (action === 'ADVANCE' || action === 'NEXT_ROUND') {
    return '进入下一学习步骤';
  }
  if (action === 'BACKTRACK') {
    return '先补齐前置基础';
  }
  return '复习并巩固当前节点';
});
const nextStepReason = computed(() => report.value?.nextStep?.reason || report.value?.nextRoundAdvice || '根据这份报告决定下一步最合适的动作。');

async function loadReport() {
  if (!sessionId.value) {
    return;
  }
  await feedbackStore.fetchReport(sessionId.value);
}

async function submitPrimaryAction() {
  const action = report.value?.recommendedAction || report.value?.nextStep?.recommendedAction || 'REVIEW';
  try {
    await feedbackStore.submitNextAction(sessionId.value, action);
  } catch {
    // Keep navigation available even if the action submit fails.
  }

  const targetTaskType = report.value?.nextStep?.targetTaskType || '';
  if (targetTaskType === 'TRAINING' || action === 'REINFORCE' || action === 'BACKTRACK') {
    await router.push(`/sessions/${sessionId.value}/quiz`);
    return;
  }

  await router.push(`/sessions/${sessionId.value}`);
}

onMounted(loadReport);
</script>

<template>
  <AppShell>
    <LoadingState v-if="feedbackStore.loading && !report" />

    <div v-else-if="feedbackStore.error && !report" class="space-y-4">
      <ErrorState :message="feedbackStore.error" />
      <button
        type="button"
        class="rounded-2xl border border-slate-200 px-4 py-2.5 text-sm font-medium text-slate-700 transition hover:bg-slate-50"
        @click="loadReport"
      >
        重试加载报告
      </button>
    </div>

    <div v-else-if="report" class="space-y-6">
      <PageSection eyebrow="会话报告" title="最新学习报告" :description="reportSummary" />

      <ReportBlock title="总结" tone="highlight" :description="reportSummary" />
      <ReportBlock title="优势" :items="strengths" />
      <ReportBlock title="薄弱点" :items="weaknesses" />
      <ReportBlock title="下一步" :description="nextStepReason">
        <p class="text-sm font-semibold text-slate-900">{{ nextStepTitle }}</p>
        <p class="mt-3 text-sm leading-7 text-slate-600">{{ nextStepReason }}</p>
      </ReportBlock>

      <NextStepCard
        :title="nextStepTitle"
        :reason="nextStepReason"
        action-label="应用下一步"
        secondary-label="返回会话"
        tertiary-label="成长看板"
        :loading="feedbackStore.loading"
        @primary="submitPrimaryAction"
        @secondary="router.push(`/sessions/${sessionId}`)"
        @tertiary="router.push(`/sessions/${sessionId}/growth`)"
      />
    </div>
  </AppShell>
</template>
