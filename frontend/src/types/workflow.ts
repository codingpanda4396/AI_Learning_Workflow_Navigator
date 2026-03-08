export type WorkflowStepNumber = 1 | 2 | 3 | 4

export type WorkflowStepStatus = 'idle' | 'running' | 'done' | 'error'

export interface WorkflowStepState {
  input: Record<string, unknown>
  output: Record<string, unknown>
  status: WorkflowStepStatus
}

export interface WorkflowStepData {
  step1?: WorkflowStepState
  step2?: WorkflowStepState
  step3?: WorkflowStepState
  step4?: WorkflowStepState
}

export interface WorkflowState {
  goal: string
  courseId: string
  chapterId: string
  workflowId: string | null
  currentStep: WorkflowStepNumber
  stepData: WorkflowStepData
  loading: boolean
}

export interface StartWorkflowPayload {
  goal: string
  courseId: string
  chapterId: string
}
