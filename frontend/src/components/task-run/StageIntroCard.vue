<template>
  <section class="rounded-2xl border border-slate-200 bg-white px-4 py-3 shadow-sm">
    <p class="text-xs font-semibold uppercase tracking-wide text-slate-500">阶段引导</p>
    <p class="mt-1 text-sm font-medium text-slate-900">{{ text }}</p>
  </section>
</template>

<script setup lang="ts">
defineProps<{
  text: string
}>()
</script>
