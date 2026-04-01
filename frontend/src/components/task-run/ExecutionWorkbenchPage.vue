<template>
  <div class="mx-auto max-w-6xl space-y-5">
    <StageWorkbenchHeader
      :topic-name="vm.topicName"
      :cognitive-action="vm.cognitiveAction"
      :stage-goal="vm.stageGoal"
      :emphasis-phase="vm.phase"
      :phase-progress="vm.phaseProgress"
      :task-index-label="vm.phaseProgress.taskIndexLabel || ''"
    />

    <div class="grid gap-4 lg:grid-cols-[minmax(0,1fr)_320px]">
      <section class="space-y-4">
        <StageIntroCard :text="vm.intro" />
        <ExplanationBlockList :blocks="vm.explanations" />
        <PhaseInteractionHost
          :phase="vm.phase"
          :render-state="vm.renderState"
          :busy="vm.busy"
          :structure-ui="vm.structureUi"
          :structure-error="structureError"
          :structure-highlight="structureHighlight"
          :understanding-question="vm.understandingQuestion"
          :understanding-selected-id="vm.understandingSelectedId"
          :training-prompt="vm.trainingPrompt"
          :training-draft="vm.trainingDraft"
          :reflection-summary="vm.reflectionSummary"
          :reflection-question="vm.reflectionQuestion"
          :reflection-strategies="vm.reflectionStrategies"
          :reflection-output="vm.reflectionOutput"
          @update-structure-ui="$emit('update-structure-ui', $event)"
          @submit-structure="$emit('submit-structure', $event)"
          @pick-understanding="$emit('pick-understanding', $event)"
          @update-training-draft="$emit('update-training-draft', $event)"
          @submit-training="$emit('submit-training')"
          @select-reflection-question="$emit('select-reflection-question', $event)"
          @select-reflection-strategy="$emit('select-reflection-strategy', $event)"
          @submit-reflection="$emit('submit-reflection')"
        />
        <PhaseFeedbackCard :feedback="vm.feedback" />
      </section>

      <div class="lg:sticky lg:top-4 lg:self-start">
        <PromptScaffoldPanel
          :phase="vm.phase"
          :actions="vm.scaffoldActions"
          :busy="vm.busy"
          @append-explanation="$emit('append-explanation', $event)"
        />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import ExplanationBlockList from '@/components/task-run/ExplanationBlockList.vue'
import PhaseFeedbackCard from '@/components/task-run/PhaseFeedbackCard.vue'
import PhaseInteractionHost from '@/components/task-run/PhaseInteractionHost.vue'
import PromptScaffoldPanel from '@/components/task-run/PromptScaffoldPanel.vue'
import StageIntroCard from '@/components/task-run/StageIntroCard.vue'
import StageWorkbenchHeader from '@/components/task-run/StageWorkbenchHeader.vue'
import type { DfsBfsStructureWorkbenchUi, DfsBfsWorkbenchHighlight } from '@/constants/dfsBfsStructureSkeleton'
import type { ExecutionWorkbenchViewModel, WorkbenchStructureSubmitPayload } from '@/types/phaseWorkbench'

defineProps<{
  vm: ExecutionWorkbenchViewModel
  structureError: string
  structureHighlight: DfsBfsWorkbenchHighlight
}>()

defineEmits<{
  'append-explanation': [actionId: string]
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
