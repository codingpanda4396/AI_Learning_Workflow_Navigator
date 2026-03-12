<script setup lang="ts">
import PageSection from '@/components/common/PageSection.vue';
import { STAGE_LABELS } from '@/constants/learningPlan';
import type { PlanTaskPreview } from '@/types/learningPlan';

defineProps<{
  tasks: PlanTaskPreview[];
  nextStepNote: string;
}>();
</script>

<template>
  <PageSection
    eyebrow="本轮任务方案"
    title="确认后，你会进入什么学习流程"
    description="这不是生成一张静态计划就结束。确认后，系统会按照四阶段学习链路继续推进，并在每个阶段给你不同方式的辅助。"
  >
    <div class="grid gap-4 xl:grid-cols-4">
      <article
        v-for="task in tasks"
        :key="task.stage"
        class="rounded-[1.7rem] border border-slate-200 bg-white p-5 shadow-[0_18px_50px_rgba(15,23,42,0.05)]"
      >
        <p class="text-xs font-semibold uppercase tracking-[0.22em] text-slate-400">{{ task.stage }}</p>
        <h3 class="mt-3 text-lg font-semibold tracking-tight text-slate-950">{{ STAGE_LABELS[task.stage] }}</h3>
        <p class="mt-4 text-sm font-medium text-slate-900">{{ task.stageGoal }}</p>
        <dl class="mt-4 space-y-3 text-sm leading-6 text-slate-600">
          <div>
            <dt class="font-semibold text-slate-900">你会做什么</dt>
            <dd class="mt-1">{{ task.learnerAction }}</dd>
          </div>
          <div>
            <dt class="font-semibold text-slate-900">AI 会如何辅助</dt>
            <dd class="mt-1">{{ task.aiSupport }}</dd>
          </div>
          <div>
            <dt class="font-semibold text-slate-900">预计时长</dt>
            <dd class="mt-1">{{ task.estimatedMinutes }} 分钟</dd>
          </div>
        </dl>
      </article>
    </div>

    <div class="mt-5 rounded-[1.5rem] bg-slate-950 px-5 py-4 text-sm leading-7 text-slate-200">
      {{ nextStepNote }}
    </div>
  </PageSection>
</template>
