<script setup lang="ts">
import { computed } from 'vue';
import { formatPercent } from '@/utils/format';
import type { SessionProgress } from '@/types/session';

const props = defineProps<{
  progress?: SessionProgress | null;
  label?: string;
}>();

const completed = computed(() => props.progress?.completedTaskCount ?? 0);
const total = computed(() => props.progress?.totalTaskCount ?? 0);
const percent = computed(() => formatPercent(props.progress?.completionRate ?? 0));
</script>

<template>
  <div class="rounded-[1.6rem] border border-slate-200 bg-slate-50 p-5">
    <div class="flex items-center justify-between gap-4">
      <div>
        <p class="text-sm font-medium text-slate-700">{{ label || '当前进度' }}</p>
        <p class="mt-1 text-2xl font-semibold text-slate-950">{{ percent }}</p>
      </div>
      <p class="text-sm text-slate-500">{{ completed }} / {{ total }}</p>
    </div>
    <div class="mt-4 h-2 rounded-full bg-slate-200">
      <div
        class="h-full rounded-full bg-slate-900 transition-all"
        :style="{ width: `${Math.max(0, Math.min(100, Number(String(percent).replace('%', '')) || 0))}%` }"
      />
    </div>
  </div>
</template>
