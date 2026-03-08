<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useSessionStore } from '@/stores/session'
import ErrorMessage from '@/components/ErrorMessage.vue'

const route = useRoute()
const router = useRouter()
const sessionStore = useSessionStore()

const taskId = computed(() => Number(route.params.id))
const userAnswer = ref('')

const task = computed(() => sessionStore.currentTask)
const result = computed(() => sessionStore.taskResult)
const isLoading = computed(() => sessionStore.runningTask)
const isSubmitting = computed(() => sessionStore.submittingTask)
const loadError = computed(() => sessionStore.error)
const objectiveTitle = computed(() => task.value?.output.sections[0]?.title || '请回答以下训练问题')
const normalizedScorePercent = computed(() => (result.value ? Math.round(result.value.normalizedScore * 100) : 0))

function getStageLabel(stage: string) {
  const map: Record<string, string> = {
    STRUCTURE: '结构构建',
    UNDERSTANDING: '理解深化',
    TRAINING: '训练实践',
    REFLECTION: '反思总结',
  }
  return map[stage] || stage
}

function getErrorTagLabel(tag: string) {
  const map: Record<string, string> = {
    CONCEPT_CONFUSION: '概念混淆',
    MISSING_STEPS: '步骤缺失',
    BOUNDARY_CASE: '边界问题',
    TERMINOLOGY: '术语不准确',
    SHALLOW_REASONING: '推理深度不足',
    MEMORY_GAP: '记忆缺口',
  }
  return map[tag] || tag
}

function getNextActionLabel(action: string) {
  const map: Record<string, string> = {
    INSERT_REMEDIAL_UNDERSTANDING: '补充理解',
    INSERT_TRAINING_VARIANTS: '训练变式',
    INSERT_TRAINING_REINFORCEMENT: '强化训练',
    ADVANCE_TO_NEXT_NODE: '进入下一节点',
    NOOP: '完成当前节点',
  }
  return map[action] || action
}

function resolveSessionId() {
  const fromQuery = Number(route.query.sessionId)
  if (Number.isFinite(fromQuery) && fromQuery > 0) {
    return fromQuery
  }
  return sessionStore.currentTaskSessionId || sessionStore.sessionId || null
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

function handleContinue() {
  const targetSessionId = resolveSessionId()
  if (!targetSessionId) {
    router.push('/')
    return
  }
  router.push(`/session/${targetSessionId}`)
}

function handleGoNextTask() {
  const nextTask = result.value?.nextTask
  if (!nextTask) return
  const targetPath = nextTask.stage === 'TRAINING' ? `/task/${nextTask.taskId}/submit` : `/task/${nextTask.taskId}/run`
  const currentSessionId = resolveSessionId()
  router.push({
    path: targetPath,
    ...(currentSessionId ? { query: { sessionId: String(currentSessionId) } } : {}),
  })
}

onMounted(async () => {
  sessionStore.resetTaskState()
  await loadTask()
})
</script>

<template>
  <div class="task-submit-page">
    <header class="header">
      <button class="back-btn" @click="router.back()">← 返回</button>
      <h1 class="task-title">训练任务</h1>
    </header>

    <div v-if="isLoading && !task" class="loading">加载中...</div>
    <ErrorMessage v-else-if="loadError && !task" :message="loadError" @retry="handleRetry" />

    <div v-else-if="task" class="task-content">
      <div class="task-meta">
        <span class="task-stage">{{ getStageLabel(task.stage) }}</span>
        <span class="task-id">任务 #{{ task.taskId }}</span>
      </div>

      <div v-if="result" class="result-section">
        <div class="score-display">
          <span class="score-label">得分</span>
          <span class="score-value">{{ result.score }}</span>
          <span class="score-subtitle">标准化分：{{ normalizedScorePercent }}%</span>
        </div>

        <div v-if="result.errorTags.length > 0" class="error-tags">
          <span v-for="tag in result.errorTags" :key="tag" class="error-tag">{{ getErrorTagLabel(tag) }}</span>
        </div>

        <div class="feedback">
          <h3 class="feedback-title">诊断</h3>
          <p class="diagnosis">{{ result.feedback.diagnosis }}</p>

          <h3 class="feedback-title">改进建议</h3>
          <ul class="fixes-list">
            <li v-for="(fix, idx) in result.feedback.fixes" :key="idx">{{ fix }}</li>
          </ul>

          <template v-if="result.strengths.length > 0">
            <h3 class="feedback-title">优势</h3>
            <ul class="fixes-list">
              <li v-for="(item, idx) in result.strengths" :key="`s-${idx}`">{{ item }}</li>
            </ul>
          </template>

          <template v-if="result.weaknesses.length > 0">
            <h3 class="feedback-title">薄弱点</h3>
            <ul class="fixes-list">
              <li v-for="(item, idx) in result.weaknesses" :key="`w-${idx}`">{{ item }}</li>
            </ul>
          </template>
        </div>

        <div class="mastery-change">
          <span class="mastery-label">掌握度</span>
          <span class="mastery-before">{{ Math.round(result.masteryBefore * 100) }}%</span>
          <span class="mastery-arrow">→</span>
          <span class="mastery-after">{{ Math.round(result.masteryAfter * 100) }}%</span>
          <span class="mastery-delta">({{ result.masteryDelta > 0 ? '+' : '' }}{{ Math.round(result.masteryDelta * 100) }}%)</span>
        </div>

        <div class="next-action">
          <span class="action-label">下一步：</span>
          <span class="action-value">{{ getNextActionLabel(result.nextAction) }}</span>
        </div>

        <div class="actions">
          <button v-if="result.nextTask" class="continue-btn secondary" @click="handleGoNextTask">开始下一任务</button>
          <button class="continue-btn" @click="handleContinue">返回会话</button>
        </div>
      </div>

      <div v-else class="submit-section">
        <div class="task-objective">
          <h3 class="objective-title">任务目标</h3>
          <p class="objective-text">{{ objectiveTitle }}</p>
        </div>

        <form class="submit-form" @submit.prevent="handleSubmit">
          <textarea
            v-model="userAnswer"
            class="answer-input"
            placeholder="请输入你的答案..."
            rows="8"
            :disabled="isSubmitting"
          ></textarea>

          <button type="submit" class="submit-btn" :disabled="isSubmitting || !userAnswer.trim()">
            {{ isSubmitting ? '提交中...' : '提交答案' }}
          </button>
        </form>

        <p v-if="loadError" class="submit-error">{{ loadError }}</p>
      </div>
    </div>
  </div>
</template>

<style scoped>
.task-submit-page { min-height: 100vh; padding: 1.5rem; }
.header { display: flex; align-items: center; gap: 1rem; margin-bottom: 2rem; }
.back-btn { padding: 0.5rem 1rem; border: 1px solid var(--color-border); border-radius: 6px; background: transparent; color: var(--color-text-secondary); }
.task-title { font-size: 1.5rem; font-weight: 600; color: var(--color-text); }
.loading { text-align: center; padding: 3rem; color: var(--color-text-secondary); }
.task-content { max-width: 720px; margin: 0 auto; }
.task-meta { display: flex; align-items: center; gap: 1rem; margin-bottom: 2rem; }
.task-stage { padding: 0.25rem 0.75rem; font-size: 0.75rem; font-weight: 600; color: var(--color-primary); background: var(--color-primary-alpha); border-radius: 4px; }
.task-id { font-size: 0.875rem; color: var(--color-text-secondary); }
.task-objective { background: var(--color-bg-elevated); border-radius: 12px; padding: 1.5rem; margin-bottom: 1.5rem; }
.objective-title { font-size: 0.875rem; font-weight: 600; color: var(--color-text-secondary); margin-bottom: 0.5rem; }
.objective-text { font-size: 1rem; color: var(--color-text); }
.submit-form { display: flex; flex-direction: column; gap: 1rem; }
.answer-input { width: 100%; padding: 1rem; border: 1px solid var(--color-border); border-radius: 8px; background: var(--color-bg-elevated); color: var(--color-text); resize: vertical; }
.submit-btn { padding: 1rem; font-weight: 600; color: #fff; background: var(--color-primary); border: none; border-radius: 8px; }
.submit-btn:disabled { opacity: 0.6; cursor: not-allowed; }
.submit-error { margin-top: 12px; color: var(--color-error); font-size: 0.875rem; }
.result-section { display: flex; flex-direction: column; gap: 1.5rem; }
.score-display { display: flex; flex-direction: column; align-items: center; padding: 2rem; background: var(--color-bg-elevated); border-radius: 12px; }
.score-label { font-size: 0.875rem; color: var(--color-text-secondary); }
.score-value { font-size: 4rem; font-weight: 700; color: var(--color-primary); }
.score-subtitle { font-size: 0.875rem; color: var(--color-text-secondary); }
.error-tags { display: flex; flex-wrap: wrap; gap: 0.5rem; }
.error-tag { padding: 0.25rem 0.75rem; font-size: 0.75rem; color: #dc2626; background: #fef2f2; border-radius: 4px; }
.feedback { background: var(--color-bg-elevated); border-radius: 12px; padding: 1.5rem; }
.feedback-title { font-size: 0.875rem; font-weight: 600; color: var(--color-text-secondary); margin-bottom: 0.5rem; }
.diagnosis { font-size: 0.9375rem; line-height: 1.6; color: var(--color-text); margin-bottom: 1rem; }
.fixes-list { padding-left: 1.25rem; margin: 0; }
.fixes-list li { font-size: 0.9375rem; line-height: 1.6; color: var(--color-text); margin-bottom: 0.5rem; }
.mastery-change { display: flex; align-items: center; justify-content: center; gap: 0.75rem; padding: 1rem; background: var(--color-bg-elevated); border-radius: 8px; }
.mastery-label { font-size: 0.875rem; color: var(--color-text-secondary); }
.mastery-before, .mastery-after { font-size: 1rem; font-weight: 600; color: var(--color-text); }
.mastery-arrow { color: var(--color-text-secondary); }
.mastery-delta { font-size: 0.875rem; color: #22c55e; }
.next-action { display: flex; align-items: center; justify-content: center; gap: 0.5rem; }
.action-label { font-size: 0.875rem; color: var(--color-text-secondary); }
.action-value { font-size: 0.875rem; font-weight: 600; color: var(--color-primary); }
.actions { text-align: center; }
.continue-btn { padding: 1rem 2rem; font-weight: 600; color: #fff; background: var(--color-primary); border: none; border-radius: 8px; }
.secondary { margin-right: 12px; background: transparent; border: 1px solid var(--color-border); color: var(--color-text); }
</style>
