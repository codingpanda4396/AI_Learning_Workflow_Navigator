<template>
  <aside data-testid="execution-progress-rail" class="rounded-[28px] border border-slate-200 bg-white p-5 shadow-card">
    <p class="text-xs font-semibold uppercase tracking-[0.18em] text-slate-500">当前进度感</p>
    <h3 class="mt-2 text-lg font-semibold text-slate-950">你正在推进一个有顺序的学习过程</h3>

    <div class="mt-5 space-y-3">
      <article class="rounded-[20px] border border-emerald-100 bg-emerald-50/80 p-4">
        <p class="text-sm font-semibold text-emerald-900">已完成</p>
        <p class="mt-2 text-sm leading-6 text-emerald-900">{{ model.done }}</p>
      </article>

      <article class="rounded-[20px] border border-primary/15 bg-primary/5 p-4">
        <p class="text-sm font-semibold text-primary">进行中</p>
        <p class="mt-2 text-sm leading-6 text-slate-700">{{ model.current }}</p>
      </article>

      <article class="rounded-[20px] border border-slate-200 bg-slate-50/80 p-4">
        <p class="text-sm font-semibold text-slate-900">下一步</p>
        <p class="mt-2 text-sm leading-6 text-slate-700">{{ model.next }}</p>
      </article>

      <article class="rounded-[20px] border border-slate-200 bg-slate-50/60 p-4">
        <p class="text-sm font-semibold text-slate-900">后续</p>
        <p class="mt-2 text-sm leading-6 text-slate-700">{{ model.later }}</p>
      </article>
    </div>
  </aside>
</template>

<script setup lang="ts">
import type { ExecutionGuideProgressRailModel } from '@/types/executionGuide'

defineProps<{
  model: ExecutionGuideProgressRailModel
}>()
</script>
