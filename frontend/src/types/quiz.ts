export interface QuizQuestion {
  questionId: number;
  type?: string;
  stem: string;
  options: string[];
  evaluationFocus?: string;
  difficulty?: string;
  status?: string;
}

export interface QuizSnapshot {
  sessionId: number;
  taskId?: number;
  quizId?: number;
  generationStatus?: string;
  quizStatus?: string;
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
