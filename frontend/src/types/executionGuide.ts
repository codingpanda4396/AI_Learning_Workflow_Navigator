export type ExecutionGuideMode = 'guided-input' | 'closure'

export interface ExecutionGuideHeaderModel {
  title: string
  stageLabel: string
  stepLabel: string
  estimatedTime: string
  subtitle: string
  taskLabel?: string
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
  done: string
  current: string
  next: string
  later: string
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
}
