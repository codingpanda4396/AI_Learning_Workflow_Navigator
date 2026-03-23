<template>
  <div class="space-y-6">
    <template v-if="phase === 'PROMPT_SHOWN'">
      <PromptCard
        :prompt="step.prompt"
        :why-title="step.promptWhyTitle"
        :why-bullets="step.promptWhyBullets"
      />
      <PrimaryButton
        class="w-full justify-center py-3 sm:w-auto sm:min-w-[200px]"
        @click="goUserConfirmed"
      >
        我去问了
      </PrimaryButton>
    </template>

    <template v-else-if="phase === 'USER_CONFIRMED'">
      <ConfirmBlock :questions="step.reflectionQuestions" />
      <PrimaryButton
        class="w-full justify-center py-3 sm:w-auto sm:min-w-[200px]"
        @click="goThinkingDone"
      >
        我想好了
      </PrimaryButton>
    </template>

    <template v-else-if="phase === 'THINKING_DONE'">
      <UserInputBox v-model="answer" />
      <PrimaryButton
        class="w-full justify-center py-3 sm:w-auto sm:min-w-[200px]"
        :disabled="!answer.trim()"
        @click="submitAnswer"
      >
        提交
      </PrimaryButton>
    </template>

    <template v-else-if="phase === 'USER_SUBMITTED'">
      <FormCard>
        <LoadingState message="正在评估你的作答…" />
      </FormCard>
    </template>

    <template v-else-if="phase === 'FEEDBACK_SHOWN' && feedback">
      <FeedbackCard :feedback="feedback" />
      <NextStepButton label="下一步" @click="goStepCompleted" />
    </template>

    <template v-else-if="phase === 'STEP_COMPLETED'">
      <FormCard>
        <p class="text-lg font-semibold text-text-primary">🎉 {{ step.completionHeadline || '本步完成' }}</p>
        <p class="mt-3 text-xs font-medium text-text-secondary">你已经：</p>
        <ul class="mt-1.5 space-y-1.5 text-sm text-text-primary">
          <li v-for="(line, i) in completionLines" :key="i" class="flex gap-2">
            <span class="text-emerald-600">✔</span>
            <span>{{ line }}</span>
          </li>
        </ul>
        <p v-if="step.completionNextHint" class="mt-4 text-sm text-text-secondary">
          👉 {{ step.completionNextHint }}
        </p>
        <p v-else class="mt-4 text-xs text-text-secondary">
          接下来进入系统里的下一项学习任务。
        </p>
        <NextStepButton label="继续任务" @click="finish" />
      </FormCard>
    </template>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import FormCard from '@/components/ui/FormCard.vue'
import PrimaryButton from '@/components/ui/PrimaryButton.vue'
import LoadingState from '@/components/ui/LoadingState.vue'
import PromptCard from '@/components/execution/PromptCard.vue'
import ConfirmBlock from '@/components/execution/ConfirmBlock.vue'
import UserInputBox from '@/components/execution/UserInputBox.vue'
import FeedbackCard from '@/components/execution/FeedbackCard.vue'
import NextStepButton from '@/components/execution/NextStepButton.vue'
import { postTaskFeedback } from '@/api/execution-feedback'
import { showToast } from '@/stores/toast'
import { getErrorMessage } from '@/api/request'
import type { ExecutionState, ExecutionStep, ExecutionStepFeedback } from '@/types/execution'

const phase = defineModel<ExecutionState>('phase', { required: true })

const props = defineProps<{
  step: ExecutionStep
}>()

const emit = defineEmits<{
  completed: []
}>()

const answer = ref('')
const feedback = ref<ExecutionStepFeedback | null>(null)
const submitting = ref(false)

const completionLines = computed(() => {
  const lines = props.step.completionAchievements
  if (lines?.length) return lines
  return ['完成了本步脚手架练习', '可以进入下一环节继续推进']
})

watch(
  () => props.step.stepId,
  () => {
    answer.value = ''
    feedback.value = null
  }
)

function goUserConfirmed() {
  phase.value = 'USER_CONFIRMED'
}

function goThinkingDone() {
  phase.value = 'THINKING_DONE'
}

async function submitAnswer() {
  const text = answer.value.trim()
  if (!text || submitting.value) return
  submitting.value = true
  phase.value = 'USER_SUBMITTED'
  try {
    const res = await postTaskFeedback(text)
    feedback.value = {
      correct: res.correct,
      comment: res.comment,
      suggestion: res.suggestion,
      praise: res.praise ?? undefined,
      gap: res.gap ?? undefined,
      nextHint: res.nextHint ?? undefined,
    }
    phase.value = 'FEEDBACK_SHOWN'
  } catch (err) {
    showToast(getErrorMessage(err))
    phase.value = 'THINKING_DONE'
  } finally {
    submitting.value = false
  }
}

function goStepCompleted() {
  phase.value = 'STEP_COMPLETED'
}

function finish() {
  emit('completed')
}
</script>
