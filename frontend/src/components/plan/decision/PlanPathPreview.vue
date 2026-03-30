<template>
  <section class="rounded-[24px] border border-slate-200/80 bg-white px-5 py-5 shadow-[0_12px_40px_rgba(15,23,42,0.06)] md:px-6">
    <div class="flex flex-col gap-2">
      <h2 class="text-sm font-semibold uppercase tracking-[0.18em] text-slate-500">
        {{ model.title }}
      </h2>
      <p class="text-lg font-semibold text-slate-950">
        {{ model.summary }}
      </p>
    </div>

    <ol class="mt-6 grid gap-3">
      <li
        v-for="stage in model.stages"
        :key="stage.key"
        class="relative rounded-2xl border px-4 py-4 transition-colors"
        :class="stage.active
          ? 'border-slate-900 bg-slate-950 text-white shadow-[0_16px_40px_rgba(15,23,42,0.24)]'
          : 'border-slate-200 bg-slate-50 text-slate-700'"
      >
        <div class="flex flex-col gap-3 md:flex-row md:items-start md:justify-between">
          <div>
            <div class="flex items-center gap-2">
              <span
                class="inline-flex h-7 min-w-7 items-center justify-center rounded-full text-xs font-semibold"
                :class="stage.active ? 'bg-white/12 text-white' : 'bg-white text-slate-600 ring-1 ring-slate-200'"
              >
                {{ stage.label.slice(0, 1) }}
              </span>
              <p class="text-sm font-semibold tracking-[0.12em]">
                {{ stage.label }}
              </p>
            </div>
            <p
              class="mt-3 text-sm leading-6"
              :class="stage.active ? 'text-slate-200' : 'text-slate-600'"
            >
              {{ stage.description }}
            </p>
          </div>
          <span
            class="inline-flex w-fit rounded-full px-3 py-1 text-xs font-medium"
            :class="stage.active ? 'bg-cyan-300/16 text-cyan-100' : 'bg-white text-slate-500 ring-1 ring-slate-200'"
          >
            {{ stage.stateLabel }}
          </span>
        </div>
      </li>
    </ol>
  </section>
</template>

<script setup lang="ts">
import type { LearningPlanDecisionViewModel } from '@/types/learningPlanDecision'

defineProps<{
  model: LearningPlanDecisionViewModel['pathPreview']
}>()
</script>
