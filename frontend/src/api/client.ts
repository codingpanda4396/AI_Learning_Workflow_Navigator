import axios from 'axios'
import type {
  CreateSessionRequest,
  CreateSessionResponse,
  PlanSessionResponse,
  SessionOverviewResponse,
  RunTaskResponse,
  SubmitTaskRequest,
  SubmitTaskResponse,
} from '@/types'

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || '/api'

const client = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json; charset=utf-8',
  },
})

export const sessionApi = {
  create: (data: CreateSessionRequest) =>
    client.post<CreateSessionResponse>('/session/create', data),

  plan: (sessionId: number) =>
    client.post<PlanSessionResponse>(`/session/${sessionId}/plan`),

  getOverview: (sessionId: number) =>
    client.get<SessionOverviewResponse>(`/session/${sessionId}/overview`),
}

export const taskApi = {
  run: (taskId: number) =>
    client.post<RunTaskResponse>(`/task/${taskId}/run`),

  submit: (taskId: number, data: SubmitTaskRequest) =>
    client.post<SubmitTaskResponse>(`/task/${taskId}/submit`, data),
}

export default client
