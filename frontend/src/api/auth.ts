import client from '@/api/client'
import type { AuthUser, LoginRequest, RegisterRequest } from '@/types'

interface RegisterResponseDto {
  user_id: number
}

interface LoginResponseDto {
  token: string
  user: AuthUser
}

export async function register(input: RegisterRequest): Promise<number> {
  const payload = {
    username: input.username,
    password: input.password,
  }
  const { data } = await client.post<RegisterResponseDto>('/auth/register', payload)
  return data.user_id
}

export async function login(input: LoginRequest): Promise<LoginResponseDto> {
  const payload = {
    username: input.username,
    password: input.password,
  }
  const { data } = await client.post<LoginResponseDto>('/auth/login', payload)
  return data
}

export async function getCurrentUser(): Promise<AuthUser> {
  const { data } = await client.get<AuthUser>('/users/me')
  return data
}
