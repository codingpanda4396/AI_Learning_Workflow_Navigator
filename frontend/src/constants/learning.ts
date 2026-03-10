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
    title: '结构建立',
    shortDescription: '梳理知识点边界、组成和关联，先搭好整体框架。',
    actionText: '开始学习',
  },
  UNDERSTANDING: {
    key: 'UNDERSTANDING',
    order: 2,
    title: '原理理解',
    shortDescription: '解释关键机制和因果链，澄清常见误区。',
    actionText: '继续学习',
  },
  TRAINING: {
    key: 'TRAINING',
    order: 3,
    title: '训练检测',
    shortDescription: '通过练习与作答验证当前掌握程度。',
    actionText: '进入训练',
  },
  EVALUATION: {
    key: 'EVALUATION',
    order: 4,
    title: '反馈调整',
    shortDescription: '回看结果与薄弱点，明确下一步改进建议。',
    actionText: '查看反馈',
  },
}

export const STATUS_DISPLAY: Record<DisplayStatus, StatusDisplay> = {
  CREATED: { label: '已创建', actionText: '生成学习计划' },
  EVALUATED: { label: '已评估', actionText: '查看评估' },
  PLANNED: { label: '已规划', actionText: '开始学习' },
  LEARNING: { label: '学习中', actionText: '继续学习' },
  QUIZ_READY: { label: '可训练', actionText: '开始训练' },
  ANSWERED: { label: '已作答', actionText: '查看结果' },
  FEEDBACK_READY: { label: '反馈已生成', actionText: '查看反馈' },
  REVIEWING: { label: '复盘中', actionText: '继续复盘' },
  NEXT_ROUND: { label: '进入下一轮', actionText: '开始下一轮' },
  FAILED: { label: '失败', actionText: '重试' },
  LOCKED: { label: '未解锁', actionText: '等待开始' },
  AVAILABLE: { label: '可开始', actionText: '立即开始' },
  ACTIVE: { label: '进行中', actionText: '继续处理' },
  DONE: { label: '已完成', actionText: '查看结果' },
  ERROR: { label: '异常', actionText: '重试' },
  PENDING: { label: '未开始', actionText: '开始学习' },
  RUNNING: { label: '进行中', actionText: '继续学习' },
  SUCCEEDED: { label: '已完成', actionText: '查看结果' },
}
