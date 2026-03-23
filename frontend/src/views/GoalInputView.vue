<template>
  <PageContainer>
    <TransitionOverlay
      v-if="transitionOverlay"
      message="我先帮你判断一下当前状态，大概需要几秒钟…"
    />
    <AppTopBar current="goal" />
    <main class="mx-auto max-w-2xl px-6 py-8">
      <div class="mb-8">
        <h1 class="text-2xl font-bold text-text-primary md:text-3xl">
          先说好你要学什么
        </h1>
        <p class="mt-2 text-text-secondary">
          填完这几项，系统会基于你的输入直接给出<strong class="font-medium text-text-primary">第一步学习动作</strong>，并带你往下走。
        </p>
      </div>

      <section class="mb-8 rounded-input border border-border bg-white p-4 shadow-sm md:p-5">
        <p class="text-sm font-medium text-text-primary">快速开始</p>
        <p class="mt-1 text-sm text-text-secondary">
          选一个最贴近你当前想学的小主题，会填入示例描述（仍可随意修改后再提交）。
        </p>
        <div class="mt-4 grid gap-3 sm:grid-cols-3">
          <button
            v-for="preset in showcasePresets"
            :key="preset.id"
            type="button"
            class="flex flex-col items-start rounded-input border px-4 py-3 text-left text-sm transition-colors"
            :class="
              activePresetId === preset.id
                ? 'border-primary bg-primary/5 ring-1 ring-primary/30'
                : 'border-border hover:border-primary/40 hover:bg-slate-50'
            "
            @click="applyShowcasePreset(preset)"
          >
            <span class="font-medium text-text-primary">{{ preset.title }}</span>
            <span class="mt-1 text-xs leading-snug text-text-secondary">{{
              preset.subtitle
            }}</span>
          </button>
        </div>
        <p
          v-if="presetFeedbackLine"
          class="mt-4 rounded-input border border-primary/25 bg-primary/[0.06] px-3 py-2.5 text-sm leading-snug text-text-primary"
        >
          <span aria-hidden="true">👉</span>
          {{ presetFeedbackLine }}
        </p>
      </section>

      <FormCard>
        <form class="space-y-6" @submit.prevent="onSubmit">
          <div>
            <label class="mb-1.5 block text-sm font-medium text-text-primary">
              学习目标 <span class="text-red-500">*</span>
            </label>
            <textarea
              v-model="form.rawGoalText"
              rows="3"
              class="w-full rounded-input border border-border px-4 py-3 text-text-primary placeholder:text-gray-400 focus:border-primary focus:outline-none focus:ring-1 focus:ring-primary"
              placeholder="比如：我总是分不清链表和顺序表，希望能搞清楚什么时候用哪个"
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
              当前基础
            </label>
            <select
              v-model="form.selfReportedLevel"
              class="w-full rounded-input border border-border px-4 py-2.5 text-text-primary focus:border-primary focus:outline-none focus:ring-1 focus:ring-primary"
            >
              <option value="">请选择</option>
              <option
                v-for="(label, val) in goalPageLevelLabels"
                :key="val"
                :value="val"
              >
                {{ label }}
              </option>
            </select>
          </div>

          <div>
            <p class="mb-2 text-sm font-medium text-text-primary">
              你现在更希望先？
            </p>
            <div class="space-y-2">
              <label
                v-for="opt in entryPreferenceOptions"
                :key="opt.value"
                class="flex cursor-pointer items-center gap-3 rounded-input border p-3 text-sm transition-colors"
                :class="
                  form.entryPreference === opt.value
                    ? 'border-primary bg-primary/5'
                    : 'border-border hover:border-primary/50'
                "
              >
                <input
                  v-model="form.entryPreference"
                  type="radio"
                  name="entryPreference"
                  :value="opt.value"
                  class="h-4 w-4 border-border text-primary focus:ring-primary"
                />
                <span class="text-text-primary">{{ opt.label }}</span>
              </label>
            </div>
          </div>

          <div>
            <label class="mb-1.5 block text-sm font-medium text-text-primary">
              学科/课程（可选）
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
              知识点（可选，逗号或顿号分隔）
            </label>
            <input
              v-model="topicHintsStr"
              type="text"
              class="w-full rounded-input border border-border px-4 py-2.5 text-text-primary placeholder:text-gray-400 focus:border-primary focus:outline-none focus:ring-1 focus:ring-primary"
              placeholder="例如：链表、栈、队列"
            />
          </div>

          <div class="flex justify-end gap-3 pt-2">
            <PrimaryButton :loading="loading" @click="onSubmit">
              帮我看看我现在在哪一步
            </PrimaryButton>
          </div>
        </form>
      </FormCard>
    </main>
  </PageContainer>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import PageContainer from '@/components/layout/PageContainer.vue'
import AppTopBar from '@/components/layout/AppTopBar.vue'
import FormCard from '@/components/ui/FormCard.vue'
import PrimaryButton from '@/components/ui/PrimaryButton.vue'
import TransitionOverlay from '@/components/ui/TransitionOverlay.vue'
import { useWorkflowStore } from '@/stores/workflow'
import { createGoal } from '@/api/goals'
import { showToast } from '@/stores/toast'
import { getErrorMessage } from '@/api/request'
import { timeBudgetLabels } from '@/types/labels'
import { PreferenceTag } from '@/types/enums'
import type { TimeBudgetType, SelfReportedLevelType, PreferenceTagType } from '@/types/enums'
import type { CreateGoalRequest } from '@/types/dto'
import {
  GOAL_SHOWCASE_PRESETS,
  GOAL_PRESET_FEEDBACK,
  findGoalShowcasePreset,
  type GoalShowcasePreset,
} from '@/constants/goalShowcasePresets'

const router = useRouter()
const route = useRoute()
const store = useWorkflowStore()

/** 目标页专用口语化基础文案（仍传后端既有 SelfReportedLevel 枚举值） */
const goalPageLevelLabels: Record<string, string> = {
  BEGINNER: '几乎零基础，刚开始接触',
  BASIC: '知道一点，但很不熟',
  PARTIAL_UNDERSTANDING: '学过一些，中间容易断档',
  CAN_EXPLAIN_BUT_NOT_APPLY: '概念大概懂，一做题就懵',
  SOLID_BUT_WANT_IMPROVE: '整体还行，想再稳一点',
}

const entryPreferenceOptions: { value: PreferenceTagType; label: string }[] = [
  { value: PreferenceTag.CONCEPT_FIRST, label: '先理解原理' },
  { value: PreferenceTag.PRACTICE_FIRST, label: '先学会做题' },
]

const showcasePresets = GOAL_SHOWCASE_PRESETS
const activePresetId = ref<string | null>(null)
const presetFeedbackLine = ref('')

const transitionOverlay = ref(false)

function applyShowcasePreset(preset: GoalShowcasePreset) {
  const patch = preset.apply()
  form.value.rawGoalText = patch.rawGoalText
  form.value.topicHints = [...patch.topicHints]
  form.value.subjectHint = patch.subjectHint
  activePresetId.value = preset.id
  presetFeedbackLine.value = GOAL_PRESET_FEEDBACK[preset.id] ?? ''
}

const loading = ref(false)
const form = ref({
  rawGoalText: '',
  timeBudget: '' as string,
  selfReportedLevel: '' as string,
  entryPreference: PreferenceTag.CONCEPT_FIRST as PreferenceTagType,
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

onMounted(() => {
  const q = route.query.preset
  const id = typeof q === 'string' ? q : Array.isArray(q) ? q[0] : undefined
  const preset = findGoalShowcasePreset(id)
  if (preset) {
    applyShowcasePreset(preset)
  }
})

function delay(ms: number) {
  return new Promise<void>((resolve) => {
    setTimeout(resolve, ms)
  })
}

async function onSubmit() {
  if (!form.value.rawGoalText.trim()) {
    showToast('请填写学习目标')
    return
  }
  loading.value = true
  const minOverlayMs = 1200
  const started = Date.now()
  try {
    const payload: CreateGoalRequest = {
      rawGoalText: form.value.rawGoalText.trim(),
      timeBudget: (form.value.timeBudget || undefined) as TimeBudgetType | undefined,
      selfReportedLevel: (form.value.selfReportedLevel || undefined) as
        | SelfReportedLevelType
        | undefined,
      preferenceTags: [form.value.entryPreference],
      subjectHint: form.value.subjectHint || undefined,
      topicHints: form.value.topicHints?.length ? form.value.topicHints : undefined,
      sourceContext: form.value.sourceContext || undefined,
      priorityModule: form.value.priorityModule || undefined,
    }
    const data = await createGoal(payload)
    store.goalId = data.goalId
    store.structuredGoal = data.structuredGoal
    store.goalContextSnapshot = data.goalContextSnapshot
    const elapsed = Date.now() - started
    const remaining = Math.max(0, minOverlayMs - elapsed)
    transitionOverlay.value = true
    loading.value = false
    if (remaining > 0) await delay(remaining)
    await router.push('/diagnosis')
  } catch (err) {
    showToast(getErrorMessage(err))
  } finally {
    transitionOverlay.value = false
    loading.value = false
  }
}
</script>
