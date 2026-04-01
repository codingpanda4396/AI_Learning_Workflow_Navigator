import type { WorkbenchPhaseCode, WorkbenchPhaseProgressModel } from '@/types/taskExecutionWorkbench'
import type { DfsBfsStructureWorkbenchUi } from '@/constants/dfsBfsStructureSkeleton'

export type WorkbenchRenderState = 'PROMPT' | 'THINK' | 'OUTPUT' | 'FEEDBACK'

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

export interface WorkbenchStructureSubmitPayload {
  ui: DfsBfsStructureWorkbenchUi
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
  intro: string
  renderState: WorkbenchRenderState
  explanations: ExplanationBlock[]
  scaffoldActions: WorkbenchScaffoldAction[]
  structureUi: DfsBfsStructureWorkbenchUi
  understandingQuestion: UnderstandingQuestion | null
  understandingSelectedId: string | null
  trainingPrompt: string
  trainingDraft: string
  reflectionSummary: string
  reflectionQuestion: ReflectionQuestion | null
  reflectionStrategies: ReflectionStrategy[]
  reflectionOutput: WorkbenchReflectionOutput
  feedback: PhaseFeedbackPayload | null
  busy: boolean
}
