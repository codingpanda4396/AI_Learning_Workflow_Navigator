<script setup lang="ts">
import { computed, onMounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import AppShell from '@/components/common/AppShell.vue';
import ErrorState from '@/components/common/ErrorState.vue';
import LoadingState from '@/components/common/LoadingState.vue';
import AppButton from '@/components/ui/AppButton.vue';
import SectionCard from '@/components/ui/SectionCard.vue';
import { useFeedbackStore } from '@/stores/feedback';
import { formatPercent } from '@/utils/format';

const route = useRoute();
const router = useRouter();
const feedbackStore = useFeedbackStore();

const sessionId = computed(() => Number(route.params.sessionId));
const report = computed(() => feedbackStore.report);

const reportSummary = computed(() => report.value?.overallSummary || report.value?.diagnosisSummary || '最新反馈还在整理中。');
const strengths = computed(() => report.value?.strengths?.length ? report.value.strengths : ['这一轮还没有稳定优势总结。']);
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
  return ['这一轮还没有明确的薄弱点总结。'];
});
const nextStepTitle = computed(() => {
  const action = report.value?.nextStep?.recommendedAction || report.value?.recommendedAction || 'REVIEW';
  if (action === 'ADVANCE' || action === 'NEXT_ROUND') return '进入下一步学习';
  if (action === 'BACKTRACK') return '先补一块基础';
  return '先复习这一块';
});
const nextStepReason = computed(() => report.value?.nextStep?.reason || report.value?.nextRoundAdvice || '按这份反馈继续往下走会更稳。');
const score = computed(() => formatPercent(report.value?.overallAccuracy ?? report.value?.overallScore));
const stepEvidence = computed(() => report.value?.stepEvidence?.slice(0, 6) ?? []);

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
    return;
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
      <AppButton variant="secondary" @click="loadReport">重新加载反馈</AppButton>
    </div>

    <div v-else-if="report" class="mx-auto max-w-[920px] space-y-6">
      <section class="app-hero">
        <div class="flex flex-col gap-8 lg:flex-row lg:items-end lg:justify-between">
          <div class="max-w-2xl">
            <p class="app-eyebrow">学习反馈</p>
            <h1 class="app-title-lg mt-4">{{ nextStepTitle }}</h1>
            <p class="app-text-lead mt-4">{{ reportSummary }}</p>
          </div>
          <div class="app-stat min-w-[180px]">
            <p class="app-stat-label">这轮结果</p>
            <p class="app-stat-value">{{ score }}</p>
          </div>
        </div>
      </section>

      <SectionCard strong title="先看结论" :description="nextStepReason">
        <div class="flex flex-wrap gap-3">
          <AppButton size="lg" :loading="feedbackStore.loading" @click="submitPrimaryAction">
            应用下一步建议
          </AppButton>
          <AppButton variant="secondary" @click="router.push(`/sessions/${sessionId}`)">返回学习导航</AppButton>
          <AppButton variant="ghost" @click="router.push(`/sessions/${sessionId}/growth`)">查看成长看板</AppButton>
        </div>
      </SectionCard>

      <div class="grid gap-5 md:grid-cols-2">
        <SectionCard title="这轮做得好的地方">
          <div class="grid gap-3">
            <div v-for="item in strengths" :key="item" class="app-option app-option-selected text-sm leading-7 text-slate-700">
              {{ item }}
            </div>
          </div>
        </SectionCard>

        <SectionCard title="接下来优先补这里">
          <div class="grid gap-3">
            <div v-for="item in weaknesses" :key="item" class="app-option text-sm leading-7 text-slate-700">
              {{ item }}
            </div>
          </div>
        </SectionCard>
      </div>

      <SectionCard title="本步证据摘要" description="来自当前任务步骤的可追溯评估证据">
        <div v-if="stepEvidence.length" class="grid gap-3">
          <div
            v-for="item in stepEvidence"
            :key="item.evidenceId"
            class="app-option text-sm leading-7 text-slate-700"
          >
            <p class="font-medium text-slate-900">
              第{{ item.stepIndex || '-' }}步 · {{ item.evidenceType || 'EVIDENCE' }}
            </p>
            <p>{{ item.summary || '已记录步骤证据。' }}</p>
          </div>
        </div>
        <p v-else class="text-sm leading-7 text-slate-500">
          当前还没有可展示的步骤证据，完成更多步骤后会自动生成。
        </p>
      </SectionCard>
    </div>
  </AppShell>
</template>
