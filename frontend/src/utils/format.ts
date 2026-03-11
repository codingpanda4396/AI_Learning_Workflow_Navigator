import { STAGE_LABELS } from '@/constants/app';

export function formatStage(stage?: string) {
  if (!stage) {
    return '待开始';
  }
  return STAGE_LABELS[stage] ?? stage;
}

export function formatPercent(value?: number | null) {
  if (value === undefined || value === null || Number.isNaN(value)) {
    return '--';
  }
  const normalized = value > 1 ? value : value * 100;
  return `${Math.round(normalized)}%`;
}

export function formatOutputContent(output: unknown) {
  if (typeof output === 'string') {
    return output;
  }
  if (output && typeof output === 'object') {
    return JSON.stringify(output, null, 2);
  }
  return '暂无内容';
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
