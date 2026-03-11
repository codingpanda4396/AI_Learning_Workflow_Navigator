<script setup lang="ts">
defineProps<{
  title: string
  description: string
}>()
</script>

<template>
  <section class="empty-state-panel">
    <h3>{{ title }}</h3>
    <p>{{ description }}</p>
  </section>
</template>

<style scoped>
.empty-state-panel {
  display: grid;
  gap: 10px;
  padding: 22px;
  border-radius: var(--radius-lg);
  border: 1px dashed rgba(61, 80, 104, 0.56);
  background: rgba(10, 15, 24, 0.72);
}

h3,
p {
  margin: 0;
}

p {
  color: var(--color-text-secondary);
  line-height: 1.7;
}
</style>
