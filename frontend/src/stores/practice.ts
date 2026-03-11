import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import type { PracticeFeedbackReport, PracticeItem, PracticeQuizResponse } from '@/types'
import {
  applyPracticeFeedbackAction,
  getPracticeFeedbackReport,
  getPracticeQuiz,
  getPracticeQuizStatus,
  requestPracticeQuiz,
  submitPracticeQuiz,
} from '@/api/practice'
import { normalizeApiError } from '@/utils/apiError'
import type { NormalizedApiError } from '@/types/api'
import { getPracticeStatusViewModel, normalizePracticeStatus } from '@/utils/learningNarrative'

function mergeQuiz(current: PracticeQuizResponse | null, incoming: PracticeQuizResponse) {
  return {
    ...incoming,
    questions: incoming.questions.length > 0 ? incoming.questions : current?.questions ?? [],
  }
}

export const usePracticeStore = defineStore('practice', () => {
  const quiz = ref<PracticeQuizResponse | null>(null)
  const feedbackReport = ref<PracticeFeedbackReport | null>(null)
  const requestingQuiz = ref(false)
  const pollingQuiz = ref(false)
  const submittingQuiz = ref(false)
  const loadingFeedback = ref(false)
  const applyingFeedbackAction = ref(false)
  const itemsError = ref<string | null>(null)
  const submitError = ref<string | null>(null)
  const lastError = ref<NormalizedApiError | null>(null)

  const items = computed<PracticeItem[]>(() => quiz.value?.questions ?? [])
  const hasItems = computed(() => items.value.length > 0)
  const generationStatus = computed(() => quiz.value?.generationStatus ?? 'PENDING')
  const quizStatus = computed(() => quiz.value?.quizStatus ?? 'GENERATING')
  const normalizedStatus = computed(() =>
    normalizePracticeStatus({
      quiz: quiz.value,
      quizError: itemsError.value,
      requestingQuiz: requestingQuiz.value,
      pollingQuiz: pollingQuiz.value,
    }),
  )
  const statusView = computed(() =>
    getPracticeStatusViewModel({
      status: normalizedStatus.value,
      quiz: quiz.value,
      error: itemsError.value,
    }),
  )

  function clearErrors() {
    itemsError.value = null
    submitError.value = null
    lastError.value = null
  }

  async function requestQuiz(sessionId: number) {
    requestingQuiz.value = true
    itemsError.value = null
    try {
      const response = await requestPracticeQuiz(sessionId)
      quiz.value = mergeQuiz(quiz.value, response)
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

  async function loadQuizStatus(sessionId: number) {
    pollingQuiz.value = true
    itemsError.value = null
    try {
      const response = await getPracticeQuizStatus(sessionId)
      quiz.value = mergeQuiz(quiz.value, response)
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

  async function loadQuiz(sessionId: number) {
    pollingQuiz.value = true
    itemsError.value = null
    try {
      const response = await getPracticeQuiz(sessionId)
      quiz.value = mergeQuiz(quiz.value, response)
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

  async function submitQuiz(sessionId: number, answers: Array<{ questionId: number; userAnswer: string }>) {
    submittingQuiz.value = true
    submitError.value = null
    try {
      const response = await submitPracticeQuiz(sessionId, answers)
      feedbackReport.value = response
      if (quiz.value) {
        quiz.value = {
          ...quiz.value,
          answeredCount: answers.length,
          quizStatus: 'FEEDBACK_READY',
        }
      }
      return response
    } catch (input) {
      const normalized = normalizeApiError(input)
      lastError.value = normalized
      submitError.value = normalized.message
      throw normalized
    } finally {
      submittingQuiz.value = false
    }
  }

  async function loadFeedbackReport(sessionId: number) {
    loadingFeedback.value = true
    submitError.value = null
    try {
      const response = await getPracticeFeedbackReport(sessionId)
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

  async function applyFeedback(sessionId: number, action: 'REVIEW' | 'NEXT_ROUND') {
    applyingFeedbackAction.value = true
    submitError.value = null
    try {
      const response = await applyPracticeFeedbackAction(sessionId, action)
      feedbackReport.value = response
      if (quiz.value) {
        quiz.value = {
          ...quiz.value,
          quizStatus: action === 'REVIEW' ? 'REVIEWING' : 'NEXT_ROUND',
        }
      }
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
    quiz.value = null
    feedbackReport.value = null
    requestingQuiz.value = false
    pollingQuiz.value = false
    submittingQuiz.value = false
    loadingFeedback.value = false
    applyingFeedbackAction.value = false
    clearErrors()
  }

  return {
    quiz,
    items,
    feedbackReport,
    lastError,
    requestingQuiz,
    pollingQuiz,
    submittingQuiz,
    loadingFeedback,
    applyingFeedbackAction,
    itemsError,
    submitError,
    hasItems,
    generationStatus,
    quizStatus,
    normalizedStatus,
    statusView,
    requestQuiz,
    loadQuizStatus,
    loadQuiz,
    submitQuiz,
    loadFeedbackReport,
    applyFeedback,
    clearErrors,
    reset,
  }
})
