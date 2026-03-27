export interface LearningActionCard {
  actionId: string
  title: string
  goal: string
  singleAction?: string
  instructions: string
  systemPrompt?: string
  llmRole?: string
  userOutputLabel: string
  allowedPrompts: string[]
  forbiddenPrompts: string[]
  forbiddenActions?: string[]
  passCriteria: string[]
  completionCriteria?: string[]
  exampleOutput?: string
  nextActionHint?: string
}

export interface StructureSkeletonBlock {
  module: string
  prerequisites: string[]
  connections: string[]
  deferTopics: string[]
}

export interface StructureSkeletonResult {
  skeleton: StructureSkeletonBlock
  structureGenerationCount: number
  structureLightInteractionCount: number
  structureExploredPromptKeys: string[]
  canCompleteStructure: boolean
  lastPromptKey: string
}

export interface CompleteStructureStageResult {
  structureStageComplete: boolean
  nextStageKey: string
  nextActionId: string
}

export interface StageScaffold {
  stageKey: string
  stageTitle: string
  stageGoal: string
  phaseGoal?: string
  stageDescription: string
  actionCards: LearningActionCard[]
  validatorType: string
  tutorMode: string
  currentActionId?: string
  structureStageComplete?: boolean
  understandingStageComplete?: boolean
  trainingStageComplete?: boolean
  reflectionStageComplete?: boolean
  completedStageKeys?: string[]
  reflectionRecord?: ReflectionRecord
  reflectionInsight?: ReflectionInsight
  structureExploredPromptKeys?: string[]
  structureGenerationCount?: number
  structureLightInteractionCount?: number
  structureCanComplete?: boolean
  structureLastPromptKey?: string
}

export interface ActionRuntime {
  sessionId: string
  stageKey: string
  actionId: string
  userInput: string
  validationStatus: string
  tutorFeedback: string
  retryCount: number
  completed: boolean
  attemptNo?: number
  runtimeStatus?: string
  canProceed?: boolean
}

export interface ValidationResult {
  passed: boolean
  errorType?: string
  message: string
  suggestions?: string[]
  matchedAspects?: string[]
  missingAspects?: string[]
}

export interface TutorResponse {
  feedbackType: string
  content: string
  nextPrompt?: string
  canProceed: boolean
}

export interface ReflectionRecord {
  errorPattern?: string
  rootCause?: string
  decisionRule?: string
  capabilityName?: string
  futureStrategy?: string
}

export interface ReflectionInsight {
  repeatedErrorTypes?: string[]
  mostDifficultActionId?: string
  totalAttempts?: number
  improvedAspects?: string[]
}

export interface ReflectionSummary {
  record?: ReflectionRecord
  insight?: ReflectionInsight
  systemObservation?: string
}

export interface TrainingDetectedProblem {
  problemText: string
  errorType: string
  evidence?: string
  fixHint?: string
}

export interface TrainingFeedback {
  detectedProblems: TrainingDetectedProblem[]
  errorTypes: string[]
  revisionInstruction: string
  canProceed: boolean
}

export interface LearningScaffoldActionResult {
  actionRuntime: ActionRuntime
  validation: ValidationResult
  tutor: TutorResponse
  stageComplete: boolean
  trainingFeedback?: TrainingFeedback
  stageCompleted?: boolean
  actionCompleted?: boolean
  stageKey?: string
  actionId?: string
  attemptNo?: number
  runtimeStatus?: string
  canProceed?: boolean
  reflectionRecord?: ReflectionRecord
  reflectionInsight?: ReflectionInsight
}
