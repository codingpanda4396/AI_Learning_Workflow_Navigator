<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useSessionStore } from '@/stores/session'
import { useWorkflowStore } from '@/stores/workflow'
import PageHeader from '@/components/PageHeader.vue'
import GoalInputCard from '@/components/GoalInputCard.vue'
import CourseSelector from '@/components/CourseSelector.vue'
import StepProgress from '@/components/StepProgress.vue'
import PrimaryButton from '@/components/PrimaryButton.vue'

const SKIP_RESUME_ONCE_KEY = 'ai_learning_skip_resume_once'
const defaultUserId = import.meta.env.VITE_DEFAULT_USER_ID || 'guest_user'

const route = useRoute()
const router = useRouter()
const sessionStore = useSessionStore()
const workflowStore = useWorkflowStore()

const goal = ref(workflowStore.goal || '')
const courseId = ref(workflowStore.courseId || '')
const chapterId = ref(workflowStore.chapterId || '')

const goalError = ref('')
const courseError = ref('')
const chapterError = ref('')
const submitError = ref('')
const diagnosisError = ref('')
const pathOptionsError = ref('')

const isCreating = computed(() => sessionStore.creatingSession || sessionStore.planning)
const isDiagnosing = computed(() => sessionStore.diagnosingGoal)
const isFetchingPaths = computed(() => sessionStore.fetchingPathOptions)
const isAnalyzing = computed(() => isDiagnosing.value || isFetchingPaths.value)
const diagnosis = computed(() => sessionStore.goalDiagnosis)
const pathOptions = computed(() => sessionStore.pathOptions)
const selectedPathId = computed(() => sessionStore.selectedPathId)
const checkingResume = ref(true)

const stepPreview = [
  { step: 1 as const, title: '目标诊断' },
  { step: 2 as const, title: '路径规划' },
  { step: 3 as const, title: '分步学习' },
  { step: 4 as const, title: '总结反馈' },
]

const canSubmit = computed(
  () =>
    goal.value.trim().length > 0 &&
    courseId.value.trim().length > 0 &&
    chapterId.value.trim().length > 0 &&
    !!selectedPathId.value &&
    !isCreating.value,
)
const hasDraftInput = computed(
  () => goal.value.trim().length > 0 || courseId.value.trim().length > 0 || chapterId.value.trim().length > 0,
)

const goalHint = computed(() =>
  goal.value.trim().length > 0
    ? '先诊断目标质量，再从候选路径中选择一条。'
    : '请描述你想学什么、希望达到什么程度。',
)

function setCourse(nextCourseId: string) {
  courseId.value = nextCourseId
  if (courseError.value) courseError.value = ''
}

function setChapter(nextChapterId: string) {
  chapterId.value = nextChapterId
  if (chapterError.value) chapterError.value = ''
}

function validateInputs() {
  let valid = true

  if (!goal.value.trim()) {
    goalError.value = '请先输入学习目标。'
    valid = false
  } else {
    goalError.value = ''
  }

  if (!courseId.value.trim()) {
    courseError.value = '请输入课程标识（支持自定义）。'
    valid = false
  } else {
    courseError.value = ''
  }

  if (!chapterId.value.trim()) {
    chapterError.value = '请输入章节标识（支持自定义）。'
    valid = false
  } else {
    chapterError.value = ''
  }

  return valid
}

async function handleAnalyze() {
  if (!validateInputs()) return
  await Promise.allSettled([handleDiagnose(), handleFetchPaths()])
}

function buildAnalyzePayload() {
  return {
    userId: defaultUserId,
    courseId: courseId.value.trim(),
    chapterId: chapterId.value.trim(),
    goalText: goal.value.trim(),
  }
}

async function handleDiagnose() {
  if (!validateInputs() || isDiagnosing.value) return
  diagnosisError.value = ''
  submitError.value = ''

  try {
    await sessionStore.diagnoseGoal(buildAnalyzePayload())
  } catch {
    diagnosisError.value = sessionStore.error || 'Goal diagnosis failed. Please retry.'
  }
}

async function handleFetchPaths() {
  if (!validateInputs() || isFetchingPaths.value) return
  pathOptionsError.value = ''
  submitError.value = ''

  try {
    await sessionStore.fetchPathOptions(buildAnalyzePayload())
  } catch {
    pathOptionsError.value = sessionStore.error || 'Path generation failed. Please retry.'
  }
}

function pickPath(pathId: string) {
  sessionStore.setSelectedPath(pathId)
}

function applyRewrittenGoal() {
  const rewritten = diagnosis.value?.feedback.rewrittenGoal?.trim()
  if (!rewritten) return
  goal.value = rewritten
}

async function handleSubmit() {
  if (!validateInputs() || isCreating.value) return

  if (!diagnosis.value || pathOptions.value.length === 0) {
    await handleAnalyze()
  }

  if (!selectedPathId.value) {
    submitError.value = '请先选择一条学习路径。'
    return
  }

  submitError.value = ''

  const userId = localStorage.getItem('ai_learning_user_id') || defaultUserId
  const payload = {
    userId,
    courseId: courseId.value.trim(),
    chapterId: chapterId.value.trim(),
    goalText: goal.value.trim(),
  }

  workflowStore.startWorkflow({
    goal: payload.goalText,
    courseId: payload.courseId,
    chapterId: payload.chapterId,
  })

  try {
    const newSessionId = await sessionStore.createSession(payload)
    await sessionStore.planSession(newSessionId)
    await sessionStore.fetchSessionOverview(newSessionId)
    workflowStore.setWorkflowId(String(newSessionId))
    await router.push(`/session/${newSessionId}`)
  } catch {
    submitError.value = sessionStore.error || '会话创建失败，请稍后重试。'
  }
}

onMounted(async () => {
  try {
    const skipByQuery = route.query.skipResume === '1'
    const skipByFlag = localStorage.getItem(SKIP_RESUME_ONCE_KEY) === '1'
    if (skipByQuery || skipByFlag) {
      localStorage.removeItem(SKIP_RESUME_ONCE_KEY)
      if (skipByQuery) {
        await router.replace({ name: 'home' })
      }
      return
    }

    const userId = localStorage.getItem('ai_learning_user_id') || defaultUserId
    const response = await sessionStore.fetchCurrentSession(userId)
    if (
      response.hasActiveSession &&
      response.session &&
      !hasDraftInput.value &&
      route.name === 'home'
    ) {
      await router.replace(`/session/${response.session.sessionId}`)
    }
  } catch {
    // ignore
  } finally {
    checkingResume.value = false
  }
})
</script>

<template>
  <main v-if="checkingResume" class="home-page">
    <section class="hero-panel">
      <PageHeader
        eyebrow="AI Learning Navigator"
        title="加载中..."
        subtitle="正在检查是否需要恢复上次学习会话。"
      />
    </section>
    <section class="form-panel"></section>
  </main>

  <main v-else class="home-page">
    <section class="hero-panel">
      <PageHeader
        eyebrow="AI Learning Navigator"
        title="先诊断目标，再开始学习"
        subtitle="输入课程、章节和目标，获取目标评估与可选学习路径。"
      />
      <StepProgress :current-step="1" :steps="stepPreview" />
    </section>

    <section class="form-panel">
      <form class="start-form" @submit.prevent="handleSubmit">
        <GoalInputCard v-model="goal" :hint="goalHint" :error="goalError" />
        <CourseSelector
          :course-id="courseId"
          :chapter-id="chapterId"
          @update:courseId="setCourse"
          @update:chapterId="setChapter"
        />

        <p v-if="courseError" class="submit-error">{{ courseError }}</p>
        <p v-if="chapterError" class="submit-error">{{ chapterError }}</p>
        <div class="action-block">
          <div class="analyze-actions">
            <PrimaryButton type="button" :disabled="isFetchingPaths" :loading="isFetchingPaths" @click="handleFetchPaths">
              生成学习路径
            </PrimaryButton>
            <PrimaryButton type="button" :disabled="isDiagnosing" :loading="isDiagnosing" @click="handleDiagnose">
              诊断目标
            </PrimaryButton>
          </div>

          <PrimaryButton type="button" :disabled="isAnalyzing" :loading="isAnalyzing" @click="handleAnalyze">
            诊断目标并生成路径
          </PrimaryButton>

          <section v-if="isDiagnosing && !diagnosis" class="diagnosis-card">
            <h3>目标诊断中...</h3>
            <p>诊断请求可能需要 10~60 秒，路径生成不受此步骤阻塞。</p>
          </section>

          <section v-if="diagnosis" class="diagnosis-card">
            <h3>目标评估：{{ diagnosis.goalScore }}/100</h3>
            <p>{{ diagnosis.feedback.summary }}</p>
            <p><strong>优势：</strong>{{ diagnosis.feedback.strengths.join('；') }}</p>
            <p><strong>风险：</strong>{{ diagnosis.feedback.risks.join('；') }}</p>
            <button type="button" class="ghost-btn" @click="applyRewrittenGoal">采用建议目标</button>
          </section>
          <section v-else-if="diagnosisError" class="diagnosis-card">
            <h3>目标诊断失败</h3>
            <p>{{ diagnosisError }}</p>
            <button type="button" class="ghost-btn" @click="handleDiagnose">重试诊断</button>
          </section>

          <section v-if="pathOptions.length > 0" class="paths-card">
            <h3>选择学习路径</h3>
            <div class="path-grid">
              <article
                v-for="path in pathOptions"
                :key="path.pathId"
                class="path-item"
                :class="{ selected: selectedPathId === path.pathId }"
                @click="pickPath(path.pathId)"
              >
                <h4>{{ path.name }}</h4>
                <p>{{ path.description }}</p>
                <p>难度：{{ path.difficulty }} | 预计 {{ path.estimatedMinutes }} 分钟</p>
                <ul>
                  <li v-for="(step, idx) in path.steps" :key="`${path.pathId}-${idx}`">{{ step }}</li>
                </ul>
              </article>
            </div>
          </section>
          <section v-else-if="pathOptionsError" class="paths-card">
            <h3>路径生成失败</h3>
            <p>{{ pathOptionsError }}</p>
            <button type="button" class="ghost-btn" @click="handleFetchPaths">重试生成路径</button>
          </section>

          <PrimaryButton type="submit" :disabled="!canSubmit" :loading="isCreating">
            开始分步学习
          </PrimaryButton>

          <p v-if="submitError" class="submit-error">{{ submitError }}</p>
        </div>
      </form>
    </section>
  </main>
</template>

<style scoped>
.home-page { min-height: 100dvh; padding: clamp(20px, 4vw, 40px); display: grid; grid-template-columns: 1.1fr 1fr; gap: clamp(18px, 3vw, 32px); }
.hero-panel, .form-panel { border: 1px solid var(--color-border); border-radius: var(--radius-xl); background: linear-gradient(160deg, rgba(16, 27, 50, 0.92), rgba(10, 16, 30, 0.95)); box-shadow: var(--shadow-md); }
.hero-panel { padding: clamp(20px, 4vw, 40px); display: flex; flex-direction: column; justify-content: space-between; gap: var(--space-xxl); }
.form-panel { padding: clamp(18px, 3vw, 28px); }
.start-form { display: flex; flex-direction: column; gap: var(--space-lg); }
.action-block { border-top: 1px solid var(--color-border); padding-top: var(--space-lg); display: flex; flex-direction: column; gap: var(--space-sm); }
.analyze-actions { display: grid; grid-template-columns: 1fr 1fr; gap: var(--space-sm); }
.diagnosis-card, .paths-card { border: 1px solid var(--color-border); border-radius: var(--radius-md); padding: var(--space-md); background: rgba(12, 21, 42, 0.8); }
.path-grid { display: grid; grid-template-columns: 1fr; gap: var(--space-sm); }
.path-item { border: 1px solid var(--color-border); border-radius: var(--radius-md); padding: 10px; cursor: pointer; }
.path-item.selected { border-color: var(--color-primary); box-shadow: 0 0 0 2px var(--color-primary-alpha); }
.ghost-btn { border: 1px solid var(--color-border); border-radius: var(--radius-sm); background: transparent; color: var(--color-text); padding: 6px 10px; }
.submit-error { margin: 0; color: var(--color-error); font-size: var(--font-size-sm); }
@media (max-width: 680px) { .analyze-actions { grid-template-columns: 1fr; } }
@media (max-width: 980px) { .home-page { grid-template-columns: 1fr; } }
</style>




