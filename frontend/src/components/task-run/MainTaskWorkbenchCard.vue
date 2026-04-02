<template>
  <section
    data-testid="main-task-workbench-card"
    class="rounded-2xl border border-slate-200 bg-white p-5 shadow-md ring-1 ring-slate-900/5 md:p-6"
    :class="emphasisClass"
  >
    <p class="text-xs font-semibold uppercase tracking-wide text-slate-500">本轮唯一任务</p>

    <div class="mt-4 space-y-4 border-t border-slate-100 pt-4">
      <div>
        <p class="text-xs font-semibold text-slate-700">现在要做什么</p>
        <p class="mt-1.5 text-base font-semibold leading-snug text-slate-950">{{ model.taskTitle }}</p>
        <p v-if="actionLine" class="mt-1 text-sm text-slate-700">{{ actionLine }}</p>
      </div>

      <div class="border-t border-slate-100 pt-4">
        <p class="text-xs font-semibold text-slate-700">为什么先做这个</p>
        <p class="mt-1.5 text-sm leading-relaxed text-slate-700">{{ whyLine }}</p>
      </div>

      <div class="border-t border-slate-100 pt-4">
        <p class="text-xs font-semibold text-slate-700">你要交付什么</p>
        <p class="mt-1.5 text-sm leading-relaxed text-slate-800">{{ deliverLine }}</p>
      </div>

      <div class="border-t border-slate-100 pt-4">
        <p class="text-xs font-semibold text-slate-700">怎么开始</p>
        <p class="mt-1.5 text-sm font-medium leading-relaxed text-accent">{{ startLine }}</p>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type {
  CurrentTaskCardModel,
  ScaffoldProductModel,
  WhyThisStepModel,
  WorkbenchGuideSectionModel,
  WorkbenchHintRevealModel,
  WorkbenchPhaseCode,
} from '@/types/taskExecutionWorkbench'

const props = defineProps<{
  model: CurrentTaskCardModel
  whyThisStep: WhyThisStepModel
  scaffoldProduct: ScaffoldProductModel
  hintReveal: WorkbenchHintRevealModel
  guideSections: WorkbenchGuideSectionModel[]
  emphasisPhase: WorkbenchPhaseCode
}>()

const actionLine = computed(() => {
  const a = props.model.coreActionLine?.trim() || props.model.currentAction?.trim() || ''
  return a && a !== props.model.taskTitle ? a : ''
})

const whyLine = computed(() => {
  const w = props.model.whyNow?.trim() || props.whyThisStep.whyNow?.trim() || ''
  return w || '先把当前这一步做完，再往下扩展。'
})

const deliverLine = computed(() => {
  const outs = props.model.outputRequirements?.filter((s) => s?.trim()) ?? []
  const merged = outs.length ? outs.slice(0, 2) : props.scaffoldProduct.whatToOutput.filter(Boolean).slice(0, 2)
  if (!merged.length) return '一小段能自检的表述即可。'
  return merged.join(' · ')
})

const startLine = computed(() => {
  const g0 = props.guideSections[0]?.standardHint?.trim()
  if (g0) return g0
  const tip = props.hintReveal.tips?.trim()
  if (tip) return tip
  const step = props.scaffoldProduct.recommendedSteps[0]?.trim()
  if (step) return step
  return '先写一版，再对照反馈收束。'
})

const emphasisClass = computed(() => {
  if (props.emphasisPhase === 'STRUCTURE') return 'ring-2 ring-slate-300/70'
  if (props.emphasisPhase === 'TRAINING') return 'ring-2 ring-accent/22'
  return 'ring-1 ring-slate-200/90'
})
</script>
