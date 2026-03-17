<template>
  <div class="rounded-card border border-border bg-white p-4 shadow-card">
    <div v-if="title" class="mb-2 text-sm font-medium text-text-secondary">
      {{ title }}
    </div>
    <div class="text-lg font-semibold text-text-primary">
      <slot />
    </div>
  </div>
</template>

<script setup lang="ts">
defineProps<{
  title?: string
}>()
</script>
