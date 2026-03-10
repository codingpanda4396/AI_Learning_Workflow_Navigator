import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import type {
  PracticeFeedbackReport,
  PracticeItem,
  PracticeQuizResponse,
  PracticeSubmission,
  SubmitPracticeAnswerResponse,
} from '@/types'
import {
  applyPracticeFeedbackAction,
  generatePracticeItems,
  getPracticeFeedbackReport,
  getPracticeQuiz,
  listPracticeItems,
  listPracticeSubmissions,
  requestPracticeQuiz,
  submitPracticeAnswer,
} from '@/api/practice'
import { normalizeApiError } from '@/utils/apiError'
import type { NormalizedApiError } from '@/types/api'

export const usePracticeStore = defineStore('practice', () => {
  const items = ref<PracticeItem[]>([])
  const quiz = ref<PracticeQuizResponse | null>(null)
  const feedbackReport = ref<PracticeFeedbackReport | null>(null)
  const submissions = ref<PracticeSubmission[]>([])
  const lastSubmitResult = ref<SubmitPracticeAnswerResponse | null>(null)
  const requestingQuiz = ref(false)
  const pollingQuiz = ref(false)
  const loadingFeedback = ref(false)
  const applyingFeedbackAction = ref(false)
  const loadingItems = ref(false)
  const generatingItems = ref(false)
  const loadingSubmissions = ref(false)
  const submittingAnswer = ref(false)
  const itemsError = ref<string | null>(null)
  const submissionsError = ref<string | null>(null)
  const submitError = ref<string | null>(null)
  const lastError = ref<NormalizedApiError | null>(null)

  const hasItems = computed(() => items.value.length > 0)
  const quizStatus = computed(() => quiz.value?.status ?? 'GENERATING')

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

  async function requestQuiz(sessionId: number, taskId: number) {
    requestingQuiz.value = true
    itemsError.value = null
    try {
      const response = await requestPracticeQuiz(sessionId, taskId)
      quiz.value = response
      items.value = response.questions
      return response
    } catch (input) {
      const normalized = normalizeApiError(input)
      lastError.value = normalized
      itemsError.value = normalized.message
      throw normalized
    } finally {
      requestingQuiz.value = false
    }
  }

  async function loadQuiz(sessionId: number, taskId: number) {
    pollingQuiz.value = true
    itemsError.value = null
    try {
      const response = await getPracticeQuiz(sessionId, taskId)
      quiz.value = response
      items.value = response.questions
      return response
    } catch (input) {
      const normalized = normalizeApiError(input)
      lastError.value = normalized
      itemsError.value = normalized.message
      throw normalized
    } finally {
      pollingQuiz.value = false
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
      if (quiz.value) {
        const answeredIds = new Set(submissions.value.map((item) => item.practiceItemId))
        quiz.value = {
          ...quiz.value,
          answeredCount: answeredIds.size,
          questions: items.value,
        }
      }
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

  async function loadFeedbackReport(sessionId: number, taskId: number) {
    loadingFeedback.value = true
    try {
      const response = await getPracticeFeedbackReport(sessionId, taskId)
      feedbackReport.value = response
      return response
    } catch (input) {
      const normalized = normalizeApiError(input)
      lastError.value = normalized
      submitError.value = normalized.message
      throw normalized
    } finally {
      loadingFeedback.value = false
    }
  }

  async function applyFeedback(sessionId: number, taskId: number, action: 'REVIEW' | 'NEXT_ROUND') {
    applyingFeedbackAction.value = true
    try {
      const response = await applyPracticeFeedbackAction(sessionId, taskId, action)
      quiz.value = response
      items.value = response.questions
      return response
    } catch (input) {
      const normalized = normalizeApiError(input)
      lastError.value = normalized
      submitError.value = normalized.message
      throw normalized
    } finally {
      applyingFeedbackAction.value = false
    }
  }

  function reset() {
    items.value = []
    quiz.value = null
    feedbackReport.value = null
    submissions.value = []
    lastSubmitResult.value = null
    requestingQuiz.value = false
    pollingQuiz.value = false
    loadingFeedback.value = false
    applyingFeedbackAction.value = false
    loadingItems.value = false
    generatingItems.value = false
    loadingSubmissions.value = false
    submittingAnswer.value = false
    clearErrors()
  }

  return {
    items,
    quiz,
    feedbackReport,
    submissions,
    lastSubmitResult,
    requestingQuiz,
    pollingQuiz,
    loadingFeedback,
    applyingFeedbackAction,
    loadingItems,
    generatingItems,
    loadingSubmissions,
    submittingAnswer,
    itemsError,
    submissionsError,
    submitError,
    hasItems,
    quizStatus,
    requestQuiz,
    loadQuiz,
    loadItems,
    generateItems,
    loadSubmissions,
    submitAnswer,
    loadFeedbackReport,
    applyFeedback,
    clearErrors,
    reset,
  }
})
