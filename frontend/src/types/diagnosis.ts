export type DiagnosisQuestionType = 'single_choice' | 'multiple_choice' | 'text';

export interface DiagnosisQuestionCopy {
  sectionLabel?: string;
  title?: string;
  description?: string;
  placeholder?: string;
  submitHint?: string;
}

export interface DiagnosisQuestion {
  questionId: string;
  dimension: string;
  type: DiagnosisQuestionType;
  title?: string;
  description?: string;
  options?: string[];
  required: boolean;
  placeholder?: string;
  copy?: DiagnosisQuestionCopy;
}

export interface DiagnosisGeneratePayload {
  sessionId: string;
}

export interface DiagnosisGenerateResponse {
  diagnosisId: string;
  sessionId: string;
  questions: DiagnosisQuestion[];
}

export type DiagnosisAnswerValue = string | string[];

export interface DiagnosisAnswer {
  questionId: string;
  value: DiagnosisAnswerValue;
}

export interface CapabilityProfile {
  currentLevel: string;
  strengths: string[];
  weaknesses: string[];
  learningPreference?: string;
  timeBudget?: string;
  goalOrientation?: string;
  summary?: string;
  planExplanation?: string;
}

export interface DiagnosisNextAction {
  type: string;
  label: string;
}

export interface DiagnosisSubmitPayload {
  diagnosisId: string;
  answers: DiagnosisAnswer[];
}

export interface DiagnosisSubmitResponse {
  capabilityProfile: CapabilityProfile;
  nextAction?: DiagnosisNextAction;
}

const DEFAULT_QUESTION_DESCRIPTION = '按你现在的真实情况作答即可，这不是考试。';
const DEFAULT_TEXT_PLACEHOLDER = '你可以结合自己的学习习惯和时间安排简单描述。';
const DEFAULT_SUBMIT_HINT = '你的回答将帮助系统构建能力画像。';
const DEFAULT_PROFILE_SUMMARY = '系统已根据你的回答整理出当前能力画像。';
const DEFAULT_PROFILE_PLAN_EXPLANATION = '后续学习内容会参考这份画像进行安排和调整。';

const diagnosisQuestionCopyByDimension: Record<string, Required<DiagnosisQuestionCopy>> = {
  FOUNDATION: {
    sectionLabel: '基础诊断',
    title: '你觉得自己目前对这部分内容的掌握程度如何？',
    description: DEFAULT_QUESTION_DESCRIPTION,
    placeholder: DEFAULT_TEXT_PLACEHOLDER,
    submitHint: DEFAULT_SUBMIT_HINT,
  },
  KNOWLEDGE_FOUNDATION: {
    sectionLabel: '知识基础',
    title: '你觉得自己目前对这部分内容的掌握程度如何？',
    description: DEFAULT_QUESTION_DESCRIPTION,
    placeholder: DEFAULT_TEXT_PLACEHOLDER,
    submitHint: DEFAULT_SUBMIT_HINT,
  },
  LEARNING_PREFERENCE: {
    sectionLabel: '学习偏好',
    title: '说说你平时更适合什么样的学习方式。',
    description: '比如喜欢先看总结再做题，还是边学边练。',
    placeholder: DEFAULT_TEXT_PLACEHOLDER,
    submitHint: DEFAULT_SUBMIT_HINT,
  },
  DIFFICULTY_FOCUS: {
    sectionLabel: '难点聚焦',
    title: '你现在最容易卡住的地方有哪些？',
    description: DEFAULT_QUESTION_DESCRIPTION,
    placeholder: DEFAULT_TEXT_PLACEHOLDER,
    submitHint: DEFAULT_SUBMIT_HINT,
  },
};

function normalizeDimensionKey(dimension: string) {
  return dimension.trim().replace(/[\s-]+/g, '_').toUpperCase();
}

export function resolveDiagnosisQuestionCopy(question: DiagnosisQuestion): Required<DiagnosisQuestionCopy> {
  const dimensionKey = normalizeDimensionKey(question.dimension);
  const defaultCopy = diagnosisQuestionCopyByDimension[dimensionKey] ?? {
    sectionLabel: dimensionKey || question.dimension || '诊断',
    title: '请根据你当前的学习情况回答这个问题。',
    description: DEFAULT_QUESTION_DESCRIPTION,
    placeholder: DEFAULT_TEXT_PLACEHOLDER,
    submitHint: DEFAULT_SUBMIT_HINT,
  };

  return {
    sectionLabel: question.copy?.sectionLabel || defaultCopy.sectionLabel,
    title: question.copy?.title || question.title || defaultCopy.title,
    description: question.copy?.description || question.description || defaultCopy.description,
    placeholder: question.copy?.placeholder || question.placeholder || defaultCopy.placeholder,
    submitHint: question.copy?.submitHint || defaultCopy.submitHint,
  };
}

export function resolveCapabilityProfileCopy(profile: CapabilityProfile) {
  return {
    summary: profile.summary || DEFAULT_PROFILE_SUMMARY,
    planExplanation: profile.planExplanation || DEFAULT_PROFILE_PLAN_EXPLANATION,
  };
}
