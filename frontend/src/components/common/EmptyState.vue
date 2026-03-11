<script setup lang="ts">
defineProps<{
  title: string;
  description: string;
}>();
</script>

<template>
  <div class="rounded-[1.8rem] border border-dashed border-slate-300 bg-white/90 p-8 text-center">
    <h3 class="text-lg font-semibold text-slate-950">{{ title }}</h3>
    <p class="mt-3 text-sm leading-7 text-slate-500">{{ description }}</p>
    <div class="mt-4">
      <slot />
    </div>
  </div>
</template>
