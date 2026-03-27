<template>
  <section class="rounded-2xl border border-slate-200/70 bg-white px-5 py-5 md:px-6">
    <h2 class="text-sm font-semibold text-slate-900">
      {{ model.title }}
    </h2>
    <ul class="mt-4 space-y-2.5">
      <li
        v-for="row in model.stages"
        :key="row.key"
        class="flex flex-wrap items-baseline gap-x-2 gap-y-1 text-sm"
        :class="row.active ? 'font-medium text-slate-900' : 'text-slate-400'"
      >
        <span>{{ row.label }}：</span>
        <span :class="row.active ? 'text-slate-800' : 'text-slate-400'">
          {{ row.description }}
        </span>
      </li>
    </ul>
  </section>
</template>

<script setup lang="ts">
import type { LearningPlanDecisionViewModel } from '@/types/learningPlanDecision'

defineProps<{
  model: LearningPlanDecisionViewModel['pathPreview']
}>()
</script>
