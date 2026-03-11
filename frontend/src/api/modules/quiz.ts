import apiClient from '@/api/client';
import { normalizeQuizSnapshot } from '@/api/normalizers';
import type { QuizAnswerPayload, QuizSnapshot } from '@/types/quiz';

export async function generateQuizApi(sessionId: number): Promise<QuizSnapshot> {
  const { data } = await apiClient.post(`/api/sessions/${sessionId}/quiz/generate`);
  return normalizeQuizSnapshot(data);
}

export async function fetchQuizStatusApi(sessionId: number): Promise<QuizSnapshot> {
  const { data } = await apiClient.get(`/api/sessions/${sessionId}/quiz/status`);
  return normalizeQuizSnapshot(data);
}

export async function fetchQuizApi(sessionId: number): Promise<QuizSnapshot> {
  const { data } = await apiClient.get(`/api/sessions/${sessionId}/quiz`);
  return normalizeQuizSnapshot(data);
}

export async function submitQuizApi(sessionId: number, answers: QuizAnswerPayload[]) {
  const { data } = await apiClient.post(`/api/sessions/${sessionId}/quiz/submit`, {
    answers: answers.map((item) => ({
      question_id: item.questionId,
      answer: item.answer,
    })),
  });
  return data as Record<string, unknown>;
}
