import client from '@/api/client'
import type {
  PracticeFeedbackReport,
  PracticeItem,
  PracticeItemsResponse,
  PracticeQuizResponse,
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

interface PracticeQuizResponseDto {
  quiz_id: number
  session_id: number
  task_id: number
  status: string
  question_count?: number | null
  answered_count?: number | null
  generation_source?: string | null
  failure_reason?: string | null
  questions?: PracticeItemDto[]
}

interface PracticeFeedbackReportDto {
  report_id: number
  quiz_id: number
  session_id: number
  task_id: number
  diagnosis_summary: string
  strengths?: string[]
  weaknesses?: string[]
  review_focus?: string[]
  next_round_advice: string
  recommended_action: 'REVIEW' | 'NEXT_ROUND'
  source: string
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

function mapPracticeQuizDto(dto: PracticeQuizResponseDto): PracticeQuizResponse {
  return {
    quizId: dto.quiz_id,
    sessionId: dto.session_id,
    taskId: dto.task_id,
    status: dto.status as PracticeQuizResponse['status'],
    questionCount: typeof dto.question_count === 'number' ? dto.question_count : 0,
    answeredCount: typeof dto.answered_count === 'number' ? dto.answered_count : 0,
    generationSource: dto.generation_source ?? '',
    failureReason: dto.failure_reason ?? '',
    questions: (dto.questions ?? []).map(mapPracticeItemDto),
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

export async function requestPracticeQuiz(sessionId: number, taskId: number): Promise<PracticeQuizResponse> {
  const { data } = await client.post<PracticeQuizResponseDto>(`/session/${sessionId}/tasks/${taskId}/quiz/generate`)
  return mapPracticeQuizDto(data)
}

export async function getPracticeQuiz(sessionId: number, taskId: number): Promise<PracticeQuizResponse> {
  const { data } = await client.get<PracticeQuizResponseDto>(`/session/${sessionId}/tasks/${taskId}/quiz`)
  return mapPracticeQuizDto(data)
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

export async function getPracticeFeedbackReport(sessionId: number, taskId: number): Promise<PracticeFeedbackReport> {
  const { data } = await client.get<PracticeFeedbackReportDto>(`/session/${sessionId}/tasks/${taskId}/quiz/feedback`)
  return {
    reportId: data.report_id,
    quizId: data.quiz_id,
    sessionId: data.session_id,
    taskId: data.task_id,
    diagnosisSummary: data.diagnosis_summary,
    strengths: Array.isArray(data.strengths) ? data.strengths.filter((item): item is string => typeof item === 'string') : [],
    weaknesses: Array.isArray(data.weaknesses) ? data.weaknesses.filter((item): item is string => typeof item === 'string') : [],
    reviewFocus: Array.isArray(data.review_focus) ? data.review_focus.filter((item): item is string => typeof item === 'string') : [],
    nextRoundAdvice: data.next_round_advice,
    recommendedAction: data.recommended_action,
    source: data.source,
  }
}

export async function applyPracticeFeedbackAction(
  sessionId: number,
  taskId: number,
  action: 'REVIEW' | 'NEXT_ROUND',
): Promise<PracticeQuizResponse> {
  const { data } = await client.post<PracticeQuizResponseDto>(`/session/${sessionId}/tasks/${taskId}/quiz/feedback/action`, {
    action,
  })
  return mapPracticeQuizDto(data)
}
