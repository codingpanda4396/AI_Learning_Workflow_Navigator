<template>
  <PageContainer>
    <TransitionOverlay v-if="transitionOverlay" message="正在进入诊断与规划..." />
    <AppTopBar current="goal" />
    <main class="relative overflow-hidden">
      <div
        class="pointer-events-none absolute inset-x-0 top-0 h-72 bg-[radial-gradient(ellipse_80%_60%_at_50%_-10%,rgba(15,23,42,0.12),transparent)]"
      />

      <div class="relative mx-auto w-full max-w-4xl px-5 py-10 md:px-8 md:py-14">
        <header class="space-y-6">
          <div class="space-y-2 text-center md:text-left">
            <p class="text-xs font-semibold uppercase tracking-[0.28em] text-slate-400">Quick Start</p>
            <h1 class="text-3xl font-semibold tracking-tight text-text-primary md:text-4xl">
              选入口，启动这一轮学习工作流
            </h1>
            <p class="max-w-2xl text-sm leading-6 text-text-secondary md:text-[15px]">
              先选知识点，再选起手方式。系统会从目标、诊断、规划一路编排到执行与报告。
            </p>
          </div>

          <div class="grid gap-4 lg:grid-cols-[1.35fr_0.65fr]">
            <section
              class="rounded-[28px] border border-slate-200/90 bg-white/90 px-5 py-5 shadow-[0_18px_48px_rgba(15,23,42,0.06)] backdrop-blur-sm"
            >
              <div class="flex flex-col gap-5 md:flex-row md:items-end md:justify-between">
                <div class="min-w-0 flex-1 space-y-2">
                  <p class="text-[11px] font-medium uppercase tracking-[0.24em] text-slate-400">本轮入口</p>
                  <p class="truncate text-lg font-semibold text-text-primary">
                    {{ selectedTopic.label }}
                    <span class="px-2 text-slate-300">/</span>
                    {{ selectedQuickStart?.label ?? '请选择起手方式' }}
                  </p>
                  <p v-if="selectedTopic.availability === 'live'" class="text-sm leading-6 text-text-secondary">
                    {{ topicOneLiner }}
                  </p>
                  <p v-else class="text-sm text-slate-400">该知识点将在后续扩展中开放，请先选择已点亮主题。</p>
                </div>

                <div class="w-full space-y-3 md:w-[220px]">
                  <div
                    v-if="recommendedForTopic && selectedTopic.availability === 'live'"
                    class="rounded-2xl bg-slate-50 px-4 py-3 text-xs text-slate-500"
                  >
                    推荐起手：
                    <span class="font-medium text-slate-700">{{ recommendedForTopic.label }}</span>
                  </div>
                  <PrimaryButton
                    class="w-full justify-center py-3 text-sm font-semibold shadow-[0_12px_24px_rgba(15,23,42,0.15)]"
                    :loading="loading"
                    :disabled="ctaDisabled"
                    @click="onSubmit"
                  >
                    {{ ctaLabel }}
                  </PrimaryButton>
                </div>
              </div>
            </section>

            <aside
              class="rounded-[28px] border border-slate-200/90 bg-slate-950 px-5 py-5 text-white shadow-[0_18px_48px_rgba(15,23,42,0.18)]"
            >
              <template v-if="auth.isAuthenticated && auth.user">
                <p class="text-[11px] font-semibold uppercase tracking-[0.24em] text-slate-400">当前账号</p>
                <p class="mt-3 text-xl font-semibold">{{ auth.user.displayName }}</p>
                <p class="mt-2 text-sm leading-6 text-slate-300">
                  新创建的学习目标和流程都会绑定到这个账号下。
                </p>

                <button
                  v-if="auth.recentLearningEntry?.sessionId"
                  type="button"
                  class="mt-5 w-full rounded-2xl border border-white/15 bg-white/8 px-4 py-3 text-left transition hover:bg-white/12"
                  @click="continueLatest"
                >
                  <p class="text-xs uppercase tracking-[0.2em] text-slate-400">Continue</p>
                  <p class="mt-2 text-sm font-medium text-white">继续上次学习</p>
                  <p class="mt-1 text-xs text-slate-400">{{ continueLabel }}</p>
                </button>
              </template>

              <template v-else>
                <p class="text-[11px] font-semibold uppercase tracking-[0.24em] text-slate-400">登录后可用</p>
                <p class="mt-3 text-xl font-semibold">保存你的学习链路</p>
                <p class="mt-2 text-sm leading-6 text-slate-300">
                  开始这一轮前先登录，目标、诊断、规划与执行进度才能归到你的账号。
                </p>
                <router-link
                  to="/auth/login"
                  class="mt-5 inline-flex w-full items-center justify-center rounded-2xl bg-white px-4 py-3 text-sm font-semibold text-slate-950 transition hover:bg-slate-100"
                >
                  登录开始
                </router-link>
              </template>
            </aside>
          </div>
        </header>

        <section class="mt-12 space-y-4">
          <div class="flex items-baseline justify-between gap-3">
            <h2 class="text-base font-semibold text-text-primary">知识点</h2>
            <p class="text-xs text-slate-400">408 个学科入口持续扩展中</p>
          </div>
          <p class="text-xs text-slate-400">亮色主题可直接开始；灰色主题代表后续将接入更大的学科覆盖。</p>

          <div class="grid gap-4 sm:grid-cols-2">
            <article
              v-for="subject in HOME_SUBJECTS"
              :key="subject.key"
              class="rounded-2xl border border-slate-200/90 bg-white p-4 shadow-sm transition-shadow hover:shadow-md"
            >
              <div class="flex items-start justify-between gap-2">
                <div class="min-w-0">
                  <h3 class="text-[15px] font-semibold text-text-primary">{{ subject.label }}</h3>
                  <p class="mt-0.5 text-xs text-slate-500">{{ subject.caption }}</p>
                </div>
              </div>
              <div class="mt-3 flex flex-wrap gap-2">
                <button
                  v-for="topicKey in subject.topicKeys"
                  :key="topicKey"
                  type="button"
                  class="rounded-full border px-3 py-1.5 text-xs font-medium transition-all"
                  :class="chipClass(topicKey)"
                  @click="selectTopic(topicKey)"
                >
                  {{ getHomeTopic(topicKey).label }}
                </button>
              </div>
            </article>
          </div>
        </section>

        <section class="mt-12 space-y-4">
          <div class="flex flex-wrap items-baseline justify-between gap-2">
            <h2 class="text-base font-semibold text-text-primary">起手方式</h2>
            <p
              v-if="selectedTopic.availability === 'live' && recommendedForTopic"
              class="text-xs text-slate-400"
            >
              推荐：{{ recommendedForTopic.label }}
            </p>
          </div>

          <div class="grid gap-3 sm:grid-cols-2">
            <button
              v-for="item in HOME_QUICK_STARTS"
              :key="item.key"
              type="button"
              class="rounded-2xl border p-4 text-left transition-all"
              :class="
                selectedQuickStartKey === item.key
                  ? 'border-slate-950 bg-slate-950 text-white shadow-[0_16px_32px_rgba(15,23,42,0.12)]'
                  : 'border-slate-200 bg-white hover:border-slate-300 hover:bg-slate-50/80'
              "
              @click="selectedQuickStartKey = item.key"
            >
              <div class="flex items-start justify-between gap-2">
                <div>
                  <p
                    class="text-[15px] font-semibold"
                    :class="selectedQuickStartKey === item.key ? 'text-white' : 'text-text-primary'"
                  >
                    {{ item.label }}
                  </p>
                  <p
                    class="mt-1 text-xs leading-relaxed"
                    :class="selectedQuickStartKey === item.key ? 'text-slate-300' : 'text-text-secondary'"
                  >
                    {{ item.subtitle }}
                  </p>
                </div>
                <span
                  class="mt-0.5 h-4 w-4 shrink-0 rounded-full border-2 transition-colors"
                  :class="
                    selectedQuickStartKey === item.key
                      ? 'border-white bg-white'
                      : 'border-slate-300 bg-white'
                  "
                />
              </div>
            </button>
          </div>
        </section>

        <div class="mt-12 space-y-4 border-t border-slate-100 pt-10">
          <PrimaryButton
            class="w-full justify-center py-3.5 text-base font-semibold shadow-[0_12px_28px_rgba(15,23,42,0.16)]"
            :loading="loading"
            :disabled="ctaDisabled"
            @click="onSubmit"
          >
            {{ ctaLabel }}
          </PrimaryButton>
          <p class="text-center text-xs text-slate-400">选好之后点此开始，将进入诊断与规划。</p>
          <p class="text-center text-[11px] text-slate-300">
            {{ HOME_TOPIC_SLOT_COUNT }} 个展示位 / {{ HOME_LIVE_TOPIC_COUNT }} 个可学 / 408+ 可扩展
          </p>
        </div>
      </div>
    </main>
  </PageContainer>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import PageContainer from '@/components/layout/PageContainer.vue'
import AppTopBar from '@/components/layout/AppTopBar.vue'
import PrimaryButton from '@/components/ui/PrimaryButton.vue'
import TransitionOverlay from '@/components/ui/TransitionOverlay.vue'
import { useAuthStore } from '@/stores/auth'
import { useWorkflowStore } from '@/stores/workflow'
import { createGoal } from '@/api/goals'
import { showToast } from '@/stores/toast'
import { getErrorMessage } from '@/api/request'
import {
  buildHomeGoalRequest,
  getHomeTopic,
  HOME_DEFAULT_TOPIC_KEY,
  HOME_LIVE_TOPIC_COUNT,
  HOME_QUICK_STARTS,
  HOME_SUBJECTS,
  HOME_TOPIC_SLOT_COUNT,
} from '@/constants/homeQuickStart'

const STORAGE_KEYS = {
  topic: 'goal_selected_topic',
  quickStart: 'goal_selected_quickstart',
} as const

function getStored(key: string) {
  try {
    return sessionStorage.getItem(key)
  } catch {
    return null
  }
}

function setStored(key: string, value: string) {
  try {
    sessionStorage.setItem(key, value)
  } catch {
    // ignore
  }
}

const router = useRouter()
const auth = useAuthStore()
const store = useWorkflowStore()

const fallbackTopic = HOME_DEFAULT_TOPIC_KEY
const initialTopicKey = getStored(STORAGE_KEYS.topic) ?? fallbackTopic
const initialTopic = getHomeTopic(initialTopicKey)
const selectedTopicKey = ref(initialTopicKey)
const selectedQuickStartKey = ref<(typeof HOME_QUICK_STARTS)[number]['key']>(
  (getStored(STORAGE_KEYS.quickStart) as (typeof HOME_QUICK_STARTS)[number]['key'] | null) ??
    initialTopic.recommendedIntent ??
    'structure'
)
const transitionOverlay = ref(false)
const loading = ref(false)

const selectedTopic = computed(() => getHomeTopic(selectedTopicKey.value))
const selectedQuickStart = computed(() =>
  HOME_QUICK_STARTS.find((item) => item.key === selectedQuickStartKey.value) ?? null
)

const recommendedForTopic = computed(() => {
  const ri = selectedTopic.value.recommendedIntent
  if (!ri) return null
  return HOME_QUICK_STARTS.find((x) => x.key === ri) ?? null
})

const topicOneLiner = computed(() => {
  const topic = selectedTopic.value
  return topic.availability === 'live' ? topic.previewBody : ''
})

const continueLabel = computed(() => {
  const entry = auth.recentLearningEntry
  if (!entry?.sessionId) return ''
  return entry.sessionStatus === 'COMPLETED' ? '回到这轮报告页，查看结果与下一步建议。' : '回到最近一轮执行进度，继续任务脚手架。'
})

const ctaDisabled = computed(
  () => selectedTopic.value.availability !== 'live' || !selectedQuickStart.value
)

const ctaLabel = computed(() => {
  if (selectedTopic.value.availability !== 'live') return '该知识点尚未开放'
  return auth.isAuthenticated ? '开始这一轮学习' : '登录后开始这一轮'
})

onMounted(async () => {
  if (!auth.isAuthenticated) return
  try {
    await auth.refresh()
  } catch {
    // Keep the goal page usable even if refreshing auth context fails.
  }
})

watch(selectedTopicKey, (value) => {
  setStored(STORAGE_KEYS.topic, value)
  const topic = getHomeTopic(value)
  if (topic.availability === 'live' && topic.recommendedIntent) {
    selectedQuickStartKey.value = topic.recommendedIntent
  }
})

watch(selectedQuickStartKey, (value) => {
  setStored(STORAGE_KEYS.quickStart, value)
})

function chipClass(topicKey: string) {
  const topic = getHomeTopic(topicKey)
  const isSelected = selectedTopicKey.value === topicKey

  if (topic.availability === 'live') {
    if (isSelected) {
      return 'border-slate-950 bg-slate-950 text-white shadow-sm'
    }
    return 'border-slate-200 bg-slate-50 text-slate-800 hover:border-slate-300'
  }

  if (isSelected) {
    return 'border-slate-300 bg-slate-100 text-slate-500'
  }

  return 'cursor-not-allowed border-slate-100 bg-slate-50 text-slate-300'
}

function selectTopic(topicKey: string) {
  selectedTopicKey.value = topicKey
}

function delay(ms: number) {
  return new Promise<void>((resolve) => {
    setTimeout(resolve, ms)
  })
}

async function continueLatest() {
  const entry = auth.recentLearningEntry
  if (!entry?.sessionId) return
  store.goalId = entry.goalId ?? null
  store.diagnosisId = entry.diagnosisId ?? null
  store.planId = entry.planId ?? null
  store.sessionId = entry.sessionId
  store.currentTaskId = entry.currentTaskId ?? null

  if (entry.sessionStatus === 'COMPLETED') {
    await router.push('/report')
    return
  }
  if (entry.currentTaskId) {
    await router.push({ name: 'taskRun', params: { taskId: entry.currentTaskId } })
    return
  }
  if (entry.diagnosisId) {
    await router.push('/diagnosis')
  }
}

async function onSubmit() {
  if (ctaDisabled.value || !selectedQuickStart.value) {
    if (selectedTopic.value.availability !== 'live') {
      showToast('请先选择已点亮的知识点')
      return
    }
    showToast('请选择起手方式')
    return
  }

  if (!auth.isAuthenticated) {
    auth.setPendingRedirect('/goal')
    showToast('登录后即可开始这一轮学习')
    await router.push('/auth/login')
    return
  }

  loading.value = true
  const minOverlayMs = 1200
  const started = Date.now()

  try {
    const payload = buildHomeGoalRequest(selectedTopicKey.value, selectedQuickStart.value.key)
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
