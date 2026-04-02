<template>
  <div class="mx-auto max-w-2xl space-y-6">
    <div class="rounded-2xl border border-slate-200/80 bg-white p-6 shadow-sm">
      <p class="mb-1 text-xs font-medium uppercase tracking-wide text-slate-400">
        第 {{ currentIndex + 1 }} / {{ totalQuestions }} 题
      </p>

      <McqQuestionCard
        :question="currentQuestion"
        :selected-id="state.selectedOptionId"
        :locked="state.isLocked"
        @pick="handlePick"
      />

      <GuidedFeedbackCard
        v-if="currentFeedback"
        :feedback="currentFeedback"
        class="mt-4"
      />

      <div v-if="state.isLocked && currentFeedback" class="mt-5 flex justify-end">
        <button
          v-if="!isLastQuestion"
          type="button"
          class="rounded-xl bg-primary px-5 py-2.5 text-sm font-semibold text-white shadow-sm transition hover:bg-primary/90 disabled:cursor-not-allowed disabled:opacity-60"
          :disabled="busy"
          @click="goNextQuestion"
        >
          {{ busy ? '同步中…' : '下一题' }}
        </button>
        <p v-else class="text-sm font-medium text-emerald-600">
          三题已完成，可以进入下一阶段
        </p>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import McqQuestionCard from '@/components/task-run/McqQuestionCard.vue'
import GuidedFeedbackCard from '@/components/task-run/GuidedFeedbackCard.vue'
import type { StructurePhaseState } from '@/types/executionWorkbench'

const props = defineProps<{
  state: StructurePhaseState
  busy?: boolean
}>()

const emit = defineEmits<{
  pick: [optionId: string]
  next: []
}>()

const currentIndex = computed(() => props.state.currentQuestionIndex)
const totalQuestions = computed(() => props.state.questions.length)

const currentQuestion = computed(() => props.state.questions[currentIndex.value]!)

const currentFeedback = computed(() => {
  if (!props.state.selectedOptionId || !props.state.isLocked) return null
  const q = currentQuestion.value
  return q.feedbackByOption[props.state.selectedOptionId] ?? null
})

const isLastQuestion = computed(() => currentIndex.value >= totalQuestions.value - 1)

function handlePick(optionId: string) {
  emit('pick', optionId)
}

function goNextQuestion() {
  if (props.busy) return
  emit('next')
}
</script>
