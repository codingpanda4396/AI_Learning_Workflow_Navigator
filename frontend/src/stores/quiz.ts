import { defineStore } from 'pinia';
import { fetchQuizApi, fetchQuizStatusApi, generateQuizApi, submitQuizApi } from '@/api/modules/quiz';
import type { QuizAnswerPayload, QuizSnapshot } from '@/types/quiz';

function resolveQuizViewStatus(quiz: QuizSnapshot | null) {
  const generationStatus = quiz?.generationStatus;
  const quizStatus = quiz?.quizStatus;

  if (generationStatus === 'PENDING' || generationStatus === 'RUNNING' || quizStatus === 'GENERATING') {
    return 'generating';
  }
  if (generationStatus === 'FAILED' || quizStatus === 'FAILED') {
    return 'failed';
  }
  switch (quizStatus) {
    case 'READY':
      return 'ready';
    case 'ANSWERING':
      return 'answering';
    case 'REVIEWING':
      return 'reviewing';
    case 'REPORT_READY':
      return 'report-ready';
    case 'NEXT_ROUND':
      return 'next-round';
    default:
      return 'idle';
  }
}

export const useQuizStore = defineStore('quiz', {
  state: () => ({
    quiz: null as QuizSnapshot | null,
    status: 'idle',
    loading: false,
    submitting: false,
    error: '',
  }),
  actions: {
    async generateQuiz(sessionId: number) {
      this.loading = true;
      this.error = '';
      this.status = 'generating';
      try {
        this.quiz = await generateQuizApi(sessionId);
        this.status = resolveQuizViewStatus(this.quiz);
        return this.quiz;
      } catch (error) {
        this.status = 'failed';
        this.error = error instanceof Error ? error.message : '获取练习题失败';
        throw error;
      } finally {
        this.loading = false;
      }
    },
    async fetchQuizStatus(sessionId: number) {
      this.loading = true;
      this.error = '';
      try {
        this.quiz = await fetchQuizStatusApi(sessionId);
        this.status = resolveQuizViewStatus(this.quiz);
        return this.quiz;
      } catch (error) {
        this.status = 'failed';
        this.error = error instanceof Error ? error.message : '获取练习状态失败';
        throw error;
      } finally {
        this.loading = false;
      }
    },
    async fetchQuiz(sessionId: number) {
      this.loading = true;
      this.error = '';
      try {
        this.quiz = await fetchQuizApi(sessionId);
        this.status = resolveQuizViewStatus(this.quiz);
        return this.quiz;
      } catch (error) {
        this.error = error instanceof Error ? error.message : '获取题目失败';
        throw error;
      } finally {
        this.loading = false;
      }
    },
    async submitQuiz(sessionId: number, answers: QuizAnswerPayload[]) {
      this.submitting = true;
      this.error = '';
      this.status = 'submitting';
      try {
        await submitQuizApi(sessionId, answers);
        this.status = 'report-ready';
      } catch (error) {
        this.status = 'failed';
        this.error = error instanceof Error ? error.message : '提交答案失败';
        throw error;
      } finally {
        this.submitting = false;
      }
    },
  },
});
