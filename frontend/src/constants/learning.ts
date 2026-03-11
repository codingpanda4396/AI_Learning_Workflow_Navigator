import type {
  DisplayStatus,
  LearningStage,
  LegacyLearningStage,
  PageLevelFlow,
} from '@/types'

export interface LearningStageDisplay {
  key: LearningStage
  order: 1 | 2 | 3 | 4
  title: string
  shortDescription: string
  actionText: string
}

export interface StatusDisplay {
  label: string
  actionText: string
}

export const PAGE_LEVEL_FLOWS = [
  'create-session',
  'learning-session',
  'feedback-review',
] as const satisfies readonly PageLevelFlow[]

export const LEARNING_STAGE_SEQUENCE = [
  'STRUCTURE',
  'UNDERSTANDING',
  'TRAINING',
  'EVALUATION',
] as const satisfies readonly LearningStage[]

export const LEGACY_STAGE_ALIASES: Record<LegacyLearningStage, LearningStage> = {
  REFLECTION: 'EVALUATION',
}

export const LEARNING_STAGE_DISPLAY: Record<LearningStage, LearningStageDisplay> = {
  STRUCTURE: {
    key: 'STRUCTURE',
    order: 1,
    title: '搭框架',
    shortDescription: '先看清这一轮要学什么、怎么学，再开始具体任务。',
    actionText: '开始这一步',
  },
  UNDERSTANDING: {
    key: 'UNDERSTANDING',
    order: 2,
    title: '学明白',
    shortDescription: '先把关键概念、原理和常见误区弄明白。',
    actionText: '开始理解',
  },
  TRAINING: {
    key: 'TRAINING',
    order: 3,
    title: '做检测',
    shortDescription: '先完成当前学习，再用检测题确认自己是否真的掌握。',
    actionText: '进入检测',
  },
  EVALUATION: {
    key: 'EVALUATION',
    order: 4,
    title: '看结果',
    shortDescription: '看懂结果，知道已经掌握了什么、接下来该补什么。',
    actionText: '查看结果',
  },
}

export const STATUS_DISPLAY: Record<DisplayStatus, StatusDisplay> = {
  GENERATING: { label: '生成中', actionText: '请稍等' },
  CREATED: { label: '已创建', actionText: '生成学习计划' },
  EVALUATED: { label: '已评估', actionText: '查看结果' },
  PLANNED: { label: '已规划', actionText: '开始学习' },
  LEARNING: { label: '学习中', actionText: '继续学习' },
  QUIZ_READY: { label: '检测已准备好', actionText: '进入检测' },
  ANSWERED: { label: '已提交', actionText: '查看结果' },
  FEEDBACK_READY: { label: '结果已准备好', actionText: '查看结果' },
  REVIEWING: { label: '正在回看结果', actionText: '继续查看' },
  NEXT_ROUND: { label: '下一轮已准备好', actionText: '继续学习' },
  FAILED: { label: '需要重试', actionText: '重试' },
  LOCKED: { label: '未开始', actionText: '等待解锁' },
  AVAILABLE: { label: '可开始', actionText: '立即开始' },
  ACTIVE: { label: '进行中', actionText: '继续' },
  DONE: { label: '已完成', actionText: '查看结果' },
  ERROR: { label: '异常', actionText: '重试' },
  PENDING: { label: '未开始', actionText: '开始学习' },
  RUNNING: { label: '进行中', actionText: '继续学习' },
  SUCCEEDED: { label: '已完成', actionText: '查看结果' },
}
