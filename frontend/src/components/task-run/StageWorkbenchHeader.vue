<template>
  <header
    data-testid="stage-workbench-header"
    class="sticky top-0 z-20 -mx-4 border-b border-slate-200/90 bg-[color-mix(in_srgb,var(--color-page-bg,#f8fafc)_96%,white)] px-4 py-3 backdrop-blur md:-mx-6 md:px-6 lg:-mx-8 lg:px-8"
  >
    <div class="flex flex-wrap items-start justify-between gap-2">
      <div class="min-w-0 flex-1">
        <p class="truncate text-sm font-semibold text-slate-950">{{ topicName }}</p>
        <p class="mt-1 text-xs font-medium uppercase tracking-wide text-slate-500">阶段学习工作台</p>
        <p class="mt-0.5 text-sm font-semibold text-slate-900">
          <span class="font-mono text-xs text-accent">{{ emphasisPhase }}</span>
          <span class="mx-1.5 text-slate-300">·</span>
          <span>{{ cognitiveAction }}</span>
        </p>
      </div>
    </div>

    <div class="mt-3 flex gap-0 rounded-lg border border-slate-200/80 bg-slate-50/80 p-0.5">
      <div
        v-for="p in phases"
        :key="p"
        class="flex min-w-0 flex-1 flex-col items-center rounded-md px-1 py-1.5 text-center"
        :class="p === emphasisPhase ? 'bg-white shadow-sm ring-1 ring-primary/20' : ''"
      >
        <span
          class="w-full truncate text-[10px] font-medium leading-tight"
          :class="p === emphasisPhase ? 'text-accent' : 'text-slate-500'"
        >
          {{ phaseStripLabel(p) }}
        </span>
      </div>
    </div>

    <p v-if="stageGoal" class="mt-3 text-sm leading-snug text-slate-800">{{ stageGoal }}</p>

    <div class="mt-2 flex flex-wrap items-baseline gap-x-3 gap-y-1 text-xs text-slate-600">
      <span v-if="taskIndexLabel" class="text-slate-500">{{ taskIndexLabel }}</span>
      <span>阶段目标：{{ stageGoal || '完成当前主交互并查看反馈' }}</span>
    </div>
  </header>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { PHASE_STRIP_LABELS_ZH } from '@/constants/stageLabels'
import type { WorkbenchPhaseCode, WorkbenchPhaseProgressModel } from '@/types/taskExecutionWorkbench'

const props = defineProps<{
  topicName: string
  cognitiveAction: string
  stageGoal: string
  emphasisPhase: WorkbenchPhaseCode
  phaseProgress: WorkbenchPhaseProgressModel
  taskIndexLabel: string
}>()

const phases = computed(() => props.phaseProgress.phases)

function phaseStripLabel(code: string) {
  return PHASE_STRIP_LABELS_ZH[code as keyof typeof PHASE_STRIP_LABELS_ZH] || code
}
</script>
