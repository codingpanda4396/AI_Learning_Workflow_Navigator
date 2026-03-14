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
  LEARN_BY_DOING: '在做中学',
};

export const PATH_STATUS_LABELS: Record<PathMasteryStatus, string> = {
  WEAK: '需要强化',
  PARTIAL: '部分掌握',
  STABLE: '已稳定',
  NEW: '本轮新增',
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
