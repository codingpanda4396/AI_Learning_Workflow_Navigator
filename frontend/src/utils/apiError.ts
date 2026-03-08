import axios from 'axios'
import type { ApiErrorResponseDto, NormalizedApiError } from '@/types/api'

const RETRYABLE_STATUS = new Set([408, 429, 500, 502, 503, 504])

function createApiError(params: {
  message: string
  status: number
  code: string
  retryable: boolean
  details?: unknown
}): NormalizedApiError {
  const error = new Error(params.message) as NormalizedApiError
  error.name = 'NormalizedApiError'
  error.status = params.status
  error.code = params.code
  error.retryable = params.retryable
  error.details = params.details
  return error
}

export function normalizeApiError(input: unknown): NormalizedApiError {
  if (axios.isAxiosError(input)) {
    const response = input.response
    const status = response?.status ?? 0
    const payload = (response?.data ?? {}) as ApiErrorResponseDto
    const code = payload.error || input.code || 'UNKNOWN_ERROR'
    const message =
      payload.message ||
      input.message ||
      'Request failed. Please try again later.'
    const retryable =
      input.code === 'ECONNABORTED' ||
      input.code === 'ERR_NETWORK' ||
      RETRYABLE_STATUS.has(status)

    return createApiError({
      message,
      status,
      code,
      retryable,
      details: response?.data,
    })
  }

  if (input instanceof Error) {
    return createApiError({
      message: input.message || 'Unexpected error.',
      status: 0,
      code: input.name || 'UNKNOWN_ERROR',
      retryable: false,
    })
  }

  return createApiError({
    message: 'Unexpected error.',
    status: 0,
    code: 'UNKNOWN_ERROR',
    retryable: false,
    details: input,
  })
}
