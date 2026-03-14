<script setup lang="ts">
import AppButton from '@/components/ui/AppButton.vue';

defineProps<{
  sourceLabel: string;
  sourceType: 'llm' | 'fallback';
  recommendationHeadline: string;
  recommendationReason: string;
  currentTaskTitle: string;
  estimatedMinutes: string;
  currentStatus: string;
  loading?: boolean;
}>();

defineEmits<{
  start: [];
  adjust: [];
  mastered: [];
}>();
</script>

<template>
  <section class="relative flex min-h-[calc(100vh-128px)] items-center overflow-hidden rounded-[36px] border border-slate-200/80 bg-[radial-gradient(circle_at_top_left,rgba(186,230,253,0.85),transparent_32%),linear-gradient(135deg,#f8fafc_0%,#e2e8f0_52%,#f8fafc_100%)] px-6 py-8 shadow-[0_28px_90px_rgba(15,23,42,0.08)] sm:px-8 lg:px-10">
    <div class="absolute inset-y-10 right-[-80px] hidden w-[320px] rounded-full bg-slate-900/6 blur-3xl lg:block" />
    <div class="relative grid w-full gap-8 lg:grid-cols-[minmax(0,1.4fr)_320px] lg:items-end">
      <div class="max-w-3xl">
        <div class="flex flex-wrap items-center gap-3">
          <p class="app-eyebrow !mb-0">AI建议</p>
          <span
            v-if="sourceLabel"
            class="rounded-full border px-3 py-1 text-xs font-semibold"
            :class="sourceType === 'llm' ? 'border-sky-200 bg-sky-50 text-sky-700' : 'border-slate-200 bg-slate-100 text-slate-700'"
          >
            {{ sourceLabel }}
          </span>
        </div>

        <h1 class="mt-4 max-w-2xl text-[38px] font-semibold leading-[1.08] tracking-[-0.05em] text-slate-950 sm:text-[48px]">
          第一步：{{ currentTaskTitle }}
        </h1>

        <p class="mt-5 text-sm font-medium text-slate-500">接下来先学这里</p>
        <p class="mt-2 max-w-2xl text-lg leading-8 text-slate-700">
          {{ recommendationReason }}
        </p>
      </div>

      <div class="rounded-[28px] border border-slate-900/10 bg-white/92 p-5 shadow-[0_20px_50px_rgba(15,23,42,0.08)] backdrop-blur">
        <div class="space-y-4">
          <div>
            <p class="text-sm text-slate-500">任务</p>
            <p class="mt-1 text-xl font-semibold text-slate-950">{{ currentTaskTitle }}</p>
          </div>
          <div class="grid gap-3 sm:grid-cols-2 lg:grid-cols-1">
            <div class="rounded-[20px] bg-slate-50 px-4 py-3">
              <p class="text-xs font-semibold uppercase tracking-[0.16em] text-slate-400">预计时间</p>
              <p class="mt-2 text-base font-semibold text-slate-950">约 {{ estimatedMinutes }}</p>
            </div>
            <div class="rounded-[20px] bg-slate-50 px-4 py-3">
              <p class="text-xs font-semibold uppercase tracking-[0.16em] text-slate-400">现在状态</p>
              <p class="mt-2 text-base font-semibold text-slate-950">{{ currentStatus }}</p>
            </div>
          </div>
        </div>

        <div class="mt-6 space-y-3">
          <AppButton size="lg" block :loading="loading" @click="$emit('start')">开始第一步</AppButton>
          <AppButton size="lg" block variant="secondary" @click="$emit('adjust')">换一种学法</AppButton>
          <button type="button" class="w-full text-center text-sm font-medium text-slate-600 transition hover:text-slate-950" @click="$emit('mastered')">
            我已经会了
          </button>
        </div>
      </div>
    </div>
  </section>
</template>
