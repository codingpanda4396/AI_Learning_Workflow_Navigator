<script setup lang="ts">
import PageSection from '@/components/common/PageSection.vue';
import { INTENSITY_LABELS, LEARNING_MODE_LABELS } from '@/constants/learningPlan';
import type { LearningIntensity, LearningMode, PlanAdjustments } from '@/types/learningPlan';

const model = defineModel<PlanAdjustments>({ required: true });

defineProps<{
  disabled?: boolean;
  regenerating?: boolean;
}>();

defineEmits<{
  regenerate: [];
}>();

const intensityOptions: LearningIntensity[] = ['LIGHT', 'STANDARD', 'INTENSIVE'];
const learningModeOptions: LearningMode[] = ['EXPLAIN_THEN_PRACTICE', 'LEARN_BY_DOING'];
</script>

<template>
  <PageSection
    eyebrow="学习设置"
    title="按你的节奏微调这轮规划"
    description="你可以先微调学习节奏、学习方式和起点难度，再重新生成这轮学习规划。"
    compact
  >
    <div class="grid gap-5 lg:grid-cols-3">
      <div class="rounded-[1.5rem] bg-slate-50 p-4">
        <p class="text-sm font-semibold text-slate-900">学习节奏</p>
        <p class="mt-2 text-xs leading-5 text-slate-500">决定这轮学习更轻松、标准还是更集中。</p>
        <div class="mt-3 flex flex-wrap gap-2">
          <button
            v-for="item in intensityOptions"
            :key="item"
            type="button"
            class="rounded-full border px-4 py-2 text-sm font-medium transition"
            :class="model.intensity === item ? 'border-slate-950 bg-slate-950 text-white' : 'border-slate-200 bg-white text-slate-600 hover:border-slate-300 hover:bg-slate-100'"
            :disabled="disabled"
            @click="model = { ...model, intensity: item }"
          >
            {{ INTENSITY_LABELS[item] }}
          </button>
        </div>
      </div>

      <div class="rounded-[1.5rem] bg-slate-50 p-4">
        <p class="text-sm font-semibold text-slate-900">学习方式</p>
        <p class="mt-2 text-xs leading-5 text-slate-500">选择更偏“先理解再练习”还是“边做边学”的方式。</p>
        <div class="mt-3 flex flex-wrap gap-2">
          <button
            v-for="item in learningModeOptions"
            :key="item"
            type="button"
            class="rounded-full border px-4 py-2 text-sm font-medium transition"
            :class="model.learningMode === item ? 'border-slate-950 bg-slate-950 text-white' : 'border-slate-200 bg-white text-slate-600 hover:border-slate-300 hover:bg-slate-100'"
            :disabled="disabled"
            @click="model = { ...model, learningMode: item }"
          >
            {{ LEARNING_MODE_LABELS[item] }}
          </button>
        </div>
      </div>

      <div class="rounded-[1.5rem] bg-slate-50 p-4">
        <p class="text-sm font-semibold text-slate-900">起点难度</p>
        <p class="mt-2 text-xs leading-5 text-slate-500">决定是先把基础打稳，还是直接切入当前重点。</p>
        <div class="mt-3 flex flex-wrap gap-2">
          <button
            type="button"
            class="rounded-full border px-4 py-2 text-sm font-medium transition"
            :class="model.prioritizeFoundation ? 'border-slate-950 bg-slate-950 text-white' : 'border-slate-200 bg-white text-slate-600 hover:border-slate-300 hover:bg-slate-100'"
            :disabled="disabled"
            @click="model = { ...model, prioritizeFoundation: true }"
          >
            先从更稳的基础开始
          </button>
          <button
            type="button"
            class="rounded-full border px-4 py-2 text-sm font-medium transition"
            :class="!model.prioritizeFoundation ? 'border-slate-950 bg-slate-950 text-white' : 'border-slate-200 bg-white text-slate-600 hover:border-slate-300 hover:bg-slate-100'"
            :disabled="disabled"
            @click="model = { ...model, prioritizeFoundation: false }"
          >
            直接进入当前重点
          </button>
        </div>
      </div>
    </div>

    <div class="mt-5 flex flex-col gap-3 rounded-[1.6rem] border border-slate-200 bg-white p-4 md:flex-row md:items-center md:justify-between">
      <p class="text-sm leading-6 text-slate-600">
        {{ regenerating ? '正在根据最新设置重新生成学习规划。' : '如果你想换一种节奏开始，可以先调整设置，再重新生成规划。' }}
      </p>
      <button
        type="button"
        class="rounded-2xl border border-slate-200 bg-slate-50 px-5 py-3 text-sm font-semibold text-slate-900 transition hover:bg-slate-100 disabled:cursor-not-allowed disabled:opacity-60"
        :disabled="disabled"
        @click="$emit('regenerate')"
      >
        {{ regenerating ? '重新生成中...' : '按当前设置重新生成' }}
      </button>
    </div>
  </PageSection>
</template>
