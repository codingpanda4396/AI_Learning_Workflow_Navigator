<template>
  <div class="mb-8">
    <div class="mb-3 flex flex-wrap items-center justify-between gap-2">
      <p class="text-xs font-medium tracking-wide text-text-secondary">
        咱们大概会走这几步
      </p>
      <p class="text-xs font-semibold text-text-primary">
        第 {{ currentStepNumber }} 步 / 共 {{ items.length }} 步
      </p>
    </div>
    <div
      class="mb-4 h-1.5 w-full overflow-hidden rounded-full bg-slate-200/90"
      role="progressbar"
      :aria-valuenow="currentStepNumber"
      :aria-valuemin="1"
      :aria-valuemax="items.length || 1"
      aria-label="学习路径进度"
    >
      <div
        class="h-full rounded-full bg-accent transition-[width] duration-300 ease-out"
        :style="{ width: progressPercent }"
      />
    </div>
    <div
      class="-mx-1 flex gap-2 overflow-x-auto pb-2 md:mx-0 md:flex-wrap md:overflow-visible"
      role="list"
    >
      <div
        v-for="item in items"
        :key="item.stepIndex"
        role="listitem"
        class="flex min-w-[148px] shrink-0 flex-col rounded-card border px-3 py-3 md:min-w-0 md:flex-1 md:shrink md:px-4"
        :class="cardClass(item)"
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
import { computed } from 'vue'
import PlanStepIcon from '@/components/plan/PlanStepIcon.vue'
import type { StepFlowItem, StepFlowStatus } from '@/utils/planPresentationModel'

const props = defineProps<{
  items: StepFlowItem[]
}>()

const currentStepNumber = computed(() => {
  const cur = props.items.find((i) => i.status === 'CURRENT')
  return cur?.stepIndex ?? props.items[0]?.stepIndex ?? 1
})

const progressPercent = computed(() => {
  const n = props.items.length
  if (!n) return '0%'
  const cur = props.items.find((i) => i.status === 'CURRENT')
  const idx = cur ? cur.stepIndex : 1
  const pct = Math.min(100, Math.max(0, (idx / n) * 100))
  return `${pct}%`
})

function cardClass(item: StepFlowItem): string {
  const { status } = item
  if (status === 'CURRENT') {
    return 'border-2 border-solid border-accent bg-accent-muted/70 shadow-md ring-1 ring-accent/25'
  }
  if (status === 'DONE') {
    return 'border border-solid border-border bg-slate-50/80 opacity-80'
  }
  return 'border border-dashed border-slate-300/90 bg-white/40 opacity-50 grayscale-[0.25]'
}

function iconWrapClass(status: StepFlowStatus): string {
  if (status === 'CURRENT') return 'text-accent'
  if (status === 'DONE') return 'text-text-secondary'
  return 'text-text-secondary/70'
}

function titleClass(status: StepFlowStatus): string {
  if (status === 'CURRENT') return 'text-accent-hover'
  if (status === 'DONE') return 'text-text-secondary line-through decoration-text-secondary/50'
  return 'text-text-secondary'
}

function subtitleClass(status: StepFlowStatus): string {
  if (status === 'CURRENT') return 'text-text-secondary'
  if (status === 'DONE') return 'text-text-secondary/70'
  return 'text-text-secondary/60'
}
</script>
