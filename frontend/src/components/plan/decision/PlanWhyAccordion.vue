<template>
  <section class="rounded-2xl border border-slate-200/60 bg-white px-4 py-1 shadow-sm md:px-5">
    <button
      type="button"
      class="flex w-full items-center justify-between gap-3 py-3 text-left"
      :aria-expanded="expanded"
      @click="$emit('update:expanded', !expanded)"
    >
      <span class="text-sm font-semibold text-slate-900">
        {{ model.accordionTitle }}
      </span>
      <span
        class="text-slate-400 transition-transform"
        :class="expanded ? 'rotate-180' : ''"
        aria-hidden="true"
      >
        <svg class="h-5 w-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 9l-7 7-7-7" />
        </svg>
      </span>
    </button>
    <div
      v-show="expanded"
      class="border-t border-slate-100 pb-4 pt-1"
    >
      <ul class="space-y-3">
        <li
          v-for="row in model.bullets"
          :key="row.label"
          class="text-sm leading-relaxed text-slate-700"
        >
          <span class="font-medium text-slate-500">{{ row.label }}：</span>
          {{ row.text }}
        </li>
      </ul>
    </div>
  </section>
</template>

<script setup lang="ts">
import type { LearningPlanDecisionViewModel } from '@/types/learningPlanDecision'

defineProps<{
  model: LearningPlanDecisionViewModel['reasoning']
  expanded: boolean
}>()

defineEmits<{
  'update:expanded': [value: boolean]
}>()
</script>
