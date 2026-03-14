<script setup lang="ts">
import PageSection from '@/components/common/PageSection.vue';
import type { PlanReason } from '@/types/learningPlan';

const props = defineProps<{
  reasons: PlanReason[];
  diagnosisSummary: string;
}>();

const defaultTitles = [
  '根据你的诊断结果，当前最需要先解决什么',
  '为什么当前更适合从这里开始',
  '这样安排能先解决哪些学习问题',
  '为什么不建议一开始直接跳到更难部分',
];
</script>

<template>
  <PageSection
    eyebrow="补充说明"
    title="为什么这样安排"
    description="这里是对路径和阶段的解释说明，帮助你理解这套安排为什么适合现在开始。"
  >
    <div class="grid gap-4 xl:grid-cols-[1.1fr_repeat(2,minmax(0,1fr))]">
      <div class="rounded-[1.9rem] border border-sky-100 bg-[linear-gradient(180deg,#f8fbff_0%,#f2f7ff_100%)] p-6 shadow-[0_14px_40px_rgba(56,189,248,0.06)]">
        <p class="text-xs font-semibold uppercase tracking-[0.24em] text-sky-600">诊断结果</p>
        <h3 class="mt-4 text-2xl font-semibold tracking-tight text-slate-950">这条路径是从你当前状态出发，而不是固定模板。</h3>
        <p class="mt-4 text-sm leading-7 text-slate-600">{{ diagnosisSummary || '暂未返回更详细的诊断摘要，系统会先从更稳妥的起点为你安排。' }}</p>
      </div>

      <article
        v-for="(reason, index) in props.reasons"
        :key="reason.key"
        class="rounded-[1.85rem] border border-slate-200/90 bg-white p-6 shadow-[0_14px_40px_rgba(15,23,42,0.05)]"
      >
        <p class="text-xs font-semibold uppercase tracking-[0.2em] text-slate-400">{{ reason.label || `依据 ${index + 1}` }}</p>
        <h3 class="mt-3 text-xl font-semibold tracking-tight text-slate-950">{{ reason.title || defaultTitles[index] || `依据 ${index + 1}` }}</h3>
        <p class="mt-3 text-sm leading-7 text-slate-600">{{ reason.description || '系统会优先按你的诊断结果安排更合适的起点和顺序。' }}</p>
      </article>
    </div>
  </PageSection>
</template>
