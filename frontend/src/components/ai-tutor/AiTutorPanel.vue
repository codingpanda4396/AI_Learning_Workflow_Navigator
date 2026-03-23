<template>
  <Teleport to="body">
    <div
      v-if="store.visible"
      class="fixed inset-0 z-[10055] bg-slate-900/25"
      aria-hidden="true"
      @click.self="store.closePanel()"
    />
    <aside
      v-if="store.visible"
      class="fixed inset-y-0 right-0 z-[10060] flex w-full max-w-md flex-col border-l border-border bg-white shadow-2xl"
      role="dialog"
      aria-labelledby="ai-tutor-panel-title"
    >
      <header
        class="flex shrink-0 items-center justify-between border-b border-border px-4 py-3"
      >
        <div>
          <h2
            id="ai-tutor-panel-title"
            class="text-base font-semibold text-text-primary"
          >
            AI导师
          </h2>
          <p class="mt-1 text-xs text-text-secondary">
            当前阶段：{{ store.context.phaseLabel }} · 当前知识：{{
              store.context.knowledgeLabel
            }}
          </p>
        </div>
        <button
          type="button"
          class="rounded-input px-2 py-1 text-sm text-text-secondary hover:bg-slate-100"
          aria-label="关闭"
          @click="store.closePanel()"
        >
          ✕
        </button>
      </header>

      <section class="shrink-0 space-y-2 border-b border-border px-4 py-3">
        <p class="text-xs font-medium text-text-secondary">快捷问题</p>
        <div class="flex flex-wrap gap-2">
          <button
            v-for="(q, i) in quickQuestions"
            :key="i"
            type="button"
            class="rounded-input border border-border bg-slate-50 px-3 py-1.5 text-left text-xs text-text-primary transition hover:border-primary/40 hover:bg-white"
            :disabled="sending"
            @click="sendQuick(q)"
          >
            {{ q }}
          </button>
        </div>
      </section>

      <div
        ref="scrollRef"
        class="min-h-0 flex-1 space-y-3 overflow-y-auto px-4 py-3"
      >
        <AiTutorMessage
          v-for="m in store.messages"
          :key="m.id"
          :role="m.role"
          :content="m.content"
        />
      </div>

      <footer class="shrink-0 border-t border-border p-4">
        <div class="flex gap-2">
          <input
            v-model="draft"
            type="text"
            class="min-w-0 flex-1 rounded-input border border-border px-3 py-2 text-sm text-text-primary placeholder:text-text-secondary/70 focus:border-primary focus:outline-none focus:ring-1 focus:ring-primary"
            placeholder="输入你想问的话…"
            maxlength="2000"
            :disabled="sending"
            @keydown.enter.prevent="sendDraft"
          />
          <PrimaryButton
            :loading="sending"
            :disabled="!draft.trim()"
            @click="sendDraft"
          >
            发送
          </PrimaryButton>
        </div>
      </footer>
    </aside>
  </Teleport>
</template>

<script setup lang="ts">
import { computed, nextTick, ref, watch } from 'vue'
import { useAiTutorStore } from '@/stores/aiTutor'
import { streamAiTutorChat } from '@/api/tutor'
import { showToast } from '@/stores/toast'
import { getErrorMessage } from '@/api/request'
import AiTutorMessage from '@/components/ai-tutor/AiTutorMessage.vue'
import PrimaryButton from '@/components/ui/PrimaryButton.vue'

const store = useAiTutorStore()
const draft = ref('')
const sending = ref(false)
const scrollRef = ref<HTMLElement | null>(null)

const quickQuestions = computed(() => {
  const k = store.context.knowledgeLabel || '这个知识点'
  return [
    `${k}到底是什么？`,
    '我这样理解对吗？',
    '能换种方式解释吗？',
  ]
})

async function scrollToBottom() {
  await nextTick()
  const el = scrollRef.value
  if (el) el.scrollTop = el.scrollHeight
}

watch(
  () => store.messages.length,
  () => {
    void scrollToBottom()
  }
)

async function sendQuick(text: string) {
  draft.value = text
  await sendDraft()
}

async function sendDraft() {
  const text = draft.value.trim()
  if (!text || sending.value) return
  sending.value = true
  store.appendUserMessage(text)
  draft.value = ''
  const assistantId = store.beginAssistantStream()
  try {
    await streamAiTutorChat(
      {
        message: text,
        context: store.contextPayload,
      },
      {
        onDelta: (d) => store.appendToAssistantMessage(assistantId, d),
      }
    )
    store.finalizeAssistantMessage(assistantId)
    const last = store.messages.find((m) => m.id === assistantId)
    if (last && !last.content) {
      last.content = '（暂无回复）'
    }
  } catch (e) {
    store.finalizeAssistantMessage(assistantId)
    const last = store.messages.find((m) => m.id === assistantId)
    if (last && !last.content) {
      store.removeMessage(assistantId)
    }
    showToast(getErrorMessage(e))
  } finally {
    sending.value = false
  }
}
</script>
