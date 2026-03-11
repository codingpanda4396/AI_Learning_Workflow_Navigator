<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { getSessionHistory, resumeSession } from '@/api/session'
import type { SessionHistoryItem } from '@/types'
import { normalizeApiError } from '@/utils/apiError'

const router = useRouter()

const loading = ref(false)
const resumingId = ref<number | null>(null)
const error = ref('')
const page = ref(1)
const pageSize = ref(10)
const total = ref(0)
const totalPages = ref(0)
const items = ref<SessionHistoryItem[]>([])

const hasPrev = computed(() => page.value > 1)
const hasNext = computed(() => page.value < totalPages.value)
const completedSessions = computed(() => items.value.filter((item) => item.progress?.completionRate === 1).length)
const activeSessions = computed(() => items.value.filter((item) => (item.progress?.completionRate ?? 0) < 1).length)
const latestItem = computed(() => items.value[0] ?? null)

function formatTime(raw: string) {
  const date = new Date(raw)
  if (Number.isNaN(date.getTime())) {
    return raw
  }
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  })
}

function progressText(item: SessionHistoryItem) {
  if (!item.progress) return '还没有开始形成进度'
  const percent = Math.round(item.progress.completionRate * 100)
  return `完成 ${percent}%（${item.progress.completedTaskCount}/${item.progress.totalTaskCount}）`
}

async function loadHistory() {
  loading.value = true
  error.value = ''
  try {
    const data = await getSessionHistory({ page: page.value, pageSize: pageSize.value })
    total.value = data.total
    totalPages.value = data.totalPages
    items.value = data.items
  } catch (input) {
    error.value = normalizeApiError(input).message
  } finally {
    loading.value = false
  }
}

async function goPrev() {
  if (!hasPrev.value) return
  page.value -= 1
  await loadHistory()
}

async function goNext() {
  if (!hasNext.value) return
  page.value += 1
  await loadHistory()
}

async function handleResume(item: SessionHistoryItem) {
  resumingId.value = item.sessionId
  error.value = ''
  try {
    await resumeSession(item.sessionId)
    await router.push(`/session/${item.sessionId}`)
  } catch (input) {
    error.value = normalizeApiError(input).message
  } finally {
    resumingId.value = null
  }
}

function goHome() {
  router.push('/')
}

onMounted(async () => {
  await loadHistory()
})
</script>

<template>
  <main class="history-page">
    <header class="toolbar">
      <button class="ghost-button" @click="goHome">返回首页</button>
      <div class="title-block">
        <h1>成长记录</h1>
        <p>这里汇总你的学习会话、最近进度和继续入口。</p>
      </div>
      <span class="summary">共 {{ total }} 条会话</span>
    </header>

    <section class="panel">
      <div class="overview-grid">
        <article class="overview-card">
          <span class="overview-label">累计学习</span>
          <strong>{{ total }}</strong>
          <p>系统已记录的学习会话数</p>
        </article>
        <article class="overview-card">
          <span class="overview-label">进行中</span>
          <strong>{{ activeSessions }}</strong>
          <p>仍可继续推进的学习会话</p>
        </article>
        <article class="overview-card">
          <span class="overview-label">已完成</span>
          <strong>{{ completedSessions }}</strong>
          <p>已经走完整轮流程的学习会话</p>
        </article>
        <article class="overview-card">
          <span class="overview-label">最近更新</span>
          <strong>{{ latestItem ? latestItem.chapter : '暂无' }}</strong>
          <p>{{ latestItem ? formatTime(latestItem.lastActiveAt) : '开始一轮学习后会显示在这里。' }}</p>
        </article>
      </div>

      <p v-if="loading" class="tip">加载中...</p>
      <p v-else-if="error" class="error">{{ error }}</p>
      <p v-else-if="items.length === 0" class="tip">还没有成长记录，先开始一轮新的学习吧。</p>

      <div v-else class="list">
        <article v-for="item in items" :key="item.sessionId" class="card">
          <div class="card-head">
            <div>
              <h3>{{ item.goal || `学习会话 #${item.sessionId}` }}</h3>
              <p>{{ item.course }} / {{ item.chapter }}</p>
            </div>
            <span class="status">{{ progressText(item) }}</span>
          </div>
          <p><strong>最近活跃：</strong>{{ formatTime(item.lastActiveAt) }}</p>
          <p><strong>当前状态：</strong>{{ item.status }}</p>
          <button
            class="resume-btn"
            :disabled="resumingId === item.sessionId"
            @click="handleResume(item)"
          >
            {{ resumingId === item.sessionId ? '进入中...' : '继续学习' }}
          </button>
        </article>
      </div>

      <nav class="pager">
        <button class="ghost-button" :disabled="!hasPrev || loading" @click="goPrev">上一页</button>
        <span>第 {{ page }} / {{ totalPages || 1 }} 页</span>
        <button class="ghost-button" :disabled="!hasNext || loading" @click="goNext">下一页</button>
      </nav>
    </section>
  </main>
</template>

<style scoped>
.history-page {
  min-height: 100dvh;
  padding: clamp(16px, 2.8vw, 30px);
}

.toolbar {
  display: flex;
  align-items: flex-start;
  gap: var(--space-md);
  justify-content: space-between;
  margin-bottom: var(--space-lg);
  flex-wrap: wrap;
}

.title-block {
  display: grid;
  gap: 6px;
}

.title-block h1,
.title-block p {
  margin: 0;
}

.title-block p,
.summary,
.tip {
  color: var(--color-text-secondary);
  font-size: var(--font-size-sm);
}

.panel {
  border: 1px solid var(--color-border);
  border-radius: var(--radius-xl);
  background: linear-gradient(165deg, rgba(16, 27, 50, 0.94), rgba(8, 14, 26, 0.96));
  padding: clamp(16px, 2.8vw, 26px);
  box-shadow: var(--shadow-md);
  display: grid;
  gap: 18px;
}

.overview-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
}

.overview-card,
.card {
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  background: rgba(12, 20, 38, 0.8);
  padding: var(--space-md);
}

.overview-card {
  display: grid;
  gap: 8px;
}

.overview-card strong {
  font-size: 1.6rem;
  color: var(--color-text);
}

.overview-card p,
.card p {
  margin: 0;
  color: var(--color-text-secondary);
}

.overview-label {
  color: var(--color-text-muted);
  font-size: var(--font-size-xs);
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.error {
  color: var(--color-error);
}

.list {
  display: grid;
  gap: var(--space-md);
}

.card {
  display: grid;
  gap: 8px;
}

.card-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.card-head h3,
.card-head p {
  margin: 0;
}

.status {
  border: 1px solid var(--color-border);
  border-radius: 999px;
  padding: 6px 10px;
  color: var(--color-text-secondary);
  font-size: var(--font-size-xs);
}

.resume-btn {
  width: 140px;
  min-height: 40px;
  border-radius: var(--radius-md);
  background: var(--color-primary);
  color: #fff;
}

.resume-btn:disabled {
  opacity: 0.6;
}

.pager {
  margin-top: var(--space-sm);
  display: flex;
  align-items: center;
  justify-content: center;
  gap: var(--space-md);
}

.ghost-button {
  min-height: 40px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  color: var(--color-text-secondary);
  background: rgba(12, 21, 42, 0.8);
  padding: 0 12px;
}

@media (max-width: 900px) {
  .overview-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 640px) {
  .overview-grid {
    grid-template-columns: 1fr;
  }
}
</style>
