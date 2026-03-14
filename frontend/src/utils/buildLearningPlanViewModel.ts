import type { LearningPlanPreview, PlanAlternative } from '@/types/learningPlan';
import { formatPlanMinutes, mapStageToNaturalLabel, normalizePlanPreviewText, pickWhatISaw } from '@/utils/normalizePlanPreview';

export interface PlanPreviewViewModel {
  learnerInsightTitle: string;
  learnerState: string;
  whySectionTitle: string;
  whatISaw: string[];
  whyThisPlanFitsYou: string;
  stepSectionTitle: string;
  taskTitle: string;
  estimatedMinutesText: string;
  thisRoundBoundary: string;
  nextStepLabel: string;
  ctaHint: string;
  riskTitle: string;
  mainRiskIfSkip: string;
  adaptationHint: string;
  alternatives: PlanAlternative[];
}

function resolveTaskTitle(preview: LearningPlanPreview): string {
  return normalizePlanPreviewText(
    preview.taskPreviews[0]?.title
    || preview.summary.recommendedStartNode.displayName
    || preview.summary.recommendedStartNode.nodeName
    || preview.priorityNodes[0]?.title,
    '先补当前最关键的一步',
  );
}

function resolveLearnerState(preview: LearningPlanPreview): string {
  return normalizePlanPreviewText(
    preview.personalization?.learnerState
    || preview.currentFocus
    || preview.keyWeaknesses[0]
    || preview.priorityNodes[0]?.reason,
    '你已经有目标方向，当前主要卡在关键基础还不够稳。先补这一小步，后续会更顺。',
  );
}

function resolveWhyFitsYou(preview: LearningPlanPreview): string {
  return normalizePlanPreviewText(
    preview.personalization?.whyThisPlanFitsYou
    || preview.whyStartHere
    || preview.reasons[0]?.description
    || preview.nextStepNote,
    '这一步是当前阶段最有杠杆的一步，先打通再推进会更稳。',
  );
}

function resolveRoundBoundary(preview: LearningPlanPreview): string {
  const firstStage = mapStageToNaturalLabel(preview.taskPreviews[0]?.stage);
  return normalizePlanPreviewText(
    preview.personalization?.thisRoundBoundary
    || preview.taskPreviews[0]?.learnerAction
    || `这轮先完成「${firstStage}」中的关键动作，不需要一次学完整章。`,
    `这轮先完成当前关键动作，不需要一次学完整章。`,
  );
}

function resolveNextStepLabel(preview: LearningPlanPreview): string {
  return normalizePlanPreviewText(
    preview.nextStepLabel
    || preview.nextStep
    || preview.nextStepNote,
    '完成并开始这一轮',
  );
}

function resolveMainRisk(preview: LearningPlanPreview): string {
  return normalizePlanPreviewText(
    preview.personalization?.mainRiskIfSkip
    || preview.riskIfSkipped,
    '如果跳过这一步，后续练习会反复卡住，整体效率会明显下降。',
  );
}

function resolveAdaptationHint(preview: LearningPlanPreview): string {
  return normalizePlanPreviewText(
    preview.personalization?.adaptationHint
    || preview.nextStepNote,
    '完成后系统会根据你的表现继续调整节奏和下一步重点。',
  );
}

export function buildLearningPlanViewModel(preview: LearningPlanPreview): PlanPreviewViewModel {
  const taskTitle = resolveTaskTitle(preview);
  const estimatedMinutes = preview.taskPreviews[0]?.estimatedTaskMinutes || preview.summary.estimatedTotalMinutes;

  return {
    learnerInsightTitle: '我对你现在状态的判断',
    learnerState: resolveLearnerState(preview),
    whySectionTitle: '为什么现在先做这一步',
    whatISaw: pickWhatISaw(preview),
    whyThisPlanFitsYou: resolveWhyFitsYou(preview),
    stepSectionTitle: '你这一轮只需要做什么',
    taskTitle,
    estimatedMinutesText: formatPlanMinutes(estimatedMinutes),
    thisRoundBoundary: resolveRoundBoundary(preview),
    nextStepLabel: resolveNextStepLabel(preview),
    ctaHint: '完成后系统会根据你的表现继续调整。',
    riskTitle: '如果跳过 / 做完之后会怎样',
    mainRiskIfSkip: resolveMainRisk(preview),
    adaptationHint: resolveAdaptationHint(preview),
    alternatives: preview.alternatives ?? [],
  };
}
