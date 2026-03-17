<template>
  <PageContainer>
    <AppTopBar current="task" />
    <main class="mx-auto max-w-3xl px-6 py-8">
      <LoadingState v-if="loading && !task" message="加载任务中..." />
      <ErrorState v-else-if="error" :message="error">
        <template #action>
          <SecondaryButton @click="fetchTask">重试</SecondaryButton>
        </template>
      </ErrorState>
      <EmptyState
        v-else-if="!task && !loading"
        message="暂无任务"
      >
        <template #action>
          <SecondaryButton @click="router.push('/report')">
            查看报告
          </SecondaryButton>
        </template>
      </EmptyState>

      <div v-else-if="task" class="space-y-6">
        <div class="flex items-center justify-between">
          <h1 class="text-2xl font-bold text-text-primary">
            {{ task.title }}
          </h1>
          <StatusBadge
            v-if="progress"
            :label="`${progress.currentIndex} / ${progress.totalTasks}`"
          />
        </div>

        <FormCard>
          <SectionHeader>任务目标</SectionHeader>
          <p class="text-text-primary">{{ task.goal }}</p>
        </FormCard>

        <FormCard v-if="task.whyThisTask">
          <SectionHeader>为什么做这一步</SectionHeader>
          <p class="text-text-secondary">{{ task.whyThisTask }}</p>
        </FormCard>

        <FormCard v-if="task.taskMethod">
          <SectionHeader>学习方法</SectionHeader>
          <p class="text-text-primary">{{ task.taskMethod }}</p>
        </FormCard>

        <FormCard v-if="promptTemplate">
          <SectionHeader>推荐提问模板</SectionHeader>
          <pre class="whitespace-pre-wrap rounded bg-gray-50 p-4 text-sm text-text-primary">{{ promptTemplate }}</pre>
        </FormCard>

        <FormCard v-if="task.completionCriteria?.length">
          <SectionHeader>完成标准</SectionHeader>
          <ul class="list-disc space-y-1 pl-5 text-text-secondary">
            <li v-for="(c, i) in task.completionCriteria" :key="i">{{ c }}</li>
          </ul>
        </FormCard>

        <FormCard v-if="task.selfEvaluationQuestions?.length">
          <SectionHeader>自评问题</SectionHeader>
          <ul class="list-disc space-y-1 pl-5 text-text-secondary">
            <li
              v-for="(q, i) in task.selfEvaluationQuestions"
              :key="i"
            >
              {{ q }}
            </li>
          </ul>
        </FormCard>

        <FormCard>
          <SectionHeader>完成任务</SectionHeader>
          <form class="space-y-4" @submit.prevent="onComplete">
            <div>
              <label class="mb-1.5 block text-sm font-medium text-text-primary">
                完成状态 <span class="text-red-500">*</span>
              </label>
              <select
                v-model="completeForm.completionStatus"
                class="w-full rounded-input border border-border px-4 py-2.5 text-text-primary focus:border-primary focus:outline-none focus:ring-1 focus:ring-primary"
                required
              >
                <option
                  v-for="(label, val) in taskCompletionStatusLabels"
                  :key="val"
                  :value="val"
                >
                  {{ label }}
                </option>
              </select>
            </div>
            <div>
              <label class="mb-1.5 block text-sm font-medium text-text-primary">
                耗时（分钟）
              </label>
              <input
                v-model.number="completeForm.durationMinutes"
                type="number"
                min="0"
                class="w-full rounded-input border border-border px-4 py-2.5 text-text-primary focus:border-primary focus:outline-none focus:ring-1 focus:ring-primary"
              />
            </div>
            <div>
              <label class="mb-1.5 block text-sm font-medium text-text-primary">
                交互次数
              </label>
              <input
                v-model.number="completeForm.interactionCount"
                type="number"
                min="0"
                class="w-full rounded-input border border-border px-4 py-2.5 text-text-primary focus:border-primary focus:outline-none focus:ring-1 focus:ring-primary"
              />
            </div>
            <div>
              <label class="mb-1.5 block text-sm font-medium text-text-primary">
                学习反思
              </label>
              <textarea
                v-model="completeForm.learnerReflection"
                rows="3"
                class="w-full rounded-input border border-border px-4 py-3 text-text-primary placeholder:text-gray-400 focus:border-primary focus:outline-none focus:ring-1 focus:ring-primary"
                placeholder="简要记录你的理解与收获"
              />
            </div>
            <div class="flex gap-3">
              <PrimaryButton :loading="completing" type="submit">
                提交完成
              </PrimaryButton>
            </div>
          </form>
        </FormCard>
      </div>
    </main>
  </PageContainer>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import PageContainer from '@/components/layout/PageContainer.vue'
import AppTopBar from '@/components/layout/AppTopBar.vue'
import FormCard from '@/components/ui/FormCard.vue'
import PrimaryButton from '@/components/ui/PrimaryButton.vue'
import SecondaryButton from '@/components/ui/SecondaryButton.vue'
import SectionHeader from '@/components/common/SectionHeader.vue'
import StatusBadge from '@/components/ui/StatusBadge.vue'
import LoadingState from '@/components/ui/LoadingState.vue'
import ErrorState from '@/components/ui/ErrorState.vue'
import EmptyState from '@/components/ui/EmptyState.vue'
import { useWorkflowStore } from '@/stores/workflow'
import { getCurrentTask, completeTask } from '@/api/task'
import { showToast } from '@/stores/toast'
import { getErrorMessage } from '@/api/request'
import { taskCompletionStatusLabels } from '@/types/labels'
import { TaskCompletionStatus } from '@/types/enums'
import type { CurrentTaskItem, ProgressItem } from '@/types/dto'

const router = useRouter()
const store = useWorkflowStore()

const loading = ref(true)
const completing = ref(false)
const error = ref<string | null>(null)
const task = ref<CurrentTaskItem | null>(null)
const progress = ref<ProgressItem | null>(null)

const promptTemplate = computed(() =>
  store.currentTask?.recommendedPromptTemplate ?? store.currentTask?.promptScaffold ?? ''
)

const completeForm = ref({
  completionStatus: TaskCompletionStatus.COMPLETED,
  durationMinutes: undefined as number | undefined,
  interactionCount: undefined as number | undefined,
  learnerReflection: '',
})

async function fetchTask() {
  if (!store.sessionId) return
  loading.value = true
  error.value = null
  try {
    const data = await getCurrentTask(store.sessionId)
    store.currentTask = data.currentTask
    store.progress = data.progress
    task.value = data.currentTask
    progress.value = data.progress
    if (!data.currentTask) {
      router.push('/report')
    }
  } catch (err) {
    error.value = getErrorMessage(err)
  } finally {
    loading.value = false
  }
}

async function onComplete() {
  if (!store.sessionId || !task.value) return
  completing.value = true
  try {
    const payload = {
      sessionId: store.sessionId,
      completionStatus: completeForm.value.completionStatus,
      durationMinutes: completeForm.value.durationMinutes,
      interactionCount: completeForm.value.interactionCount,
      learnerReflection: completeForm.value.learnerReflection || undefined,
    }
    const data = await completeTask(task.value.taskId, payload)
    store.currentTask = null
    completeForm.value = {
      completionStatus: TaskCompletionStatus.COMPLETED,
      durationMinutes: undefined,
      interactionCount: undefined,
      learnerReflection: '',
    }
    if (data.nextTaskAvailable && data.nextTaskId) {
      store.currentTaskId = data.nextTaskId
      await fetchTask()
    } else {
      router.push('/report')
    }
  } catch (err) {
    showToast(getErrorMessage(err))
  } finally {
    completing.value = false
  }
}

onMounted(() => {
  fetchTask()
})
</script>
