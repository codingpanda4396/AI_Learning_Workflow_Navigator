import apiClient from '@/api/client';
import { normalizeGrowthDashboard, normalizeSessionReport } from '@/api/normalizers';
import type { GrowthDashboard, LearningReport } from '@/types/feedback';

export async function fetchReportApi(sessionId: number): Promise<LearningReport> {
  const { data } = await apiClient.get(`/api/sessions/${sessionId}/report`);
  return normalizeSessionReport(data);
}

export async function submitNextActionApi(sessionId: number, action: string): Promise<LearningReport> {
  await apiClient.post(`/api/sessions/${sessionId}/next-action`, { action });
  return fetchReportApi(sessionId);
}

export async function fetchGrowthDashboardApi(sessionId: number): Promise<GrowthDashboard> {
  const { data } = await apiClient.get(`/api/sessions/${sessionId}/growth-dashboard`);
  return normalizeGrowthDashboard(data);
}
