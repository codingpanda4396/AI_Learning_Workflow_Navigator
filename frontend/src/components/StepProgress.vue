<script setup lang="ts">
import type { WorkflowStepNumber } from '@/types/workflow'

type StepStatus = 'pending' | 'running' | 'done' | 'blocked'
type LearningFlowState = 'completed' | 'current' | 'upcoming'

interface WorkflowStepMeta {
  step: WorkflowStepNumber
  title: string
  description?: string
  statusLabel?: string
  actionHint?: string
  doneCount?: number
  totalCount?: number
  percent?: number
  status?: StepStatus
  state?: LearningFlowState
}

interface StepProgressProps {
  currentStep: WorkflowStepNumber
  steps: WorkflowStepMeta[]
}

const props = defineProps<StepProgressProps>()

function resolveState(step: WorkflowStepMeta): LearningFlowState {
  if (step.state) {
    return step.state
  }
  if (step.status === 'done') {
    return 'completed'
  }
  if (step.step === props.currentStep || step.status === 'running' || step.status === 'blocked') {
    return 'current'
  }
  return 'upcoming'
}

function markerText(step: WorkflowStepMeta) {
  return resolveState(step) === 'completed' ? '✓' : String(step.step)
}

function percent(step: WorkflowStepMeta) {
  return step.percent ?? 0
}

function showPercent(step: WorkflowStepMeta) {
  return typeof step.percent === 'number'
}
</script>

<template>
  <div class="progress-container">
    <div
      v-for="item in steps"
      :key="item.step"
      class="step-item"
      :class="`step-${resolveState(item)}`"
    >
      <div class="step-rail" />
      <div class="step-indicator">
        {{ markerText(item) }}
      </div>
      <div class="step-copy">
        <div class="step-head">
          <span class="step-title">{{ item.title }}</span>
          <span v-if="item.statusLabel" class="step-status">{{ item.statusLabel }}</span>
        </div>
        <span v-if="item.description" class="step-description">{{ item.description }}</span>
        <span v-else-if="showPercent(item)" class="step-description">{{ percent(item) }}%</span>
        <span v-if="item.actionHint" class="step-action">{{ item.actionHint }}</span>
      </div>
    </div>
  </div>
</template>

<style scoped>
.progress-container {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: clamp(10px, 1.8vw, 18px);
  padding: clamp(16px, 2.6vw, 24px);
  background: linear-gradient(145deg, rgba(21, 29, 43, 0.94), rgba(28, 38, 54, 0.9));
  border: 1px solid var(--color-border);
  border-radius: var(--radius-xl);
  box-shadow: var(--shadow-md);
}

.step-item {
  position: relative;
  display: grid;
  gap: var(--space-md);
  padding: var(--space-md);
  border-radius: var(--radius-lg);
  border: 1px solid transparent;
  background: rgba(13, 17, 23, 0.55);
  min-height: 172px;
  transition: all var(--duration-normal) var(--ease-smooth);
}

.step-item:hover {
  transform: translateY(-2px);
}

.step-rail {
  height: 3px;
  border-radius: var(--radius-full);
  background: rgba(61, 80, 104, 0.5);
}

.step-indicator {
  width: 42px;
  height: 42px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 14px;
  font-family: var(--font-display);
  font-size: var(--font-size-md);
  font-weight: 700;
}

.step-copy {
  display: grid;
  gap: 8px;
}

.step-head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 8px;
}

.step-title {
  font-size: var(--font-size-md);
  font-weight: 700;
  color: var(--color-text);
}

.step-status {
  padding: 4px 10px;
  border-radius: var(--radius-full);
  background: rgba(255, 255, 255, 0.08);
  color: var(--color-text-secondary);
  font-size: var(--font-size-xs);
  white-space: nowrap;
}

.step-description,
.step-action {
  font-size: var(--font-size-sm);
  line-height: 1.5;
  color: var(--color-text-secondary);
}

.step-action {
  color: var(--color-text);
}

.step-completed {
  border-color: rgba(93, 212, 166, 0.38);
  background: linear-gradient(160deg, rgba(22, 50, 46, 0.8), rgba(15, 27, 27, 0.9));
}

.step-completed .step-rail,
.step-completed .step-indicator {
  background: linear-gradient(135deg, rgba(93, 212, 166, 0.9), rgba(65, 182, 137, 1));
  color: #08110e;
}

.step-current {
  border-color: rgba(107, 159, 255, 0.48);
  background: linear-gradient(160deg, rgba(20, 34, 59, 0.92), rgba(12, 20, 35, 0.92));
  box-shadow: 0 0 0 1px rgba(107, 159, 255, 0.2), 0 12px 32px rgba(16, 27, 49, 0.45);
}

.step-current .step-rail,
.step-current .step-indicator {
  background: linear-gradient(135deg, var(--color-primary), var(--color-primary-hover));
  color: #ffffff;
}

.step-upcoming {
  border-color: rgba(42, 58, 82, 0.9);
}

.step-upcoming .step-indicator {
  border: 1px solid rgba(61, 80, 104, 0.9);
  background: rgba(20, 28, 40, 0.9);
  color: var(--color-text-secondary);
}

@media (max-width: 900px) {
  .progress-container {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 560px) {
  .progress-container {
    grid-template-columns: 1fr;
  }

  .step-item {
    min-height: auto;
  }

  .step-head {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
