import type { PracticeFeedbackReport, PracticeItem, PracticeQuizResponse, RunTaskResponse } from '@/types'

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
  submittingQuiz: boolean
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
  SINGLE_CHOICE: '单选题',
  TRUE_FALSE: '判断题',
  SHORT_ANSWER: '简答题',
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
  } else if (input.submittingQuiz) {
    quizStatus = 'submitting'
  } else if (input.quizError && !input.quiz) {
    quizStatus = 'error'
  } else if (!input.quiz) {
    quizStatus = 'not_generated'
  } else if (input.quiz.generationStatus === 'PENDING' || input.quiz.generationStatus === 'RUNNING') {
    quizStatus = 'generating'
  } else if (input.quiz.generationStatus === 'FAILED' || input.quiz.quizStatus === 'FAILED') {
    quizStatus = 'error'
  } else {
    switch (input.quiz.quizStatus) {
      case 'QUIZ_READY':
        quizStatus = 'ready'
        break
      case 'ANSWERED':
      case 'REVIEWING':
      case 'NEXT_ROUND':
      case 'FEEDBACK_READY':
        quizStatus = 'submitted'
        break
      case 'GENERATING':
        quizStatus = 'generating'
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
  } else if (input.quiz?.quizStatus === 'ANSWERED') {
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
  const titles = ['学习计划', '理解任务', '开始检测', '提交答案', '查看反馈']
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
        title: '检测题生成中',
        description: '系统正在准备本轮检测题，稍后会自动刷新为可作答状态。',
        buttonText: '生成中...',
        loading: true,
        disabled: true,
      }
    case 'quiz_ready':
      return {
        title: '已生成，可开始检测',
        description: '题目已经准备完成，可以逐题填写后一次性提交。',
        buttonText: '开始检测',
        loading: false,
        disabled: false,
      }
    case 'feedback_generating':
      return {
        title: '反馈整理中',
        description: '答案已提交，系统正在生成整体反馈和下一步建议。',
        buttonText: '整理中...',
        loading: true,
        disabled: true,
      }
    case 'feedback_ready':
      return {
        title: '反馈已生成',
        description: '现在可以查看整体总结、逐题结果和下一步动作。',
        buttonText: '查看反馈',
        loading: false,
        disabled: false,
      }
    case 'error':
      return {
        title: '检测流程失败，可重试',
        description: '重新加载后会继续使用当前 session，不会重构页面流程。',
        buttonText: '重试',
        loading: false,
        disabled: false,
      }
    case 'quiz_not_generated':
    default:
      return {
        title: '开始生成检测题',
        description: '系统会基于当前训练任务生成检测题，帮助完成可演示闭环。',
        buttonText: '生成检测题',
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

export function getPracticeItemStatusLabel(item: PracticeItem) {
  return itemStatusLabelMap[item.status?.trim().toUpperCase()] ?? '未作答'
}

export function getQuestionMeta(item: PracticeItem, index: number) {
  return {
    orderLabel: `第 ${index + 1} 题`,
    typeLabel: getQuestionTypeLabel(item.type, index),
    difficultyLabel: getDifficultyLabel(item.difficulty),
    statusLabel: getPracticeItemStatusLabel(item),
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
  if (report.suggestedNextAction) {
    items.push(report.suggestedNextAction)
  }
  if (report.recommendedAction === 'REVIEW') {
    items.push('建议先进入复习，再开始下一轮学习。')
  } else {
    items.push('可以直接进入下一轮学习。')
  }
  return items
}
