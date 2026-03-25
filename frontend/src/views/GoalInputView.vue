<template>
  <PageContainer>
    <TransitionOverlay v-if="transitionOverlay" message="正在进入下一步..." />
    <AppTopBar current="goal" />

    <main class="relative overflow-hidden bg-[linear-gradient(180deg,#f8fafc_0%,#ffffff_28%,#f8fafc_100%)]">
      <div
        class="pointer-events-none absolute inset-x-0 top-0 h-80 bg-[radial-gradient(ellipse_70%_55%_at_50%_0%,rgba(15,23,42,0.08),transparent)]"
      />

      <div class="relative mx-auto flex w-full max-w-5xl flex-col gap-10 px-5 py-8 pb-40 md:px-8 md:py-12 md:pb-44">
        <header class="grid gap-4 lg:grid-cols-[minmax(0,1fr)_320px] lg:items-start">
          <section
            class="rounded-[32px] bg-white/70 px-2 py-3 md:px-3"
          >
            <h1 class="mt-3 text-3xl font-semibold tracking-tight text-slate-950 md:text-[42px] md:leading-[1.1]">
              {{ GOAL_COPY.title }}
            </h1>
            <p class="mt-4 max-w-2xl text-base leading-7 text-slate-600">
              {{ GOAL_COPY.subtitle }}
            </p>
          </section>

          <aside class="rounded-[24px] border border-slate-200/70 bg-white/82 p-5 shadow-[0_16px_36px_rgba(15,23,42,0.05)]">
            <p class="text-sm font-medium text-slate-500">继续上次学习</p>

            <template v-if="auth.isAuthenticated && auth.recentLearningEntry?.sessionId">
              <p class="mt-3 text-base font-semibold text-slate-950">{{ continueLabel }}</p>
              <SecondaryButton class="mt-5 w-full justify-center" @click="continueLatest">
                继续学习
              </SecondaryButton>
            </template>

            <template v-else-if="auth.isAuthenticated">
              <p class="mt-3 text-base font-semibold text-slate-950">还没有上次进度</p>
              <p class="mt-2 text-sm leading-6 text-slate-600">这次开始后，这里会保留你的最近一轮学习。</p>
            </template>

            <template v-else>
              <p class="mt-3 text-base font-semibold text-slate-950">登录后可继续上次学习</p>
              <p class="mt-2 text-sm leading-6 text-slate-600">学习记录会自动保留在账号里。</p>
              <SecondaryButton class="mt-5 w-full justify-center" @click="goToLogin">
                登录
              </SecondaryButton>
            </template>
          </aside>
        </header>

        <section class="space-y-4">
          <div class="flex items-center justify-between gap-4">
            <div>
              <h2 class="text-xl font-semibold text-slate-950">学科</h2>
            </div>
            <p class="hidden text-sm text-slate-400 md:block">408 四个核心学科</p>
          </div>

          <div class="grid gap-4 md:grid-cols-2">
            <button
              v-for="subject in HOME_SUBJECTS"
              :key="subject.key"
              type="button"
              class="group rounded-[28px] border p-5 text-left transition-all duration-200"
              :class="subjectCardClass(subject.key)"
              @click="selectSubject(subject.key)"
            >
              <div class="flex items-start justify-between gap-4">
                <div>
                  <h3
                    class="text-xl font-semibold"
                    :class="selectedSubjectKey === subject.key ? 'text-white' : 'text-slate-950'"
                  >
                    {{ subject.label }}
                  </h3>
                  <p
                    class="mt-3 text-sm leading-6"
                    :class="selectedSubjectKey === subject.key ? 'text-slate-200' : 'text-slate-600'"
                  >
                    {{ subject.description }}
                  </p>
                </div>
                <span
                  class="mt-1 flex h-8 w-8 shrink-0 items-center justify-center rounded-full border text-sm font-semibold"
                  :class="
                    selectedSubjectKey === subject.key
                      ? 'border-slate-950 bg-slate-950 text-white'
                      : 'border-slate-200 bg-white text-slate-400'
                  "
                >
                  {{ subject.topicKeys.length }}
                </span>
              </div>

              <p
                class="mt-5 text-sm font-medium"
                :class="selectedSubjectKey === subject.key ? 'text-slate-300' : 'text-slate-500'"
              >
                代表知识点
              </p>
              <p
                class="mt-2 text-sm"
                :class="selectedSubjectKey === subject.key ? 'text-white' : 'text-slate-700'"
              >
                {{ subject.cardHint }}
              </p>
            </button>
          </div>
        </section>

        <section class="space-y-4">
          <div class="flex flex-wrap items-end justify-between gap-3">
            <div>
              <h2 class="text-xl font-semibold text-slate-950">知识点</h2>
            </div>
            <div class="rounded-full border border-slate-200 bg-white px-4 py-2 text-sm text-slate-600">
              当前学科：{{ selectedSubject.label }}
            </div>
          </div>

          <div class="rounded-[28px] bg-white/72 p-1">
            <div class="grid gap-3 md:grid-cols-2 xl:grid-cols-3">
              <button
                v-for="topic in selectedSubjectTopics"
                :key="topic.key"
                type="button"
                class="rounded-[24px] border p-4 text-left transition-all duration-200"
                :class="topicCardClass(topic.key)"
                @click="selectTopic(topic.key)"
              >
                <div class="flex items-center justify-between gap-3">
                  <p class="text-base font-semibold">{{ topic.label }}</p>
                  <span
                    class="h-3.5 w-3.5 shrink-0 rounded-full transition-colors"
                    :class="selectedTopicKey === topic.key ? 'bg-white/90 ring-4 ring-white/20' : 'bg-slate-200'"
                  />
                </div>
                <p class="mt-3 text-sm leading-6" :class="selectedTopicKey === topic.key ? 'text-slate-200' : 'text-slate-600'">
                  {{ topic.description }}
                </p>
              </button>
            </div>
          </div>
        </section>
      </div>

      <div class="fixed inset-x-0 bottom-0 z-20 border-t border-slate-200/80 bg-white/95 backdrop-blur">
        <div class="mx-auto flex w-full max-w-5xl flex-col gap-4 px-5 py-4 md:flex-row md:items-center md:justify-between md:px-8">
          <div class="min-w-0">
            <p class="text-sm font-medium text-slate-500">当前选择</p>
            <p class="mt-1 truncate text-base font-semibold text-slate-950">{{ selectionSummary }}</p>
            <p class="mt-1 text-sm text-slate-500">系统将自动为你安排本轮起点</p>
          </div>

          <PrimaryButton
            class="w-full justify-center px-6 py-3 text-base font-semibold shadow-[0_14px_28px_rgba(15,23,42,0.14)] md:w-auto"
            :loading="loading"
            :disabled="ctaDisabled"
            @click="onSubmit"
          >
            {{ ctaLabel }}
          </PrimaryButton>
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
import SecondaryButton from '@/components/ui/SecondaryButton.vue'
import TransitionOverlay from '@/components/ui/TransitionOverlay.vue'
import { useAuthStore } from '@/stores/auth'
import { useWorkflowStore } from '@/stores/workflow'
import { createGoal } from '@/api/goals'
import { showToast } from '@/stores/toast'
import { getErrorMessage } from '@/api/request'
import { GOAL_COPY } from '@/constants/uiCopy'
import {
  buildHomeGoalRequest,
  getHomeSubject,
  getHomeSubjectByTopic,
  getHomeTopic,
  getHomeTopicsBySubject,
  HOME_DEFAULT_SUBJECT_KEY,
  HOME_DEFAULT_TOPIC_KEY,
  HOME_SUBJECTS,
} from '@/constants/homeQuickStart'

const STORAGE_KEYS = {
  subject: 'goal_selected_subject',
  topic: 'goal_selected_topic',
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

const initialTopicKey = getStored(STORAGE_KEYS.topic) ?? HOME_DEFAULT_TOPIC_KEY
const initialSubjectKey =
  getStored(STORAGE_KEYS.subject) ?? getHomeSubjectByTopic(initialTopicKey)?.key ?? HOME_DEFAULT_SUBJECT_KEY

const selectedSubjectKey = ref(initialSubjectKey)
const selectedTopicKey = ref(initialTopicKey)
const transitionOverlay = ref(false)
const loading = ref(false)

const selectedSubject = computed(() => getHomeSubject(selectedSubjectKey.value))
const selectedSubjectTopics = computed(() => getHomeTopicsBySubject(selectedSubjectKey.value))
const selectedTopic = computed(() => getHomeTopic(selectedTopicKey.value))
const autoStartIntent = computed(() => selectedTopic.value.recommendedIntent ?? 'structure')
const continueLabel = computed(() => {
  const entry = auth.recentLearningEntry
  if (!entry?.sessionId) return ''
  return entry.sessionStatus === 'COMPLETED' ? '继续上次：回到这轮学习结果' : '继续上次：回到刚才的学习进度'
})
const selectionSummary = computed(() => `${selectedSubject.value.label} / ${selectedTopic.value.label}`)
const ctaDisabled = computed(() => false)
const ctaLabel = computed(() => (auth.isAuthenticated ? '开始学习' : '登录后开始'))

onMounted(async () => {
  if (!auth.isAuthenticated) return
  try {
    await auth.refresh()
  } catch {
    // Keep the goal page usable even if refreshing auth context fails.
  }
})

watch(selectedSubjectKey, (value) => {
  setStored(STORAGE_KEYS.subject, value)
  const nextTopics = getHomeSubject(value).topicKeys
  if (!nextTopics.includes(selectedTopicKey.value)) {
    selectedTopicKey.value = nextTopics[0]
  }
})

watch(selectedTopicKey, (value) => {
  setStored(STORAGE_KEYS.topic, value)

  const subject = getHomeSubjectByTopic(value)
  if (subject && subject.key !== selectedSubjectKey.value) {
    selectedSubjectKey.value = subject.key
  }
})

function selectSubject(subjectKey: string) {
  selectedSubjectKey.value = subjectKey
}

function selectTopic(topicKey: string) {
  selectedTopicKey.value = topicKey
}

function subjectCardClass(subjectKey: string) {
  if (selectedSubjectKey.value === subjectKey) {
    return 'border-slate-900 bg-slate-900 text-white shadow-[0_24px_50px_rgba(15,23,42,0.14)]'
  }
  return 'border-slate-200/90 bg-white hover:border-slate-300 hover:shadow-[0_18px_40px_rgba(15,23,42,0.08)]'
}

function topicCardClass(topicKey: string) {
  if (selectedTopicKey.value === topicKey) {
    return 'border-slate-950 bg-slate-950 text-white shadow-[0_18px_36px_rgba(15,23,42,0.14)]'
  }
  return 'border-slate-200 bg-white text-slate-950 hover:border-slate-300 hover:bg-slate-50'
}

function delay(ms: number) {
  return new Promise<void>((resolve) => {
    setTimeout(resolve, ms)
  })
}

async function goToLogin() {
  auth.setPendingRedirect('/goal')
  await router.push('/auth/login')
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
  if (!auth.isAuthenticated) {
    auth.setPendingRedirect('/goal')
    showToast('登录后就能开始这一轮')
    await router.push('/auth/login')
    return
  }

  loading.value = true
  const minOverlayMs = 1200
  const started = Date.now()

  try {
    const payload = buildHomeGoalRequest(selectedTopicKey.value, autoStartIntent.value)
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

