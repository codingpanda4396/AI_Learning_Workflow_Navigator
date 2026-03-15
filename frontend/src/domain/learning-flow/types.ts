/**
 * 学习执行阶段统一域模型：基于 sessionId 的单向流，taskId 仅作为内部数据。
 */

export type LearningStage =
  | 'DIAGNOSIS'
  | 'PLAN_PREVIEW'
  | 'LEARNING_TASK'
  | 'TRAINING'
  | 'EVALUATION'
  | 'NEXT_ACTION';

/** 当前主 CTA：用于导航/按钮文案与跳转目标 */
export interface FlowPrimaryCTA {
  label: string;
  stage: LearningStage;
  /** 仅用于 UI 展示，不用于路由决策 */
  hint?: string;
}

/** 任务摘要（来自 overview/nextTask 或当前运行任务） */
export interface FlowTaskSummary {
  taskId: number;
  stage: string;
  nodeId?: number;
  title?: string;
  description?: string;
}

/** 练习状态（来自 quiz status） */
export interface FlowTrainingState {
  canEnter: boolean;
  status: 'idle' | 'generating' | 'ready' | 'answering' | 'submitting' | 'report-ready' | 'next-round' | 'failed';
  questionCount: number;
}

/** 评估状态（来自 report） */
export interface FlowEvaluationState {
  canEnter: boolean;
  hasReport: boolean;
  recommendedAction?: string;
}

/** 错误/阻塞态 */
export interface FlowBlockedState {
  blocked: boolean;
  reason?: string;
  recoverable?: boolean;
}

/** 统一 flow snapshot：由 mapper 从 overview/task/quiz/report 聚合 */
export interface LearningFlowSnapshot {
  sessionId: number;
  currentStage: LearningStage;
  currentTaskId: number | null;
  taskSummary: FlowTaskSummary | null;
  canEnterTask: boolean;
  canEnterTraining: boolean;
  canEnterEvaluation: boolean;
  primaryCTA: FlowPrimaryCTA | null;
  training: FlowTrainingState;
  evaluation: FlowEvaluationState;
  blocked: FlowBlockedState;
  /** 兼容：保留后端给的 path，UI 不得单独据此跳转 */
  legacyPrimaryActionPath?: string;
}

export type FlowStatus = 'idle' | 'loading' | 'ready' | 'blocked' | 'error';
