<script setup lang="ts">
import type { WorkflowStepNumber } from '@/types/workflow'

interface WorkflowStepMeta {
  step: WorkflowStepNumber
  title: string
}

interface StepProgressProps {
  currentStep: WorkflowStepNumber
  steps: WorkflowStepMeta[]
}

defineProps<StepProgressProps>()
</script>

<template>
  <ol class="step-progress" aria-label="学习流程步骤">
    <li
      v-for="item in steps"
      :key="item.step"
      class="step-item"
      :class="{
        active: item.step === currentStep,
        completed: item.step < currentStep,
      }"
    >
      <span class="marker">{{ item.step }}</span>
      <span class="text">{{ item.title }}</span>
    </li>
  </ol>
</template>

<style scoped>
.step-progress {
  list-style: none;
  margin: 0;
  padding: 0;
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: var(--space-sm);
}

.step-item {
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  background: var(--color-bg-surface);
  min-height: 56px;
  padding: var(--space-sm) var(--space-md);
  display: flex;
  align-items: center;
  gap: var(--space-sm);
  color: var(--color-text-secondary);
}

.marker {
  width: 24px;
  height: 24px;
  border-radius: 999px;
  border: 1px solid currentColor;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: var(--font-size-xs);
  font-weight: 700;
}

.text {
  font-size: var(--font-size-sm);
  font-weight: 500;
}

.step-item.active {
  color: var(--color-text);
  border-color: var(--color-primary);
  box-shadow: inset 0 0 0 1px var(--color-primary);
}

.step-item.completed {
  color: #cde2ff;
  border-color: rgba(62, 140, 255, 0.4);
}

@media (max-width: 900px) {
  .step-progress {
    grid-template-columns: 1fr 1fr;
  }
}
</style>
