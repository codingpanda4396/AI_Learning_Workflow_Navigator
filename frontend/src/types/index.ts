// API Types based on spec/02_api_contract.md

export type Stage = 'STRUCTURE' | 'UNDERSTANDING' | 'TRAINING' | 'REFLECTION'
export type TaskStatus = 'PENDING' | 'RUNNING' | 'SUCCEEDED' | 'FAILED'
export type ErrorTag = 'CONCEPT_CONFUSION' | 'MISSING_STEPS' | 'BOUNDARY_CASE' | 'TERMINOLOGY' | 'SHALLOW_REASONING' | 'MEMORY_GAP'
export type NextAction = 'INSERT_REMEDIAL_UNDERSTANDING' | 'INSERT_TRAINING_VARIANTS' | 'INSERT_TRAINING_REINFORCEMENT' | 'ADVANCE_TO_NEXT_NODE' | 'NOOP'

// Create Session
export interface CreateSessionRequest {
  user_id: string
  course_id: string
  chapter_id: string
  goal_text: string
}

export interface CreateSessionResponse {
  session_id: number
}

// Plan Session
export interface PlannedTask {
  task_id: number
  stage: Stage
  node_id: number
  objective: string
  status: TaskStatus
}

export interface PlanSessionResponse {
  session_id: number
  tasks: PlannedTask[]
}

// Session Overview
export interface TimelineItem {
  task_id: number
  stage: Stage
  node_id: number
  status: TaskStatus
}

export interface NextTask {
  task_id: number
  stage: Stage
  node_id: number
}

export interface MasterySummary {
  node_id: number
  node_name: string
  mastery_value: number
}

export interface SessionOverviewResponse {
  session_id: number
  course_id: string
  chapter_id: string
  goal_text: string
  current_node_id: number
  current_stage: Stage
  timeline: TimelineItem[]
  next_task: NextTask
  mastery_summary: MasterySummary[]
}

// Run Task
export interface TaskOutputSection {
  type: 'concepts' | 'mechanism' | 'misconceptions' | 'summary'
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
  task_id: number
  stage: Stage
  node_id: number
  status: TaskStatus
  output: TaskOutput
}

// Submit Task
export interface SubmitTaskRequest {
  user_answer: string
}

export interface FeedbackFix {
  fix: string
}

export interface Feedback {
  diagnosis: string
  fixes: FeedbackFix[]
}

export interface SubmitTaskResponse {
  task_id: number
  stage: Stage
  node_id: number
  score: number
  error_tags: ErrorTag[]
  feedback: Feedback
  mastery_before: number
  mastery_delta: number
  mastery_after: number
  next_action: NextAction
  next_task: NextTask
}

// Error Response
export interface ApiError {
  error: string
  message: string
}
