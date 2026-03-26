import type { KnowledgePointExecutionTemplate } from '@/types/knowledgePack'
import type { TaskExecutionWorkbenchModel } from '@/types/taskExecutionWorkbench'

export type ExecutionGuideMode = 'guided-input' | 'closure'

export type ExecutionKnowledgePointStatus = 'done' | 'active' | 'upcoming'

export interface ExecutionKnowledgePointModel {
  id: string
  index: number
  title: string
  subtitle?: string
  status: ExecutionKnowledgePointStatus
}

/** 操作台核心信息（知识点主角 + 目标/标准/耗时） */
export interface ExecutionOperationConsoleModel {
  knowledgePointName: string
  knowledgePointType: KnowledgePointExecutionTemplate
  /** 本轮目标一句话 */
  roundGoal: string
  /** 完成标准（顶部展示，宜 1～2 条） */
  completionStandardLines: string[]
  estimatedTimeLabel: string
}

export interface ExecutionGuideHeaderModel {
  /** 四阶段机读码：STRUCTURE / UNDERSTANDING / TRAINING / REFLECTION */
  phaseCode?: string
  /** 四阶段中文展示名 */
  phaseDisplayZh?: string
  /** 首屏唯一行动锚点：你现在要做的一件事（一句话） */
  anchorActionLine?: string
  /** 首屏主标题，口语化（如「这轮只补一个点：xxx」） */
  heroTitle: string
  /** 一句话说明「现在先做什么」 */
  heroSubtitle: string
  /** 右栏：做到什么算完成（2~3 条） */
  completionCriteria: string[]
  /** 小号辅助行：阶段 · 任务序号 · 知识点序号 */
  metaLine: string
  /** 演示四知识点：与 KnowledgeConfig.plan.strategy 同源 */
  strategyLine?: string
  title: string
  stageLabel: string
  stepLabel: string
  estimatedTime: string
  subtitle: string
  taskLabel?: string
  trackTitle?: string
  trackSubtitle?: string
  knowledgePoints?: ExecutionKnowledgePointModel[]
  /** 紧凑操作台摘要 */
  operationConsole?: ExecutionOperationConsoleModel
}

export interface ExecutionGuideChip {
  id: string
  label: string
  fill: string
}

export interface ExecutionGuideActionModel {
  mode: ExecutionGuideMode
  /** 与 header 同步，用于主操作卡顶部阶段条 */
  phaseCode?: string
  phaseDisplayZh?: string
  eyebrow: string
  title: string
  description: string
  /** 主输入区上方系统指令 */
  directive?: string
  inputLabel?: string
  inputPlaceholder?: string
  primaryActionLabel: string
  chips: ExecutionGuideChip[]
  helperText?: string
  passHint?: string
  focusLabel?: string
  focusTitle?: string
  focusObjective?: string
  focusReason?: string
  focusTips?: string[]
}

export interface ExecutionGuideFeedbackAction {
  id: string
  label: string
}

export interface ExecutionGuideFeedbackModel {
  visible: boolean
  title: string
  mastered: string
  gap: string
  nextStep: string
  actions: ExecutionGuideFeedbackAction[]
  /** 结构化反馈（与 mastered/gap 同步，供 FeedbackPanel 拆分展示） */
  strengths?: string
  keyIssues?: string[]
  errorTags?: string[]
  nextRestateAsk?: string
  primaryCta?: 'restate' | 'apply_suggestion'
}

export interface ExecutionGuideProgressRailModel {
  /** 当前阶段 */
  stageSectionTitle: string
  stageLabel: string
  /** 当前这一步要产出什么 */
  deliverableSectionTitle: string
  deliverableLine: string
  /** 卡住时 */
  stuckSectionTitle: string
  stuckActions: string[]
  /** 下一步预告 */
  nextSectionTitle: string
  nextPreview: string
  knowledgeOutline?: ExecutionKnowledgePointModel[]
}

/** Prompt 脚手架大卡（一键发任务消息） */
export interface ExecutionScaffoldCardModel {
  id: string
  title: string
  hint: string
  prompt: string
  /** 主按钮文案，动作导向 */
  actionLabel: string
  /** send：直接发给导师；prefill：只填入输入框并聚焦，由用户补全后再发 */
  behavior?: 'send' | 'prefill'
}

export interface ExecutionGuideHelpSection {
  id: string
  title: string
  body?: string
  bullets?: string[]
}

export interface ExecutionPageViewModel {
  currentStepIndex: number
  currentStepTitle: string
  header: ExecutionGuideHeaderModel
  mainAction: ExecutionGuideActionModel
  feedback: ExecutionGuideFeedbackModel
  progressRail: ExecutionGuideProgressRailModel
  helpSections: ExecutionGuideHelpSection[]
  /** ORIENT/EXPLORE 等阶段用的三张脚手架卡 */
  scaffoldCards: ExecutionScaffoldCardModel[]
  /** 导师区：当前指令、软占位等 */
  tutorConsole: {
    currentDirective: string
    inputPlaceholderSoft: string
    stageDisplay: string
    currentDeliverable: string
    stuckActions: string[]
    /** 本阶段可点击填入的短线索（KnowledgeConfig） */
    phasePromptChips?: string[]
  }
  /** 单任务工作台：布局与特化文案 */
  workbench: TaskExecutionWorkbenchModel
}
