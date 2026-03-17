import { request } from './request'
import type { CreateGoalRequest, CreateGoalData } from '@/types/dto'

export async function createGoal(payload: CreateGoalRequest): Promise<CreateGoalData> {
  const { data } = await request.post<CreateGoalData>('/api/goals', payload)
  return data
}
