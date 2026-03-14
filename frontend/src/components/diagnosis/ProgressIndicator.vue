<script setup lang="ts">
import StepProgress from '@/components/ui/StepProgress.vue';

defineProps<{
  current: number;
  total: number;
}>();
</script>

<template>
  <StepProgress :current="current" :total="total" label="诊断进度" />
</template>
