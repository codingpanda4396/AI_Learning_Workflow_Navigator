<script setup lang="ts">
interface PageHeaderProps {
  title: string
  subtitle?: string
  eyebrow?: string
}

defineProps<PageHeaderProps>()
</script>

<template>
  <header class="header">
    <p v-if="eyebrow" class="eyebrow">{{ eyebrow }}</p>
    <h1 class="title">{{ title }}</h1>
    <p v-if="subtitle" class="subtitle">{{ subtitle }}</p>
    <slot />
  </header>
</template>

<style scoped>
.header {
  display: flex;
  flex-direction: column;
  gap: var(--space-sm);
}

.eyebrow {
  font-family: var(--font-body);
  font-size: var(--font-size-xs);
  font-weight: 500;
  color: var(--color-accent-coral);
  text-transform: uppercase;
  letter-spacing: 1.5px;
}

.title {
  font-family: var(--font-display);
  font-size: var(--font-size-2xl);
  font-weight: 600;
  color: var(--color-text);
  line-height: 1.2;
  margin: 0;
}

.subtitle {
  font-family: var(--font-body);
  font-size: var(--font-size-md);
  color: var(--color-text-secondary);
  line-height: 1.5;
  margin: 0;
}
</style>
