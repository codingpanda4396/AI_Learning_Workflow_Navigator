<script setup lang="ts">
import PageSection from '@/components/common/PageSection.vue';
import type { PlanReason } from '@/types/learningPlan';

const props = defineProps<{
  reasons: PlanReason[];
  diagnosisSummary: string;
}>();

const defaultTitles = [
  '优先处理什么',
  '为何以此为最佳起点',
  '此顺序避免了什么风险',
  '本轮重点优化什么',
];
</script>

<template>
  <PageSection
    eyebrow="推理"
    title="预览为何如此编排"
    description="本节保留后端推理的可读性，无需在路由中硬编码说明文案。"
  >
    <div class="grid gap-4 xl:grid-cols-[1.1fr_repeat(2,minmax(0,1fr))]">
      <div class="rounded-[1.9rem] bg-[linear-gradient(180deg,#f7fbff_0%,#edf6ff_100%)] p-6 shadow-[0_20px_50px_rgba(56,189,248,0.08)] ring-1 ring-sky-100">
        <p class="text-xs font-semibold uppercase tracking-[0.24em] text-sky-600">诊断摘要</p>
        <h3 class="mt-4 text-2xl font-semibold tracking-tight text-slate-950">计划从诊断结果出发，而非前端硬编码规则。</h3>
        <p class="mt-4 text-sm leading-7 text-slate-600">{{ diagnosisSummary || '后端未返回本预览的诊断摘要。' }}</p>
      </div>

      <article
        v-for="(reason, index) in props.reasons"
        :key="reason.key"
        class="rounded-[1.85rem] border border-slate-200/90 bg-white p-6 shadow-[0_18px_50px_rgba(15,23,42,0.06)] transition hover:-translate-y-0.5 hover:shadow-[0_22px_65px_rgba(15,23,42,0.08)]"
      >
        <p class="text-xs font-semibold uppercase tracking-[0.2em] text-slate-400">{{ reason.label || `原因 ${index + 1}` }}</p>
        <h3 class="mt-3 text-xl font-semibold tracking-tight text-slate-950">{{ reason.title || defaultTitles[index] || `原因 ${index + 1}` }}</h3>
        <p class="mt-3 text-sm leading-7 text-slate-600">{{ reason.description }}</p>
      </article>
    </div>
  </PageSection>
</template>
