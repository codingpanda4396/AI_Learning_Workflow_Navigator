<template>
  <div class="space-y-6">
    <template v-if="phase === ExecutionPhaseR0003.AI_PROMPT">
      <PromptCard
        :prompt="displayPrompt"
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

    <template v-else-if="phase === ExecutionPhaseR0003.AI_USER_CONFIRMED">
      <ConfirmBlock :questions="step.reflectionQuestions" />
      <PrimaryButton
        class="w-full justify-center py-3 sm:w-auto sm:min-w-[200px]"
        @click="goAiResponseShown"
      >
        下一步，看导师怎么说
      </PrimaryButton>
    </template>

    <template v-else-if="phase === ExecutionPhaseR0003.AI_EXPLAIN">
      <AIResponseCard :explanation="aiExplanation" />
      <PrimaryButton
        class="w-full justify-center py-3 sm:w-auto sm:min-w-[200px]"
        @click="goThinkingDone"
      >
        想好了，写下来
      </PrimaryButton>
    </template>

    <template v-else-if="phase === ExecutionPhaseR0003.WAIT_INPUT">
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

    <template v-else-if="phase === ExecutionPhaseR0003.AI_FEEDBACK_PENDING">
      <FormCard>
        <LoadingState message="AI正在思考…" />
      </FormCard>
    </template>

    <template v-else-if="phase === ExecutionPhaseR0003.AI_FEEDBACK && feedback">
      <FeedbackCard :feedback="feedback" />
      <NextStepButton label="好，进入本步总结" @click="goStepCompleted" />
    </template>

    <template v-else-if="phase === ExecutionPhaseR0003.STEP_COMPLETED">
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
import {
  getTutorExplain,
  getTutorPrompt,
  postTutorPrefetch,
} from '@/api/tutor'
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
import {
  ExecutionPhaseR0003,
  type ExecutionState,
  type ExecutionStep,
  type ExecutionStepFeedback,
} from '@/types/execution'

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
const aiGuidePrompt = ref<string | null>(null)
const aiExplanation = ref<string | null>(null)

const completionLines = computed(() => {
  const lines = props.step.completionAchievements
  if (lines?.length) return lines
  return [
    '按提示真的去问过一次了',
    '能用自己的话碰一碰这一步的核心',
    '走完了「问—想—写」这一小段',
  ]
})

function tutorKnowledgeLabel(): string {
  const k = props.step.knowledgePoint?.trim()
  if (k) return k
  if (props.step.goal?.trim()) return props.step.goal.trim()
  return props.step.title?.trim() || '当前主题'
}

const displayPrompt = computed(
  () => (aiGuidePrompt.value?.trim() ? aiGuidePrompt.value.trim() : props.step.prompt)
)

watch(
  () => props.step.stepId,
  () => {
    answer.value = ''
    feedback.value = null
    aiGuidePrompt.value = null
    aiExplanation.value = null
    const kp = tutorKnowledgeLabel()
    postTutorPrefetch(props.step.stepId, kp)
  },
  { immediate: true }
)

watch(
  () => [phase.value, props.step.stepId] as const,
  async ([ph]) => {
    const stepId = props.step.stepId
    const kp = tutorKnowledgeLabel()
    if (ph === ExecutionPhaseR0003.AI_PROMPT) {
      try {
        const env = await getTutorPrompt(stepId, kp)
        aiGuidePrompt.value = env.content
      } catch {
        aiGuidePrompt.value = null
      }
    }
    if (ph === ExecutionPhaseR0003.AI_EXPLAIN) {
      aiExplanation.value = null
      try {
        const env = await getTutorExplain(stepId, kp)
        aiExplanation.value = env.content
      } catch {
        aiExplanation.value = null
      }
    }
  },
  { immediate: true }
)

function goUserConfirmed() {
  phase.value = ExecutionPhaseR0003.AI_USER_CONFIRMED
}

function goAiResponseShown() {
  phase.value = ExecutionPhaseR0003.AI_EXPLAIN
}

function goThinkingDone() {
  phase.value = ExecutionPhaseR0003.WAIT_INPUT
}

async function submitAnswer() {
  const text = answer.value.trim()
  if (!text || submitting.value) return
  submitting.value = true
  phase.value = ExecutionPhaseR0003.AI_FEEDBACK_PENDING
  try {
    const res = await postTaskFeedback({
      answer: text,
      step: props.step.stepId,
      knowledgePoint: props.step.knowledgePoint,
    })
    feedback.value = {
      correct: res.correct,
      comment: res.comment,
      suggestion: res.suggestion,
      praise: res.praise ?? undefined,
      gap: res.gap ?? undefined,
      nextHint: res.nextHint ?? undefined,
    }
    phase.value = ExecutionPhaseR0003.AI_FEEDBACK
  } catch (err) {
    showToast(getErrorMessage(err))
    phase.value = ExecutionPhaseR0003.WAIT_INPUT
  } finally {
    submitting.value = false
  }
}

function goStepCompleted() {
  phase.value = ExecutionPhaseR0003.STEP_COMPLETED
}

function finish() {
  emit('completed')
}
</script>
