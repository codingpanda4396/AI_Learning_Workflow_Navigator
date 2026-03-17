<template>
  <PageContainer>
    <AppTopBar current="goal" />
    <main class="mx-auto max-w-2xl px-6 py-8">
      <div class="mb-8">
        <h1 class="text-2xl font-bold text-text-primary md:text-3xl">
          开启你的学习导航
        </h1>
        <p class="mt-2 text-text-secondary">
          系统将基于你的目标生成个性化学习路径，请填写以下信息。
        </p>
      </div>

      <FormCard>
        <form class="space-y-5" @submit.prevent="onSubmit">
          <div>
            <label class="mb-1.5 block text-sm font-medium text-text-primary">
              学习目标 <span class="text-red-500">*</span>
            </label>
            <textarea
              v-model="form.rawGoalText"
              rows="3"
              class="w-full rounded-input border border-border px-4 py-3 text-text-primary placeholder:text-gray-400 focus:border-primary focus:outline-none focus:ring-1 focus:ring-primary"
              placeholder="例如：我想搞懂链表，会一点概念但做题总是不会"
              required
            />
          </div>

          <div>
            <label class="mb-1.5 block text-sm font-medium text-text-primary">
              时间预算
            </label>
            <select
              v-model="form.timeBudget"
              class="w-full rounded-input border border-border px-4 py-2.5 text-text-primary focus:border-primary focus:outline-none focus:ring-1 focus:ring-primary"
            >
              <option value="">请选择</option>
              <option
                v-for="(label, val) in timeBudgetLabels"
                :key="val"
                :value="val"
              >
                {{ label }}
              </option>
            </select>
          </div>

          <div>
            <label class="mb-1.5 block text-sm font-medium text-text-primary">
              自评基础
            </label>
            <select
              v-model="form.selfReportedLevel"
              class="w-full rounded-input border border-border px-4 py-2.5 text-text-primary focus:border-primary focus:outline-none focus:ring-1 focus:ring-primary"
            >
              <option value="">请选择</option>
              <option
                v-for="(label, val) in selfReportedLevelLabels"
                :key="val"
                :value="val"
              >
                {{ label }}
              </option>
            </select>
          </div>

          <div>
            <label class="mb-1.5 block text-sm font-medium text-text-primary">
              目标类型
            </label>
            <select
              v-model="form.goalTypeHint"
              class="w-full rounded-input border border-border px-4 py-2.5 text-text-primary focus:border-primary focus:outline-none focus:ring-1 focus:ring-primary"
            >
              <option value="">请选择</option>
              <option
                v-for="(label, val) in goalTypeLabels"
                :key="val"
                :value="val"
              >
                {{ label }}
              </option>
            </select>
          </div>

          <div>
            <label class="mb-1.5 block text-sm font-medium text-text-primary">
              学习偏好（可多选）
            </label>
            <div class="flex flex-wrap gap-2">
              <label
                v-for="(label, val) in preferenceTagLabels"
                :key="val"
                class="inline-flex cursor-pointer items-center gap-2 rounded-input border px-3 py-2 text-sm transition-colors"
                :class="
                  form.preferenceTags.includes(val as PreferenceTagType)
                    ? 'border-primary bg-primary/5 text-primary'
                    : 'border-border hover:border-primary/50'
                "
              >
                <input
                  v-model="form.preferenceTags"
                  type="checkbox"
                  :value="val"
                  class="h-4 w-4 rounded border-border text-primary focus:ring-primary"
                />
                {{ label }}
              </label>
            </div>
          </div>

          <div>
            <label class="mb-1.5 block text-sm font-medium text-text-primary">
              学科/课程
            </label>
            <input
              v-model="form.subjectHint"
              type="text"
              class="w-full rounded-input border border-border px-4 py-2.5 text-text-primary placeholder:text-gray-400 focus:border-primary focus:outline-none focus:ring-1 focus:ring-primary"
              placeholder="例如：数据结构、408"
            />
          </div>

          <div>
            <label class="mb-1.5 block text-sm font-medium text-text-primary">
              知识点（逗号分隔）
            </label>
            <input
              v-model="topicHintsStr"
              type="text"
              class="w-full rounded-input border border-border px-4 py-2.5 text-text-primary placeholder:text-gray-400 focus:border-primary focus:outline-none focus:ring-1 focus:ring-primary"
              placeholder="例如：链表、栈、队列"
            />
          </div>

          <div class="flex justify-end gap-3 pt-4">
            <PrimaryButton :loading="loading" type="submit">
              开始 AI 诊断
            </PrimaryButton>
          </div>
        </form>
      </FormCard>
    </main>
  </PageContainer>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import PageContainer from '@/components/layout/PageContainer.vue'
import AppTopBar from '@/components/layout/AppTopBar.vue'
import FormCard from '@/components/ui/FormCard.vue'
import PrimaryButton from '@/components/ui/PrimaryButton.vue'
import { useWorkflowStore } from '@/stores/workflow'
import { createGoal } from '@/api/goals'
import { showToast } from '@/stores/toast'
import { getErrorMessage } from '@/api/request'
import {
  timeBudgetLabels,
  selfReportedLevelLabels,
  goalTypeLabels,
  preferenceTagLabels,
} from '@/types/labels'
import type {
  PreferenceTagType,
  TimeBudgetType,
  SelfReportedLevelType,
  GoalTypeType,
} from '@/types/enums'
import type { CreateGoalRequest } from '@/types/dto'

const router = useRouter()
const store = useWorkflowStore()

const loading = ref(false)
const form = ref({
  rawGoalText: '',
  timeBudget: '' as string,
  selfReportedLevel: '' as string,
  goalTypeHint: '' as string,
  preferenceTags: [] as PreferenceTagType[],
  subjectHint: '',
  topicHints: [] as string[],
  sourceContext: '',
  priorityModule: '',
})

const topicHintsStr = computed({
  get: () => form.value.topicHints?.join('、') ?? '',
  set: (v: string) => {
    form.value.topicHints = v
      ? v.split(/[,，、]/).map((s) => s.trim()).filter(Boolean)
      : []
  },
})

async function onSubmit() {
  if (!form.value.rawGoalText.trim()) {
    showToast('请填写学习目标')
    return
  }
  loading.value = true
  try {
    const payload: CreateGoalRequest = {
      rawGoalText: form.value.rawGoalText.trim(),
      timeBudget: (form.value.timeBudget || undefined) as TimeBudgetType | undefined,
      selfReportedLevel: (form.value.selfReportedLevel || undefined) as SelfReportedLevelType | undefined,
      preferenceTags: form.value.preferenceTags?.length ? form.value.preferenceTags : undefined,
      goalTypeHint: (form.value.goalTypeHint || undefined) as GoalTypeType | undefined,
      subjectHint: form.value.subjectHint || undefined,
      topicHints: form.value.topicHints?.length ? form.value.topicHints : undefined,
      sourceContext: form.value.sourceContext || undefined,
      priorityModule: form.value.priorityModule || undefined,
    }
    const data = await createGoal(payload)
    store.goalId = data.goalId
    store.structuredGoal = data.structuredGoal
    store.goalContextSnapshot = data.goalContextSnapshot
    router.push('/diagnosis')
  } catch (err) {
    showToast(getErrorMessage(err))
  } finally {
    loading.value = false
  }
}
</script>
