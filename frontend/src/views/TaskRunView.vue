<script setup lang="ts">
import { computed, onMounted } from 'vue';
import { useRoute } from 'vue-router';
import AppShell from '@/components/common/AppShell.vue';
import LearningActionBar from '@/components/learning/LearningActionBar.vue';
import LearningContentSection from '@/components/common/LearningContentSection.vue';
import LearningPageHeader from '@/components/learning/LearningPageHeader.vue';
import LearningStatePanel from '@/components/learning/LearningStatePanel.vue';
import AppButton from '@/components/ui/AppButton.vue';
import { PRIMARY_CTA, SECONDARY_LABELS } from '@/constants/learningFlow';
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

const primaryLabel = computed(() =>
  isTraining.value ? PRIMARY_CTA.ENTER_TRAINING : PRIMARY_CTA.ENTER_TRAINING_AFTER_DONE,
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

async function retry() {
  if (isLearnRoute.value && sessionId.value) {
    await flowStore.ensureCurrentTaskLoaded();
  } else {
    const tid = Number(route.params.taskId);
    if (Number.isFinite(tid)) {
      await taskStore.fetchTaskDetail(tid);
      await taskStore.runTask(tid);
    }
  }
}

onMounted(retry);
</script>

<template>
  <AppShell>
    <LearningStatePanel v-if="taskStore.loading && !result" state="loading" />
    <LearningStatePanel
      v-else-if="taskStore.error && !result"
      state="error"
      :message="taskStore.error"
      :action-label="SECONDARY_LABELS.RETRY"
      @action="retry"
    />

    <div v-else class="mx-auto max-w-[720px] space-y-8">
      <LearningPageHeader
        eyebrow="当前学习任务"
        :title="content.title"
        :lead="content.summary || '完成本步后进入练习。'"
      />

      <LearningContentSection title="这一步的重点" :description="detail?.objective || content.summary" />
      <LearningContentSection title="建议顺序" :items="content.suggestedSequence" numbered />

      <LearningActionBar>
        <template #primary>
          <AppButton size="lg" block @click="goPrimary">{{ primaryLabel }}</AppButton>
        </template>
        <template #secondary>
          <AppButton variant="secondary" block @click="backToProgress">{{ SECONDARY_LABELS.BACK_TO_PROGRESS }}</AppButton>
        </template>
      </LearningActionBar>
    </div>
  </AppShell>
</template>
