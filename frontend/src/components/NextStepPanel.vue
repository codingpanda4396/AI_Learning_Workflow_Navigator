<script setup lang="ts">
defineProps<{
  title: string
  description: string
}>()
</script>

<template>
  <section class="next-step-panel">
    <span class="label">下一步建议</span>
    <h3>{{ title }}</h3>
    <p>{{ description }}</p>
  </section>
</template>

<style scoped>
.next-step-panel {
  display: grid;
  gap: 10px;
  padding: 20px;
  border-radius: var(--radius-lg);
  border: 1px solid rgba(107, 159, 255, 0.28);
  background: rgba(13, 21, 36, 0.9);
}

.label {
  color: var(--color-text-muted);
  font-size: var(--font-size-xs);
  letter-spacing: 0.08em;
  text-transform: uppercase;
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
