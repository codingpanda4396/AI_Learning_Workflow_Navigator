import { request } from './request'
import type { ReportData, NextActionConfirmData } from '@/types/dto'
import type { NextActionTypeType } from '@/types/enums'

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
