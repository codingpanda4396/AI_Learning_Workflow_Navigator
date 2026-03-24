<template>
  <header
    data-testid="execution-task-header"
    class="overflow-hidden rounded-[32px] border border-slate-200 bg-[linear-gradient(180deg,_rgba(255,255,255,0.98),_rgba(248,250,252,0.98))] p-5 shadow-card md:p-7"
  >
    <div class="flex flex-wrap items-start justify-between gap-4">
      <div class="min-w-0 flex-1">
        <div class="flex flex-wrap items-center gap-2 text-xs font-semibold uppercase tracking-[0.18em] text-slate-500">
          <span>{{ model.stageLabel }}</span>
          <span class="text-slate-300">·</span>
          <span>步骤 {{ model.stepLabel }}</span>
          <template v-if="model.taskLabel">
            <span class="text-slate-300">·</span>
            <span>{{ model.taskLabel }}</span>
          </template>
        </div>
        <h1 class="mt-3 text-2xl font-semibold tracking-tight text-slate-950 md:text-4xl">
          {{ model.title }}
        </h1>
        <p class="mt-3 max-w-3xl text-sm leading-6 text-slate-600 md:text-base">
          {{ model.subtitle }}
        </p>
      </div>

      <div class="rounded-[22px] border border-slate-200 bg-slate-50 px-4 py-3 text-right">
        <p class="text-xs font-semibold uppercase tracking-[0.18em] text-slate-500">预计耗时</p>
        <p class="mt-2 text-lg font-semibold text-slate-950">{{ model.estimatedTime }}</p>
      </div>
    </div>
  </header>
</template>

<script setup lang="ts">
import type { ExecutionGuideHeaderModel } from '@/types/executionGuide'

defineProps<{
  model: ExecutionGuideHeaderModel
}>()
</script>
