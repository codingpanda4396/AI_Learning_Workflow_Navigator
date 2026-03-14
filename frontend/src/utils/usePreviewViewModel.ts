import type { LearningPlanPreview, PlanAlternative, PlanStrategyComparison, PlanStrategyOption } from '@/types/learningPlan';
import { formatPlanMinutes, mapStageToNaturalLabel, normalizePlanPreviewText, pickWhatISaw } from '@/utils/normalizePlanPreview';

interface PreviewSignalItem {
  key: string;
  label: string;
  value: string;
}

interface PreviewStrategyCard {
  key: string;
  title: string;
  fitFor: string;
  tradeoff: string;
  timeShortPlan?: string;
  recommended?: boolean;
}

interface PreviewKickoffScript {
  firstAction: string;
  systemLead: string;
  firstCheckpoint: string;
  ifPerformWell: string;
  ifStillStruggle: string;
  ifNoTime: string;
  steps: string[];
}

export interface PreviewDecisionViewModel {
  hero: {
    title: string;
    strongestReason: string;
    riskIfSkip: string;
    ctaLabel: string;
    estimate: string;
  };
  signals: PreviewSignalItem[];
  strategy: {
    recommendedReason: string;
    recommended: PreviewStrategyCard;
    others: PreviewStrategyCard[];
  };
  kickoff: PreviewKickoffScript;
}

function text(value: unknown, fallback: string): string {
  return normalizePlanPreviewText(value, fallback);
}

function firstNonEmpty(...values: unknown[]): string {
  for (const value of values) {
    const candidate = String(value ?? '').trim();
    if (candidate) {
      return candidate;
    }
  }
  return '';
}

function getMainTaskTitle(preview: LearningPlanPreview): string {
  return text(
    preview.taskPreviews[0]?.title
      || preview.summary.recommendedStartNode.displayName
      || preview.summary.recommendedStartNode.nodeName
      || preview.priorityNodes[0]?.title,
    '先补稳当前最关键的一步',
  );
}

function buildStrategyOptionsFromAlternatives(alternatives: PlanAlternative[]): PlanStrategyOption[] {
  return alternatives.slice(0, 3).map((item, index) => ({
    key: item.key || `alternative-${index + 1}`,
    title: item.title || `备选方案 ${index + 1}`,
    fitFor: item.description || '更适合你想切换当前节奏时使用。',
    tradeoff: '短期上手快，但可能会降低关键薄弱点的修复效率。',
  }));
}

function resolveStrategyComparison(preview: LearningPlanPreview): PlanStrategyComparison {
  const fromBackend = preview.strategyComparison ?? preview.optionComparison;
  if (fromBackend?.options?.length) {
    return fromBackend;
  }

  return {
    recommendedReason: '',
    options: buildStrategyOptionsFromAlternatives(preview.alternatives ?? []),
  };
}

function toStrategyCard(option: PlanStrategyOption, recommended = false): PreviewStrategyCard {
  return {
    key: option.key,
    title: text(option.title, '策略方案'),
    fitFor: text(option.fitFor, recommended ? '适合现在先补稳关键薄弱点，再继续推进。' : '适合你希望切换当前学习方式时使用。'),
    tradeoff: text(option.tradeoff, recommended ? '前几轮可能感觉慢一点，但后面会更稳更快。' : '短期推进更快，但后续可能反复卡在基础缺口。'),
    timeShortPlan: option.timeShortPlan?.trim() || undefined,
    recommended,
  };
}

function buildStrategySection(preview: LearningPlanPreview): PreviewDecisionViewModel['strategy'] {
  const comparison = resolveStrategyComparison(preview);
  const options = comparison.options ?? [];

  const recommendedKey = comparison.recommendedKey?.trim();
  const recommendedOption = recommendedKey
    ? options.find((item) => item.key === recommendedKey)
    : options[0];

  const fallbackRecommended: PlanStrategyOption = {
    key: 'recommended-main',
    title: firstNonEmpty(preview.recommendationHeadline, '稳扎稳打补基础，再推进'),
    fitFor: '适合你当前想要稳定推进，不想后面反复返工。',
    tradeoff: '当前投入会多一点，但能减少后面反复卡住。',
    timeShortPlan: preview.ifNoTime || undefined,
  };

  const main = toStrategyCard(recommendedOption ?? fallbackRecommended, true);
  const others = options
    .filter((item) => item.key !== (recommendedOption?.key ?? ''))
    .slice(0, 3)
    .map((item) => toStrategyCard(item, false));

  if (!others.length) {
    others.push(
      toStrategyCard(
        {
          key: 'fast-track',
          title: '快速推进',
          fitFor: '适合你今天状态很好、时间也足够时。',
          tradeoff: '如果基础还没稳，后面容易反复卡住。',
          timeShortPlan: preview.ifNoTime || '时间少时，先做 10 分钟压缩版，再补完整轮。',
        },
        false,
      ),
      toStrategyCard(
        {
          key: 'practice-first',
          title: '先做题带学',
          fitFor: '适合你更偏好边做边学的节奏。',
          tradeoff: '短期参与感更强，但可能遗漏底层理解。',
          timeShortPlan: preview.ifNoTime || undefined,
        },
        false,
      ),
    );
  }

  return {
    recommendedReason: text(
      comparison.recommendedReason
        || preview.confidenceExplanation
        || preview.personalization?.whyThisPlanFitsYou
        || preview.whyStartHere,
      '当前先补稳这一块，整体推进效率会更高。',
    ),
    recommended: main,
    others,
  };
}

function buildKickoffSteps(preview: LearningPlanPreview): string[] {
  if (preview.kickoffSteps?.length) {
    return preview.kickoffSteps.slice(0, 4);
  }

  const stage = mapStageToNaturalLabel(preview.taskPreviews[0]?.stage);
  const goal = preview.taskPreviews[0]?.learningGoal;
  const action = preview.taskPreviews[0]?.learnerAction;
  const support = preview.taskPreviews[0]?.aiSupport;
  return [
    text(preview.firstAction || goal, `先用一轮 ${stage} 把核心概念理顺。`),
    text(action, '你按提示完成关键动作，先把最容易卡住的点打通。'),
    text(support, '我会给你针对性提示，不让你在细节里绕圈。'),
  ];
}

export function buildPreviewViewModel(preview: LearningPlanPreview): PreviewDecisionViewModel {
  const taskTitle = getMainTaskTitle(preview);
  const estimatedMinutes = preview.taskPreviews[0]?.estimatedTaskMinutes || preview.summary.estimatedTotalMinutes;
  const strategySection = buildStrategySection(preview);
  const whatISaw = pickWhatISaw(preview);

  return {
    hero: {
      title: text(
        preview.recommendationHeadline || preview.summary.personalizedHeadline,
        `这次先学「${taskTitle}」`,
      ),
      strongestReason: text(
        preview.narrative
          || preview.planNarrative
          || preview.guidance
          || preview.confidenceExplanation
          || preview.whyStartHere
          || preview.reasons[0]?.description,
        '这次先别急着往后赶，先补稳这一块，后面的推进会明显顺很多。',
      ),
      riskIfSkip: text(
        preview.personalization?.mainRiskIfSkip || preview.riskIfSkipped,
        '如果跳过这一步，后面很可能反复卡住，同样的问题会不断重来。',
      ),
      ctaLabel: text(preview.nextStepLabel || preview.nextStep, '确认并开始'),
      estimate: formatPlanMinutes(estimatedMinutes),
    },
    signals: [
      {
        key: 'goal',
        label: '目标',
        value: text(preview.learnerGoal || preview.context.goalText, '先把当前最值得推进的一步学明白'),
      },
      {
        key: 'state',
        label: '当前状态判断',
        value: text(
          preview.personalization?.learnerState || preview.currentFocus || preview.keyWeaknesses[0],
          '方向明确，但关键基础还不够稳。',
        ),
      },
      {
        key: 'evidence',
        label: '证据强度',
        value: text(
          preview.confidenceExplanation || preview.confidence,
          '证据足够支持先补关键薄弱点。',
        ),
      },
      {
        key: 'start',
        label: '推荐起点',
        value: taskTitle,
      },
      {
        key: 'strategy',
        label: '策略倾向',
        value: strategySection.recommended.title,
      },
      ...whatISaw.slice(0, 2).map((item, index) => ({
        key: `signal-${index + 1}`,
        label: `观察 ${index + 1}`,
        value: item,
      })),
    ],
    strategy: strategySection,
    kickoff: {
      firstAction: text(
        preview.firstAction || preview.taskPreviews[0]?.learningGoal,
        '确认后先完成这一轮最关键的理解动作。',
      ),
      systemLead: text(
        preview.guidance || preview.taskPreviews[0]?.aiSupport || preview.nextStepNote,
        '我会先带你过一轮关键理解，再根据你的反馈即时调整节奏。',
      ),
      firstCheckpoint: text(
        preview.firstCheckpoint || preview.taskPreviews[0]?.learnerAction,
        '第一检查点是你能否独立解释核心思路并完成一次短练习。',
      ),
      ifPerformWell: text(
        preview.ifPerformWell,
        '如果你表现稳定，下一轮会直接加快推进。',
      ),
      ifStillStruggle: text(
        preview.ifStillStruggle,
        '如果仍有卡点，下一轮会先补缺口再推进新内容。',
      ),
      ifNoTime: text(
        preview.ifNoTime,
        '如果今天时间少，可以先切到 10 分钟压缩版，保住连续性。',
      ),
      steps: buildKickoffSteps(preview),
    },
  };
}
