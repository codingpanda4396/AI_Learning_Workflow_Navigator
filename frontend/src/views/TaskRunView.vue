<script setup lang="ts">
import { computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useSessionStore } from '@/stores/session'
import ErrorMessage from '@/components/ErrorMessage.vue'

const route = useRoute()
const router = useRouter()
const sessionStore = useSessionStore()

const taskId = computed(() => Number(route.params.id))
const loadError = computed(() => sessionStore.error)
const task = computed(() => sessionStore.currentTask)
const isLoading = computed(() => sessionStore.runningTask)
const sections = computed(() => task.value?.output.sections ?? [])
const generationReason = computed(() => task.value?.generationReason?.trim() ?? '')

function getStageLabel(stage: string) {
  const map: Record<string, string> = {
    STRUCTURE: '结构构建',
    UNDERSTANDING: '理解深化',
    TRAINING: '训练实践',
    REFLECTION: '反思总结',
  }
  return map[stage] || stage
}

function getGenerationLabel(mode?: string) {
  const map: Record<string, string> = {
    LLM: 'LLM 生成',
    TEMPLATE_FALLBACK: '模板降级生成',
    RULE_FALLBACK: '规则降级生成',
    CACHED: '历史缓存结果',
  }
  return mode ? map[mode] || mode : '未知来源'
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

onMounted(async () => {
  sessionStore.resetTaskState()
  await loadTask()
})
</script>

<template>
  <div class="task-run-page">
    <header class="header">
      <button class="back-btn" @click="router.back()">← 返回</button>
      <h1 class="task-title">任务详情</h1>
    </header>

    <div v-if="isLoading" class="loading">加载中...</div>
    <ErrorMessage v-else-if="loadError && !task" :message="loadError" @retry="handleRetry" />

    <div v-else-if="task" class="task-content">
      <div class="task-meta">
        <span class="task-stage">{{ getStageLabel(task.stage) }}</span>
        <span class="task-id">任务 #{{ task.taskId }}</span>
        <span class="task-source">来源：{{ getGenerationLabel(task.generationMode) }}</span>
      </div>
      <p v-if="generationReason" class="generation-reason">原因：{{ generationReason }}</p>

      <div v-if="sections.length > 0" class="output-sections">
        <div v-for="section in sections" :key="section.title" class="output-section">
          <h3 class="section-title">{{ section.title }}</h3>

          <ul v-if="section.bullets?.length" class="bullet-list">
            <li v-for="(bullet, idx) in section.bullets" :key="idx">{{ bullet }}</li>
          </ul>

          <ol v-else-if="section.steps?.length" class="step-list">
            <li v-for="(step, idx) in section.steps" :key="idx">{{ step }}</li>
          </ol>

          <ul v-else-if="section.items?.length" class="bullet-list">
            <li v-for="(item, idx) in section.items" :key="idx">{{ item }}</li>
          </ul>

          <p v-else-if="section.text" class="summary-text">{{ section.text }}</p>
          <p v-else class="summary-text">暂无可展示内容。</p>
        </div>
      </div>

      <div v-else class="summary-text">当前任务输出为空，请返回会话页重试。</div>

      <div class="actions">
        <button class="continue-btn" @click="handleContinue">返回会话</button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.task-run-page { min-height: 100vh; padding: 1.5rem; }
.header { display: flex; align-items: center; gap: 1rem; margin-bottom: 2rem; }
.back-btn { padding: 0.5rem 1rem; border: 1px solid var(--color-border); border-radius: 6px; background: transparent; color: var(--color-text-secondary); }
.task-title { font-size: 1.5rem; font-weight: 600; color: var(--color-text); }
.loading { text-align: center; padding: 3rem; color: var(--color-text-secondary); }
.task-content { max-width: 720px; margin: 0 auto; }
.task-meta { display: flex; align-items: center; gap: 1rem; margin-bottom: 2rem; flex-wrap: wrap; }
.task-stage { padding: 0.25rem 0.75rem; font-size: 0.75rem; font-weight: 600; color: var(--color-primary); background: var(--color-primary-alpha); border-radius: 4px; }
.task-id { font-size: 0.875rem; color: var(--color-text-secondary); }
.task-source { font-size: 0.875rem; color: var(--color-text-secondary); }
.generation-reason { margin: -1rem 0 1.5rem; color: var(--color-text-secondary); font-size: 0.875rem; }
.output-sections { display: flex; flex-direction: column; gap: 2rem; margin-bottom: 2rem; }
.output-section { background: var(--color-bg-elevated); border-radius: 12px; padding: 1.5rem; }
.section-title { font-size: 1rem; font-weight: 600; color: var(--color-text); margin-bottom: 1rem; }
.bullet-list, .step-list { padding-left: 1.25rem; margin: 0; }
.bullet-list li, .step-list li { line-height: 1.7; color: var(--color-text); margin-bottom: 0.5rem; }
.summary-text { line-height: 1.7; color: var(--color-text); padding: 1rem; background: var(--color-bg); border-radius: 8px; border-left: 3px solid var(--color-primary); }
.actions { text-align: center; }
.continue-btn { padding: 1rem 2rem; font-weight: 600; color: #fff; background: var(--color-primary); border: none; border-radius: 8px; }
</style>
