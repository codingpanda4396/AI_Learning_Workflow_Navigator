<template>
  <section
    data-testid="user-expression-panel"
    class="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm md:p-6"
    :class="panelEmphasisClass"
  >
    <div class="flex flex-wrap items-end justify-between gap-2">
      <div>
        <h2 class="text-lg font-semibold tracking-tight text-slate-950">你的表达区</h2>
        <p class="mt-1 text-xs text-slate-500">{{ helperText }}</p>
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

    <div class="mt-5 grid gap-4">
      <label
        v-for="(field, index) in structuredFields"
        :key="field.id"
        class="block"
      >
        <span class="text-sm font-semibold text-slate-950">{{ field.label }}</span>
        <textarea
          v-if="field.multiline"
          :value="structuredInputs[index] ?? ''"
          :rows="3"
          class="mt-2 w-full rounded-2xl border border-slate-200 bg-slate-50/50 px-4 py-3 text-sm leading-7 text-slate-900 outline-none transition focus:border-primary focus:bg-white focus:ring-4 focus:ring-primary/10"
          :placeholder="field.placeholder"
          :disabled="sending"
          @input="updateStructured(index, ($event.target as HTMLTextAreaElement).value)"
        />
        <input
          v-else
          :value="structuredInputs[index] ?? ''"
          type="text"
          class="mt-2 w-full rounded-2xl border border-slate-200 bg-slate-50/50 px-4 py-3 text-sm text-slate-900 outline-none transition focus:border-primary focus:bg-white focus:ring-4 focus:ring-primary/10"
          :placeholder="field.placeholder"
          :disabled="sending"
          @input="updateStructured(index, ($event.target as HTMLInputElement).value)"
        />
      </label>
    </div>

    <p class="mt-4 text-xs text-slate-500">{{ lowFrictionPrompt }}</p>

    <details class="mt-5">
      <summary class="cursor-pointer text-xs font-medium text-slate-400 hover:text-slate-700">
        辅助草稿 / 对话记录（{{ chatTurns.length }}）
      </summary>
      <div class="mt-3 grid gap-3">
        <label class="block">
          <span class="text-xs font-medium text-slate-500">{{ inputLabel }}</span>
          <textarea
            data-testid="driving-seat-input"
            :value="draftValue"
            :rows="4"
            class="mt-2 w-full rounded-2xl border border-slate-200 bg-slate-50/60 px-4 py-3 text-sm leading-7 text-slate-900 outline-none transition focus:border-primary focus:bg-white focus:ring-4 focus:ring-primary/10"
            :placeholder="inputPlaceholderSoft || inputPlaceholder"
            :disabled="sending"
            @input="$emit('update:draft-value', ($event.target as HTMLTextAreaElement).value)"
          />
        </label>

        <div
          v-if="chatTurns.length"
          class="max-h-40 space-y-2 overflow-y-auto rounded-xl border border-slate-100 bg-slate-50/80 p-3"
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
            <p class="mt-1 whitespace-pre-wrap leading-relaxed text-slate-800">{{ t.content }}</p>
          </div>
        </div>
      </div>
    </details>

    <details v-if="showAdvance" class="mt-4 rounded-xl border border-emerald-200/80 bg-emerald-50/30 p-4">
      <summary class="cursor-pointer text-sm font-semibold text-slate-900">微检查（进入下一阶段前）</summary>
      <p class="mt-2 text-xs text-slate-600">勾齐后，用底部「进入下一步」。</p>
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
import type {
  WorkbenchExpressionFieldModel,
  WorkbenchPhaseCode,
} from '@/types/taskExecutionWorkbench'

interface ChatTurn {
  role: 'USER' | 'ASSISTANT'
  content: string
  detectedAction?: string
}

const props = withDefaults(
  defineProps<{
    chatTurns: ChatTurn[]
    draftValue: string
    inputLabel: string
    inputPlaceholder: string
    inputPlaceholderSoft?: string
    sending: boolean
    microCheckLabels: string[]
    checks: boolean[]
    emphasisPhase: WorkbenchPhaseCode
    showStuck?: boolean
    showAdvance?: boolean
    helperText?: string
    lowFrictionPrompt?: string
    structuredFields?: WorkbenchExpressionFieldModel[]
    structuredInputs?: string[]
  }>(),
  {
    showStuck: true,
    showAdvance: false,
    helperText: '不确定也没关系，先写当前最稳的一版。',
    lowFrictionPrompt: '先把你现在能确认的部分写出来。',
    structuredFields: () => [],
    structuredInputs: () => [],
  }
)

const emit = defineEmits<{
  'update:draft-value': [value: string]
  'update:structured-inputs': [value: string[]]
  'update:checks': [value: boolean[]]
  'save-draft': []
  stuck: []
}>()

const panelEmphasisClass = computed(() =>
  props.emphasisPhase === 'TRAINING'
    ? 'ring-2 ring-primary/25 shadow-md'
    : props.emphasisPhase === 'STRUCTURE'
      ? 'ring-1 ring-slate-200/90'
      : ''
)

function toggleCheck(index: number) {
  const next = [...props.checks]
  next[index] = !next[index]
  emit('update:checks', next)
}

function updateStructured(index: number, value: string) {
  const next = [...props.structuredInputs]
  next[index] = value
  emit('update:structured-inputs', next)
}
</script>
