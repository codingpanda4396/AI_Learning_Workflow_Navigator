<template>
  <section class="rounded-xl border border-slate-200 bg-white p-3 shadow-sm">
    <p class="text-[10px] font-semibold uppercase tracking-wide text-slate-400">阶段进展</p>
    <p class="mt-2 text-sm font-semibold text-slate-900">{{ model.roundLabel }}</p>
    <p class="mt-2 line-clamp-2 text-xs text-slate-600">
      还差：{{ model.untilNextPhase }}
    </p>
    <p
      class="mt-2 text-xs font-medium"
      :class="model.passedGate ? 'text-emerald-800' : 'text-slate-500'"
    >
      {{ model.passedGate ? '当前已达标，可进入下一阶段' : '当前未达标' }}
    </p>
  </section>
</template>

<script setup lang="ts">
import type { StageProgressMiniModel } from '@/types/taskExecutionWorkbench'

defineProps<{
  model: StageProgressMiniModel
}>()
</script>
