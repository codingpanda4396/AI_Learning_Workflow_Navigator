<template>
  <section class="rounded-xl border border-slate-200 bg-white p-3 shadow-sm">
    <p class="text-[10px] font-semibold uppercase tracking-wide text-slate-400">为什么是这一步</p>
    <p class="mt-2 truncate text-sm text-slate-800" :title="model.whyNow">{{ model.whyNow }}</p>
    <p class="mt-2 truncate text-sm text-slate-700" :title="model.skipRisk">
      <span class="text-slate-400">跳过：</span>{{ model.skipRisk }}
    </p>
    <p class="mt-2 truncate text-sm text-slate-700" :title="model.expectedGain">
      <span class="text-slate-400">做完：</span>{{ model.expectedGain }}
    </p>
  </section>
</template>

<script setup lang="ts">
import type { WhyThisStepModel } from '@/types/taskExecutionWorkbench'

defineProps<{
  model: WhyThisStepModel
}>()
</script>
