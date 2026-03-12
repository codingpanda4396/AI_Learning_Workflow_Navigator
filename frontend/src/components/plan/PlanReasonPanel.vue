<script setup lang="ts">
import PageSection from '@/components/common/PageSection.vue';
import type { PlanReason } from '@/types/learningPlan';

defineProps<{
  reasons: PlanReason[];
  diagnosisSummary: string;
}>();
</script>

<template>
  <PageSection
    eyebrow="个性化依据"
    title="为什么这一轮会这样安排"
    description="这里不是模板推荐，而是把诊断结果翻译成这轮学习动作。你会看到系统为什么这样判断，以及它在帮你规避什么风险。"
  >
    <div class="grid gap-4 xl:grid-cols-[1.15fr_1fr_1fr]">
      <div class="rounded-[1.7rem] bg-[linear-gradient(180deg,#f8fbff_0%,#eef6ff_100%)] p-5 ring-1 ring-sky-100">
        <p class="text-xs font-semibold uppercase tracking-[0.22em] text-sky-600">诊断转译</p>
        <p class="mt-4 text-base font-semibold tracking-tight text-slate-950">系统先读懂你当前的位置，再决定这轮从哪里起步。</p>
        <p class="mt-3 text-sm leading-7 text-slate-600">{{ diagnosisSummary }}</p>
      </div>

      <article
        v-for="reason in reasons"
        :key="reason.key"
        class="rounded-[1.7rem] border border-slate-200 bg-white p-5 shadow-[0_16px_45px_rgba(15,23,42,0.05)]"
      >
        <p class="text-xs font-semibold uppercase tracking-[0.2em] text-slate-400">{{ reason.label }}</p>
        <h3 class="mt-3 text-lg font-semibold tracking-tight text-slate-950">{{ reason.title }}</h3>
        <p class="mt-3 text-sm leading-7 text-slate-600">{{ reason.description }}</p>
      </article>
    </div>
  </PageSection>
</template>
