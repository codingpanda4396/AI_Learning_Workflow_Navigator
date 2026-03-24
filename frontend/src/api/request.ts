import axios, { type AxiosError } from 'axios'
import type { GlobalResponse } from '@/types/dto'
import { errorCodeLabels } from '@/types/labels'

const baseURL = import.meta.env.DEV ? '' : 'http://localhost:8080'

export const request = axios.create({
  baseURL,
  timeout: 30000,
  withCredentials: true,
  headers: {
    'Content-Type': 'application/json',
  },
})

export interface ApiError {
  code: string
  message: string
}

request.interceptors.response.use(
  (response): typeof response => {
    const body = response.data as GlobalResponse<unknown>
    if (body.code !== 'OK') {
      const msg =
        errorCodeLabels[body.code] ?? body.message ?? '请求失败'
      throw { code: body.code, message: msg }
    }
    response.data = body.data
    return response
  },
  (err: AxiosError<GlobalResponse<unknown>>) => {
    if (err.response?.data?.code) {
      const body = err.response.data
      const msg =
        errorCodeLabels[body.code] ?? body.message ?? '请求失败'
      return Promise.reject<ApiError>({ code: body.code, message: msg })
    }
    const msg = err.message || '网络错误'
    return Promise.reject<ApiError>({ code: 'NETWORK_ERROR', message: msg })
  }
)

export function getErrorMessage(err: unknown): string {
  if (err && typeof err === 'object' && 'message' in err) {
    return (err as ApiError).message
  }
  return '未知错误'
}
