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
        <StageIntroCard :title="vm.intro.title" :subtitle="vm.intro.subtitle" />
        <ExplanationBlockList :blocks="vm.explanations" />
        <PhaseInteractionHost
          :phase="vm.phase"
          :render-state="vm.renderState"
          :busy="vm.busy"
          :structure-question="vm.structureQuestion"
          :structure-selected-id="vm.structureSelectedId"
          :understanding-question="vm.understandingQuestion"
          :understanding-selected-id="vm.understandingSelectedId"
          :training-task-title="vm.trainingTaskTitle"
          :training-task-requirement="vm.trainingTaskRequirement"
          :training-prompt="vm.trainingPrompt"
          :training-draft="vm.trainingDraft"
          :reflection-summary="vm.reflectionSummary"
          :reflection-question="vm.reflectionQuestion"
          :reflection-strategies="vm.reflectionStrategies"
          :reflection-output="vm.reflectionOutput"
          @pick-structure="$emit('pick-structure', $event)"
          @pick-understanding="$emit('pick-understanding', $event)"
          @update-training-draft="$emit('update-training-draft', $event)"
          @submit-training="$emit('submit-training')"
          @select-reflection-question="$emit('select-reflection-question', $event)"
          @select-reflection-strategy="$emit('select-reflection-strategy', $event)"
          @submit-reflection="$emit('submit-reflection')"
        />
        <PhaseFeedbackCard v-if="vm.renderState === 'feedback'" :feedback="vm.feedback" />
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
import type { ExecutionWorkbenchViewModel } from '@/types/phaseWorkbench'

defineProps<{
  vm: ExecutionWorkbenchViewModel
}>()

defineEmits<{
  'append-explanation': [actionId: string]
  'pick-structure': [optionId: string]
  'pick-understanding': [optionId: string]
  'update-training-draft': [value: string]
  'submit-training': []
  'select-reflection-question': [optionId: string]
  'select-reflection-strategy': [strategyId: string]
  'submit-reflection': []
}>()
</script>
