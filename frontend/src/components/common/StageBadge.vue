<script setup lang="ts">
import { computed } from 'vue';
import { formatStage } from '@/utils/format';

const props = defineProps<{
  stage?: string;
}>();

const tone = computed(() => {
  switch (props.stage) {
    case 'TRAINING':
      return 'bg-amber-100 text-amber-800';
    case 'REFLECTION':
      return 'bg-emerald-100 text-emerald-800';
    case 'UNDERSTANDING':
      return 'bg-sky-100 text-sky-800';
    default:
      return 'bg-slate-100 text-slate-700';
  }
});
</script>

<template>
  <span class="inline-flex rounded-full px-3 py-1 text-xs font-medium" :class="tone">
    {{ formatStage(stage) }}
  </span>
</template>
