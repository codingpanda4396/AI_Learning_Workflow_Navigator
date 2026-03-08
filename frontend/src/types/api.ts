export interface ApiErrorResponseDto {
  error?: string
  message?: string
}

export interface NormalizedApiError extends Error {
  status: number
  code: string
  retryable: boolean
  details?: unknown
}

export interface ApiRequestConfig {
  dedupeKey?: string
}
