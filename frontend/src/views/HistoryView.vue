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
  if (!item.progress) return '-'
  const percent = Math.round(item.progress.completionRate * 100)
  return `${percent}% (${item.progress.completedTaskCount}/${item.progress.totalTaskCount})`
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
      <h1>历史记录</h1>
      <span class="summary">共 {{ total }} 条</span>
    </header>

    <section class="panel">
      <p v-if="loading" class="tip">加载中...</p>
      <p v-else-if="error" class="error">{{ error }}</p>
      <p v-else-if="items.length === 0" class="tip">暂无历史记录。</p>

      <div v-else class="list">
        <article v-for="item in items" :key="item.sessionId" class="card">
          <div class="card-head">
            <h3>Session #{{ item.sessionId }}</h3>
            <span class="status">{{ item.status }}</span>
          </div>
          <p><strong>课程：</strong>{{ item.course }} / {{ item.chapter }}</p>
          <p><strong>目标：</strong>{{ item.goal }}</p>
          <p><strong>进度：</strong>{{ progressText(item) }}</p>
          <p><strong>最近活跃：</strong>{{ formatTime(item.lastActiveAt) }}</p>
          <button
            class="resume-btn"
            :disabled="resumingId === item.sessionId"
            @click="handleResume(item)"
          >
            {{ resumingId === item.sessionId ? '恢复中...' : '继续学习' }}
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
.history-page { min-height: 100dvh; padding: clamp(16px, 2.8vw, 30px); }
.toolbar { display: flex; align-items: center; gap: var(--space-md); justify-content: space-between; margin-bottom: var(--space-lg); }
.summary { color: var(--color-text-secondary); font-size: var(--font-size-sm); }
.panel { border: 1px solid var(--color-border); border-radius: var(--radius-xl); background: linear-gradient(165deg, rgba(16, 27, 50, 0.94), rgba(8, 14, 26, 0.96)); padding: clamp(16px, 2.8vw, 26px); box-shadow: var(--shadow-md); }
.tip { color: var(--color-text-secondary); }
.error { color: var(--color-error); }
.list { display: grid; gap: var(--space-md); }
.card { border: 1px solid var(--color-border); border-radius: var(--radius-md); background: rgba(12, 20, 38, 0.8); padding: var(--space-md); display: grid; gap: 8px; }
.card-head { display: flex; align-items: center; justify-content: space-between; }
.status { border: 1px solid var(--color-border); border-radius: 999px; padding: 2px 8px; color: var(--color-text-secondary); font-size: var(--font-size-xs); }
.resume-btn { width: 140px; min-height: 40px; border-radius: var(--radius-md); background: var(--color-primary); color: #fff; }
.resume-btn:disabled { opacity: 0.6; }
.pager { margin-top: var(--space-lg); display: flex; align-items: center; justify-content: center; gap: var(--space-md); }
.ghost-button { min-height: 40px; border: 1px solid var(--color-border); border-radius: var(--radius-md); color: var(--color-text-secondary); background: rgba(12, 21, 42, 0.8); padding: 0 12px; }
</style>
