import type {
  LearningIntensity,
  LearningMode,
  LearningStage,
  PathDifficulty,
  PathMasteryStatus,
  PlanAdjustments,
} from '@/types/learningPlan';

export const STAGE_LABELS: Record<LearningStage, string> = {
  STRUCTURE: '搭建结构',
  UNDERSTANDING: '理解原理',
  TRAINING: '进入训练',
  REFLECTION: '复盘巩固',
};

export const STAGE_ORDER: LearningStage[] = ['STRUCTURE', 'UNDERSTANDING', 'TRAINING', 'REFLECTION'];

export const INTENSITY_LABELS: Record<LearningIntensity, string> = {
  LIGHT: '轻松',
  STANDARD: '标准',
  INTENSIVE: '强化',
};

export const LEARNING_MODE_LABELS: Record<LearningMode, string> = {
  EXPLAIN_THEN_PRACTICE: '先讲解后练习',
  LEARN_BY_DOING: '边做边学',
};

export const PATH_STATUS_LABELS: Record<PathMasteryStatus, string> = {
  WEAK: '需要优先补强',
  PARTIAL: '已有部分基础',
  STABLE: '已经相对稳定',
  NEW: '本轮新接触',
};

export const PATH_DIFFICULTY_LABELS: Record<PathDifficulty, string> = {
  FOUNDATION: '基础',
  CORE: '核心',
  CHALLENGE: '挑战',
};

export const DEFAULT_PLAN_ADJUSTMENTS: PlanAdjustments = {
  intensity: 'STANDARD',
  learningMode: 'EXPLAIN_THEN_PRACTICE',
  prioritizeFoundation: true,
};
