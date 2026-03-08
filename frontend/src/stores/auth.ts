import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { getCurrentUser, login, register } from '@/api/auth'
import {
  clearAccessToken,
  clearStoredUser,
  getAccessToken,
  getStoredUser,
  setAccessToken,
  setStoredUser,
} from '@/auth/storage'
import { normalizeApiError } from '@/utils/apiError'
import type { AuthUser, LoginRequest, RegisterRequest } from '@/types'
import type { NormalizedApiError } from '@/types/api'

export const useAuthStore = defineStore('auth', () => {
  const token = ref<string | null>(getAccessToken())
  const currentUser = ref<AuthUser | null>(getStoredUser())
  const initializing = ref(false)
  const signingIn = ref(false)
  const signingUp = ref(false)
  const lastError = ref<NormalizedApiError | null>(null)

  const isAuthenticated = computed(() => !!token.value && !!currentUser.value)
  const error = computed(() => lastError.value?.message ?? null)

  function clearError() {
    lastError.value = null
  }

  function setError(input: unknown) {
    lastError.value = normalizeApiError(input)
    return lastError.value
  }

  function applyAuth(nextToken: string, user: AuthUser) {
    token.value = nextToken
    currentUser.value = user
    setAccessToken(nextToken)
    setStoredUser(user)
  }

  function clearAuth() {
    token.value = null
    currentUser.value = null
    clearAccessToken()
    clearStoredUser()
  }

  async function bootstrap() {
    const storedToken = getAccessToken()
    const storedUser = getStoredUser()
    token.value = storedToken
    currentUser.value = storedUser
    if (!storedToken) {
      clearAuth()
      return
    }
    initializing.value = true
    try {
      const me = await getCurrentUser()
      currentUser.value = me
      setStoredUser(me)
      clearError()
    } catch {
      clearAuth()
    } finally {
      initializing.value = false
    }
  }

  async function signIn(input: LoginRequest) {
    signingIn.value = true
    clearError()
    try {
      const result = await login(input)
      applyAuth(result.token, result.user)
      return result.user
    } catch (errorInput) {
      throw setError(errorInput)
    } finally {
      signingIn.value = false
    }
  }

  async function signUp(input: RegisterRequest) {
    signingUp.value = true
    clearError()
    try {
      await register(input)
    } catch (errorInput) {
      throw setError(errorInput)
    } finally {
      signingUp.value = false
    }
  }

  async function refreshCurrentUser() {
    if (!token.value) {
      return null
    }
    clearError()
    try {
      const me = await getCurrentUser()
      currentUser.value = me
      setStoredUser(me)
      return me
    } catch (errorInput) {
      clearAuth()
      throw setError(errorInput)
    }
  }

  return {
    token,
    currentUser,
    initializing,
    signingIn,
    signingUp,
    lastError,
    error,
    isAuthenticated,
    bootstrap,
    signIn,
    signUp,
    refreshCurrentUser,
    clearAuth,
    clearError,
  }
})
