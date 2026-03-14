<script setup lang="ts">
defineProps<{
  title: string;
  learnerState: string;
}>();
</script>

<template>
  <section class="rounded-[28px] border border-slate-200 bg-white px-6 py-6 shadow-sm sm:px-8">
    <p class="text-xs font-semibold uppercase tracking-[0.18em] text-slate-400">系统已感知到</p>
    <h1 class="mt-3 text-2xl font-semibold leading-tight text-slate-950 sm:text-3xl">{{ title }}</h1>
    <p class="mt-4 text-base leading-8 text-slate-700">{{ learnerState }}</p>
  </section>
</template>
