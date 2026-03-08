import { defineStore } from 'pinia'
import { computed, ref, watch } from 'vue'
import type {
  StartWorkflowPayload,
  WorkflowState,
  WorkflowStepNumber,
  WorkflowStepState,
  WorkflowStepStatus,
} from '@/types/workflow'

const WORKFLOW_STORAGE_KEY = 'ai-learning-workflow-state'

function initialState(): WorkflowState {
  return {
    goal: '',
    courseId: 'computer_network',
    chapterId: 'tcp',
    workflowId: null,
    currentStep: 1,
    stepData: {},
    loading: false,
  }
}

export const useWorkflowStore = defineStore('workflow', () => {
  const state = ref<WorkflowState>(initialState())

  // Restores the last in-progress workflow so mobile/refresh does not lose context.
  function loadFromStorage() {
    const raw = localStorage.getItem(WORKFLOW_STORAGE_KEY)
    if (!raw) {
      return
    }

    try {
      const parsed = JSON.parse(raw) as Partial<WorkflowState>
      state.value = {
        ...initialState(),
        ...parsed,
      }
    } catch (error) {
      console.warn('Failed to parse workflow state from localStorage', error)
    }
  }

  function persistState() {
    localStorage.setItem(WORKFLOW_STORAGE_KEY, JSON.stringify(state.value))
  }

  function startWorkflow(payload: StartWorkflowPayload) {
    state.value.goal = payload.goal
    state.value.courseId = payload.courseId
    state.value.chapterId = payload.chapterId
    state.value.currentStep = 1
  }

  function setWorkflowId(workflowId: string | null) {
    state.value.workflowId = workflowId
  }

  function setLoading(loading: boolean) {
    state.value.loading = loading
  }

  function setCurrentStep(step: WorkflowStepNumber) {
    state.value.currentStep = step
  }

  function setStepState(step: WorkflowStepNumber, stepState: Partial<WorkflowStepState>) {
    const key = `step${step}` as const
    const current = state.value.stepData[key] ?? {
      input: {},
      output: {},
      status: 'idle' as WorkflowStepStatus,
    }

    state.value.stepData[key] = {
      ...current,
      ...stepState,
    }
  }

  function reset() {
    state.value = initialState()
  }

  watch(
    state,
    () => {
      // Persist every workflow mutation to support "continue where you left off".
      persistState()
    },
    { deep: true },
  )

  const goal = computed(() => state.value.goal)
  const courseId = computed(() => state.value.courseId)
  const chapterId = computed(() => state.value.chapterId)
  const workflowId = computed(() => state.value.workflowId)
  const currentStep = computed(() => state.value.currentStep)
  const stepData = computed(() => state.value.stepData)
  const loading = computed(() => state.value.loading)

  return {
    state,
    goal,
    courseId,
    chapterId,
    workflowId,
    currentStep,
    stepData,
    loading,
    loadFromStorage,
    startWorkflow,
    setWorkflowId,
    setLoading,
    setCurrentStep,
    setStepState,
    reset,
  }
})
