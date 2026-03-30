<template>
  <section class="relative overflow-hidden rounded-[28px] border border-slate-200/80 bg-slate-950 px-5 py-6 text-white shadow-[0_24px_80px_rgba(15,23,42,0.28)] md:px-8 md:py-8">
    <div class="absolute inset-0 bg-[radial-gradient(circle_at_top_left,rgba(45,212,191,0.22),transparent_36%),radial-gradient(circle_at_bottom_right,rgba(251,191,36,0.18),transparent_28%),linear-gradient(135deg,rgba(15,23,42,0.96),rgba(17,24,39,0.88))]" />
    <div class="absolute -right-16 top-10 h-40 w-40 rounded-full border border-white/10 bg-white/5 blur-2xl" />

    <div class="relative">
      <p class="text-xs font-medium uppercase tracking-[0.24em] text-cyan-200/90">
        {{ model.eyebrow }}
      </p>
      <h1 class="mt-4 max-w-3xl text-3xl font-semibold tracking-tight text-white md:text-5xl md:leading-[1.1]">
        {{ model.title }}
      </h1>
      <p class="mt-4 max-w-2xl text-base leading-7 text-slate-200 md:text-lg">
        {{ model.decisionText }}
      </p>

      <div class="mt-6 grid gap-3 md:grid-cols-2">
        <div class="rounded-2xl border border-white/12 bg-white/8 px-4 py-4 backdrop-blur">
          <p class="text-xs font-medium uppercase tracking-[0.18em] text-slate-300">
            为什么先做
          </p>
          <p class="mt-2 text-sm leading-6 text-white/90 md:text-[15px]">
            {{ model.reasonText }}
          </p>
        </div>
        <div class="rounded-2xl border border-cyan-300/20 bg-cyan-300/10 px-4 py-4 backdrop-blur">
          <p class="text-xs font-medium uppercase tracking-[0.18em] text-cyan-100/90">
            做完先得到
          </p>
          <p class="mt-2 text-sm leading-6 text-white/90 md:text-[15px]">
            {{ model.outcomeText }}
          </p>
        </div>
      </div>

      <div class="mt-8 flex flex-col gap-3">
        <p class="text-sm font-medium text-slate-100">
          现在先把这一小步走稳。
        </p>
        <div class="flex flex-col gap-3 sm:flex-row sm:items-center">
          <PrimaryButton
            class="w-full justify-center bg-white text-slate-950 hover:bg-slate-100 sm:w-auto sm:min-w-[220px]"
            :loading="loading"
            :disabled="disabled"
            @click="$emit('start')"
          >
            {{ model.ctaLabel }}
          </PrimaryButton>
          <p
            v-if="model.ctaSubtext"
            class="text-sm text-slate-300"
          >
            {{ model.ctaSubtext }}
          </p>
        </div>
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
}>()
</script>
