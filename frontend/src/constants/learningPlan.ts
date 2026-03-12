import type {
  LearningIntensity,
  LearningMode,
  LearningStage,
  PathDifficulty,
  PathMasteryStatus,
  PlanAdjustments,
} from '@/types/learningPlan';

export const STAGE_LABELS: Record<LearningStage, string> = {
  STRUCTURE: '建立框架',
  UNDERSTANDING: '理解原理',
  TRAINING: '训练应用',
  REFLECTION: '评估反思',
};

export const STAGE_ORDER: LearningStage[] = ['STRUCTURE', 'UNDERSTANDING', 'TRAINING', 'REFLECTION'];

export const INTENSITY_LABELS: Record<LearningIntensity, string> = {
  LIGHT: '轻量',
  STANDARD: '标准',
  INTENSIVE: '强化',
};

export const LEARNING_MODE_LABELS: Record<LearningMode, string> = {
  EXPLAIN_THEN_PRACTICE: '先讲解后练习',
  LEARN_BY_DOING: '边学边练',
};

export const PATH_STATUS_LABELS: Record<PathMasteryStatus, string> = {
  WEAK: '当前薄弱',
  PARTIAL: '已有基础',
  STABLE: '相对稳定',
  NEW: '本轮新进入',
};

export const PATH_DIFFICULTY_LABELS: Record<PathDifficulty, string> = {
  FOUNDATION: '基础',
  CORE: '核心',
  CHALLENGE: '进阶',
};

export const DEFAULT_PLAN_ADJUSTMENTS: PlanAdjustments = {
  intensity: 'STANDARD',
  learningMode: 'EXPLAIN_THEN_PRACTICE',
  prioritizeFoundation: true,
};
