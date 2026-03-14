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
  STRUCTURE: '先建立章节地图，知道这轮学习在学什么、先后顺序是什么。',
  UNDERSTANDING: '把关键概念真正讲明白，避免只记结论却不会迁移。',
  TRAINING: '把理解变成可操作的能力，开始能独立完成对应练习。',
  REFLECTION: '完成闭环复盘，明确已经掌握什么以及下一步怎么继续。',
};
</script>

<template>
  <PageSection
    eyebrow="学习阶段"
    title="这轮学习会经历哪些阶段"
    description="每个阶段都说明你要达成什么、AI 会怎么辅助，以及完成后你会获得什么。"
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
          <p class="mt-4 text-sm font-medium leading-6 text-slate-900">{{ task.title || STAGE_LABELS[task.stage] }}</p>
          <dl class="mt-4 space-y-3 text-sm leading-6 text-slate-600">
            <div>
              <dt class="font-semibold text-slate-900">学习目标</dt>
              <dd class="mt-1">{{ task.learningGoal || '围绕当前阶段完成对应学习目标。' }}</dd>
            </div>
            <div>
              <dt class="font-semibold text-slate-900">你会怎么学</dt>
              <dd class="mt-1">{{ task.learnerAction || '按照系统给出的任务提示逐步推进。' }}</dd>
            </div>
            <div>
              <dt class="font-semibold text-slate-900">AI 会怎么辅助</dt>
              <dd class="mt-1">{{ task.aiSupport || 'AI 会根据当前阶段提供解释、引导和反馈。' }}</dd>
            </div>
            <div>
              <dt class="font-semibold text-slate-900">完成后你会获得什么</dt>
              <dd class="mt-1">{{ stageOutcomes[task.stage] }}</dd>
            </div>
          </dl>
        </article>
      </div>
    </div>

    <div class="mt-5 rounded-[2rem] bg-slate-950 px-5 py-5 text-white shadow-[0_26px_70px_rgba(15,23,42,0.22)] md:px-6">
      <p class="text-sm font-semibold text-white">确认后就会按这个节奏开始</p>
      <p class="mt-2 text-sm leading-7 text-slate-300">{{ props.nextStepNote || '确认后会创建正式学习会话，并从第一步学习任务开始。' }}</p>
      <div class="mt-5 flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
        <p class="text-xs leading-6 text-slate-400">你不需要重新选择步骤，系统会按规划直接进入第一阶段任务。</p>
        <button
          type="button"
          class="rounded-2xl bg-white px-5 py-3 text-sm font-semibold text-slate-950 transition hover:bg-slate-100 disabled:cursor-not-allowed disabled:opacity-60"
          :disabled="props.busy"
          @click="$emit('focusConfirm')"
        >
          查看开始入口
        </button>
      </div>
    </div>
  </PageSection>
</template>
