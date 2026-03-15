<script setup lang="ts">
import { computed, onMounted } from 'vue';
import { useRoute } from 'vue-router';
import AppShell from '@/components/common/AppShell.vue';
import ErrorState from '@/components/common/ErrorState.vue';
import LoadingState from '@/components/common/LoadingState.vue';
import AppButton from '@/components/ui/AppButton.vue';
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

const reason = computed(() => report.value?.nextStep?.reason || report.value?.nextRoundAdvice || '按系统建议进行，效果更稳。');

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
    <LoadingState v-if="feedbackStore.loading && !report" />

    <div v-else-if="feedbackStore.error && !report" class="space-y-4">
      <ErrorState :message="feedbackStore.error" />
      <AppButton variant="secondary" @click="loadReport">重新加载</AppButton>
    </div>

    <div v-else-if="report" class="mx-auto max-w-[640px] space-y-8">
      <section class="app-hero">
        <p class="app-eyebrow">下一步建议</p>
        <h1 class="app-title-lg mt-4">系统建议你接下来这样做</h1>
        <p class="app-text-lead mt-4">{{ suggestionLabel }}</p>
        <p class="mt-4 text-sm text-slate-600">{{ reason }}</p>
      </section>

      <div class="flex flex-col gap-3">
        <AppButton size="lg" :loading="feedbackStore.loading" @click="startNext">开始下一步</AppButton>
        <AppButton variant="secondary" @click="backToProgress">返回当前进度</AppButton>
      </div>
    </div>
  </AppShell>
</template>
