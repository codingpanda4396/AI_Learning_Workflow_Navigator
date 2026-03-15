import apiClient from '@/api/client';
import { getGrowthDashboardMock, getLearningReportMock } from '@/api/learnMocks';
import { normalizeGrowthDashboard, normalizeSessionReport } from '@/api/normalizers';
import type { GrowthDashboard, LearningReport } from '@/types/feedback';

const useLearnMock = import.meta.env.VITE_USE_LEARN_MOCK === 'true';

export async function fetchReportApi(sessionId: number): Promise<LearningReport> {
  if (useLearnMock) return Promise.resolve(getLearningReportMock(sessionId));
  const { data } = await apiClient.get(`/api/sessions/${sessionId}/report`);
  return normalizeSessionReport(data);
}

export async function submitNextActionApi(sessionId: number, action: string): Promise<LearningReport> {
  if (useLearnMock) return Promise.resolve(getLearningReportMock(sessionId));
  await apiClient.post(`/api/sessions/${sessionId}/next-action`, { action });
  return fetchReportApi(sessionId);
}

export async function fetchGrowthDashboardApi(sessionId: number): Promise<GrowthDashboard> {
  if (useLearnMock) return Promise.resolve(getGrowthDashboardMock(sessionId));
  const { data } = await apiClient.get(`/api/sessions/${sessionId}/growth-dashboard`);
  return normalizeGrowthDashboard(data);
}
