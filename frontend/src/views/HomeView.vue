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

const goal = ref('')
const courseId = ref(defaultCourse?.id ?? '')
const chapterId = ref(defaultChapter?.id ?? '')
const goalError = ref('')
const submitError = ref('')
const isCreating = ref(false)

const stepPreview = [
  { step: 1 as const, title: '诊断目标' },
  { step: 2 as const, title: '生成路径' },
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
  if (!validateGoal()) {
    return
  }

  submitError.value = ''
  isCreating.value = true
  workflowStore.setLoading(true)

  const payload = {
    user_id: 'mock_user_001',
    course_id: courseId.value,
    chapter_id: chapterId.value,
    goal_text: goal.value.trim(),
  }

  workflowStore.startWorkflow({
    goal: payload.goal_text,
    courseId: payload.course_id,
    chapterId: payload.chapter_id,
  })

  workflowStore.setStepState(1, {
    status: 'running',
    input: {
      goal: payload.goal_text,
      courseId: payload.course_id,
      chapterId: payload.chapter_id,
    },
    output: {
      summary: '准备开始目标诊断',
    },
  })

  try {
    const sessionId = await sessionStore.createSession(payload)
    await sessionStore.planSession(sessionId)
    await sessionStore.fetchSessionOverview(sessionId)

    workflowStore.setWorkflowId(String(sessionId))
    workflowStore.setCurrentStep(1)
    workflowStore.setStepState(1, {
      status: 'done',
      output: {
        summary: '目标已提交，系统已准备诊断路径。',
      },
    })

    router.push(`/session/${sessionId}`)
  } catch (error) {
    console.error('Failed to create session:', error)
    submitError.value = '会话创建失败，请稍后重试。'
    workflowStore.setStepState(1, {
      status: 'error',
      output: {
        summary: '目标诊断初始化失败。',
      },
    })
  } finally {
    isCreating.value = false
    workflowStore.setLoading(false)
  }
}

onMounted(() => {
  workflowStore.loadFromStorage()

  if (workflowStore.goal) {
    goal.value = workflowStore.goal
  }
  if (workflowStore.courseId) {
    courseId.value = workflowStore.courseId
  }
  if (workflowStore.chapterId) {
    chapterId.value = workflowStore.chapterId
  }
})
</script>

<template>
  <main class="home-page">
    <section class="hero-panel">
      <PageHeader
        eyebrow="AI Learning Navigator"
        title="把模糊学习目标，拆成可执行学习流程"
        subtitle="不是普通聊天，而是围绕你的目标自动完成诊断、路径规划、分步学习与总结反馈。"
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
            {{ isCreating ? '正在生成学习流程...' : '4. 开始学习流程' }}
          </PrimaryButton>
          <p v-if="submitError" class="submit-error">{{ submitError }}</p>
          <p class="action-hint">
            进入流程页后，你将看到每一步的输入、产出与进度状态，支持后续联调。
          </p>
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

.action-hint {
  color: var(--color-text-secondary);
  font-size: var(--font-size-sm);
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

@media (max-width: 768px) {
  .home-page {
    padding: max(16px, env(safe-area-inset-top)) 16px calc(20px + env(safe-area-inset-bottom));
    gap: 14px;
  }
}
</style>
