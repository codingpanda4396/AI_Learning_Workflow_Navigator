<template>
  <PageContainer>
    <AppTopBar current="diagnosis" />
    <main class="mx-auto max-w-2xl px-6 py-8">
      <div class="mb-8">
        <h1 class="text-2xl font-bold text-text-primary md:text-3xl">
          个性化诊断
        </h1>
        <p class="mt-2 text-text-secondary">
          请回答以下问题，系统将据此生成适合你的学习计划。
        </p>
      </div>

      <LoadingState v-if="loading && !questions.length" message="正在生成诊断题目..." />
      <ErrorState v-else-if="error" :message="error">
        <template #action>
          <SecondaryButton @click="fetchQuestions">重试</SecondaryButton>
        </template>
      </ErrorState>

      <form v-else class="space-y-6" @submit.prevent="onSubmit">
        <FormCard v-for="(q, idx) in questions" :key="q.questionId" class="space-y-4">
          <div class="flex items-baseline gap-2">
            <span class="text-sm font-medium text-primary">第 {{ idx + 1 }} 题</span>
            <span v-if="q.required" class="text-red-500">*</span>
          </div>
          <h3 class="text-lg font-medium text-text-primary">{{ q.title }}</h3>
          <p v-if="q.whyAsking" class="text-sm text-text-secondary">
            {{ q.whyAsking }}
          </p>
          <div class="space-y-2 pt-2">
            <label
              v-for="opt in q.options"
              :key="opt.code"
              class="flex cursor-pointer items-start gap-3 rounded-input border p-4 transition-colors"
              :class="
                isSelected(q.questionId, opt.code)
                  ? 'border-primary bg-primary/5'
                  : 'border-border hover:border-primary/50'
              "
            >
              <input
                :type="q.type === 'MULTI_CHOICE' ? 'checkbox' : 'radio'"
                :name="q.questionId"
                :value="opt.code"
                :checked="isSelected(q.questionId, opt.code)"
                class="mt-1 h-4 w-4 rounded border-border text-primary focus:ring-primary"
                @change="onSelect(q.questionId, opt.code, q.type === 'MULTI_CHOICE')"
              />
              <span class="text-text-primary">{{ opt.label }}</span>
            </label>
          </div>
        </FormCard>

        <div class="flex justify-end">
          <PrimaryButton :loading="submitting" type="submit">
            提交诊断
          </PrimaryButton>
        </div>
      </form>
    </main>
  </PageContainer>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import PageContainer from '@/components/layout/PageContainer.vue'
import AppTopBar from '@/components/layout/AppTopBar.vue'
import FormCard from '@/components/ui/FormCard.vue'
import PrimaryButton from '@/components/ui/PrimaryButton.vue'
import SecondaryButton from '@/components/ui/SecondaryButton.vue'
import LoadingState from '@/components/ui/LoadingState.vue'
import ErrorState from '@/components/ui/ErrorState.vue'
import { useWorkflowStore } from '@/stores/workflow'
import { createSession, submitDiagnosis } from '@/api/diagnosis'
import { showToast } from '@/stores/toast'
import { getErrorMessage } from '@/api/request'
import type { DiagnosisQuestion, DiagnosisAnswer } from '@/types/dto'

const router = useRouter()
const store = useWorkflowStore()

const loading = ref(true)
const submitting = ref(false)
const error = ref<string | null>(null)
const questions = ref<DiagnosisQuestion[]>([])
const answers = ref<Record<string, string[]>>({})

function isSelected(questionId: string, code: string): boolean {
  return answers.value[questionId]?.includes(code) ?? false
}

function onSelect(questionId: string, code: string, multi: boolean) {
  const arr = answers.value[questionId] ?? []
  if (multi) {
    if (arr.includes(code)) {
      answers.value[questionId] = arr.filter((c) => c !== code)
    } else {
      answers.value[questionId] = [...arr, code]
    }
  } else {
    answers.value[questionId] = [code]
  }
}

async function fetchQuestions() {
  if (!store.goalId) return
  loading.value = true
  error.value = null
  try {
    const data = await createSession(store.goalId)
    store.diagnosisId = data.diagnosisId
    store.sessionId = data.sessionId
    questions.value = data.questions
    answers.value = {}
  } catch (err) {
    error.value = getErrorMessage(err)
  } finally {
    loading.value = false
  }
}

async function onSubmit() {
  const required = questions.value.filter((q) => q.required)
  for (const q of required) {
    if (!answers.value[q.questionId]?.length) {
      showToast(`请回答：${q.title}`)
      return
    }
  }
  const ans: DiagnosisAnswer[] = Object.entries(answers.value).map(
    ([questionId, selectedOptions]) => ({ questionId, selectedOptions })
  )
  submitting.value = true
  try {
    const data = await submitDiagnosis(store.diagnosisId!, ans)
    store.learnerProfileSnapshot = data.learnerProfileSnapshot
    store.diagnosisEvidenceSummary = data.diagnosisEvidenceSummary
    router.push('/plan')
  } catch (err) {
    showToast(getErrorMessage(err))
  } finally {
    submitting.value = false
  }
}

onMounted(() => {
  if (store.diagnosisId && questions.value.length) return
  fetchQuestions()
})
</script>
