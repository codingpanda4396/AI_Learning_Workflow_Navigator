/**
 * DTO 与领域模型 - 与 API 文档对齐
 */
import type {
  TimeBudgetType,
  SelfReportedLevelType,
  PreferenceTagType,
  GoalTypeType,
  TaskCompletionStatusType,
  NextActionTypeType,
} from './enums'

// --- 1. 目标相关 ---
export interface CreateGoalRequest {
  rawGoalText: string
  timeBudget?: TimeBudgetType
  selfReportedLevel?: SelfReportedLevelType
  preferenceTags?: PreferenceTagType[]
  goalTypeHint?: GoalTypeType
  subjectHint?: string
  topicHints?: string[]
  sourceContext?: string
  priorityModule?: string
}

export interface StructuredLearningGoal {
  rawGoalText: string
  normalizedGoalText?: string
  goalType: GoalTypeType
  subject?: string
  topicScopeType?: string
  topics?: string[]
  intentDescription?: string
  timeBudget?: TimeBudgetType
  urgencyLevel?: string
  expectedDepth?: string
  selfReportedLevel?: SelfReportedLevelType
  preferenceTags?: PreferenceTagType[]
  constraints?: string[]
  sourceContext?: string
  priorityModule?: string
}

export interface GoalContextSnapshot {
  structuredGoal?: StructuredLearningGoal
  requiresDiagnosis: boolean
  planningMode: string
  entryGranularity: string
  strategyHints?: string[]
  riskTags?: string[]
  explanationFocus?: string[]
  createdFrom?: string
  version?: number
}

export interface CreateGoalData {
  goalId: string
  structuredGoal: StructuredLearningGoal
  goalContextSnapshot: GoalContextSnapshot
}

export interface AuthUser {
  id: number
  username: string
  displayName: string
}

export interface RecentLearningEntry {
  goalId?: string
  diagnosisId?: string
  planId?: string
  sessionId?: string
  currentTaskId?: string
  sessionStatus?: string
}

export interface AuthMeResponse {
  authenticated: boolean
  user?: AuthUser | null
  recentLearningEntry?: RecentLearningEntry | null
}

export interface LoginRequest {
  username: string
  password: string
}

export interface RegisterRequest {
  username: string
  password: string
}

// --- 2. 诊断相关 ---
export interface DiagnosisOption {
  code: string
  label: string
  order?: number
}

export interface DiagnosisQuestion {
  questionId: string
  dimension: string
  type: string
  required: boolean
  title: string
  description?: string
  whyAsking?: string
  impactsPlanning?: string[]
  options: DiagnosisOption[]
}

export interface DiagnosisAnswer {
  questionId: string
  selectedOptions: string[]
  textAnswer?: string
}

export interface DiagnosisSessionData {
  diagnosisId: string
  sessionId: string
  status: string
  generationMode: string
  questions: DiagnosisQuestion[]
}

export interface LearnerProfileSnapshot {
  diagnosisId: string
  foundationLevel?: string
  executionStability?: string
  timeBudgetLevel?: string
  learningPreference?: string
  blockingPoint?: string
  urgencyLevel?: string
  blockerTags?: string[]
  riskTags?: string[]
}

export interface DiagnosisEvidenceSummary {
  summary?: string
  keyEvidence?: string[]
  primaryGapType?: string
  primaryRiskTags?: string[]
  explanationPoints?: string[]
}

export interface SubmitDiagnosisData {
  diagnosisId: string
  learnerProfileSnapshot: LearnerProfileSnapshot
  diagnosisEvidenceSummary: DiagnosisEvidenceSummary
}

// --- 3. 规划相关 ---
export interface RecommendedEntry {
  conceptId?: string
  title: string
  estimatedMinutes?: number
  reason?: string
}

export interface RecommendedStrategy {
  code: string
  label?: string
  reason?: string
}

export interface PlanStage {
  stageCode: string
  title: string
  objective?: string
  estimatedMinutes?: number
}

export interface TaskBlueprint {
  taskId: string
  title: string
  taskType: string
  goal: string
  taskMethod?: string
  recommendedPromptTemplate?: string
  estimatedMinutes?: number
  promptScaffold?: string
  completionCriteria?: string[]
  evidenceToCollect?: string[]
  selfEvaluationQuestions?: string[]
  fallbackAction?: string
}

export interface PlanPreviewData {
  planId: string
  status: string
  previewOnly: boolean
  committed: boolean
  goal?: string
  recommendedEntry: RecommendedEntry
  recommendedStrategy: RecommendedStrategy
  stages: PlanStage[]
  tasks: TaskBlueprint[]
  successCriteria?: string[]
  keyEvidence?: string[]
  risks?: string[]
}

export interface CommitPlanData {
  sessionId: string
  planId: string
  taskSequence: string[]
  currentTaskId: string
  status: string
}

// --- 4. 任务执行 ---
export interface CurrentTaskItem {
  taskId: string
  title: string
  taskType: string
  goal: string
  whyThisTask?: string
  taskMethod?: string
  recommendedPromptTemplate?: string
  estimatedMinutes?: number
  promptScaffold?: string
  completionCriteria?: string[]
  selfEvaluationQuestions?: string[]
  fallbackAction?: string
}

export interface ProgressItem {
  currentIndex: number
  totalTasks: number
}

export interface CurrentTaskData {
  sessionId: string
  currentTask: CurrentTaskItem
  progress: ProgressItem
}

export type SelfRatedConfidenceType = 'LOW' | 'MEDIUM' | 'HIGH'

export interface CompleteTaskRequest {
  sessionId: string
  completionStatus: TaskCompletionStatusType
  durationMinutes?: number
  interactionCount?: number
  userSummarySubmitted?: boolean
  microPracticeResult?: string
  detectedIssueTags?: string[]
  behaviorSignals?: string[]
  learnerReflection?: string
  /** Sprint 4 脚手架任务收束（与后端 TaskClosureValidator 对齐） */
  summaryText?: string
  learnedFrameworkPoints?: string[]
  nextPracticeIntent?: string
  selfRatedConfidence?: SelfRatedConfidenceType
  closurePayloadVersion?: string
}

export interface TaskExecutionRecord {
  taskId: string
  taskType: string
  completionStatus: string
  durationMinutes?: number
  interactionCount?: number
  userSummarySubmitted?: boolean
  microPracticeResult?: string
  detectedIssueTags?: string[]
  behaviorSignals?: string[]
  learnerReflection?: string
}

export interface SessionProgressItem {
  completedTasks: number
  totalTasks: number
}

export interface CompleteTaskData {
  taskExecutionRecord: TaskExecutionRecord
  nextTaskAvailable: boolean
  nextTaskId?: string
  sessionProgress?: SessionProgressItem
}

export interface ScaffoldPromptItem {
  promptId?: string
  prompt: string
  intent?: string
  required: boolean
}

export interface CognitiveUnitItem {
  unitId: string
  order: number
  label?: string
  learningObjective: string
  targetOutcome: string
  failureSignal: string
  actionBullets?: string[]
  prompts?: ScaffoldPromptItem[]
}

export interface TaskScaffoldResponse {
  taskId: string
  taskType: string
  /** 任务级认知意图（与 learningObjective 同源时可并存） */
  taskLevelLearningIntent?: string
  learningObjective: string
  whyThisTask?: string
  /** 认知推进单元 */
  cognitiveUnits?: CognitiveUnitItem[]
  recommendedAskTemplates?: string[]
  recommendedFollowupTemplates?: string[]
  selfCheckTemplates?: string[]
  fallbackHints?: string[]
  completionSignals?: string[]
  antiPatterns?: string[]
  currentExecutionState: string
  executionSnapshot?: {
    currentState: string
    exploreTurnCount: number
    checkpointQuestion?: string
    canComplete: boolean
  }
  recentMessages?: {
    role: 'USER' | 'ASSISTANT' | 'SYSTEM'
    content: string
    detectedAction?: string
    createdAt?: string
  }[]
}

export interface RecommendedUserActionItem {
  code: string
  label: string
}

export interface CurrentGuidanceBlock {
  title: string
  bullets?: string[]
}

export interface CurrentTaskGuidanceData {
  sessionId: string
  taskId: string
  taskExecutionState: string
  guidancePhase?: string | null
  currentGuidance?: CurrentGuidanceBlock | null
  recommendedUserActions?: RecommendedUserActionItem[]
  policyVersion?: string | null
}

export interface TaskMessageResponse {
  assistantReply: string
  detectedAction: string
  taskState: string
  nextSuggestedPrompts?: string[]
  fallbackMode?: string
  guidancePhase?: string | null
  recommendedUserActions?: RecommendedUserActionItem[]
  whetherCanComplete?: boolean | null
}

export interface SelfExplanationResponse {
  evaluation: string
  missingPoints?: string[]
  nextAction?: string
  taskState: string
  checkpointQuestion?: string
}

export interface CheckpointResponse {
  result: string
  reason?: string
  suggestedRemedialAction?: string | null
  taskState: string
}

// --- 5. 报告与 Next Action ---
export interface NextActionDecision {
  actionType: NextActionTypeType
  reason?: string
  nextEntryPoint?: string
  adjustmentSignals?: string[]
  requiresReplan: boolean
}

export interface LearningMethodProfile {
  sessionId?: string
  taskId?: string
  questioningQuality?: string
  selfExplanationPerformed?: boolean
  selfExplanationQuality?: string
  checkPassed?: boolean
  antiPatternObserved?: string[]
  positiveSignals?: string[]
  dominantActionTypes?: string[]
  nextMethodAdvice?: string[]
}

export interface LearningReport {
  sessionId: string
  resultStatus: string
  goalReview?: string
  completedProgress?: string[]
  unresolvedIssues?: string[]
  evidenceSummary?: string[]
  summaryText?: string
  nextAction?: NextActionDecision
  learningMethodProfile?: LearningMethodProfile
}

export interface ReportData {
  learningReport: LearningReport
  nextActionDecision: NextActionDecision
}

export interface NextActionConfirmData {
  sessionId: string
  acceptedAction: NextActionTypeType
  requiresReplan: boolean
  nextHint?: string
}

// --- GlobalResponse ---
export interface GlobalResponse<T> {
  code: string
  message: string
  data: T | null
}
