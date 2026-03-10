import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import type { TutorMessage } from '@/types'
import { getTutorMessages, sendTutorMessage, streamTutorMessage } from '@/api/tutor'
import { normalizeApiError } from '@/utils/apiError'
import type { NormalizedApiError } from '@/types/api'

export const useTutorStore = defineStore('tutor', () => {
  const messages = ref<TutorMessage[]>([])
  const loadingMessages = ref(false)
  const sendingMessage = ref(false)
  const loadError = ref<string | null>(null)
  const sendError = ref<string | null>(null)
  const lastFailedContent = ref<string | null>(null)
  const lastSessionId = ref<number | null>(null)
  const lastTaskId = ref<number | null>(null)
  const lastError = ref<NormalizedApiError | null>(null)

  const hasMessages = computed(() => messages.value.length > 0)
  const streamingAssistantId = ref<number | null>(null)

  function clearErrors() {
    loadError.value = null
    sendError.value = null
    lastError.value = null
  }

  async function load(sessionId: number, taskId: number) {
    loadingMessages.value = true
    loadError.value = null
    lastSessionId.value = sessionId
    lastTaskId.value = taskId
    try {
      const response = await getTutorMessages(sessionId, taskId)
      messages.value = response.messages
      return response
    } catch (input) {
      const normalized = normalizeApiError(input)
      lastError.value = normalized
      loadError.value = normalized.message
      throw normalized
    } finally {
      loadingMessages.value = false
    }
  }

  async function send(sessionId: number, taskId: number, content: string) {
    sendingMessage.value = true
    sendError.value = null
    lastSessionId.value = sessionId
    lastTaskId.value = taskId
    try {
      const response = await sendTutorMessage(sessionId, taskId, content)
      messages.value.push(response.userMessage, response.assistantMessage)
      lastFailedContent.value = null
      return response
    } catch (input) {
      const normalized = normalizeApiError(input)
      lastError.value = normalized
      sendError.value = normalized.message
      lastFailedContent.value = content
      throw normalized
    } finally {
      sendingMessage.value = false
    }
  }

  async function sendStream(sessionId: number, taskId: number, content: string) {
    sendingMessage.value = true
    sendError.value = null
    lastSessionId.value = sessionId
    lastTaskId.value = taskId
    try {
      await streamTutorMessage(sessionId, taskId, content, {
        onUserMessage(message) {
          messages.value.push(message as TutorMessage)
        },
        onAssistantDelta(chunk) {
          upsertStreamingAssistant(sessionId, taskId, chunk)
        },
        onCompleted(message) {
          finalizeStreamingAssistant(message as TutorMessage)
        },
      })
      lastFailedContent.value = null
      return true
    } catch (input) {
      const normalized = normalizeApiError(input)
      lastError.value = normalized
      sendError.value = normalized.message
      lastFailedContent.value = content
      clearStreamingAssistant()
      throw normalized
    } finally {
      sendingMessage.value = false
    }
  }

  async function retryLastSend() {
    if (!lastSessionId.value || !lastTaskId.value || !lastFailedContent.value) {
      return null
    }
    return sendStream(lastSessionId.value, lastTaskId.value, lastFailedContent.value)
  }

  function reset() {
    messages.value = []
    loadingMessages.value = false
    sendingMessage.value = false
    clearErrors()
    lastFailedContent.value = null
    lastSessionId.value = null
    lastTaskId.value = null
    streamingAssistantId.value = null
  }

  function upsertStreamingAssistant(sessionId: number, taskId: number, chunk: string) {
    if (!streamingAssistantId.value) {
      const tempId = -Date.now()
      streamingAssistantId.value = tempId
      messages.value.push({
        id: tempId,
        sessionId,
        taskId,
        role: 'assistant',
        content: chunk,
        createdAt: new Date().toISOString(),
      })
      return
    }
    const target = messages.value.find((message) => message.id === streamingAssistantId.value)
    if (target) {
      target.content += chunk
    }
  }

  function finalizeStreamingAssistant(message: TutorMessage) {
    if (!streamingAssistantId.value) {
      messages.value.push(message)
      return
    }
    const index = messages.value.findIndex((item) => item.id === streamingAssistantId.value)
    if (index >= 0) {
      messages.value.splice(index, 1, message)
    } else {
      messages.value.push(message)
    }
    streamingAssistantId.value = null
  }

  function clearStreamingAssistant() {
    if (!streamingAssistantId.value) {
      return
    }
    messages.value = messages.value.filter((message) => message.id !== streamingAssistantId.value)
    streamingAssistantId.value = null
  }

  return {
    messages,
    loadingMessages,
    sendingMessage,
    loadError,
    sendError,
    lastFailedContent,
    hasMessages,
    load,
    send,
    sendStream,
    retryLastSend,
    clearErrors,
    reset,
  }
})
