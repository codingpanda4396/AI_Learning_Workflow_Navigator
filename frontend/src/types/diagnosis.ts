import type { CodeLabel } from '@/types/common';

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
  dimension: CodeLabel;
  type: CodeLabel;
  title?: string;
  description?: string;
  options?: CodeLabel[];
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
  fallbackApplied?: boolean;
  fallbackReasons?: string[];
  contentSource?: string;
}

export type DiagnosisAnswerValue = string | string[];

export interface DiagnosisAnswer {
  questionId: string;
  answerCodes?: string[];
  answerText?: string;
}

export interface CapabilityProfile {
  currentLevel: CodeLabel;
  strengths: string[];
  weaknesses: string[];
  learningPreference?: CodeLabel;
  timeBudget?: CodeLabel;
  goalOrientation?: CodeLabel;
  summary?: string;
  planExplanation?: string;
}

export interface DiagnosisNextAction {
  code: string;
  label: string;
}

export interface DiagnosisSubmitPayload {
  diagnosisId: string;
  answers: DiagnosisAnswer[];
}

export interface DiagnosisSubmitResponse {
  capabilityProfile: CapabilityProfile;
  nextAction?: DiagnosisNextAction;
  fallbackApplied?: boolean;
  fallbackReasons?: string[];
  contentSource?: string;
}

const DEFAULT_QUESTION_DESCRIPTION = 'Answer based on your real learning situation. This is not a test.';
const DEFAULT_TEXT_PLACEHOLDER = 'You can briefly describe your current habit, pace, or challenge.';
const DEFAULT_SUBMIT_HINT = 'Your answers will be used to build the capability profile.';
const DEFAULT_PROFILE_SUMMARY = 'Your capability profile has been generated from the diagnosis answers.';
const DEFAULT_PROFILE_PLAN_EXPLANATION = 'The next learning plan will use this profile as an input.';

const diagnosisQuestionCopyByDimension: Record<string, Required<DiagnosisQuestionCopy>> = {
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

export function resolveDiagnosisQuestionCopy(question: DiagnosisQuestion): Required<DiagnosisQuestionCopy> {
  const dimensionKey = normalizeDimensionKey(question.dimension.code);
  const defaultCopy = diagnosisQuestionCopyByDimension[dimensionKey] ?? {
    sectionLabel: dimensionKey || question.dimension.code || 'Diagnosis',
    title: 'Please answer based on your current situation.',
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
