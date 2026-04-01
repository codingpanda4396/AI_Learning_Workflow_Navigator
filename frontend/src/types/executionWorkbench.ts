import type { WorkbenchPhaseCode } from '@/types/taskExecutionWorkbench'

export type PhaseKey = 'structure' | 'understanding' | 'training' | 'reflection'

export type RenderState = 'prompt' | 'think' | 'output' | 'feedback'

export interface ChatMessage {
  id: string
  role: 'user' | 'assistant' | 'system'
  content: string
  timestamp: number
}

export interface McqOption {
  id: string
  label: string
}

export interface McqQuestion {
  id: string
  prompt: string
  options: McqOption[]
  correctId: string
  feedbackByOption: Record<string, { tone: 'positive' | 'neutral' | 'redirect'; body: string }>
}

export interface PhaseFeedback {
  title: string
  tag?: string
  body: string
  nextAction?: string
}

export interface TrainingEvaluation {
  masteredParts: string[]
  gaps: string[]
  followUp: string
}

export interface ReflectionSummary {
  learnedPoints: string[]
  finalUnderstanding: string
}

export interface ReflectionStrategy {
  id: string
  label: string
}

export interface ScaffoldButton {
  id: string
  group: string
  title: string
  injectPrompt: string
}

export interface StructurePhaseState {
  questions: McqQuestion[]
  currentQuestionIndex: number
  selectedOptionId: string | null
  isLocked: boolean
  feedback: PhaseFeedback | null
  completedQuestionIds: string[]
}

export interface UnderstandingPhaseState {
  messages: ChatMessage[]
  draftInput: string
  injectedPrompt: string | null
  scaffoldKey: string | null
  turnCount: number
  streaming: boolean
  error: string | null
  canProceed: boolean
  completionHint: string | null
  stageSummary: string | null
  feedback: PhaseFeedback | null
}

export interface TrainingPhaseState {
  messages: ChatMessage[]
  draftInput: string
  roundCount: number
  streaming: boolean
  error: string | null
  canProceed: boolean
  completionHint: string | null
  stageSummary: string | null
  evaluation: TrainingEvaluation | null
  finalDraft: string | null
  feedback: PhaseFeedback | null
}

export interface ReflectionPhaseState {
  summary: ReflectionSummary | null
  confusionPoints: string[]
  availableStrategies: ReflectionStrategy[]
  selectedStrategyIds: string[]
  userReflectionText: string
  isSubmitted: boolean
  feedback: PhaseFeedback | null
}

export interface PhaseProgressModel {
  phases: WorkbenchPhaseCode[]
  currentPhase: WorkbenchPhaseCode
  overallRatio: number
}

export interface ExecutionWorkbenchVm {
  topicName: string
  phase: PhaseKey
  renderState: RenderState
  phaseGoal: string
  phaseProgress: PhaseProgressModel
  structure: StructurePhaseState
  understanding: UnderstandingPhaseState
  training: TrainingPhaseState
  reflection: ReflectionPhaseState
  busy: boolean
}

export const PHASE_KEY_TO_CODE: Record<PhaseKey, WorkbenchPhaseCode> = {
  structure: 'STRUCTURE',
  understanding: 'UNDERSTANDING',
  training: 'TRAINING',
  reflection: 'REFLECTION',
}

export const PHASE_CODE_TO_KEY: Record<WorkbenchPhaseCode, PhaseKey> = {
  STRUCTURE: 'structure',
  UNDERSTANDING: 'understanding',
  TRAINING: 'training',
  REFLECTION: 'reflection',
}

export const PHASE_SEQUENCE: PhaseKey[] = ['structure', 'understanding', 'training', 'reflection']

export function nextPhase(current: PhaseKey): PhaseKey | null {
  const idx = PHASE_SEQUENCE.indexOf(current)
  return idx >= 0 && idx < PHASE_SEQUENCE.length - 1 ? PHASE_SEQUENCE[idx + 1]! : null
}

export function createEmptyStructureState(questions: McqQuestion[]): StructurePhaseState {
  return {
    questions,
    currentQuestionIndex: 0,
    selectedOptionId: null,
    isLocked: false,
    feedback: null,
    completedQuestionIds: [],
  }
}

export function createEmptyUnderstandingState(): UnderstandingPhaseState {
  return {
    messages: [],
    draftInput: '',
    injectedPrompt: null,
    scaffoldKey: null,
    turnCount: 0,
    streaming: false,
    error: null,
    canProceed: false,
    completionHint: null,
    stageSummary: null,
    feedback: null,
  }
}

export function createEmptyTrainingState(): TrainingPhaseState {
  return {
    messages: [],
    draftInput: '',
    roundCount: 0,
    streaming: false,
    error: null,
    canProceed: false,
    completionHint: null,
    stageSummary: null,
    evaluation: null,
    finalDraft: null,
    feedback: null,
  }
}

export function createEmptyReflectionState(strategies: ReflectionStrategy[]): ReflectionPhaseState {
  return {
    summary: null,
    confusionPoints: [],
    availableStrategies: strategies,
    selectedStrategyIds: [],
    userReflectionText: '',
    isSubmitted: false,
    feedback: null,
  }
}
