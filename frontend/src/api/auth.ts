import { request } from './request'
import type {
  AuthMeResponse,
  AuthUser,
  LoginRequest,
  RegisterRequest,
} from '@/types/dto'

export async function fetchMe(): Promise<AuthMeResponse> {
  const { data } = await request.get<AuthMeResponse>('/api/auth/me')
  return data
}

export async function login(payload: LoginRequest): Promise<AuthUser> {
  const { data } = await request.post<AuthUser>('/api/auth/login', payload)
  return data
}

export async function register(payload: RegisterRequest): Promise<AuthUser> {
  const { data } = await request.post<AuthUser>('/api/auth/register', payload)
  return data
}

export async function logout(): Promise<void> {
  await request.post('/api/auth/logout')
}
