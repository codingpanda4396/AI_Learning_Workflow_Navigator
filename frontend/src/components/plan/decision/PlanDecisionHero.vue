<template>
  <section class="rounded-2xl border border-slate-200/90 bg-white px-5 py-6 shadow-sm md:px-8 md:py-8">
    <h1 class="text-xl font-semibold tracking-tight text-slate-950 md:text-2xl">
      {{ model.title }}
    </h1>
    <p
      v-if="model.goalHint"
      class="mt-3 text-xs text-slate-500"
    >
      {{ model.goalHint }}
    </p>
    <div class="mt-6 space-y-3 text-[15px] leading-7 text-slate-800 md:text-base">
      <p>你当前最该先解决的是：{{ model.blockerText }}</p>
      <p>
        所以第一步不是 <span class="font-medium text-slate-600">{{ model.wrongActionText }}</span>，而是
        <span class="font-semibold text-slate-900">{{ model.correctActionText }}</span>
      </p>
      <p class="text-slate-700">先把这一步站稳，再往后推。</p>
    </div>
    <div class="mt-8 flex flex-col gap-2">
      <PrimaryButton
        class="w-full justify-center sm:w-auto sm:min-w-[200px]"
        :loading="loading"
        :disabled="disabled"
        @click="$emit('start')"
      >
        {{ model.ctaLabel }}
      </PrimaryButton>
      <p
        v-if="model.ctaSubtext"
        class="text-xs text-slate-500"
      >
        {{ model.ctaSubtext }}
      </p>
      <p class="text-xs text-slate-400">
        进入下一页后，系统会一步步带你完成这一任务。
      </p>
    </div>
  </section>
</template>

<script setup lang="ts">
import PrimaryButton from '@/components/ui/PrimaryButton.vue'
import type { LearningPlanDecisionViewModel } from '@/types/learningPlanDecision'

defineProps<{
  model: LearningPlanDecisionViewModel['hero']
  loading?: boolean
  disabled?: boolean
}>()

defineEmits<{
  start: []
}>()
</script>
