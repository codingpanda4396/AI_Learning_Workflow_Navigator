/**
 * 阶段准入：是否可进入某阶段，由 flow snapshot 驱动。
 */

import type { LearningFlowSnapshot, LearningStage } from './types';

export function canEnterStage(snapshot: LearningFlowSnapshot | null, stage: LearningStage): boolean {
  if (!snapshot || snapshot.blocked.blocked) return false;
  switch (stage) {
    case 'LEARNING_TASK':
      return snapshot.canEnterTask;
    case 'TRAINING':
      return snapshot.canEnterTraining;
    case 'EVALUATION':
      return snapshot.canEnterEvaluation;
    case 'NEXT_ACTION':
    case 'DIAGNOSIS':
    case 'PLAN_PREVIEW':
      return true;
    default:
      return false;
  }
}

export function isLearningStage(stage: string): stage is LearningStage {
  return [
    'DIAGNOSIS',
    'PLAN_PREVIEW',
    'LEARNING_TASK',
    'TRAINING',
    'EVALUATION',
    'NEXT_ACTION',
  ].includes(stage);
}
