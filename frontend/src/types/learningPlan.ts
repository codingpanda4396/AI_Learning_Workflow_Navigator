import type { CodeLabel } from '@/types/common';

export type LearningStage = 'STRUCTURE' | 'UNDERSTANDING' | 'TRAINING' | 'REFLECTION';
export type LearningIntensity = 'LIGHT' | 'STANDARD' | 'INTENSIVE';
export type LearningMode = 'EXPLAIN_THEN_PRACTICE' | 'LEARN_BY_DOING';
export type PathMasteryStatus = 'WEAK' | 'PARTIAL' | 'STABLE' | 'NEW';
export type PathDifficulty = 'FOUNDATION' | 'CORE' | 'CHALLENGE';
export type PlanPreviewStatus = 'PREVIEW_READY' | 'COMMITTED';

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
  previewId: number;
  status: PlanPreviewStatus;
  previewOnly: boolean;
  committed: boolean;
  summary: PlanSummary;
  reasons: PlanReason[];
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
}

export interface PlanConfirmResult {
  planId?: number;
  sessionId: number;
  currentNodeId?: number;
  firstTaskId?: number;
  nextPage?: string;
}
