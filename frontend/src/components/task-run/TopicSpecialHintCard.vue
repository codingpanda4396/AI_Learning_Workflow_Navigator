<template>
  <section class="rounded-2xl border border-slate-200 bg-white p-4 shadow-sm">
    <p class="text-xs font-semibold uppercase tracking-wide text-slate-500">知识点观察视角</p>
    <p class="mt-1 text-sm font-semibold text-slate-900">{{ model.topicDisplayName }}</p>

    <div class="relative mt-3 overflow-hidden rounded-xl bg-slate-50/90 p-3" :class="accentClass">
      <div
        class="pointer-events-none absolute inset-0 opacity-[0.35]"
        aria-hidden="true"
      >
        <svg
          v-if="model.visualVariant === 'graph'"
          class="h-full w-full"
          viewBox="0 0 120 40"
        >
          <circle cx="20" cy="20" r="6" class="fill-primary/30" />
          <circle cx="60" cy="12" r="6" class="fill-primary/20" />
          <circle cx="100" cy="24" r="6" class="fill-primary/25" />
          <line x1="26" y1="20" x2="54" y2="12" class="stroke-slate-300" stroke-width="1" />
          <line x1="66" y1="12" x2="94" y2="24" class="stroke-slate-300" stroke-width="1" />
        </svg>
        <svg
          v-else-if="model.visualVariant === 'timeline'"
          class="h-full w-full"
          viewBox="0 0 120 32"
        >
          <line x1="8" y1="16" x2="112" y2="16" class="stroke-sky-300/80" stroke-width="2" />
          <circle cx="24" cy="16" r="5" class="fill-sky-400/50" />
          <circle cx="60" cy="16" r="5" class="fill-sky-400/40" />
          <circle cx="96" cy="16" r="5" class="fill-sky-400/50" />
        </svg>
        <svg
          v-else-if="model.visualVariant === 'container'"
          class="h-full w-full"
          viewBox="0 0 120 40"
        >
          <rect
            x="8"
            y="8"
            width="104"
            height="26"
            rx="4"
            fill="none"
            class="stroke-violet-300/70"
            stroke-width="1.5"
          />
          <line x1="60" y1="8" x2="60" y2="34" class="stroke-violet-300/50" stroke-dasharray="3 2" />
        </svg>
        <svg v-else class="h-full w-full" viewBox="0 0 120 40">
          <rect x="40" y="4" width="40" height="10" rx="2" class="fill-accent/35" />
          <rect x="20" y="22" width="32" height="10" rx="2" class="fill-accent/25" />
          <rect x="68" y="22" width="32" height="10" rx="2" class="fill-accent/25" />
        </svg>
      </div>
      <ul class="relative space-y-2 text-sm leading-relaxed text-slate-800">
        <li v-for="(b, i) in model.bullets" :key="i" class="flex gap-2">
          <span class="mt-1.5 h-1 w-1 shrink-0 rounded-full bg-accent/80" />
          <span>{{ b }}</span>
        </li>
      </ul>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { TopicSpecialHintModel } from '@/types/taskExecutionWorkbench'

const props = defineProps<{
  model: TopicSpecialHintModel
}>()

const accentClass = computed(() => {
  const v = props.model.visualVariant
  if (v === 'graph') return 'border border-slate-100'
  if (v === 'timeline') return 'border border-sky-100'
  if (v === 'container') return 'border border-violet-100'
  return 'border border-accent/20'
})
</script>
