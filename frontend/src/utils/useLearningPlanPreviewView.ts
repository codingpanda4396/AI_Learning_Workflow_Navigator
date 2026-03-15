import type { LearningPlanPreview } from '@/types/learningPlan';
import { formatPlanMinutes } from '@/utils/normalizePlanPreview';
import {
  cleanText,
  formatPreviewDisplayTitle,
  pickCompactList,
  productCopy,
} from '@/utils/learningPlanDisplay';

/** 四区块：为什么从这一步开始 / 这一小步做什么 / 为什么适合你 / 确认后获得什么 */
export interface LearningPlanPreviewViewModel {
  hero: {
    goal: string;
    startPoint: string;
    oneLineReason: string;
  };
  taskCard: {
    title: string;
    estimatedTime: string;
    goal: string;
    tasks: string[];
    completionGains: string[];
  };
  whyFitsYou: string[];
  afterConfirm: {
    expectedGain: string;
    startGuide: string;
  };
}

function buildHero(preview: LearningPlanPreview): LearningPlanPreviewViewModel['hero'] {
  const goal = productCopy(
    preview.context?.goalText || preview.learnerGoal,
    '把当前最值得推进的一步学明白',
  );
  const startPoint = formatPreviewDisplayTitle(
    preview.summary?.recommendedStartNode?.displayName
      || preview.summary?.recommendedStartNode?.nodeName
      || preview.recommendedEntry?.title
      || preview.currentFocus
      || preview.priorityNodes?.[0]?.title,
    '当前推荐起步点',
  );
  const oneLineReason = productCopy(
    preview.whyStartHere
      || preview.whyThisStep
      || preview.personalizedReasons?.whyThisStepFirst
      || preview.personalizedReasons?.whyRecommended
      || preview.recommendedEntry?.reason
      || preview.reasons?.[0]?.description
      || preview.nextStepNote,
    '先完成这一步，后续会更顺。',
  );
  return { goal, startPoint, oneLineReason };
}

function buildTaskCard(preview: LearningPlanPreview): LearningPlanPreviewViewModel['taskCard'] {
  const firstTask = preview.nextActionsDetail?.[0];
  const fallbackTitle = cleanText(
    preview.recommendedEntry?.title || preview.taskPreviews?.[0]?.title,
    '完成当前关键学习任务',
  );
  const estimatedMinutes = preview.currentTaskCard?.estimatedMinutes
    ?? firstTask?.estimatedMinutes
    ?? preview.recommendedEntry?.estimatedMinutes
    ?? preview.taskPreviews?.[0]?.estimatedTaskMinutes;
  const tasks = preview.currentTaskCard?.tasks?.length
    ? pickCompactList(preview.currentTaskCard.tasks, [], 5)
    : pickCompactList(
      [
        ...((preview.nextActionsDetail ?? []).map((item) => item.learnerAction || item.title)),
        ...(preview.nextActionsV2 ?? []),
      ],
      ['明确本步关键概念', '完成一个最小练习', '自测并确认掌握'],
      5,
    );
  const completionGains = preview.currentTaskCard?.completionGains?.length
    ? pickCompactList(preview.currentTaskCard.completionGains, [], 3)
    : pickCompactList(
      [preview.expectedGain, preview.taskPreviews?.[0]?.completionCriteria, preview.taskPreviews?.[0]?.learningGoal],
      ['能解释本步核心概念', '能独立完成基础练习'],
      3,
    );

  return {
    title: formatPreviewDisplayTitle(preview.currentTaskCard?.title || fallbackTitle, '完成当前关键学习任务'),
    estimatedTime: formatPlanMinutes(estimatedMinutes),
    goal: cleanText(preview.currentTaskCard?.goal || preview.taskPreviews?.[0]?.learningGoal, '先完成这一步，再进入下一阶段。'),
    tasks,
    completionGains,
  };
}

/** 融合 explanationPanel / personalizedReasons / whyThisStep / keyEvidence，去重为 1～2 条短句 */
function buildWhyFitsYou(preview: LearningPlanPreview): string[] {
  const candidates: string[] = [];
  const add = (raw: unknown, fallback: string) => {
    const s = productCopy(raw, fallback, 80);
    if (s !== fallback && !candidates.includes(s)) candidates.push(s);
  };
  add(preview.personalizedReasons?.whyRecommended, '');
  add(preview.personalizedReasons?.whyThisStepFirst, '');
  add(preview.whyThisStep, '');
  add(preview.whyStartHere, '');
  add(preview.explanationPanel?.learnerProfile, '');
  add(preview.learnerSnapshotV2?.currentState, '');
  add(preview.explanationPanel?.systemDecision, '');
  add(preview.recommendedStrategy?.explanation, '');
  if (preview.keyEvidence?.length) {
    const one = productCopy(preview.keyEvidence[0], '', 60);
    if (one && !candidates.includes(one)) candidates.push(one);
  }
  const out = candidates.filter(Boolean).slice(0, 2);
  if (out.length) return out;
  return ['根据你当前状态，先做这一步最合适。'];
}

function buildAfterConfirm(preview: LearningPlanPreview): LearningPlanPreviewViewModel['afterConfirm'] {
  return {
    expectedGain: productCopy(
      preview.expectedGain
        || preview.currentTaskCard?.completionGains?.[0]
        || preview.taskPreviews?.[0]?.completionCriteria,
      '完成后你会更容易进入下一步，系统会根据表现继续为你规划。',
    ),
    startGuide: productCopy(
      preview.startGuide,
      '确认后立即进入第一步，按你的完成情况实时调节。',
      80,
    ),
  };
}

export function buildLearningPlanPreviewView(preview: LearningPlanPreview): LearningPlanPreviewViewModel {
  return {
    hero: buildHero(preview),
    taskCard: buildTaskCard(preview),
    whyFitsYou: buildWhyFitsYou(preview),
    afterConfirm: buildAfterConfirm(preview),
  };
}
