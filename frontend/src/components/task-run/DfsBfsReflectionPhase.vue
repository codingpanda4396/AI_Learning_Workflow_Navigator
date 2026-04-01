<template>
  <div class="mx-auto max-w-2xl space-y-5">
    <LearningSummaryCard :summary="state.summary" />

    <ConfusionReviewCard :points="confusionPoints" />

    <NextTimeRuleCard
      :strategies="state.availableStrategies"
      :selected-ids="state.selectedStrategyIds"
      :reflection-text="state.userReflectionText"
      :placeholder="inputPlaceholder"
      @toggle-strategy="handleToggle"
      @update:reflection-text="handleReflectionUpdate"
    />
  </div>
</template>

<script setup lang="ts">
import LearningSummaryCard from '@/components/task-run/LearningSummaryCard.vue'
import ConfusionReviewCard from '@/components/task-run/ConfusionReviewCard.vue'
import NextTimeRuleCard from '@/components/task-run/NextTimeRuleCard.vue'
import type { ReflectionPhaseState } from '@/types/executionWorkbench'
import {
  DFS_BFS_CONFUSION_POINTS,
  REFLECTION_INPUT_PLACEHOLDER,
} from '@/constants/dfsBfsExecutionConfig'

const props = defineProps<{
  state: ReflectionPhaseState
}>()

const emit = defineEmits<{
  'toggle-strategy': [strategyId: string]
  'update:reflectionText': [value: string]
}>()

const confusionPoints = props.state.confusionPoints.length
  ? props.state.confusionPoints
  : DFS_BFS_CONFUSION_POINTS

const inputPlaceholder = REFLECTION_INPUT_PLACEHOLDER

function handleToggle(strategyId: string) {
  emit('toggle-strategy', strategyId)
}

function handleReflectionUpdate(value: string) {
  emit('update:reflectionText', value)
}
</script>
