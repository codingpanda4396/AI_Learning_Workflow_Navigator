import { request } from './request'
import type {
  CurrentTaskData,
  CurrentTaskGuidanceData,
  CompleteTaskRequest,
  CompleteTaskData,
  TaskMessageResponse,
  SelfExplanationResponse,
  CheckpointResponse,
} from '@/types/dto'
import type { StageScaffold } from '@/types/scaffoldEngine'

export async function getCurrentTask(
  sessionId: string
): Promise<CurrentTaskData> {
  const { data } = await request.get<CurrentTaskData>(
    `/api/sessions/${sessionId}/current-task`
  )
  return data
}

export async function getCurrentTaskGuidance(
  sessionId: string
): Promise<CurrentTaskGuidanceData> {
  const { data } = await request.get<CurrentTaskGuidanceData>(
    `/api/sessions/${sessionId}/current-task-guidance`
  )
  return data
}

export type WorkbenchMode = 'fast' | 'full'

export async function getTaskScaffold(
  taskId: string,
  sessionId: string,
  stage: string,
  workbenchMode: WorkbenchMode = 'full'
): Promise<StageScaffold> {
  const { data } = await request.get<StageScaffold>(
    `/api/tasks/${taskId}/scaffold`,
    { params: { sessionId, stage, workbenchMode } }
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
