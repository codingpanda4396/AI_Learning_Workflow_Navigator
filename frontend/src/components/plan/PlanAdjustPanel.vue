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
    eyebrow="调整"
    title="不手动编辑计划即可微调预览"
    description="这些控件仅改变发送给后端的规划意图，不会在前端硬编码任何路径。"
    compact
  >
    <div class="grid gap-5 lg:grid-cols-3">
      <div class="rounded-[1.5rem] bg-slate-50 p-4">
        <p class="text-sm font-semibold text-slate-900">强度</p>
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
        <p class="text-sm font-semibold text-slate-900">学习模式</p>
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
        <p class="text-sm font-semibold text-slate-900">起始策略</p>
        <div class="mt-3 flex flex-wrap gap-2">
          <button
            type="button"
            class="rounded-full border px-4 py-2 text-sm font-medium transition"
            :class="model.prioritizeFoundation ? 'border-slate-950 bg-slate-950 text-white' : 'border-slate-200 bg-white text-slate-600 hover:border-slate-300 hover:bg-slate-100'"
            :disabled="disabled"
            @click="model = { ...model, prioritizeFoundation: true }"
          >
            优先打基础
          </button>
          <button
            type="button"
            class="rounded-full border px-4 py-2 text-sm font-medium transition"
            :class="!model.prioritizeFoundation ? 'border-slate-950 bg-slate-950 text-white' : 'border-slate-200 bg-white text-slate-600 hover:border-slate-300 hover:bg-slate-100'"
            :disabled="disabled"
            @click="model = { ...model, prioritizeFoundation: false }"
          >
            直接进入重点
          </button>
        </div>
      </div>
    </div>

    <div class="mt-5 flex flex-col gap-3 rounded-[1.6rem] border border-slate-200 bg-white p-4 md:flex-row md:items-center md:justify-between">
      <p class="text-sm leading-6 text-slate-600">
        {{ regenerating ? '正在根据最新调整重新生成预览。' : '确认前可通过重新生成验证预览状态与后端推理。' }}
      </p>
      <button
        type="button"
        class="rounded-2xl border border-slate-200 bg-slate-50 px-5 py-3 text-sm font-semibold text-slate-900 transition hover:bg-slate-100 disabled:cursor-not-allowed disabled:opacity-60"
        :disabled="disabled"
        @click="$emit('regenerate')"
      >
        {{ regenerating ? '重新生成中...' : '重新生成预览' }}
      </button>
    </div>
  </PageSection>
</template>
