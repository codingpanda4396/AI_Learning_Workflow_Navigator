import type {
  TimeBudgetType,
  SelfReportedLevelType,
  PreferenceTagType,
  GoalTypeType,
} from '../enums'

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
