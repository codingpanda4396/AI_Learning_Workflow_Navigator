<template>
  <div class="flex h-full flex-col rounded-2xl border border-slate-200/80 bg-white shadow-sm">
    <div class="border-b border-slate-100 px-5 py-3">
      <p class="text-xs font-medium text-slate-500">机制理解 · 学习讨论区</p>
    </div>

    <div ref="messagesRef" class="flex-1 space-y-4 overflow-y-auto px-5 py-4">
      <div
        v-for="msg in messages"
        :key="msg.id"
        class="flex"
        :class="msg.role === 'user' ? 'justify-end' : 'justify-start'"
      >
        <div
          class="max-w-[85%] rounded-2xl px-4 py-3 text-sm leading-relaxed"
          :class="bubbleClass(msg.role)"
        >
          <p class="whitespace-pre-wrap">{{ msg.content }}</p>
        </div>
      </div>

      <div v-if="busy" class="flex justify-start">
        <div class="flex items-center gap-1.5 rounded-2xl bg-slate-100 px-4 py-3">
          <span class="h-1.5 w-1.5 animate-bounce rounded-full bg-slate-400" style="animation-delay: 0ms" />
          <span class="h-1.5 w-1.5 animate-bounce rounded-full bg-slate-400" style="animation-delay: 150ms" />
          <span class="h-1.5 w-1.5 animate-bounce rounded-full bg-slate-400" style="animation-delay: 300ms" />
        </div>
      </div>
    </div>

    <div class="border-t border-slate-100 px-4 py-3">
      <div
        class="flex items-end gap-2 rounded-xl border-2 px-3 py-2 transition-colors"
        :class="inputHighlight ? 'border-primary/50 bg-primary/5' : 'border-slate-200 bg-slate-50/50'"
      >
        <textarea
          ref="inputRef"
          v-model="localDraft"
          :placeholder="placeholder"
          rows="2"
          class="flex-1 resize-none bg-transparent text-sm text-slate-800 outline-none placeholder:text-slate-400"
          :disabled="busy"
          @keydown.enter.exact.prevent="handleSend"
        />
        <button
          type="button"
          class="shrink-0 rounded-lg bg-primary px-3 py-1.5 text-xs font-semibold text-white transition hover:bg-primary/90 disabled:opacity-50"
          :disabled="busy || !localDraft.trim()"
          @click="handleSend"
        >
          发送
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch, nextTick } from 'vue'
import type { ChatMessage } from '@/types/executionWorkbench'

const props = defineProps<{
  messages: ChatMessage[]
  draftInput: string
  busy: boolean
  inputHighlight: boolean
  placeholder?: string
}>()

const emit = defineEmits<{
  send: [text: string]
  'update:draftInput': [value: string]
}>()

const messagesRef = ref<HTMLElement | null>(null)
const inputRef = ref<HTMLTextAreaElement | null>(null)
const localDraft = ref(props.draftInput)

watch(
  () => props.draftInput,
  (v) => {
    if (v !== localDraft.value) {
      localDraft.value = v
    }
  },
)

watch(localDraft, (v) => {
  emit('update:draftInput', v)
})

watch(
  () => props.messages.length,
  async () => {
    await nextTick()
    if (messagesRef.value) {
      messagesRef.value.scrollTop = messagesRef.value.scrollHeight
    }
  },
)

watch(
  () => props.inputHighlight,
  async (v) => {
    if (v) {
      await nextTick()
      inputRef.value?.focus()
    }
  },
)

function handleSend() {
  const text = localDraft.value.trim()
  if (!text || props.busy) return
  emit('send', text)
  localDraft.value = ''
}

function bubbleClass(role: string): string {
  if (role === 'user') {
    return 'bg-primary/10 text-slate-800 rounded-br-md'
  }
  return 'bg-slate-100 text-slate-800 rounded-bl-md'
}
</script>
