<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useSessionStore } from '@/stores/session'
import { useWorkflowStore } from '@/stores/workflow'
import { getSessionHistory, resumeSession } from '@/api/session'
import type { SessionHistoryItem } from '@/types'
import { normalizeApiError } from '@/utils/apiError'
import PageHeader from '@/components/PageHeader.vue'
import GoalInputCard from '@/components/GoalInputCard.vue'
import CourseSelector from '@/components/CourseSelector.vue'
import PrimaryButton from '@/components/PrimaryButton.vue'
import ContinueSessionCard from '@/components/ContinueSessionCard.vue'

const SKIP_RESUME_ONCE_KEY = 'ai_learning_skip_resume_once'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const sessionStore = useSessionStore()
const workflowStore = useWorkflowStore()

const goal = ref(workflowStore.goal || '')
const courseId = ref(workflowStore.courseId || '')
const chapterId = ref(workflowStore.chapterId || '')

const goalError = ref('')
const courseError = ref('')
const chapterError = ref('')
const submitError = ref('')
const checkingResume = ref(true)
const historyLoading = ref(false)
const historyError = ref('')
const recentHistory = ref<SessionHistoryItem[]>([])
const resumingSessionId = ref<number | null>(null)

const isCreating = computed(() => sessionStore.creatingSession || sessionStore.planning)
const username = computed(() => authStore.currentUser?.username ?? '')
const latestSession = computed(() => recentHistory.value[0] ?? null)
const canSubmit = computed(
  () =>
    goal.value.trim().length > 0 &&
    courseId.value.trim().length > 0 &&
    chapterId.value.trim().length > 0 &&
    !isCreating.value,
)
const startButtonLabel = computed(() => (isCreating.value ? '正在准备学习计划...' : '开始学习'))

function setCourse(nextCourseId: string) {
  courseId.value = nextCourseId
  courseError.value = ''
}

function setChapter(nextChapterId: string) {
  chapterId.value = nextChapterId
  chapterError.value = ''
}

function validateInputs() {
  let valid = true

  if (!goal.value.trim()) {
    goalError.value = '请先写下这轮想学会什么。'
    valid = false
  } else {
    goalError.value = ''
  }

  if (!courseId.value.trim()) {
    courseError.value = '请选择课程。'
    valid = false
  } else {
    courseError.value = ''
  }

  if (!chapterId.value.trim()) {
    chapterError.value = '请选择章节。'
    valid = false
  } else {
    chapterError.value = ''
  }

  return valid
}

async function handleSubmit() {
  if (!validateInputs() || isCreating.value) return

  submitError.value = ''
  const payload = {
    courseId: courseId.value.trim(),
    chapterId: chapterId.value.trim(),
    goalText: goal.value.trim(),
  }

  workflowStore.startWorkflow({
    goal: payload.goalText,
    courseId: payload.courseId,
    chapterId: payload.chapterId,
  })

  try {
    const newSessionId = await sessionStore.createSession(payload)
    await sessionStore.planSession(newSessionId)
    await sessionStore.fetchSessionOverview(newSessionId)
    workflowStore.setWorkflowId(String(newSessionId))
    await router.push(`/session/${newSessionId}`)
  } catch {
    submitError.value = sessionStore.error || '这轮学习还没创建成功，请稍后再试。'
  }
}

function formatTime(raw: string) {
  const date = new Date(raw)
  if (Number.isNaN(date.getTime())) return raw
  return `上次学习 ${date.toLocaleString('zh-CN', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  })}`
}

function formatProgress(item: SessionHistoryItem) {
  if (!item.progress) return '还没有进度'
  return `学到第 ${item.progress.completedTaskCount + 1} 步，共 ${item.progress.totalTaskCount} 步`
}

async function loadRecentHistory() {
  historyLoading.value = true
  historyError.value = ''
  try {
    const response = await getSessionHistory({ page: 1, pageSize: 1 })
    recentHistory.value = response.items
  } catch (input) {
    historyError.value = normalizeApiError(input).message
  } finally {
    historyLoading.value = false
  }
}

async function openHistorySession(item: SessionHistoryItem) {
  resumingSessionId.value = item.sessionId
  try {
    await resumeSession(item.sessionId)
    await router.push(`/session/${item.sessionId}`)
  } catch (input) {
    historyError.value = normalizeApiError(input).message
  } finally {
    resumingSessionId.value = null
  }
}

async function goHistory() {
  await router.push('/history')
}

async function handleLogout() {
  authStore.clearAuth()
  sessionStore.reset()
  workflowStore.reset()
  await router.replace('/auth')
}

onMounted(async () => {
  try {
    const skipByQuery = route.query.skipResume === '1'
    const skipByFlag = localStorage.getItem(SKIP_RESUME_ONCE_KEY) === '1'
    if (skipByQuery || skipByFlag) {
      localStorage.removeItem(SKIP_RESUME_ONCE_KEY)
      if (skipByQuery) {
        await router.replace({ name: 'home' })
      }
      await loadRecentHistory()
      return
    }

    await sessionStore.fetchCurrentSession()
    await loadRecentHistory()
  } catch {
    await loadRecentHistory()
  } finally {
    checkingResume.value = false
  }
})
</script>

<template>
  <main v-if="checkingResume" class="home-page">
    <section class="surface-card loading-card">
      <PageHeader eyebrow="Learning Navigator" title="正在恢复学习" subtitle="先检查你是否有未完成的学习。" />
    </section>
  </main>

  <main v-else class="home-page">
    <header class="home-toolbar">
      <span class="username">{{ username }}</span>
      <button type="button" class="ghost-btn" @click="goHistory">历史记录</button>
      <button type="button" class="ghost-btn" @click="handleLogout">退出登录</button>
    </header>

    <section class="surface-card hero-card">
      <PageHeader
        eyebrow="开始一轮学习"
        title="告诉系统你想学什么"
        subtitle="填好目标、课程和章节，然后直接开始。"
      />

      <form class="start-form" @submit.prevent="handleSubmit">
        <GoalInputCard
          v-model="goal"
          :hint="'用一句话写下你这轮想学会什么，系统会按这个目标带着你往下学。'"
          :error="goalError"
        />
        <CourseSelector
          :course-id="courseId"
          :chapter-id="chapterId"
          @update:courseId="setCourse"
          @update:chapterId="setChapter"
        />
        <p v-if="courseError" class="submit-error">{{ courseError }}</p>
        <p v-if="chapterError" class="submit-error">{{ chapterError }}</p>
        <div class="primary-action">
          <PrimaryButton type="submit" :disabled="!canSubmit" :loading="isCreating">
            {{ startButtonLabel }}
          </PrimaryButton>
          <p v-if="submitError" class="submit-error">{{ submitError }}</p>
        </div>
      </form>
    </section>

    <section class="surface-card resume-card">
      <div class="resume-head">
        <PageHeader eyebrow="继续上次学习" title="从你上次停下的地方继续" />
        <button type="button" class="ghost-btn subtle" @click="goHistory">更多记录</button>
      </div>

      <p v-if="historyLoading" class="muted-text">正在加载最近学习...</p>
      <p v-else-if="historyError" class="submit-error">{{ historyError }}</p>
      <p v-else-if="!latestSession" class="muted-text">还没有上次学习记录，直接开始一轮新的学习吧。</p>
      <ContinueSessionCard
        v-else
        :title="latestSession.goal || '继续上次学习目标'"
        :subtitle="`${latestSession.course} / ${latestSession.chapter}`"
        :progress="formatProgress(latestSession)"
        :updated-at="formatTime(latestSession.lastActiveAt)"
        stage="继续当前进度"
        :loading="resumingSessionId === latestSession.sessionId"
        :disabled="resumingSessionId === latestSession.sessionId"
        @continue="openHistorySession(latestSession)"
      />
    </section>
  </main>
</template>

<style scoped>
.home-page {
  min-height: 100dvh;
  padding: clamp(20px, 4vw, 40px);
  display: grid;
  gap: 22px;
}

.home-toolbar,
.surface-card {
  width: min(880px, 100%);
  margin: 0 auto;
}

.home-toolbar {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.surface-card {
  border: 1px solid var(--color-border);
  border-radius: var(--radius-xl);
  background: rgba(15, 21, 33, 0.96);
}

.hero-card,
.resume-card,
.loading-card {
  padding: clamp(24px, 4vw, 36px);
  display: grid;
  gap: 22px;
}

.start-form {
  display: grid;
  gap: 20px;
}

.primary-action {
  display: grid;
  gap: 10px;
  justify-items: start;
}

.resume-head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
}

.username,
.muted-text {
  color: var(--color-text-secondary);
  font-size: var(--font-size-sm);
}

.ghost-btn {
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  background: rgba(10, 15, 24, 0.9);
  color: var(--color-text-secondary);
  padding: 10px 14px;
  font-size: var(--font-size-sm);
}

.subtle {
  padding-inline: 12px;
}

.submit-error {
  margin: 0;
  color: var(--color-error);
  font-size: var(--font-size-sm);
}

@media (max-width: 720px) {
  .resume-head {
    flex-direction: column;
    align-items: stretch;
  }

  .primary-action :deep(.btn) {
    width: 100%;
  }
}
</style>
