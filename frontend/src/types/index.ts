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
  courseId: string
  chapterId: string
  goalText: string
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
  generationMode?: string
  generationReason?: string
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

export type TutorMessageRole = 'user' | 'assistant'

export interface TutorMessage {
  id: number
  sessionId: number
  taskId: number
  role: TutorMessageRole
  content: string
  createdAt: string
}

export interface TutorMessageListResponse {
  sessionId: number
  taskId: number
  messages: TutorMessage[]
}

export interface TutorSendMessageResponse {
  sessionId: number
  taskId: number
  userMessage: TutorMessage
  assistantMessage: TutorMessage
}

export interface AuthUser {
  id: number
  username: string
}

export interface LoginRequest {
  username: string
  password: string
}

export interface RegisterRequest {
  username: string
  password: string
}

export interface SessionHistoryItem {
  sessionId: number
  course: string
  chapter: string
  goal: string
  status: string
  progress: Progress | null
  lastActiveAt: string
}

export interface SessionHistoryResponse {
  page: number
  pageSize: number
  total: number
  totalPages: number
  items: SessionHistoryItem[]
}

export interface PracticeItem {
  itemId: number
  questionType: string
  stem: string
  options: unknown
  difficulty: string
  source: string
  status: string
}

export interface PracticeItemsResponse {
  sessionId: number
  taskId: number
  itemCount: number
  items: PracticeItem[]
}

export interface PracticeSubmission {
  submissionId: number
  practiceItemId: number
  userAnswer: string
  score: number | null
  isCorrect: boolean | null
  feedback: string
  errorTags: string[]
  submittedAt: string
}

export interface PracticeSubmissionsResponse {
  sessionId: number
  taskId: number
  submissionCount: number
  submissions: PracticeSubmission[]
}

export interface PracticeJudgement {
  score: number | null
  isCorrect: boolean | null
  feedback: string
  errorTags: string[]
}

export interface SubmitPracticeAnswerResponse {
  submission: PracticeSubmission
  practiceItem: PracticeItem
  judgement: PracticeJudgement
}

export interface WeakPointNode {
  nodeId: number
  nodeName: string
  masteryScore: number
  trainingAccuracy: number
  latestEvaluationScore: number | null
  attemptCount: number
  recentErrorTags: string[]
  reasons: string[]
}

export interface LearningFeedbackResponse {
  sessionId: number
  diagnosisSummary: string
  weakNodes: WeakPointNode[]
}
