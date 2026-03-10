<script setup lang="ts">
import type { WorkflowStepNumber } from '@/types/workflow'

type StepStatus = 'pending' | 'running' | 'done' | 'blocked'

interface WorkflowStepMeta {
  step: WorkflowStepNumber
  title: string
  doneCount?: number
  totalCount?: number
  percent?: number
  status?: StepStatus
}

interface StepProgressProps {
  currentStep: WorkflowStepNumber
  steps: WorkflowStepMeta[]
}

defineProps<StepProgressProps>()

function markerText(step: WorkflowStepMeta) {
  return step.status === 'done' ? '✓' : String(step.step)
}

function hasProgress(step: WorkflowStepMeta) {
  return (
    typeof step.doneCount === 'number' &&
    typeof step.totalCount === 'number' &&
    typeof step.percent === 'number' &&
    typeof step.status === 'string'
  )
}

function percent(step: WorkflowStepMeta) {
  return step.percent ?? 0
}
</script>

<template>
  <div class="progress-container">
    <div
      v-for="item in steps"
      :key="item.step"
      class="step-item"
      :class="{
        'step-active': item.step === currentStep,
        'step-done': item.status === 'done',
        'step-blocked': item.status === 'blocked',
        'step-pending': !item.status || item.status === 'pending'
      }"
    >
      <div class="step-indicator">
        {{ markerText(item) }}
      </div>
      <span class="step-title">{{ item.title }}</span>
      <div v-if="hasProgress(item)" class="step-line" :class="{ completed: item.status === 'done' }"></div>
      <span v-if="hasProgress(item)" class="step-progress-text">{{ percent(item) }}%</span>
    </div>
  </div>
</template>

<style scoped>
.progress-container {
  display: flex;
  align-items: center;
  gap: var(--space-sm);
  padding: var(--space-lg);
  background: linear-gradient(
    145deg,
    var(--color-bg-elevated),
    var(--color-bg-surface)
  );
  border: 1px solid var(--color-border);
  border-radius: var(--radius-xl);
  overflow-x: auto;
}

.step-item {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--space-sm);
  min-width: 80px;
}

.step-indicator {
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  font-family: var(--font-display);
  font-weight: 600;
  font-size: var(--font-size-sm);
  transition: all var(--duration-normal) var(--ease-smooth);
}

.step-pending {
  background: var(--color-bg);
  border: 2px solid var(--color-border);
  color: var(--color-text-muted);
}

.step-active {
  background: linear-gradient(135deg, var(--color-primary), var(--color-primary-hover));
  border: 2px solid var(--color-primary);
  color: #fff;
  box-shadow: 0 4px 16px rgba(107, 159, 255, 0.4);
}

.step-done {
  background: var(--color-success);
  border: 2px solid var(--color-success);
  color: #fff;
}

.step-blocked {
  background: var(--color-warning);
  border: 2px solid var(--color-warning);
  color: #fff;
}

.step-title {
  font-size: var(--font-size-xs);
  color: var(--color-text-secondary);
  text-align: center;
  white-space: nowrap;
}

.step-line {
  flex: 1;
  height: 2px;
  background: var(--color-border);
  min-width: 20px;
}

.step-line.completed {
  background: linear-gradient(90deg, var(--color-success), var(--color-primary));
}

.step-progress-text {
  font-size: var(--font-size-xs);
  color: var(--color-text-muted);
}

@media (max-width: 900px) {
  .progress-container {
    flex-wrap: wrap;
  }

  .step-item {
    flex: 0 0 calc(50% - var(--space-sm));
  }
}
</style>
