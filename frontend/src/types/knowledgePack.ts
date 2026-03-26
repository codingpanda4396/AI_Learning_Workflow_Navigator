import type { PlanStepIconKey } from '@/constants/planStepShell'

export type KnowledgePackId =
  | 'os_process_thread'
  | 'net_tcp_handshake'
  | 'ds_dfs_bfs'
  | 'arch_cache_locality'

export type KnowledgeType = 'COMPARE' | 'SEQUENCE' | 'CHOICE' | 'MECHANISM'

/** 执行页知识点脚手架模板（与后端 knowledgeType 独立，用于 UI 特化） */
export type KnowledgePointExecutionTemplate = 'CONCEPT' | 'PROCESS' | 'STRUCTURE' | 'PROBLEM'

export type PlanningPackStep = {
  flowTitle: string
  flowSubtitle: string
  icon: PlanStepIconKey
  headline: string
  whyFirst: string
  objectiveIntro: string
  objectiveBullets: string[]
  timeLabel: string
  suggestedPrompt: string
  reflectionQuestions: string[]
  closingLine: string
}

export type PlanningPack = {
  hero: {
    title: string
    subtitle: string
    auxiliaryLine: string
  }
  optionalTips: string[]
  commonMisconceptions: string[]
  phaseHighlights?: Partial<Record<'STRUCTURE' | 'UNDERSTANDING' | 'TRAINING' | 'REFLECTION', string>>
  steps: PlanningPackStep[]
}

export type ExecutionPack = {
  starterPrompts: string[]
  /** 执行页模板类型；未设置时由前端根据 knowledgeType / taskType 推断 */
  executionTemplate?: KnowledgePointExecutionTemplate
  scaffoldCards: Array<{
    id: string
    title: string
    hint: string
    prompt: string
    /** 主按钮动作文案，如「开始搭骨架」 */
    actionLabel?: string
  }>
  phaseHero: Partial<Record<'ORIENT' | 'EXPLORE' | 'SELF_EXPLAIN' | 'CHECK' | 'REMEDIAL' | 'PASS', string>>
  phaseObjective: Partial<Record<'ORIENT' | 'EXPLORE' | 'SELF_EXPLAIN' | 'CHECK' | 'REMEDIAL' | 'PASS', string>>
  microCheckLabels: string[]
  phaseUi?: Partial<Record<'ORIENT' | 'EXPLORE' | 'SELF_EXPLAIN' | 'CHECK' | 'REMEDIAL' | 'PASS', string>>
}

export type TutorPack = {
  focusLabel: string
  constrainedHints: string[]
  suggestedQuestions: string[]
}

export type CheckpointPack = {
  checkpointPrompt: string
  checkpointRubric: string[]
}

export type KnowledgePack = {
  id: KnowledgePackId
  knowledgeType: KnowledgeType
  /** 显式执行模板（演示包建议都填） */
  executionTemplate: KnowledgePointExecutionTemplate
  planning: PlanningPack
  execution: ExecutionPack
  tutor: TutorPack
  checkpoint: CheckpointPack
}
