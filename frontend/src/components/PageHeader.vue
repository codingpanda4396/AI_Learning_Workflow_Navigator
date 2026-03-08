<script setup lang="ts">
interface PageHeaderProps {
  title: string
  subtitle?: string
  eyebrow?: string
}

defineProps<PageHeaderProps>()
</script>

<template>
  <header class="page-header">
    <p v-if="eyebrow" class="eyebrow">{{ eyebrow }}</p>
    <h1 class="title">{{ title }}</h1>
    <p v-if="subtitle" class="subtitle">{{ subtitle }}</p>
    <slot />
  </header>
</template>

<style scoped>
.page-header {
  display: flex;
  flex-direction: column;
  gap: var(--space-sm);
}

.eyebrow {
  color: var(--color-primary-hover);
  font-weight: 600;
  font-size: var(--font-size-xs);
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.title {
  font-size: clamp(1.75rem, 5vw, 2.5rem);
  line-height: 1.15;
  letter-spacing: -0.02em;
  color: var(--color-text);
  margin: 0;
}

.subtitle {
  margin: 0;
  color: var(--color-text-secondary);
  max-width: 68ch;
}
</style>
