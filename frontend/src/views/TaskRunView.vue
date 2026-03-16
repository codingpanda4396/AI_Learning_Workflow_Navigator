<template>
  <div class="min-h-screen py-8 px-4">
    <div class="max-w-3xl mx-auto">
      <!-- Loading State -->
      <div v-if="loading" class="glass-card p-8 text-center animate-fade-in">
        <svg class="animate-spin h-10 w-10 mx-auto text-primary" fill="none" viewBox="0 0 24 24">
          <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
          <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
        </svg>
        <p class="mt-4 text-gray-600">加载任务中...</p>
      </div>

      <!-- Task Content -->
      <div v-else-if="currentTask" class="space-y-6">
        <!-- Progress Header -->
        <div class="glass-card p-4 animate-fade-in">
          <div class="flex items-center justify-between mb-2">
            <span class="text-sm font-medium text-gray-600">
              任务进度
            </span>
            <span class="text-sm text-primary font-medium">
              {{ currentTaskIndex }} / {{ totalTasks }}
            </span>
          </div>
          <div class="h-2 bg-gray-200 rounded-full overflow-hidden">
            <div
              class="h-full bg-gradient-to-r from-primary to-primary-light transition-all duration-500"
              :style="{ width: `${progressPercentage}%` }"
            ></div>
          </div>
        </div>

        <!-- Task Info Card -->
        <div class="glass-card p-6 animate-fade-in stagger-1">
          <div class="flex items-start justify-between gap-4 mb-4">
            <div>
              <span class="inline-block px-3 py-1 bg-primary/10 text-primary text-xs font-medium rounded-full mb-2">
                {{ currentTask.taskType }}
              </span>
              <h2 class="text-xl font-heading font-semibold text-gray-800">
                {{ currentTask.title }}
              </h2>
            </div>
            <div class="text-right flex-shrink-0">
              <p class="text-sm text-gray-500">预计</p>
              <p class="font-medium text-gray-700">{{ currentTask.estimatedMinutes || 15 }} 分钟</p>
            </div>
          </div>

          <div class="p-4 bg-gray-50 rounded-xl mb-4">
            <p class="text-sm text-gray-600">
              {{ currentTask.goal || currentTask.description }}
            </p>
          </div>

          <div v-if="currentTask.completionCriteria?.length" class="p-4 bg-accent/10 border border-accent/20 rounded-xl">
            <p class="text-sm font-medium text-accent-dark mb-2">完成标准</p>
            <ul class="text-sm text-gray-700 space-y-1">
              <li v-for="(criteria, idx) in currentTask.completionCriteria" :key="idx">
                • {{ criteria }}
              </li>
            </ul>
          </div>
        </div>

        <!-- Prompt Scaffold -->
        <div v-if="currentTask.promptScaffold" class="glass-card p-6 animate-fade-in stagger-2">
          <h3 class="text-lg font-heading font-semibold text-primary-dark mb-4">
            提问框架
          </h3>
          <div class="p-4 bg-white/60 rounded-xl border border-primary/20">
            <pre class="text-sm text-gray-700 whitespace-pre-wrap font-body">{{ currentTask.promptScaffold }}</pre>
          </div>
        </div>

        <!-- Why This Task -->
        <div v-if="currentTask.whyThisTask" class="glass-card p-6 animate-fade-in stagger-3">
          <h3 class="text-lg font-heading font-semibold text-primary-dark mb-4">
            为什么要学这个
          </h3>
          <p class="text-sm text-gray-600">
            {{ currentTask.whyThisTask }}
          </p>
        </div>

        <!-- Interaction Area -->
        <div class="glass-card p-6 animate-fade-in stagger-4">
          <h3 class="text-lg font-heading font-semibold text-gray-800 mb-4">
            记录你的学习
          </h3>

          <textarea
            v-model="interactionInput"
            rows="4"
            class="input-field resize-none mb-4"
            placeholder="在这里记录你的学习心得、问题或答案..."
          ></textarea>

          <div class="flex gap-3">
            <button
              @click="sendInteraction"
              :disabled="!interactionInput.trim() || interacting"
              :class="[
                'btn-secondary flex-1 flex items-center justify-center gap-2',
                (!interactionInput.trim() || interacting) && 'opacity-50 cursor-not-allowed'
              ]"
            >
              <svg v-if="interacting" class="animate-spin h-4 w-4" fill="none" viewBox="0 0 24 24">
                <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
                <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
              </svg>
              <span>{{ interacting ? '保存中...' : '保存记录' }}</span>
            </button>
          </div>
        </div>

        <!-- Complete Button -->
        <button
          @click="completeTask"
          :disabled="completing"
          :class="[
            'w-full btn-primary flex items-center justify-center gap-2 py-4 text-lg',
            completing && 'opacity-50 cursor-not-allowed'
          ]"
        >
          <svg v-if="completing" class="animate-spin h-5 w-5" fill="none" viewBox="0 0 24 24">
            <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
            <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
          </svg>
          <span>{{ completing ? '完成中...' : '完成任务' }}</span>
        </button>

        <!-- Error Message -->
        <div v-if="error" class="p-4 bg-red-50 border border-red-200 rounded-xl text-red-600 text-sm">
          {{ error }}
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useLearningFlowStore } from '@/stores/learningFlow'
import { sessionApi, taskApi } from '@/api/goal'

const router = useRouter()
const store = useLearningFlowStore()

const loading = ref(true)
const interacting = ref(false)
const completing = ref(false)
const error = ref('')
const interactionInput = ref('')
const currentTask = ref(null)

const currentTaskIndex = computed(() => store.currentTaskIndex)
const totalTasks = computed(() => store.totalTasks)

// 后端返回的 currentIndex 是 1-based，所以直接使用不需要 +1
// 进度百分比计算
const progressPercentage = computed(() => {
  if (totalTasks.value === 0) return 0
  return Math.min((currentTaskIndex.value / totalTasks.value) * 100, 100)
})

onMounted(async () => {
  await loadCurrentTask()
})

async function loadCurrentTask() {
  loading.value = true
  error.value = ''

  try {
    const response = await sessionApi.getCurrentTask(store.sessionId)
    const data = response.data

    if (data.code === 'OK') {
      currentTask.value = data.data.currentTask
      store.setCurrentTask(data.data)
    } else if (data.code === 'SESSION_NOT_COMPLETED') {
      error.value = data.message
    } else if (data.code === 'SESSION_ALREADY_COMPLETED') {
      router.push({ name: 'Report' })
    } else {
      error.value = data.message || '加载任务失败'
    }
  } catch (err) {
    error.value = err.response?.data?.message || '网络错误，请重试'
  } finally {
    loading.value = false
  }
}

async function sendInteraction() {
  if (!interactionInput.value.trim()) return

  interacting.value = true
  error.value = ''

  try {
    const response = await taskApi.sendInteraction(currentTask.value.taskId, {
      sessionId: store.sessionId,
      interactionType: 'USER_SUMMARY',
      userSummarySubmitted: true,
      contentSummary: interactionInput.value
    })

    const data = response.data

    if (data.code === 'OK') {
      // 交互成功，但不清空输入，让用户可以继续补充
    } else {
      error.value = data.message || '保存记录失败'
    }
  } catch (err) {
    error.value = err.response?.data?.message || '网络错误，请重试'
  } finally {
    interacting.value = false
  }
}

async function completeTask() {
  completing.value = true
  error.value = ''

  try {
    const response = await taskApi.completeTask(currentTask.value.taskId, {
      sessionId: store.sessionId,
      completionStatus: 'COMPLETED',
      userSummarySubmitted: interactionInput.value.trim() ? true : false,
      userSummary: interactionInput.value
    })

    const data = response.data

    if (data.code === 'OK') {
      if (data.data.nextTaskAvailable) {
        interactionInput.value = ''
        await loadCurrentTask()
      } else {
        router.push({ name: 'Report' })
      }
    } else {
      error.value = data.message || '完成任务失败'
    }
  } catch (err) {
    error.value = err.response?.data?.message || '网络错误，请重试'
  } finally {
    completing.value = false
  }
}
</script>
