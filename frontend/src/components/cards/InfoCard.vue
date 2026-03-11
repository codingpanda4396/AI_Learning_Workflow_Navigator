<script setup lang="ts">
defineProps<{
  title: string;
  value?: string | number;
  hint?: string;
}>();
</script>

<template>
  <div class="rounded-3xl bg-white p-5 shadow-sm ring-1 ring-slate-200">
    <p class="text-sm text-slate-500">{{ title }}</p>
    <p class="mt-2 text-2xl font-semibold text-slate-900">{{ value || '--' }}</p>
    <p v-if="hint" class="mt-2 text-xs text-slate-400">{{ hint }}</p>
  </div>
</template>
