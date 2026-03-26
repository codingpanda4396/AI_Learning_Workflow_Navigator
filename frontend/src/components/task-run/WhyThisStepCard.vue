<template>
  <section class="rounded-2xl border border-slate-200 bg-white p-4 shadow-sm">
    <p class="text-xs font-semibold uppercase tracking-wide text-slate-500">为什么现在学这一步</p>
    <p class="mt-2 text-sm leading-relaxed text-slate-800">{{ model.whyNow }}</p>
    <div class="mt-4 border-t border-slate-100 pt-3">
      <p class="text-xs font-medium text-slate-500">跳过风险</p>
      <p class="mt-1 text-sm text-slate-700">{{ model.skipRisk }}</p>
    </div>
    <div class="mt-3">
      <p class="text-xs font-medium text-slate-500">预期收益</p>
      <p class="mt-1 text-sm text-slate-700">{{ model.expectedGain }}</p>
    </div>
  </section>
</template>

<script setup lang="ts">
import type { WhyThisStepModel } from '@/types/taskExecutionWorkbench'

defineProps<{
  model: WhyThisStepModel
}>()
</script>
