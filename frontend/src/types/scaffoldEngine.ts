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

export interface CompleteConversationStageResult {
  completedStageKey: string
  nextStageKey: string
  nextActionId: string
}

export interface CompleteReflectionStageResult {
  reflectionStageComplete: boolean
  completedStageKey: string
  nextStageKey: string | null
  nextActionId: string
}

export interface PromptScaffoldBlock {
  id: string
  title: string
  intent?: string
  prompt: string
  placeholder?: string
  constraint?: string
  maxLength?: number
  sentenceLimit?: number
  required?: boolean
  kind?: string
}

export interface PromptScaffold {
  blocks: PromptScaffoldBlock[]
}

export interface ExpressionSchemaPayload {
  mode: string
  fieldIds: string[]
  minChars?: number
  maxChars?: number
}

export interface WorkbenchFeedbackSchemaPayload {
  completenessLabel?: string
  issuePointsLabel?: string
  minimalRevisionLabel?: string
  nextActionLabel?: string
  maxIssuePoints?: number
}

/** 驱动脚手架工作台（与后端 StageScaffoldWorkbenchPayload 对齐） */
export interface StageScaffoldWorkbenchPayload {
  stageKey: string
  cognitiveAction: string
  stageGoal: string
  currentTaskTitle: string
  currentTaskInstruction: string
  deliverable: string
  completionCriteria: string[]
  promptScaffold: PromptScaffold
  expressionSchema: ExpressionSchemaPayload
  starterPrompts: string[]
  hintPrompts: string[]
  feedbackSchema?: WorkbenchFeedbackSchemaPayload
  llmGeneratedGuide?: string
  llmGeneratedMicroHint?: string
  llmGeneratedExampleBoundary?: string
  submitConstraint?: string
  emphasisMode: string
}

export interface StructuredScaffoldFeedbackPayload {
  completeness?: string
  issuePoints: string[]
  minimalRevision?: string
  nextAction?: string
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
  workbench?: StageScaffoldWorkbenchPayload
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
  feedbackPayload?: StructuredScaffoldFeedbackPayload
  /** 与 GET learning-scaffold/stage 一致；存在时无需再拉阶段 */
  updatedStage?: StageScaffold
}
