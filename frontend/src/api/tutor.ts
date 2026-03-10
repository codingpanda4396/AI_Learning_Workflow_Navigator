import client from '@/api/client'
import type { TutorMessageListResponse, TutorSendMessageResponse } from '@/types'
import { mapTutorMessageDto, mapTutorMessageListDto, mapTutorSendMessageDto } from '@/mappers/tutorMapper'
import { getAccessToken } from '@/auth/storage'

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || '/api'

interface SendTutorMessageRequestDto {
  content: string
}

export async function getTutorMessages(sessionId: number, taskId: number): Promise<TutorMessageListResponse> {
  const { data } = await client.get(`/session/${sessionId}/tasks/${taskId}/tutor/messages`)
  return mapTutorMessageListDto(data)
}

export async function sendTutorMessage(
  sessionId: number,
  taskId: number,
  content: string,
): Promise<TutorSendMessageResponse> {
  const payload: SendTutorMessageRequestDto = { content }
  const { data } = await client.post(`/session/${sessionId}/tasks/${taskId}/tutor/messages`, payload)
  return mapTutorSendMessageDto(data)
}

export async function streamTutorMessage(
  sessionId: number,
  taskId: number,
  content: string,
  handlers: {
    onUserMessage?: (message: unknown) => void
    onAssistantDelta?: (chunk: string) => void
    onCompleted?: (message: unknown) => void
  },
): Promise<void> {
  const response = await fetch(`${API_BASE_URL}/session/${sessionId}/tasks/${taskId}/tutor/messages/stream`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json; charset=utf-8',
      ...(getAccessToken() ? { Authorization: `Bearer ${getAccessToken()}` } : {}),
    },
    body: JSON.stringify({ content }),
  })

  if (!response.ok || !response.body) {
    throw new Error(`Tutor stream failed: ${response.status}`)
  }

  const reader = response.body.getReader()
  const decoder = new TextDecoder()
  let buffer = ''
  while (true) {
    const { done, value } = await reader.read()
    if (done) {
      break
    }
    buffer += decoder.decode(value, { stream: true })
    let separatorIndex = buffer.indexOf('\n\n')
    while (separatorIndex >= 0) {
      const rawEvent = buffer.slice(0, separatorIndex)
      buffer = buffer.slice(separatorIndex + 2)
      handleSseEvent(rawEvent, handlers)
      separatorIndex = buffer.indexOf('\n\n')
    }
  }
}

function handleSseEvent(
  rawEvent: string,
  handlers: {
    onUserMessage?: (message: unknown) => void
    onAssistantDelta?: (chunk: string) => void
    onCompleted?: (message: unknown) => void
  },
) {
  const lines = rawEvent.split('\n')
  const eventName = lines.find((line) => line.startsWith('event:'))?.slice(6).trim()
  const data = lines
    .filter((line) => line.startsWith('data:'))
    .map((line) => line.slice(5).trim())
    .join('\n')
  if (!eventName) {
    return
  }
  if (eventName === 'assistant_delta') {
    handlers.onAssistantDelta?.(data)
    return
  }
  if (!data) {
    return
  }
  const parsed = JSON.parse(data)
  if (eventName === 'user_message') {
    handlers.onUserMessage?.(mapTutorMessageDto(parsed))
  }
  if (eventName === 'completed') {
    handlers.onCompleted?.(mapTutorMessageDto(parsed))
  }
}
