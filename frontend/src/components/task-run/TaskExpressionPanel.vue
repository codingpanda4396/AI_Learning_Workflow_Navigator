<template>
  <section
    data-testid="task-expression-panel"
    class="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm md:p-6"
    :class="panelClass"
  >
    <div class="flex flex-wrap items-end justify-between gap-2">
      <h2 class="text-lg font-semibold tracking-tight text-slate-950">先写你的版本</h2>
      <p v-if="helperText" class="max-w-md text-xs text-slate-500">{{ helperText }}</p>
    </div>

    <div v-if="checkpointPrompt" class="mt-4 rounded-xl border border-amber-200/80 bg-amber-50/60 px-4 py-3">
      <p class="text-xs font-semibold text-amber-950">检查题</p>
      <p class="mt-1 text-sm leading-relaxed text-amber-950/90">{{ checkpointPrompt }}</p>
    </div>

    <div v-if="structuredFields.length" class="mt-5 grid gap-4">
      <label v-for="(field, index) in structuredFields" :key="field.id" class="block">
        <span class="text-sm font-semibold text-slate-950">{{ field.label }}</span>
        <textarea
          v-if="field.multiline"
          :value="structuredInputs[index] ?? ''"
          :rows="3"
          class="mt-2 w-full rounded-2xl border border-slate-200 bg-slate-50/50 px-4 py-3 text-sm leading-7 text-slate-900 outline-none transition focus:border-primary focus:bg-white focus:ring-4 focus:ring-primary/10"
          :placeholder="field.placeholder"
          :disabled="sending"
          @input="emitStructured(index, ($event.target as HTMLTextAreaElement).value)"
        />
        <input
          v-else
          :value="structuredInputs[index] ?? ''"
          type="text"
          class="mt-2 w-full rounded-2xl border border-slate-200 bg-slate-50/50 px-4 py-3 text-sm text-slate-900 outline-none transition focus:border-primary focus:bg-white focus:ring-4 focus:ring-primary/10"
          :placeholder="field.placeholder"
          :disabled="sending"
          @input="emitStructured(index, ($event.target as HTMLInputElement).value)"
        />
      </label>
    </div>

    <label v-if="useMainTextarea" class="mt-5 block">
      <span v-if="structuredFields.length" class="text-sm font-semibold text-slate-950">补充说明</span>
      <textarea
        ref="mainTextareaRef"
        data-testid="task-expression-main"
        :value="draftValue"
        :rows="8"
        class="mt-2 w-full rounded-2xl border border-slate-200 bg-slate-50/50 px-4 py-3 text-sm leading-7 text-slate-900 outline-none transition focus:border-primary focus:bg-white focus:ring-4 focus:ring-primary/10"
        :placeholder="placeholderMerged"
        :disabled="sending"
        @input="$emit('update:draft-value', ($event.target as HTMLTextAreaElement).value)"
      />
    </label>

    <div v-if="starterChips.length" class="mt-4 flex flex-wrap gap-2">
      <button
        v-for="chip in starterChips"
        :key="chip.id"
        type="button"
        class="rounded-full border border-slate-200 bg-slate-50 px-3 py-1.5 text-xs font-medium text-slate-800 transition hover:border-primary/40 hover:bg-primary/5"
        :disabled="sending"
        @click="$emit('chip', chip.fill)"
      >
        {{ chip.label }}
      </button>
    </div>

    <p v-if="lowFrictionPrompt" class="mt-3 text-xs text-slate-500">{{ lowFrictionPrompt }}</p>

    <div v-if="showAdvance" class="mt-5 rounded-xl border border-emerald-200/80 bg-emerald-50/40 px-4 py-3">
      <p class="text-sm font-semibold text-slate-900">进入下一阶段前</p>
      <p class="mt-1 text-xs text-slate-600">勾齐下面三项后，主按钮将变为「进入下一阶段」。</p>
      <div class="mt-3 space-y-2">
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
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import type { ExecutionGuideChip } from '@/types/executionGuide'
import type { WorkbenchExpressionFieldModel, WorkbenchPhaseCode } from '@/types/taskExecutionWorkbench'

const props = withDefaults(
  defineProps<{
    draftValue: string
    placeholder: string
    placeholderSoft?: string
    sending: boolean
    helperText?: string
    lowFrictionPrompt?: string
    structuredFields: WorkbenchExpressionFieldModel[]
    structuredInputs: string[]
    starterChips: ExecutionGuideChip[]
    emphasisPhase: WorkbenchPhaseCode
    checkpointPrompt?: string
    showAdvance?: boolean
    microCheckLabels: string[]
    checks: boolean[]
    /** 有结构化字段时是否再显示大框（默认可不显示） */
    showMainTextarea?: boolean
  }>(),
  {
    placeholderSoft: '',
    helperText: '',
    lowFrictionPrompt: '',
    checkpointPrompt: '',
    showAdvance: false,
    showMainTextarea: false,
  }
)

const emit = defineEmits<{
  'update:draft-value': [value: string]
  'update:structured-inputs': [value: string[]]
  'update:checks': [value: boolean[]]
  chip: [fill: string]
}>()

const mainTextareaRef = ref<HTMLTextAreaElement | null>(null)

const placeholderMerged = computed(() => props.placeholderSoft || props.placeholder)

const useMainTextarea = computed(
  () => props.structuredFields.length === 0 || props.showMainTextarea
)

const panelClass = computed(() => {
  if (props.emphasisPhase === 'TRAINING') return 'ring-1 ring-primary/10'
  return ''
})

function emitStructured(index: number, value: string) {
  const next = [...props.structuredInputs]
  next[index] = value
  emit('update:structured-inputs', next)
}

function toggleCheck(i: number) {
  const next = [...props.checks]
  next[i] = !next[i]
  emit('update:checks', next)
}

defineExpose({
  focus: () => mainTextareaRef.value?.focus(),
})
</script>
