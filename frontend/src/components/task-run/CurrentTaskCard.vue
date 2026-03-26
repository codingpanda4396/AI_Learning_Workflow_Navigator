<template>
  <section
    data-testid="current-task-card"
    class="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm md:p-6"
    :class="emphasisClass"
  >
    <div class="flex flex-wrap items-center gap-2">
      <span
        class="rounded-full bg-primary/10 px-2.5 py-0.5 text-[11px] font-bold uppercase tracking-wide text-primary"
      >
        {{ model.phaseCode }}
      </span>
      <span class="text-sm font-semibold text-slate-800">{{ model.phaseDisplayZh }}</span>
    </div>
    <p class="mt-3 text-lg font-semibold leading-snug text-slate-950 md:text-xl">
      {{ model.taskTitle }}
    </p>
    <p class="mt-2 text-sm leading-6 text-slate-700">
      <span class="font-medium text-slate-800">你现在要做的是：</span>
      {{ model.coreActionLine }}
    </p>
    <div v-if="model.completionLines.length" class="mt-4 border-t border-slate-100 pt-4">
      <p class="text-xs font-semibold uppercase tracking-wide text-slate-500">完成标准</p>
      <ul class="mt-2 space-y-1.5 text-sm text-slate-600">
        <li v-for="(line, i) in model.completionLines" :key="i" class="flex gap-2">
          <span class="font-medium text-slate-400">{{ i + 1 }}.</span>
          <span>{{ line }}</span>
        </li>
      </ul>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { CurrentTaskCardModel, WorkbenchPhaseCode } from '@/types/taskExecutionWorkbench'

const props = defineProps<{
  model: CurrentTaskCardModel
  emphasisPhase: WorkbenchPhaseCode
}>()

const emphasisClass = computed(() => {
  if (props.emphasisPhase === 'STRUCTURE') return 'ring-1 ring-slate-200/80'
  if (props.emphasisPhase === 'TRAINING') return 'ring-2 ring-primary/15'
  return ''
})
</script>
