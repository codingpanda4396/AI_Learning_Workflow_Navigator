<template>
  <div class="mx-auto max-w-6xl space-y-5">
    <ExecutionTopHeader
      :topic-name="vm.topicName"
      :phase="vm.phase"
      :phase-goal="vm.phaseGoal"
      :phase-progress="vm.phaseProgress"
      @exit="$emit('exit')"
    />

    <PhaseInteractionHost
      :phase="vm.phase"
      :vm="vm"
      :scaffold-buttons="scaffoldButtons"
      @structure:pick="$emit('structure:pick', $event)"
      @structure:next="$emit('structure:next')"
      @understanding:send="$emit('understanding:send', $event)"
      @understanding:draft="$emit('understanding:draft', $event)"
      @understanding:inject="(p, k) => $emit('understanding:inject', p, k)"
      @training:send="$emit('training:send', $event)"
      @training:draft="$emit('training:draft', $event)"
      @reflection:toggle-strategy="$emit('reflection:toggle-strategy', $event)"
      @reflection:text="$emit('reflection:text', $event)"
    />
  </div>
</template>

<script setup lang="ts">
import ExecutionTopHeader from '@/components/task-run/ExecutionTopHeader.vue'
import PhaseInteractionHost from '@/components/task-run/PhaseInteractionHost.vue'
import type { ExecutionWorkbenchVm, ScaffoldButton } from '@/types/executionWorkbench'

defineProps<{
  vm: ExecutionWorkbenchVm
  scaffoldButtons: ScaffoldButton[]
}>()

defineEmits<{
  exit: []
  'structure:pick': [optionId: string]
  'structure:next': []
  'understanding:send': [text: string]
  'understanding:draft': [value: string]
  'understanding:inject': [prompt: string, scaffoldKey: string]
  'training:send': [text: string]
  'training:draft': [value: string]
  'reflection:toggle-strategy': [strategyId: string]
  'reflection:text': [value: string]
}>()
</script>
