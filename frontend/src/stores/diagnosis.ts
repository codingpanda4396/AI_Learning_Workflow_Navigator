import { defineStore } from 'pinia';
import { generateDiagnosisApi, submitDiagnosisApi } from '@/api/modules/diagnosis';
import type {
  CapabilityProfile,
  DiagnosisAnswer,
  DiagnosisAnswerValue,
  DiagnosisQuestion,
  DiagnosisSubmitResponse,
} from '@/types/diagnosis';

interface DiagnosisState {
  diagnosisId: string;
  questions: DiagnosisQuestion[];
  currentQuestionIndex: number;
  answers: Record<string, DiagnosisAnswerValue>;
  capabilityProfile: CapabilityProfile | null;
  nextAction: DiagnosisSubmitResponse['nextAction'] | null;
  loading: boolean;
  submitting: boolean;
  error: string;
}

export const useDiagnosisStore = defineStore('diagnosis', {
  state: (): DiagnosisState => ({
    diagnosisId: '',
    questions: [],
    currentQuestionIndex: 0,
    answers: {},
    capabilityProfile: null,
    nextAction: null,
    loading: false,
    submitting: false,
    error: '',
  }),
  actions: {
    async generateDiagnosis(sessionId: string) {
      this.loading = true;
      this.error = '';
      this.capabilityProfile = null;
      this.nextAction = null;
      this.currentQuestionIndex = 0;
      this.answers = {};

      try {
        const response = await generateDiagnosisApi(sessionId);
        this.diagnosisId = response.diagnosisId;
        this.questions = response.questions;
        return response;
      } catch (error) {
        this.error = error instanceof Error ? error.message : '生成诊断问题失败，请稍后重试。';
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
          .map((question) => ({
            questionId: question.questionId,
            value: this.answers[question.questionId],
          }));

        const response = await submitDiagnosisApi(this.diagnosisId, answers);
        this.capabilityProfile = response.capabilityProfile;
        this.nextAction = response.nextAction ?? null;
        return response;
      } catch (error) {
        this.error = error instanceof Error ? error.message : '提交诊断回答失败，请稍后重试。';
        throw error;
      } finally {
        this.submitting = false;
      }
    },
    reset() {
      this.diagnosisId = '';
      this.questions = [];
      this.currentQuestionIndex = 0;
      this.answers = {};
      this.capabilityProfile = null;
      this.nextAction = null;
      this.loading = false;
      this.submitting = false;
      this.error = '';
    },
  },
});
