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
    eyebrow="偏好校准"
    title="如果你想微调，这里只校准推进偏好"
    description="不用手动排课。你只需要告诉 AI 这轮更想怎么推进，它会按这个方向重新组织起点、节奏和任务顺序。"
    compact
  >
    <div class="grid gap-5 lg:grid-cols-3">
      <div class="rounded-[1.5rem] bg-slate-50 p-4">
        <p class="text-sm font-semibold text-slate-900">这轮强度</p>
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
        <p class="text-sm font-semibold text-slate-900">吸收方式</p>
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
        <p class="text-sm font-semibold text-slate-900">起步策略</p>
        <div class="mt-3 flex flex-wrap gap-2">
          <button
            type="button"
            class="rounded-full border px-4 py-2 text-sm font-medium transition"
            :class="model.prioritizeFoundation ? 'border-slate-950 bg-slate-950 text-white' : 'border-slate-200 bg-white text-slate-600 hover:border-slate-300 hover:bg-slate-100'"
            :disabled="disabled"
            @click="model = { ...model, prioritizeFoundation: true }"
          >
            先补前置
          </button>
          <button
            type="button"
            class="rounded-full border px-4 py-2 text-sm font-medium transition"
            :class="!model.prioritizeFoundation ? 'border-slate-950 bg-slate-950 text-white' : 'border-slate-200 bg-white text-slate-600 hover:border-slate-300 hover:bg-slate-100'"
            :disabled="disabled"
            @click="model = { ...model, prioritizeFoundation: false }"
          >
            直接主线
          </button>
        </div>
      </div>
    </div>

    <div class="mt-5 flex flex-col gap-3 rounded-[1.6rem] border border-slate-200 bg-white p-4 md:flex-row md:items-center md:justify-between">
      <p class="text-sm leading-6 text-slate-600">
        {{ regenerating ? 'AI 正在按你的新偏好重排这轮方案，请稍等片刻。' : '这些调整只会校准推进方式，不会把页面变成复杂配置中心。' }}
      </p>
      <button
        type="button"
        class="rounded-2xl border border-slate-200 bg-slate-50 px-5 py-3 text-sm font-semibold text-slate-900 transition hover:bg-slate-100 disabled:cursor-not-allowed disabled:opacity-60"
        :disabled="disabled"
        @click="$emit('regenerate')"
      >
        {{ regenerating ? '重新生成中...' : '按当前偏好重排方案' }}
      </button>
    </div>
  </PageSection>
</template>
