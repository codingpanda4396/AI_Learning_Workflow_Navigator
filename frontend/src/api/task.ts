import { request } from './request'
import type {
  CurrentTaskData,
  CompleteTaskRequest,
  CompleteTaskData,
  TaskScaffoldResponse,
  TaskMessageResponse,
  SelfExplanationResponse,
  CheckpointResponse,
} from '@/types/dto'

export async function getCurrentTask(
  sessionId: string
): Promise<CurrentTaskData> {
  const { data } = await request.get<CurrentTaskData>(
    `/api/sessions/${sessionId}/current-task`
  )
  return data
}

export async function getTaskScaffold(
  taskId: string,
  sessionId: string
): Promise<TaskScaffoldResponse> {
  const { data } = await request.get<TaskScaffoldResponse>(
    `/api/tasks/${taskId}/scaffold`,
    { params: { sessionId } }
  )
  return data
}

export async function postTaskMessage(
  taskId: string,
  sessionId: string,
  content: string
): Promise<TaskMessageResponse> {
  const { data } = await request.post<TaskMessageResponse>(
    `/api/tasks/${taskId}/messages`,
    { sessionId, role: 'USER', content }
  )
  return data
}

export async function postSelfExplanation(
  taskId: string,
  sessionId: string,
  content: string
): Promise<SelfExplanationResponse> {
  const { data } = await request.post<SelfExplanationResponse>(
    `/api/tasks/${taskId}/self-explanation`,
    { sessionId, content }
  )
  return data
}

export async function postCheckpoint(
  taskId: string,
  sessionId: string,
  answer: string
): Promise<CheckpointResponse> {
  const { data } = await request.post<CheckpointResponse>(
    `/api/tasks/${taskId}/checkpoint`,
    { sessionId, answer }
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
