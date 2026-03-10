import type {
  CurrentSessionResponse,
  PathResponse,
  PlanSessionResponse,
  SessionOverviewResponse,
} from '@/types'

interface PlannedNodeStageDto {
  task_id: number
  stage: string
  objective: string
  status: string
}

interface PlannedNodeDto {
  node_id: number
  node_name: string
  status: string
  stages?: PlannedNodeStageDto[]
}

interface PlanSessionResponseDto {
  session_id: number
  plans?: PlannedNodeDto[]
}

interface TimelineItemDto {
  task_id: number
  stage: string
  node_id: number
  status: string
}

interface NextTaskDto {
  task_id: number
  stage: string
  node_id: number
}

interface MasterySummaryDto {
  node_id: number
  node_name: string
  mastery_value: number
}

interface ProgressDto {
  completed_task_count: number
  total_task_count: number
  completion_rate: number
}

interface SessionOverviewResponseDto {
  session_id: number
  course_id: string
  chapter_id: string
  goal_text: string
  current_node_id: number
  current_stage: string
  timeline?: TimelineItemDto[]
  next_task?: NextTaskDto | null
  mastery_summary?: MasterySummaryDto[]
  progress?: ProgressDto | null
}

interface CurrentSessionInfoDto {
  session_id: number
  user_id: string
  course_id: string
  chapter_id: string
  goal_text: string
  current_node_id: number
  current_stage: string
}

interface CurrentSessionResponseDto {
  has_active_session: boolean
  session?: CurrentSessionInfoDto | null
}

interface PathNodeDto {
  node_id: number
  node_name: string
  status: string
  mastery_value: number
}

interface PathResponseDto {
  session_id: number
  current_node_id: number
  nodes?: PathNodeDto[]
}

export function mapPlanSessionDto(dto: PlanSessionResponseDto): PlanSessionResponse {
  return {
    sessionId: dto.session_id,
    plans: (dto.plans ?? []).map((plan) => ({
      nodeId: plan.node_id,
      nodeName: plan.node_name,
      status: plan.status,
      stages: (plan.stages ?? []).map((stage) => ({
        taskId: stage.task_id,
        stage: stage.stage,
        objective: stage.objective,
        status: stage.status,
      })),
    })),
  }
}

export function mapSessionOverviewDto(dto: SessionOverviewResponseDto): SessionOverviewResponse {
  return {
    sessionId: dto.session_id,
    courseId: dto.course_id,
    chapterId: dto.chapter_id,
    goalText: dto.goal_text,
    currentNodeId: dto.current_node_id,
    currentStage: dto.current_stage,
    timeline: (dto.timeline ?? []).map((item) => ({
      taskId: item.task_id,
      stage: item.stage,
      nodeId: item.node_id,
      status: item.status,
    })),
    nextTask: dto.next_task
      ? {
          taskId: dto.next_task.task_id,
          stage: dto.next_task.stage,
          nodeId: dto.next_task.node_id,
        }
      : null,
    masterySummary: (dto.mastery_summary ?? []).map((item) => ({
      nodeId: item.node_id,
      nodeName: item.node_name,
      masteryValue: item.mastery_value,
    })),
    progress: dto.progress
      ? {
          completedTaskCount: dto.progress.completed_task_count,
          totalTaskCount: dto.progress.total_task_count,
          completionRate: dto.progress.completion_rate,
        }
      : null,
  }
}

export function mapCurrentSessionDto(dto: CurrentSessionResponseDto): CurrentSessionResponse {
  return {
    hasActiveSession: dto.has_active_session,
    session: dto.session
      ? {
          sessionId: dto.session.session_id,
          userId: dto.session.user_id,
          courseId: dto.session.course_id,
          chapterId: dto.session.chapter_id,
          goalText: dto.session.goal_text,
          currentNodeId: dto.session.current_node_id,
          currentStage: dto.session.current_stage,
        }
      : null,
  }
}

export function mapPathDto(dto: PathResponseDto): PathResponse {
  return {
    sessionId: dto.session_id,
    currentNodeId: dto.current_node_id,
    nodes: (dto.nodes ?? []).map((node) => ({
      nodeId: node.node_id,
      nodeName: node.node_name,
      status: node.status,
      masteryValue: node.mastery_value,
    })),
  }
}
