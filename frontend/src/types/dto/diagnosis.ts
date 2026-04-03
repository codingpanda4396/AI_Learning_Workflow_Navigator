export interface DiagnosisOption {
  code: string
  label: string
  order?: number
}

export interface DiagnosisQuestion {
  questionId: string
  dimension: string
  type: string
  required: boolean
  title: string
  description?: string
  whyAsking?: string
  impactsPlanning?: string[]
  options: DiagnosisOption[]
}

export interface DiagnosisAnswer {
  questionId: string
  selectedOptions: string[]
  textAnswer?: string
}

export interface DiagnosisSessionData {
  diagnosisId: string
  sessionId: string
  status: string
  generationMode: string
  questions: DiagnosisQuestion[]
}

export interface LearnerProfileSnapshot {
  diagnosisId: string
  foundationLevel?: string
  executionStability?: string
  timeBudgetLevel?: string
  learningPreference?: string
  blockingPoint?: string
  urgencyLevel?: string
  blockerTags?: string[]
  riskTags?: string[]
}

export interface DiagnosisEvidenceSummary {
  summary?: string
  keyEvidence?: string[]
  primaryGapType?: string
  primaryRiskTags?: string[]
  explanationPoints?: string[]
}

export interface SubmitDiagnosisData {
  diagnosisId: string
  learnerProfileSnapshot: LearnerProfileSnapshot
  diagnosisEvidenceSummary: DiagnosisEvidenceSummary
}
