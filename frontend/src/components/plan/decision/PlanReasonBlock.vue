<template>
  <section class="rounded-2xl border border-slate-200/80 bg-slate-50/50 px-5 py-5 md:px-6">
    <h2 class="text-sm font-semibold text-slate-900">
      {{ model.title }}
    </h2>
    <div class="mt-4 space-y-3 text-sm leading-7 text-slate-700">
      <p>你的目标是：{{ model.goalText }}</p>
      <p>你现在先卡住的，不是知识点数量，而是 {{ model.blockerText }}</p>
      <p>{{ model.consequenceText }}</p>
    </div>
  </section>
</template>

<script setup lang="ts">
import type { LearningPlanDecisionViewModel } from '@/types/learningPlanDecision'

defineProps<{
  model: LearningPlanDecisionViewModel['reason']
}>()
</script>
