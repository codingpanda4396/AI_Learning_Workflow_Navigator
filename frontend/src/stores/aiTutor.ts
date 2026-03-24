import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import {
  postAiTutorChat,
  postAiTutorFeedback,
  streamAiTutorChat,
} from '@/api/tutor'

export interface AiTutorContextState {
  step: number
  knowledgeKey: string
  knowledgeLabel: string
  phaseCode: string
  phaseLabel: string
  stepId: string
}

export interface AiTutorMessage {
  id: string
  role: 'ai' | 'user'
  content: string
  type?: 'prompt' | 'feedback' | 'hint'
  source?: string
  streaming?: boolean
  at: number
}

function newId(): string {
  return `${Date.now()}-${Math.random().toString(36).slice(2, 9)}`
}

const defaultContext = (): AiTutorContextState => ({
  step: 1,
  knowledgeKey: 'unknown',
  knowledgeLabel: '\u5f53\u524d\u4e3b\u9898',
  phaseCode: 'STRUCTURE',
  phaseLabel: '\u7ed3\u6784\u8ba4\u77e5',
  stepId: 'step1',
})

export const useAiTutorStore = defineStore('aiTutor', () => {
  const visible = ref(false)
  const messages = ref<AiTutorMessage[]>([])
  const context = ref<AiTutorContextState>(defaultContext())
  const lastBoundStepId = ref<string | null>(null)
  const sending = ref(false)

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

  function addUserMessage(content: string) {
    const text = content.trim()
    if (!text) return
    messages.value.push({
      id: newId(),
      role: 'user',
      content: text,
      at: Date.now(),
    })
  }

  function addAiMessage(
    content: string,
    type?: AiTutorMessage['type'],
    source?: string
  ) {
    const text = content.trim()
    if (!text) return
    messages.value.push({
      id: newId(),
      role: 'ai',
      content: text,
      type,
      source,
      at: Date.now(),
    })
  }

  function createAiDraftMessage(type: AiTutorMessage['type'] = 'prompt') {
    const draft: AiTutorMessage = {
      id: newId(),
      role: 'ai',
      content: '\u8ba9\u6211\u60f3\u4e00\u60f3...',
      streaming: true,
      type,
      at: Date.now(),
    }
    messages.value.push(draft)
    return draft
  }

  function removeMessage(id: string) {
    const idx = messages.value.findIndex((item) => item.id === id)
    if (idx >= 0) {
      messages.value.splice(idx, 1)
    }
  }

  function buildOpeningPrompt(label: string) {
    const display = label.trim() || '\u5f53\u524d\u77e5\u8bc6\u70b9'
    return `\u6211\u4eec\u5148\u4e0d\u6025\u7740\u80cc\u5b9a\u4e49\u3002\u4f60\u89c9\u5f97${display}\u66f4\u50cf\u4ec0\u4e48\uff0c\u6216\u8005\u4f60\u8111\u4e2d\u4f1a\u51fa\u73b0\u4ec0\u4e48\u753b\u9762\uff1f`
  }

  function resetForStep(next: Partial<AiTutorContextState> & { stepId: string }) {
    setContext(next)
    clearMessages()
    addAiMessage(buildOpeningPrompt(context.value.knowledgeLabel), 'prompt')
  }

  function buildFeedbackMessage(feedback: {
    comment: string
    suggestion: string
    praise?: string | null
    gap?: string | null
  }) {
    return [feedback.praise, feedback.comment, feedback.gap, feedback.suggestion]
      .map((item) => item?.trim())
      .filter((item): item is string => Boolean(item))
      .join('\n\n')
  }

  async function streamTutorReply(text: string): Promise<void> {
    const draft = createAiDraftMessage('prompt')
    let hasRealChunk = false
    let pendingText = ''
    let flushScheduled = false

    const flushDraft = () => {
      flushScheduled = false
      if (!pendingText) return
      if (!hasRealChunk) {
        draft.content = ''
        hasRealChunk = true
      }
      draft.content += pendingText
      pendingText = ''
    }

    const queueChunk = (chunk: string) => {
      pendingText += chunk
      if (flushScheduled) return
      flushScheduled = true
      requestAnimationFrame(flushDraft)
    }

    try {
      await streamAiTutorChat(
        {
          message: text,
          context: contextPayload.value,
        },
        {
          onMeta: (source) => {
            draft.source = source || draft.source
          },
          onDelta: (chunk) => {
            queueChunk(chunk)
          },
        }
      )

      if (flushScheduled) {
        flushDraft()
      }

      const finalText = draft.content.trim()
      if (!finalText) {
        removeMessage(draft.id)
        throw new Error('AI\u5bfc\u5e08\u6682\u65f6\u6ca1\u6709\u8fd4\u56de\u5185\u5bb9')
      }
      draft.content = finalText
      draft.streaming = false
    } catch (streamError) {
      removeMessage(draft.id)
      const chatRes = await postAiTutorChat({
        message: text,
        context: contextPayload.value,
      })
      addAiMessage(chatRes.reply, 'prompt', chatRes.source)
      if (streamError instanceof Error) {
        console.warn('streamAiTutorChat failed, fallback to chat:', streamError)
      }
    }
  }

  async function submitTutorTurn(input: string): Promise<void> {
    const text = input.trim()
    if (!text || sending.value) return
    sending.value = true
    addUserMessage(text)

    const replyTask = streamTutorReply(text)
    const feedbackTask = postAiTutorFeedback({
      answer: text,
      step: context.value.stepId,
      knowledgePoint: context.value.knowledgeLabel,
    })
      .then((feedbackRes) => {
        addAiMessage(
          buildFeedbackMessage(feedbackRes),
          'feedback',
          feedbackRes.source ?? undefined
        )
        if (feedbackRes.nextHint?.trim()) {
          addAiMessage(
            feedbackRes.nextHint,
            'hint',
            feedbackRes.source ?? undefined
          )
        }
      })
      .catch(() => {
        addAiMessage(
          '\u6211\u5148\u5e2e\u4f60\u8bb0\u4e0b\u8fd9\u6b21\u8868\u8fbe\u3002\u521a\u624d\u8fd9\u6bb5\u5df2\u7ecf\u6709\u601d\u8def\u4e86\uff0c\u4f60\u53ef\u4ee5\u6362\u4e2a\u66f4\u5177\u4f53\u7684\u4f8b\u5b50\u518d\u8bf4\u4e00\u904d\uff0c\u6211\u4eec\u7ee7\u7eed\u4e00\u8d77\u63a8\u3002',
          'hint',
          'FALLBACK'
        )
      })

    try {
      await Promise.all([replyTask, feedbackTask])
    } finally {
      sending.value = false
    }
  }

  return {
    visible,
    messages,
    context,
    contextPayload,
    sending,
    toggleVisible,
    openPanel,
    closePanel,
    clearMessages,
    setContext,
    addAiMessage,
    addUserMessage,
    resetForStep,
    submitTutorTurn,
  }
})
