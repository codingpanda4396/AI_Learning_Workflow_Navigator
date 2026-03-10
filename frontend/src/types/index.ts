export type PageLevelFlow = 'create-session' | 'learning-session' | 'feedback-review'
export type LearningStage = 'STRUCTURE' | 'UNDERSTANDING' | 'TRAINING' | 'EVALUATION'
export type LegacyLearningStage = 'REFLECTION'
export type Stage = LearningStage | LegacyLearningStage | 'UNKNOWN'
export type TaskStatus = 'PENDING' | 'RUNNING' | 'SUCCEEDED' | 'FAILED'
export type SessionStatus =
  | 'GENERATING'
  | 'CREATED'
  | 'EVALUATED'
  | 'PLANNED'
  | 'LEARNING'
  | 'QUIZ_READY'
  | 'ANSWERED'
  | 'FEEDBACK_READY'
  | 'REVIEWING'
  | 'NEXT_ROUND'
  | 'FAILED'
export type StepStatus = 'LOCKED' | 'AVAILABLE' | 'ACTIVE' | 'DONE' | 'ERROR'
export type AsyncStatus = 'IDLE' | 'PENDING' | 'RUNNING' | 'SUCCEEDED' | 'FAILED'
export type DisplayStatus = SessionStatus | StepStatus | TaskStatus
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

export interface PlannedNodeStage {
  taskId: number
  stage: string
  objective: string
  status: string
}

export interface PlannedNode {
  nodeId: number
  nodeName: string
  status: string
  stages: PlannedNodeStage[]
}

export interface PlanSessionResponse {
  sessionId: number
  plans: PlannedNode[]
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
  questionId: number
  type: string
  stem: string
  options: unknown
  evaluationFocus: string
  difficulty: string
  status: string
}

export type PracticeQuizStatus =
  | 'GENERATING'
  | 'QUIZ_READY'
  | 'ANSWERED'
  | 'FEEDBACK_READY'
  | 'REVIEWING'
  | 'NEXT_ROUND'
  | 'FAILED'

export interface PracticeQuizResponse {
  quizId: number
  sessionId: number
  taskId: number
  generationStatus: TaskStatus
  quizStatus: PracticeQuizStatus
  questionCount: number
  answeredCount: number
  failureReason: string
  retryable: boolean
  questions: PracticeItem[]
}

export interface PracticeQuestionResult {
  questionId: number
  type: string
  stem: string
  userAnswer: string
  score: number | null
  correct: boolean
  feedback: string
  errorTags: string[]
}

export interface PracticeFeedbackReport {
  reportId: number
  quizId: number
  sessionId: number
  taskId: number
  reportStatus: TaskStatus
  overallSummary: string
  questionResults: PracticeQuestionResult[]
  strengths: string[]
  weaknesses: string[]
  reviewFocus: string[]
  nextRoundAdvice: string
  suggestedNextAction: string
  recommendedAction: 'REVIEW' | 'NEXT_ROUND'
  selectedAction: 'REVIEW' | 'NEXT_ROUND' | ''
  source: string
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
