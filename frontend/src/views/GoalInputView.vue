<template>
  <PageContainer>
    <TransitionOverlay
      v-if="transitionOverlay"
      message="正在为你接通这轮学习流程，马上进入诊断..."
    />
    <AppTopBar current="goal" />
    <main class="relative overflow-hidden">
      <div class="absolute inset-x-0 top-0 h-80 bg-[radial-gradient(circle_at_top,_rgba(79,70,229,0.18),_transparent_58%)]" />
      <div
        class="absolute right-0 top-24 h-64 w-64 rounded-full bg-[radial-gradient(circle,_rgba(245,158,11,0.18),_transparent_68%)] blur-3xl"
      />

      <div class="relative mx-auto flex w-full max-w-6xl flex-col gap-8 px-6 py-8 lg:py-12">
        <section
          class="overflow-hidden rounded-[28px] border border-slate-200/80 bg-[linear-gradient(140deg,_rgba(15,23,42,0.98)_0%,_rgba(30,41,59,0.96)_56%,_rgba(79,70,229,0.9)_100%)] px-6 py-8 text-white shadow-[0_24px_80px_rgba(15,23,42,0.22)] md:px-8 md:py-10"
        >
          <div class="flex flex-col gap-8 lg:flex-row lg:items-end lg:justify-between">
            <div class="max-w-3xl">
              <p class="text-xs font-semibold uppercase tracking-[0.32em] text-white/60">
                Learning Workflow
              </p>
              <h1 class="mt-4 max-w-2xl text-3xl font-semibold leading-tight md:text-5xl">
                不是问 AI 一个问题，而是开启一轮学习流程
              </h1>
              <p class="mt-4 max-w-2xl text-sm leading-7 text-slate-200 md:text-base">
                先选一个当前可用知识点，再决定这轮学习要先搭结构、讲机制、做练习还是做复盘。首页不再让你从空白输入开始。
              </p>
            </div>

            <div
              class="grid min-w-[280px] gap-3 rounded-[24px] border border-white/12 bg-white/8 p-4 text-sm text-slate-100 backdrop-blur"
            >
              <div class="flex items-center justify-between gap-4">
                <span class="text-white/65">当前入口</span>
                <span class="rounded-full bg-emerald-400/18 px-3 py-1 text-xs font-medium text-emerald-100">
                  Quick Start
                </span>
              </div>
              <div class="grid grid-cols-2 gap-3">
                <div class="rounded-2xl border border-white/10 bg-black/10 p-3">
                  <p class="text-[11px] uppercase tracking-[0.24em] text-white/45">Topic</p>
                  <p class="mt-2 font-medium text-white">{{ selectedTopic.label }}</p>
                </div>
                <div class="rounded-2xl border border-white/10 bg-black/10 p-3">
                  <p class="text-[11px] uppercase tracking-[0.24em] text-white/45">Stage</p>
                  <p class="mt-2 font-medium text-white">
                    {{ selectedQuickStart?.label ?? '先选启动方式' }}
                  </p>
                </div>
              </div>
              <p class="text-sm leading-6 text-slate-200/90">
                {{ heroSupportCopy }}
              </p>
            </div>
          </div>
        </section>

        <div class="grid gap-8 xl:grid-cols-[minmax(0,1.35fr)_360px]">
          <section class="space-y-5">
            <div class="flex flex-wrap items-end justify-between gap-3">
              <div>
                <p class="text-xs font-semibold uppercase tracking-[0.28em] text-slate-500">
                  Subject Matrix
                </p>
                <h2 class="mt-2 text-2xl font-semibold text-text-primary">
                  四个入口，先点亮你这轮要走的知识点
                </h2>
              </div>
              <div
                class="rounded-full border border-amber-200 bg-amber-50 px-4 py-2 text-sm text-amber-900"
              >
                当前仅点亮 4 个真实知识点，灰态章节用于展示 408 学科扩展能力
              </div>
            </div>

            <div class="grid gap-4 md:grid-cols-2">
              <article
                v-for="subject in HOME_SUBJECTS"
                :key="subject.key"
                class="rounded-[24px] border border-slate-200 bg-white p-5 shadow-[0_10px_30px_rgba(15,23,42,0.06)] transition-transform hover:-translate-y-0.5"
              >
                <div class="flex items-start justify-between gap-4">
                  <div>
                    <h3 class="text-lg font-semibold text-text-primary">{{ subject.label }}</h3>
                    <p class="mt-1 text-sm leading-6 text-text-secondary">{{ subject.caption }}</p>
                  </div>
                  <span class="rounded-full bg-slate-100 px-3 py-1 text-xs font-medium text-slate-600">
                    {{ liveTopicCount(subject) }} / {{ subject.topicKeys.length }} 已点亮
                  </span>
                </div>

                <div class="mt-5 flex flex-wrap gap-2.5">
                  <button
                    v-for="topicKey in subject.topicKeys"
                    :key="topicKey"
                    type="button"
                    class="rounded-full border px-3 py-2 text-sm transition-all"
                    :class="chipClass(topicKey)"
                    @click="selectTopic(topicKey)"
                  >
                    {{ getHomeTopic(topicKey).label }}
                  </button>
                </div>
              </article>
            </div>
          </section>

          <aside class="space-y-5">
            <section
              class="rounded-[24px] border border-slate-200 bg-white p-5 shadow-[0_12px_36px_rgba(15,23,42,0.08)]"
            >
              <div class="flex items-start justify-between gap-3">
                <div>
                  <p class="text-xs font-semibold uppercase tracking-[0.28em] text-slate-500">
                    Topic Preview
                  </p>
                  <h2 class="mt-2 text-xl font-semibold text-text-primary">
                    {{ selectedTopic.previewTitle }}
                  </h2>
                </div>
                <span
                  class="rounded-full px-3 py-1 text-xs font-medium"
                  :class="
                    selectedTopic.availability === 'live'
                      ? 'bg-emerald-50 text-emerald-700'
                      : 'bg-slate-100 text-slate-500'
                  "
                >
                  {{ selectedTopic.availability === 'live' ? '已开放' : '即将开放' }}
                </span>
              </div>

              <p class="mt-4 text-sm leading-7 text-text-secondary">
                {{ selectedTopic.previewBody }}
              </p>

              <div
                class="mt-5 rounded-[20px] border px-4 py-4"
                :class="
                  selectedTopic.availability === 'live'
                    ? 'border-indigo-200 bg-indigo-50/80'
                    : 'border-slate-200 bg-slate-50'
                "
              >
                <p class="text-sm font-medium text-text-primary">{{ selectedTopic.launchSummary }}</p>
                <p class="mt-2 text-sm leading-6 text-text-secondary">
                  {{ availabilityHint }}
                </p>
              </div>
            </section>

            <section
              class="rounded-[24px] border border-slate-200 bg-[linear-gradient(180deg,_#ffffff_0%,_#f8fafc_100%)] p-5 shadow-[0_12px_36px_rgba(15,23,42,0.08)]"
            >
              <div class="flex items-center justify-between gap-3">
                <div>
                  <p class="text-xs font-semibold uppercase tracking-[0.28em] text-slate-500">
                    Quick Start
                  </p>
                  <h2 class="mt-2 text-xl font-semibold text-text-primary">
                    选择这轮流程的起手方式
                  </h2>
                </div>
                <span class="rounded-full bg-slate-900 px-3 py-1 text-xs font-medium text-white">
                  4 stages
                </span>
              </div>

              <div class="mt-5 grid gap-3">
                <button
                  v-for="item in HOME_QUICK_STARTS"
                  :key="item.key"
                  type="button"
                  class="rounded-[20px] border p-4 text-left transition-all"
                  :class="
                    selectedQuickStartKey === item.key
                      ? 'border-primary bg-[linear-gradient(135deg,_rgba(79,70,229,0.12),_rgba(14,165,233,0.08))] shadow-[0_12px_24px_rgba(79,70,229,0.12)]'
                      : 'border-slate-200 bg-white hover:border-primary/35 hover:bg-slate-50'
                  "
                  @click="selectedQuickStartKey = item.key"
                >
                  <div class="flex items-start justify-between gap-3">
                    <div>
                      <p class="text-base font-semibold text-text-primary">{{ item.label }}</p>
                      <p class="mt-1 text-sm leading-6 text-text-secondary">{{ item.subtitle }}</p>
                    </div>
                    <span
                      class="mt-1 h-5 w-5 rounded-full border transition-colors"
                      :class="
                        selectedQuickStartKey === item.key
                          ? 'border-primary bg-primary'
                          : 'border-slate-300 bg-white'
                      "
                    />
                  </div>
                </button>
              </div>

              <div class="mt-5 rounded-[20px] border border-slate-200 bg-white p-4">
                <p class="text-sm font-medium text-text-primary">{{ quickStartPreviewTitle }}</p>
                <p class="mt-2 text-sm leading-6 text-text-secondary">
                  {{ quickStartPreviewBody }}
                </p>
              </div>

              <div class="mt-6 flex flex-col gap-3">
                <PrimaryButton
                  class="w-full justify-center py-4 text-base font-semibold shadow-[0_16px_32px_rgba(79,70,229,0.18)]"
                  :loading="loading"
                  :disabled="ctaDisabled"
                  @click="onSubmit"
                >
                  {{ ctaLabel }}
                </PrimaryButton>
                <p class="text-sm leading-6 text-text-secondary">
                  {{ ctaHint }}
                </p>
              </div>
            </section>
          </aside>
        </div>
      </div>
    </main>
  </PageContainer>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import PageContainer from '@/components/layout/PageContainer.vue'
import AppTopBar from '@/components/layout/AppTopBar.vue'
import PrimaryButton from '@/components/ui/PrimaryButton.vue'
import TransitionOverlay from '@/components/ui/TransitionOverlay.vue'
import { useWorkflowStore } from '@/stores/workflow'
import { createGoal } from '@/api/goals'
import { showToast } from '@/stores/toast'
import { getErrorMessage } from '@/api/request'
import {
  buildHomeGoalRequest,
  getHomeSubjectByTopic,
  getHomeTopic,
  HOME_DEFAULT_TOPIC_KEY,
  HOME_QUICK_STARTS,
  HOME_SUBJECTS,
} from '@/constants/homeQuickStart'

const router = useRouter()
const store = useWorkflowStore()

const selectedTopicKey = ref(HOME_DEFAULT_TOPIC_KEY)
const selectedQuickStartKey = ref<(typeof HOME_QUICK_STARTS)[number]['key'] | null>(null)
const transitionOverlay = ref(false)
const loading = ref(false)

const selectedTopic = computed(() => getHomeTopic(selectedTopicKey.value))
const selectedSubject = computed(() => getHomeSubjectByTopic(selectedTopicKey.value))
const selectedQuickStart = computed(() =>
  HOME_QUICK_STARTS.find((item) => item.key === selectedQuickStartKey.value) ?? null
)

const ctaDisabled = computed(
  () => selectedTopic.value.availability !== 'live' || !selectedQuickStart.value
)

const heroSupportCopy = computed(() => {
  if (!selectedQuickStart.value) {
    return `当前已选 ${selectedTopic.value.label}，下一步只差确定这轮流程先从哪个阶段起步。`
  }
  return `${selectedQuickStart.value.previewPrefix}，围绕 ${selectedTopic.value.label} 生成诊断与后续学习编排。`
})

const availabilityHint = computed(() =>
  selectedTopic.value.availability === 'live'
    ? '点亮知识点可以直接进入诊断，让系统生成这一轮的学习路径。'
    : '这个 chip 现在只做能力预览，开放后会接入同样的诊断、规划、执行闭环。'
)

const quickStartPreviewTitle = computed(() => {
  if (!selectedQuickStart.value) return '先选一个 Quick Start，再决定这轮怎么启动'
  return `${selectedQuickStart.value.label} · ${selectedSubject.value?.label ?? ''}`
})

const quickStartPreviewBody = computed(() => {
  if (!selectedQuickStart.value) {
    return '四个 Quick Start 分别对应 STRUCTURE / UNDERSTANDING / TRAINING / REFLECTION，不需要自由输入也能开始。'
  }
  return `${selectedQuickStart.value.previewPrefix}，围绕「${selectedTopic.value.label}」生成更适合这轮学习的起手路径。`
})

const ctaLabel = computed(() => {
  if (selectedTopic.value.availability !== 'live') return '该章节即将开放'
  if (!selectedQuickStart.value) return '先选择 Quick Start'
  return selectedQuickStart.value.ctaLabel
})

const ctaHint = computed(() => {
  if (selectedTopic.value.availability !== 'live') {
    return '灰态章节当前只用于展示 408 学科扩展能力，暂不直接开启流程。'
  }
  if (!selectedQuickStart.value) {
    return '选择一个 Quick Start 后，我们会按你的启动方式进入诊断页。'
  }
  return `点击后会以「${selectedTopic.value.label} + ${selectedQuickStart.value.label}」创建目标，并继续进入诊断。`
})

function chipClass(topicKey: string) {
  const topic = getHomeTopic(topicKey)
  const isSelected = selectedTopicKey.value === topicKey

  if (topic.availability === 'live') {
    if (isSelected) {
      return 'border-primary bg-primary text-white shadow-[0_12px_24px_rgba(79,70,229,0.18)]'
    }
    return 'border-indigo-200 bg-indigo-50 text-indigo-700 hover:border-primary hover:bg-indigo-100'
  }

  if (isSelected) {
    return 'border-slate-300 bg-slate-200 text-slate-700'
  }

  return 'border-slate-200 bg-slate-50 text-slate-400 hover:border-slate-300 hover:bg-slate-100'
}

function liveTopicCount(subject: (typeof HOME_SUBJECTS)[number]) {
  return subject.topicKeys.filter((topicKey) => getHomeTopic(topicKey).availability === 'live').length
}

function selectTopic(topicKey: string) {
  selectedTopicKey.value = topicKey
}

function delay(ms: number) {
  return new Promise<void>((resolve) => {
    setTimeout(resolve, ms)
  })
}

async function onSubmit() {
  if (ctaDisabled.value || !selectedQuickStart.value) {
    if (selectedTopic.value.availability !== 'live') {
      showToast('这个章节还未开放，请先选择已点亮的知识点')
      return
    }
    showToast('请先选择 Quick Start')
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
