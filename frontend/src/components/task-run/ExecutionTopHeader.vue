<template>
  <header
    data-testid="execution-top-header"
    class="sticky top-0 z-20 -mx-4 border-b border-border bg-background/95 px-4 py-3 backdrop-blur md:-mx-6 md:px-6 lg:-mx-8 lg:px-8"
  >
    <div class="flex items-center justify-between gap-4">
      <div class="min-w-0 shrink-0">
        <p class="truncate text-sm font-semibold text-slate-950">{{ topicName }}</p>
        <p class="mt-0.5 text-[11px] font-medium uppercase tracking-wide text-slate-400">
          阶段学习工作台
        </p>
      </div>

      <div class="flex min-w-0 flex-1 items-center gap-0 rounded-lg border border-slate-200/80 bg-slate-50/80 p-0.5">
        <div
          v-for="(p, idx) in phaseEntries"
          :key="p.key"
          class="relative flex min-w-0 flex-1 flex-col items-center rounded-md px-1 py-1.5 text-center transition-all duration-200"
          :class="p.key === phase
            ? 'bg-white shadow-card ring-1 ring-accent/25'
            : isPhaseCompleted(idx)
              ? 'text-slate-600'
              : 'text-slate-400'"
        >
          <span
            class="flex items-center gap-1 text-[10px] font-semibold leading-tight"
            :class="p.key === phase ? 'text-accent' : ''"
          >
            <svg
              v-if="isPhaseCompleted(idx)"
              class="h-3 w-3 shrink-0 text-emerald-500"
              viewBox="0 0 20 20"
              fill="currentColor"
            >
              <path
                fill-rule="evenodd"
                d="M16.707 5.293a1 1 0 010 1.414l-8 8a1 1 0 01-1.414 0l-4-4a1 1 0 011.414-1.414L8 12.586l7.293-7.293a1 1 0 011.414 0z"
                clip-rule="evenodd"
              />
            </svg>
            <span class="truncate">{{ p.label }}</span>
          </span>
        </div>
      </div>

      <button
        type="button"
        class="shrink-0 rounded-md px-2.5 py-1.5 text-xs font-medium text-slate-500 transition hover:bg-slate-100 hover:text-slate-700"
        @click="$emit('exit')"
      >
        退出
      </button>
    </div>

    <p class="mt-2.5 text-sm leading-snug text-slate-700">{{ phaseGoal }}</p>
  </header>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { PhaseKey, PhaseProgressModel } from '@/types/executionWorkbench'
import { DFS_BFS_PHASE_STRIP } from '@/constants/dfsBfsExecutionConfig'
import { PHASE_SEQUENCE } from '@/types/executionWorkbench'

const props = defineProps<{
  topicName: string
  phase: PhaseKey
  phaseGoal: string
  phaseProgress: PhaseProgressModel
}>()

defineEmits<{
  exit: []
}>()

const phaseEntries = computed(() =>
  PHASE_SEQUENCE.map((key) => ({
    key,
    label: DFS_BFS_PHASE_STRIP[key],
  })),
)

function isPhaseCompleted(idx: number): boolean {
  const currentIdx = PHASE_SEQUENCE.indexOf(props.phase)
  return idx < currentIdx
}
</script>
