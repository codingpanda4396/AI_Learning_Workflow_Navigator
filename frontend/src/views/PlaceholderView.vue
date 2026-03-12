<script setup lang="ts">
defineProps<{
  title: string;
  description?: string;
}>();
</script>

<template>
  <div class="mx-auto max-w-4xl px-5 py-12 md:px-6">
    <section class="rounded-[2rem] border border-slate-200/80 bg-white p-8 shadow-[0_20px_60px_rgba(15,23,42,0.06)]">
      <p class="text-xs font-semibold uppercase tracking-[0.28em] text-slate-400">Placeholder</p>
      <h1 class="mt-3 text-3xl font-semibold tracking-tight text-slate-950">{{ title }}</h1>
      <p class="mt-4 text-sm leading-7 text-slate-600">{{ description || '页面占位已就绪，后续可在此接入对应模块业务。' }}</p>
    </section>
  </div>
</template>
