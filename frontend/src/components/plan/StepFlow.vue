<template>
  <div class="mb-8">
    <p class="mb-4 text-xs font-medium tracking-wide text-text-secondary">
      咱们大概会走这几步
    </p>
    <div
      class="-mx-1 flex gap-2 overflow-x-auto pb-2 md:mx-0 md:flex-wrap md:overflow-visible"
      role="list"
    >
      <div
        v-for="item in items"
        :key="item.stepIndex"
        role="listitem"
        class="flex min-w-[148px] shrink-0 flex-col rounded-card border px-3 py-3 md:min-w-0 md:flex-1 md:shrink md:px-4"
        :class="cardClass(item.status)"
      >
        <div class="flex items-start gap-2">
          <span
            class="mt-0.5"
            :class="iconWrapClass(item.status)"
          >
            <PlanStepIcon :name="item.icon" size="sm" />
          </span>
          <div class="min-w-0 flex-1">
            <span class="text-xs font-medium text-text-secondary">
              第 {{ item.stepIndex }} 步
            </span>
            <span
              class="mt-1 block text-sm font-semibold leading-snug"
              :class="titleClass(item.status)"
            >
              {{ item.title }}
            </span>
            <span
              class="mt-1 block text-xs leading-relaxed"
              :class="subtitleClass(item.status)"
            >
              {{ item.subtitle }}
            </span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import PlanStepIcon from '@/components/plan/PlanStepIcon.vue'
import type { StepFlowItem, StepFlowStatus } from '@/utils/planPresentationModel'

defineProps<{
  items: StepFlowItem[]
}>()

function cardClass(status: StepFlowStatus): string {
  if (status === 'CURRENT') {
    return 'border-2 border-primary bg-primary/5 shadow-md ring-1 ring-primary/25'
  }
  if (status === 'DONE') {
    return 'border border-border bg-slate-50/80 opacity-80'
  }
  return 'border border-dashed border-border bg-white/50 opacity-55 grayscale-[0.35]'
}

function iconWrapClass(status: StepFlowStatus): string {
  if (status === 'CURRENT') return 'text-primary'
  if (status === 'DONE') return 'text-text-secondary'
  return 'text-text-secondary/70'
}

function titleClass(status: StepFlowStatus): string {
  if (status === 'CURRENT') return 'text-primary'
  if (status === 'DONE') return 'text-text-secondary line-through decoration-text-secondary/50'
  return 'text-text-secondary'
}

function subtitleClass(status: StepFlowStatus): string {
  if (status === 'CURRENT') return 'text-text-secondary'
  if (status === 'DONE') return 'text-text-secondary/70'
  return 'text-text-secondary/60'
}
</script>
