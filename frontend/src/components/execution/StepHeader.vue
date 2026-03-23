<template>
  <header class="mb-6">
    <p class="text-sm font-semibold text-primary">当前步骤</p>
    <h1 class="mt-1 text-2xl font-bold text-text-primary md:text-3xl">
      {{ title }}
    </h1>
    <p class="mt-2 text-sm text-text-secondary">{{ goal }}</p>
  </header>
</template>

<script setup lang="ts">
defineProps<{
  title: string
  goal: string
}>()
</script>
