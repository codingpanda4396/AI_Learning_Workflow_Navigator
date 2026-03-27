<template>
  <section class="rounded-2xl border border-indigo-200/60 bg-indigo-50/30 px-5 py-5 md:px-6">
    <h2 class="text-sm font-semibold text-slate-900">
      {{ model.title }}
    </h2>
    <div class="mt-4 space-y-2 text-sm leading-7 text-slate-800">
      <p class="font-medium">{{ model.intro }}</p>
      <p>{{ model.actionText }}</p>
      <p class="text-slate-600">{{ model.estimatedTimeText }}</p>
      <p>做完后，你会得到 {{ model.benefitText }}</p>
    </div>
  </section>
</template>

<script setup lang="ts">
import type { LearningPlanDecisionViewModel } from '@/types/learningPlanDecision'

defineProps<{
  model: LearningPlanDecisionViewModel['firstTask']
}>()
</script>
