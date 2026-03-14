<script setup lang="ts">
import AppButton from '@/components/ui/AppButton.vue';

defineProps<{
  title: string;
  strongestReason: string;
  riskIfSkip: string;
  estimate: string;
  ctaLabel: string;
  loading?: boolean;
}>();

defineEmits<{
  start: [];
}>();
</script>

<template>
  <section class="app-hero">
    <div class="grid gap-6 lg:grid-cols-[minmax(0,1fr)_240px] lg:items-end">
      <div class="space-y-4">
        <p class="app-eyebrow">为什么现在先学这个</p>
        <h1 class="app-title-lg">{{ title }}</h1>
        <p class="text-base leading-8 text-slate-700">{{ strongestReason }}</p>
        <p class="rounded-2xl border border-rose-100 bg-rose-50/70 px-4 py-3 text-sm leading-7 text-rose-900">
          {{ riskIfSkip }}
        </p>
      </div>

      <div class="rounded-3xl border border-slate-200 bg-white/95 p-5 shadow-[0_18px_48px_rgba(15,23,42,0.08)]">
        <p class="text-xs font-semibold uppercase tracking-[0.18em] text-slate-500">预计投入</p>
        <p class="mt-2 text-2xl font-semibold tracking-tight text-slate-950">{{ estimate }}</p>
        <p class="mt-2 text-sm leading-6 text-slate-600">确认后立刻进入第一轮，不用再做额外配置。</p>
        <AppButton class="mt-5 w-full" size="lg" :loading="loading" @click="$emit('start')">
          {{ ctaLabel }}
        </AppButton>
      </div>
    </div>
  </section>
</template>
