<script setup lang="ts">
import { computed, onMounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import AppShell from '@/components/common/AppShell.vue';
import ErrorState from '@/components/common/ErrorState.vue';
import LearningContentSection from '@/components/common/LearningContentSection.vue';
import LoadingState from '@/components/common/LoadingState.vue';
import PageSection from '@/components/common/PageSection.vue';
import SecondaryInfoCard from '@/components/common/SecondaryInfoCard.vue';
import StagePill from '@/components/common/StagePill.vue';
import { formatStage } from '@/utils/format';
import { useTaskStore } from '@/stores/task';
import { normalizeLearningContent } from '@/utils/taskContent';

const route = useRoute();
const router = useRouter();
const taskStore = useTaskStore();

const taskId = computed(() => Number(route.params.taskId));
const detail = computed(() => taskStore.currentTaskDetail);
const result = computed(() => taskStore.currentTaskResult);
const stage = computed(() => result.value?.stage || detail.value?.stage);
const isTraining = computed(() => stage.value === 'TRAINING');

const content = computed(() =>
  normalizeLearningContent(result.value?.output ?? detail.value?.output, detail.value?.objective || `任务 ${taskId.value}`),
);

const taskGoal = computed(() => detail.value?.objective || content.value.summary || '先完成这一步的学习内容。');
const sessionId = computed(() => detail.value?.sessionId);

async function backToSession() {
  if (sessionId.value) {
    await router.push(`/sessions/${sessionId.value}`);
  }
}

async function goPrimary() {
  if (isTraining.value && sessionId.value) {
    await router.push(`/sessions/${sessionId.value}/quiz`);
    return;
  }
  await backToSession();
}

onMounted(async () => {
  await taskStore.fetchTaskDetail(taskId.value);
  await taskStore.runTask(taskId.value);
});
</script>

<template>
  <AppShell>
    <LoadingState v-if="taskStore.loading && !result" />
    <ErrorState v-else-if="taskStore.error && !result" :message="taskStore.error" />
    <div v-else class="grid gap-6 lg:grid-cols-[minmax(0,1fr)_280px]">
      <div class="space-y-5">
        <PageSection eyebrow="专注学习" :title="content.title" :description="content.summary" />

        <LearningContentSection title="本任务目标" :description="taskGoal" />
        <LearningContentSection title="核心知识点" :items="content.keyPoints" />
        <LearningContentSection title="常见误区" :items="content.misconceptions">
          当前没有额外误区提示，建议重点检查自己是否能用自己的话解释核心概念。
        </LearningContentSection>
        <LearningContentSection title="推荐学习顺序" :items="content.suggestedSequence" numbered />
        <LearningContentSection v-if="content.supplementaryNotes.length" title="补充说明" :items="content.supplementaryNotes" />
      </div>

      <aside class="space-y-4">
        <SecondaryInfoCard title="当前阶段">
          <div class="flex items-center justify-between gap-3">
            <StagePill :stage="stage" />
            <span class="text-sm text-slate-500">{{ formatStage(stage) }}</span>
          </div>
        </SecondaryInfoCard>

        <SecondaryInfoCard title="页面操作" description="右侧只保留和当前这一步直接相关的动作。">
          <div class="space-y-3">
            <button class="w-full rounded-2xl border border-slate-200 px-4 py-3 text-sm font-medium text-slate-700 transition hover:bg-slate-50" @click="backToSession">
              返回总览
            </button>
            <button class="w-full rounded-2xl bg-slate-950 px-4 py-3 text-sm font-semibold text-white transition hover:bg-slate-800" @click="goPrimary">
              {{ isTraining ? '进入检测' : '我学完了，返回总览' }}
            </button>
          </div>
        </SecondaryInfoCard>
      </aside>
    </div>
  </AppShell>
</template>
