import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import type {
  CreateSessionRequest,
  CurrentSessionResponse,
  GoalDiagnosisResponse,
  PathResponse,
  PathOption,
  RunTaskResponse,
  SessionOverviewResponse,
  SubmitTaskResponse,
} from '@/types'
import type { NormalizedApiError } from '@/types/api'
import {
  createSession,
  diagnoseGoal,
  getCurrentSession,
  getPathOptions,
  getSessionOverview,
  getSessionPath,
  planSession,
} from '@/api/session'
import { getTaskDetail, runTask, submitTask } from '@/api/task'
import { normalizeApiError } from '@/utils/apiError'

const RETRY_DELAY_MS = 300

function sleep(ms: number) {
  return new Promise((resolve) => {
    setTimeout(resolve, ms)
  })
}

export const useSessionStore = defineStore('session', () => {
  const currentSession = ref<SessionOverviewResponse | null>(null)
  const currentSessionPath = ref<PathResponse | null>(null)
  const currentTask = ref<RunTaskResponse | null>(null)
  const currentTaskSessionId = ref<number | null>(null)
  const taskResult = ref<SubmitTaskResponse | null>(null)
  const currentUserSession = ref<CurrentSessionResponse | null>(null)
  const goalDiagnosis = ref<GoalDiagnosisResponse | null>(null)
  const pathOptions = ref<PathOption[]>([])
  const selectedPathId = ref<string | null>(null)
  const lastError = ref<NormalizedApiError | null>(null)

  const creatingSession = ref(false)
  const planning = ref(false)
  const fetchingSession = ref(false)
  const runningTask = ref(false)
  const submittingTask = ref(false)
  const recoveringSession = ref(false)
  const diagnosingGoal = ref(false)
  const fetchingPathOptions = ref(false)

  const isLoading = computed(
    () =>
      creatingSession.value ||
      planning.value ||
      fetchingSession.value ||
      runningTask.value ||
      submittingTask.value ||
      recoveringSession.value ||
      diagnosingGoal.value ||
      fetchingPathOptions.value,
  )

  const sessionId = computed(() => currentSession.value?.sessionId ?? null)
  const timeline = computed(() => currentSession.value?.timeline ?? [])
  const masterySummary = computed(() => currentSession.value?.masterySummary ?? [])
  const nextTask = computed(() => currentSession.value?.nextTask ?? null)
  const currentStage = computed(() => currentSession.value?.currentStage ?? null)
  const error = computed(() => lastError.value?.message ?? null)

  function clearError() {
    lastError.value = null
  }

  function setError(input: unknown) {
    lastError.value = normalizeApiError(input)
    return lastError.value
  }

  async function createSessionAction(data: CreateSessionRequest) {
    creatingSession.value = true
    clearError()
    try {
      return await createSession(data)
    } catch (input) {
      throw setError(input)
    } finally {
      creatingSession.value = false
    }
  }

  async function diagnoseGoalAction(data: CreateSessionRequest) {
    diagnosingGoal.value = true
    clearError()
    try {
      const response = await diagnoseGoal(data)
      goalDiagnosis.value = response
      return response
    } catch (input) {
      throw setError(input)
    } finally {
      diagnosingGoal.value = false
    }
  }

  async function fetchPathOptionsAction(data: CreateSessionRequest) {
    fetchingPathOptions.value = true
    clearError()
    try {
      const response = await getPathOptions(data)
      pathOptions.value = response
      const stillValid = response.some((item) => item.pathId === selectedPathId.value)
      if (!stillValid) {
        selectedPathId.value = null
      }
      if (!selectedPathId.value && response.length > 0) {
        const first = response[0]
        selectedPathId.value = first ? first.pathId : null
      }
      return response
    } catch (input) {
      throw setError(input)
    } finally {
      fetchingPathOptions.value = false
    }
  }

  async function planSessionAction(inputSessionId: number) {
    planning.value = true
    clearError()
    try {
      await planSession(inputSessionId)
    } catch (input) {
      throw setError(input)
    } finally {
      planning.value = false
    }
  }

  async function fetchSessionOverviewAction(inputSessionId: number, retry = 1) {
    fetchingSession.value = true
    clearError()
    try {
      const overview = await getSessionOverview(inputSessionId)
      currentSession.value = overview
      return overview
    } catch (input) {
      const normalized = setError(input)
      if (retry > 0 && normalized.retryable) {
        await sleep(RETRY_DELAY_MS)
        return fetchSessionOverviewAction(inputSessionId, retry - 1)
      }
      throw normalized
    } finally {
      fetchingSession.value = false
    }
  }

  async function fetchSessionPathAction(inputSessionId: number) {
    recoveringSession.value = true
    clearError()
    try {
      const path = await getSessionPath(inputSessionId)
      currentSessionPath.value = path
      return path
    } catch (input) {
      throw setError(input)
    } finally {
      recoveringSession.value = false
    }
  }

  async function fetchCurrentSessionAction() {
    recoveringSession.value = true
    clearError()
    try {
      const response = await getCurrentSession()
      currentUserSession.value = response
      return response
    } catch (input) {
      throw setError(input)
    } finally {
      recoveringSession.value = false
    }
  }

  async function runTaskAction(taskId: number) {
    runningTask.value = true
    clearError()
    try {
      const detail = await getTaskDetail(taskId)
      currentTaskSessionId.value = detail.sessionId
      const response = await runTask(taskId)
      currentTask.value = response
      return response
    } catch (input) {
      throw setError(input)
    } finally {
      runningTask.value = false
    }
  }

  async function submitTaskAction(taskId: number, userAnswer: string) {
    submittingTask.value = true
    clearError()
    try {
      const response = await submitTask(taskId, userAnswer)
      taskResult.value = response
      if (response.nextTask) {
        currentTaskSessionId.value = currentTaskSessionId.value ?? currentSession.value?.sessionId ?? null
      }
      return response
    } catch (input) {
      throw setError(input)
    } finally {
      submittingTask.value = false
    }
  }

  function resetTaskState() {
    currentTask.value = null
    taskResult.value = null
    currentTaskSessionId.value = null
  }

  function setSelectedPath(pathId: string | null) {
    selectedPathId.value = pathId
  }

  function reset() {
    currentSession.value = null
    currentSessionPath.value = null
    currentTask.value = null
    currentTaskSessionId.value = null
    taskResult.value = null
    currentUserSession.value = null
    goalDiagnosis.value = null
    pathOptions.value = []
    selectedPathId.value = null
    clearError()
  }

  return {
    currentSession,
    currentSessionPath,
    currentTask,
    currentTaskSessionId,
    taskResult,
    currentUserSession,
    goalDiagnosis,
    pathOptions,
    selectedPathId,
    lastError,
    creatingSession,
    planning,
    fetchingSession,
    runningTask,
    submittingTask,
    recoveringSession,
    diagnosingGoal,
    fetchingPathOptions,
    isLoading,
    error,
    sessionId,
    timeline,
    masterySummary,
    nextTask,
    currentStage,
    createSession: createSessionAction,
    planSession: planSessionAction,
    fetchSessionOverview: fetchSessionOverviewAction,
    fetchSessionPath: fetchSessionPathAction,
    fetchCurrentSession: fetchCurrentSessionAction,
    diagnoseGoal: diagnoseGoalAction,
    fetchPathOptions: fetchPathOptionsAction,
    runTask: runTaskAction,
    submitTask: submitTaskAction,
    setSelectedPath,
    clearError,
    resetTaskState,
    reset,
  }
})
