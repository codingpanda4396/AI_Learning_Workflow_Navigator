<script setup lang="ts">
defineProps<{
  title: string;
  description?: string;
}>();
</script>

<template>
  <div class="rounded-[1.6rem] border border-slate-200 bg-white p-5 shadow-sm">
    <h3 class="text-sm font-semibold text-slate-900">{{ title }}</h3>
    <p v-if="description" class="mt-2 text-sm leading-7 text-slate-600">{{ description }}</p>
    <div :class="description ? 'mt-4' : 'mt-3'">
      <slot />
    </div>
  </div>
</template>
