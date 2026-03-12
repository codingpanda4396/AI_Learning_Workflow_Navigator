import { STAGE_LABELS } from '@/constants/learningPlan';
import type { LearningStage } from '@/types/learningPlan';
import type { SessionBusinessStatus } from '@/types/session';

export function formatStage(stage?: string) {
  if (!stage) {
    return '待开始';
  }
  return STAGE_LABELS[stage as LearningStage] ?? stage;
}

export function formatSessionStatus(status?: SessionBusinessStatus | string) {
  switch (status) {
    case 'ANALYZING':
      return '诊断分析中';
    case 'PLANNING':
      return '规划路径中';
    case 'LEARNING':
      return '学习中';
    case 'PRACTICING':
      return '练习中';
    case 'REPORT_READY':
      return '报告已就绪';
    case 'COMPLETED':
      return '已完成';
    case 'FAILED':
      return '进行失败';
    default:
      return '待开始';
  }
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
