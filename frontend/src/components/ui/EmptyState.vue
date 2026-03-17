<template>
  <div class="flex flex-col items-center justify-center rounded-card border border-dashed border-border bg-gray-50/50 py-12 text-center">
    <p class="text-text-secondary">{{ message }}</p>
    <slot name="action" />
  </div>
</template>

<script setup lang="ts">
defineProps<{
  message?: string
}>()
</script>
