<template>
  <div class="rounded-2xl border border-slate-200/80 bg-white shadow-sm">
    <div class="border-b border-slate-100 px-5 py-3">
      <p class="text-xs font-medium text-slate-500">表达训练 · 第 {{ roundCount }} / {{ maxRounds }} 轮</p>
    </div>

    <div ref="messagesRef" class="max-h-[420px] space-y-4 overflow-y-auto px-5 py-4">
      <div
        v-for="msg in messages"
        :key="msg.id"
        class="flex"
        :class="msg.role === 'user' ? 'justify-end' : 'justify-start'"
      >
        <div
          class="max-w-[85%] rounded-2xl px-4 py-3 text-sm leading-relaxed"
          :class="msg.role === 'user'
            ? 'rounded-br-md bg-primary/10 text-slate-800'
            : msg.role === 'system'
              ? 'rounded-bl-md bg-slate-50 text-slate-500'
              : 'rounded-bl-md bg-slate-100 text-slate-800'"
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

    <div v-if="!isComplete" class="border-t border-slate-100 px-4 py-3">
      <p v-if="error" class="mb-2 text-xs text-rose-500">{{ error }}</p>
      <p v-else-if="completionHint" class="mb-2 text-xs text-slate-500">{{ completionHint }}</p>
      <div class="flex items-end gap-2 rounded-xl border-2 border-slate-200 bg-slate-50/50 px-3 py-2">
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
          class="shrink-0 rounded-lg bg-primary px-3 py-1.5 text-xs font-semibold text-white transition hover:bg-primary/90 disabled:opacity-50"
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
import { nextTick, ref, watch } from 'vue'
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

const messagesRef = ref<HTMLElement | null>(null)
const localDraft = ref(props.draftInput)

watch(
  () => props.draftInput,
  (v) => {
    if (v !== localDraft.value) localDraft.value = v
  }
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
  }
)

function handleSend() {
  const text = localDraft.value.trim()
  if (!text || props.busy) return
  emit('send', text)
  localDraft.value = ''
}
</script>
