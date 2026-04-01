<template>
  <div class="space-y-4">
    <DfsBfsStructureWorkbench
      v-if="phase === 'STRUCTURE'"
      :model-value="structureUi"
      :disabled="busy"
      :error-message="structureError"
      :highlight="structureHighlight"
      @update:model-value="$emit('update-structure-ui', $event)"
    />

    <UnderstandingMcqWorkbench
      v-else-if="phase === 'UNDERSTANDING' && understandingQuestion"
      :question="understandingQuestion"
      :selected-option-id="understandingSelectedId"
      :locked="renderState !== 'THINK'"
      :busy="busy"
      @pick="$emit('pick-understanding', $event)"
    />

    <TrainingExpressionWorkbench
      v-else-if="phase === 'TRAINING'"
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
          提交反思
        </button>
      </div>
    </div>

    <div v-if="phase === 'STRUCTURE'" class="flex justify-end">
      <button
        type="button"
        class="rounded-xl bg-primary px-4 py-2 text-sm font-semibold text-white disabled:cursor-not-allowed disabled:opacity-60"
        :disabled="busy || renderState !== 'THINK'"
        @click="$emit('submit-structure', { ui: structureUi })"
      >
        提交结构判断
      </button>
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
  UnderstandingQuestion,
  WorkbenchReflectionOutput,
  WorkbenchRenderState,
  WorkbenchStructureSubmitPayload,
} from '@/types/phaseWorkbench'
import type { DfsBfsStructureWorkbenchUi, DfsBfsWorkbenchHighlight } from '@/constants/dfsBfsStructureSkeleton'
import type { WorkbenchPhaseCode } from '@/types/taskExecutionWorkbench'

defineProps<{
  phase: WorkbenchPhaseCode
  renderState: WorkbenchRenderState
  busy: boolean
  structureUi: DfsBfsStructureWorkbenchUi
  structureError: string
  structureHighlight: DfsBfsWorkbenchHighlight
  understandingQuestion: UnderstandingQuestion | null
  understandingSelectedId: string | null
  trainingPrompt: string
  trainingDraft: string
  reflectionSummary: string
  reflectionQuestion: ReflectionQuestion | null
  reflectionStrategies: ReflectionStrategy[]
  reflectionOutput: WorkbenchReflectionOutput
}>()

defineEmits<{
  'update-structure-ui': [value: DfsBfsStructureWorkbenchUi]
  'submit-structure': [payload: WorkbenchStructureSubmitPayload]
  'pick-understanding': [optionId: string]
  'update-training-draft': [value: string]
  'submit-training': []
  'select-reflection-question': [optionId: string]
  'select-reflection-strategy': [strategyId: string]
  'submit-reflection': []
}>()
</script>
