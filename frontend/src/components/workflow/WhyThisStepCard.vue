<template>
  <div class="rounded-card border border-border bg-white p-4 md:p-5">
    <p class="text-xs font-medium text-text-secondary">为什么先做这个</p>
    <p class="mt-1.5 text-sm leading-relaxed text-text-primary">
      {{ text }}
    </p>
  </div>
</template>

<script setup lang="ts">
defineProps<{
  text: string
}>()
</script>
