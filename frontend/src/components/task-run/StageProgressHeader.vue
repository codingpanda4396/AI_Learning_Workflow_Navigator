<template>
  <header
    data-testid="stage-progress-header"
    class="rounded-2xl border border-slate-200/90 bg-white px-4 py-3 shadow-sm md:px-5 md:py-3.5"
  >
    <div class="flex flex-wrap items-center justify-between gap-3">
      <div class="min-w-0 flex flex-wrap items-center gap-2">
        <h1 class="truncate text-base font-semibold text-slate-950 md:text-lg">
          {{ topicName }}
        </h1>
        <span
          v-if="strategyLine"
          class="hidden shrink-0 rounded-full border border-slate-200 bg-slate-50 px-2.5 py-0.5 text-xs font-medium text-slate-600 sm:inline"
        >
          {{ strategyLine }}
        </span>
      </div>
      <div class="flex shrink-0 flex-wrap items-center gap-2">
        <span
          class="rounded-full px-2.5 py-1 text-xs font-semibold"
          :class="statusPillClass"
        >
          {{ taskStatusLabel }}
        </span>
        <button
          type="button"
          class="rounded-lg border border-slate-200 bg-white px-3 py-1.5 text-xs font-medium text-slate-700 transition hover:border-slate-300 hover:bg-slate-50"
          @click="router.push('/plan')"
        >
          回规划
        </button>
      </div>
    </div>

    <div class="mt-3 flex flex-wrap items-center gap-3 text-xs text-slate-500">
      <span>{{ taskIndexLabel }}</span>
      <span class="text-slate-300">|</span>
      <span>本步 {{ stepLabel }}</span>
      <div class="ml-auto min-w-[120px] flex-1 md:max-w-[200px]">
        <div class="h-1.5 overflow-hidden rounded-full bg-slate-100">
          <div
            class="h-full rounded-full bg-primary transition-[width]"
            :style="{ width: `${Math.round(overallRatio * 100)}%` }"
          />
        </div>
      </div>
    </div>

    <div class="mt-3 flex gap-1 border-t border-slate-100 pt-3">
      <div
        v-for="p in phases"
        :key="p"
        class="flex min-w-0 flex-1 flex-col items-center gap-1 rounded-lg px-1 py-1.5 text-center transition"
        :class="
          p === currentPhase
            ? 'bg-primary/10 ring-1 ring-primary/25'
            : 'bg-slate-50/80'
        "
      >
        <span
          class="text-[10px] font-bold uppercase leading-none tracking-wide"
          :class="p === currentPhase ? 'text-primary' : 'text-slate-400'"
        >
          {{ p.slice(0, 3) }}
        </span>
        <span
          class="hidden w-full truncate text-[11px] font-medium leading-tight sm:block"
          :class="p === currentPhase ? 'text-slate-900' : 'text-slate-500'"
        >
          {{ phaseShortZh(p) }}
        </span>
      </div>
    </div>
  </header>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import type { WorkbenchPhaseCode, WorkbenchPhaseProgressModel } from '@/types/taskExecutionWorkbench'

const router = useRouter()

const props = defineProps<{
  topicName: string
  phaseProgress: WorkbenchPhaseProgressModel
  taskStatusLabel: string
  strategyLine?: string
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

function phaseShortZh(code: WorkbenchPhaseCode): string {
  const m: Record<WorkbenchPhaseCode, string> = {
    STRUCTURE: '结构',
    UNDERSTANDING: '机制',
    TRAINING: '表达',
    REFLECTION: '反思',
  }
  return m[code] ?? code
}
</script>
