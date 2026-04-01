import { request } from './request'
import type { TaskFeedbackResponse } from '@/types/execution'

export interface AiTutorEnvelope {
  source: string
  content: string
}

export interface AiTutorExplainPostData {
  source: string
  explanation: string
}

export function postTutorPrefetch(step: string, knowledgePoint: string): void {
  void request.post<void>('/api/ai-tutor/prefetch', { step, knowledgePoint }).catch(() => {
    /* noop */
  })
}

export async function getTutorPrompt(step: string, knowledgePoint: string): Promise<AiTutorEnvelope> {
  const { data } = await request.get<AiTutorEnvelope>('/api/ai-tutor/prompt', {
    params: { step, knowledgePoint },
  })
  return data
}

export async function getTutorExplain(step: string, knowledgePoint: string): Promise<AiTutorEnvelope> {
  const { data } = await request.get<AiTutorEnvelope>('/api/ai-tutor/explain', {
    params: { step, knowledgePoint },
  })
  return data
}

export async function postTutorExplain(body: {
  step: string
  knowledgePoint: string
  userPrompt?: string
}): Promise<AiTutorExplainPostData> {
  const { data } = await request.post<AiTutorExplainPostData>('/api/ai-tutor/explain', body)
  return data
}

export interface AiTutorChatMessagePayload {
  role: 'system' | 'user' | 'assistant'
  content: string
}

export interface AiTutorChatContextPayload {
  step?: number
  knowledge: string
  phase: string
  knowledgeLabel?: string
  sessionId?: string
  taskId?: string
}

export interface AiTutorChatRequestPayload {
  messages: AiTutorChatMessagePayload[]
  context: AiTutorChatContextPayload
}

export interface AiTutorChatResponseData {
  reply: string
  source?: string
  canProceed?: boolean
  finalDraft?: string | null
  completionHint?: string | null
  summary?: string | null
}

export interface AiTutorFeedbackPayload {
  answer: string
  step?: string
  knowledgePoint?: string
}

export async function postAiTutorChat(
  payload: AiTutorChatRequestPayload
): Promise<AiTutorChatResponseData> {
  const { data } = await request.post<AiTutorChatResponseData>('/api/ai-tutor/chat', payload)
  return data
}

export async function postAiTutorFeedback(
  payload: AiTutorFeedbackPayload
): Promise<TaskFeedbackResponse> {
  const { data } = await request.post<TaskFeedbackResponse>('/api/ai-tutor/feedback', payload)
  return data
}

function parseOneSseEvent(
  block: string,
  handlers: {
    onMeta?: (payload: Record<string, string>) => void
    onDelta: (text: string) => void
    onDone?: (payload: Record<string, string>) => void
    onStreamError?: (message: string) => void
  }
): void {
  let eventType = 'message'
  const dataLines: string[] = []
  for (const line of block.split('\n')) {
    if (line.startsWith('event:')) {
      eventType = line.slice(6).trim()
    } else if (line.startsWith('data:')) {
      dataLines.push(line.slice(5).trimStart())
    }
  }
  const dataStr = dataLines.join('\n').trim()
  if (!dataStr) return
  if (eventType === 'meta') {
    try {
      handlers.onMeta?.(JSON.parse(dataStr) as Record<string, string>)
    } catch {
      /* ignore */
    }
    return
  }
  if (eventType === 'delta') {
    try {
      const o = JSON.parse(dataStr) as { text?: string }
      if (o.text) handlers.onDelta(o.text)
    } catch {
      /* ignore */
    }
    return
  }
  if (eventType === 'done') {
    try {
      handlers.onDone?.(JSON.parse(dataStr) as Record<string, string>)
    } catch {
      handlers.onDone?.({})
    }
    return
  }
  if (eventType === 'error') {
    try {
      const o = JSON.parse(dataStr) as { message?: string }
      handlers.onStreamError?.(o.message ?? '导师回复失败')
    } catch {
      handlers.onStreamError?.('导师回复失败')
    }
  }
}

export async function streamAiTutorChat(
  payload: AiTutorChatRequestPayload,
  handlers: {
    onMeta?: (payload: Record<string, string>) => void
    onDelta: (text: string) => void
    onDone?: (payload: Record<string, string>) => void
    signal?: AbortSignal
  }
): Promise<void> {
  const baseURL = import.meta.env.DEV ? '' : 'http://localhost:8080'
  const res = await fetch(`${baseURL}/api/ai-tutor/chat/stream`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      Accept: 'text/event-stream',
    },
    body: JSON.stringify(payload),
    signal: handlers.signal,
  })
  if (!res.ok) {
    const t = await res.text().catch(() => '')
    throw new Error(t || `请求失败 ${res.status}`)
  }
  const reader = res.body?.getReader()
  if (!reader) {
    throw new Error('无响应流')
  }
  const decoder = new TextDecoder()
  let buffer = ''
  let streamErr: Error | null = null
  const h = {
    ...handlers,
    onStreamError: (m: string) => {
      streamErr = new Error(m)
    },
  }
  while (true) {
    const { done, value } = await reader.read()
    if (value) {
      buffer += decoder.decode(value, { stream: true })
      buffer = buffer.replace(/\r\n/g, '\n')
      let sep: number
      while ((sep = buffer.indexOf('\n\n')) >= 0) {
        const chunk = buffer.slice(0, sep).trim()
        buffer = buffer.slice(sep + 2)
        if (chunk) parseOneSseEvent(chunk, h)
        if (streamErr) {
          await reader.cancel().catch(() => {})
          throw streamErr
        }
      }
    }
    if (done) break
  }
  const tail = buffer.replace(/\r\n/g, '\n').trim()
  if (tail) {
    parseOneSseEvent(tail, h)
    if (streamErr) throw streamErr
  }
}
