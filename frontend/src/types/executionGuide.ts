export type ExecutionGuideMode = 'guided-input' | 'closure'

export type ExecutionKnowledgePointStatus = 'done' | 'active' | 'upcoming'

export interface ExecutionKnowledgePointModel {
  id: string
  index: number
  title: string
  subtitle?: string
  status: ExecutionKnowledgePointStatus
}

export interface ExecutionGuideHeaderModel {
  /** 首屏主标题，口语化（如「这轮只补一个点：xxx」） */
  heroTitle: string
  /** 一句话说明「现在先做什么」 */
  heroSubtitle: string
  /** 右栏：做到什么算完成（2~3 条） */
  completionCriteria: string[]
  /** 小号辅助行：阶段 · 任务序号 · 知识点序号 */
  metaLine: string
  title: string
  stageLabel: string
  stepLabel: string
  estimatedTime: string
  subtitle: string
  taskLabel?: string
  trackTitle?: string
  trackSubtitle?: string
  knowledgePoints?: ExecutionKnowledgePointModel[]
}

export interface ExecutionGuideChip {
  id: string
  label: string
  fill: string
}

export interface ExecutionGuideActionModel {
  mode: ExecutionGuideMode
  eyebrow: string
  title: string
  description: string
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
}

export interface ExecutionGuideProgressRailModel {
  /** 侧栏第一块标题 */
  progressSectionTitle: string
  /** 本轮进度（多行短句） */
  progressLines: string[]
  /** 第二块标题 */
  criteriaSectionTitle: string
  /** 本轮完成标准 */
  completionLines: string[]
  /** 第三块标题 */
  nextSectionTitle: string
  /** 下一步预告（一段） */
  nextPreview: string
  /** 极简知识点目录（一行一点） */
  knowledgeOutline?: ExecutionKnowledgePointModel[]
}

/** Prompt 脚手架大卡（一键发任务消息） */
export interface ExecutionScaffoldCardModel {
  id: string
  title: string
  hint: string
  prompt: string
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
}
