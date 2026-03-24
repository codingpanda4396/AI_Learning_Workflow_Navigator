<template>
  <section class="space-y-5">
    <div class="rounded-card border border-border bg-white shadow-card">
      <div
        ref="scrollRef"
        class="max-h-[56vh] min-h-[360px] space-y-4 overflow-y-auto px-4 py-5 sm:px-6"
      >
        <AiTutorMessage
          v-for="message in store.messages"
          :key="message.id"
          :role="message.role"
          :content="message.content"
          :type="message.type"
          :source="message.source"
        />
      </div>
    </div>

    <div class="rounded-card border border-border bg-white p-4 shadow-card sm:p-5">
      <label
        class="block text-sm font-semibold text-text-primary"
        for="execution-message-input"
      >
        跟着导师继续想一想
      </label>
      <p class="mt-1 text-xs leading-relaxed text-text-secondary">
        不用追求标准答案。先说出你现在脑中的理解，AI 会立刻反馈，再继续追问。
      </p>
      <textarea
        id="execution-message-input"
        v-model="draft"
        rows="4"
        class="mt-4 w-full rounded-input border border-border px-3 py-3 text-sm text-text-primary placeholder:text-text-secondary/70 focus:border-primary focus:outline-none focus:ring-1 focus:ring-primary"
        :disabled="store.sending"
        :placeholder="step.inputPlaceholder || '比如：我觉得它有点像...'"
        @keydown.enter.exact.prevent="submit"
      />
      <div
        class="mt-4 flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between"
      >
        <PrimaryButton
          :loading="store.sending"
          :disabled="!draft.trim()"
          @click="submit"
        >
          发送给导师
        </PrimaryButton>
        <NextStepButton label="我先进入下一步" @click="$emit('completed')" />
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import { nextTick, ref, watch } from 'vue'
import { getErrorMessage } from '@/api/request'
import AiTutorMessage from '@/components/ai-tutor/AiTutorMessage.vue'
import NextStepButton from '@/components/execution/NextStepButton.vue'
import PrimaryButton from '@/components/ui/PrimaryButton.vue'
import { useAiTutorStore } from '@/stores/aiTutor'
import { showToast } from '@/stores/toast'
import type { ExecutionStep } from '@/types/execution'

defineProps<{
  step: ExecutionStep
}>()

defineEmits<{
  completed: []
}>()

const store = useAiTutorStore()
const draft = ref('')
const scrollRef = ref<HTMLElement | null>(null)

async function scrollToBottom() {
  await nextTick()
  const element = scrollRef.value
  if (element) {
    element.scrollTop = element.scrollHeight
  }
}

watch(
  () => store.messages.length,
  () => {
    void scrollToBottom()
  },
  { immediate: true }
)

async function submit() {
  const text = draft.value.trim()
  if (!text || store.sending) return
  draft.value = ''
  try {
    await store.submitTutorTurn(text)
  } catch (error) {
    draft.value = text
    showToast(getErrorMessage(error))
  }
}
</script>
