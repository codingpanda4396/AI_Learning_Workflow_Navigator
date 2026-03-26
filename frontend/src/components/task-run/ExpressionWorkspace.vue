<template>
  <section
    data-testid="user-expression-panel"
    class="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm md:p-6"
    :class="panelEmphasisClass"
  >
    <div class="flex flex-wrap items-end justify-between gap-2">
      <div>
        <h2 class="text-lg font-semibold tracking-tight text-slate-950">我的表达</h2>
        <p class="mt-1 text-xs text-slate-500">不确定也没关系，先写一版；主操作在页面底部。</p>
      </div>
      <div class="flex flex-wrap items-center gap-3">
        <button
          type="button"
          class="text-xs font-medium text-slate-500 underline-offset-2 hover:text-primary hover:underline"
          @click="$emit('save-draft')"
        >
          保存草稿
        </button>
        <button
          v-if="showStuck"
          type="button"
          class="text-xs font-medium text-slate-400 underline-offset-2 hover:text-amber-800 hover:underline"
          @click="$emit('stuck')"
        >
          我卡住了
        </button>
      </div>
    </div>

    <details class="mt-3">
      <summary
        class="cursor-pointer text-xs font-medium text-slate-400 hover:text-slate-700"
      >
        对话记录（{{ chatTurns.length }}）
      </summary>
      <div
        v-if="chatTurns.length"
        class="mt-3 max-h-32 space-y-2 overflow-y-auto rounded-xl border border-slate-100 bg-slate-50/80 p-3"
      >
        <div
          v-for="(t, i) in chatTurns"
          :key="i"
          class="rounded-lg border px-3 py-2 text-sm"
          :class="t.role === 'ASSISTANT' ? 'border-primary/15 bg-primary/5' : 'border-slate-200 bg-white'"
        >
          <p class="text-[10px] font-semibold uppercase tracking-wide text-slate-500">
            {{ t.role === 'USER' ? '你' : '脚手架反馈' }}
          </p>
          <p class="mt-1 line-clamp-6 whitespace-pre-wrap leading-relaxed text-slate-800">{{ t.content }}</p>
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
        class="mt-2 min-h-[140px] w-full rounded-2xl border-2 border-slate-200 bg-slate-50/50 px-4 py-3 text-sm leading-7 text-slate-900 shadow-inner outline-none transition focus:border-primary focus:bg-white focus:ring-4 focus:ring-primary/10 md:min-h-[180px]"
        :placeholder="inputPlaceholderSoft || inputPlaceholder"
        :disabled="sending"
        @input="$emit('update:draftValue', ($event.target as HTMLTextAreaElement).value)"
      />
    </label>

    <details v-if="showRestate" class="mt-6 rounded-xl border border-amber-200/80 bg-amber-50/30 p-4">
      <summary class="cursor-pointer text-sm font-semibold text-slate-900">用自己的话再写三行（过关前）</summary>
      <p class="mt-2 text-xs text-slate-600">不要求一次说对。</p>
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
    </details>

    <details v-if="showAdvance" class="mt-4 rounded-xl border border-emerald-200/80 bg-emerald-50/30 p-4">
      <summary class="cursor-pointer text-sm font-semibold text-slate-900">微检查（进入下一阶段前）</summary>
      <p class="mt-2 text-xs text-slate-600">勾齐后，用底部「进入下一阶段」。</p>
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
    </details>
  </section>
</template>

<script setup lang="ts">
import { computed } from 'vue'
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
    sending: boolean
    showRestate: boolean
    showAdvance: boolean
    microCheckLabels: string[]
    checks: boolean[]
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
  'save-draft': []
  stuck: []
}>()

const textareaRows = computed(() => (props.emphasisPhase === 'TRAINING' ? 10 : 6))

const panelEmphasisClass = computed(() =>
  props.emphasisPhase === 'TRAINING'
    ? 'ring-2 ring-primary/25 shadow-md'
    : props.emphasisPhase === 'STRUCTURE'
      ? ''
      : ''
)

function toggleCheck(index: number) {
  const next = [...props.checks]
  next[index] = !next[index]
  emit('update:checks', next)
}
</script>
