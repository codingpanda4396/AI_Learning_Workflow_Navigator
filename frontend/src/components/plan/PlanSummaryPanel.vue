<script setup lang="ts">
import { INTENSITY_LABELS } from '@/constants/learningPlan';
import type { LearningPlanPreview } from '@/types/learningPlan';

const props = defineProps<{
  preview: LearningPlanPreview;
}>();

const metrics = [
  {
    label: '本轮起点',
    value: () => props.preview.summary.recommendedStart,
    hint: '从最该补齐的位置切入',
  },
  {
    label: '推荐推进方式',
    value: () => props.preview.summary.recommendedRhythmLabel || INTENSITY_LABELS[props.preview.summary.recommendedRhythm],
    hint: '按你当前节奏安排，不硬拉时长',
  },
  {
    label: '预计投入',
    value: () => `${props.preview.summary.estimatedMinutes} 分钟`,
    hint: '这一轮的建议专注时间',
  },
  {
    label: '涉及节点',
    value: () => `${props.preview.summary.estimatedKnowledgeCount} 个`,
    hint: '本轮会真正碰到的知识点',
  },
  {
    label: '前置补齐数',
    value: () => `${Math.max(0, props.preview.pathNodes.filter(node => node.isPrerequisite).length)} 个`,
    hint: '先稳住再推进主线',
  },
];
</script>

<template>
  <section class="relative overflow-hidden rounded-[2.7rem] bg-[radial-gradient(circle_at_top_right,rgba(110,231,255,0.16),transparent_26%),linear-gradient(135deg,#08111f_0%,#112136_48%,#143b52_100%)] px-6 py-8 text-white shadow-[0_34px_110px_rgba(8,17,31,0.34)] md:px-8 md:py-10">
    <div class="absolute inset-0 bg-[linear-gradient(180deg,rgba(255,255,255,0.04),transparent_26%,rgba(255,255,255,0.02))]" />
    <div class="absolute -right-10 -top-10 h-44 w-44 rounded-full bg-cyan-300/14 blur-3xl" />
    <div class="absolute bottom-0 left-0 h-36 w-36 rounded-full bg-amber-300/10 blur-3xl" />
    <div class="relative">
      <p class="text-xs font-semibold uppercase tracking-[0.32em] text-cyan-200/78">AI Learning Decision</p>
      <h1 class="mt-4 max-w-4xl text-3xl font-semibold tracking-tight md:text-5xl">
        {{ preview.summary.personalizedHeadline }}
      </h1>
      <p class="mt-4 max-w-3xl text-sm leading-7 text-slate-200/92 md:text-base">
        基于你的学习目标、诊断结果和当前薄弱点，这一轮先帮你把最影响推进效率的环节排清楚。
        {{ preview.summary.personalizedSummary }}
      </p>

      <div class="mt-8 grid gap-3 md:grid-cols-5">
        <div
          v-for="item in metrics"
          :key="item.label"
          class="rounded-[1.55rem] border border-white/12 bg-white/8 p-4 shadow-[inset_0_1px_0_rgba(255,255,255,0.08)] backdrop-blur-md"
        >
          <p class="text-[11px] font-semibold uppercase tracking-[0.18em] text-slate-300/92">{{ item.label }}</p>
          <p class="mt-3 text-sm font-semibold leading-6 text-white">{{ item.value() }}</p>
          <p class="mt-3 text-xs leading-5 text-slate-300/80">{{ item.hint }}</p>
        </div>
      </div>

      <div class="mt-6 flex flex-wrap items-center gap-2 text-[11px] text-slate-300/72">
        <span class="rounded-full border border-white/10 bg-white/6 px-3 py-1">规划依据</span>
        <span class="rounded-full border border-white/10 bg-white/6 px-3 py-1">{{ preview.goalText }}</span>
        <span class="rounded-full border border-white/10 bg-white/6 px-3 py-1">{{ preview.courseName }} / {{ preview.chapterName }}</span>
        <span class="rounded-full border border-white/10 bg-white/6 px-3 py-1">诊断薄弱点 + 学习偏好</span>
      </div>
    </div>
  </section>
</template>
