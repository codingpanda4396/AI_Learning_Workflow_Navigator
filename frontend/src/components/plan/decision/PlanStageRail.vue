<template>
  <section>
    <h2 class="text-xs font-semibold uppercase tracking-[0.18em] text-slate-500">
      {{ model.title }}
    </h2>
    <ol class="mt-4 flex flex-col gap-2">
      <li
        v-for="stage in model.stages"
        :key="stage.key"
        class="rounded-xl px-4 py-3 transition-colors"
        :class="
          stage.active
            ? 'border border-indigo-200 bg-indigo-50/90 text-slate-900 shadow-sm ring-1 ring-indigo-100/80'
            : 'border border-slate-200/90 bg-white text-slate-800'
        "
      >
        <div class="flex flex-wrap items-start justify-between gap-2">
          <div class="min-w-0 flex-1">
            <p
              class="text-xs font-semibold tracking-[0.14em]"
              :class="stage.active ? 'text-indigo-800' : 'text-slate-500'"
            >
              {{ stage.label }}
            </p>
            <p
              class="mt-1.5 text-sm leading-relaxed"
              :class="stage.active ? 'text-slate-700' : 'text-slate-600'"
            >
              {{ stage.railLine }}
            </p>
          </div>
          <span
            class="shrink-0 rounded-full px-2.5 py-0.5 text-[11px] font-medium"
            :class="
              stage.active
                ? 'bg-white text-indigo-700 shadow-sm ring-1 ring-indigo-100'
                : 'bg-slate-50 text-slate-500 ring-1 ring-slate-200/90'
            "
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
