<template>
  <PageContainer>
    <TransitionOverlay v-if="transitionOverlay" message="进入诊断与规划…" />
    <AppTopBar current="goal" />
    <main class="relative overflow-hidden">
      <div
        class="pointer-events-none absolute inset-x-0 top-0 h-72 bg-[radial-gradient(ellipse_80%_60%_at_50%_-10%,rgba(79,70,229,0.12),transparent)]"
      />

      <div class="relative mx-auto w-full max-w-3xl px-5 py-10 md:px-8 md:py-14">
        <!-- A 顶部引导：一句话 + 当前入口（单视觉核心） -->
        <header class="space-y-6">
          <div class="space-y-2 text-center md:text-left">
            <h1 class="text-2xl font-semibold tracking-tight text-text-primary md:text-3xl">
              选入口，启动本轮学习
            </h1>
            <p class="text-sm text-text-secondary md:text-[15px]">
              先选知识点，再选起手方式，然后进入诊断与规划。
            </p>
          </div>

          <div
            class="rounded-2xl border border-slate-200/90 bg-white/90 px-5 py-4 shadow-[0_12px_40px_rgba(15,23,42,0.06)] backdrop-blur-sm md:flex md:items-stretch md:justify-between md:gap-6 md:py-5"
          >
            <div class="min-w-0 flex-1 space-y-1">
              <p class="text-[11px] font-medium uppercase tracking-[0.2em] text-slate-400">本轮入口</p>
              <p class="truncate text-base font-semibold text-text-primary">
                {{ selectedTopic.label }}
                <span class="font-normal text-slate-400">·</span>
                {{ selectedQuickStart?.label ?? '—' }}
              </p>
              <p v-if="selectedTopic.availability === 'live'" class="text-sm text-text-secondary">
                {{ topicOneLiner }}
              </p>
              <p v-else class="text-sm text-slate-400">该知识点即将开放，请选已点亮项。</p>
              <p
                v-if="recommendedForTopic && selectedTopic.availability === 'live'"
                class="pt-1 text-xs text-slate-400 md:hidden"
              >
                推荐起手：{{ recommendedForTopic.label }}
              </p>
            </div>
            <div class="mt-4 flex shrink-0 flex-col justify-center gap-3 md:mt-0 md:w-[200px]">
              <div
                v-if="recommendedForTopic && selectedTopic.availability === 'live'"
                class="hidden text-right text-xs text-slate-400 md:block"
              >
                <span>推荐起手</span>
                <span class="ml-1 font-medium text-slate-600">{{ recommendedForTopic.label }}</span>
              </div>
              <PrimaryButton
                class="w-full justify-center py-3 text-sm font-semibold shadow-[0_10px_24px_rgba(79,70,229,0.18)]"
                :loading="loading"
                :disabled="ctaDisabled"
                @click="onSubmit"
              >
                {{ ctaLabel }}
              </PrimaryButton>
            </div>
          </div>
        </header>

        <!-- B 知识点 -->
        <section class="mt-12 space-y-4">
          <div class="flex items-baseline justify-between gap-3">
            <h2 class="text-base font-semibold text-text-primary">知识点</h2>
          </div>
          <p class="text-xs text-slate-400">点选知识点后，会更新上方入口与推荐起手。</p>

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

        <!-- C 起手方式 -->
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
                  ? 'border-primary bg-indigo-50/80 ring-2 ring-primary/25'
                  : 'border-slate-200 bg-white hover:border-slate-300 hover:bg-slate-50/80'
              "
              @click="selectedQuickStartKey = item.key"
            >
              <div class="flex items-start justify-between gap-2">
                <div>
                  <p class="text-[15px] font-semibold text-text-primary">{{ item.label }}</p>
                  <p class="mt-1 text-xs leading-relaxed text-text-secondary">{{ item.subtitle }}</p>
                </div>
                <span
                  class="mt-0.5 h-4 w-4 shrink-0 rounded-full border-2 transition-colors"
                  :class="
                    selectedQuickStartKey === item.key
                      ? 'border-primary bg-primary'
                      : 'border-slate-300 bg-white'
                  "
                />
              </div>
            </button>
          </div>
        </section>

        <!-- D 底部：再强调行动 + 弱提示 -->
        <div class="mt-12 space-y-4 border-t border-slate-100 pt-10">
          <PrimaryButton
            class="w-full justify-center py-3.5 text-base font-semibold shadow-[0_12px_28px_rgba(79,70,229,0.2)]"
            :loading="loading"
            :disabled="ctaDisabled"
            @click="onSubmit"
          >
            {{ ctaLabel }}
          </PrimaryButton>
          <p class="text-center text-xs text-slate-400">选好后点此开始；将进入诊断与规划。</p>
          <p class="text-center text-[11px] text-slate-300">
            {{ HOME_TOPIC_SLOT_COUNT }} 个展示位 · {{ HOME_LIVE_TOPIC_COUNT }} 个可学 · 408+ 可扩展
          </p>
        </div>
      </div>
    </main>
  </PageContainer>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
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
  getHomeTopic,
  HOME_DEFAULT_TOPIC_KEY,
  HOME_LIVE_TOPIC_COUNT,
  HOME_QUICK_STARTS,
  HOME_SUBJECTS,
  HOME_TOPIC_SLOT_COUNT,
} from '@/constants/homeQuickStart'

const router = useRouter()
const store = useWorkflowStore()

const initialTopic = getHomeTopic(HOME_DEFAULT_TOPIC_KEY)
const selectedTopicKey = ref(HOME_DEFAULT_TOPIC_KEY)
const selectedQuickStartKey = ref<(typeof HOME_QUICK_STARTS)[number]['key']>(
  initialTopic.recommendedIntent ?? 'structure'
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
  const t = selectedTopic.value
  if (t.availability !== 'live') return ''
  return t.previewBody
})

const ctaDisabled = computed(
  () => selectedTopic.value.availability !== 'live' || !selectedQuickStart.value
)

const ctaLabel = computed(() => {
  if (selectedTopic.value.availability !== 'live') return '该知识点未开放'
  return '开始这轮学习'
})

watch(
  () => selectedTopicKey.value,
  (key) => {
    const t = getHomeTopic(key)
    if (t.availability === 'live' && t.recommendedIntent) {
      selectedQuickStartKey.value = t.recommendedIntent
    }
  }
)

function chipClass(topicKey: string) {
  const topic = getHomeTopic(topicKey)
  const isSelected = selectedTopicKey.value === topicKey

  if (topic.availability === 'live') {
    if (isSelected) {
      return 'border-primary bg-primary text-white shadow-sm'
    }
    return 'border-indigo-200/80 bg-indigo-50/90 text-indigo-800 hover:border-primary/50'
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

async function onSubmit() {
  if (ctaDisabled.value || !selectedQuickStart.value) {
    if (selectedTopic.value.availability !== 'live') {
      showToast('请先选择已点亮的知识点。')
      return
    }
    showToast('请选择起手方式')
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
