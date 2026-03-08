import client from '@/api/client'
import type {
  CreateSessionRequest,
  CurrentSessionResponse,
  GoalDiagnosisResponse,
  PathResponse,
  PathOption,
  PlanSessionResponse,
  SessionHistoryResponse,
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

interface SessionHistoryItemDto {
  session_id: number
  course: string
  chapter: string
  goal: string
  status: string
  progress?: {
    completed_task_count: number
    total_task_count: number
    completion_rate: number
  } | null
  last_active_at: string
}

interface SessionHistoryResponseDto {
  page: number
  page_size: number
  total: number
  total_pages: number
  items?: SessionHistoryItemDto[]
}

export async function createSession(request: CreateSessionRequest): Promise<number> {
  const payload = {
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
  const { data } = await client.post('/session/goal-diagnose', payload, { timeout: 90000 })
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

export async function getCurrentSession(): Promise<CurrentSessionResponse> {
  const { data } = await client.get('/session/current')
  return mapCurrentSessionDto(data)
}

export async function getSessionPath(sessionId: number): Promise<PathResponse> {
  const { data } = await client.get(`/session/${sessionId}/path`)
  return mapPathDto(data)
}

export async function getSessionHistory(params?: {
  page?: number
  pageSize?: number
  status?: string
}): Promise<SessionHistoryResponse> {
  const query = {
    page: params?.page,
    page_size: params?.pageSize,
    status: params?.status,
  }
  const { data } = await client.get<SessionHistoryResponseDto>('/session/history', {
    params: query,
  })
  return {
    page: data.page,
    pageSize: data.page_size,
    total: data.total,
    totalPages: data.total_pages,
    items: (data.items ?? []).map((item) => ({
      sessionId: item.session_id,
      course: item.course,
      chapter: item.chapter,
      goal: item.goal,
      status: item.status,
      progress: item.progress
        ? {
            completedTaskCount: item.progress.completed_task_count,
            totalTaskCount: item.progress.total_task_count,
            completionRate: item.progress.completion_rate,
          }
        : null,
      lastActiveAt: item.last_active_at,
    })),
  }
}

export async function resumeSession(sessionId: number): Promise<SessionOverviewResponse> {
  const { data } = await client.post(`/session/${sessionId}/resume`)
  return mapSessionOverviewDto(data)
}
