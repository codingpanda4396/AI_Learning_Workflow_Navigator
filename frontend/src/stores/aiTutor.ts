import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

export interface AiTutorContextState {
  step: number
  knowledgeKey: string
  knowledgeLabel: string
  phaseCode: string
  phaseLabel: string
  stepId: string
}

export interface AiTutorChatMessage {
  id: string
  role: 'user' | 'assistant'
  content: string
  at: number
}

function newId(): string {
  return `${Date.now()}-${Math.random().toString(36).slice(2, 9)}`
}

const defaultContext = (): AiTutorContextState => ({
  step: 1,
  knowledgeKey: 'unknown',
  knowledgeLabel: '当前主题',
  phaseCode: 'STRUCTURE',
  phaseLabel: '结构认知',
  stepId: 'step1',
})

export const useAiTutorStore = defineStore('aiTutor', () => {
  const visible = ref(false)
  const messages = ref<AiTutorChatMessage[]>([])
  const context = ref<AiTutorContextState>(defaultContext())
  const lastBoundStepId = ref<string | null>(null)

  const contextPayload = computed(() => ({
    step: context.value.step,
    knowledge: context.value.knowledgeKey,
    phase: context.value.phaseCode,
    knowledgeLabel: context.value.knowledgeLabel,
  }))

  function toggleVisible() {
    visible.value = !visible.value
  }

  function openPanel() {
    visible.value = true
  }

  function closePanel() {
    visible.value = false
  }

  function clearMessages() {
    messages.value = []
  }

  function setContext(next: Partial<AiTutorContextState> & { stepId: string }) {
    const sid = next.stepId
    if (lastBoundStepId.value !== sid) {
      messages.value = []
      lastBoundStepId.value = sid
    }
    context.value = {
      ...context.value,
      ...next,
      stepId: sid,
    }
  }

  function appendUserMessage(content: string) {
    const text = content.trim()
    if (!text) return
    messages.value.push({
      id: newId(),
      role: 'user',
      content: text,
      at: Date.now(),
    })
  }

  function appendAssistantMessage(content: string) {
    messages.value.push({
      id: newId(),
      role: 'assistant',
      content: content.trim(),
      at: Date.now(),
    })
  }

  /** 创建空助手气泡，返回 id，供流式追加正文 */
  function beginAssistantStream(): string {
    const id = newId()
    messages.value.push({
      id,
      role: 'assistant',
      content: '',
      at: Date.now(),
    })
    return id
  }

  function appendToAssistantMessage(id: string, chunk: string) {
    if (!chunk) return
    const m = messages.value.find((x) => x.id === id)
    if (m) m.content += chunk
  }

  function finalizeAssistantMessage(id: string) {
    const m = messages.value.find((x) => x.id === id)
    if (m) m.content = m.content.trim()
  }

  function removeMessage(id: string) {
    const i = messages.value.findIndex((x) => x.id === id)
    if (i >= 0) messages.value.splice(i, 1)
  }

  function seedAssistantHint(content: string) {
    if (messages.value.length > 0) return
    appendAssistantMessage(content)
  }

  return {
    visible,
    messages,
    context,
    contextPayload,
    toggleVisible,
    openPanel,
    closePanel,
    clearMessages,
    setContext,
    appendUserMessage,
    appendAssistantMessage,
    beginAssistantStream,
    appendToAssistantMessage,
    finalizeAssistantMessage,
    removeMessage,
    seedAssistantHint,
  }
})
