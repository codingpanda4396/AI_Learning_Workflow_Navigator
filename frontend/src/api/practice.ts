import client from '@/api/client'
import type { PracticeFeedbackReport, PracticeItem, PracticeQuestionResult, PracticeQuizResponse } from '@/types'

interface PracticeQuestionDto {
  question_id: number
  type: string
  stem: string
  options: unknown
  evaluation_focus?: string | null
  difficulty: string
  status: string
}

interface PracticeQuizStatusDto {
  session_id: number
  task_id: number
  quiz_id: number
  generation_status: string
  quiz_status: string
  question_count?: number | null
  answered_count?: number | null
  failure_reason?: string | null
  retryable?: boolean | null
}

interface PracticeQuizDto extends PracticeQuizStatusDto {
  questions?: PracticeQuestionDto[]
}

interface PracticeQuestionResultDto {
  question_id: number
  type: string
  stem: string
  user_answer: string
  score?: number | null
  correct?: boolean | null
  feedback?: string | null
  error_tags?: string[]
}

interface PracticeFeedbackReportDto {
  report_id: number
  quiz_id: number
  session_id: number
  task_id: number
  report_status: string
  overall_summary: string
  question_results?: PracticeQuestionResultDto[]
  strengths?: string[]
  weaknesses?: string[]
  review_focus?: string[]
  next_round_advice: string
  suggested_next_action?: string | null
  recommended_action: 'REVIEW' | 'NEXT_ROUND'
  selected_action?: 'REVIEW' | 'NEXT_ROUND' | null
  source: string
}

interface SubmitSessionQuizRequestDto {
  answers: Array<{
    question_id: number
    user_answer: string
  }>
}

function mapPracticeQuestionDto(dto: PracticeQuestionDto): PracticeItem {
  return {
    questionId: dto.question_id,
    type: dto.type,
    stem: dto.stem,
    options: dto.options,
    evaluationFocus: dto.evaluation_focus ?? '',
    difficulty: dto.difficulty,
    status: dto.status,
  }
}

function mapPracticeQuizStatusDto(dto: PracticeQuizStatusDto): Omit<PracticeQuizResponse, 'questions'> {
  return {
    quizId: dto.quiz_id,
    sessionId: dto.session_id,
    taskId: dto.task_id,
    generationStatus: dto.generation_status as PracticeQuizResponse['generationStatus'],
    quizStatus: dto.quiz_status as PracticeQuizResponse['quizStatus'],
    questionCount: typeof dto.question_count === 'number' ? dto.question_count : 0,
    answeredCount: typeof dto.answered_count === 'number' ? dto.answered_count : 0,
    failureReason: dto.failure_reason ?? '',
    retryable: dto.retryable === true,
  }
}

function mapPracticeQuestionResultDto(dto: PracticeQuestionResultDto): PracticeQuestionResult {
  return {
    questionId: dto.question_id,
    type: dto.type,
    stem: dto.stem,
    userAnswer: dto.user_answer,
    score: typeof dto.score === 'number' ? dto.score : null,
    correct: dto.correct === true,
    feedback: dto.feedback ?? '',
    errorTags: Array.isArray(dto.error_tags) ? dto.error_tags.filter((item): item is string => typeof item === 'string') : [],
  }
}

function mapPracticeFeedbackReportDto(dto: PracticeFeedbackReportDto): PracticeFeedbackReport {
  return {
    reportId: dto.report_id,
    quizId: dto.quiz_id,
    sessionId: dto.session_id,
    taskId: dto.task_id,
    reportStatus: dto.report_status as PracticeFeedbackReport['reportStatus'],
    overallSummary: dto.overall_summary,
    questionResults: (dto.question_results ?? []).map(mapPracticeQuestionResultDto),
    strengths: Array.isArray(dto.strengths) ? dto.strengths.filter((item): item is string => typeof item === 'string') : [],
    weaknesses: Array.isArray(dto.weaknesses) ? dto.weaknesses.filter((item): item is string => typeof item === 'string') : [],
    reviewFocus: Array.isArray(dto.review_focus) ? dto.review_focus.filter((item): item is string => typeof item === 'string') : [],
    nextRoundAdvice: dto.next_round_advice,
    suggestedNextAction: dto.suggested_next_action ?? '',
    recommendedAction: dto.recommended_action,
    selectedAction: dto.selected_action ?? '',
    source: dto.source,
  }
}

export async function requestPracticeQuiz(sessionId: number): Promise<PracticeQuizResponse> {
  const { data } = await client.post<PracticeQuizStatusDto>(`/sessions/${sessionId}/quiz/generate`)
  return {
    ...mapPracticeQuizStatusDto(data),
    questions: [],
  }
}

export async function getPracticeQuizStatus(sessionId: number): Promise<PracticeQuizResponse> {
  const { data } = await client.get<PracticeQuizStatusDto>(`/sessions/${sessionId}/quiz/status`)
  return {
    ...mapPracticeQuizStatusDto(data),
    questions: [],
  }
}

export async function getPracticeQuiz(sessionId: number): Promise<PracticeQuizResponse> {
  const { data } = await client.get<PracticeQuizDto>(`/sessions/${sessionId}/quiz`)
  return {
    ...mapPracticeQuizStatusDto(data),
    questions: (data.questions ?? []).map(mapPracticeQuestionDto),
  }
}

export async function submitPracticeQuiz(
  sessionId: number,
  answers: Array<{ questionId: number; userAnswer: string }>,
): Promise<PracticeFeedbackReport> {
  const payload: SubmitSessionQuizRequestDto = {
    answers: answers.map((item) => ({
      question_id: item.questionId,
      user_answer: item.userAnswer,
    })),
  }
  const { data } = await client.post<PracticeFeedbackReportDto>(`/sessions/${sessionId}/quiz/submit`, payload)
  return mapPracticeFeedbackReportDto(data)
}

export async function getPracticeFeedbackReport(sessionId: number): Promise<PracticeFeedbackReport> {
  const { data } = await client.get<PracticeFeedbackReportDto>(`/sessions/${sessionId}/feedback`)
  return mapPracticeFeedbackReportDto(data)
}

export async function applyPracticeFeedbackAction(
  sessionId: number,
  action: 'REVIEW' | 'NEXT_ROUND',
): Promise<PracticeFeedbackReport> {
  const { data } = await client.post<PracticeFeedbackReportDto>(`/sessions/${sessionId}/next-action`, {
    action,
  })
  return mapPracticeFeedbackReportDto(data)
}
