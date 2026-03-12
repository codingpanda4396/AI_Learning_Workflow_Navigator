import apiClient from '@/api/client';
import type { DiagnosisAnswer, DiagnosisGenerateResponse, DiagnosisSubmitResponse } from '@/types/diagnosis';

export async function generateDiagnosisApi(sessionId: string): Promise<DiagnosisGenerateResponse> {
  const { data } = await apiClient.post('/api/diagnosis/generate', { sessionId });
  return data as DiagnosisGenerateResponse;
}

export async function submitDiagnosisApi(diagnosisId: string, answers: DiagnosisAnswer[]): Promise<DiagnosisSubmitResponse> {
  const { data } = await apiClient.post('/api/diagnosis/submit', {
    diagnosisId,
    answers,
  });
  return data as DiagnosisSubmitResponse;
}
