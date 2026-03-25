<template>
  <section
    class="overflow-hidden rounded-[32px] border border-sky-100/90 bg-[linear-gradient(140deg,#f3f8ff_0%,#ffffff_46%,#f8fafc_100%)] p-6 shadow-[0_26px_70px_rgba(15,23,42,0.08)] md:p-8"
  >
    <div class="grid gap-5 lg:grid-cols-[1.2fr_0.8fr] lg:items-start">
      <div class="space-y-5">
        <div class="space-y-3">
          <p class="text-[11px] font-semibold uppercase tracking-[0.28em] text-sky-700/80">
            Plan
          </p>
          <div>
            <p class="text-sm font-medium text-sky-700">
              {{ topic }}
            </p>
            <h1 class="mt-2 text-3xl font-semibold tracking-tight text-slate-950 md:text-[2.35rem]">
              {{ headline }}
            </h1>
          </div>
          <p class="max-w-2xl text-sm leading-7 text-slate-600 md:text-[15px]">
            {{ subline }}
          </p>
        </div>

        <div class="flex flex-wrap items-center gap-3 text-sm">
          <span class="rounded-full border border-sky-200 bg-white px-3 py-1.5 font-medium text-slate-700">
            {{ totalEstimatedLabel }}
          </span>
          <span class="rounded-full border border-slate-200 bg-slate-50 px-3 py-1.5 font-medium text-slate-700">
            共 {{ totalSteps }} 步
          </span>
          <span class="rounded-full border border-slate-200 bg-white px-3 py-1.5 text-slate-500">
            {{ recommendedStageLabelZh }}
            <span class="ml-2 text-[11px] font-semibold uppercase tracking-[0.18em] text-slate-400">
              {{ recommendedStageLabelEn }}
            </span>
          </span>
        </div>

        <PrimaryButton
          class="min-w-[220px] justify-center px-7 py-3.5 text-base font-semibold shadow-[0_16px_36px_rgba(79,70,229,0.26)]"
          :loading="loading"
          @click="$emit('start')"
        >
          {{ startButtonLabel }}
        </PrimaryButton>
      </div>

      <aside class="rounded-[28px] border border-slate-200/80 bg-white/88 p-5 shadow-[0_18px_40px_rgba(15,23,42,0.05)]">
        <div class="flex items-center justify-between gap-3">
          <div>
            <p class="text-base font-semibold text-slate-900">{{ PLAN_COPY.whyFirst }}</p>
            <p class="mt-1 text-xs uppercase tracking-[0.22em] text-slate-400">
              {{ PLAN_COPY.currentAdvice }}
            </p>
          </div>
          <span class="rounded-full bg-sky-50 px-3 py-1 text-xs font-semibold text-sky-700">
            {{ currentProblemLabel }}
          </span>
        </div>

        <div class="mt-4 space-y-3">
          <div
            v-for="line in whyThisFirst"
            :key="line"
            class="rounded-2xl border border-slate-100 bg-slate-50/80 px-4 py-3"
          >
            <p class="text-sm leading-6 text-slate-700">
              {{ line }}
            </p>
          </div>
        </div>
      </aside>
    </div>
  </section>
</template>

<script setup lang="ts">
import PrimaryButton from '@/components/ui/PrimaryButton.vue'
import { PLAN_COPY } from '@/constants/uiCopy'

defineProps<{
  topic: string
  headline: string
  subline: string
  currentProblemLabel: string
  recommendedStageLabelZh: string
  recommendedStageLabelEn: string
  totalEstimatedLabel: string
  totalSteps: number
  whyThisFirst: string[]
  startButtonLabel: string
  loading?: boolean
}>()

defineEmits<{
  start: []
}>()
</script>
