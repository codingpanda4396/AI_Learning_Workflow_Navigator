<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useSessionStore } from '@/stores/session'
import { useWorkflowStore } from '@/stores/workflow'
import { getSessionHistory, resumeSession } from '@/api/session'
import type { SessionHistoryItem } from '@/types'
import { normalizeApiError } from '@/utils/apiError'
import { getStatusLabel } from '@/utils/learningPlanDisplay'
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

const hasDraftInput = computed(
  () => goal.value.trim().length > 0 || courseId.value.trim().length > 0 || chapterId.value.trim().length > 0,
)

const goalHint = computed(() =>
  goal.value.trim().length > 0
    ? '提交后会先生成学习计划，再自动进入本轮学习。'
    : '用一句话写清楚你想学什么、希望达到什么程度。',
)

function setCourse(nextCourseId: string) {
  courseId.value = nextCourseId
  if (courseError.value) courseError.value = ''
}

function setChapter(nextChapterId: string) {
  chapterId.value = nextChapterId
  if (chapterError.value) chapterError.value = ''
}

function validateInputs() {
  let valid = true

  if (!goal.value.trim()) {
    goalError.value = '请输入学习目标。'
    valid = false
  } else {
    goalError.value = ''
  }

  if (!courseId.value.trim()) {
    courseError.value = '请输入课程标识。'
    valid = false
  } else {
    courseError.value = ''
  }

  if (!chapterId.value.trim()) {
    chapterError.value = '请输入章节标识。'
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
    submitError.value = sessionStore.error || '创建学习会话失败，请稍后重试。'
  }
}

function formatTime(raw: string) {
  const date = new Date(raw)
  if (Number.isNaN(date.getTime())) {
    return raw
  }
  return `最近学习 ${date.toLocaleString('zh-CN', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  })}`
}

function formatProgress(item: SessionHistoryItem) {
  if (!item.progress) return '暂无进度'
  const percent = Math.round(item.progress.completionRate * 100)
  return `进度 ${percent}%`
}

function formatHistoryStage(item: SessionHistoryItem) {
  return getStatusLabel(item.status)
}

function formatHistoryTitle(item: SessionHistoryItem) {
  return item.goal?.trim() || `${item.course} / ${item.chapter}`
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

    const response = await sessionStore.fetchCurrentSession()
    if (response.hasActiveSession && response.session && !hasDraftInput.value && route.name === 'home') {
      await router.replace(`/session/${response.session.sessionId}`)
      return
    }

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
      <PageHeader eyebrow="AI Learning Navigator" title="正在恢复学习" subtitle="检查你是否有未完成的学习会话。" />
    </section>
  </main>

  <main v-else class="home-page">
    <header class="home-toolbar">
      <span class="username">{{ username }}</span>
      <button type="button" class="ghost-btn" @click="goHistory">历史记录</button>
      <button type="button" class="ghost-btn" @click="handleLogout">退出登录</button>
    </header>

    <section class="surface-card form-card">
      <PageHeader
        eyebrow="Create Plan"
        title="创建学习计划"
        subtitle="填完这三个信息后，系统会直接带你进入本轮学习。"
      />

      <form class="start-form" @submit.prevent="handleSubmit">
        <GoalInputCard v-model="goal" :hint="goalHint" :error="goalError" />
        <CourseSelector
          :course-id="courseId"
          :chapter-id="chapterId"
          @update:courseId="setCourse"
          @update:chapterId="setChapter"
        />

        <p v-if="courseError" class="submit-error">{{ courseError }}</p>
        <p v-if="chapterError" class="submit-error">{{ chapterError }}</p>

        <div class="action-block">
          <PrimaryButton type="submit" :disabled="!canSubmit" :loading="isCreating">
            生成学习计划
          </PrimaryButton>
          <p v-if="submitError" class="submit-error">{{ submitError }}</p>
        </div>
      </form>
    </section>

    <section class="surface-card resume-card">
      <div class="resume-head">
        <PageHeader
          eyebrow="Continue"
          title="继续上次学习"
          subtitle="如果你刚学到一半，可以从这里直接接着学。"
        />
        <button type="button" class="ghost-btn" @click="goHistory">查看全部</button>
      </div>

      <p v-if="historyLoading" class="muted-text">正在加载最近学习...</p>
      <p v-else-if="historyError" class="submit-error">{{ historyError }}</p>
      <p v-else-if="!latestSession" class="muted-text">还没有最近学习记录，先创建一轮新的学习计划吧。</p>
      <ContinueSessionCard
        v-else
        :title="formatHistoryTitle(latestSession)"
        :subtitle="`${latestSession.course} / ${latestSession.chapter}`"
        :progress="formatProgress(latestSession)"
        :updated-at="formatTime(latestSession.lastActiveAt)"
        :stage="formatHistoryStage(latestSession)"
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
  gap: 20px;
}

.home-toolbar {
  width: min(880px, 100%);
  margin: 0 auto;
  display: flex;
  justify-content: flex-end;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.username {
  color: var(--color-text-secondary);
  font-size: var(--font-size-sm);
}

.surface-card {
  width: min(880px, 100%);
  margin: 0 auto;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-xl);
  background: rgba(15, 21, 33, 0.96);
}

.form-card {
  padding: clamp(24px, 4vw, 36px);
  display: grid;
  gap: 24px;
}

.resume-card,
.loading-card {
  padding: clamp(22px, 3vw, 30px);
  display: grid;
  gap: 18px;
}

.resume-head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
}

.start-form {
  display: grid;
  gap: 20px;
}

.action-block {
  display: grid;
  gap: 10px;
  padding-top: 8px;
}

.ghost-btn {
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  background: rgba(10, 15, 24, 0.9);
  color: var(--color-text-secondary);
  padding: 10px 14px;
  font-size: var(--font-size-sm);
}

.ghost-btn:hover:not(:disabled) {
  color: var(--color-text);
  border-color: var(--color-border-hover);
}

.submit-error {
  margin: 0;
  color: var(--color-error);
  font-size: var(--font-size-sm);
}

.muted-text {
  margin: 0;
  color: var(--color-text-secondary);
}

@media (max-width: 720px) {
  .resume-head {
    flex-direction: column;
    align-items: stretch;
  }
}
</style>
