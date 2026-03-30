<template>
  <section class="rounded-[24px] border border-amber-200/70 bg-[linear-gradient(180deg,rgba(255,251,235,0.96),rgba(255,255,255,0.94))] px-5 py-5 shadow-[0_16px_48px_rgba(245,158,11,0.10)] md:px-6">
    <p class="text-xs font-medium uppercase tracking-[0.18em] text-amber-700">
      {{ model.title }}
    </p>
    <h2 class="mt-3 text-xl font-semibold tracking-tight text-slate-950">
      {{ model.taskName }}
    </h2>
    <p class="mt-3 text-sm leading-6 text-slate-700">
      {{ model.reasonText }}
    </p>

    <div class="mt-5 flex flex-wrap gap-2">
      <span class="rounded-full bg-white px-3 py-1 text-xs font-medium text-slate-700 ring-1 ring-inset ring-amber-200">
        {{ model.estimatedTimeText }}
      </span>
      <span class="rounded-full bg-white px-3 py-1 text-xs font-medium text-slate-700 ring-1 ring-inset ring-amber-200">
        一步完成后再继续
      </span>
    </div>

    <div class="mt-5 rounded-2xl border border-white/80 bg-white/80 px-4 py-4">
      <p class="text-xs font-medium uppercase tracking-[0.16em] text-slate-500">
        做完先得到
      </p>
      <p class="mt-2 text-sm leading-6 text-slate-800">
        {{ model.benefitText }}
      </p>
    </div>
  </section>
</template>

<script setup lang="ts">
import type { LearningPlanDecisionViewModel } from '@/types/learningPlanDecision'

defineProps<{
  model: LearningPlanDecisionViewModel['firstTask']
}>()
</script>
