import { STAGE_LABELS } from '@/constants/learningPlan';
import type { LearningStage } from '@/types/learningPlan';

export function formatStage(stage?: string) {
  if (!stage) {
    return '待开始';
  }
  return STAGE_LABELS[stage as LearningStage] ?? stage;
}

export function formatPercent(value?: number | null) {
  if (value === undefined || value === null || Number.isNaN(value)) {
    return '--';
  }
  const normalized = value > 1 ? value : value * 100;
  return `${Math.round(normalized)}%`;
}

export function toNumber(value: unknown) {
  if (typeof value === 'number') {
    return value;
  }
  if (typeof value === 'string' && value.trim()) {
    const parsed = Number(value);
    return Number.isNaN(parsed) ? undefined : parsed;
  }
  return undefined;
}
