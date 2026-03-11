<script setup lang="ts">
import { computed } from 'vue';
import { formatStage } from '@/utils/format';

const props = defineProps<{
  stage?: string;
}>();

const tone = computed(() => {
  switch (props.stage) {
    case 'TRAINING':
      return 'bg-amber-100 text-amber-900 border-amber-200';
    case 'REFLECTION':
      return 'bg-emerald-100 text-emerald-900 border-emerald-200';
    case 'UNDERSTANDING':
      return 'bg-sky-100 text-sky-900 border-sky-200';
    case 'STRUCTURE':
      return 'bg-violet-100 text-violet-900 border-violet-200';
    default:
      return 'bg-slate-100 text-slate-700 border-slate-200';
  }
});
</script>

<template>
  <span class="inline-flex items-center rounded-full border px-3 py-1 text-xs font-semibold" :class="tone">
    {{ formatStage(stage) }}
  </span>
</template>
