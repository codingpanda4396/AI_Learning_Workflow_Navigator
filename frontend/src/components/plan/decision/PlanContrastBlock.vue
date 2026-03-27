<template>
  <section class="rounded-xl border border-slate-200/60 bg-slate-50/80 px-4 py-4 text-xs leading-6 text-slate-500 md:px-5">
    <h2 class="text-sm font-semibold text-slate-600">
      {{ model.title }}
    </h2>
    <p class="mt-2 text-slate-600">
      {{ model.riskText }}
    </p>
    <p class="mt-2 text-slate-500">
      {{ model.betterPathText }}
    </p>
  </section>
</template>

<script setup lang="ts">
import type { LearningPlanDecisionViewModel } from '@/types/learningPlanDecision'

defineProps<{
  model: LearningPlanDecisionViewModel['contrast']
}>()
</script>
