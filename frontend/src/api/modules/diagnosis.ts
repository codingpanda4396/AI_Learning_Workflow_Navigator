import apiClient from '@/api/client';
import { normalizeDiagnosisGenerateResponse, normalizeDiagnosisSubmitResponse } from '@/api/normalizers';
import type { ApiEnvelope } from '@/types/common';
import type { DiagnosisAnswer, DiagnosisGenerateResponse, DiagnosisSubmitResponse } from '@/types/diagnosis';

export async function generateDiagnosisApi(sessionId: string): Promise<DiagnosisGenerateResponse> {
  const { data } = await apiClient.post<ApiEnvelope<Record<string, unknown>>>('/api/diagnosis/sessions', { sessionId });
  return normalizeDiagnosisGenerateResponse(data.data ?? {});
}

export async function submitDiagnosisApi(diagnosisId: string, answers: DiagnosisAnswer[]): Promise<DiagnosisSubmitResponse> {
  const { data } = await apiClient.post<ApiEnvelope<Record<string, unknown>>>(`/api/diagnosis/sessions/${diagnosisId}/submissions`, { answers });
  return normalizeDiagnosisSubmitResponse(data.data ?? {});
}
