<script setup lang="ts">
import PageSection from '@/components/common/PageSection.vue';
import { STAGE_LABELS } from '@/constants/learningPlan';
import type { PlanTaskPreview } from '@/types/learningPlan';

const props = defineProps<{
  tasks: PlanTaskPreview[];
  nextStepNote: string;
  busy?: boolean;
}>();

defineEmits<{
  focusConfirm: [];
}>();

const stageMarkers: Record<string, string> = {
  STRUCTURE: '阶段 01',
  UNDERSTANDING: '阶段 02',
  TRAINING: '阶段 03',
  REFLECTION: '阶段 04',
};

const stageOutcomes: Record<string, string> = {
  STRUCTURE: '先把这一轮要走的知识骨架立住',
  UNDERSTANDING: '把关键原理真正讲透，不靠硬记',
  TRAINING: '把理解转成可用能力，暴露不稳点',
  REFLECTION: '收束结果，带着结论进入下一轮',
};
</script>

<template>
  <PageSection
    eyebrow="执行流程"
    title="确认后，这轮学习会这样开始"
    description="这不是静态计划页。确认之后，AI 会按下面这条四阶段流程接管推进，让你知道每一步要做什么、它会怎么帮、这一阶段要解决什么问题。"
  >
    <div class="rounded-[2rem] border border-slate-200 bg-[linear-gradient(180deg,#ffffff_0%,#f8fafc_100%)] p-4 md:p-5">
      <div class="grid gap-4 xl:grid-cols-4">
        <article
          v-for="task in props.tasks"
          :key="task.stage"
          class="relative rounded-[1.8rem] border border-slate-200 bg-white p-5 shadow-[0_18px_45px_rgba(15,23,42,0.05)]"
        >
          <div class="flex items-center justify-between gap-3">
            <div>
              <p class="text-xs font-semibold uppercase tracking-[0.22em] text-slate-400">{{ stageMarkers[task.stage] || task.stage }}</p>
              <h3 class="mt-2 text-lg font-semibold tracking-tight text-slate-950">{{ STAGE_LABELS[task.stage] }}</h3>
            </div>
            <span class="rounded-full bg-slate-950 px-3 py-1 text-xs font-semibold text-white">
              {{ task.estimatedMinutes }} 分钟
            </span>
          </div>
          <p class="mt-4 text-sm font-medium leading-6 text-slate-900">{{ task.stageGoal }}</p>
          <dl class="mt-4 space-y-3 text-sm leading-6 text-slate-600">
            <div>
              <dt class="font-semibold text-slate-900">你会做什么</dt>
              <dd class="mt-1">{{ task.learnerAction }}</dd>
            </div>
            <div>
              <dt class="font-semibold text-slate-900">AI 会怎么帮你</dt>
              <dd class="mt-1">{{ task.aiSupport }}</dd>
            </div>
            <div>
              <dt class="font-semibold text-slate-900">这一阶段解决什么</dt>
              <dd class="mt-1">{{ stageOutcomes[task.stage] }}</dd>
            </div>
          </dl>
        </article>
      </div>
    </div>

    <div class="mt-5 rounded-[2rem] bg-slate-950 px-5 py-5 text-white shadow-[0_26px_70px_rgba(15,23,42,0.22)] md:px-6">
      <p class="text-sm font-semibold text-white">看完这条流程后，你就可以确认这轮安排了。</p>
      <p class="mt-2 text-sm leading-7 text-slate-300">{{ props.nextStepNote }}</p>
      <div class="mt-5 flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
        <p class="text-xs leading-6 text-slate-400">确认后会创建 session，并从第一阶段直接进入学习执行链路。</p>
        <button
          type="button"
          class="rounded-2xl bg-white px-5 py-3 text-sm font-semibold text-slate-950 transition hover:bg-slate-100 disabled:cursor-not-allowed disabled:opacity-60"
          :disabled="props.busy"
          @click="$emit('focusConfirm')"
        >
          去确认这轮安排
        </button>
      </div>
    </div>
  </PageSection>
</template>
