import type { LearningPlanPreview } from '@/types/learningPlan';
import { formatPlanMinutes } from '@/utils/normalizePlanPreview';
import {
  cleanText,
  formatPreviewDisplayTitle,
  mapRhythmCodeToLabel,
  mapStrategyCodeToLabel,
  pickCompactList,
} from '@/utils/learningPlanDisplay';

export interface LearningPlanPreviewViewModel {
  summary: {
    title: string;
    description: string;
    tags: string[];
  };
  taskCard: {
    title: string;
    estimatedTime: string;
    goal: string;
    tasks: string[];
    completionGains: string[];
  };
  explanation: {
    whyRecommended: string;
    whyThisStepFirst: string;
    learnerProfile: string;
    systemDecision: string;
  };
}

function buildSummary(preview: LearningPlanPreview): LearningPlanPreviewViewModel['summary'] {
  const fallbackTitle = `为你安排：${formatPreviewDisplayTitle(preview.recommendedEntry?.title)}`;
  const fallbackDescription = cleanText(
    preview.learnerSnapshotV2?.currentState || preview.recommendedEntry?.reason,
    '根据你当前的学习状态，先完成这一步会更稳。',
  );
  const tags = preview.personalizedSummary?.tags?.length
    ? pickCompactList(preview.personalizedSummary.tags, [], 3)
    : pickCompactList(
      [
        mapStrategyCodeToLabel(preview.recommendedStrategy?.code, cleanText(preview.recommendedStrategy?.label, '稳步推进')),
        mapRhythmCodeToLabel(preview.adjustments?.intensity, cleanText(preview.summary?.recommendedRhythmLabel, '按当前节奏推进')),
        preview.adjustments?.prioritizeFoundation ? '先补基础' : '直接推进',
      ],
      ['个性化安排'],
      3,
    );

  return {
    title: cleanText(preview.personalizedSummary?.title, fallbackTitle),
    description: cleanText(preview.personalizedSummary?.description, fallbackDescription),
    tags,
  };
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
    ? pickCompactList(preview.currentTaskCard.tasks, [], 4)
    : pickCompactList(
      [
        ...((preview.nextActionsDetail ?? []).map((item) => item.learnerAction || item.title)),
        ...(preview.nextActionsV2 ?? []),
      ],
      ['明确本步关键概念', '完成一个最小练习', '自测并确认掌握情况'],
      4,
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

function buildExplanation(preview: LearningPlanPreview): LearningPlanPreviewViewModel['explanation'] {
  const evidence = preview.keyEvidence?.length ? `关键依据：${preview.keyEvidence.slice(0, 2).join('；')}` : '';
  return {
    whyRecommended: cleanText(
      preview.personalizedReasons?.whyRecommended || preview.whyThisStep || preview.recommendedEntry?.reason,
      '这一步最能直接解决你当前的关键卡点。',
    ),
    whyThisStepFirst: cleanText(
      preview.personalizedReasons?.whyThisStepFirst || preview.whyStartHere || preview.nextStepNote,
      '先完成这一步，后续学习会更顺畅。',
    ),
    learnerProfile: cleanText(
      preview.explanationPanel?.learnerProfile || preview.learnerSnapshotV2?.currentState || preview.profileDrivenReasoning,
      '你当前需要先补稳基础，再进入强化训练。',
    ),
    systemDecision: cleanText(
      preview.explanationPanel?.systemDecision || preview.recommendedStrategy?.explanation || evidence,
      '系统会根据你的完成情况动态调整下一步。',
    ),
  };
}

export function buildLearningPlanPreviewView(preview: LearningPlanPreview): LearningPlanPreviewViewModel {
  return {
    summary: buildSummary(preview),
    taskCard: buildTaskCard(preview),
    explanation: buildExplanation(preview),
  };
}
