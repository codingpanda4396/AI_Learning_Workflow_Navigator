<template>
  <header
    data-testid="execution-stage-header"
    class="border-b border-slate-200/80 bg-[color-mix(in_srgb,var(--color-page-bg,#f8fafc)_96%,white)] px-1 py-2 md:px-2"
  >
    <div class="flex flex-wrap items-start justify-between gap-2">
      <div class="min-w-0 flex-1">
        <h1 class="truncate text-base font-semibold tracking-tight text-slate-950 md:text-lg">
          {{ topicName }}
        </h1>
      </div>
      <div class="flex shrink-0 flex-wrap items-center gap-1.5">
        <button
          type="button"
          class="rounded-md border border-slate-200 bg-white px-2.5 py-1 text-[11px] font-medium text-slate-700 transition hover:bg-slate-50"
          @click="$emit('help')"
        >
          {{ TASKRUN_COPY.workbenchHelp }}
        </button>
        <button
          type="button"
          class="rounded-md border border-slate-200 bg-white px-2.5 py-1 text-[11px] font-medium text-slate-700 transition hover:bg-slate-50"
          @click="router.push('/plan')"
        >
          {{ TASKRUN_COPY.workbenchExit }}
        </button>
      </div>
    </div>

    <div class="mt-3 flex gap-0 border-t border-slate-200/80 pt-2.5">
      <div
        v-for="p in phases"
        :key="p"
        class="flex min-w-0 flex-1 flex-col items-center gap-0.5 rounded-md px-0.5 py-1 text-center transition"
        :class="p === currentPhase ? 'bg-primary/10 ring-1 ring-primary/20' : 'bg-slate-50/80'"
      >
        <span
          class="w-full truncate text-center text-[10px] font-medium leading-tight text-slate-500 sm:hidden"
          :class="p === currentPhase ? 'text-primary' : ''"
        >
          {{ phaseCodeToShortZh(p) }}
        </span>
        <span
          class="hidden w-full truncate text-center text-[10px] font-medium leading-tight sm:block"
          :class="p === currentPhase ? 'font-semibold text-slate-900' : 'text-slate-500'"
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
import { PHASE_STRIP_LABELS_ZH, phaseCodeToFullZh, phaseCodeToShortZh } from '@/constants/stageLabels'
import { TASKRUN_COPY } from '@/constants/uiCopy'
import type { WorkbenchPhaseProgressModel } from '@/types/taskExecutionWorkbench'

const props = defineProps<{
  topicName: string
  phaseProgress: WorkbenchPhaseProgressModel
}>()

defineEmits<{
  help: []
}>()

const router = useRouter()

function phaseStripLabel(code: string) {
  const k = code as keyof typeof PHASE_STRIP_LABELS_ZH
  return PHASE_STRIP_LABELS_ZH[k] ?? phaseCodeToFullZh(code)
}

const phases = computed(() => props.phaseProgress.phases)
const currentPhase = computed(() => props.phaseProgress.currentPhase)
</script>
