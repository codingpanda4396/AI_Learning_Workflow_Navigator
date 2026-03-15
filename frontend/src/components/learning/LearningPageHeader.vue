<script setup lang="ts">
defineProps<{
  eyebrow: string;
  title: string;
  lead?: string;
}>();
</script>

<template>
  <section class="app-hero">
    <p class="app-eyebrow">{{ eyebrow }}</p>
    <h1 class="app-title-lg mt-4">{{ title }}</h1>
    <p v-if="lead" class="app-text-lead mt-4">{{ lead }}</p>
  </section>
</template>
