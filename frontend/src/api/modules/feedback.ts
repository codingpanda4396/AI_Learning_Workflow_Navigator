import apiClient from '@/api/client';
import { mergeReportPayloads, normalizeGrowthDashboard } from '@/api/normalizers';
import type { GrowthDashboard, LearningReport } from '@/types/feedback';

export async function fetchReportApi(sessionId: number): Promise<LearningReport> {
  const [feedbackResponse, reportResponse, weakPointsResponse] = await Promise.allSettled([
    apiClient.get(`/api/sessions/${sessionId}/feedback`),
    apiClient.get(`/api/session/${sessionId}/learning-feedback/report`),
    apiClient.get(`/api/session/${sessionId}/learning-feedback/weak-points`),
  ]);

  return mergeReportPayloads({
    feedback: feedbackResponse.status === 'fulfilled' ? feedbackResponse.value.data : null,
    report: reportResponse.status === 'fulfilled' ? reportResponse.value.data : null,
    weakPoints: weakPointsResponse.status === 'fulfilled' ? weakPointsResponse.value.data : null,
  });
}

export async function submitNextActionApi(sessionId: number, action: string): Promise<LearningReport> {
  await apiClient.post(`/api/sessions/${sessionId}/next-action`, { action });
  return fetchReportApi(sessionId);
}

export async function fetchGrowthDashboardApi(sessionId: number): Promise<GrowthDashboard> {
  const { data } = await apiClient.get(`/api/session/${sessionId}/growth-dashboard`);
  return normalizeGrowthDashboard(data);
}
