<template>
  <div class="flex flex-col items-center justify-center rounded-card border border-red-200 bg-red-50/50 py-12 text-center">
    <p class="text-red-700">{{ message }}</p>
    <slot name="action" />
  </div>
</template>

<script setup lang="ts">
defineProps<{
  message?: string
}>()
</script>
