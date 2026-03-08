import type {
  TutorMessage,
  TutorMessageListResponse,
  TutorSendMessageResponse,
} from '@/types'

interface TutorMessageDto {
  id: number
  session_id: number
  task_id: number
  role: 'user' | 'assistant'
  content: string
  created_at: string
}

interface TutorMessageListResponseDto {
  session_id: number
  task_id: number
  messages?: TutorMessageDto[]
}

interface TutorSendMessageResponseDto {
  session_id: number
  task_id: number
  user_message: TutorMessageDto
  assistant_message: TutorMessageDto
}

function mapTutorMessage(dto: TutorMessageDto): TutorMessage {
  return {
    id: dto.id,
    sessionId: dto.session_id,
    taskId: dto.task_id,
    role: dto.role,
    content: dto.content,
    createdAt: dto.created_at,
  }
}

export function mapTutorMessageListDto(dto: TutorMessageListResponseDto): TutorMessageListResponse {
  return {
    sessionId: dto.session_id,
    taskId: dto.task_id,
    messages: (dto.messages ?? []).map(mapTutorMessage),
  }
}

export function mapTutorSendMessageDto(dto: TutorSendMessageResponseDto): TutorSendMessageResponse {
  return {
    sessionId: dto.session_id,
    taskId: dto.task_id,
    userMessage: mapTutorMessage(dto.user_message),
    assistantMessage: mapTutorMessage(dto.assistant_message),
  }
}
