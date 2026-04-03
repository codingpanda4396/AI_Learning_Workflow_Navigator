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
  knowledgeKey?: string
  packId?: string
  knowledgeType?: 'COMPARE' | 'SEQUENCE' | 'CHOICE' | 'MECHANISM'
  displayMode?: string
  phaseHighlights?: string[]
  commonMisconceptions?: string[]
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
  currentPhase?: string
  nextRoute?: string
}
