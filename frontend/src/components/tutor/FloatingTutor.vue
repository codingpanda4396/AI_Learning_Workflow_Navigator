<script setup lang="ts">
import { nextTick, ref, watch } from 'vue'
import type { TutorMessage } from '@/types'

const props = defineProps<{
  open: boolean
  available: boolean
  contextTitle: string
  contextMeta?: string
  messages: TutorMessage[]
  loading: boolean
  loadError: string | null
  sendError: string | null
  sending: boolean
  input: string
  quickPrompts: string[]
}>()

const emit = defineEmits<{
  close: []
  retryLoad: []
  retrySend: []
  submit: []
  updateInput: [value: string]
  useQuickPrompt: [value: string]
}>()

const messageListRef = ref<HTMLElement | null>(null)

function formatMessageTime(input: string) {
  const date = new Date(input)
  if (Number.isNaN(date.getTime())) {
    return ''
  }
  return date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
}

async function scrollToBottom() {
  await nextTick()
  const target = messageListRef.value
  if (!target) {
    return
  }
  target.scrollTop = target.scrollHeight
}

watch(
  () => [props.open, props.messages.length, props.sending],
  async ([open]) => {
    if (!open) {
      return
    }
    await scrollToBottom()
  },
)
</script>

<template>
  <Teleport to="body">
    <Transition name="tutor-fade">
      <div v-if="open" class="tutor-overlay" @click.self="emit('close')">
        <Transition name="tutor-slide">
          <aside v-if="open" class="tutor-panel" aria-label="AI Tutor">
            <header class="tutor-panel__head">
              <div class="tutor-panel__title">
                <span>AI Tutor</span>
                <strong>{{ contextTitle }}</strong>
                <p v-if="contextMeta">{{ contextMeta }}</p>
              </div>
              <button type="button" class="icon-btn" aria-label="关闭 Tutor" @click="emit('close')">×</button>
            </header>

            <section class="tutor-panel__chips">
              <button
                v-for="prompt in quickPrompts"
                :key="prompt"
                type="button"
                class="chip-btn"
                :disabled="sending || !available"
                @click="emit('useQuickPrompt', prompt)"
              >
                {{ prompt }}
              </button>
            </section>

            <div v-if="!available" class="state-card">
              当前页面还没有可求助的任务上下文。
            </div>
            <div v-else-if="loading" class="state-card">正在加载 Tutor 对话…</div>
            <div v-else-if="loadError" class="state-card error">
              <span>{{ loadError }}</span>
              <button type="button" class="link-btn" @click="emit('retryLoad')">重试</button>
            </div>
            <div v-else-if="messages.length === 0" class="state-card">
              你可以从上面的快捷问题开始，Tutor 会结合当前学习步骤来回答。
            </div>
            <div v-else ref="messageListRef" class="message-list">
              <article
                v-for="message in messages"
                :key="message.id"
                class="message-item"
                :class="message.role === 'user' ? 'user' : 'assistant'"
              >
                <p>{{ message.content }}</p>
                <span>{{ formatMessageTime(message.createdAt) }}</span>
              </article>
            </div>

            <div v-if="sending" class="sending-state">Tutor 正在整理回答…</div>

            <form class="input-row" @submit.prevent="emit('submit')">
              <textarea
                class="input-box"
                rows="3"
                :value="input"
                :disabled="sending || !available"
                placeholder="例如：这一步为什么要先这样做？"
                @input="emit('updateInput', ($event.target as HTMLTextAreaElement).value)"
              ></textarea>
              <button type="submit" class="send-btn" :disabled="sending || !available || !input.trim()">发送</button>
            </form>

            <div v-if="sendError" class="state-card error">
              <span>{{ sendError }}</span>
              <button type="button" class="link-btn" @click="emit('retrySend')">重试发送</button>
            </div>
          </aside>
        </Transition>
      </div>
    </Transition>
  </Teleport>
</template>

<style scoped>
.tutor-overlay {
  position: fixed;
  inset: 0;
  z-index: 47;
  background: rgba(5, 8, 14, 0.5);
  backdrop-filter: blur(4px);
}

.tutor-panel {
  position: absolute;
  top: 0;
  right: 0;
  display: grid;
  grid-template-rows: auto auto minmax(0, 1fr) auto auto;
  gap: var(--space-lg);
  width: min(420px, 100vw);
  height: 100dvh;
  padding: clamp(18px, 2vw, 24px);
  border-left: 1px solid rgba(61, 80, 104, 0.5);
  background:
    linear-gradient(180deg, rgba(20, 28, 42, 0.98), rgba(11, 17, 27, 0.98)),
    rgba(12, 18, 28, 0.98);
  box-shadow: -18px 0 48px rgba(0, 0, 0, 0.3);
}

.tutor-panel__head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: var(--space-md);
}

.tutor-panel__title {
  display: grid;
  gap: 4px;
}

.tutor-panel__title span {
  color: var(--color-text-muted);
  font-size: var(--font-size-xs);
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.tutor-panel__title strong {
  font-family: var(--font-display);
  font-size: 1.15rem;
}

.tutor-panel__title p,
.state-card,
.sending-state,
.message-item span {
  color: var(--color-text-secondary);
}

.icon-btn {
  min-width: 40px;
  min-height: 40px;
  border-radius: 50%;
  border: 1px solid rgba(61, 80, 104, 0.48);
  color: var(--color-text-secondary);
  background: rgba(15, 21, 33, 0.9);
}

.tutor-panel__chips {
  display: flex;
  gap: var(--space-sm);
  flex-wrap: wrap;
}

.chip-btn {
  min-height: 36px;
  padding: 0 12px;
  border-radius: var(--radius-full);
  border: 1px solid rgba(61, 80, 104, 0.48);
  color: var(--color-text);
  background: rgba(9, 14, 23, 0.92);
}

.chip-btn:disabled {
  opacity: 0.55;
  cursor: not-allowed;
}

.state-card {
  display: flex;
  justify-content: space-between;
  gap: var(--space-md);
  padding: var(--space-lg);
  border-radius: var(--radius-lg);
  background: rgba(8, 13, 22, 0.92);
  border: 1px solid rgba(61, 80, 104, 0.28);
}

.state-card.error {
  border-color: rgba(255, 122, 138, 0.32);
}

.message-list {
  min-height: 0;
  overflow-y: auto;
  display: grid;
  gap: var(--space-sm);
  padding-right: 4px;
}

.message-item {
  display: grid;
  gap: 8px;
  padding: 14px 14px 12px;
  border-radius: 16px;
  border: 1px solid rgba(61, 80, 104, 0.32);
}

.message-item.user {
  background: rgba(20, 39, 69, 0.76);
}

.message-item.assistant {
  background: rgba(9, 14, 22, 0.84);
}

.message-item p {
  margin: 0;
  white-space: pre-wrap;
  line-height: 1.65;
}

.message-item span {
  font-size: var(--font-size-xs);
}

.sending-state {
  font-size: var(--font-size-sm);
}

.input-row {
  display: grid;
  gap: var(--space-md);
}

.input-box {
  width: 100%;
  resize: vertical;
  min-height: 108px;
  padding: 14px 16px;
  border-radius: var(--radius-lg);
  border: 1px solid rgba(61, 80, 104, 0.48);
  background: rgba(7, 12, 20, 0.92);
  color: var(--color-text);
}

.send-btn,
.link-btn {
  align-self: flex-start;
  min-height: 42px;
  padding: 0 16px;
  border-radius: var(--radius-md);
  color: var(--color-primary-hover);
}

.send-btn {
  border: 1px solid rgba(107, 159, 255, 0.34);
  background: rgba(107, 159, 255, 0.14);
}

.send-btn:disabled {
  opacity: 0.55;
  cursor: not-allowed;
}

.tutor-fade-enter-active,
.tutor-fade-leave-active,
.tutor-slide-enter-active,
.tutor-slide-leave-active {
  transition: all var(--duration-normal) var(--ease-smooth);
}

.tutor-fade-enter-from,
.tutor-fade-leave-to {
  opacity: 0;
}

.tutor-slide-enter-from,
.tutor-slide-leave-to {
  transform: translateX(100%);
}

@media (max-width: 768px) {
  .tutor-panel {
    width: 100vw;
    border-left: none;
  }
}
</style>
