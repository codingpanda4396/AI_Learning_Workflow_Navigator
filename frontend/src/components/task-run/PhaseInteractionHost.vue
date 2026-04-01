<template>
  <div>
    <DfsBfsStructurePhase
      v-if="phase === 'structure'"
      :state="vm.structure"
      @pick="$emit('structure:pick', $event)"
      @next="$emit('structure:next')"
    />

    <DfsBfsUnderstandingPhase
      v-else-if="phase === 'understanding'"
      :state="vm.understanding"
      :scaffold-buttons="scaffoldButtons"
      :busy="vm.busy"
      @send="$emit('understanding:send', $event)"
      @update:draft-input="$emit('understanding:draft', $event)"
      @inject="(prompt, key) => $emit('understanding:inject', prompt, key)"
    />

    <DfsBfsTrainingPhase
      v-else-if="phase === 'training'"
      :state="vm.training"
      :busy="vm.busy"
      @send="$emit('training:send', $event)"
      @update:draft-input="$emit('training:draft', $event)"
    />

    <DfsBfsReflectionPhase
      v-else-if="phase === 'reflection'"
      :state="vm.reflection"
      @toggle-strategy="$emit('reflection:toggle-strategy', $event)"
      @update:reflection-text="$emit('reflection:text', $event)"
    />
  </div>
</template>

<script setup lang="ts">
import DfsBfsStructurePhase from '@/components/task-run/DfsBfsStructurePhase.vue'
import DfsBfsUnderstandingPhase from '@/components/task-run/DfsBfsUnderstandingPhase.vue'
import DfsBfsTrainingPhase from '@/components/task-run/DfsBfsTrainingPhase.vue'
import DfsBfsReflectionPhase from '@/components/task-run/DfsBfsReflectionPhase.vue'
import type { ExecutionWorkbenchVm, PhaseKey, ScaffoldButton } from '@/types/executionWorkbench'

defineProps<{
  phase: PhaseKey
  vm: ExecutionWorkbenchVm
  scaffoldButtons: ScaffoldButton[]
}>()

defineEmits<{
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
