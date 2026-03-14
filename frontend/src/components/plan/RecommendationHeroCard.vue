<script setup lang="ts">
import AppButton from '@/components/ui/AppButton.vue';

defineProps<{
  sourceLabel: string;
  sourceType: 'llm' | 'fallback';
  confidenceLabel: string;
  confidenceLevel: 'high' | 'medium' | 'low';
  recommendationHeadline: string;
  recommendationSubtitle: string;
  currentTaskTitle: string;
  estimatedMinutes: string;
  priority: string;
  currentStatus: string;
  loading?: boolean;
}>();

defineEmits<{
  start: [];
  adjust: [];
  mastered: [];
}>();

function confidenceClass(level: 'high' | 'medium' | 'low') {
  switch (level) {
    case 'high':
      return 'bg-emerald-100 text-emerald-700';
    case 'low':
      return 'bg-amber-100 text-amber-700';
    case 'medium':
    default:
      return 'bg-sky-100 text-sky-700';
  }
}
</script>

<template>
  <section class="app-hero">
    <div class="grid gap-6 xl:grid-cols-[minmax(0,1.2fr)_320px]">
      <div>
        <div class="flex flex-wrap gap-2">
          <span class="app-badge" :class="sourceType === 'llm' ? 'border-sky-200 bg-sky-50 text-sky-700' : 'border-slate-200 bg-slate-100 text-slate-700'">
            {{ sourceLabel }}
          </span>
          <span class="app-badge" :class="confidenceClass(confidenceLevel)">
            {{ confidenceLabel }}
          </span>
        </div>

        <p class="app-eyebrow mt-5">AI 决策结论</p>
        <h1 class="app-title-lg mt-3">{{ recommendationHeadline }}</h1>
        <p class="mt-3 max-w-2xl text-sm leading-7 text-slate-600">{{ recommendationSubtitle }}</p>

        <div class="mt-6 rounded-[28px] border border-slate-200/80 bg-white/80 p-5 shadow-[0_18px_50px_rgba(15,23,42,0.08)]">
          <p class="text-xs font-semibold uppercase tracking-[0.18em] text-slate-400">当前学习动作</p>
          <h2 class="mt-3 text-[28px] font-semibold leading-[1.15] tracking-[-0.04em] text-slate-950">{{ currentTaskTitle }}</h2>
          <div class="mt-5 grid gap-3 sm:grid-cols-3">
            <div class="rounded-[20px] bg-slate-50 px-4 py-3">
              <p class="text-xs font-semibold uppercase tracking-[0.16em] text-slate-400">预计耗时</p>
              <p class="mt-2 text-base font-semibold text-slate-950">{{ estimatedMinutes }}</p>
            </div>
            <div class="rounded-[20px] bg-slate-50 px-4 py-3">
              <p class="text-xs font-semibold uppercase tracking-[0.16em] text-slate-400">当前优先级</p>
              <p class="mt-2 text-base font-semibold text-slate-950">{{ priority }}</p>
            </div>
            <div class="rounded-[20px] bg-slate-50 px-4 py-3">
              <p class="text-xs font-semibold uppercase tracking-[0.16em] text-slate-400">当前状态</p>
              <p class="mt-2 text-base font-semibold text-slate-950">{{ currentStatus }}</p>
            </div>
          </div>
        </div>
      </div>

      <div class="flex flex-col justify-between rounded-[28px] border border-slate-200/70 bg-[linear-gradient(180deg,rgba(15,23,42,0.96),rgba(30,41,59,0.92))] p-6 text-white shadow-[0_24px_80px_rgba(15,23,42,0.2)]">
        <div>
          <p class="text-xs font-semibold uppercase tracking-[0.18em] text-slate-300">现在最值得开始的一步</p>
          <p class="mt-4 text-2xl font-semibold leading-tight">{{ currentTaskTitle }}</p>
          <p class="mt-4 text-sm leading-7 text-slate-200">
            先把这一步打通，再决定要不要提速、跳学或换策略，会更稳。
          </p>
        </div>

        <div class="mt-8 space-y-3">
          <AppButton size="lg" block :loading="loading" @click="$emit('start')">开始这一小步</AppButton>
          <AppButton size="lg" block variant="secondary" @click="$emit('adjust')">换一种学法</AppButton>
          <button type="button" class="text-sm font-medium text-slate-200 transition hover:text-white" @click="$emit('mastered')">
            我已经会了，调整路径
          </button>
        </div>
      </div>
    </div>
  </section>
</template>
