<script setup lang="ts">
import PageSection from '@/components/common/PageSection.vue';
import type { PlanReason } from '@/types/learningPlan';

const props = defineProps<{
  reasons: PlanReason[];
  diagnosisSummary: string;
}>();

const defaultTitles = [
  '这轮先补哪里',
  '为什么从这里开始',
  '现在直接推进的风险',
  '这轮最值得优先解决的问题',
];
</script>

<template>
  <PageSection
    eyebrow="个性化解释"
    title="为什么这轮要这样学"
    description="这里不是在介绍系统能力，而是在把这轮安排背后的判断摊开给你看：先处理什么，为什么这样排，以及如果跳过这一步会卡在哪里。"
  >
    <div class="grid gap-4 xl:grid-cols-[1.1fr_repeat(2,minmax(0,1fr))]">
      <div class="rounded-[1.9rem] bg-[linear-gradient(180deg,#f7fbff_0%,#edf6ff_100%)] p-6 shadow-[0_20px_50px_rgba(56,189,248,0.08)] ring-1 ring-sky-100">
        <p class="text-xs font-semibold uppercase tracking-[0.24em] text-sky-600">你的当前状态</p>
        <h3 class="mt-4 text-2xl font-semibold tracking-tight text-slate-950">这轮先不追求铺开，而是先把最影响推进的断点补稳。</h3>
        <p class="mt-4 text-sm leading-7 text-slate-600">{{ diagnosisSummary }}</p>
        <div class="mt-5 rounded-[1.4rem] border border-sky-100/90 bg-white/72 px-4 py-4 text-sm leading-7 text-slate-700">
          这样安排的目的，是让你确认“现在该先学什么”之后，后面的理解、训练和复盘都能顺着同一条主线走。
        </div>
      </div>

      <article
        v-for="(reason, index) in props.reasons"
        :key="reason.key"
        class="rounded-[1.85rem] border border-slate-200/90 bg-white p-6 shadow-[0_18px_50px_rgba(15,23,42,0.06)] transition hover:-translate-y-0.5 hover:shadow-[0_22px_65px_rgba(15,23,42,0.08)]"
      >
        <p class="text-xs font-semibold uppercase tracking-[0.2em] text-slate-400">{{ reason.label || `判断 ${index + 1}` }}</p>
        <h3 class="mt-3 text-xl font-semibold tracking-tight text-slate-950">{{ defaultTitles[index] || reason.title }}</h3>
        <p class="mt-3 text-sm leading-7 text-slate-600">{{ reason.description }}</p>
      </article>
    </div>
  </PageSection>
</template>
