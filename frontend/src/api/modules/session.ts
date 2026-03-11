import apiClient from '@/api/client';
import { normalizeCurrentSession, normalizeOverview } from '@/api/normalizers';
import type { CurrentSessionInfo, SessionCreatePayload, SessionOverview } from '@/types/session';

export async function createSessionApi(payload: SessionCreatePayload) {
  const { data } = await apiClient.post('/api/session/create', {
    course_id: payload.courseId,
    chapter_id: payload.chapterId,
    goal_text: payload.goalText,
  });
  return data as { session_id?: number };
}

export async function planSessionApi(sessionId: number) {
  const { data } = await apiClient.post(`/api/session/${sessionId}/plan?mode=auto`);
  return data as Record<string, unknown>;
}

export async function fetchOverviewApi(sessionId: number): Promise<SessionOverview> {
  const { data } = await apiClient.get(`/api/session/${sessionId}/overview`);
  return normalizeOverview(data);
}

export async function fetchCurrentSessionApi(): Promise<CurrentSessionInfo | null> {
  const { data } = await apiClient.get('/api/session/current');
  return normalizeCurrentSession(data);
}
