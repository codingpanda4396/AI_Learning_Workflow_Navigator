import type { TaskCompletionStatusType } from '../enums'
import type { ReflectionSummary } from '../scaffoldEngine'

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

/** GET /api/sessions/:id/current-task — 仅元信息与阶段进度 */
export interface CurrentTaskData {
  sessionId: string
  taskId: string
  knowledge: string
  currentStage: string | null
  progressMap: Record<string, boolean>
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
  unresolvedQuestions?: string[]
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
  sessionStatus?: string
  currentPhase?: string
  nextRoute?: string
  reportReady?: boolean
}

export interface SessionFlowState {
  sessionId: string
  sessionStatus: string
  currentPhase: string
  currentRoute: string
  currentTaskId?: string | null
  reportReady: boolean
  completedTaskCount: number
  totalTaskCount: number
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
  knowledgeKey?: string
  packId?: string
  knowledgeType?: 'COMPARE' | 'SEQUENCE' | 'CHOICE' | 'MECHANISM'
  scaffoldType?: string
  starterPrompts?: string[]
  checkpointMode?: string
  visualHintType?: string
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
  /** Phase 4：脚手架反思沉淀（DFS/BFS） */
  reflectionSummary?: ReflectionSummary
  phaseProgress?: {
    phases: string[]
    currentPhase: string
    overallRatio: number
    taskIndexLabel: string
    stepLabel: string
  }
  currentTaskCard?: {
    phaseCode: string
    phaseDisplay: string
    currentAction: string
    taskTitle: string
    objective: string
    whyNow: string
    outputRequirements: string[]
    completionCriteria: string[]
  }
  scaffoldGuide?: {
    sections: {
      id: string
      title: string
      description: string
      lightHint: string
      standardHint: string
      strongHint: string
    }[]
    observationBullets: string[]
  }
  expressionLayout?: {
    helperText: string
    fields: {
      id: string
      label: string
      placeholder: string
      multiline: boolean
    }[]
    lowFrictionPrompt: string
  }
  feedbackSchema?: {
    correctTitle: string
    missingTitle: string
    confusedTitle: string
    nextFixTitle: string
  }
  actionBar?: {
    hintActionLabel: string
    submitActionLabel: string
    nextActionLabel: string
  }
  tutorAssist?: {
    floatingLabel: string
    panelTitle: string
    quickQuestions: string[]
  }
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
  feedbackBoard?: {
    correct: string
    missing: string
    confused: string
    nextFix: string
    actions?: {
      id: string
      label: string
    }[]
  }
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
  feedbackBoard?: TaskMessageResponse['feedbackBoard']
}

export interface CheckpointResponse {
  result: string
  reason?: string
  suggestedRemedialAction?: string | null
  taskState: string
  feedbackBoard?: TaskMessageResponse['feedbackBoard']
}
