<template>
  <section
    class="relative overflow-hidden rounded-2xl border border-sky-100/90 bg-white px-5 py-5 shadow-[0_12px_40px_rgba(79,70,229,0.08)] md:rounded-[24px] md:px-7 md:py-6"
  >
    <div
      class="pointer-events-none absolute inset-0 bg-[radial-gradient(ellipse_120%_80%_at_0%_-20%,rgba(56,189,248,0.14),transparent_50%),radial-gradient(ellipse_90%_60%_at_100%_100%,rgba(129,140,248,0.12),transparent_45%),linear-gradient(180deg,rgba(255,255,255,0.98),rgba(240,249,255,0.35))]"
    />
    <div
      class="pointer-events-none absolute -right-8 -top-8 h-36 w-36 rounded-full bg-sky-200/30 blur-3xl"
    />
    <div
      class="pointer-events-none absolute -bottom-10 -left-6 h-28 w-28 rounded-full bg-indigo-200/25 blur-2xl"
    />

    <div class="relative">
      <p class="text-xs font-medium uppercase tracking-[0.2em] text-sky-700">
        {{ model.eyebrow }}
      </p>
      <h1 class="mt-3 max-w-3xl text-2xl font-semibold tracking-tight text-slate-900 md:text-4xl md:leading-[1.12]">
        {{ model.title }}
      </h1>
      <p class="mt-3 max-w-2xl text-sm leading-relaxed text-slate-600 md:text-base">
        {{ model.subtitle }}
      </p>

      <div class="mt-5 flex flex-wrap gap-2">
        <span
          v-for="(chip, i) in model.chips"
          :key="i"
          class="inline-flex rounded-full border border-sky-200/80 bg-sky-50 px-3 py-1 text-xs font-medium text-sky-900"
        >
          {{ chip }}
        </span>
      </div>

      <div class="mt-6 flex flex-col gap-3 sm:flex-row sm:flex-wrap sm:items-center">
        <PrimaryButton
          class="w-full justify-center sm:w-auto sm:min-w-[200px]"
          :loading="loading"
          :disabled="disabled"
          @click="$emit('start')"
        >
          {{ model.ctaLabel }}
        </PrimaryButton>
        <button
          type="button"
          class="w-full rounded-input border border-slate-200 bg-white px-5 py-2.5 text-sm font-medium text-slate-700 shadow-sm transition-colors hover:border-slate-300 hover:bg-slate-50 sm:w-auto"
          @click="$emit('toggleWhy')"
        >
          {{ model.secondaryCtaLabel }}
        </button>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import PrimaryButton from '@/components/ui/PrimaryButton.vue'
import type { LearningPlanDecisionViewModel } from '@/types/learningPlanDecision'

defineProps<{
  model: LearningPlanDecisionViewModel['hero']
  loading?: boolean
  disabled?: boolean
}>()

defineEmits<{
  start: []
  toggleWhy: []
}>()
</script>
