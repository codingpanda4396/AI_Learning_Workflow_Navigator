<template>
  <div
    class="overflow-hidden rounded-xl border border-border bg-white shadow-card transition duration-200 ease-out hover:shadow-card-hover"
  >
    <div class="border-b border-border bg-slate-50/80 px-5 py-3">
      <p class="text-xs font-semibold uppercase tracking-wide text-text-secondary">
        表达反馈 · 第 {{ roundCount }} / {{ maxRounds }} 轮（从属于上方主任务）
      </p>
    </div>

    <div
      ref="messagesRef"
      class="max-h-[min(320px,42vh)] space-y-4 overflow-y-auto px-5 py-4"
      @scroll="handleScroll"
    >
      <div
        v-for="(msg, index) in messages"
        :key="msg.id"
        class="flex"
        :class="msg.role === 'user' ? 'justify-end' : 'justify-start'"
      >
        <div
          class="max-w-[85%] rounded-2xl px-4 py-3 text-sm leading-relaxed"
          :class="
            msg.role === 'user'
              ? 'rounded-br-md bg-primary-muted text-text-primary'
              : msg.role === 'system'
                ? 'rounded-bl-md bg-slate-50 text-slate-500'
                : 'rounded-bl-md bg-slate-100 text-slate-800'
          "
        >
          <p v-if="msg.content" class="whitespace-pre-wrap">{{ msg.content }}</p>
          <div
            v-if="isStreamingAssistantMessage(msg, index)"
            class="mt-2 inline-flex items-center gap-2 text-xs text-slate-500"
          >
            <span>正在生成</span>
            <span class="inline-flex items-center gap-1">
              <span class="h-1.5 w-1.5 animate-pulse rounded-full bg-slate-400" />
              <span
                class="h-1.5 w-1.5 animate-pulse rounded-full bg-slate-400 [animation-delay:120ms]"
              />
              <span
                class="h-1.5 w-1.5 animate-pulse rounded-full bg-slate-400 [animation-delay:240ms]"
              />
            </span>
          </div>
        </div>
      </div>
    </div>

    <div v-if="!isComplete" class="border-t border-border px-4 py-3">
      <p v-if="error" class="mb-2 text-xs text-rose-500">{{ error }}</p>
      <p v-else-if="busy" class="mb-2 text-xs text-slate-500">正在生成当前回复…</p>
      <p v-else-if="completionHint" class="mb-2 text-xs text-slate-500">{{ completionHint }}</p>
      <div class="flex items-end gap-2 rounded-md border-2 border-border bg-slate-50/50 px-3 py-2">
        <textarea
          v-model="localDraft"
          placeholder="用你自己的话说说 DFS 和 BFS..."
          rows="3"
          class="flex-1 resize-none bg-transparent text-sm text-slate-800 outline-none placeholder:text-slate-400"
          :disabled="busy"
          @keydown.enter.exact.prevent="handleSend"
        />
        <button
          type="button"
          class="shrink-0 rounded-md bg-accent px-3 py-1.5 text-xs font-semibold text-white transition hover:bg-accent-hover disabled:opacity-50"
          :disabled="busy || !localDraft.trim()"
          @click="handleSend"
        >
          提交
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useStreamingMessageViewport } from '@/composables/useStreamingMessageViewport'
import type { ChatMessage } from '@/types/executionWorkbench'

const props = defineProps<{
  messages: ChatMessage[]
  draftInput: string
  roundCount: number
  maxRounds: number
  busy: boolean
  isComplete: boolean
  error?: string | null
  completionHint?: string | null
}>()

const emit = defineEmits<{
  send: [text: string]
  'update:draftInput': [value: string]
}>()

const localDraft = ref(props.draftInput)
const { messagesRef, handleScroll } = useStreamingMessageViewport(
  () => props.messages,
  () => props.busy,
)
void messagesRef
const streamingMessageId = computed(() => {
  if (!props.busy) return null
  const lastMessage = props.messages[props.messages.length - 1]
  return lastMessage?.role === 'assistant' ? lastMessage.id : null
})

watch(
  () => props.draftInput,
  (value) => {
    if (value !== localDraft.value) localDraft.value = value
  },
)

watch(localDraft, (value) => {
  emit('update:draftInput', value)
})

function handleSend() {
  const text = localDraft.value.trim()
  if (!text || props.busy) return
  emit('send', text)
  localDraft.value = ''
}

function isStreamingAssistantMessage(message: ChatMessage, index: number): boolean {
  return (
    message.role === 'assistant' &&
    index === props.messages.length - 1 &&
    message.id === streamingMessageId.value
  )
}
</script>
