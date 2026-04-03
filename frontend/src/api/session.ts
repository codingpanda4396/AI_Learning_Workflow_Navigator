import { request } from './request'
import type { ReportData, NextActionConfirmData, SessionFlowState } from '@/types/dto'
import type { NextActionTypeType } from '@/types/enums'

export async function getSessionFlowState(sessionId: string): Promise<SessionFlowState> {
  const { data } = await request.get<SessionFlowState>(
    `/api/sessions/${sessionId}/flow-state`
  )
  return data
}

export async function getReport(sessionId: string): Promise<ReportData> {
  const { data } = await request.get<ReportData>(
    `/api/sessions/${sessionId}/report`
  )
  return data
}

export async function confirmNextAction(
  sessionId: string,
  actionType: NextActionTypeType
): Promise<NextActionConfirmData> {
  const { data } = await request.post<NextActionConfirmData>(
    `/api/sessions/${sessionId}/next-action`,
    { actionType }
  )
  return data
}
