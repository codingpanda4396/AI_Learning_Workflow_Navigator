<script setup lang="ts">
import { computed, onMounted } from 'vue';
import { useRoute } from 'vue-router';
import AppShell from '@/components/common/AppShell.vue';
import LearningActionBar from '@/components/learning/LearningActionBar.vue';
import LearningPageHeader from '@/components/learning/LearningPageHeader.vue';
import LearningStatePanel from '@/components/learning/LearningStatePanel.vue';
import AppButton from '@/components/ui/AppButton.vue';
import SectionCard from '@/components/ui/SectionCard.vue';
import { PRIMARY_CTA, SECONDARY_LABELS } from '@/constants/learningFlow';
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

onMounted(loadReport);
</script>

<template>
  <AppShell>
    <LearningStatePanel v-if="feedbackStore.loading && !report" state="loading" />
    <LearningStatePanel
      v-else-if="feedbackStore.error && !report"
      state="error"
      :message="feedbackStore.error"
      :action-label="SECONDARY_LABELS.RETRY"
      @action="loadReport"
    />

    <div v-else-if="report" class="mx-auto max-w-[720px] space-y-8">
      <LearningPageHeader
        eyebrow="学习结果评估"
        title="这轮结果怎么样"
        :lead="summary"
      />
      <div v-if="score !== '--'" class="text-2xl font-semibold text-slate-900">{{ score }}</div>

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

      <LearningActionBar>
        <template #primary>
          <AppButton size="lg" @click="$router.push(`/learn/${sessionId}/next`)">
            {{ PRIMARY_CTA.VIEW_NEXT_SUGGESTION }}
          </AppButton>
        </template>
        <template #secondary>
          <AppButton variant="secondary" @click="flowStore.goToStage('NEXT_ACTION')">
            {{ SECONDARY_LABELS.BACK_TO_PROGRESS }}
          </AppButton>
        </template>
      </LearningActionBar>
    </div>
  </AppShell>
</template>
