<template>
  <header
    data-testid="stage-progress-header"
    :class="headerShellClass"
  >
    <div class="flex flex-wrap items-start justify-between gap-2">
      <div class="min-w-0 flex-1">
        <h1 class="truncate text-base font-semibold tracking-tight text-slate-950 md:text-lg">
          {{ titleOverride ?? topicName }}
        </h1>
        <p v-if="subtitleLine" class="mt-0.5 text-[13px] text-slate-600">
          {{ subtitleLine }}
        </p>
        <p v-else class="mt-0.5 flex flex-wrap items-center gap-x-2 gap-y-0.5 text-[11px] text-slate-500">
          <span>{{ taskIndexLabel }}</span>
          <span class="text-slate-300">·</span>
          <span>本步 {{ stepLabel }}</span>
        </p>
      </div>
      <div class="flex shrink-0 flex-wrap items-center gap-1.5">
        <span
          class="rounded-full px-2.5 py-0.5 text-[11px] font-semibold"
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

    <div
      v-if="!subtitleLine && !compact"
      class="mt-2 flex flex-wrap items-center gap-2 text-[11px] text-slate-500"
    >
      <span>{{ taskIndexLabel }}</span>
      <span class="text-slate-300">·</span>
      <span>本步 {{ stepLabel }}</span>
      <div class="ml-auto min-w-[100px] flex-1 md:max-w-[160px]">
        <div class="h-1 overflow-hidden rounded-full bg-slate-100">
          <div
            class="h-full rounded-full bg-accent transition-[width]"
            :style="{ width: `${Math.round(overallRatio * 100)}%` }"
          />
        </div>
      </div>
    </div>

    <div
      class="mt-3 flex gap-0 border-t border-slate-200/80 pt-3"
      :class="compact ? 'pt-2.5' : ''"
    >
      <div
        v-for="p in phases"
        :key="p"
        class="flex min-w-0 flex-1 flex-col items-center gap-0.5 rounded-md px-0.5 py-1 text-center transition"
        :class="p === currentPhase ? 'bg-accent-muted/70 ring-1 ring-accent/25' : 'bg-slate-50/80'"
      >
        <span
          class="w-full truncate text-center text-[10px] font-medium leading-tight text-slate-500 sm:hidden"
          :class="p === currentPhase ? 'text-accent' : ''"
        >
          {{ phaseCodeToShortZh(p) }}
        </span>
        <span
          class="hidden w-full truncate text-center text-[10px] font-medium leading-tight sm:block"
          :class="p === currentPhase ? 'font-semibold text-accent-hover' : 'text-slate-500'"
        >
          {{ phaseStripLabel(p) }}
        </span>
      </div>
    </div>
  </header>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import {
  PHASE_STRIP_LABELS_ZH,
  phaseCodeToFullZh,
  phaseCodeToShortZh,
} from '@/constants/stageLabels'
import { EXECUTION_COPY } from '@/constants/uiCopy'
import type { WorkbenchPhaseProgressModel } from '@/types/taskExecutionWorkbench'

const router = useRouter()

const props = withDefaults(
  defineProps<{
    topicName: string
    /** 覆盖主标题（如结构建立阶段「先搭知识骨架」） */
    titleOverride?: string
    phaseProgress: WorkbenchPhaseProgressModel
    taskStatusLabel: string
    /** 例如：结构建立 · 任务 1 / 5；有则合并任务序号展示 */
    subtitleLine?: string
    /** 轻量顶栏：单任务工作台，减少面板感 */
    compact?: boolean
  }>(),
  { titleOverride: undefined, subtitleLine: undefined, compact: false }
)

const headerShellClass = computed(() =>
  props.compact
    ? 'border-b border-slate-200/80 bg-[color-mix(in_srgb,var(--color-page-bg,#f8fafc)_96%,white)] px-1 py-2 md:px-2'
    : 'rounded-xl border border-slate-200/90 bg-white px-3 py-2.5 shadow-sm md:px-4'
)

function phaseStripLabel(code: string) {
  const k = code as keyof typeof PHASE_STRIP_LABELS_ZH
  return PHASE_STRIP_LABELS_ZH[k] ?? phaseCodeToFullZh(code)
}

const phases = computed(() => props.phaseProgress.phases)
const currentPhase = computed(() => props.phaseProgress.currentPhase)
const overallRatio = computed(() => props.phaseProgress.overallRatio)
const stepLabel = computed(() => props.phaseProgress.stepLabel)
const taskIndexLabel = computed(() => props.phaseProgress.taskIndexLabel)

const statusPillClass = computed(() => {
  const s = props.taskStatusLabel
  if (s.includes('可进入') || s.includes('下一阶段')) return 'bg-emerald-50 text-emerald-800 ring-1 ring-emerald-200/80'
  if (s.includes('待修正')) return 'bg-accent-muted/90 text-accent-hover ring-1 ring-accent/25'
  if (s.includes('已提交')) return 'bg-sky-50 text-sky-900 ring-1 ring-sky-200/80'
  return 'bg-slate-100 text-slate-700 ring-1 ring-slate-200/80'
})

</script>
