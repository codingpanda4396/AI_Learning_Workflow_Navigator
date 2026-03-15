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

export interface PlanStrategyOption {
  key: string;
  title: string;
  fitFor: string;
  tradeoff: string;
  timeShortPlan?: string;
}

export interface PlanStrategyComparison {
  recommendedKey?: string;
  recommendedReason?: string;
  options: PlanStrategyOption[];
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

export interface LearningPlanPersonalization {
  learnerState?: string;
  whatISaw: string[];
  whyThisPlanFitsYou?: string;
  mainRiskIfSkip?: string;
  thisRoundBoundary?: string;
  adaptationHint?: string;
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
  expectedArtifact?: string;
  completionCriteria?: string;
  checkMethod?: string;
}

export interface PreviewNextAction {
  title: string;
  learnerAction?: string;
  expectedArtifact?: string;
  completionCriteria?: string;
  estimatedMinutes?: number;
  aiSupport?: string;
  checkMethod?: string;
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

export interface PreviewRecommendedEntry {
  conceptId: string;
  title: string;
  estimatedMinutes: number;
  reason: string;
}

export interface PreviewLearnerSnapshot {
  currentState: string;
  evidence: string[];
}

export interface PreviewRecommendedStrategy {
  code: string;
  label: string;
  explanation: string;
}

export interface PreviewAlternativeStrategy {
  code: string;
  label: string;
  notRecommendedReason: string;
}

export interface PreviewPersonalizedSummary {
  title: string;
  description: string;
  tags: string[];
}

export interface PreviewCurrentTaskCard {
  title: string;
  estimatedMinutes?: number;
  goal?: string;
  tasks: string[];
  completionGains: string[];
}

export interface PreviewPersonalizedReasons {
  whyRecommended?: string;
  whyThisStepFirst?: string;
}

export interface PreviewExplanationPanel {
  learnerProfile?: string;
  systemDecision?: string;
}

export interface LearningPlanPreview {
  id: string;
  status: CodeLabel;
  previewOnly: boolean;
  committed: boolean;
  goal: string;
  recommendedEntry: PreviewRecommendedEntry;
  learnerSnapshotV2: PreviewLearnerSnapshot;
  recommendedStrategy: PreviewRecommendedStrategy;
  alternativesV2: PreviewAlternativeStrategy[];
  nextActionsV2: string[];
  nextActionsDetail?: PreviewNextAction[];
  whyThisStep?: string;
  keyEvidence?: string[];
  riskFlags?: string[];
  skipRisk?: string;
  expectedGain?: string;
  confidenceHint?: string;
  startGuide: string;
  explanationGenerated: boolean;
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
  nextStepLabel?: string;
  personalization?: LearningPlanPersonalization;
  narrative?: string;
  planNarrative?: string;
  guidance?: string;
  confidenceExplanation?: string;
  strategyComparison?: PlanStrategyComparison;
  optionComparison?: PlanStrategyComparison;
  kickoffSteps?: string[];
  firstAction?: string;
  firstCheckpoint?: string;
  ifPerformWell?: string;
  ifStillStruggle?: string;
  ifNoTime?: string;
  profileDrivenReasoning?: string;
  personalizedSummary?: PreviewPersonalizedSummary;
  currentTaskCard?: PreviewCurrentTaskCard;
  personalizedReasons?: PreviewPersonalizedReasons;
  explanationPanel?: PreviewExplanationPanel;
}

export interface PlanConfirmResult {
  planId?: string;
  sessionId: number;
  currentNodeId?: number;
  firstTaskId?: number;
  nextPage?: string;
}
