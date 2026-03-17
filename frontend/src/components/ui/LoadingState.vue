<template>
  <div class="flex flex-col items-center justify-center py-12">
    <div class="h-8 w-8 animate-spin rounded-full border-2 border-primary border-t-transparent" />
    <p class="mt-3 text-sm text-text-secondary">{{ message }}</p>
  </div>
</template>

<script setup lang="ts">
defineProps<{
  message?: string
}>()
</script>
