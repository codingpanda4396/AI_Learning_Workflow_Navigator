<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useSessionStore } from '@/stores/session'
import ErrorMessage from '@/components/ErrorMessage.vue'
import NextStepPanel from '@/components/NextStepPanel.vue'
import PrimaryButton from '@/components/PrimaryButton.vue'
import {
  formatMasteryDelta,
  formatPercent,
  getFeedbackSummary,
  getNextActionLabel,
  getNextActionReason,
  getPerformanceLabel,
  getStageShortLabel,
} from '@/utils/learningNarrative'

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

const primaryActionLabel = computed(() => (result.value?.nextTask ? '继续下一步' : '回到本轮学习'))
const evaluationStateLabel = computed(() => {
  if (isSubmitting.value) return '结果生成中'
  if (result.value) return '推荐动作已生成'
  return '等待提交'
})

const reasoningItems = computed(() => {
  if (!result.value) return []
  return [
    `本轮表现：${getPerformanceLabel(result.value)}（${formatPercent(result.value.normalizedScore)}）`,
    `系统推荐：${getNextActionLabel(result.value.nextAction)}`,
    `推荐原因：${getNextActionReason(result.value.nextAction)}`,
  ]
})

const growthItems = computed(() => {
  if (!result.value) return []
  return [
    `掌握度变化：${formatMasteryDelta(result.value.masteryDelta)}`,
    `当前掌握度：${formatPercent(result.value.masteryAfter)}`,
    '本轮结果会继续计入你的学习记录，供后续路径推荐使用。',
  ]
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
    const currentSessionId = resolveSessionId()
    if (currentSessionId) {
      try {
        await sessionStore.fetchLearningFeedback(currentSessionId)
      } catch {
        // Optional for this page.
      }
    }
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
      <div>
        <h1 class="task-title">结果与下一步</h1>
        <p class="header-copy">提交后，系统会说明你学得怎么样，以及接下来建议怎么走。</p>
      </div>
    </header>

    <div v-if="isLoading && !task" class="loading">加载中...</div>
    <ErrorMessage v-else-if="loadError && !task" :message="loadError" @retry="handleRetry" />

    <div v-else-if="task" class="task-content">
      <div class="task-meta">
        <span class="task-stage">{{ getStageShortLabel(task.stage) }}</span>
        <span class="task-id">任务 #{{ task.taskId }}</span>
        <span class="task-state">{{ evaluationStateLabel }}</span>
      </div>

      <div v-if="result" class="result-section">
        <section class="hero-card">
          <p class="eyebrow">结果总览</p>
          <h2>{{ getPerformanceLabel(result) }}</h2>
          <p class="hero-copy">{{ result.feedback.diagnosis }}</p>
        </section>

        <section class="result-grid">
          <section class="result-card">
            <h3>你已经掌握了什么</h3>
            <ul v-if="summary.strengths.length" class="result-list">
              <li v-for="item in summary.strengths" :key="item">{{ item }}</li>
            </ul>
            <p v-else class="muted-copy">这一轮已经覆盖了基础理解，可以继续看哪些点还需要补强。</p>
          </section>

          <section class="result-card">
            <h3>你还不稳的是什么</h3>
            <ul v-if="summary.weaknesses.length" class="result-list">
              <li v-for="item in summary.weaknesses" :key="item">{{ item }}</li>
            </ul>
            <p v-else class="muted-copy">当前没有明显薄弱点，可以继续推进下一步。</p>
          </section>
        </section>

        <section class="result-card">
          <h3>系统怎么判断的</h3>
          <ul class="result-list">
            <li v-for="item in reasoningItems" :key="item">{{ item }}</li>
          </ul>
          <ul v-if="result.feedback.fixes.length" class="result-list secondary-list">
            <li v-for="item in result.feedback.fixes" :key="item">{{ item }}</li>
          </ul>
        </section>

        <NextStepPanel
          title="建议下一步做什么"
          :description="summary.nextStep || getNextActionLabel(result.nextAction)"
        />

        <section class="result-card">
          <h3>成长沉淀</h3>
          <ul class="result-list">
            <li v-for="item in growthItems" :key="item">{{ item }}</li>
          </ul>
        </section>

        <section class="action-row">
          <button type="button" class="ghost-btn" @click="handleBackToSession">
            回到本轮学习
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
          <p class="hero-copy">这不是正式考试。提交后系统会给出掌握情况、薄弱点和下一步建议。</p>
        </section>

        <section class="timeline-card">
          <div class="timeline-item current">
            <strong>1. 提交当前回答</strong>
            <p>先独立完成这一步，系统才有依据判断你的掌握情况。</p>
          </div>
          <div class="timeline-item" :class="{ current: isSubmitting }">
            <strong>2. 生成结果</strong>
            <p>系统会整理表现、问题和推荐动作。</p>
          </div>
          <div class="timeline-item">
            <strong>3. 决定下一步</strong>
            <p>结果页会告诉你该继续、复习还是强化。</p>
          </div>
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
  align-items: flex-start;
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
.header-copy,
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

.header-copy,
.loading,
.task-id,
.task-state,
.hero-copy,
.muted-copy,
.result-list,
.submit-error,
.timeline-item p {
  color: var(--color-text-secondary);
  line-height: 1.7;
}

.loading {
  text-align: center;
  padding: 3rem;
}

.task-content {
  max-width: 920px;
  margin: 0 auto;
  display: grid;
  gap: 18px;
}

.task-meta,
.action-row,
.result-grid {
  display: flex;
  align-items: stretch;
  gap: 1rem;
  flex-wrap: wrap;
}

.task-stage,
.task-state {
  padding: 0.25rem 0.75rem;
  font-size: 0.75rem;
  font-weight: 600;
  border-radius: 999px;
}

.task-stage {
  color: var(--color-primary);
  background: var(--color-primary-alpha);
}

.task-state {
  color: var(--color-text);
  background: rgba(107, 159, 255, 0.16);
}

.result-section,
.submit-section {
  display: grid;
  gap: 18px;
}

.hero-card,
.result-card,
.timeline-card {
  display: grid;
  gap: 12px;
  background: var(--color-bg-elevated);
  border-radius: 16px;
  padding: 1.5rem;
  border: 1px solid var(--color-border);
}

.result-grid > .result-card {
  flex: 1 1 280px;
}

.eyebrow {
  margin: 0;
  color: var(--color-text-muted);
  font-size: var(--font-size-xs);
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.result-list {
  margin: 0;
  padding-left: 1.25rem;
}

.secondary-list {
  border-top: 1px solid rgba(61, 80, 104, 0.3);
  padding-top: 12px;
}

.timeline-card {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.timeline-item {
  padding: 1rem;
  border-radius: 12px;
  background: rgba(10, 16, 26, 0.72);
  border: 1px solid rgba(61, 80, 104, 0.34);
}

.timeline-item.current {
  border-color: rgba(107, 159, 255, 0.48);
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

@media (max-width: 760px) {
  .timeline-card {
    grid-template-columns: 1fr;
  }
}
</style>
