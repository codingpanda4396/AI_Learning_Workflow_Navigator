<script setup lang="ts">
interface StepItem {
  key: string
  title: string
  state: 'done' | 'current' | 'pending'
}

defineProps<{
  steps: StepItem[]
}>()
</script>

<template>
  <section class="stepper-card">
    <div class="stepper-head">
      <h2>学习闭环</h2>
      <p>你现在所处的位置和下一步操作会随着训练进度自动更新。</p>
    </div>

    <div class="stepper-track">
      <article v-for="(step, index) in steps" :key="step.key" class="step-item" :class="step.state">
        <div class="step-marker">
          <span v-if="step.state === 'done'">✓</span>
          <span v-else>{{ index + 1 }}</span>
        </div>
        <div class="step-copy">
          <strong>{{ step.title }}</strong>
          <span>{{ step.state === 'done' ? '已完成' : step.state === 'current' ? '当前步骤' : '待进行' }}</span>
        </div>
      </article>
    </div>
  </section>
</template>

<style scoped>
.stepper-card {
  display: grid;
  gap: var(--space-lg);
  padding: clamp(18px, 2.6vw, 28px);
  border: 1px solid rgba(61, 80, 104, 0.48);
  border-radius: var(--radius-xl);
  background: rgba(16, 23, 35, 0.9);
}

.stepper-head {
  display: flex;
  justify-content: space-between;
  gap: var(--space-md);
  align-items: baseline;
  flex-wrap: wrap;
}

.stepper-head p {
  color: var(--color-text-secondary);
}

.stepper-track {
  display: grid;
  grid-template-columns: repeat(5, minmax(160px, 1fr));
  gap: var(--space-md);
  overflow-x: auto;
  padding-bottom: 4px;
}

.step-item {
  display: flex;
  gap: var(--space-md);
  padding: var(--space-lg);
  border-radius: var(--radius-lg);
  border: 1px solid rgba(61, 80, 104, 0.36);
  background: rgba(11, 17, 28, 0.78);
  min-width: 160px;
}

.step-marker {
  width: 36px;
  height: 36px;
  flex-shrink: 0;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  font-weight: 700;
  color: var(--color-text);
  background: rgba(94, 113, 143, 0.22);
}

.step-copy {
  display: grid;
  gap: 6px;
}

.step-copy span {
  color: var(--color-text-secondary);
  font-size: var(--font-size-sm);
}

.step-item.done {
  border-color: rgba(93, 212, 166, 0.45);
  background: rgba(19, 44, 38, 0.78);
}

.step-item.done .step-marker {
  background: rgba(93, 212, 166, 0.24);
  color: var(--color-success);
}

.step-item.current {
  border-color: rgba(107, 159, 255, 0.55);
  background: rgba(18, 37, 68, 0.86);
  box-shadow: 0 0 0 1px rgba(107, 159, 255, 0.22);
}

.step-item.current .step-marker {
  background: rgba(107, 159, 255, 0.24);
  color: var(--color-primary-hover);
}

.step-item.pending {
  opacity: 0.72;
}
</style>
