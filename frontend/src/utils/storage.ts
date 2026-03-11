import { STORAGE_KEYS } from '@/constants/app';

export function getStoredToken() {
  return localStorage.getItem(STORAGE_KEYS.token) ?? '';
}

export function setStoredAuth(token: string, username: string) {
  localStorage.setItem(STORAGE_KEYS.token, token);
  localStorage.setItem(STORAGE_KEYS.username, username);
}

export function clearStoredAuth() {
  localStorage.removeItem(STORAGE_KEYS.token);
  localStorage.removeItem(STORAGE_KEYS.username);
}

export function getStoredUsername() {
  return localStorage.getItem(STORAGE_KEYS.username) ?? '';
}

export function getLastSessionId() {
  return localStorage.getItem(STORAGE_KEYS.lastSessionId) ?? '';
}

export function setLastSessionId(sessionId: number | string) {
  localStorage.setItem(STORAGE_KEYS.lastSessionId, String(sessionId));
}
