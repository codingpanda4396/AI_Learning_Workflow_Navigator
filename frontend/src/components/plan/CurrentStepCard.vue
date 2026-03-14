<script setup lang="ts">
import AppButton from '@/components/ui/AppButton.vue';

defineProps<{
  title: string;
  taskTitle: string;
  estimatedMinutesText: string;
  thisRoundBoundary: string;
  nextStepLabel: string;
  ctaHint: string;
  loading?: boolean;
  hasAlternatives?: boolean;
}>();

defineEmits<{
  start: [];
  showAlternatives: [];
}>();
</script>

<template>
  <section class="rounded-[28px] border border-slate-900/10 bg-[linear-gradient(140deg,#f8fafc_0%,#eef2ff_100%)] px-6 py-6 shadow-[0_20px_55px_rgba(15,23,42,0.08)] sm:px-8">
    <p class="text-xs font-semibold uppercase tracking-[0.18em] text-slate-400">本轮行动</p>
    <h2 class="mt-3 text-2xl font-semibold text-slate-950">{{ title }}</h2>

    <div class="mt-5 rounded-2xl border border-slate-200 bg-white px-4 py-4">
      <p class="text-sm text-slate-500">当前任务</p>
      <p class="mt-1 text-xl font-semibold text-slate-950">{{ taskTitle }}</p>
      <p class="mt-2 text-sm text-slate-600">预计投入：约 {{ estimatedMinutesText }}</p>
      <p class="mt-4 text-sm leading-7 text-slate-700">{{ thisRoundBoundary }}</p>
    </div>

    <div class="mt-6 space-y-3">
      <AppButton size="lg" block :loading="loading" @click="$emit('start')">
        确认并开始这一轮
      </AppButton>
      <AppButton v-if="hasAlternatives" size="lg" block variant="secondary" @click="$emit('showAlternatives')">
        查看其他方案
      </AppButton>
      <p class="text-sm text-slate-600">{{ nextStepLabel }}</p>
      <p class="text-sm text-slate-500">{{ ctaHint }}</p>
    </div>
  </section>
</template>
