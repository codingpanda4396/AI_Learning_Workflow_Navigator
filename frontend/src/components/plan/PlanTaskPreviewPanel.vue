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
  STRUCTURE: '先搭起知识骨架，明确当前轮次要解决的问题',
  UNDERSTANDING: '把关键原理真正讲透，而不是只记结论',
  TRAINING: '通过训练暴露不稳点，把理解转成可用能力',
  REFLECTION: '收束结果，确认下一轮继续推进还是回补',
};
</script>

<template>
  <PageSection
    eyebrow="执行流程"
    title="确认后，这轮学习会这样开始"
    description="这里展示的是确认后的执行模板，让你提前知道每个阶段要做什么、AI 会怎么支持、每段时间大致怎么分配。"
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
              {{ task.estimatedTaskMinutes }} 分钟
            </span>
          </div>
          <p class="mt-4 text-sm font-medium leading-6 text-slate-900">{{ task.learningGoal }}</p>
          <dl class="mt-4 space-y-3 text-sm leading-6 text-slate-600">
            <div>
              <dt class="font-semibold text-slate-900">你会做什么</dt>
              <dd class="mt-1">{{ task.learnerAction }}</dd>
            </div>
            <div>
              <dt class="font-semibold text-slate-900">AI 会如何支持</dt>
              <dd class="mt-1">{{ task.aiSupport }}</dd>
            </div>
            <div>
              <dt class="font-semibold text-slate-900">阶段目标</dt>
              <dd class="mt-1">{{ stageOutcomes[task.stage] }}</dd>
            </div>
          </dl>
        </article>
      </div>
    </div>

    <div class="mt-5 rounded-[2rem] bg-slate-950 px-5 py-5 text-white shadow-[0_26px_70px_rgba(15,23,42,0.22)] md:px-6">
      <p class="text-sm font-semibold text-white">看完流程后，就可以确认这份预览了。</p>
      <p class="mt-2 text-sm leading-7 text-slate-300">{{ props.nextStepNote }}</p>
      <div class="mt-5 flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
        <p class="text-xs leading-6 text-slate-400">确认后会创建正式学习计划与 session，并从第一个阶段直接进入执行链路。</p>
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
