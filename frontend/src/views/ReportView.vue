<template>
  <div class="min-h-screen py-8 px-4 relative overflow-hidden">
    <!-- Floating Background -->
    <div class="fixed inset-0 pointer-events-none">
      <div class="absolute top-20 left-20 w-28 h-28 bg-accent/10 rounded-full floating"></div>
      <div class="absolute top-1/3 right-20 w-24 h-24 bg-primary/10 rounded-full floating floating-delay-1"></div>
      <div class="absolute bottom-1/4 right-1/3 w-20 h-20 bg-primary-light/10 rounded-full floating floating-delay-2"></div>
    </div>

    <div class="max-w-3xl mx-auto relative z-10">
      <!-- Header -->
      <div class="text-center mb-8 animate-fade-in">
        <div class="inline-flex items-center justify-center w-20 h-20 bg-gradient-to-br from-accent to-green-400 rounded-2xl shadow-float mb-4">
          <svg class="w-10 h-10 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z"></path>
          </svg>
        </div>
        <h1 class="text-3xl font-heading font-bold gradient-text mb-2">
          学习报告
        </h1>
        <p class="text-lg text-primary/70">
          恭喜完成本次学习！🎉
        </p>
      </div>

      <!-- Loading State -->
      <div v-if="loading" class="clay-card p-12 text-center animate-fade-in">
        <div class="relative w-24 h-24 mx-auto mb-6">
          <div class="absolute inset-0 bg-accent/20 rounded-full animate-ping"></div>
          <div class="relative w-full h-full bg-gradient-to-br from-accent to-green-400 rounded-full flex items-center justify-center">
            <svg class="animate-spin h-10 w-10 text-white" fill="none" viewBox="0 0 24 24">
              <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
              <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8v4l5-5-5-5v4a10 10 0 00-10 10h2z"></path>
            </svg>
          </div>
        </div>
        <p class="text-xl font-heading text-primary-dark">生成报告中...</p>
      </div>

      <!-- Error State -->
      <div v-else-if="error" class="clay-card p-8 text-center">
        <div class="w-16 h-16 bg-red-100 rounded-full flex items-center justify-center mx-auto mb-4">
          <svg class="w-8 h-8 text-red-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"></path>
          </svg>
        </div>
        <p class="text-red-600 mb-4 text-lg">{{ error }}</p>
        <button @click="loadReport" class="btn-clay text-primary">🔄 点击重试</button>
      </div>

      <!-- Report Content -->
      <div v-else-if="reportData" class="space-y-6">
        <!-- Result Summary -->
        <div class="clay-card p-6 animate-fade-in hover-lift">
          <div class="flex items-center gap-4 mb-6">
            <div class="w-16 h-16 bg-gradient-to-br from-accent to-green-400 rounded-2xl flex items-center justify-center shadow-float">
              <svg class="w-8 h-8 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7"></path>
              </svg>
            </div>
            <div>
              <h3 class="text-2xl font-heading font-bold text-gray-800">
                {{ reportData.resultStatus === 'COMPLETED' ? '学习完成！🎉' : '部分完成' }}
              </h3>
              <p class="text-gray-500">
                {{ formatCompletedProgress(reportData.completedProgress) }}
              </p>
            </div>
          </div>

          <div class="p-4 bg-gray-50 rounded-xl">
            <p class="text-gray-600">
              {{ reportData.goalReview || '本次学习目标已达成' }}
            </p>
          </div>
        </div>

        <!-- Key Gains -->
        <div class="clay-card p-6 animate-fade-in stagger-1 hover-lift">
          <h3 class="text-xl font-heading font-semibold text-primary-dark mb-4 flex items-center gap-2">
            <span class="w-8 h-8 bg-primary/10 rounded-lg flex items-center justify-center">💡</span>
            关键收获
          </h3>
          <div class="p-4 bg-primary/5 rounded-xl">
            <p class="text-gray-700 whitespace-pre-line">
              {{ reportData.summaryText || '暂无总结' }}
            </p>
          </div>
        </div>

        <!-- Evidence Summary -->
        <div v-if="reportData.evidenceSummary?.length" class="clay-card p-6 animate-fade-in stagger-2 hover-lift">
          <h3 class="text-xl font-heading font-semibold text-primary-dark mb-4 flex items-center gap-2">
            <span class="w-8 h-8 bg-primary-light/20 rounded-lg flex items-center justify-center">📊</span>
            证据与反馈
          </h3>
          <div class="p-4 bg-primary/5 rounded-xl">
            <ul class="space-y-2">
              <li v-for="(evidence, idx) in reportData.evidenceSummary" :key="idx" class="flex items-start gap-2">
                <span class="text-primary">•</span>
                <span class="text-gray-700">{{ evidence }}</span>
              </li>
            </ul>
          </div>
        </div>

        <!-- Unresolved Issues -->
        <div v-if="reportData.unresolvedIssues?.length" class="clay-card p-6 animate-fade-in stagger-3 hover-lift">
          <h3 class="text-xl font-heading font-semibold text-amber-700 mb-4 flex items-center gap-2">
            <span class="w-8 h-8 bg-amber-100 rounded-lg flex items-center justify-center">⚠️</span>
            待改进项
          </h3>
          <div class="space-y-3">
            <div
              v-for="(issue, idx) in reportData.unresolvedIssues"
              :key="idx"
              class="p-3 bg-amber-50 rounded-xl flex items-start gap-3"
            >
              <span class="text-amber-500">•</span>
              <p class="text-amber-700">{{ issue }}</p>
            </div>
          </div>
        </div>

        <!-- Next Action Section -->
        <div class="clay-card p-6 animate-fade-in stagger-4">
          <h3 class="text-xl font-heading font-semibold text-primary-dark mb-4 flex items-center gap-2">
            <span class="w-8 h-8 bg-accent/20 rounded-lg flex items-center justify-center">🚀</span>
            下一步建议
          </h3>

          <!-- If next action already in report -->
          <div v-if="reportData.nextAction && !showNextAction" class="space-y-4">
            <div class="p-4 bg-gradient-to-r from-primary/10 to-accent/10 rounded-xl border-2 border-primary/20">
              <p class="font-heading font-medium text-primary-dark mb-2">
                📍 推荐方向：{{ reportData.nextAction.actionType }}
              </p>
              <p class="text-gray-700">
                {{ reportData.nextAction.reason }}
              </p>
            </div>

            <div v-if="reportData.nextAction.nextEntryPoint" class="p-4 bg-gray-50 rounded-xl">
              <p class="text-gray-600">
                <span class="font-medium">💡 建议入口：</span>
                {{ reportData.nextAction.nextEntryPoint }}
              </p>
            </div>

            <div v-if="reportData.nextAction.requiresReplan" class="p-4 bg-amber-50 rounded-xl border-l-4 border-amber-400">
              <p class="text-amber-700">
                ⚠️ 需要重新规划学习路径
              </p>
            </div>

            <button @click="startNewSession" class="btn-primary-clay w-full text-lg flex items-center justify-center gap-2">
              <span>🚀 开始新一轮学习</span>
            </button>
          </div>

          <!-- Button to request next action -->
          <div v-else-if="!showNextAction">
            <button
              @click="requestNextAction"
              :disabled="requesting"
              :class="[
                'btn-primary-clay w-full text-lg flex items-center justify-center gap-2',
                requesting && 'opacity-50 cursor-not-allowed'
              ]"
            >
              <svg v-if="requesting" class="animate-spin h-6 w-6" fill="none" viewBox="0 0 24 24">
                <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
                <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8v4l5-5-5-5v4a10 10 0 00-10 10h2z"></path>
              </svg>
              <span>{{ requesting ? '分析中...' : '✨ 获取下一步建议' }}</span>
            </button>
          </div>

          <!-- Display next action after API call -->
          <div v-else class="space-y-4">
            <div class="p-4 bg-gradient-to-r from-primary/10 to-accent/10 rounded-xl border-2 border-primary/20">
              <p class="font-heading font-medium text-primary-dark mb-2">
                📍 推荐方向：{{ nextActionData.actionType }}
              </p>
              <p class="text-gray-700">
                {{ nextActionData.reason }}
              </p>
            </div>

            <div v-if="nextActionData.nextEntryPoint" class="p-4 bg-gray-50 rounded-xl">
              <p class="text-gray-600">
                <span class="font-medium">💡 建议入口：</span>
                {{ nextActionData.nextEntryPoint }}
              </p>
            </div>

            <div v-if="nextActionData.requiresReplan" class="p-4 bg-amber-50 rounded-xl border-l-4 border-amber-400">
              <p class="text-amber-700">
                ⚠️ 需要重新规划学习路径
              </p>
            </div>

            <button @click="startNewSession" class="btn-primary-clay w-full text-lg flex items-center justify-center gap-2">
              <span>🚀 开始新一轮学习</span>
            </button>
          </div>
        </div>

        <div v-if="error" class="p-4 bg-red-100 border-2 border-red-300 rounded-xl text-red-600 text-sm">
          ⚠️ {{ error }}
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useLearningFlowStore } from '@/stores/learningFlow'
import { sessionApi } from '@/api/goal'

const router = useRouter()
const store = useLearningFlowStore()

const loading = ref(true)
const requesting = ref(false)
const error = ref('')
const reportData = ref(null)
const showNextAction = ref(false)
const nextActionData = ref(null)

onMounted(async () => {
  await loadReport()
})

function formatCompletedProgress(completedProgress) {
  if (!completedProgress || !Array.isArray(completedProgress)) {
    return '暂无进度信息'
  }
  return completedProgress.join('，')
}

async function loadReport() {
  loading.value = true
  error.value = ''

  try {
    const response = await sessionApi.getReport(store.sessionId)
    const data = response.data

    if (data.code === 'OK') {
      reportData.value = data.data.learningReport
      store.setReport(data.data)
      if (reportData.value.nextAction) {
        showNextAction.value = true
        nextActionData.value = reportData.value.nextAction
      }
    } else {
      error.value = data.message || '加载报告失败'
    }
  } catch (err) {
    error.value = err.response?.data?.message || '网络错误，请重试'
  } finally {
    loading.value = false
  }
}

async function requestNextAction() {
  requesting.value = true
  error.value = ''

  try {
    const actionType = reportData.value?.nextAction?.actionType || 'CONTINUE'

    const response = await sessionApi.requestNextAction(store.sessionId, actionType)
    const data = response.data

    if (data.code === 'OK') {
      nextActionData.value = data.data
      store.setNextAction(data.data)
      showNextAction.value = true
    } else {
      error.value = data.message || '获取建议失败'
    }
  } catch (err) {
    error.value = err.response?.data?.message || '网络错误，请重试'
  } finally {
    requesting.value = false
  }
}

function startNewSession() {
  store.resetAll()
  router.push({ name: 'GoalInput' })
}
</script>
