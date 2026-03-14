<script setup lang="ts">
import { INTENSITY_LABELS } from '@/constants/learningPlan';
import type { LearningPlanPreview } from '@/types/learningPlan';

const props = defineProps<{
  preview: LearningPlanPreview;
}>();

const metrics = [
  {
    label: '本轮起点',
    value: () => props.preview.summary.recommendedStartNode.nodeName,
    hint: '从当前最值得优先补齐的位置切入',
  },
  {
    label: '推荐节奏',
    value: () => props.preview.summary.recommendedRhythmLabel || INTENSITY_LABELS[props.preview.summary.recommendedRhythm],
    hint: '结合当前基础与任务密度给出的建议',
  },
  {
    label: '预估总耗时',
    value: () => `${props.preview.summary.estimatedTotalMinutes} 分钟`,
    hint: '口径为本次 pathPreview 的总投入时间',
  },
  {
    label: '涉及节点',
    value: () => `${props.preview.summary.estimatedKnowledgeCount} 个`,
    hint: '本轮真正会推进的知识节点数量',
  },
  {
    label: '前置补齐数',
    value: () => `${Math.max(0, props.preview.pathNodes.filter((node) => node.isPrerequisite).length)} 个`,
    hint: '优先稳住再进入主线推进',
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
        基于学习目标、诊断结果和近期薄弱点，这份预览先帮你确定最合理的切入点与推进顺序。
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
        <span class="rounded-full border border-white/10 bg-white/6 px-3 py-1">{{ preview.context.goalText }}</span>
        <span class="rounded-full border border-white/10 bg-white/6 px-3 py-1">{{ preview.context.courseName }} / {{ preview.context.chapterName }}</span>
        <span class="rounded-full border border-white/10 bg-white/6 px-3 py-1">{{ preview.status }}</span>
      </div>
    </div>
  </section>
</template>
