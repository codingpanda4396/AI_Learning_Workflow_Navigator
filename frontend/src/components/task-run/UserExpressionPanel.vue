<template>
  <section
    data-testid="user-expression-panel"
    class="rounded-2xl border border-slate-200 bg-[linear-gradient(180deg,_#fff,_#f8fafc)] p-5 shadow-sm md:p-7"
    :class="panelEmphasisClass"
  >
    <div class="flex flex-wrap items-end justify-between gap-2">
      <div>
        <h2 class="text-lg font-semibold tracking-tight text-slate-950">我的表达</h2>
        <p class="mt-1 text-xs text-slate-500">
          先写一版不完整的也行，提交后再根据反馈改。
        </p>
      </div>
      <button
        v-if="showStuck"
        type="button"
        class="text-xs font-medium text-slate-500 underline-offset-2 hover:text-primary hover:underline"
        @click="$emit('stuck')"
      >
        我卡住了
      </button>
    </div>

    <details class="mt-3">
      <summary
        class="cursor-pointer text-xs font-medium text-slate-400 hover:text-slate-700"
      >
        对话记录（{{ chatTurns.length }}）
      </summary>
      <div
        v-if="chatTurns.length"
        class="mt-3 max-h-36 space-y-2 overflow-y-auto rounded-xl border border-slate-100 bg-white/80 p-3"
      >
        <div
          v-for="(t, i) in chatTurns"
          :key="i"
          class="rounded-lg border px-3 py-2 text-sm"
          :class="t.role === 'ASSISTANT' ? 'border-primary/15 bg-primary/5' : 'border-slate-200 bg-white'"
        >
          <p class="text-[10px] font-semibold uppercase tracking-wide text-slate-500">
            {{ t.role === 'USER' ? '你' : '导师' }}
          </p>
          <p class="mt-1 whitespace-pre-wrap leading-relaxed text-slate-800">{{ t.content }}</p>
        </div>
      </div>
      <p v-else class="mt-2 text-center text-xs text-slate-400">暂无</p>
    </details>

    <label class="mt-5 block">
      <span class="text-sm font-semibold text-slate-950">{{ inputLabel }}</span>
      <textarea
        data-testid="driving-seat-input"
        :value="draftValue"
        :rows="textareaRows"
        class="mt-2 w-full rounded-2xl border border-slate-200 bg-white px-4 py-3 text-sm leading-7 text-slate-900 shadow-sm outline-none transition focus:border-primary focus:ring-4 focus:ring-primary/10"
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

    <div
      v-if="showRestate"
      class="mt-8 rounded-2xl border border-amber-200/80 bg-amber-50/35 p-5"
    >
      <h3 class="text-base font-semibold text-slate-950">用自己的话再写三行</h3>
      <p class="mt-1 text-sm text-slate-600">不要求一次说对。</p>
      <div class="mt-4 space-y-4">
        <label class="block">
          <span class="text-sm font-medium text-slate-800">它是什么</span>
          <input
            :value="restateWhat"
            type="text"
            class="mt-2 w-full rounded-xl border border-slate-200 bg-white px-4 py-2.5 text-sm text-slate-900 outline-none focus:border-primary focus:ring-2 focus:ring-primary/15"
            placeholder="一句话定位"
            @input="$emit('update:restateWhat', ($event.target as HTMLInputElement).value)"
          />
        </label>
        <label class="block">
          <span class="text-sm font-medium text-slate-800">它解决什么问题</span>
          <input
            :value="restateProblem"
            type="text"
            class="mt-2 w-full rounded-xl border border-slate-200 bg-white px-4 py-2.5 text-sm text-slate-900 outline-none focus:border-primary focus:ring-2 focus:ring-primary/15"
            placeholder="它在帮什么忙"
            @input="$emit('update:restateProblem', ($event.target as HTMLInputElement).value)"
          />
        </label>
        <label class="block">
          <span class="text-sm font-medium text-slate-800">它和谁有关</span>
          <input
            :value="restateRelate"
            type="text"
            class="mt-2 w-full rounded-xl border border-slate-200 bg-white px-4 py-2.5 text-sm text-slate-900 outline-none focus:border-primary focus:ring-2 focus:ring-primary/15"
            placeholder="相关概念"
            @input="$emit('update:restateRelate', ($event.target as HTMLInputElement).value)"
          />
        </label>
      </div>
    </div>

    <div
      v-if="showAdvance"
      class="mt-6 rounded-2xl border border-emerald-200/90 bg-emerald-50/40 p-5"
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
      <PrimaryButton class="mt-6" :disabled="!canAdvance" :loading="advancing" @click="$emit('advance')">
        {{ advanceLabel }}
      </PrimaryButton>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import PrimaryButton from '@/components/ui/PrimaryButton.vue'
import type { WorkbenchPhaseCode } from '@/types/taskExecutionWorkbench'

interface Turn {
  role: 'USER' | 'ASSISTANT'
  content: string
}

const props = withDefaults(
  defineProps<{
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
    emphasisPhase: WorkbenchPhaseCode
    showStuck?: boolean
  }>(),
  { showStuck: true }
)

const emit = defineEmits<{
  'update:draftValue': [value: string]
  'update:restateWhat': [value: string]
  'update:restateProblem': [value: string]
  'update:restateRelate': [value: string]
  'update:checks': [value: boolean[]]
  'submit-chat': []
  advance: []
  stuck: []
}>()

const textareaRows = computed(() => (props.emphasisPhase === 'TRAINING' ? 8 : 5))

const panelEmphasisClass = computed(() =>
  props.emphasisPhase === 'TRAINING'
    ? 'ring-2 ring-primary/20 shadow-md'
    : props.emphasisPhase === 'STRUCTURE'
      ? 'opacity-[0.98]'
      : ''
)

function toggleCheck(index: number) {
  const next = [...props.checks]
  next[index] = !next[index]
  emit('update:checks', next)
}
</script>
