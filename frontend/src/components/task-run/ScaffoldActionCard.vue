<template>
  <section
    data-testid="scaffold-action-card"
    class="rounded-2xl border border-slate-200 bg-slate-50/50 p-5 md:p-6"
  >
    <h2 class="text-sm font-semibold text-slate-950">学习动作脚手架</h2>
    <p class="mt-1 text-xs text-slate-500">按下面结构完成输出；需要时再用快捷动作。</p>

    <div class="mt-4 space-y-4">
      <div>
        <p class="text-xs font-semibold uppercase tracking-wide text-slate-500">你现在要输出</p>
        <ul class="mt-2 list-inside list-disc space-y-1 text-sm leading-relaxed text-slate-800">
          <li v-for="(line, i) in product.whatToOutput" :key="`out-${i}`">{{ line }}</li>
        </ul>
      </div>
      <div>
        <p class="text-xs font-semibold uppercase tracking-wide text-slate-500">推荐思路</p>
        <ul class="mt-2 space-y-1.5 text-sm text-slate-700">
          <li v-for="(line, i) in product.recommendedSteps" :key="`rec-${i}`" class="flex gap-2">
            <span class="text-slate-400">{{ i + 1 }}.</span>
            <span>{{ line }}</span>
          </li>
        </ul>
      </div>
      <div class="rounded-xl border border-amber-200/80 bg-amber-50/40 px-4 py-3">
        <p class="text-xs font-semibold text-amber-900/90">不要这样做</p>
        <ul class="mt-2 space-y-1 text-sm text-amber-950/90">
          <li v-for="(line, i) in product.avoid" :key="`avoid-${i}`" class="flex gap-2">
            <span>×</span>
            <span>{{ line }}</span>
          </li>
        </ul>
      </div>
    </div>

    <div class="mt-5 border-t border-slate-200/80 pt-5">
      <p class="text-xs font-medium text-slate-500">快捷开始</p>
      <div class="mt-3 flex flex-col gap-2">
        <button
          v-for="card in cards"
          :key="card.id"
          type="button"
          :disabled="sending"
          class="flex w-full flex-col rounded-xl border px-4 py-3 text-left transition disabled:opacity-60"
          :class="
            card.id === cards[0]?.id
              ? 'border-primary/40 bg-white shadow-sm hover:border-primary'
              : 'border-slate-200 bg-white/80 hover:border-slate-300'
          "
          @click="onCardClick(card)"
        >
          <span class="text-sm font-semibold text-slate-950">{{ card.actionLabel }}</span>
          <span v-if="card.hint?.trim()" class="mt-1 text-xs leading-relaxed text-slate-600">{{
            card.hint
          }}</span>
        </button>
      </div>
      <div v-if="phasePromptChips?.length" class="mt-3 flex flex-wrap gap-2">
        <button
          v-for="(line, i) in phasePromptChips"
          :key="`chip-${i}`"
          type="button"
          :disabled="sending"
          class="rounded-full border border-slate-200 bg-white px-3 py-1 text-left text-xs font-medium text-slate-800 transition hover:border-primary/40 disabled:opacity-60"
          @click="$emit('prefill-chip', line)"
        >
          {{ line }}
        </button>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import type { ExecutionScaffoldCardModel } from '@/types/executionGuide'
import type { ScaffoldProductModel } from '@/types/taskExecutionWorkbench'

const props = defineProps<{
  product: ScaffoldProductModel
  cards: ExecutionScaffoldCardModel[]
  phasePromptChips?: string[]
  sending: boolean
}>()

const emit = defineEmits<{
  'send-card': [prompt: string]
  'prefill-card': [prompt: string]
  'prefill-chip': [text: string]
}>()

function onCardClick(card: ExecutionScaffoldCardModel) {
  if (card.behavior === 'prefill') {
    emit('prefill-card', card.prompt)
    return
  }
  emit('send-card', card.prompt)
}
</script>
