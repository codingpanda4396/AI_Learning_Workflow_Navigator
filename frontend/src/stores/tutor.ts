import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import type { TutorMessage } from '@/types'
import { getTutorMessages, sendTutorMessage } from '@/api/tutor'
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

  async function retryLastSend() {
    if (!lastSessionId.value || !lastTaskId.value || !lastFailedContent.value) {
      return null
    }
    return send(lastSessionId.value, lastTaskId.value, lastFailedContent.value)
  }

  function reset() {
    messages.value = []
    loadingMessages.value = false
    sendingMessage.value = false
    clearErrors()
    lastFailedContent.value = null
    lastSessionId.value = null
    lastTaskId.value = null
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
    retryLastSend,
    clearErrors,
    reset,
  }
})
