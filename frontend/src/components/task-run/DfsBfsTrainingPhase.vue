<template>
  <div class="mx-auto max-w-2xl space-y-5">
    <TrainingTaskCard
      :title="taskTitle"
      :subtitle="taskSubtitle"
      :requirements="taskRequirements"
    />

    <ExpressionDialoguePanel
      :messages="state.messages"
      :draft-input="state.draftInput"
      :round-count="state.roundCount"
      :max-rounds="maxRounds"
      :busy="busy"
      :is-complete="!!state.finalDraft"
      @send="handleSend"
      @update:draft-input="handleDraftUpdate"
    />

    <TrainingSummaryDraftCard :final-draft="state.finalDraft" />
  </div>
</template>

<script setup lang="ts">
import TrainingTaskCard from '@/components/task-run/TrainingTaskCard.vue'
import ExpressionDialoguePanel from '@/components/task-run/ExpressionDialoguePanel.vue'
import TrainingSummaryDraftCard from '@/components/task-run/TrainingSummaryDraftCard.vue'
import type { TrainingPhaseState } from '@/types/executionWorkbench'
import {
  TRAINING_TASK_TITLE,
  TRAINING_TASK_SUBTITLE,
  TRAINING_REQUIREMENTS,
  TRAINING_MAX_ROUNDS,
} from '@/constants/dfsBfsExecutionConfig'

defineProps<{
  state: TrainingPhaseState
  busy: boolean
}>()

const emit = defineEmits<{
  send: [text: string]
  'update:draftInput': [value: string]
}>()

const taskTitle = TRAINING_TASK_TITLE
const taskSubtitle = TRAINING_TASK_SUBTITLE
const taskRequirements = TRAINING_REQUIREMENTS
const maxRounds = TRAINING_MAX_ROUNDS

function handleSend(text: string) {
  emit('send', text)
}

function handleDraftUpdate(value: string) {
  emit('update:draftInput', value)
}
</script>
