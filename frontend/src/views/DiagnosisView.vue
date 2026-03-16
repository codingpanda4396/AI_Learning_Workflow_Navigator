<template>
  <div class="min-h-screen py-8 px-4">
    <div class="max-w-2xl mx-auto">
      <!-- Header -->
      <div class="text-center mb-8 animate-fade-in">
        <h1 class="text-2xl font-heading font-bold text-primary-dark mb-2">
          学习诊断
        </h1>
        <p class="text-gray-600 text-sm">
          为了更好地为你规划学习路径，请回答几个问题
        </p>
      </div>

      <!-- Loading State -->
      <div v-if="loading" class="glass-card p-8 text-center animate-fade-in">
        <svg class="animate-spin h-10 w-10 mx-auto text-primary" fill="none" viewBox="0 0 24 24">
          <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
          <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
        </svg>
        <p class="mt-4 text-gray-600">正在生成诊断问题...</p>
      </div>

      <!-- Error State -->
      <div v-else-if="error" class="glass-card p-8 text-center">
        <p class="text-red-600 mb-4">{{ error }}</p>
        <button @click="createDiagnosisSession" class="btn-secondary">
          重试
        </button>
      </div>

      <!-- Questions State -->
      <div v-else-if="!submitted && questions.length > 0" class="space-y-6">
        <div
          v-for="(question, qIndex) in questions"
          :key="question.questionId"
          class="glass-card p-6 animate-fade-in"
          :style="{ animationDelay: `${qIndex * 0.1}s` }"
        >
          <h3 class="text-lg font-heading font-semibold text-gray-800 mb-2">
            {{ question.title }}
          </h3>
          <p v-if="question.description" class="text-sm text-gray-500 mb-4">
            {{ question.description }}
          </p>

          <!-- Single Choice -->
          <div v-if="question.multiSelect === false" class="space-y-2">
            <button
              v-for="option in question.options"
              :key="option.code"
              @click="selectAnswer(question.questionId, option.code, false)"
              :class="[
                'w-full p-3 rounded-xl text-left text-sm transition-all duration-200 cursor-pointer',
                answers[question.questionId]?.includes(option.code)
                  ? 'bg-primary text-white shadow-md'
                  : 'bg-white text-gray-700 border border-gray-200 hover:border-primary'
              ]"
            >
              {{ option.label }}
            </button>
          </div>

          <!-- Multi Choice -->
          <div v-else class="space-y-2">
            <button
              v-for="option in question.options"
              :key="option.code"
              @click="toggleMultiAnswer(question.questionId, option.code)"
              :class="[
                'w-full p-3 rounded-xl text-left text-sm transition-all duration-200 cursor-pointer',
                answers[question.questionId]?.includes(option.code)
                  ? 'bg-primary text-white shadow-md'
                  : 'bg-white text-gray-700 border border-gray-200 hover:border-primary'
              ]"
            >
              {{ option.label }}
            </button>
          </div>
        </div>

        <!-- Submit Button -->
        <button
          @click="submitAnswers"
          :disabled="!canSubmit || submitting"
          :class="[
            'w-full btn-primary flex items-center justify-center gap-2',
            (!canSubmit || submitting) && 'opacity-50 cursor-not-allowed'
          ]"
        >
          <svg v-if="submitting" class="animate-spin h-5 w-5" fill="none" viewBox="0 0 24 24">
            <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
            <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
          </svg>
          <span>{{ submitting ? '提交中...' : '生成学习画像' }}</span>
        </button>
      </div>

      <!-- Result Card -->
      <Transition name="slide">
        <div v-if="submitted && profileData" class="space-y-6">
          <!-- Result Summary -->
          <div class="glass-card p-6 animate-slide-in">
            <h3 class="text-lg font-heading font-semibold text-primary-dark mb-4">
              诊断完成！这是你的学习画像
            </h3>

            <div class="space-y-4">
              <div class="p-4 bg-white/60 rounded-xl">
                <p class="text-sm text-gray-500 mb-1">当前基础</p>
                <p class="font-medium text-gray-800">
                  {{ profileData.foundationLevel || '-' }}
                </p>
              </div>

              <div class="p-4 bg-white/60 rounded-xl">
                <p class="text-sm text-gray-500 mb-1">主要卡点</p>
                <p class="font-medium text-gray-800">
                  {{ formatList(profileData.blockerTags) || '-' }}
                </p>
              </div>

              <div class="p-4 bg-white/60 rounded-xl">
                <p class="text-sm text-gray-500 mb-1">推荐推进方式</p>
                <p class="font-medium text-gray-800">
                  {{ profileData.suggestedEntryStrategy || '-' }}
                </p>
              </div>

              <div v-if="profileData.riskTags?.length" class="p-4 bg-amber-50 border border-amber-200 rounded-xl">
                <p class="text-sm text-amber-700 font-medium mb-2">风险提示</p>
                <ul class="text-sm text-amber-600 space-y-1">
                  <li v-for="(risk, idx) in profileData.riskTags" :key="idx">
                    • {{ risk }}
                  </li>
                </ul>
              </div>
            </div>
          </div>

          <!-- Action Button -->
          <button
            @click="goToPlan"
            class="w-full btn-primary flex items-center justify-center gap-2"
          >
            <span>查看学习规划</span>
            <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7"></path>
            </svg>
          </button>
        </div>
      </Transition>

      <!-- Error Message -->
      <div v-if="error" class="mt-4 p-4 bg-red-50 border border-red-200 rounded-xl text-red-600 text-sm">
        {{ error }}
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
      // Initialize empty answers
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

function selectAnswer(questionId, optionCode, isMulti) {
  if (isMulti) {
    toggleMultiAnswer(questionId, optionCode)
  } else {
    answers.value[questionId] = [optionCode]
  }
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
.slide-enter-active,
.slide-leave-active {
  transition: all 0.4s ease;
}

.slide-enter-from,
.slide-leave-to {
  opacity: 0;
  transform: translateY(-10px);
}
</style>
