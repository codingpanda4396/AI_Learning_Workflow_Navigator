<template>
  <section class="space-y-3">
    <h2 class="text-sm font-semibold text-text-primary">学习顺序</h2>
    <div class="grid gap-3 md:grid-cols-[1fr_auto_1fr_auto_1fr_auto_1fr] md:items-stretch">
      <template v-for="(item, index) in items" :key="item.code">
        <div
          class="flex min-w-0 flex-col rounded-2xl border p-3 md:min-h-[104px]"
          :class="nodeClass(item)"
        >
          <div class="flex flex-wrap items-center gap-2">
            <p class="font-mono text-[11px] font-bold tracking-tight text-slate-500">
              {{ item.title }}
            </p>
            <span
              v-if="item.code === expandedStageCode"
              class="rounded-full bg-accent-muted px-2 py-0.5 text-[10px] font-semibold text-accent-hover"
            >
              当前这步
            </span>
            <span
              v-else-if="item.isRecommended"
              class="rounded-full bg-slate-100 px-2 py-0.5 text-[10px] font-medium text-slate-600"
            >
              从这里开始
            </span>
          </div>
          <p class="mt-2 text-sm font-medium leading-snug text-text-primary">
            {{ item.scanLine }}
          </p>
          <p class="mt-auto pt-2 text-xs text-text-secondary">
            {{ item.estimatedLabel }}
          </p>
        </div>
        <div
          v-if="index < items.length - 1"
          class="hidden items-center justify-center text-slate-300 md:flex"
          aria-hidden="true"
        >
          <span class="text-lg font-light">→</span>
        </div>
      </template>
    </div>
  </section>
</template>

<script setup lang="ts">
import type { PlanPathStripItem, PlanStageCode } from '@/utils/planPresentationModel'

const props = defineProps<{
  items: PlanPathStripItem[]
  expandedStageCode: PlanStageCode
}>()

function nodeClass(item: PlanPathStripItem): string {
  const focus = item.code === props.expandedStageCode
  if (focus) {
    return 'border-accent/45 bg-gradient-to-b from-accent-muted/90 to-white shadow-[0_12px_28px_rgba(217,119,6,0.12)] ring-2 ring-accent/25'
  }
  return 'border-slate-200/90 bg-white/70 text-slate-800 shadow-sm'
}
</script>
