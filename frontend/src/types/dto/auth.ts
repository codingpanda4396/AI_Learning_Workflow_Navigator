export interface AuthUser {
  id: number
  username: string
  displayName: string
}

export interface RecentLearningEntry {
  goalId?: string
  diagnosisId?: string
  planId?: string
  sessionId?: string
  currentTaskId?: string
  sessionStatus?: string
}

export interface AuthMeResponse {
  authenticated: boolean
  user?: AuthUser | null
  recentLearningEntry?: RecentLearningEntry | null
}

export interface LoginRequest {
  username: string
  password: string
}

export interface RegisterRequest {
  username: string
  password: string
}
