import { request } from './request'
import { getTaskScaffold } from './task'
import type {
  CompleteConversationStageResult,
  CompleteStructureStageResult,
  LearningScaffoldActionResult,
  StageScaffold,
  StructureSkeletonResult,
} from '@/types/scaffoldEngine'

/** 与 GET /api/tasks/{taskId}/scaffold?stage= 一致（须传引擎当前阶段 STAGE_KEY） */
export async function getLearningScaffoldStage(
  taskId: string,
  sessionId: string,
  stageKey: string
): Promise<StageScaffold> {
  return getTaskScaffold(taskId, sessionId, stageKey)
}

export async function submitLearningScaffoldAction(
  taskId: string,
  body: {
    sessionId: string
    stageKey: string
    actionId: string
    userInput: string
  }
): Promise<LearningScaffoldActionResult> {
  const { data } = await request.post<LearningScaffoldActionResult>(
    `/api/tasks/${taskId}/learning-scaffold/action`,
    body
  )
  return data
}

export async function postStructureSkeleton(
  taskId: string,
  body: { sessionId: string; promptKey: string; followUpKind?: string }
): Promise<StructureSkeletonResult> {
  const { data } = await request.post<StructureSkeletonResult>(
    `/api/tasks/${taskId}/learning-scaffold/structure/skeleton`,
    body
  )
  return data
}

export async function postCompleteStructureStage(
  taskId: string,
  body: { sessionId: string; optionalOneLiner?: string }
): Promise<CompleteStructureStageResult> {
  const { data } = await request.post<CompleteStructureStageResult>(
    `/api/tasks/${taskId}/learning-scaffold/structure/complete`,
    body
  )
  return data
}

export async function postCompleteConversationStage(
  taskId: string,
  body: { sessionId: string; stageKey: string; finalDraft?: string }
): Promise<CompleteConversationStageResult> {
  const { data } = await request.post<CompleteConversationStageResult>(
    `/api/tasks/${taskId}/learning-scaffold/conversation/complete`,
    body
  )
  return data
}
