import client from '@/api/client'
import type {
  CreateSessionRequest,
  CurrentSessionResponse,
  GoalDiagnosisResponse,
  PathResponse,
  PathOption,
  PlanSessionResponse,
  SessionOverviewResponse,
} from '@/types'
import {
  mapCurrentSessionDto,
  mapGoalDiagnosisDto,
  mapPathDto,
  mapPathOptionsDto,
  mapPlanSessionDto,
  mapSessionOverviewDto,
} from '@/mappers/sessionMapper'

interface CreateSessionResponseDto {
  session_id: number
}

export async function createSession(request: CreateSessionRequest): Promise<number> {
  const payload = {
    user_id: request.userId,
    course_id: request.courseId,
    chapter_id: request.chapterId,
    goal_text: request.goalText,
  }

  const { data } = await client.post<CreateSessionResponseDto>('/session/create', payload)
  return data.session_id
}

export async function diagnoseGoal(request: CreateSessionRequest): Promise<GoalDiagnosisResponse> {
  const payload = {
    course_id: request.courseId,
    chapter_id: request.chapterId,
    goal_text: request.goalText,
  }
  const { data } = await client.post('/session/goal-diagnose', payload)
  return mapGoalDiagnosisDto(data)
}

export async function getPathOptions(request: CreateSessionRequest): Promise<PathOption[]> {
  const payload = {
    course_id: request.courseId,
    chapter_id: request.chapterId,
    goal_text: request.goalText,
  }
  const { data } = await client.post('/session/path-options', payload)
  return mapPathOptionsDto(data)
}

export async function planSession(sessionId: number): Promise<PlanSessionResponse> {
  const { data } = await client.post(`/session/${sessionId}/plan`)
  return mapPlanSessionDto(data)
}

export async function getSessionOverview(sessionId: number): Promise<SessionOverviewResponse> {
  const { data } = await client.get(`/session/${sessionId}/overview`)
  return mapSessionOverviewDto(data)
}

export async function getCurrentSession(userId: string): Promise<CurrentSessionResponse> {
  const { data } = await client.get('/session/current', {
    params: { user_id: userId },
  })
  return mapCurrentSessionDto(data)
}

export async function getSessionPath(sessionId: number): Promise<PathResponse> {
  const { data } = await client.get(`/session/${sessionId}/path`)
  return mapPathDto(data)
}
