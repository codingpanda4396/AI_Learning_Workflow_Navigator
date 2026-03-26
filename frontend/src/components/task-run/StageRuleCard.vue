<template>
  <section class="rounded-2xl border border-slate-200 bg-white p-4 shadow-sm">
    <p class="text-xs font-semibold uppercase tracking-wide text-slate-500">本阶段规则</p>
    <ul class="mt-3 space-y-2 text-sm leading-relaxed text-slate-700">
      <li v-for="(rule, i) in model.rules" :key="i" class="flex gap-2">
        <span class="font-medium text-primary">·</span>
        <span>{{ rule }}</span>
      </li>
    </ul>
  </section>
</template>

<script setup lang="ts">
import type { StageRulesModel } from '@/types/taskExecutionWorkbench'

defineProps<{
  model: StageRulesModel
}>()
</script>
