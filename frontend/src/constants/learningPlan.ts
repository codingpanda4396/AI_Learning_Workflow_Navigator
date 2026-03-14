import type {
  LearningIntensity,
  LearningMode,
  LearningStage,
  PathDifficulty,
  PathMasteryStatus,
  PlanAdjustments,
} from '@/types/learningPlan';

export const STAGE_LABELS: Record<LearningStage, string> = {
  STRUCTURE: 'Structure',
  UNDERSTANDING: 'Understanding',
  TRAINING: 'Training',
  REFLECTION: 'Reflection',
};

export const STAGE_ORDER: LearningStage[] = ['STRUCTURE', 'UNDERSTANDING', 'TRAINING', 'REFLECTION'];

export const INTENSITY_LABELS: Record<LearningIntensity, string> = {
  LIGHT: 'Light',
  STANDARD: 'Standard',
  INTENSIVE: 'Intensive',
};

export const LEARNING_MODE_LABELS: Record<LearningMode, string> = {
  EXPLAIN_THEN_PRACTICE: 'Explain then practice',
  LEARN_BY_DOING: 'Learn by doing',
};

export const PATH_STATUS_LABELS: Record<PathMasteryStatus, string> = {
  WEAK: 'Needs reinforcement',
  PARTIAL: 'Partially ready',
  STABLE: 'Stable',
  NEW: 'New in this round',
};

export const PATH_DIFFICULTY_LABELS: Record<PathDifficulty, string> = {
  FOUNDATION: 'Foundation',
  CORE: 'Core',
  CHALLENGE: 'Challenge',
};

export const DEFAULT_PLAN_ADJUSTMENTS: PlanAdjustments = {
  intensity: 'STANDARD',
  learningMode: 'EXPLAIN_THEN_PRACTICE',
  prioritizeFoundation: true,
};
