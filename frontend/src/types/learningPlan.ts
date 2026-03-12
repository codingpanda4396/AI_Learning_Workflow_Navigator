export type LearningStage = 'STRUCTURE' | 'UNDERSTANDING' | 'TRAINING' | 'REFLECTION';

export type LearningIntensity = 'LIGHT' | 'STANDARD' | 'INTENSIVE';

export type LearningMode = 'EXPLAIN_THEN_PRACTICE' | 'LEARN_BY_DOING';

export type PathMasteryStatus = 'WEAK' | 'PARTIAL' | 'STABLE' | 'NEW';

export type PathDifficulty = 'FOUNDATION' | 'CORE' | 'CHALLENGE';

export interface PlanAdjustments {
  intensity: LearningIntensity;
  learningMode: LearningMode;
  prioritizeFoundation: boolean;
}

export interface LearningPlanRequest {
  sessionId?: number;
  goalId: string;
  diagnosisId: string;
  goalText: string;
  courseId: string;
  chapterId: string;
  adjustments: PlanAdjustments;
}

export interface PlanSummary {
  recommendedStart: string;
  recommendedRhythm: LearningIntensity;
  estimatedMinutes: number;
  estimatedKnowledgeCount: number;
  stageCount: number;
  personalizedHeadline: string;
  personalizedSummary: string;
}

export interface PlanReason {
  key: string;
  title: string;
  label: string;
  description: string;
}

export interface PlanPathNode {
  id: string;
  name: string;
  masteryStatus: PathMasteryStatus;
  difficulty: PathDifficulty;
  reasonTags: string[];
  estimatedMinutes: number;
  isStartingPoint: boolean;
  isPrerequisite: boolean;
  isFocus: boolean;
}

export interface PlanTaskPreview {
  stage: LearningStage;
  stageGoal: string;
  learnerAction: string;
  aiSupport: string;
  estimatedMinutes: number;
}

export interface LearningPlanPreview {
  planId: number;
  summary: PlanSummary;
  reasons: PlanReason[];
  pathNodes: PlanPathNode[];
  taskPreviews: PlanTaskPreview[];
  adjustments: PlanAdjustments;
  goalText: string;
  courseId: string;
  chapterId: string;
  diagnosisSummary: string;
  nextStepNote: string;
}

export interface PlanConfirmResult {
  planId?: number;
  sessionId: number;
  currentNodeId?: number;
  firstTaskId?: number;
  nextPage?: string;
}
