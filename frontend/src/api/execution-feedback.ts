import { request } from './request'
import type { TaskFeedbackResponse } from '@/types/execution'

export async function postTaskFeedback(
  answer: string
): Promise<TaskFeedbackResponse> {
  const { data } = await request.post<TaskFeedbackResponse>(
    '/api/task/feedback',
    { answer }
  )
  return data
}
