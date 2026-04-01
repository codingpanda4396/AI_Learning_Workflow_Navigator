import type { WorkbenchPhaseCode, WorkbenchPhaseProgressModel } from '@/types/taskExecutionWorkbench'

export type WorkbenchRenderState = 'prompt' | 'think' | 'output' | 'feedback'

export interface ExplanationBlock {
  id: string
  title: string
  body: string
  tone?: 'base' | 'scaffold' | 'feedback'
}

export interface McqOption {
  id: string
  label: string
}

export interface StageIntroCopy {
  title: string
  subtitle: string
}

export interface StructureQuestion {
  id: string
  prompt: string
  options: McqOption[]
}

export interface UnderstandingQuestion {
  id: string
  prompt: string
  options: McqOption[]
  expectedId: string
  feedbackByOption: Record<string, { tag: string; body: string }>
}

export interface ReflectionQuestion {
  id: string
  prompt: string
  options: McqOption[]
}

export interface ReflectionStrategy {
  id: string
  label: string
}

export interface PhaseFeedbackPayload {
  title: string
  tag?: string
  body: string
  nextAction?: string
}

export interface WorkbenchScaffoldAction {
  id: string
  title: string
  prompt: string
}

export interface WorkbenchReflectionOutput {
  questionOptionId: string | null
  strategyId: string | null
}

export interface ExecutionWorkbenchViewModel {
  topicName: string
  stageGoal: string
  cognitiveAction: string
  phase: WorkbenchPhaseCode
  phaseProgress: WorkbenchPhaseProgressModel
  intro: StageIntroCopy
  renderState: WorkbenchRenderState
  explanations: ExplanationBlock[]
  scaffoldActions: WorkbenchScaffoldAction[]
  structureQuestion: StructureQuestion | null
  structureSelectedId: string | null
  understandingQuestion: UnderstandingQuestion | null
  understandingSelectedId: string | null
  trainingTaskTitle: string
  trainingTaskRequirement: string
  trainingPrompt: string
  trainingDraft: string
  reflectionSummary: string
  reflectionQuestion: ReflectionQuestion | null
  reflectionStrategies: ReflectionStrategy[]
  reflectionOutput: WorkbenchReflectionOutput
  feedback: PhaseFeedbackPayload | null
  busy: boolean
}
