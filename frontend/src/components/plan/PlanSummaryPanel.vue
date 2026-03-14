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
    label: '学习主题',
    value: props.preview.context.chapterName || '未提供章节信息',
    hint: props.preview.context.courseName || '本轮规划会围绕当前章节推进。',
  },
  {
    label: '当前起点',
    value:
      props.preview.summary.recommendedStartNode.displayName
      || props.preview.summary.recommendedStartNode.nodeName
      || '待系统确认',
    hint: '确认后会先进入这里，作为这轮学习的正式起点。',
  },
  {
    label: '预计时间',
    value: formatMinutes(props.preview.summary.estimatedTotalMinutes),
    hint: `推荐节奏：${props.preview.summary.recommendedRhythmLabel || INTENSITY_LABELS[props.preview.summary.recommendedRhythm]}`,
  },
  {
    label: '包含内容',
    value: `${props.preview.summary.estimatedKnowledgeCount || 0} 个知识点 / ${props.preview.summary.stageCount || 0} 个阶段`,
    hint: '会先覆盖本轮最值得优先学习的内容，再逐步推进到后续阶段。',
  },
]);

const statusTags = computed(() => {
  const tags = [props.preview.previewOnly ? '当前处于确认开始前的预览阶段' : '当前为正式学习规划'].filter(Boolean);

  if (props.preview.fallbackApplied) {
    tags.push('部分内容使用了兜底结果');
  }

  return tags;
});

const focusSummary = computed(() => {
  const items = props.preview.focuses?.filter(Boolean) ?? [];
  if (!items.length) {
    return ['起点确认', '阶段推进', 'AI 陪练支持'];
  }
  return items.slice(0, 4);
});
</script>

<template>
  <section class="relative overflow-hidden rounded-[2.7rem] bg-[radial-gradient(circle_at_top_right,rgba(110,231,255,0.16),transparent_26%),linear-gradient(135deg,#08111f_0%,#112136_48%,#143b52_100%)] px-6 py-8 text-white shadow-[0_34px_110px_rgba(8,17,31,0.34)] md:px-8 md:py-10">
    <div class="absolute inset-0 bg-[linear-gradient(180deg,rgba(255,255,255,0.04),transparent_26%,rgba(255,255,255,0.02))]" />
    <div class="absolute -right-10 -top-10 h-44 w-44 rounded-full bg-cyan-300/14 blur-3xl" />
    <div class="absolute bottom-0 left-0 h-36 w-36 rounded-full bg-amber-300/10 blur-3xl" />
    <div class="relative space-y-8">
      <div class="grid gap-6 xl:grid-cols-[minmax(0,1.3fr)_21rem] xl:items-start">
        <div>
          <p class="text-xs font-semibold uppercase tracking-[0.32em] text-cyan-200/78">学习确认页</p>
          <h1 class="mt-4 max-w-4xl text-3xl font-semibold tracking-tight md:text-5xl">{{ heroTitle }}</h1>
          <p class="mt-4 max-w-3xl text-sm leading-7 text-slate-200/92 md:text-base">{{ heroSubtitle }}</p>

          <div class="mt-5 rounded-[1.8rem] border border-white/10 bg-white/8 p-5 backdrop-blur-md">
            <p class="text-xs font-semibold uppercase tracking-[0.22em] text-cyan-100/80">本轮目标</p>
            <p class="mt-3 text-lg font-semibold text-white">{{ props.preview.context.goalText }}</p>
            <p class="mt-3 text-sm leading-7 text-slate-200/85">
              确认后会正式创建学习会话，并从推荐起点进入第一步学习任务。
            </p>
          </div>
        </div>

        <aside class="rounded-[2rem] border border-white/10 bg-white/8 p-5 shadow-[inset_0_1px_0_rgba(255,255,255,0.08)] backdrop-blur-md">
          <p class="text-xs font-semibold uppercase tracking-[0.22em] text-cyan-100/80">开始前总览</p>
          <div class="mt-4 space-y-4">
            <div>
              <p class="text-[11px] font-semibold uppercase tracking-[0.18em] text-slate-300/82">起点节点</p>
              <p class="mt-2 text-base font-semibold text-white">{{ props.preview.summary.recommendedStartNode.displayName || props.preview.summary.recommendedStartNode.nodeName }}</p>
            </div>
            <div>
              <p class="text-[11px] font-semibold uppercase tracking-[0.18em] text-slate-300/82">学习范围</p>
              <p class="mt-2 text-sm leading-6 text-slate-100/90">{{ props.preview.context.courseName }} / {{ props.preview.context.chapterName }}</p>
            </div>
            <div>
              <p class="text-[11px] font-semibold uppercase tracking-[0.18em] text-slate-300/82">这次会包含</p>
              <div class="mt-3 flex flex-wrap gap-2">
                <span v-for="focus in focusSummary" :key="focus" class="rounded-full border border-white/10 bg-white/8 px-3 py-1 text-xs text-slate-100/88">
                  {{ focus }}
                </span>
              </div>
            </div>
          </div>
        </aside>
      </div>

      <div class="grid gap-3 md:grid-cols-4">
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

      <div class="flex flex-wrap items-center gap-2 text-[11px] text-slate-300/72">
        <span v-for="tag in statusTags" :key="tag" class="rounded-full border border-white/10 bg-white/6 px-3 py-1">
          {{ tag }}
        </span>
      </div>
    </div>
  </section>
</template>
