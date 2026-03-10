import client from '@/api/client'
import type {
  PracticeItem,
  PracticeItemsResponse,
  PracticeSubmission,
  PracticeSubmissionsResponse,
  SubmitPracticeAnswerResponse,
} from '@/types'

interface PracticeItemDto {
  item_id: number
  question_type: string
  stem: string
  options: unknown
  difficulty: string
  source: string
  status: string
}

interface PracticeItemsResponseDto {
  session_id: number
  task_id: number
  item_count: number
  items?: PracticeItemDto[]
}

interface PracticeSubmissionDto {
  submission_id: number
  practice_item_id: number
  user_answer: string
  score?: number | null
  is_correct?: boolean | null
  feedback?: string | null
  error_tags?: string[]
  submitted_at: string
}

interface PracticeSubmissionsResponseDto {
  session_id: number
  task_id: number
  submission_count: number
  submissions?: PracticeSubmissionDto[]
}

interface SubmitPracticeAnswerResponseDto {
  submission: PracticeSubmissionDto
  practiceItem: PracticeItemDto
  judgement?: {
    score?: number | null
    is_correct?: boolean | null
    feedback?: string | null
    error_tags?: string[]
  }
}

interface SubmitPracticeAnswerRequestDto {
  user_answer: string
}

function mapPracticeItemDto(dto: PracticeItemDto): PracticeItem {
  return {
    itemId: dto.item_id,
    questionType: dto.question_type,
    stem: dto.stem,
    options: dto.options,
    difficulty: dto.difficulty,
    source: dto.source,
    status: dto.status,
  }
}

function mapPracticeSubmissionDto(dto: PracticeSubmissionDto): PracticeSubmission {
  return {
    submissionId: dto.submission_id,
    practiceItemId: dto.practice_item_id,
    userAnswer: dto.user_answer,
    score: typeof dto.score === 'number' ? dto.score : null,
    isCorrect: typeof dto.is_correct === 'boolean' ? dto.is_correct : null,
    feedback: dto.feedback ?? '',
    errorTags: Array.isArray(dto.error_tags) ? dto.error_tags.filter((item): item is string => typeof item === 'string') : [],
    submittedAt: dto.submitted_at,
  }
}

export async function listPracticeItems(sessionId: number, taskId: number): Promise<PracticeItemsResponse> {
  const { data } = await client.get<PracticeItemsResponseDto>(`/session/${sessionId}/tasks/${taskId}/practice-items`)
  return {
    sessionId: data.session_id,
    taskId: data.task_id,
    itemCount: data.item_count,
    items: (data.items ?? []).map(mapPracticeItemDto),
  }
}

export async function generatePracticeItems(sessionId: number, taskId: number): Promise<PracticeItemsResponse> {
  const { data } = await client.post<PracticeItemsResponseDto>(`/session/${sessionId}/tasks/${taskId}/practice-items/generate`)
  return {
    sessionId: data.session_id,
    taskId: data.task_id,
    itemCount: data.item_count,
    items: (data.items ?? []).map(mapPracticeItemDto),
  }
}

export async function submitPracticeAnswer(
  sessionId: number,
  taskId: number,
  practiceItemId: number,
  userAnswer: string,
): Promise<SubmitPracticeAnswerResponse> {
  const payload: SubmitPracticeAnswerRequestDto = {
    user_answer: userAnswer,
  }
  const { data } = await client.post<SubmitPracticeAnswerResponseDto>(
    `/session/${sessionId}/tasks/${taskId}/practice-items/${practiceItemId}/submit`,
    payload,
  )
  return {
    submission: mapPracticeSubmissionDto(data.submission),
    practiceItem: mapPracticeItemDto(data.practiceItem),
    judgement: {
      score: typeof data.judgement?.score === 'number' ? data.judgement.score : null,
      isCorrect: typeof data.judgement?.is_correct === 'boolean' ? data.judgement.is_correct : null,
      feedback: data.judgement?.feedback ?? '',
      errorTags: Array.isArray(data.judgement?.error_tags)
        ? data.judgement.error_tags.filter((item): item is string => typeof item === 'string')
        : [],
    },
  }
}

export async function listPracticeSubmissions(sessionId: number, taskId: number): Promise<PracticeSubmissionsResponse> {
  const { data } = await client.get<PracticeSubmissionsResponseDto>(`/session/${sessionId}/tasks/${taskId}/practice-submissions`)
  return {
    sessionId: data.session_id,
    taskId: data.task_id,
    submissionCount: data.submission_count,
    submissions: (data.submissions ?? []).map(mapPracticeSubmissionDto),
  }
}
