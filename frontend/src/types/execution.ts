export type ExecutionState =
  | 'INIT'
  | 'PROMPT_SHOWN'
  | 'USER_CONFIRMED'
  | 'THINKING_DONE'
  | 'USER_SUBMITTED'
  | 'FEEDBACK_SHOWN'
  | 'STEP_COMPLETED'

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
}
