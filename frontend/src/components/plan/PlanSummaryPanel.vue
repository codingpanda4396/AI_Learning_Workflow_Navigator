<script setup lang="ts">
import { computed } from 'vue';
import { INTENSITY_LABELS } from '@/constants/learningPlan';
import type { LearningPlanPreview } from '@/types/learningPlan';

const props = defineProps<{
  preview: LearningPlanPreview;
}>();

function formatMinutes(minutes?: number) {
  if (!minutes || minutes <= 0) return '时间待确认';
  if (minutes >= 60) {
    const hours = Math.floor(minutes / 60);
    const rest = minutes % 60;
    return rest ? `${hours} 小时 ${rest} 分钟` : `${hours} 小时`;
  }
  return `${minutes} 分钟`;
}

const heroTitle = computed(() => {
  const headline = props.preview.summary.personalizedHeadline?.trim();
  if (headline) return headline;
  return `${props.preview.context.chapterName || '当前章节'}学习规划`;
});

const heroSubtitle = computed(() => {
  const summary = props.preview.summary.personalizedSummary?.trim();
  if (summary) return summary;
  return '这条学习路径基于你的诊断结果生成，帮助你从更合适的起点开始。';
});

const metrics = computed(() => [
  {
    label: '当前起点',
    value: props.preview.summary.recommendedStartNode.displayName || props.preview.summary.recommendedStartNode.nodeName || '待系统确定',
    hint: '这会是你确认后首先进入的学习内容。',
  },
  {
    label: '当前章节',
    value: props.preview.context.chapterName || '未提供章节信息',
    hint: props.preview.context.courseName || '本轮规划会围绕当前章节推进。',
  },
  {
    label: '预计学习时间',
    value: formatMinutes(props.preview.summary.estimatedTotalMinutes),
    hint: `建议节奏：${props.preview.summary.recommendedRhythmLabel || INTENSITY_LABELS[props.preview.summary.recommendedRhythm]}`,
  },
  {
    label: '知识点数',
    value: `${props.preview.summary.estimatedKnowledgeCount || 0} 个`,
    hint: '本轮会优先覆盖当前最值得先学的知识点。',
  },
  {
    label: '学习阶段',
    value: `${props.preview.summary.stageCount || 0} 个阶段`,
    hint: '你会按阶段逐步推进，而不是一次性接收全部内容。',
  },
]);

const secondaryTags = computed(() => {
  const tags = [
    props.preview.context.goalText,
    props.preview.context.courseName ? `${props.preview.context.courseName} / ${props.preview.context.chapterName}` : '',
    props.preview.previewOnly ? '当前为规划确认阶段' : '已生成正式学习规划',
  ].filter(Boolean);

  if (props.preview.fallbackApplied) {
    tags.push('部分内容使用兜底结果');
  }

  return tags;
});
</script>

<template>
  <section class="relative overflow-hidden rounded-[2.7rem] bg-[radial-gradient(circle_at_top_right,rgba(110,231,255,0.16),transparent_26%),linear-gradient(135deg,#08111f_0%,#112136_48%,#143b52_100%)] px-6 py-8 text-white shadow-[0_34px_110px_rgba(8,17,31,0.34)] md:px-8 md:py-10">
    <div class="absolute inset-0 bg-[linear-gradient(180deg,rgba(255,255,255,0.04),transparent_26%,rgba(255,255,255,0.02))]" />
    <div class="absolute -right-10 -top-10 h-44 w-44 rounded-full bg-cyan-300/14 blur-3xl" />
    <div class="absolute bottom-0 left-0 h-36 w-36 rounded-full bg-amber-300/10 blur-3xl" />
    <div class="relative">
      <p class="text-xs font-semibold uppercase tracking-[0.32em] text-cyan-200/78">学习规划</p>
      <h1 class="mt-4 max-w-4xl text-3xl font-semibold tracking-tight md:text-5xl">{{ heroTitle }}</h1>
      <p class="mt-4 max-w-3xl text-sm leading-7 text-slate-200/92 md:text-base">{{ heroSubtitle }}</p>
      <p class="mt-3 max-w-3xl text-sm leading-7 text-cyan-100/86">
        基于你的诊断结果生成，确认后会按这条路径从第一步学习任务开始。
      </p>

      <div class="mt-8 grid gap-3 md:grid-cols-5">
        <div
          v-for="item in metrics"
          :key="item.label"
          class="rounded-[1.55rem] border border-white/12 bg-white/8 p-4 shadow-[inset_0_1px_0_rgba(255,255,255,0.08)] backdrop-blur-md"
        >
          <p class="text-[11px] font-semibold uppercase tracking-[0.18em] text-slate-300/92">{{ item.label }}</p>
          <p class="mt-3 text-sm font-semibold leading-6 text-white">{{ item.value }}</p>
          <p class="mt-3 text-xs leading-5 text-slate-300/80">{{ item.hint }}</p>
        </div>
      </div>

      <div class="mt-6 flex flex-wrap items-center gap-2 text-[11px] text-slate-300/72">
        <span v-for="tag in secondaryTags" :key="tag" class="rounded-full border border-white/10 bg-white/6 px-3 py-1">
          {{ tag }}
        </span>
      </div>
    </div>
  </section>
</template>
