import client from '@/api/client'
import type {
  RunTaskResponse,
  SubmitTaskResponse,
  TaskDetailResponse,
} from '@/types'
import {
  mapRunTaskDto,
  mapSubmitTaskDto,
  mapTaskDetailDto,
} from '@/mappers/taskMapper'

interface SubmitTaskRequestDto {
  user_answer: string
}

export async function getTaskDetail(taskId: number): Promise<TaskDetailResponse> {
  const { data } = await client.get(`/task/${taskId}`)
  return mapTaskDetailDto(data)
}

export async function runTask(taskId: number): Promise<RunTaskResponse> {
  const { data } = await client.post(`/task/${taskId}/run`)
  return mapRunTaskDto(data)
}

export async function submitTask(taskId: number, userAnswer: string): Promise<SubmitTaskResponse> {
  const payload: SubmitTaskRequestDto = {
    user_answer: userAnswer,
  }
  const { data } = await client.post(`/task/${taskId}/submit`, payload)
  return mapSubmitTaskDto(data)
}
