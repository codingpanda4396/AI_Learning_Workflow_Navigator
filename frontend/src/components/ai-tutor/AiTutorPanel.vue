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
            导师
          </h2>
          <p class="mt-1 text-xs text-text-secondary">
            当前阶段：{{ store.context.phaseLabel }} · 当前知识：{{ store.context.knowledgeLabel }}
          </p>
        </div>
        <button
          type="button"
          class="rounded-input px-2 py-1 text-sm text-text-secondary hover:bg-slate-100"
          aria-label="关闭"
          @click="store.closePanel()"
        >
          ×
        </button>
      </header>

      <section class="shrink-0 space-y-2 border-b border-border px-4 py-3">
        <p class="text-xs font-medium text-text-secondary">快速追问</p>
        <div class="flex flex-wrap gap-2">
          <button
            v-for="(q, i) in quickQuestions"
            :key="i"
            type="button"
            class="rounded-input border border-border bg-slate-50 px-3 py-1.5 text-left text-xs text-text-primary transition hover:border-primary/40 hover:bg-white disabled:cursor-not-allowed disabled:opacity-60"
            :disabled="store.sending"
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
          :type="m.type"
          :source="m.source"
          :streaming="m.streaming"
        />
      </div>

      <footer class="shrink-0 border-t border-border px-4 py-4">
        <p class="text-sm leading-relaxed text-text-secondary">
          这里只看本步记录。继续输入在主卡完成。
        </p>
      </footer>
    </aside>
  </Teleport>
</template>

<script setup lang="ts">
import { computed, nextTick, ref, watch } from 'vue'
import AiTutorMessage from '@/components/ai-tutor/AiTutorMessage.vue'
import { showToast } from '@/stores/toast'
import { getErrorMessage } from '@/api/request'
import { useAiTutorStore } from '@/stores/aiTutor'

const store = useAiTutorStore()
const scrollRef = ref<HTMLElement | null>(null)

const quickQuestions = computed(() => {
  const knowledge = store.context.knowledgeLabel || '这个知识点'
  return [
    `${knowledge}到底更像什么？`,
    '我刚才的理解哪里还不够完整？',
    '你能再换一种方式追问我吗？',
  ]
})

async function scrollToBottom() {
  await nextTick()
  const el = scrollRef.value
  if (el) el.scrollTop = el.scrollHeight
}

watch(
  () => store.messages.map((message) => `${message.id}:${message.content.length}:${message.streaming ? 1 : 0}`).join('|'),
  () => {
    void scrollToBottom()
  },
  { immediate: true }
)

async function sendQuick(text: string) {
  if (!text.trim() || store.sending) return
  try {
    await store.submitTutorTurn(text)
  } catch (error) {
    showToast(getErrorMessage(error))
  }
}
</script>
