<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useSessionStore } from '@/stores/session'
import type { WorkflowStepNumber } from '@/types/workflow'
import PageHeader from '@/components/PageHeader.vue'
import StepProgress from '@/components/StepProgress.vue'
import PrimaryButton from '@/components/PrimaryButton.vue'
import SessionSkeleton from '@/components/SessionSkeleton.vue'
import ErrorMessage from '@/components/ErrorMessage.vue'

const SKIP_RESUME_ONCE_KEY = 'ai_learning_skip_resume_once'
const defaultUserId = import.meta.env.VITE_DEFAULT_USER_ID || 'guest_user'

const route = useRoute()
const router = useRouter()
const sessionStore = useSessionStore()

type StepMeta = {
  step: WorkflowStepNumber
  title: string
  task: string
}

const sessionId = computed(() => Number(route.params.id))
const browsingStep = ref<WorkflowStepNumber>(1)
const expandedTaskIds = ref<number[]>([])

function resolveStep(raw: unknown): WorkflowStepNumber {
  const value = Number(raw)
  if (Number.isFinite(value) && value >= 1 && value <= 4) {
    return value as WorkflowStepNumber
  }
  return 1
}

const steps: StepMeta[] = [
  { step: 1, title: '目标诊断', task: '查看目标质量评价' },
  { step: 2, title: '路径规划', task: '选择学习路径' },
  { step: 3, title: '分步学习', task: '按步骤执行任务' },
  { step: 4, title: '总结反馈', task: '查看阶段总结与下一步建议' },
]

const currentSession = computed(() => sessionStore.currentSession)
const isLoading = computed(() => sessionStore.fetchingSession || sessionStore.recoveringSession)
const error = computed(() => sessionStore.error)
const diagnosis = computed(() => sessionStore.goalDiagnosis)
const pathOptions = computed(() => sessionStore.pathOptions)
const selectedPathId = computed(() => sessionStore.selectedPathId)

const currentStep = computed(() => browsingStep.value)
const currentStepMeta = computed(() => steps.find((item) => item.step === currentStep.value) ?? steps[0]!)
const selectedPath = computed(() => pathOptions.value.find((item) => item.pathId === selectedPathId.value) ?? null)

function stageLabel(stage: string) {
  const map: Record<string, string> = {
    STRUCTURE: '结构构建',
    UNDERSTANDING: '理解深化',
    TRAINING: '训练实践',
    REFLECTION: '反思总结',
  }
  return map[stage] || stage
}

function stageGuide(stage: string) {
  const map: Record<string, string> = {
    STRUCTURE: '梳理关键概念、边界与关系，形成知识框架。',
    UNDERSTANDING: '解释机制和因果链，识别并纠正常见误区。',
    TRAINING: '完成训练题并提交答案，根据反馈修正。',
    REFLECTION: '复盘错误模式，提炼下一步改进动作。',
  }
  return map[stage] || '按任务提示完成学习。'
}

function resolveTaskPath(taskId: number, stage: string) {
  return stage === 'TRAINING' ? `/task/${taskId}/submit` : `/task/${taskId}/run`
}

function openTask(taskId: number, stage: string) {
  router.push({
    path: resolveTaskPath(taskId, stage),
    query: {
      sessionId: String(sessionId.value),
      step: String(currentStep.value),
    },
  })
}

function toggleTask(taskId: number) {
  if (expandedTaskIds.value.includes(taskId)) {
    expandedTaskIds.value = expandedTaskIds.value.filter((id) => id !== taskId)
    return
  }
  expandedTaskIds.value = [...expandedTaskIds.value, taskId]
}

function isTaskExpanded(taskId: number) {
  return expandedTaskIds.value.includes(taskId)
}

function selectPath(pathId: string) {
  sessionStore.setSelectedPath(pathId)
}

async function refreshModuleData() {
  if (!currentSession.value) return
  const payload = {
    userId: defaultUserId,
    courseId: currentSession.value.courseId,
    chapterId: currentSession.value.chapterId,
    goalText: currentSession.value.goalText,
  }
  await Promise.all([sessionStore.diagnoseGoal(payload), sessionStore.fetchPathOptions(payload)])
}

async function fetchSession() {
  await sessionStore.fetchSessionOverview(sessionId.value)
  await sessionStore.fetchSessionPath(sessionId.value)
  await refreshModuleData()
}

function handleRunNextTask() {
  if (!sessionStore.nextTask) return
  openTask(sessionStore.nextTask.taskId, sessionStore.nextTask.stage)
}

async function handleRetry() {
  await fetchSession()
}

function goToStep(step: WorkflowStepNumber) {
  browsingStep.value = step
}

function handlePrevious() {
  if (currentStep.value > 1) goToStep((currentStep.value - 1) as WorkflowStepNumber)
}

function handleNext() {
  if (currentStep.value < 4) goToStep((currentStep.value + 1) as WorkflowStepNumber)
}

async function goHome() {
  localStorage.setItem(SKIP_RESUME_ONCE_KEY, '1')
  await router.replace({ name: 'home', query: { skipResume: '1' } })
}

onMounted(async () => {
  browsingStep.value = resolveStep(route.query.step)
  await fetchSession()
})
</script>

<template>
  <main class="workflow-page">
    <header class="toolbar">
      <button class="ghost-button" @click="goHome">返回首页</button>
      <span class="workflow-id">Session #{{ sessionId }}</span>
    </header>

    <SessionSkeleton v-if="isLoading && !currentSession" />
    <ErrorMessage v-else-if="error && !currentSession" :message="error" @retry="handleRetry" />

    <section v-else class="workflow-content">
      <PageHeader eyebrow="Learning Flow" title="四步学习流程" :subtitle="`当前关注：${currentStepMeta.task}`" />
      <StepProgress :steps="steps" :current-step="currentStep" />

      <article class="step-card">
        <div class="step-head">
          <h2>{{ currentStepMeta.step }}. {{ currentStepMeta.title }}</h2>
          <span class="status-tag">可切换查看全部步骤</span>
        </div>

        <section v-if="currentStep === 1" class="panel">
          <h3>目标诊断结果</h3>
          <p v-if="!diagnosis">暂无诊断结果，请刷新。</p>
          <template v-else>
            <p><strong>评分：</strong>{{ diagnosis.goalScore }}/100</p>
            <p><strong>结论：</strong>{{ diagnosis.feedback.summary }}</p>
            <p><strong>优势：</strong>{{ diagnosis.feedback.strengths.join('；') }}</p>
            <p><strong>风险：</strong>{{ diagnosis.feedback.risks.join('；') }}</p>
            <p><strong>建议目标：</strong>{{ diagnosis.feedback.rewrittenGoal }}</p>
          </template>
        </section>

        <section v-if="currentStep === 2" class="panel">
          <h3>路径选项</h3>
          <div class="path-grid">
            <article
              v-for="path in pathOptions"
              :key="path.pathId"
              class="path-item"
              :class="{ selected: path.pathId === selectedPathId }"
              @click="selectPath(path.pathId)"
            >
              <h4>{{ path.name }}</h4>
              <p>{{ path.description }}</p>
              <p>难度：{{ path.difficulty }}，预计 {{ path.estimatedMinutes }} 分钟</p>
              <ul>
                <li v-for="(s, idx) in path.steps" :key="`${path.pathId}-${idx}`">{{ s }}</li>
              </ul>
            </article>
          </div>
          <p v-if="selectedPath"><strong>已选：</strong>{{ selectedPath.name }}</p>
        </section>

        <section v-if="currentStep === 3" class="panel">
          <h3>分步学习清单</h3>
          <p v-if="!currentSession || currentSession.timeline.length === 0">暂无学习步骤。</p>
          <div v-else class="task-list">
            <article v-for="item in currentSession.timeline" :key="item.taskId" class="task-item">
              <button class="task-toggle" @click="toggleTask(item.taskId)">
                <span>任务 #{{ item.taskId }} - {{ stageLabel(item.stage) }}</span>
                <span>{{ isTaskExpanded(item.taskId) ? '收起' : '展开' }}</span>
              </button>
              <div v-if="isTaskExpanded(item.taskId)" class="task-body">
                <p><strong>状态：</strong>{{ item.status }}</p>
                <p><strong>该做什么：</strong>{{ stageGuide(item.stage) }}</p>
                <PrimaryButton
                  type="button"
                  @click="openTask(item.taskId, item.stage)"
                >
                  打开该步骤任务
                </PrimaryButton>
              </div>
            </article>
          </div>
          <PrimaryButton v-if="sessionStore.nextTask" type="button" @click="handleRunNextTask">继续当前推荐任务</PrimaryButton>
        </section>

        <section v-if="currentStep === 4" class="panel">
          <h3>学习总结</h3>
          <p v-if="!currentSession">暂无总结数据。</p>
          <template v-else>
            <p><strong>课程：</strong>{{ currentSession.courseId }} / {{ currentSession.chapterId }}</p>
            <p><strong>目标：</strong>{{ currentSession.goalText }}</p>
            <p>
              <strong>完成进度：</strong>
              {{ Math.round((currentSession.progress?.completionRate ?? 0) * 100) }}%
              ({{ currentSession.progress?.completedTaskCount ?? 0 }}/{{ currentSession.progress?.totalTaskCount ?? 0 }})
            </p>
            <p><strong>当前阶段：</strong>{{ stageLabel(currentSession.currentStage) }}</p>
            <div class="mastery-list">
              <p v-for="item in currentSession.masterySummary" :key="item.nodeId">
                {{ item.nodeName }}：{{ Math.round(item.masteryValue * 100) }}%
              </p>
            </div>
            <p v-if="sessionStore.nextTask">
              <strong>下一建议：</strong>
              进入任务 #{{ sessionStore.nextTask.taskId }}（{{ stageLabel(sessionStore.nextTask.stage) }}）
            </p>
          </template>
        </section>

        <nav class="actions">
          <button class="ghost-button" :disabled="currentStep === 1" @click="handlePrevious">上一步</button>
          <PrimaryButton v-if="currentStep < 4" type="button" @click="handleNext">下一步</PrimaryButton>
          <PrimaryButton v-else type="button" @click="goHome">完成并返回首页</PrimaryButton>
        </nav>
      </article>
    </section>
  </main>
</template>

<style scoped>
.workflow-page { min-height: 100dvh; padding: clamp(16px, 2.8vw, 30px); }
.toolbar { display: flex; justify-content: space-between; align-items: center; margin-bottom: var(--space-lg); }
.workflow-id { color: var(--color-text-secondary); font-size: var(--font-size-sm); }
.workflow-content { display: flex; flex-direction: column; gap: var(--space-lg); max-width: 960px; margin: 0 auto; }
.step-card { border: 1px solid var(--color-border); border-radius: var(--radius-xl); background: linear-gradient(165deg, rgba(16, 27, 50, 0.94), rgba(8, 14, 26, 0.96)); padding: clamp(16px, 2.8vw, 26px); box-shadow: var(--shadow-md); display: flex; flex-direction: column; gap: var(--space-lg); }
.step-head { display: flex; justify-content: space-between; align-items: center; gap: var(--space-sm); }
.status-tag { border: 1px solid var(--color-border); border-radius: 999px; padding: 4px 10px; color: var(--color-text-secondary); font-size: var(--font-size-xs); }
.panel { border: 1px solid var(--color-border); border-radius: var(--radius-md); background: rgba(12, 20, 38, 0.8); padding: var(--space-md); }
.path-grid { display: grid; gap: var(--space-sm); }
.path-item { border: 1px solid var(--color-border); border-radius: var(--radius-md); padding: 10px; cursor: pointer; }
.path-item.selected { border-color: var(--color-primary); box-shadow: 0 0 0 2px var(--color-primary-alpha); }
.task-list { display: grid; gap: 10px; margin-bottom: 10px; }
.task-item { border: 1px solid var(--color-border); border-radius: var(--radius-sm); overflow: hidden; }
.task-toggle { width: 100%; display: flex; justify-content: space-between; background: rgba(10, 18, 37, 0.9); border: none; color: var(--color-text); padding: 10px; }
.task-body { padding: 10px; display: grid; gap: 8px; }
.mastery-list { border: 1px solid var(--color-border); border-radius: var(--radius-sm); padding: 8px 10px; margin: 8px 0; }
.actions { display: grid; grid-template-columns: 140px 1fr; gap: var(--space-md); }
.ghost-button { min-height: 44px; border: 1px solid var(--color-border); border-radius: var(--radius-md); color: var(--color-text-secondary); background: rgba(12, 21, 42, 0.8); }
@media (max-width: 900px) { .actions { grid-template-columns: 1fr; } }
</style>
