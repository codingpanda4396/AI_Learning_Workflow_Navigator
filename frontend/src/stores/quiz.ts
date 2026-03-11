import { defineStore } from 'pinia';
import { fetchQuizApi, fetchQuizStatusApi, generateQuizApi, submitQuizApi } from '@/api/modules/quiz';
import type { QuizAnswerPayload, QuizSnapshot } from '@/types/quiz';

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
        this.status = this.quiz.generationStatus?.toLowerCase() ?? 'generating';
        return this.quiz;
      } catch (error) {
        this.status = 'error';
        this.error = error instanceof Error ? error.message : '生成训练题失败';
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
        const generationStatus = this.quiz.generationStatus?.toUpperCase();
        const quizStatus = this.quiz.quizStatus?.toUpperCase();
        if (generationStatus === 'PENDING' || generationStatus === 'RUNNING') {
          this.status = 'generating';
        } else if (generationStatus === 'FAILED') {
          this.status = 'error';
        } else if (quizStatus === 'COMPLETED' || quizStatus === 'SUBMITTED') {
          this.status = 'completed';
        } else {
          this.status = 'ready';
        }
        return this.quiz;
      } catch (error) {
        this.status = 'error';
        this.error = error instanceof Error ? error.message : '获取训练状态失败';
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
        this.status = 'ready';
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
        this.status = 'completed';
      } catch (error) {
        this.status = 'error';
        this.error = error instanceof Error ? error.message : '提交答案失败';
        throw error;
      } finally {
        this.submitting = false;
      }
    },
  },
});
