import { STAGE_LABELS, STAGE_ORDER } from '@/constants/learningPlan';
import type { LearningPlanPreview, LearningStage, PlanStageStatus } from '@/types/learningPlan';

type PlanSourceType = 'llm' | 'fallback';
type PathStageState = 'CURRENT' | 'LOCKED' | 'PENDING' | 'COMPLETED' | 'OPTIONAL' | 'REVIEW';

export interface LearningPlanHeroVm {
  sourceLabel: string;
  sourceType: PlanSourceType;
  recommendationHeadline: string;
  recommendationReason: string;
  currentTaskTitle: string;
  estimatedMinutes: string;
  currentStatus: string;
}

export interface LearningPlanReasonCardVm {
  title: string;
  description: string;
}

export interface LearningPlanStageVm {
  key: LearningStage;
  title: string;
  state: PathStageState;
  isCurrent: boolean;
}

export interface LearningPlanNavigatorVm {
  hero: LearningPlanHeroVm;
  reasonCards: LearningPlanReasonCardVm[];
  currentFocus: string;
  nextStep: string;
  pathStages: LearningPlanStageVm[];
}

function formatMinutes(minutes?: number) {
  if (!minutes || minutes <= 0) return '15分钟';
  if (minutes >= 60) {
    const hours = Math.floor(minutes / 60);
    const rest = minutes % 60;
    return rest ? `${hours}小时${rest}分钟` : `${hours}小时`;
  }
  return `${minutes}分钟`;
}

function clampText(value: string | undefined, fallback: string) {
  const text = String(value ?? '').trim();
  if (!text) return fallback;
  return text
    .replace(/^foundation of\s*/i, '')
    .replace(/^basics? of\s*/i, '')
    .replace(/^intro(?:duction)? to\s*/i, '')
    .replace(/\s+/g, ' ')
    .trim();
}

function toSource(preview: LearningPlanPreview): { label: string; type: PlanSourceType } {
  if (preview.fallbackApplied) {
    return { label: '规则兜底', type: 'fallback' };
  }

  const sourceText = `${preview.planSource?.code ?? ''} ${preview.planSource?.label ?? ''} ${preview.contentSource?.code ?? ''} ${preview.contentSource?.label ?? ''}`.toLowerCase();
  if (/llm|model|ai/.test(sourceText)) {
    return { label: 'LLM生成', type: 'llm' };
  }

  return { label: '', type: 'fallback' };
}

function toCurrentStatus(preview: LearningPlanPreview) {
  const mastery = preview.pathNodes.find((item) => item.isStartingPoint || item.isFocus)?.masteryStatus;
  if (mastery === 'STABLE') return '可以直接进入下一步';
  if (mastery === 'PARTIAL') return '建议立即开始';
  if (mastery === 'WEAK') return '建议立即开始';
  return '建议立即开始';
}

function inferStageState(preview: LearningPlanPreview, stage: LearningStage): PlanStageStatus {
  const matched = preview.stageStatuses?.find((item) => item.stage === stage);
  if (matched) return matched;

  const currentStage = preview.taskPreviews[0]?.stage ?? 'STRUCTURE';
  const currentIndex = STAGE_ORDER.findIndex((item) => item === currentStage);
  const stageIndex = STAGE_ORDER.findIndex((item) => item === stage);

  if (stageIndex === currentIndex) {
    return { stage, status: 'CURRENT' };
  }
  if (stageIndex < currentIndex) {
    return { stage, status: 'COMPLETED' };
  }
  if (stageIndex === currentIndex + 1) {
    return { stage, status: 'LOCKED' };
  }
  return { stage, status: 'PENDING' };
}

function buildReasonCards(preview: LearningPlanPreview, currentTaskTitle: string): LearningPlanReasonCardVm[] {
  const learnerGoal = clampText(
    preview.learnerGoal || preview.context.goalText,
    '先把当前目标拆成能马上开始的一小步。',
  );
  const currentWeakness = clampText(
    preview.keyWeaknesses[0] || preview.priorityNodes[0]?.reason || currentTaskTitle,
    '当前最需要先补的是基础结构。',
  );
  const whyNow = clampText(
    preview.whyStartHere || preview.reasons[0]?.description || preview.nextStepNote,
    '这是后面继续推进的前置知识。',
  );

  return [
    { title: '你的目标', description: learnerGoal },
    { title: '当前短板', description: currentWeakness },
    { title: '为什么先补这里', description: whyNow },
  ];
}

export function adaptLearningPlanPreview(preview: LearningPlanPreview): LearningPlanNavigatorVm {
  const source = toSource(preview);
  const firstTask = preview.taskPreviews[0];
  const currentTaskTitle = clampText(
    firstTask?.title
      || preview.summary.recommendedStartNode.displayName
      || preview.summary.recommendedStartNode.nodeName
      || preview.priorityNodes[0]?.title,
    '建立整体框架',
  );
  const recommendationTarget = clampText(
    preview.summary.recommendedStartNode.displayName
      || preview.summary.recommendedStartNode.nodeName
      || preview.currentFocus
      || currentTaskTitle,
    currentTaskTitle,
  );
  const heroReason = clampText(
    preview.whyStartHere || preview.reasons[0]?.description,
    '这是你当前目标最关键的前置知识。',
  );
  const pathStages = STAGE_ORDER.map((stage) => {
    const normalized = inferStageState(preview, stage);
    return {
      key: stage,
      title: STAGE_LABELS[stage],
      state: normalized.status,
      isCurrent: normalized.status === 'CURRENT',
    };
  });

  return {
    hero: {
      sourceLabel: source.label,
      sourceType: source.type,
      recommendationHeadline: `你现在应该先补「${recommendationTarget}」`,
      recommendationReason: heroReason,
      currentTaskTitle,
      estimatedMinutes: formatMinutes(firstTask?.estimatedTaskMinutes || preview.summary.estimatedTotalMinutes),
      currentStatus: toCurrentStatus(preview),
    },
    reasonCards: buildReasonCards(preview, currentTaskTitle),
    currentFocus: clampText(preview.currentFocus || preview.priorityNodes[0]?.title || recommendationTarget, recommendationTarget),
    nextStep: clampText(
      preview.nextStep
        || preview.pathNodes.find((item) => !item.isStartingPoint && !item.isFocus)?.node.displayName
        || preview.pathNodes.find((item) => !item.isStartingPoint && !item.isFocus)?.node.nodeName,
      '理解原理',
    ),
    pathStages,
  };
}
