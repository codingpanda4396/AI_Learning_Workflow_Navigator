<template>
  <div class="min-h-screen py-8 px-4">
    <div class="max-w-3xl mx-auto">
      <!-- Header -->
      <div class="text-center mb-8 animate-fade-in">
        <h1 class="text-2xl font-heading font-bold text-primary-dark mb-2">
          学习报告
        </h1>
        <p class="text-gray-600 text-sm">
          恭喜完成本次学习！让我们一起看看你的收获
        </p>
      </div>

      <!-- Loading State -->
      <div v-if="loading" class="glass-card p-8 text-center animate-fade-in">
        <svg class="animate-spin h-10 w-10 mx-auto text-primary" fill="none" viewBox="0 0 24 24">
          <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
          <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
        </svg>
        <p class="mt-4 text-gray-600">生成报告中...</p>
      </div>

      <!-- Error State -->
      <div v-else-if="error" class="glass-card p-8 text-center">
        <p class="text-red-600 mb-4">{{ error }}</p>
        <button @click="loadReport" class="btn-secondary">
          重试
        </button>
      </div>

      <!-- Report Content -->
      <div v-else-if="reportData" class="space-y-6">
        <!-- Result Summary -->
        <div class="glass-card p-6 animate-fade-in">
          <div class="flex items-center gap-3 mb-4">
            <div class="w-12 h-12 bg-accent rounded-xl flex items-center justify-center">
              <svg class="w-6 h-6 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7"></path>
              </svg>
            </div>
            <div>
              <h3 class="text-lg font-heading font-semibold text-gray-800">
                {{ reportData.resultStatus === 'COMPLETED' ? '学习完成' : '部分完成' }}
              </h3>
              <p class="text-sm text-gray-500">
                {{ formatCompletedProgress(reportData.completedProgress) }}
              </p>
            </div>
          </div>

          <div class="p-4 bg-gray-50 rounded-xl">
            <p class="text-sm text-gray-600">
              {{ reportData.goalReview || '本次学习目标已达成' }}
            </p>
          </div>
        </div>

        <!-- Key Gains -->
        <div class="glass-card p-6 animate-fade-in stagger-1">
          <h3 class="text-lg font-heading font-semibold text-primary-dark mb-4">
            关键收获
          </h3>
          <div class="p-4 bg-white/60 rounded-xl">
            <p class="text-sm text-gray-700 whitespace-pre-line">
              {{ reportData.summaryText || '暂无总结' }}
            </p>
          </div>
        </div>

        <!-- Evidence Summary -->
        <div v-if="reportData.evidenceSummary?.length" class="glass-card p-6 animate-fade-in stagger-2">
          <h3 class="text-lg font-heading font-semibold text-primary-dark mb-4">
            证据与反馈
          </h3>
          <div class="p-4 bg-white/60 rounded-xl">
            <ul class="text-sm text-gray-700 space-y-2">
              <li v-for="(evidence, idx) in reportData.evidenceSummary" :key="idx">
                • {{ evidence }}
              </li>
            </ul>
          </div>
        </div>

        <!-- Unresolved Issues -->
        <div v-if="reportData.unresolvedIssues?.length" class="glass-card p-6 animate-fade-in stagger-3">
          <h3 class="text-lg font-heading font-semibold text-amber-700 mb-4">
            待改进项
          </h3>
          <div class="space-y-2">
            <div
              v-for="(issue, idx) in reportData.unresolvedIssues"
              :key="idx"
              class="p-3 bg-amber-50 border border-amber-200 rounded-xl flex items-start gap-3"
            >
              <svg class="w-5 h-5 text-amber-500 flex-shrink-0 mt-0.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z"></path>
              </svg>
              <p class="text-sm text-amber-700">{{ issue }}</p>
            </div>
          </div>
        </div>

        <!-- Next Action Section -->
        <div class="glass-card p-6 animate-fade-in stagger-4">
          <h3 class="text-lg font-heading font-semibold text-primary-dark mb-4">
            下一步建议
          </h3>

          <!-- If next action already in report -->
          <div v-if="reportData.nextAction && !showNextAction" class="space-y-4">
            <div class="p-4 bg-gradient-to-r from-primary/10 to-primary-light/10 rounded-xl border border-primary/20">
              <p class="text-sm font-medium text-primary-dark mb-2">
                推荐方向：{{ reportData.nextAction.actionType }}
              </p>
              <p class="text-sm text-gray-700">
                {{ reportData.nextAction.reason }}
              </p>
            </div>

            <div v-if="reportData.nextAction.nextEntryPoint" class="p-4 bg-white/60 rounded-xl">
              <p class="text-sm text-gray-600">
                <span class="font-medium">建议入口：</span>
                {{ reportData.nextAction.nextEntryPoint }}
              </p>
            </div>

            <div v-if="reportData.nextAction.requiresReplan" class="p-4 bg-amber-50 border border-amber-200 rounded-xl">
              <p class="text-sm text-amber-700">
                需要重新规划学习路径
              </p>
            </div>

            <button
              @click="startNewSession"
              class="w-full btn-primary flex items-center justify-center gap-2"
            >
              <span>开始新一轮学习</span>
            </button>
          </div>

          <!-- Button to request next action -->
          <div v-else-if="!showNextAction">
            <button
              @click="requestNextAction"
              :disabled="requesting"
              :class="[
                'w-full btn-primary flex items-center justify-center gap-2',
                requesting && 'opacity-50 cursor-not-allowed'
              ]"
            >
              <svg v-if="requesting" class="animate-spin h-5 w-5" fill="none" viewBox="0 0 24 24">
                <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
                <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
              </svg>
              <span>{{ requesting ? '分析中...' : '获取下一步建议' }}</span>
            </button>
          </div>

          <!-- Display next action after API call -->
          <div v-else class="space-y-4">
            <div class="p-4 bg-gradient-to-r from-primary/10 to-primary-light/10 rounded-xl border border-primary/20">
              <p class="text-sm font-medium text-primary-dark mb-2">
                推荐方向：{{ nextActionData.actionType }}
              </p>
              <p class="text-sm text-gray-700">
                {{ nextActionData.reason }}
              </p>
            </div>

            <div v-if="nextActionData.nextEntryPoint" class="p-4 bg-white/60 rounded-xl">
              <p class="text-sm text-gray-600">
                <span class="font-medium">建议入口：</span>
                {{ nextActionData.nextEntryPoint }}
              </p>
            </div>

            <div v-if="nextActionData.requiresReplan" class="p-4 bg-amber-50 border border-amber-200 rounded-xl">
              <p class="text-sm text-amber-700">
                需要重新规划学习路径
              </p>
            </div>

            <button
              @click="startNewSession"
              class="w-full btn-primary flex items-center justify-center gap-2"
            >
              <span>开始新一轮学习</span>
            </button>
          </div>
        </div>

        <!-- Error Message -->
        <div v-if="error" class="p-4 bg-red-50 border border-red-200 rounded-xl text-red-600 text-sm">
          {{ error }}
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
      // Check if nextAction is already in the report
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
    // Use actionType from report if available, otherwise default
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
