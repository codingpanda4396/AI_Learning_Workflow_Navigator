<script setup lang="ts">
import { computed, onMounted } from 'vue';
import { useRoute } from 'vue-router';
import AppShell from '@/components/common/AppShell.vue';
import ErrorState from '@/components/common/ErrorState.vue';
import LoadingState from '@/components/common/LoadingState.vue';
import AppButton from '@/components/ui/AppButton.vue';
import SectionCard from '@/components/ui/SectionCard.vue';
import { useFeedbackStore } from '@/stores/feedback';
import { useLearningFlowStore } from '@/stores/learningFlow';
import { formatPercent } from '@/utils/format';

const route = useRoute();
const feedbackStore = useFeedbackStore();
const flowStore = useLearningFlowStore();

const sessionId = computed(() => Number(route.params.sessionId));
const report = computed(() => feedbackStore.report);

const summary = computed(() => report.value?.overallSummary || report.value?.diagnosisSummary || '结果整理中。');
const strengths = computed(() => report.value?.strengths?.length ? report.value.strengths : ['本轮暂无总结。']);
const weaknesses = computed(() => {
  if (report.value?.weakPoints?.length) {
    return report.value.weakPoints.slice(0, 5).map((item) => item.nodeName);
  }
  if (report.value?.weaknesses?.length) return report.value.weaknesses;
  if (report.value?.reviewFocus?.length) return report.value.reviewFocus;
  return ['本轮暂无明确薄弱点。'];
});
const score = computed(() => formatPercent(report.value?.overallAccuracy ?? report.value?.overallScore));

async function loadReport() {
  if (!sessionId.value) return;
  await feedbackStore.fetchReport(sessionId.value);
}

function goToNextSuggestions() {
  flowStore.goToStage('NEXT_ACTION');
}
</script>

<template>
  <AppShell>
    <LoadingState v-if="feedbackStore.loading && !report" />

    <div v-else-if="feedbackStore.error && !report" class="space-y-4">
      <ErrorState :message="feedbackStore.error" />
      <AppButton variant="secondary" @click="loadReport">重新加载</AppButton>
    </div>

    <div v-else-if="report" class="mx-auto max-w-[720px] space-y-8">
      <section class="app-hero">
        <p class="app-eyebrow">学习结果评估</p>
        <h1 class="app-title-lg mt-4">这轮结果怎么样</h1>
        <p class="app-text-lead mt-4">{{ summary }}</p>
        <div v-if="score !== '--'" class="mt-4 text-2xl font-semibold text-slate-900">{{ score }}</div>
      </section>

      <div class="grid gap-5 md:grid-cols-2">
        <SectionCard title="已掌握">
          <ul class="grid gap-2">
            <li v-for="item in strengths" :key="item" class="text-sm leading-7 text-slate-700">{{ item }}</li>
          </ul>
        </SectionCard>
        <SectionCard title="还不稳">
          <ul class="grid gap-2">
            <li v-for="item in weaknesses" :key="item" class="text-sm leading-7 text-slate-700">{{ item }}</li>
          </ul>
        </SectionCard>
      </div>

      <div class="flex flex-col gap-3">
        <AppButton size="lg" @click="$router.push(`/learn/${sessionId}/next`)">查看下一步建议</AppButton>
        <AppButton variant="secondary" @click="flowStore.goToStage('NEXT_ACTION')">返回当前进度</AppButton>
      </div>
    </div>
  </AppShell>
</template>
