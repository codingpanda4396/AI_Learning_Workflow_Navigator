<script setup lang="ts">
import { INTENSITY_LABELS } from '@/constants/learningPlan';
import type { LearningPlanPreview } from '@/types/learningPlan';

defineProps<{
  preview: LearningPlanPreview;
}>();
</script>

<template>
  <section class="relative overflow-hidden rounded-[2.4rem] bg-[linear-gradient(135deg,#0f172a_0%,#13263f_48%,#1d4f6f_100%)] px-6 py-7 text-white shadow-[0_28px_90px_rgba(15,23,42,0.22)] md:px-8 md:py-9">
    <div class="absolute -right-10 -top-12 h-44 w-44 rounded-full bg-cyan-300/16 blur-3xl" />
    <div class="absolute bottom-0 left-0 h-32 w-32 rounded-full bg-amber-300/12 blur-3xl" />
    <div class="relative">
      <p class="text-xs font-semibold uppercase tracking-[0.28em] text-cyan-200/80">Learning Plan</p>
      <h1 class="mt-3 max-w-4xl text-3xl font-semibold tracking-tight md:text-4xl">
        {{ preview.summary.personalizedHeadline }}
      </h1>
      <p class="mt-4 max-w-3xl text-sm leading-7 text-slate-200 md:text-base">
        {{ preview.summary.personalizedSummary }}
      </p>

      <div class="mt-7 grid gap-3 md:grid-cols-5">
        <div class="rounded-[1.5rem] bg-white/10 p-4 ring-1 ring-white/12 backdrop-blur">
          <p class="text-xs uppercase tracking-[0.18em] text-slate-300">本轮建议起点</p>
          <p class="mt-2 text-sm font-semibold text-white">{{ preview.summary.recommendedStart }}</p>
        </div>
        <div class="rounded-[1.5rem] bg-white/10 p-4 ring-1 ring-white/12 backdrop-blur">
          <p class="text-xs uppercase tracking-[0.18em] text-slate-300">推荐节奏</p>
          <p class="mt-2 text-sm font-semibold text-white">{{ INTENSITY_LABELS[preview.summary.recommendedRhythm] }}</p>
        </div>
        <div class="rounded-[1.5rem] bg-white/10 p-4 ring-1 ring-white/12 backdrop-blur">
          <p class="text-xs uppercase tracking-[0.18em] text-slate-300">预计学习时长</p>
          <p class="mt-2 text-sm font-semibold text-white">{{ preview.summary.estimatedMinutes }} 分钟</p>
        </div>
        <div class="rounded-[1.5rem] bg-white/10 p-4 ring-1 ring-white/12 backdrop-blur">
          <p class="text-xs uppercase tracking-[0.18em] text-slate-300">预计知识点</p>
          <p class="mt-2 text-sm font-semibold text-white">{{ preview.summary.estimatedKnowledgeCount }} 个</p>
        </div>
        <div class="rounded-[1.5rem] bg-white/10 p-4 ring-1 ring-white/12 backdrop-blur">
          <p class="text-xs uppercase tracking-[0.18em] text-slate-300">阶段数</p>
          <p class="mt-2 text-sm font-semibold text-white">{{ preview.summary.stageCount }} 个阶段</p>
        </div>
      </div>
    </div>
  </section>
</template>
