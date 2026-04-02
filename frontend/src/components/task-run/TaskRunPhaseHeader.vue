<template>
  <header
    data-testid="task-run-phase-header"
    class="sticky top-0 z-20 -mx-4 border-b border-slate-200/90 bg-[color-mix(in_srgb,var(--color-page-bg,#f8fafc)_96%,white)] px-4 py-3 backdrop-blur md:-mx-6 md:px-6 lg:-mx-8 lg:px-8"
  >
    <div class="flex flex-wrap items-start justify-between gap-2">
      <div class="min-w-0 flex-1">
        <p class="truncate text-sm font-semibold text-slate-950">{{ topicName }}</p>
        <p class="mt-1 text-xs font-medium uppercase tracking-wide text-slate-500">当前阶段</p>
        <p class="mt-0.5 text-sm font-semibold text-slate-900">
          <span class="font-mono text-xs text-accent">{{ currentPhaseCode }}</span>
          <span class="mx-1.5 text-slate-300">·</span>
          <span>{{ phaseDisplayZh }}</span>
        </p>
      </div>
      <button
        type="button"
        class="shrink-0 rounded-lg border border-slate-200 bg-white px-2.5 py-1 text-xs font-medium text-slate-700 transition hover:bg-slate-50"
        @click="router.push('/plan')"
      >
        返回规划
      </button>
    </div>

    <div class="mt-3 flex gap-0 rounded-lg border border-slate-200/80 bg-slate-50/80 p-0.5">
      <div
        v-for="p in phases"
        :key="p"
        class="flex min-w-0 flex-1 flex-col items-center rounded-md px-1 py-1.5 text-center"
        :class="p === currentPhase ? 'bg-white shadow-sm ring-1 ring-accent/25' : ''"
      >
        <span
          class="w-full truncate text-[10px] font-medium leading-tight"
          :class="p === currentPhase ? 'text-accent' : 'text-slate-500'"
        >
          {{ phaseStripLabel(p) }}
        </span>
      </div>
    </div>

    <p v-if="goalLine" class="mt-3 text-sm leading-snug text-slate-800">
      {{ goalLine }}
    </p>

    <div class="mt-2 flex flex-wrap items-baseline gap-x-3 gap-y-1 text-xs text-slate-600">
      <span>
        进度 <span class="font-semibold text-slate-900">{{ stepLabel }}</span>
      </span>
      <span v-if="taskIndexLabel" class="text-slate-500">{{ taskIndexLabel }}</span>
    </div>

    <p v-if="phaseHint" class="mt-2 rounded-lg bg-slate-100/80 px-3 py-2 text-xs leading-relaxed text-slate-700">
      {{ phaseHint }}
    </p>
  </header>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { PHASE_STRIP_LABELS_ZH, phaseCodeToFullZh } from '@/constants/stageLabels'
import type { WorkbenchPhaseCode, WorkbenchPhaseProgressModel } from '@/types/taskExecutionWorkbench'

const props = defineProps<{
  topicName: string
  phaseProgress: WorkbenchPhaseProgressModel
  goalLine: string
  phaseHint: string
}>()

const router = useRouter()

const phases = computed(() => props.phaseProgress.phases)
const currentPhase = computed(() => props.phaseProgress.currentPhase)
const currentPhaseCode = computed(() => props.phaseProgress.currentPhase)
const phaseDisplayZh = computed(() => phaseCodeToFullZh(props.phaseProgress.currentPhase))
const stepLabel = computed(() => props.phaseProgress.stepLabel)
const taskIndexLabel = computed(() => props.phaseProgress.taskIndexLabel)

function phaseStripLabel(code: string) {
  const k = code as keyof typeof PHASE_STRIP_LABELS_ZH
  return PHASE_STRIP_LABELS_ZH[k as WorkbenchPhaseCode] ?? phaseCodeToFullZh(code)
}
</script>
