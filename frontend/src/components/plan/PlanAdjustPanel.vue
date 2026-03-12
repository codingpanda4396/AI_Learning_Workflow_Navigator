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
    eyebrow="轻量微调"
    title="如果你想微调节奏，可以只改这几项"
    description="你不是在手动排课，而是在告诉系统这一轮更偏向哪种推进方式。调整后会重新生成方案。"
    compact
  >
    <div class="grid gap-5 lg:grid-cols-3">
      <div>
        <p class="text-sm font-semibold text-slate-900">学习强度</p>
        <div class="mt-3 flex flex-wrap gap-2">
          <button
            v-for="item in intensityOptions"
            :key="item"
            type="button"
            class="rounded-full px-4 py-2 text-sm font-medium transition"
            :class="model.intensity === item ? 'bg-slate-950 text-white' : 'bg-slate-100 text-slate-600 hover:bg-slate-200'"
            :disabled="disabled"
            @click="model = { ...model, intensity: item }"
          >
            {{ INTENSITY_LABELS[item] }}
          </button>
        </div>
      </div>

      <div>
        <p class="text-sm font-semibold text-slate-900">学习模式</p>
        <div class="mt-3 flex flex-wrap gap-2">
          <button
            v-for="item in learningModeOptions"
            :key="item"
            type="button"
            class="rounded-full px-4 py-2 text-sm font-medium transition"
            :class="model.learningMode === item ? 'bg-slate-950 text-white' : 'bg-slate-100 text-slate-600 hover:bg-slate-200'"
            :disabled="disabled"
            @click="model = { ...model, learningMode: item }"
          >
            {{ LEARNING_MODE_LABELS[item] }}
          </button>
        </div>
      </div>

      <div>
        <p class="text-sm font-semibold text-slate-900">是否优先补前置基础</p>
        <div class="mt-3 flex flex-wrap gap-2">
          <button
            type="button"
            class="rounded-full px-4 py-2 text-sm font-medium transition"
            :class="model.prioritizeFoundation ? 'bg-slate-950 text-white' : 'bg-slate-100 text-slate-600 hover:bg-slate-200'"
            :disabled="disabled"
            @click="model = { ...model, prioritizeFoundation: true }"
          >
            是
          </button>
          <button
            type="button"
            class="rounded-full px-4 py-2 text-sm font-medium transition"
            :class="!model.prioritizeFoundation ? 'bg-slate-950 text-white' : 'bg-slate-100 text-slate-600 hover:bg-slate-200'"
            :disabled="disabled"
            @click="model = { ...model, prioritizeFoundation: false }"
          >
            否
          </button>
        </div>
      </div>
    </div>

    <div class="mt-5 flex flex-col gap-3 rounded-[1.5rem] bg-slate-50 p-4 md:flex-row md:items-center md:justify-between">
      <p class="text-sm leading-6 text-slate-600">
        {{ regenerating ? '系统正在结合你的新偏好重新组织这轮路径，请稍等片刻。' : '当前微调不会改成复杂配置页，只会影响系统重新组织起点、节奏和任务顺序。' }}
      </p>
      <button
        type="button"
        class="rounded-2xl bg-white px-5 py-3 text-sm font-semibold text-slate-900 ring-1 ring-slate-200 transition hover:bg-slate-100 disabled:cursor-not-allowed disabled:opacity-60"
        :disabled="disabled"
        @click="$emit('regenerate')"
      >
        {{ regenerating ? '重新生成中...' : '按当前偏好重新生成' }}
      </button>
    </div>
  </PageSection>
</template>
