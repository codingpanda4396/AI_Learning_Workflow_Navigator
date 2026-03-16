<template>
  <div class="min-h-screen py-8 px-4 relative overflow-hidden">
    <!-- Floating Background -->
    <div class="fixed inset-0 pointer-events-none">
      <div class="absolute top-20 right-10 w-28 h-28 bg-accent/10 rounded-full floating"></div>
      <div class="absolute bottom-1/3 left-20 w-24 h-24 bg-primary/10 rounded-full floating floating-delay-2"></div>
    </div>

    <div class="max-w-3xl mx-auto relative z-10">
      <!-- Header -->
      <div class="text-center mb-8 animate-fade-in">
        <div class="inline-flex items-center justify-center w-16 h-16 bg-gradient-to-br from-accent to-green-400 rounded-2xl shadow-float mb-3">
          <svg class="w-8 h-8 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2m-3 7h3m-3 4h3m-6-4h.01M9 16h.01"></path>
          </svg>
        </div>
        <h1 class="text-3xl font-heading font-bold gradient-text mb-2">
          学习规划
        </h1>
        <p class="text-primary/70">
          为你定制的个性化学习路径 🎯
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
        <p class="text-xl font-heading text-primary-dark">正在生成学习规划...</p>
        <p class="text-sm text-gray-500 mt-2">为你精心准备中 ✨</p>
      </div>

      <!-- Error State -->
      <div v-else-if="error" class="clay-card p-8 text-center">
        <div class="w-16 h-16 bg-red-100 rounded-full flex items-center justify-center mx-auto mb-4">
          <svg class="w-8 h-8 text-red-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"></path>
          </svg>
        </div>
        <p class="text-red-600 mb-4 text-lg">{{ error }}</p>
        <button @click="previewPlan" class="btn-clay text-primary">🔄 点击重试</button>
      </div>

      <!-- Plan Content -->
      <div v-else-if="planData" class="space-y-6">
        <!-- Strategy Card -->
        <div class="clay-card p-6 animate-fade-in hover-lift">
          <h3 class="text-xl font-heading font-semibold text-primary-dark mb-4 flex items-center gap-2">
            <span class="w-8 h-8 bg-primary/10 rounded-lg flex items-center justify-center text-sm">🤔</span>
            为什么这样规划
          </h3>

          <div class="grid md:grid-cols-2 gap-4">
            <div class="p-4 bg-primary/5 rounded-xl">
              <p class="text-sm text-gray-500 mb-1">📋 推荐策略</p>
              <p class="font-heading font-semibold text-lg text-gray-800">
                {{ planData.recommendedStrategy?.label || '-' }}
              </p>
            </div>
            <div class="p-4 bg-primary/5 rounded-xl">
              <p class="text-sm text-gray-500 mb-1">💭 推荐理由</p>
              <p class="font-body text-gray-700 text-sm">
                {{ planData.recommendedStrategy?.reason || '-' }}
              </p>
            </div>
          </div>

          <div v-if="planData.keyEvidence?.length" class="mt-4 p-4 bg-primary-light/10 rounded-xl">
            <p class="text-sm font-medium text-primary-dark mb-2">📊 关键证据</p>
            <ul class="text-sm text-gray-600 space-y-1">
              <li v-for="(evidence, idx) in planData.keyEvidence" :key="idx">• {{ evidence }}</li>
            </ul>
          </div>

          <div v-if="planData.risks?.length" class="mt-4 p-4 bg-amber-50 rounded-xl border-l-4 border-amber-400">
            <p class="text-sm font-medium text-amber-700 mb-2">⚠️ 潜在风险</p>
            <ul class="text-sm text-amber-600 space-y-1">
              <li v-for="(risk, idx) in planData.risks" :key="idx">• {{ risk }}</li>
            </ul>
          </div>
        </div>

        <!-- Entry Card -->
        <div v-if="planData.recommendedEntry" class="clay-card p-6 animate-fade-in stagger-1 hover-lift">
          <h3 class="text-xl font-heading font-semibold text-primary-dark mb-4 flex items-center gap-2">
            <span class="w-8 h-8 bg-accent/20 rounded-lg flex items-center justify-center text-sm">🚀</span>
            从这里开始
          </h3>

          <div class="p-6 bg-gradient-to-r from-primary/10 to-accent/10 rounded-2xl border-2 border-primary/20">
            <div class="flex items-start gap-4">
              <div class="w-14 h-14 bg-gradient-to-br from-primary to-accent rounded-xl flex items-center justify-center flex-shrink-0 shadow-float">
                <svg class="w-7 h-7 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M14.752 11.168l-3.197-2.132A1 1 0 0010 9.87v4.263a1 1 0 001.555.832l3.197-2.132a1 1 0 000-1.664z"></path>
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 12a9 9 0 11-18 0 9 9 0 0118 0z"></path>
                </svg>
              </div>
              <div class="flex-1">
                <h4 class="font-heading font-semibold text-xl text-gray-800">
                  {{ planData.recommendedEntry.title || '推荐入口' }}
                </h4>
                <p class="text-gray-600 mt-1">
                  ⏱️ 预计约 {{ planData.recommendedEntry.estimatedMinutes || '-' }} 分钟
                </p>
                <p class="text-gray-500 mt-2 text-sm">
                  {{ planData.recommendedEntry.reason || '' }}
                </p>
              </div>
            </div>
          </div>
        </div>

        <!-- Stages -->
        <div class="clay-card p-6 animate-fade-in stagger-2">
          <h3 class="text-xl font-heading font-semibold text-primary-dark mb-4 flex items-center gap-2">
            <span class="w-8 h-8 bg-primary-light/20 rounded-lg flex items-center justify-center text-sm">📚</span>
            学习路径
          </h3>

          <div class="space-y-4">
            <div
              v-for="(stage, sIndex) in planData.stages"
              :key="sIndex"
              class="relative"
            >
              <!-- Connector Line -->
              <div v-if="sIndex < planData.stages.length - 1" class="absolute left-5 top-14 w-0.5 h-8 bg-primary/30"></div>

              <div class="flex items-start gap-4 p-4 bg-gray-50 rounded-xl hover:bg-primary/5 transition-colors">
                <div class="w-10 h-10 bg-gradient-to-br from-primary to-primary-light rounded-xl flex items-center justify-center flex-shrink-0 shadow-float">
                  <span class="font-heading font-bold text-white">{{ sIndex + 1 }}</span>
                </div>
                <div class="flex-1">
                  <h4 class="font-heading font-semibold text-gray-800">{{ stage.title }}</h4>
                  <p class="text-sm text-gray-500 mt-1">{{ stage.objective }}</p>
                  <div class="flex items-center gap-2 mt-2">
                    <span class="text-xs px-2 py-1 bg-primary/10 text-primary rounded-full">
                      ⏱️ {{ stage.estimatedMinutes || 0 }} 分钟
                    </span>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- Success Criteria -->
        <div class="clay-card p-6 animate-fade-in stagger-3">
          <h3 class="text-xl font-heading font-semibold text-primary-dark mb-4 flex items-center gap-2">
            <span class="w-8 h-8 bg-accent/20 rounded-lg flex items-center justify-center text-sm">🎯</span>
            本次达成标准
          </h3>
          <div class="p-4 bg-accent/10 rounded-xl border-l-4 border-accent">
            <ul class="space-y-2">
              <li v-for="(criteria, idx) in planData.successCriteria" :key="idx" class="flex items-start gap-2">
                <span class="text-accent">✓</span>
                <span class="text-gray-700">{{ criteria }}</span>
              </li>
            </ul>
          </div>
        </div>

        <!-- Commit Button -->
        <button
          @click="commitPlan"
          :disabled="committing"
          :class="[
            'btn-primary-clay w-full text-xl flex items-center justify-center gap-2 py-5',
            committing && 'opacity-50 cursor-not-allowed'
          ]"
        >
          <svg v-if="committing" class="animate-spin h-6 w-6" fill="none" viewBox="0 0 24 24">
            <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
            <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8v4l5-5-5-5v4a10 10 0 00-10 10h2z"></path>
          </svg>
          <span>{{ committing ? '启动中...' : '🚀 开始执行这份计划' }}</span>
        </button>
      </div>

      <div v-if="error" class="mt-4 p-4 bg-red-100 border-2 border-red-300 rounded-xl text-red-600 text-sm">
        ⚠️ {{ error }}
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
