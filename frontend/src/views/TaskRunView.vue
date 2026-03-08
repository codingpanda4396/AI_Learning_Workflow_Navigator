<script setup lang="ts">
import { onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useSessionStore } from '@/stores/session'

const route = useRoute()
const router = useRouter()
const sessionStore = useSessionStore()

const taskId = computed(() => Number(route.params.id))

onMounted(async () => {
  await sessionStore.runTask(taskId.value)
})

const task = computed(() => sessionStore.currentTask)
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

function handleContinue() {
  router.push(`/session/${sessionStore.sessionId}`)
}
</script>

<template>
  <div class="task-run-page">
    <header class="header">
      <button class="back-btn" @click="router.back()">← 返回</button>
      <h1 class="task-title">任务详情</h1>
    </header>

    <div v-if="isLoading" class="loading">加载中...</div>

    <div v-else-if="task" class="task-content">
      <!-- 任务信息 -->
      <div class="task-meta">
        <span class="task-stage">{{ getStageLabel(task.stage) }}</span>
        <span class="task-id">任务 #{{ task.task_id }}</span>
      </div>

      <!-- 输出内容 -->
      <div class="output-sections">
        <div
          v-for="section in task.output.sections"
          :key="section.title"
          class="output-section"
        >
          <h3 class="section-title">{{ section.title }}</h3>

          <!-- concepts / misconceptions -->
          <ul v-if="section.bullets" class="bullet-list">
            <li v-for="(bullet, idx) in section.bullets" :key="idx">
              {{ bullet }}
            </li>
          </ul>

          <!-- mechanism -->
          <ol v-else-if="section.steps" class="step-list">
            <li v-for="(step, idx) in section.steps" :key="idx">
              {{ step }}
            </li>
          </ol>

          <!-- items for misconceptions -->
          <ul v-else-if="section.items" class="bullet-list">
            <li v-for="(item, idx) in section.items" :key="idx">
              {{ item }}
            </li>
          </ul>

          <!-- summary text -->
          <p v-else-if="section.text" class="summary-text">
            {{ section.text }}
          </p>
        </div>
      </div>

      <!-- 继续按钮 -->
      <div class="actions">
        <button class="continue-btn" @click="handleContinue">
          继续 →
        </button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.task-run-page {
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
  max-width: 720px;
  margin: 0 auto;
}

.task-meta {
  display: flex;
  align-items: center;
  gap: 1rem;
  margin-bottom: 2rem;
}

.task-stage {
  padding: 0.25rem 0.75rem;
  font-size: 0.75rem;
  font-weight: 600;
  text-transform: uppercase;
  color: var(--color-primary);
  background: var(--color-primary-alpha);
  border-radius: 4px;
}

.task-id {
  font-size: 0.875rem;
  color: var(--color-text-secondary);
}

.output-sections {
  display: flex;
  flex-direction: column;
  gap: 2rem;
  margin-bottom: 2rem;
}

.output-section {
  background: var(--color-bg-elevated);
  border-radius: 12px;
  padding: 1.5rem;
}

.section-title {
  font-size: 1rem;
  font-weight: 600;
  color: var(--color-text);
  margin-bottom: 1rem;
}

.bullet-list,
.step-list {
  padding-left: 1.25rem;
  margin: 0;
}

.bullet-list li,
.step-list li {
  font-size: 0.9375rem;
  line-height: 1.7;
  color: var(--color-text);
  margin-bottom: 0.5rem;
}

.step-list li {
  list-style-type: decimal;
}

.summary-text {
  font-size: 0.9375rem;
  line-height: 1.7;
  color: var(--color-text);
  padding: 1rem;
  background: var(--color-bg);
  border-radius: 8px;
  border-left: 3px solid var(--color-primary);
}

.actions {
  text-align: center;
}

.continue-btn {
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

.continue-btn:hover {
  background: var(--color-primary-hover);
  transform: translateY(-2px);
  box-shadow: 0 4px 12px var(--color-primary-alpha);
}
</style>
