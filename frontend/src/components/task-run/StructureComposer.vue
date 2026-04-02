<template>
  <section
    data-testid="structure-composer"
    class="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm md:p-6"
  >
    <h2 class="text-lg font-semibold tracking-tight text-slate-950">{{ TASKRUN_COPY.structureComposerTitle }}</h2>
    <textarea
      ref="textareaRef"
      data-testid="structure-composer-input"
      :value="modelValue"
      :rows="12"
      class="mt-4 w-full rounded-2xl border border-slate-200 bg-slate-50/50 px-4 py-3 text-sm leading-7 text-slate-900 outline-none transition focus:border-accent focus:bg-white focus:ring-4 focus:ring-accent/15"
      :placeholder="placeholder"
      :disabled="sending"
      @input="onInput"
    />
    <div class="mt-3 flex flex-wrap gap-2">
      <button
        v-for="chip in chips"
        :key="chip.prefix"
        type="button"
        class="rounded-full border border-slate-200 bg-white px-3 py-1.5 text-xs font-medium text-slate-700 transition hover:border-accent/40 hover:bg-accent-muted/50"
        :disabled="sending"
        @click="insertPrefix(chip.prefix)"
      >
        {{ chip.label }}
      </button>
    </div>
  </section>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { TASKRUN_COPY } from '@/constants/uiCopy'

const props = defineProps<{
  modelValue: string
  placeholder: string
  sending: boolean
}>()

const emit = defineEmits<{
  'update:modelValue': [value: string]
}>()

const textareaRef = ref<HTMLTextAreaElement | null>(null)

const chips = [
  { label: TASKRUN_COPY.composerChip1, prefix: '它是什么：\n' },
  { label: TASKRUN_COPY.composerChip2, prefix: '它解决什么问题：\n' },
  { label: TASKRUN_COPY.composerChip3, prefix: '和相近概念的区别：\n' },
]

function onInput(e: Event) {
  emit('update:modelValue', (e.target as HTMLTextAreaElement).value)
}

function insertPrefix(prefix: string) {
  const cur = props.modelValue.trim()
  const next = cur ? `${cur}\n\n${prefix}` : prefix
  emit('update:modelValue', next)
}

defineExpose({
  focus: () => textareaRef.value?.focus(),
})
</script>
