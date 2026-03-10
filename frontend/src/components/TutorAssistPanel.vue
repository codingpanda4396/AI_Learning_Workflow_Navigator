<script setup lang="ts">
import type { TutorMessage } from '@/types'

defineProps<{
  messages: TutorMessage[]
  loading: boolean
  loadError: string | null
  sendError: string | null
  sending: boolean
  input: string
  chips: string[]
}>()

const emit = defineEmits<{
  retryLoad: []
  retrySend: []
  submit: []
  updateInput: [value: string]
  useChip: [value: string]
}>()

function formatMessageTime(input: string) {
  const date = new Date(input)
  if (Number.isNaN(date.getTime())) {
    return ''
  }
  return date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
}
</script>

<template>
  <section class="tutor-card">
    <div class="tutor-head">
      <div>
        <span class="tutor-label">辅助区</span>
        <h3>学习助手</h3>
      </div>
      <p>围绕当前任务随时提问，获得即时讲解</p>
    </div>

    <div class="chip-row">
      <button v-for="chip in chips" :key="chip" type="button" class="chip-btn" @click="emit('useChip', chip)">
        {{ chip }}
      </button>
    </div>

    <div v-if="loading" class="state-card">正在加载提问记录...</div>
    <div v-else-if="loadError" class="state-card error">
      <span>{{ loadError }}</span>
      <button type="button" class="link-btn" @click="emit('retryLoad')">重新加载</button>
    </div>
    <div v-else-if="messages.length === 0" class="state-card">
      还没有提问记录，可以从下面这些问题开始
    </div>
    <div v-else class="message-list">
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

    <div v-if="sending" class="sending-state">学习助手正在整理回答...</div>

    <form class="input-row" @submit.prevent="emit('submit')">
      <textarea
        class="input-box"
        rows="3"
        :value="input"
        :disabled="sending"
        placeholder="例如：为什么这里要先求齐次解？"
        @input="emit('updateInput', ($event.target as HTMLTextAreaElement).value)"
      ></textarea>
      <button type="submit" class="send-btn" :disabled="sending || !input.trim()">发送提问</button>
    </form>

    <div v-if="sendError" class="state-card error">
      <span>{{ sendError }}</span>
      <button type="button" class="link-btn" @click="emit('retrySend')">重试发送</button>
    </div>
  </section>
</template>

<style scoped>
.tutor-card {
  display: grid;
  gap: var(--space-lg);
  padding: clamp(18px, 2.6vw, 26px);
  border-radius: var(--radius-xl);
  border: 1px solid rgba(61, 80, 104, 0.42);
  background: rgba(15, 22, 34, 0.82);
}

.tutor-head {
  display: flex;
  justify-content: space-between;
  gap: var(--space-md);
  align-items: flex-start;
  flex-wrap: wrap;
}

.tutor-head p,
.state-card,
.sending-state,
.message-item span {
  color: var(--color-text-secondary);
}

.tutor-label {
  display: inline-block;
  margin-bottom: 6px;
  color: var(--color-text-muted);
  font-size: var(--font-size-xs);
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.chip-row {
  display: flex;
  gap: var(--space-sm);
  flex-wrap: wrap;
}

.chip-btn {
  min-height: 38px;
  padding: 0 12px;
  border-radius: var(--radius-full);
  border: 1px solid rgba(61, 80, 104, 0.5);
  color: var(--color-text);
  background: rgba(10, 16, 26, 0.92);
}

.state-card {
  display: flex;
  justify-content: space-between;
  gap: var(--space-md);
  padding: var(--space-lg);
  border-radius: var(--radius-md);
  background: rgba(10, 15, 24, 0.9);
}

.state-card.error {
  border: 1px solid rgba(255, 122, 138, 0.3);
}

.message-list {
  max-height: 360px;
  overflow-y: auto;
  display: grid;
  gap: var(--space-sm);
}

.message-item {
  display: grid;
  gap: 8px;
  padding: var(--space-md);
  border-radius: var(--radius-md);
  border: 1px solid rgba(61, 80, 104, 0.38);
}

.message-item.user {
  background: rgba(18, 41, 78, 0.65);
}

.message-item.assistant {
  background: rgba(9, 15, 24, 0.78);
}

.message-item p {
  white-space: pre-wrap;
  line-height: 1.65;
}

.message-item span {
  font-size: var(--font-size-xs);
}

.input-row {
  display: grid;
  gap: var(--space-md);
}

.input-box {
  width: 100%;
  resize: vertical;
  padding: 14px 16px;
  border-radius: var(--radius-md);
  border: 1px solid rgba(61, 80, 104, 0.5);
  background: rgba(7, 12, 20, 0.88);
  color: var(--color-text);
}

.send-btn,
.link-btn {
  align-self: flex-start;
  color: var(--color-primary-hover);
}

.send-btn {
  min-height: 44px;
  padding: 0 18px;
  border-radius: var(--radius-md);
  background: rgba(107, 159, 255, 0.16);
  border: 1px solid rgba(107, 159, 255, 0.38);
}

.send-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}
</style>
