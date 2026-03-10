<script setup lang="ts">
interface SummaryMetaItem {
  label: string
  value: string
}

defineProps<{
  eyebrow?: string
  title: string
  subtitle?: string
  goal: string
  metaItems: SummaryMetaItem[]
}>()
</script>

<template>
  <section class="summary-hero">
    <div class="hero-copy">
      <p v-if="eyebrow" class="eyebrow">{{ eyebrow }}</p>
      <h1>{{ title }}</h1>
      <p v-if="subtitle" class="subtitle">{{ subtitle }}</p>
      <p class="goal">{{ goal }}</p>
    </div>

    <div class="meta-grid">
      <article v-for="item in metaItems" :key="item.label" class="meta-item">
        <span>{{ item.label }}</span>
        <strong>{{ item.value }}</strong>
      </article>
    </div>
  </section>
</template>

<style scoped>
.summary-hero {
  display: grid;
  gap: 20px;
  padding: clamp(24px, 3vw, 34px);
  border: 1px solid rgba(61, 80, 104, 0.5);
  border-radius: var(--radius-xl);
  background: rgba(15, 21, 33, 0.96);
}

.hero-copy {
  display: grid;
  gap: 10px;
}

.eyebrow,
.meta-item span {
  margin: 0;
  color: var(--color-text-muted);
  font-size: var(--font-size-xs);
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.hero-copy h1,
.subtitle,
.goal {
  margin: 0;
}

.subtitle,
.goal {
  color: var(--color-text-secondary);
  line-height: 1.7;
}

.meta-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}

.meta-item {
  display: grid;
  gap: 8px;
  padding: 16px;
  border-radius: var(--radius-lg);
  border: 1px solid rgba(61, 80, 104, 0.4);
  background: rgba(10, 15, 24, 0.7);
}

.meta-item strong {
  color: var(--color-text);
  line-height: 1.5;
}

@media (max-width: 900px) {
  .meta-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 560px) {
  .meta-grid {
    grid-template-columns: 1fr;
  }
}
</style>
