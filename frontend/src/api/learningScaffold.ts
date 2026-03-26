import { request } from './request'
import type {
  CompleteStructureStageResult,
  LearningScaffoldActionResult,
  StageScaffold,
  StructureSkeletonResult,
} from '@/types/scaffoldEngine'

export async function getLearningScaffoldStage(
  taskId: string,
  sessionId: string,
  stageKey?: string
): Promise<StageScaffold> {
  if (stageKey == null || stageKey === '') {
    const { data } = await request.get<StageScaffold>(
      `/api/tasks/${taskId}/learning-scaffold/stage/current`,
      { params: { sessionId } }
    )
    return data
  }
  const { data } = await request.get<StageScaffold>(
    `/api/tasks/${taskId}/learning-scaffold/stage`,
    { params: { sessionId, stageKey } }
  )
  return data
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
