<template>
  <section
    data-testid="scaffold-action-card"
    class="rounded-2xl border border-primary/20 bg-gradient-to-b from-primary/[0.06] to-slate-50/80 p-5 md:p-6"
  >
    <h2 class="text-base font-semibold text-slate-950">系统会这样带你完成这一步</h2>
    <p class="mt-1 text-xs text-slate-600">受约束的学习动作，不是开放式问答。</p>

    <div class="mt-4 space-y-4">
      <div>
        <p class="text-xs font-semibold uppercase tracking-wide text-slate-500">你现在要产出什么</p>
        <ul class="mt-2 list-inside list-disc space-y-1 text-sm leading-relaxed text-slate-800">
          <li v-for="(line, i) in product.whatToOutput" :key="`out-${i}`">{{ line }}</li>
        </ul>
      </div>
      <div>
        <p class="text-xs font-semibold uppercase tracking-wide text-slate-500">系统会如何引导你</p>
        <ul class="mt-2 space-y-1.5 text-sm text-slate-700">
          <li v-for="(line, i) in product.recommendedSteps" :key="`rec-${i}`" class="flex gap-2">
            <span class="text-slate-400">{{ i + 1 }}.</span>
            <span>{{ line }}</span>
          </li>
        </ul>
      </div>
      <div class="rounded-xl border border-amber-200/80 bg-amber-50/50 px-4 py-3">
        <p class="text-xs font-semibold text-amber-900/90">这一步不要怎么做</p>
        <ul class="mt-2 space-y-1 text-sm text-amber-950/90">
          <li v-for="(line, i) in product.avoid" :key="`avoid-${i}`" class="flex gap-2">
            <span class="shrink-0">×</span>
            <span>{{ line }}</span>
          </li>
        </ul>
      </div>
    </div>

    <div
      v-if="(topicObservationBullets ?? []).length"
      class="mt-4 rounded-lg border border-slate-200/80 bg-white/60 px-3 py-2"
    >
      <p class="text-[10px] font-semibold uppercase tracking-wide text-slate-400">本轮观察点</p>
      <ul class="mt-1.5 space-y-0.5 text-[11px] leading-snug text-slate-600">
        <li v-for="(b, i) in (topicObservationBullets ?? []).slice(0, 4)" :key="i">· {{ b }}</li>
      </ul>
    </div>

    <div class="mt-5 border-t border-slate-200/80 pt-4">
      <button
        v-if="primaryCard"
        type="button"
        :disabled="sending"
        class="flex w-full flex-col rounded-xl border border-primary/35 bg-white px-4 py-3 text-left shadow-sm transition hover:border-primary/55 disabled:opacity-60"
        @click="onPrimaryClick"
      >
        <span class="text-sm font-semibold text-slate-950">{{ primaryCard.actionLabel }}</span>
        <span v-if="primaryCard.hint?.trim()" class="mt-1 text-xs leading-relaxed text-slate-600">{{
          primaryCard.hint
        }}</span>
      </button>
      <details v-if="moreChips.length" class="mt-3">
        <summary class="cursor-pointer text-xs font-medium text-slate-400 hover:text-slate-700">
          更多线索（{{ moreChips.length }}）
        </summary>
        <div class="mt-2 flex flex-wrap gap-2">
          <button
            v-for="(line, i) in moreChips"
            :key="`chip-${i}`"
            type="button"
            :disabled="sending"
            class="rounded-full border border-slate-200 bg-white px-3 py-1 text-left text-xs font-medium text-slate-700 transition hover:border-primary/40 disabled:opacity-60"
            @click="$emit('prefill-chip', line)"
          >
            {{ line }}
          </button>
        </div>
      </details>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { ExecutionScaffoldCardModel } from '@/types/executionGuide'
import type { ScaffoldProductModel } from '@/types/taskExecutionWorkbench'

const props = defineProps<{
  product: ScaffoldProductModel
  cards: ExecutionScaffoldCardModel[]
  phasePromptChips?: string[]
  topicObservationBullets?: string[]
  sending: boolean
}>()

const emit = defineEmits<{
  'send-card': [prompt: string]
  'prefill-card': [prompt: string]
  'prefill-chip': [text: string]
}>()

const primaryCard = computed(() => props.cards[0] ?? null)

const moreChips = computed(() => {
  const chips = props.phasePromptChips?.filter(Boolean) ?? []
  return chips
})

function onPrimaryClick() {
  const card = primaryCard.value
  if (!card) return
  if (card.behavior === 'prefill') {
    emit('prefill-card', card.prompt)
    return
  }
  emit('send-card', card.prompt)
}
</script>
