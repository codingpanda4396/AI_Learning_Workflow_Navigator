<template>
  <section class="rounded-2xl border border-slate-200 bg-white p-4 shadow-sm">
    <p class="text-xs font-semibold uppercase tracking-wide text-slate-500">系统总结</p>
    <p class="mt-1 text-sm leading-relaxed text-slate-900">{{ summary }}</p>
  </section>
</template>

<script setup lang="ts">
defineProps<{
  summary: string
}>()
</script>
