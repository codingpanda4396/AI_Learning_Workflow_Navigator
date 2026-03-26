<script setup lang="ts">
import type { LearningActionCard as Card } from '@/types/scaffoldEngine'

defineProps<{
  card: Card | null
}>()
</script>

<template>
  <div class="space-y-3">
    <div class="rounded-2xl border border-slate-200/80 bg-white/90 p-4 shadow-sm">
      <h3 class="text-sm font-semibold text-slate-900">可以怎么问</h3>
      <ul class="mt-2 list-inside list-disc space-y-1 text-xs leading-relaxed text-slate-600">
        <li v-for="(p, i) in card?.allowedPrompts ?? []" :key="'a-' + i">{{ p }}</li>
      </ul>
    </div>
    <div class="rounded-2xl border border-rose-200/60 bg-rose-50/50 p-4 shadow-sm">
      <h3 class="text-sm font-semibold text-rose-900">本阶段先别做</h3>
      <ul class="mt-2 list-inside list-disc space-y-1 text-xs leading-relaxed text-rose-800/90">
        <li v-for="(p, i) in card?.forbiddenPrompts ?? []" :key="'f-' + i">{{ p }}</li>
      </ul>
    </div>
    <div class="rounded-2xl border border-emerald-200/60 bg-emerald-50/50 p-4 shadow-sm">
      <h3 class="text-sm font-semibold text-emerald-900">通过标准</h3>
      <ul class="mt-2 list-inside list-disc space-y-1 text-xs leading-relaxed text-emerald-900/90">
        <li v-for="(p, i) in card?.passCriteria ?? []" :key="'c-' + i">{{ p }}</li>
      </ul>
    </div>
  </div>
</template>
