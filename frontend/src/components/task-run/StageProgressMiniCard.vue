<template>
  <section class="rounded-2xl border border-slate-200 bg-white p-4 shadow-sm">
    <p class="text-xs font-semibold uppercase tracking-wide text-slate-500">阶段进展</p>
    <p class="mt-2 text-sm font-semibold text-slate-900">{{ model.roundLabel }}</p>
    <div class="mt-3 flex flex-wrap gap-2 text-xs text-slate-600">
      <span class="rounded-full bg-slate-100 px-2 py-0.5">
        已完成动作 {{ model.actionsDone }} / {{ model.actionsTarget }}
      </span>
      <span
        class="rounded-full px-2 py-0.5"
        :class="model.passedGate ? 'bg-emerald-50 text-emerald-800' : 'bg-slate-100 text-slate-600'"
      >
        {{ model.passedGate ? '已达本关标准' : '尚未达标' }}
      </span>
    </div>
    <p class="mt-3 text-xs leading-relaxed text-slate-500">
      <span class="font-medium text-slate-600">距下一阶段：</span>
      {{ model.untilNextPhase }}
    </p>
  </section>
</template>

<script setup lang="ts">
import type { StageProgressMiniModel } from '@/types/taskExecutionWorkbench'

defineProps<{
  model: StageProgressMiniModel
}>()
</script>
