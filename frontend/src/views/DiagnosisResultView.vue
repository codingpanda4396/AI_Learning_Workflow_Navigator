<script setup lang="ts">
import DiagnosisExplanationCard from '@/components/panels/DiagnosisExplanationCard.vue';
import DiagnosisHeroCard from '@/components/panels/DiagnosisHeroCard.vue';
import NextStepActionCard from '@/components/panels/NextStepActionCard.vue';
import type { DiagnosisResultViewModel } from '@/types/diagnosis';

defineProps<{
  result: DiagnosisResultViewModel;
  actionDisabled?: boolean;
}>();

defineEmits<{
  action: [];
}>();
</script>

<template>
  <div class="mx-auto flex max-w-4xl flex-col gap-6 pb-12">
    <DiagnosisHeroCard :level="result.level" :summary="result.summary" />
    <DiagnosisExplanationCard
      :summary="result.summary"
      :strengths="result.strengths"
      :weaknesses="result.weaknesses"
    />
    <NextStepActionCard :suggestion="result.suggestion" :disabled="actionDisabled" @action="$emit('action')" />
  </div>
</template>
