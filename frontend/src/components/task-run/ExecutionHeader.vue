<template>
  <header
    data-testid="stage-progress-header"
    class="rounded-xl border border-slate-200/90 bg-white px-3 py-2.5 shadow-sm md:px-4"
  >
    <div class="flex flex-wrap items-center justify-between gap-2">
      <div class="min-w-0 flex flex-1 flex-wrap items-center gap-2">
        <h1 class="truncate text-sm font-semibold text-slate-950 md:text-base">
          {{ topicName }}
        </h1>
      </div>
      <div class="flex shrink-0 flex-wrap items-center gap-1.5">
        <span
          class="rounded-full px-2 py-0.5 text-[11px] font-semibold"
          :class="statusPillClass"
        >
          {{ taskStatusLabel }}
        </span>
        <button
          type="button"
          class="rounded-md border border-slate-200 bg-white px-2.5 py-1 text-[11px] font-medium text-slate-700 transition hover:bg-slate-50"
          @click="router.push('/plan')"
        >
          {{ EXECUTION_COPY.backToPlan }}
        </button>
      </div>
    </div>

    <div class="mt-2 flex flex-wrap items-center gap-2 text-[11px] text-slate-500">
      <span>{{ taskIndexLabel }}</span>
      <span class="text-slate-300">·</span>
      <span>本步 {{ stepLabel }}</span>
      <div class="ml-auto min-w-[100px] flex-1 md:max-w-[160px]">
        <div class="h-1 overflow-hidden rounded-full bg-slate-100">
          <div
            class="h-full rounded-full bg-primary transition-[width]"
            :style="{ width: `${Math.round(overallRatio * 100)}%` }"
          />
        </div>
      </div>
    </div>

    <div class="mt-2 flex gap-0.5 border-t border-slate-100 pt-2">
      <div
        v-for="p in phases"
        :key="p"
        class="flex min-w-0 flex-1 flex-col items-center gap-0.5 rounded-md px-0.5 py-1 text-center transition"
        :class="p === currentPhase ? 'bg-primary/10 ring-1 ring-primary/20' : 'bg-slate-50/80'"
      >
        <span
          class="w-full truncate text-center text-[10px] font-medium leading-tight sm:hidden"
          :class="p === currentPhase ? 'text-primary' : 'text-slate-500'"
        >
          {{ phaseCodeToShortZh(p) }}
        </span>
        <span
          class="hidden w-full truncate text-center text-[10px] font-medium leading-tight sm:block"
          :class="p === currentPhase ? 'text-slate-900' : 'text-slate-500'"
        >
          {{ phaseCodeToFullZh(p) }}
        </span>
      </div>
    </div>
  </header>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { phaseCodeToFullZh, phaseCodeToShortZh } from '@/constants/stageLabels'
import { EXECUTION_COPY } from '@/constants/uiCopy'
import type { WorkbenchPhaseProgressModel } from '@/types/taskExecutionWorkbench'

const router = useRouter()

const props = defineProps<{
  topicName: string
  phaseProgress: WorkbenchPhaseProgressModel
  taskStatusLabel: string
}>()

const phases = computed(() => props.phaseProgress.phases)
const currentPhase = computed(() => props.phaseProgress.currentPhase)
const overallRatio = computed(() => props.phaseProgress.overallRatio)
const stepLabel = computed(() => props.phaseProgress.stepLabel)
const taskIndexLabel = computed(() => props.phaseProgress.taskIndexLabel)

const statusPillClass = computed(() => {
  const s = props.taskStatusLabel
  if (s.includes('可进入') || s.includes('下一阶段')) return 'bg-emerald-50 text-emerald-800 ring-1 ring-emerald-200/80'
  if (s.includes('待修正')) return 'bg-amber-50 text-amber-900 ring-1 ring-amber-200/80'
  if (s.includes('已提交')) return 'bg-sky-50 text-sky-900 ring-1 ring-sky-200/80'
  return 'bg-slate-100 text-slate-700 ring-1 ring-slate-200/80'
})

</script>
