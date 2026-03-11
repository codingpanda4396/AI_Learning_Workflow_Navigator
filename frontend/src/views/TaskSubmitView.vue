<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useSessionStore } from '@/stores/session'
import ErrorMessage from '@/components/ErrorMessage.vue'
import NextStepPanel from '@/components/NextStepPanel.vue'
import PrimaryButton from '@/components/PrimaryButton.vue'
import { getFeedbackSummary, getNextActionLabel, getStageShortLabel } from '@/utils/learningNarrative'

const route = useRoute()
const router = useRouter()
const sessionStore = useSessionStore()

const taskId = computed(() => Number(route.params.id))
const userAnswer = ref('')

const task = computed(() => sessionStore.currentTask)
const result = computed(() => sessionStore.taskResult)
const summary = computed(() => getFeedbackSummary(null, result.value))
const isLoading = computed(() => sessionStore.runningTask)
const isSubmitting = computed(() => sessionStore.submittingTask)
const loadError = computed(() => sessionStore.error)
const objectiveTitle = computed(() => task.value?.output.sections[0]?.title || '请先完成这一道检测')

const primaryActionLabel = computed(() => {
  if (result.value?.nextTask) return '继续下一步'
  return '返回本轮学习'
})

function resolveSessionId() {
  const fromQuery = Number(route.query.sessionId)
  if (Number.isFinite(fromQuery) && fromQuery > 0) return fromQuery
  return sessionStore.currentTaskSessionId || sessionStore.sessionId || null
}

function isValidPositiveId(value: number | null | undefined) {
  return Number.isFinite(value) && (value ?? 0) > 0
}

async function loadTask() {
  await sessionStore.runTask(taskId.value)
}

async function handleSubmit() {
  if (!userAnswer.value.trim() || isSubmitting.value) return
  try {
    await sessionStore.submitTask(taskId.value, userAnswer.value.trim())
  } catch (error) {
    console.error('提交失败:', error)
  }
}

async function handleRetry() {
  await loadTask()
}

function handleBackToSession() {
  const targetSessionId = resolveSessionId()
  if (!isValidPositiveId(targetSessionId)) {
    router.push({ name: 'home' })
    return
  }
  router.push({
    name: 'session',
    params: { id: String(targetSessionId) },
  })
}

function handleContinue() {
  const targetSessionId = resolveSessionId()
  if (!isValidPositiveId(targetSessionId)) {
    router.push({ name: 'home' })
    return
  }

  const step = Number(route.query.step)
  const resolvedStep = Number.isFinite(step) && step >= 1 && step <= 4 ? String(step) : '3'

  if (result.value?.nextTask && isValidPositiveId(result.value.nextTask.taskId)) {
    router.push({
      name: 'task-run',
      params: { id: String(result.value.nextTask.taskId) },
      query: { sessionId: String(targetSessionId), step: resolvedStep },
    })
    return
  }

  router.push({
    name: 'session',
    params: { id: String(targetSessionId) },
    query: { step: resolvedStep },
  })
}

onMounted(async () => {
  sessionStore.resetTaskState()
  await loadTask()
  if (sessionStore.currentTask?.stage === 'TRAINING') {
    const currentSessionId = resolveSessionId()
    const step = Number(route.query.step)
    const resolvedStep = Number.isFinite(step) && step >= 1 && step <= 4 ? String(step) : '3'
    await router.replace({
      name: 'task-run',
      params: { id: String(taskId.value) },
      ...(currentSessionId ? { query: { sessionId: String(currentSessionId), step: resolvedStep } } : { query: { step: resolvedStep } }),
    })
  }
})
</script>

<template>
  <div class="task-submit-page">
    <header class="header">
      <button class="back-btn" @click="router.back()">返回</button>
      <h1 class="task-title">检测与结果</h1>
    </header>

    <div v-if="isLoading && !task" class="loading">加载中...</div>
    <ErrorMessage v-else-if="loadError && !task" :message="loadError" @retry="handleRetry" />

    <div v-else-if="task" class="task-content">
      <div class="task-meta">
        <span class="task-stage">{{ getStageShortLabel(task.stage) }}</span>
        <span class="task-id">任务 #{{ task.taskId }}</span>
      </div>

      <div v-if="result" class="result-section">
        <section class="hero-card">
          <p class="eyebrow">检测结果</p>
          <h2>这次检测帮你看清了当前掌握情况</h2>
          <p class="hero-copy">{{ result.feedback.diagnosis }}</p>
        </section>

        <section class="result-card">
          <h3>你已经掌握了什么</h3>
          <ul v-if="summary.strengths.length" class="result-list">
            <li v-for="item in summary.strengths" :key="item">{{ item }}</li>
          </ul>
          <p v-else class="muted-copy">基础理解已经覆盖，可以继续看还不稳的地方。</p>
        </section>

        <section class="result-card">
          <h3>你还不稳的是什么</h3>
          <ul v-if="summary.weaknesses.length" class="result-list">
            <li v-for="item in summary.weaknesses" :key="item">{{ item }}</li>
          </ul>
          <p v-else class="muted-copy">当前没有明显薄弱点，可以继续下一步。</p>
        </section>

        <NextStepPanel
          title="建议下一步做什么"
          :description="summary.nextStep || getNextActionLabel(result.nextAction)"
        />

        <section class="action-row">
          <button type="button" class="ghost-btn" @click="handleBackToSession">
            返回本轮学习
          </button>
          <PrimaryButton type="button" @click="handleContinue">
            {{ primaryActionLabel }}
          </PrimaryButton>
        </section>
      </div>

      <div v-else class="submit-section">
        <section class="hero-card">
          <p class="eyebrow">开始检测</p>
          <h2>{{ objectiveTitle }}</h2>
          <p class="hero-copy">这是检测，不是正式考试。提交后系统会告诉你已经掌握了什么，以及建议下一步做什么。</p>
        </section>

        <form class="submit-form" @submit.prevent="handleSubmit">
          <textarea
            v-model="userAnswer"
            class="answer-input"
            placeholder="先独立作答，再写下你的思路或答案"
            rows="8"
            :disabled="isSubmitting"
          ></textarea>

          <PrimaryButton type="submit" :loading="isSubmitting" :disabled="!userAnswer.trim()">
            提交检测
          </PrimaryButton>
        </form>

        <p v-if="loadError" class="submit-error">{{ loadError }}</p>
      </div>
    </div>
  </div>
</template>

<style scoped>
.task-submit-page {
  min-height: 100vh;
  padding: 1.5rem;
}

.header {
  display: flex;
  align-items: center;
  gap: 1rem;
  margin-bottom: 2rem;
}

.back-btn,
.ghost-btn {
  padding: 0.75rem 1rem;
  border: 1px solid var(--color-border);
  border-radius: 8px;
  background: transparent;
  color: var(--color-text-secondary);
}

.task-title,
.hero-card h2,
.hero-copy,
.result-card h3,
.muted-copy {
  margin: 0;
}

.task-title {
  font-size: 1.5rem;
  font-weight: 600;
  color: var(--color-text);
}

.loading {
  text-align: center;
  padding: 3rem;
  color: var(--color-text-secondary);
}

.task-content {
  max-width: 820px;
  margin: 0 auto;
  display: grid;
  gap: 18px;
}

.task-meta,
.action-row {
  display: flex;
  align-items: center;
  gap: 1rem;
  flex-wrap: wrap;
}

.task-stage {
  padding: 0.25rem 0.75rem;
  font-size: 0.75rem;
  font-weight: 600;
  color: var(--color-primary);
  background: var(--color-primary-alpha);
  border-radius: 999px;
}

.task-id {
  font-size: 0.875rem;
  color: var(--color-text-secondary);
}

.result-section,
.submit-section {
  display: grid;
  gap: 18px;
}

.hero-card,
.result-card {
  display: grid;
  gap: 12px;
  background: var(--color-bg-elevated);
  border-radius: 16px;
  padding: 1.5rem;
  border: 1px solid var(--color-border);
}

.eyebrow {
  margin: 0;
  color: var(--color-text-muted);
  font-size: var(--font-size-xs);
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.hero-copy,
.muted-copy,
.result-list,
.submit-error {
  color: var(--color-text-secondary);
  line-height: 1.7;
}

.result-list {
  margin: 0;
  padding-left: 1.25rem;
}

.submit-form {
  display: grid;
  gap: 1rem;
}

.answer-input {
  width: 100%;
  padding: 1rem;
  border: 1px solid var(--color-border);
  border-radius: 8px;
  background: var(--color-bg-elevated);
  color: var(--color-text);
  resize: vertical;
}

.submit-error {
  margin: 0;
  color: var(--color-error);
  font-size: 0.875rem;
}
</style>
