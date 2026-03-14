import type { CodeLabel } from '@/types/common';

export type DiagnosisQuestionType = 'SINGLE_CHOICE' | 'MULTIPLE_CHOICE' | 'TEXT';
export type DiagnosisStatus = 'GENERATED' | 'SUBMITTED' | 'PROFILED';

export interface DiagnosisQuestionOption {
  code: string;
  label: string;
  order?: number;
}

export interface DiagnosisQuestion {
  questionId: string;
  dimension: string;
  type: DiagnosisQuestionType;
  required: boolean;
  options?: DiagnosisQuestionOption[];
  title?: string;
  description?: string;
  placeholder?: string;
  submitHint?: string;
  sectionLabel?: string;
}

export interface DiagnosisGeneratePayload {
  sessionId: string;
}

export interface DiagnosisFallback {
  applied: boolean;
  reasons: string[];
  contentSource?: CodeLabel;
}

export interface DiagnosisMetadata {
  questionCount?: number;
  answerCount?: number;
  profileVersion?: number;
}

export interface DiagnosisActionTarget {
  route: string;
  params?: Record<string, string | number>;
}

export interface DiagnosisNextAction {
  code: string;
  label: string;
  target?: DiagnosisActionTarget;
}

export interface DiagnosisGenerateResponse {
  diagnosisId: string;
  sessionId: string;
  status: DiagnosisStatus;
  questions: DiagnosisQuestion[];
  nextAction?: DiagnosisNextAction;
  fallback: DiagnosisFallback;
  metadata?: DiagnosisMetadata;
}

export type DiagnosisAnswerValue = string | string[];

export interface DiagnosisAnswer {
  questionId: string;
  selectedOptionCode?: string;
  selectedOptionCodes?: string[];
  text?: string;
}

export interface CapabilityProfile {
  currentLevel: CodeLabel;
  strengths: string[];
  weaknesses: string[];
  learningPreference?: CodeLabel;
  timeBudget?: CodeLabel;
  goalOrientation?: CodeLabel;
}

export interface DiagnosisInsights {
  summary?: string;
  planExplanation?: string;
}

export interface DiagnosisSubmitPayload {
  diagnosisId: string;
  answers: DiagnosisAnswer[];
}

export interface DiagnosisSubmitResponse {
  diagnosisId: string;
  sessionId: string;
  status: DiagnosisStatus;
  capabilityProfile: CapabilityProfile;
  insights?: DiagnosisInsights;
  nextAction?: DiagnosisNextAction;
  fallback: DiagnosisFallback;
  metadata?: DiagnosisMetadata;
}

const DEFAULT_QUESTION_DESCRIPTION = '请根据你当前的真实情况作答。这不是考试，只是帮助系统更准确地理解你的学习起点。';
const DEFAULT_TEXT_PLACEHOLDER = '可以简单描述你当前的经验、习惯，或最近遇到的学习阻碍。';
const DEFAULT_SUBMIT_HINT = '你的回答将用于生成能力快照，并据此安排下一步学习路径。';
const DEFAULT_PROFILE_SUMMARY = '系统已经根据你的回答整理出当前能力快照，接下来会据此安排更贴合你的学习路径。';
const DEFAULT_PROFILE_PLAN_EXPLANATION = '后续学习规划会基于这份能力快照继续细化，帮助你从当前起点更顺畅地往前推进。';

const diagnosisStatusLabelMap: Record<string, string> = {
  GENERATED: '诊断进行中',
  SUBMITTED: '已完成问答',
  PROFILED: '能力快照已生成',
};

const diagnosisSourceLabelMap: Record<string, string> = {
  LLM: 'AI 生成',
  RULE_TEMPLATE: '规则生成',
  RULE_ENGINE: '规则生成',
  CONTRACT_RESPONSE: '系统生成',
};

const diagnosisFallbackReasonLabelMap: Record<string, string> = {
  PROFILE_SUMMARY_TIMEOUT: '本次摘要已切换为稳定版本展示',
};

const diagnosisQuestionCopyByDimension: Record<string, Required<Pick<DiagnosisQuestion, 'sectionLabel' | 'title' | 'description' | 'placeholder' | 'submitHint'>>> = {
  FOUNDATION: {
    sectionLabel: '基础',
    title: '你在这个主题上的基础大致如何？',
    description: DEFAULT_QUESTION_DESCRIPTION,
    placeholder: DEFAULT_TEXT_PLACEHOLDER,
    submitHint: DEFAULT_SUBMIT_HINT,
  },
  EXPERIENCE: {
    sectionLabel: '经验',
    title: '你之前接触过哪些相关内容或实践？',
    description: DEFAULT_QUESTION_DESCRIPTION,
    placeholder: DEFAULT_TEXT_PLACEHOLDER,
    submitHint: DEFAULT_SUBMIT_HINT,
  },
  GOAL_STYLE: {
    sectionLabel: '目标',
    title: '这一次学习里，你最想优先达成什么？',
    description: DEFAULT_QUESTION_DESCRIPTION,
    placeholder: DEFAULT_TEXT_PLACEHOLDER,
    submitHint: DEFAULT_SUBMIT_HINT,
  },
  TIME_BUDGET: {
    sectionLabel: '时间',
    title: '你每周大概能投入多少学习时间？',
    description: DEFAULT_QUESTION_DESCRIPTION,
    placeholder: DEFAULT_TEXT_PLACEHOLDER,
    submitHint: DEFAULT_SUBMIT_HINT,
  },
  LEARNING_PREFERENCE: {
    sectionLabel: '偏好',
    title: '哪种学习方式更适合你进入状态？',
    description: DEFAULT_QUESTION_DESCRIPTION,
    placeholder: DEFAULT_TEXT_PLACEHOLDER,
    submitHint: DEFAULT_SUBMIT_HINT,
  },
};

function normalizeDimensionKey(dimension: string) {
  return dimension.trim().replace(/[\s-]+/g, '_').toUpperCase();
}

export function resolveDiagnosisQuestionCopy(question: DiagnosisQuestion) {
  const dimensionKey = normalizeDimensionKey(question.dimension);
  const defaultCopy = diagnosisQuestionCopyByDimension[dimensionKey] ?? {
    sectionLabel: dimensionKey || '诊断',
    title: '请根据你当前的实际情况作答。',
    description: DEFAULT_QUESTION_DESCRIPTION,
    placeholder: DEFAULT_TEXT_PLACEHOLDER,
    submitHint: DEFAULT_SUBMIT_HINT,
  };

  return {
    sectionLabel: question.sectionLabel || defaultCopy.sectionLabel,
    title: question.title || defaultCopy.title,
    description: question.description || defaultCopy.description,
    placeholder: question.placeholder || defaultCopy.placeholder,
    submitHint: question.submitHint || defaultCopy.submitHint,
  };
}

export function resolveCapabilityProfileCopy(insights?: DiagnosisInsights | null) {
  return {
    summary: insights?.summary || DEFAULT_PROFILE_SUMMARY,
    planExplanation: insights?.planExplanation || DEFAULT_PROFILE_PLAN_EXPLANATION,
  };
}

export function resolveDiagnosisStatusLabel(status?: string) {
  const normalized = String(status || '').trim().toUpperCase();
  return diagnosisStatusLabelMap[normalized] || '已生成诊断结果';
}

export function resolveDiagnosisSourceLabel(source?: CodeLabel | null) {
  if (!source) {
    return '系统生成';
  }
  const normalized = String(source.code || source.label || '').trim().toUpperCase();
  const mapped = diagnosisSourceLabelMap[normalized];
  if (mapped) {
    return mapped;
  }
  if (source.label && source.label !== source.code) {
    return source.label;
  }
  return '系统生成';
}

export function resolveDiagnosisFallbackText(fallback?: DiagnosisFallback | null) {
  if (!fallback) {
    return '本次结果已按默认生成策略整理。';
  }
  if (!fallback.applied) {
    return '本次结果按标准生成流程整理。';
  }
  const reasons = fallback.reasons
    .map((reason) => diagnosisFallbackReasonLabelMap[String(reason).trim().toUpperCase()] || '部分说明已切换为稳定版本展示')
    .filter(Boolean);
  return reasons[0] || '部分说明已切换为稳定版本展示';
}

export function resolveDiagnosisMetaSummary(metadata?: DiagnosisMetadata | null) {
  if (!metadata) {
    return '本次诊断的辅助信息将在这里展示。';
  }

  const parts: string[] = [];
  if (typeof metadata.questionCount === 'number') {
    parts.push(`题目 ${metadata.questionCount} 道`);
  }
  if (typeof metadata.answerCount === 'number') {
    parts.push(`已回答 ${metadata.answerCount} 道`);
  }
  if (typeof metadata.profileVersion === 'number') {
    parts.push(`版本 v${metadata.profileVersion}`);
  }

  return parts.join(' / ') || '本次诊断的辅助信息将在这里展示。';
}
