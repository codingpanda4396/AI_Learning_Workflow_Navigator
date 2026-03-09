import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import type {
  PracticeItem,
  PracticeSubmission,
  SubmitPracticeAnswerResponse,
} from '@/types'
import {
  generatePracticeItems,
  listPracticeItems,
  listPracticeSubmissions,
  submitPracticeAnswer,
} from '@/api/practice'
import { normalizeApiError } from '@/utils/apiError'
import type { NormalizedApiError } from '@/types/api'

export const usePracticeStore = defineStore('practice', () => {
  const items = ref<PracticeItem[]>([])
  const submissions = ref<PracticeSubmission[]>([])
  const lastSubmitResult = ref<SubmitPracticeAnswerResponse | null>(null)
  const loadingItems = ref(false)
  const generatingItems = ref(false)
  const loadingSubmissions = ref(false)
  const submittingAnswer = ref(false)
  const itemsError = ref<string | null>(null)
  const submissionsError = ref<string | null>(null)
  const submitError = ref<string | null>(null)
  const lastError = ref<NormalizedApiError | null>(null)

  const hasItems = computed(() => items.value.length > 0)

  function clearErrors() {
    itemsError.value = null
    submissionsError.value = null
    submitError.value = null
    lastError.value = null
  }

  async function loadItems(sessionId: number, taskId: number) {
    loadingItems.value = true
    itemsError.value = null
    try {
      const response = await listPracticeItems(sessionId, taskId)
      items.value = response.items
      return response
    } catch (input) {
      const normalized = normalizeApiError(input)
      lastError.value = normalized
      itemsError.value = normalized.message
      throw normalized
    } finally {
      loadingItems.value = false
    }
  }

  async function generateItems(sessionId: number, taskId: number) {
    generatingItems.value = true
    itemsError.value = null
    try {
      const response = await generatePracticeItems(sessionId, taskId)
      items.value = response.items
      return response
    } catch (input) {
      const normalized = normalizeApiError(input)
      lastError.value = normalized
      itemsError.value = normalized.message
      throw normalized
    } finally {
      generatingItems.value = false
    }
  }

  async function loadSubmissions(sessionId: number, taskId: number) {
    loadingSubmissions.value = true
    submissionsError.value = null
    try {
      const response = await listPracticeSubmissions(sessionId, taskId)
      submissions.value = response.submissions
      return response
    } catch (input) {
      const normalized = normalizeApiError(input)
      lastError.value = normalized
      submissionsError.value = normalized.message
      throw normalized
    } finally {
      loadingSubmissions.value = false
    }
  }

  async function submitAnswer(sessionId: number, taskId: number, practiceItemId: number, userAnswer: string) {
    submittingAnswer.value = true
    submitError.value = null
    try {
      const response = await submitPracticeAnswer(sessionId, taskId, practiceItemId, userAnswer)
      lastSubmitResult.value = response
      submissions.value = [response.submission, ...submissions.value]
      items.value = items.value.map((item) => (item.itemId === response.practiceItem.itemId ? response.practiceItem : item))
      return response
    } catch (input) {
      const normalized = normalizeApiError(input)
      lastError.value = normalized
      submitError.value = normalized.message
      throw normalized
    } finally {
      submittingAnswer.value = false
    }
  }

  function reset() {
    items.value = []
    submissions.value = []
    lastSubmitResult.value = null
    loadingItems.value = false
    generatingItems.value = false
    loadingSubmissions.value = false
    submittingAnswer.value = false
    clearErrors()
  }

  return {
    items,
    submissions,
    lastSubmitResult,
    loadingItems,
    generatingItems,
    loadingSubmissions,
    submittingAnswer,
    itemsError,
    submissionsError,
    submitError,
    hasItems,
    loadItems,
    generateItems,
    loadSubmissions,
    submitAnswer,
    clearErrors,
    reset,
  }
})
