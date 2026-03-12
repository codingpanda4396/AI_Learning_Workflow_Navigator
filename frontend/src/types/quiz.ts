export type QuizBusinessStatus =
  | 'GENERATING'
  | 'READY'
  | 'ANSWERING'
  | 'REVIEWING'
  | 'REPORT_READY'
  | 'NEXT_ROUND'
  | 'FAILED';

export type QuizRuntimeStatus = 'PENDING' | 'RUNNING' | 'SUCCEEDED' | 'FAILED';

export interface QuizQuestion {
  questionId: number;
  type?: string;
  stem: string;
  options: string[];
  evaluationFocus?: string;
  difficulty?: string;
  status?: 'READY' | 'ANSWERED' | 'ARCHIVED';
}

export interface QuizSnapshot {
  sessionId: number;
  taskId?: number;
  quizId?: number;
  generationStatus?: QuizRuntimeStatus;
  quizStatus?: QuizBusinessStatus;
  questionCount?: number;
  answeredCount?: number;
  failureReason?: string;
  retryable?: boolean;
  questions: QuizQuestion[];
}

export interface QuizAnswerPayload {
  questionId: number;
  answer: string;
}
