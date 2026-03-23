import { request } from './request'
import type { TaskFeedbackResponse } from '@/types/execution'

export interface TaskFeedbackPayload {
  answer: string
  step?: string
  knowledgePoint?: string
}

export async function postTaskFeedback(
  payload: TaskFeedbackPayload
): Promise<TaskFeedbackResponse> {
  const { data } = await request.post<TaskFeedbackResponse>(
    '/api/task/feedback',
    payload
  )
  return data
}
