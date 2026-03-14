<script setup lang="ts">
defineProps<{
  level: string;
  summary?: string;
}>();
</script>

<template>
  <section
    class="rounded-[2.2rem] border border-emerald-100 bg-[linear-gradient(180deg,rgba(236,253,245,0.98)_0%,rgba(255,255,255,0.98)_100%)] px-6 py-10 text-center shadow-[0_24px_80px_rgba(16,185,129,0.10)] md:px-10 md:py-14"
  >
    <p class="text-xs font-semibold uppercase tracking-[0.28em] text-emerald-600">AI 诊断完成</p>
    <p class="mt-8 text-sm font-medium tracking-[0.18em] text-slate-500">当前能力等级</p>
    <h1 class="mt-4 text-4xl font-semibold tracking-tight text-slate-950 md:text-6xl">
      你当前处于：<span class="text-emerald-600">{{ level }}</span>
    </h1>
    <p v-if="summary" class="mx-auto mt-6 max-w-2xl text-base leading-8 text-slate-600 md:text-lg">
      {{ summary }}
    </p>
  </section>
</template>
