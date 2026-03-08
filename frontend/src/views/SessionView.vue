<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useSessionStore } from '@/stores/session'
import { useWorkflowStore } from '@/stores/workflow'
import type { WorkflowStepNumber } from '@/types/workflow'
import PageHeader from '@/components/PageHeader.vue'
import StepProgress from '@/components/StepProgress.vue'
import PrimaryButton from '@/components/PrimaryButton.vue'
import SessionSkeleton from '@/components/SessionSkeleton.vue'
import ErrorMessage from '@/components/ErrorMessage.vue'

const SKIP_RESUME_ONCE_KEY = 'ai_learning_skip_resume_once'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const sessionStore = useSessionStore()
const workflowStore = useWorkflowStore()

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
  { step: 1, title: 'Goal diagnosis', task: 'Review goal quality feedback' },
  { step: 2, title: 'Path planning', task: 'Choose a learning path' },
  { step: 3, title: 'Step-by-step tasks', task: 'Execute tasks in order' },
  { step: 4, title: 'Summary', task: 'Review progress and next action' },
]

const currentSession = computed(() => sessionStore.currentSession)
const isLoading = computed(() => sessionStore.fetchingSession || sessionStore.recoveringSession)
const error = computed(() => sessionStore.error)
const diagnosis = computed(() => sessionStore.goalDiagnosis)
const diagnosisData = computed(() => ({
  goalScore: diagnosis.value?.goalScore ?? 0,
  feedback: {
    summary: diagnosis.value?.feedback.summary ?? '',
    strengths: diagnosis.value?.feedback.strengths ?? [],
    risks: diagnosis.value?.feedback.risks ?? [],
    rewrittenGoal: diagnosis.value?.feedback.rewrittenGoal ?? '',
  },
}))
const pathOptions = computed(() => sessionStore.pathOptions)
const selectedPathId = computed(() => sessionStore.selectedPathId)
const sessionData = computed(() => ({
  courseId: currentSession.value?.courseId ?? '',
  chapterId: currentSession.value?.chapterId ?? '',
  goalText: currentSession.value?.goalText ?? '',
  currentStage: currentSession.value?.currentStage ?? '',
  timeline: currentSession.value?.timeline ?? [],
  masterySummary: currentSession.value?.masterySummary ?? [],
  progress: currentSession.value?.progress ?? null,
}))

const currentStep = computed(() => browsingStep.value)
const currentStepMeta = computed(() => steps.find((item) => item.step === currentStep.value) ?? steps[0]!)
const selectedPath = computed(() => pathOptions.value.find((item) => item.pathId === selectedPathId.value) ?? null)
const username = computed(() => authStore.currentUser?.username ?? '')

function stageLabel(stage: string) {
  const map: Record<string, string> = {
    STRUCTURE: '缁撴瀯鏋勫缓',
    UNDERSTANDING: '鐞嗚В娣卞寲',
    TRAINING: '璁粌瀹炶返',
    REFLECTION: '鍙嶆€濇€荤粨',
  }
  return map[stage] || stage
}

function stageGuide(stage: string) {
  const map: Record<string, string> = {
    STRUCTURE: 'Organize key concepts and their relationships.',
    UNDERSTANDING: 'Explain mechanisms and fix misunderstandings.',
    TRAINING: 'Complete exercises and iterate from feedback.',
    REFLECTION: 'Summarize mistakes and plan next improvements.',
  }
  return map[stage] || 'Follow the task guidance for this stage.'
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

async function handleLogout() {
  authStore.clearAuth()
  sessionStore.reset()
  workflowStore.reset()
  await router.replace('/auth')
}

onMounted(async () => {
  browsingStep.value = resolveStep(route.query.step)
  await fetchSession()
})
</script>

<template>
  <main class="workflow-page">
    <header class="toolbar">
      <button class="ghost-button" @click="goHome">杩斿洖棣栭〉</button>
      <span class="workflow-id">Session #{{ sessionId }}</span>
      <span class="workflow-id">{{ username }}</span>
      <button type="button" class="ghost-button" @click="handleLogout">退出登录</button>
    </header>

    <SessionSkeleton v-if="isLoading && !currentSession" />
    <ErrorMessage v-else-if="error && !currentSession" :message="error" @retry="handleRetry" />

    <section v-else class="workflow-content">
      <PageHeader eyebrow="Learning Flow" title="鍥涙瀛︿範娴佺▼" :subtitle="`褰撳墠鍏虫敞锛?{currentStepMeta.task}`" />
      <StepProgress :steps="steps" :current-step="currentStep" />

      <article class="step-card">
        <div class="step-head">
          <h2>{{ currentStepMeta.step }}. {{ currentStepMeta.title }}</h2>
          <span class="status-tag">鍙垏鎹㈡煡鐪嬪叏閮ㄦ楠</span>
        </div>

        <section v-if="currentStep === 1" class="panel">
          <h3>鐩爣璇婃柇缁撴灉</h3>
          <p v-if="!diagnosis">鏆傛棤璇婃柇缁撴灉锛岃鍒锋柊銆</p>
          <template v-else>
            <p><strong>璇勫垎锛</strong>{{ diagnosisData.goalScore }}/100</p>
            <p><strong>缁撹锛</strong>{{ diagnosisData.feedback.summary }}</p>
            <p><strong>浼樺娍锛</strong>{{ diagnosisData.feedback.strengths.join(', ') }}</p>
            <p><strong>椋庨櫓锛</strong>{{ diagnosisData.feedback.risks.join(', ') }}</p>
            <p><strong>寤鸿鐩爣锛</strong>{{ diagnosisData.feedback.rewrittenGoal }}</p>
          </template>
        </section>

        <section v-if="currentStep === 2" class="panel">
          <h3>璺緞閫夐」</h3>
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
              <p>闅惧害锛歿{ path.difficulty }}锛岄璁?{{ path.estimatedMinutes }} 鍒嗛挓</p>
              <ul>
                <li v-for="(s, idx) in path.steps" :key="`${path.pathId}-${idx}`">{{ s }}</li>
              </ul>
            </article>
          </div>
          <p v-if="selectedPath"><strong>宸查€夛細</strong>{{ selectedPath.name }}</p>
        </section>

        <section v-if="currentStep === 3" class="panel">
          <h3>鍒嗘瀛︿範娓呭崟</h3>
          <p v-if="!currentSession || sessionData.timeline.length === 0">鏆傛棤瀛︿範姝ラ銆</p>
          <div v-else class="task-list">
            <article v-for="item in sessionData.timeline" :key="item.taskId" class="task-item">
              <button class="task-toggle" @click="toggleTask(item.taskId)">
                <span>浠诲姟 #{{ item.taskId }} - {{ stageLabel(item.stage) }}</span>
                <span>{{ isTaskExpanded(item.taskId) ? '鏀惰捣' : '灞曞紑' }}</span>
              </button>
              <div v-if="isTaskExpanded(item.taskId)" class="task-body">
                <p><strong>鐘舵€侊細</strong>{{ item.status }}</p>
                <p><strong>璇ュ仛浠€涔堬細</strong>{{ stageGuide(item.stage) }}</p>
                <PrimaryButton
                  type="button"
                  @click="openTask(item.taskId, item.stage)"
                >
                  鎵撳紑璇ユ楠や换鍔?                </PrimaryButton>
              </div>
            </article>
          </div>
          <PrimaryButton v-if="sessionStore.nextTask" type="button" @click="handleRunNextTask">缁х画褰撳墠鎺ㄨ崘浠诲姟</PrimaryButton>
        </section>

        <section v-if="currentStep === 4" class="panel">
          <h3>瀛︿範鎬荤粨</h3>
          <p v-if="!currentSession">鏆傛棤鎬荤粨鏁版嵁銆</p>
          <template v-else>
            <p><strong>璇剧▼锛</strong>{{ sessionData.courseId }} / {{ sessionData.chapterId }}</p>
            <p><strong>鐩爣锛</strong>{{ sessionData.goalText }}</p>
            <p>
              <strong>瀹屾垚杩涘害锛</strong>
              {{ Math.round((sessionData.progress?.completionRate ?? 0) * 100) }}%
              ({{ sessionData.progress?.completedTaskCount ?? 0 }}/{{ sessionData.progress?.totalTaskCount ?? 0 }})
            </p>
            <p><strong>褰撳墠闃舵锛</strong>{{ stageLabel(sessionData.currentStage) }}</p>
            <div class="mastery-list">
              <p v-for="item in sessionData.masterySummary" :key="item.nodeId">
                {{ item.nodeName }}锛歿{ Math.round(item.masteryValue * 100) }}%
              </p>
            </div>
            <p v-if="sessionStore.nextTask">
              <strong>涓嬩竴寤鸿锛</strong>
              杩涘叆浠诲姟 #{{ sessionStore.nextTask.taskId }}锛坽{ stageLabel(sessionStore.nextTask.stage) }}锛?            </p>
          </template>
        </section>

        <nav class="actions">
          <button class="ghost-button" :disabled="currentStep === 1" @click="handlePrevious">涓婁竴姝</button>
          <PrimaryButton v-if="currentStep < 4" type="button" @click="handleNext">涓嬩竴姝</PrimaryButton>
          <PrimaryButton v-else type="button" @click="goHome">瀹屾垚骞惰繑鍥為椤</PrimaryButton>
        </nav>
      </article>
    </section>
  </main>
</template>

<style scoped>
.workflow-page { min-height: 100dvh; padding: clamp(16px, 2.8vw, 30px); }
.toolbar { display: flex; justify-content: flex-end; align-items: center; gap: var(--space-md); margin-bottom: var(--space-lg); flex-wrap: wrap; }
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





