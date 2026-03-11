import apiClient from '@/api/client';
import { normalizeLoginResponse } from '@/api/normalizers';
import type { LoginPayload, LoginResponse, RegisterPayload } from '@/types/auth';

export async function registerApi(payload: RegisterPayload) {
  const { data } = await apiClient.post('/api/auth/register', payload);
  return data as { user_id?: number };
}

export async function loginApi(payload: LoginPayload): Promise<LoginResponse> {
  const { data } = await apiClient.post('/api/auth/login', payload);
  return normalizeLoginResponse(data);
}

export async function fetchMeApi() {
  const { data } = await apiClient.get('/api/users/me');
  return data as { id?: number; username?: string };
}
