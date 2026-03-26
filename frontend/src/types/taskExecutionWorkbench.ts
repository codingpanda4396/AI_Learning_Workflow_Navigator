import type { KnowledgePackId } from '@/types/knowledgePack'

/** 与顶栏四阶段一致 */
export type WorkbenchPhaseCode =
  | 'STRUCTURE'
  | 'UNDERSTANDING'
  | 'TRAINING'
  | 'REFLECTION'

export type WorkbenchTaskStatus = 'running' | 'submitted' | 'needs_fix' | 'can_advance'

export interface WorkbenchPhaseProgressModel {
  phases: WorkbenchPhaseCode[]
  currentPhase: WorkbenchPhaseCode
  /** 0–1，用于顶栏细进度 */
  overallRatio: number
  stepLabel: string
  taskIndexLabel: string
}

/** 产品化脚手架展示（非原始 prompt） */
export interface ScaffoldProductModel {
  whatToOutput: string[]
  recommendedSteps: string[]
  avoid: string[]
  /** 主开始按钮：文案与第一张脚手架卡对齐时可覆盖 */
  startActionLabel: string
  startBehavior?: 'send' | 'prefill'
}

export interface WhyThisStepModel {
  whyNow: string
  skipRisk: string
  expectedGain: string
}

export interface StageRulesModel {
  rules: string[]
}

export type TopicVisualVariant = 'graph' | 'timeline' | 'container' | 'hierarchy'

export interface TopicSpecialHintModel {
  topicDisplayName: string
  bullets: string[]
  visualVariant: TopicVisualVariant
}

export interface StageProgressMiniModel {
  roundLabel: string
  actionsDone: number
  actionsTarget: number
  untilNextPhase: string
  passedGate: boolean
}

export interface CurrentTaskCardModel {
  phaseDisplayZh: string
  phaseCode: WorkbenchPhaseCode
  taskTitle: string
  coreActionLine: string
  completionLines: string[]
}

export interface TaskExecutionWorkbenchModel {
  packId: KnowledgePackId | null
  phaseProgress: WorkbenchPhaseProgressModel
  taskStatus: WorkbenchTaskStatus
  taskStatusLabel: string
  scaffoldProduct: ScaffoldProductModel
  whyThisStep: WhyThisStepModel
  stageRules: StageRulesModel
  topicHints: TopicSpecialHintModel
  stageMini: StageProgressMiniModel
  currentTask: CurrentTaskCardModel
  /** 主列 data-phase */
  emphasisPhase: WorkbenchPhaseCode
}
