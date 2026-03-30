<template>
  <section class="rounded-[24px] border border-slate-200/80 bg-white/88 px-5 py-5 shadow-[0_12px_40px_rgba(15,23,42,0.06)] backdrop-blur md:px-6">
    <div class="flex flex-col gap-2 md:flex-row md:items-end md:justify-between">
      <div>
        <h2 class="text-sm font-semibold uppercase tracking-[0.18em] text-slate-500">
          {{ model.title }}
        </h2>
        <p class="mt-2 text-lg font-semibold text-slate-950">
          {{ model.summary }}
        </p>
      </div>
      <p class="text-sm text-slate-500">
        只保留这 3 个判断。
      </p>
    </div>

    <div class="mt-5 grid gap-3 md:grid-cols-3">
      <article
        v-for="bullet in model.bullets"
        :key="bullet.label"
        class="rounded-2xl border border-slate-200/80 bg-slate-50 px-4 py-4"
      >
        <p class="text-xs font-medium uppercase tracking-[0.16em] text-slate-500">
          {{ bullet.label }}
        </p>
        <p class="mt-2 text-sm leading-6 text-slate-800">
          {{ bullet.text }}
        </p>
      </article>
    </div>
  </section>
</template>

<script setup lang="ts">
import type { LearningPlanDecisionViewModel } from '@/types/learningPlanDecision'

defineProps<{
  model: LearningPlanDecisionViewModel['reasoning']
}>()
</script>
