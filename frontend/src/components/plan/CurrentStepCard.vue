<template>
  <section
    class="rounded-card border-2 bg-white p-6 md:p-8"
    :class="cardShellClass"
    aria-labelledby="current-step-heading"
  >
    <p
      class="text-sm font-semibold"
      :class="methodSelection ? 'text-violet-800' : 'text-primary'"
    >
      {{ leadIn }}
    </p>

    <div
      class="mt-4 rounded-lg border border-primary/25 bg-primary/[0.06] px-4 py-3 text-sm leading-snug text-text-primary md:text-base"
    >
      <p class="font-semibold text-text-primary">
        <span aria-hidden="true">👉</span>
        你只需要做一件事
      </p>
      <p class="mt-1.5 font-medium leading-snug text-text-primary/95">
        {{ singleActionLine }}
      </p>
    </div>

    <div
      class="mt-4 rounded-lg border border-amber-200/80 bg-amber-50/90 px-4 py-3 text-sm leading-snug text-text-primary"
    >
      <p class="font-semibold text-amber-900/90">
        <span aria-hidden="true">👉</span>
        {{ whyHeading }}
      </p>
      <p class="mt-1.5 line-clamp-2 text-text-primary/95">
        {{ step.whyThisStep }}
      </p>
    </div>

    <h2
      id="current-step-heading"
      class="mt-5 text-xl font-bold leading-snug text-text-primary md:text-2xl"
    >
      {{ step.headline }}
    </h2>

    <template v-if="step.objectiveBullets?.length && step.objectiveIntro">
      <p
        class="mt-3 line-clamp-2 text-sm font-medium leading-snug text-text-primary md:text-base"
      >
        <span aria-hidden="true">👉</span>
        {{ step.objectiveIntro }}
      </p>
      <ul
        class="mt-2 list-disc space-y-1.5 pl-5 text-sm leading-snug text-text-primary md:text-base"
      >
        <li
          v-for="(line, i) in step.objectiveBullets"
          :key="i"
          class="line-clamp-2"
        >
          {{ line }}
        </li>
      </ul>
    </template>
    <p
      v-else-if="step.oneLineObjective?.trim()"
      class="mt-3 line-clamp-2 text-sm font-medium leading-snug text-text-primary md:text-base"
    >
      <span aria-hidden="true">👉</span>
      {{ step.oneLineObjective }}
    </p>

    <p
      v-if="showcase && step.timeLabel"
      class="mt-4 text-xs font-medium text-text-secondary md:text-sm"
    >
      {{ step.timeLabel }}
    </p>

    <div class="mt-5 space-y-4 text-sm leading-snug text-text-primary md:text-base">
      <div class="rounded-lg border border-border bg-slate-50/90 p-4">
        <p class="font-semibold text-text-primary">
          <span aria-hidden="true">👉</span>
          先复制这句话，去问 AI
        </p>
        <p
          class="mt-2 rounded-md border px-3 py-2.5 text-text-primary"
          :class="aiPromptBoxClass"
        >
          「{{ step.actionGuide.aiPrompt }}」
        </p>
        <div class="mt-3 flex flex-wrap gap-2">
          <SecondaryButton class="text-sm" @click="copyAiPrompt">
            复制这句话
          </SecondaryButton>
          <SecondaryButton class="text-sm" @click="openAiMock">
            打开 AI（演示）
          </SecondaryButton>
        </div>
      </div>

      <div class="rounded-lg border border-border bg-white px-4 py-3">
        <p class="font-semibold text-text-primary">
          <span aria-hidden="true">👉</span>
          {{ labelStep2Reflect }}
        </p>
        <ul class="mt-2 list-disc space-y-1.5 pl-5">
          <li
            v-for="(q, i) in limitedReflections"
            :key="i"
            class="line-clamp-2"
          >
            {{ q }}
          </li>
        </ul>
      </div>

      <div class="rounded-lg border border-border bg-white px-4 py-3">
        <p class="font-semibold text-text-primary">
          <span aria-hidden="true">👉</span>
          {{ labelStep3Closing }}
        </p>
        <p class="mt-2 line-clamp-2 text-text-primary/95">
          {{ step.actionGuide.closingLine }}
        </p>
      </div>
    </div>

    <div class="mt-8">
      <PrimaryButton
        class="w-full justify-center py-3.5 text-base md:w-auto md:min-w-[260px]"
        :loading="loading"
        :disabled="disabled"
        @click="$emit('start')"
      >
        <span class="mr-1" aria-hidden="true">👉</span>
        好，开始这一小步
      </PrimaryButton>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import PrimaryButton from '@/components/ui/PrimaryButton.vue'
import SecondaryButton from '@/components/ui/SecondaryButton.vue'
import { showToast } from '@/stores/toast'
import type { PlanStep } from '@/utils/planPresentationModel'

const props = withDefaults(
  defineProps<{
    step: PlanStep
    loading?: boolean
    disabled?: boolean
    showcaseFocus?: 'default' | 'method-selection'
  }>(),
  { showcaseFocus: 'default' }
)

defineEmits<{
  start: []
}>()

const showcase = computed(() => props.step.uiVariant === 'showcase')

const methodSelection = computed(
  () => showcase.value && props.showcaseFocus === 'method-selection'
)

function pickSingleAction(step: PlanStep): string {
  const o = step.oneLineObjective?.trim()
  if (o) {
    const first = o.split(/[。！？\n]/)[0]?.trim() || o
    return first.length > 80 ? `${first.slice(0, 77)}…` : first
  }
  const intro = step.objectiveIntro?.trim()
  if (intro) {
    return intro.length > 80 ? `${intro.slice(0, 77)}…` : intro
  }
  const h = step.headline?.trim()
  if (h) return h.length > 80 ? `${h.slice(0, 77)}…` : h
  return '跟着下面这句话去问一遍，就已经算推进了一大步。'
}

const singleActionLine = computed(() => pickSingleAction(props.step))

const limitedReflections = computed(() =>
  (props.step.actionGuide.reflectionQuestions ?? []).slice(0, 2)
)

async function copyAiPrompt() {
  const text = props.step.actionGuide.aiPrompt?.trim() ?? ''
  if (!text) {
    showToast('暂无可复制的内容')
    return
  }
  try {
    await navigator.clipboard.writeText(text)
    showToast('已复制，去粘贴到 AI 里即可')
  } catch {
    showToast('复制失败，请手动选中复制')
  }
}

function openAiMock() {
  showToast('演示：这里会打开你常用的 AI 对话窗口')
}

const cardShellClass = computed(() => {
  if (!showcase.value) {
    return 'border-primary/35 shadow-lg shadow-primary/10 ring-1 ring-primary/20'
  }
  if (methodSelection.value) {
    return 'border-violet-400/45 shadow-lg shadow-violet-500/10 ring-2 ring-violet-400/35'
  }
  return 'border-primary/35 shadow-xl shadow-primary/[0.12] ring-2 ring-primary/30'
})

const aiPromptBoxClass = computed(() => {
  if (!showcase.value) {
    return 'border-border bg-white text-sm md:text-base'
  }
  if (methodSelection.value) {
    return 'border-violet-400/35 bg-violet-50/90 text-sm font-medium md:text-base'
  }
  return 'border-primary/30 bg-primary/[0.08] text-sm font-medium md:text-base'
})

const leadIn = computed(() =>
  showcase.value ? '你现在要做的是👇' : '下面这一小步，咱们一起走：'
)

const whyHeading = computed(() =>
  showcase.value ? '为什么要先做这一步？' : '为什么要先从这里开始？'
)

const labelStep2Reflect = computed(() =>
  showcase.value ? '看完后，先自己想两个问题：' : '看完后，先自己想一下：'
)

const labelStep3Closing = computed(() =>
  showcase.value ? '最后用你自己的话收个尾：' : '最后用你自己的话收个尾：'
)
</script>
