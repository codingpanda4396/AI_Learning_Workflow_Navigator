<script setup lang="ts">
defineProps<{
  title: string;
  description: string;
}>();
</script>

<template>
  <div class="app-empty">
    <h3 class="text-xl font-semibold tracking-[-0.03em] text-slate-950">{{ title }}</h3>
    <p class="mx-auto mt-3 max-w-xl text-sm leading-7 text-slate-600">{{ description }}</p>
    <div class="mt-5 flex justify-center">
      <slot />
    </div>
  </div>
</template>
