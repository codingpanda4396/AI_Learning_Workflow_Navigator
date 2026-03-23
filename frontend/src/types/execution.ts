export type ExecutionState =
  | 'INIT'
  | 'PROMPT_SHOWN'
  | 'USER_CONFIRMED'
  | 'AI_RESPONSE_SHOWN'
  | 'THINKING_DONE'
  | 'USER_SUBMITTED'
  | 'FEEDBACK_SHOWN'
  | 'STEP_COMPLETED'

/**
 * R0003 导师执行流语义别名（值与 R0002 状态机一致，仅便于阅读与检索）。
 */
export const ExecutionPhaseR0003 = {
  AI_PROMPT: 'PROMPT_SHOWN',
  AI_USER_CONFIRMED: 'USER_CONFIRMED',
  AI_EXPLAIN: 'AI_RESPONSE_SHOWN',
  WAIT_INPUT: 'THINKING_DONE',
  AI_FEEDBACK_PENDING: 'USER_SUBMITTED',
  AI_FEEDBACK: 'FEEDBACK_SHOWN',
  STEP_COMPLETED: 'STEP_COMPLETED',
} as const satisfies Record<string, ExecutionState>

export interface ExecutionStepFeedback {
  correct: boolean
  comment: string
  suggestion: string
  /** 分层反馈：肯定（优先展示） */
  praise?: string | null
  /** 分层反馈：缺口提醒 */
  gap?: string | null
  /** 分层反馈：建议补充 */
  nextHint?: string | null
}

export interface ExecutionStep {
  stepId: string
  /** 传给后端上下文的稳定知识点 key（如 binary_tree） */
  knowledgeKey?: string
  /** 传给 R0003 导师接口的知识点标签 */
  knowledgePoint?: string
  title: string
  goal: string
  prompt: string
  reflectionQuestions: string[]
  /** 「为什么这样问」标题 */
  promptWhyTitle?: string
  /** 「为什么这样问」要点 */
  promptWhyBullets?: string[]
  /** 作答区副文案（强调学习方法，非知识点灌输） */
  inputHint?: string
  inputPlaceholder?: string
  /** 本步完成时的成就标题 */
  completionHeadline?: string
  completionAchievements?: string[]
  /** 下一步预告 */
  completionNextHint?: string
  userAnswer?: string
  feedback?: ExecutionStepFeedback
  state: ExecutionState
}

export interface TaskFeedbackResponse {
  correct: boolean
  comment: string
  suggestion: string
  praise?: string | null
  gap?: string | null
  nextHint?: string | null
  /** R0003：LLM | FALLBACK */
  source?: string | null
}
