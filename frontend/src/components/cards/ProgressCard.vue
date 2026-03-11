<script setup lang="ts">
import type { SessionProgress } from '@/types/session';
import { formatPercent } from '@/utils/format';

defineProps<{
  progress: SessionProgress;
}>();
</script>

<template>
  <div class="rounded-3xl bg-white p-6 shadow-sm ring-1 ring-slate-200">
    <div class="flex items-center justify-between">
      <div>
        <p class="text-sm text-slate-500">当前进度</p>
        <p class="mt-1 text-xl font-semibold text-slate-900">
          {{ progress.completedTaskCount }}/{{ progress.totalTaskCount }}
        </p>
      </div>
      <p class="text-sm font-medium text-slate-600">{{ formatPercent(progress.completionRate) }}</p>
    </div>
    <div class="mt-4 h-2 rounded-full bg-slate-100">
      <div
        class="h-2 rounded-full bg-slate-900 transition-all"
        :style="{ width: `${Math.min((progress.completionRate || 0) * 100, 100)}%` }"
      />
    </div>
  </div>
</template>
