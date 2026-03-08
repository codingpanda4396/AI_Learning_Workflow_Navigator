<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useSessionStore } from '@/stores/session'
import { useWorkflowStore } from '@/stores/workflow'
import { courseOptions } from '@/constants/courses'
import PageHeader from '@/components/PageHeader.vue'
import GoalInputCard from '@/components/GoalInputCard.vue'
import CourseSelector from '@/components/CourseSelector.vue'
import StepProgress from '@/components/StepProgress.vue'
import PrimaryButton from '@/components/PrimaryButton.vue'

const router = useRouter()
const sessionStore = useSessionStore()
const workflowStore = useWorkflowStore()

const defaultCourse = courseOptions[0]
const defaultChapter = defaultCourse?.chapters[0]
const defaultUserId = import.meta.env.VITE_DEFAULT_USER_ID || 'guest_user'

const goal = ref(workflowStore.goal || '')
const courseId = ref(workflowStore.courseId || defaultCourse?.id || '')
const chapterId = ref(workflowStore.chapterId || defaultChapter?.id || '')
const goalError = ref('')
const submitError = ref('')
const isCreating = computed(() => sessionStore.creatingSession || sessionStore.planning)

const stepPreview = [
  { step: 1 as const, title: '目标诊断' },
  { step: 2 as const, title: '路径规划' },
  { step: 3 as const, title: '分步学习' },
  { step: 4 as const, title: '总结反馈' },
]

const canSubmit = computed(() => goal.value.trim().length > 0 && !isCreating.value)
const goalHint = computed(() =>
  goal.value.trim().length > 0
    ? '系统将先诊断你的目标清晰度，再生成可执行学习路径。'
    : '请用一句话描述你想掌握什么、达到什么程度。',
)

function setCourse(nextCourseId: string) {
  courseId.value = nextCourseId
}

function setChapter(nextChapterId: string) {
  chapterId.value = nextChapterId
}

function validateGoal() {
  if (!goal.value.trim()) {
    goalError.value = '请先输入学习目标，才能开始流程导航。'
    return false
  }
  goalError.value = ''
  return true
}

async function handleSubmit() {
  if (!validateGoal() || isCreating.value) {
    return
  }

  submitError.value = ''

  const userId = localStorage.getItem('ai_learning_user_id') || defaultUserId
  const payload = {
    userId,
    courseId: courseId.value,
    chapterId: chapterId.value,
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
    workflowStore.setCurrentStep(1)
    router.push(`/session/${newSessionId}`)
  } catch {
    submitError.value = sessionStore.error || '会话创建失败，请稍后重试。'
  }
}

onMounted(async () => {
  const userId = localStorage.getItem('ai_learning_user_id') || defaultUserId
  try {
    const response = await sessionStore.fetchCurrentSession(userId)
    if (response.hasActiveSession && response.session) {
      router.replace(`/session/${response.session.sessionId}`)
    }
  } catch {
    // Ignore recovery failure on entry page.
  }
})
</script>

<template>
  <main class="home-page">
    <section class="hero-panel">
      <PageHeader
        eyebrow="AI Learning Navigator"
        title="把模糊学习目标拆成可执行流程"
        subtitle="围绕你的目标自动完成诊断、路径规划、分步学习与总结反馈。"
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

        <div class="action-block">
          <PrimaryButton type="submit" :disabled="!canSubmit" :loading="isCreating">
            {{ isCreating ? '正在生成学习流程...' : '开始学习流程' }}
          </PrimaryButton>
          <p v-if="submitError" class="submit-error">{{ submitError }}</p>
        </div>
      </form>
    </section>
  </main>
</template>

<style scoped>
.home-page {
  min-height: 100dvh;
  padding: clamp(20px, 4vw, 40px);
  display: grid;
  grid-template-columns: 1.1fr 1fr;
  gap: clamp(18px, 3vw, 32px);
}

.hero-panel,
.form-panel {
  border: 1px solid var(--color-border);
  border-radius: var(--radius-xl);
  background: linear-gradient(160deg, rgba(16, 27, 50, 0.92), rgba(10, 16, 30, 0.95));
  box-shadow: var(--shadow-md);
}

.hero-panel {
  padding: clamp(20px, 4vw, 40px);
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  gap: var(--space-xxl);
}

.form-panel {
  padding: clamp(18px, 3vw, 28px);
}

.start-form {
  display: flex;
  flex-direction: column;
  gap: var(--space-lg);
}

.action-block {
  border-top: 1px solid var(--color-border);
  padding-top: var(--space-lg);
  display: flex;
  flex-direction: column;
  gap: var(--space-sm);
}

.submit-error {
  margin: 0;
  color: var(--color-error);
  font-size: var(--font-size-sm);
}

@media (max-width: 980px) {
  .home-page {
    grid-template-columns: 1fr;
  }
}
</style>
