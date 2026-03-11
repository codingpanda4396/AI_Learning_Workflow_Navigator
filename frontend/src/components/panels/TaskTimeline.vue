<script setup lang="ts">
import StageBadge from '@/components/common/StageBadge.vue';
import type { SessionTimelineItem } from '@/types/session';

defineProps<{
  items: SessionTimelineItem[];
  currentStage?: string;
}>();
</script>

<template>
  <div class="rounded-3xl bg-white p-6 shadow-sm ring-1 ring-slate-200">
    <div class="flex items-center justify-between">
      <h3 class="text-base font-semibold text-slate-900">任务时间线</h3>
      <p class="text-xs text-slate-400">按真实 session 顺序展示</p>
    </div>

    <div class="mt-5 space-y-4">
      <div
        v-for="item in items"
        :key="item.taskId"
        class="flex items-center justify-between rounded-2xl px-4 py-3"
        :class="item.stage === currentStage ? 'bg-slate-900 text-white' : 'bg-slate-50 text-slate-800'"
      >
        <div>
          <p class="text-sm font-medium">任务 #{{ item.taskId }}</p>
          <p class="mt-1 text-xs" :class="item.stage === currentStage ? 'text-slate-300' : 'text-slate-500'">
            状态：{{ item.status }}
          </p>
        </div>
        <StageBadge :stage="item.stage" />
      </div>
    </div>
  </div>
</template>
