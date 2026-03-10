<script setup lang="ts">
interface SummaryItem {
  label: string
  value: string
}

defineProps<{
  title: string
  course: string
  chapter: string
  stageLabel: string
  goal: string
  secondaryMeta?: string
  summaryItems?: SummaryItem[]
}>()
</script>

<template>
  <section class="header-card">
    <div class="header-main">
      <div class="eyebrow">训练检测</div>
      <h1>{{ title }}</h1>
      <p class="meta">课程：{{ course }}｜章节：{{ chapter }}｜当前阶段：{{ stageLabel }}</p>
      <p class="goal">{{ goal }}</p>
      <p v-if="secondaryMeta" class="secondary-meta">{{ secondaryMeta }}</p>
    </div>

    <div v-if="summaryItems?.length" class="summary-grid">
      <article v-for="item in summaryItems" :key="item.label" class="summary-item">
        <span>{{ item.label }}</span>
        <strong>{{ item.value }}</strong>
      </article>
    </div>
  </section>
</template>

<style scoped>
.header-card {
  display: grid;
  gap: var(--space-xl);
  padding: clamp(20px, 3vw, 32px);
  border: 1px solid rgba(61, 80, 104, 0.5);
  border-radius: var(--radius-xl);
  background:
    radial-gradient(circle at top right, rgba(107, 159, 255, 0.16), transparent 32%),
    linear-gradient(160deg, rgba(21, 29, 43, 0.97), rgba(12, 18, 28, 0.95));
  box-shadow: var(--shadow-md);
}

.header-main {
  display: grid;
  gap: 10px;
}

.eyebrow,
.summary-item span,
.secondary-meta {
  font-size: var(--font-size-xs);
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: var(--color-text-muted);
}

.meta,
.goal {
  color: var(--color-text-secondary);
}

.goal {
  font-size: var(--font-size-md);
  line-height: 1.7;
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: var(--space-md);
}

.summary-item {
  display: grid;
  gap: 8px;
  padding: var(--space-lg);
  border-radius: var(--radius-lg);
  border: 1px solid rgba(61, 80, 104, 0.45);
  background: rgba(10, 16, 27, 0.65);
}

.summary-item strong {
  font-size: var(--font-size-md);
}

@media (max-width: 780px) {
  .summary-grid {
    grid-template-columns: 1fr;
  }
}
</style>
