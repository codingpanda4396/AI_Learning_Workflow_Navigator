<template>
  <aside class="space-y-4">
    <section
      class="overflow-hidden rounded-[28px] border border-slate-200 bg-gradient-to-br from-slate-900 via-slate-800 to-slate-900 p-5 text-white shadow-[0_20px_50px_rgba(15,23,42,0.22)]"
    >
      <p class="text-xs font-semibold uppercase tracking-[0.24em] text-slate-300">
        Task Context
      </p>
      <h2 class="mt-3 text-lg font-semibold leading-tight">
        {{ title }}
      </h2>
      <div class="mt-4 flex flex-wrap items-center gap-2 text-xs">
        <StatusBadge :label="currentStageLabel" />
        <StatusBadge :label="taskStateLabel" :variant="stateVariant" />
      </div>
      <p v-if="progressText" class="mt-4 text-sm text-slate-300">
        {{ progressText }}
      </p>
      <p v-if="guidancePhaseLabel" class="mt-1 text-sm text-slate-300">
        引导相位：{{ guidancePhaseLabel }}
      </p>
    </section>

    <section class="rounded-[24px] border border-border bg-white p-5 shadow-card">
      <div class="flex items-center justify-between gap-3">
        <div>
          <p class="text-xs font-semibold uppercase tracking-[0.22em] text-text-secondary">
            Workflow Rail
          </p>
          <p class="mt-1 text-sm font-medium text-text-primary">
            当前落点：{{ currentStepLabel }}
          </p>
        </div>
        <StatusBadge :label="taskStateLabel" :variant="stateVariant" />
      </div>

      <div class="mt-5 space-y-3">
        <div
          v-for="step in steps"
          :key="step.id"
          class="relative rounded-[18px] border px-4 py-3 transition"
          :class="stepClass(step.id)"
        >
          <div class="flex items-start gap-3">
            <div
              class="mt-0.5 flex h-7 w-7 shrink-0 items-center justify-center rounded-full text-xs font-semibold"
              :class="stepDotClass(step.id)"
            >
              {{ step.index }}
            </div>
            <div>
              <p class="text-sm font-semibold">{{ step.label }}</p>
              <p v-if="step.hint" class="mt-1 text-xs leading-5 opacity-80">
                {{ step.hint }}
              </p>
            </div>
          </div>
        </div>
      </div>
    </section>

    <section class="grid grid-cols-2 gap-3">
      <MetricCard
        v-for="metric in metrics"
        :key="metric.label"
        :title="metric.label"
      >
        {{ metric.value }}
      </MetricCard>
    </section>

    <section
      v-if="guidanceTitle || guidanceBullets.length"
      class="rounded-[24px] border border-border bg-white p-5 shadow-card"
    >
      <p class="text-xs font-semibold uppercase tracking-[0.22em] text-text-secondary">
        Guidance
      </p>
      <p class="mt-2 text-sm font-semibold text-text-primary">
        {{ guidanceTitle || '沿着当前脚手架推进' }}
      </p>
      <ul
        v-if="guidanceBullets.length"
        class="mt-3 list-disc space-y-2 pl-5 text-sm leading-6 text-text-secondary"
      >
        <li v-for="(bullet, index) in guidanceBullets" :key="index">
          {{ bullet }}
        </li>
      </ul>
    </section>
  </aside>
</template>

<script setup lang="ts">
import MetricCard from '@/components/ui/MetricCard.vue'
import StatusBadge from '@/components/ui/StatusBadge.vue'
import type { TaskGuidedStep } from '@/utils/taskGuidedSteps'

interface SidebarMetric {
  label: string
  value: string
}

const props = withDefaults(
  defineProps<{
    title: string
    currentStageLabel: string
    currentStepLabel: string
    taskStateLabel: string
    stateVariant: 'default' | 'success' | 'warning' | 'error'
    progressText?: string
    guidancePhaseLabel?: string
    guidanceTitle?: string
    guidanceBullets?: string[]
    steps: TaskGuidedStep[]
    currentStepId: string
    metrics: SidebarMetric[]
  }>(),
  {
    progressText: '',
    guidancePhaseLabel: '',
    guidanceTitle: '',
    guidanceBullets: () => [],
  }
)

function stepClass(stepId: string) {
  if (stepId === props.currentStepId) {
    return 'border-primary/40 bg-primary/5 text-text-primary shadow-[0_12px_30px_rgba(42,157,143,0.12)]'
  }
  const activeIndex = props.steps.findIndex((step) => step.id === props.currentStepId)
  const stepIndex = props.steps.findIndex((step) => step.id === stepId)
  if (activeIndex >= 0 && stepIndex >= 0 && stepIndex < activeIndex) {
    return 'border-emerald-200 bg-emerald-50/80 text-emerald-900'
  }
  return 'border-border bg-slate-50/65 text-text-secondary'
}

function stepDotClass(stepId: string) {
  if (stepId === props.currentStepId) {
    return 'bg-primary text-white'
  }
  const activeIndex = props.steps.findIndex((step) => step.id === props.currentStepId)
  const stepIndex = props.steps.findIndex((step) => step.id === stepId)
  if (activeIndex >= 0 && stepIndex >= 0 && stepIndex < activeIndex) {
    return 'bg-emerald-600 text-white'
  }
  return 'bg-slate-200 text-slate-700'
}
</script>
