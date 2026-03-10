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
    ? '提交后将进入学习会话页，系统会先生成学习计划，再继续后续学习。'
    : '请描述你想学什么，以及希望达到什么程度。',
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
    goalError.value = '请先输入学习目标。'
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
    submitError.value = sessionStore.error || '学习会话创建失败，请稍后重试。'
  }
}

function formatTime(raw: string) {
  const date = new Date(raw)
  if (Number.isNaN(date.getTime())) {
    return raw
  }
  return date.toLocaleString('zh-CN', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  })
}

function formatProgress(item: SessionHistoryItem) {
  if (!item.progress) return '暂无进度'
  const percent = Math.round(item.progress.completionRate * 100)
  return `${percent}% (${item.progress.completedTaskCount}/${item.progress.totalTaskCount})`
}

function formatHistoryStage(item: SessionHistoryItem) {
  return getStatusLabel(item.status)
}

function formatHistoryTitle(item: SessionHistoryItem) {
  return item.goal?.trim() || `${item.course} / ${item.chapter}`
}

function formatHistoryTechMeta(item: SessionHistoryItem) {
  return `会话 #${item.sessionId}`
}

async function loadRecentHistory() {
  historyLoading.value = true
  historyError.value = ''
  try {
    const response = await getSessionHistory({ page: 1, pageSize: 5 })
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
    <section class="hero-panel">
      <PageHeader
        eyebrow="AI Learning Navigator"
        title="加载中..."
        subtitle="正在检查是否需要恢复上一次学习会话。"
      />
    </section>
    <section class="form-panel"></section>
  </main>

  <main v-else class="home-page">
    <header class="home-toolbar">
      <span class="username">{{ username }}</span>
      <button type="button" class="ghost-btn" @click="goHistory">历史记录</button>
      <button type="button" class="ghost-btn" @click="handleLogout">退出登录</button>
    </header>

    <section class="hero-panel">
      <PageHeader
        eyebrow="AI Learning Navigator"
        title="发起一次学习会话"
        subtitle="输入学习目标、课程标识和章节标识，系统会先生成学习计划，再进入学习会话。"
      />

      <section class="intro-card">
        <div class="intro-head">
          <h3>本页用途</h3>
          <span class="intro-tag">创建入口</span>
        </div>
        <p class="intro-copy">这里不是学习进度条，而是新会话入口。真正的学习阶段会在会话页内展示。</p>
        <div class="intro-points">
          <article class="intro-point">
            <strong>生成学习计划</strong>
            <span>先梳理目标与章节范围，确定起始学习路径。</span>
          </article>
          <article class="intro-point">
            <strong>进入学习会话</strong>
            <span>在会话页查看当前阶段、推荐动作和详细任务。</span>
          </article>
          <article class="intro-point">
            <strong>继续最近学习</strong>
            <span>从右侧最近学习记录快速恢复上次进度。</span>
          </article>
        </div>
      </section>

      <section class="history-card">
        <div class="history-head">
          <h3>最近学习记录</h3>
          <button type="button" class="ghost-btn" @click="goHistory">查看全部</button>
        </div>

        <p v-if="historyLoading" class="history-tip">加载最近学习记录中...</p>
        <p v-else-if="historyError" class="history-error">{{ historyError }}</p>
        <div v-else-if="recentHistory.length === 0" class="history-empty">
          <p class="history-tip">还没有最近学习记录。</p>
          <p class="history-tip muted">填写左侧表单并生成学习计划后，这里会显示可继续的学习会话。</p>
        </div>

        <div v-else class="history-list">
          <article v-for="item in recentHistory" :key="item.sessionId" class="history-item animate-fade-in-up">
            <div class="history-item-head">
              <p class="history-goal">{{ formatHistoryTitle(item) }}</p>
              <span class="history-stage">{{ formatHistoryStage(item) }}</span>
            </div>
            <p class="history-meta">{{ item.course }} / {{ item.chapter }}</p>
            <div class="history-grid">
              <span class="history-field">当前阶段：{{ formatHistoryStage(item) }}</span>
              <span class="history-field">当前进度：{{ formatProgress(item) }}</span>
              <span class="history-field">最近更新时间：{{ formatTime(item.lastActiveAt) }}</span>
              <span class="history-tech">{{ formatHistoryTechMeta(item) }}</span>
            </div>
            <div class="history-actions">
              <PrimaryButton
                type="button"
                :disabled="resumingSessionId === item.sessionId"
                @click="openHistorySession(item)"
              >
                {{ resumingSessionId === item.sessionId ? '进入中...' : '继续学习' }}
              </PrimaryButton>
            </div>
          </article>
        </div>
      </section>
    </section>

    <section class="form-panel">
      <form class="start-form" @submit.prevent="handleSubmit">
        <PageHeader
          eyebrow="New Session"
          title="创建学习会话"
          subtitle="完成下面三项输入后，即可生成本次学习计划。"
        />

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
  </main>
</template>

<style scoped>
.home-page {
  min-height: 100dvh;
  padding: clamp(20px, 4vw, 40px);
  display: grid;
  grid-template-columns: 1.1fr 1fr;
  gap: clamp(18px, 3vw, 32px);
}

.home-toolbar {
  grid-column: 1 / -1;
  display: flex;
  justify-content: flex-end;
  align-items: center;
  gap: var(--space-md);
}

.username {
  color: var(--color-text-secondary);
  font-size: var(--font-size-sm);
}

.hero-panel,
.form-panel {
  border: 1px solid var(--color-border);
  border-radius: var(--radius-xl);
  background: linear-gradient(160deg, var(--color-bg-elevated), var(--color-bg-surface));
  box-shadow: var(--shadow-md);
  transition: all var(--duration-normal) var(--ease-smooth);
}

.hero-panel {
  padding: clamp(20px, 4vw, 40px);
  display: flex;
  flex-direction: column;
  gap: var(--space-xl);
}

.hero-panel:hover {
  box-shadow: var(--shadow-lg);
}

.form-panel {
  padding: clamp(18px, 3vw, 28px);
}

.start-form {
  display: flex;
  flex-direction: column;
  gap: var(--space-lg);
}

.action-block {
  border-top: 1px solid var(--color-border);
  padding-top: var(--space-lg);
  display: flex;
  flex-direction: column;
  gap: var(--space-sm);
}

.intro-card,
.history-card {
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: var(--space-lg);
  background: rgba(12, 21, 42, 0.8);
  display: grid;
  gap: var(--space-md);
}

.intro-head,
.history-head,
.history-item-head,
.history-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: var(--space-sm);
}

.intro-tag,
.history-stage {
  border: 1px solid rgba(107, 159, 255, 0.25);
  border-radius: var(--radius-full);
  padding: 6px 12px;
  color: var(--color-text-secondary);
  background: rgba(107, 159, 255, 0.1);
  font-size: var(--font-size-xs);
}

.intro-copy {
  margin: 0;
  color: var(--color-text-secondary);
  line-height: 1.7;
}

.intro-points {
  display: grid;
  gap: var(--space-sm);
}

.intro-point {
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  padding: var(--space-md);
  background: var(--color-bg);
  display: grid;
  gap: 6px;
}

.intro-point strong {
  color: var(--color-text);
}

.intro-point span {
  color: var(--color-text-secondary);
  font-size: var(--font-size-sm);
  line-height: 1.6;
}

.history-list {
  display: grid;
  gap: var(--space-md);
}

.history-item {
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  padding: var(--space-md);
  background: var(--color-bg);
  transition: all var(--duration-normal) var(--ease-smooth);
}

.history-item:hover {
  border-color: var(--color-border-hover);
  transform: translateY(-2px);
  box-shadow: var(--shadow-sm);
}

.history-goal {
  margin: 0;
  color: var(--color-text);
  font-size: var(--font-size-md);
}

.history-meta,
.history-field,
.history-tip,
.history-tech {
  color: var(--color-text-secondary);
  font-size: var(--font-size-sm);
}

.history-grid {
  display: grid;
  gap: 6px;
  margin-top: var(--space-sm);
}

.history-tech {
  color: var(--color-text-muted);
  font-size: var(--font-size-xs);
}

.history-empty {
  display: grid;
  gap: 4px;
}

.muted {
  color: var(--color-text-muted);
}

.history-error {
  margin: 0;
  color: var(--color-error);
}

.ghost-btn {
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  background: var(--color-bg-surface);
  color: var(--color-text-secondary);
  padding: 8px 14px;
  font-size: var(--font-size-sm);
  transition: all var(--duration-fast) var(--ease-smooth);
}

.ghost-btn:hover:not(:disabled) {
  background: var(--color-bg-hover);
  border-color: var(--color-border-hover);
  color: var(--color-text);
}

.ghost-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.submit-error {
  margin: 0;
  color: var(--color-error);
  font-size: var(--font-size-sm);
}

@media (max-width: 980px) {
  .home-page {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 720px) {
  .intro-head,
  .history-head,
  .history-item-head,
  .history-actions {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
