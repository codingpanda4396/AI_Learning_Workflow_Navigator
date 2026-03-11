import type { NextAction, PracticeFeedbackReport, PracticeQuizResponse, RunTaskResponse, Stage, SubmitTaskResponse } from '@/types'
import { normalizeLearningStage } from '@/utils/learningPlanDisplay'

export type UserPracticeStatus = 'idle' | 'generating' | 'ready' | 'failed' | 'unavailable'

export interface PracticeStatusViewModel {
  status: UserPracticeStatus
  badge: string
  title: string
  description: string
  helper?: string
}

export interface FeedbackSummaryBlock {
  strengths: string[]
  weaknesses: string[]
  nextStep: string
}

const stageActionMap: Record<'STRUCTURE' | 'UNDERSTANDING' | 'TRAINING' | 'EVALUATION' | 'UNKNOWN', {
  title: string
  summary: string
  primaryAction: string
  reason: string
}> = {
  STRUCTURE: {
    title: '先搭起这一节的知识框架',
    summary: '先知道这轮会学哪些内容、顺序怎么安排，再进入具体任务。',
    primaryAction: '开始这一步',
    reason: '先理清框架，后面学习会更顺。',
  },
  UNDERSTANDING: {
    title: '先把这个概念学明白',
    summary: '这一步会帮你抓住核心概念、关键原理和容易混淆的地方。',
    primaryAction: '开始理解',
    reason: '先学明白，再做检测，错误会更有价值。',
  },
  TRAINING: {
    title: '先完成当前学习，再做检测',
    summary: '这一步要先读任务、抓关键点，再用检测题确认是否真的掌握。',
    primaryAction: '进入这一步',
    reason: '检测不是刷题，而是帮你找出不稳的地方。',
  },
  EVALUATION: {
    title: '看看结果，再决定下一步',
    summary: '这一步会根据你的表现告诉你已经掌握了什么，还需要补什么。',
    primaryAction: '查看结果',
    reason: '结果页会直接给出下一步建议。',
  },
  UNKNOWN: {
    title: '继续当前学习',
    summary: '系统正在同步当前阶段，你可以先进入当前任务。',
    primaryAction: '继续学习',
    reason: '等阶段信息准备好后，这里会自动更新。',
  },
}

const nextActionLabelMap: Record<NextAction | 'UNKNOWN', string> = {
  INSERT_REMEDIAL_UNDERSTANDING: '先补一轮理解，再继续往下学',
  INSERT_TRAINING_VARIANTS: '再练一轮针对性检测',
  INSERT_TRAINING_REINFORCEMENT: '补几道强化练习，稳住关键点',
  ADVANCE_TO_NEXT_NODE: '可以进入下一章节或下一知识点',
  NOOP: '回到学习总览，继续当前安排',
  UNKNOWN: '回到学习总览，按系统建议继续',
}

function sanitizeFailureReason(raw?: string | null) {
  if (!raw?.trim()) {
    return ''
  }
  const normalized = raw.trim().toLowerCase()
  if (normalized.includes('not found') || normalized.includes('quiz')) {
    return '检测题还没准备好'
  }
  return '这一步生成失败了'
}

export function getStageNarrative(stage?: string | null) {
  return stageActionMap[normalizeLearningStage(stage) as keyof typeof stageActionMap] ?? stageActionMap.UNKNOWN
}

export function getStageShortLabel(stage?: Stage | string | null) {
  const normalized = normalizeLearningStage(stage)
  if (normalized === 'STRUCTURE') return '搭框架'
  if (normalized === 'UNDERSTANDING') return '学明白'
  if (normalized === 'TRAINING') return '做检测'
  if (normalized === 'EVALUATION') return '看结果'
  return '继续学习'
}

export function getPrimaryStageAction(stage?: string | null) {
  return getStageNarrative(stage).primaryAction
}

export function normalizePracticeStatus(input: {
  task?: RunTaskResponse | null
  quiz?: PracticeQuizResponse | null
  quizError?: string | null
  requestingQuiz?: boolean
  pollingQuiz?: boolean
}): UserPracticeStatus {
  if (input.task && normalizeLearningStage(input.task.stage) !== 'TRAINING') {
    return 'unavailable'
  }
  if (input.requestingQuiz || input.pollingQuiz) {
    return 'generating'
  }
  if (!input.quiz) {
    return input.quizError ? 'failed' : 'idle'
  }
  if (input.quiz.generationStatus === 'FAILED' || input.quiz.quizStatus === 'FAILED') {
    return 'failed'
  }
  if (input.quiz.generationStatus === 'PENDING' || input.quiz.generationStatus === 'RUNNING' || input.quiz.quizStatus === 'GENERATING') {
    return 'generating'
  }
  if (input.quiz.quizStatus === 'QUIZ_READY' || input.quiz.quizStatus === 'ANSWERED' || input.quiz.quizStatus === 'FEEDBACK_READY' || input.quiz.quizStatus === 'REVIEWING' || input.quiz.quizStatus === 'NEXT_ROUND') {
    return 'ready'
  }
  return 'idle'
}

export function getPracticeStatusViewModel(input: {
  status: UserPracticeStatus
  quiz?: PracticeQuizResponse | null
  error?: string | null
}): PracticeStatusViewModel {
  const failureText = sanitizeFailureReason(input.quiz?.failureReason || input.error)

  if (input.status === 'unavailable') {
    return {
      status: 'unavailable',
      badge: '当前无需检测',
      title: '这一步先专注理解内容',
      description: '当前任务不需要单独做检测题，先完成学习内容即可。',
    }
  }

  if (input.status === 'generating') {
    return {
      status: 'generating',
      badge: '正在准备',
      title: '正在准备检测题',
      description: '学完这一步后即可开始检测，页面会自动刷新状态。',
      helper: '这组检测会从概念理解、基本应用、易错点三个角度检查掌握情况。',
    }
  }

  if (input.status === 'ready') {
    return {
      status: 'ready',
      badge: '已准备好',
      title: '检测题已准备好',
      description: '完成当前学习后可直接进入检测，系统会根据结果给出下一步建议。',
      helper: '这不是正式考试，而是帮助你找出还不稳的地方。',
    }
  }

  if (input.status === 'failed') {
    return {
      status: 'failed',
      badge: '准备失败',
      title: failureText || '这一步生成失败了',
      description: '你可以重试生成，不影响先继续看当前内容或先问 Tutor。',
      helper: '重试后会继续留在当前学习任务中。',
    }
  }

  return {
    status: 'idle',
    badge: '尚未开始',
    title: '检测题还没开始准备',
    description: '先完成这一页内容，系统会在合适的时候为你准备检测题。',
    helper: '等检测题准备好后，你可以直接进入检测。',
  }
}

export function getFeedbackSummary(report: PracticeFeedbackReport | null, result: SubmitTaskResponse | null): FeedbackSummaryBlock {
  return {
    strengths: report?.strengths?.length ? report.strengths : result?.strengths ?? [],
    weaknesses: report?.weaknesses?.length ? report.weaknesses : result?.weaknesses ?? [],
    nextStep:
      report?.nextRoundAdvice ||
      report?.suggestedNextAction ||
      nextActionLabelMap[(result?.nextAction as NextAction | undefined) ?? 'UNKNOWN'] ||
      nextActionLabelMap.UNKNOWN,
  }
}

export function getNextActionLabel(action?: string | null) {
  const normalized = (action ?? '').trim().toUpperCase() as NextAction
  return nextActionLabelMap[normalized] ?? nextActionLabelMap.UNKNOWN
}
