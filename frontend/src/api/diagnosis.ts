import { request } from './request'
import type {
  DiagnosisSessionData,
  DiagnosisAnswer,
  SubmitDiagnosisData,
} from '@/types/dto'

export async function createSession(goalId: string): Promise<DiagnosisSessionData> {
  const { data } = await request.post<DiagnosisSessionData>(
    '/api/diagnosis/sessions',
    { goalId }
  )
  return data
}

export async function submitDiagnosis(
  diagnosisId: string,
  answers: DiagnosisAnswer[]
): Promise<SubmitDiagnosisData> {
  const { data } = await request.post<SubmitDiagnosisData>(
    '/api/diagnosis/submissions',
    { diagnosisId, answers }
  )
  return data
}
