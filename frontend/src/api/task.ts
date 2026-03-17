import { request } from './request'
import type {
  CurrentTaskData,
  CompleteTaskRequest,
  CompleteTaskData,
} from '@/types/dto'

export async function getCurrentTask(
  sessionId: string
): Promise<CurrentTaskData> {
  const { data } = await request.get<CurrentTaskData>(
    `/api/sessions/${sessionId}/current-task`
  )
  return data
}

export async function completeTask(
  taskId: string,
  payload: CompleteTaskRequest
): Promise<CompleteTaskData> {
  const { data } = await request.post<CompleteTaskData>(
    `/api/tasks/${taskId}/complete`,
    payload
  )
  return data
}
