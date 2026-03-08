import client from '@/api/client'
import type { TutorMessageListResponse, TutorSendMessageResponse } from '@/types'
import { mapTutorMessageListDto, mapTutorSendMessageDto } from '@/mappers/tutorMapper'

interface SendTutorMessageRequestDto {
  content: string
}

export async function getTutorMessages(sessionId: number, taskId: number): Promise<TutorMessageListResponse> {
  const { data } = await client.get(`/session/${sessionId}/tasks/${taskId}/tutor/messages`)
  return mapTutorMessageListDto(data)
}

export async function sendTutorMessage(
  sessionId: number,
  taskId: number,
  content: string,
): Promise<TutorSendMessageResponse> {
  const payload: SendTutorMessageRequestDto = { content }
  const { data } = await client.post(`/session/${sessionId}/tasks/${taskId}/tutor/messages`, payload)
  return mapTutorSendMessageDto(data)
}
