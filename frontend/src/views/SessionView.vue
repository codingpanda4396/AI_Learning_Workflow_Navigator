<script setup lang="ts">
import { onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useSessionStore } from '@/stores/session'

const route = useRoute()
const router = useRouter()
const sessionStore = useSessionStore()

const sessionId = computed(() => Number(route.params.id))

onMounted(async () => {
  await sessionStore.fetchSessionOverview(sessionId.value)
})

const currentSession = computed(() => sessionStore.currentSession)
const timeline = computed(() => sessionStore.timeline)
const masterySummary = computed(() => sessionStore.masterySummary)
const nextTask = computed(() => sessionStore.nextTask)
const isLoading = computed(() => sessionStore.isLoading)

function getStageLabel(stage: string) {
  const map: Record<string, string> = {
    STRUCTURE: '结构',
    UNDERSTANDING: '理解',
    TRAINING: '训练',
    REFLECTION: '反思',
  }
  return map[stage] || stage
}

function getStatusClass(status: string) {
  const map: Record<string, string> = {
    PENDING: 'status-pending',
    RUNNING: 'status-running',
    SUCCEEDED: 'status-succeeded',
    FAILED: 'status-failed',
  }
  return map[status] || ''
}

function handleTaskClick(taskId: number, stage: string) {
  if (stage === 'TRAINING') {
    router.push(`/task/${taskId}/submit`)
  } else {
    router.push(`/task/${taskId}/run`)
  }
}

function handleStartNext() {
  if (nextTask.value) {
    const taskId = nextTask.value.task_id
    const stage = nextTask.value.stage
    if (stage === 'TRAINING') {
      router.push(`/task/${taskId}/submit`)
    } else {
      router.push(`/task/${taskId}/run`)
    }
  }
}
</script>

<template>
  <div class="session-page">
    <header class="header">
      <button class="back-btn" @click="router.push('/')">← 返回</button>
      <h1 class="session-title">学习会话</h1>
    </header>

    <div v-if="isLoading" class="loading">加载中...</div>

    <div v-else-if="currentSession" class="session-content">
      <!-- 目标 -->
      <section class="goal-section">
        <h2 class="section-title">学习目标</h2>
        <p class="goal-text">{{ currentSession.goal_text }}</p>
      </section>

      <!-- 掌握度 -->
      <section class="mastery-section">
        <h2 class="section-title">掌握度</h2>
        <div class="mastery-list">
          <div v-for="item in masterySummary" :key="item.node_id" class="mastery-item">
            <span class="node-name">{{ item.node_name }}</span>
            <div class="mastery-bar">
              <div
                class="mastery-fill"
                :style="{ width: `${item.mastery_value * 100}%` }"
              ></div>
            </div>
            <span class="mastery-value">{{ Math.round(item.mastery_value * 100) }}%</span>
          </div>
        </div>
      </section>

      <!-- 任务时间线 -->
      <section class="timeline-section">
        <h2 class="section-title">任务进度</h2>
        <div class="timeline">
          <div
            v-for="item in timeline"
            :key="item.task_id"
            class="timeline-item"
            :class="getStatusClass(item.status)"
            @click="handleTaskClick(item.task_id, item.stage)"
          >
            <div class="task-status">
              <span v-if="item.status === 'SUCCEEDED'" class="icon-success">✓</span>
              <span v-else-if="item.status === 'FAILED'" class="icon-failed">✗</span>
              <span v-else class="icon-pending">○</span>
            </div>
            <div class="task-info">
              <span class="task-stage">{{ getStageLabel(item.stage) }}</span>
              <span class="task-id">#{{ item.task_id }}</span>
            </div>
          </div>
        </div>
      </section>

      <!-- 下一任务 -->
      <section v-if="nextTask" class="next-action">
        <button class="next-btn" @click="handleStartNext">
          开始下一任务 →
        </button>
      </section>
    </div>
  </div>
</template>

<style scoped>
.session-page {
  min-height: 100vh;
  padding: 1.5rem;
}

.header {
  display: flex;
  align-items: center;
  gap: 1rem;
  margin-bottom: 2rem;
}

.back-btn {
  padding: 0.5rem 1rem;
  font-size: 0.875rem;
  color: var(--color-text-secondary);
  background: transparent;
  border: 1px solid var(--color-border);
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.2s;
}

.back-btn:hover {
  border-color: var(--color-primary);
  color: var(--color-primary);
}

.session-title {
  font-size: 1.5rem;
  font-weight: 600;
  color: var(--color-text);
}

.loading {
  text-align: center;
  padding: 3rem;
  color: var(--color-text-secondary);
}

.session-content {
  max-width: 640px;
  margin: 0 auto;
}

.section-title {
  font-size: 0.875rem;
  font-weight: 600;
  color: var(--color-text-secondary);
  text-transform: uppercase;
  letter-spacing: 0.05em;
  margin-bottom: 1rem;
}

.goal-section {
  margin-bottom: 2rem;
}

.goal-text {
  font-size: 1.125rem;
  color: var(--color-text);
  line-height: 1.6;
}

.mastery-section {
  margin-bottom: 2rem;
}

.mastery-list {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.mastery-item {
  display: flex;
  align-items: center;
  gap: 1rem;
}

.node-name {
  width: 80px;
  font-size: 0.875rem;
  color: var(--color-text);
}

.mastery-bar {
  flex: 1;
  height: 8px;
  background: var(--color-border);
  border-radius: 4px;
  overflow: hidden;
}

.mastery-fill {
  height: 100%;
  background: var(--color-primary);
  border-radius: 4px;
  transition: width 0.3s ease;
}

.mastery-value {
  width: 40px;
  font-size: 0.875rem;
  color: var(--color-text-secondary);
  text-align: right;
}

.timeline-section {
  margin-bottom: 2rem;
}

.timeline {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.timeline-item {
  display: flex;
  align-items: center;
  gap: 1rem;
  padding: 1rem;
  background: var(--color-bg-elevated);
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
}

.timeline-item:hover {
  transform: translateX(4px);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}

.task-status {
  width: 24px;
  height: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 0.875rem;
}

.status-succeeded .icon-success {
  color: #22c55e;
}

.status-failed .icon-failed {
  color: #ef4444;
}

.status-pending .icon-pending,
.status-running .icon-pending {
  color: var(--color-text-secondary);
}

.task-info {
  display: flex;
  gap: 0.75rem;
}

.task-stage {
  font-weight: 500;
  color: var(--color-text);
}

.task-id {
  color: var(--color-text-secondary);
  font-size: 0.875rem;
}

.next-action {
  text-align: center;
}

.next-btn {
  padding: 1rem 2rem;
  font-size: 1rem;
  font-weight: 600;
  color: white;
  background: var(--color-primary);
  border: none;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
}

.next-btn:hover {
  background: var(--color-primary-hover);
  transform: translateY(-2px);
  box-shadow: 0 4px 12px var(--color-primary-alpha);
}
</style>
