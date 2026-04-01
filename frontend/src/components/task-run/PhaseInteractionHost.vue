<template>
  <div class="space-y-4">
    <DfsBfsStructureWorkbench
      v-if="phase === 'STRUCTURE' && structureQuestion"
      :question="structureQuestion"
      :selected-option-id="structureSelectedId"
      :locked="renderState !== 'prompt'"
      :busy="busy"
      @pick="$emit('pick-structure', $event)"
    />

    <UnderstandingMcqWorkbench
      v-else-if="phase === 'UNDERSTANDING' && understandingQuestion"
      :question="understandingQuestion"
      :selected-option-id="understandingSelectedId"
      :locked="renderState !== 'prompt'"
      :busy="busy"
      @pick="$emit('pick-understanding', $event)"
    />

    <TrainingExpressionWorkbench
      v-else-if="phase === 'TRAINING'"
      :task-title="trainingTaskTitle"
      :task-requirement="trainingTaskRequirement"
      :prompt="trainingPrompt"
      :draft="trainingDraft"
      :busy="busy"
      @update:draft="$emit('update-training-draft', $event)"
      @submit="$emit('submit-training')"
    />

    <div v-else-if="phase === 'REFLECTION'" class="space-y-4">
      <SystemSummaryCard :summary="reflectionSummary" />
      <ReflectionQuestionCard
        v-if="reflectionQuestion"
        :question="reflectionQuestion"
        :selected="reflectionOutput.questionOptionId"
        @select="$emit('select-reflection-question', $event)"
      />
      <ReflectionStrategyCard
        :strategies="reflectionStrategies"
        :selected="reflectionOutput.strategyId"
        @select="$emit('select-reflection-strategy', $event)"
      />
      <div class="flex justify-end">
        <button
          type="button"
          class="rounded-xl bg-primary px-4 py-2 text-sm font-semibold text-white disabled:cursor-not-allowed disabled:opacity-60"
          :disabled="busy || !reflectionOutput.questionOptionId || !reflectionOutput.strategyId"
          @click="$emit('submit-reflection')"
        >
          提交本轮反思
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import DfsBfsStructureWorkbench from '@/components/task-run/DfsBfsStructureWorkbench.vue'
import ReflectionQuestionCard from '@/components/task-run/ReflectionQuestionCard.vue'
import ReflectionStrategyCard from '@/components/task-run/ReflectionStrategyCard.vue'
import SystemSummaryCard from '@/components/task-run/SystemSummaryCard.vue'
import TrainingExpressionWorkbench from '@/components/task-run/TrainingExpressionWorkbench.vue'
import UnderstandingMcqWorkbench from '@/components/task-run/UnderstandingMcqWorkbench.vue'
import type {
  ReflectionQuestion,
  ReflectionStrategy,
  StructureQuestion,
  UnderstandingQuestion,
  WorkbenchReflectionOutput,
  WorkbenchRenderState,
} from '@/types/phaseWorkbench'
import type { WorkbenchPhaseCode } from '@/types/taskExecutionWorkbench'

defineProps<{
  phase: WorkbenchPhaseCode
  renderState: WorkbenchRenderState
  busy: boolean
  structureQuestion: StructureQuestion | null
  structureSelectedId: string | null
  understandingQuestion: UnderstandingQuestion | null
  understandingSelectedId: string | null
  trainingTaskTitle: string
  trainingTaskRequirement: string
  trainingPrompt: string
  trainingDraft: string
  reflectionSummary: string
  reflectionQuestion: ReflectionQuestion | null
  reflectionStrategies: ReflectionStrategy[]
  reflectionOutput: WorkbenchReflectionOutput
}>()

defineEmits<{
  'pick-structure': [optionId: string]
  'pick-understanding': [optionId: string]
  'update-training-draft': [value: string]
  'submit-training': []
  'select-reflection-question': [optionId: string]
  'select-reflection-strategy': [strategyId: string]
  'submit-reflection': []
}>()
</script>
