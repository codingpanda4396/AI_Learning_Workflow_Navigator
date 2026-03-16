<template>
  <div class="min-h-screen py-8 px-4">
    <div class="max-w-3xl mx-auto">
      <!-- Header -->
      <div class="text-center mb-8 animate-fade-in">
        <h1 class="text-2xl font-heading font-bold text-primary-dark mb-2">
          学习规划
        </h1>
        <p class="text-gray-600 text-sm">
          基于你的目标和诊断结果生成的个性化学习路径
        </p>
      </div>

      <!-- Loading State -->
      <div v-if="loading" class="glass-card p-8 text-center animate-fade-in">
        <svg class="animate-spin h-10 w-10 mx-auto text-primary" fill="none" viewBox="0 0 24 24">
          <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
          <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
        </svg>
        <p class="mt-4 text-gray-600">正在生成学习规划...</p>
      </div>

      <!-- Error State -->
      <div v-else-if="error" class="glass-card p-8 text-center">
        <p class="text-red-600 mb-4">{{ error }}</p>
        <button @click="previewPlan" class="btn-secondary">
          重试
        </button>
      </div>

      <!-- Plan Content -->
      <div v-else-if="planData" class="space-y-6">
        <!-- Strategy Card -->
        <div class="glass-card p-6 animate-fade-in">
          <h3 class="text-lg font-heading font-semibold text-primary-dark mb-4">
            为什么这样规划
          </h3>

          <div class="grid md:grid-cols-2 gap-4">
            <div class="p-4 bg-white/60 rounded-xl">
              <p class="text-sm text-gray-500 mb-1">推荐策略</p>
              <p class="font-medium text-gray-800">
                {{ planData.recommendedStrategy?.label || '-' }}
              </p>
            </div>
            <div class="p-4 bg-white/60 rounded-xl">
              <p class="text-sm text-gray-500 mb-1">推荐理由</p>
              <p class="font-medium text-gray-800 text-sm">
                {{ planData.recommendedStrategy?.reason || '-' }}
              </p>
            </div>
          </div>

          <div v-if="planData.keyEvidence?.length" class="mt-4 p-4 bg-white/60 rounded-xl">
            <p class="text-sm text-gray-500 mb-2">关键证据</p>
            <ul class="text-sm text-gray-700 space-y-1">
              <li v-for="(evidence, idx) in planData.keyEvidence" :key="idx">
                • {{ evidence }}
              </li>
            </ul>
          </div>

          <div v-if="planData.risks?.length" class="mt-4 p-4 bg-amber-50 border border-amber-200 rounded-xl">
            <p class="text-sm text-amber-700 font-medium mb-2">潜在风险</p>
            <ul class="text-sm text-amber-600 space-y-1">
              <li v-for="(risk, idx) in planData.risks" :key="idx">• {{ risk }}</li>
            </ul>
          </div>
        </div>

        <!-- Entry Card -->
        <div v-if="planData.recommendedEntry" class="glass-card p-6 animate-fade-in stagger-1">
          <h3 class="text-lg font-heading font-semibold text-primary-dark mb-4">
            从这里开始
          </h3>

          <div class="p-4 bg-gradient-to-r from-primary/10 to-primary-light/10 rounded-xl border border-primary/20">
            <div class="flex items-start gap-4">
              <div class="w-12 h-12 bg-primary rounded-xl flex items-center justify-center flex-shrink-0">
                <svg class="w-6 h-6 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M14.752 11.168l-3.197-2.132A1 1 0 0010 9.87v4.263a1 1 0 001.555.832l3.197-2.132a1 1 0 000-1.664z"></path>
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 12a9 9 0 11-18 0 9 9 0 0118 0z"></path>
                </svg>
              </div>
              <div>
                <h4 class="font-medium text-gray-800">
                  {{ planData.recommendedEntry.title || '推荐入口' }}
                </h4>
                <p class="text-sm text-gray-600 mt-1">
                  预计约 {{ planData.recommendedEntry.estimatedMinutes || '-' }} 分钟
                </p>
                <p class="text-sm text-gray-500 mt-2">
                  {{ planData.recommendedEntry.reason || '' }}
                </p>
              </div>
            </div>
          </div>
        </div>

        <!-- Stages & Tasks -->
        <div class="glass-card p-6 animate-fade-in stagger-2">
          <h3 class="text-lg font-heading font-semibold text-primary-dark mb-4">
            学习路径
          </h3>

          <div class="space-y-4">
            <div
              v-for="(stage, sIndex) in planData.stages"
              :key="sIndex"
              class="border border-gray-200 rounded-xl overflow-hidden"
            >
              <div class="p-4 bg-gray-50 flex items-center gap-3">
                <span class="w-8 h-8 bg-primary text-white rounded-lg flex items-center justify-center text-sm font-medium">
                  {{ sIndex + 1 }}
                </span>
                <div class="flex-1">
                  <span class="font-medium text-gray-800">{{ stage.title }}</span>
                  <span class="text-sm text-gray-500 ml-2">({{ stage.estimatedMinutes || 0 }} 分钟)</span>
                </div>
              </div>

              <div class="p-4">
                <p class="text-sm text-gray-600 mb-3">{{ stage.objective }}</p>
              </div>
            </div>
          </div>
        </div>

        <!-- Tasks List -->
        <div v-if="planData.tasks?.length" class="glass-card p-6 animate-fade-in stagger-2">
          <h3 class="text-lg font-heading font-semibold text-primary-dark mb-4">
            任务列表
          </h3>

          <div class="space-y-3">
            <div
              v-for="(task, tIndex) in planData.tasks"
              :key="tIndex"
              class="p-4 bg-white/60 rounded-xl flex items-start gap-3"
            >
              <div class="w-6 h-6 rounded-full bg-primary/20 flex items-center justify-center flex-shrink-0 mt-0.5">
                <span class="text-xs text-primary">{{ tIndex + 1 }}</span>
              </div>
              <div>
                <p class="text-sm font-medium text-gray-800">{{ task.title }}</p>
                <p class="text-xs text-gray-500 mt-0.5">{{ task.taskType }}</p>
              </div>
            </div>
          </div>
        </div>

        <!-- Success Criteria -->
        <div class="glass-card p-6 animate-fade-in stagger-3">
          <h3 class="text-lg font-heading font-semibold text-primary-dark mb-4">
            本次达成标准
          </h3>
          <div class="p-4 bg-accent/10 border border-accent/20 rounded-xl">
            <ul class="text-sm text-gray-700 space-y-2">
              <li v-for="(criteria, idx) in planData.successCriteria" :key="idx">
                • {{ criteria }}
              </li>
            </ul>
          </div>
        </div>

        <!-- Commit Button -->
        <button
          @click="commitPlan"
          :disabled="committing"
          :class="[
            'w-full btn-primary flex items-center justify-center gap-2 py-4 text-lg',
            committing && 'opacity-50 cursor-not-allowed'
          ]"
        >
          <svg v-if="committing" class="animate-spin h-5 w-5" fill="none" viewBox="0 0 24 24">
            <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
            <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
          </svg>
          <span>{{ committing ? '启动中...' : '开始执行这份计划' }}</span>
        </button>
      </div>

      <!-- Error Message -->
      <div v-if="error" class="mt-4 p-4 bg-red-50 border border-red-200 rounded-xl text-red-600 text-sm">
        {{ error }}
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useLearningFlowStore } from '@/stores/learningFlow'
import { learningPlanApi } from '@/api/goal'

const router = useRouter()
const store = useLearningFlowStore()

const loading = ref(true)
const committing = ref(false)
const error = ref('')
const planData = ref(null)

onMounted(async () => {
  await previewPlan()
})

async function previewPlan() {
  loading.value = true
  error.value = ''

  try {
    const response = await learningPlanApi.preview(store.goalId, store.diagnosisId)
    const data = response.data

    if (data.code === 'OK') {
      planData.value = data.data
      store.setPlanPreview(data.data)
    } else {
      error.value = data.message || '生成规划失败'
    }
  } catch (err) {
    error.value = err.response?.data?.message || '网络错误，请重试'
  } finally {
    loading.value = false
  }
}

async function commitPlan() {
  if (!planData.value?.planId) return

  committing.value = true
  error.value = ''

  try {
    const response = await learningPlanApi.commit(planData.value.planId)
    const data = response.data

    if (data.code === 'OK') {
      store.setCommittedSession(data.data)
      router.push({ name: 'TaskRun' })
    } else {
      error.value = data.message || '启动计划失败'
    }
  } catch (err) {
    error.value = err.response?.data?.message || '网络错误，请重试'
  } finally {
    committing.value = false
  }
}
</script>
