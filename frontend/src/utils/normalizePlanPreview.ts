import type { LearningPlanPreview, LearningStage } from '@/types/learningPlan';

const STAGE_TO_CN: Record<LearningStage, string> = {
  STRUCTURE: '建立框架',
  UNDERSTANDING: '补齐理解',
  TRAINING: '定向训练',
  REFLECTION: '收口复盘',
};

export function formatPlanMinutes(minutes?: number): string {
  if (!minutes || minutes <= 0) return '15分钟';
  if (minutes >= 60) {
    const hours = Math.floor(minutes / 60);
    const rest = minutes % 60;
    return rest ? `${hours}小时${rest}分钟` : `${hours}小时`;
  }
  return `${minutes}分钟`;
}

export function mapStageToNaturalLabel(stage?: string): string {
  if (!stage) return '建立框架';
  if (stage in STAGE_TO_CN) {
    return STAGE_TO_CN[stage as LearningStage];
  }
  return String(stage);
}

export function normalizePlanPreviewText(value: unknown, fallback: string): string {
  const text = String(value ?? '').replace(/\s+/g, ' ').trim();
  return text || fallback;
}

export function pickWhatISaw(preview: LearningPlanPreview): string[] {
  const fromPersonalization = preview.personalization?.whatISaw?.map((item) => item.trim()).filter(Boolean) ?? [];
  if (fromPersonalization.length) {
    return fromPersonalization.slice(0, 4);
  }

  const fromReasons = preview.reasons
    .map((item) => item.description.trim())
    .filter(Boolean)
    .slice(0, 2);
  const fromWeaknesses = preview.keyWeaknesses.map((item) => item.trim()).filter(Boolean).slice(0, 2);
  return [...fromReasons, ...fromWeaknesses].slice(0, 4);
}
