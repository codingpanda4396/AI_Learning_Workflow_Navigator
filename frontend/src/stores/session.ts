import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type {
  CreateSessionRequest,
  SessionOverviewResponse,
  RunTaskResponse,
  SubmitTaskResponse,
} from '@/types'
import { sessionApi, taskApi } from '@/api/client'

export const useSessionStore = defineStore('session', () => {
  // State
  const currentSession = ref<SessionOverviewResponse | null>(null)
  const currentTask = ref<RunTaskResponse | null>(null)
  const taskResult = ref<SubmitTaskResponse | null>(null)
  const isLoading = ref(false)
  const error = ref<string | null>(null)

  // Getters
  const sessionId = computed(() => currentSession.value?.session_id ?? null)
  const timeline = computed(() => currentSession.value?.timeline ?? [])
  const masterySummary = computed(() => currentSession.value?.mastery_summary ?? [])
  const nextTask = computed(() => currentSession.value?.next_task ?? null)
  const currentStage = computed(() => currentSession.value?.current_stage ?? null)

  // Actions
  async function createSession(data: CreateSessionRequest) {
    isLoading.value = true
    error.value = null
    try {
      const response = await sessionApi.create(data)
      return response.data.session_id
    } catch (e) {
      error.value = (e as Error).message
      throw e
    } finally {
      isLoading.value = false
    }
  }

  async function planSession(sessionId: number) {
    isLoading.value = true
    error.value = null
    try {
      await sessionApi.plan(sessionId)
    } catch (e) {
      error.value = (e as Error).message
      throw e
    } finally {
      isLoading.value = false
    }
  }

  async function fetchSessionOverview(sessionId: number) {
    isLoading.value = true
    error.value = null
    try {
      const response = await sessionApi.getOverview(sessionId)
      currentSession.value = response.data
    } catch (e) {
      error.value = (e as Error).message
      throw e
    } finally {
      isLoading.value = false
    }
  }

  async function runTask(taskId: number) {
    isLoading.value = true
    error.value = null
    try {
      const response = await taskApi.run(taskId)
      currentTask.value = response.data
      return response.data
    } catch (e) {
      error.value = (e as Error).message
      throw e
    } finally {
      isLoading.value = false
    }
  }

  async function submitTask(taskId: number, userAnswer: string) {
    isLoading.value = true
    error.value = null
    try {
      const response = await taskApi.submit(taskId, { user_answer: userAnswer })
      taskResult.value = response.data
      return response.data
    } catch (e) {
      error.value = (e as Error).message
      throw e
    } finally {
      isLoading.value = false
    }
  }

  function reset() {
    currentSession.value = null
    currentTask.value = null
    taskResult.value = null
    error.value = null
  }

  return {
    // State
    currentSession,
    currentTask,
    taskResult,
    isLoading,
    error,
    // Getters
    sessionId,
    timeline,
    masterySummary,
    nextTask,
    currentStage,
    // Actions
    createSession,
    planSession,
    fetchSessionOverview,
    runTask,
    submitTask,
    reset,
  }
})
