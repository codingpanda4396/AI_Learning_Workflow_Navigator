<template>
  <div class="min-h-screen py-8 px-4">
    <div class="max-w-2xl mx-auto">
      <!-- Header -->
      <div class="text-center mb-10 animate-fade-in">
        <h1 class="text-3xl font-heading font-bold text-primary-dark mb-3">
          AI 学习导航
        </h1>
        <p class="text-gray-600">
          让我们一起规划你的学习路径
        </p>
      </div>

      <!-- Form Card -->
      <div class="glass-card p-8 animate-fade-in stagger-1">
        <h2 class="text-xl font-heading font-semibold text-gray-800 mb-6">
          输入你的学习目标
        </h2>

        <form @submit.prevent="handleSubmit" class="space-y-6">
          <!-- Raw Goal Text -->
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-2">
              想学什么？ <span class="text-red-500">*</span>
            </label>
            <textarea
              v-model="form.rawGoalText"
              rows="3"
              class="input-field resize-none"
              placeholder="例如：我想学数据结构，特别是栈和队列"
              required
            ></textarea>
          </div>

          <!-- Time Budget -->
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-2">
              你有多少时间？ <span class="text-red-500">*</span>
            </label>
            <div class="grid grid-cols-2 md:grid-cols-3 gap-3">
              <button
                v-for="option in timeBudgetOptions"
                :key="option.value"
                type="button"
                @click="form.timeBudget = option.value"
                :class="[
                  'p-3 rounded-xl text-sm font-medium transition-all duration-200 cursor-pointer',
                  form.timeBudget === option.value
                    ? 'bg-primary text-white shadow-md'
                    : 'bg-white text-gray-600 border border-gray-200 hover:border-primary'
                ]"
              >
                {{ option.label }}
              </button>
            </div>
          </div>

          <!-- Self Reported Level -->
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-2">
              你的基础水平？ <span class="text-red-500">*</span>
            </label>
            <div class="grid grid-cols-1 md:grid-cols-2 gap-3">
              <button
                v-for="option in levelOptions"
                :key="option.value"
                type="button"
                @click="form.selfReportedLevel = option.value"
                :class="[
                  'p-3 rounded-xl text-sm font-medium transition-all duration-200 cursor-pointer',
                  form.selfReportedLevel === option.value
                    ? 'bg-primary text-white shadow-md'
                    : 'bg-white text-gray-600 border border-gray-200 hover:border-primary'
                ]"
              >
                {{ option.label }}
              </button>
            </div>
          </div>

          <!-- Preference Tags -->
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-2">
              学习偏好（可选）
            </label>
            <div class="flex flex-wrap gap-2">
              <button
                v-for="tag in preferenceTags"
                :key="tag.value"
                type="button"
                @click="toggleTag(tag.value)"
                :class="[
                  'px-4 py-2 rounded-full text-sm font-medium transition-all duration-200 cursor-pointer',
                  form.preferenceTags.includes(tag.value)
                    ? 'bg-primary-light text-white'
                    : 'bg-white text-gray-600 border border-gray-200 hover:border-primary'
                ]"
              >
                {{ tag.label }}
              </button>
            </div>
          </div>

          <!-- Submit Button -->
          <div class="pt-4">
            <button
              type="submit"
              :disabled="loading || !isFormValid"
              :class="[
                'w-full btn-primary flex items-center justify-center gap-2',
                (loading || !isFormValid) && 'opacity-50 cursor-not-allowed'
              ]"
            >
              <svg v-if="loading" class="animate-spin h-5 w-5" fill="none" viewBox="0 0 24 24">
                <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
                <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
              </svg>
              <span>{{ loading ? '提交中...' : '开始诊断' }}</span>
            </button>
          </div>
        </form>
      </div>

      <!-- Result Card (after submission) -->
      <Transition name="slide">
        <div v-if="showResult && resultData" class="glass-card p-6 mt-6 animate-slide-in">
          <h3 class="text-lg font-heading font-semibold text-primary-dark mb-4">
            目标已创建
          </h3>
          <div class="space-y-2 text-sm">
            <p><span class="font-medium">目标类型：</span>{{ resultData.structuredGoal?.normalizedGoalText || '-' }}</p>
            <p><span class="font-medium">规划模式：</span>{{ resultData.goalContextSnapshot?.planningMode || '-' }}</p>
            <p><span class="font-medium">时间约束：</span>{{ resultData.goalContextSnapshot?.timeConstraint || '-' }}</p>
          </div>
          <p class="mt-4 text-sm text-gray-500">
            正在跳转到诊断页...
          </p>
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
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useLearningFlowStore } from '@/stores/learningFlow'
import { goalApi } from '@/api/goal'

const router = useRouter()
const store = useLearningFlowStore()

const form = ref({
  rawGoalText: '',
  timeBudget: '',
  selfReportedLevel: '',
  preferenceTags: []
})

const loading = ref(false)
const error = ref('')
const showResult = ref(false)
const resultData = ref(null)

const timeBudgetOptions = [
  { value: 'WITHIN_15_MIN', label: '15分钟内' },
  { value: 'WITHIN_30_MIN', label: '30分钟内' },
  { value: 'WITHIN_60_MIN', label: '1小时内' },
  { value: 'MULTI_DAY', label: '多天' },
  { value: 'LONG_TERM', label: '长期' }
]

const levelOptions = [
  { value: 'BEGINNER', label: '零基础' },
  { value: 'BASIC', label: '学过一点' },
  { value: 'PARTIAL_UNDERSTANDING', label: '部分理解' },
  { value: 'CAN_EXPLAIN_BUT_NOT_APPLY', label: '能说不会用' },
  { value: 'SOLID_BUT_WANT_IMPROVE', label: '想提升' }
]

const preferenceTags = [
  { value: 'CONCEPT_FIRST', label: '概念优先' },
  { value: 'EXAMPLE_FIRST', label: '例子优先' },
  { value: 'PRACTICE_FIRST', label: '练习优先' },
  { value: 'STEP_BY_STEP', label: '循序渐进' }
]

const isFormValid = computed(() => {
  return form.value.rawGoalText.trim() &&
    form.value.timeBudget &&
    form.value.selfReportedLevel
})

function toggleTag(tag) {
  const index = form.value.preferenceTags.indexOf(tag)
  if (index === -1) {
    form.value.preferenceTags.push(tag)
  } else {
    form.value.preferenceTags.splice(index, 1)
  }
}

async function handleSubmit() {
  if (!isFormValid.value) return

  loading.value = true
  error.value = ''

  try {
    const response = await goalApi.createGoal(form.value)
    const data = response.data

    if (data.code === 'OK') {
      store.setGoalResult(data.data)
      resultData.value = data.data
      showResult.value = true

      setTimeout(() => {
        router.push({ name: 'Diagnosis' })
      }, 1500)
    } else {
      error.value = data.message || '创建目标失败'
    }
  } catch (err) {
    error.value = err.response?.data?.message || '网络错误，请重试'
  } finally {
    loading.value = false
  }
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
