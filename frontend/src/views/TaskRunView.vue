<script setup lang="ts">
import { computed, onMounted } from 'vue';
import { useRoute } from 'vue-router';
import AppShell from '@/components/common/AppShell.vue';
import ErrorState from '@/components/common/ErrorState.vue';
import LearningContentSection from '@/components/common/LearningContentSection.vue';
import LoadingState from '@/components/common/LoadingState.vue';
import AppButton from '@/components/ui/AppButton.vue';
import SectionCard from '@/components/ui/SectionCard.vue';
import { formatStage } from '@/utils/format';
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
const stage = computed(() => result.value?.stage || detail.value?.stage);
const isTraining = computed(() => stage.value === 'TRAINING');

const content = computed(() =>
  normalizeLearningContent(result.value?.output ?? detail.value?.output, detail.value?.objective || `任务 ${taskId.value}`),
);

const taskGoal = computed(() => detail.value?.objective || content.value.summary || '先完成这一步的学习内容。');

function backToSession() {
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

onMounted(async () => {
  if (isLearnRoute.value && sessionId.value) {
    await flowStore.ensureCurrentTaskLoaded();
  } else {
    const tid = Number(route.params.taskId);
    if (Number.isFinite(tid)) {
      await taskStore.fetchTaskDetail(tid);
      await taskStore.runTask(tid);
    }
  }
});
</script>

<template>
  <AppShell>
    <LoadingState v-if="taskStore.loading && !result" />
    <ErrorState v-else-if="taskStore.error && !result" :message="taskStore.error" />

    <div v-else class="app-grid-2">
      <div class="app-stack-md">
        <section class="app-hero">
          <p class="app-eyebrow">当前学习任务</p>
          <h1 class="app-title-lg mt-4">{{ content.title }}</h1>
          <p class="app-text-lead mt-4">{{ content.summary || '先把这一块学会，后面的练习和反馈会更有效。' }}</p>
        </section>

        <LearningContentSection title="这一步的重点" :description="taskGoal" />
        <LearningContentSection title="核心知识点" :items="content.keyPoints" />
        <LearningContentSection title="容易卡住的地方" :items="content.misconceptions">
          当前没有额外提醒时，可以试着用自己的话解释这部分内容。
        </LearningContentSection>
        <LearningContentSection title="建议顺序" :items="content.suggestedSequence" numbered />
        <LearningContentSection v-if="content.supplementaryNotes.length" title="补充说明" :items="content.supplementaryNotes" />
      </div>

      <aside class="app-stack-md">
        <SectionCard strong title="你现在在做什么" description="Tutor 和其他模块都退到辅助位置，先把主学习内容完成。">
          <div class="flex items-center justify-between gap-3">
            <span class="app-pill">{{ formatStage(stage) }}</span>
            <span class="text-sm text-slate-500">{{ detail?.nodeName || '当前节点' }}</span>
          </div>
          <div class="app-divider my-5" />
          <div class="grid gap-3">
            <AppButton size="lg" block @click="goPrimary">
              {{ isTraining ? '进入学习检测' : '我学完了，继续下一步' }}
            </AppButton>
            <AppButton variant="secondary" block @click="backToSession">返回学习导航</AppButton>
          </div>
        </SectionCard>

        <SectionCard title="辅助提示" muted>
          <p class="text-sm leading-7 text-slate-600">
            先消化主内容，再去做检测或提问，效果通常会更稳。
          </p>
        </SectionCard>
      </aside>
    </div>
  </AppShell>
</template>
