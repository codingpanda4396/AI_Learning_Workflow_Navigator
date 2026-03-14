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

const DEFAULT_QUESTION_DESCRIPTION = 'Answer based on your real learning situation. This is not a test.';
const DEFAULT_TEXT_PLACEHOLDER = 'You can briefly describe your current habit, pace, or challenge.';
const DEFAULT_SUBMIT_HINT = 'Your answers will be used to build the capability profile.';
const DEFAULT_PROFILE_SUMMARY = 'Your capability profile has been generated from the diagnosis answers.';
const DEFAULT_PROFILE_PLAN_EXPLANATION = 'The next learning plan will use this profile as an input.';

const diagnosisQuestionCopyByDimension: Record<string, Required<Pick<DiagnosisQuestion, 'sectionLabel' | 'title' | 'description' | 'placeholder' | 'submitHint'>>> = {
  FOUNDATION: {
    sectionLabel: 'Foundation',
    title: 'How solid is your current foundation on this topic?',
    description: DEFAULT_QUESTION_DESCRIPTION,
    placeholder: DEFAULT_TEXT_PLACEHOLDER,
    submitHint: DEFAULT_SUBMIT_HINT,
  },
  EXPERIENCE: {
    sectionLabel: 'Experience',
    title: 'What related experience have you had before?',
    description: DEFAULT_QUESTION_DESCRIPTION,
    placeholder: DEFAULT_TEXT_PLACEHOLDER,
    submitHint: DEFAULT_SUBMIT_HINT,
  },
  GOAL_STYLE: {
    sectionLabel: 'Goal',
    title: 'What is your main learning goal this round?',
    description: DEFAULT_QUESTION_DESCRIPTION,
    placeholder: DEFAULT_TEXT_PLACEHOLDER,
    submitHint: DEFAULT_SUBMIT_HINT,
  },
  TIME_BUDGET: {
    sectionLabel: 'Time',
    title: 'How much time can you invest each week?',
    description: DEFAULT_QUESTION_DESCRIPTION,
    placeholder: DEFAULT_TEXT_PLACEHOLDER,
    submitHint: DEFAULT_SUBMIT_HINT,
  },
  LEARNING_PREFERENCE: {
    sectionLabel: 'Preference',
    title: 'Which learning style suits you best?',
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
    sectionLabel: dimensionKey || 'Diagnosis',
    title: 'Please answer based on your current situation.',
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
