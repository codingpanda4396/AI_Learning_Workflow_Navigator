<template>
  <div class="min-h-screen py-8 px-4 relative overflow-hidden">
    <!-- Floating Background -->
    <div class="fixed inset-0 pointer-events-none">
      <div class="absolute top-1/4 right-10 w-32 h-32 bg-primary/5 rounded-full floating"></div>
      <div class="absolute bottom-1/4 left-10 w-24 h-24 bg-accent/10 rounded-full floating floating-delay-1"></div>
    </div>

    <div class="max-w-3xl mx-auto relative z-10">
      <!-- Loading State -->
      <div v-if="loading" class="clay-card p-12 text-center animate-fade-in">
        <div class="relative w-24 h-24 mx-auto mb-6">
          <div class="absolute inset-0 bg-primary/20 rounded-full animate-ping"></div>
          <div class="relative w-full h-full bg-gradient-to-br from-primary to-accent rounded-full flex items-center justify-center">
            <svg class="animate-spin h-10 w-10 text-white" fill="none" viewBox="0 0 24 24">
              <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
              <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8v4l5-5-5-5v4a10 10 0 00-10 10h2z"></path>
            </svg>
          </div>
        </div>
        <p class="text-xl font-heading text-primary-dark">加载任务中...</p>
      </div>

      <!-- Task Content -->
      <div v-else-if="currentTask" class="space-y-6">
        <!-- Progress Header -->
        <div class="clay-card p-4 animate-fade-in">
          <div class="flex items-center justify-between mb-3">
            <span class="font-heading font-medium text-gray-600">
              📊 任务进度
            </span>
            <span class="font-heading font-bold text-primary text-lg">
              {{ currentTaskIndex }} / {{ totalTasks }}
            </span>
          </div>
          <div class="progress-clay">
            <div class="progress-clay-fill" :style="{ width: `${progressPercentage}%` }"></div>
          </div>
        </div>

        <!-- Task Info Card -->
        <div class="clay-card p-6 animate-fade-in stagger-1 hover-lift">
          <div class="flex items-start justify-between gap-4 mb-4">
            <div class="flex-1">
              <span class="inline-block px-3 py-1 bg-primary/10 text-primary text-sm font-heading rounded-full mb-3">
                {{ currentTask.taskType }}
              </span>
              <h2 class="text-2xl font-heading font-bold text-gray-800">
                {{ currentTask.title }}
              </h2>
            </div>
            <div class="text-right flex-shrink-0 p-3 bg-accent/10 rounded-xl">
              <p class="text-sm text-gray-500">预计</p>
              <p class="font-heading font-bold text-accent text-xl">{{ currentTask.estimatedMinutes || 15 }} 分钟</p>
            </div>
          </div>

          <div class="p-4 bg-gray-50 rounded-xl mb-4">
            <p class="text-gray-600">
              {{ currentTask.goal || currentTask.description }}
            </p>
          </div>

          <div v-if="currentTask.taskMethod" class="p-4 bg-primary/5 rounded-xl mb-4 border-l-4 border-primary/30">
            <p class="font-heading font-medium text-primary-dark mb-2">📖 学习方法</p>
            <p class="text-sm text-gray-700">{{ currentTask.taskMethod }}</p>
          </div>

          <div v-if="currentTask.completionCriteria?.length" class="p-4 bg-accent/10 rounded-xl border-l-4 border-accent mb-4">
            <p class="font-heading font-medium text-accent-dark mb-2">🎯 完成标准</p>
            <ul class="space-y-1">
              <li v-for="(criteria, idx) in currentTask.completionCriteria" :key="idx" class="flex items-start gap-2 text-sm text-gray-700">
                <span class="text-accent">✓</span>
                {{ criteria }}
              </li>
            </ul>
          </div>

          <div v-if="currentTask.selfEvaluationQuestions?.length" class="p-4 bg-amber-50 rounded-xl border-l-4 border-amber-400 mb-4">
            <p class="font-heading font-medium text-amber-800 mb-2">🔄 自评问题</p>
            <ul class="space-y-1">
              <li v-for="(q, idx) in currentTask.selfEvaluationQuestions" :key="idx" class="flex items-start gap-2 text-sm text-gray-700">
                <span class="text-amber-600">?</span>
                {{ q }}
              </li>
            </ul>
          </div>
        </div>

        <!-- Recommended Prompt Template -->
        <div v-if="currentTask.recommendedPromptTemplate || currentTask.promptScaffold" class="clay-card p-6 animate-fade-in stagger-2 hover-lift">
          <h3 class="text-xl font-heading font-semibold text-primary-dark mb-4 flex items-center gap-2">
            <span class="w-8 h-8 bg-primary/10 rounded-lg flex items-center justify-center">💡</span>
            推荐提问模板
          </h3>
          <div class="p-4 bg-primary/5 rounded-xl border border-primary/20">
            <pre class="text-sm text-gray-700 whitespace-pre-wrap font-body">{{ currentTask.recommendedPromptTemplate || currentTask.promptScaffold }}</pre>
          </div>
        </div>

        <!-- Why This Task -->
        <div v-if="currentTask.whyThisTask" class="clay-card p-6 animate-fade-in stagger-3 hover-lift">
          <h3 class="text-xl font-heading font-semibold text-primary-dark mb-4 flex items-center gap-2">
            <span class="w-8 h-8 bg-accent/20 rounded-lg flex items-center justify-center">🤔</span>
            为什么要学这个
          </h3>
          <p class="text-gray-600">
            {{ currentTask.whyThisTask }}
          </p>
        </div>

        <!-- Interaction Area -->
        <div class="clay-card p-6 animate-fade-in stagger-4">
          <h3 class="text-xl font-heading font-semibold text-gray-800 mb-4 flex items-center gap-2">
            <span class="w-8 h-8 bg-primary-light/20 rounded-lg flex items-center justify-center">📝</span>
            记录你的学习
          </h3>

          <textarea
            v-model="interactionInput"
            rows="4"
            class="input-clay mb-4 resize-none"
            placeholder="在这里记录你的学习心得、问题或答案..."
          ></textarea>

          <div class="flex gap-3">
            <button
              @click="sendInteraction"
              :disabled="!interactionInput.trim() || interacting"
              :class="[
                'btn-clay flex-1 flex items-center justify-center gap-2 text-primary',
                (!interactionInput.trim() || interacting) && 'opacity-50 cursor-not-allowed'
              ]"
            >
              <svg v-if="interacting" class="animate-spin h-5 w-5" fill="none" viewBox="0 0 24 24">
                <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
                <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8v4l5-5-5-5v4a10 10 0 00-10 10h2z"></path>
              </svg>
              <span>{{ interacting ? '保存中...' : '💾 保存记录' }}</span>
            </button>
          </div>
        </div>

        <!-- Complete Button -->
        <button
          @click="completeTask"
          :disabled="completing"
          :class="[
            'btn-primary-clay w-full text-xl flex items-center justify-center gap-2 py-5',
            completing && 'opacity-50 cursor-not-allowed'
          ]"
        >
          <svg v-if="completing" class="animate-spin h-6 w-6" fill="none" viewBox="0 0 24 24">
            <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
            <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8v4l5-5-5-5v4a10 10 0 00-10 10h2z"></path>
          </svg>
          <span>{{ completing ? '完成中...' : '✅ 完成任务' }}</span>
        </button>

        <div v-if="error" class="p-4 bg-red-100 border-2 border-red-300 rounded-xl text-red-600 text-sm">
          ⚠️ {{ error }}
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
      // 交互成功
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
