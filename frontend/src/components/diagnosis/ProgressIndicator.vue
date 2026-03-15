<script setup lang="ts">
import { computed } from 'vue';
import StepProgress from '@/components/ui/StepProgress.vue';

const props = defineProps<{
  current: number;
  total: number;
}>();

const remaining = computed(() => Math.max(0, props.total - props.current));
</script>

<template>
  <div class="space-y-1">
    <StepProgress :current="current" :total="total" label="答题进度" />
    <p v-if="total > 0 && remaining > 0" class="text-right text-xs text-slate-500">
      还剩 {{ remaining }} 题即可生成你的学习路径
    </p>
  </div>
</template>
