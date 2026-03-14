<script setup lang="ts">
import { computed } from 'vue';

const props = withDefaults(defineProps<{
  current: number;
  total: number;
  label?: string;
}>(), {
  label: '当前进度',
});

const progress = computed(() => {
  if (props.total <= 0) {
    return 0;
  }
  return Math.min(100, Math.max(0, (props.current / props.total) * 100));
});
</script>

<template>
  <div class="app-card app-card-padding">
    <div class="flex items-center justify-between gap-4">
      <div>
        <p class="app-eyebrow">{{ label }}</p>
        <p class="mt-2 text-2xl font-semibold tracking-[-0.03em] text-slate-950">{{ current }} / {{ total }}</p>
      </div>
      <span class="app-pill">{{ Math.round(progress) }}%</span>
    </div>
    <div class="app-progress-track mt-5">
      <div class="app-progress-fill" :style="{ width: `${progress}%` }" />
    </div>
  </div>
</template>
