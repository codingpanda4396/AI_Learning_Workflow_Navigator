<template>
  <div class="min-h-screen py-8 px-4 relative overflow-hidden">
    <!-- Floating Background Elements -->
    <div class="fixed inset-0 pointer-events-none overflow-hidden">
      <div class="absolute top-20 left-10 w-24 h-24 bg-primary/10 rounded-full floating"></div>
      <div class="absolute top-40 right-20 w-16 h-16 bg-accent/20 rounded-full floating floating-delay-1"></div>
      <div class="absolute bottom-40 left-1/4 w-20 h-20 bg-primary-light/15 rounded-full floating floating-delay-2"></div>
      <div class="absolute bottom-20 right-1/3 w-12 h-12 bg-accent/10 rounded-full floating floating-delay-3"></div>
    </div>

    <div class="max-w-2xl mx-auto relative z-10">
      <!-- Header -->
      <div class="text-center mb-10 animate-fade-in">
        <div class="inline-flex items-center justify-center w-20 h-20 bg-gradient-to-br from-primary to-primary-light rounded-2xl shadow-float mb-4">
          <svg class="w-10 h-10 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 6.253v13m0-13C10.832 5.477 9.246 5 7.5 5S4.168 5.477 3 6.253v13C4.168 18.477 5.754 18 7.5 18s3.332.477 4.5 1.253m0-13C13.168 5.477 14.754 5 16.5 5c1.747 0 3.332.477 4.5 1.253v13C19.832 18.477 18.247 18 16.5 18c-1.746 0-3.332.477-4.5 1.253"></path>
          </svg>
        </div>
        <h1 class="text-4xl font-heading font-bold gradient-text mb-3">
          AI 学习导航
        </h1>
        <p class="text-lg text-primary/70">
          让我们一起规划你的学习路径 ✨
        </p>
      </div>

      <!-- Form Card -->
      <div class="clay-card p-8 animate-fade-in stagger-1">
        <h2 class="text-2xl font-heading font-semibold text-primary-dark mb-6 flex items-center gap-2">
          <span class="w-8 h-8 bg-primary/10 rounded-lg flex items-center justify-center text-sm">1</span>
          输入你的学习目标
        </h2>

        <form @submit.prevent="handleSubmit" class="space-y-8">
          <!-- Raw Goal Text -->
          <div class="space-y-3">
            <label class="block text-lg font-heading font-medium text-gray-700">
              🎯 想学什么？ <span class="text-red-500">*</span>
            </label>
            <textarea
              v-model="form.rawGoalText"
              rows="3"
              class="input-clay resize-none text-lg"
              placeholder="例如：我想学数据结构，特别是栈和队列"
              required
            ></textarea>
          </div>

          <!-- Time Budget -->
          <div class="space-y-3">
            <label class="block text-lg font-heading font-medium text-gray-700">
              ⏰ 你有多少时间？ <span class="text-red-500">*</span>
            </label>
            <div class="grid grid-cols-2 md:grid-cols-3 gap-4">
              <button
                v-for="(option, idx) in timeBudgetOptions"
                :key="option.value"
                type="button"
                @click="form.timeBudget = option.value"
                :class="[
                  'option-clay text-center font-heading',
                  form.timeBudget === option.value && 'selected'
                ]"
              >
                <span class="text-2xl mb-1 block">{{ option.emoji }}</span>
                {{ option.label }}
              </button>
            </div>
          </div>

          <!-- Self Reported Level -->
          <div class="space-y-3">
            <label class="block text-lg font-heading font-medium text-gray-700">
              📚 你的基础水平？ <span class="text-red-500">*</span>
            </label>
            <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
              <button
                v-for="(option, idx) in levelOptions"
                :key="option.value"
                type="button"
                @click="form.selfReportedLevel = option.value"
                :class="[
                  'option-clay flex items-center gap-3',
                  form.selfReportedLevel === option.value && 'selected'
                ]"
              >
                <span class="text-2xl">{{ option.emoji }}</span>
                <span class="font-heading">{{ option.label }}</span>
              </button>
            </div>
          </div>

          <!-- Preference Tags -->
          <div class="space-y-3">
            <label class="block text-lg font-heading font-medium text-gray-700">
              💡 学习偏好（可选）
            </label>
            <div class="flex flex-wrap gap-3">
              <button
                v-for="tag in preferenceTags"
                :key="tag.value"
                type="button"
                @click="toggleTag(tag.value)"
                :class="[
                  'tag-clay',
                  form.preferenceTags.includes(tag.value) && 'selected'
                ]"
              >
                {{ tag.emoji }} {{ tag.label }}
              </button>
            </div>
          </div>

          <!-- Submit Button -->
          <div class="pt-4">
            <button
              type="submit"
              :disabled="loading || !isFormValid"
              :class="[
                'btn-primary-clay w-full text-xl flex items-center justify-center gap-3',
                (loading || !isFormValid) && 'opacity-50 cursor-not-allowed transform-none'
              ]"
            >
              <svg v-if="loading" class="animate-spin h-6 w-6" fill="none" viewBox="0 0 24 24">
                <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
                <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
              </svg>
              <span>{{ loading ? '提交中...' : '🚀 开始诊断' }}</span>
            </button>
          </div>
        </form>
      </div>

      <!-- Result Card -->
      <Transition name="pop">
        <div v-if="showResult && resultData" class="clay-card p-6 mt-6 animate-pop-in">
          <div class="flex items-center gap-4 mb-4">
            <div class="w-12 h-12 bg-accent rounded-xl flex items-center justify-center">
              <svg class="w-6 h-6 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7"></path>
              </svg>
            </div>
            <h3 class="text-xl font-heading font-semibold text-primary-dark">
              目标已创建！🎉
            </h3>
          </div>
          <div class="space-y-2 text-gray-600 ml-16">
            <p><span class="font-medium">目标类型：</span>{{ resultData.structuredGoal?.normalizedGoalText || '-' }}</p>
            <p><span class="font-medium">规划模式：</span>{{ resultData.goalContextSnapshot?.planningMode || '-' }}</p>
          </div>
          <p class="mt-4 text-sm text-gray-500 ml-16 animate-pulse">
            正在跳转到诊断页...
          </p>
        </div>
      </Transition>

      <!-- Error Message -->
      <div v-if="error" class="mt-4 p-4 bg-red-100 border-2 border-red-300 rounded-xl text-red-600 text-sm">
        ⚠️ {{ error }}
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
  { value: 'WITHIN_15_MIN', label: '15分钟', emoji: '⚡' },
  { value: 'WITHIN_30_MIN', label: '30分钟', emoji: '⏱️' },
  { value: 'WITHIN_60_MIN', label: '1小时', emoji: '🕐' },
  { value: 'MULTI_DAY', label: '多天', emoji: '📅' },
  { value: 'LONG_TERM', label: '长期', emoji: '🎯' }
]

const levelOptions = [
  { value: 'BEGINNER', label: '零基础', emoji: '🌱' },
  { value: 'BASIC', label: '学过一点', emoji: '📖' },
  { value: 'PARTIAL_UNDERSTANDING', label: '部分理解', emoji: '🔍' },
  { value: 'CAN_EXPLAIN_BUT_NOT_APPLY', label: '能说不会用', emoji: '💬' },
  { value: 'SOLID_BUT_WANT_IMPROVE', label: '想提升', emoji: '🚀' }
]

const preferenceTags = [
  { value: 'CONCEPT_FIRST', label: '概念优先', emoji: '💡' },
  { value: 'EXAMPLE_FIRST', label: '例子优先', emoji: '📝' },
  { value: 'PRACTICE_FIRST', label: '练习优先', emoji: '✏️' },
  { value: 'STEP_BY_STEP', label: '循序渐进', emoji: '🪜' }
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
