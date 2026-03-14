<script setup lang="ts">
import { computed } from 'vue';
import FlowFieldCard from '@/components/common/FlowFieldCard.vue';
import { resolveCapabilityProfileCopy } from '@/types/diagnosis';
import type { CapabilityProfile, DiagnosisInsights } from '@/types/diagnosis';

const props = defineProps<{
  profile: CapabilityProfile;
  insights?: DiagnosisInsights | null;
  status?: string;
}>();

const profileCopy = computed(() => resolveCapabilityProfileCopy(props.insights ?? undefined));
const strengths = computed(() => (props.profile.strengths.length ? props.profile.strengths : ['系统已捕捉到你的当前优势，后续学习会继续放大这些长处。']));
const weaknesses = computed(() => (props.profile.weaknesses.length ? props.profile.weaknesses : ['当前没有明显短板提示，系统会按稳妥节奏继续推进。']));
</script>

<template>
  <section class="rounded-[2rem] border border-slate-200 bg-white p-6 shadow-[0_20px_60px_rgba(15,23,42,0.08)] md:p-7">
    <div class="grid gap-5 lg:grid-cols-[minmax(0,1.55fr)_minmax(280px,0.85fr)]">
      <div class="rounded-[1.8rem] border border-emerald-100 bg-linear-to-br from-emerald-50 via-white to-white p-6">
        <p class="text-xs font-semibold uppercase tracking-[0.22em] text-emerald-600">诊断结果</p>
        <h2 class="mt-2 text-2xl font-semibold tracking-tight text-slate-950 md:text-[2rem]">这份能力快照将作为后续学习规划的起点</h2>
        <p class="mt-4 text-base leading-8 text-slate-700">
          {{ profileCopy.summary }}
        </p>
        <div class="mt-6 rounded-[1.4rem] border border-slate-200 bg-white/90 p-5">
          <p class="text-xs font-semibold uppercase tracking-[0.18em] text-slate-400">建议下一步</p>
          <p class="mt-3 text-sm leading-7 text-slate-700">{{ profileCopy.planExplanation }}</p>
        </div>
      </div>

      <div class="rounded-[1.8rem] border border-slate-200 bg-slate-50/90 p-5">
        <p class="text-xs font-semibold uppercase tracking-[0.18em] text-slate-400">次级信息</p>
        <p class="mt-3 text-lg font-semibold tracking-tight text-slate-950">
          {{ status || '能力快照已生成' }}
        </p>
        <p class="mt-2 text-sm leading-6 text-slate-500">下面四项会直接映射到学习规划页，用来决定起点、节奏和路径重点。</p>
      </div>
    </div>

    <div class="mt-5 grid gap-4 md:grid-cols-2">
      <FlowFieldCard label="当前水平" tone="soft">
        <p class="mt-3 text-2xl font-semibold tracking-tight text-slate-950">
          {{ profile.currentLevel.label || '系统正在判断中' }}
        </p>
      </FlowFieldCard>
      <FlowFieldCard label="学习偏好" tone="soft">
        <p class="mt-3 text-sm leading-7 text-slate-800">
          {{ profile.learningPreference?.label || '系统会先按通用节奏为你安排内容，并在后续学习中继续微调。' }}
        </p>
      </FlowFieldCard>
      <FlowFieldCard label="时间预算" tone="soft">
        <p class="mt-3 text-sm leading-7 text-slate-800">
          {{ profile.timeBudget?.label || '暂未识别到明确时间预算，系统会先采用相对稳妥的节奏。' }}
        </p>
      </FlowFieldCard>
      <FlowFieldCard label="目标导向" tone="soft">
        <p class="mt-3 text-sm leading-7 text-slate-800">
          {{ profile.goalOrientation?.label || '系统会优先从与你目标最相关的起步内容开始安排。' }}
        </p>
      </FlowFieldCard>
    </div>

    <div class="mt-5 grid gap-4 md:grid-cols-2">
      <div class="rounded-[1.7rem] border border-emerald-200 bg-emerald-50/80 p-5">
        <p class="text-sm font-semibold text-emerald-900">你的优势</p>
        <div class="mt-3 flex flex-wrap gap-2">
          <span
            v-for="item in strengths"
            :key="item"
            class="rounded-full border border-emerald-100 bg-white px-3 py-1.5 text-xs font-medium text-emerald-700"
          >
            {{ item }}
          </span>
        </div>
      </div>
      <div class="rounded-[1.7rem] border border-amber-200 bg-amber-50/85 p-5">
        <p class="text-sm font-semibold text-amber-900">当前需要关注</p>
        <div class="mt-3 flex flex-wrap gap-2">
          <span
            v-for="item in weaknesses"
            :key="item"
            class="rounded-full border border-amber-100 bg-white px-3 py-1.5 text-xs font-medium text-amber-700"
          >
            {{ item }}
          </span>
        </div>
      </div>
    </div>
  </section>
</template>
