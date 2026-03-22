import type { DiagnosisAnswer } from '@/types/dto'

/**
 * 前端「3 题快速定位」→ 后端固定 6 题题库（DiagnosisQuestionBank）的标准答案。
 * 后端未改题目前，用映射保证 submit 的 questionId / option code 合法。
 */

export type FoundationUiId = 'fu_none' | 'fu_fuzzy' | 'fu_shaky' | 'fu_solid_practice'
export type BlockerUiId =
  | 'bk_concept'
  | 'bk_no_problem'
  | 'bk_error_prone'
  | 'bk_transfer'
export type PaceUiId = 'pc_tight' | 'pc_normal' | 'pc_relaxed'

export interface QuickDiagnosisUiState {
  foundation: FoundationUiId | null
  blocker: BlockerUiId | null
  pace: PaceUiId | null
}

/** 第 1 题：基础熟悉度 → q_foundation_state */
const FOUNDATION_TO_CODE: Record<FoundationUiId, string> = {
  fu_none: 'BEGINNER',
  fu_fuzzy: 'LEARNED_BUT_FORGOTTEN',
  fu_shaky: 'CAN_EXPLAIN_BUT_NOT_APPLY',
  fu_solid_practice: 'SOLID_WITH_LOCAL_GAPS',
}

/** 第 2 题：最大卡点 → q_primary_gap */
const BLOCKER_TO_CODE: Record<BlockerUiId, string> = {
  bk_concept: 'CONCEPT_GAP',
  bk_no_problem: 'PROCEDURE_GAP',
  bk_error_prone: 'QUESTION_TYPE_RECOGNITION_GAP',
  bk_transfer: 'RELATIONSHIP_GAP',
}

/** 第 3 题：时间策略 → 同时推导 goal / scope / preference / execution_risk */
const PACE_DERIVED: Record<
  PaceUiId,
  {
    executionRisk: string
    preferredEntry: string
    goalOutcome: string
    scope: string
  }
> = {
  pc_tight: {
    executionRisk: 'TIME_PRESSURE',
    preferredEntry: 'CONCEPT_FIRST',
    goalOutcome: 'PASS_THE_BASICS',
    scope: 'SINGLE_POINT',
  },
  pc_normal: {
    executionRisk: 'LOW_RISK',
    preferredEntry: 'EXAMPLE_FIRST',
    goalOutcome: 'SOLVE_TYPICAL_PROBLEMS',
    scope: 'MULTI_POINT',
  },
  pc_relaxed: {
    executionRisk: 'LOW_RISK',
    preferredEntry: 'CONCEPT_FIRST',
    goalOutcome: 'DEEP_UNDERSTANDING',
    scope: 'CHAPTER_LEVEL',
  },
}

export function mapQuickDiagnosisToAnswers(
  ui: QuickDiagnosisUiState
): DiagnosisAnswer[] | null {
  if (!ui.foundation || !ui.blocker || !ui.pace) return null
  const p = PACE_DERIVED[ui.pace]
  return [
    {
      questionId: 'q_foundation_state',
      selectedOptions: [FOUNDATION_TO_CODE[ui.foundation]],
    },
    {
      questionId: 'q_primary_gap',
      selectedOptions: [BLOCKER_TO_CODE[ui.blocker]],
    },
    { questionId: 'q_execution_risk', selectedOptions: [p.executionRisk] },
    { questionId: 'q_preferred_entry_mode', selectedOptions: [p.preferredEntry] },
    { questionId: 'q_goal_outcome', selectedOptions: [p.goalOutcome] },
    { questionId: 'q_scope_of_problem', selectedOptions: [p.scope] },
  ]
}
