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

const conclusion = computed(() => {
  if (!report.value) {
    return '本轮学习反馈正在生成。';
  }
  return report.value.overallSummary || report.value.diagnosisSummary || '你已经完成了这一轮学习，可以根据建议进入下一步。';
});

const strengths = computed(() => (report.value?.strengths?.length ? report.value.strengths.slice(0, 4) : ['你已经完成了当前检测，系统会继续根据结果补全优势总结。']));
const weaknesses = computed(() => (report.value?.weaknesses?.length ? report.value.weaknesses.slice(0, 4) : report.value?.reviewFocus?.slice(0, 4) || ['目前还没有明确弱项，可以直接进入下一步。']));
const nextStepAction = computed(() => report.value?.nextStep?.recommendedAction || report.value?.suggestedNextAction || report.value?.recommendedAction || 'NEXT');
const nextStepTitle = computed(() => {
  switch (nextStepAction.value) {
    case 'REVIEW':
      return '先回到总览，继续巩固不稳定的内容';
    case 'QUIZ':
    case 'TRAINING':
      return '进入下一轮检测';
    default:
      return '进入下一步';
  }
});
const nextStepReason = computed(() => report.value?.nextStep?.reason || report.value?.nextRoundAdvice || '系统会根据这轮表现继续安排最合适的后续学习。');

async function submitPrimaryAction() {
  const action = nextStepAction.value || 'NEXT';
  try {
    await feedbackStore.submitNextAction(sessionId.value, action);
  } catch {
    // Keep navigation available even if submit fails.
  }

  if (action.includes('QUIZ') || action.includes('TRAIN')) {
    await router.push(`/sessions/${sessionId.value}/quiz`);
    return;
  }

  await router.push(`/sessions/${sessionId.value}`);
}

async function goSession() {
  await router.push(`/sessions/${sessionId.value}`);
}

async function goGrowth() {
  await router.push(`/sessions/${sessionId.value}/growth`);
}

onMounted(async () => {
  await feedbackStore.fetchReport(sessionId.value);
});
</script>

<template>
  <AppShell>
    <LoadingState v-if="feedbackStore.loading && !report" />
    <ErrorState v-else-if="feedbackStore.error && !report" :message="feedbackStore.error" />
    <div v-else-if="report" class="space-y-6">
      <PageSection eyebrow="学习报告" title="这一轮学习反馈" :description="conclusion" />

      <ReportBlock title="总体结论" tone="highlight" :description="conclusion" />
      <ReportBlock title="学会了什么" :items="strengths" />
      <ReportBlock title="还不稳定的地方" :items="weaknesses" />
      <ReportBlock title="系统建议下一步做什么" :description="nextStepReason">
        <p class="text-sm font-semibold text-slate-900">{{ nextStepTitle }}</p>
        <p class="mt-3 text-sm leading-7 text-slate-600">{{ nextStepReason }}</p>
      </ReportBlock>

      <NextStepCard
        :title="nextStepTitle"
        :reason="nextStepReason"
        action-label="进入下一步"
        secondary-label="返回学习总览"
        tertiary-label="查看成长记录"
        :loading="feedbackStore.loading"
        @primary="submitPrimaryAction"
        @secondary="goSession"
        @tertiary="goGrowth"
      />
    </div>
  </AppShell>
</template>
