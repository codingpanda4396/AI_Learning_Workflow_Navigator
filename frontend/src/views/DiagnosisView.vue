<template>
  <PageContainer>
    <AppTopBar current="diagnosis" />
    <main class="mx-auto max-w-2xl px-6 py-8">
      <LoadingState v-if="loading && !sessionReady" message="正在准备快速定位…" />
      <ErrorState v-else-if="error" :message="error">
        <template #action>
          <SecondaryButton @click="fetchSession">重试</SecondaryButton>
        </template>
      </ErrorState>

      <WorkflowPageScaffold v-else-if="sessionReady">
        <template #title>
          <h1 class="text-2xl font-bold text-text-primary md:text-3xl">
            三步快速定位
          </h1>
          <p class="mt-2 text-text-secondary">
            不用写小作文，选最贴近的一项即可；系统会据此生成你的学习路径。
          </p>
        </template>

        <template #primary>
          <form class="space-y-5" @submit.prevent="onSubmit">
            <FormCard class="space-y-3">
              <p class="text-sm font-semibold text-primary">第 1 题</p>
              <h3 class="text-base font-medium text-text-primary">
                你对这个内容现在大概处于什么状态？
              </h3>
              <div class="space-y-2 pt-1">
                <label
                  v-for="opt in foundationOptions"
                  :key="opt.id"
                  class="flex cursor-pointer items-start gap-3 rounded-input border p-3 text-sm transition-colors"
                  :class="
                    ui.foundation === opt.id
                      ? 'border-primary bg-primary/5'
                      : 'border-border hover:border-primary/50'
                  "
                >
                  <input
                    v-model="ui.foundation"
                    type="radio"
                    name="foundation"
                    :value="opt.id"
                    class="mt-0.5 h-4 w-4 border-border text-primary focus:ring-primary"
                  />
                  <span class="text-text-primary">{{ opt.label }}</span>
                </label>
              </div>
            </FormCard>

            <FormCard class="space-y-3">
              <p class="text-sm font-semibold text-primary">第 2 题</p>
              <h3 class="text-base font-medium text-text-primary">
                你现在最大的困难更像哪一种？
              </h3>
              <div class="space-y-2 pt-1">
                <label
                  v-for="opt in blockerOptions"
                  :key="opt.id"
                  class="flex cursor-pointer items-start gap-3 rounded-input border p-3 text-sm transition-colors"
                  :class="
                    ui.blocker === opt.id
                      ? 'border-primary bg-primary/5'
                      : 'border-border hover:border-primary/50'
                  "
                >
                  <input
                    v-model="ui.blocker"
                    type="radio"
                    name="blocker"
                    :value="opt.id"
                    class="mt-0.5 h-4 w-4 border-border text-primary focus:ring-primary"
                  />
                  <span class="text-text-primary">{{ opt.label }}</span>
                </label>
              </div>
            </FormCard>

            <FormCard class="space-y-3">
              <p class="text-sm font-semibold text-primary">第 3 题</p>
              <h3 class="text-base font-medium text-text-primary">
                你这次更希望系统怎么带你学？
              </h3>
              <div class="space-y-2 pt-1">
                <label
                  v-for="opt in paceOptions"
                  :key="opt.id"
                  class="flex cursor-pointer items-start gap-3 rounded-input border p-3 text-sm transition-colors"
                  :class="
                    ui.pace === opt.id
                      ? 'border-primary bg-primary/5'
                      : 'border-border hover:border-primary/50'
                  "
                >
                  <input
                    v-model="ui.pace"
                    type="radio"
                    name="pace"
                    :value="opt.id"
                    class="mt-0.5 h-4 w-4 border-border text-primary focus:ring-primary"
                  />
                  <span class="text-text-primary">{{ opt.label }}</span>
                </label>
              </div>
            </FormCard>

            <div class="flex justify-end pt-2">
              <PrimaryButton :loading="submitting" @click="onSubmit">
                生成我的学习路径
              </PrimaryButton>
            </div>
          </form>
        </template>
      </WorkflowPageScaffold>
    </main>
  </PageContainer>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import PageContainer from '@/components/layout/PageContainer.vue'
import AppTopBar from '@/components/layout/AppTopBar.vue'
import FormCard from '@/components/ui/FormCard.vue'
import PrimaryButton from '@/components/ui/PrimaryButton.vue'
import SecondaryButton from '@/components/ui/SecondaryButton.vue'
import LoadingState from '@/components/ui/LoadingState.vue'
import ErrorState from '@/components/ui/ErrorState.vue'
import WorkflowPageScaffold from '@/components/workflow/WorkflowPageScaffold.vue'
import { useWorkflowStore } from '@/stores/workflow'
import { createSession, submitDiagnosis } from '@/api/diagnosis'
import { showToast } from '@/stores/toast'
import { getErrorMessage } from '@/api/request'
import {
  mapQuickDiagnosisToAnswers,
  type FoundationUiId,
  type BlockerUiId,
  type PaceUiId,
  type QuickDiagnosisUiState,
} from '@/utils/diagnosisSubmitMapper'

const router = useRouter()
const store = useWorkflowStore()

const loading = ref(true)
const submitting = ref(false)
const error = ref<string | null>(null)

const ui = ref<QuickDiagnosisUiState>({
  foundation: null,
  blocker: null,
  pace: null,
})

const sessionReady = computed(
  () => !!store.diagnosisId && !!store.sessionId && !loading.value && !error.value
)

const foundationOptions: { id: FoundationUiId; label: string }[] = [
  { id: 'fu_none', label: '完全不会，几乎没学过' },
  { id: 'fu_fuzzy', label: '学过一点，但很模糊' },
  { id: 'fu_shaky', label: '基本懂，但做题不稳' },
  { id: 'fu_solid_practice', label: '已经会了，想提高做题能力' },
]

const blockerOptions: { id: BlockerUiId; label: string }[] = [
  { id: 'bk_concept', label: '概念听不懂' },
  { id: 'bk_no_problem', label: '看懂了但不会做题' },
  { id: 'bk_error_prone', label: '做题容易出错' },
  { id: 'bk_transfer', label: '会做基础题，但不会迁移应用' },
]

const paceOptions: { id: PaceUiId; label: string }[] = [
  { id: 'pc_tight', label: '时间紧，先快速抓重点' },
  { id: 'pc_normal', label: '正常推进，理解和练习并重' },
  { id: 'pc_relaxed', label: '时间充裕，想学扎实一点' },
]

async function fetchSession() {
  if (!store.goalId) return
  loading.value = true
  error.value = null
  try {
    const data = await createSession(store.goalId)
    store.diagnosisId = data.diagnosisId
    store.sessionId = data.sessionId
    ui.value = { foundation: null, blocker: null, pace: null }
  } catch (err) {
    error.value = getErrorMessage(err)
  } finally {
    loading.value = false
  }
}

async function onSubmit() {
  if (!ui.value.foundation) {
    showToast('请选择第 1 题')
    return
  }
  if (!ui.value.blocker) {
    showToast('请选择第 2 题')
    return
  }
  if (!ui.value.pace) {
    showToast('请选择第 3 题')
    return
  }
  const ans = mapQuickDiagnosisToAnswers(ui.value)
  if (!ans?.length || !store.diagnosisId) {
    showToast('提交失败，请重试')
    return
  }
  submitting.value = true
  try {
    const data = await submitDiagnosis(store.diagnosisId, ans)
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
  if (store.diagnosisId && store.sessionId) {
    loading.value = false
    return
  }
  fetchSession()
})
</script>
