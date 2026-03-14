import { defineStore } from 'pinia';
import { generateDiagnosisApi, submitDiagnosisApi } from '@/api/modules/diagnosis';
import type {
  CapabilityProfile,
  DiagnosisAnswer,
  DiagnosisAnswerValue,
  DiagnosisEvidenceSource,
  DiagnosisFallback,
  DiagnosisInsights,
  DiagnosisMetadata,
  DiagnosisNextAction,
  DiagnosisQuestion,
  DiagnosisReasoningStep,
} from '@/types/diagnosis';

interface DiagnosisState {
  diagnosisId: string;
  sessionId: string;
  questions: DiagnosisQuestion[];
  currentQuestionIndex: number;
  answers: Record<string, DiagnosisAnswerValue>;
  capabilityProfile: CapabilityProfile | null;
  insights: DiagnosisInsights | null;
  nextAction: DiagnosisNextAction | null;
  fallback: DiagnosisFallback | null;
  metadata: DiagnosisMetadata | null;
  reasoningSteps: DiagnosisReasoningStep[];
  strengthSources: DiagnosisEvidenceSource[];
  weaknessSources: DiagnosisEvidenceSource[];
  status: string;
  loading: boolean;
  submitting: boolean;
  error: string;
}

export const useDiagnosisStore = defineStore('diagnosis', {
  state: (): DiagnosisState => ({
    diagnosisId: '',
    sessionId: '',
    questions: [],
    currentQuestionIndex: 0,
    answers: {},
    capabilityProfile: null,
    insights: null,
    nextAction: null,
    fallback: null,
    metadata: null,
    reasoningSteps: [],
    strengthSources: [],
    weaknessSources: [],
    status: '',
    loading: false,
    submitting: false,
    error: '',
  }),
  actions: {
    async generateDiagnosis(sessionId: string) {
      this.loading = true;
      this.error = '';
      this.capabilityProfile = null;
      this.insights = null;
      this.nextAction = null;
      this.fallback = null;
      this.metadata = null;
      this.reasoningSteps = [];
      this.strengthSources = [];
      this.weaknessSources = [];
      this.status = '';
      this.currentQuestionIndex = 0;
      this.answers = {};

      try {
        const response = await generateDiagnosisApi(sessionId);
        this.diagnosisId = response.diagnosisId;
        this.sessionId = response.sessionId;
        this.questions = response.questions;
        this.nextAction = response.nextAction ?? null;
        this.fallback = response.fallback;
        this.metadata = response.metadata ?? null;
        this.reasoningSteps = [];
        this.strengthSources = [];
        this.weaknessSources = [];
        this.status = response.status;
        return response;
      } catch (error) {
        this.error = error instanceof Error ? error.message : '诊断问卷生成失败，请稍后重试。';
        throw error;
      } finally {
        this.loading = false;
      }
    },
    updateAnswer(questionId: string, value: DiagnosisAnswerValue) {
      this.answers = {
        ...this.answers,
        [questionId]: value,
      };
    },
    setCurrentQuestionIndex(index: number) {
      const maxIndex = Math.max(this.questions.length - 1, 0);
      this.currentQuestionIndex = Math.min(Math.max(index, 0), maxIndex);
    },
    async submitDiagnosis() {
      this.submitting = true;
      this.error = '';

      try {
        const answers: DiagnosisAnswer[] = this.questions
          .filter((question) => this.answers[question.questionId] !== undefined)
          .map((question) => {
            const value = this.answers[question.questionId];
            if (question.type === 'TEXT') {
              return {
                questionId: question.questionId,
                text: typeof value === 'string' ? value : '',
              };
            }
            if (question.type === 'MULTIPLE_CHOICE') {
              return {
                questionId: question.questionId,
                selectedOptionCodes: Array.isArray(value) ? value : typeof value === 'string' ? [value] : [],
              };
            }
            return {
              questionId: question.questionId,
              selectedOptionCode: typeof value === 'string' ? value : Array.isArray(value) ? value[0] : undefined,
            };
          });

        const response = await submitDiagnosisApi(this.diagnosisId, answers);
        this.capabilityProfile = response.capabilityProfile;
        this.insights = response.insights ?? null;
        this.nextAction = response.nextAction ?? null;
        this.fallback = response.fallback;
        this.metadata = response.metadata ?? null;
        this.reasoningSteps = response.reasoningSteps ?? [];
        this.strengthSources = response.strengthSources ?? [];
        this.weaknessSources = response.weaknessSources ?? [];
        this.status = response.status;
        return response;
      } catch (error) {
        this.error = error instanceof Error ? error.message : '诊断答案提交失败，请稍后重试。';
        throw error;
      } finally {
        this.submitting = false;
      }
    },
    reset() {
      this.diagnosisId = '';
      this.sessionId = '';
      this.questions = [];
      this.currentQuestionIndex = 0;
      this.answers = {};
      this.capabilityProfile = null;
      this.insights = null;
      this.nextAction = null;
      this.fallback = null;
      this.metadata = null;
      this.reasoningSteps = [];
      this.strengthSources = [];
      this.weaknessSources = [];
      this.status = '';
      this.loading = false;
      this.submitting = false;
      this.error = '';
    },
  },
});
