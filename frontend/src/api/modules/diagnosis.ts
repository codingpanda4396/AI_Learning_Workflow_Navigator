import apiClient from '@/api/client';
import type { ApiEnvelope } from '@/types/common';
import type { DiagnosisAnswer, DiagnosisGenerateResponse, DiagnosisSubmitResponse } from '@/types/diagnosis';

export async function generateDiagnosisApi(sessionId: string): Promise<DiagnosisGenerateResponse> {
  const { data } = await apiClient.post<ApiEnvelope<DiagnosisGenerateResponse>>('/api/diagnosis/sessions', { sessionId });
  return data.data;
}

export async function submitDiagnosisApi(diagnosisId: string, answers: DiagnosisAnswer[]): Promise<DiagnosisSubmitResponse> {
  const { data } = await apiClient.post<ApiEnvelope<DiagnosisSubmitResponse>>(`/api/diagnosis/sessions/${diagnosisId}/submissions`, { answers });
  return data.data;
}
