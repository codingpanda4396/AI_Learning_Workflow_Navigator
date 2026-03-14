import type { CodeLabel } from '@/types/common';

export type LearningStage = 'STRUCTURE' | 'UNDERSTANDING' | 'TRAINING' | 'REFLECTION';
export type LearningIntensity = 'LIGHT' | 'STANDARD' | 'INTENSIVE';
export type LearningMode = 'EXPLAIN_THEN_PRACTICE' | 'LEARN_BY_DOING';
export type PathMasteryStatus = 'WEAK' | 'PARTIAL' | 'STABLE' | 'NEW';
export type PathDifficulty = 'FOUNDATION' | 'CORE' | 'CHALLENGE';
export type PlanPreviewStatus = 'PREVIEW_READY' | 'COMMITTED' | 'READY';
export type StrategyAdjustAction =
  | 'faster'
  | 'steadier'
  | 'practice-first'
  | 'ten-minute'
  | 'already-know'
  | 'not-enough-time'
  | 'not-clear';

export interface PlanAdjustments {
  intensity: LearningIntensity;
  learningMode: LearningMode;
  prioritizeFoundation: boolean;
}

export interface LearningPlanRequest {
  sessionId?: number;
  diagnosisId: string;
  goalText: string;
  courseName: string;
  chapterName: string;
  adjustments: PlanAdjustments;
}

export interface PlanNodeRef {
  id: string;
  nodeKey: string;
  nodeName: string;
  displayName?: string;
}

export interface PlanSummary {
  recommendedStartNode: PlanNodeRef;
  recommendedRhythm: LearningIntensity;
  recommendedRhythmLabel?: string;
  estimatedTotalMinutes: number;
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
  node: PlanNodeRef;
  masteryStatus: PathMasteryStatus;
  difficulty: PathDifficulty;
  reasonTags: string[];
  estimatedNodeMinutes: number;
  isStartingPoint: boolean;
  isPrerequisite: boolean;
  isFocus: boolean;
}

export interface PlanPriorityNode {
  nodeId: string;
  title: string;
  reason: string;
}

export interface PlanAlternative {
  key: string;
  title: string;
  description: string;
}

export interface PlanBenefit {
  key: string;
  title: string;
  description: string;
}

export interface PlanUnlock {
  key: string;
  title: string;
  description: string;
}

export interface PlanStageStatus {
  stage: LearningStage;
  label?: string;
  status: 'CURRENT' | 'LOCKED' | 'PENDING' | 'COMPLETED' | 'OPTIONAL' | 'REVIEW';
  description?: string;
}

export interface PlanTaskPreview {
  stage: LearningStage;
  title: string;
  learningGoal: string;
  learnerAction: string;
  aiSupport: string;
  estimatedTaskMinutes: number;
}

export interface LearningPlanContext {
  sessionId?: number;
  diagnosisId: string;
  goalText: string;
  courseName: string;
  chapterName: string;
  diagnosisSummary: string;
}

export interface LearningPlanMetadata {
  schemaVersion: string;
  persistedPreview: boolean;
  estimatedTotalMinutesScope: string;
  estimatedNodeMinutesScope: string;
  estimatedTaskMinutesScope: string;
}

export interface LearningPlanPreview {
  id: string;
  status: CodeLabel;
  previewOnly: boolean;
  committed: boolean;
  focuses: string[];
  summary: PlanSummary;
  reasons: PlanReason[];
  whyStartHere: string;
  keyWeaknesses: string[];
  priorityNodes: PlanPriorityNode[];
  pathNodes: PlanPathNode[];
  taskPreviews: PlanTaskPreview[];
  adjustments: PlanAdjustments;
  context: LearningPlanContext;
  nextStepNote: string;
  planSource?: CodeLabel;
  contentSource?: CodeLabel;
  fallbackApplied?: boolean;
  fallbackReasons?: string[];
  metadata?: LearningPlanMetadata;
  confidence?: string | number;
  recommendationHeadline?: string;
  recommendationSubtitle?: string;
  learnerGoal?: string;
  masteryScore?: number;
  riskIfSkipped?: string;
  alternatives?: PlanAlternative[];
  benefits?: PlanBenefit[];
  nextUnlocks?: PlanUnlock[];
  currentFocus?: string;
  nextStep?: string;
  stageStatuses?: PlanStageStatus[];
}

export interface PlanConfirmResult {
  planId?: string;
  sessionId: number;
  currentNodeId?: number;
  firstTaskId?: number;
  nextPage?: string;
}
