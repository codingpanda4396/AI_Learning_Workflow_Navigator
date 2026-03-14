import { STAGE_LABELS, STAGE_ORDER } from '@/constants/learningPlan';
import type {
  LearningPlanPreview,
  LearningStage,
  PathMasteryStatus,
  PlanAlternative,
  PlanBenefit,
  PlanReason,
  PlanStageStatus,
  PlanUnlock,
} from '@/types/learningPlan';

type ConfidenceLevel = 'high' | 'medium' | 'low';
type PlanSourceType = 'llm' | 'fallback';
type PathStageState = 'CURRENT' | 'LOCKED' | 'PENDING' | 'COMPLETED' | 'OPTIONAL' | 'REVIEW';

export interface LearningPlanDecisionCardVm {
  sourceLabel: string;
  sourceType: PlanSourceType;
  confidenceLabel: string;
  confidenceLevel: ConfidenceLevel;
  recommendationHeadline: string;
  recommendationSubtitle: string;
  currentTaskTitle: string;
  estimatedMinutes: string;
  priority: string;
  currentStatus: string;
}

export interface LearningPlanReasonItemVm {
  title: string;
  description: string;
}

export interface LearningPlanAlternativeVm {
  key: string;
  title: string;
  description: string;
}

export interface LearningPlanBenefitVm {
  key: string;
  title: string;
  description: string;
}

export interface LearningPlanStageVm {
  key: LearningStage;
  title: string;
  state: PathStageState;
  stateLabel: string;
  description: string;
}

export interface LearningPlanNavigatorVm {
  decisionCard: LearningPlanDecisionCardVm;
  learnerGoal: string;
  currentWeaknesses: string[];
  masteryScore: string;
  riskIfSkipped: string;
  basedOnCurrentState: LearningPlanReasonItemVm[];
  decisionReasons: LearningPlanReasonItemVm[];
  alternatives: LearningPlanAlternativeVm[];
  benefits: LearningPlanBenefitVm[];
  nextUnlocks: LearningPlanBenefitVm[];
  currentFocus: string;
  nextStep: string;
  pathRisk: string;
  pathStages: LearningPlanStageVm[];
}

function formatMinutes(minutes?: number) {
  if (!minutes || minutes <= 0) return '约 15 分钟';
  if (minutes >= 60) {
    const hours = Math.floor(minutes / 60);
    const rest = minutes % 60;
    return rest ? `约 ${hours} 小时 ${rest} 分钟` : `约 ${hours} 小时`;
  }
  return `约 ${minutes} 分钟`;
}

function toTextList(items: Array<string | undefined | null>, fallback: string) {
  const result = items.map((item) => item?.trim()).filter((item): item is string => Boolean(item));
  return result.length ? result : [fallback];
}

function toConfidenceLabel(value: LearningPlanPreview['confidence']): { label: string; level: ConfidenceLevel } {
  if (typeof value === 'number') {
    if (value >= 0.75) return { label: '高把握', level: 'high' };
    if (value >= 0.45) return { label: '中等把握', level: 'medium' };
    return { label: '低把握', level: 'low' };
  }

  const text = String(value ?? '').trim().toLowerCase();
  if (/高|high|strong/.test(text)) return { label: '高把握', level: 'high' };
  if (/低|low|weak/.test(text)) return { label: '低把握', level: 'low' };
  return { label: '中等把握', level: 'medium' };
}

function toSource(preview: LearningPlanPreview): { label: string; type: PlanSourceType } {
  if (preview.fallbackApplied) {
    return { label: '规则兜底', type: 'fallback' };
  }

  const sourceText = `${preview.planSource?.code ?? ''} ${preview.planSource?.label ?? ''} ${preview.contentSource?.code ?? ''} ${preview.contentSource?.label ?? ''}`.toLowerCase();
  if (/llm|model|ai/.test(sourceText)) {
    return { label: 'LLM生成', type: 'llm' };
  }

  return { label: '规则兜底', type: 'fallback' };
}

function toPriority(preview: LearningPlanPreview) {
  if (preview.riskIfSkipped?.trim()) return '高优先级';
  if (preview.priorityNodes.length > 1) return '优先推进';
  return '建议现在开始';
}

function toCurrentStatus(preview: LearningPlanPreview, masteryText: string) {
  const mastery = preview.pathNodes.find((item) => item.isStartingPoint || item.isFocus)?.masteryStatus;
  if (mastery === 'STABLE') return '可跳过';
  if (mastery === 'PARTIAL') return '建议立即开始';
  if (mastery === 'WEAK') return '建议立即开始';
  if (masteryText !== '--') return '建议复习';
  return '建议立即开始';
}

function buildCurrentState(preview: LearningPlanPreview): LearningPlanReasonItemVm[] {
  const items: LearningPlanReasonItemVm[] = [
    {
      title: '你的目标',
      description: preview.learnerGoal?.trim() || preview.context.goalText || '先把当前目标拆成可执行的一小步。',
    },
    {
      title: '当前短板',
      description: preview.keyWeaknesses[0] || preview.priorityNodes[0]?.reason || '当前最先影响推进效率的薄弱点已经被标记出来。',
    },
    {
      title: '当前掌握度',
      description: preview.masteryScore !== undefined && preview.masteryScore !== null ? `${Math.round(preview.masteryScore)}% 左右，仍有明显提升空间。` : '系统判断你还没有稳稳跨过这一步。',
    },
  ];

  return items;
}

function buildDecisionReasons(preview: LearningPlanPreview): LearningPlanReasonItemVm[] {
  const fromReasons = preview.reasons.slice(0, 3).map((reason) => ({
    title: reason.title || reason.label || '先推这一步',
    description: reason.description || '这一步能更稳地承接后续学习。',
  }));

  if (fromReasons.length) {
    return fromReasons;
  }

  const nextNode = preview.pathNodes.find((item) => !item.isStartingPoint && !item.isFocus);
  return [
    {
      title: '这是后续内容的前置',
      description: nextNode ? `先打通这里，下一跳更容易进入「${nextNode.node.displayName || nextNode.node.nodeName}」。` : '这一步先通了，后面的学习链路会更顺。',
    },
    {
      title: '当前收益最高',
      description: preview.whyStartHere || '系统判断这是此刻最值得投入的起点。',
    },
    {
      title: '跳过会有风险',
      description: preview.riskIfSkipped?.trim() || '现在跳过，后面更难的内容会变得吃力。',
    },
  ];
}

function mapAlternatives(alternatives: PlanAlternative[], preview: LearningPlanPreview): LearningPlanAlternativeVm[] {
  if (alternatives.length) {
    return alternatives.slice(0, 2).map((item) => ({
      key: item.key,
      title: item.title,
      description: item.description,
    }));
  }

  const candidates = preview.pathNodes.filter((item) => !item.isStartingPoint).slice(0, 2);
  return candidates.map((item, index) => ({
    key: item.node.id || `alt-${index + 1}`,
    title: item.node.displayName || item.node.nodeName || `备选方向 ${index + 1}`,
    description: item.isPrerequisite ? '也重要，但现在先学它会让路径变长。' : '收益不低，但不如当前建议更贴近你的短板。',
  }));
}

function mapBenefits(benefits: PlanBenefit[], preview: LearningPlanPreview): LearningPlanBenefitVm[] {
  if (benefits.length) {
    return benefits.map((item) => ({
      key: item.key,
      title: item.title,
      description: item.description,
    }));
  }

  const task = preview.taskPreviews[0];
  return toTextList(
    [
      task?.learningGoal,
      task?.learnerAction,
      task?.aiSupport,
    ],
    '先打通这一步，你会立刻知道接下来该怎么学、怎么练、怎么验证。'
  ).map((item, index) => ({
    key: `benefit-${index + 1}`,
    title: index === 0 ? '学完后立刻获得' : index === 1 ? '执行动作更清晰' : 'AI 会继续托底',
    description: item,
  }));
}

function mapUnlocks(nextUnlocks: PlanUnlock[], preview: LearningPlanPreview): LearningPlanBenefitVm[] {
  if (nextUnlocks.length) {
    return nextUnlocks.map((item) => ({
      key: item.key,
      title: item.title,
      description: item.description,
    }));
  }

  const candidates = preview.pathNodes.filter((item) => !item.isStartingPoint).slice(0, 3);
  return (candidates.length ? candidates : STAGE_ORDER.slice(1).map((stage) => ({ stage } as { stage: LearningStage }))).map((item, index) => ({
    key: `unlock-${index + 1}`,
    title: 'node' in item ? item.node.displayName || item.node.nodeName : STAGE_LABELS[item.stage],
    description: '完成当前建议后，这一段会更容易被解锁。',
  }));
}

function inferStageState(preview: LearningPlanPreview, stage: LearningStage): PlanStageStatus {
  const currentStage = preview.taskPreviews[0]?.stage || preview.pathNodes.find((item) => item.isStartingPoint || item.isFocus)?.reasonTags[0];
  if (preview.stageStatuses?.length) {
    const matched = preview.stageStatuses.find((item) => item.stage === stage);
    if (matched) return matched;
  }

  const currentIndex = STAGE_ORDER.findIndex((item) => item === preview.taskPreviews[0]?.stage);
  const stageIndex = STAGE_ORDER.findIndex((item) => item === stage);

  if (currentIndex >= 0 && stageIndex === currentIndex) {
    return { stage, status: 'CURRENT', description: '当前主攻阶段' };
  }
  if (currentIndex >= 0 && stageIndex < currentIndex) {
    return { stage, status: 'COMPLETED', description: '可以回看巩固' };
  }
  if (currentIndex >= 0 && stageIndex === currentIndex + 1) {
    return { stage, status: 'LOCKED', description: '完成当前建议后解锁' };
  }
  if (currentStage && stageIndex < 0) {
    return { stage, status: 'PENDING', description: '等待推进到这一阶段' };
  }
  return { stage, status: stage === 'STRUCTURE' ? 'CURRENT' : 'PENDING', description: stage === 'STRUCTURE' ? '当前建议从这里开始' : '尚未开始' };
}

function toStageStateLabel(state: PathStageState) {
  switch (state) {
    case 'CURRENT':
      return '当前建议';
    case 'LOCKED':
      return '待解锁';
    case 'COMPLETED':
      return '已完成';
    case 'OPTIONAL':
      return '可跳过';
    case 'REVIEW':
      return '建议复习';
    case 'PENDING':
    default:
      return '未开始';
  }
}

function buildPathStages(preview: LearningPlanPreview): LearningPlanStageVm[] {
  return STAGE_ORDER.map((stage) => {
    const normalized = inferStageState(preview, stage);
    return {
      key: stage,
      title: STAGE_LABELS[stage],
      state: normalized.status,
      stateLabel: normalized.label || toStageStateLabel(normalized.status),
      description: normalized.description || '系统会按阶段继续推进。',
    };
  });
}

export function adaptLearningPlanPreview(preview: LearningPlanPreview): LearningPlanNavigatorVm {
  const source = toSource(preview);
  const confidence = toConfidenceLabel(preview.confidence);
  const firstTask = preview.taskPreviews[0];
  const currentTaskTitle =
    firstTask?.title?.trim()
    || preview.summary.recommendedStartNode.displayName
    || preview.summary.recommendedStartNode.nodeName
    || preview.nextStepNote
    || '从最关键的一步开始';
  const masteryText = preview.masteryScore !== undefined && preview.masteryScore !== null ? `${Math.round(preview.masteryScore)}%` : '--';
  const recommendationHeadline = preview.recommendationHeadline?.trim() || '系统判断你现在最该先学什么';
  const recommendationSubtitle =
    preview.recommendationSubtitle?.trim()
    || preview.whyStartHere
    || '这一步最贴近你当前目标，也最能弥补现在的短板。';

  return {
    decisionCard: {
      sourceLabel: source.label,
      sourceType: source.type,
      confidenceLabel: confidence.label,
      confidenceLevel: confidence.level,
      recommendationHeadline,
      recommendationSubtitle,
      currentTaskTitle,
      estimatedMinutes: formatMinutes(firstTask?.estimatedTaskMinutes || preview.summary.estimatedTotalMinutes),
      priority: toPriority(preview),
      currentStatus: toCurrentStatus(preview, masteryText),
    },
    learnerGoal: preview.learnerGoal?.trim() || preview.context.goalText || '先聚焦当前最值得推进的目标。',
    currentWeaknesses: toTextList(preview.keyWeaknesses, '系统已识别出当前路径上的关键薄弱点。'),
    masteryScore: masteryText,
    riskIfSkipped: preview.riskIfSkipped?.trim() || preview.fallbackReasons?.[0] || '跳过这一步，后续更难内容会更容易卡住。',
    basedOnCurrentState: buildCurrentState(preview),
    decisionReasons: buildDecisionReasons(preview),
    alternatives: mapAlternatives(preview.alternatives ?? [], preview),
    benefits: mapBenefits(preview.benefits ?? [], preview),
    nextUnlocks: mapUnlocks(preview.nextUnlocks ?? [], preview),
    currentFocus:
      preview.currentFocus?.trim()
      || preview.priorityNodes[0]?.title
      || currentTaskTitle,
    nextStep:
      preview.nextStep?.trim()
      || preview.pathNodes.find((item) => !item.isStartingPoint && !item.isFocus)?.node.displayName
      || preview.pathNodes.find((item) => !item.isStartingPoint && !item.isFocus)?.node.nodeName
      || '完成这一步后继续推进下一阶段',
    pathRisk: preview.riskIfSkipped?.trim() || '先跳过会让后续理解和练习成本变高。',
    pathStages: buildPathStages(preview),
  };
}
