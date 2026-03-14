import type { LearningPlanPreview } from '@/types/learningPlan';
import { formatPlanMinutes } from '@/utils/normalizePlanPreview';

interface PreviewAlternativeVm {
  label: string;
  reason: string;
}

export interface PreviewDecisionViewModel {
  hero: {
    title: string;
    reason: string;
    estimate: string;
    ctaLabel: string;
  };
  aiObserved: {
    currentState: string;
    evidence: string[];
  };
  strategy: {
    recommendedLabel: string;
    explanation: string;
    alternatives: PreviewAlternativeVm[];
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
  const evidence = (preview.learnerSnapshotV2?.evidence ?? [])
    .map((item) => item.trim())
    .filter(Boolean)
    .slice(0, 3);

  const alternatives = (preview.alternativesV2 ?? [])
    .slice(0, 2)
    .map((item) => ({
      label: text(item.label, '备选方案'),
      reason: text(item.notRecommendedReason, '这次不优先，因为当前阶段更适合先补关键薄弱点。'),
    }));

  return {
    hero: {
      title: `下一步先学「${recommendedTitle}」`,
      reason: text(preview.recommendedEntry?.reason, '先补稳这一块，后面的学习会更顺。'),
      estimate: formatPlanMinutes(preview.recommendedEntry?.estimatedMinutes),
      ctaLabel: '确认并进入',
    },
    aiObserved: {
      currentState: text(preview.learnerSnapshotV2?.currentState, '你当前需要先补稳关键基础。'),
      evidence: evidence.length ? evidence : ['当前起点与后续路径紧密相关。', '先补关键步骤能降低后续卡住概率。', '系统会根据你的表现动态调整下一步。'],
    },
    strategy: {
      recommendedLabel: text(preview.recommendedStrategy?.label, '稳步推进'),
      explanation: text(
        preview.explanationGenerated ? preview.recommendedStrategy?.explanation : '这次先走更稳妥的路径，优先降低后续反复卡住的风险。',
        '这次先走更稳妥的路径，优先降低后续反复卡住的风险。',
      ),
      alternatives,
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
