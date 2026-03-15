/**
 * 阶段与路由的映射：仅表达位置，准入由 guards 判断。
 */

import type { LearningStage } from './types';

const STAGE_TO_PATH: Record<LearningStage, string> = {
  DIAGNOSIS: '/diagnosis',
  PLAN_PREVIEW: '/plan',
  LEARNING_TASK: '/learn/:sessionId/task',
  TRAINING: '/learn/:sessionId/training',
  EVALUATION: '/learn/:sessionId/evaluation',
  NEXT_ACTION: '/learn/:sessionId',
};

/**
 * 根据 sessionId 和 stage 解析出实际路径（用于 router.push）
 */
export function resolveRouteByStage(sessionId: number, stage: LearningStage): string {
  if (stage === 'DIAGNOSIS') return '/diagnosis';
  if (stage === 'PLAN_PREVIEW') return '/plan';
  return STAGE_TO_PATH[stage].replace(':sessionId', String(sessionId));
}

/**
 * 学习执行阶段的路由名（与 router 中 name 一致）
 */
export const LEARN_ROUTE_NAMES = {
  hub: 'learn',
  task: 'learn-task',
  training: 'learn-training',
  evaluation: 'learn-evaluation',
  next: 'learn-next',
} as const;
