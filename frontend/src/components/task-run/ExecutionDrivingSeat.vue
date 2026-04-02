<template>
  <section data-testid="execution-driving-seat" class="space-y-8">
    <!-- 核心：脚手架 = 主入口 -->
    <div>
      <p class="text-sm font-semibold text-slate-950">选择一种方式开始</p>
      <p class="mt-1 text-xs text-slate-500">点按钮即开始；需要补全时会在下方输入框继续写。</p>
      <div class="mt-4 flex flex-col gap-3">
        <button
          v-for="card in cards"
          :key="card.id"
          type="button"
          :disabled="sending"
          class="group flex w-full flex-col rounded-[22px] border-2 border-slate-200 bg-white px-5 py-4 text-left shadow-sm transition hover:border-primary hover:shadow-md disabled:cursor-not-allowed disabled:opacity-60"
          @click="onScaffoldClick(card)"
        >
          <span class="text-base font-semibold text-slate-950">{{ card.actionLabel }}</span>
          <span
            v-if="card.hint?.trim()"
            class="mt-1 text-sm leading-6 text-slate-600"
          >
            {{ card.hint }}
          </span>
        </button>
      </div>
      <div
        v-if="phasePromptChips?.length"
        class="mt-4 flex flex-wrap gap-2"
      >
        <button
          v-for="(line, i) in phasePromptChips"
          :key="i"
          type="button"
          :disabled="sending"
          class="max-w-full rounded-full border border-slate-200 bg-slate-50 px-3 py-1.5 text-left text-xs font-medium leading-snug text-slate-800 transition hover:border-primary hover:bg-white disabled:opacity-60"
          @click="$emit('prefill-chip', line)"
        >
          {{ line }}
        </button>
      </div>
    </div>

    <!-- 执行区：输入 -->
    <div
      class="rounded-[28px] border border-slate-200 bg-[linear-gradient(180deg,_rgba(255,255,255,1),_rgba(248,250,252,0.96))] p-5 shadow-card md:p-7"
    >
      <h2 class="text-lg font-semibold tracking-tight text-slate-950">你的尝试</h2>

      <details class="mt-3">
        <summary
          class="cursor-pointer text-xs font-medium text-slate-500 hover:text-slate-800"
        >
          对话记录（{{ chatTurns.length }}）
        </summary>
        <div
          v-if="chatTurns.length"
          class="mt-3 max-h-40 space-y-3 overflow-y-auto rounded-[20px] border border-slate-100 bg-slate-50/50 p-3"
        >
          <div
            v-for="(t, i) in chatTurns"
            :key="i"
            class="rounded-[18px] border px-3 py-2.5 text-sm"
            :class="
              t.role === 'ASSISTANT'
                ? 'border-primary/20 bg-primary/5'
                : 'border-slate-200 bg-white'
            "
          >
            <p class="text-xs font-semibold uppercase tracking-[0.14em] text-slate-500">
              {{ t.role === 'USER' ? '你' : '导师' }}
            </p>
            <p class="mt-1.5 whitespace-pre-wrap leading-7 text-slate-800">
              {{ t.content }}
            </p>
          </div>
        </div>
        <p
          v-else
          class="mt-3 rounded-[16px] border border-dashed border-slate-200 px-3 py-3 text-center text-xs text-slate-500"
        >
          还没有记录。点上方按钮，或直接在下面输入后提交。
        </p>
      </details>

      <label class="mt-5 block">
        <span class="text-sm font-semibold text-slate-950">{{ inputLabel }}</span>
        <textarea
          data-testid="driving-seat-input"
          :value="draftValue"
          rows="4"
          class="mt-2 w-full rounded-[22px] border border-slate-200 bg-white px-4 py-3 text-sm leading-7 text-slate-900 shadow-sm outline-none transition focus:border-primary focus:ring-4 focus:ring-primary/10"
          :placeholder="inputPlaceholderSoft || inputPlaceholder"
          :disabled="sending"
          @input="$emit('update:draftValue', ($event.target as HTMLTextAreaElement).value)"
        />
      </label>
      <div class="mt-4 flex flex-wrap items-center gap-3">
        <PrimaryButton
          data-testid="driving-seat-send"
          :loading="sending"
          :disabled="!canSubmitChat"
          @click="$emit('submit-chat')"
        >
          {{ primaryActionLabel }}
        </PrimaryButton>
      </div>
    </div>

    <div
      v-if="showRestate"
      class="rounded-[28px] border border-accent/25 bg-accent-muted/40 p-5 shadow-sm md:p-7"
    >
      <h2 class="text-lg font-semibold tracking-tight text-slate-950">用自己的话再写三行</h2>
      <p class="mt-1 text-sm text-slate-600">不要求一次说对。</p>
      <div class="mt-5 space-y-4">
        <label class="block">
          <span class="text-sm font-medium text-slate-800">它是什么</span>
          <input
            :value="restateWhat"
            type="text"
            class="mt-2 w-full rounded-[18px] border border-slate-200 bg-white px-4 py-2.5 text-sm text-slate-900 outline-none focus:border-primary focus:ring-2 focus:ring-primary/15"
            placeholder="一句话定位"
            @input="$emit('update:restateWhat', ($event.target as HTMLInputElement).value)"
          />
        </label>
        <label class="block">
          <span class="text-sm font-medium text-slate-800">它解决什么问题</span>
          <input
            :value="restateProblem"
            type="text"
            class="mt-2 w-full rounded-[18px] border border-slate-200 bg-white px-4 py-2.5 text-sm text-slate-900 outline-none focus:border-primary focus:ring-2 focus:ring-primary/15"
            placeholder="它在帮什么忙"
            @input="$emit('update:restateProblem', ($event.target as HTMLInputElement).value)"
          />
        </label>
        <label class="block">
          <span class="text-sm font-medium text-slate-800">它和谁有关</span>
          <input
            :value="restateRelate"
            type="text"
            class="mt-2 w-full rounded-[18px] border border-slate-200 bg-white px-4 py-2.5 text-sm text-slate-900 outline-none focus:border-primary focus:ring-2 focus:ring-primary/15"
            placeholder="相关概念或相邻知识点"
            @input="$emit('update:restateRelate', ($event.target as HTMLInputElement).value)"
          />
        </label>
      </div>
    </div>

    <div
      v-if="showAdvance"
      class="rounded-[28px] border border-emerald-200/90 bg-emerald-50/40 p-5 md:p-7"
    >
      <p class="text-sm font-semibold text-slate-900">微检查</p>
      <p class="mt-1 text-sm text-slate-600">勾一下，确认你真的过关了。</p>
      <div class="mt-4 space-y-3">
        <label
          v-for="(label, i) in microCheckLabels"
          :key="i"
          class="flex cursor-pointer items-start gap-3 text-sm leading-6 text-slate-800"
        >
          <input
            type="checkbox"
            class="mt-1 h-4 w-4 rounded border-slate-300 text-primary focus:ring-primary/30"
            :checked="checks[i]"
            @change="toggleCheck(i)"
          />
          <span>{{ label }}</span>
        </label>
      </div>
      <PrimaryButton
        class="mt-6"
        :disabled="!canAdvance"
        :loading="advancing"
        @click="$emit('advance')"
      >
        {{ advanceLabel }}
      </PrimaryButton>
    </div>
  </section>
</template>

<script setup lang="ts">
import PrimaryButton from '@/components/ui/PrimaryButton.vue'
import type { ExecutionScaffoldCardModel } from '@/types/executionGuide'

interface Turn {
  role: 'USER' | 'ASSISTANT'
  content: string
}

const props = defineProps<{
  cards: ExecutionScaffoldCardModel[]
  /** 本阶段线索短句（点击填入输入框） */
  phasePromptChips?: string[]
  chatTurns: Turn[]
  draftValue: string
  inputLabel: string
  inputPlaceholder: string
  inputPlaceholderSoft?: string
  primaryActionLabel: string
  sending: boolean
  canSubmitChat: boolean
  showRestate: boolean
  showAdvance: boolean
  microCheckLabels: string[]
  checks: boolean[]
  canAdvance: boolean
  advancing?: boolean
  advanceLabel: string
  restateWhat: string
  restateProblem: string
  restateRelate: string
}>()

const emit = defineEmits<{
  'send-card': [prompt: string]
  'prefill-card': [prompt: string]
  'prefill-chip': [text: string]
  'submit-chat': []
  advance: []
  'update:draftValue': [value: string]
  'update:restateWhat': [value: string]
  'update:restateProblem': [value: string]
  'update:restateRelate': [value: string]
  'update:checks': [value: boolean[]]
}>()

function onScaffoldClick(card: ExecutionScaffoldCardModel) {
  if (card.behavior === 'prefill') {
    emit('prefill-card', card.prompt)
    return
  }
  emit('send-card', card.prompt)
}

function toggleCheck(index: number) {
  const next = [...props.checks]
  next[index] = !next[index]
  emit('update:checks', next)
}
</script>
