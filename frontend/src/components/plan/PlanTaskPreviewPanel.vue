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
  STRUCTURE: '在深入细节前先搭建章节框架。',
  UNDERSTANDING: '将关键概念转化为真实理解。',
  TRAINING: '将理解转化为可用的实战能力。',
  REFLECTION: '闭环复盘，确定下一轮迭代方向。',
};
</script>

<template>
  <PageSection
    eyebrow="执行"
    title="确认后预览将转化为"
    description="预览与已确认状态明确区分，便于判断当前是否仍为草稿。"
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
          <p class="mt-4 text-sm font-medium leading-6 text-slate-900">{{ task.title || task.learningGoal }}</p>
          <dl class="mt-4 space-y-3 text-sm leading-6 text-slate-600">
            <div>
              <dt class="font-semibold text-slate-900">学习者动作</dt>
              <dd class="mt-1">{{ task.learnerAction }}</dd>
            </div>
            <div>
              <dt class="font-semibold text-slate-900">AI 支持</dt>
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
      <p class="text-sm font-semibold text-white">预览已就绪，可确认</p>
      <p class="mt-2 text-sm leading-7 text-slate-300">{{ props.nextStepNote || '确认后将创建正式学习会话。' }}</p>
      <div class="mt-5 flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
        <p class="text-xs leading-6 text-slate-400">确认后我们将创建会话，并从后端返回的第一个阶段开始。</p>
        <button
          type="button"
          class="rounded-2xl bg-white px-5 py-3 text-sm font-semibold text-slate-950 transition hover:bg-slate-100 disabled:cursor-not-allowed disabled:opacity-60"
          :disabled="props.busy"
          @click="$emit('focusConfirm')"
        >
          查看确认区域
        </button>
      </div>
    </div>
  </PageSection>
</template>
