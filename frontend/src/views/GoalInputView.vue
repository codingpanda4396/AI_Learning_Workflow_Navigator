<template>
  <PageContainer>
    <TransitionOverlay v-if="transitionOverlay" :message="GOAL_COPY.transition" />
    <AppTopBar current="goal" />

    <main class="relative overflow-hidden bg-background">
      <div class="relative mx-auto flex w-full max-w-5xl flex-col gap-8 px-5 py-6 pb-44 md:px-8 md:py-10 md:pb-48">
        <header class="grid gap-4 lg:grid-cols-[minmax(0,1fr)_280px] lg:items-start">
          <section class="rounded-[30px] border border-white/70 bg-white/85 p-5 shadow-card md:p-7">
            <p class="text-sm font-medium tracking-[0.18em] text-slate-500">{{ GOAL_COPY.eyebrow }}</p>
            <h1 class="mt-3 text-3xl font-semibold tracking-tight text-text-primary md:text-[42px] md:leading-[1.05]">
              {{ GOAL_COPY.title }}
            </h1>
            <p class="mt-3 max-w-2xl text-base leading-7 text-text-secondary">
              {{ GOAL_COPY.subtitle }}
            </p>
            <div class="mt-5 rounded-[22px] border border-slate-200/80 bg-slate-50/80 px-4 py-3">
              <p class="text-xs font-semibold uppercase tracking-[0.18em] text-slate-500">{{ GOAL_COPY.flowTitle }}</p>
              <div class="mt-3 flex flex-wrap items-center gap-2 text-sm font-medium text-slate-700">
                <template v-for="(step, index) in GOAL_COPY.flowSteps" :key="step">
                  <span
                    class="rounded-full px-3 py-1"
                    :class="index === 0 ? 'bg-primary text-white' : 'bg-white text-slate-500 ring-1 ring-border'"
                  >
                    {{ step }}
                  </span>
                  <span v-if="index < GOAL_COPY.flowSteps.length - 1" class="text-slate-300">→</span>
                </template>
              </div>
            </div>
          </section>

          <aside class="rounded-[26px] border border-slate-200/80 bg-white/72 p-5 shadow-[0_12px_32px_rgba(15,23,42,0.06)] backdrop-blur-sm">
            <div class="flex items-center justify-between gap-3">
              <p class="text-sm font-medium text-text-secondary">{{ GOAL_COPY.continueTitle }}</p>
              <span class="rounded-full bg-slate-100 px-3 py-1 text-[11px] font-medium text-slate-500">
                {{ GOAL_COPY.continueCardHint }}
              </span>
            </div>

            <template v-if="auth.isAuthenticated && auth.recentLearningEntry?.sessionId">
              <p class="mt-4 text-base font-semibold text-text-primary">{{ continueLabel }}</p>
              <SecondaryButton class="mt-4 w-full justify-center" @click="continueLatest">
                {{ GOAL_COPY.continueCta }}
              </SecondaryButton>
            </template>

            <template v-else-if="auth.isAuthenticated">
              <p class="mt-4 text-base font-semibold text-text-primary">{{ GOAL_COPY.continueEmpty }}</p>
              <p class="mt-2 text-sm leading-6 text-text-secondary">{{ GOAL_COPY.noProgressBody }}</p>
            </template>

            <template v-else>
              <p class="mt-4 text-base font-semibold text-text-primary">{{ GOAL_COPY.continueLoginTitle }}</p>
              <p class="mt-2 text-sm leading-6 text-text-secondary">{{ GOAL_COPY.continueLoginHint }}</p>
              <SecondaryButton class="mt-4 w-full justify-center" @click="goToLogin">
                {{ GOAL_COPY.loginCta }}
              </SecondaryButton>
            </template>
          </aside>
        </header>

        <section class="space-y-4">
          <div class="flex items-center justify-between gap-4">
            <div>
              <h2 class="text-xl font-semibold text-text-primary">{{ GOAL_COPY.subjectSection }}</h2>
            </div>
            <p class="hidden text-sm text-text-secondary md:block">{{ GOAL_COPY.demoHint }}</p>
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
                    :class="selectedSubjectKey === subject.key ? 'text-white' : 'text-text-primary'"
                  >
                    {{ subject.label }}
                  </h3>
                  <p
                    class="mt-3 text-sm leading-6"
                    :class="selectedSubjectKey === subject.key ? 'text-slate-200' : 'text-text-secondary'"
                  >
                    {{ subject.description }}
                  </p>
                </div>
                <span
                  class="mt-1 flex h-8 w-8 shrink-0 items-center justify-center rounded-full border text-sm font-semibold"
                  :class="
                    selectedSubjectKey === subject.key
                      ? 'border-primary bg-primary text-white'
                      : 'border-slate-200 bg-white text-slate-400'
                  "
                >
                  {{ subject.topicKeys.length }}
                </span>
              </div>

              <p
                class="mt-4 text-xs font-semibold uppercase tracking-[0.12em]"
                :class="selectedSubjectKey === subject.key ? 'text-slate-300' : 'text-slate-500'"
              >
                {{ GOAL_COPY.representativePoints }}
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
              <h2 class="text-xl font-semibold text-text-primary">{{ GOAL_COPY.topicSection }}</h2>
              <p class="mt-1 text-sm text-text-secondary">{{ GOAL_COPY.topicSectionHint }}</p>
            </div>
            <div class="flex flex-wrap items-center gap-2">
              <div class="rounded-full border border-border bg-white px-4 py-2 text-sm text-text-secondary">
                {{ GOAL_COPY.currentSubjectPrefix }}{{ selectedSubject.label }}
              </div>
              <div class="rounded-full bg-primary px-4 py-2 text-sm font-medium text-white">
                {{ GOAL_COPY.topicStartTag }}
              </div>
            </div>
          </div>

          <div class="rounded-[30px] border border-border/60 bg-white/82 p-1.5 shadow-card">
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
                  <div class="min-w-0">
                    <p class="text-base font-semibold">{{ topic.label }}</p>
                    <p
                      v-if="selectedTopicKey === topic.key && isHomeTopicConfigured(topic.key)"
                      class="mt-1 text-xs font-medium uppercase tracking-[0.14em]"
                      :class="selectedTopicKey === topic.key ? 'text-slate-300' : 'text-slate-500'"
                    >
                      {{ GOAL_COPY.topicStartTag }}
                    </p>
                  </div>
                  <span
                    v-if="!isHomeTopicConfigured(topic.key)"
                    class="shrink-0 rounded-full border border-slate-200 bg-slate-50 px-2 py-0.5 text-[11px] font-medium text-slate-400"
                  >
                    {{ GOAL_COPY.comingSoon }}
                  </span>
                  <span
                    v-else
                    class="h-3.5 w-3.5 shrink-0 rounded-full transition-colors"
                    :class="selectedTopicKey === topic.key ? 'bg-white/90 ring-4 ring-white/20' : 'bg-slate-200'"
                  />
                </div>
                <p class="mt-3 text-sm leading-6" :class="topicDescClass(topic.key)">
                  {{ topic.description }}
                </p>
              </button>
            </div>
          </div>
        </section>
      </div>

      <div class="fixed inset-x-0 bottom-0 z-20 border-t border-border bg-white/95 backdrop-blur">
        <div class="mx-auto flex w-full max-w-5xl flex-col gap-4 px-5 py-4 md:flex-row md:items-center md:justify-between md:px-8">
          <div class="min-w-0">
            <p class="text-sm font-medium text-text-secondary">{{ GOAL_COPY.footerTitle }}</p>
            <p class="mt-1 truncate text-base font-semibold text-text-primary">{{ selectionSummary }}</p>
            <p class="mt-1 text-sm text-text-secondary">{{ GOAL_COPY.footerHint }}</p>
            <p class="mt-1 text-sm font-medium text-slate-700">
              {{ GOAL_COPY.footerNextLabel }}：{{ GOAL_COPY.footerNextValue }}
            </p>
          </div>

          <PrimaryButton
            class="w-full justify-center px-6 py-3 text-base font-semibold shadow-card md:w-auto"
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
  HOME_SUBJECTS,
  isHomeTopicConfigured,
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

function firstConfiguredTopicInSubject(subjectKey: string): string {
  const keys = getHomeSubject(subjectKey).topicKeys
  const hit = keys.find((k) => isHomeTopicConfigured(k))
  return hit ?? keys[0]
}

const storedSubjectKey = getStored(STORAGE_KEYS.subject)
const storedTopicKey = getStored(STORAGE_KEYS.topic)

const initialTopicKey = (() => {
  if (storedTopicKey && isHomeTopicConfigured(storedTopicKey)) {
    const subj = getHomeSubjectByTopic(storedTopicKey)
    if (subj) return storedTopicKey
  }
  const subjKey = storedSubjectKey ?? HOME_DEFAULT_SUBJECT_KEY
  return firstConfiguredTopicInSubject(subjKey)
})()

const initialSubjectKey =
  getHomeSubjectByTopic(initialTopicKey)?.key ?? storedSubjectKey ?? HOME_DEFAULT_SUBJECT_KEY

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
  return entry.sessionStatus === 'COMPLETED' ? GOAL_COPY.continueResult : GOAL_COPY.continueProgress
})
const selectionSummary = computed(() => `${selectedSubject.value.label} / ${selectedTopic.value.label}`)
const ctaDisabled = computed(() => !isHomeTopicConfigured(selectedTopicKey.value))
const ctaLabel = computed(() => (auth.isAuthenticated ? GOAL_COPY.ctaStart : GOAL_COPY.ctaLogin))

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
  const cur = selectedTopicKey.value
  if (!nextTopics.includes(cur) || !isHomeTopicConfigured(cur)) {
    selectedTopicKey.value = firstConfiguredTopicInSubject(value)
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
  if (!isHomeTopicConfigured(topicKey)) return
  selectedTopicKey.value = topicKey
}

function subjectCardClass(subjectKey: string) {
  if (selectedSubjectKey.value === subjectKey) {
    return 'border-primary bg-primary text-white shadow-[0_24px_50px_rgba(30,58,95,0.22)]'
  }
  return 'border-slate-200/90 bg-white hover:border-slate-300 hover:shadow-[0_18px_40px_rgba(15,23,42,0.08)]'
}

function topicCardClass(topicKey: string) {
  const configured = isHomeTopicConfigured(topicKey)
  if (!configured) {
    return 'pointer-events-none cursor-not-allowed border-dashed border-slate-200 bg-slate-50/65 text-slate-400 opacity-65'
  }
  if (selectedTopicKey.value === topicKey) {
    return 'border-primary bg-primary text-white shadow-[0_18px_36px_rgba(30,58,95,0.2)]'
  }
  return 'border-slate-200 bg-white text-slate-950 hover:border-slate-300 hover:bg-slate-50'
}

function topicDescClass(topicKey: string) {
  const configured = isHomeTopicConfigured(topicKey)
  if (!configured) return 'text-slate-400'
  return selectedTopicKey.value === topicKey ? 'text-slate-200' : 'text-slate-600'
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
    showToast(GOAL_COPY.toastStart)
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
