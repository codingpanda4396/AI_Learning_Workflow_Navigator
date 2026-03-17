import { request } from './request'
import type { PlanPreviewData, CommitPlanData } from '@/types/dto'

export async function previewPlan(
  goalId: string,
  diagnosisId: string
): Promise<PlanPreviewData> {
  const { data } = await request.post<PlanPreviewData>(
    '/api/learning-plans/preview',
    { goalId, diagnosisId }
  )
  return data
}

export async function commitPlan(planId: string): Promise<CommitPlanData> {
  const { data } = await request.post<CommitPlanData>(
    '/api/learning-plans/commit',
    { planId }
  )
  return data
}
