<script setup lang="ts">
import { computed } from 'vue';
import FlowFieldCard from '@/components/common/FlowFieldCard.vue';
import FlowPageHero from '@/components/common/FlowPageHero.vue';
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

const contextItems = computed(() => [
  { label: '目标导向', value: props.preview.context.goalText || '按当前目标推进' },
  { label: '学习主题', value: props.preview.context.courseName || '通用课程' },
  { label: '当前章节', value: props.preview.context.chapterName || '当前章节' },
]);

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
    label: '预计学习时间',
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
  <section class="space-y-5">
    <FlowPageHero
      step-label="规划"
      :title="heroTitle"
      :description="heroSubtitle"
      meta-label="规划预览"
      :meta-value="preview.id"
      :context-items="contextItems"
    />

    <div class="grid gap-4 md:grid-cols-2 xl:grid-cols-4">
      <FlowFieldCard v-for="item in metrics" :key="item.label" :label="item.label" tone="soft" :hint="item.hint">
        <p class="text-sm font-semibold leading-6 text-slate-950">{{ item.value }}</p>
      </FlowFieldCard>
    </div>

    <div class="grid gap-4 xl:grid-cols-[minmax(0,1fr)_22rem]">
      <FlowFieldCard label="建议下一步" tone="strong" hint="确认后会正式创建学习会话，并从推荐起点进入第一步学习任务。">
        <p class="text-lg font-semibold leading-7 text-white">按这条路径开始学习</p>
      </FlowFieldCard>

      <FlowFieldCard label="学习阶段" tone="default">
        <div class="flex flex-wrap gap-2">
          <span v-for="focus in focusSummary" :key="focus" class="rounded-full border border-slate-200 bg-slate-50 px-3 py-1 text-xs font-medium text-slate-700">
            {{ focus }}
          </span>
        </div>
      </FlowFieldCard>
    </div>

    <div class="flex flex-wrap items-center gap-2 text-[11px] text-slate-500">
      <span v-for="tag in statusTags" :key="tag" class="rounded-full border border-slate-200 bg-slate-50 px-3 py-1">
        {{ tag }}
      </span>
    </div>
  </section>
</template>
