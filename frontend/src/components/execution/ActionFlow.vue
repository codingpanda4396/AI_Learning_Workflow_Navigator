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
        问过了，继续
      </PrimaryButton>
    </template>

    <template v-else-if="phase === 'USER_CONFIRMED'">
      <ConfirmBlock :questions="step.reflectionQuestions" />
      <PrimaryButton
        class="w-full justify-center py-3 sm:w-auto sm:min-w-[200px]"
        @click="goAiResponseShown"
      >
        下一步，看导师怎么说
      </PrimaryButton>
    </template>

    <template v-else-if="phase === 'AI_RESPONSE_SHOWN'">
      <AIResponseCard />
      <PrimaryButton
        class="w-full justify-center py-3 sm:w-auto sm:min-w-[200px]"
        @click="goThinkingDone"
      >
        想好了，写下来
      </PrimaryButton>
    </template>

    <template v-else-if="phase === 'THINKING_DONE'">
      <UserInputBox
        v-model="answer"
        :hint="step.inputHint"
        :placeholder="step.inputPlaceholder"
      />
      <PrimaryButton
        class="w-full justify-center py-3 sm:w-auto sm:min-w-[200px]"
        :disabled="!answer.trim()"
        @click="submitAnswer"
      >
        写好了，给导师看看
      </PrimaryButton>
    </template>

    <template v-else-if="phase === 'USER_SUBMITTED'">
      <FormCard>
        <LoadingState message="我帮你看一下你写的内容…" />
      </FormCard>
    </template>

    <template v-else-if="phase === 'FEEDBACK_SHOWN' && feedback">
      <FeedbackCard :feedback="feedback" />
      <NextStepButton label="好，进入本步总结" @click="goStepCompleted" />
    </template>

    <template v-else-if="phase === 'STEP_COMPLETED'">
      <FormCard>
        <p class="text-xs font-medium text-text-secondary">👨‍🏫 AI导师</p>
        <p class="mt-2 text-sm leading-relaxed text-text-primary">
          很好，这一步你已经完成了。
        </p>
        <p
          v-if="step.completionHeadline"
          class="mt-2 text-base font-semibold text-text-primary"
        >
          {{ step.completionHeadline }}
        </p>
        <p class="mt-3 text-xs font-medium text-text-secondary">你现在已经：</p>
        <ul class="mt-1.5 space-y-1.5 text-sm text-text-primary">
          <li v-for="(line, i) in completionLines" :key="i" class="flex gap-2">
            <span class="text-emerald-600">✔</span>
            <span>{{ line }}</span>
          </li>
        </ul>
        <p class="mt-4 text-sm text-text-primary">👉 我们进入下一步。</p>
        <p v-if="step.completionNextHint" class="mt-2 text-sm text-text-secondary">
          {{ step.completionNextHint }}
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
import AIResponseCard from '@/components/execution/AIResponseCard.vue'
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
  return [
    '按提示真的去问过一次了',
    '能用自己的话碰一碰这一步的核心',
    '走完了「问—想—写」这一小段',
  ]
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

function goAiResponseShown() {
  phase.value = 'AI_RESPONSE_SHOWN'
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
