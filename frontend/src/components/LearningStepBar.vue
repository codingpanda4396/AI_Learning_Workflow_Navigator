<script setup lang="ts">
interface StepItem {
  key: string
  order: number
  title: string
  description?: string
  statusLabel?: string
  state: 'completed' | 'current' | 'upcoming'
}

defineProps<{
  steps: StepItem[]
}>()

function marker(step: StepItem) {
  return step.state === 'completed' ? '✓' : String(step.order)
}

function stateLabel(step: StepItem) {
  if (step.state === 'completed') return '已完成'
  if (step.state === 'current') return '当前'
  return '待开始'
}
</script>

<template>
  <section class="step-bar">
    <article v-for="step in steps" :key="step.key" class="step-item" :class="step.state">
      <div class="step-top">
        <span class="step-marker">{{ marker(step) }}</span>
        <span class="step-state">{{ step.statusLabel || stateLabel(step) }}</span>
      </div>
      <strong class="step-title">{{ step.title }}</strong>
      <p v-if="step.description" class="step-description">{{ step.description }}</p>
    </article>
  </section>
</template>

<style scoped>
.step-bar {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}

.step-item {
  display: grid;
  gap: 10px;
  padding: 18px;
  border: 1px solid rgba(61, 80, 104, 0.45);
  border-radius: var(--radius-lg);
  background: rgba(12, 18, 28, 0.82);
}

.step-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 10px;
}

.step-marker {
  width: 34px;
  height: 34px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  font-weight: 700;
  background: rgba(90, 104, 128, 0.22);
  color: var(--color-text);
}

.step-state {
  font-size: var(--font-size-xs);
  color: var(--color-text-muted);
}

.step-title {
  font-size: var(--font-size-md);
  color: var(--color-text);
}

.step-description {
  margin: 0;
  color: var(--color-text-secondary);
  line-height: 1.6;
  font-size: var(--font-size-sm);
}

.step-item.current {
  border-color: rgba(107, 159, 255, 0.45);
  background: rgba(16, 26, 43, 0.92);
}

.step-item.current .step-marker {
  background: rgba(107, 159, 255, 0.22);
  color: var(--color-primary-hover);
}

.step-item.completed {
  border-color: rgba(93, 212, 166, 0.35);
}

.step-item.completed .step-marker {
  background: rgba(93, 212, 166, 0.2);
  color: var(--color-success);
}

.step-item.upcoming {
  opacity: 0.72;
}

@media (max-width: 900px) {
  .step-bar {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 560px) {
  .step-bar {
    grid-template-columns: 1fr;
  }
}
</style>
