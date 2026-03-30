<template>
  <aside
    data-testid="execution-mini-rail"
    class="space-y-4 rounded-2xl border border-slate-200/90 bg-white p-4 shadow-sm lg:sticky lg:top-6"
  >
    <section>
      <p class="text-xs font-semibold uppercase tracking-[0.14em] text-slate-500">
        {{ TASKRUN_COPY.structureRailStage }}
      </p>
      <p class="mt-1.5 text-sm font-semibold text-slate-900">{{ phaseLabel }}</p>
      <p class="mt-2 text-sm leading-6 text-slate-600">{{ objectiveLine }}</p>
    </section>

    <section class="border-t border-slate-100 pt-4">
      <p class="text-xs font-semibold uppercase tracking-[0.14em] text-slate-500">
        {{ TASKRUN_COPY.structureRailOutput }}
      </p>
      <ul class="mt-2 space-y-1.5 text-sm leading-6 text-slate-700">
        <li v-for="(line, i) in outputLines" :key="`out-${i}`" class="flex gap-2">
          <span class="shrink-0 text-slate-400">{{ i + 1 }}.</span>
          <span>{{ line }}</span>
        </li>
      </ul>
    </section>

    <section class="border-t border-slate-100 pt-4">
      <p class="text-xs font-semibold uppercase tracking-[0.14em] text-slate-500">
        {{ TASKRUN_COPY.structureRailProgress }}
      </p>
      <p class="mt-2 text-sm font-medium tabular-nums text-slate-900">{{ stepLabel }}</p>
    </section>
  </aside>
</template>

<script setup lang="ts">
import { TASKRUN_COPY } from '@/constants/uiCopy'

defineProps<{
  phaseLabel: string
  objectiveLine: string
  outputLines: string[]
  stepLabel: string
}>()
</script>
