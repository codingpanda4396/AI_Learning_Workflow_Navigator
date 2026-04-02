<template>
  <section
    v-if="feedback"
    class="rounded-2xl border border-accent/25 bg-accent-muted/65 p-4 shadow-sm"
  >
    <p class="text-xs font-semibold uppercase tracking-wide text-text-primary">{{ feedback.title }}</p>
    <p v-if="feedback.tag" class="mt-1 text-xs font-medium text-accent-hover">{{ feedback.tag }}</p>
    <p class="mt-1 text-sm leading-relaxed text-slate-900">{{ feedback.body }}</p>
    <p v-if="feedback.nextAction" class="mt-2 text-xs text-slate-700">下一步：{{ feedback.nextAction }}</p>
  </section>
</template>

<script setup lang="ts">
import type { PhaseFeedbackPayload } from '@/types/phaseWorkbench'

defineProps<{
  feedback: PhaseFeedbackPayload | null
}>()
</script>
