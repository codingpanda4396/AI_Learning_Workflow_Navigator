<script setup lang="ts">
import { LEARNING_STAGE_LABELS } from '@/constants/learningFlow';
import type { LearningStage } from '@/domain/learning-flow/types';

defineProps<{
  stage: LearningStage;
}>();
</script>

<template>
  <span class="inline-flex items-center rounded-full bg-slate-100 px-3 py-1 text-xs font-medium text-slate-700">
    {{ LEARNING_STAGE_LABELS[stage] }}
  </span>
</template>
