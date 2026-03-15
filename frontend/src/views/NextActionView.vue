<script setup lang="ts">
import { computed, onMounted } from 'vue';
import { useRoute } from 'vue-router';
import AppShell from '@/components/common/AppShell.vue';
import LearningActionBar from '@/components/learning/LearningActionBar.vue';
import LearningPageHeader from '@/components/learning/LearningPageHeader.vue';
import LearningStatePanel from '@/components/learning/LearningStatePanel.vue';
import AppButton from '@/components/ui/AppButton.vue';
import { PRIMARY_CTA, SECONDARY_LABELS } from '@/constants/learningFlow';
import { useFeedbackStore } from '@/stores/feedback';
import { useLearningFlowStore } from '@/stores/learningFlow';

const route = useRoute();
const feedbackStore = useFeedbackStore();
const flowStore = useLearningFlowStore();

const sessionId = computed(() => Number(route.params.sessionId));
const report = computed(() => feedbackStore.report);

const suggestionLabel = computed(() => {
  const action = report.value?.nextStep?.recommendedAction || report.value?.recommendedAction || 'REVIEW';
  if (action === 'ADVANCE' || action === 'NEXT_ROUND') return '继续学下一块';
  if (action === 'BACKTRACK') return '先补一块基础';
  return '先巩固这一块';
});

const reason = computed(() => report.value?.nextStep?.reason || report.value?.nextRoundAdvice || '');

async function loadReport() {
  if (!sessionId.value) return;
  await feedbackStore.fetchReport(sessionId.value);
}

async function startNext() {
  const action = report.value?.recommendedAction || report.value?.nextStep?.recommendedAction || 'REVIEW';
  try {
    await feedbackStore.submitNextAction(sessionId.value, action);
  } catch {
    return;
  }
  const targetTaskType = report.value?.nextStep?.targetTaskType || '';
  if (targetTaskType === 'TRAINING' || action === 'REINFORCE' || action === 'BACKTRACK') {
    await flowStore.goToStage('TRAINING');
    return;
  }
  await flowStore.goToStage('NEXT_ACTION');
}

function backToProgress() {
  flowStore.goToStage('NEXT_ACTION');
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

    <div v-else-if="report" class="mx-auto max-w-[640px] space-y-8">
      <LearningPageHeader
        eyebrow="下一步建议"
        title="系统建议你接下来这样做"
        :lead="suggestionLabel"
      />
      <p v-if="reason" class="text-sm text-slate-600">{{ reason }}</p>

      <LearningActionBar>
        <template #primary>
          <AppButton size="lg" :loading="feedbackStore.loading" @click="startNext">
            {{ PRIMARY_CTA.START_NEXT }}
          </AppButton>
        </template>
        <template #secondary>
          <AppButton variant="secondary" @click="backToProgress">
            {{ SECONDARY_LABELS.BACK_TO_PROGRESS }}
          </AppButton>
        </template>
      </LearningActionBar>
    </div>
  </AppShell>
</template>
