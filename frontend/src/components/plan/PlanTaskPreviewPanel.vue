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
    title="你会经历这四个学习阶段"
    description="每张卡片都对应一个阶段，先看清这一轮会怎么学，再决定开始。"
  >
    <div class="rounded-[2rem] border border-slate-200 bg-[linear-gradient(180deg,#ffffff_0%,#f8fafc_100%)] p-4 md:p-5">
      <div class="grid gap-4 xl:grid-cols-4">
        <article
          v-for="task in props.tasks"
          :key="task.stage"
          class="relative flex h-full flex-col rounded-[1.8rem] border border-slate-200 bg-white p-5 shadow-[0_18px_45px_rgba(15,23,42,0.05)]"
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
          <p class="mt-4 min-h-[3rem] text-sm font-medium leading-6 text-slate-900">{{ task.title || STAGE_LABELS[task.stage] }}</p>
          <dl class="mt-4 flex-1 space-y-3 text-sm leading-6 text-slate-600">
            <div class="rounded-[1.2rem] bg-slate-50 p-3">
              <dt class="font-semibold text-slate-900">学习动作</dt>
              <dd class="mt-1">{{ task.learnerAction || '按系统给出的任务提示逐步推进。' }}</dd>
            </div>
            <div class="rounded-[1.2rem] bg-slate-50 p-3">
              <dt class="font-semibold text-slate-900">AI 支持</dt>
              <dd class="mt-1">{{ task.aiSupport || 'AI 会根据当前阶段提供讲解、引导和反馈。' }}</dd>
            </div>
            <div class="rounded-[1.2rem] bg-slate-50 p-3">
              <dt class="font-semibold text-slate-900">阶段目标</dt>
              <dd class="mt-1">{{ task.learningGoal || '围绕当前阶段完成对应学习目标。' }}</dd>
            </div>
            <div class="rounded-[1.2rem] border border-dashed border-slate-200 p-3">
              <dt class="font-semibold text-slate-900">完成后你会获得什么</dt>
              <dd class="mt-1">{{ stageOutcomes[task.stage] }}</dd>
            </div>
          </dl>
        </article>
      </div>
    </div>

    <div class="mt-5 rounded-[1.8rem] border border-slate-200 bg-slate-50 px-5 py-5 md:px-6">
      <p class="text-sm font-semibold text-slate-950">确认后会从第一阶段正式开始</p>
      <p class="mt-2 text-sm leading-7 text-slate-600">{{ props.nextStepNote || '确认后会创建正式学习会话，并从第一步学习任务开始。' }}</p>
      <div class="mt-5 flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
        <p class="text-xs leading-6 text-slate-500">不需要再手动选择步骤，系统会按这套阶段顺序推进。</p>
        <button
          type="button"
          class="rounded-2xl bg-slate-950 px-5 py-3 text-sm font-semibold text-white transition hover:bg-slate-800 disabled:cursor-not-allowed disabled:opacity-60"
          :disabled="props.busy"
          @click="$emit('focusConfirm')"
        >
          去确认按路径开始学习
        </button>
      </div>
    </div>
  </PageSection>
</template>
