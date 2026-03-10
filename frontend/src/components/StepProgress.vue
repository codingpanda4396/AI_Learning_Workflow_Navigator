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

function doneCount(step: WorkflowStepMeta) {
  return step.doneCount ?? 0
}

function totalCount(step: WorkflowStepMeta) {
  return step.totalCount ?? 0
}

function percent(step: WorkflowStepMeta) {
  return step.percent ?? 0
}

function statusText(step: WorkflowStepMeta) {
  switch (step.status) {
    case 'done':
      return '已完成'
    case 'running':
      return '进行中'
    case 'blocked':
      return '受阻'
    default:
      return '未开始'
  }
}
</script>

<template>
  <ol class="step-progress" aria-label="学习流程步骤进度">
    <li
      v-for="item in steps"
      :key="item.step"
      class="step-item"
      :class="{
        active: item.step === currentStep,
        completed: item.status === 'done',
        blocked: item.status === 'blocked',
      }"
    >
      <div class="head">
        <span class="marker">{{ markerText(item) }}</span>
        <span class="text">{{ item.title }}</span>
        <span v-if="hasProgress(item)" class="ratio">{{ doneCount(item) }}/{{ totalCount(item) }}</span>
      </div>
      <div v-if="hasProgress(item)" class="track">
        <span class="fill" :style="{ width: `${percent(item)}%` }"></span>
      </div>
      <span v-if="hasProgress(item)" class="percent">{{ percent(item) }}% · {{ statusText(item) }}</span>
    </li>
  </ol>
</template>

<style scoped>
.step-progress {
  list-style: none;
  margin: 0;
  padding: 0;
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: var(--space-sm);
}

.step-item {
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  background: var(--color-bg-surface);
  min-height: 86px;
  padding: var(--space-sm) var(--space-md);
  display: grid;
  gap: 6px;
  color: var(--color-text-secondary);
}

.head {
  display: grid;
  grid-template-columns: auto 1fr auto;
  align-items: center;
  gap: var(--space-sm);
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

.ratio {
  font-size: var(--font-size-xs);
}

.track {
  height: 6px;
  border-radius: 999px;
  background: rgba(145, 164, 209, 0.25);
  overflow: hidden;
}

.fill {
  display: block;
  height: 100%;
  border-radius: 999px;
  background: var(--color-primary);
  transition: width 180ms ease;
}

.percent {
  font-size: var(--font-size-xs);
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

.step-item.completed .fill {
  background: var(--color-success);
}

.step-item.blocked {
  border-color: rgba(255, 176, 79, 0.55);
}

.step-item.blocked .fill {
  background: var(--color-warning);
}

@media (max-width: 900px) {
  .step-progress {
    grid-template-columns: 1fr 1fr;
  }
}
</style>
