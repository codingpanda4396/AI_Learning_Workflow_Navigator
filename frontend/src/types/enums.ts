/**
 * 枚举常量 - 与后端保持一致
 */
export const TimeBudget = {
  WITHIN_15_MIN: 'WITHIN_15_MIN',
  WITHIN_30_MIN: 'WITHIN_30_MIN',
  WITHIN_60_MIN: 'WITHIN_60_MIN',
  MULTI_DAY: 'MULTI_DAY',
  LONG_TERM: 'LONG_TERM',
} as const

export const SelfReportedLevel = {
  BEGINNER: 'BEGINNER',
  BASIC: 'BASIC',
  PARTIAL_UNDERSTANDING: 'PARTIAL_UNDERSTANDING',
  CAN_EXPLAIN_BUT_NOT_APPLY: 'CAN_EXPLAIN_BUT_NOT_APPLY',
  SOLID_BUT_WANT_IMPROVE: 'SOLID_BUT_WANT_IMPROVE',
} as const

export const PreferenceTag = {
  CONCEPT_FIRST: 'CONCEPT_FIRST',
  EXAMPLE_FIRST: 'EXAMPLE_FIRST',
  PRACTICE_FIRST: 'PRACTICE_FIRST',
  STEP_BY_STEP: 'STEP_BY_STEP',
  FAST_TRACK: 'FAST_TRACK',
  FRAMEWORK_FIRST: 'FRAMEWORK_FIRST',
} as const

export const GoalType = {
  LEARN_NEW_CONCEPT: 'LEARN_NEW_CONCEPT',
  REVIEW_FOR_EXAM: 'REVIEW_FOR_EXAM',
  FIX_SPECIFIC_BLOCKER: 'FIX_SPECIFIC_BLOCKER',
  PRACTICE_ENHANCEMENT: 'PRACTICE_ENHANCEMENT',
  BUILD_SYSTEMATIC_UNDERSTANDING: 'BUILD_SYSTEMATIC_UNDERSTANDING',
} as const

export const TaskCompletionStatus = {
  NOT_STARTED: 'NOT_STARTED',
  IN_PROGRESS: 'IN_PROGRESS',
  COMPLETED: 'COMPLETED',
  SKIPPED: 'SKIPPED',
  BLOCKED: 'BLOCKED',
} as const

export const ResultStatus = {
  ACHIEVED: 'ACHIEVED',
  PARTIALLY_ACHIEVED: 'PARTIALLY_ACHIEVED',
  NOT_ACHIEVED: 'NOT_ACHIEVED',
} as const

export const NextActionType = {
  CONTINUE: 'CONTINUE',
  REINFORCE: 'REINFORCE',
  REMEDIATE_PREREQUISITE: 'REMEDIATE_PREREQUISITE',
  REDUCE_GRANULARITY: 'REDUCE_GRANULARITY',
  CHANGE_STRATEGY: 'CHANGE_STRATEGY',
} as const

export type TimeBudgetType = (typeof TimeBudget)[keyof typeof TimeBudget]
export type SelfReportedLevelType = (typeof SelfReportedLevel)[keyof typeof SelfReportedLevel]
export type PreferenceTagType = (typeof PreferenceTag)[keyof typeof PreferenceTag]
export type GoalTypeType = (typeof GoalType)[keyof typeof GoalType]
export type TaskCompletionStatusType = (typeof TaskCompletionStatus)[keyof typeof TaskCompletionStatus]
export type ResultStatusType = (typeof ResultStatus)[keyof typeof ResultStatus]
export type NextActionTypeType = (typeof NextActionType)[keyof typeof NextActionType]
