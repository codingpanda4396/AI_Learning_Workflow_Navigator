import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { fetchMe, login as loginApi, logout as logoutApi, register as registerApi } from '@/api/auth'
import type { AuthUser, RecentLearningEntry } from '@/types/dto'

const STORAGE_KEYS = {
  redirect: 'auth_redirect',
} as const

function getStoredRedirect() {
  try {
    return sessionStorage.getItem(STORAGE_KEYS.redirect)
  } catch {
    return null
  }
}

function setStoredRedirect(value: string | null) {
  try {
    if (value) {
      sessionStorage.setItem(STORAGE_KEYS.redirect, value)
    } else {
      sessionStorage.removeItem(STORAGE_KEYS.redirect)
    }
  } catch {
    // ignore
  }
}

export const useAuthStore = defineStore('auth', () => {
  const user = ref<AuthUser | null>(null)
  const recentLearningEntry = ref<RecentLearningEntry | null>(null)
  const isReady = ref(false)
  const pendingRedirect = ref<string | null>(getStoredRedirect())
  let initPromise: Promise<void> | null = null

  const isAuthenticated = computed(() => !!user.value)

  async function ensureReady() {
    if (isReady.value) return
    if (!initPromise) {
      initPromise = (async () => {
        try {
          await refresh()
        } finally {
          isReady.value = true
        }
      })()
    }
    await initPromise
  }

  async function refresh() {
    const data = await fetchMe()
    user.value = data.user ?? null
    recentLearningEntry.value = data.recentLearningEntry ?? null
  }

  function setPendingRedirect(path: string | null) {
    pendingRedirect.value = path
    setStoredRedirect(path)
  }

  function consumePendingRedirect() {
    const value = pendingRedirect.value
    setPendingRedirect(null)
    return value
  }

  async function login(username: string, password: string) {
    user.value = await loginApi({ username, password })
    await refresh()
  }

  async function register(username: string, password: string) {
    user.value = await registerApi({ username, password })
    await refresh()
  }

  async function logout() {
    await logoutApi()
    user.value = null
    recentLearningEntry.value = null
    setPendingRedirect(null)
  }

  return {
    user,
    recentLearningEntry,
    isReady,
    isAuthenticated,
    pendingRedirect,
    ensureReady,
    refresh,
    setPendingRedirect,
    consumePendingRedirect,
    login,
    register,
    logout,
  }
})
