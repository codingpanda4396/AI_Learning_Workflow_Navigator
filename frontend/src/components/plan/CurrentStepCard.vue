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
      class="mt-4 rounded-lg border border-amber-200/80 bg-amber-50/90 px-4 py-3 text-sm leading-relaxed text-text-primary"
    >
      <p class="font-semibold text-amber-900/90">
        <span aria-hidden="true">👉</span>
        {{ whyHeading }}
      </p>
      <p class="mt-1.5 whitespace-pre-line text-text-primary/95">
        {{ step.whyThisStep }}
      </p>
    </div>

    <h2
      id="current-step-heading"
      class="mt-6 text-xl font-bold leading-snug text-text-primary md:text-2xl"
    >
      {{ step.headline }}
    </h2>

    <template v-if="step.objectiveBullets?.length && step.objectiveIntro">
      <p
        class="mt-4 text-base leading-relaxed text-text-primary"
        :class="showcase ? 'font-semibold text-[1.05rem]' : 'font-medium'"
      >
        <span aria-hidden="true">👉</span>
        {{ step.objectiveIntro }}
      </p>
      <ul
        class="mt-2 list-disc pl-5 text-base leading-relaxed text-text-primary"
        :class="showcase ? 'space-y-2.5' : 'space-y-1.5'"
      >
        <li
          v-for="(line, i) in step.objectiveBullets"
          :key="i"
        >
          {{ line }}
        </li>
      </ul>
    </template>
    <p
      v-else-if="step.oneLineObjective?.trim()"
      class="mt-3 text-base font-medium leading-relaxed text-text-primary"
    >
      <span aria-hidden="true">👉</span>
      {{ step.oneLineObjective }}
    </p>

    <p
      v-if="showcase && step.timeLabel"
      class="mt-5 text-sm font-medium text-text-secondary"
    >
      {{ step.timeLabel }}
    </p>

    <div class="mt-6 space-y-5 text-base leading-relaxed text-text-primary">
      <div>
        <p class="font-semibold text-text-primary">
          <span aria-hidden="true">👉</span>
          第一步，先问 AI：
        </p>
        <p
          class="mt-2 rounded-lg border px-4 py-3 leading-relaxed text-text-primary"
          :class="aiPromptBoxClass"
        >
          「{{ step.actionGuide.aiPrompt }}」
        </p>
      </div>

      <div>
        <p class="font-semibold text-text-primary">
          <span aria-hidden="true">👉</span>
          {{ labelStep2Reflect }}
        </p>
        <ul class="mt-2 list-disc space-y-2 pl-5 text-text-primary">
          <li
            v-for="(q, i) in step.actionGuide.reflectionQuestions"
            :key="i"
            class="leading-relaxed"
          >
            {{ q }}
          </li>
        </ul>
      </div>

      <div>
        <p class="font-semibold text-text-primary">
          <span aria-hidden="true">👉</span>
          {{ labelStep3Closing }}
        </p>
        <p class="mt-2 leading-relaxed text-text-primary/95">
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
import type { PlanStep } from '@/utils/planPresentationModel'

const props = withDefaults(
  defineProps<{
    step: PlanStep
    loading?: boolean
    disabled?: boolean
    /** 演示知识点气质：method-selection 时强化「判断感」与层级区分 */
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
    return 'border-border bg-slate-50/90 text-sm md:text-base'
  }
  if (methodSelection.value) {
    return 'border-violet-400/35 bg-violet-50/90 py-3.5 text-base font-medium md:text-[1.05rem]'
  }
  return 'border-primary/30 bg-primary/[0.08] py-3.5 text-base font-medium md:text-[1.05rem]'
})

const leadIn = computed(() =>
  showcase.value ? '你现在要做的是👇' : '下面这一小步，咱们一起走：'
)

const whyHeading = computed(() =>
  showcase.value ? '为什么先学这个？' : '为什么先从这里开始？'
)

const labelStep2Reflect = computed(() =>
  showcase.value
    ? '第二步，看完后试着自己想一想：'
    : '看完之后，试着自己想一想：'
)

const labelStep3Closing = computed(() =>
  showcase.value
    ? '第三步，用你自己的话讲一遍：'
    : '最后，用你自己的话讲一遍：'
)
</script>
