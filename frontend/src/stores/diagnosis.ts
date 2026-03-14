import { defineStore } from 'pinia';
import type { DiagnosisAnswerValue, DiagnosisQuestion } from '@/types/diagnosis';

interface DiagnosisState {
  currentQuestion: DiagnosisQuestion | null;
  answers: Record<string, DiagnosisAnswerValue>;
  currentIndex: number;
}

export const useDiagnosisStore = defineStore('diagnosis', {
  state: (): DiagnosisState => ({
    currentQuestion: null,
    answers: {},
    currentIndex: 0,
  }),
  actions: {
    setCurrentQuestion(question: DiagnosisQuestion | null) {
      this.currentQuestion = question;
    },
    setCurrentIndex(index: number) {
      this.currentIndex = Math.max(index, 0);
    },
    updateAnswer(questionId: string, value: DiagnosisAnswerValue) {
      this.answers = {
        ...this.answers,
        [questionId]: value,
      };
    },
    reset() {
      this.currentQuestion = null;
      this.answers = {};
      this.currentIndex = 0;
    },
  },
});
