<script setup lang="ts">
defineProps<{
  current: number;
  total: number;
}>();
</script>

<template>
  <section class="text-center">
    <p class="text-sm text-slate-500">问题进度</p>
    <p class="mt-1 text-2xl font-semibold text-slate-950">{{ current }} / {{ total }}</p>
  </section>
</template>
