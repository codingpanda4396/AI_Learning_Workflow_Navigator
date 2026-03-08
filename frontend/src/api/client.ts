import axios from 'axios'
import type { AxiosRequestConfig, InternalAxiosRequestConfig } from 'axios'
import { normalizeApiError } from '@/utils/apiError'

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || '/api'
const API_TIMEOUT_MS = 90000
const pendingControllers = new Map<string, AbortController>()

type RequestConfigWithMeta = InternalAxiosRequestConfig & {
  dedupeKey?: string
}

function buildRequestKey(config: AxiosRequestConfig) {
  const method = (config.method ?? 'get').toUpperCase()
  const url = config.url ?? ''
  const params = config.params ? JSON.stringify(config.params) : ''
  const data = config.data ? JSON.stringify(config.data) : ''
  return `${method}:${url}:${params}:${data}`
}

function cleanupPendingRequest(config: AxiosRequestConfig | undefined) {
  if (!config) {
    return
  }
  const dedupeKey = (config as RequestConfigWithMeta).dedupeKey
  if (dedupeKey) {
    pendingControllers.delete(dedupeKey)
  }
}

const client = axios.create({
  baseURL: API_BASE_URL,
  timeout: API_TIMEOUT_MS,
  headers: {
    'Content-Type': 'application/json; charset=utf-8',
  },
})

client.interceptors.request.use((config) => {
  const dedupeKey = buildRequestKey(config)
  const existingController = pendingControllers.get(dedupeKey)
  if (existingController) {
    existingController.abort()
  }

  const controller = new AbortController()
  pendingControllers.set(dedupeKey, controller)
  config.signal = controller.signal
  ;(config as RequestConfigWithMeta).dedupeKey = dedupeKey

  return config
})

client.interceptors.response.use(
  (response) => {
    cleanupPendingRequest(response.config)
    return response
  },
  (error: unknown) => {
    if (axios.isAxiosError(error)) {
      cleanupPendingRequest(error.config)
    }
    return Promise.reject(normalizeApiError(error))
  },
)

export default client
