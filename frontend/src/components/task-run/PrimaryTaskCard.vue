<template>
  <section
    data-testid="current-task-card"
    class="rounded-card border border-border bg-white p-5 shadow-card md:p-6"
    :class="emphasisClass"
  >
    <div class="flex flex-wrap items-center gap-2">
      <span
        class="rounded-full bg-accent-muted/90 px-2.5 py-0.5 text-xs font-semibold text-accent-hover"
      >
        {{ phaseLabel }}
      </span>
      <span class="rounded-full bg-slate-100 px-2.5 py-0.5 text-xs font-medium text-slate-700">
        {{ model.currentAction }}
      </span>
    </div>
    <p class="mt-3 text-lg font-semibold leading-snug text-text-primary md:text-xl">
      {{ model.taskTitle }}
    </p>
    <p class="mt-3 text-sm leading-6 text-text-secondary">
      <span class="font-semibold text-text-primary">{{ EXECUTION_COPY.currentTaskSingleThing }}</span>
      {{ model.objective }}
    </p>
    <div class="mt-4 grid gap-4 border-t border-border pt-4 md:grid-cols-2">
      <div>
        <p class="text-xs font-semibold text-text-secondary">为什么先做这一步</p>
        <p class="mt-2 text-sm leading-6 text-text-secondary">{{ model.whyNow }}</p>
      </div>
      <div v-if="(model.outputRequirements ?? []).length">
        <p class="text-xs font-semibold text-text-secondary">这一步要产出什么</p>
        <ul class="mt-2 space-y-1.5 text-sm text-text-secondary">
          <li v-for="(line, i) in (model.outputRequirements ?? [])" :key="`out-${i}`" class="flex gap-2">
            <span class="shrink-0 font-medium text-slate-400">{{ i + 1 }}.</span>
            <span>{{ line }}</span>
          </li>
        </ul>
      </div>
    </div>
    <div v-if="model.completionLines.length" class="mt-4 border-t border-border pt-4">
      <p class="text-xs font-semibold text-text-secondary">{{ EXECUTION_COPY.completionCriteriaTitle }}</p>
      <ul class="mt-2 space-y-1.5 text-sm text-text-secondary">
        <li v-for="(line, i) in model.completionLines" :key="i" class="flex gap-2">
          <span class="shrink-0 font-medium text-slate-400">{{ i + 1 }}.</span>
          <span>{{ line }}</span>
        </li>
      </ul>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { EXECUTION_COPY } from '@/constants/uiCopy'
import { phaseCodeToFullZh } from '@/constants/stageLabels'
import type { CurrentTaskCardModel, WorkbenchPhaseCode } from '@/types/taskExecutionWorkbench'

const props = defineProps<{
  model: CurrentTaskCardModel
  emphasisPhase: WorkbenchPhaseCode
}>()

const phaseLabel = computed(() => props.model.phaseDisplayZh || phaseCodeToFullZh(props.model.phaseCode))

const emphasisClass = computed(() => {
  if (props.emphasisPhase === 'STRUCTURE') return 'ring-2 ring-slate-300/80'
  if (props.emphasisPhase === 'TRAINING') return 'ring-2 ring-accent/25'
  if (props.emphasisPhase === 'REFLECTION') return 'ring-1 ring-slate-200'
  return 'ring-1 ring-slate-200/80'
})
</script>
