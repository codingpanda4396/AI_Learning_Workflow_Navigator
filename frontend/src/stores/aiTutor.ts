import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import { postAiTutorChat, postAiTutorFeedback } from '@/api/tutor'

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

  function buildOpeningPrompt(label: string) {
    const display = label.trim() || '当前知识点'
    return `我们先不急着背定义。你觉得${display}更像什么，或者你脑中会出现什么画面？`
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

  async function submitTutorTurn(input: string): Promise<void> {
    const text = input.trim()
    if (!text || sending.value) return
    sending.value = true
    addUserMessage(text)

    try {
      const chatRes = await postAiTutorChat({
        message: text,
        context: contextPayload.value,
      })
      addAiMessage(chatRes.reply, 'prompt', chatRes.source)
    } catch (error) {
      sending.value = false
      throw error
    }

    try {
      const feedbackRes = await postAiTutorFeedback({
        answer: text,
        step: context.value.stepId,
        knowledgePoint: context.value.knowledgeLabel,
      })
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
    } catch {
      addAiMessage(
        '我先帮你记下这次表达。刚才这段已经有思路了，你可以换个更具体的例子再说一遍，我们继续一起推。',
        'hint',
        'FALLBACK'
      )
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
