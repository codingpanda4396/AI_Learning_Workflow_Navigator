<template>
  <div class="min-h-screen py-8 px-4 relative overflow-hidden">
    <!-- Floating Background -->
    <div class="fixed inset-0 pointer-events-none">
      <div class="absolute top-1/4 left-10 w-32 h-32 bg-primary/5 rounded-full floating"></div>
      <div class="absolute top-1/3 right-20 w-20 h-20 bg-accent/10 rounded-full floating floating-delay-1"></div>
    </div>

    <div class="max-w-2xl mx-auto relative z-10">
      <!-- Header -->
      <div class="text-center mb-8 animate-fade-in">
        <div class="inline-flex items-center justify-center w-16 h-16 bg-gradient-to-br from-primary to-accent rounded-2xl shadow-float mb-3">
          <svg class="w-8 h-8 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2m-6 9l2 2 4-4"></path>
          </svg>
        </div>
        <h1 class="text-3xl font-heading font-bold gradient-text mb-2">
          学习诊断
        </h1>
        <p class="text-primary/70">
          回答几个问题，让我更好地了解你 📝
        </p>
      </div>

      <!-- Loading State -->
      <div v-if="loading" class="clay-card p-12 text-center animate-fade-in">
        <div class="relative w-24 h-24 mx-auto mb-6">
          <div class="absolute inset-0 bg-primary/20 rounded-full animate-ping"></div>
          <div class="relative w-full h-full bg-gradient-to-br from-primary to-primary-light rounded-full flex items-center justify-center">
            <svg class="animate-spin h-10 w-10 text-white" fill="none" viewBox="0 0 24 24">
              <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
              <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"></path>
            </svg>
          </div>
        </div>
        <p class="text-xl font-heading text-primary-dark">正在生成诊断问题...</p>
        <p class="text-sm text-gray-500 mt-2">请稍候</p>
      </div>

      <!-- Error State -->
      <div v-else-if="error" class="clay-card p-8 text-center">
        <div class="w-16 h-16 bg-red-100 rounded-full flex items-center justify-center mx-auto mb-4">
          <svg class="w-8 h-8 text-red-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"></path>
          </svg>
        </div>
        <p class="text-red-600 mb-4 text-lg">{{ error }}</p>
        <button @click="createDiagnosisSession" class="btn-clay text-primary">
          🔄 点击重试
        </button>
      </div>

      <!-- Questions State -->
      <div v-else-if="!submitted && questions.length > 0" class="space-y-6">
        <div
          v-for="(question, qIndex) in questions"
          :key="question.questionId"
          class="clay-card p-6 animate-fade-in"
          :style="{ animationDelay: `${qIndex * 0.15}s` }"
        >
          <div class="flex items-start gap-4 mb-4">
            <div class="w-10 h-10 bg-primary/10 rounded-xl flex items-center justify-center flex-shrink-0">
              <span class="font-heading font-bold text-primary">{{ qIndex + 1 }}</span>
            </div>
            <div>
              <h3 class="text-xl font-heading font-semibold text-gray-800">
                {{ question.title }}
              </h3>
              <p v-if="question.description" class="text-sm text-gray-500 mt-1">
                {{ question.description }}
              </p>
            </div>
          </div>

          <!-- Single Choice -->
          <div v-if="question.multiSelect === false" class="space-y-3">
            <button
              v-for="option in question.options"
              :key="option.code"
              @click="selectAnswer(question.questionId, option.code)"
              :class="[
                'option-clay flex items-center gap-3',
                answers[question.questionId]?.includes(option.code) && 'selected'
              ]"
            >
              <div :class="[
                'w-6 h-6 rounded-full border-2 flex items-center justify-center transition-all',
                answers[question.questionId]?.includes(option.code)
                  ? 'bg-primary border-primary'
                  : 'border-gray-300'
              ]">
                <svg v-if="answers[question.questionId]?.includes(option.code)" class="w-4 h-4 text-white" fill="currentColor" viewBox="0 0 20 20">
                  <path fill-rule="evenodd" d="M16.707 5.293a1 1 0 010 1.414l-8 8a1 1 0 01-1.414 0l-4-4a1 1 0 011.414-1.414L8 12.586l7.293-7.293a1 1 0 011.414 0z" clip-rule="evenodd"></path>
                </svg>
              </div>
              <span class="font-body">{{ option.label }}</span>
            </button>
          </div>

          <!-- Multi Choice -->
          <div v-else class="space-y-3">
            <button
              v-for="option in question.options"
              :key="option.code"
              @click="toggleMultiAnswer(question.questionId, option.code)"
              :class="[
                'option-clay flex items-center gap-3',
                answers[question.questionId]?.includes(option.code) && 'selected'
              ]"
            >
              <div :class="[
                'w-6 h-6 rounded-md border-2 flex items-center justify-center transition-all',
                answers[question.questionId]?.includes(option.code)
                  ? 'bg-primary border-primary'
                  : 'border-gray-300'
              ]">
                <svg v-if="answers[question.questionId]?.includes(option.code)" class="w-4 h-4 text-white" fill="currentColor" viewBox="0 0 20 20">
                  <path fill-rule="evenodd" d="M16.707 5.293a1 1 0 010 1.414l-8 8a1 1 0 01-1.414 0l-4-4a1 1 0 011.414-1.414L8 12.586l7.293-7.293a1 1 0 011.414 0z" clip-rule="evenodd"></path>
                </svg>
              </div>
              <span class="font-body">{{ option.label }}</span>
            </button>
          </div>
        </div>

        <!-- Submit Button -->
        <button
          @click="submitAnswers"
          :disabled="!canSubmit || submitting"
          :class="[
            'btn-primary-clay w-full text-xl flex items-center justify-center gap-2',
            (!canSubmit || submitting) && 'opacity-50 cursor-not-allowed'
          ]"
        >
          <svg v-if="submitting" class="animate-spin h-6 w-6" fill="none" viewBox="0 0 24 24">
            <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
            <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8v4l5-5-5-5v4a10 10 0 00-10 10h2z"></path>
          </svg>
          <span>{{ submitting ? '分析中...' : '✨ 生成学习画像' }}</span>
        </button>
      </div>

      <!-- Result Card -->
      <Transition name="pop">
        <div v-if="submitted && profileData" class="space-y-6">
          <div class="clay-card p-6 animate-pop-in">
            <div class="flex items-center gap-4 mb-6">
              <div class="w-16 h-16 bg-gradient-to-br from-accent to-green-400 rounded-2xl flex items-center justify-center shadow-float">
                <svg class="w-8 h-8 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z"></path>
                </svg>
              </div>
              <div>
                <h3 class="text-2xl font-heading font-bold text-primary-dark">
                  诊断完成！🎉
                </h3>
                <p class="text-gray-500">这是你的学习画像</p>
              </div>
            </div>

            <div class="space-y-4">
              <div class="p-4 bg-primary/5 rounded-xl border-l-4 border-primary">
                <p class="text-sm text-gray-500 mb-1">📚 当前基础</p>
                <p class="font-heading font-semibold text-lg text-gray-800">
                  {{ profileData.foundationLevel || '-' }}
                </p>
              </div>

              <div class="p-4 bg-accent/10 rounded-xl border-l-4 border-accent">
                <p class="text-sm text-gray-500 mb-1">🎯 主要卡点</p>
                <p class="font-heading font-semibold text-lg text-gray-800">
                  {{ formatList(profileData.blockerTags) || '-' }}
                </p>
              </div>

              <div class="p-4 bg-primary-light/20 rounded-xl border-l-4 border-primary-light">
                <p class="text-sm text-gray-500 mb-1">💡 推荐推进方式</p>
                <p class="font-heading font-semibold text-lg text-gray-800">
                  {{ profileData.suggestedEntryStrategy || '-' }}
                </p>
              </div>

              <div v-if="profileData.riskTags?.length" class="p-4 bg-amber-50 rounded-xl border-l-4 border-amber-400">
                <p class="text-sm text-amber-600 font-medium mb-2">⚠️ 风险提示</p>
                <ul class="text-sm text-amber-700 space-y-1">
                  <li v-for="(risk, idx) in profileData.riskTags" :key="idx">• {{ risk }}</li>
                </ul>
              </div>
            </div>
          </div>

          <button @click="goToPlan" class="btn-primary-clay w-full text-xl flex items-center justify-center gap-2">
            <span>查看学习规划</span>
            <svg class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 7l5 5m0 0l-5 5m5-5H6"></path>
            </svg>
          </button>
        </div>
      </Transition>

      <div v-if="error" class="mt-4 p-4 bg-red-100 border-2 border-red-300 rounded-xl text-red-600 text-sm">
        ⚠️ {{ error }}
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useLearningFlowStore } from '@/stores/learningFlow'
import { diagnosisApi } from '@/api/goal'

const router = useRouter()
const store = useLearningFlowStore()

const loading = ref(true)
const submitting = ref(false)
const submitted = ref(false)
const error = ref('')
const diagnosisId = ref('')
const questions = ref([])
const answers = ref({})
const profileData = ref(null)

const canSubmit = computed(() => {
  return questions.value.every(q => {
    const answer = answers.value[q.questionId]
    return answer && answer.length > 0
  })
})

onMounted(async () => {
  await createDiagnosisSession()
})

async function createDiagnosisSession() {
  loading.value = true
  error.value = ''

  try {
    const response = await diagnosisApi.createSession(store.goalId)
    const data = response.data

    if (data.code === 'OK') {
      diagnosisId.value = data.data.diagnosisId
      questions.value = data.data.questions || []
      questions.value.forEach(q => {
        answers.value[q.questionId] = []
      })
    } else {
      error.value = data.message || '创建诊断会话失败'
    }
  } catch (err) {
    error.value = err.response?.data?.message || '网络错误，请重试'
  } finally {
    loading.value = false
  }
}

function formatList(list) {
  if (!list || !Array.isArray(list)) return ''
  return list.join('、')
}

function selectAnswer(questionId, optionCode) {
  answers.value[questionId] = [optionCode]
}

function toggleMultiAnswer(questionId, optionCode) {
  if (!answers.value[questionId]) {
    answers.value[questionId] = []
  }
  const index = answers.value[questionId].indexOf(optionCode)
  if (index === -1) {
    answers.value[questionId].push(optionCode)
  } else {
    answers.value[questionId].splice(index, 1)
  }
}

async function submitAnswers() {
  if (!canSubmit.value) return

  submitting.value = true
  error.value = ''

  try {
    const formattedAnswers = Object.entries(answers.value).map(([questionId, selectedOptions]) => ({
      questionId,
      selectedOptions
    }))

    const response = await diagnosisApi.submitAnswers(diagnosisId.value, formattedAnswers)
    const data = response.data

    if (data.code === 'OK') {
      store.setDiagnosisResult(data.data)
      profileData.value = data.data.learnerProfileSnapshot
      submitted.value = true
    } else {
      error.value = data.message || '提交答案失败'
    }
  } catch (err) {
    error.value = err.response?.data?.message || '网络错误，请重试'
  } finally {
    submitting.value = false
  }
}

function goToPlan() {
  router.push({ name: 'Plan' })
}
</script>

<style scoped>
.pop-enter-active {
  animation: popIn 0.4s ease-out;
}

.pop-leave-active {
  transition: all 0.3s ease;
}

.pop-enter-from,
.pop-leave-to {
  opacity: 0;
  transform: scale(0.8);
}
</style>
