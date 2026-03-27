<template>
  <section class="rounded-2xl border border-slate-200/80 bg-white px-5 py-5 md:px-6">
    <h2 class="text-sm font-semibold text-slate-900">
      {{ model.title }}
    </h2>
    <div class="mt-4 space-y-3 text-sm leading-7 text-slate-700">
      <p>{{ model.currentState }}</p>
      <p>所以先 {{ model.strategyAction }}</p>
      <p>这样后面才 {{ model.expectedResult }}。</p>
    </div>
  </section>
</template>

<script setup lang="ts">
import type { LearningPlanDecisionViewModel } from '@/types/learningPlanDecision'

defineProps<{
  model: LearningPlanDecisionViewModel['causal']
}>()
</script>
