import type { NextActionTypeType } from '../enums'

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

export interface LearningMethodReview {
  headline?: string
  summary?: string
  strengths?: string[]
  risks?: string[]
  nextFocus?: string[]
}

export interface RecommendedNextStep {
  actionType: NextActionTypeType
  title?: string
  reason?: string
  actionLabel?: string
  nextEntryPoint?: string
  signals?: string[]
  requiresReplan: boolean
}

export interface TaskHighlight {
  taskId: string
  title: string
  completionStatus: string
  learned?: string
  issue?: string
}

export interface LearningReport {
  sessionId: string
  resultStatus: string
  goalReview?: string
  finalSummary?: string
  whatYouLearned?: string[]
  whatStillNeedsWork?: string[]
  evidenceDigest?: string[]
  learningMethodReview?: LearningMethodReview
  recommendedNextStep?: RecommendedNextStep
  taskHighlights?: TaskHighlight[]
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
