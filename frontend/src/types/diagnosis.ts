export type DiagnosisQuestionType = 'single_choice' | 'multiple_choice' | 'text';

export interface DiagnosisQuestion {
  questionId: string;
  dimension: string;
  type: DiagnosisQuestionType;
  title: string;
  description?: string;
  options?: string[];
  required: boolean;
  placeholder?: string;
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
