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

const DEFAULT_QUESTION_DESCRIPTION = '请根据你的真实学习情况回答。这不是考试。';
const DEFAULT_TEXT_PLACEHOLDER = '可简要描述你当前的习惯、节奏或遇到的挑战。';
const DEFAULT_SUBMIT_HINT = '你的回答将用于生成能力画像。';
const DEFAULT_PROFILE_SUMMARY = '已根据诊断回答生成你的能力画像。';
const DEFAULT_PROFILE_PLAN_EXPLANATION = '下一份学习计划将以此画像为输入。';

const diagnosisQuestionCopyByDimension: Record<string, Required<Pick<DiagnosisQuestion, 'sectionLabel' | 'title' | 'description' | 'placeholder' | 'submitHint'>>> = {
  FOUNDATION: {
    sectionLabel: '基础',
    title: '你在这个主题上的基础如何？',
    description: DEFAULT_QUESTION_DESCRIPTION,
    placeholder: DEFAULT_TEXT_PLACEHOLDER,
    submitHint: DEFAULT_SUBMIT_HINT,
  },
  EXPERIENCE: {
    sectionLabel: '经验',
    title: '你之前有过哪些相关经验？',
    description: DEFAULT_QUESTION_DESCRIPTION,
    placeholder: DEFAULT_TEXT_PLACEHOLDER,
    submitHint: DEFAULT_SUBMIT_HINT,
  },
  GOAL_STYLE: {
    sectionLabel: '目标',
    title: '本轮你主要的学习目标是什么？',
    description: DEFAULT_QUESTION_DESCRIPTION,
    placeholder: DEFAULT_TEXT_PLACEHOLDER,
    submitHint: DEFAULT_SUBMIT_HINT,
  },
  TIME_BUDGET: {
    sectionLabel: '时间',
    title: '你每周能投入多少时间？',
    description: DEFAULT_QUESTION_DESCRIPTION,
    placeholder: DEFAULT_TEXT_PLACEHOLDER,
    submitHint: DEFAULT_SUBMIT_HINT,
  },
  LEARNING_PREFERENCE: {
    sectionLabel: '偏好',
    title: '哪种学习方式更适合你？',
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
    title: '请根据你当前的情况回答。',
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
