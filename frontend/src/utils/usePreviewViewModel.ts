import type { LearningPlanPreview } from '@/types/learningPlan';
import { formatPlanMinutes } from '@/utils/normalizePlanPreview';

export interface PreviewDecisionViewModel {
  hero: {
    title: string;
    subtitle: string;
    ctaLabel: string;
    task: {
      learnerAction: string;
      expectedArtifact: string;
      completionCriteria: string;
      estimate: string;
      aiSupport: string;
      checkMethod: string;
    };
    secondaryActions: string[];
  };
  explanation: {
    whyThisStep: string;
    evidence: string[];
    confidenceHint: string;
  };
  outcomes: {
    expectedGain: string;
    skipRisk: string;
  };
  kickoff: {
    actions: string[];
    systemGuide: string;
  };
}

function text(value: unknown, fallback: string): string {
  const result = String(value ?? '').replace(/\s+/g, ' ').trim();
  return result || fallback;
}

function normalizeConceptTitle(value: string): string {
  const source = value.trim();
  if (!source) {
    return '当前关键知识点';
  }
  if (!/[a-zA-Z]/.test(source)) {
    return source;
  }
  const dict: Record<string, string> = {
    'tree basics': '树的基础结构',
    'binary tree traversal': '二叉树遍历',
    'linked list': '链表',
    'stack': '栈',
    'queue': '队列',
    'hash table': '哈希表',
  };
  const key = source.toLowerCase();
  if (dict[key]) {
    return dict[key];
  }
  return `知识点：${source}`;
}

export function buildPreviewViewModel(preview: LearningPlanPreview): PreviewDecisionViewModel {
  const recommendedTitle = normalizeConceptTitle(preview.recommendedEntry?.title || '');
  const primaryTask = preview.nextActionsDetail?.[0];
  const fallbackTask = preview.taskPreviews?.[0];
  const secondaryActions = (preview.nextActionsDetail ?? [])
    .slice(1, 4)
    .map((item) => item.title)
    .filter(Boolean);
  const evidence = (preview.learnerSnapshotV2?.evidence ?? [])
    .map((item) => item.trim())
    .filter(Boolean)
    .slice(0, 3);

  return {
    hero: {
      title: `现在先做「${recommendedTitle}」`,
      subtitle: text(preview.whyThisStep || preview.recommendedEntry?.reason, '先补稳这一块，后面的学习会更顺。'),
      ctaLabel: '确认并进入',
      task: {
        learnerAction: text(
          primaryTask?.learnerAction ?? fallbackTask?.learnerAction ?? primaryTask?.title,
          '按系统给出的步骤完成这一步学习。',
        ),
        expectedArtifact: text(
          primaryTask?.expectedArtifact,
          '形成一份你可复述的要点清单或简短笔记。',
        ),
        completionCriteria: text(
          primaryTask?.completionCriteria,
          '你能用自己的话解释关键概念，并完成一个对应小练习。',
        ),
        estimate: formatPlanMinutes(primaryTask?.estimatedMinutes ?? fallbackTask?.estimatedTaskMinutes ?? preview.recommendedEntry?.estimatedMinutes),
        aiSupport: text(
          primaryTask?.aiSupport ?? fallbackTask?.aiSupport,
          'AI 会按当前步骤提供提示、示例和纠错反馈。',
        ),
        checkMethod: text(
          primaryTask?.checkMethod,
          '用 1 道自测题或一段口头讲解来确认自己已掌握。',
        ),
      },
      secondaryActions: secondaryActions.length ? secondaryActions : (preview.nextActionsV2 ?? []).slice(1, 3),
    },
    explanation: {
      whyThisStep: text(
        preview.whyThisStep || preview.recommendedEntry?.reason,
        '系统优先推荐这一步，因为它最能减少后续学习卡点。',
      ),
      evidence: (preview.keyEvidence ?? evidence).length
        ? (preview.keyEvidence ?? evidence).slice(0, 3)
        : ['当前起点与后续路径紧密相关。', '先补关键步骤能降低后续卡住概率。', '系统会根据你的表现动态调整下一步。'],
      confidenceHint: text(
        preview.confidenceHint,
        preview.explanationGenerated ? '当前推荐基于近期学习证据，可信度中等偏高。' : '当前证据有限，系统会在你完成第一步后继续校准。',
      ),
    },
    outcomes: {
      expectedGain: text(preview.expectedGain, '完成后你会更容易进入后续训练，并且错因定位会更聚焦。'),
      skipRisk: text(preview.skipRisk, '如果跳过这一步，后续节点更容易反复卡住。'),
    },
    kickoff: {
      actions: (preview.nextActionsV2 ?? []).slice(0, 3),
      systemGuide: text(
        preview.startGuide || (preview.explanationGenerated ? '' : '确认后系统会带你进入第一步，并根据你的完成情况实时调节节奏。'),
        '确认后系统会带你进入第一步，并根据你的完成情况实时调节节奏。',
      ),
    },
  };
}
