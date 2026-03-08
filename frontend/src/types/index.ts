export type Stage = 'STRUCTURE' | 'UNDERSTANDING' | 'TRAINING' | 'REFLECTION'
export type TaskStatus = 'PENDING' | 'RUNNING' | 'SUCCEEDED' | 'FAILED'
export type ErrorTag =
  | 'CONCEPT_CONFUSION'
  | 'MISSING_STEPS'
  | 'BOUNDARY_CASE'
  | 'TERMINOLOGY'
  | 'SHALLOW_REASONING'
  | 'MEMORY_GAP'
export type NextAction =
  | 'INSERT_REMEDIAL_UNDERSTANDING'
  | 'INSERT_TRAINING_VARIANTS'
  | 'INSERT_TRAINING_REINFORCEMENT'
  | 'ADVANCE_TO_NEXT_NODE'
  | 'NOOP'

export interface CreateSessionRequest {
  userId: string
  courseId: string
  chapterId: string
  goalText: string
}

export interface GoalDiagnosisResponse {
  goalScore: number
  feedback: {
    summary: string
    strengths: string[]
    risks: string[]
    rewrittenGoal: string
  }
}

export interface PathOption {
  pathId: string
  name: string
  description: string
  difficulty: string
  estimatedMinutes: number
  steps: string[]
}

export interface PlannedTask {
  taskId: number
  stage: string
  nodeId: number
  objective: string
  status: string
}

export interface PlanSessionResponse {
  sessionId: number
  tasks: PlannedTask[]
}

export interface TimelineItem {
  taskId: number
  stage: string
  nodeId: number
  status: string
}

export interface NextTask {
  taskId: number
  stage: string
  nodeId: number
}

export interface MasterySummary {
  nodeId: number
  nodeName: string
  masteryValue: number
}

export interface Progress {
  completedTaskCount: number
  totalTaskCount: number
  completionRate: number
}

export interface SessionOverviewResponse {
  sessionId: number
  courseId: string
  chapterId: string
  goalText: string
  currentNodeId: number
  currentStage: string
  timeline: TimelineItem[]
  nextTask: NextTask | null
  masterySummary: MasterySummary[]
  progress: Progress | null
}

export interface TaskOutputSection {
  type: string
  title: string
  bullets?: string[]
  steps?: string[]
  items?: string[]
  text?: string
}

export interface TaskOutput {
  sections: TaskOutputSection[]
}

export interface RunTaskResponse {
  taskId: number
  stage: string
  nodeId: number
  status: string
  output: TaskOutput
}

export interface SubmitTaskResponse {
  taskId: number
  stage: string
  nodeId: number
  score: number
  normalizedScore: number
  errorTags: string[]
  feedback: {
    diagnosis: string
    fixes: string[]
  }
  strengths: string[]
  weaknesses: string[]
  masteryBefore: number
  masteryDelta: number
  masteryAfter: number
  nextAction: string
  nextTask: NextTask | null
}

export interface CurrentSessionInfo {
  sessionId: number
  userId: string
  courseId: string
  chapterId: string
  goalText: string
  currentNodeId: number
  currentStage: string
}

export interface CurrentSessionResponse {
  hasActiveSession: boolean
  session: CurrentSessionInfo | null
}

export interface PathNode {
  nodeId: number
  nodeName: string
  status: string
  masteryValue: number
}

export interface PathResponse {
  sessionId: number
  currentNodeId: number
  nodes: PathNode[]
}

export interface TaskDetailResponse {
  taskId: number
  sessionId: number
  nodeId: number
  nodeName: string
  stage: string
  objective: string
  status: string
  hasOutput: boolean
  output: TaskOutput
}
