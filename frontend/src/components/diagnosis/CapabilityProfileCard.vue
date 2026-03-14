<script setup lang="ts">
import { computed } from 'vue';
import { resolveCapabilityProfileCopy } from '@/types/diagnosis';
import type { CapabilityProfile, DiagnosisInsights, DiagnosisMetadata, DiagnosisNextAction } from '@/types/diagnosis';

const props = defineProps<{
  profile: CapabilityProfile;
  insights?: DiagnosisInsights | null;
  nextAction?: DiagnosisNextAction | null;
  status?: string;
  fallbackText?: string;
  sourceText?: string;
  metadata?: DiagnosisMetadata | null;
}>();

const profileCopy = computed(() => resolveCapabilityProfileCopy(props.insights ?? undefined));
const strengths = computed(() => (props.profile.strengths.length ? props.profile.strengths : ['收集足够信号后将显示当前优势。']));
const weaknesses = computed(() => (props.profile.weaknesses.length ? props.profile.weaknesses : ['本次响应未返回明确的薄弱区域。']));
</script>

<template>
  <section class="rounded-[1.9rem] border border-slate-200 bg-white p-6 shadow-sm">
    <div class="flex flex-col gap-4 lg:flex-row lg:items-start lg:justify-between">
      <div>
        <p class="text-xs font-semibold uppercase tracking-[0.22em] text-emerald-600">画像</p>
        <h2 class="mt-2 text-2xl font-semibold tracking-tight text-slate-950">能力快照</h2>
        <p class="mt-3 max-w-3xl text-sm leading-6 text-slate-600">
          {{ profileCopy.summary }}
        </p>
      </div>
      <div class="grid gap-3 sm:grid-cols-2">
        <div class="rounded-2xl bg-slate-50 p-4 text-sm text-slate-700">
          <p class="text-xs font-semibold uppercase tracking-[0.18em] text-slate-400">状态</p>
          <p class="mt-2 font-medium text-slate-900">{{ status || '已提交' }}</p>
        </div>
        <div class="rounded-2xl bg-slate-50 p-4 text-sm text-slate-700">
          <p class="text-xs font-semibold uppercase tracking-[0.18em] text-slate-400">下一步动作</p>
          <p class="mt-2 font-medium text-slate-900">{{ nextAction?.label || '计划预览' }}</p>
        </div>
      </div>
    </div>

    <div class="mt-6 grid gap-4 md:grid-cols-2">
      <div class="rounded-2xl bg-slate-50 p-4">
        <p class="text-xs font-semibold uppercase tracking-[0.18em] text-slate-400">当前水平</p>
        <p class="mt-2 text-lg font-semibold text-slate-950">{{ profile.currentLevel.label }}</p>
      </div>
      <div class="rounded-2xl bg-slate-50 p-4">
        <p class="text-xs font-semibold uppercase tracking-[0.18em] text-slate-400">学习偏好</p>
        <p class="mt-2 text-sm leading-6 text-slate-800">
          {{ profile.learningPreference?.label || '系统将保持学习流程循序渐进、易读。' }}
        </p>
      </div>
      <div class="rounded-2xl bg-slate-50 p-4">
        <p class="text-xs font-semibold uppercase tracking-[0.18em] text-slate-400">时间预算</p>
        <p class="mt-2 text-sm leading-6 text-slate-800">
          {{ profile.timeBudget?.label || '建议保持稳定的周节奏。' }}
        </p>
      </div>
      <div class="rounded-2xl bg-slate-50 p-4">
        <p class="text-xs font-semibold uppercase tracking-[0.18em] text-slate-400">目标导向</p>
        <p class="mt-2 text-sm leading-6 text-slate-800">
          {{ profile.goalOrientation?.label || '下一计划将从最相关的前置知识开始。' }}
        </p>
      </div>
    </div>

    <div class="mt-6 grid gap-4 md:grid-cols-2">
      <div>
        <p class="text-sm font-semibold text-slate-900">优势</p>
        <div class="mt-3 flex flex-wrap gap-2">
          <span
            v-for="item in strengths"
            :key="item"
            class="rounded-full bg-emerald-50 px-3 py-1 text-xs font-medium text-emerald-700"
          >
            {{ item }}
          </span>
        </div>
      </div>
      <div>
        <p class="text-sm font-semibold text-slate-900">需关注</p>
        <div class="mt-3 flex flex-wrap gap-2">
          <span
            v-for="item in weaknesses"
            :key="item"
            class="rounded-full bg-amber-50 px-3 py-1 text-xs font-medium text-amber-700"
          >
            {{ item }}
          </span>
        </div>
      </div>
    </div>

    <div class="mt-6 grid gap-4 md:grid-cols-3">
      <div class="rounded-2xl border border-slate-200 bg-slate-50 p-4 text-sm leading-6 text-slate-700">
        <p class="text-xs font-semibold uppercase tracking-[0.18em] text-slate-400">来源</p>
        <p class="mt-2 font-medium text-slate-900">{{ sourceText || '合约响应' }}</p>
      </div>
      <div class="rounded-2xl border border-slate-200 bg-slate-50 p-4 text-sm leading-6 text-slate-700">
        <p class="text-xs font-semibold uppercase tracking-[0.18em] text-slate-400">兜底</p>
        <p class="mt-2 font-medium text-slate-900">{{ fallbackText || '未应用兜底' }}</p>
      </div>
      <div class="rounded-2xl border border-slate-200 bg-slate-50 p-4 text-sm leading-6 text-slate-700">
        <p class="text-xs font-semibold uppercase tracking-[0.18em] text-slate-400">元数据</p>
        <p class="mt-2 font-medium text-slate-900">
          {{ metadata?.questionCount || 0 }} 题 / {{ metadata?.answerCount || 0 }} 答 / v{{ metadata?.profileVersion || 1 }}
        </p>
      </div>
    </div>

    <div class="mt-6 rounded-2xl border border-slate-200 bg-slate-50 p-4 text-sm leading-7 text-slate-700">
      {{ profileCopy.planExplanation }}
    </div>
  </section>
</template>
