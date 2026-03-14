<script setup lang="ts">
import { computed } from 'vue';
import { resolveCapabilityProfileCopy, resolveDiagnosisMetaSummary } from '@/types/diagnosis';
import type { CapabilityProfile, DiagnosisInsights, DiagnosisMetadata } from '@/types/diagnosis';

const props = defineProps<{
  profile: CapabilityProfile;
  insights?: DiagnosisInsights | null;
  status?: string;
  sourceText?: string;
  fallbackText?: string;
  metadata?: DiagnosisMetadata | null;
}>();

const profileCopy = computed(() => resolveCapabilityProfileCopy(props.insights ?? undefined));
const strengths = computed(() => (props.profile.strengths.length ? props.profile.strengths : ['系统已捕捉到你的当前优势，后续学习会继续放大这些长处。']));
const weaknesses = computed(() => (props.profile.weaknesses.length ? props.profile.weaknesses : ['当前没有明显短板提示，系统会按稳妥节奏继续推进。']));
const metadataSummary = computed(() => resolveDiagnosisMetaSummary(props.metadata));
</script>

<template>
  <section class="rounded-[1.9rem] border border-slate-200 bg-white p-6 shadow-sm">
    <div class="flex flex-col gap-4 lg:flex-row lg:items-start lg:justify-between">
      <div class="max-w-3xl">
        <p class="text-xs font-semibold uppercase tracking-[0.22em] text-emerald-600">能力快照</p>
        <h2 class="mt-2 text-2xl font-semibold tracking-tight text-slate-950">系统已经理解了你的当前起点</h2>
        <p class="mt-3 text-sm leading-7 text-slate-600">
          {{ profileCopy.summary }}
        </p>
      </div>
      <div class="rounded-2xl bg-emerald-50 px-4 py-3 text-sm text-emerald-800">
        <p class="text-xs font-semibold uppercase tracking-[0.18em] text-emerald-600">诊断状态</p>
        <p class="mt-2 font-semibold">{{ status || '能力快照已生成' }}</p>
      </div>
    </div>

    <div class="mt-6 grid gap-4 md:grid-cols-2">
      <div class="rounded-2xl bg-slate-50 p-4">
        <p class="text-xs font-semibold uppercase tracking-[0.18em] text-slate-400">当前水平</p>
        <p class="mt-2 text-lg font-semibold text-slate-950">{{ profile.currentLevel.label || '系统正在判断中' }}</p>
      </div>
      <div class="rounded-2xl bg-slate-50 p-4">
        <p class="text-xs font-semibold uppercase tracking-[0.18em] text-slate-400">学习偏好</p>
        <p class="mt-2 text-sm leading-6 text-slate-800">
          {{ profile.learningPreference?.label || '系统会先按通用节奏为你安排内容，并在后续学习中继续微调。' }}
        </p>
      </div>
      <div class="rounded-2xl bg-slate-50 p-4">
        <p class="text-xs font-semibold uppercase tracking-[0.18em] text-slate-400">时间预算</p>
        <p class="mt-2 text-sm leading-6 text-slate-800">
          {{ profile.timeBudget?.label || '暂未识别到明确时间预算，系统会先采用相对稳妥的节奏。' }}
        </p>
      </div>
      <div class="rounded-2xl bg-slate-50 p-4">
        <p class="text-xs font-semibold uppercase tracking-[0.18em] text-slate-400">目标导向</p>
        <p class="mt-2 text-sm leading-6 text-slate-800">
          {{ profile.goalOrientation?.label || '系统会优先从与你目标最相关的起步内容开始安排。' }}
        </p>
      </div>
    </div>

    <div class="mt-6 grid gap-4 md:grid-cols-2">
      <div class="rounded-2xl border border-emerald-100 bg-emerald-50/70 p-5">
        <p class="text-sm font-semibold text-emerald-900">你的优势</p>
        <div class="mt-3 flex flex-wrap gap-2">
          <span
            v-for="item in strengths"
            :key="item"
            class="rounded-full bg-white px-3 py-1 text-xs font-medium text-emerald-700"
          >
            {{ item }}
          </span>
        </div>
      </div>
      <div class="rounded-2xl border border-amber-100 bg-amber-50/80 p-5">
        <p class="text-sm font-semibold text-amber-900">当前需要注意</p>
        <div class="mt-3 flex flex-wrap gap-2">
          <span
            v-for="item in weaknesses"
            :key="item"
            class="rounded-full bg-white px-3 py-1 text-xs font-medium text-amber-700"
          >
            {{ item }}
          </span>
        </div>
      </div>
    </div>

    <div class="mt-6 rounded-2xl border border-slate-200 bg-slate-50 p-5 text-sm leading-7 text-slate-700">
      <p class="text-sm font-semibold text-slate-900">诊断结论</p>
      <p class="mt-2">{{ profileCopy.planExplanation }}</p>
    </div>

    <div class="mt-6 rounded-2xl border border-slate-200 bg-slate-50/80 p-5 text-sm text-slate-600">
      <p class="text-xs font-semibold uppercase tracking-[0.18em] text-slate-400">辅助信息</p>
      <div class="mt-3 grid gap-3 md:grid-cols-3">
        <div>
          <p class="text-xs text-slate-400">生成方式</p>
          <p class="mt-1 font-medium text-slate-700">{{ sourceText || '系统生成' }}</p>
        </div>
        <div>
          <p class="text-xs text-slate-400">生成说明</p>
          <p class="mt-1 font-medium text-slate-700">{{ fallbackText || '本次结果按标准生成流程整理。' }}</p>
        </div>
        <div>
          <p class="text-xs text-slate-400">诊断辅助信息</p>
          <p class="mt-1 font-medium text-slate-700">{{ metadataSummary }}</p>
        </div>
      </div>
    </div>
  </section>
</template>
