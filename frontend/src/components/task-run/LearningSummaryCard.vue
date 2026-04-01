<template>
  <div class="rounded-2xl border border-slate-200/80 bg-white p-6 shadow-sm">
    <p class="mb-3 text-xs font-semibold uppercase tracking-wide text-slate-400">
      本次学习总结
    </p>

    <div v-if="summary" class="space-y-3">
      <div v-if="summary.learnedPoints.length" class="space-y-2">
        <div
          v-for="(point, idx) in summary.learnedPoints"
          :key="idx"
          class="flex items-start gap-2 text-sm leading-relaxed text-slate-700"
        >
          <svg class="mt-0.5 h-4 w-4 shrink-0 text-emerald-500" viewBox="0 0 20 20" fill="currentColor">
            <path
              fill-rule="evenodd"
              d="M16.707 5.293a1 1 0 010 1.414l-8 8a1 1 0 01-1.414 0l-4-4a1 1 0 011.414-1.414L8 12.586l7.293-7.293a1 1 0 011.414 0z"
              clip-rule="evenodd"
            />
          </svg>
          <span>{{ point }}</span>
        </div>
      </div>

      <div v-if="summary.finalUnderstanding" class="mt-3 rounded-lg bg-slate-50 p-4">
        <p class="text-sm leading-relaxed text-slate-700 whitespace-pre-wrap">
          {{ summary.finalUnderstanding }}
        </p>
      </div>
    </div>

    <p v-else class="text-sm text-slate-500">
      系统正在生成学习总结...
    </p>
  </div>
</template>

<script setup lang="ts">
import type { ReflectionSummary } from '@/types/executionWorkbench'

defineProps<{
  summary: ReflectionSummary | null
}>()
</script>
