import type { PracticeFeedbackReport, PracticeItem, PracticeQuizResponse, PracticeSubmission, RunTaskResponse } from '@/types'

export type TrainingTaskStatus = 'idle' | 'ready' | 'error'

export type TrainingQuizStatus =
  | 'not_generated'
  | 'generating'
  | 'ready'
  | 'submitting'
  | 'submitted'
  | 'error'

export type TrainingFeedbackStatus = 'not_available' | 'generating' | 'ready' | 'error'

export type TrainingStageView =
  | 'quiz_not_generated'
  | 'quiz_generating'
  | 'quiz_ready'
  | 'feedback_generating'
  | 'feedback_ready'
  | 'error'

export interface TrainingStateInput {
  task: RunTaskResponse | null
  taskError: string | null
  quiz: PracticeQuizResponse | null
  quizError: string | null
  feedbackReport: PracticeFeedbackReport | null
  feedbackError: string | null
  hasSubmittedAnswers: boolean
  requestingQuiz: boolean
  pollingQuiz: boolean
  submittingAnswer: boolean
  loadingFeedback: boolean
}

export interface TrainingStepItem {
  key: string
  title: string
  state: 'done' | 'current' | 'pending'
}

export interface TrainingActionContent {
  title: string
  description: string
  buttonText: string
  loading: boolean
  disabled: boolean
}

const questionTypeLabelMap: Record<string, string> = {
  BASIC: '基础题',
  APPLICATION: '应用题',
  REASONING: '思考题',
  SINGLE_CHOICE: '基础题',
  TRUE_FALSE: '基础题',
  SHORT_ANSWER: '应用题',
}

const difficultyLabelMap: Record<string, string> = {
  EASY: '简单',
  BASIC: '简单',
  MEDIUM: '中等',
  NORMAL: '中等',
  HARD: '困难',
  DIFFICULT: '困难',
}

const itemStatusLabelMap: Record<string, string> = {
  GENERATED: '未作答',
  ACTIVE: '未作答',
  ANSWERED: '已作答',
  ARCHIVED: '已归档',
}

export function deriveTrainingStatuses(input: TrainingStateInput) {
  const taskStatus: TrainingTaskStatus = input.taskError ? 'error' : input.task ? 'ready' : 'idle'

  let quizStatus: TrainingQuizStatus = 'not_generated'
  if (input.requestingQuiz || input.pollingQuiz) {
    quizStatus = 'generating'
  } else if (input.submittingAnswer) {
    quizStatus = 'submitting'
  } else if (input.quizError && !input.quiz) {
    quizStatus = 'error'
  } else if (!input.quiz) {
    quizStatus = 'not_generated'
  } else {
    switch (input.quiz.status) {
      case 'GENERATING':
        quizStatus = 'generating'
        break
      case 'QUIZ_READY':
        quizStatus = 'ready'
        break
      case 'ANSWERED':
      case 'REVIEWING':
      case 'NEXT_ROUND':
      case 'FEEDBACK_READY':
        quizStatus = 'submitted'
        break
      case 'FAILED':
        quizStatus = 'error'
        break
      default:
        quizStatus = 'not_generated'
    }
  }

  let feedbackStatus: TrainingFeedbackStatus = 'not_available'
  if (!input.hasSubmittedAnswers) {
    feedbackStatus = input.feedbackError ? 'error' : 'not_available'
  } else if (input.loadingFeedback) {
    feedbackStatus = 'generating'
  } else if (input.feedbackReport) {
    feedbackStatus = 'ready'
  } else if (input.feedbackError) {
    feedbackStatus = 'error'
  } else if (input.quiz?.status === 'ANSWERED') {
    feedbackStatus = 'generating'
  }

  let view: TrainingStageView = 'quiz_not_generated'
  if (taskStatus === 'error' || quizStatus === 'error' || feedbackStatus === 'error') {
    view = 'error'
  } else if (feedbackStatus === 'ready') {
    view = 'feedback_ready'
  } else if (quizStatus === 'generating') {
    view = 'quiz_generating'
  } else if (quizStatus === 'ready') {
    view = 'quiz_ready'
  } else if (quizStatus === 'submitting' || quizStatus === 'submitted' || feedbackStatus === 'generating') {
    view = 'feedback_generating'
  } else {
    view = 'quiz_not_generated'
  }

  return { taskStatus, quizStatus, feedbackStatus, view }
}

export function buildTrainingSteps(view: TrainingStageView): TrainingStepItem[] {
  const currentIndexMap: Record<TrainingStageView, number> = {
    quiz_not_generated: 2,
    quiz_generating: 2,
    quiz_ready: 3,
    feedback_generating: 4,
    feedback_ready: 5,
    error: 2,
  }
  const currentIndex = currentIndexMap[view]
  const titles = ['学习计划', '学明白', '做练习', '提交答案', '看结果']
  return titles.map((title, index) => {
    const stepIndex = index + 1
    return {
      key: title,
      title,
      state: stepIndex < currentIndex ? 'done' : stepIndex === currentIndex ? 'current' : 'pending',
    }
  })
}

export function getTrainingActionContent(view: TrainingStageView): TrainingActionContent {
  switch (view) {
    case 'quiz_generating':
      return {
        title: '练习题正在生成',
        description: '系统正在准备本章练习题，你可以先继续向 Tutor 提问。',
        buttonText: '生成中...',
        loading: true,
        disabled: true,
      }
    case 'quiz_ready':
      return {
        title: '可以开始做练习了',
        description: '练习已经准备好，现在可以开始作答。',
        buttonText: '开始做题',
        loading: false,
        disabled: false,
      }
    case 'feedback_generating':
      return {
        title: '正在整理结果',
        description: '你的答案已提交，系统正在分析表现并生成建议。',
        buttonText: '整理中...',
        loading: true,
        disabled: true,
      }
    case 'feedback_ready':
      return {
        title: '结果已经准备好',
        description: '现在可以查看你的薄弱点和下一步建议。',
        buttonText: '查看结果',
        loading: false,
        disabled: false,
      }
    case 'error':
      return {
        title: '训练流程暂时不可用',
        description: '重试后会继续保留你当前的学习进度。',
        buttonText: '重新加载',
        loading: false,
        disabled: false,
      }
    case 'quiz_not_generated':
    default:
      return {
        title: '开始本章练习',
        description: '系统会生成少量练习题，帮助你快速确认是否学会了。',
        buttonText: '生成练习题',
        loading: false,
        disabled: false,
      }
  }
}

export function getQuestionTypeLabel(rawType: string, index: number) {
  const normalized = rawType.trim().toUpperCase()
  if (questionTypeLabelMap[normalized]) {
    return questionTypeLabelMap[normalized]
  }
  const fallback = ['基础题', '应用题', '思考题']
  return fallback[index % fallback.length]
}

export function getDifficultyLabel(rawDifficulty: string) {
  const normalized = rawDifficulty.trim().toUpperCase()
  return difficultyLabelMap[normalized] ?? '中等'
}

export function getPracticeItemStatusLabel(item: PracticeItem, submission: PracticeSubmission | null) {
  if (submission && submission.score !== null) {
    return '已批改'
  }
  if (submission) {
    return '已作答'
  }
  return itemStatusLabelMap[item.status?.trim().toUpperCase()] ?? '未作答'
}

export function getQuestionMeta(item: PracticeItem, index: number, submission: PracticeSubmission | null) {
  return {
    orderLabel: `第 ${index + 1} 题`,
    typeLabel: getQuestionTypeLabel(item.questionType, index),
    difficultyLabel: getDifficultyLabel(item.difficulty),
    statusLabel: getPracticeItemStatusLabel(item, submission),
  }
}

export function getSystemAdjustments(report: PracticeFeedbackReport | null) {
  if (!report) {
    return []
  }
  const items: string[] = []
  if (report.reviewFocus.length > 0) {
    items.push(`系统已标记 ${report.reviewFocus.length} 个优先复习点`)
  }
  if (report.recommendedAction === 'REVIEW') {
    items.push('建议先回到薄弱点复习，再进入下一轮。')
  } else {
    items.push('可以继续进入下一轮学习。')
  }
  return items
}
